package com.appgestion.api.dto.response;

import java.time.Instant;

public record EmailOAuthStatusResponse(
        String emailProvider,
        String oauthProvider,
        boolean oauthConnected,
        Instant oauthConnectedAt,
        String oauthOnFailure,
        String systemFromOverride,
        /** true si el servidor tiene client id + secret de Google (app.email.google.*). */
        boolean googleOAuthConfigured,
        /** true si el servidor tiene client id + secret de Microsoft (app.email.microsoft.*). */
        boolean microsoftOAuthConfigured
) {}
