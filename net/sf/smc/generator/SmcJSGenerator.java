//
// The contents of this file are subject to the Mozilla Public
// License Version 1.1 (the "License"); you may not use this file
// except in compliance with the License. You may obtain a copy
// of the License at http://www.mozilla.org/MPL/
// 
// Software distributed under the License is distributed on an
// "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
// implied. See the License for the specific language governing
// rights and limitations under the License.
// 
// The Original Code is State Machine Compiler (SMC).
// 
// The Initial Developer of the Original Code is Charles W. Rapp.
// Portions created by Charles W. Rapp are
// Copyright (C) 2005 - 2009. Charles W. Rapp.
// All Rights Reserved.
//
// Port to Python by Francois Perrad, francois.perrad@gadz.org
// Copyright 2004, Francois Perrad.
// All Rights Reserved.
//
// Contributor(s):
//   Eitan Suez contributed examples/Ant.
//   (Name withheld) contributed the C# code generation and
//   examples/C#.
//   Francois Perrad contributed the Python code generation and
//   examples/Python.
//   Chris Liscio contributed the Objective-C code generation
//   and examples/ObjC.
//   Toni Arnold contributed the PHP code generation and
//   examples/Php.
//
//  PHP note:
// "Exit" and "Default" are PHP keywords, so for code generation,
// "Exit_" and "Default_" is used instead - but the .sm input file
// still should use "Default" for default transitions.
//
// RCS ID
// $Id$
//
// CHANGE LOG
// (See the bottom of this file.)
//

package net.sf.smc.generator;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import net.sf.smc.model.SmcAction;
import net.sf.smc.model.SmcElement;
import net.sf.smc.model.SmcElement.TransType;
import net.sf.smc.model.SmcFSM;
import net.sf.smc.model.SmcGuard;
import net.sf.smc.model.SmcMap;
import net.sf.smc.model.SmcParameter;
import net.sf.smc.model.SmcState;
import net.sf.smc.model.SmcTransition;
import net.sf.smc.model.SmcVisitor;

/**
 * Visits the abstract syntax tree, emitting Python code.
 * @see SmcElement
 * @see SmcCodeGenerator
 * @see SmcVisitor
 *
 * @author Francois Perrad
 */

public final class SmcJSGenerator
    extends SmcCodeGenerator
{
//---------------------------------------------------------------
// Member methods
//
    private JSCode jsCode;

    //-----------------------------------------------------------
    // Constructors.
    //

    /**
     * Creates a PHP code generator for the given parameters.
     * @param srcfileBase write the emitted code to this target
     * source file name sans the suffix.
     * @param srcDirectory place the target source file in this
     * directory.
     * @param headerDirectory place the target header file in
     * this directory. Ignored if there is no generated header
     * file.
     * @param castType use this type cast (C++ code generation
     * only).
     * @param graphLevel amount of detail in the generated
     * GraphViz graph (graph code generation only).
     * @param serialFlag if {@code true}, generate unique
     * identifiers for persisting the FSM.
     * @param debugFlag if {@code true} add debug output messages
     * to code.
     * @param noExceptionFlag if {@code true} then use asserts
     * rather than exceptions (C++ only).
     * @param noCatchFlag if {@code true} then do <i>not</i>
     * generate try/catch/rethrow code.
     * @param noStreamsFlag if {@code true} then use TRACE macro
     * for debug output.
     * @param reflectFlag if {@code true} then generate
     * reflection code.
     * @param syncFlag if {@code true} then generate
     * synchronization code.
     * @param genericFlag if {@code true} then use generic
     * collections.
     */
    public SmcJSGenerator(String srcfileBase,
                           String srcDirectory,
                           String headerDirectory,
                           String castType,
                           int graphLevel,
                           boolean serialFlag,
                           boolean debugFlag,
                           boolean noExceptionFlag,
                           boolean noCatchFlag,
                           boolean noStreamsFlag,
                           boolean reflectFlag,
                           boolean syncFlag,
                           boolean genericFlag)
    {
        super (srcfileBase,
               "{0}{1}_sm.{2}",
               "js",
               srcDirectory,
               headerDirectory,
               castType,
               graphLevel,
               serialFlag,
               debugFlag,
               noExceptionFlag,
               noCatchFlag,
               noStreamsFlag,
               reflectFlag,
               syncFlag,
               genericFlag);
        jsCode = new JSCode();
    } // end of SmcPhpGenerator(...)

    //
    // end of Constructors.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // SmcVisitor Abstract Method Impelementation.
    //

    /**
     * Emits PHP code for the finite state machine.
     * @param fsm emit PHP code for this finite state machine.
     */
    public void visit(SmcFSM fsm)
    {
        String context = fsm.getContext();
        String rawSource = fsm.getSource();
        String startState = fsm.getStartState();
        String phpState;
        List<SmcMap> maps = fsm.getMaps();
        List<SmcTransition> transitions;
        List<SmcParameter> params;
        Iterator<SmcParameter> pit;
        String transName;
        String separator;

        _source.println("/*");
        _source.println(" ex: set ro:");
        _source.println(" DO NOT EDIT.");
        _source.println(
            " generated by smc (http://smc.sourceforge.net/)");
        _source.print(" from file : ");
        _source.print(_srcfileBase);
        _source.println(".sm");
        _source.println("*/");
        _source.println();

        // Dump out the raw source code, if any.
        if (rawSource != null && rawSource.length () > 0)
        {
            _source.println(rawSource);
            _source.println();
        }

        _source.println("//require_once 'StateMachine/statemap.php';");
        _source.println();

        // Do user-specified imports now.
        for (String imp: fsm.getImports())
        {
            _source.print("//require_once '");
            _source.print(imp);
            _source.println("';");
        }

        // Declare the state class.
        _source.println();
        jsCode.addClass(context+"State","State");
        jsCode.getCurrentClass().addParams("name,id").addFunction("Entry","fsm","").addFunction("Exit_","fsm","");

        // Get the transition list.
        // Generate the default transition definitions.
        transitions = fsm.getTransitions();
        for (SmcTransition trans: transitions)
        {
            params = trans.getParameters();

            // Don't generate the Default transition here.
            if (trans.getName().equals("Default") == false)
            {
                jsCode.getCurrentClass().addFunction(trans.getName(),"fsm","this.Default_(fsm);");

                for (SmcParameter param: params)
                {
                    jsCode.getCurrentClass().getCurrentFunction().addParams(param.getName());
                }

            }
        }

        // Generate the overall Default transition for all maps.
        //_source.println("    public function Default_($fsm) {");
        jsCode.getCurrentClass().addFunction("Default_","fsm");

        if (_debugFlag == true)
        {
          //  _source.println(
            //    "        if ($fsm->getDebugFlag() == true) {");
           // _source.println(
           //     "            fwrite($fsm->getDebugStream(), \"TRANSITION   : Default\\n\");");
           // _source.println(
           //     "        }");
        }

        /*_source.println(
            "        $state = $fsm->getState()->getName();");
        _source.println(
            "        $transition = $fsm->getTransition();");
        _source.println(
            "        $msg = \"\\n\\tState: $state\\n\\tTransition: $transition\";");
        _source.println(
            "        throw new TransitionUndefinedException($msg);");
        _source.println("    }");

        _source.println("}");   // end of state class
          */
        // Have each map print out its source code now.
        for (SmcMap map: maps)
        {
            map.accept(this);
        }

       phpState = phpStateName(startState);
       jsCode.addClass(context+"_sm","FSMContext","owner","this.setState("+phpState+"); this.owner=owner;");
       // _source.println();

        // Generate the transition methods.
        for (SmcTransition trans: transitions)
        {
            transName = trans.getName();
            params = trans.getParameters();

            if (transName.equals("Default") == false)
            {
                //_source.print("    public function ");
                //_source.print(transName);
                jsCode.getCurrentClass().addFunction(transName);
                //_source.print("(");

                // Now output the transition's parameters.
                params = trans.getParameters();
                for (pit = params.iterator(), separator = "";
                     pit.hasNext() == true;
                     separator = ", ")
                {
                    //_source.print(separator);
                    jsCode.getCurrentClass().getCurrentFunction().addParams(pit.next().getName());
                         
                }
                //_source.println(") {");

                // Save away the transition name in case it is
                // need in an UndefinedTransitionException.
                jsCode.getCurrentClass().getCurrentFunction().addCode("this._transition = \""+transName+"\";"+
                                                                 "     this.getState()."+transName+"(this");

                for (pit = params.iterator();
                     pit.hasNext() == true;
                    )
                {
                   // _source.print(", ");
                    jsCode.getCurrentClass().getCurrentFunction().addCode(","+(pit.next()).getName());
                }
                jsCode.getCurrentClass().getCurrentFunction().addCode("); this._transition = null;");

               // _source.println("    }");
               // _source.println();
            }
        }

        // getState() method.
       // _source.println("    public function getState() {");
        jsCode.getCurrentClass().addFunction("getState",""," "+

                "       if (this._state == null) {"+
                "            throw new StateUndefinedException();"+
                "        }"+
                "        return this._state;").
                
                               addFunction("enterStartState","","this._state.Entry(this);").
                               addFunction("getOwner","","return this._owner;");
        _source.print(jsCode.generateCode());
        return;
    } // end of visit(SmcFSM)

    /**
     * Emits PHP code for the FSM map.
     * @param map emit PHP code for this map.
     */
    public void visit(SmcMap map)
    {
        List<SmcTransition> definedDefaultTransitions;
        SmcState defaultState = map.getDefaultState();
        String context = map.getFSM().getContext();
        String mapName = map.getName();
        List<SmcState> states = map.getStates();
        boolean needPass = true;

        // Initialize the default transition list to all the
        // default state's transitions.
        if (defaultState != null)
        {
            definedDefaultTransitions =
                    defaultState.getTransitions();
        }
        else
        {
            definedDefaultTransitions =
                new ArrayList<SmcTransition>();
        }

        // Declare the map default state class.
        jsCode.addClass(mapName+"_Default",context+"State");

        // Declare the user-defined default transitions first.
        for (SmcTransition transition: definedDefaultTransitions)
        {
            transition.accept(this);
        }

        // If -reflect was specified, then generate the
        // _transitions table.
        if (_reflectFlag == true)
        {
            List<SmcTransition> allTransitions =
                map.getFSM().getTransitions();
            String transName;
            int transDefinition;

            // Generate the getTransitions() method.
            _source.println();
           // _source.println("    public function getTransitions() {");
            jsCode.getCurrentClass().addFunction("getTransitions","","return [");
            //_source.println("        return array(");

            // Now place all transition names and states into the
            // map.
            for (SmcTransition transition: allTransitions)
            {
                transName = transition.getName();

                // If the transition is defined in this map's
                // default state, then the value is 2.
                if (definedDefaultTransitions.contains(
                        transition) == true)
                {
                    transDefinition = 2;
                }
                // Otherwise the value is 0 - undefined.
                else
                {
                    transDefinition = 0;
                }

                jsCode.getCurrentClass().getCurrentFunction().addCode("  '"+transName+"':"+transDefinition+",");
            }
            jsCode.getCurrentClass().getCurrentFunction().addCode("        ];");
        }



        // Have each state now generate its code.
        for (SmcState state: states)
        {
            state.accept(this);
        }

        // Declare and initialize the map class.
        // In PHP, static objects need to be instantiated
        // outside of the class, see
        // http://ch2.php.net/manual/en/language.oop5.static.php#51627
      //  _source.println();
      //  _source.print("class ");
      //  _source.print(mapName);
      //  _source.println(" {");
          jsCode.addClass(mapName,"");
        // declare the static members
        for (SmcState state: states)
        {
            jsCode.getCurrentClass().addStaticMember(state.getInstanceName(),"new "+mapName+"_"+state.getClassName()+"('"+mapName+"."+state.getClassName()+"',"+map.getNextStateId()+");");
        }
        jsCode.getCurrentClass().addStaticMember("Default_"," new "+mapName+"_Default('"+mapName+".Default_',-1);");
        //_source.println("}");

        // after the class declaration, instantiate the static members
        /* 
        for (SmcState state: states)
        {
            _source.print(mapName);
            _source.print("::$");
            _source.print(state.getInstanceName());
            _source.print(" = new ");
            _source.print(mapName);
            _source.print('_');
            _source.print(state.getClassName());
            _source.print("('");
            _source.print(mapName);
            _source.print('.');
            _source.print(state.getClassName());
            _source.print("', ");
            _source.print(map.getNextStateId());
            _source.println(");");
        }

        // Instantiate a default state as well.
        _source.print(mapName);
        _source.print("::$Default_");
        _source.print(" = new ");
        _source.print(mapName);
        _source.print("_Default('");
        _source.print(mapName);
        _source.println(".Default_', -1);");

        */
        return;
    } // end of visit(SmcMap)

    /**
     * Emits PHP code for this FSM state.
     * @param state emits PHP code for this state.
     */
    public void visit(SmcState state)
    {
        
        SmcMap map = state.getMap();
        String mapName = map.getName();
        String stateName = state.getClassName();
        List<SmcAction> actions;
        String indent2;
        boolean needPass = true;

        // Declare the state class.
        //_source.println();
        //_source.print("class ");
        //_source.print(mapName);
        //_source.print('_');
        //_source.print(stateName);
        //_source.print(" extends ");
        //_source.print(mapName);
        //_source.println("_Default {");
        jsCode.addClass(mapName+"_"+stateName,mapName+"_Default");
        // Add the Entry() and Exit_() member functions if this
        // state defines them.
        actions = state.getEntryActions();
        if (actions != null && actions.size() > 0)
        {
            needPass = false;

         //   _source.println();
         //   _source.println("    public function Entry($fsm) {");
            jsCode.getCurrentClass().addFunction("Entry","fsm","ctxt = fsm.getOwner();");

            // Declare the "ctxt" local variable.
          //  addCode("        ctxt = fsm.getOwner();");

            // Generate the actions associated with this code.
            indent2 = _indent;
            _indent = "        ";
            for (SmcAction action: actions)
            {
                action.accept(this);
            }
            _indent = indent2;

           // _source.println("    }");
        }

        actions = state.getExitActions();
        if (actions != null && actions.size() > 0)
        {
            needPass = false;

            //_source.println();
            //_source.println("    public function Exit_($fsm) {");
            jsCode.getCurrentClass().addFunction("Exit_","fsm","ctxt = fsm.getOwner();\n");

            // Generate the actions associated with this code.
            indent2 = _indent;
            _indent = "        ";
            for (SmcAction action: actions)
            {
                action.accept(this);
            }
            _indent = indent2;

           // _source.println("    }");
        }

        // Have each transition generate its code.
        for (SmcTransition transition: state.getTransitions())
        {
            transition.accept(this);
        }

        // If -reflect was specified, then generate the
        // _transitions table.
        if (_reflectFlag == true)
        {
            List<SmcTransition> allTransitions =
                map.getFSM().getTransitions();
            List<SmcTransition> stateTransitions =
                state.getTransitions();
            SmcState defaultState = map.getDefaultState();
            List<SmcTransition> defaultTransitions;
            String transName;
            int transDefinition;

            // Initialize the default transition list to all the
            // default state's transitions.
            if (defaultState != null)
            {
                defaultTransitions =
                    defaultState.getTransitions();
            }
            else
            {
                defaultTransitions =
                    new ArrayList<SmcTransition>();
            }

            // Generate the getTransitions() method.
            //_source.println();
            //_source.println("    public function getTransitions() {");
            //_source.println("        return array(");
              jsCode.getCurrentClass().addFunction("getTransition","return [");
            // Now place all transition names and states into the
            // map.
            for (SmcTransition transition: allTransitions)
            {
                transName = transition.getName();

                // If the transition is in this state, then its
                // value is 1.
                if (stateTransitions.contains(
                        transition) == true)
                {
                    transDefinition = 1;
                }
                // If the transition is defined in this map's
                // default state, then the value is 2.
                else if (defaultTransitions.contains(
                             transition) == true)
                {
                    transDefinition = 2;
                }
                // Otherwise the value is 0 - undefined.
                else
                {
                    transDefinition = 0;
                }

                //_source.print("            '");
                jsCode.getCurrentClass().getCurrentFunction().addCode(transName+":"+transDefinition+",");
              //  _source.print("' => ");
               // _source.print(transDefinition);
               // _source.println(",");
            }
            jsCode.getCurrentClass().getCurrentFunction().addCode("];");
            //_source.println("    }");
        }

       // _source.println();
       // _source.println("}");

        // End of this state class declaration.
      
        return;
    } // end of visit(SmcState)

    /**
     * Emits PHP code for this FSM state transition.
     * @param transition emits PHP code for this state transition.
     */
    public void visit(SmcTransition transition)
    {
        
        SmcState state = transition.getState();
        SmcMap map = state.getMap();
        String mapName = map.getName();
        String stateName = state.getClassName();
        String transName = transition.getName();
        List<SmcParameter> parameters =
            transition.getParameters();
        List<SmcGuard> guards = transition.getGuards();
        boolean nullCondition = false;
        Iterator<SmcParameter> pit;
        Iterator<SmcGuard> git;
        SmcGuard guard;

        //_source.println();
        //_source.print("    public function ");
        JSClass jsClass = jsCode.getCurrentClass();
        jsClass.addFunction(sanitizeKeyword(transName),"fsm");
        JSFunction jsFunc = jsClass.getCurrentFunction();
       // _source.print("($fsm");

        // Add user-defined parameters.
        for (SmcParameter param: parameters)
        {
         //   _source.print(", ");
            jsClass.getCurrentFunction().addParams(param.getName());
        }
       // _source.println(") {");

        // All transitions have a "ctxt" local variable.
        // 8/14/2003:
        // Do this only if there are any transition actions or
        // guard conditions which reference it.
        if (transition.hasCtxtReference() == true)
        {
            jsFunc.addCode("     var   ctxt = fsm.getOwner(); ");
        }
         
        // Output transition to debug stream.
        if (_debugFlag == true)
        {
            String sep;

            jsFunc.addCode(
                "        if (fsm.getDebugFlag() == true) {}");
           /* addCode(
                "            fwrite(fsm.getDebugStream(), \"TRANSITION   : ");
            addCode(mapName);
            addCode("::\\$");
            addCode(stateName);
            addCode"->");
            addCode(transName);

            _source.print("(");
            for (pit = parameters.iterator(), sep = "";
                 pit.hasNext() == true;
                 sep = ", ")
            {
                _source.print(sep);
                (pit.next()).accept(this);
            }
            _source.print(");");

            _source.println("\\n\");");*/
           // _source.println(
             //   "        }");
        }

        // Loop through the guards and print each one.
        _indent = "        ";
        for (git = guards.iterator(),
                  _guardIndex = 0,
                  _guardCount = guards.size();
             git.hasNext() == true;
             ++_guardIndex)
        {
            guard = git.next();

            // Count up the guards with no condition.
            if (guard.getCondition().length() == 0)
            {
                nullCondition = true;
            }

            guard.accept(this);
        }

        // If all guards have a condition, then create a final
        // "else" clause which passes control to the default
        // transition. Pass all arguments into the default
        // transition.
        if (_guardIndex > 0 && nullCondition == false)
        {
            jsFunc.addCode("        } else {");

            // Call the super class' transition method using
            // the "parent" keyword and not the class name.
            jsFunc.addCode("            ");
            jsFunc.addCode(jsCode.getCurrentClass().getBaseClass()+".prototype."+transName+".apply(this,arguments);");
           /* _source.print("($fsm");

            for (SmcParameter param: parameters)
            {
                _source.print(", ");
                _source.print(param.getName());
            }

            _source.println(");");*/
           jsFunc.addCode("        }");
        }
        // Need to add a final newline after a multiguard block.
        else if (_guardCount > 1)
        {
         //   _source.println();
        }

          //  jsFunc.addCode("    }");
        return;
    } // end of visit(SmcTransition)

    /**
     * Emits PHP code for this FSM transition guard.
     * @param guard emits PHP code for this transition guard.
     */
    public void visit(SmcGuard guard)
    {
        
        SmcTransition transition = guard.getTransition();
        SmcState state = transition.getState();
        SmcMap map = state.getMap();
        String context = map.getFSM().getContext();
        String mapName = map.getName();
        String stateName = state.getClassName();
        TransType transType = guard.getTransType();
        boolean loopbackFlag = false;
        String indent2;
        String indent3;
        String indent4;
        String endStateName = guard.getEndState();
        String fqEndStateName = "";
        String pushStateName = guard.getPushState();
        String condition = guard.getCondition();
        List<SmcAction> actions = guard.getActions();
        JSFunction jsFunc = jsCode.getCurrentClass().getCurrentFunction();
        // If this guard's end state is not of the form
        // "map::state", then prepend the map name to the
        // state name.
        // DON'T DO THIS IF THIS IS A POP TRANSITION!
        // The "state" is actually a transition name.
        if (transType != TransType.TRANS_POP &&
            endStateName.length () > 0 &&
            endStateName.equals(SmcElement.NIL_STATE) == false &&
            endStateName.indexOf("::") < 0)
        {
            endStateName = mapName + "." + endStateName;
        }
        endStateName = phpStateName(endStateName);

        // Qualify the state and push state names as well.
        if (stateName.indexOf("::") < 0)
        {
            stateName = mapName + "." + stateName;
        }
        stateName = phpStateName(stateName);

        // v. 2.0.2: If the push state is not fully-qualified,
        // then prepend the current map's name and make if
        // fully-qualified.
        if (pushStateName != null &&
            pushStateName.length() > 0)
        {
            if (pushStateName.indexOf("::") < 0)
            {
                pushStateName = mapName + "." + pushStateName;
            }
        }
        pushStateName = phpStateName(pushStateName);

        loopbackFlag = isLoopback(transType, endStateName);

        // The guard code generation is a bit tricky. The first
        // question is how many guards are there? If there are
        // more than one, then we will need to generate the
        // proper "if-then-else" code.
        if (_guardCount > 1)
        {
            indent2 = _indent + "    ";

            // There are multiple guards. Is this the first
            // guard?
            if (_guardIndex == 0 && condition.length() > 0)
            {
                // Yes, this is the first. This means an "if"
                // should be used.
                //addCode(_indent);
                jsFunc.addCode("if ("+condition+") {");
            }
            else if (condition.length() > 0)
            {
                // No, this is not the first transition but it
                // does have a condition. Use an "else if".
               // addCode(_indent);
                 jsFunc.addCode("} elseif ("+condition+") {");
            }
            else
            {
                // This is not the first transition and it has
                // no condition.
                jsFunc.addCode("else {");
            }
        }
        // There is only one guard. Does this guard have
        // a condition?
        else if (condition.length() == 0)
        {
            // No. This is a plain, old. vanilla transition.
            indent2 = _indent;
        }
        else
        {
            // Yes there is a condition.
            indent2 = _indent + "    ";

            //addCode(_indent);
            jsFunc.addCode("if ("+condition+") {");
        }

        // Now that the necessary conditions are in place, it's
        // time to dump out the transition's actions. First, do
        // the proper handling of the state change. If this
        // transition has no actions, then set the end state
        // immediately. Otherwise, unset the current state so
        // that if an action tries to issue a transition, it will
        // fail.
        if (actions.size() == 0 && endStateName.length() != 0)
        {
            fqEndStateName = endStateName;
        }
        else if (actions.size() > 0)
        {
            // Save away the current state if this is a loopback
            // transition. Storing current state allows the
            // current state to be cleared before any actions are
            // executed. Remember: actions are not allowed to
            // issue transitions and clearing the current state
            // prevents them from doing do.
            if (loopbackFlag == true)
            {
                fqEndStateName = "endState";

                //addCode(indent2);
                jsFunc.addCode(fqEndStateName+" = fsm.getState();");
            }
            else
            {
                fqEndStateName = endStateName;
            }
        }

        // Dump out the exit actions - but only for the first
        // guard.
        // v. 1.0, beta 3: Not any more. The exit actions are
        // executed only if 1) this is a standard, non-loopback
        // transition or a pop transition.
        if (transType == TransType.TRANS_POP ||
            loopbackFlag == false)
        {
            //addCode(indent2);
            jsFunc.addCode("fsm.getState().Exit_(fsm);");
        }

        // Dump out this transition's actions.
        if (actions.isEmpty() == true)
        {
            List<SmcAction> entryActions =
                state.getEntryActions();
            List<SmcAction> exitActions = state.getExitActions();

            // If this is an if or else body, then give it a
            // pass.
            if (condition.length() > 0 ||
                _guardCount > 1)
            {
               // addCode(indent2);
                jsFunc.addCode("//# No actions.");
            }
            // If there are:
            // 1. No entry actions,
            // 2. No exit actions,
            // 3. Only one guard,
            // 4. No condition,
            // 5. No actions,
            // 6. Not a loopback, push or pop transition and
            // 7. No debug code being generated.
            // then give this transition a pass.
            else if (_guardCount == 1 &&
                     (entryActions == null ||
                      entryActions.isEmpty() == true) &&
                     (exitActions == null ||
                      exitActions.isEmpty() == true) &&
                     transType != TransType.TRANS_PUSH &&
                     transType != TransType.TRANS_POP &&
                     loopbackFlag == true &&
                     _debugFlag == false)
            {
              //  addCode(indent2);
                jsFunc.addCode("//# No actions.");
              //  addCode(indent2);
            }

            indent3 = indent2;
        }
        else
        {
            // Now that we are in the transition, clear the
            // current state.
           // addCode(indent2);
            jsFunc.addCode("fsm.clearState();");

            // v. 2.0.0: Place the actions inside a try/finally
            // block. This way the state will be set before an
            // exception leaves the transition method.
            // v. 2.2.0: Check if the user has turned off this
            // feature first.
            if (_noCatchFlag == false)
            {
                //jsFunc.addCode(indent2);
                jsFunc.addCode("var exception = null;");
                jsFunc.addCode(indent2);
                jsFunc.addCode("try {");

                indent3 = indent2 + "    ";
            }
            else
            {
                indent3 = indent2;
            }

            indent4 = _indent;
            _indent = indent3;

            for (SmcAction action: actions)
            {
                action.accept(this);
            }

            _indent = indent4;

            // v. 2.2.0: Check if the user has turned off this
            // feature first.
            // PHP has no 'finally', thus emulate it with catch
            // and rethrow at the end.
            // http://bugs.php.net/bug.php?id=32100
            if (_noCatchFlag == false)
            {
                //addCode(indent2);
                jsFunc.addCode("}");
               // addCode(indent2);
                jsFunc.addCode("catch (exception) {}");
            }
        }

        // Print the setState() call, if necessary. Do NOT
        // generate the set state it:
        // 1. The transition has no actions AND is a loopback OR
        // 2. This is a push or pop transition.
        if (transType == TransType.TRANS_SET &&
            (actions.isEmpty() == false ||
             loopbackFlag == false))
        {
            //addCode(indent2);
            jsFunc.addCode("fsm.setState("+fqEndStateName+");");
        }
        else if (transType == TransType.TRANS_PUSH)
        {
            // Set the next state so this it can be pushed
            // onto the state stack. But only do so if a clear
            // state was done.
            if (loopbackFlag == false ||
                actions.isEmpty() == false)
            {
                //addCode(indent2);
                jsFunc.addCode("fsm.setState("+fqEndStateName+");");
            }

            // Before doing the push, execute the end state's
            // entry actions (if any) if this is not a loopback.
            if (loopbackFlag == false)
            {
                //addCode(indent2);
                jsFunc.addCode("fsm.getState().Entry(fsm);");
            }

            //addCode(indent2);
            jsFunc.addCode("fsm.pushState("+pushStateName+");");
        }
        else if (transType == TransType.TRANS_POP)
        {
            //addCode(indent2);
            jsFunc.addCode("fsm.popState();");
        }

        // Perform the new state's enty actions.
        // v. 1.0, beta 3: Not any more. The entry actions are
        // executed only if 1) this is a standard, non-loopback
        // transition or a push transition.
        if ((transType == TransType.TRANS_SET &&
             loopbackFlag == false) ||
             transType == TransType.TRANS_PUSH)
        {
            //addCode(indent2);
            jsFunc.addCode("fsm.getState().Entry(fsm);");
        }

        // If there was a try/finally, then put the closing
        // brace on the finally block.
        // v. 2.2.0: Check if the user has turned off this
        // feature first.
        // PHP: rethrow the exception to emulate finally.
        if (actions.size() > 0 && _noCatchFlag == false)
        {
            //addCode(indent2);
            jsFunc.addCode("if (exception) {    throw exception; }");
        }

        // If there is a transition associated with the pop, then
        // issue that transition here.
        if (transType == TransType.TRANS_POP &&
            endStateName.equals(SmcElement.NIL_STATE) == false &&
            endStateName.length() > 0)
        {
            String popArgs = guard.getPopArgs();

            //addCode(indent2);
            jsFunc.addCode("fsm."+endStateName+"(");

            // Output any and all pop arguments.
            if (popArgs.length() > 0)
            {
                jsFunc.addCode(popArgs+");");
            }
            else 
            {
                jsFunc.addCode(");");
            }
        }

        // If this is a guarded transition, it will be necessary
        // to close off the "if" body. DON'T PRINT A NEW LINE!
        // Why? Because an "else" or "else if" may follow and we
        // won't know until we go back to the transition source
        // generator whether all clauses have been done.
        if (_guardCount > 1)
        {
           // addCode(_indent);
            jsFunc.addCode("}");
        }
        
        return;
    } // end of visit(SmcGuard)

    /**
     * Emits PHP code for this FSM action.
     * @param action emits PHP code for this action.
     */
    public void visit(SmcAction action)
    {
        
        String name = action.getName();
        Iterator<String> it;
        String sep;
        JSFunction jsFunc = jsCode.getCurrentClass().getCurrentFunction();
        // Need to distinguish between FSMContext actions and
        // application class actions. If the action is
        // "emptyStateStack", then pass it to the context.
        // Otherwise, let the application class handle it.
        _source.print(_indent);
        if (name.equals("emptyStateStack") == true)
        {
            jsFunc.addCode("fsm.");
        }
        else
        {
            jsFunc.addCode("ctxt.");
        }
        jsFunc.addCode(name);
        jsFunc.addCode("(");

        for (it = action.getArguments().iterator(), sep = "";
             it.hasNext() == true;
             sep = ", ")
        {
            jsFunc.addCode(sep);
            jsFunc.addCode(it.next());
        }

        jsFunc.addCode(");");

        return;
    } // end of visit(SmcAction)

    /**
     * Emits PHP code for this transition parameter.
     * @param parameter emits PHP code for this transition
     * parameter.
     */
    public void visit(SmcParameter parameter)
    {
        String type = parameter.getType();

        if (type.equals(""))
        {
            _source.print(parameter.getName());
        }
        else
        {
            _source.print(type);
            _source.print(" ");
            _source.print(parameter.getName());
        }

        return;
    } // end of visit(SmcParameter)

    //
    // end of SmcVisitor Abstract Method Impelementation.
    //-----------------------------------------------------------

    // State names like "map::state" must be changed to
    // "map::$state" for accessing static members in PHP.
    private String phpStateName(String state)
    {
        int index = state.indexOf("::");
        String retval = state;

        if (index >= 0)
        {
            retval =
                state.substring(0, index) +
                "." +
                state.substring(index + 2);
        }

        return (retval);
    } // end of phpStateName(String)

    // Sanitize PHP keywords used as transition name
    // by appending a "_".
    // Extend the conditional for other keywords surfacing.
    private String sanitizeKeyword(String transName)
    {
        String retval = transName;

        if (transName.equalsIgnoreCase("Default") )
        {
            retval = transName + "_";
        }

        return (retval);
    } // end of sanitizeKeywork(String)

//---------------------------------------------------------------
// Member data
//
} // end of class SmcJSGenerator

// JavaScript Code generator
class JSCode {
    private ArrayList<JSClass> jsClasses;
    JSCode(){
         jsClasses = new ArrayList<JSClass>();
    }
    public JSClass addClass(String name,String baseClass){
        return addClass(name,baseClass,"","");
    }
    public JSClass addClass(String name,String baseClass,String params){
        return addClass(name,baseClass,params,"");
    }
    public JSClass addClass(String name,String baseClass,String params,String code){
         JSClass jsClass = new JSClass();
         jsClass.setName(name);
         jsClass.setBaseClass(baseClass);
         jsClass.addParams(params);
         jsClass.addConstructorCode(code);
         jsClasses.add(jsClass);
         return jsClass;
    }
    public JSClass getCurrentClass(){
        return jsClasses.get(jsClasses.size()-1);
    }
    public String generateCode(){
        String tmp="";
        for(JSClass jsc:jsClasses){
            tmp+=jsc.generateCode();
        }
        return tmp;
    }
    

}
class JSClass {
    private String name;
    private String baseClass;
    private String argumentList;
    private String constructorCode;
    private HashMap<String,String> staticMembers;
    private ArrayList<JSFunction> functionList;
    public JSClass(){
        this.constructorCode="";
        this.argumentList="";
        staticMembers = new HashMap<String,String>();
    }
    public void setName(String cn){
        this.name = cn;
    }
    public void setBaseClass(String bc){
        this.baseClass = bc;
    }
    public JSClass addParams(String p){
        if(argumentList==null||argumentList.length()<1){
            argumentList=p;
        } else {
            argumentList+=","+p;
        }
        functionList = new ArrayList<JSFunction>();
        return this;
    }
    public JSClass addConstructorCode(String cc){
        this.constructorCode += cc;
        return this;
    }
    public void setFunctionList(ArrayList<JSFunction> fl){
        this.functionList = fl;
    }

    public JSClass addFunction(String name){
        return addFunction(name,"");
    }
    public JSClass addFunction(String name,String arg){
        return addFunction(name,arg,"");
    }
    public JSClass addFunction(String name,String arg,String code){
        JSFunction jsFunc = new JSFunction();
        jsFunc.setName(name);
        jsFunc.setArgumentList(arg);
        jsFunc.addCode(code);
        functionList.add(jsFunc);
        return this;
    }
    public JSClass addStaticMember(String key,String val){
        staticMembers.put(key,val);
        return this;
    }
    public JSFunction getCurrentFunction(){
        return functionList.get(functionList.size()-1);
    }
    public String getBaseClass(){
        return baseClass;
    }
    public String generateCode(){
        String tmpl="";
        tmpl+="function "+name+" ("+argumentList+"){\n";
        if(baseClass!=null&&baseClass.length()>0){
            tmpl+=baseClass+".apply(this,arguments);\n";
        }
        tmpl+=constructorCode+"\n}\n";
        tmpl+="\nState.mixin("+name+".prototype";
        if(baseClass!=null&&baseClass.length()>0){
            tmpl+=",State.mixin(new "+baseClass+"()";
        }
        tmpl+=",{\n";
        String comma="";
        for(JSFunction jsFunction:functionList){
            tmpl+=comma+jsFunction.generateCode()+"\n";
            comma=",";
        }
        tmpl+="\n})";
        if(baseClass!=null&&baseClass.length()>0){
          tmpl+=")";
        }
        tmpl+=";\n";
        for(Map.Entry<String,String> me : staticMembers.entrySet()){
            String key = me.getKey();
            String val = me.getValue();
            tmpl+=name+"."+key+"="+val+";\n";
        }
        return tmpl;
    }
}

class JSFunction {
    private String name;
    private String argumentList;
    private String functionCode;
    JSFunction(){
        functionCode="";
    }
    public void setName(String fn){
        name = fn;
    }
    public void setArgumentList(String al){
        argumentList = al;
    }
    public String getArgumentList(){
        return argumentList;
    }
    public JSFunction addParams(String p){
        if(p!=null&&p.length()>0){
            argumentList+=","+p;
        } else {
            argumentList = p;
        }
        return this;
    }
    public void setCode(String fc){
        functionCode = fc;
    }
    public JSFunction addCode(String fc){
        functionCode+=fc;
        return this;
    }
    public String generateCode(){
        return name+":function("+argumentList+") {\n"+functionCode+"\n}";
    }
}



//
// CHANGE LOG
// $Log$
// Revision 1.1  2011/02/14 18:32:04  nitin-nizhawan
// added generator class for JavaScript
//
// Revision 1.3  2009/04/22 20:26:29  fperrad
// Added enterStartState method
//
// Revision 1.2  2009/03/27 09:41:47  cwrapp
// Added F. Perrad changes back in.
//
// Revision 1.1  2009/03/01 18:20:42  cwrapp
// Preliminary v. 6.0.0 commit.
//
//