FROM maven:3.8.7-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Utiliser Debian-based image (compatible ARM64 et x86_64)
FROM eclipse-temurin:17-jre
WORKDIR /app

# Install Chrome/Chromium for scraping
# Utiliser snap pour avoir une version récente compatible multi-arch
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    gnupg \
    ca-certificates \
    fonts-liberation \
    libnss3 \
    libxss1 \
    libasound2t64 \
    libatk-bridge2.0-0 \
    libgtk-3-0 \
    libgbm1 \
    && rm -rf /var/lib/apt/lists/*

# Installer Chromium via snap ou package Debian selon disponibilité
RUN apt-get update && \
    (apt-get install -y chromium chromium-driver 2>/dev/null || \
     (apt-get install -y chromium-browser chromium-chromedriver 2>/dev/null || \
      echo "Chromium not available in repos, scraping will be disabled")) && \
    rm -rf /var/lib/apt/lists/*

# Set Chrome environment variables (chemin générique)
ENV CHROME_BIN=/usr/bin/chromium
ENV CHROMEDRIVER_BIN=/usr/bin/chromedriver
ENV PATH="/usr/lib/chromium-browser:${PATH}"

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "app.jar"]
