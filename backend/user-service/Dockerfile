FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml /workspace/pom.xml
COPY user-service/pom.xml /workspace/user-service/pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn -q -f user-service/pom.xml -N dependency:go-offline || true
COPY user-service /workspace/user-service
RUN --mount=type=cache,target=/root/.m2 mvn -q -f user-service/pom.xml package -DskipTests

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /workspace/user-service/target/user-service.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
