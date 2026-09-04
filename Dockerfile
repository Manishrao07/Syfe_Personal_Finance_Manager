FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline
COPY src ./src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN mkdir -p /opt/render/project/src/data
COPY --from=build /app/target/personal-finance-manager.jar app.jar
ENV SPRING_PROFILES_ACTIVE=render
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
