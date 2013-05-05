ReviewSummarizer
================

Review summarization system built as part of a course project.

##Dependencies:
Following are the external java libraries being used by our project. We assume that all of them are present in the java classpath.
1. Stanford NLP library : stanford-parser.jar
2. WordNet java library : jaws-bin.jar
3. MongoDb java client Driver : mongo-2.x.x.jar
4. Hadoop platform libraries

We also assume that the home folder contains the a folder named "Parser", which contains the file "englishPCFG.ser.gz" (unlexicalized PCFG English Grammar for Stanford NLP parser)

##Running Procedure:

###Training 
$ java -jar ReviewSummary.jar <review training file>

###Review Tagging and Inserting
$ hadoop -D  mapred.child.java.opts=-Xmx1024m  jar ReviewSummary.jar dist.MyDriver

###ning MongoDB in Rest mode serving JSONP
$ mongod --rest --jsonp

##ToDo
* mavenize 
* 
