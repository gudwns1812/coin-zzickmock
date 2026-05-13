FROM eclipse-temurin:17-jre AS runtime-base

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app app

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM runtime-base AS jar-runtime

COPY app/build/libs/app.jar /app/app.jar

FROM eclipse-temurin:17-jdk AS source-build

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY core ./core
COPY app ./app
COPY stream ./stream
COPY storage ./storage
COPY external ./external

RUN chmod +x ./gradlew && ./gradlew :app:bootJar --no-daemon

FROM runtime-base AS source-runtime

COPY --from=source-build /workspace/app/build/libs/app.jar /app/app.jar
