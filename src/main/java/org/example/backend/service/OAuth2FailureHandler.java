package org.example.backend.service;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        // Log the exception to help with debugging the "Agree" failure
        System.out.println("OAuth2 Login Failed: " + exception.getMessage());
        
        String clientUrl = "http://localhost:5173";
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("oauth2_client_url".equals(cookie.getName())) {
                    clientUrl = cookie.getValue();
                    break;
                }
            }
        }
        
        // Redirect back to frontend login page with error parameter
        String targetUrl = clientUrl + "/login?error=oauth2_failed";
        
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
