package server;
/*
 * Copyright (c) 2010-2017 Nathan Rajlich
 *
 *  Permission is hereby granted, free of charge, to any person
 *  obtaining a copy of this software and associated documentation
 *  files (the "Software"), to deal in the Software without
 *  restriction, including without limitation the rights to use,
 *  copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following
 *  conditions:
 *
 *  The above copyright notice and this permission notice shall be
 *  included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 *  OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 *  HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 *  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 *  OTHER DEALINGS IN THE SOFTWARE.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * A simple WebSocketServer implementation. Keeps track of a "chatroom".
 */
public class Server extends WebSocketServer {
	private UserDAO userDao;
	private BudgetDAO budgetDao;

	public Server( int port ) throws UnknownHostException {
		super( new InetSocketAddress( port ) );
		userDao = new UserDAO();
		budgetDao = new BudgetDAO();
	}

	public Server( InetSocketAddress address ) {
		super( address );
		userDao = new UserDAO();
		budgetDao = new BudgetDAO();
	}

	@Override
	public void onOpen( WebSocket conn, ClientHandshake handshake ) {
		broadcast( "new connection: " + handshake.getResourceDescriptor() );
		System.out.println( conn.getRemoteSocketAddress().getAddress().getHostAddress() + " connected!" );
	}

	@Override
	public void onClose( WebSocket conn, int code, String reason, boolean remote ) {
		broadcast( conn + " disconnect" );
		System.out.println( conn + " disconnect" );
	}

	@Override
	public void onMessage( WebSocket conn, String message ) {	
		JSONObject JSONMessage;
		try{
			JSONMessage = new JSONObject(message);
			String message1 = JSONMessage.getString("function");
			if(message1.equals("register")){		
				User user=new User(JSONMessage.getJSONObject("information"));
				JSONObject returnMessage=userDao.Register(user);	
				sendMessagetoClient(conn,returnMessage);
			}
			else if(message1.equals("login")){	
				User user = new User(JSONMessage.getJSONObject("information"));
				JSONObject returnMessage=userDao.Login(user);
				sendMessagetoClient(conn,returnMessage);
			}
			else if(message1.equals("createBudget")){
				Budget toAdd = new Budget(JSONMessage.getJSONObject("information"));
				JSONObject returnMessage= budgetDao.createBudget(toAdd);
				sendMessagetoClient(conn,returnMessage);
			}
		}catch(JSONException e){
			System.out.println(e.getMessage());
		}
	}

	@Override
	public void onFragment( WebSocket conn, Framedata fragment ) {
		System.out.println( "received fragment: " + fragment );
	}

	@Override
	public void onError( WebSocket conn, Exception ex ) {
		ex.printStackTrace();
		if( conn != null ) {
			// some errors like port binding failed may not be assignable to a specific websocket
		}
	}

	@Override
	public void onStart() {
		System.out.println("Server started!");
	}
	
	private void sendMessagetoClient(WebSocket conn,JSONObject Message){
		 List<WebSocket> client = new ArrayList<WebSocket>();
		 client.add(conn);
		 broadcast(Message.toString(),client);
	}
	
	public static void main( String[] args ) throws InterruptedException , IOException {
		WebSocketImpl.DEBUG = true;
		int port = 9999; // 843 flash policy port
		/*try {
			port = Integer.parseInt( args[ 0 ] );
		} catch ( Exception ex ) {
		}*/
		Server s = new Server( port );
		s.start();
		System.out.println( "ChatServer started on port: " + s.getPort() );

		BufferedReader sysin = new BufferedReader( new InputStreamReader( System.in ) );
		while ( true ) {
			String in = sysin.readLine();
			s.broadcast( in );
			if( in.equals( "exit" ) ) {
				s.stop();
				break;
			}
		}
	}

}
