package db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.bson.types.BasicBSONList;

import scema.Feature;
import scema.ProductReview;
import scema.ReviewSummary;

import synservice.WordNetJAWS;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

public class Search {
	
	static DBCollection productCollection;
	static DBCollection summaryCollection;
	static DBCollection nounAdjCollection;
	private final Double synThreshold = 0.3;
	WordNetJAWS w;
	
	public Search() {
		productCollection = DBController.getDB().getCollection("ProductReview");
		summaryCollection = DBController.getDB().getCollection("ReviewSummary");
		nounAdjCollection = DBController.getDB().getCollection("NounAdj");
		w = new WordNetJAWS("/home/hadoop/WordNet-3.0/dict");
	}
	
	public ArrayList<ReviewSummary> getSummary(String product) {
		BasicDBObject query = new BasicDBObject();
        	query.put("product", product);
        	DBCursor cur = summaryCollection.find(query);
        	ArrayList<ReviewSummary> list= new ArrayList<ReviewSummary>();
        
        	while(cur.hasNext()) {
        		list.add(parseReview(cur.next()));
        	}
        	return list;
	}

	public double getAvgRatingofFeature(String feature) {
		BasicDBObject query = new BasicDBObject();
        	query.put("feature", feature);
        	DBCursor cur = summaryCollection.find(query);
        	double sum=0;
        
        	while(cur.hasNext()){
        		sum+=(Double) cur.next().get("avgRating");
        	}
        	if(cur.count()==0)
        		return 0;
        	else
        		return sum/cur.count();
	}

	public double getAvgRating(String product, String feature) {
		BasicDBObject query = new BasicDBObject();
        query.put("product", product);
        query.put("feature", feature);
        DBCursor cur = summaryCollection.find(query);

        if(cur.hasNext())
        	return (Double) cur.next().get("avgRating");
        else 
        	return 0;
	}

	public WriteResult insertProduct(ProductReview pr){
		// update the summary
		for (Feature feature : pr.getFeatureList()){
			
			//ArrayList<String> existingfeatures = getAllFeatureNames();
			//WordNetJAWS syndb = new WordNetJAWS("/home/dharmesh/WordNet-3.0/dict/");
			//Pop sy = new Pop();
			
			/*for(String f : existingfeatures) {
				syndb.areSynonyms(feature.getName(),f);
			}*/
			String save=null;
			ArrayList<String> known_features = getAllNouns();
			for(String s : known_features){
				if(w.areSynonyms(s,feature.getName())){
					save = s;
					break;
				}
			}
			BasicDBObject q = new BasicDBObject("nouns.noun",save);
			DBCursor qwe = nounAdjCollection.find(q);
			
			if(qwe.hasNext()){
				BasicDBList li = (BasicDBList)((BasicDBObject)qwe.next()).get("nouns");
				int maxCount =0;
				String maxNoun=null;
				for(int i=0;i<li.size();i++) {
					BasicDBObject current = (BasicDBObject)li.get(i);
					if(((Number)current.get("count")).intValue()>maxCount){
						maxCount=((Number)current.get("count")).intValue();
						maxNoun = (String)current.get("noun");
					}
				}
				feature.setName(maxNoun);
			}
			
			DBObject searchQuery = new BasicDBObject("product", pr.getName());		
			searchQuery.put("feature", feature.getName());	
			DBCursor cur = summaryCollection.find(searchQuery);
			
			if(cur.count()==0){
				summaryCollection.insert(creatReviewSummary(pr.getName(),feature.getName(),feature.getRating()));
			}else {
				
				DBObject updateQuery = new BasicDBObject("$inc",new BasicDBObject("totalReviews",1));
				DBObject obj = cur.next();
				Double newAvg = (((Double)obj.get("avgRating"))*(Integer)obj.get("totalReviews")+feature.getRating())/((Integer)obj.get("totalReviews")+1);
				
				updateQuery.put("$set", new BasicDBObject("avgRating",newAvg));
				summaryCollection.update(cur.curr(), updateQuery,false,false);
			}
		}
		// insert the actual review
		return productCollection.insert(createProduct(pr));
	}
	
	public ArrayList<ProductReview> getAllPRoductReviews() {
		DBCursor cur = productCollection.find();
		ArrayList<ProductReview> list = new ArrayList<ProductReview>();
		
		while(cur.hasNext()){
			list.add(parseProduct(cur.next()));
		}
		return list;
	}
	
	public ArrayList<ProductReview> getProductDetail(String product) {
		BasicDBObject query = new BasicDBObject();
        query.put("name", product);
        DBCursor cur = productCollection.find(query);
        
		ArrayList<ProductReview> list = new ArrayList<ProductReview>();
		
		while(cur.hasNext()){
			list.add(parseProduct(cur.next()));
		}
		return list;
	}

	public double getRating(String product,String feature) {
		BasicDBObject query = new BasicDBObject();
        query.put("name", product);
        query.put("features.name", feature);
        DBCursor cur = productCollection.find(query);
        
		return parseProduct(cur.next()).getFeatureList().get(0).getRating();
	}
	
	public ArrayList<String> getFeatureNames(String product) {
		BasicDBObject query = new BasicDBObject();
        query.put("product", product);
        DBCursor cur = summaryCollection.find(query);
        ArrayList<String> list = new ArrayList<String>();
        
        for (int i=0;cur.hasNext();i++){
        	ReviewSummary r = parseReview(cur.next());
        	list.add(r.getFeature());	
        }
        
		return list;
	}
	
	public ArrayList<String> getAllFeatureNames() {
        DBCursor cur = summaryCollection.find();
        ArrayList<String> list = new ArrayList<String>();
        
        for (int i=0;cur.hasNext();i++){
        	ReviewSummary r = parseReview(cur.next());
        	list.add(r.getFeature());	
        }
        
		return list;
	}

	public ArrayList<Feature> getAllFeatureDetails(String product) {
		BasicDBObject query = new BasicDBObject();
        query.put("name", product);
        DBCursor cur = productCollection.find(query);
        
		ArrayList<Feature> list = new ArrayList<Feature>();
		
		while(cur.hasNext()){
			list.addAll(parseProduct(cur.next()).getFeatureList());
		}
		return list;
	}

	public ProductReview parseProduct(DBObject obj) {
		BasicBSONList features = (BasicBSONList)obj.get("features");
		ArrayList<Feature> fea = new ArrayList<Feature>();
		for(int i=0;i<features.size();i++){
			BasicDBObject f = (BasicDBObject)features.get(i);
			fea.add(new Feature((String)f.get("name"),(Double)f.get("rating")));
		}
		
		return new ProductReview((String)obj.get("name"),fea,(String)obj.get("review"));
	}
	
	public ReviewSummary parseReview(DBObject obj) {
		return new ReviewSummary((String)obj.get("product"),(String)obj.get("feature"),(Number)(obj.get("totalReviews")),(Number)(obj.get("avgRating")));
	}

	public DBObject createProduct(ProductReview pr) {
		
		BasicDBObject product = new BasicDBObject();
		product.put("name", pr.getName());
		product.put("review", pr.getReview());
		
		ArrayList<Feature> fetures = pr.getFeatureList();
		ArrayList<BasicDBObject> dbFeatures = new ArrayList<BasicDBObject>();
		for(Feature f : fetures) {
			BasicDBObject featureObject = new BasicDBObject();
			featureObject.append("name", f.getName());
			featureObject.append("rating", f.getRating());
			dbFeatures.add(featureObject);
		}
		product.put("features", dbFeatures);
		
		return product;
	}
	
	public DBObject creatReviewSummary(String product, String feature, double rating) {
		BasicDBObject reviewSummary = new BasicDBObject();
		reviewSummary.put("product", product);
		reviewSummary.put("feature", feature);
		reviewSummary.put("totalReviews", 1);
		reviewSummary.put("avgRating", rating);		
		
		return reviewSummary;
	}
	
/*
	public WriteResult insertNounAdj(String noun, String adj) throws IOException{
		DBObject nounQuery = new BasicDBObject(); 
		nounQuery.put("nouns.noun",noun);	
		DBCursor cur = nounAdjCollection.find(nounQuery);
		WriteResult ret;
		
		if(cur.count()==0){
			// noun doesn't exist
			System.out.println("noun doesn't exist");
			// check for synonyms of noun
			Runtime runtime = Runtime.getRuntime();
			Process process;	
			ArrayList<String> wordsInTable = getAllNouns();
			double maxSyn = 0;
			String maxNoun = null;
			
			for(String word : wordsInTable) {
				String commandString = "perl similarity.pl --type WordNet::Similarity::vector -allsenses " + word + " " + noun;
				
				process = runtime.exec(commandString);
				try {
					process.waitFor();
				} catch (InterruptedException e) {
					System.out.println(e);
				}
				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String s = reader.readLine();
				
				// convert to DOUBLE
				Double d = new Double(0);
				try {
					d = Double.parseDouble(s);
					if(d>synThreshold && d > maxSyn) { // syn found
						maxSyn = d;
						maxNoun = word;
					}
				} catch (Exception e) {
					continue;
				}
			}
			
			if(maxNoun != null){
				// syn with max similarity
				// incerment the noun
				DBObject synQuery = new BasicDBObject(); 
				synQuery.put("nouns.noun", maxNoun);
				
				
				DBObject updateQuery = new BasicDBObject("$inc",new BasicDBObject("nouns.$.count",1));
				nounAdjCollection.update(synQuery, updateQuery);
				BasicDBObject temp = new BasicDBObject("$inc", new BasicDBObject("totalNouns",1));
				ret=nounAdjCollection.update(synQuery, temp);
				
				BasicDBObject nn = new BasicDBObject("noun",noun);
				nn.append("count",1);
				BasicDBObject temp2 = new BasicDBObject("$push", new BasicDBObject("nouns",nn));
				//System.out.println("######"+nounAdjCollection.find(synQuery).next());
				ret=nounAdjCollection.update(synQuery, temp2);
				
				// update adj
				if(adj != null)
					return insertOrUpdateAdj(noun, adj);
				return ret;
			}else {
				// insert new noun-adj
				return insertNewNounAdj(noun, adj);
			}
					
		}else {
			// update count of noun and add adj
			System.out.println("noun exist : update count and handle adj");
			//DBObject matchedObj = cur.next();
			
			DBObject updateQuery = new BasicDBObject("$inc",new BasicDBObject("nouns.$.count",1));
			nounAdjCollection.update(nounQuery, updateQuery);
			BasicDBObject temp = new BasicDBObject("$inc", new BasicDBObject("totalNouns",1));
			ret=nounAdjCollection.update(nounQuery, temp);
			
			if(adj != null)
				return insertOrUpdateAdj(noun,adj);
			return ret;
		}
	}
*/
	
	public WriteResult insertOrUpdateAdj(String noun, String adj) {
		DBObject adjQuery = new BasicDBObject();
		adjQuery.put("nouns.noun",noun);
		adjQuery.put("adjs.adj", adj);

		if (nounAdjCollection.find(adjQuery).count() == 0 ){
			// adj doesnt exist
			//-System.out.println("adj doesnt exist");
			BasicDBObject n1 = new BasicDBObject();
			n1.put("nouns.noun",noun);
			
			BasicDBObject a1 = new BasicDBObject("adj",adj);
			a1.append("count", 1);
			DBObject u1 = new BasicDBObject("$push", new BasicDBObject("adjs",a1));
			nounAdjCollection.update(n1, u1);
			//u1.put("$inc", new BasicDBObject("totalAdjs",1));
			BasicDBObject temp = new BasicDBObject("$inc",new BasicDBObject("totalAdjs",1));
			return nounAdjCollection.update(n1, temp);
			
		}else {
			// adj is there : update count
			//System.out.println("adj is there : update count");
			BasicDBObject u1 = new BasicDBObject();
			u1.append("$inc", new BasicDBObject("adjs.$.count",1));
			//u1.append("$inc", new BasicDBObject("totalAdjs",1));
			nounAdjCollection.update(adjQuery, u1);
			
			BasicDBObject temp = new BasicDBObject("$inc",new BasicDBObject("totalAdjs",1));
			return nounAdjCollection.update(adjQuery, temp);
		}		
	}

	public WriteResult insertNewNounAdj(String noun, String adj) {
		BasicDBObject newObj = new BasicDBObject();
		BasicDBObject nounObj = new BasicDBObject("noun",noun);
		nounObj.put("count", 1);
		ArrayList<BasicDBObject> nlist = new ArrayList<BasicDBObject>();
		ArrayList<BasicDBObject> alist = new ArrayList<BasicDBObject>();
		nlist.add(nounObj);
		if(adj != null){
			BasicDBObject adjObj = new BasicDBObject("adj",adj);
			adjObj.put("count", 1);
			alist.add(adjObj);
		}
		
		newObj.put("nouns", nlist);
		newObj.put("adjs", alist);
		newObj.put("totalNouns",1);
		if(adj!=null)
			newObj.put("totalAdjs",1);
		else
			newObj.put("totalAdjs",0);
		return nounAdjCollection.insert(newObj);
	}
	
	public WriteResult insertNounAdj(String noun, ArrayList<String> adj) throws IOException {
		
		DBObject nounQuery = new BasicDBObject();
		nounQuery.put("nouns.noun", noun);
		DBCursor cur = nounAdjCollection.find(nounQuery);
		WriteResult ret = null;
		//System.out.println("count of rees"+cur.count());
		if(cur.count() == 0) {
			// noun doesnt exist
			//System.out.println(noun + ": noun doesn't exist");
			// Check for synonyms of noun
			// use Wordnet rather than "perl version"
			WordNetJAWS w = new WordNetJAWS("/home/hadoop/WordNet-3.0/dict/");
			
			ArrayList<String> wordsInTable = getAllNouns();
			//System.out.println("All nouns in table so far : " + wordsInTable);
			double maxSyn = 0;
			String maxNoun = null;
			for(String word : wordsInTable) {
				if(word.equals(noun) || (noun+"s").equals(word) || (word+"s").equals(noun)) {					
					maxNoun = word;
				}
			}
						
			
			// check if null or has a word
			if(maxNoun != null) {
				// syn with max similarity
				// incerment the noun
				//System.out.println(noun + " = " + maxNoun);
				DBObject synQuery = new BasicDBObject(); 
				synQuery.put("nouns.noun", maxNoun);
				
				DBObject updateQuery = new BasicDBObject("$inc",new BasicDBObject("nouns.$.count",1));
				nounAdjCollection.update(synQuery, updateQuery);
				BasicDBObject temp = new BasicDBObject("$inc", new BasicDBObject("totalNouns",1));
				ret=nounAdjCollection.update(synQuery, temp);
				
				BasicDBObject nn = new BasicDBObject("noun",noun);
				nn.append("count",1);
				BasicDBObject temp2 = new BasicDBObject("$push", new BasicDBObject("nouns",nn));
				//System.out.println("######"+nounAdjCollection.find(synQuery).next());
				ret=nounAdjCollection.update(synQuery, temp2);
				//System.out.println(nounAdjCollection.find(synQuery));

				// update adj
				// iterate through the adjectives list and insert them one by one
				// TODO:  check what this method returns?? - ASK DHARMESH!!!
				if(adj != null) {
					for(String adjectives : adj) {
						if(adjectives != null)
							insertOrUpdateAdj(noun, adjectives);
					}
				}
				return ret;
			} else {
				// insert the new noun and its adjectives
				if(adj != null) {
					for(String adjectives : adj) {
						ret=insertNewNounAdj(noun, adjectives);
					}
				}
				return ret;
			}
		} else {
			// ok we have a match
			// update the current noun and its adjective count
			// update count of noun and add adj
			//System.out.println(noun + ": noun exist"+((BasicDBObject)cur.next()).get("nouns"));
			//DBObject matchedObj = cur.next();
			
			DBObject updateQuery = new BasicDBObject("$inc",new BasicDBObject("nouns.$.count",1));
			nounAdjCollection.update(nounQuery, updateQuery);
			BasicDBObject temp = new BasicDBObject("$inc", new BasicDBObject("totalNouns",1));
			ret=nounAdjCollection.update(nounQuery, temp);
			if(adj != null) {
				for(String adjectives : adj) {
					if(adjectives != null)
						insertOrUpdateAdj(noun,adjectives);
				}
			}
			return ret;
		}
	}
	
	public ArrayList<String> getAllNouns() {
		DBCursor cur = nounAdjCollection.find();
        	ArrayList<String> list = new ArrayList<String>();
        	//-System.out.println(cur.count());
        
        	while(cur.hasNext())
        	{
        			BasicBSONList itr = (BasicBSONList)cur.next().get("nouns");
        			for(int i=0;i<itr.size();i++){
        				list.add((String)((BasicDBObject)itr.get(i)).get("noun"));
        			}
        	}
        
		return list;
	}
		
	public Integer getNounCount(String noun) {
		DBCursor cur = nounAdjCollection.find(new BasicDBObject("nouns.noun",noun));
		if(cur.hasNext()){
			BasicBSONList itr = (BasicBSONList)cur.next().get("nouns");
			for(int i=0;i<itr.size();i++){
				BasicDBObject obj = (BasicDBObject)itr.get(i);
				if(obj.get("noun").equals(noun))
					return (Integer)obj.get("count");
			}
			return new Integer(0);
		}
		else 
			return new Integer(0);
	}
	
	public Integer getAdjectiveCount(String noun) {
		DBCursor cur = nounAdjCollection.find(new BasicDBObject("nouns.noun", noun));
		if(cur.hasNext()) {
			BasicBSONList itr = (BasicBSONList)cur.next().get("adjs");
			return new Integer(itr.size());
		}else 
			return 0;
	}

	public int getTotalNounCount(String noun) {
		DBCursor cur = nounAdjCollection.find(new BasicDBObject("nouns.noun",noun));
		if(cur.hasNext())
			return ((Number)((BasicDBObject)cur.next()).get("totalNouns")).intValue();
		else
			return 0;
	}

	
	public ArrayList<String> getAdjs(String noun) {
		ArrayList<String> list = new ArrayList<String>();
		DBCursor cur = nounAdjCollection.find(new BasicDBObject("nouns.noun",noun));

		if(cur.hasNext()){
			BasicBSONList itr = (BasicBSONList)(cur.next().get("adjs"));
			for(int i=0;i<itr.size();i++){
				BasicDBObject obj = (BasicDBObject)itr.get(i);
				list.add((String)obj.get("adj"));
			}
		}
		return list;
	}

	// Check synonymexists in database
	public boolean checkSynonymExistsInWordTable(String noun) {
		// get all nouns first
		ArrayList<String> nounsInTable = getAllNouns();
		WordNetJAWS w = new WordNetJAWS("/home/hadoop/WordNet-3.0/dict/");
		for(String word : nounsInTable) {
				if((!word.equals(noun)) && w.areSynonyms(word, noun)) {
					return true;
				}
		}
		return false;
	}
				
}