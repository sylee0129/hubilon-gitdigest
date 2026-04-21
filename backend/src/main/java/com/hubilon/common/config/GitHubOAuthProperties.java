package com.hubilon.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("github.oauth")
public record GitHubOAuthProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        String frontendOrigin
) {}
