package com.bsmart.scoretracker.scraper;

import com.bsmart.scoretracker.dto.MatchSnapshot;
import com.bsmart.scoretracker.model.enums.ProviderType;

public interface MatchScraperProvider {

    /**
     * Returns the provider type this implementation supports
     */
    ProviderType supports();

    /**
     * Fetches match data from the provider's URL
     * @param url The match URL
     * @return MatchSnapshot containing scraped data
     */
    MatchSnapshot fetch(String url);
}
