package com.technokratos.pact.auth.handler;

import com.technokratos.pact.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class DefaultAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;

    public DefaultAuthenticationSuccessHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req,
                                        HttpServletResponse resp,
                                        Authentication authentication) throws IOException {

        req.getSession().removeAttribute("error");

        RequestCache requestCache = new HttpSessionRequestCache();
        SavedRequest savedRequest = requestCache.getRequest(req, resp);

        if (savedRequest != null) {
            String targetUrl = savedRequest.getRedirectUrl();

            if (!targetUrl.contains("/auth/")) {
                requestCache.removeRequest(req, resp);
                resp.sendRedirect(targetUrl);
                return;
            }
        }

        resp.sendRedirect("/");
    }
}
