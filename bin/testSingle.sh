#!/bin/bash

# runs the tests against the docker image provided as argument
# logs are redirected to a generate logfile named like out-$(date +%s%N).txt
# if tests are successfull the logfile will be removed

# USAGE:
#   export ARANGO_LICENSE_KEY=<arangodb-enterprise-license>
#   ./testSingle.sh <dockerImage>

# EXAMPLE:
#   ./testSingle.sh docker.io/arangodb/arangodb:3.7.2

# exit when any command fails
set -e

echo "==================================================="
echo "=== $1 "
echo "==================================================="

docker pull "$1"
logfile=out-$(date +%s%N).txt
# mvn clean test -e -Dtest.docker.image="$1" -Darango.license.key="$ARANGO_LICENSE_KEY" >"$logfile" 2>&1
mvn -e test \
  -DexcludedGroups="resiliency" \
  -Dtest.arangodb.requestTimeout="5000" \
  -Dtest.docker.image="$1" \
  -Darango.license.key="$ARANGO_LICENSE_KEY" >"$logfile" 2>&1

# remove logfile if the test completed successfully
rm "$logfile"
