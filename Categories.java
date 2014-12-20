import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;


/**
 * @author $uh@$
 * Helper Class to do Lucene indexing for all types of analyzers and get top words by TF-IDF approach
 */
class Categories
{
	/* Ap89corpus path set for external IDF calculation */
	private String _ap89Corpuspath; 
	
	Categories(String ap89CorpusPath)
	{
		_ap89Corpuspath = ap89CorpusPath;
	}
	
	/**
	 * @param businessNumber : This in terms of Lucene Indexed Number of business : 1,2,3,4,5,6............. 
	 * @param rootPath : Path for which kind of analyzer: Unigram, BiGram or Trigram
	 * @return : Business_iD of that Business( which was Lucene Indexed) : "KHoBFKiWf0pJntUzDQPkgw" Used to fetch review from MongoDB 
	 * @throws IOException
	 */
	public String getBusinessID(int businessNumber,String rootPath) throws IOException
	{
		/*
		 * Indexreader set to that particular Lucene path , Get field BIZID which was Indexed and return it 
		 */
		IndexReader indexReader = DirectoryReader.open(FSDirectory.open(new File(rootPath + File.separatorChar + "XAnalyzer")));
		Document doc = indexReader.document(businessNumber);
		String [] terms = doc.getValues("BIZID");
		String businessID = "";
		for(String t: terms)		
		{
			businessID+=t;
		}
		return businessID;
	}
	
	/**
	 * Helper Function to sort Map by values
	 * @param map : Contains words and their Scores
	 * @return : Same map sorted by their scores(values)
	 */
	private static HashMap sortByValues(HashMap map) 
	{ 
	       List list = new LinkedList(map.entrySet());
	       // Defined Custom Comparator here
	       Collections.sort(list, new Comparator() 
	       {
	            public int compare(Object o1, Object o2) {
	               return ((Comparable) ((Map.Entry) (o2)).getValue())
	                  .compareTo(((Map.Entry) (o1)).getValue());
	        }
	       });

	       HashMap sortedHashMap = new LinkedHashMap();
	       for (Iterator it = list.iterator(); it.hasNext();)
	       {
	              Map.Entry entry = (Map.Entry) it.next();
	              sortedHashMap.put(entry.getKey(), entry.getValue());
	       } 
	       return sortedHashMap;
	}
	
	/**
	 * Creating a map with all indexed words for  particular business fields; NAME:REVIEW,TIP with scores initialised to zero 
	 * @param names: Indexed words of business names
	 * @param review : Indexed words of business Reviews
	 * @param tip : Indexed words of Business Tip
	 * @param termScore : Map to put all the words and initialise it to zero
	 */
	private void createMap(List<String> names,List<String> review,List<String> tip,HashMap<String, Float> termScore)
	{
		Iterator<String> iterator = names.iterator();
		while(iterator.hasNext())
		{
			String term = iterator.next().toLowerCase();
			if(!termScore.containsKey(term))
			{
				termScore.put(term, (float) 0.0);
			}
		}
		
		iterator = review.iterator();
		while(iterator.hasNext())
		{
			String term = iterator.next().toLowerCase();
			if(!termScore.containsKey(term))
			{
				termScore.put(term, (float) 0.0);
			}
		}
		
		iterator = tip.iterator();
		while(iterator.hasNext())
		{
			String term = iterator.next().toLowerCase();
			if(!termScore.containsKey(term))
			{
				termScore.put(term, (float) 0.0);
			}
		}
		
	}
	
	/**
	 * This is to get all topwords based on analyzer: Unigram, Bigram and Trigram
	 * Acts as a facade to all functions in the class. basically entry point of class( from Yelp.java)
	 * @param rootPath : root path of Lucene index
	 * @param analyzer : Type analyzer used 
	 * @param businessNumber : Business Number based on Lucene Index: 1,2,3,4,5,6,7,8,9
	 * @return : Map of words to score(Sortred by values)
	 * @throws IOException
	 * @throws ParseException
	 */
	public HashMap<String, Float> getTopWordsForEachBusinees(String rootPath,BaseAnalyzer analyzer, int businessNumber) throws IOException, ParseException
	{
		List<String> names,review,tip,ID;
		HashMap<String, Float> termScore = new HashMap<String, Float>();
		
		String indexPath;
		if(analyzer.getType() == BaseAnalyzer.TYPE.CustomStandardAnalyzer )
		{
			indexPath = rootPath + File.separatorChar + "XAnalyzer";
		}
		else if(analyzer.getGram() == 2)
		{
			indexPath = rootPath + File.separatorChar + "BiGramAnalyzer";
		}
		else
		{
			indexPath = rootPath + File.separatorChar + "TriGramAnalyzer";
		}
		
		/*
		 * Get ID,REVIEW,TIP,NAME for that paricular Business
		 */
		ID = getWords(indexPath,businessNumber,"BIZID");
		review = getWords(indexPath,businessNumber,"BIZREVIEW");
		tip = getWords(indexPath,businessNumber,"BIZTIP");
		names = getWords(indexPath,businessNumber,"BIZNAME");
		
//		System.out.println(names);
//		System.out.println(review);
//		System.out.println(tip);
//		System.out.println(ID);
		
		/*
		 * Creates an map with scores zero,  Kind of dictionary
		 */
		createMap(names,review,tip,termScore);
		
		/*
		 * Calculate score for each entry on the map
		 */
		Iterator iterator = termScore.entrySet().iterator();
		while(iterator.hasNext())
		{
			Map.Entry pairs = (Map.Entry)iterator.next();
			//System.out.println(pairs.getKey());
			
			float score = getScore(pairs.getKey().toString().toLowerCase(),indexPath,businessNumber,analyzer);
			//System.out.println(score);
			pairs.setValue(score);
		}
		termScore = sortByValues(termScore);
		
//		iterator = termScore.entrySet().iterator();
//		while(iterator.hasNext())
//		{
//			Map.Entry pairs = (Map.Entry)iterator.next();
//			System.out.println(pairs.getKey()+"   "+pairs.getValue());
//		}
		
		return termScore;
	}
	
	/**
	 * To pull out words which are present in inverted index on Lucene index fields 
	 * @param indexpath : path of the index
	 * @param businessNumber: Lucene index Number
	 * @param field : REVIEW,TIP,NAME field used for indexing
	 * @return : collection of indexed words in that list
	 * @throws IOException
	 */
	private List<String> getWords(String indexpath, int businessNumber,String field) throws IOException
	{
		IndexReader indexReader = DirectoryReader.open(FSDirectory.open(new File(indexpath)));
		List<String> collectionStrings = new ArrayList<String>();
	
		Terms vocab = indexReader.getTermVector(businessNumber, field);
		if(vocab != null)
		{
			TermsEnum iterator = vocab.iterator(null);
			BytesRef byteRef = null;
			
			while((byteRef = iterator.next()) !=null) 
			{
				String term = byteRef.utf8ToString();
				collectionStrings.add(term);
			}
		}
		
		return collectionStrings;
	}
	
	/**
	 * Calculate IF-IDF score for word based on the index
	 * @param query : word to which TF-IDF score is calculated
	 * @param indexPath : path of index
	 * @param businessNumber : Lucene index number
	 * @param analyzer : type of analyzer used: For Lucene api's to pull out statistics
	 * @return : Score for each word:
	 * @throws IOException
	 * @throws ParseException
	 */
	private float getScore(String query, String indexPath,int businessNumber, Analyzer analyzer) throws IOException, ParseException
	{
		float score  = (float)0.0;
		float nameScore,reviewScore,tipScore;
	 	
		/*
		 * Calculate individual scores for BIZNAME, BIZREVIEW and BIZTIP and add the total
		 */
		nameScore = score(query,indexPath,"BIZNAME",businessNumber, analyzer);
		reviewScore = score(query,indexPath,"BIZREVIEW",businessNumber, analyzer);
		tipScore = score(query,indexPath,"BIZTIP",businessNumber, analyzer);
		
		score = (float) ((0.7 * reviewScore) + (0.2 * tipScore) + (0.1 * nameScore));
		//Tuning parameters
		//score = (float) ((0.2 * reviewScore) + (0.6 * tipScore) + (0.2 * nameScore));
		//score = (float) ((0.6 * reviewScore) + (0.25 * tipScore) + (0.15 * nameScore));
		//score = (float) ((0.8 * reviewScore) + (0.1 * tipScore) + (0.1 * nameScore));
		return score;
	}
	
	/**
	 * Calculate IDF from other neutral Index dataset(AP89corpus)
	 * @param term : word for which IDF needs to be calculated
	 * @return : IDF score
	 * @throws IOException
	 */
	public double idfFromOtherDataSet(String term) throws IOException 
	{
		IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(_ap89Corpuspath)));
		
		int freq;
		/*
		 * Note field indexed in that corpus and their analyzer type
		 */
		freq = reader.docFreq(new Term("TEXT", new BytesRef(term)));
		double idf = 0;
		if(freq != 0)	
			idf = Math.log10(1+(float)(reader.maxDoc()/freq));
		return idf;
		//idfMap.put(t.text(),freq);
		
	}
	
	/**
	 * Calculate TF*IDF score for word given and the field
	 * @param queryString : Word for which score needs to be calculated
	 * @param indexPath : path of the index:
	 * @param field : BIZNAME, BIZREVIEW and BIZTIP
	 * @param businessNumber ; For which business
	 * @param analyzer : type of analyzer used 
	 * @return : TF-IDF score
	 * @throws IOException
	 * @throws ParseException
	 */
	private float score(String queryString,String indexPath,String field,int businessNumber, Analyzer analyzer) throws IOException, ParseException
	{
		float score = (float)0.0;
		
		//HashMap<String, Integer> queryWordCount = new HashMap <String, Integer>();;
		IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
		//IndexSearcher searcher = new IndexSearcher(reader);
		
		int totalBusiness = reader.maxDoc();
		
		QueryParser parser = new QueryParser(field,analyzer); // checking in only field mentioned
		Query query = parser.parse(queryString.replace(" ", "+")); 
		Set<Term> queryTerms = new LinkedHashSet<Term>(); 
		
		query.extractTerms(queryTerms);
		DefaultSimilarity defaultSimilarity = new DefaultSimilarity();
		List<AtomicReaderContext> leafContexts = reader.getContext().reader().leaves();
		
		/*
		 * Get the TF for the term from that field  
		 */
		
		for(int leaf = 0; leaf < leafContexts.size() ; ++leaf )
		{
			AtomicReaderContext leafContext = leafContexts.get(leaf);
			int countWordinDoc = 0;
			int startDocNumber = leafContext.docBase;
			for (Term t : queryTerms)
			{
				//int len = t.toString().length() - t.toString().indexOf(':') - 1;
				//if(len == queryString.length())
				{
					DocsEnum docEnum = MultiFields.getTermDocsEnum(leafContext.reader(),MultiFields.getLiveDocs(leafContext.reader()),field, new BytesRef(t.text()));
					int doc;
					while (docEnum != null && (doc = docEnum.nextDoc()) != DocsEnum.NO_MORE_DOCS)
					{
						if(startDocNumber+docEnum.docID() == businessNumber)
						{
							countWordinDoc += docEnum.freq();
							//queryWordCount.put(startDocNumber+docEnum.docID()+t.text(), countWordinDoc);
							break;
						}
					}
				}
			}
			
			int numberOfDoc = leafContext.reader().maxDoc();
			for (int docId = startDocNumber; docId < startDocNumber + numberOfDoc; docId++)
			{	 
				if(docId == businessNumber)
				{
					float normDocLen = defaultSimilarity.decodeNormValue(leafContext.reader().getNormValues(field).get(docId - startDocNumber));
									
					for (Term t : queryTerms) 
					{
						int totalBusinessesContainingQueryTerm = -1;
						totalBusinessesContainingQueryTerm = reader.docFreq(new Term(field, t.text()));
//						int termsinDoc = 0;
//						if (queryWordCount.containsKey(docId+t.text())) 
//						{
//							termsinDoc = queryWordCount.get(docId+t.text());
//						}
						
						/*
						 * Normalise the TF
						 */
						float normTF = countWordinDoc/normDocLen;
						/*
						 * TF-IDF and IDF*
						 */
						double idf= 0;
						if (totalBusinessesContainingQueryTerm !=0)
						{ 
							if(field == "BIZREVIEW")
								idf = idfFromOtherDataSet(t.text());
							idf += Math.log10(1+(float)(totalBusiness/totalBusinessesContainingQueryTerm));
						}			
						score += (normTF * idf) ;	
					}
					return score;
				}
			}										
		}
		return score;
	}
}
