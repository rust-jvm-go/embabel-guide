# Standalone Guide backend image
# Uses jammy (not alpine) because ONNX Runtime needs libstdc++
# Expects guide-app.jar to be pre-built and placed in this directory

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY guide-app.jar app.jar

EXPOSE 1337

ENTRYPOINT ["java", "-jar", "app.jar"]