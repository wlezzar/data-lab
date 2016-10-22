#!/bin/bash

set -- java -cp /kafka-autoscaling-example.jar net.wlezzar.kafka.jobs.MortgageDocumentProcessor $@
exec "$@"
