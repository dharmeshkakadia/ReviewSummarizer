package scema;

import java.io.Serializable;

public class Feature implements Serializable{
	String name;
	double rating;
	
	public Feature() {
		this.name = null;
		this.rating = 0;
	}
	
	public Feature(String name, double rating) {
		this.name = name;
		this.rating = rating;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public double getRating() {
		return rating;
	}
	public void setRating(double rating) {
		this.rating = rating;
	}
	
	public String toString() {
		return "feature name : " + name + " rating : " + rating;
	}
}
