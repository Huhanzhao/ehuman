FROM eclipse-temurin:21-jdk
RUN apt-get update && apt-get install -y unzip
RUN apt-get update && apt-get install -y unzip p7zip-full


WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java","-jar","app.jar"]
