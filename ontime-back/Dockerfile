FROM eclipse-temurin:17-jdk
RUN microdnf install -y tzdata && \
    ln -snf /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone
WORKDIR /app
COPY project.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]