import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.logging.RedwoodConfiguration;



/** To set up Stanford NLP package and few other helper functions
 * @author $uh@$
 *
 */
public class NLP 
{
	private StanfordCoreNLP _pipeline;
	
	/**
	 * Properties set of Stanford NLP
	 * @return : properties
	 */
	private Properties createPipelieProperties() 
    {
       Properties props = new Properties();
       props.setProperty("annotators", "tokenize, ssplit, parse, sentiment");
       
       return props;
    }
	
	
	/**
	 * initialise StanfordNLP with the properties 
	 */
	public void init() 
    {
       Properties props = createPipelieProperties();
       _pipeline = new StanfordCoreNLP(props);
    }

     /**
     * @param line: Sentence for which sentiment needs to be calculated
     * Scale: 0-Very Negative 1-Mostly negative 2-Neutral 3-mostly positive 4-Highly positive  
     * @return : Sentiment Score
     */
    public int findSentiment(String line) 
     {
        Annotation annotation = _pipeline.process(line);
        int mainSentiment = findMainSentiment(annotation);
        return mainSentiment;
     }


     /**
      * gets sentiment of the largest sentence.
     * @param annotation
     * @return
     */
    private int findMainSentiment(Annotation annotation) 
     {

        int mainSentiment = Integer.MIN_VALUE;
        int longest = Integer.MIN_VALUE;

        for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) 
        {
           int sentenceLength = String.valueOf(sentence).length();
           if(sentenceLength > longest) 
           {
             Tree tree = sentence.get(SentimentCoreAnnotations.AnnotatedTree.class);
             mainSentiment = RNNCoreAnnotations.getPredictedClass(tree);
             longest = sentenceLength ;
            }
        }
        return mainSentiment;
     }
     
     /**
      * to extract nouns from the sentence 
     * @param words: Sentence for which nouns extraction to be done
     * @param maxentTaggerPath : maxtentTaggerPath(stanford-postagger-2014-01-04\models\english-left3words-distsim.tagger)
     * @return : List of nouns in the sentence
     */
    public List<String> getNouns(String words, String maxentTaggerPath)
     {
		MaxentTagger tagger = new MaxentTagger(maxentTaggerPath);
		List<String> nounList = new ArrayList<String>();
		String []wordList = words.split(" ");
	    List<HasWord> sent = Sentence.toWordList(wordList);   //("The", "slimy", "slug", "crawled", "over", "the", "long", ",", "green", "grass", ".");
	    List<TaggedWord> taggedSent = tagger.tagSentence(sent);
	    for (TaggedWord tw : taggedSent) {
	    	if( "NN".equalsIgnoreCase( tw.tag() ) ) {
	    		//System.out.println(tw.word());
	    		nounList.add(tw.word());
	      }
	    }
	    return nounList;
 	}
 }