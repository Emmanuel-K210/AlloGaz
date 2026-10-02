# --- Build ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# --- Exécution ---
FROM eclipse-temurin:21-jre
ENV TZ=Africa/Abidjan \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Duser.timezone=Africa/Abidjan -Duser.language=fr"
WORKDIR /app
RUN useradd --system --uid 1001 allogaz
COPY --from=build /workspace/target/allogaz-backend-*.jar app.jar
USER allogaz
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
