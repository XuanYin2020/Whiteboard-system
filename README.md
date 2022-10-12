# Whiteboard-system
Implement an event-based collaborative whiteboard system that enables users to create and share whiteboards and share information on the whiteboard in real time.

o	A multi-threaded distributed system is built on the basis of client/server architecture to achieve multi-node communication between client and server.

o	Support for multiple users to create and modify any number of whiteboards and share them with other peers and real-time updates of shared information, and server priority processing.

## Client/Server Framework Overview
• The Client and Server each have a main method which is simply used to parse
command line options and set default values. They instantiate ClientManager and
ServerManager resp. to do the work required for the Client/Server interactions.

• The ClientManager and ServerManager extend Manager . Both client and server are
almost identical in what they do. The biggest difference is that the ServerManager
creates an instance of IOThread that listens for socket connections, whereas the
ClientManager simply creates a socket to connect to the server.

• Each socket connection, for either the server or the client, is wrapped inside an
instance of Endpoint . The Endpoint class extends Thread and the run method does
a blocking read on the socket. The Endpoint therefore processes messages as they
arrive, until the thread is interrupted or errors occur that force it to stop. The
Endpoint informs its manager of any significant events, such as socket
disconnection.

• Every message in the system is a class that extends Message . The Message class
knows how to marshal itself, using JSON format, to be transmitted over the socket.
Also the Message class knows how to unmarshal a JSON formatted string (it provides
a factory method for all message types).

• Each Endpoint handles a set of protocols. Each protocol extends Protocol and
implements a protocol interface. The only interface available so far is
IRequestReplyProtocol . The Endpoint will route messages to the correct protocol
and the protocol will determine what other messages, if any, need to be sent. The
protocol informs its manager of any significant protocol events, such as message
timeout.


## WhiteboardServer

The purpose of the whiteboard server is to allow whiteboard peers to advertise
themselves and their whiteboards that they are sharing, and to share and unshare
those boards as desired. Therefore it uses the following events:
◦ SHARE_BOARD : emitted by a peer to tell the server it wants to share a board

◦ UNSHARE_BOARD : emitted by a peer to tell the server it wants to unshare a board

◦ SHARING_BOARD : emitted by the server to tell a peer that a board is being shared

◦ UNSHARING_BOARD : emitted by the server to tell a peer that a board is being
unshared

◦ ERROR : emitted by the server to tell a peer that an error has occurred

• The basic operation of the whiteboard server is: for every peer that connects tell
the peer all boards that are currently being shared, and allow the peer to share and
unshare boards, telling all connected peers whenever a peer shares or unshares a
board.


## WhiteboardPeer
The purpose of the peer is to allow the user to create and modify any number of
whiteboards, and to allow the user to selectively share them with other peers.


### Peer operation
• Users are free to create any number of local whiteboards using the GUI (code is
supplied and does this).

• For each locally created board, user can select Shared and the peer will tell the
server that the board is shared (share toggle is provided in GUI).

• For each locally created board, the user can unselect Shared and the peer will tell
the server that the board is unshared.

• The peer will list for selection (list provided in GUI) any boards that the server says
are shared (including those that are not local).

• The peer will remove from selection any boards that the server says that are
unshared, only if those boards are not local boards.

• The user may attempt to draw a path on any board listed for selection.
• The user may attempt to clear any board listed for selection.

• The user may attempt to undo the last path for any board listed for selection.

• The above three operations are called updates. An update is only successful if it
applies to a board that has the same version as the board that the user was
updating.

• For any board that is shared and not local, called a remote board, the peer will
obtain the board details from the remote peer that is sharing it and will listen for
updates.

• Any update by a user that should apply to a remote board will be sent to the remote
peer.

• Any update received from a peer for a local board may or may not be successful, as
above.

• All successful updates for a local board are sent to any peers that are listening to
that board.

• When a user deletes a board, the board is removed from selection and if that board
is a local board then all peers listening to it are told the board is deleted.

• When a peer is told that a board is deleted, then the board is removed from the
selection.

• When a peer shuts down, i.e. the user closes the GUI, all local boards are unshared
and deleted before shutdown.

### Whiteboard peer events
The whiteboard peer uses the followingevents:

·BOARD LISTEN:emitted by the peer to another peer to tell it that it wants to receive
updates about the board

·BOARD UNLISTEN:emitted by the peer to another peer to tell it that it no longer
wants to receive updates aboutthe board

·GET BOARD DATA:emitted by the peer to another peer to tell it that it wants to
receive the entire board data for a given board

·BOARD DATA:emitted by the peer to another peer to tell it the entire board data for a
given board

·BOARD PATH UPDATE:emitted by the peer to another peer to request that a path be added to a given board

·BOARD PATH ACCEPTED:emitted by the peer to another peer to indicate that a path
has been added to a given boaro

·BOARD UNDO UPDATE: emitted by the peer to another peer to request that the last path be removed from a given board

·BOARD UNDO ACCEPTED: emitted by the peer to another peer to indicate that a the
last path has been removed for a given board

·BOARD CLEAR_UPDATE:emitted by the peer to another peer to request that a board be cleared
