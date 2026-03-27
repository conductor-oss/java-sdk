FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN ./gradlew :harness:installDist -x test --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/harness/build/install/harness/lib/ ./lib/

ENTRYPOINT ["java", "-cp", "lib/*", "io.orkes.conductor.harness.HarnessMain"]
