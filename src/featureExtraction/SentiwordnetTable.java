package featureExtraction;

import java.io.*;
import java.util.*;

public class SentiwordnetTable {

     // private Data members
     String input_string;
     String sentiWordNetFile;
     Hashtable<String, Word> wordList;
     BufferedReader br;

     // constructor
     public SentiwordnetTable(String sentiWordNetFilePath) {
         sentiWordNetFile = sentiWordNetFilePath;
         try {
             FileInputStream fs = new FileInputStream(sentiWordNetFile);
             DataInputStream in = new DataInputStream(fs);
             br = new BufferedReader(new InputStreamReader(in));
         } catch(Exception e) {
              System.out.println(e);
         }
         constructHashTable();
     }

     // Construct the hashmap
     public void constructHashTable() {
         // This method constructs the hash map by parsing the input file
         // Key for the hashmap would be the word itself along with the instance number
         // and corresponding scores would be created with the "Word" object
         try {
             wordList = new Hashtable<String, Word>();
             while((input_string = br.readLine())!= null && input_string.trim().length() != 0) {
                 try {
                     
		     	// split the string
                     	String[] tokens = input_string.split("\t");
                     	//DEBUG
                     	//System.out.println(input_string + tokens);               
                     	// sentiword net structure is as follows
                     	// token0 = type of word (Adj or noun)
                     	// token2 = pos score
                     	// token3 = neg score
                     	// token4 = word#instance number
		     
                     	String[] words = tokens[4].split(" ");
                     	for(int i = 0; i < words.length; i++) {
                        	String[] temp = words[i].split("#");
                         	Word word = new Word(temp[0],tokens[0].charAt(0), Float.valueOf(tokens[2]).floatValue(),Float.valueOf(tokens[3]).floatValue(), Integer.valueOf(temp[1]).intValue());
                         	wordList.put(words[i], word);
                     	}
		} catch (Exception e) {
			System.out.println(input_string);                         
			System.out.println(e);
                        System.out.println("Error in constructing the wordlist");
                        System.out.println("Check code here");
               	}
	    }
         } catch (Exception e) {
             System.out.println(e);
         }
     }

     public ArrayList<Word> getWord(String _word) {
         int i = 1;
         Word w;
         ArrayList<Word> returnList = new ArrayList<Word> ();
         do {
             String new_string = new String(_word + "#" + Integer.toString(i));
             w = wordList.get(new_string);
             if(w != null) {
                 returnList.add(w);
             }
             i++;
         } while ( w != null);

         return returnList;
     }
}