# Multi-stage Dockerfile para backend FutManager (Spring Boot 3.3.4 / Java 21)
# Estágio 1: Build com Maven
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

ENV MAVEN_OPTS="-Xmx384m -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Dorg.slf4j.simpleLogger.defaultLogLevel=warn"

# Copia pom.xml e código-fonte
COPY pom.xml .
COPY src ./src

# Compila o JAR de produção sem testes (otimizado para ambientes com pouca memória como Render Free)
RUN mvn clean package -DskipTests -B

# Estágio 2: Imagem de Produção leve (JRE 21 Alpine ~150MB)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Cria usuário não-root por segurança
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copia o JAR empacotado do estágio de build
COPY --from=build /app/target/futmanager-api-*.jar app.jar

# Permissões do diretório de uploads local (fallback de storage)
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app

USER appuser

# Render e outros PaaS injetam a variável $PORT dinamicamente
EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
