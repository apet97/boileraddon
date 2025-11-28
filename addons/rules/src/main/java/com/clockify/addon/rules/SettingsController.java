package com.clockify.addon.rules;

import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.RequestHandler;
import com.clockify.addon.sdk.middleware.PlatformAuthFilter;
import com.clockify.addon.sdk.middleware.WorkspaceContextFilter;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Serves HTML for add-on settings or UI components.
 *
 * Replace this placeholder HTML with your actual UI implementation.
 */
public class SettingsController implements RequestHandler {
    private final boolean allowWorkspaceParam;

    public SettingsController() {
        this(false);
    }

    public SettingsController(String environment) {
        this("dev".equalsIgnoreCase(environment));
    }

    private SettingsController(boolean allowWorkspaceParam) {
        this.allowWorkspaceParam = allowWorkspaceParam;
    }

    @Override
    public HttpResponse handle(HttpServletRequest request) {
        String workspaceId = resolveWorkspaceId(request);
        if (workspaceId == null || workspaceId.isBlank()) {
            return HttpResponse.error(401, "{\"error\":\"Valid auth_token required\"}", "application/json");
        }
        String html = """
                <!DOCTYPE html>
                <html lang=\"en\">
                <head>
                    <meta charset=\"UTF-8\" />
                    <title>Rules Add-on</title>
                    <style>
                        :root { color-scheme: light; }
                        * { box-sizing: border-box; }
                        body { margin: 0; font-family: "Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; background: radial-gradient(circle at 20% 20%, #f5f8ff, #ffffff); color: #0f172a; }
                        header { padding: 1.25rem 1.5rem; border-bottom: 1px solid #e2e8f0; background: #fff; }
                        h1 { margin: 0; font-size: 1.4rem; letter-spacing: -0.01em; }
                        main { max-width: 900px; margin: 0 auto; padding: 1.5rem; display: grid; gap: 1rem; }
                        .card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06); padding: 1.25rem; }
                        label { font-weight: 600; display: block; margin-bottom: 0.35rem; color: #0f172a; }
                        input[type=text] { width: 100%; padding: 0.65rem 0.75rem; border: 1px solid #cbd5e1; border-radius: 10px; font-size: 0.95rem; transition: border-color 0.2s, box-shadow 0.2s; }
                        input[type=text]:focus { border-color: #2563eb; outline: none; box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15); }
                        button { border: none; border-radius: 10px; padding: 0.65rem 0.9rem; font-weight: 700; cursor: pointer; transition: transform 0.1s, box-shadow 0.2s, background 0.2s; }
                        button.primary { background: linear-gradient(135deg, #2563eb, #1d4ed8); color: #fff; box-shadow: 0 8px 20px rgba(37, 99, 235, 0.3); }
                        button.primary:hover { transform: translateY(-1px); }
                        button.ghost { background: #f8fafc; color: #0f172a; border: 1px solid #e2e8f0; }
                        .row { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; }
                        .rules { display: grid; gap: 0.75rem; }
                        .rule { padding: 0.8rem 0.9rem; border: 1px solid #e2e8f0; border-radius: 10px; display: flex; align-items: center; justify-content: space-between; }
                        .rule strong { color: #0f172a; }
                        .muted { color: #64748b; font-size: 0.9rem; }
                        .badge { display: inline-block; padding: 0.2rem 0.45rem; border-radius: 999px; background: #eff6ff; color: #1d4ed8; font-weight: 700; font-size: 0.75rem; letter-spacing: 0.01em; }
                        #status { font-size: 0.95rem; }
                        .pill { display: inline-flex; align-items: center; gap: 0.4rem; background: #f1f5f9; border-radius: 999px; padding: 0.35rem 0.75rem; font-size: 0.85rem; color: #0f172a; }
                    </style>
                </head>
                <body>
                    <header>
                        <h1>Rules Automation</h1>
                        <p class=\"muted\">Create simple \"if description contains, then add tag\" rules. Webhook: <span class=\"badge\">TIME_ENTRY_UPDATED</span></p>
                    </header>
                    <main>
                        <section class=\"card\">
                            <h2 style=\"margin-top:0;\">Add rule</h2>
                            <p class=\"muted\">Rules are per-workspace. Matching is case-insensitive and triggers tag application on update webhooks.</p>
                            <div class=\"row\">
                                <div>
                                    <label for=\"matchText\">Description contains</label>
                                    <input id=\"matchText\" type=\"text\" placeholder=\"e.g. meeting\" />
                                </div>
                                <div>
                                    <label for=\"tag\">Apply tag</label>
                                    <input id=\"tag\" type=\"text\" placeholder=\"e.g. meetings\" />
                                </div>
                            </div>
                            <div style=\"margin-top:0.9rem; display:flex; gap:0.5rem; align-items:center;\">
                                <button class=\"primary\" id=\"addRule\">Save rule</button>
                                <span id=\"status\" class=\"muted\">Idle</span>
                            </div>
                        </section>
                        <section class=\"card\">
                            <div style=\"display:flex; align-items:center; justify-content:space-between; gap:0.5rem;\">
                                <h2 style=\"margin:0;\">Current rules</h2>
                                <button class=\"ghost\" id=\"refresh\">Refresh</button>
                            </div>
                            <div class=\"rules\" id=\"rulesList\"></div>
                        </section>
                    </main>
                    <script>
                        const statusEl = document.getElementById('status');
                        const rulesList = document.getElementById('rulesList');
                        const matchInput = document.getElementById('matchText');
                        const tagInput = document.getElementById('tag');
                        const setStatus = (msg, isError=false) => {
                            statusEl.textContent = msg;
                            statusEl.style.color = isError ? '#b91c1c' : '#64748b';
                        };

                        const workspaceParam = () => {
                            const ws = new URLSearchParams(location.search).get('workspaceId');
                            return ws ? `?workspaceId=${encodeURIComponent(ws)}` : '';
                        };

                        async function fetchRules() {
                            setStatus('Loading rules...');
                            try {
                                const res = await fetch('/api/rules' + workspaceParam(), { credentials: 'include' });
                                const data = await res.json();
                                rulesList.innerHTML = '';
                                (data.rules || []).forEach(rule => {
                                    const div = document.createElement('div');
                                    div.className = 'rule';
                                    div.innerHTML = `
                                        <div>
                                            <strong>${rule.matchText}</strong>
                                            <div class=\"muted\">Add tag: <span class=\"pill\">${rule.tag}</span></div>
                                        </div>
                                        <button class=\"ghost\" data-id=\"${rule.id}\">Delete</button>
                                    `;
                                    div.querySelector('button').addEventListener('click', () => deleteRule(rule.id));
                                    rulesList.appendChild(div);
                                });
                                if ((data.rules || []).length === 0) {
                                    rulesList.innerHTML = '<div class=\"muted\">No rules yet.</div>';
                                }
                                setStatus('Ready');
                            } catch (e) {
                                console.error(e);
                                setStatus('Failed to load rules (add workspaceId query param?)', true);
                            }
                        }

                        async function addRule() {
                            const matchText = matchInput.value.trim();
                            const tag = tagInput.value.trim();
                            if (!matchText || !tag) {
                                setStatus('Both fields are required', true);
                                return;
                            }
                            setStatus('Saving...');
                            try {
                                const res = await fetch('/api/rules' + workspaceParam(), {
                                    method: 'POST',
                                    headers: { 'Content-Type': 'application/json' },
                                    credentials: 'include',
                                    body: JSON.stringify({ matchText, tag })
                                });
                                if (!res.ok) throw new Error(await res.text());
                                matchInput.value = '';
                                tagInput.value = '';
                                setStatus('Saved');
                                fetchRules();
                            } catch (e) {
                                console.error(e);
                                setStatus('Failed to save rule', true);
                            }
                        }

                        async function deleteRule(id) {
                            setStatus('Deleting...');
                            try {
                                const suffix = workspaceParam();
                                const res = await fetch(`/api/rules?id=${encodeURIComponent(id)}${suffix ? '&'+suffix.substring(1) : ''}`, { method: 'DELETE', credentials: 'include' });
                                if (!res.ok) throw new Error(await res.text());
                                setStatus('Deleted');
                                fetchRules();
                            } catch (e) {
                                console.error(e);
                                setStatus('Failed to delete rule', true);
                            }
                        }

                        document.getElementById('addRule').addEventListener('click', addRule);
                        document.getElementById('refresh').addEventListener('click', fetchRules);
                        fetchRules();
                    </script>
                </body>
                </html>
                """;
        return HttpResponse.ok(html, "text/html");
    }

    private String resolveWorkspaceId(HttpServletRequest request) {
        Object workspace = request.getAttribute(PlatformAuthFilter.ATTR_WORKSPACE_ID);
        if (workspace instanceof String w && !w.isBlank()) {
            return w;
        }
        Object attr = request.getAttribute(WorkspaceContextFilter.WORKSPACE_ID_ATTR);
        if (attr instanceof String v && !v.isBlank()) {
            return v;
        }
        if (allowWorkspaceParam) {
            String fromParam = request.getParameter("workspaceId");
            if (fromParam != null && !fromParam.isBlank()) {
                return fromParam;
            }
        }
        return null;
    }
}
