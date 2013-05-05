package dist;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import scema.ProductReview;

public class MyDriver {

	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException  {
		Configuration conf = new Configuration();
		Job job = new Job(conf, "ReviewSummary");
		job.setJarByClass(MyDriver.class);
		job.setMapperClass(MyMapper.class);
		job.setReducerClass(MyReduce.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(ProductReview.class);
		FileInputFormat.addInputPath(job, new Path("input"));
		FileOutputFormat.setOutputPath(job, new Path("output"));
		job.waitForCompletion(true);
	}

}
