#ifndef _H_TCPCONNECTIONLISTENER
#define _H_TCPCONNECTIONLISTENER

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
//  TcpConnectionListener.h
//
// Description
//  TCP connection connection callback handler.
//
// RCS ID
// $Id$
//
// CHANGE LOG
// $Log$
// Revision 1.1  2001/01/03 03:14:00  cwrapp
// Initial revision
//

// Forward declarations.
class TcpConnection;
class TcpClient;
class TcpServer;

class TcpConnectionListener
{
// Member functions.
public:

    virtual void opened(TcpConnection& connection)=0;
    virtual void openFailed(const char *reason,
                            TcpConnection& connection)=0;
    virtual void halfClosed(TcpConnection& connection)=0;
    virtual void closed(const char *reason,
                        TcpConnection& connection)=0;
    virtual void transmitted(TcpConnection& connection)=0;
    virtual void transmitFailed(const char *reason,
                                TcpConnection& connection)=0;
    virtual void receive(const char *data,
                         int size,
                         TcpConnection& connection)=0;
    virtual void accepted(TcpClient& client, TcpServer& server)=0;

protected:
private:

// Member data.
public:
protected:
private:
};

#endif