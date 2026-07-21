FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace/backend

COPY backend/pom.xml ./pom.xml
RUN mvn -B -ntp dependency:go-offline

COPY backend/src ./src
RUN mvn -B -ntp clean package -DskipTests

FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install --yes --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --home-dir /app app

WORKDIR /app
COPY --from=build /workspace/backend/target/tiktok-product-insight-backend-*.jar app.jar
COPY database/migrations ./database/migrations
RUN mkdir -p /data/uploads && chown -R app:app /app /data/uploads

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
