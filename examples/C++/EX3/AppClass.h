#ifndef _H_APPCLASS
#define _H_APPCLASS

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
// Name
//	AppClass
//
// Description
//	When a state map executes an action, it is really calling a member function
//	in the context class.
//
// RCS ID
// $Id$
//
// CHANGE LOG
// $Log$
// Revision 1.1  2001/01/03 03:14:00  cwrapp
// Initial revision
//
// Revision 1.1.1.1  2000/08/02 12:50:59  charlesr
// Initial source import, SMC v. 1.0, Beta 1.
//

#include "AppClass_sm.h"

class AppClass
{
private:
    AppClassContext _state_map;

	bool isAcceptable;
		// If a string is acceptable, then this variable is set to true;
		// false, otherwise.

public:
	AppClass();
		// Default constructor.

	~AppClass() {};
		// Destructor.

	bool CheckString(const char*);
		// Checks if the string is acceptable.

	inline void Acceptable()
	{ isAcceptable = true; };

	inline void Unacceptable()
	{ isAcceptable = false; };
		// State map actions.
};

#endif