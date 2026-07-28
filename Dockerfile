FROM maven:3.9.9-eclipse-temurin-8 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:8-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
RUN addgroup --system tucang && adduser --system --ingroup tucang tucang
COPY --from=build /build/target/tu-cang-backend-0.0.1-SNAPSHOT.jar app.jar
USER tucang
EXPOSE 8123
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "/app/app.jar"]
