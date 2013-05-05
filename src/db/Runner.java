package db;

import java.io.IOException;
import java.util.ArrayList;

import scema.Feature;
import scema.ProductReview;

public class Runner {
	
	public static void main(String args[])  throws IOException{
		Search search = new Search();
		//Pop.dataFiller("diaper");
		ArrayList<Feature> feature = new ArrayList<Feature>(); 
		feature.add(new Feature("picture",3));
		feature.add(new Feature("camara4",5));
		feature.add(new Feature("display2",3));
		//search.insertProduct(new ProductReview("mobile2",feature, "This is a review"));
		//search.insertNounAdj("img", null);
		//System.out.println(search.getAllPRoductReviews());
		//System.out.println(search.getProductDetail("mobile"));
		//System.out.println(search.getAllFeatureDetails("mobile"));
		//System.out.println(search.getRating("mobile","battery"));
		//System.out.println(search.getAllFeatureNames());
		//System.out.println(search.getAvgRating("mobile","battery"));
		//System.out.println(search.getAvgRatingofFeature("battery"));
		//System.out.println(search.getSummary("mobile2"));
		System.out.println(search.getNounCount("img"));
		//System.out.println(search.getAdjectiveCount("pic"));
	}
	
}
