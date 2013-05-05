/*
This class implements a trainer that we intend to train a classifier to identify possible features of a product in a file.

*/

package featureExtraction;

//package trainer;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import scema.Feature;
import scema.ProductReview;

// stanford parser required import files
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.process.*;
import edu.stanford.nlp.objectbank.TokenizerFactory;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.ling.*;

// import mongodb implementation class
import db.*;

public class FeatureTester {
	
	Collection<TypedDependency> dependencyTree;
	ArrayList<TaggedWord> taggedTokens;
	//static FeatureDB DB;
	Search DB;				// MONGO DB .. static now.. errors being throw.. resolve them.. TODO
	SentiwordnetTable scoreTable;
		
	// public methods
	public FeatureTester() {
		DB = new Search();
		scoreTable=new SentiwordnetTable("/home/hadoop/WordList.txt");
	}

	// This method returns the typed dependency for any word in the whole typed dependency tree
	private TypedDependency checkRelationExists(String word, String relation, boolean governor) {
		//Noun is assumed to be with position data i.e : noun-#posvalue
		for(TypedDependency t : dependencyTree) {
			if(t.reln().toString().equals(relation)) {
				// check for noun now
				if(!governor) {
					// check the dependency
					if(t.dep().toString().equals(word))
						return t;
				}
				else {
					if(t.gov().toString().equals(word))
						return t;
				}
			}
		}
		return null;
	}
	
	// This method returns all the conjunctions
	private ArrayList<TypedDependency> conjunctions(String word) {
		//Noun is assumed to be with position data i.e : noun-#posvalue
		ArrayList<TypedDependency> conjs = new ArrayList<TypedDependency>();
		for(TypedDependency t : dependencyTree) {
			if(t.reln().toString().equals("conj")) {
					if(t.gov().toString().equals(word))
						conjs.add(t);
			}
		}
		return conjs;
	}
	
	// This method returns all the nns
	private TypedDependency nns(String word) {
		//Noun is assumed to be with position data i.e : noun-#posvalue
		TypedDependency ret = null;
		for(TypedDependency t : dependencyTree) {
			if(t.reln().toString().equals("nn") || t.reln().toString().equals("amod") || t.reln().toString().equals("advmod")) {
					if(t.gov().toString().equals(word))
						ret = t;
			}
		}
		return ret;
	}
	
	// This method returns all the neg
	private boolean checkNeg(ArrayList<String> adjs) {
		if(adjs.isEmpty()) return false;
		//Adj is assumed to be with position data i.e : adj-#posvalue
		for(String words : adjs){
			for(String word : words.split(" ")){
				for(TypedDependency t : dependencyTree) {
					if(t.reln().toString().equals("neg")) {
							if(t.gov().toString().equals(word))
								return true;
					}
				}
			}
		}
		return false;
	}
	
	// This method iterates through the parts of speech string and checks if a word is actual part of speech
	private boolean checkWordPOS(String word, String postag) {
		// iterate through the tagged tokens and find if the word is the actual parts of speech
		for(TaggedWord t : taggedTokens) {
			if(t.value().equals(word) && t.tag().contains(postag))
				return true;
		}
		return false;
	}

	// This method returns a string without the position information
	private String getWordSansPosition(String word) {
		int index = word.indexOf('-');
		// check if the string from next character till word length is a number and if so get the first string
		try {
			int expected_pos = Integer.parseInt(word.substring(index));
			return word.substring(0, index);
			
		} catch (NumberFormatException e) {
			// else check again for the next '-' occurence
		}
		return null;
		
	}
	
	public ProductReview test(String fileName, String sentence,LexicalizedParser lp,TokenizerFactory tf,GrammaticalStructureFactory gsf) {
		// Step 1:
		GrammaticalStructure gs;
		ProductReview rev = new ProductReview();
		rev.setName(fileName);
		rev.setReview(sentence);
		sentence=sentence.toLowerCase();
		
		try {
			// Step 2:
			// parse all tokens first
			List tokens = tf.getTokenizer(new StringReader(sentence)).tokenize();
			lp.parse(tokens);
			Tree t = lp.getBestParse();
			taggedTokens = t.taggedYield();
			ArrayList<String> nounsPerLine = new ArrayList<String> ();
			ArrayList<String> nounsPerLineWithPosition = new ArrayList<String> ();
			int pos = 1;
			
			// Step 3:
			// Collect all nouns
			for(TaggedWord w : taggedTokens) {
				if(w.tag().contains("NN")  && w.value().length()>1) {
					nounsPerLine.add(w.value());
					nounsPerLineWithPosition.add(w.value() + "-" + pos);
				}
				pos++;
			}
					
			//Step 4:
			//Ask the trained classifier if it is a feature or not; store valid features
			gs = gsf.newGrammaticalStructure(t);
			dependencyTree = gs.typedDependencies();
			ArrayList<String> validNouns = nounsPerLineWithPosition;

			// Step 5:
			// Get modifiers for corresponding nouns
			HashSet<String> checkedNouns = new HashSet<String>();
			for(String noun : validNouns) {
			if(!checkedNouns.contains(noun)){
				boolean amod = false;
				// COMPLEX LOGIC : !!!!!
				/************************ENTER AT OWN RISK**************************
				*/
		
				// Step #1 : Check if nsubj is available . If not proceed down
				TypedDependency tdy;
				ArrayList<String> features = new ArrayList<String> ();
				ArrayList<String> modifiers = new ArrayList<String> ();
				boolean isNeg = false;
				String feature = null;
				String modifier = null;
				if((tdy = checkRelationExists(noun, "nsubj", false)) != null) {
					TreeGraphNode governor = tdy.gov();
					// check if governor is an adjective - from the taggedtokens list
					if(checkWordPOS(governor.value(), "JJ")) {
						// check for amod relation with this noun
						TypedDependency intermediate = checkRelationExists(noun, "amod", true);
						feature = tdy.dep().toString();
						modifier = tdy.gov().toString();
					
					} else {
						// check if verb 
						if(checkWordPOS(governor.value(), "VB")) {
						feature = tdy.dep().toString();
						modifier = tdy.gov().toString();
							// check if "acomp" relation exists for this governor
							TypedDependency temp = checkRelationExists(governor.toString(), "acomp", true);
							if(temp != null && (checkWordPOS(temp.dep().value(), "JJ"))) {
								feature = temp.gov().toString();
								modifier = temp.dep().toString();
							} else {
								temp = checkRelationExists(governor.toString(), "dobj", true);
								if(temp != null && (checkWordPOS(temp.dep().value(), "NN"))) {
									// check if dependency is a noun
									TypedDependency temp2 = checkRelationExists(temp.dep().toString(), "amod", true);
									if(temp2 != null && (checkWordPOS(temp2.dep().value(), "JJ"))) {
										feature = feature + " " +temp2.gov().toString();
										modifier = temp2.dep().toString();
									}
								}
							}
						}
					}
				}
				else {
					// 3rd segment
					// Check for if "nsubjpass" exists with noun
					tdy = checkRelationExists(noun, "nsubjpass", false);
					if(tdy != null && (checkWordPOS(tdy.gov().value(), "VB"))) {
						feature = tdy.dep().toString();
						modifier = tdy.gov().toString();
					} else {
						tdy = checkRelationExists(noun, "dobj", false);
						if(tdy != null && (checkWordPOS(tdy.gov().value(), "JJ"))) {
							feature = tdy.dep().toString();
							modifier = tdy.gov().toString();
						}
						else{
							tdy = checkRelationExists(noun, "amod", true);
							if(tdy != null && (checkWordPOS(tdy.dep().value(), "JJ") || checkWordPOS(tdy.dep().value(), "VB"))) {
								amod = true;
								feature = tdy.gov().toString();
								modifier = tdy.dep().toString();
							}
						}
					}
				}

				//CONJ and NN code
				// This segment is to identify nouns connected using a conjunction modifier.
				if(feature !=null){
					TypedDependency nn_temp = null;
					nn_temp = nns(noun);
					if(nn_temp != null && (!amod || nn_temp.reln().toString().equals("nn"))) {
						//features.add(nn_temp.dep().toString());
						if(!nn_temp.dep().toString().equals(feature))
							features.add(nn_temp.dep().toString() + " " + feature);
					}
					else features.add(feature);

					for(TypedDependency tConj : conjunctions(noun)){
						nn_temp = null;
						nn_temp = nns(tConj.dep().toString());
						if(nn_temp != null) {
							//features.add(nn_temp.dep().toString());
							features.add(nn_temp.dep().toString() + " " + tConj.dep().toString());
						}
						else features.add(tConj.dep().toString());
					}
						
					// This segment is to identify the adjectives that modify the nouns
					nn_temp = nns(modifier);
					if(nn_temp != null) {
						modifiers.add(nn_temp.dep().toString() + " " + nn_temp.gov().toString());
					}
					else modifiers.add(modifier);
					
					for(TypedDependency tConj : conjunctions(modifier)){
						nn_temp = null;
						nn_temp = nns(tConj.dep().toString());
						if(nn_temp != null) {
							modifiers.add(nn_temp.dep().toString() + " " + nn_temp.gov().toString());
						}
						else modifiers.add(tConj.dep().toString());
					}
				}
				
				//Hashing all the found features (with modifiers)
				for(String foundFeature : features){
					String[] foundFeatures = foundFeature.split(" ");
					checkedNouns.add(foundFeatures[0]);
					if (foundFeatures.length == 2) checkedNouns.add(foundFeatures[1]);
				}
				
				//Removing position in modifiers
				ArrayList<String> modNotPosition = new ArrayList<String>();
				for(String s : modifiers)
					modNotPosition.add(s.replaceAll("-\\d*", ""));
				
				//check for -ve ness
				isNeg = checkNeg(modifiers);
				
				//Found some features with adjectives
				if(feature!=null){
/*					double score = 3; //NOTE:: GET SCORE WITH modNotPosition and isNeg
					for(String outputFeature : features)
						if(validFeature(outputFeature.replaceAll("-\\d*", ""))){
							//System.out.print( "[" + outputFeature.replaceAll("-\\d*", "") + "-" + score + "]");
							Feature feature_obj = new Feature();
							feature_obj.setName(outputFeature.replaceAll("-\\d*", ""));
							feature_obj.setRating(new Double(score));
							rev.getFeatureList().add(feature_obj);
						}*/
					int score = 0; //NOTE:: GET SCORE WITH modNotPosition and isNeg
					for(String outputFeature : features){
						String newFeature = null;
						if((newFeature = validFeature(outputFeature.replaceAll("-\\d*", ""))) != null){
							//System.out.println("Yes we have a valid feature : " + newFeature + " and list of adjectives for this is : " + modNotPosition); 							
							score = getFinalScore(newFeature, modNotPosition, isNeg);
							//System.out.print( "[" + newFeature + "- " + score + "]");
							Feature feature_obj = new Feature();
							feature_obj.setName(newFeature);
							feature_obj.setRating(new Double(score));
							rev.getFeatureList().add(feature_obj);
						}
					}
				}
				else{
					//No adjective Wat to do?
					//System.out.println("Found no modifier for " + noun.replaceAll("-\\d*", ""));
				}
			}
			}
			for(String noun : validNouns) {
				String newFeature = null;
				if(!checkedNouns.contains(noun)){
					TypedDependency nn_temp = null;
					nn_temp = nns(noun);
					if(nn_temp != null && (nn_temp.reln().toString().equals("nn"))) {
						String depen = nn_temp.dep().toString();
						String gover = nn_temp.gov().toString();
						//DB.insert(depen.replaceAll("-\\d*", ""),null);
						int score = 0;
						if((newFeature = validFeature(depen.replaceAll("-\\d*", ""))) != null){
							score += getFinalScore(depen, null, false);
							score += getFinalScore(gover, null, false);						
							//System.out.print("[" + depen.replaceAll("-\\d*", "") + " " + gover.replaceAll("-\\d*", "") + "- NoScore]");
							Feature feature_obj = new Feature();
							feature_obj.setName(newFeature + " " + gover.replaceAll("-\\d*", ""));
							feature_obj.setRating(new Double(0));
							rev.getFeatureList().add(feature_obj);
						}
						checkedNouns.add(depen);
						checkedNouns.add(gover);
					}
					else if((newFeature = validFeature(noun.replaceAll("-\\d*", ""))) != null) {
						int score = 0;
						score += getFinalScore(noun, null, false);
						//System.out.print("[" + noun.replaceAll("-\\d*", "") + "- NoScore]");
						Feature feature_obj = new Feature();
						feature_obj.setName(newFeature);
						//feature_obj.setRating(new Double(0));
						feature_obj.setRating(score);
						rev.getFeatureList().add(feature_obj);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(rev.getFeatureList().size() > 0)
			return rev;
		return null;
	}
/*
	public boolean validFeature(String noun) throws IOException{
		int FEATURE_THRESHOLD = 1;
		int ADJ_THRESHOLD = 1;
		
		// MONGODB CODE BELOW
		if(DB.getTotalNounCount(noun) > FEATURE_THRESHOLD && DB.getAdjectiveCount(noun) > ADJ_THRESHOLD)
			return true;
		if(DB.checkSynonymExistsInWordTable(noun)){									//yes it is a synonymn
			DB.insertNounAdj(noun, null);
			if(DB.getTotalNounCount(noun) > FEATURE_THRESHOLD && DB.getAdjectiveCount(noun) > ADJ_THRESHOLD) return true;
		}
		return false;
	}
	*/
	
	public String validFeature(String noun) throws IOException{
		System.err.println(noun);
		int FEATURE_THRESHOLD = 4;
		int ADJ_THRESHOLD = 4;
		//if the feature has two words
		noun = noun.replaceAll("-\\d*", "");
		String[] nouns = noun.split(" ");
		noun = nouns[0];

		// MONGODB CODE BELOW checking for first word
		if(DB.getNounCount(noun) > FEATURE_THRESHOLD && DB.getAdjectiveCount(noun) > ADJ_THRESHOLD)
			return noun;
		if(DB.checkSynonymExistsInWordTable(noun)){									//yes it is a synonymn
			DB.insertNounAdj(noun, null);
			if(DB.getNounCount(noun) > FEATURE_THRESHOLD && DB.getAdjectiveCount(noun) > ADJ_THRESHOLD) return noun;
		}

		// MONGODB CODE BELOW checking for Second word
		if(nouns.length >1){
			if(DB.getNounCount(nouns[1]) > FEATURE_THRESHOLD && DB.getAdjectiveCount(nouns[1]) > ADJ_THRESHOLD)
				return nouns[1];
			if(DB.checkSynonymExistsInWordTable(nouns[1])){									//yes it is a synonymn
				//DB.insertNounAdj(nouns[1], null);
				if(DB.getNounCount(nouns[1]) > FEATURE_THRESHOLD && DB.getAdjectiveCount(nouns[1]) > ADJ_THRESHOLD) return nouns[1];
			}
		}
		return null;
	}

	
	public int getFinalScore(String features, ArrayList<String> adjectives, boolean isNeg) {
		
		// DEBUG
		//System.out.println("Score for :" + features);
		//System.out.println("Adjectives for :" + adjectives);		
					
		// Scoring Scheme
		// these scores are only for the nouns themselves
		float npositiveScore = 0.0f;
		float nnegativeScore = 0.0f;
		float nobjectiveScore = 0.0f;
		float ntotalScore = 0.0f;
		float navgScore = 0.0f;
		int ntotalItems = 0;
		
		// these scores are for the adjectives
		float apositiveScore = 0.0f;
		float anegativeScore = 0.0f;
		float aobjectiveScore = 0.0f;
		float atotalScore = 0.0f;
		float aavgScore = 0.0f;
		int atotalItems = 0;
		
		// contact sentiwordnet and get word list
		String f = features.replaceAll("-\\d*", "");	
		ArrayList<Word> words_for_feature = scoreTable.getWord(f);
		if(words_for_feature != null) {
			for(Word w : words_for_feature) {
				npositiveScore += w.getPositiveScore();
				nnegativeScore += w.getNegativeScore();
				ntotalScore += npositiveScore - nnegativeScore;						
			}
		}
		ntotalItems = words_for_feature.size();
		if(ntotalItems != 0) {
			// to overcome divide by zero error man
			navgScore = ntotalScore / ntotalItems;
		}
		
		// get the score for adjectives
		// split the words for multiple words in array
		if(adjectives != null) {						
			for(String adj : adjectives) {
				String individual_adjectives[] = adj.split(" ");
				// DEBUG
				//for(int i = 0; i < individual_adjectives.length; i++) {
				//	System.out.println("Getting score for : " + individual_adjectives[i]);
				//}
				// end of debug
				for(int i = 0; i < individual_adjectives.length; i++) {
					ArrayList<Word> words_for_adjectives = scoreTable.getWord(adj);
					//System.out.println(words_for_adjectives);					
					if(words_for_adjectives != null) {
						for(Word w : words_for_adjectives) {
							apositiveScore += w.getPositiveScore();
							anegativeScore += w.getNegativeScore();
							atotalScore += apositiveScore - anegativeScore;
							//System.out.println("Adj is : " + w.getWord() + " pos score is : " + w.getPositiveScore() + " neg score is : " + w.getNegativeScore());
						}
					}
					atotalItems += words_for_adjectives.size();
				}
			}
					
			// get total items
			//atotalItems = words_for_adjectives.size();
			if(atotalItems != 0) {
				aavgScore = atotalScore / atotalItems;
				//aavgScore = atotalScore;
			}
		}
		
			
		// boosting factor is the adjective. Hence we multiply.
		// check if there is no adjective then we return only the noun score
		// else we multiply both and send it back
		float finalScore = 0.0f;
		if(aavgScore != 0.0f) {
			if(navgScore != 0.0f) {
				finalScore = navgScore + aavgScore;
			} else {
				finalScore = aavgScore;
			}
		} else {
			finalScore = navgScore;
		}
	
		if(isNeg)
			finalScore*=-1;	
		
		// set a ceil on the finalscore as [+100, -100]
		finalScore *= 100;

		// round to nearest integer
		int ifinalScore = Math.round(finalScore);
		if(ifinalScore > 100) ifinalScore = 100;
		else if(ifinalScore < -100) ifinalScore = -100;		
		
		return ifinalScore;
}	
	
/*
	public static void main(String[] args) {
		
		System.out.println("------------ Tagging started -----------------------");
		
		FeatureTester ft = new FeatureTester();
		ProductReview pr = null;

		
		//Read file
		FileInputStream fs = new FileInputStream(arg[0]);
		DataInputStream in = new DataInputStream(fs);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		
		//Read each line and tag
		while((sentence = br.readLine()) != null) {	
			if(!sentence.trim().equals("")){
				pr = ft.test(sentence,lp,tf,gsf);
			}
		}
	}	*/
	
}