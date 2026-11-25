# --- Build stage: compile and package the Spring Boot jar ---
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Download dependencies first so code changes don't re-download them
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -q package -DskipTests

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
