FROM openjdk:11.0.8-jdk

RUN apt-get update && apt-get install curl -y

COPY ./target/scala-2.12/ActorMatrix-assembly-*.jar /application.jar

ENV WEB_PORT 8080
ENV MY_POD_NAMESPACE default
ENV AKKA_PORT 2551
ENV MY_POD_IP 0.0.0.0
ENV MANAGEMENT_PORT 8558
ENV KAMON_PORT 9225
ENV KAMON_STATUS_PAGE 5266

CMD java \
    -Dcom.sun.management.jmxremote \
    -Dcom.sun.management.jmxremote.ssl=false \
    -Dcom.sun.management.jmxremote.authenticate=false \
    -Dcom.sun.management.jmxremote.port=1099 \
    -Dcom.sun.management.jmxremote.rmi.port=1099 \
    -Djava.rmi.server.hostname=127.0.0.1 \
 -jar application.jar