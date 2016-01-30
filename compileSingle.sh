#!/bin/bash
mvn clean
mvn compile assembly:single
cd target
rm -R archive-tmp  classes  generated-sources
mv MatreshkaAnalyticsIntegration*jar MatreshkaAnalyticsIntegration.jar
cp ../src/main/shell/* .
chmod a+w .
fbsql < ../src/main/sql/createDatabase.sql
chmod o-w .
