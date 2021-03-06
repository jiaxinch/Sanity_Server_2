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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.MidiDevice.Info;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONException;
import org.json.JSONObject;


public class Server extends WebSocketServer {
	private UserDAO userDao;
	private BudgetDAO budgetDao;
	private TransactionDAO transactionDao;

	public Server( int port ) throws UnknownHostException {
		super( new InetSocketAddress( port ) );
		userDao = new UserDAO();
		budgetDao = new BudgetDAO();
		transactionDao = new TransactionDAO();
	}

	public Server( InetSocketAddress address ) {
		super( address );
		userDao = new UserDAO();
		budgetDao = new BudgetDAO();
		transactionDao = new TransactionDAO();
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
				if(returnMessage.getString("status").equals("success")){
					System.out.println("send Email");
					RegisterEmailSender sendEmail= new RegisterEmailSender(user.username, user.email);
					sendEmail.start();
				}
			}
			else if(message1.equals("login")){	
				User user = new User(JSONMessage.getJSONObject("information"));
				JSONObject returnMessage=userDao.Login(user);
				if(returnMessage.getString("status").equals("success")){
					JSONObject info=budgetDao.getEverything(user, 0);
					returnMessage.put("information", info.getJSONObject("information"));
				}
				sendMessagetoClient(conn,returnMessage);
			}
			else if(message1.equals("createBudget")){
				Budget toAdd = new Budget(JSONMessage.getJSONObject("information"));
				JSONObject returnMessage= budgetDao.createBudget(toAdd);
				User user = new User(JSONMessage.getJSONObject("information").getString("email"));
				if(returnMessage.getString("status").equals("success")){
					JSONObject info=budgetDao.getEverything(user, 0);
					returnMessage.put("information", info.getJSONObject("information"));
				}
				sendMessagetoClient(conn,returnMessage);
			}
			else if (message1.equals("addTransaction")){
				Transaction toAdd = new Transaction(JSONMessage.getJSONObject("information"));
				toAdd.lat=JSONMessage.getJSONObject("information").getDouble("lat");
				toAdd.longi=JSONMessage.getJSONObject("information").getDouble("longi");
				JSONObject returnMessage = transactionDao.createTransaction(toAdd);
				User user = new User(JSONMessage.getJSONObject("information").getString("email"));
				if(returnMessage.getString("status").equals("success")){
					JSONObject info=budgetDao.getEverything(user, 0);
					returnMessage.put("information", info.getJSONObject("information"));
				}
				sendMessagetoClient(conn, returnMessage);
				transactionDao.duplicateTransaction(toAdd);
			}
			else if(message1.equals("requestBudgetList")){//deplete
				User user = new User(JSONMessage.getJSONObject("information").getString("email"));
				JSONObject returnMessage = budgetDao.getBudgetList(user);
				sendMessagetoClient(conn, returnMessage);
			}
			else if(message1.equals("requestBudget")){
				User user = new User(JSONMessage.getJSONObject("information").getString("email"));
				Budget budget=new Budget(JSONMessage.getJSONObject("information").getString("name"));
				JSONObject returnMessage=budgetDao.CateDao.getCategories(user, budget);
				sendMessagetoClient(conn, returnMessage);
			}
			else if(message1.equals("requestCategory")){
				User user = new User(JSONMessage.getJSONObject("information").getString("email"));
				Budget budget= new Budget(JSONMessage.getJSONObject("information").getString("budget"));
				Category category = new Category(JSONMessage.getJSONObject("information").getString("category"));
				JSONObject returnMessage = transactionDao.getTransactions(user,budget, category);
				sendMessagetoClient(conn, returnMessage);
			}
			else if(message1.equals("requestEverything")){
				User user= new User(JSONMessage.getJSONObject("information").getString("email"));
				JSONObject returnMessage=budgetDao.getEverything(user, 0);
				sendMessagetoClient(conn, returnMessage);
			}
			else if(message1.equals("changeUsername")){	
				User user = new User(JSONMessage.getJSONObject("information"));
				JSONObject returnMessage=userDao.changeUsername(user);
				sendMessagetoClient(conn,returnMessage);
			}
			else if(message1.equals("changePassword")){	
				User user1 = new User(JSONMessage.getJSONObject("information1"));
				User user2 = new User(JSONMessage.getJSONObject("information2"));
				JSONObject returnMessage=userDao.changePassword(user1, user2);
				sendMessagetoClient(conn,returnMessage);
			}
			else if(message1.equals("requestHistory")){	
				User user= new User(JSONMessage.getJSONObject("information").getString("email"));
				JSONObject returnMessage1=budgetDao.getEverything(user, 1);
				JSONObject returnMessage2=budgetDao.getEverything(user, 2);
				JSONObject returnMessage3=budgetDao.getEverything(user, 3);
				JSONObject returnMessage4=budgetDao.getEverything(user, 4);
				JSONObject returnMessage5=budgetDao.getEverything(user, 5);
				JSONObject returnMessage6=budgetDao.getEverything(user, 6);
				JSONObject returnMessage7=budgetDao.getEverything(user, 7);
				JSONObject messagenew= new JSONObject();
				messagenew.put("function", "requestHistory");
				messagenew.put("status", "success");
				messagenew.put("information1", returnMessage1.getJSONObject("information"));
				messagenew.put("information2", returnMessage2.getJSONObject("information"));
				messagenew.put("information3", returnMessage3.getJSONObject("information"));
				messagenew.put("information4", returnMessage4.getJSONObject("information"));
				messagenew.put("information5", returnMessage5.getJSONObject("information"));
				messagenew.put("information6", returnMessage6.getJSONObject("information"));
				messagenew.put("information7", returnMessage7.getJSONObject("information"));
				sendMessagetoClient(conn,messagenew);
			}
			else if(message1.equals("editBudget")){
				Budget toAdd = new Budget(JSONMessage.getJSONObject("information"));
				Budget old=new Budget(JSONMessage.getJSONObject("information").getString("oldName"));
				JSONObject returnMessage=budgetDao.editBudget(toAdd, old);
				User user = new User(JSONMessage.getJSONObject("information").getString("email"));
				if(returnMessage.getString("status").equals("success")){
					JSONObject info=budgetDao.getEverything(user, 0);
					returnMessage.put("information", info.getJSONObject("information"));
				}
				sendMessagetoClient(conn, returnMessage);
			}
			else if(message1.equals("deleteBudget")){
				String email = JSONMessage.getJSONObject("information").getString("email");
				String budgetName = JSONMessage.getJSONObject("information").getString("name");
				JSONObject returnMessage = budgetDao.deleteBudget(email, budgetName);
				if(returnMessage.getString("status").equals("success")){
					JSONObject info=budgetDao.getEverything(new User(email), 0);
					returnMessage.put("information", info.getJSONObject("information"));
				}
				sendMessagetoClient(conn, returnMessage);
			}
			else if (message1.equals("deleteCategory")){
				String email = JSONMessage.getJSONObject("information").getString("email");
				String budgetName = JSONMessage.getJSONObject("information").getString("budgetName");
				String categoryName = JSONMessage.getJSONObject("information").getString("categoryName");
				JSONObject returnMessage =budgetDao.CateDao.deleteCategories(email, budgetName, categoryName);
				if(returnMessage.getString("status").equals("success")){
					JSONObject info=budgetDao.getEverything(new User(email), 0);
					returnMessage.put("information", info.getJSONObject("information"));
				}
				sendMessagetoClient(conn, returnMessage);
			}
			else if (message1.equals("editCategory")){
				String email = JSONMessage.getJSONObject("information").getString("email");
				String budgetName = JSONMessage.getJSONObject("information").getString("budgetName");
				String oldName = JSONMessage.getJSONObject("information").getString("categoryOldName");
				String newName = JSONMessage.getJSONObject("information").getString("categoryNewName");
				Double newLimit = JSONMessage.getJSONObject("information").getDouble("limit");
				JSONObject returnMessage = budgetDao.CateDao.editCategory(email, oldName, newName, budgetName, newLimit);
				if(returnMessage.getString("status").equals("success")){
					JSONObject info=budgetDao.getEverything(new User(email), 0);
					returnMessage.put("information", info.getJSONObject("information"));
				}
				sendMessagetoClient(conn, returnMessage);
			}
			else if (message1.equals("addCategory")){
				String email =JSONMessage.getJSONObject("information").getString("email");
				String budgetName = JSONMessage.getJSONObject("information").getString("budgetName");
				String categoryName = JSONMessage.getJSONObject("information").getString("categoryName");
				Double limit = JSONMessage.getJSONObject("information").getDouble("limit");
				JSONObject returnMessage=budgetDao.CateDao.addSingleCategory(email, budgetName,categoryName,limit);
				if(returnMessage.getString("status").equals("success")){
					JSONObject info=budgetDao.getEverything(new User(email), 0);
					returnMessage.put("information", info.getJSONObject("information"));
				}
				sendMessagetoClient(conn, returnMessage);
			}
			else if (message1.equals("autoLogin")){
				String email = JSONMessage.getJSONObject("information").getString("email");
				JSONObject returnMessage = userDao.autoLogin(email);
				if(returnMessage.getString("status").equals("success")){
					JSONObject info=budgetDao.getEverything(new User(email), 0);
					returnMessage.put("information", info.getJSONObject("information"));
				}
				sendMessagetoClient(conn,returnMessage);
			}
			else if(message1.equals("requestSummary")){
				String email = JSONMessage.getJSONObject("information").getString("email");
				String budgetName = JSONMessage.getJSONObject("information").getString("budgetName");
				JSONObject returnMessage = budgetDao.sendBudgetSummary(budgetName, email);
				sendMessagetoClient(conn, returnMessage);
			}
			else if(message1.equals("shareBudget")){
				String email=JSONMessage.getJSONObject("information").getString("email");
				String budgetName=JSONMessage.getJSONObject("information").getString("budgetName");
				String emailShare=JSONMessage.getJSONObject("information").getString("emailShare");
				Boolean success=budgetDao.shareBudget(email, budgetName, emailShare);
				
				JSONObject returnMessage = new JSONObject();
				returnMessage.put("function", "shareBudget");
				if(success){
					returnMessage.put("status", "success");
				}
				else{
					returnMessage.put("status", "fail");
				}
				sendMessagetoClient(conn, returnMessage);
			}
			else if(message1.equals("editTransaction")){
				JSONObject info = JSONMessage.getJSONObject("information");
				Transaction toDelete = new Transaction(info.getString("email"),
						info.getString("oldDescription"),info.getString("oldCategory"),info.getString("oldBudget"),
						info.getString("oldDate"),info.getDouble("oldAmount"));
				Transaction toAdd = new Transaction(info.getString("email"),
						info.getString("newDescription"),info.getString("newCategory"),info.getString("newBudget"),
						info.getString("newDate"),info.getDouble("newAmount"));
				JSONObject messageDelete = transactionDao.deleteTransaction(toDelete);
				JSONObject messageAdd = transactionDao.createTransaction(toAdd);
				System.out.println("finish add and delete");
				if(messageDelete.getString("status").equals("success")&&messageAdd.getString("status").equals("success")){
					JSONObject returnMessage = new JSONObject();
					returnMessage.put("function", "editTransaction");
					returnMessage.put("status", "success");
					JSONObject infomation=budgetDao.getEverything(new User(toDelete.email), 0);
					returnMessage.put("information", infomation.getJSONObject("information"));
					sendMessagetoClient(conn, returnMessage);
				}			
			}
			else if(message1.equals("deleteTransaction")){
				Transaction toDelete = new Transaction(JSONMessage.getJSONObject("information"));
				JSONObject returnMessage =transactionDao.deleteTransaction(toDelete);
				System.out.println("finish delete transaction");
				if(returnMessage.getString("status").equals("success")){
					JSONObject info=budgetDao.getEverything(new User(toDelete.email), 0);
					returnMessage.put("information", info.getJSONObject("information"));
					System.out.println("put information in the message");
					sendMessagetoClient(conn, returnMessage);
				}	
			}
			else if(message1.equals("forgetPassword")){
				JSONObject info=JSONMessage.getJSONObject("information");
				String email=info.getString("email");
				userDao.ForgetPassword(email);
			}
			else if(message1.equals("forgetChangePassword")){
				JSONObject info=JSONMessage.getJSONObject("information");
				Boolean success=userDao.forgetChangePassword(info.getString("email"), info.getInt("code"), 
						info.getString("password1"), info.getString("password2"));
				
				JSONObject returnMessage = new JSONObject();
				returnMessage.put("function", "forgetChangePassword");
				if(success){
					returnMessage.put("status", "success");
				}
				else{
					returnMessage.put("status", "fail");
				}
				sendMessagetoClient(conn, returnMessage);
				
				
			}
		}catch(JSONException e){
			System.out.println(e.getMessage());
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
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
