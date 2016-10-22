# spawn a single node kafka cluster on aws
# IMPORTANT : remember to allow all traffic from my ip
docker-machine create --amazonec2-region eu-west-1 \
                      --driver amazonec2 \
                      --amazonec2-instance-type t2.medium \
                      --amazonec2-security-group 'docker-machine' \
                      kafka-cluster
docker-machine ssh kafka-cluster <<-SCRIPT
sudo curl -L git.io/scope -o /usr/local/bin/scope
sudo chmod a+x /usr/local/bin/scope
sudo scope launch --service-token=9ffx3ncm6c9ugdrghn31uharohoa3pmp
SCRIPT

eval "$(docker-machine env kafka-cluster)"
export KAFKA_CLUSTER_PRIVATE_IP="$(docker-machine inspect --format='{{ .Driver.PrivateIPAddress }}' kafka-cluster)"
docker-compose -f kafka-cluster.yml up && docker-compose -f kafka-cluster.yml rm -afv
eval "$(docker-machine env --unset)"

docker $(docker-machine config kafka-cluster) run --name burrow -it --rm --net host wlezzar/burrow

# create kafka topics 
docker run -it --rm --net host -v $(pwd)/.files/create_topic.sh:/create_topic.sh wlezzar/kafka:confluent-2.0.1 dockerize -wait tcp://$(docker-machine ip kafka-cluster):9092 -timeout 60s /create_topic.sh $(docker-machine ip kafka-cluster)

# spawn producer on aws
docker $(docker-machine config kafka-cluster) run \
       -it --rm --net host --name producer1 \
       wlezzar/kafka-autoscaling-example-producer:1.0 \
       --topic mortgage.documents \
       --producer-properties \
       bootstrap.servers=localhost:9092 \
       --interval 2000

################################################################################# 

# spawn an AWS swarm cluster
for node in "node1" "node2" "node3"
do 
docker-machine create --amazonec2-region eu-west-1 \
                      --driver amazonec2 \
                      --amazonec2-instance-type t2.small \
                      --amazonec2-security-group 'docker-machine' \
                      $node && \
docker-machine ssh $node <<-SCRIPT
sudo curl -L git.io/scope -o /usr/local/bin/scope
sudo chmod a+x /usr/local/bin/scope
sudo scope launch --service-token=9ffx3ncm6c9ugdrghn31uharohoa3pmp
SCRIPT
done

docker $(docker-machine config node1) swarm init --advertise-addr $(docker-machine inspect --format='{{ .Driver.PrivateIPAddress }}' node1)
TOKEN=SWMTKN-1-4xjws53g86hi2df86dlb2xbzt277knxqr1aq7j3w2nqg01iyes-26vbulwchx0l5xz21vicy7cqb
# visualizer
docker $(docker-machine config node1) run --rm -it -p 5000:5000 -e HOST=$(docker-machine ip node1) -e PORT=5000 -v /var/run/docker.sock:/var/run/docker.sock manomarks/visualizer
# nodes
docker $(docker-machine config node2) swarm join --token $TOKEN $(docker-machine inspect --format='{{ .Driver.PrivateIPAddress }}' node1):2377
docker $(docker-machine config node3) swarm join --token $TOKEN $(docker-machine inspect --format='{{ .Driver.PrivateIPAddress }}' node1):2377

docker $(docker-machine config node3) swarm leave



################################################################################

# create the consumers
docker $(docker-machine config node1) service create \
    --name consumers \
    --mode global \
    wlezzar/kafka-autoscaling-example-consumer:1.0 \
    --topic mortgage.documents \
    --consumer-properties max.partition.fetch.bytes=100,bootstrap.servers=$(docker-machine inspect --format='{{ .Driver.PrivateIPAddress }}' kafka-cluster):9092,group.id=processors.group \
    --processing-time 2000 \
    --es-host "$(docker-machine inspect --format='{{ .Driver.PrivateIPAddress }}' kafka-cluster):9300" \
    --es-index mortgage_documents \
    --es-mapping processed_documents

docker $(docker-machine config node1) service rm consumers

# lag monitor
./consumers-lag.py http://$(docker-machine ip kafka-cluster):8000 20 5 $TOKEN

# consumers group members
docker $(docker-machine config kafka-cluster) run -it --rm --net host wlezzar/confluent:2.0.1 /bin/bash
watch -n 1 'export IFS="," && kafka-consumer-groups --new-consumer --bootstrap-server localhost:9092 --describe --group processors.group | while read a b c d e f g; do echo "$b, $c, $d, $g"; done'
#docker $(docker-machine config kafka-cluster) run -it --rm --net host wlezzar/confluent:2.0.1 watch -n 2 "kafka-consumer-groups --new-consumer --bootstrap-server localhost:9092 --describe --group processors.group"

# Stop everything
docker-machine rm $(docker-machine ls -q)


## Debug section ################################################
docker $(docker-machine config node1) node ls

docker $(docker-machine config node1) service ls
docker $(docker-machine config node1) service ps -f "desired-state=running" consumers