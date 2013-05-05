package scema;

import java.io.Serializable;
import java.util.ArrayList;

public class ProductReview implements Serializable{
	String name;
	ArrayList<Feature> featureList;
	String review;
	
	public ProductReview() {
		this.name = null;
		this.featureList = null;
		this.review = null;
	}
	
	public ProductReview(String name, ArrayList<Feature> featureList, String review) {
		this.name = name;
		this.featureList = featureList;
		this.review = review;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ArrayList<Feature> getFeatureList() {
		return featureList;
	}

	public void setFeatureList(ArrayList<Feature> featureList) {
		this.featureList = featureList;
	}

	public String getReview() {
		return review;
	}

	public void setReview(String review) {
		this.review = review;
	}
	
	public String toString () {
		return "name : "+ name + " features : "+ featureList + " review : " + review;
	}
	
}
