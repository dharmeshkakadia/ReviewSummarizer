//NOTE :: All classes yet to be serialized and ported to Map-Reduce

import java.util.regex.*;
import java.util.*;

/**
 * Feature class. Contains, Feature name and Rating associated
 * 
 * */

class Feature{
	String name;
	String rating;
}

/**
 * Review class. Contains, Product_Name, Review Text and Features_List extracted from each sentence
 * 
 * */

class Review{
	String productName;
	ArrayList<Feature> featureList = new ArrayList<Feature>();
	String reviewText = "";
}

/**
 * Review class. Contains, 
 * 
 * */

public class Pop{
	//TODO::Get the known features from Database.
	static ArrayList<String> known_features = new ArrayList<String>();
		
	public static void main(String [] args){
	try{
		WordNetJAWS w = new WordNetJAWS("/usr/local/WordNet-3.0/dict/");
		// Sample known features list populated
		known_features.add("bluetooth");
		known_features.add("display");
		//Get the product name
		String fileName = "mobile_name";
		//Sample input sentences of a review.
		String[] input = new String[2];
		input[0] = "image[+2],battery[-2]##Review sentence 1.";
		input[1] = "picture[+2],bluetooth[-2]##Review sentence 2.";
		//Creating a new review object
		Review rev = new Review();
		rev.productName = fileName;
		//Parsing the structured output from POS and saving in Review object.
		//Along with Synonymn check.
		for(String s : input){
			String[] sentence = s.split("##");
			Pattern pattern = Pattern.compile("((\\w+\\s?)*)\\[(.*?)\\]");
			Matcher match = pattern.matcher(sentence[0]);
			while(match.find()){
				Feature f1 = new Feature();
				f1.name = feature_match(match.group(1));
				f1.rating = match.group(3);
				rev.featureList.add(f1);
				//Testing
				//System.out.println("new feature added.");
			}
			rev.reviewText += sentence[1];
		}
		/** Testing
		System.out.println("Review text = " + rev.reviewText);
		System.out.println("Features = " + rev.featureList.size());
		for(Feature f2 : rev.featureList)
			System.out.println(f2.name + "\t" + f2.rating);
		System.out.println("Known Features...");
		for(String ss :known_features)
			System.out.println(ss);
		*/
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	/** Returns existing word if the two words are synonymous;
	 *  otherwise insertes the word in known_features list returns the same word. */
	static String feature_match(String feature){
		//Creating new Wornet DB instance
		WordNetJAWS w = new WordNetJAWS("/usr/local/WordNet-3.0/dict/");
		//checking if any synonymn match occures
		for(String s : known_features)
			if(w.areSynonyms(s,feature))
				return s;
		//updating the know features_list. 
		//TODO::Should be updated in Database.
		known_features.add(feature);
		return feature;
	}
}