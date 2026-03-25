# Standalone Guide backend image
# Expects guide-app.jar to be pre-built and placed in this directory
# Used by: Cloud Run deployment (via Terraform) and docker compose

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY guide-app.jar app.jar

EXPOSE 1337

ENTRYPOINT ["java", "-jar", "app.jar"]