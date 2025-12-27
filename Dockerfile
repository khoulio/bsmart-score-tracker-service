FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Utiliser Debian-based image (compatible ARM64 et x86_64)
FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app

# Install Chrome/Chromium for scraping (UNIQUEMENT si mode local)
# En mode remote (Selenium Grid), ces packages ne sont PAS nécessaires
# Commenté par défaut pour réduire la taille de l'image et le temps de build
# Décommenter si vous souhaitez utiliser le mode local
# RUN apt-get update && apt-get install -y --no-install-recommends \
#     wget \
#     gnupg \
#     ca-certificates \
#     fonts-liberation \
#     libnss3 \
#     libxss1 \
#     libasound2 \
#     libatk-bridge2.0-0 \
#     libgtk-3-0 \
#     libgbm1 \
#     && rm -rf /var/lib/apt/lists/*
#
# RUN apt-get update && \
#     (apt-get install -y chromium chromium-driver 2>/dev/null || \
#      (apt-get install -y chromium-browser chromium-chromedriver 2>/dev/null || \
#       echo "Chromium not available in repos, scraping will be disabled")) && \
#     rm -rf /var/lib/apt/lists/*
#
# # Set Chrome environment variables (uniquement pour mode local)
# ENV CHROME_BIN=/usr/bin/chromium
# ENV CHROMEDRIVER_BIN=/usr/bin/chromedriver
# ENV PATH="/usr/lib/chromium-browser:${PATH}"

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "app.jar"]
