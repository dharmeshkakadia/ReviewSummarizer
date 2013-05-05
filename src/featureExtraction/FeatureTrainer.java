/*
This class implements a trainer that we intend to train a classifier to identify possible features of a product in a file.

*/

package featureExtraction;

//package trainer;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.apache.hadoop.fs.FileSystem;

import com.mongodb.WriteResult;

// stanford parser required import files
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.process.*;
import edu.stanford.nlp.objectbank.TokenizerFactory;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.ling.*;

// import mongodb implementation class
import db.*;

public class FeatureTrainer {
	
	Collection<TypedDependency> dependencyTree;
	ArrayList<TaggedWord> taggedTokens;
	//static FeatureDB DB;
	Search DB;				// MONGO DB .. static now.. errors being throw.. resolve them.. TODO

	
	// public methods
	public FeatureTrainer() {
		DB = new Search();	
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

	// Train methods based on inital logic.
	/*
		Sequence of steps as follows:
			1. Give an input training file for the trainer.
			2. Tag all the nouns per sentence using the Stanford POS tagger or StanfordParserTagger
			3. Collect all nouns using tags : /NN and /NNS
			4. Get corresponding adjectives for each noun from typed dependencies using complex rules as framed by Manoj Kumar..
			5. Insert into training DB
	*/
	//public FeatureDB train(String trainingfile) {
	public void train(String trainingfile) {
		//FeatureDB DB = new FeatureDB();
		//Search DB = new Search();

			// Step 1:
			FileInputStream fs;
			DataInputStream in;
			BufferedReader br;
			
			String parserFile = "/home/hadoop/englishPCFG.ser.gz";
			
			try {
				fs = new FileInputStream(trainingfile);
				in = new DataInputStream(fs);
				br = new BufferedReader(new InputStreamReader(in));
				
				// Step 2:
				// Setup the stanford parser's required objects and prepare to fill the DB
				
				LexicalizedParser lp = new LexicalizedParser(parserFile);
				TokenizerFactory tf = PTBTokenizer.factory(false, new WordTokenFactory());
				TreebankLanguagePack tlp = new PennTreebankLanguagePack();
				GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
				GrammaticalStructure gs;
				String sentence;
				while((sentence = br.readLine()) != null) {	
					if(!sentence.trim().equals("")){
						sentence=sentence.toLowerCase();
						// parse all tokens first
						List tokens = tf.getTokenizer(new StringReader(sentence)).tokenize();
						lp.parse(tokens);
						Tree t = lp.getBestParse();
						taggedTokens = t.taggedYield();
						ArrayList<String> nounsPerLine = new ArrayList<String> ();
						ArrayList<String> nounsPerLineWithPosition = new ArrayList<String> ();
						int pos = 1;
						for(TaggedWord w : taggedTokens) {
							// Step 3:
							// Collect all nouns
							//System.out.println(w.tag());
							if(w.tag().contains("NN") && w.value().length()>1) {
								nounsPerLine.add(w.value());
								nounsPerLineWithPosition.add(w.value() + "-" + pos);
							}
							pos++;
						}
										
		
						// Step 4:
						gs = gsf.newGrammaticalStructure(t);
						dependencyTree = gs.typedDependencies();
						HashMap<String,Object> checkedNouns = new HashMap<String,Object>();
						for(String noun : nounsPerLineWithPosition) {
							boolean amod = false;
							// COMPLEX LOGIC : !!!!!
							/************************ENTER AT OWN RISK**************************
							*/
					
							// Step #1 : Check if nsubj is available . If not proceed down
							TypedDependency tdy;
	
							ArrayList<String> features = new ArrayList<String> ();
							ArrayList<String> modifiers = new ArrayList<String> ();
							String feature = null;
							String modifier = null;
							if((tdy = checkRelationExists(noun, "nsubj", false)) != null) {
								TreeGraphNode governor = tdy.gov();
								// check if governor is an adjective - from the taggedtokens list
								if(checkWordPOS(governor.value(), "JJ")) {
									// check for amod relation with this noun
									TypedDependency intermediate = checkRelationExists(noun, "amod", true);
					
									// Split in the road - "Two roads diverged in an yellow wood - Robert Frost kinda situation"
									// Travel both !!!!!!!!
									feature = tdy.dep().toString();
									modifier = tdy.gov().toString();/*
									if(intermediate != null) {
										if(checkWordPOS(intermediate.dep().value(), "JJ")) {
											feature = intermediate.dep().value() + " " + feature;
										}
									}
									
									// Road 2 !!!! 
									TypedDependency temp1 = checkRelationExists(governor.toString(), "amod", true);
									if(temp1 != null || (temp1 = checkRelationExists(governor.toString(), "advmod", true)) != null) {
										modifier = temp1.dep().toString() + " " + temp1.gov().toString();
									} else {
										modifier = tdy.gov().toString();
									}*/
								
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
													feature = temp2.gov().toString();
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
									//-features.add(nn_temp.dep().toString());
									if(!nn_temp.dep().toString().equals(feature))
										features.add(nn_temp.dep().toString() + " " + feature);
								}
								else features.add(feature);

								for(TypedDependency tConj : conjunctions(noun)){
									nn_temp = null;
									nn_temp = nns(tConj.dep().toString());
									if(nn_temp != null) {
										//-features.add(nn_temp.dep().toString());
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
								checkedNouns.put(foundFeatures[0],null);
								if (foundFeatures.length == 2) checkedNouns.put(foundFeatures[1],null);
							}
							
							//Removing position in modifiers
							ArrayList<String> modNotPosition = new ArrayList<String>();
							for(String s : modifiers)
								modNotPosition.add(s.replaceAll("-\\d*", ""));
							
							//Inserting into DB
	                           if(feature!=null){
	                               // REPLACED WITH THE NEW MONGODB code below comment
	                               //DB.insert(features.get(0).replaceAll("-\\d*", ""),modNotPosition);
	                               //DB.insertNounAdj(features.get(0).replaceAll("-\\d*", ""), modNotPosition);
	                               String[] temps = features.get(0).split(" ");                    /**************new change***************/
	                               DB.insertNounAdj(temps[0].replaceAll("-\\d*", ""), modNotPosition);
	                               if(temps.length == 2) {
	                                   DB.insertNounAdj(temps[1].replaceAll("-\\d*", ""), modNotPosition);
	                               }

	                               if(features.size() > 1){
	                                   for(int i = 1 ; i < features.size(); ++i){
	                                       // MONGODB code
	                                       //DB.insert(features.get(i).replaceAll("-\\d*", ""),null);
	                                       temps = features.get(i).split(" ");

	                                       DB.insertNounAdj(temps[0].replaceAll("-\\d*", ""), null);    /**************new change***************/
	                                       if(temps.length == 2)
	                                           DB.insertNounAdj(temps[1].replaceAll("-\\d*", ""), null);
	                                   }
	                               }
	                           }
							/* replaced by new code above 
							 * 
							//Inserting into DB
							if(feature!=null){
								// REPLACED WITH THE NEW MONGODB code below comment								
								//DB.insert(features.get(0).replaceAll("-\\d*", ""),modNotPosition);
								//System.out.println(features.get(0).replaceAll("-\\d*", "")+ "," + modNotPosition);
								//DBController.getDB().requestEnsureConnection();
								
								DB.insertNounAdj(features.get(0).replaceAll("-\\d*", ""), modNotPosition);
								//System.out.println("######" +ret.getError());
								
								if(features.size() > 1){
									for(int i = 1 ; i < features.size(); ++i)
										// MONGODB code										
										//DB.insert(features.get(i).replaceAll("-\\d*", ""),null);
										DB.insertNounAdj(features.get(i).replaceAll("-\\d*", ""), null);
								}
							}
							*/
						}
						for(String noun : nounsPerLineWithPosition) {
							if(!checkedNouns.containsKey(noun)){
								TypedDependency nn_temp = null;
								nn_temp = nns(noun);
								if(nn_temp != null && (nn_temp.reln().toString().equals("nn"))) {
									String depen = nn_temp.dep().toString();
									String gover = nn_temp.gov().toString();
									//DB.insert(depen.replaceAll("-\\d*", ""),null);
									//DB.insert(depen.replaceAll("-\\d*", "") + " " + gover.replaceAll("-\\d*", ""),null);
									// MONGO DB CODE BELOW
									DB.insertNounAdj(depen.replaceAll("-\\d*", "") + " " + gover.replaceAll("-\\d*", ""), null);
															
									checkedNouns.put(depen,null);
									checkedNouns.put(gover,null);
								}
								//else DB.insert(noun.replaceAll("-\\d*", ""),null);
								// MONGO DB CODE
								DB.insertNounAdj(noun.replaceAll("-\\d*", ""), null);
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		//return DB;
	}
	
	//public void test(String testfile, FeatureDB DB){
	
	public boolean validFeature(String noun) throws IOException{
		int FEATURE_THRESHOLD = 6;
		int ADJ_THRESHOLD = 4;
		//Long key = DB.wordTable.get(noun);
		//if(key != null && DB.wordObjectTable.get(key).frequency > FEATURE_THRESHOLD && DB.wordObjectTable.get(key).adjectives.size() > ADJ_THRESHOLD )		//check if word is present in list
		//	return true;
		
		// MONGODB CODE BELOW
		if(DB.getNounCount(noun) > FEATURE_THRESHOLD && DB.getAdjectiveCount(noun) > ADJ_THRESHOLD)
			return true;
		//key = DB.checkSynonymExistsInWordTable(noun);	//or is it any synonymn of existing feature
		if(DB.checkSynonymExistsInWordTable(noun)){									//yes it is a synonymn
			//DB.wordObjectTable.get(key).insert(noun + " ",null,false); //adding to synonynm list
			DB.insertNounAdj(noun + " ", null);
			//return DB.wordObjectTable.get(key).frequency > FEATURE_THRESHOLD  && DB.wordObjectTable.get(key).adjectives.size() > ADJ_THRESHOLD ;
			if(DB.getNounCount(noun) > FEATURE_THRESHOLD && DB.getAdjectiveCount(noun) > ADJ_THRESHOLD) return true;
		}
		return false;
	}

	public static void main(String[] args) {
		
		FeatureTrainer ft = new FeatureTrainer();
		File dir;
		
		if(args.length != 1)
			System.err.println("Usage : FeatureTrainer <Directory of training files>");
		else{
			dir = new File(args[0]);
			for (String file : dir.list()){
				ft.train(dir.getAbsolutePath()+"/"+file);
			}
		}

	}	
	
}