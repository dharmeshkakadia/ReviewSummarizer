package scema;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import org.apache.hadoop.io.Writable;

public class ProductReview implements Writable{
	String name;
	ArrayList<Feature> featureList;
	String review;
	
	public ProductReview() {
		this.name = null;
		this.featureList = new ArrayList<Feature>();
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

	@Override
	public void readFields(DataInput in) throws IOException {
		int size=in.readInt();
		featureList = new ArrayList<Feature>(size);
		for(int i=0;i<size;i++) {
			Feature f = new Feature();
			f.readFields(in);
			featureList.add(f);
		}
		name=in.readUTF();
		review=in.readUTF();
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(featureList.size());
		for(Feature f : featureList) {
			f.write(out);
		}
		out.writeUTF(name);
		out.writeUTF(review);
	}
	
}
