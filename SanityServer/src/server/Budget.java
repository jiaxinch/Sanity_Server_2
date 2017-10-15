package server;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Budget {
	String email, budgetName,date;
	Integer period;
	Integer budgetTotal;
	ArrayList<Category> categories;
	public Budget(JSONObject JSONMessage){
		try{
			email=JSONMessage.getString("email");
			budgetName=JSONMessage.getString("name");
			date=JSONMessage.getString("date");
			period=Integer.parseInt(JSONMessage.getString("period"));
			budgetTotal= Integer.parseInt(JSONMessage.getString("budgetTotal"));
			JSONArray jArray=JSONMessage.getJSONArray("categories");
			for(int i=0;i<jArray.length();++i){
				jArray.get(i);
			}
		}catch(JSONException e){
			System.out.println(e.getMessage());
			System.out.println("contructing budget Error");
		}
	}
}
