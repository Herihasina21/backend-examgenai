# Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

COPY src src
RUN ./mvnw -q -B -DskipTests package \
    && mv target/examgenai-backend-*.jar app.jar

# Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app \
    && mkdir -p /tmp/uploads \
    && chown -R app:app /app /tmp/uploads

COPY --from=build /app/app.jar app.jar

USER app
EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod \
    FILE_UPLOAD_DIR=/tmp/uploads \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
