FROM maven:3.8.4-openjdk-17

WORKDIR /app
COPY . .

RUN mvn clean package -DskipTests

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=0 /app/target/partner-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 9090
CMD ["java", "-Xmx1024m", "-Xms512m", "--add-opens=java.base/java.nio=ALL-UNNAMED", "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED", "--add-opens=java.base/java.util=ALL-UNNAMED", "-jar", "app.jar"] 