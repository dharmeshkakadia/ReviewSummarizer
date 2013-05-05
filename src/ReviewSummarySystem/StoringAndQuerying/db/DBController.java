package db;

import java.net.UnknownHostException;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

public class DBController {
	
	private static DBController instance; 
	private static Mongo mongoDb;
	private static DB db;
	
	private DBController() {
		try {
			mongoDb = new Mongo();
			db = mongoDb.getDB("ProductReview");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (MongoException e) {
			e.printStackTrace();
		}
	}
	
	public static synchronized DBController getInstance() {
		if (instance == null) {
			instance = new DBController();
		}
		return instance;
	}
	
	public static DB getDB(){
		return getInstance().db;
	}

}
