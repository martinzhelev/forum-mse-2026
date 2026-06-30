FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY src src
COPY frontend/dist frontend/dist

RUN mvn -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/target/forum-0.0.1-SNAPSHOT.jar app.jar
COPY --from=build /workspace/frontend/dist frontend/dist

EXPOSE 9000

ENTRYPOINT ["java", "-jar", "app.jar"]
