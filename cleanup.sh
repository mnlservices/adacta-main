#!/bin/bash

rm -r alf_data_dev
find . -type f -name 'pom.xml.releaseBackup' -delete
find . -type f -name 'pom.xml.versionsBackup' -delete
rm alfresco.log*
rm share.log*
rm solr.log*

mvn clean -q