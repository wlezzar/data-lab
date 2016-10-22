# Task 1 - HDP Cluster using vagrant
### Instantiate the machines
```bash
vagrant up
```
This should instantiate 4 machines : ambari, master, slave1, slave2.
Vagrantfile contains an inline script that configures the machines by :
- setting passwordless SSH between all the machines (line 5)
- installing and starting ntpd (line 6 - 8)
- adding dns entries of the machines into /etc/hosts (line 9)
- disabling iptables (line 10 - 11)
- disabling selinux (line 12 - 13)
- updating soft and hard ulimits (line 14 - 15)
- disabling redhat huge pages feature (line 16 - 17)

### Install Ambari
```bash
vagrant ssh ambari
sudo /vagrant/data/provisionning/ambari.sh
```

### Connect to Ambari
Ambari can be reached at : `192.168.50.11:8080`
