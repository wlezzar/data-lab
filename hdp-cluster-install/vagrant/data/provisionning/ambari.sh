#!/bin/bash
set -ex

# Install
sudo wget -nv http://public-repo-1.hortonworks.com/ambari/centos6/2.x/updates/2.2.2.0/ambari.repo -O /etc/yum.repos.d/ambari.repo
sudo yum -y install ambari-server

# Setup
sudo ambari-server setup

# Start
sudo ambari-server start
