package com.example.autotagassistant.sdk;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Embedded Jetty server wrapper.
 */
public class EmbeddedServer {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddedServer.class);
    private final AddonServlet servlet;
    private Server server;

    public EmbeddedServer(AddonServlet servlet) {
        this.servlet = servlet;
    }

    public void start(int port) throws Exception {
        server = new Server(port);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        ServletHolder servletHolder = new ServletHolder(servlet);
        context.addServlet(servletHolder, "/*");

        server.start();
        logger.info("Server started on port {}", port);
        server.join();
    }

    public void stop() throws Exception {
        if (server != null) {
            server.stop();
            logger.info("Server stopped");
        }
    }
}
