FROM eclipse-temurin:17-jre AS runtime-base

ARG SCOUTER_VERSION=2.21.3
ARG SCOUTER_DOWNLOAD_URL=https://github.com/scouter-project/scouter/releases/download/v${SCOUTER_VERSION}/scouter-all-${SCOUTER_VERSION}.tar.gz

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl tar \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app app \
    && curl -fsSL "${SCOUTER_DOWNLOAD_URL}" -o /tmp/scouter.tgz \
    && tar -xzf /tmp/scouter.tgz -C /tmp \
    && mkdir -p /app/scouter/conf \
    && cp /tmp/scouter/agent.java/scouter.agent.jar /app/scouter/scouter.agent.jar \
    && { \
      echo 'obj_name=coin-zzickmock-backend'; \
      echo 'net_collector_ip=scouter-collector'; \
      echo 'net_collector_udp_port=6100'; \
      echo 'net_collector_tcp_port=6100'; \
    } > /app/scouter/conf/scouter.conf \
    && rm -rf /tmp/scouter /tmp/scouter.tgz \
    && chown -R app:app /app/scouter

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
COPY push-app ./push-app

RUN chmod +x ./gradlew && ./gradlew :app:bootJar --no-daemon

FROM runtime-base AS source-runtime

COPY --from=source-build /workspace/app/build/libs/app.jar /app/app.jar

FROM eclipse-temurin:21-jre AS push-runtime-base

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app app

USER app

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/push-app.jar"]

FROM push-runtime-base AS push-jar-runtime

COPY push-app/build/libs/push-app.jar /app/push-app.jar

FROM eclipse-temurin:17-jdk AS java17-toolchain

FROM eclipse-temurin:21-jdk AS push-source-build

WORKDIR /workspace

COPY --from=java17-toolchain /opt/java/openjdk /opt/java/openjdk-17

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY core ./core
COPY stream ./stream
COPY push-app ./push-app

RUN chmod +x ./gradlew \
    && ./gradlew :push-app:bootJar --no-daemon \
        -Dorg.gradle.java.installations.paths=/opt/java/openjdk,/opt/java/openjdk-17

FROM push-runtime-base AS push-source-runtime

COPY --from=push-source-build /workspace/push-app/build/libs/push-app.jar /app/push-app.jar

FROM eclipse-temurin:8-jre AS scouter-collector-runtime

ARG SCOUTER_VERSION=2.21.3
ARG SCOUTER_DOWNLOAD_URL=https://github.com/scouter-project/scouter/releases/download/v${SCOUTER_VERSION}/scouter-all-${SCOUTER_VERSION}.tar.gz

WORKDIR /home

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl tar \
    && rm -rf /var/lib/apt/lists/* \
    && curl -fsSL "${SCOUTER_DOWNLOAD_URL}" -o /tmp/scouter.tgz \
    && tar -xzf /tmp/scouter.tgz \
    && mv scouter/server scouter-server \
    && rm -rf scouter /tmp/scouter.tgz \
    && groupadd --system app \
    && useradd --system --gid app app \
    && mkdir -p /home/scouter-server/logs /home/scouter-server/database \
    && { \
      echo 'server_id=%SC_SERVER_ID%'; \
      echo ''; \
      echo 'net_http_server_enabled=%NET_HTTP_SERVER_ENABLED%'; \
      echo 'net_http_api_swagger_enabled=%NET_HTTP_API_SWAGGER_ENABLED%'; \
      echo 'net_http_api_enabled=%NET_HTTP_API_ENABLED%'; \
      echo 'mgr_purge_profile_keep_days=%MGR_PURGE_PROFILE_KEEP_DAYS%'; \
      echo 'mgr_purge_xlog_keep_days=%MGR_PURGE_XLOG_KEEP_DAYS%'; \
      echo 'mgr_purge_counter_keep_days=%MGR_PURGE_COUNTER_KEEP_DAYS%'; \
    } > /home/scouter-server/conf/scouter.conf.template \
    && { \
      echo '#!/bin/sh'; \
      echo 'set -eu'; \
      echo 'SC_SERVER_ID="${SC_SERVER_ID:-COIN-ZZICKMOCK-COLLECTOR}"'; \
      echo 'NET_HTTP_SERVER_ENABLED="${NET_HTTP_SERVER_ENABLED:-true}"'; \
      echo 'NET_HTTP_API_SWAGGER_ENABLED="${NET_HTTP_API_SWAGGER_ENABLED:-true}"'; \
      echo 'NET_HTTP_API_ENABLED="${NET_HTTP_API_ENABLED:-true}"'; \
      echo 'MGR_PURGE_PROFILE_KEEP_DAYS="${MGR_PURGE_PROFILE_KEEP_DAYS:-2}"'; \
      echo 'MGR_PURGE_XLOG_KEEP_DAYS="${MGR_PURGE_XLOG_KEEP_DAYS:-5}"'; \
      echo 'MGR_PURGE_COUNTER_KEEP_DAYS="${MGR_PURGE_COUNTER_KEEP_DAYS:-15}"'; \
      echo 'JAVA_OPT="${JAVA_OPT:--Xms256m -Xmx512m}"'; \
      echo 'cp /home/scouter-server/conf/scouter.conf.template /home/scouter-server/conf/scouter.conf'; \
      echo 'sed -i "s/%SC_SERVER_ID%/${SC_SERVER_ID}/g" /home/scouter-server/conf/scouter.conf'; \
      echo 'sed -i "s/%NET_HTTP_SERVER_ENABLED%/${NET_HTTP_SERVER_ENABLED}/g" /home/scouter-server/conf/scouter.conf'; \
      echo 'sed -i "s/%NET_HTTP_API_SWAGGER_ENABLED%/${NET_HTTP_API_SWAGGER_ENABLED}/g" /home/scouter-server/conf/scouter.conf'; \
      echo 'sed -i "s/%NET_HTTP_API_ENABLED%/${NET_HTTP_API_ENABLED}/g" /home/scouter-server/conf/scouter.conf'; \
      echo 'sed -i "s/%MGR_PURGE_PROFILE_KEEP_DAYS%/${MGR_PURGE_PROFILE_KEEP_DAYS}/g" /home/scouter-server/conf/scouter.conf'; \
      echo 'sed -i "s/%MGR_PURGE_XLOG_KEEP_DAYS%/${MGR_PURGE_XLOG_KEEP_DAYS}/g" /home/scouter-server/conf/scouter.conf'; \
      echo 'sed -i "s/%MGR_PURGE_COUNTER_KEEP_DAYS%/${MGR_PURGE_COUNTER_KEEP_DAYS}/g" /home/scouter-server/conf/scouter.conf'; \
      echo 'exec java ${JAVA_OPT} -classpath /home/scouter-server/scouter-server-boot.jar scouter.boot.Boot /home/scouter-server/lib'; \
    } > /home/scouter-server/startup.sh \
    && chmod +x /home/scouter-server/startup.sh \
    && chown -R app:app /home/scouter-server

WORKDIR /home/scouter-server

EXPOSE 6180 6188 6100/tcp 6100/udp

ENTRYPOINT ["/home/scouter-server/startup.sh"]
