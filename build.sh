# compiling phase
javac \
	-d target \
	-cp lib/json.jar:lib/stanford-parser-3.8.0-models.jar:lib/servlet-api-3.0.jar:lib/jieba-analysis-1.0.3-SNAPSHOT.jar:lib/lucene-2.0.0/contrib/ant/lucene-ant-2.0.0.jar:lib/lucene-2.0.0/contrib/misc/lucene-misc-2.0.0.jar:lib/lucene-2.0.0/contrib/analyzers/lucene-analyzers-2.0.0.jar:lib/lucene-2.0.0/contrib/memory/lucene-memory-2.0.0.jar:lib/lucene-2.0.0/contrib/swing/lucene-swing-2.0.0.jar:lib/lucene-2.0.0/contrib/similarity/lucene-similarity-2.0.0.jar:lib/lucene-2.0.0/contrib/queries/lucene-queries-2.0.0.jar:lib/lucene-2.0.0/contrib/regex/lucene-regex-2.0.0.jar:lib/lucene-2.0.0/contrib/snowball/lucene-snowball-2.0.0.jar:lib/lucene-2.0.0/contrib/wordnet/lucene-wordnet-2.0.0.jar:lib/lucene-2.0.0/contrib/lucli/lucene-lucli-2.0.0.jar:lib/lucene-2.0.0/contrib/highlighter/lucene-highlighter-2.0.0.jar:lib/lucene-2.0.0/contrib/surround/lucene-surround-2.0.0.jar:lib/lucene-2.0.0/contrib/xml-query-parser/lucene-xml-query-parser-2.0.0.jar:lib/lucene-2.0.0/contrib/spellchecker/lucene-spellchecker-2.0.0.jar:lib/lucene-2.0.0/lucene-demos-2.0.0.jar:lib/lucene-2.0.0/lucene-core-2.0.0.jar:lib/jetty-all-9.0.4.v20130625.jar:lib/GstoreJavaAPI.jar:lib/stanford-parser.jar \
	-sourcepath src \
	-Xlint:unchecked \
	src/**/*.java

# packaging phase
echo "jar packaging..."
cd target
jar -cfm Ganswer.jar META-INF/MANIFEST.MF .
cd ..
rm Ganswer.jar
ln -s target/Ganswer.jar .
