package com.example.rules;

import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.RequestHandler;
import com.clockify.addon.sdk.middleware.SecurityHeadersFilter;
import com.example.rules.web.Nonce;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Renders the IFTTT builder UI for creating webhook-driven automations.
 * Users can select any Clockify webhook as a trigger and compose API actions.
 */
public class IftttController implements RequestHandler {

    @Override
    public HttpResponse handle(HttpServletRequest request) {
        String base = System.getenv().getOrDefault("ADDON_BASE_URL", "");

        // CRITICAL: Use the same nonce that SecurityHeadersFilter puts in CSP header
        // to avoid browser rejecting all scripts/styles due to nonce mismatch
        String nonce = (String) request.getAttribute(SecurityHeadersFilter.CSP_NONCE_ATTR);
        if (nonce == null) {
            // Fallback for tests that don't run SecurityHeadersFilter
            nonce = Nonce.create();
        }

        String html = generateHtml(base, nonce);
        return HttpResponse.ok(html, "text/html");
    }

    private String generateHtml(String baseUrl, String nonce) {
        return String.format("""
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>IFTTT Builder - Rules</title>
  <style nonce="%s">
    * { box-sizing: border-box; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      margin: 0;
      padding: 16px;
      background: #f5f7fa;
      font-size: 14px;
      color: #333;
    }
    .container { max-width: 1400px; margin: 0 auto; }
    h1 { font-size: 24px; margin: 0 0 16px 0; color: #222; }
    h2 { font-size: 18px; margin: 16px 0 8px 0; color: #444; }
    h3 { font-size: 14px; margin: 12px 0 6px 0; color: #555; font-weight: 600; }

    .header {
      background: white;
      padding: 16px;
      margin-bottom: 16px;
      border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.08);
    }
    .card {
      background: white;
      padding: 16px;
      margin-bottom: 16px;
      border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.08);
    }
    .split-view {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
    }
    @media (max-width: 900px) {
      .split-view { grid-template-columns: 1fr; }
    }

    .pill {
      background: #e3f2fd;
      padding: 4px 10px;
      border-radius: 12px;
      font-size: 12px;
      color: #1565c0;
      display: inline-block;
      margin-right: 6px;
    }
    .pill.success { background: #e8f5e9; color: #2e7d32; }
    .pill.warning { background: #fff3e0; color: #e65100; }

    input[type=text], select, textarea {
      padding: 8px 10px;
      border: 1px solid #d0d0d0;
      border-radius: 6px;
      font-size: 13px;
      width: 100%%;
    }
    input[type=text]:focus, select:focus, textarea:focus {
      outline: none;
      border-color: #1976d2;
      box-shadow: 0 0 0 2px rgba(25, 118, 210, 0.1);
    }

    button {
      padding: 8px 16px;
      font-size: 13px;
      cursor: pointer;
      border: 1px solid #1976d2;
      background: #1976d2;
      color: white;
      border-radius: 6px;
      transition: all 0.2s;
    }
    button:hover { background: #1565c0; border-color: #1565c0; }
    button.secondary { background: #f5f5f5; color: #333; border-color: #ccc; }
    button.secondary:hover { background: #e0e0e0; }
    button.danger { background: #d32f2f; border-color: #d32f2f; }
    button.danger:hover { background: #c62828; }
    button.small { padding: 4px 8px; font-size: 12px; }

    .row { display: flex; gap: 8px; align-items: center; margin-bottom: 12px; flex-wrap: wrap; }
    .field-group { margin-bottom: 12px; }
    .field-group label { display: block; margin-bottom: 4px; font-size: 13px; font-weight: 500; color: #555; }
    .field-group .hint { font-size: 12px; color: #777; margin-top: 2px; }

    .trigger-list, .action-catalog { max-height: 500px; overflow-y: auto; }
    .trigger-item {
      padding: 10px;
      margin-bottom: 6px;
      border: 1px solid #e0e0e0;
      border-radius: 6px;
      cursor: pointer;
      transition: all 0.2s;
    }
    .trigger-item:hover { border-color: #1976d2; background: #f0f7ff; }
    .trigger-item.selected { border-color: #1976d2; background: #e3f2fd; }
    .trigger-item h4 { margin: 0 0 4px 0; font-size: 13px; color: #222; }
    .trigger-item p { margin: 0; font-size: 12px; color: #666; }

    .action-section {
      border: 1px solid #e0e0e0;
      border-radius: 6px;
      padding: 12px;
      margin-bottom: 12px;
      background: #fafafa;
    }
    .action-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
    }

    .endpoint-selector { margin-bottom: 12px; }
    .endpoint-group h4 {
      font-size: 13px;
      margin: 8px 0 4px 0;
      color: #555;
      cursor: pointer;
    }
    .endpoint-group h4:hover { color: #1976d2; }
    .endpoint-list { padding-left: 12px; }
    .endpoint-option {
      padding: 6px 8px;
      margin-bottom: 4px;
      border-radius: 4px;
      cursor: pointer;
      font-size: 12px;
    }
    .endpoint-option:hover { background: #e8e8e8; }
    .endpoint-option.selected { background: #e3f2fd; color: #1565c0; font-weight: 500; }

    .http-preview {
      background: #263238;
      color: #aed581;
      padding: 12px;
      border-radius: 6px;
      font-family: 'Courier New', monospace;
      font-size: 12px;
      white-space: pre-wrap;
      word-break: break-all;
      margin-top: 12px;
    }
    .http-preview .method { color: #81c784; font-weight: bold; }
    .http-preview .path { color: #64b5f6; }
    .http-preview .body { color: #ffb74d; }

    .placeholder-picker {
      background: #fff9e6;
      border: 1px solid #ffe082;
      padding: 8px;
      border-radius: 4px;
      margin-top: 4px;
      font-size: 12px;
    }
    .placeholder-chip {
      display: inline-block;
      background: #fff3cd;
      padding: 2px 6px;
      border-radius: 8px;
      margin: 2px;
      cursor: pointer;
      font-size: 11px;
    }
    .placeholder-chip:hover { background: #ffe082; }
    .placeholder-chip.copied { background: #81c784; }

    .console {
      background: #1e1e1e;
      color: #d4d4d4;
      padding: 12px;
      border-radius: 6px;
      font-family: 'Courier New', monospace;
      font-size: 12px;
      max-height: 300px;
      overflow-y: auto;
      white-space: pre-wrap;
    }
    .console .success { color: #4caf50; }
    .console .error { color: #f44336; }

    .muted { color: #777; font-size: 12px; }
    .error { color: #d32f2f; }
    .ok { color: #2e7d32; }

    .search-box {
      margin-bottom: 12px;
      padding: 8px;
      background: #f9f9f9;
      border-radius: 6px;
    }
    .filter-chips { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 8px; }
    .filter-chip {
      padding: 4px 8px;
      background: white;
      border: 1px solid #ddd;
      border-radius: 12px;
      font-size: 12px;
      cursor: pointer;
    }
    .filter-chip.active { background: #1976d2; color: white; border-color: #1976d2; }

    /* Utility classes for CSP compliance */
    .hidden { display: none; }
    .flex-1 { flex: 1; }
    .align-end { align-items: flex-end; }
    .conditions-heading { margin-top: 16px; }
    .endpoint-summary { color: #666; }
    .required-star { color: red; }
    .trigger-event-code { font-size: 11px; color: #999; margin-top: 4px; }
    .trigger-description { margin: 8px 0; font-size: 12px; }
    .trigger-meta { font-size: 12px; color: #666; }
    .endpoint-label-margin { margin-top: 4px; }
    .modal-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0,0,0,0.5);
      z-index: 1000;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .modal-content {
      background: white;
      padding: 20px;
      border-radius: 8px;
      max-width: 800px;
      max-height: 80%%;
      overflow: auto;
    }
  </style>
</head>
<body>
  <div class="container">
    <div class="header">
      <h1>⚡ IFTTT Automation Builder</h1>
      <p class="muted">Create powerful automations: when any Clockify webhook fires, execute custom API actions</p>
      <div class="row">
        <span class="pill">Base: %s</span>
        <button class="secondary small" id="btnBack">← Back to Settings</button>
      </div>
    </div>

    <div class="card">
      <h2>Workspace Configuration</h2>
      <div class="field-group">
        <label>Workspace ID</label>
        <input id="wsid" type="text" placeholder="Enter your workspaceId" />
        <div class="hint">Required for API calls and cache loading</div>
      </div>
      <div class="row">
        <button class="secondary" id="btnLoadCache">Load Workspace Data</button>
        <span id="cacheStatus" class="muted"></span>
      </div>
    </div>

    <div class="split-view">
      <!-- Left: Trigger Panel -->
      <div>
        <div class="card">
          <h2>📥 If This (Trigger)</h2>
          <div class="search-box">
            <input id="triggerSearch" type="text" placeholder="Search webhook events..." />
            <div class="filter-chips" id="triggerCategories"></div>
          </div>
          <div class="trigger-list" id="triggerList">
            <p class="muted">Loading triggers...</p>
          </div>
        </div>

        <div class="card hidden" id="selectedTriggerCard">
          <h3>Selected Trigger</h3>
          <div id="selectedTriggerInfo"></div>
          <h3 class="conditions-heading">Filter Conditions (Optional)</h3>
          <div id="triggerConditions"></div>
          <button class="secondary small" id="btnAddCondition">+ Add Condition</button>
        </div>
      </div>

      <!-- Right: Action Panel -->
      <div>
        <div class="card">
          <h2>📤 Then That (Actions)</h2>
          <div id="actionsList"></div>
          <button class="secondary" id="btnAddAction">+ Add Action</button>
        </div>

        <div class="card">
          <h3>HTTP Preview</h3>
          <div class="http-preview" id="httpPreview">No actions defined yet</div>
        </div>
      </div>
    </div>

    <div class="card">
      <h2>Save & Test</h2>
      <div class="row">
        <div class="field-group flex-1">
          <label>Rule Name</label>
          <input id="ruleName" type="text" placeholder="My IFTTT Automation" />
        </div>
        <div class="row align-end">
          <label><input id="ruleEnabled" type="checkbox" checked /> Enabled</label>
        </div>
      </div>
      <div class="row">
        <button id="btnSaveRule">💾 Save Rule</button>
        <button class="secondary" id="btnTestRule">🧪 Dry Run (Time Entry events only)</button>
        <button class="secondary" id="btnClearForm">🔄 Clear Form</button>
        <span id="saveStatus" class="muted"></span>
      </div>
    </div>

    <div class="card hidden" id="consoleCard">
      <h3>Console Output</h3>
      <div class="console" id="console"></div>
    </div>
  </div>

  <script nonce="%s">
    const baseUrl = '%s';
    let triggers = [];
    let actions = [];
    let endpoints = [];
    let selectedTrigger = null;
    let cacheData = null;
    let actionCounter = 0;

    // Initialize
    document.addEventListener('DOMContentLoaded', () => {
      loadTriggers();
      loadActions();

      // Bind event listeners (CSP-compliant, no inline handlers)
      document.getElementById('btnBack').addEventListener('click', () => {
        window.location.href = baseUrl + '/settings';
      });
      document.getElementById('btnLoadCache').addEventListener('click', loadCache);
      document.getElementById('triggerSearch').addEventListener('input', filterTriggers);
      document.getElementById('btnAddCondition').addEventListener('click', addCondition);
      document.getElementById('btnAddAction').addEventListener('click', addAction);
      document.getElementById('btnSaveRule').addEventListener('click', saveRule);
      document.getElementById('btnTestRule').addEventListener('click', testRule);
      document.getElementById('btnClearForm').addEventListener('click', clearForm);
    });

    function loadTriggers() {
      fetch(baseUrl + '/api/catalog/triggers')
        .then(r => r.json())
        .then(data => {
          triggers = data.triggers || [];
          renderTriggers();
          renderTriggerCategories();
        })
        .catch(e => {
          log('Failed to load triggers: ' + e.message, 'error');
        });
    }

    function loadActions() {
      fetch(baseUrl + '/api/catalog/actions')
        .then(r => r.json())
        .then(data => {
          endpoints = data.tags || [];
          log('Loaded ' + data.count + ' endpoints from OpenAPI spec', 'success');
        })
        .catch(e => {
          log('Failed to load actions catalog: ' + e.message, 'error');
        });
    }

    function loadCache() {
      const wsid = document.getElementById('wsid').value.trim();
      if (!wsid) {
        alert('Please enter Workspace ID');
        return;
      }

      document.getElementById('cacheStatus').textContent = 'Loading...';
      fetch(baseUrl + '/api/cache/data?workspaceId=' + encodeURIComponent(wsid))
        .then(r => r.json())
        .then(data => {
          cacheData = data;
          document.getElementById('cacheStatus').innerHTML =
            '<span class="ok">✓ Loaded: ' + (data.projects?.length || 0) + ' projects, ' +
            (data.tags?.length || 0) + ' tags, ' + (data.clients?.length || 0) + ' clients</span>';
        })
        .catch(e => {
          document.getElementById('cacheStatus').innerHTML = '<span class="error">Failed to load cache</span>';
        });
    }

    function renderTriggerCategories() {
      const categories = [...new Set(triggers.map(t => t.category))].sort();
      const container = document.getElementById('triggerCategories');
      container.innerHTML = '';

      categories.forEach(cat => {
        const chip = document.createElement('span');
        chip.className = 'filter-chip';
        chip.textContent = cat;
        chip.onclick = () => {
          chip.classList.toggle('active');
          filterTriggers();
        };
        container.appendChild(chip);
      });
    }

    function filterTriggers() {
      const searchTerm = document.getElementById('triggerSearch').value.toLowerCase();
      const activeCategories = Array.from(document.querySelectorAll('.filter-chip.active'))
        .map(c => c.textContent);

      const filtered = triggers.filter(t => {
        const matchesSearch = !searchTerm || t.name.toLowerCase().includes(searchTerm) ||
                             t.event.toLowerCase().includes(searchTerm);
        const matchesCategory = activeCategories.length === 0 || activeCategories.includes(t.category);
        return matchesSearch && matchesCategory;
      });

      renderTriggers(filtered);
    }

    function renderTriggers(list = triggers) {
      const container = document.getElementById('triggerList');
      container.innerHTML = '';

      if (list.length === 0) {
        container.innerHTML = '<p class="muted">No triggers found</p>';
        return;
      }

      list.forEach(trigger => {
        const item = document.createElement('div');
        item.className = 'trigger-item' + (selectedTrigger?.event === trigger.event ? ' selected' : '');
        item.innerHTML = `
          <h4>${trigger.name} <span class="pill">${trigger.category}</span></h4>
          <p>${trigger.description}</p>
          <div class="trigger-event-code">Event: <code>${trigger.event}</code></div>
        `;
        item.onclick = () => selectTrigger(trigger);
        container.appendChild(item);
      });
    }

    function selectTrigger(trigger) {
      selectedTrigger = trigger;
      renderTriggers();

      const card = document.getElementById('selectedTriggerCard');
      const info = document.getElementById('selectedTriggerInfo');
      card.classList.remove('hidden');

      info.innerHTML = `
        <div class="pill success">${trigger.name}</div>
        <p class="trigger-description">${trigger.description}</p>
        <div class="trigger-meta">
          <strong>Sample fields:</strong> ${trigger.sampleFields.join(', ')}
        </div>
      `;

      updatePreview();
    }

    function addCondition() {
      const container = document.getElementById('triggerConditions');
      const condId = 'cond-' + Date.now();

      const div = document.createElement('div');
      div.className = 'field-group';
      div.id = condId;
      div.innerHTML = `
        <div class="row">
          <select>
            <option value="jsonPathContains">Field Contains</option>
            <option value="jsonPathEquals">Field Equals</option>
          </select>
          <input type="text" placeholder="Field path (e.g., description, project.name)" class="cond-field-path" />
          <input type="text" placeholder="Value" class="cond-value" />
          <button class="danger small cond-remove">×</button>
        </div>
      `;
      container.appendChild(div);

      // Bind event listeners (CSP-compliant)
      const select = div.querySelector('select');
      const inputs = div.querySelectorAll('input');
      const removeBtn = div.querySelector('.cond-remove');

      select.addEventListener('change', updatePreview);
      inputs.forEach(input => input.addEventListener('input', updatePreview));
      removeBtn.addEventListener('click', () => {
        div.remove();
        updatePreview();
      });
    }

    function addAction() {
      const actionId = 'action-' + (++actionCounter);
      const container = document.getElementById('actionsList');

      const section = document.createElement('div');
      section.className = 'action-section';
      section.id = actionId;
      section.innerHTML = `
        <div class="action-header">
          <h3>Action #${actionCounter}</h3>
          <button class="danger small action-remove">Remove</button>
        </div>
        <div class="field-group">
          <label>API Endpoint</label>
          <button class="secondary small action-pick-endpoint">Select Endpoint</button>
          <div id="${actionId}-endpoint" class="muted endpoint-label-margin">No endpoint selected</div>
        </div>
        <div id="${actionId}-params" class="hidden"></div>
        <div id="${actionId}-body" class="hidden"></div>
        <div class="placeholder-picker hidden" id="${actionId}-placeholders">
          <strong>Insert placeholders:</strong>
          <div id="${actionId}-placeholder-chips"></div>
        </div>
      `;
      container.appendChild(section);

      // Bind event listeners (CSP-compliant)
      section.querySelector('.action-remove').addEventListener('click', () => removeAction(actionId));
      section.querySelector('.action-pick-endpoint').addEventListener('click', () => showEndpointPicker(actionId));

      actions.push({ id: actionId, endpoint: null, params: {}, body: {} });
    }

    function removeAction(actionId) {
      document.getElementById(actionId)?.remove();
      actions = actions.filter(a => a.id !== actionId);
      updatePreview();
    }

    function showEndpointPicker(actionId) {
      const modal = document.createElement('div');
      modal.className = 'modal-overlay';

      const content = document.createElement('div');
      content.className = 'modal-content';
      content.innerHTML = '<h3>Select API Endpoint</h3><div id="endpointCatalog"></div><button class="secondary modal-close">Close</button>';

      modal.appendChild(content);
      document.body.appendChild(modal);

      // Bind close button (CSP-compliant)
      content.querySelector('.modal-close').addEventListener('click', () => modal.remove());

      renderEndpointCatalog('endpointCatalog', actionId, () => modal.remove());
    }

    function renderEndpointCatalog(containerId, actionId, onSelect) {
      const container = document.getElementById(containerId);
      container.innerHTML = '';

      endpoints.forEach(tagGroup => {
        const group = document.createElement('div');
        group.className = 'endpoint-group';

        const header = document.createElement('h4');
        header.textContent = `${tagGroup.tag} (${tagGroup.endpoints.length})`;
        header.onclick = () => {
          const list = group.querySelector('.endpoint-list');
          list.classList.toggle('hidden');
        };
        group.appendChild(header);

        const list = document.createElement('div');
        list.className = 'endpoint-list hidden';

        tagGroup.endpoints.forEach(ep => {
          const option = document.createElement('div');
          option.className = 'endpoint-option';
          option.innerHTML = `<strong>${ep.method}</strong> ${ep.path}<br><span class="endpoint-summary">${ep.summary}</span>`;
          option.onclick = () => {
            selectEndpoint(actionId, ep);
            onSelect();
          };
          list.appendChild(option);
        });

        group.appendChild(list);
        container.appendChild(group);
      });
    }

    function selectEndpoint(actionId, endpoint) {
      const action = actions.find(a => a.id === actionId);
      if (!action) return;

      action.endpoint = endpoint;
      action.params = {};
      action.body = {};

      document.getElementById(actionId + '-endpoint').innerHTML =
        `<strong>${endpoint.method}</strong> ${endpoint.path}<br><span class="endpoint-summary">${endpoint.summary}</span>`;

      // Render parameter inputs
      if (endpoint.parameters && endpoint.parameters.length > 0) {
        const paramsDiv = document.getElementById(actionId + '-params');
        paramsDiv.className = '';
        paramsDiv.innerHTML = '<h4>Parameters</h4>';

        endpoint.parameters.forEach(param => {
          const field = document.createElement('div');
          field.className = 'field-group';
          field.innerHTML = `
            <label>${param.name} ${param.required ? '<span class="required-star">*</span>' : ''}</label>
            <input type="text" placeholder="${param.description || param.type}"
                   data-param="${param.name}" data-in="${param.in}" class="action-param-input" />
            <div class="hint">${param.in}: ${param.type}</div>
          `;
          paramsDiv.appendChild(field);

          // Bind event listener (CSP-compliant)
          const input = field.querySelector('.action-param-input');
          input.addEventListener('input', (e) => {
            updateActionParam(actionId, param.name, param.in, e.target.value);
          });
        });
      }

      // Render body fields
      if (endpoint.hasRequestBody && endpoint.requestBodySchema) {
        const bodyDiv = document.getElementById(actionId + '-body');
        bodyDiv.className = '';
        bodyDiv.innerHTML = '<h4>Request Body</h4>';

        const fields = endpoint.requestBodySchema.fields || [];
        fields.forEach(field => {
          const fieldDiv = document.createElement('div');
          fieldDiv.className = 'field-group';
          fieldDiv.innerHTML = `
            <label>${field.name} ${field.required ? '<span class="required-star">*</span>' : ''}</label>
            <input type="text" placeholder="${field.description || field.type}"
                   class="action-body-input"
                   data-field="${field.name}" />
            <div class="hint">${field.type}</div>
          `;
          bodyDiv.appendChild(fieldDiv);

          // Bind event listener (CSP-compliant)
          const input = fieldDiv.querySelector('.action-body-input');
          input.addEventListener('input', (e) => {
            updateActionBody(actionId, field.name, e.target.value);
          });
        });
      }

      // Show placeholder picker if trigger selected
      if (selectedTrigger) {
        const placeholdersDiv = document.getElementById(actionId + '-placeholders');
        placeholdersDiv.className = 'placeholder-picker';

        const chipsDiv = document.getElementById(actionId + '-placeholder-chips');
        chipsDiv.innerHTML = '';
        selectedTrigger.sampleFields.forEach(field => {
          const chip = document.createElement('span');
          chip.className = 'placeholder-chip';
          chip.textContent = '{{' + field + '}}';
          chip.onclick = () => {
            // Copy to clipboard or show hint
            navigator.clipboard?.writeText('{{' + field + '}}');
            chip.classList.add('copied');
            setTimeout(() => chip.classList.remove('copied'), 500);
          };
          chipsDiv.appendChild(chip);
        });
      }

      updatePreview();
    }

    function updateActionParam(actionId, paramName, paramIn, value) {
      const action = actions.find(a => a.id === actionId);
      if (action) {
        if (!action.params) action.params = {};
        action.params[paramName] = { in: paramIn, value };
        updatePreview();
      }
    }

    function updateActionBody(actionId, fieldName, value) {
      const action = actions.find(a => a.id === actionId);
      if (action) {
        if (!action.body) action.body = {};
        action.body[fieldName] = value;
        updatePreview();
      }
    }

    function updatePreview() {
      const preview = document.getElementById('httpPreview');
      if (actions.length === 0) {
        preview.textContent = 'No actions defined yet';
        return;
      }

      let previewText = '';
      actions.forEach((action, idx) => {
        if (!action.endpoint) {
          previewText += `Action ${idx + 1}: (no endpoint selected)\\n\\n`;
          return;
        }

        const ep = action.endpoint;
        let path = ep.path;

        // Substitute path params
        if (action.params) {
          Object.keys(action.params).forEach(key => {
            const p = action.params[key];
            if (p.in === 'path') {
              path = path.replace('{' + key + '}', p.value || '{' + key + '}');
            }
          });
        }

        previewText += `<span class="method">${ep.method}</span> <span class="path">${path}</span>\\n`;

        // Query params
        const queryParams = Object.keys(action.params || {})
          .filter(k => action.params[k].in === 'query')
          .map(k => k + '=' + (action.params[k].value || ''));
        if (queryParams.length > 0) {
          previewText += 'Query: ' + queryParams.join('&') + '\\n';
        }

        // Body
        if (Object.keys(action.body || {}).length > 0) {
          previewText += '<span class="body">Body: ' + JSON.stringify(action.body, null, 2) + '</span>\\n';
        }

        previewText += '\\n';
      });

      preview.innerHTML = previewText;
    }

    function saveRule() {
      const wsid = document.getElementById('wsid').value.trim();
      const ruleName = document.getElementById('ruleName').value.trim();
      const enabled = document.getElementById('ruleEnabled').checked;

      if (!wsid) {
        alert('Please enter Workspace ID');
        return;
      }
      if (!selectedTrigger) {
        alert('Please select a trigger');
        return;
      }
      if (!ruleName) {
        alert('Please enter a rule name');
        return;
      }

      // Build rule JSON
      const rule = {
        name: ruleName,
        enabled: enabled,
        trigger: {
          event: selectedTrigger.event,
          conditions: []
        },
        actions: []
      };

      // Extract conditions
      const conditionEls = document.querySelectorAll('#triggerConditions .field-group');
      conditionEls.forEach(el => {
        const selects = el.querySelectorAll('select');
        const inputs = el.querySelectorAll('input');
        if (selects[0] && inputs[0] && inputs[1]) {
          rule.trigger.conditions.push({
            type: selects[0].value,
            path: inputs[0].value,
            value: inputs[1].value
          });
        }
      });

      // Extract actions
      actions.forEach(action => {
        if (action.endpoint) {
          rule.actions.push({
            type: 'openapi_call',
            endpoint: {
              method: action.endpoint.method,
              path: action.endpoint.path,
              operationId: action.endpoint.operationId
            },
            params: action.params || {},
            body: action.body || {}
          });
        }
      });

      // Save rule
      document.getElementById('saveStatus').textContent = 'Saving...';
      fetch(baseUrl + '/api/rules?workspaceId=' + encodeURIComponent(wsid), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(rule)
      })
        .then(r => r.json())
        .then(data => {
          document.getElementById('saveStatus').innerHTML = '<span class="ok">✓ Saved successfully</span>';
          log('Rule saved: ' + JSON.stringify(data, null, 2), 'success');
        })
        .catch(e => {
          document.getElementById('saveStatus').innerHTML = '<span class="error">Failed to save</span>';
          log('Save failed: ' + e.message, 'error');
        });
    }

    function testRule() {
      log('Dry-run functionality coming soon (requires sample payload)', 'warning');
    }

    function clearForm() {
      if (confirm('Clear all fields?')) {
        selectedTrigger = null;
        actions = [];
        actionCounter = 0;
        document.getElementById('selectedTriggerCard').classList.add('hidden');
        document.getElementById('actionsList').innerHTML = '';
        document.getElementById('ruleName').value = '';
        renderTriggers();
        updatePreview();
      }
    }

    function log(message, type = 'info') {
      const consoleCard = document.getElementById('consoleCard');
      const consoleDiv = document.getElementById('console');
      consoleCard.classList.remove('hidden');

      const timestamp = new Date().toLocaleTimeString();
      const className = type === 'error' ? 'error' : (type === 'success' ? 'success' : '');
      consoleDiv.innerHTML += `<div class="${className}">[${timestamp}] ${message}</div>`;
      consoleDiv.scrollTop = consoleDiv.scrollHeight;
    }
  </script>
</body>
</html>
""", nonce, nonce, baseUrl, baseUrl);
    }
}
