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
// Copyright (C) 2005, 2008 - 2009. Charles W. Rapp.
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
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
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
 * Visits the abstract syntax tree, emitting Ruby code.
 * @see SmcElement
 * @see SmcCodeGenerator
 * @see SmcVisitor
 *
 * @author Francois Perrad
 */

public final class SmcRubyGenerator
    extends SmcCodeGenerator
{
//---------------------------------------------------------------
// Member methods
//

    //-----------------------------------------------------------
    // Constructors.
    //

    /**
     * Creates a Ruby code generator for the given parameters.
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
    public SmcRubyGenerator(String srcfileBase,
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
               "rb",
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
    } // end of SmcRubyGenerator(...)

    //
    // end of Constructors.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // SmcVisitor Abstract Method Impelementation.
    //

    /**
     * Emits Ruby code for the finite state machine.
     * @param fsm emit Ruby code for this finite state machine.
     */
    public void visit(SmcFSM fsm)
    {
        String packageName = fsm.getPackage();
        String context = fsm.getContext();
        String rawSource = fsm.getSource();
        String startState = fsm.getStartState();
        List<SmcMap> maps = fsm.getMaps();
        List<SmcTransition> transitions;
        List<SmcParameter> params;
        String transName;
        int packageDepth = 0;
        int index;

        _source.println("# ex: set ro:");
        _source.println("# DO NOT EDIT.");
        _source.println(
            "# generated by smc (http://smc.sourceforge.net/)");
        _source.print("# from file : ");
        _source.print(_srcfileBase);
        _source.println(".sm");
        _source.println();

        // Dump out the raw source code, if any.
        if (rawSource != null && rawSource.length () > 0)
        {
            _source.println(rawSource);
            _source.println();
        }

        // If a package has been specified, then output that
        // module now. If the package name is "a::b::c", then
        // this must be converted to:
        // module a
        //   module b
        //     module c
        //       ...
        //     end
        //   end
        // end
        _indent = "";
        if (packageName != null && packageName.length() > 0)
        {
            StringTokenizer tokenizer =
                new StringTokenizer(packageName, "::");
            String token;

            while (tokenizer.hasMoreTokens() == true)
            {
                token = tokenizer.nextToken();
                ++packageDepth;

                _source.print(_indent);
                _source.print("module ");
                _source.println(token);
                _source.println();
                _indent += "    ";
            }
        }
        _source.print(_indent);
        _source.println("require 'statemap'");

        // Do user-specified imports now.
        for (String imp: fsm.getImports())
        {
            _source.print(_indent);
            _source.print("require '");
            _source.print(imp);
            _source.println("'");
        }

        // Declare the inner state class.
        _source.println();
        _source.print(_indent);
        _source.print("class ");
        _source.print(context);
        _source.println("State < Statemap::State");
        _source.println();

        _source.print(_indent);
        _source.println("    def Entry(fsm) end");
        _source.println();
        _source.print(_indent);
        _source.println("    def Exit(fsm) end");
        _source.println();

        // Don't generate the default transition methods.
        // Use automatic delegation.
        _source.print(_indent);
        _source.println("    def method_missing(name, *args)");
        _source.print(_indent);
        _source.println("        fsm = args.shift");

        // If this method is reached, that means that
        // this transition was passed to a state which
        // does not define the transition. Call the
        // state's default transition method.
        _source.print(_indent);
        _source.println("        Default(fsm)");

        _source.print(_indent);
        _source.println("    end");
        _source.println();

        // Generate the overall Default transition for all maps.
        _source.print(_indent);
        _source.println("    def Default(fsm)");

        if (_debugFlag == true)
        {
            _source.print(_indent);
            _source.println(
                "        if fsm.getDebugFlag then");
            _source.print(_indent);
            _source.println(
                "            fsm.getDebugStream.write(\"TRANSITION   : Default\\n\")");
            _source.print(_indent);
            _source.println("        end");
        }

        _source.print(_indent);
        _source.println(
            "        msg = \"\\nState: \" + fsm.getState.getName +");
        _source.print(_indent);
        _source.println(
            "            \"\\nTransition: \" + fsm.getTransition.to_s + \"\\n\"");
        _source.print(_indent);
        _source.println(
            "        raise Statemap::TransitionUndefinedException, msg");
        _source.print(_indent);
        _source.println("    end");

        // End of context class.
        _source.println();
        _source.print(_indent);
        _source.println("end");

        // Have each map print out its source code now.
        for (SmcMap map: maps)
        {
            map.accept(this);
        }

        // The context class contains all the state classes as
        // inner classes, so generate the context first rather
        // than last.
        _source.println();
        _source.print(_indent);
        _source.print("class ");
        _source.print(context);
        _source.println("_sm < Statemap::FSMContext");

        // Generate the context class' constructor.
        _source.println();
        _source.print(_indent);
        _source.println("    def initialize(owner)");
        _source.print(_indent);
        _source.println("        super()");
        _source.print(_indent);
        _source.println("        @_owner = owner");

        _source.print(_indent);
        _source.print("        setState(");
        _source.print(startState);
        _source.println(")");

        // Execute the start state's entry actions.
        _source.print(_indent);
        _source.print("        ");
        _source.print(startState);
        _source.println(".Entry(self)");

        _source.print(_indent);
        _source.println("    end");
        _source.println();

        // Don't generate the transition methods.
        // Use automatic delegation.
        _source.print(_indent);
        _source.println("    def method_missing(name, *args)");
        _source.print(_indent);
        _source.println("        @_transition = name");
        _source.print(_indent);
        _source.println("        getState.send(name, self, *args)");
        _source.print(_indent);
        _source.println("        @_transition = nil");
        _source.print(_indent);
        _source.println("    end");
        _source.println();

        // getOwner() method.
        _source.print(_indent);
        _source.println("    def getOwner()");
        _source.print(_indent);
        _source.println("        return @_owner");
        _source.print(_indent);
        _source.println("    end");
        _source.println();

        _source.print(_indent);
        _source.println("end");

        // If necessary, place an end for the module.
        if (packageName != null && packageName.length() > 0)
        {
            int i;
            int j;

            for (i = (packageDepth - 1); i >= 0; --i)
            {
                _source.println();
                // Output the proper indent.
                for (j = 0; j < i; ++j)
                {
                    _source.print("    ");
                }

                _source.println("end");
            }
        }

        _source.println();
        _source.println("# Local variables:");
        _source.println("#  buffer-read-only: t");
        _source.println("# End:");

        return;
    } // end of visit(SmcFSM)

    /**
     * Emits Ruby code for the FSM map.
     * @param map emit Ruby code for this map.
     */
    public void visit(SmcMap map)
    {
        List<SmcTransition> definedDefaultTransitions;
        SmcState defaultState = map.getDefaultState();
        String context = map.getFSM().getContext();
        String mapName = map.getName();
        List<SmcState> states = map.getStates();
        String indent2;

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
        _source.println();
        _source.print(_indent);
        _source.print("class ");
        _source.print(mapName);
        _source.print("_Default < ");
        _source.print(context);
        _source.println("State");

        // Declare the user-defined default transitions first.
        indent2 = _indent;
        _indent = _indent + "    ";
        for (SmcTransition transition: definedDefaultTransitions)
        {
            transition.accept(this);
        }
        _indent = indent2;

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
            _source.print(_indent);
            _source.println("    def getTransitions()");
            _source.print(_indent);
            _source.println("        return {");

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

                _source.print(_indent);
                _source.print("            '");
                _source.print(transName);
                _source.print("' => ");
                _source.print(transDefinition);
                _source.println(",");
            }
            _source.print(_indent);
            _source.println("        }");
            _source.print(_indent);
            _source.println("    end");
        }

        _source.println();
        _source.print(_indent);
        _source.println("end");

        // Have each state now generate its code. Each state
        // class is an inner class.
        for (SmcState state: states)
        {
            state.accept(this);
        }

        // Initialize the map.
        _source.println();
        _source.print(_indent);
        _source.print("module ");
        _source.println(mapName);
        _source.println();

        for (SmcState state: states)
        {
            _source.print(_indent);
            _source.print("    ");
            _source.print(state.getInstanceName());
            _source.print(" = ");
            _source.print(mapName);
            _source.print('_');
            _source.print(state.getClassName());
            _source.print("::new('");
            _source.print(mapName);
            _source.print('.');
            _source.print(state.getClassName());
            _source.print("', ");
            _source.print(map.getNextStateId());
            _source.println(").freeze");
        }

        // Instantiate a default state as well.
        _source.print(_indent);
        _source.print("    Default = ");
        _source.print(mapName);
        _source.print("_Default::new('");
        _source.print(mapName);
        _source.println(".Default', -1).freeze");

        _source.println();
        _source.print(_indent);
        _source.println("end");

        return;
    } // end of visit(SmcMap)

    /**
     * Emits Ruby code for this FSM state.
     * @param state emits Ruby code for this state.
     */
    public void visit(SmcState state)
    {
        SmcMap map = state.getMap();
        String mapName = map.getName();
        String stateName = state.getClassName();
        List<SmcAction> actions;
        String indent2;

        // Declare the inner state class.
        _source.println();
        _source.print(_indent);
        _source.print("class ");
        _source.print(mapName);
        _source.print('_');
        _source.print(stateName);
        _source.print(" < ");
        _source.print(mapName);
        _source.println("_Default");

        // Add the Entry() and Exit() member functions if this
        // state defines them.
        actions = state.getEntryActions();
        if (actions != null && actions.size() > 0)
        {
            _source.println();
            _source.print(_indent);
            _source.println("    def Entry(fsm)");

            // Declare the "ctxt" local variable.
            _source.print(_indent);
            _source.println("        ctxt = fsm.getOwner");

            // Generate the actions associated with this code.
            indent2 = _indent;
            _indent = _indent + "        ";
            for (SmcAction action: actions)
            {
                action.accept(this);
            }
            _indent = indent2;

            // End the Entry() member function with a return.
            _source.print(_indent);
            _source.println("    end");
        }

        actions = state.getExitActions();
        if (actions != null && actions.size() > 0)
        {
            _source.println();
            _source.print(_indent);
            _source.println("    def Exit(fsm)");

            // Declare the "ctxt" local variable.
            _source.print(_indent);
            _source.println("        ctxt = fsm.getOwner");

            // Generate the actions associated with this code.
            indent2 = _indent;
            _indent = _indent + "        ";
            for (SmcAction action: actions)
            {
                action.accept(this);
            }
            _indent = indent2;

            // End the Exit() member function with a return.
            _source.print(_indent);
            _source.println("    end");
        }

        // Have each transition generate its code.
        indent2 = _indent;
        _indent = _indent + "    ";
        for (SmcTransition transition: state.getTransitions())
        {
            transition.accept(this);
        }
        _indent = indent2;

        // If -reflect was specified, then generate the
        // _transitions table.
        if (_reflectFlag == true)
        {
            List<SmcTransition> allTransitions =
                map.getFSM().getTransitions();
            List<SmcTransition> stateTransitions =
                state.getTransitions();
            List<SmcTransition> defaultTransitions;
            SmcState defaultState = map.getDefaultState();
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
            _source.println();
            _source.print(_indent);
            _source.println("    def getTransitions()");
            _source.print(_indent);
            _source.println("        return {");

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

                _source.print(_indent);
                _source.print("            '");
                _source.print(transName);
                _source.print("' => ");
                _source.print(transDefinition);
                _source.println(",");
            }
            _source.print(_indent);
            _source.println("        }");
            _source.print(_indent);
            _source.println("    end");
        }

        // End of this state class declaration.
        _source.println();
        _source.print(_indent);
        _source.println("end");

        return;
    } // end of visit(SmcState)

    /**
     * Emits Ruby code for this FSM state transition.
     * @param transition emits Ruby code for this state transition.
     */
    public void visit(SmcTransition transition)
    {
        SmcState state = transition.getState();
        SmcMap map = state.getMap();
        String packageName = map.getFSM().getPackage();
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
        String indent2;

        _source.println();
        _source.print(_indent);
        _source.print("def ");
        _source.print(transName);
        _source.print("(fsm");

        // Add user-defined parameters.
        for (SmcParameter param: parameters)
        {
            _source.print(", ");
            _source.print(param.getName());
        }
        _source.println(")");

        // All transitions have a "ctxt" local variable.
        // 8/14/2003:
        // Do this only if there are any transition actions or
        // guard conditions which reference it.
        if (transition.hasCtxtReference() == true)
        {
            _source.print(_indent);
            _source.println("    ctxt = fsm.getOwner");
        }

        // Output transition to debug stream.
        if (_debugFlag == true)
        {
            String sep;

            _source.print(_indent);
            _source.println(
                "    if fsm.getDebugFlag then");
            _source.print(_indent);
            _source.print(
                "        fsm.getDebugStream.write(\"TRANSITION   : ");
            if (packageName != null && packageName.length() > 0)
            {
                _source.print(packageName);
                _source.print("::");
            }
            _source.print(mapName);
            _source.print("::");
            _source.print(stateName);
            _source.print(".");
            _source.print(transName);

            if (parameters.size() != 0)
            {
                _source.print("(");
                for (pit = parameters.iterator(), sep = "";
                     pit.hasNext() == true;
                     sep = ", ")
                {
                    _source.print(sep);
                    _source.print((pit.next()).getName());
                }
                _source.print(")");
            }

            _source.println("\\n\")");
            _source.print(_indent);
            _source.println("    end");
        }

        // Loop through the guards and print each one.
        indent2 = _indent;
        _indent = _indent + "    ";
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
        _indent = indent2;

        // If all guards have a condition, then create a final
        // "else" clause which passes control to the default
        // transition. Pass all arguments into the default
        // transition.
        if (_guardIndex > 0 && nullCondition == false)
        {
            _source.print(_indent);
            _source.println("    else");

            // Call the super class' transition method using
            // the "super" keyword and not the class name.
            _source.print(_indent);
            _source.println("        super");
            _source.print(_indent);
            _source.println("    end");
        }
        // Need to add a final newline after a multiguard block.
        else if (_guardCount > 1)
        {
            _source.print(_indent);
            _source.println("    end");
        }

        _source.print(_indent);
        _source.println("end");

        return;
    } // end of visit(SmcTransition)

    /**
     * Emits Ruby code for this FSM transition guard.
     * @param guard emits Ruby code for this transition guard.
     */
    public void visit(SmcGuard guard)
    {
        SmcTransition transition = guard.getTransition();
        SmcState state = transition.getState();
        SmcMap map = state.getMap();
        String packageName = map.getFSM().getPackage();
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
            endStateName = mapName + "::" + endStateName;
        }

        // Qualify the state and push state names as well.
        if (stateName.indexOf("::") < 0)
        {
            stateName = mapName + "::" + stateName;
        }

        // v. 2.0.2: If the push state is not fully-qualified,
        // then prepend the current map's name and make if
        // fully-qualified.
        if (pushStateName != null &&
            pushStateName.length() > 0)
        {
            if (pushStateName.indexOf("::") < 0)
            {
                pushStateName = mapName + "::" + pushStateName;
            }
            else if (packageName != null &&
                     packageName.length() > 0)
            {
                pushStateName =
                    packageName + "::" + pushStateName;
            }
        }

        loopbackFlag = isLoopback(transType, endStateName);

        // The guard code generation is a bit tricky. The first
        // question is how many guards are there? If there are
        // more than one, then we will need to generate the
        // proper "if-then-else" code.
        if (_guardCount > 1)
        {
            indent2 = _indent + "    ";

            // There are multiple guards.
            // Is this the first guard?
            if (_guardIndex == 0 && condition.length() > 0)
            {
                // Yes, this is the first. This means an "if"
                // should be used.
                _source.print(_indent);
                _source.print("if ");
                _source.print(condition);
                _source.println(" then");
            }
            else if (condition.length() > 0)
            {
                // No, this is not the first transition but it
                // does have a condition. Use an "else if".
                _source.print(_indent);
                _source.print("elsif ");
                _source.print(condition);
                _source.println(" then");
            }
            else
            {
                // This is not the first transition and it has
                // no condition.
                _source.print(_indent);
                _source.println("else");
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
            indent2 = _indent + "        ";

            _source.print(_indent);
            _source.print("    if ");
            _source.print(condition);
            _source.println(" then");
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

                _source.print(indent2);
                _source.print(fqEndStateName);
                _source.println(" = fsm.getState");
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
            _source.print(indent2);
            _source.println("fsm.getState.Exit(fsm)");
        }

        // Dump out this transition's actions.
        if (actions.size() == 0)
        {
            List<SmcAction> entryActions =
                state.getEntryActions();
            List<SmcAction> exitActions = state.getExitActions();

            if (condition.length() > 0)
            {
                _source.print(indent2);
                _source.println("# No actions.");
            }

            indent3 = indent2;
        }
        else
        {
            // Now that we are in the transition, clear the
            // current state.
            _source.print(indent2);
            _source.println("fsm.clearState");

            // v. 2.0.0: Place the actions inside a try/finally
            // block. This way the state will be set before an
            // exception leaves the transition method.
            // v. 2.2.0: Check if the user has turned off this
            // feature first.
            if (_noCatchFlag == false)
            {
                _source.print(indent2);
                _source.println("begin");

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
            if (_noCatchFlag == false)
            {
                if (_debugFlag == true)
                {
                    _source.print(indent2);
                    _source.println("rescue RuntimeError => e");
                    _source.print(indent2);
                    _source.println(
                        "    fsm.getDebugStream.write e");
                }

                _source.print(indent2);
                _source.println("ensure");
            }
        }

        // Print the setState() call, if necessary. Do NOT
        // generate the set state it:
        // 1. The transition has no actions AND is a loopback OR
        // 2. This is a push or pop transition.
        if (transType == TransType.TRANS_SET &&
            (actions.size() > 0 || loopbackFlag == false))
        {
            _source.print(indent3);
            _source.print("fsm.setState(");
            _source.print(fqEndStateName);
            _source.println(")");
        }
        else if (transType == TransType.TRANS_PUSH)
        {
            // Set the next state so this it can be pushed
            // onto the state stack. But only do so if a clear
            // state was done.
            if (loopbackFlag == false || actions.size() > 0)
            {
                _source.print(indent3);
                _source.print("fsm.setState(");
                _source.print(fqEndStateName);
                _source.println(")");
            }

            // Before doing the push, execute the end state's
            // entry actions (if any) if this is not a loopback.
            if (loopbackFlag == false)
            {
                _source.print(indent3);
                _source.println("fsm.getState.Entry(fsm)");
            }

            _source.print(indent3);
            _source.print("fsm.pushState(");
            _source.print(pushStateName);
            _source.println(")");
        }
        else if (transType == TransType.TRANS_POP)
        {
            _source.print(indent3);
            _source.println("fsm.popState");
        }

        // Perform the new state's enty actions.
        // v. 1.0, beta 3: Not any more. The entry actions are
        // executed only if 1) this is a standard, non-loopback
        // transition or a push transition.
        if ((transType == TransType.TRANS_SET &&
             loopbackFlag == false) ||
             transType == TransType.TRANS_PUSH)
        {
            _source.print(indent3);
            _source.println("fsm.getState.Entry(fsm)");
        }

        // If there was a begin/rescue/ensure, then put the
        // closing end on the block.
        // v. 2.2.0: Check if the user has turned off this
        // feature first.
        if (actions.size() > 0 && _noCatchFlag == false)
        {
            _source.print(indent2);
            _source.println("end");
        }

        // If there is a transition associated with the pop, then
        // issue that transition here.
        if (transType == TransType.TRANS_POP &&
            endStateName.equals(SmcElement.NIL_STATE) == false &&
            endStateName.length() > 0)
        {
            String popArgs = guard.getPopArgs();

            _source.print(indent2);
            _source.print("fsm.");
            _source.print(endStateName);

            // Output any and all pop arguments.
            if (popArgs.length() > 0)
            {
                _source.print("(");
                _source.print(popArgs);
                _source.println();
                _source.print(indent2);
                _source.print(")");
            }
            _source.println();
        }

        return;
    } // end of visit(SmcGuard)

    /**
     * Emits Ruby code for this FSM action.
     * @param action emits Ruby code for this action.
     */
    public void visit(SmcAction action)
    {
        String name = action.getName();
        Iterator<String> it;
        String sep;

        // Need to distinguish between FSMContext actions and
        // application class actions. If the action is
        // "emptyStateStack", then pass it to the context.
        // Otherwise, let the application class handle it.
        _source.print(_indent);
        if (name.equals("emptyStateStack") == true)
        {
            _source.print("fsm.");
        }
        else
        {
            _source.print("ctxt.");
        }
        _source.print(name);
        _source.print("(");

        for (it = action.getArguments().iterator(), sep = "";
             it.hasNext() == true;
             sep = ", ")
        {
            _source.print(sep);
            _source.print(it.next());
        }

        _source.println(")");

        return;
    } // end of visit(SmcAction)

    //
    // end of SmcVisitor Abstract Method Impelementation.
    //-----------------------------------------------------------

//---------------------------------------------------------------
// Member data
//
} // end of class SmcRubyGenerator

//
// CHANGE LOG
// $Log$
// Revision 1.2  2009/03/27 09:41:47  cwrapp
// Added F. Perrad changes back in.
//
// Revision 1.1  2009/03/01 18:20:42  cwrapp
// Preliminary v. 6.0.0 commit.
//
// Revision 1.10  2008/07/14 09:31:25  fperrad
// + Added the generation of read-only macro for Vi & Emacs
//
// Revision 1.9  2008/07/08 11:25:43  fperrad
// + automatic delegation (suite)
//
// Revision 1.8  2008/07/07 14:42:17  fperrad
// + automatic delegation (more rubyesque)
//
// Revision 1.7  2008/03/21 14:03:17  fperrad
// refactor : move from the main file Smc.java to each language generator the following data :
//  - the default file name suffix,
//  - the file name format for the generated SMC files
//
// Revision 1.6  2007/02/21 13:56:32  cwrapp
// Moved Java code to release 1.5.0
//
// Revision 1.5  2007/01/15 00:23:52  cwrapp
// Release 4.4.0 initial commit.
//
// Revision 1.4  2007/01/03 14:34:16  fperrad
// + Added -reflect option for Perl, Python and Ruby code generation
//
// Revision 1.3  2006/09/16 15:04:29  cwrapp
// Initial v. 4.3.3 check-in.
//
// Revision 1.2  2005/11/07 19:34:54  cwrapp
// Changes in release 4.3.0:
// New features:
//
// + Added -reflect option for Java, C#, VB.Net and Tcl code
//   generation. When used, allows applications to query a state
//   about its supported transitions. Returns a list of transition
//   names. This feature is useful to GUI developers who want to
//   enable/disable features based on the current state. See
//   Programmer's Manual section 11: On Reflection for more
//   information.
//
// + Updated LICENSE.txt with a missing final paragraph which allows
//   MPL 1.1 covered code to work with the GNU GPL.
//
// + Added a Maven plug-in and an ant task to a new tools directory.
//   Added Eiten Suez's SMC tutorial (in PDF) to a new docs
//   directory.
//
// Fixed the following bugs:
//
// + (GraphViz) DOT file generation did not properly escape
//   double quotes appearing in transition guards. This has been
//   corrected.
//
// + A note: the SMC FAQ incorrectly stated that C/C++ generated
//   code is thread safe. This is wrong. C/C++ generated is
//   certainly *not* thread safe. Multi-threaded C/C++ applications
//   are required to synchronize access to the FSM to allow for
//   correct performance.
//
// + (Java) The generated getState() method is now public.
//
// Revision 1.1  2005/06/16 18:11:01  fperrad
// Added C, Perl & Ruby generators.
//
//