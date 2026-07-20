FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
RUN wget -q https://apache.org && \
    tar -xzf maven-mvnd-1.0.6-bin.tar.gz && \
    mv maven-mvnd-1.0.6-bin /opt/mvnd && \
    ln -s /opt/mvnd/bin/mvnd /usr/bin/mvnd
COPY . .
RUN mvnd clean package -DskipTests -Dmvnd.noDaemon=true

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/vector-tool-service-1.0.0.jar app.jar
COPY ServiceConfig.json ServiceConfig.json
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
