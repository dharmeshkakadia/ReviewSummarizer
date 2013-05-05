package db;

import java.util.ArrayList;

import org.bson.types.BasicBSONList;

import scema.Feature;
import scema.ProductReview;
import scema.ReviewSummary;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

public class Search {
	
	static DBCollection productCollection;
	static DBCollection summaryCollection;
	
	public Search() {
		productCollection = DBController.getDB().getCollection("ProductReview");
		summaryCollection = DBController.getDB().getCollection("ReviewSummary");
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
			DBObject searchQuery = new BasicDBObject("product", pr.getName());
			
			for(String syn : getSynonyms(feature)) {
				
			}
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
	
	private ArrayList<String> getSynonyms(String feature) {
		// TODO Auto-generated method stub
		ArrayList<String> ret = new ArrayList<String>();
		ret.add(feature);
		return ret;
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
        query.put("name", product);
        DBCursor cur = productCollection.find(query);
        ArrayList<String> list = new ArrayList<String>();
        
        for (int i=0;cur.hasNext();i++){
        	for (Feature f : parseProduct(cur.next()).getFeatureList()) {
        		list.add(f.getName());	
        	}
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
		return new ReviewSummary((String)obj.get("product"),(String)obj.get("feature"),(Integer)obj.get("totalReviews"),(Double)obj.get("avgRating"));
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
}
