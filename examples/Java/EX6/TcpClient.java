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
//  TcpClient.java
//
// Description
//  A TCP client connection.
//
// RCS ID
// $Id$
//
// CHANGE LOG
// $Log$
// Revision 1.1  2001/01/03 03:14:00  cwrapp
// Initial revision
//
// Revision 1.2  2000/10/16 21:05:36  charlesr
// Added MPL comments.
//

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public final class TcpClient
    extends TcpConnection
{
// Member methods

    // Create an unopened client.
    public TcpClient(TcpConnectionListener listener)
    {
        super(listener);
        return;
    }

    // The address and port to which I am connected.
    public InetAddress getAddress()
    {
        return(_address);
    }

    public int getPort()
    {
        return(_port);
    }

    public void open(int port)
    {
        try
        {
            activeOpen(InetAddress.getLocalHost(), port);
        }
        catch (UnknownHostException netex)
        {}

        return;
    }

    public void open(InetAddress address, int port)
    {
        activeOpen(address, port);
        return;
    }

    // Create an "accepted" client connection. This constructor
    // may only be called by TcpConnection.
    /* package */ TcpClient(InetAddress address,
                            int port,
                            DatagramSocket socket,
                            int sequence_number,
                            TcpServer server,
                            TcpConnectionListener listener)
    {
        super(address,
              port,
              socket,
              sequence_number,
              server,
              listener);
        return;
    }

// Member data
}