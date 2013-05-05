package featureExtraction;
import java.io.*;
import java.util.*;
import java.lang.*;

// This class will store data about each word, associated adjectives, frequency etc as needed
// NOTE: Please modify as needed
class WordFrequency {
	
	private ArrayList<String> synonyms;
	private Hashtable<String, Integer> syn_frequency;
	private long frequency;
	private Hashtable<String, Integer> adjectives;
	private int maxAdjectiveFrequency;
		
	// ctor
	public WordFrequency() {
		frequency = 0;
		synonyms = new ArrayList<String> ();
		adjectives = new Hashtable<String, Integer> ();

		// per word stats
		maxAdjectiveFrequency = 0;
	}
	
	// public methods
	public void insert(String word, String adj, boolean duplicate) {
		// logic :
		// The inserted word has to be a synonym for the word already present with the datastructure
		if(!duplicate) 
			synonyms.add(word);
		frequency++;
		
		int count = 0;
		if(adjectives.containsKey(adj)) {
			// increment the count of the adjective for this word
			count = adjectives.get(adj).intValue();
			count++;
			adjectives.put(adj, new Integer(count));
		} else {
			adjectives.put(adj, new Integer(1));
		}
		
		// update per word stats
		if(count > maxAdjectiveFrequency) {
			maxAdjectiveFrequency = count;
		}
		
	}
	
	public ArrayList<String> getAllWords() { return synonyms; }
	public long getTotalFrequency() { return frequency; }
	public int getMaxAdjFrequency() { return maxAdjectiveFrequency; }
}

public class FeatureDB {
	
	// private data
	private long word_key;
	private Hashtable<String, Long> wordTable;
	private Hashtable<Long, WordFrequency> wordObjectTable;
		
	// ctor
	public FeatureDB() {
		
		word_key = 0;
		wordTable = new Hashtable<String, Long> ();
		wordObjectTable = new Hashtable<Long, WordFrequency> ();
	}

	// public methods
	public long checkSynonymExistsInWordTable(String word) throws IOException {
		// iterate through the hash table and check for synonym with the keys
		ArrayList<String> wordsInTable = new ArrayList<String>(wordTable.keySet());
		
		// WARNING : Enter at your own risk below !!! PERL below..!!!
		// **********************************************************
		// check for synonym
		ArrayList<Double> distance = new ArrayList<Double> ();
		Runtime runtime = Runtime.getRuntime();
		Process process;	
	
		for(String keys : wordsInTable) {
			String commandString = "perl similarity.pl --type WordNet::Similarity::vector -allsenses " + word + " " + keys;
			
			process = runtime.exec(commandString);
			try {
				process.waitFor();
			} catch (InterruptedException e) {
				System.out.println(e);
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String s = reader.readLine();
			
			// convert to DOUBLE
			Double d;
			try {
				d = Double.parseDouble(s);
				distance.add(d);
			} catch (NumberFormatException e) {
				distance.add(new Double(0));
			}
		}
				
		Double dmax = new Double(0);
		int max_index = 0;
		int index = 0;
		for(Double d : distance) {
			if(d.compareTo(dmax) > 0) {
				dmax = d;
				max_index = index;
			}
			index++;
		}
		
		// return index
		return dmax > 0.3 ? wordTable.get(wordsInTable.get(max_index)) : 0;
	
	}
	public void insert(String word, String adjective) throws IOException{
		/* logic :
			#1. First check for the actual key value of the word from "word hash table".
				if already exists, get the "WordFrequency" object for that "key" and update its contents appropriately.
			#2. Else enter new value in the "wordTable" hash table. Create a new WordKey object and put it in the "wordObjectTable" hashtable
		*/
	
		if(wordTable.containsKey(word)) {
			// get the key
			long actual_key = wordTable.get(word);
			WordFrequency obj = wordObjectTable.get(actual_key);
			
			// update stats for the object of the word
			obj.insert(word, adjective, true);
		} else {
			
			// check if synonym of this word is already present in the wordTable
			long synonym_key = 0;
			
			// NOTE : Below is a skeleton method "checkSynonymExistsInWordTable" that has to be done
			// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!1
			if(((synonym_key = checkSynonymExistsInWordTable(word)) != 0)) {
				// insert into wordobjecttable with this synonym key
				wordTable.put(word, synonym_key);
				WordFrequency obj = wordObjectTable.get(synonym_key);
				obj.insert(word, adjective, false);
			} else {
				// else only add it
				word_key++;
				wordTable.put(word, word_key);
				WordFrequency obj = new WordFrequency();
				obj.insert(word, adjective, false);
				wordObjectTable.put(word_key, obj);	
			}
		}
	}
	
}
	
		
