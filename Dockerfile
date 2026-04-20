FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn -B -DskipTests clean install
RUN ls fairsplit-api/target/

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/fairsplit-api/target/fairsplit-api-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
