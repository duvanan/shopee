FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml /workspace/pom.xml
COPY config-server/pom.xml /workspace/config-server/pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn -q -f config-server/pom.xml -N dependency:go-offline || true
COPY config-server /workspace/config-server
RUN --mount=type=cache,target=/root/.m2 mvn -q -f config-server/pom.xml package -DskipTests

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /workspace/config-server/target/config-server.jar app.jar
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "app.jar"]
