package com.example.templateaddon;

import com.example.templateaddon.sdk.HttpResponse;
import com.example.templateaddon.sdk.RequestHandler;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Serves HTML for add-on settings or UI components.
 *
 * TODO: Replace the placeholder HTML with your actual UI.
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
                    <p class=\"todo\">TODO: Replace this HTML with your sidebar or settings UI.</p>
                </body>
                </html>
                """;
        return HttpResponse.ok(html, "text/html");
    }
}
