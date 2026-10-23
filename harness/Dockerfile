FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN ./gradlew :harness:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/harness/build/libs/harness.jar ./harness.jar

ENTRYPOINT ["java", "-jar", "harness.jar"]
