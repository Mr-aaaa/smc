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
// The Original Code is State Machine Compiler (SMC).
// 
// The Initial Developer of the Original Code is Charles W. Rapp.
// Portions created by Charles W. Rapp are
// Copyright (C) 2000 Charles W. Rapp.
// All Rights Reserved.
// 
// Contributor(s): 
//
// SmcArgument --
//
//  Stores an action's argument. An action may have 0 or more arguments.
//
// RCS ID
// $Id$
//
// CHANGE LOG
// $Log$
// Revision 1.1  2002/05/07 00:10:20  cwrapp
// Changes in release 1.3.2:
// Add the following feature:
// + 528321: Modified push transition syntax to be:
//
// 	  <transname> <state1>/push(<state2>)  {<actions>}
//
// 	  which means "transition to <state1> and then
// 	  immediately push to <state2>". The current
// 	  syntax:
//
// 	  <transname> push(<state2>)  {<actions>}
//
//           is still valid and <state1> is assumed to be "nil".
//
// No bug fixes.
//

package net.sf.smc;

import java.io.PrintStream;

public abstract class SmcArgument
    implements Comparable
{
// Member methods.

    public SmcArgument(String name, int line_number)
    {
        _name = new String(name);
        _line_number = line_number;
    }

    public String getName()
    {
        return (_name);
    }

    public int getLineNumber()
    {
        return (_line_number);
    }

    public abstract int compareTo(Object obj);

    // This language-specific method must be implemented by
    // the derived classes.
    public abstract void generateCode(PrintStream source);

// Member data.

    protected String _name;
    protected int _line_number;
}