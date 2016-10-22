#!/usr/bin/env bash

# Package the project
BASEDIR=$(dirname $0)
PROJECT_DIR=${BASEDIR}/..
if [[ "$DIR" != /* ]]
then
    PROJECT_DIR=$(pwd)/${PROJECT_DIR}
fi
# create maven volume if not exists
docker ps -a | grep 'maven-data' > /dev/null
if [[ $? != 0 ]]; then
    docker create --name maven-data maven:3.3.9-jdk-8
fi
DOCKER_COMMAND="docker run --rm -it --volumes-from maven-data"
( cd ${PROJECT_DIR} && ${DOCKER_COMMAND} -w /project -v ${PROJECT_DIR}:/project maven:3.3.9-jdk-8 mvn clean package )

cp ${PROJECT_DIR}/target/kafka-autoscaling-example-1.0.jar ${BASEDIR}/base/kafka-autoscaling-example.jar
( cd ${BASEDIR}/base && docker build -t wlezzar/kafka-autoscaling-example-base:1.0 . )
( cd ${BASEDIR}/consumer && docker build -t wlezzar/kafka-autoscaling-example-consumer:1.0 . )
( cd ${BASEDIR}/producer && docker build -t wlezzar/kafka-autoscaling-example-producer:1.0 . )
rm ${BASEDIR}/base/kafka-autoscaling-example.jar

docker push wlezzar/kafka-autoscaling-example-base:1.0
docker push wlezzar/kafka-autoscaling-example-producer:1.0
docker push wlezzar/kafka-autoscaling-example-consumer:1.0
