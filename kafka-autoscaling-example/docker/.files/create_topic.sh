#!/bin/bash

HOST=$1
echo "Creating topic on host : $1"

function create_topic {
  echo
  echo -e "\e[1;92mCreating topic: $1\e[0m"
  kafka-topics --create --topic $1 \
  --partitions 10 \
  --replication 1 \
  --zookeeper $HOST:2181
}

topic="mortgage.documents"

create_topic $topic
