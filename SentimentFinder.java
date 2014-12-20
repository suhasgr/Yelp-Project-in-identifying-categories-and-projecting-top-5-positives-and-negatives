import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.tartarus.snowball.ext.PorterStemmer;

/**
 * Category and Sentiment Finder helper functions 
 * @author $uh@$
 *
 */
public class SentimentFinder 
{
	
	/**
	 * To find stem of a word using porter stemmer 
	 * @param term : word which needs to be stemmed
	 * @return: Stemmed word
	 */
	private String stemTerm(String term) 
	{
		PorterStemmer stem = new PorterStemmer();
		stem.setCurrent(term);
		stem.stem(); 
		return stem.getCurrent();
	}
	
	
	/**
	 * Process the word with existing categories. if matches then it is updated in new Category
	 * @param category : can this word become category word
	 * @param existingCategories : List of existing categories  from yelp
	 * @param newCategories : The category list of that business generated form category
	 */
	private void CategoryCheck(String category, List<String> existingCategories,ArrayList<String> newCategories)
	{
		String[] categoryWords = category.split(" ");
		for(String existCat: existingCategories){
			String tempCategory = "";
			Boolean canBeAdded = false;
			for(String splitExistCat: existCat.split(" ")){
				String stemmedExistingCategory = stemTerm(splitExistCat.toLowerCase());
				 for(String splitCategory: categoryWords){
					int count =0; 
					String stemmedCategory = stemTerm(splitCategory);
					if(stemmedExistingCategory.equals(stemmedCategory))
					{
						canBeAdded = true;
						//if(!tempCategory.matches(splitCategory))
							tempCategory = splitCategory;
						
					}
					//else
						//canBeAdded = false;
					
				}
				if(canBeAdded && !newCategories.contains(tempCategory))
				{
					newCategories.add(tempCategory);
					//System.out.println("Added to category word: " +tempCategory);
				}
			}
			
		}	
	}
	
	/**
	 * Process the word with existing categories. if matches then it is updated in new Category
	 * @param category : can this word become category word
	 * @param existingCategories : List of existing categories  from yelp
	 * @param newCategories : The category list of that business generated form category
	 */

	private void CategoryCheckOld(String category, ArrayList<String> existingCategories,ArrayList<String> newCategories)
	{
		String[] categoryWords = category.split(" ");
		int categoryLength = categoryWords.length;
		
		for(String cat: existingCategories)
		{
			int tempCategoryLength = categoryLength;
			if(cat.split(" ").length == tempCategoryLength)
			{
				String newCat = stemTerm(cat.toLowerCase());
				Boolean categoryWord = true;
				for(String word:categoryWords)
				{
					word = stemTerm(word);
					if(word.length() > 3)
					{
						if(!( newCat.matches(word+" (.*)") || newCat.matches("(.*) "+word+" (.*)") || newCat.matches("(.*) "+word) || newCat.matches(word) ))
						{
							categoryWord = false;
							break;
						}
					}
					else if((--tempCategoryLength) == 0)
					{
						categoryWord = false;
						break;
					}
				}
				if(categoryWord && !(newCategories.contains(cat)))
				{
					newCategories.add(cat);
				}
			}
		}	
	}
	
	
	/**
	 * To find top 5 positives and negatives form array of trigrams 
	 * @param maxtentTaggerPath : path of maxtagger
	 * @param sentimentTypeList : Array of trigrams
	 * @param nlp : NLP object to call for sentiment analysis and noun extraction
	 * @param resultWordlist : positive/negative trigram words added 
	 * @return : resultWordlistis an Out parameter
	 */
	public void findTopResults(String maxtentTaggerPath,
			ArrayList<String> sentimentTypeList, NLP nlp,ArrayList<String> resultWordList ) {
		
		int size = sentimentTypeList.size();
		//ArrayList<String> resultWordList = new ArrayList<String>();;
		for(int i = 0,j = 0 ; j< 5 && i< size; ++i)
		{
			List<String> nounList = nlp.getNouns(sentimentTypeList.get(i),maxtentTaggerPath);
			String completeWords="";
			//Creating a single String of multiple nouns
			Boolean toBeAdded = true;
			
			for (String word : nounList) {
				if (checkDuplicacy(resultWordList, word)) {
					toBeAdded = false;
					break;
				}
				completeWords += word + " ";
			}
			if (toBeAdded && completeWords != "") {
				resultWordList.add(completeWords);
				++j;
			}
		}
		
	}
	
	
	/**
	 * Extract category words from topwords extracted through TF-IDF(Considered top 20)
	 * @param existingCategories : Existing categories list : taken from yelp
	 * @param newCategories : Out Parameter Containing new categories
	 * @param topWords : topwords got from TF-IDF approach
	 */
	public void extractCategories(List<String> existingCategories,
			ArrayList<String> newCategories, HashMap<String, Float> topWords) {
		Iterator iterator = topWords.entrySet().iterator();
		int count = 0;
		while(iterator.hasNext())
		{
			Map.Entry pairs = (Map.Entry)iterator.next();
			//System.out.println(pairs.getKey() + " "+ pairs.getValue());
			CategoryCheck((String)pairs.getKey(),existingCategories,newCategories);			
			if(++count == 20)
				break;
		}
	}
	
	/**
	 * Find the sentiment of each sentences for all trigrams and calculate whether it is passing threshold and add it is postive/negetive based on that  
	 * @param iData : object of IndexData for helper functions 
	 * @param triGrams : All the trigrams sorted based on values( Descending order)
	 * @param reviewRating : review-rating map of business reviews and trigrams
	 * @param positives : Outpameter containing all positive trigrams passing threshold  
	 * @param negatives : outparameter containing all negetive trigrams passing threshold
	 * @param nlp : NLP object for nlp calling functions
	 */
	public void computeSentiments(IndexData iData,
			HashMap<String, Float> triGrams,
			HashMap<String, Integer> reviewRating, ArrayList<String> positives,
			ArrayList<String> negatives, NLP nlp) {
		Iterator iterator = triGrams.entrySet().iterator();
		Integer count1 = 0;
		
		/*
		 * For each top Trigram 
		 */
		while(iterator.hasNext())
		{
			Map.Entry pairs1 = (Map.Entry)iterator.next();
			/*
			 * Get sentences from the reviews which contain that trigram
			 */
			HashMap<String, Integer> sentencesContainingSubstringWithRating = new HashMap<String, Integer>();
			iData.getSentence(reviewRating, (String)pairs1.getKey(), sentencesContainingSubstringWithRating);
			
			/*
			 * For each such sentences check +ve/-ve from NLP
			 * correct the NLp sentiment score to common scale
			 * Take the review Stars(rating) into consideration as well
			 * Calculate cumulative +ve score and -ve score based on sentiment  
			 */
			Iterator ratingStart = sentencesContainingSubstringWithRating.entrySet().iterator();
			double positiveOpinion = 0.0,negetiveOpinion = 0.0;
			int posi = 0, nege = 0;
			while(ratingStart.hasNext())
			{
				Map.Entry reviewRate = (Map.Entry)ratingStart.next();
				
				int sentiment = nlp.findSentiment((String)reviewRate.getKey());
				
				if(sentiment > 2 && sentiment <=4)              //positive
				{	
					positiveOpinion += ((sentiment - 2) * (Integer)reviewRate.getValue());
					posi++;
				}
				else if(sentiment < 2 && sentiment >=0)
				{
					if(sentiment  == 0)
						sentiment = 2;
					negetiveOpinion+= (sentiment * (Integer)reviewRate.getValue());
					nege++;
				}
			}
			
			/*
			 * Normalize the +ve and -ve scores
			 */
			
			if(posi != 0)
				positiveOpinion/=posi;
			if(nege != 0)
				negetiveOpinion/=nege;

			/*
			 * Passes threshold then put into +ve or -ve category 
			 */
			if(positiveOpinion > negetiveOpinion)
			{
				if(positiveOpinion >= 4)
				{
					positives.add((String)pairs1.getKey());
				}
			}
			else
			{
				if(negetiveOpinion >= 4)
				{
					negatives.add((String)pairs1.getKey());
				}
			}
			
			/*
			 * Only top 100 trigrams considered
			 */
			if(++count1 == 100)
				break;
		}
	}

	/**
	 * Printing helper for +ve and -ve words
	 * @param msg: +ve/-ve
	 * @param sentimentType : List of =Ve/-ve trigrams
	 */
	public void printTopResults(String msg,	ArrayList<String> sentimentType) 
	{
		System.out.println("---------------\n"+msg+"\n---------------\n");
		Iterator<String> word = sentimentType.iterator();
		while(word.hasNext())
		{
			System.out.println(word.next().toString());
		}
		
	}

	 /**
	  * Helper to remove duplicates from the list
	 * @param list : The string list
	 * @return : list in which duplicates are removed
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
	 * To check whether the word is present in array of strings 
	 * @param array : Array of strings
	 * @param word : word for which presence is checked
	 * @return : True/False based on presence /absence of word in array.
	 */
	private Boolean checkDuplicacy(ArrayList<String> array, String word) {
		Boolean duplicate = false;
		for (String t : array) {
			if (t.matches("(.*)" + word + "(.*)")) {
				duplicate = true;
				break;
			}
		}
		return duplicate;
	
	}
}
	