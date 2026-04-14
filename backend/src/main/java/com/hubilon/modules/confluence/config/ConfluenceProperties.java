package com.hubilon.modules.confluence.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "confluence")
public record ConfluenceProperties(
        String baseUrl,
        String userEmail,
        String apiToken,
        String spaceKey,
        String parentPageTitle,
        String parentPageId
) {}
