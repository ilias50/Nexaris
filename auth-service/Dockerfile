# --- ÉTAPE 1 : COMPILATION (BUILD) ---
FROM maven AS build
WORKDIR /app

# Copie du pom.xml et téléchargement des dépendances
COPY pom.xml .
RUN mvn dependency:go-offline

# Copie du code source et compilation
COPY src ./src
RUN mvn clean package

# --- ÉTAPE 2 : EXÉCUTION (RUNTIME) ---
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Récupération du JAR compilé
COPY --from=build /app/target/*.jar app.jar

# Sécurité : On crée un utilisateur non-root pour l'isolation 
RUN addgroup -S spring && adduser -S spring -G spring

# Création du répertoire logs
RUN mkdir -p logs /tmp/nexaris/uploads/profile-images && chown -R spring:spring logs /tmp/nexaris/uploads

USER spring:spring

# Port par défaut pour Spring Boot
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]