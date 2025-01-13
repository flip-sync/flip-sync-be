FROM mcr.microsoft.com/openjdk/jdk:17-ubuntu

ARG JAR_FILE=build/libs/flip-sync-server-0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE} flip-sync-server.jar

ENTRYPOINT [ "java", "-jar", "/flip-sync-server.jar", "--spring.profiles.active=prod" ]
