package com.hubilon.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gitlab.oauth")
public record GitLabOAuthProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        String frontendOrigin
) {}
