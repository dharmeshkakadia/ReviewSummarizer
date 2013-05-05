package scema;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import org.apache.hadoop.io.Writable;

public class Feature implements Writable{
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

	@Override
	public void readFields(DataInput in) throws IOException {
		rating=in.readDouble();
		name=in.readUTF();
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeDouble(rating);
		out.writeUTF(name);
		
	}
}
