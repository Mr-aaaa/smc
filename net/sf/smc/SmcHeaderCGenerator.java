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
// Copyright (C) 2005. Charles W. Rapp.
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

package net.sf.smc;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

/**
 * Visits the abstract syntax tree emitting a C++ header file.
 * @see SmcElement
 * @see SmcVisitor
 * @see SmcCGenerator
 *
 * @author Francois Perrad
 */

public final class SmcHeaderCGenerator
    extends SmcCodeGenerator
{
//---------------------------------------------------------------
// Member methods
//

    public SmcHeaderCGenerator(PrintStream source,
                              String srcfileBase)
    {
        super (source, srcfileBase);
    } // end of SmcHeaderCGenerator(PrintStream, String)

    public void visit(SmcFSM fsm)
    {
        String srcfileCaps;
        String packageName = fsm.getPackage();
        String context = fsm.getContext();
        List<SmcTransition> transList;
        List<SmcParameter> params;

        // If a package has been specified, 
        if (packageName != null && packageName.length() > 0)
        {
              context = packageName + "_" + context;
        }

        // The first two lines in the header file should be:
        //
        //    #ifndef _H_<source file name>_SM
        //    #define _H_<source file name>_SM
        //
        // where the source file name is all in caps.
        // The last line is:
        //
        //    #endif
        //

        // Make the file name upper case and replace
        // slashes with underscores.
        srcfileCaps = _srcfileBase.replace('\\', '_');
        srcfileCaps = srcfileCaps.replace('/', '_');
        srcfileCaps = srcfileCaps.toUpperCase();
        _source.print("#ifndef _H_");
        _source.print(srcfileCaps);
        _source.println("_SM");
        _source.print("#define _H_");
        _source.print(srcfileCaps);
        _source.println("_SM");

        // Include required standard .h files.
        _source.println();
        _source.println("#include <statemap.h>");

        _source.println();

        // Do user-specified forward declarations now.
        for (String declaration: fsm.getDeclarations())
        {
            _source.print(declaration);

            // Add a semicolon if the user did not use one.
            if (declaration.endsWith(";") == false)
            {
                _source.print(";");
            }

            _source.println();
        }

        // Forward declare the application class.
        _source.println();
        _source.print("struct ");
        _source.print(context);
        _source.println(";");
        _source.print("struct ");
        _source.print(context);
        _source.println("Context;");

        // Declare user's base state class.
        _source.println();
        _source.print("struct ");
        _source.print(context);
        _source.println("State");
        _source.println("{");

        // Add the default Entry() and Exit() definitions.
        _source.print("    void(*Entry)(struct ");
        _source.print(context);
        _source.println("Context*);");
        _source.print("    void(*Exit)(struct ");
        _source.print(context);
        _source.println("Context*);");
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
                _source.print("    void(*");
                _source.print(trans.getName());
                _source.print(")(struct ");
                _source.print(context);
                _source.print("Context*");

                params = trans.getParameters();
                for (SmcParameter param: params)
                {
                    _source.print(", ");
                    _source.print(param.getType());
                }

                _source.println(");");
            }
        }
        _source.println();
        _source.print("    void(*Default)(struct ");
        _source.print(context);
        _source.println("Context*);");

        _source.println();
        _source.println("    STATE_MEMBERS");

        // The base class has been defined.
        _source.println("};");
        _source.println();

        // Generate the map classes. The maps will, in turn,
        // generate the state classes.
        for (SmcMap map: fsm.getMaps())
        {
            map.accept(this);
        }

        // Generate the FSM context class.
        _source.println();
        _source.print("struct ");
        _source.print(context);
        _source.println("Context");
        _source.println("{");
        _source.print("    FSM_MEMBERS(");
        _source.print(context);
        _source.println(")");
        _source.print("    struct ");
        _source.print(context);
        _source.println(" *_owner;");

        // Put the closing brace on the context class.
        _source.println("};");
        _source.println();

        // Constructor
        _source.print("extern void ");
        _source.print(context);
        _source.print("Context_Init");
        _source.print("(struct ");
        _source.print(context);
        _source.print("Context*, struct ");
        _source.print(context);
        _source.println("*);");

        // Generate a method for every transition in every map
        // *except* the default transition.
        for (SmcTransition trans: transList)
        {
            if (trans.getName().equals("Default") == false)
            {
                _source.print("extern void ");
                _source.print(context);
                _source.print("Context_");
                _source.print(trans.getName());
                _source.print("(struct ");
                _source.print(context);
                _source.print("Context*");

                params = trans.getParameters();
                for (SmcParameter param: params)
                {
                    _source.print(", ");
                    _source.print(param.getType());
                }
                _source.println(");");
            }
        }

        _source.println();
        _source.println("#endif");

        return;
    } // end of visit(SmcFSM)

    // Generate the map class declaration
    public void visit(SmcMap map)
    {
        String packageName = map.getFSM().getPackage();
        String context = map.getFSM().getContext();
        String mapName = map.getName();

        // If a package has been specified, 
        if (packageName != null && packageName.length() > 0)
        {
              context = packageName + "_" + context;
        }

        for (SmcState state: map.getStates())
        {
            _source.print("extern const struct ");
            _source.print(context);
            _source.print("State ");
            if (packageName != null && packageName.length() > 0)
            {
                _source.print(packageName);
                _source.print("_");
            }
            _source.print(mapName);
            _source.print("_");
            _source.print(state.getClassName());
            _source.println(";");    
        }

        return;
    } // end of visit(SmcMap)

//---------------------------------------------------------------
// Member data
//
} // end of class SmcHeaderCGenerator

//
// CHANGE LOG
// $Log$
// Revision 1.6  2007/02/21 13:55:02  cwrapp
// Moved Java code to release 1.5.0
//
// Revision 1.5  2007/01/15 00:23:51  cwrapp
// Release 4.4.0 initial commit.
//
// Revision 1.4  2006/09/16 15:04:29  cwrapp
// Initial v. 4.3.3 check-in.
//
// Revision 1.3  2005/11/07 19:34:54  cwrapp
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
// Revision 1.2  2005/07/07 12:07:28  fperrad
// When the .sm is in a subdirectory the forward- or backslashes in the file name are kept in the "#ifndef" in the generated header file. This is syntactically wrong. SMC nowreplaces the slashes with underscores.
//
// Revision 1.1  2005/06/16 18:11:01  fperrad
// Added C, Perl & Ruby generators.
//
//
