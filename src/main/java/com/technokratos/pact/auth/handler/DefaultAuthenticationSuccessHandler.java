package com.technokratos.pact.auth.handler;

import com.technokratos.pact.common.dto.AuthenticatedUserSessionInfo;
import com.technokratos.pact.file.service.AvatarService;
import com.technokratos.pact.security.model.UserDetailsImpl;
import com.technokratos.pact.user.exception.UserNotFoundException;
import com.technokratos.pact.user.model.User;
import com.technokratos.pact.user.repository.UserRepository;
import com.technokratos.pact.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
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
    private final UserRepository userRepository;
    private final AvatarService avatarService;

    public DefaultAuthenticationSuccessHandler(UserService userService,
                                               UserRepository userRepository,
                                               AvatarService avatarService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.avatarService = avatarService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req,
                                        HttpServletResponse resp,
                                        Authentication authentication) throws IOException {

        req.getSession().removeAttribute("error");

        String username = ((UserDetailsImpl) authentication.getPrincipal()).getUsername();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> UserNotFoundException.byUsername(username));

        AuthenticatedUserSessionInfo info = new AuthenticatedUserSessionInfo(
                user.getEmail(),
                user.getUsername(),
                avatarService.getAvatarUrl(user.getAvatarFilename())
        );

        req.getSession().setAttribute("authenticatedUserSessionInfo", info);

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
