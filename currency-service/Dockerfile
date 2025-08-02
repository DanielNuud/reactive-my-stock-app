FROM gradle:8.5-jdk17-alpine AS build
WORKDIR /app
COPY . .
RUN gradle clean build --no-daemon

FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=build /app/build/libs/currency-service.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
