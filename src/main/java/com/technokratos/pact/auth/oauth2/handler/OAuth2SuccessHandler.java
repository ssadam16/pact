package com.technokratos.pact.auth.oauth2.handler;

import com.technokratos.pact.auth.CookieUtils;
import com.technokratos.pact.auth.oauth2.HttpCookieOAuth2RequestRepository;
import com.technokratos.pact.security.model.UserDetailsImpl;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final HttpCookieOAuth2RequestRepository httpCookieOAuth2RequestRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            log.debug("Response has already been committed. Unable to redirect to {}", targetUrl);
            return;
        }

        clearAuthenticationAttributes(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected @NonNull String determineTargetUrl(HttpServletRequest request,
                                                 HttpServletResponse response,
                                                 Authentication authentication) {

        Optional<String> redirectUri = CookieUtils.getCookie(request,
                        HttpCookieOAuth2RequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue);

        String targetUrl = redirectUri.orElse(getDefaultTargetUrl());

        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();

        return UriComponentsBuilder.fromUriString(targetUrl)

                .build().toUriString();
    }

    private void clearAuthenticationAttributes(HttpServletRequest request,
                                               HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        httpCookieOAuth2RequestRepository.removeAuthorizationRequestCookies(request, response);
    }













}
