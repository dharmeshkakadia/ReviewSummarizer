import java.util.regex.*;
import java.io.*;
import java.util.*;

import edu.smu.tspell.wordnet.AdjectiveSynset;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.WordNetDatabase;


class ExtractFeatures{

	public static void main(String args[]) throws Exception {
	
		String strLine,strLine1,strLine2;
		String token[] = new String[100];
		int score=0;
		
		//Read the review file
		
		System.setProperty("wordnet.database.dir", "/home/sandhya/Wordnet-3.0/dict");
		FileInputStream fstream = new FileInputStream("/home/sandhya/Sample.txt");
		 DataInputStream in = new DataInputStream(fstream);
  		BufferedReader br = new BufferedReader(new InputStreamReader(in));
  		
  		
  		//Read File Line By Line
  		while ((strLine = br.readLine()) != null)   {
			StringTokenizer str = new StringTokenizer(strLine);
			int i =0;
			int count = str.countTokens();
			while(str.hasMoreTokens())
			{
				token[i] = str.nextToken().toString();
				i++;	
			}
			
			for(int k=0;k<count;k++)
			{
  				
  				Pattern p1 = Pattern.compile("/NN");
        		Matcher mat1 = p1.matcher(token[k]);
				int j =0,m=0,feature = 0;
				
				if(mat1.find()){
					
					FileInputStream fstream2 = new FileInputStream("/home/sandhya/features.txt");
					DataInputStream in2 = new DataInputStream(fstream2);
			  		BufferedReader br2 = new BufferedReader(new InputStreamReader(in2));
					
			  		while ((strLine2 = br2.readLine()) != null) {
			  			
			  			if(strLine2.equals(token[k]))
			  			{
			  				feature = 1;
			  				break;
			  			}
			  			
			  		}
			  		if(feature==1)
			  		{
			  			System.out.print(token[k]);	
			  		}
			  		else
			  		{
			  			System.out.print("phone");
			  		}
			  		j= k-4<=0?0:k-4;
			  		m = k+4>=token.length?token.length:k+4;
			  		while(j<m)
			  		{
			  			Pattern p = Pattern.compile("/JJ");
			  			Matcher mat = p.matcher(token[j]);
			  			if(mat.find()){
			  				//System.out.print(token[j]);
			  				//find the orientation of the adjective	
			  				AdjectiveSynset nounSynset;
			  				AdjectiveSynset[] hyponyms = null;
		  					WordNetDatabase database = WordNetDatabase.getFileInstance();
		  					Synset[] synsets = database.getSynsets(token[i], SynsetType.ADJECTIVE);
		  					for (int l = 0; l < synsets.length; l++) 
		  					{
		  						nounSynset = (AdjectiveSynset)(synsets[l]);
		  						hyponyms = nounSynset.getSimilar();
		  					} 	
							FileInputStream fstream1 = new FileInputStream("/home/sandhya/seed.csv");
							DataInputStream in1 = new DataInputStream(fstream1);
							BufferedReader br1 = new BufferedReader(new InputStreamReader(in1));
							while ((strLine1 = br1.readLine()) != null)   {
								String s[] = strLine1.split(",");
								for(int r=0;r< hyponyms.length;r++)
								{
									if(s[0].equals(hyponyms[r].toString()))
									{
										//System.out.print(s[1]);
										if(s[1].equals("positive"))
										{
											score+=1;
										}
										else
										{
											score+=-1;
										}
									}
								}
							}
			  			}
			  			j++;							
			  		}
			  		System.out.println("");			
				}
			}
			System.out.println("["+score+"]"+"#"+strLine);
			score = 0;
		}
  		
 		//Close the input stream
  		in.close();		 
	}
}