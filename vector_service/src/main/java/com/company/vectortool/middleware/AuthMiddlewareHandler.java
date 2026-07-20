package com.company.vectortool.middleware;

import org.springframework.stereotype.Component;
import jakarta.servlet.*;
import java.io.IOException;

@Component
public class AuthMiddlewareHandler implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        chain.doFilter(req, res);
    }
}
