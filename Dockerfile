FROM eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app

ARG JAR_FILE=build/libs/*.jar
COPY --chown=app:app ${JAR_FILE} app.jar

USER app

EXPOSE 8091

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
