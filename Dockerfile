# Use official Maven image to build the app
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline # Cache dependencies
COPY src ./src
RUN mvn clean package -DskipTests

# Use lightweight JRE image to run the app
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# postgresql-client provides pg_dump/pg_restore, used by the backup/restore endpoints
RUN apk add --no-cache postgresql-client

COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Duser.timezone=Asia/Kolkata", "-jar", "app.jar"]