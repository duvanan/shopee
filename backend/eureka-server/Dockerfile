FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml /workspace/pom.xml
COPY eureka-server/pom.xml /workspace/eureka-server/pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn -q -f eureka-server/pom.xml -N dependency:go-offline || true
COPY eureka-server /workspace/eureka-server
RUN --mount=type=cache,target=/root/.m2 mvn -q -f eureka-server/pom.xml package -DskipTests

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /workspace/eureka-server/target/eureka-server.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
