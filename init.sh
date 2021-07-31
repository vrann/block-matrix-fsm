apt-get update && apt-get install curl gnupg -y
echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list
echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list
echo "deb http://gb.archive.ubuntu.com/ubuntu/ bionic main universe" >> /etc/apt/sources.list
curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add
apt-get update && apt-get install openjdk-11-jdk sbt coreutils procps bash libc6-dev libnss3-dev curl vim make gcc gfortran python libgfortran-6-dev -y