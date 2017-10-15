package server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.JSONException;
import org.json.JSONObject;

public class BudgetDAO extends DAO{
	public JSONObject addBudget(Budget toAdd){	
		try{		
			JSONObject message = new JSONObject();
			message.put("function", "register");	
			if(checkBudgetExist(toAdd)){
				message.put("status", "fail");
			}
			else{
				//addUser(u);
				System.out.println("return false");
				message.put("status", "success");
			}	
			return message;
		}catch(SQLException e){
			System.out.println(e.getMessage());
		}catch (JSONException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}	
		return null;
	}
	
	private Boolean checkBudgetExist(Budget toAdd) throws SQLException{
		Connection conn=getDBConnection();
		int user_id=-1;
		PreparedStatement userStatement = conn.prepareStatement("SELECT * FROM SanityDB.User WHERE Email=?");
		userStatement.setString(1, toAdd.email);
		try{
			ResultSet rs= userStatement.executeQuery();
			user_id=rs.getInt("User_id");
		}catch(SQLException e){
			System.out.println(e.getMessage());
			System.out.println("add budget error");
		}finally{
			if (userStatement != null) {
				userStatement.close();
			}
		}
		PreparedStatement st = conn.prepareStatement("SELECT * FROM SanityDB.Budget WHERE Budget_name=? "
				+ "AND User_id=? AND Budget_period=? AND Start_date AND Budget_total AND Budget_spent");
		st.setString( 1, toAdd.budgetName);
		st.setInt( 2, user_id);
		st.setInt( 3, toAdd.period);
		st.setDate(4, java.sql.Date.valueOf(toAdd.date));
		st.setInt(5, 0);
		st.setInt(6, 0);
		try{
			ResultSet rs = st.executeQuery();
			if(rs.next()){
				return true;
			}
			else{
				return false;
			}	
		}catch(SQLException e){
			System.out.println(e.getMessage());
		}finally{
			if (conn != null) {
				conn.close();
			}
			if (st != null) {
				st.close();
			}
		}
		return true;
	}
}