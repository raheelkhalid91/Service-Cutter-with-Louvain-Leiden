FROM eclipse-temurin:8-jdk
COPY target/engine-0.0.1-SNAPSHOT.jar /app/engine.jar

EXPOSE 8080
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app/engine.jar"]