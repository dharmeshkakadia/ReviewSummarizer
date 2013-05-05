import java.io.File;
import edu.smu.tspell.wordnet.*;

/**
 * Provides a simple interface to the JAWS Java WordNet package. 
 * 
 * */

public class WordNetJAWS
{
	private WordNetDatabase database = null;
	
	public WordNetJAWS (String wordNetDir) 
	{
		if (!new File(wordNetDir).exists()) {
			System.err.println("ERROR WordNetJAWS : WordNet directory does not exist! " + wordNetDir);
			System.exit(1);
		}
		System.setProperty("wordnet.database.dir",wordNetDir);
		this.database = WordNetDatabase.getFileInstance();
	}
	
	/** Returns true if the two words are synonymous; that is if they have at least one 
	 *  Synset in common. */
	public boolean areSynonyms (String word1, String word2)
	{
		/** As of now only synonymns are considered
		 *  This can be extended to hypernyms by using other similarity measures */
		word1 = word1.trim().toLowerCase();
		word2 = word2.trim().toLowerCase();

		for (Synset s1 : database.getSynsets(word1))
		{
			for (Synset s2 : database.getSynsets(word2))
			{
				if (s1 == s2)
					return true;
			}
		}
		return (word1.indexOf(word2)>-1) || (word2.indexOf(word1)>-1) || (word1.replaceAll(" ", "").indexOf(word2.replaceAll(" ", ""))>-1);
	}
}