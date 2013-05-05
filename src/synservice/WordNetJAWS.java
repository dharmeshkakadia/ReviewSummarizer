package synservice;

import java.io.File;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Mapper.Context;

import edu.smu.tspell.wordnet.*;

/**
 * Provides a simple interface to the JAWS Java WordNet package. 
 * 
 * */

public class WordNetJAWS
{
	private WordNetDatabase database = null;
	
	public WordNetJAWS (String wordNetDir)  
	{/*
		FileSystem dfs = FileSystem.get(cntx.getConfiguration());
		Path src = new Path(dfs.getWorkingDirectory() + wordNetDir);
		FSDataInputStream reader = dfs.open(src);
		
		if (!new File(wordNetDir).exists()) {
			System.err.println("ERROR WordNetJAWS : WordNet directory does not exist! " + wordNetDir);
			System.exit(1);
		}*/
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
	       /*return word1.equals(word2);*/
	       String[] word1s = word1.split(" ");
	       String[] word2s = word2.split(" ");

	       if(word1.contains(" ")){
	           if(word2.contains(" ")){
	               //both are double words
	               if( (word1s[0].equals(word2s[0])) || (word1s[0]+"s").equals(word2s[0]) || (word2s[0]+"s").equals(word1s[0]))
	                   return areSynonymsHelper(word1s[1],word2s[1]);
	               if( (word1s[1].equals(word2s[1])) || (word1s[1]+"s").equals(word2s[1]) || (word2s[1]+"s").equals(word1s[1]) )
	                   return areSynonymsHelper(word1s[0],word2s[0]);
	               return areSynonymsHelper(word1s[1],word2s[1]) && areSynonymsHelper(word1s[0],word2s[0]);
	           }
	           else{
	               //word2 is only double
	               if( (word2.equals(word1s[0])) || (word2+"s").equals(word1s[0]) || (word1s[0]+"s").equals(word2))
	                   return true;
	               if( (word2.equals(word1s[1])) || (word2+"s").equals(word1s[1]) || (word1s[1]+"s").equals(word2) )
	                   return true;
	               return (areSynonymsHelper(word2,word1s[0]) || areSynonymsHelper(word2,word1s[1]));
	           }
	       }
	       else{
	           if(word2.contains(" ")){
	               //word1 is only double
	               if( (word1.equals(word2s[0])) || (word1+"s").equals(word2s[0]) || (word2s[0]+"s").equals(word1) )
	                   return true;
	               if( (word1.equals(word2s[1])) || (word1+"s").equals(word2s[1]) || (word2s[1]+"s").equals(word1) )
	                   return true;
	               return (areSynonymsHelper(word1,word2s[0]) || areSynonymsHelper(word1,word2s[1]));
	           }
	           else{
	               //no word is double
	               return areSynonymsHelper(word1,word2);
	           }
	       }
	   }

	
	   public boolean areSynonymsHelper(String word1, String word2){
	       for (Synset s1 : database.getSynsets(word1))
	       {
	           for (Synset s2 : database.getSynsets(word2))
	           {
	               if (s1 == s2)
	                   return true;
	           }
	       }
	       return false;
	   }
}