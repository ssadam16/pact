package com.technokratos.pact.common;

import com.technokratos.pact.common.dto.AuthenticatedUserSessionInfo;
import com.technokratos.pact.file.service.AvatarService;
import com.technokratos.pact.user.exception.UserNotFoundException;
import com.technokratos.pact.user.model.User;
import com.technokratos.pact.user.repository.UserRepository;
import com.technokratos.pact.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserSessionInterceptor implements HandlerInterceptor {

    private final AvatarService avatarService;
    private final UserRepository userRepository;

    private final Map<String, Long> lastUpdateCache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = 60000;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !(authentication.getPrincipal() instanceof String)) {

            String username = authentication.getName();
            HttpSession session = request.getSession();

            if (shouldUpdateSession(session, username)) {
                updateUserSession(session, username);
                lastUpdateCache.put(username, System.currentTimeMillis());
                log.debug("User session updated for: {}", username);
            }
        }

        return true;
    }

    private boolean shouldUpdateSession(HttpSession session, String username) {
        if (session.getAttribute("username") == null) {
            return true;
        }

        Long lastUpdate = lastUpdateCache.get(username);
        return lastUpdate == null || System.currentTimeMillis() - lastUpdate > CACHE_DURATION_MS;
    }

    private void updateUserSession(HttpSession session, String username) {
        try {
            User user = userRepository.findByUsername(username)
                            .orElseThrow(() -> UserNotFoundException.byUsername(username));

            AuthenticatedUserSessionInfo info = new AuthenticatedUserSessionInfo(
                    user.getUsername(),
                    user.getEmail(),
                    avatarService.getAvatarUrl(user.getAvatarFilename())
            );

            session.setAttribute("authenticatedUserSessionInfo", info);

        } catch (Exception e) {
            log.error("Failed to update user session: {}", e.getMessage());
        }
    }
}