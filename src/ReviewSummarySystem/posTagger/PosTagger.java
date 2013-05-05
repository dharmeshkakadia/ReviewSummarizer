package review.summary.system;

// java specific imports
import java.io.IOException;
import java.util.*;

// Hadoop specific imports
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

// POS tagger specific imports
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class PosTagger {
	
	static class PosTaggerMapper extends Mapper<LongWritable, Text, LongWritable, Text> {
		
		/* data members specific to mapper.
		   logic part:
		   We have a mapper that will pos tag the word file
		*/
		static String modelFile = "bidirectional-distsim-wsj-0-18.tagger";		// This will be the model file to use for tagging
		MaxentTagger tagger;
		
		public void setup(Context context) throws IOException, InterruptedException {
			// called once for each task
			// setup POS tagging variables
			try {
				tagger = new MaxentTagger(modelFile);
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public void map(LongWritable key, Text input, Context context) throws IOException, InterruptedException {
			
			// Pos tagger should do its job here
			String inputString = input.toString();
			String outputString = tagger.tagString(inputString);
			context.write(key, new Text(outputString));
		}
	}
	
	static class PosTaggerReducer extends Reducer<LongWritable, Text, LongWritable, Text>  {
		
		public void reduce(LongWritable key, Text value, Context context) throws IOException, InterruptedException{
			// just write to output
			context.write(key, value);
		}
	}
	
	public static void main(String args[]) throws Exception {
		
		Configuration conf = new Configuration();
		Job job = new Job();
		job.setJarByClass(PosTagger.class);
		// set output formats of job
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		
		job.setMapperClass(PosTaggerMapper.class);
		job.setReducerClass(PosTaggerReducer.class);
		job.setOutputKeyClass(LongWritable.class);
		job.setOutputValueClass(Text.class);
		job.waitForCompletion(true);
	}
}
