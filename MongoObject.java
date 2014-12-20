import java.net.UnknownHostException;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;

/**
 * MongoClass to initiate Mongo object and load database
 * @author $uh@$
 *
 */
class MongoObject
{
	/**
	 * Constructor to create a mongo client 
	 * @throws UnknownHostException
	 */
	MongoObject() throws UnknownHostException
	{
		_mongoClient = new MongoClient();
		_mongoClient.setWriteConcern(WriteConcern.JOURNALED);
	}
	
	private MongoClient _mongoClient;
	
	/**
	 * @return : mongoclient to Indexdata object to read from MongoServer 
	 */
	MongoClient getClient()
	{
		return _mongoClient;
	}
	
	/**
	 * Ton set Mongo client to particular database
	 * @param databaseName : name of database
	 * @return : Mongo database object  
	 */
	public DB getDB(String databaseName)
	{
		DB db = _mongoClient.getDB(databaseName);
		return db;
	}
	
}