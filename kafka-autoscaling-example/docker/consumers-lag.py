#!/usr/bin/env python 

import os
from datetime import datetime
import requests
import json
import sys
import time

class KafkaLagCheck:

    consumers_endpoint = "{burrow_url}/v2/kafka/{cluster_name}/consumer"
    lag_endpoint = "{burrow_url}/v2/kafka/{cluster_name}/consumer/{group}/lag"


    @staticmethod
    def burrow_request(url):
        raw_response = requests.get(url)
        if raw_response.status_code != 200:
            raise Exception("Response code from burrow : "+str(raw_response.status_code))
        data = raw_response.json()
        if data["error"] == True:
            raise Exception("Response contains error : "+str(data))
        return data


    @staticmethod
    def get_consumers(burrow_url, cluster_name):
        consumers_url = KafkaLagCheck.consumers_endpoint.format(burrow_url=burrow_url, cluster_name=cluster_name)
        data = KafkaLagCheck.burrow_request(consumers_url)
        return data["consumers"]

    @staticmethod
    def get_consumer_lag(burrow_url, cluster_name, consumer_group):
        consumer_lag_url = KafkaLagCheck.lag_endpoint.format(burrow_url=burrow_url, cluster_name=cluster_name, group=consumer_group)
        data = KafkaLagCheck.burrow_request(consumer_lag_url)
        partitions = data["status"]["partitions"]
        total_lag = 0
        for partition in partitions:
            topic = partition["topic"]
            partition_number = partition["partition"]
            lag = partition["end"]["lag"]
            total_lag = total_lag + lag
        return {"consumer":consumer_group, "lag":total_lag}    


if __name__ == "__main__":
    burrow_url = sys.argv[1]
    lag_threshold_to_spawn = int(sys.argv[2])
    lag_threshold_to_terminate = int(sys.argv[3])
    token = sys.argv[4]

    sleep_time = 5
    has_already_spawned = False
    has_already_terminated = False 
    while True:
        lag = KafkaLagCheck.get_consumer_lag(burrow_url, "local", "processors.group")
        print(lag)
        if lag["lag"] >= lag_threshold_to_spawn and not has_already_spawned :
            print("lag has gone beyond threshold ! Spawning a new machine !")
            os.system("docker $(docker-machine config node3) swarm join --token "+token+" $(docker-machine inspect --format='{{ .Driver.PrivateIPAddress }}' node1):2377")
            has_already_spawned = True
        if has_already_spawned and lag["lag"] <= lag_threshold_to_terminate and not has_already_terminated:
            print("lag has gone below threshold ! Terminating an instance !")
            os.system("docker $(docker-machine config node3) swarm leave")
            has_already_terminated = True
        time.sleep(sleep_time)
