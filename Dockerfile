
FROM openjdk:6-jdk

COPY ./gradle/wrapper/gradle-wrapper.jar app.jar

ENTRYPOINT ["java","-jar", "/app.jar"]