# Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Run
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/emotes-*.jar app.jar

# Images and the SQLite file live here; mount a volume so they survive deploys.
ENV DATA_DIR=/data
VOLUME /data
EXPOSE 8080

# Headless, or ImageIO's scaling pulls in a display it can't have.
ENV JAVA_TOOL_OPTIONS="-Djava.awt.headless=true -XX:MaxRAMPercentage=75"

ENTRYPOINT ["java", "-jar", "app.jar"]
