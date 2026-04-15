FROM azul/zulu-openjdk-debian:21

WORKDIR /app
COPY target/media-transcoder-1.0.0.jar /app/app.jar
COPY src/main/resources/data /app/data

ENV CONDUCTOR_SERVER_URL=http://conductor-server:8080/api
ENV AWS_S3_ENDPOINT=http://minio:9000
ENV AWS_ACCESS_KEY_ID=minioadmin
ENV AWS_SECRET_ACCESS_KEY=minioadmin

CMD ["java", "-jar", "/app/app.jar"]
