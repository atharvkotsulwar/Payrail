FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY . .


RUN chmod 755 mvnw


RUN ./mvnw clean package -DskipTests

EXPOSE 8080

CMD ["java", "-jar", "target/*.jar"]
