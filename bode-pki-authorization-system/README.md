# BODE-PKI Authorization System

This repository provides the source code of the BODE-PKI prototype.

## Prerequisites

* Java 11.0.6
* Maven 3.6.1

## Setup

1. Clone repository
2. Navigate into the cloned directory
3. Continue with "Setup Primary" or "Setup Replica"

<ins>Setup Primary:</ins>

Create the configuration file

```
$ cp conf/config.json.sample_primary conf/config.json
```

Edit the configuration file to your requirements:
* Possible fields are:
    * `PORT`
    * `CLI_PORT`
    * `PRIMARY`
* Mandatory fields are: 
    * `PRIMARY`

Build with maven: 

```$ mvn clean package```

Start the primary:

```$ java -jar target\authorization-system-fat.jar```

<ins>Setup Replica:</ins>

Create the configuration file:

```
cp conf/config.json.sample_replica conf/config.json
```

Edit the configuration file to your requirements:

* Possible fields are:
    * `PORT`
    * `CLI_PORT`
    * `PRIMARY`
    * `REPLICA`
* Mandatory fields are: 
    * `PRIMARY`
    * `REPLICA`

Build with maven: 

```$ mvn clean package```

Start the primary:

```$ java -jar target\authorization-system-fat.jar``` 

## Configuration

The following lists all configuration fields and all their subfields if existing. 

##### PORT 

* mandatory: false
* type: positive integer

<ins>Example:</ins>

```
"PORT": 8080
```

##### CLI_PORT

* mandatory: false
* type: positive integer

<ins>Example:</ins>

```
"CLI_PORT": 8081
```

##### PRIMARY

* description: configuration with regards to PRIMARY related information
* mandatory: true
* type: json
* subfields:
    * certificate
        * description: certificate of the primary
        * mandatory: true
        * type: json
        * subfields
            * key
                * description: public key of the primary
                * mandatory: true
                * type: string
            * host
                * description: host name of the primary
                * mandatory: true
                * type: string
            * asPort
                * description: authorization system port of the primary
                * mandatory: true
                * type: positive integer
                * range: positive integer
    * private_key:
        * description: private key of the primary
        * mandatory: true, if running as PRIMARY
        * type: string

<ins>Example:</ins>

```
"PRIMARY": {
    "certificate": {
        "key": "5c911c14a9c997c7d6cf1a87a765042a697386e0f9688227243d206a17a8c46079546c0e2d5d88d554fa58277cb0817c086f0d506b10d1d2938854aaf586e4d5",
        "host": "na.org",
        "asPort": 8080
    }
}
```
  
##### REPLICA

* description: configuration with regards to REPLICA related information
* mandatory: true, if running as REPLICA
* type: json
* subfields:
    * certificate
        * description: certificate of the replica
        * mandatory: true
        * type: json
        * subfields
            * key
                * description: public key of the replica
                * mandatory: true
                * type: string
            * host
                * description: host name of the replica
                * mandatory: true
                * type: string
            * nodePort:
                * description: port of the blockchain node that is used publicly available
                * mandatory: true
                * type: positive integer
                * range: positive integer
            * asPort
                * description: authorization system port of the replica
                * mandatory: true
                * type: positive integer
                * range: positive integer
    * connector:
        * description: connector related configuration
        * mandatory: true
        * type: json
        * subfields:
            * port
                * description: port of the blockchain node that is used internally
                * mandatory: true
                * type: positive integer
                * range: positive integer
            * update_period:
                * description: millseconds interval with which nodes are updated
                * mandatory: false
                * type: positive integer
                * default: 5000
            * type:
                * description: sets the connector type
                * mandatory: false
                * type: string
                * default: besu
    * private_key:
        * description: private key of the replica
        * mandatory: true
        * type: string
    * sync_period: 
        * description: millseconds interval with which replicas sync with each other
        * mandatory: false
        * type: positive integer
        * default: 5000
        
<ins>Example:</ins>

```
"REPLICA": {
    "certificate":{
        "key": "95fc259fd6fc3eaf82d02685e69dc1fa6f7eef6858c5c456c092b12072b41c5822f30bd33610a08af621704a442c3caf62f8f98a1d875dca0679168d1a78ff64",
        "host": "opendata.a.org",
        "asPort": 8080,
        "nodePort": 30303
    },
    "private_key":"ac594f2a43f92f38608f81b374684c996b15e20ed7625990165b2b17402398a9",
    "connector": {
        "port":8546,
        "update_period": 5000,
        "type": "besu"
    },
    "sync_period": 1000
}
``` 
