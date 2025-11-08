package com.clockify.addon.sdk;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenience wrapper around Jetty for local development of Clockify add-ons.
 * <p>
 * The server installs the provided {@link AddonServlet} under the supplied
 * context path and blocks until shutdown:
 * </p>
 * <pre>{@code
 * AddonServlet servlet = new AddonServlet(addon);
 * EmbeddedServer server = new EmbeddedServer(servlet, "/auto-tag-assistant");
 * server.start(8080);
 * }</pre>
 */
public class EmbeddedServer {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddedServer.class);
    private final AddonServlet servlet;
    private final String contextPath;
    private Server server;

    public EmbeddedServer(AddonServlet servlet) {
        this(servlet, "/");
    }

    public EmbeddedServer(AddonServlet servlet, String contextPath) {
        this.servlet = servlet;
        this.contextPath = contextPath;
    }

    public void start(int port) throws Exception {
        server = new Server(port);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath(contextPath);
        server.setHandler(context);

        ServletHolder servletHolder = new ServletHolder(servlet);
        context.addServlet(servletHolder, "/*");

        server.start();
        logger.info("Server started on port {} with context path {}", port, contextPath);
        server.join();
    }

    public void stop() throws Exception {
        if (server != null) {
            server.stop();
            logger.info("Server stopped");
        }
    }
}
