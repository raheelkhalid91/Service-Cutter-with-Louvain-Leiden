FROM eclipse-temurin:8-jdk
# Use a prebuilt WAR from the Editor module target directory.
# This avoids building frontend assets inside the image (works around apt/npm issues).
COPY target/editor-0.0.1-SNAPSHOT.war /app/editor.war

EXPOSE 8080
# Use shell form so environment variables like $ENGINE_PORT are expanded by the shell
ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom -jar /app/editor.war --spring.datasource.serverName=db --spring.datasource.username=$POSTGRES_USER --spring.datasource.password=$POSTGRES_PASSWORD --application.links.engine.port=$ENGINE_PORT --application.links.engine.host=$ENGINE_HOST

