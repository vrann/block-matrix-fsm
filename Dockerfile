FROM ubuntu:20.04

ENV DEBIAN_FRONTEND=noninteractive

ARG LAPACK_PATH=lapack-3.8.0
COPY ${LAPACK_PATH} /opt/lapack-3.8.0

RUN apt-get update && apt-get install curl gnupg -y
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list
RUN echo "deb http://gb.archive.ubuntu.com/ubuntu/ bionic main universe" >> /etc/apt/sources.list
RUN curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add
#libatlas3-base libopenblas-base
RUN apt-get update && apt-get install openjdk-11-jdk sbt coreutils procps bash libc6-dev libnss3-dev curl vim make gcc gfortran python libgfortran-6-dev -y
# RUN ln -s /usr/lib/x86_64-linux-gnu/atlas/libblas.so.3.10.3 /usr/lib/libblas.so
# RUN ln -s /usr/lib/x86_64-linux-gnu/atlas/liblapack.so.3.10.3 /usr/lib/liblapack.so
# RUN ln -s /usr/lib/x86_64-linux-gnu/atlas/liblapack.so.3.10.3 /usr/lib/liblapack.so.3
# RUN ln -s /usr/lib/x86_64-linux-gnu/atlas/libblas.so.3.10.3 /usr/lib/libblas.so.3
# WORKDIR /opt/lapack-3.8.0
# RUN  ulimit -s unlimited && make && cp -rf liblapack.a /usr/local/lib/ && ls -la BLAS


COPY src /app/src
COPY build.sbt /app
COPY project/assembly.sbt /app/project/assembly.sbt
COPY project/build.properties /app/project/build.properties
COPY docker/application.local2552.conf /app/application.local.conf

WORKDIR /app
RUN sbt assembly
COPY ./target/scala-2.12/actormatrix-assembly-*.jar /app/application.jar

CMD java \
    -Dconfig.file=application.local.conf \
    -jar application.jar
#
# ENV WEB_PORT 8080
# ENV MY_POD_NAMESPACE default
# ENV AKKA_PORT 2551
# ENV MY_POD_IP 0.0.0.0
# ENV MANAGEMENT_PORT 8558
# ENV KAMON_PORT 9225
# ENV KAMON_STATUS_PAGE 5266
#
# CMD java \
#     -Dcom.sun.management.jmxremote \
#     -Dcom.sun.management.jmxremote.ssl=false \
#     -Dcom.sun.management.jmxremote.authenticate=false \
#     -Dcom.sun.management.jmxremote.port=1099 \
#     -Dcom.sun.management.jmxremote.rmi.port=1099 \
#     -Djava.rmi.server.hostname=127.0.0.1 \
#  -jar application.jar