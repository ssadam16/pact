package com.technokratos.pact.security.config;

import com.technokratos.pact.auth.handler.DefaultAuthenticationFailureHandler;
import com.technokratos.pact.auth.handler.DefaultAuthenticationSuccessHandler;
import com.technokratos.pact.auth.oauth2.CustomOAuth2UserService;
import com.technokratos.pact.auth.oauth2.HttpCookieOAuth2RequestRepository;
import com.technokratos.pact.auth.oauth2.handler.OAuth2FailureHandler;
import com.technokratos.pact.auth.oauth2.handler.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final DefaultAuthenticationSuccessHandler successHandler;
    private final DefaultAuthenticationFailureHandler failureHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final HttpCookieOAuth2RequestRepository cookieOAuth2RequestRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/auth/**", "/oauth2/**")
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/egg/**",
                                "/error",
                                "/static/**",
                                "/images/**",
                                "/css/**",
                                "/js/**",
                                "/favicon.ico"
                        ).permitAll()
                        .requestMatchers("/auth/**", "/oauth2/**").permitAll()
                        .requestMatchers("/admin").hasRole("ADMIN")
                        .requestMatchers("/moder").hasRole("MODER")
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login")
                        .passwordParameter("password")
                        .usernameParameter("username")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .permitAll()
                )

//                .oauth2Login(oauth2 -> oauth2
//                        .authorizationEndpoint(authorization -> authorization
//                                .baseUri("/oauth2/authorize")
//                                .authorizationRequestRepository(cookieOAuth2RequestRepository)
//                        )
//                        .redirectionEndpoint(redirection -> redirection
//                                .baseUri("/oauth2/callback/*")
//                        )
//                        .userInfoEndpoint(userInfo -> userInfo
//                                .userService(customOAuth2UserService)
//                        )
//                        .successHandler(oAuth2SuccessHandler)
//                        .failureHandler(oAuth2FailureHandler)
//                )

                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/")
                        .permitAll()
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        //todo: спросить у Федора Андреевича че это за метод
        configuration.setAllowedOrigins(List.of(
                "*"
        ));
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE"
        ));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "X-CSRF-TOKEN"
        ));
        configuration.setExposedHeaders(List.of(
                "Authorization",
                "X-CSRF-TOKEN"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {

        return authenticationConfiguration.getAuthenticationManager();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
