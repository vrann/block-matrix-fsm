#!/bin/bash

i=1
for ip in $(cat cluster-ips.txt )
do
    IFS=';' read -ra ADDR <<< "$ip"
    echo " $COUNTER "
    echo "Working on $i :${ADDR[0]}:${ADDR[1]}: now"

    ssh -o StrictHostKeyChecking=no -i ~/Projects/aws-nvirgin.cer ubuntu@${ADDR[0]} << EOF

rm -rf target/myapp-dev.log
rm -rf .actorchoreography/section$i

EOF

  scp -i ~/Projects/aws-nvirgin.cer /home/ubuntu/.actorchoreography/section0/ ubuntu@${ADDR[0]}:/home/ubuntu/.actorchoreography/section$i
  scp -i ~/Projects/aws-nvirgin.cer /home/ubuntu/actormatrix-assembly-0.0.1.jar ubuntu@${ADDR[0]}:/home/ubuntu/actormatrix-assembly-0.0.1.jar

  i=$[$i +1]
done