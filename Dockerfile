# ---- build ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
# mvnw pode ter fim de linha do Windows (CRLF); corrige para rodar no Linux
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
# Baixa as dependências numa camada própria (rebuilds mais rápidos)
RUN ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q -DskipTests package

# ---- execução ----
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Pastas gravadas em runtime (uploads do admin, configurações de aparência)
RUN mkdir -p /app/uploads && useradd -r -u 1001 app && chown -R app /app
USER app
ENV SPRING_PROFILES_ACTIVE=render
# O Render define PORT; a aplicação já lê server.port=${PORT:8080}
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]
