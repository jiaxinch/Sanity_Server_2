package server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class TransactionDAO extends DAO{
	public JSONObject createTransaction(Transaction toAdd){	
		try{
			JSONObject message = new JSONObject();
			message.put("function", "createTransaction");// we need give more info to this message	
			if(checkTransactionExistNew(toAdd)){
				message.put("status", "fail");
				message.put("detail", "same transaction alrealy exist");
			}
			else{
				addTransactionDB(toAdd);
				updateDB(toAdd);
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
	
	public void duplicateTransaction(Transaction toAdd){
		try{
			checkTransactionExistNew(toAdd);
			Integer budgetID=toAdd.budgetID;
			System.out.println(budgetID);
			
			String result=findShare(budgetID);
			
			
			String[] resultInd=result.split(",");
			for(int i=0;i<resultInd.length;i++){
				if(!resultInd[i].equals(budgetID.toString())){
					Connection conn = getDBConnection();
					int newID=Integer.parseInt(resultInd[i]);
					System.out.println(newID);
					PreparedStatement statement=conn.prepareStatement("SELECT * FROM SanityDB.Sanity_budget WHERE Budget_id=?");
					statement.setInt(1, newID);
					ResultSet rs=statement.executeQuery();
					String newEmail="";
					if(rs.next()){
						newEmail=rs.getString("Email");
					}
					toAdd.email=newEmail;
					System.out.println("email"+newEmail);
					createTransaction(toAdd);
				}
			}	
		}catch(SQLException e){
			System.out.println(e.getMessage());
		}
	}
	
	public JSONObject getTransactions(User user, Budget budget, Category category){
		JSONObject returnMessage = new JSONObject();
		try{
			category.userID =UserFindUserID(user);
			budget.userId=category.userID;
			BudgetFindBudgetID(budget);
			category.budgetID=budget.budgetId;
			CategoryFindCategoryID(category);
			Connection conn= getDBConnection();
			PreparedStatement getTransactions = conn.prepareStatement("SELECT * FROM SanityDB.Transaction "
					+ "WHERE User_id=? AND Budget_id=? AND Category_id=?");
			getTransactions.setInt(1, category.userID);
			getTransactions.setInt(2, category.budgetID);
			getTransactions.setInt(3, category.categoryID);
			ResultSet rs=getTransactions.executeQuery();
			JSONObject generalInfo = new JSONObject();
			JSONArray Jarray= new JSONArray();
			generalInfo.put("budget", budget.budgetName);
			generalInfo.put("category", category.categoryName);
			while(rs.next()){
				JSONObject temp = new JSONObject();
				temp.put("description", rs.getString("Transaction_description"));
				temp.put("amount", rs.getDouble("Transaction_amount"));
				temp.put("date", rs.getDate("Transaction_date").toString());
				Jarray.put(temp);
			}
			generalInfo.put("transctionList", Jarray);
			returnMessage.put("function", "returnTransactionsList");
			returnMessage.put("Information", generalInfo);
		}catch (SQLException e) {
			System.out.println(e.getMessage());
			System.out.println("getTrasactions error");
		}catch(JSONException e){
			System.out.println("getTransactions error");
		}
		return returnMessage;
	}
	
	public JSONObject deleteTransaction(Transaction transaction){
		JSONObject returnMessage = new JSONObject();
		try{
			returnMessage.put("function", "deleteTransaction");
			Connection conn=getDBConnection();
			PreparedStatement findID =conn.prepareStatement("SELECT * FROM SanityDB.Sanity_transaction WHERE Transaction_description=? "
					+ "AND Email=? AND Transaction_amount=? AND Transaction_date=? AND Budget_name=? AND Category_name=? ");
			findID.setString(1, transaction.description);
			findID.setString(2, transaction.email);
			findID.setDouble(3, transaction.amount);
			findID.setDate(4,java.sql.Date.valueOf(transaction.date));
			findID.setString(5, transaction.budget);
			findID.setString(6, transaction.category);
			ResultSet rs=findID.executeQuery();
			Integer transactionID =-1;
			if(rs.next()){
				transactionID=rs.getInt("Transaction_id");
			}
			PreparedStatement statement= conn.prepareStatement("DELETE FROM SanityDB.Transaction WHERE "
					+ "Transaction_id=?");
			statement.setInt(1, transactionID);
			statement.executeUpdate();
			if(statement!=null){
				statement.close();
			}
			if(conn!=null){
				conn.close();
			}
			returnMessage.put("status", "success");
		}catch(SQLException e){
			System.out.println(e.getMessage());
			System.out.println("delete Transaction Error");
		}catch(JSONException e){
			System.out.println(e.getMessage());
			System.out.println("delete Transaction Error");
		}
		return returnMessage;
	}
	public Boolean checkTransactionExistNew(Transaction toAdd) throws SQLException{
		Connection conn =getDBConnection();
		PreparedStatement findID = conn.prepareStatement("SELECT * FROM SanityDB.Sanity_category WHERE "
				+ "Email=? AND Budget_name=? AND Category_name=?");
		findID.setString(1, toAdd.email);
		findID.setString(2, toAdd.budget);
		findID.setString(3, toAdd.category);
		try{
			ResultSet resultSet= findID.executeQuery();
			if(resultSet.next()){
				toAdd.userID=resultSet.getInt("User_id");
				toAdd.budgetID=resultSet.getInt("Budget_id");
				toAdd.categoryID=resultSet.getInt("Category_id");
			}
			if(resultSet!=null){
				resultSet.close();
			}
		}catch (SQLException e) {
			System.out.println("SQLException in checkTransactionExistNew");
			System.out.println(e.getMessage());
		}finally{
			if(findID!=null){
				findID.close();
			}
		}
		PreparedStatement findTransaction = conn.prepareStatement("SELECT * FROM SanityDB.Transaction WHERE "
				+ "Transaction_description=? AND User_id=? AND Transaction_amount=? AND Transaction_date=?"
				+ "AND Budget_id=? AND Category_id=?");
		findTransaction.setString(1, toAdd.description);
		findTransaction.setInt(2, toAdd.userID);
		findTransaction.setDouble(3, toAdd.amount);
		findTransaction.setDate(4,java.sql.Date.valueOf(toAdd.date));
		findTransaction.setInt(5, toAdd.budgetID);
		findTransaction.setInt(6, toAdd.categoryID);
		try{
			ResultSet rs = findTransaction.executeQuery();
			if(rs.next()){
				return true;
			}
			else{
				return false;
			}
		}catch(SQLException e){
			System.out.println(e.getMessage());
			System.out.println("check transaction error ");
		}finally{
			if(findTransaction!=null){
				findTransaction.close();
			}
			if(conn!=null){
				conn.close();
			}
		}
		return false;
	}
	public Boolean checkTransactionExist(Transaction toAdd) throws SQLException{
		Connection conn= getDBConnection();
		TransactionFindUserID(toAdd);
		TransactionFindBudgetID(toAdd);
		TransactionFindCategoryID(toAdd);
		PreparedStatement findTransaction = conn.prepareStatement("SELECT * FROM SanityDB.Transaction WHERE "
				+ "Transaction_description=? AND User_id=? AND Transaction_amount=? AND Transaction_date=?"
				+ "AND Budget_id=? AND Category_id=?");
		findTransaction.setString(1, toAdd.description);
		findTransaction.setInt(2, toAdd.userID);
		findTransaction.setDouble(3, toAdd.amount);
		findTransaction.setDate(4,java.sql.Date.valueOf(toAdd.date));
		findTransaction.setInt(5, toAdd.budgetID);
		findTransaction.setInt(6, toAdd.categoryID);
		try{
			ResultSet rs = findTransaction.executeQuery();
			if(rs.next()){
				return true;
			}
			else{
				return false;
			}
		}catch(SQLException e){
			System.out.println(e.getMessage());
			System.out.println("check transaction error ");
		}finally{
			if(findTransaction!=null){
				findTransaction.close();
			}
			if(conn!=null){
				conn.close();
			}
		}
		return false;
	}	
	private void addTransactionDB(Transaction toAdd) throws SQLException{
		Connection conn= getDBConnection();
		PreparedStatement insert = conn.prepareStatement("INSERT INTO SanityDB.Transaction (Transaction_description, "
				+ "User_id, Transaction_amount, Transaction_date,Budget_id,Category_id,Longi,Lat) VALUE (?,?,?,?,?,?,?,?)");
		insert.setString(1, toAdd.description);
		insert.setInt(2, toAdd.userID);
		insert.setDouble(3, toAdd.amount);
		insert.setDate(4, java.sql.Date.valueOf(toAdd.date));
		insert.setInt(5, toAdd.budgetID);
		insert.setInt(6, toAdd.categoryID);
		insert.setDouble(7, toAdd.longi);
		insert.setDouble(8, toAdd.lat);
		try{
			insert.executeUpdate();
		}catch(SQLException e){
			System.out.println("insert transaction error ");
			System.out.println(e.getMessage());
		}finally{
			if(insert!=null){
				insert.close();
			}
			if(conn!=null){
				conn.close();
			}
		}
	}
	private void updateDB(Transaction toAdd) throws SQLException{
		Connection conn= getDBConnection();
		Double budgetspent=0.0;
		Double categoryspent=0.0;
		PreparedStatement getbudget = conn.prepareStatement("SELECT * FROM SanityDB.Budget WHERE Budget_id=?");
		PreparedStatement getcategory = conn.prepareStatement("SELECT * FROM SanityDB.Category WHERE Category_id=?");
		getbudget.setInt(1, toAdd.budgetID);
		getcategory.setInt(1, toAdd.categoryID);
		try{
			ResultSet budget =getbudget.executeQuery();
			ResultSet cate = getcategory.executeQuery();
			budget.next();
			budgetspent=budget.getDouble("Budget_spent");
			cate.next();
			categoryspent=cate.getDouble("Category_spent");
		}catch(SQLException e){
			System.out.println("get budget and category error(update transaction) ");
		}finally{
			if(getbudget!=null){
				getbudget.close();
			}
			if(getcategory!=null){
				getcategory.close();
			}
		}
		budgetspent+= toAdd.amount;
		categoryspent+=toAdd.amount;
		PreparedStatement updateBudget= conn.prepareStatement("UPDATE SanityDB.Budget SET Budget_spent=?"
				+ "WHERE Budget_id =? ");
		PreparedStatement updateCategory= conn.prepareStatement("UPDATE SanityDB.Category SET Category_spent=?"
				+ "WHERE Category_id =? ");
		updateBudget.setDouble(1, budgetspent);
		updateBudget.setDouble(2, toAdd.budgetID);
		updateCategory.setDouble(1, categoryspent);
		updateCategory.setDouble(2, toAdd.categoryID);
		try{
			updateBudget.executeUpdate();
			updateCategory.executeUpdate();
		}catch(SQLException e){
			System.out.println("update transaction error");
		}finally{
			if(updateBudget!=null){
				updateBudget.close();
			}
			if(updateCategory!=null){
				updateCategory.close();
			}
			if(conn!=null){
				conn.close();
			}
		}
	}
	
}
