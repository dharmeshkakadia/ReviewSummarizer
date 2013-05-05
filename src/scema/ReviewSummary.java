package scema;

public class ReviewSummary {
	String product;
	String feature;
	int totalReviews;
	double avgRating;
		
	public ReviewSummary() {
		this.product = null;
		this.feature = null;
		this.avgRating = 0;
		totalReviews = 0;
	}
	
	public ReviewSummary(String product, String feature, int totalReviews, double avgRating) {
		this.product = product;
		this.feature = feature;
		this.totalReviews =totalReviews;
		this.avgRating = avgRating;
	}
	public ReviewSummary(String product, String feature, Number totalReviews, Number avgRating) {
		this.product = product;
		this.feature = feature;
		this.totalReviews =totalReviews.intValue();
		this.avgRating = avgRating.doubleValue();
	}
	
	public int getTotalReviews() {
		return totalReviews;
	}

	public void setTotalReviews(int totalReviews) {
		this.totalReviews = totalReviews;
	}

	public String getProduct() {
		return product;
	}
	public void setProduct(String product) {
		this.product = product;
	}
	public String getFeature() {
		return feature;
	}
	public void setFeature(String feature) {
		this.feature = feature;
	}
	public double getAvgRating() {
		return avgRating;
	}
	public void setAvgRating(double avgRating) {
		this.avgRating = avgRating;
	}
	
	public String toString() {
		return "product : " + product + " feature : " + feature + " totalReviews : " + totalReviews + " avgRating: " + avgRating;
	}
}
