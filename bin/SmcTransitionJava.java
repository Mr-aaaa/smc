//
// The contents of this file are subject to the Mozilla Public
// License Version 1.1 (the "License"); you may not use this file
// except in compliance with the License. You may obtain a copy of
// the License at http://www.mozilla.org/MPL/
// 
// Software distributed under the License is distributed on an "AS
// IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
// implied. See the License for the specific language governing
// rights and limitations under the License.
// 
// The Original Code is State Map Compiler (SMC).
// 
// The Initial Developer of the Original Code is Charles W. Rapp.
// Portions created by Charles W. Rapp are
// Copyright (C) 2000 Charles W. Rapp.
// All Rights Reserved.
// 
// Contributor(s): 
//
// RCS ID
// $Id$
//
// CHANGE LOG
// $Log$
// Revision 1.1  2001/01/03 03:14:00  cwrapp
// Initial revision
//
// Revision 1.2  2000/09/01 15:32:23  charlesr
// Changes for v. 1.0, Beta 2:
//
// + Removed order dependency on "%start", "%class" and "%header"
//   appearance. These three tokens may now appear in any order but
//   still must appear before the first map definition.
//
// + Modified SMC parser so that it will continue after finding an
//   error. Also improved the error message quality.
//
// + Made error messages so emacs is able to parse them.
//
// Revision 1.1.1.1  2000/08/02 12:50:57  charlesr
// Initial source import, SMC v. 1.0, Beta 1.
//

import java.io.PrintStream;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.ListIterator;

public final class SmcTransitionJava
    extends SmcTransition
{
    public SmcTransitionJava(String name,
                             LinkedList parameters,
                             int line_number)
    {
        super(name, parameters, line_number);
    }

    public void generateCode(PrintStream header,
                             PrintStream source,
                             String context,
                             String mapName,
                             String stateName,
                             String indent)
        throws ParseException
    {
        int guardCount;
        int guardIndex;
        int nullConditions;
        ListIterator paramIt;
        SmcParameter parameter;
        ListIterator guardIt;
        SmcGuard guard;

        source.print(indent +
                     "protected void " +
                     _name +
                     "(" +
                     context +
                     "Context s");

        // Add user-defined parameters.
        for (paramIt = _parameters.listIterator();
             paramIt.hasNext() == true;
            )
        {
            parameter = (SmcParameter) paramIt.next();

            source.print(", ");
            parameter.generateCode(source);
        }
        source.println(")");

        source.println(indent + "{");

        // Output transtion to debug stream.
        if (Smc.isDebug() == true)
        {
            source.println(indent +
                           "    if (s.getDebugFlag() == true)");
            source.println(indent + "    {");
            source.print(indent +
                         "        System.err.println(\"TRANSITION   : " +
                         mapName +
                         "." +
                         stateName +
                         "." +
                         _name);
            if (_parameters.size() > 0)
            {
                String separator;

                source.print("(");
                for (paramIt = _parameters.listIterator(),
                         separator = "";
                     paramIt.hasNext() == true;
                     separator = ", ")
                {
                    parameter = (SmcParameter) paramIt.next();
                    source.print(separator);
                    parameter.generateCode(source);
                }
                source.print(")");
            }
            source.println("\");");
            source.println(indent + "    }\n");
        }

        // Loop through the guards and print each one.
        for (guardIt = _guards.listIterator(),
                     guardIndex = 0,
                     guardCount = _guards.size(),
                     nullConditions = 0;
             guardIt.hasNext() == true;
             ++guardIndex)
        {
            guard = (SmcGuard) guardIt.next();

            // Count up the guards with no condition.
            if (guard.getCondition() == null)
            {
                ++nullConditions;
            }

            guard.generateCode(source,
                               guardIndex,
                               guardCount,
                               context,
                               mapName,
                               stateName,
                               indent);
        }

        // If all guards have a condition, then create a final
        // "else" clause which passes control to the default
        // transition. Pass all arguments into the default
        // transition.
        if (guardCount > 0 && nullConditions == 0)
        {
            source.println("}\n" +
                           indent +
                           "    else");
            source.println(indent + "    {");
            source.print(indent +
                         "        " +
                         mapName +
                         ".Default." +
                         _name +
                         "(s");

            for (paramIt = _parameters.listIterator();
                 paramIt.hasNext() == true;
                )
            {
                parameter = (SmcParameter) paramIt.next();
                source.print(", " + parameter.getName());
            }
            
            source.println(");");
            source.println(indent + "    }");
        }
        else if (nullConditions > 1)
        {
            // If there are multiple transition with the same
            // name, then only one of them can be unguarded.
            ParseException e =
                    new ParseException("State " +
                                       mapName +
                                       "::" +
                                       stateName +
                                       " has multiple transitions with the same name (\"" +
                                       _name +
                                       "\") and guard.",
                                       0);
            throw(e);
        }

        source.println("\n" + indent + "    return;");
        source.println(indent + "}\n");

        return;
    }
}