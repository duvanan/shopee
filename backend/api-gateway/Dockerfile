FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml /workspace/pom.xml
COPY api-gateway/pom.xml /workspace/api-gateway/pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn -q -f api-gateway/pom.xml -N dependency:go-offline || true
COPY api-gateway /workspace/api-gateway
RUN --mount=type=cache,target=/root/.m2 mvn -q -f api-gateway/pom.xml package -DskipTests

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /workspace/api-gateway/target/api-gateway.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
