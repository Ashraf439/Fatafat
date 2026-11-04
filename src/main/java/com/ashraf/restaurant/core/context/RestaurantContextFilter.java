package com.ashraf.restaurant.core.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;


@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RestaurantContextFilter extends GenericFilterBean {
    public static final String HEADER = "X-Restaurant-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            if (request instanceof HttpServletRequest http) {
                String id = http.getHeader(HEADER);
                if (id != null && !id.isBlank()) {
                    RestaurantContext.set(id.trim());
                }
            }
            chain.doFilter(request, response);
        } finally {
            RestaurantContext.clear();
        }
    }
}