## About

Today Open Data has gained popularity and its impact and relevancy has been shown in
several studies. Notable Open Data ecosystems, such as the European Data Portal (EDP),
host more than a million datasets and involve a wide range of stakeholders. Above all,
Open Data lives from its reusability which is endangered by reliability, data quality and
interoperability issues of current Open Data ecosystems. To overcome these drawbacks,
research proposes the application of blockchain. For this, blockchain may solve reliability
issues with regard to shared common state and a uniform infrastructure may reduce interoperability
issues and barriers between involved stakeholders. In addition, the application
of blockchain may increase the data quality by introducing rules when data is published
through the blockchain. Open Data is readable without permission, but the right to write is
reserved only for known data publishers and data providers. Therefore, new ones have go
through an authorization process to take part in a public permissioned blockchain-based
Open Data ecosystem. However, current research does not consider how data publishers
and data providers can practically join such an ecosystem. Especially, simple mechanisms
with as little human interaction as possible are indispensable. We propose an onboarding
process for nodes of a blockchain-based Open Data ecosystem. This includes a dedicated
Public Key Infrastructure for a Blockchain-based Open Data Ecosystem (BODE-PKI)
which adopts the certification of new nodes and the distribution of certificates and their
revocation status across all involved nodes. For this, the certification is highly inspired by
the Automatic Certificate Management Environment (ACME) protocol which provides usability
with little human interaction. The BODE-PKI is based on a authorization database
that is stored distributed across all nodes and provides transparency and accountability with
regard to Open Data principles. We evaluate the presented approach with Hyperledger Besu
as the underlying blockchain system against derived requirements and demonstrate the
desired applicability and performance. We show that the presented approach can cope with
real-world environments but at the same time complies with fundamental requirements.
Finally, the presented approach can serve as a basis for practical onboarding of nodes in a
blockchain-based Open Data ecosystem.

## Repository

This repository provides instructions on how to perform the tests with the prototype. These tests were used for the evaluation of this thesis. This readme includes the prerequisites, a description of the system under test, the folder structure, how the prototype is build and the actual execution of the tests.

### Prerequisites

In the following prerequisites are listed that are necessary to build the prototype and perform the tests:

* docker 20.10.2
* docker-compose 1.27.4
* maven 3.6.1
* java 11.0.6
* python 3.9.0
* pip 20.3.1

In addition, the following Python libraries are mandatory to run the tests: web3, numpy, grequests, matplotlib, pathlib. The following command installs the required libraries:

```
$ pip install web3 numpy grequests matplotlib pathlib
```

The specification of the system used to perform the tests are listed in the following:

* CPU: Intel Core i5-8265U CPU @ 1.60GHz 1.80 GHz, 4 Cores, 8 Threads
* Memory: 16 GB
* Operating System: Windows 10

Moreover, docker desktop was configured with 4 CPUs, 8 GB memory and 2 GB swap. You can configure the virtual resources under `Settings | Resources`. For the execution both Windows PowerShell and PyCharm/IntelliJ Terminal have been tested and are recommended. In contrast, Git Bash does not print any steps and is therefore not recommended.

### Folder Structure

The following folder structure is present in the source code:

* bode-pki
  * bode-pki-authorization-system
    * conf
    * src   
  * bode-pki-eval
    * docker
    * eval
    * results

### Build

To build the prototype, please perform the following five steps: 

1. Go to the evaluation folder:

```
$ cd ./bode-pki-eval/
```

2. Go to the build folder:

```
$ cd ./docker/build/
```

3. Make the build script executable:

```
$ chmod +x build.sh
```

4. Run the build script:

```
$ ./build.sh
```

5. Return to the evaluation folder:

```
$ cd ../../
```

### Run

The tests are written Python and Python 3 is mandatory. If your Python 3 executable is reachable by `python3` please replace it in the following commands accordingly. The main script is using a reset script. Both the main script and the reset script are located in the `eval`  folder. The reset script is used to restore all docker container to their initial state between tests. If your docker is running as root under Unix you may have to run these tests with sudo. 

Run all tests by:

```
$ python eval/main.py all
```

Run the basic functionality test by:

```
$ python eval/main.py basic
```

Run the synchronization test by:

```
$ python eval/main.py sync
```

Run the access control test by:

```
$ python eval/main.py access
```

After performing the tests, you will find the results in the results folder. The folder with the highest number contains the results of the last test run. 
