package com.technokratos.pact.auth.handler;

import com.technokratos.pact.security.exception.DisabledAccountException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class DefaultAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest req,
                                        HttpServletResponse resp,
                                        AuthenticationException exception) throws IOException {

        req.getSession().removeAttribute("error");

        String errorMsg;

        if (exception instanceof BadCredentialsException) {
            errorMsg = "Неверный логин или пароль";
        } else if (exception instanceof DisabledAccountException) {
            errorMsg = "Аккаунт был удален";
        } else if (exception instanceof InternalAuthenticationServiceException) {
            Throwable cause = exception.getCause();
            if (cause instanceof DisabledAccountException) {
                errorMsg = "Аккаунт был удален";
            } else {
                errorMsg = "Ошибка аутентификации";
            }
        } else {
            errorMsg = "Ошибка аутентификации";
        }

        req.getSession().setAttribute("error", errorMsg);
        resp.sendRedirect("/auth/login?error=true");
    }
}
