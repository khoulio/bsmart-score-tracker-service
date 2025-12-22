package com.bsmart.scoretracker.scraper;

import com.bsmart.scoretracker.model.enums.ProviderType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ScraperProviderFactory {

    private final Map<ProviderType, MatchScraperProvider> providers;

    public ScraperProviderFactory(List<MatchScraperProvider> providerList) {
        this.providers = providerList.stream()
            .collect(Collectors.toMap(
                MatchScraperProvider::supports,
                Function.identity()
            ));
    }

    public MatchScraperProvider getProvider(ProviderType type) {
        MatchScraperProvider provider = providers.get(type);
        if (provider == null) {
            throw new IllegalArgumentException("No provider found for: " + type);
        }
        return provider;
    }
}
