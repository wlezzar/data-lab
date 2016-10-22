#!/bin/bash
set -ex

echo "Setting passwordless SSH"
cp /vagrant/data/keys/* ~vagrant/.ssh
chown vagrant ~vagrant/.ssh/id_rsa*
chmod 600 ~vagrant/.ssh/id_rsa*
{ echo; cat /vagrant/data/keys/id_rsa.pub; } >> ~vagrant/.ssh/authorized_keys
