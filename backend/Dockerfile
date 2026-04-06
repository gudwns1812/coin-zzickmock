# 1단계: Build Stage
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle clean build -x test

# 2단계: Runtime Stage
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
# 🔽 로그 디렉토리 추가
RUN mkdir -p /stock-logs


# 실행 (환경변수는 application.yml에서 ${ENV_VAR} 형태로 참조)
ENTRYPOINT ["java", "-jar", "app.jar"]