package dist;

import java.io.IOException;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Reducer.Context;

import db.Search;

import scema.Feature;
import scema.ProductReview;

public class MyReduce extends Reducer<Text, ProductReview, Text, DoubleWritable> {
	
	public static Search db;
	
	public void reduce( Text productName,Iterable<ProductReview> productReviews, Context context) throws IOException, InterruptedException {
		db = new Search();
		for(ProductReview pr : productReviews){
			db.insertProduct(pr);
			for(Feature itr : pr.getFeatureList()) {
				context.write(new Text(itr.getName()),new DoubleWritable(itr.getRating()));
			}
			//context.write(new Text(pr.getName()), new Text(pr.getReview()));
		}
	} 
	
	protected void setup(Reducer.Context context) throws IOException, InterruptedException{
		//db = new Search();
	}

}