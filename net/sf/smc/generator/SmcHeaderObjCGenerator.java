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
// Copyright (C) 2006 - 2008. Charles W. Rapp.
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
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import net.sf.smc.model.SmcAction;
import net.sf.smc.model.SmcElement;
import net.sf.smc.model.SmcFSM;
import net.sf.smc.model.SmcGuard;
import net.sf.smc.model.SmcMap;
import net.sf.smc.model.SmcParameter;
import net.sf.smc.model.SmcState;
import net.sf.smc.model.SmcTransition;
import net.sf.smc.model.SmcVisitor;

/**
 * Visits the abstract syntax tree emitting an Objective C header
 * file.
 * @see SmcElement
 * @see SmcVisitor
 * @see SmcCppGenerator
 *
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */

public final class SmcHeaderObjCGenerator
    extends SmcCodeGenerator
{
//---------------------------------------------------------------
// Member methods
//

    //-----------------------------------------------------------
    // Constructors.
    //

    /**
     * Creates an Objective C header code generator for the given
     * parameters.
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
     * @param accessLevel use this access keyword for the
     * generated classes.
     */
    public SmcHeaderObjCGenerator(final String srcfileBase,
                                  final String srcDirectory,
                                  final String headerDirectory,
                                  final String castType,
                                  final int graphLevel,
                                  final boolean serialFlag,
                                  final boolean debugFlag,
                                  final boolean noExceptionFlag,
                                  final boolean noCatchFlag,
                                  final boolean noStreamsFlag,
                                  final boolean reflectFlag,
                                  final boolean syncFlag,
                                  final boolean genericFlag,
                                  final String accessLevel)
    {
        super (srcfileBase,
               "{0}{1}_sm.{2}",
               "h",
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
               genericFlag,
               accessLevel);
    } // end of SmcHeaderObjCGenerator(...)

    //
    // end of Constructors.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // SmcVisitor Abstract Method Impelementation.
    //

    /**
     * Emits Objective C header code for the finite state
     * machine.
     * @param fsm emit Objective C header code for this finite
     * state machine.
     */
    public void visit(SmcFSM fsm)
    {
        String srcfileCaps;
        String packageName = fsm.getPackage();
        String context = fsm.getContext();
        String mapName;
        List<SmcTransition> transList;
        String separator;
        List<SmcParameter> params;
        Iterator<SmcParameter> pit;
        int packageDepth = 0;
        int index;

        _source.println("/*");
        _source.println(" * ex: set ro:");
        _source.println(" * DO NOT EDIT.");
        _source.println(" * generated by smc (http://smc.sourceforge.net/)");
        _source.print(" * from file : ");
        _source.print(_srcfileBase);
        _source.println(".sm");
        _source.println(" */");
        _source.println();

        // Include required standard .h files.
        _source.println();
        _source.println("#import \"statemap.h\"");

        _source.println();

        // Forward declare all the state classes in all the maps.
        _source.print(_indent);
        _source.println("// Forward declarations.");
        for (SmcMap map: fsm.getMaps())
        {
            mapName = map.getName();

            // class <map name>;
            _source.print(_indent);
            _source.print("@class ");
            _source.print(mapName);
            _source.println(";");

            // Iterate over the map's states.
            for (SmcState state: map.getStates())
            {
                _source.print(_indent);
                _source.print("@class ");
                _source.print(mapName);
                _source.print("_");
                _source.print(state.getClassName());
                _source.println(";");
            }

            // Forward declare the default state as well.
            _source.print(_indent);
            _source.print("@class ");
            _source.print(mapName);
            _source.println("_Default;");
        }

        // Forward declare the state class and its
        // context as well.
        _source.print(_indent);
        _source.print("@class ");
        _source.print(context);
        _source.println("State;");
        _source.print(_indent);
        _source.print("@class ");
        _source.print(context);
        _source.println("Context;");

        // Forward declare the application class.
        _source.print(_indent);
        _source.print("@class ");
        _source.print(context);
        _source.println(";");

        // Do user-specified forward declarations now.
        for (String declaration: fsm.getDeclarations())
        {
            _source.print(_indent);
            _source.print(declaration);

            // Add a semicolon if the user did not use one.
            if (declaration.endsWith(";") == false)
            {
                _source.print(";");
            }

            _source.println();
        }
        _source.println();

        // Declare user's base state class.
        _source.print(_indent);
        _source.print("@interface ");
        _source.print(context);
        _source.println("State : SMCState");
        _source.println("{");
        _source.println("}");

        // Add the default Entry() and Exit() definitions.
        _source.print(_indent);
        _source.print("- (void)Entry:(");
        _source.print(context);
        _source.println("Context*)context;");
        _source.print(_indent);
        _source.print("- (void)Exit:(");
        _source.print(context);
        _source.println("Context*)context;");

        _source.println();

        // Print out the default definitions for all the
        // transitions. First, get the transitions list.
        transList = fsm.getTransitions();

        // Output the global transition declarations.
        for (SmcTransition trans: transList)
        {
            // Don't output the default state here.
            if (trans.getName().equals("Default") == false)
            {
                _source.print(_indent);
                _source.print("- (void)");
                _source.print(trans.getName());
                _source.print(":(");
                _source.print(context);
                _source.print("Context*)context");

                for (SmcParameter param:
                         trans.getParameters())
                {
                    _source.print(" :");
                    param.accept(this);
                }

                _source.println(";");
            }
        }

        // Declare the global Default transition.
        _source.println("");
        _source.print(_indent);
        _source.print("- (void)Default:(");
        _source.print(context);
        _source.println("Context*)context;");

        // The base class has been defined.
        _source.print(_indent);
        _source.println("@end");
        _source.println();

        // Generate the map classes. The maps will, in turn,
        // generate the state classes.
        for (SmcMap map: fsm.getMaps())
        {
            map.accept(this);
        }

        // Generate the FSM context class.
        // class FooContext :
        //     public statemap::FSMContext
        // {
        // public:
        //     FOOContext(FOO& owner)
        //
        _source.print(_indent);
        _source.print("@interface ");
        _source.print(context);
        _source.println("Context : SMCFSMContext");
        _source.print(_indent);
        _source.println("{");

        _source.print(_indent);
        _source.print("    ");
        _source.print(context);
        _source.println(" *_owner;");

        _source.print(_indent);
        _source.println("}");

        _source.print(_indent);
        _source.print("- (id)initWithOwner:(");
        _source.print(context);
        _source.print("*)");
        _source.println("owner;");

        _source.print(_indent);
        _source.print("- (id)initWithOwner:(");
        _source.print(context);
        _source.print("*)");
        _source.println("owner state:(SMCState*)aState;");

        _source.print(_indent);
        _source.print("- (");
        _source.print(context);
        _source.println("*)owner;");

        _source.print(_indent);
        _source.print("- (" );
        _source.print(context);
        _source.println("State*)state;");

        _source.println();

        _source.print(_indent);
        _source.println("- (void)enterStartState;");
        _source.println();

        // Generate a method for every transition in every map
        // *except* the default transition.
        for (SmcTransition trans: transList)
        {
            if (trans.getName().equals("Default") == false)
            {
                SmcParameter param;

                _source.print(_indent);
                _source.print("- (void)");
                _source.print(trans.getName());

                for (pit = (trans.getParameters()).iterator(),
                       separator = ":";
                     pit.hasNext() == true;
                     separator = " :")
                {
                    param = pit.next();

                    _source.print(separator);
                    param.accept(this);
                }
                _source.println(";");
            }
        }

        // End the context class.
        _source.print(_indent);
        _source.println("@end");
        _source.println();

        _source.println();
        _source.println("/*");
        _source.println(" * Local variables:");
        _source.println(" *  buffer-read-only: t");
        _source.println(" * End:");
        _source.println(" */");

        return;
    } // end of visit(SmcFSM)

    /**
     * Generates the map class declaration and then the state
     * classes:
     * <code>
     *   <pre>
     * class <i>map name</i>
     * {
     * public:
     *
     *     static <i>map name</i>_<i>state name</i> <i>state name</i>;
     * };
     *   </pre>
     * </code>
     * @param map emit Objective C header code for this map.
    */
    public void visit(SmcMap map)
    {
        String context = map.getFSM().getContext();
        String mapName = map.getName();
        String stateName;

        _source.print(_indent);
        _source.print("@interface ");
        _source.print(mapName);
        _source.println(" : NSObject");
        _source.print(_indent);
        _source.println("{");
        _source.println("}");

        // Define class methods to access the state instances
        for (SmcState state: map.getStates())
        {
            stateName = state.getClassName();

            _source.print(_indent);
            _source.print("+ (");
            _source.print(mapName);
            _source.print("_");
            _source.print(stateName);
            _source.print("*)");
            _source.print(stateName);
            _source.println(";");
        }

        // The map class is now defined.
        _source.print(_indent);
        _source.println("@end");
        _source.println();

        // Declare the map's default state class.
        //
        // @interface <map name>_Default : <context>State
        // {
        // }
        // (user-defined Default state transitions.)
        // @end

        _source.print(_indent);
        _source.print("@interface ");
        _source.print(mapName);
        _source.print("_Default : ");
        _source.print(context);
        _source.println("State");
        _source.print(_indent);
        _source.println("{");
        _source.print(_indent);
        _source.println("}");

        // Declare the user-defined default transitions first.
        if (map.hasDefaultState() == true)
        {
            SmcState defaultState = map.getDefaultState();

            for (SmcTransition transition:
                     defaultState.getTransitions())
            {
                transition.accept(this);
            }
        }

        // The map's default state class is now defined.
        _source.print(_indent);
        _source.println("@end");
        _source.println();

        // Now output the state class declarations.
        for (SmcState state: map.getStates())
        {
            state.accept(this);
        }

        return;
    } // end of visit(SmcMap)

    /**
     * Generates the state class declaration.
     * <code>
     *   <pre>
     * {@literal @interface} <i>map name</i>_<i>state name</i> : <i>map name</i>_Default
     * {
     * }
     * - (id)initWithName(NSString*)name stateId:(int)stateId;
     * (declare the transition methods.)
     * - (void)<i>transition name</i>:(<i>context</i>*)context <i>args</i>;
     * {@literal @end}
     *   </pre>
     * </code>
     * @param state emits Objective C header code for this state.
     */
    public void visit(SmcState state)
    {
        SmcMap map = state.getMap();
        String context = map.getFSM().getContext();
        String mapName = map.getName();
        String stateName = state.getClassName();
        List<SmcAction> actions;

        _source.print(_indent);
        _source.print("@interface ");
        _source.print(mapName);
        _source.print('_');
        _source.print(stateName);
        _source.print(" : ");
        _source.print(mapName);
        _source.println("_Default");
        _source.print(_indent);
        _source.println("{");
        _source.println("}");

        // Add the Entry() and Exit() methods if this state
        // defines them.
        actions = state.getEntryActions();
        if (actions != null && actions.size() > 0)
        {
            _source.print(_indent);
            _source.print(" -(void)Entry:(");
            _source.print(context);
            _source.println("Context*)context;");
        }

        actions = state.getExitActions();
        if (actions != null && actions.size() > 0)
        {
            _source.print(_indent);
            _source.print(" -(void)Entry:(");
            _source.print(context);
            _source.println("Context*)context;");        }

        // Now generate the transition methods.
        for (SmcTransition transition: state.getTransitions())
        {
            transition.accept(this);
        }

        // End of the state class declaration.
        _source.print(_indent);
        _source.println("@end");
        _source.println();

        return;
    } // end of visit(SmcState)

    /**
     * Generates the transition method declaration:
     * <code>
     *   <pre>
     * - (void)<i>transition name</i>:(<i>context</i>Context*)context <i>args</i>;
     *   </pre>
     * </code>
     * @param transition emits Groovy code for this state transition.
     */
    public void visit(SmcTransition transition)
    {
        SmcState state = transition.getState();
        String stateName = state.getClassName();

        _source.print(_indent);
        _source.print("- (void)");
        _source.print(transition.getName());
        _source.print(":(");
        _source.print(
            state.getMap().getFSM().getContext());
        _source.print("Context*)context");

        // Add user-defined parameters.
        for (SmcParameter parameter: transition.getParameters())
        {
            _source.print(" :");
            parameter.accept(this);
        }

        // End of transition method declaration.
        _source.println(";");

        return;
    } // end of visit(SmcTransition)

    /**
     * Emits Objective C header code for this transition
     * parameter.
     * @param parameter emits Objective C header code for this
     * transition parameter.
     */
    public void visit(SmcParameter parameter)
    {
        _source.print("(");
        _source.print(parameter.getType());
        _source.print(")");
        _source.print(parameter.getName());

        return;
    } // end of visit(SmcParameter)

    //
    // end of SmcVisitor Abstract Method Impelementation.
    //-----------------------------------------------------------

//---------------------------------------------------------------
// Member data
//
} // end of class SmcHeaderObjCGenerator

//
// CHANGE LOG
// $Log$
// Revision 1.4  2009/09/05 15:39:20  cwrapp
// Checking in fixes for 1944542, 1983929, 2731415, 2803547 and feature 2797126.
//
// Revision 1.3  2009/04/10 14:02:48  cwrapp
// Set initial state via initializer.
//
// Revision 1.2  2009/03/27 09:41:47  cwrapp
// Added F. Perrad changes back in.
//
// Revision 1.1  2009/03/01 18:20:42  cwrapp
// Preliminary v. 6.0.0 commit.
//
// Revision 1.3  2008/03/21 14:03:16  fperrad
// refactor : move from the main file Smc.java to each language generator the following data :
//  - the default file name suffix,
//  - the file name format for the generated SMC files
//
// Revision 1.2  2007/02/21 13:55:27  cwrapp
// Moved Java code to release 1.5.0
//
// Revision 1.1  2007/01/15 00:23:51  cwrapp
// Release 4.4.0 initial commit.
//
