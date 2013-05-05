package db;

import java.util.ArrayList;

import scema.Feature;
import scema.ProductReview;

public class Runner {
	
	public static void main(String args[]) {
		Search search = new Search();
		
		ArrayList<Feature> feature = new ArrayList<Feature>(); 
		feature.add(new Feature("battery",3));
		feature.add(new Feature("camara",5));
		feature.add(new Feature("display",3));
		search.insertProduct(new ProductReview("mobile2",feature, "This is a review"));

		System.out.println(search.getAllPRoductReviews());
		//System.out.println(search.getProductDetail("mobile"));
		//System.out.println(search.getAllFeatureDetails("mobile"));
		//System.out.println(search.getRating("mobile","battery"));
		//System.out.println(search.getFeatureNames("mobile2"));
		//System.out.println(search.getAvgRating("mobile","battery"));
		//System.out.println(search.getAvgRatingofFeature("battery"));
		System.out.println(search.getSummary("mobile2"));		
	}
	
}
