package com.clockify.addon.sdk.middleware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * SECURITY: Request size limit filter.
 *
 * Prevents DoS attacks via:
 * - Extremely large webhook payloads
 * - Memory exhaustion from big uploads
 * - Unbounded request processing
 *
 * Limits:
 * - Max 10 MB per request (configurable)
 * - Checked before body is read
 * - Fast reject (413 Payload Too Large)
 *
 * Environment variable: MAX_REQUEST_SIZE_MB (default: 10)
 */
public class RequestSizeLimitFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(RequestSizeLimitFilter.class);

    private final long maxSizeBytes;
    private static final long DEFAULT_MAX_SIZE_MB = 10;
    private static final long MB_TO_BYTES = 1024 * 1024;

    /**
     * Creates request size limit filter with default limit (10 MB).
     */
    public RequestSizeLimitFilter() {
        this(DEFAULT_MAX_SIZE_MB);
    }

    /**
     * Creates request size limit filter with custom limit in MB.
     *
     * @param maxSizeMB maximum request size in megabytes
     */
    public RequestSizeLimitFilter(long maxSizeMB) {
        this.maxSizeBytes = maxSizeMB * MB_TO_BYTES;
        logger.info("Request size limit filter initialized: {}MB ({}bytes)", maxSizeMB, maxSizeBytes);
    }

    /**
     * Creates request size limit filter from environment variable.
     *
     * @return filter configured from MAX_REQUEST_SIZE_MB env var or default
     */
    public static RequestSizeLimitFilter fromEnvironment() {
        String maxMbEnv = System.getenv("MAX_REQUEST_SIZE_MB");
        if (maxMbEnv != null && !maxMbEnv.isBlank()) {
            try {
                long maxMb = Long.parseLong(maxMbEnv);
                return new RequestSizeLimitFilter(maxMb);
            } catch (NumberFormatException e) {
                logger.warn("Invalid MAX_REQUEST_SIZE_MB value: {} (using default: {}MB)",
                        maxMbEnv, DEFAULT_MAX_SIZE_MB);
            }
        }
        return new RequestSizeLimitFilter(DEFAULT_MAX_SIZE_MB);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Check Content-Length header
        int contentLength = httpRequest.getContentLength();
        if (contentLength > 0 && contentLength > maxSizeBytes) {
            String clientIp = getClientIp(httpRequest);
            String path = httpRequest.getRequestURI();
            String method = httpRequest.getMethod();

            logger.warn("SECURITY: Request size {} exceeds limit {} from {} ({} {})",
                    contentLength, maxSizeBytes, clientIp, method, path);

            sendSizeExceededError(httpResponse, maxSizeBytes / MB_TO_BYTES);
            return;
        }

        // For requests without Content-Length, wrap the input stream to check size
        ServletRequest wrappedRequest = new SizeLimitedRequestWrapper(httpRequest, maxSizeBytes);

        try {
            chain.doFilter(wrappedRequest, response);
        } catch (IOException e) {
            // Check if size limit was exceeded
            if (e.getCause() instanceof SizeLimitExceededException) {
                logger.warn("SECURITY: Request size exceeded limit {} during processing from {}",
                        maxSizeBytes, getClientIp(httpRequest));
                sendSizeExceededError(httpResponse, maxSizeBytes / MB_TO_BYTES);
            } else {
                // Re-throw other IOExceptions
                throw e;
            }
        }
    }

    /**
     * Gets client IP address, accounting for proxies.
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip != null ? ip : "unknown";
    }

    /**
     * Sends 413 Payload Too Large response.
     */
    private void sendSizeExceededError(HttpServletResponse response, long maxSizeMB) throws IOException {
        response.setStatus(413);
        response.setContentType("application/json");

        String json = String.format(
                "{\"error\":\"payload_too_large\"," +
                "\"message\":\"Request exceeds maximum size of %d MB\"," +
                "\"max_size_mb\":%d}",
                maxSizeMB, maxSizeMB);

        response.getWriter().write(json);
        response.getWriter().flush();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("Request size limit filter initialized");
    }

    @Override
    public void destroy() {
        logger.info("Request size limit filter destroyed");
    }

    /**
     * Exception thrown when request size exceeds limit.
     */
    public static class SizeLimitExceededException extends ServletException {
        public SizeLimitExceededException(String message) {
            super(message);
        }
    }

    /**
     * Wrapper that tracks request size during reading.
     */
    private static class SizeLimitedRequestWrapper extends jakarta.servlet.http.HttpServletRequestWrapper {
        private final long maxSize;
        private long bytesRead = 0;

        SizeLimitedRequestWrapper(HttpServletRequest request, long maxSize) {
            super(request);
            this.maxSize = maxSize;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            ServletInputStream wrapped = super.getInputStream();
            return new FilteringServletInputStream(wrapped, maxSize);
        }
    }

    /**
     * Input stream wrapper that enforces size limit.
     */
    private static class FilteringServletInputStream extends ServletInputStream {
        private final ServletInputStream delegate;
        private final long maxSize;
        private long bytesRead = 0;

        FilteringServletInputStream(ServletInputStream delegate, long maxSize) {
            this.delegate = delegate;
            this.maxSize = maxSize;
        }

        @Override
        public int read() throws IOException {
            if (bytesRead >= maxSize) {
                IOException ioe = new IOException("Request size exceeds limit");
                ioe.initCause(new SizeLimitExceededException("Request size exceeds limit"));
                throw ioe;
            }
            int result = delegate.read();
            if (result != -1) {
                bytesRead++;
            }
            return result;
        }

        @Override
        public int read(byte[] b) throws IOException {
            if (bytesRead >= maxSize) {
                IOException ioe = new IOException("Request size exceeds limit");
                ioe.initCause(new SizeLimitExceededException("Request size exceeds limit"));
                throw ioe;
            }
            int result = delegate.read(b);
            if (result > 0) {
                bytesRead += result;
                if (bytesRead > maxSize) {
                    IOException ioe = new IOException("Request size exceeds limit");
                    ioe.initCause(new SizeLimitExceededException("Request size exceeds limit"));
                    throw ioe;
                }
            }
            return result;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (bytesRead >= maxSize) {
                IOException ioe = new IOException("Request size exceeds limit");
                ioe.initCause(new SizeLimitExceededException("Request size exceeds limit"));
                throw ioe;
            }
            int result = delegate.read(b, off, len);
            if (result > 0) {
                bytesRead += result;
                if (bytesRead > maxSize) {
                    IOException ioe = new IOException("Request size exceeds limit");
                    ioe.initCause(new SizeLimitExceededException("Request size exceeds limit"));
                    throw ioe;
                }
            }
            return result;
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener listener) {
            delegate.setReadListener(listener);
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }
}
