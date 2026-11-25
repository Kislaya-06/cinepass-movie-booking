# --- Build stage: compile and package the Spring Boot jar ---
# Maven comes preinstalled: the wrapper's own Maven download failed on Render's builders
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Download dependencies first so code changes don't re-download them
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src/ src/
RUN mvn -B -q package -DskipTests

# --- Run stage: small JRE image, non-root user ---
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 cinepass
COPY --from=build /app/target/moviereservation-*.jar app.jar
USER cinepass

ENV SPRING_PROFILES_ACTIVE=prod
# Render/Railway inject PORT; application.yml (prod) reads it, default 8080
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
