FROM eclipse-temurin:21-jre
WORKDIR /app
#COPY target/*.jar app.jar
COPY target/data-processing-batch-0.0.1.jar app.jar
VOLUME /app/data
ENTRYPOINT ["java", "-jar", "app.jar"]