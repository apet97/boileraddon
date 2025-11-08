package com.example.rules;

import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.RequestHandler;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Renders the settings UI for the Rules add-on.
 * This is displayed in the sidebar component in Clockify.
 */
public class SettingsController implements RequestHandler {

    @Override
    public HttpResponse handle(HttpServletRequest request) {
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Rules Add-on</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        margin: 0;
                        padding: 20px;
                        background: #f5f5f5;
                    }
                    h1 {
                        font-size: 20px;
                        margin-top: 0;
                        color: #333;
                    }
                    h2 {
                        font-size: 16px;
                        margin-top: 20px;
                        color: #555;
                    }
                    p {
                        font-size: 14px;
                        color: #666;
                        line-height: 1.5;
                    }
                    code {
                        background: #e8e8e8;
                        padding: 2px 6px;
                        border-radius: 3px;
                        font-size: 12px;
                    }
                    .section {
                        background: white;
                        padding: 15px;
                        margin-bottom: 15px;
                        border-radius: 6px;
                        box-shadow: 0 1px 3px rgba(0,0,0,0.1);
                    }
                    ul {
                        font-size: 14px;
                        color: #666;
                    }
                </style>
            </head>
            <body>
                <h1>Rules Automation</h1>

                <div class="section">
                    <h2>Overview</h2>
                    <p>Create automation rules to automatically modify time entries based on conditions.</p>
                    <p>Rules can add/remove tags, set descriptions, and more when time entries match your criteria.</p>
                </div>

                <div class="section">
                    <h2>API Endpoints</h2>
                    <ul>
                        <li><code>GET /api/rules</code> - List all rules</li>
                        <li><code>POST /api/rules</code> - Create or update a rule</li>
                        <li><code>DELETE /api/rules?id={id}</code> - Delete a rule</li>
                        <li><code>POST /api/test</code> - Test rules against sample data</li>
                    </ul>
                </div>

                <div class="section">
                    <h2>Example Rule</h2>
                    <p>Create a rule to tag client meetings as billable:</p>
                    <pre style="background:#f8f8f8;padding:10px;border-radius:4px;overflow-x:auto;"><code>{
  "name": "Tag client meetings",
  "enabled": true,
  "combinator": "AND",
  "conditions": [
    {"type": "descriptionContains", "operator": "CONTAINS", "value": "meeting"},
    {"type": "hasTag", "operator": "EQUALS", "value": "client"}
  ],
  "actions": [
    {"type": "add_tag", "args": {"tag": "billable"}}
  ]
}</code></pre>
                </div>

                <div class="section">
                    <h2>Supported Conditions</h2>
                    <ul>
                        <li><code>descriptionContains</code> - Check if description contains text</li>
                        <li><code>descriptionEquals</code> - Check if description matches exactly</li>
                        <li><code>hasTag</code> - Check if time entry has a specific tag</li>
                        <li><code>projectIdEquals</code> - Check if project matches</li>
                        <li><code>isBillable</code> - Check billable status</li>
                    </ul>
                </div>

                <div class="section">
                    <h2>Supported Actions</h2>
                    <ul>
                        <li><code>add_tag</code> - Add a tag to the time entry</li>
                        <li><code>remove_tag</code> - Remove a tag from the time entry</li>
                        <li><code>set_description</code> - Set the description</li>
                        <li><code>set_billable</code> - Set billable status</li>
                    </ul>
                </div>
            </body>
            </html>
            """;

        return HttpResponse.ok(html, "text/html; charset=utf-8");
    }
}
