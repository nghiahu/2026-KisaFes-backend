//package org.example.backend.service.impl;
//
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//import org.example.backend.security.jwt.JwtProvider;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//
//@Component
//@RequiredArgsConstructor
//public class OAuth2AuthenticationSuccessHandler
//        extends SimpleUrlAuthenticationSuccessHandler {
//
//    private final JwtProvider jwtProvider;
//
//    @Override
//    public void onAuthenticationSuccess(
//            HttpServletRequest request,
//            HttpServletResponse response,
//            Authentication authentication
//    ) throws IOException {
//
//        CustomUserPrincipal principal =
//                (CustomUserPrincipal) authentication.getPrincipal();
//
//        String accessToken = jwtProvider.generateAccessToken(principal);
//        String refreshToken = jwtProvider.generateRefreshToken(principal);
//
//        String redirectUrl =
//                "http://localhost:3000/oauth2/success"
//                        + "?accessToken=" + accessToken
//                        + "&refreshToken=" + refreshToken;
//
//        getRedirectStrategy().sendRedirect(
//                request,
//                response,
//                redirectUrl
//        );
//    }
//}