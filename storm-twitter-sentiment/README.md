# Twitter sentiment analysis with Storm
### What is this topology about ?
This storm topology tries to achieve sentiment analysis on twitter data about a particular **subject** (I tried to have a different approach to the sentiment analysis. See below). For example : what does people think about Docker ? Or how people love Trump ?

Here is a schema that represents this topology :

![Twitter storm topology](./images/twitter-storm-topology.png).

### Quickly test the topology in local mode (with docker and docker-compose)
Put yourself in this directory (tasks/task-3) and perform the following tasks :
- spawn HDFS and Zeppelin (for preview) : `( cd storm-twitter/docker && docker-compose rm -afv && docker-compose up )`
- in another terminal, compile and package the topology : `docker run -it -v $HOME/docker-maven:/root/.m2 -v $(pwd)/storm-twitter:/app -w /app maven:3-jdk-8 mvn clean package`
- configure the job launch command by setting these environment variables :
```bash
TWITTER_KEYWORDS="trump"
SENTIMENT_SUBJECT="trump"
TWITTER_API_KEY="key"
TWITTER_SECRET_KEY="secret"
TWITTER_TOKEN="token"
TWITTER_TOKEN_SECRET="token-secret"
```
- launch the topology with the following command :
```bash
docker run --net host --rm -it \
    -v $(pwd)/storm-twitter/target/storm-twitter-sentiment-1.0.jar:/storm-twitter/target/storm-twitter-sentiment-1.0.jar \
    java:8 \
    java -cp storm-twitter/target/storm-twitter-sentiment-1.0.jar \
    net.wlezzar.storm.TwitterTopology \
    --keywords $TWITTER_KEYWORDS \
    --subject $SENTIMENT_SUBJECT \
    --topology-name TwitterSentimentAnalysis \
    --twitter-api-key $TWITTER_API_KEY \
    --twitter-secret-key $TWITTER_SECRET_KEY \
    --twitter-token $TWITTER_TOKEN \
    --twitter-token-secret $TWITTER_TOKEN_SECRET \
    --fs-url hdfs://localhost:8020 \
    --fs-path /twitter/ \
    --hsync-threshold 100 \
    --local \
    --debug
```
- to preview the result files in HDFS, open up a browser with the following address : `localhost:8080`
- open the notebook "StormTwitter" and execute all the commands. You should be able to perform SQL commands at the end of the notebook. 

### Another sentiment analysis application... Boring !
A lot of streaming examples that involve twitter data are about sentiment analysis. However, I wanted to take a different approach that seemed interesting to me. But first let's take a look at a simpler approach.

A simple and widely used approach to sentiment analysis is to take all postive and negative words of the sentence and calculate a score based exclusively on them. This is the bag of word approach. It can work pretty well in some cases but has many drawbacks :
- It makes us lose the context. We do not know any more if these words are related or not to the subject we want to evaluate the sentiment on. For example, if we want to evaluate the sentiment on the subject **Docker** then : "I love **Docker** and hates virtualization" would be evaluated the same as "I love **Docker** and hates virtualization" even if in reality it means the opposite.
- It doesn't take negations into account. For example : "**Docker** is good" would be evaluated the same as "**Docker** is not good" 

The sentiment analysis algorithm I tried to do in this topology tries to get rid of this drawbacks by taking into account the semantic relationships between words using a dependency graph discovery algorithm (I used the Stanford NLP library for that). Here is how it works :
- We have to give it a subject in the beginning (that represents the entity we want to evaluate sentiment on) and for each text it receives, it tries to guess if the positive and negative words present in it are related or not to the subject. For example, if we choose **Docker** as the subject :
    - "I love **Docker** and hates virtualization" would be evaluated positively because the postive word *"love"* is related to docker while the negative word *"hates"* is not
    - "I love virtualization and hates **Docker**" would be evaluated negatively for the same reasons
- It takes into account negations :
    - "**Docker** is amazing" would be evaluated positively.
    - "**Docker** is not amazing" would be evaluated negatively.
- It takes into account only verbs and adjectives because I think it is the main way people express their sentiments with.

For example, here are the results for some sentences for the subject **Docker** :
```
I hate something but I love docker | sentiment : 1
I hate docker but I love something | sentiment : -1
I hate docker but that makes me love something | sentiment : -1
docker is the best technology of this last decade ! | sentiment : 1
I am crazy about Docker | sentiment : -1
``` 

If you want to give this algorithm a try :
- compile the project : `( cd storm-twitter && mvn clean package )`
- if you do not have maven and java 8 in your machine : `docker run -it -v $(pwd)/storm-twitter:/app -w /app maven:3-jdk-8 clean package`
- If you want to try your own examples : `java -cp storm-twitter/target/storm-twitter-1.0.jar net.wlezzar.storm.ml.sentiments.AdvancedSentimentEstimator "<subject>" "<sentence>"`
- if you do not hava java 8 installed in your machine, you can launch the application inside a java 8 docker container by preceding the java commands with : `docker run --net host --rm -it -v $(pwd)/storm-twitter/target/storm-twitter-1.0.jar:/storm-twitter/target/storm-twitter-1.0.jar java:8`
- for example, try :
    - subject -> "docker" and sentence : "I love Docker and hates virtualization"
    - subject -> "docker" and sentence : "I love virtualization and hates Docker"