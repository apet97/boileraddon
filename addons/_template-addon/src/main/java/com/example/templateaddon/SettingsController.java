package com.example.templateaddon;

import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.RequestHandler;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Serves HTML for add-on settings or UI components.
 *
 * Replace this placeholder HTML with your actual UI implementation.
 */
public class SettingsController implements RequestHandler {
    @Override
    public HttpResponse handle(HttpServletRequest request) {
        String html = """
                <!DOCTYPE html>
                <html lang=\"en\">
                <head>
                    <meta charset=\"UTF-8\" />
                    <title>Template Add-on</title>
                    <style>
                        body { font-family: sans-serif; padding: 1.5rem; }
                        .todo { background: #fff8e1; border: 1px solid #ffecb3; padding: 1rem; }
                    </style>
                </head>
                <body>
                    <h1>Template Add-on</h1>
                    <p>Welcome to your add-on settings. Customize this UI to match your add-on's functionality.</p>
                </body>
                </html>
                """;
        return HttpResponse.ok(html, "text/html");
    }
}
