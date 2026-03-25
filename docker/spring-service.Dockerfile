FROM eclipse-temurin:21-jdk AS builder

ARG MODULE
ARG MODULE_DIR

WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle
COPY libs ./libs
COPY services ./services
COPY e2e-test ./e2e-test

RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN ./gradlew ${MODULE}:bootJar -x test

FROM eclipse-temurin:21-jre

ARG MODULE_DIR

WORKDIR /app

COPY --from=builder /workspace/${MODULE_DIR}/build/libs/*.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

