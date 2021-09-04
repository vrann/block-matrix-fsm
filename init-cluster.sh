#!/bin/bash
#java -jar -Xms8G -Xmx16G -Dconfig.file=application.local.conf actormatrix-assembly-0.0.1.jar > log.log
i=0
SEED_IP="172.30.1.23"
SEED_EXTERNAL_IP="184.72.189.117"
scp -i ~/Projects/aws-nvirgin.cer target/scala-2.12/actormatrix-assembly-0.0.1.jar ubuntu@$SEED_EXTERNAL_IP:/home/ubuntu/actormatrix-assembly-0.0.1.jar
for ip in $(cat cluster-ips.txt )
do
    IFS=';' read -ra ADDR <<< "$ip"
    echo " $COUNTER "
    echo "Working on $i :${ADDR[0]}:${ADDR[1]}: now"

    read -r -d '' config << EOM
include "application"

web {
  port = 8081
}
section = $i
matrixSize = 347
akka {
    loglevel = "INFO"
    stdout-loglevel = "INFO"
    actor {
      allow-java-serialization = off
      provider = "cluster"
      serializers {
        jackson-json = "akka.serialization.jackson.JacksonJsonSerializer"
      }
      serialization-bindings {
        "com.vrann.Message" = jackson-json
        "com.vrann.BlockMatrixType" = jackson-json
        "com.vrann.positioned.PositionCommand" = jackson-json
      }

      provider = cluster
    }
    cluster {
        seed-nodes = [
          "akka://actormatrix@$SEED_IP:2551",
        ]
    }
    remote {
        artery {
          canonical {
            port = 2551
            hostname = "${ADDR[1]}"
          }
          log-sent-messages = on
          log-received-messages = on
        }
    }
}
EOM
    mkdir ${ADDR[0]}
    rm -rf ${ADDR[0]}/application.local.conf
    echo "$config" >> ${ADDR[0]}/application.local.conf

    scp -i ~/Projects/aws-nvirgin.cer init.sh ubuntu@${ADDR[0]}:/home/ubuntu/
    scp -i ~/Projects/aws-nvirgin.cer  ~/Projects/aws-nvirgin.cer ubuntu@${ADDR[0]}:/home/ubuntu/
    ssh -o StrictHostKeyChecking=no -i ~/Projects/aws-nvirgin.cer ubuntu@${ADDR[0]} << EOF

rm -rf log.log
rm -rf .actorchoreography
mkdir .actorchoreography
mkdir .actorchoreography/section$i
scp -o StrictHostKeyChecking=no -i aws-nvirgin.cer ubuntu@$SEED_IP:/home/ubuntu/actormatrix-assembly-0.0.1.jar actormatrix-assembly-0.0.1.jar

EOF

    #ssh -i ~/Projects/aws-nvirgin.cer ubuntu@${ADDR[0]} $command


    scp -i ~/Projects/aws-nvirgin.cer ${ADDR[0]}/application.local.conf ubuntu@${ADDR[0]}:/home/ubuntu/application.local.conf
    scp -i ~/Projects/aws-nvirgin.cer "/Users/etulika/Projects/block-matrix-fsm/sections/sections-large/section$i/section.conf" "ubuntu@${ADDR[0]}:/home/ubuntu/.actorchoreography/section$i/section.conf"

    i=$[$i +1]

done
scp -o StrictHostKeyChecking=no -i ~/Projects/aws-nvirgin.cer -r ~/Projects/matrixgen/sections ubuntu@$SEED_EXTERNAL_IP:/home/ubuntu/.actorchoreography/

#sudo apt-get install libblas-dev liblapack-dev
#gfortran -c *.f
#mkdir bin
#gfortran *.o LinearEquations.f90 -o bin/writer -llapack -lblas -ffree-line-length-512 -g -fcheck=all -Wall
#scp -i ~/Projects/aws-nvirgin.cer ../lapack-3.8.0/TESTING/MATGEN/*.f ubuntu@54.204.74.79:/home/ubuntu/.actorchoreography/
#scp -i ~/Projects/aws-nvirgin.cer ../matrixgen/LinearEquations.f90 ubuntu@54.204.74.79:/home/ubuntu/.actorchoreography/

#for ip in $(cat cluster-ips.txt )
#do
#    #rm -rf $ip
#done