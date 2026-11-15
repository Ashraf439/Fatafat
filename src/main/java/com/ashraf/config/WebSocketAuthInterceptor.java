package com.ashraf.config;

import com.ashraf.customer.security.CustomUserDetails;
import com.ashraf.shared.security.UserDetailsServiceImpl;
import com.ashraf.shared.utils.JWTUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private static final String ACCESS_TOKEN_COOKIE = "access_token";

    private final JWTUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    public WebSocketAuthInterceptor(JWTUtil jwtUtil, UserDetailsServiceImpl userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) return false;

        String token = readCookie(servletRequest.getServletRequest());
        if (token == null || !jwtUtil.isTokenValid(token)) {
            return false; // rejected — client sees a failed connection
        }

        CustomUserDetails principal = userDetailsService.loadUserByUsername(jwtUtil.extractEmail(token));
        attributes.put("userId", principal.getUser().getId());
        if (principal.getUser().getRider() != null) {
            attributes.put("riderId", principal.getUser().getRider().getId());
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) { }

    private String readCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }
}