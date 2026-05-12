# =========================
# Build Stage (JDK) -> contains JDK for building the application, but not needed at runtime
# =========================

FROM eclipse-temurin:21-jdk AS build
# /app is simply a folder inside the Docker container. (Container File System)
WORKDIR /app

# Copy Maven Wrapper and Maven configuration files first
# to leverage Docker layer caching for dependencies.
# Dependencies change less frequently than source code.
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .
# Make Maven Wrapper executable
RUN chmod +x mvnw
# Download dependencies in advance for better layer caching
RUN ./mvnw dependency:go-offline
# Copy application source code
COPY src src
# Build the Spring Boot application and create the executable JAR
RUN ./mvnw clean package

# =========================
# Runtime Stage (JRE) -> contains only runtime and jar file, making it smaller and more secure
# =========================

FROM eclipse-temurin:21-jre
WORKDIR /app
# Copy built jar from build stage
COPY --from=build /app/target/data-processing-batch-0.0.1.jar app.jar
# as external/persistent storage to access output files. When container is removed, data in this volume will persist on the host machine.
VOLUME /app/data
# When the container starts, execute this command directly. (It's the main process of the container like java -jar app.jar)
ENTRYPOINT ["java", "-jar", "app.jar"]

# => Seperating JDK and JRE stages allows us to have a smaller runtime image, improving security and performance.
# The build stage contains the full JDK for compiling the application, while the runtime stage only includes the
# JRE needed to run the application.