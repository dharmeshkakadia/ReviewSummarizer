package dist;

import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

import scema.Feature;
import scema.ProductReview;

import db.Search;
import edu.stanford.nlp.objectbank.TokenizerFactory;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.WordTokenFactory;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import featureExtraction.FeatureTester;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.regex.Pattern;

public class MyMapper extends Mapper<Object, Text, Text, ProductReview> {
	
	public static LexicalizedParser lp;
	public static TokenizerFactory tf;
	public static TreebankLanguagePack tlp;
	public static GrammaticalStructureFactory gsf;
	//public static FeatureTester ft;
	

	public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
		
		FileSplit fileSplit = (FileSplit)context.getInputSplit();
		String filename = fileSplit.getPath().getName().replaceAll(".txt", "");
		FeatureTester ft = new FeatureTester();
		String paragraph = value.toString();
		Pattern p = Pattern.compile("[\\.\\!\\?]\\s+", Pattern.MULTILINE);
		String[] sentences = p.split(paragraph);

		for(String s : sentences){
			if(!("".equals(s.trim()))){
				ProductReview pr = ft.test(filename, s,lp,tf,gsf);
				if(pr != null)
					context.write(new Text(filename),pr);
			}
		}
	}
	
	protected void setup(Mapper.Context context) throws IOException, InterruptedException{
		// Setup the stanford parser's required objects

		String parserFile = "/home/hadoop/englishPCFG.ser.gz";
		lp = new LexicalizedParser(parserFile);
		tf = PTBTokenizer.factory(false, new WordTokenFactory());
		tlp = new PennTreebankLanguagePack();
		gsf = tlp.grammaticalStructureFactory();
		//ft = new FeatureTester();
		
		
	}

	
}