import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.lucene.queryparser.classic.ParseException;

public class Yelp 
{
	/**
	 * Helper Functions: Remove duplicates from a list  
	 * @param list : Input list of strings to remove duplicates
	 * @return: returns list of strings with removed comments 
	 */
	
	 public static List <String>  removeDuplicate(List <String> list) 
	 {
		    Set <String> set = new HashSet <String>();
		    List <String> newList = new ArrayList <String>();
		    for (Iterator <String>iter = list.iterator();    iter.hasNext();) 
		    {
		      Object element = iter.next();
		       if (set.add((String) element))
		          newList.add((String) element);
		    }
		    list.clear();
		    list.addAll(newList);
		    return list;
	}

		  
	/**
	 * @param args : Input arguments of project : 1 - path of Index to be stored
	 * 2- Path of indexed files AP89corpus
	 * 3 - Path of Input maxtentTaggerPath(stanford-postagger-2014-01-04\models\english-left3words-distsim.tagger)
	 * @throws IOException
	 * @throws ParseException
	 */
	 /* Main Function of the project*/
	public static void main(String [] args) throws IOException, ParseException
	{
		
		String maxtentTaggerPath = args[2];
		
		List<String> existingCategories;
		
		/* Customized Analyzer with stopword removed and stemming done */
		BaseAnalyzer xAnalyzer = new XAnalyzer();
		
		/* NGram analyzers with stop words removed: Bigram and Trigram used*/
		BaseAnalyzer biGramAnalyzer = new BAnalyzer(2, 2);
		BaseAnalyzer triGramAnalyzer = new BAnalyzer(3, 3);
		
		/* Path of StandardIndex,Bigram Index and TriGram index */
		String standardIndexDir = args[0] + File.separatorChar + "XAnalyzer";
		String biGramIndexDir = args[0] + File.separatorChar + "BiGramAnalyzer";
		String triGramIndexDir = args[0] + File.separatorChar + "TriGramAnalyzer";
		
		/* Index data is used to get collections from MongoDB */
		IndexData iData = new IndexData();
		iData.getCollections();
		
		/* Lucene Index the data based on requirement */
//		iData.indexBasedonBusiness(standardIndexDir, xAnalyzer);
//		iData.indexBasedonBusiness(biGramIndexDir, biGramAnalyzer);
//		iData.indexBasedonBusiness(triGramIndexDir, triGramAnalyzer);
		
		/* Get existing categories from existing Yelp data through MongoDB 
		   This is the ground truth data for categories when used for evaluation */ 
		existingCategories = iData.getCategories();
		existingCategories = removeDuplicate(existingCategories);
		
		
		/* For each business get top words from unigarm, Bigram and trigram */
		ArrayList<String> newCategories = new ArrayList<String>();
		HashMap<String, Float> topWords;
		HashMap<String, Float> biGrams,triGrams;
		Categories category = new Categories(args[1]);
		topWords = category.getTopWordsForEachBusinees(args[0],xAnalyzer,4);
		biGrams = category.getTopWordsForEachBusinees(args[0],biGramAnalyzer,4);
		triGrams = category.getTopWordsForEachBusinees(args[0],triGramAnalyzer,4);
		
//		System.out.println("size of TopWords Index:" +topWords.size());
//		System.out.println("size of Bigram Index:" +nGrams.size());
//		System.out.println("biGrams:" +nGrams);
		//System.out.println("topWords:" +topWords);
		//System.out.println("existingCategories:" +existingCategories);
		
		SentimentFinder sentimentFinder = new SentimentFinder();
		
		/* Extract category words from unigram, Bigram and Trigram */
		sentimentFinder.extractCategories(existingCategories, newCategories, topWords);
		sentimentFinder.extractCategories(existingCategories, newCategories, biGrams);
		sentimentFinder.extractCategories(existingCategories, newCategories, triGrams);
		
		/* Category words printing out for each business */
		System.out.println("\n Categories for this business are \n");
		Iterator<String> strIter = newCategories.iterator();
		while(strIter.hasNext())
		{
			System.out.println(strIter.next());
		}
		
		/* For Second Part : to know top 5 positives and negatives of each business
		 *  Get all review for that particular business
		 */
		String businessID = category.getBusinessID(4, args[0]);	
		HashMap<String, Integer> reviewRating = iData.getCompleteReview(businessID);
		
		ArrayList<String> positives = new ArrayList<String>();
		ArrayList<String> negatives = new ArrayList<String>();
		
		/* 
		 * Iniatialze NLP with sentiment analysis tool fo Stanford NLP
		 */
		NLP nlp = new NLP();
		nlp.init();
		
		/* Get Sentences and its ratings for each review containing Trigrams */ 
		sentimentFinder.computeSentiments(iData, triGrams, reviewRating, positives, negatives,nlp);
				
		ArrayList<String> positiveList = new ArrayList<String>();
		ArrayList<String> negativeList = new ArrayList<String>();
		
		/* Compute positives and negatives using all the ratings and the formula */ 
		sentimentFinder.findTopResults(maxtentTaggerPath, positives, nlp, positiveList);
		sentimentFinder.findTopResults(maxtentTaggerPath, negatives, nlp, negativeList);
		
		/* Print the positives and negatives */ 
		sentimentFinder.printTopResults("Positives",positiveList);		
		sentimentFinder.printTopResults("Negatives",negativeList);
	}
	
}
