#!/bin/sh

cd ~/git/pi-services/pi/modules/services
mvn clean package
java -jar target/pi-services-1.0-SNAPSHOT.jar server default-configuration.yml


