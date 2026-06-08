package org.example.backend.service;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.backend.security.jwt.JwtProvider;
import org.example.backend.security.principle.CustomOAuth2User;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        CustomOAuth2User oAuth2User =
                (CustomOAuth2User) authentication.getPrincipal();
        String temporaryToken =
                jwtProvider.generateAccessToken(authentication);

        String clientUrl = "http://localhost:5173";
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("oauth2_client_url".equals(cookie.getName())) {
                    clientUrl = cookie.getValue();
                    break;
                }
            }
        }

        String targetUrl = clientUrl + "/oauth2/redirect?token=" + temporaryToken;

        clearAuthenticationAttributes(request);

        getRedirectStrategy().sendRedirect(
                request,
                response,
                targetUrl
        );
    }
}