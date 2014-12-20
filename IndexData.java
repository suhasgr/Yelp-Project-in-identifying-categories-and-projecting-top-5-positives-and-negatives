import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * Index data is used to Lucene Index the Data extracted from mongoDB 
 * @author $uh@$
 *
 */
class IndexData
{
	/**
	 * Constructor sets up mongo client 
	 * @throws UnknownHostException
	 */
	IndexData() throws UnknownHostException
	{
		_mongoObject = new MongoObject();
		//MongoClient mongoClient = mongoObject.getClient();
	
	}
	
	/**
	 *  collects all the collections in Yelp database on MongoDB 
	 */
	public void getCollections()
	{
		DB yelpDB = _mongoObject.getDB("yelp_db");
		
		_businessCollection = yelpDB.getCollection("business");
		_reviewCollection = yelpDB.getCollection("review");
		_tipCollection = yelpDB.getCollection("tip");
	}
	
	/**
	 * @return : total number of business present in Yelp dataset
	 */
	public int getBusinessCount()
	{
		return  (int) _businessCollection.count();
	}
	
	/**
	 * To get complete text from specified query and fields from a specified field of collection 
	 * @param collection : Collection Business/review/tip
	 * @param query : query containing for which business ID
	 * @param fields : fields to be extracted for that business
	 * @param field : field from which data should be extracted 
	 * @return : collective string(appended if many extracted)
	 */
	private String completeExtraction(DBCollection collection, BasicDBObject query, BasicDBObject fields, String field)
	{
		String completeExt = ""; // Don't Keep it null as we cannot insert null in lucene doc;
		DBObject temporaryObject;
		DBCursor cursor = collection.find(query,fields);
		while(cursor.hasNext())
		{
			temporaryObject = cursor.next();
			completeExt+= temporaryObject.get(field).toString().replace("\n", "").replace("\n", "");
		}
		return completeExt;
	}
	
	/**
	 * Complete review extraction along with stars for each review for given business 
	 * @param collection : It will be usually review collection
	 * @param query : Contains buinessID for which review needs to be extracted 
	 * @param fields : text and starts needed to be extracted 
	 * @param reviewRating : Out parameter which is a map of containing Review and starts
	 */
	private void completeReviewExtractionWithRating(DBCollection collection,BasicDBObject query, BasicDBObject fields,HashMap<String, Integer> reviewRating)
	{
		DBObject temporaryObject;
		DBCursor cursor = collection.find(query,fields);
		while(cursor.hasNext())
		{
			temporaryObject = cursor.next();
			String text = temporaryObject.get("text").toString().replace("\n", "").replace("\n", "");
			Integer rating = (Integer)temporaryObject.get("stars");
			reviewRating.put(text, rating);
		}
		
	}
	
	/**
	 * Index all the fields based on BusineesID(easier for extraction)
	 * @param indexDirPath : path of Index to which Lucene index needs to be stored 
	 * @param analyzer : type of analyzer needed to be used for indexing
	 * @throws IOException
	 */
	public void indexBasedonBusiness(String indexDirPath, Analyzer analyzer) throws IOException
	{
		ArrayList<Document> docList = new ArrayList<Document>();
		BasicDBObject query,fields;
		query = new BasicDBObject();
		fields = new BasicDBObject("business_id",1).append("_id", 0).append("name",1);
		
		DBCursor businessCursor = _businessCollection.find(query, fields);
		long totalBusiness = _businessCollection.count();
		long count = 0;
		/*
		 * For each business
		 */
		while(businessCursor.hasNext())
		{
			DBObject businessObject = businessCursor.next();
			
			/*
			 * Get BusinessID and Name form business collection
			 */
			String businessID = businessObject.get("business_id").toString();
			String businessName = businessObject.get("name").toString();			
			//System.out.println("Business name = "+businessName);                //remove
			
			/*
			 * Get all reviews for this business
			 */
			BasicDBObject reviewQuery,reviewFields;
			reviewQuery = new BasicDBObject();
			reviewQuery.put("business_id", businessID);
			reviewFields = new BasicDBObject("business_id",1).append("_id", 0).append("text",1);
			String reviewText = completeExtraction(_reviewCollection, reviewQuery, reviewFields, "text");
			
			/*
			 * Get all tip for this business
			 */
			BasicDBObject tipQuery,tipFields;
			tipQuery = new BasicDBObject();
			tipQuery.put("business_id", businessID);
			tipFields = new BasicDBObject("business_id",1).append("_id", 0).append("text",1);
			String tipText = completeExtraction(_tipCollection, tipQuery, tipFields, "text");			
			
			Document luceneDoc = new Document();
			
			FieldType type = new FieldType();
			type.setIndexed(true);
			type.setStored(true);
			type.setStoreTermVectors(true);
			
			/*
			 * add Name,ID,Review,tip to lucene doc
			 */
			Field fieldID = new Field("BIZID", businessID, type);
			luceneDoc.add(fieldID);
			
			Field fieldName = new Field("BIZNAME", businessName, type);
			luceneDoc.add(fieldName);
			
			Field fieldTip = new Field("BIZTIP", tipText, type);
			luceneDoc.add(fieldTip);
			
			Field fieldReview = new Field("BIZREVIEW", reviewText, type);
			luceneDoc.add(fieldReview);
			
			Field fieldReviewTip = new Field("BIZREVIEWTIP", reviewText+" "+tipText, type);
			luceneDoc.add(fieldReviewTip);
			
			/*
			 * Add Lucene doc to document list
			 */
			docList.add(luceneDoc);
			
			/*
			 * Index for every 1000 documents and clear memory otherwise heap memory of system fills up
			 */
			if((++count % 1000) == 0)
			{
				index(indexDirPath,analyzer,docList);
				docList.clear();
			}
			
			if(count >= (0.6 * totalBusiness) || count == 10)   //remove this after the check
			{
				index(indexDirPath,analyzer,docList);// delete this later
				break;//Training Data
			}
			System.out.println(count);
		}
	}
	
	/**
	 * To get categories list form yelp data
	 * @return : Category list of all business in an array
	 * @throws IOException
	 */
	public ArrayList<String> getCategories() throws IOException
	{
		ArrayList<String> categories = new ArrayList<String>();
		BasicDBObject query,fields;
		query = new BasicDBObject();
		fields = new BasicDBObject("business_id",1).append("categories",1);
		
		DBCursor businessCursor = _businessCollection.find(query, fields);
		
		while(businessCursor.hasNext())
		{
			DBObject businessObject = businessCursor.next();
			StringBuilder category = new StringBuilder(businessObject.get("categories").toString());
			
			int k1 = category.indexOf("\"");
			while(k1 > 0)    
			{
			   k1++;
			   int k2 = category.indexOf("\"",k1);
			      
			   if (k2>=0)
			   {
				   categories.add(category.substring(k1,k2).trim());  
			   }
			   k2++;
			   k1 = category.indexOf("\"", k2);
			}
		}
		return categories;
	}
	
	/**
	 * Lucene index the documents to given position 
	 * @param indexDirPath : path to which index files to be stored 
	 * @param analyzer : Analyzer for indexing 
	 * @param docList : list of Lucene documents needed to be indexed.
	 * @throws IOException
	 */
	private void index(String indexDirPath, Analyzer analyzer, ArrayList<Document> docList) throws IOException
	{
		File indexDir = new File(indexDirPath);		
		Directory dir = FSDirectory.open(indexDir);	
		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_4_10_0, analyzer);	
		iwc.setOpenMode(OpenMode.CREATE);
		IndexWriter indexWriter = new IndexWriter(dir, iwc);
	
		Iterator<Document> docIterator = docList.iterator();
		while (docIterator.hasNext()) 
		{
			indexWriter.addDocument(docIterator.next());
		}
		
		indexWriter.forceMerge(1);
		indexWriter.commit();												
		indexWriter.close();
	}
	
	/**
	 * get complete review given business ID
	 * @param businessID : Business ID- example: xDXVHNv6socQ7a2zF9q-mw
	 * @return : map containing review and Stars
	 */
	public HashMap<String, Integer> getCompleteReview(String businessID)
	{
		HashMap<String, Integer> reviewRating =  new HashMap<String, Integer>();
		BasicDBObject reviewQuery,reviewFields;
		reviewQuery = new BasicDBObject();
		reviewQuery.put("business_id", businessID);
		reviewFields = new BasicDBObject("business_id",1).append("_id", 0).append("stars",1).append("text",1);
		completeReviewExtractionWithRating(_reviewCollection, reviewQuery, reviewFields,reviewRating);
		return reviewRating;
	}
	
	/**
	 * We have reviews with ratings for a business. Given a string we need to
	 * pick those sentences which contains string. Create a map for all these matched sentences along with the stars
	 * @param sentencesWithRating : Contains Map which has reviews and stars for a business
	 * @param subString : Trigram for which sentences in review needs to be matched
	 * @param sentencelistWithRating : Out parameter containing sentences matching substring with along with stars 
	 */
	public void getSentence(HashMap<String, Integer> sentencesWithRating,String subString,HashMap<String, Integer> sentencelistWithRating)
	{
		Iterator start = sentencesWithRating.entrySet().iterator();
		/*
		 * For each review
		 */
		while(start.hasNext())
		{
			Map.Entry pair = (Map.Entry)start.next();
			String sentences = ((String)pair.getKey()).toLowerCase();
			subString = subString.toLowerCase();
			int k = 0;
			while(k < sentences.length())
			{
				/* get sentence from review*/
				int k1 = sentences.indexOf(".", k);
				if(k1 > 0 )
				{
					String sentence = sentences.substring(k,k1 );
					String[] words = subString.split(" ");
					Boolean contains = true;
					for(String t:words)
					{
						if(!sentence.contains(t))
						{
							contains = false;
							break;
						}
					}
					/* match sentence with substring. if present put it into map with stars*/ 
					if(contains)
					{
						sentencelistWithRating.put(sentence,(Integer)pair.getValue());
					}	
				}
				else
					break;
				k=++k1;
			}
		}
	}
	
	private MongoObject _mongoObject;
	private DBCollection _businessCollection;
	private DBCollection _reviewCollection;
	private DBCollection _tipCollection;
}