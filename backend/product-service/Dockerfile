FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml /workspace/pom.xml
COPY product-service/pom.xml /workspace/product-service/pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn -q -f product-service/pom.xml -N dependency:go-offline || true
COPY product-service /workspace/product-service
RUN --mount=type=cache,target=/root/.m2 mvn -q -f product-service/pom.xml package -DskipTests

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /workspace/product-service/target/product-service.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
