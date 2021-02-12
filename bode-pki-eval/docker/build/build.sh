#!/bin/bash

printf "\n>>> Start building bode-pki-authorization-system\n\n"

cd ../../../bode-pki-authorization-system/
mvn clean package
cd -

cd authorization-system
cp ../../../../bode-pki-authorization-system/target/authorization-system-fat.jar .
cp ../../../../bode-pki-authorization-system/conf/config.json.sample_primary .
docker build -t bode-pki-authorization-system .

rm authorization-system-fat.jar
rm config.json.sample_primary

cd ..

printf "\n>>> Finished building bode-pki-authorization-system\n"

printf "\n>>> Start building bode-pki-besu\n\n"

cd hyperledger-besu
docker build -t bode-pki-besu .

cd ..

printf "\n>>> Finished building bode-pki-besu\n"
