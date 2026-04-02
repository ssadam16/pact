package com.technokratos.pact.auth.oauth2.handler;

import com.technokratos.pact.auth.CookieUtils;
import com.technokratos.pact.auth.oauth2.HttpCookieOAuth2RequestRepository;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final HttpCookieOAuth2RequestRepository httpCookieOAuth2RequestRepository;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        log.error("OAuth2 authentication failed for request: {}", request.getRequestURI(), exception);

        String targetUrl = CookieUtils.getCookie(request,
                        HttpCookieOAuth2RequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue)
                .orElse("/login");

        String errorMessage = resolveErrorMessage(exception);

        targetUrl = UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("error", errorMessage)
                .queryParam("error_code", getErrorCode(exception))
                .build().toUriString();

        httpCookieOAuth2RequestRepository.removeAuthorizationRequestCookies(request, response);

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String resolveErrorMessage(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException) {
            return exception.getMessage();
        }

        String message = exception.getMessage();
        if (message != null) {
            if (message.contains("access_denied")) {
                return "Вы отменили авторизацию";
            } else if (message.contains("user_info_not_found")) {
                return "Не удалось получить информацию о пользователе";
            } else if (message.contains("email_not_found")) {
                return "Провайдер не предоставил email";
            } else if (message.contains("provider_mismatch")) {
                return "Этот email уже зарегистрирован через другой провайдер";
            }
        }

        return "Ошибка при входе через соцсеть";
    }

    private String getErrorCode(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException) {
            return "OAUTH2_ERROR";
        }
        return "AUTHENTICATION_FAILED";
    }
}