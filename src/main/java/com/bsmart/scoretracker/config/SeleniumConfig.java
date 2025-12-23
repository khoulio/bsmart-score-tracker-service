package com.bsmart.scoretracker.config;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.net.MalformedURLException;
import java.net.URL;

@Configuration
@Slf4j
public class SeleniumConfig {

    /**
     * selenium.enabled=true/false : active ou non Selenium
     * selenium.mode=local|remote  : local = ChromeDriver, remote = Selenium Grid
     * selenium.remote-url         : URL du Grid (ex: http://selenium:4444/wd/hub ou http://selenium:4444)
     */
    @Value("${selenium.mode:local}")
    private String mode;

    @Value("${selenium.remote-url:}")
    private String remoteUrl;

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
        ChromeOptions options = buildChromeOptions();

        if ("remote".equalsIgnoreCase(mode)) {
            return buildRemoteWebDriver(options);
        }

        return buildLocalChromeDriver(options);
    }

    private ChromeOptions buildChromeOptions() {
        ChromeOptions options = new ChromeOptions();

        // Si tu veux forcer un binaire Chrome en LOCAL uniquement
        String chromeBin = System.getenv("CHROME_BIN");
        if (chromeBin != null && !chromeBin.isEmpty()) {
            options.setBinary(chromeBin);
            log.info("Using Chrome binary: {}", chromeBin);
        }

        if (headless) {
            options.addArguments("--headless=new");
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

        options.addArguments("--window-size=" + windowSize);

        // Garde seulement ce qui est utile (le reste peut être conservé)
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-software-rasterizer");

        // ⚠️ remote-debugging-port peut poser souci en remote (inutile)
        // options.addArguments("--remote-debugging-port=9222");

        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        log.info("Chrome options prepared: headless={}, noSandbox={}, disableDevShmUsage={}",
                headless, noSandbox, disableDevShmUsage);

        return options;
    }

    private WebDriver buildRemoteWebDriver(ChromeOptions options) {
        if (remoteUrl == null || remoteUrl.isBlank()) {
            throw new IllegalStateException("selenium.mode=remote mais selenium.remote-url est vide. " +
                    "Ex: http://selenium:4444/wd/hub (docker) ou http://localhost:4444/wd/hub (local)");
        }

        try {
            log.info("Initializing REMOTE WebDriver (Selenium Grid) with url={}", remoteUrl);
            RemoteWebDriver driver = new RemoteWebDriver(new URL(remoteUrl), options);
            log.info("Remote WebDriver initialized successfully");
            return driver;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("selenium.remote-url invalide: " + remoteUrl, e);
        } catch (Exception e) {
            log.error("Failed to initialize Remote WebDriver: {}", e.getMessage(), e);
            throw e;
        }
    }

    private WebDriver buildLocalChromeDriver(ChromeOptions options) {
        try {
            log.info("Initializing LOCAL ChromeDriver");
            ChromeDriver driver = new ChromeDriver(options);
            log.info("Local ChromeDriver initialized successfully");
            return driver;
        } catch (Exception e) {
            log.error("Failed to initialize Local ChromeDriver: {}", e.getMessage(), e);
            throw e;
        }
    }
}
