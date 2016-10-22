#!/bin/bash
set -ex

echo "Updating Hosts file"
cat >> /etc/hosts <<-HOSTS
192.168.50.11 ambari.mycluster ambari
192.168.50.12 master.mycluster master
192.168.50.21 slave1.mycluster slave1
192.168.50.22 slave2.mycluster slave2
HOSTS
