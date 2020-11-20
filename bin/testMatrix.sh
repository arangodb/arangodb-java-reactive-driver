#!/bin/bash

# execute the tests for every docker image in the matrix
# every failing test will generate a logfile named like out-$(date +%s%N).txt

for img in \
  docker.io/arangodb/arangodb:3.5.5 \
  docker.io/arangodb/enterprise:3.5.5 \
  docker.io/arangodb/arangodb:3.6.6 \
  docker.io/arangodb/enterprise:3.6.6 \
  docker.io/arangodb/arangodb:3.7.2 \
  docker.io/arangodb/enterprise:3.7.2.1; do
  ./bin/testSingle.sh $img
done

echo "***************"
echo "*** SUCCESS ***"
echo "***************"
