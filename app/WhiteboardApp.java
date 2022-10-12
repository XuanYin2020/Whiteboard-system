package pb.app;

import org.apache.commons.codec.binary.Base64;
import pb.WhiteboardServer;
import pb.managers.ClientManager;
import pb.managers.IOThread;
import pb.managers.PeerManager;
import pb.managers.ServerManager;
import pb.managers.endpoint.Endpoint;
import pb.utils.Utils;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;


/**
 * Initial code obtained from:
 * https://www.ssaurel.com/blog/learn-how-to-make-a-swing-painting-and-drawing-application/
 */
public class WhiteboardApp {
	private static Logger log = Logger.getLogger(WhiteboardApp.class.getName());

	/**
	 * Emitted to another peer to subscribe to updates for the given board. Argument
	 * must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String listenBoard = "BOARD_LISTEN";

	/**
	 * Emitted to another peer to unsubscribe to updates for the given board.
	 * Argument must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String unlistenBoard = "BOARD_UNLISTEN";

	/**
	 * Emitted to another peer to get the entire board data for a given board.
	 * Argument must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String getBoardData = "GET_BOARD_DATA";

	/**
	 * Emitted to another peer to give the entire board data for a given board.
	 * Argument must have format "host:port:boardid%version%PATHS".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardData = "BOARD_DATA";

	/**
	 * Emitted to another peer to add a path to a board managed by that peer.
	 * Argument must have format "host:port:boardid%version%PATH". The numeric value
	 * of version must be equal to the version of the board without the PATH added,
	 * i.e. the current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardPathUpdate = "BOARD_PATH_UPDATE";

	/**
	 * Emitted to another peer to indicate a new path has been accepted. Argument
	 * must have format "host:port:boardid%version%PATH". The numeric value of
	 * version must be equal to the version of the board without the PATH added,
	 * i.e. the current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardPathAccepted = "BOARD_PATH_ACCEPTED";

	/**
	 * Emitted to another peer to remove the last path on a board managed by that
	 * peer. Argument must have format "host:port:boardid%version%". The numeric
	 * value of version must be equal to the version of the board without the undo
	 * applied, i.e. the current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardUndoUpdate = "BOARD_UNDO_UPDATE";

	/**
	 * Emitted to another peer to indicate an undo has been accepted. Argument must
	 * have format "host:port:boardid%version%". The numeric value of version must
	 * be equal to the version of the board without the undo applied, i.e. the
	 * current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardUndoAccepted = "BOARD_UNDO_ACCEPTED";

	/**
	 * Emitted to another peer to clear a board managed by that peer. Argument must
	 * have format "host:port:boardid%version%". The numeric value of version must
	 * be equal to the version of the board without the clear applied, i.e. the
	 * current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardClearUpdate = "BOARD_CLEAR_UPDATE";

	/**
	 * Emitted to another peer to indicate an clear has been accepted. Argument must
	 * have format "host:port:boardid%version%". The numeric value of version must
	 * be equal to the version of the board without the clear applied, i.e. the
	 * current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardClearAccepted = "BOARD_CLEAR_ACCEPTED";

	/**
	 * Emitted to another peer to indicate a board no longer exists and should be
	 * deleted. Argument must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardDeleted = "BOARD_DELETED";

	/**
	 * Emitted to another peer to indicate an error has occurred.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardError = "BOARD_ERROR";

	/**
	 * White board map from board name to board object
	 */
	Map<String,Whiteboard> whiteboards;

	/**
	 * The currently selected white board
	 */
	Whiteboard selectedBoard = null;

	/**
	 * The peer:port string of the peer. This is synonomous with IP:port, host:port,
	 * etc. where it may appear in comments.
	 */
	String peerport="standalone"; // a default value for the non-distributed version

	/*
	 * GUI objects, you probably don't need to modify these things... you don't
	 * need to modify these things... don't modify these things [LOTR reference?].
	 */

	JButton clearBtn, blackBtn, redBtn, createBoardBtn, deleteBoardBtn, undoBtn;
	JCheckBox sharedCheckbox ;
	DrawArea drawArea;
	JComboBox<String> boardComboBox;
	boolean modifyingComboBox=false;
	boolean modifyingCheckBox=false;
	public static final String peerStarted = "PEER_STARTED";
	public static final String peerStopped = "PEER_STOPPED";
	public static final String peerError = "PEER_ERROR";
	public static final String shareBoard = "SHARE_BOARD";

	PeerManager peerManager;
	ClientManager clientManager = null;
	ClientManager clientManager1 = null;
	Endpoint endpoint;
	Endpoint endpoint1 = null;
	Endpoint endpoint2 = null;
	ArrayList<Endpoint> endpoint_2array = new ArrayList<Endpoint>();


	/**
	 * Initialize the white board app.
	 */
	public WhiteboardApp(int peerPort,String whiteboardServerHost,
						 int whiteboardServerPort) {
		whiteboards=new HashMap<>();


		PeerManager peerManager = new PeerManager(peerPort);//server





		try {
			clientManager = peerManager.connect(whiteboardServerPort,whiteboardServerHost);

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}


		clientManager.on(peerStarted,(eventArgs)->{
			endpoint = (Endpoint) eventArgs[0];

			this.peerport = Utils.serverHost+":"+ peerPort;   //?
			endpoint.on(WhiteboardServer.sharingBoard,(eventArgs2)->{
				String update = (String) eventArgs2[0];
				onShareBoard(update);
				log.info("the update is : "+update );
				if (!whiteboards.containsKey(update)){
					Whiteboard newWhiteboard = new Whiteboard(update,true);
					log.info("it is not contained");
					addBoard(newWhiteboard,true);
				}

				try {
					if(whiteboards.get(update).isRemote()){//remote
						sharingPeer(peerManager,update);
					}
					else{
						returnPeer(peerManager,update,peerPort);
					}

				} catch (InterruptedException e) {
					System.out.println("the exception: "+update);
				}
				//onBoardListen(parts[0],Integer.parseInt(parts[1]),update);
				log.info("there is the end of the peer started");

			}).on(WhiteboardServer.unsharingBoard,(eventArgs2)->{
				log.info("received unsharing board");
				String update = (String) eventArgs2[0];
				if(whiteboards.get(update).isRemote()){
					log.info("it is a remote board");
					unShareBoard(update);
					deleteBoard(update);
				}


			});


		}).on(peerStopped,(eventArgs)->{
			log.info("peer stoped");
		}).on(peerError,(eventArgs)->{
			log.info("peer Error");
		});
		peerManager.start();
		clientManager.start();
		Utils.getInstance().setTimeout(()->{
			show(peerport);
		}, 500);


//		try {
//			clientManager.join();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
	}

	private void sharingPeer(PeerManager peerManager,String response) throws InterruptedException {
		// Create a independent client manager (thread) for each download
		// response has the format: PeerIP:PeerPort:filename

		String[] parts=response.split(":",3);//host,port,boardid
		ClientManager clientManager;
		try {
			//port,host
			clientManager = peerManager.connect(Integer.valueOf(parts[1]),parts[0]);
		} catch (NumberFormatException e) {
			System.out.println("Response from index server is bad, port is not a number: "+parts[1]);
			return;
		} catch (UnknownHostException e) {
			System.out.println("Could not find the peer IP address: "+parts[0]);
			return;
		}

		clientManager.on(PeerManager.peerStarted, (args)->{
			endpoint1 = (Endpoint)args[0];
			log.info("the sharing peer connection has been established."+args[0]);

			log.info("sending get board date,and is remote");
			endpoint1.emit(getBoardData, response);


			endpoint1.on(boardData,(eventArgs2)-> {
				log.info("receive data" + eventArgs2[0]);
				String name = getBoardName((String)eventArgs2[0]);
				String path = getBoardData((String)eventArgs2[0]);
				whiteboards.get(name).whiteboardFromString(name,path);

				//selected board
				try{
					String selectedBoardName=(String) boardComboBox.getSelectedItem();
					if(whiteboards.get(selectedBoardName)==null) {
						log.severe("selected a board that does not exist: "+selectedBoardName);
						return;
					}
					selectedBoard = whiteboards.get(name);
					drawSelectedWhiteboard();
				}catch (NullPointerException e){

				}
//				String selectedBoardName=(String) boardComboBox.getSelectedItem();
//				if(whiteboards.get(selectedBoardName)==null) {
//					log.severe("selected a board that does not exist: "+selectedBoardName);
//					return;
//				}
//				selectedBoard = whiteboards.get(name);
//				drawSelectedWhiteboard();


			}).on(boardPathUpdate,(eventArgs2)->{
				log.info("draw on sharing Board Data " + eventArgs2[0]);
				String data = (String) eventArgs2[0];
				WhiteboardPath path = new WhiteboardPath(getBoardPaths(data));
				long version = getBoardVersion(data) ;
				log.info("the version is :"+version);

				String selectedBoardName=(String) boardComboBox.getSelectedItem();
				if(whiteboards.get(selectedBoardName)==null) {
					log.severe("selected a board that does not exist: "+selectedBoardName);
					return;
				}
				selectedBoard = whiteboards.get(selectedBoardName);

				System.out.println(selectedBoard.addPath(path,version));

				drawSelectedWhiteboard();

			}).on(boardUndoUpdate,(eventArgs2)->{
				log.info("onBoardDataUndo " + eventArgs2[0]);
				String data = (String) eventArgs2[0];
				String name = getBoardName(data);
				long version = getBoardVersion(data) - 1;
				String path = getBoardPaths(data);
				String versionandPath = version+"%"+path;
				whiteboards.get(name).whiteboardFromString(name,versionandPath);

				String selectedBoardName=(String) boardComboBox.getSelectedItem();
				if(whiteboards.get(selectedBoardName)==null) {
					log.severe("selected a board that does not exist: "+selectedBoardName);
					return;
				}
				selectedBoard = whiteboards.get(selectedBoardName);
				drawSelectedWhiteboard();

			}).on(boardClearUpdate,(eventArgs2)->{
				log.info("onBoardDataClear " + eventArgs2[0]);
				String data = (String) eventArgs2[0];
				String name = getBoardName(data);
				long version = getBoardVersion(data) - 1;
				String path = getBoardPaths(data);
				String versionandPath = version+"%"+path;
				whiteboards.get(name).whiteboardFromString(name,versionandPath);

				String selectedBoardName=(String) boardComboBox.getSelectedItem();
				if(whiteboards.get(selectedBoardName)==null) {
					log.severe("selected a board that does not exist: "+selectedBoardName);
					return;
				}
				selectedBoard = whiteboards.get(selectedBoardName);
				drawSelectedWhiteboard();
			}).on(boardDeleted,(eventArgs2)->{
				String hostport = (String) eventArgs2[0];
				//remove
				log.info("received the delete board");
				deleteBoard(hostport);

			});


			log.info("it is the end of peer connected");
		}).on(PeerManager.peerStopped,(args)->{
			Endpoint endpoint = (Endpoint)args[0];
			System.out.println("Disconnected from peer: "+endpoint.getOtherEndpointId());
		}).on(PeerManager.peerError,(args)->{
			Endpoint endpoint = (Endpoint)args[0];
			System.out.println("There was an error communicating with the peer: "
					+endpoint.getOtherEndpointId());
		});

		clientManager.start();

	}


	private void returnPeer(PeerManager peerManager,String response,int peerPort) throws InterruptedException {
		// Create a independent client manager (thread) for each download
		// response has the format: PeerIP:PeerPort:filename

		String[] parts=response.split(":",3);//host,port,boardid

		PeerManager peerManager1 = new PeerManager(peerPort);
		peerManager.on(PeerManager.peerStarted, (args)->{
			Endpoint endpoint = (Endpoint)args[0];
			endpoint2 = (Endpoint)args[0];

			endpoint_2array.add(endpoint2);
			log.info("the returned peer connection has been established."+args[0]);

			endpoint2.on(getBoardData,(eventArgs2)->{
				System.out.println("no quanju " + endpoint);
				log.info("received get board data");
				Whiteboard newwhitboard = whiteboards.get(eventArgs2[0]);
				String data = newwhitboard.toString();
				for(int i=0;i<endpoint_2array.size();i++){
					endpoint_2array.get(i).emit(boardData,data);
				}

				log.info("sending data:"+data);

			}).on(boardPathUpdate,(eventArgs2)->{
				log.info("draw on Board Data " + eventArgs2[0]);
				String data = (String) eventArgs2[0];
				WhiteboardPath path = new WhiteboardPath(getBoardPaths(data));
				long version = getBoardVersion(data);
				log.info("the received version is :"+version);

				//geng xin selectedboard
				String selectedBoardName=(String) boardComboBox.getSelectedItem();
				if(whiteboards.get(selectedBoardName)==null) {
					log.severe("selected a board that does not exist: "+selectedBoardName);
					return;
				}
				selectedBoard = whiteboards.get(selectedBoardName);
				//path add
				boolean neworold = selectedBoard.addPath(path,version);
				System.out.println(neworold);
				log.info("the current version is :"+selectedBoard.getVersion());
				//draw
				if(neworold){
					drawSelectedWhiteboard();
					//sending
					log.info("sending the update path to all the peer");
					//pathCreatedLocally(path,endpoint2);
					if((!selectedBoard.isRemote())&&selectedBoard.isShared()){

						for(int i=0;i<endpoint_2array.size();i++){
							if (endpoint_2array.get(i)!= null && endpoint_2array.get(i)!=endpoint2){
								String send_data = selectedBoard.getName()+"%"+version+"%"+path.toString();
								//System.out.println(endpoint_2array.get(i));
								endpoint_2array.get(i).emit(boardPathUpdate,send_data);
							}
						}
					}
				}





				//System.out.println(whiteboards.get(getBoardName(data)).addPath(path,version));
			}).on(boardUndoUpdate,(eventArgs2)->{
				log.info("onBoardDataUndo " + eventArgs2[0]);
				String data = (String) eventArgs2[0];
				String name = getBoardName(data);
				long version = getBoardVersion(data) - 1;
				String path = getBoardPaths(data);
				String versionandPath = version+"%"+path;
				whiteboards.get(name).whiteboardFromString(name,versionandPath);

				String selectedBoardName=(String) boardComboBox.getSelectedItem();
				if(whiteboards.get(selectedBoardName)==null) {
					log.severe("selected a board that does not exist: "+selectedBoardName);
					return;
				}
				selectedBoard = whiteboards.get(selectedBoardName);
				drawSelectedWhiteboard();

			}).on(boardClearUpdate,(eventArgs2)->{
				log.info("on Board Data Clear " + eventArgs2[0]);
				String data = (String) eventArgs2[0];
				String name = getBoardName(data);
				long version = getBoardVersion(data) - 1;
				String path = getBoardPaths(data);
				String versionandPath = version+"%"+path;
				whiteboards.get(name).whiteboardFromString(name,versionandPath);

				String selectedBoardName=(String) boardComboBox.getSelectedItem();
				if(whiteboards.get(selectedBoardName)==null) {
					log.severe("selected a board that does not exist: "+selectedBoardName);
					return;
				}
				selectedBoard = whiteboards.get(selectedBoardName);
				drawSelectedWhiteboard();
			});

			log.info("it is the end of peer connected");
		}).on(PeerManager.peerStopped,(args)->{
			Endpoint endpoint = (Endpoint)args[0];
			System.out.println("Disconnected from peer: "+endpoint.getOtherEndpointId());
		}).on(PeerManager.peerError,(args)->{
			Endpoint endpoint = (Endpoint)args[0];
			System.out.println("There was an error communicating with the peer: "
					+endpoint.getOtherEndpointId());
		}).on(PeerManager.peerServerManager, (args)-> {
			ServerManager serverManager = (ServerManager) args[0];
			serverManager.on(IOThread.ioThread, (args2) -> {
				String peerport = (String) args2[0];
			});
		});

		//peerManager.start();

	}
	/******
	 *
	 * Utility methods to extract fields from argument strings.
	 *
	 ******/

	/**
	 *
	 * @param data = peer:port:boardid%version%PATHS
	 * @return peer:port:boardid
	 */
	public static String getBoardName(String data) {
		String[] parts=data.split("%",2);
		return parts[0];
	}

	/**
	 *
	 * @param data = peer:port:boardid%version%PATHS
	 * @return boardid%version%PATHS
	 */
	public static String getBoardIdAndData(String data) {
		String[] parts=data.split(":");
		return parts[2];
	}

	/**
	 *
	 * @param data = peer:port:boardid%version%PATHS
	 * @return version%PATHS
	 */
	public static String getBoardData(String data) {
		String[] parts=data.split("%",2);
		return parts[1];
	}

	/**
	 *
	 * @param data = peer:port:boardid%version%PATHS
	 * @return version
	 */
	public static long getBoardVersion(String data) {
		String[] parts=data.split("%",3);
		return Long.parseLong(parts[1]);
	}

	/**
	 *
	 * @param data = peer:port:boardid%version%PATHS
	 * @return PATHS
	 */
	public static String getBoardPaths(String data) {
		String[] parts=data.split("%",3);
		return parts[2];
	}

	/**
	 *
	 * @param data = peer:port:boardid%version%PATHS
	 * @return peer
	 */
	public static String getIP(String data) {
		String[] parts=data.split(":");
		return parts[0];
	}

	/**
	 *
	 * @param data = peer:port:boardid%version%PATHS
	 * @return port
	 */
	public static int getPort(String data) {
		String[] parts=data.split(":");
		return Integer.parseInt(parts[1]);
	}

	/******
	 *
	 * Methods called from events.
	 *
	 ******/

	// From whiteboard server

	public void onShareBoard(String update){
		log.info("onSharingBoard: " + update);
	}

	public void unShareBoard(String update){
		log.info("unSharingBoard: " + update);

	}


	// From whiteboard peer




	/******
	 *
	 * Methods to manipulate data locally. Distributed systems related code has been
	 * cut from these methods.
	 *
	 ******/

	/**
	 * Wait for the peer manager to finish all threads.
	 */
	public void waitToFinish() {
		try {
			clientManager.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Add a board to the list that the user can select from. If select is
	 * true then also select this board.
	 * @param whiteboard
	 * @param select
	 */
	public void addBoard(Whiteboard whiteboard,boolean select) {
		synchronized(whiteboards) {
			whiteboards.put(whiteboard.getName(), whiteboard);
		}
		updateComboBox(select?whiteboard.getName():null);
	}

	/**
	 * Delete a board from the list.
	 * @param boardname must have the form peer:port:boardid
	 */
	public void deleteBoard(String boardname) {
		synchronized(whiteboards) {
			Whiteboard whiteboard = whiteboards.get(boardname);
			if(whiteboard!=null) {
				whiteboards.remove(boardname);
				log.info("it is the deleted board operation :");
				for(int i=0;i<endpoint_2array.size();i++){
					if(endpoint_2array.get(i) != null){
						log.info("sending board deleted :"+whiteboard.getName());
						endpoint_2array.get(i).emit(boardDeleted,whiteboard.getName());
					}

				}
			}


		}
		updateComboBox(null);
	}

	/**
	 * Create a new local board with name peer:port:boardid.
	 * The boardid includes the time stamp that the board was created at.
	 */
	public void createBoard() {
		String name = peerport+":board"+Instant.now().toEpochMilli();
		Whiteboard whiteboard = new Whiteboard(name,false);
		addBoard(whiteboard,true);
	}

	/**
	 * Add a path to the selected board. The path has already
	 * been drawn on the draw area; so if it can't be accepted then
	 * the board needs to be redrawn without it.
	 * @param currentPath
	 */
	public void pathCreatedLocally(WhiteboardPath currentPath) {

		if(selectedBoard!=null) {
			long cur_version = selectedBoard.getVersion();
			if(!selectedBoard.addPath(currentPath,selectedBoard.getVersion())) {
				// some other peer modified the board in between
				drawSelectedWhiteboard(); // just redraw the screen without the path
			} else {
				// was accepted locally, so do remote stuff if needed
				if(selectedBoard.isRemote()){//remote
					if (endpoint1!= null){
						log.info("on path1 update:" + currentPath);
						String data = selectedBoard.getName()+"%"+cur_version+"%"+currentPath.toString();
						endpoint1.emit(boardPathUpdate,data);

					}
				}
				else {//local
					for(int i=0;i<endpoint_2array.size();i++){

						if (endpoint_2array.get(i)!= null){
							String data = selectedBoard.getName()+"%"+cur_version+"%"+currentPath.toString();
							log.info("on path2 update:" + currentPath);
							System.out.println(endpoint_2array.get(i));
							endpoint_2array.get(i).emit(boardPathUpdate,data);
						}
					}



				}
				//drawSelectedWhiteboard();
			}
		} else {
			log.severe("path created without a selected board: "+currentPath);
		}
	}

	/**
	 * Clear the selected whiteboard.
	 */
	public void clearedLocally() {
		if(selectedBoard!=null) {
			if(!selectedBoard.clear(selectedBoard.getVersion())) {
				// some other peer modified the board in between
				drawSelectedWhiteboard();
			} else {
				// was accepted locally, so do remote stuff if needed
				if(selectedBoard.isRemote()){
					if (endpoint1!= null){
						log.info("on path1 update: clear" + selectedBoard.toString());
						String data = selectedBoard.toString();
						endpoint1.emit(boardClearUpdate,data);

					}

				}
				else {//local
					for(int i=0;i<endpoint_2array.size();i++) {
						if (endpoint_2array.get(i) != null) {
							log.info("on path2 update: clear" + selectedBoard.toString());
							String data = selectedBoard.toString();
							endpoint_2array.get(i).emit(boardClearUpdate,data);
						}
					}


				}
				drawSelectedWhiteboard();
			}
		} else {
			log.severe("cleared without a selected board");
		}
	}

	/**
	 * Undo the last path of the selected whiteboard.
	 */
	public void undoLocally() {
		if(selectedBoard!=null) {
			if(!selectedBoard.undo(selectedBoard.getVersion())) {
				// some other peer modified the board in between
				drawSelectedWhiteboard();
			} else {
				if(selectedBoard.isRemote()){
					if (endpoint1!= null){
						log.info("on path1 update: undo" + selectedBoard.toString());
						String data = selectedBoard.toString();
						endpoint1.emit(boardUndoUpdate,data);

					}

				}
				else{
					for(int i=0;i<endpoint_2array.size();i++) {
						if (endpoint_2array.get(i) != null) {
							log.info("on path1 update: undo" + selectedBoard.toString());
							String data = selectedBoard.toString();
							endpoint_2array.get(i).emit(boardUndoUpdate,data);
						}
					}


				}
				drawSelectedWhiteboard();
			}
		} else {
			log.severe("undo without a selected board");
		}
	}

	/**
	 * The variable selectedBoard has been set.
	 */
	public void selectedABoard() {
		drawSelectedWhiteboard();
		log.info("selected board: "+selectedBoard.getName());
	}

	/**
	 * Set the share status on the selected board.
	 */
	public void setShare(boolean share) {

		if(selectedBoard!=null) {
			selectedBoard.setShared(share);
			//todo task
			if (share){
				// emit message to tell share
				System.out.println(endpoint);
				endpoint.emit(WhiteboardServer.shareBoard,selectedBoard.getName());
				//onShareBoard(endpoint);
			}
			if(!share){
				//emit message to tell unshare
				endpoint.emit(WhiteboardServer.unshareBoard,selectedBoard.getName());
				//unShareBoard(endpoint);
			}
		} else {
			log.severe("there is no selected board");
		}
	}

	/**
	 * Called by the gui when the user closes the app.
	 */
	public void guiShutdown() {
		// do some final cleanup
		log.info("it is the shut down for gui");
		HashSet<Whiteboard> existingBoards= new HashSet<>(whiteboards.values());
		existingBoards.forEach((board)->{
			deleteBoard(board.getName());
		});

		whiteboards.values().forEach((whiteboard)->{

			deleteBoard(whiteboard.getName());

		});
		if(peerport != "standalone") {
			clientManager.shutdown();
			try{
				peerManager.shutdown();
			}
			catch (NullPointerException e){
				System.exit(0);
			}
		}
		else{
			System.exit(0);
		}
	}



	/******
	 *
	 * GUI methods and callbacks from GUI for user actions.
	 * You probably do not need to modify anything below here.
	 *
	 ******/

	/**
	 * Redraw the screen with the selected board
	 */
	public void drawSelectedWhiteboard() {
		drawArea.clear();
		if(selectedBoard!=null) {
			selectedBoard.draw(drawArea);
		}
	}

	/**
	 * Setup the Swing components and start the Swing thread, given the
	 * peer's specific information, i.e. peer:port string.
	 */
	public void show(String peerport) {
		// create main frame
		JFrame frame = new JFrame("Whiteboard Peer: "+peerport);
		Container content = frame.getContentPane();
		// set layout on content pane
		content.setLayout(new BorderLayout());
		// create draw area
		drawArea = new DrawArea(this);

		// add to content pane
		content.add(drawArea, BorderLayout.CENTER);

		// create controls to apply colors and call clear feature
		JPanel controls = new JPanel();
		controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));

		/**
		 * Action listener is called by the GUI thread.
		 */
		ActionListener actionListener = new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == clearBtn) {
					clearedLocally();
				} else if (e.getSource() == blackBtn) {
					drawArea.setColor(Color.black);
				} else if (e.getSource() == redBtn) {
					drawArea.setColor(Color.red);
				} else if (e.getSource() == boardComboBox) {
					if(modifyingComboBox) return;
					if(boardComboBox.getSelectedIndex()==-1) return;
					String selectedBoardName=(String) boardComboBox.getSelectedItem();
					if(whiteboards.get(selectedBoardName)==null) {
						log.severe("selected a board that does not exist: "+selectedBoardName);
						return;
					}
					selectedBoard = whiteboards.get(selectedBoardName);
					// remote boards can't have their shared status modified
					if(selectedBoard.isRemote()) {
						sharedCheckbox.setEnabled(false);
						sharedCheckbox.setVisible(false);
					} else {
						modifyingCheckBox=true;
						sharedCheckbox.setSelected(selectedBoard.isShared());
						modifyingCheckBox=false;
						sharedCheckbox.setEnabled(true);
						sharedCheckbox.setVisible(true);
					}
					selectedABoard();
				} else if (e.getSource() == createBoardBtn) {
					createBoard();
				} else if (e.getSource() == undoBtn) {
					if(selectedBoard==null) {
						log.severe("there is no selected board to undo");
						return;
					}
					undoLocally();
				} else if (e.getSource() == deleteBoardBtn) {
					if(selectedBoard==null) {
						log.severe("there is no selected board to delete");
						return;
					}
					deleteBoard(selectedBoard.getName());
				}
			}
		};

		clearBtn = new JButton("Clear Board");
		clearBtn.addActionListener(actionListener);
		clearBtn.setToolTipText("Clear the current board - clears remote copies as well");
		clearBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		blackBtn = new JButton("Black");
		blackBtn.addActionListener(actionListener);
		blackBtn.setToolTipText("Draw with black pen");
		blackBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		redBtn = new JButton("Red");
		redBtn.addActionListener(actionListener);
		redBtn.setToolTipText("Draw with red pen");
		redBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		deleteBoardBtn = new JButton("Delete Board");
		deleteBoardBtn.addActionListener(actionListener);
		deleteBoardBtn.setToolTipText("Delete the current board - only deletes the board locally");
		deleteBoardBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		createBoardBtn = new JButton("New Board");
		createBoardBtn.addActionListener(actionListener);
		createBoardBtn.setToolTipText("Create a new board - creates it locally and not shared by default");
		createBoardBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		undoBtn = new JButton("Undo");
		undoBtn.addActionListener(actionListener);
		undoBtn.setToolTipText("Remove the last path drawn on the board - triggers an undo on remote copies as well");
		undoBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		sharedCheckbox = new JCheckBox("Shared");
		sharedCheckbox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(!modifyingCheckBox) setShare(e.getStateChange()==1);
			}
		});
		sharedCheckbox.setToolTipText("Toggle whether the board is shared or not - tells the whiteboard server");
		sharedCheckbox.setAlignmentX(Component.CENTER_ALIGNMENT);


		// create a drop list for boards to select from
		JPanel controlsNorth = new JPanel();
		boardComboBox = new JComboBox<String>();
		boardComboBox.addActionListener(actionListener);


		// add to panel
		controlsNorth.add(boardComboBox);
		controls.add(sharedCheckbox);
		controls.add(createBoardBtn);
		controls.add(deleteBoardBtn);
		controls.add(blackBtn);
		controls.add(redBtn);
		controls.add(undoBtn);
		controls.add(clearBtn);

		// add to content pane
		content.add(controls, BorderLayout.WEST);
		content.add(controlsNorth,BorderLayout.NORTH);

		frame.setSize(600, 600);

		// create an initial board
		createBoard();

		// closing the application
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent windowEvent) {
				if (JOptionPane.showConfirmDialog(frame,
						"Are you sure you want to close this window?", "Close Window?",
						JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)
				{
					guiShutdown();
					frame.dispose();
				}
			}
		});

		// show the swing paint result
		frame.setVisible(true);

	}

	/**
	 * Update the GUI's list of boards. Note that this method needs to update data
	 * that the GUI is using, which should only be done on the GUI's thread, which
	 * is why invoke later is used.
	 *
	 * @param select, board to select when list is modified or null for default
	 *                selection
	 */
	private void updateComboBox(String select) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				modifyingComboBox=true;
				boardComboBox.removeAllItems();
				int anIndex=-1;
				synchronized(whiteboards) {
					ArrayList<String> boards = new ArrayList<String>(whiteboards.keySet());
					Collections.sort(boards);
					for(int i=0;i<boards.size();i++) {
						String boardname=boards.get(i);
						boardComboBox.addItem(boardname);
						if(select!=null && select.equals(boardname)) {
							anIndex=i;
						} else if(anIndex==-1 && selectedBoard!=null &&
								selectedBoard.getName().equals(boardname)) {
							anIndex=i;
						}
					}
				}
				modifyingComboBox=false;
				if(anIndex!=-1) {
					boardComboBox.setSelectedIndex(anIndex);
				} else {
					if(whiteboards.size()>0) {
						boardComboBox.setSelectedIndex(0);
					} else {
						drawArea.clear();
						createBoard();
					}
				}

			}
		});
	}

}
