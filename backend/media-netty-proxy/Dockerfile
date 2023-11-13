FROM openjdk:11-jre-slim

WORKDIR /home

COPY ./target/media-netty-*.jar /home/app.jar

EXPOSE 9999

ENTRYPOINT ["java", "-Dserver.port=9999", "-Duser.timezone=GMT+08", "-jar",  "app.jar"]
