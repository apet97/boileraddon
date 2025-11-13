package com.example.rules.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Wraps {@link PlatformAuthFilter} (or any servlet {@link Filter}) so that authentication
 * is enforced only for selected request paths. This allows us to require bearer tokens
 * on sensitive API endpoints while keeping health/manifest endpoints publicly readable.
 */
public final class ScopedPlatformAuthFilter implements Filter {
    private final Filter delegate;
    private final Set<String> exactPaths;
    private final List<String> protectedPrefixes;

    public ScopedPlatformAuthFilter(Filter delegate,
                                    Set<String> exactPaths,
                                    List<String> protectedPrefixes) {
        this.delegate = Objects.requireNonNull(delegate, "delegate filter is required");
        this.exactPaths = exactPaths == null ? Set.of() : Set.copyOf(exactPaths);
        this.protectedPrefixes = protectedPrefixes == null ? List.of() : List.copyOf(protectedPrefixes);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        delegate.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest)) {
            delegate.doFilter(request, response, chain);
            return;
        }
        String path = pathWithinContext(httpRequest);
        if (shouldProtect(path)) {
            delegate.doFilter(request, response, chain);
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        delegate.destroy();
    }

    private boolean shouldProtect(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        if (exactPaths.contains(path)) {
            return true;
        }
        for (String prefix : protectedPrefixes) {
            if (matchesPrefix(path, prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesPrefix(String path, String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return false;
        }
        if (path.equals(prefix)) {
            return true;
        }
        String normalized = prefix.endsWith("/") ? prefix : prefix + "/";
        if (path.startsWith(normalized)) {
            return true;
        }
        if (prefix.endsWith("/") && path.equals(prefix.substring(0, prefix.length() - 1))) {
            return true;
        }
        return false;
    }

    private static String pathWithinContext(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return null;
        }
        String context = request.getContextPath();
        if (context != null && !context.isBlank() && uri.startsWith(context)) {
            return uri.substring(context.length());
        }
        return uri;
    }
}
