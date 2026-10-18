# ---- Build stage ----
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

# Cache the Gradle wrapper and dependencies before copying sources
COPY gradlew settings.gradle build.gradle gradle.properties ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY src src
RUN ./gradlew --no-daemon shadowJar -x test

# ---- Runtime stage ----
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/build/libs/api_v2-0.1-all.jar app.jar
USER app

EXPOSE 8086
ENTRYPOINT ["java", "-XX:+UseSerialGC", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
