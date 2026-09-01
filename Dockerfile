FROM eclipse-temurin:21-alpine
WORKDIR /app
COPY target/*.jar app.jar
COPY sentiment_feedback.txt sentiment_feedback.txt
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]