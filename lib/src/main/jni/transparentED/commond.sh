#!/bin/bash

rm enc*
rm des*
rm log*

for i in {1..100}
#for i in {1..1}
do
  echo $i times test begin ...
  ./e_test -mt org.jpg enc$i.xdja > log$i.txt 2>&1
  ./e_test -de enc$i.xdja des$i.jpg > /dev/null 2>&1
  echo $i times test end
done
