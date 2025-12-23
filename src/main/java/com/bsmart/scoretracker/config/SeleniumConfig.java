package com.bsmart.scoretracker.config;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
@Slf4j
public class SeleniumConfig {

    @Value("${selenium.chrome.headless:true}")
    private boolean headless;

    @Value("${selenium.chrome.disable-gpu:true}")
    private boolean disableGpu;

    @Value("${selenium.chrome.no-sandbox:true}")
    private boolean noSandbox;

    @Value("${selenium.chrome.disable-dev-shm-usage:true}")
    private boolean disableDevShmUsage;

    @Value("${selenium.chrome.window-size:1920,1080}")
    private String windowSize;

    @Bean
    @Scope("prototype")
    @ConditionalOnProperty(name = "selenium.enabled", havingValue = "true", matchIfMissing = true)
    public WebDriver webDriver() {
        ChromeOptions options = new ChromeOptions();

        // Configuration pour Docker/conteneur
        // Spécifier le binaire Chrome si défini via variable d'environnement
        String chromeBin = System.getenv("CHROME_BIN");
        if (chromeBin != null && !chromeBin.isEmpty()) {
            options.setBinary(chromeBin);
            log.info("Using Chrome binary: {}", chromeBin);
        }

        // Arguments essentiels pour Chrome headless dans Docker
        if (headless) {
            options.addArguments("--headless=new"); // Nouvelle syntaxe headless
            log.info("Chrome headless mode enabled");
        }

        if (disableGpu) {
            options.addArguments("--disable-gpu");
        }

        if (noSandbox) {
            options.addArguments("--no-sandbox");
        }

        if (disableDevShmUsage) {
            options.addArguments("--disable-dev-shm-usage");
        }

        // Options supplémentaires pour stabilité dans Docker
        options.addArguments("--window-size=" + windowSize);
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-software-rasterizer");
        options.addArguments("--disable-setuid-sandbox");
        options.addArguments("--remote-debugging-port=9222");

        // Additional stability options for complex environments
        options.addArguments("--disable-features=TranslateUI");
        options.addArguments("--disable-default-apps");
        options.addArguments("--disable-background-networking");
        options.addArguments("--disable-sync");
        options.addArguments("--metrics-recording-only");
        options.addArguments("--disable-ipc-flooding-protection");
        options.addArguments("--disable-component-extensions-with-background-pages");
        
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        log.info("Initializing Chrome WebDriver with options: headless={}, no-sandbox={}, disable-dev-shm={}, binary={}",
            headless, noSandbox, disableDevShmUsage, chromeBin != null ? chromeBin : "default");

        try {
            ChromeDriver driver = new ChromeDriver(options);
            log.info("Chrome WebDriver initialized successfully");
            return driver;
        } catch (Exception e) {
            log.error("Failed to initialize Chrome WebDriver: {}", e.getMessage(), e);
            throw e;
        }
    }
}
