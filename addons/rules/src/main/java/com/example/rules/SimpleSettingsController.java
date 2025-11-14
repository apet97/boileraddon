package com.example.rules;

import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.RequestHandler;
import com.clockify.addon.sdk.middleware.SecurityHeadersFilter;
import com.example.rules.config.RuntimeFlags;
import com.example.rules.web.Nonce;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Simplified settings UI with wizard and templates for the Rules add-on.
 * Provides a more user-friendly interface for non-technical users.
 */
public class SimpleSettingsController implements RequestHandler {

    // Base URL is injected from RulesConfiguration; keeps env lookups out of runtime paths.
    private final String baseUrl;

    public SimpleSettingsController(String baseUrl) {
        this.baseUrl = baseUrl == null ? "" : baseUrl;
    }

    @Override
    public HttpResponse handle(HttpServletRequest request) {
        String applyMode = RuntimeFlags.applyChangesEnabled() ? "Apply" : "Log-only";
        String skipSig = RuntimeFlags.skipSignatureVerification()
                ? "ON"
                : (RuntimeFlags.isDevEnvironment() ? "OFF" : "LOCKED");
        String envLabel = RuntimeFlags.environmentLabel();
        String base = this.baseUrl;
        String nonce = (String) request.getAttribute(SecurityHeadersFilter.CSP_NONCE_ATTR);
        if (nonce == null) {
            nonce = Nonce.create();
        }

        String html = """
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <meta http-equiv="Content-Security-Policy" content="default-src 'self'; connect-src 'self'; img-src 'self' data:; frame-ancestors 'self'; style-src 'nonce-%s'; script-src 'nonce-%s'; base-uri 'self'; form-action 'self'" />
  <title>Rules Automation - Simple Builder</title>
  <style nonce="%s">
    :root {
      --primary: #1976d2;
      --primary-light: #e3f2fd;
      --success: #2e7d32;
      --warning: #ed6c02;
      --error: #d32f2f;
      --text: #333;
      --text-light: #666;
      --border: #e0e0e0;
      --background: #fafafa;
      --card-bg: #ffffff;
    }

    * { box-sizing: border-box; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      margin: 0;
      padding: 20px;
      background: var(--background);
      color: var(--text);
      line-height: 1.5;
    }

    .container { max-width: 800px; margin: 0 auto; }

    .header {
      background: var(--card-bg);
      padding: 20px;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
      margin-bottom: 20px;
      border-left: 4px solid var(--primary);
    }

    .header h1 {
      margin: 0 0 8px 0;
      font-size: 24px;
      font-weight: 600;
      color: var(--primary);
    }

    .header p {
      margin: 0;
      color: var(--text-light);
      font-size: 14px;
    }

    .card {
      background: var(--card-bg);
      padding: 20px;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
      margin-bottom: 20px;
    }

    .card h2 {
      margin: 0 0 16px 0;
      font-size: 18px;
      font-weight: 600;
      color: var(--text);
    }

    .form-group { margin-bottom: 16px; }

    .form-group label {
      display: block;
      margin-bottom: 6px;
      font-weight: 500;
      font-size: 14px;
      color: var(--text);
    }

    .form-control {
      width: 100%;
      padding: 10px 12px;
      border: 1px solid var(--border);
      border-radius: 6px;
      font-size: 14px;
      transition: border-color 0.2s;
    }

    .form-control:focus {
      outline: none;
      border-color: var(--primary);
      box-shadow: 0 0 0 2px rgba(25, 118, 210, 0.1);
    }

    .btn {
      padding: 10px 16px;
      border: none;
      border-radius: 6px;
      font-size: 14px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s;
      text-decoration: none;
      display: inline-flex;
      align-items: center;
      gap: 6px;
    }

    .btn-primary {
      background: var(--primary);
      color: white;
    }

    .btn-primary:hover {
      background: #1565c0;
      transform: translateY(-1px);
      box-shadow: 0 4px 8px rgba(25, 118, 210, 0.2);
    }

    .btn-primary:active {
      transform: translateY(0);
      box-shadow: 0 2px 4px rgba(25, 118, 210, 0.2);
    }

    .btn-secondary {
      background: #f5f5f5;
      color: var(--text);
      border: 1px solid var(--border);
    }

    .btn-secondary:hover {
      background: #eeeeee;
      transform: translateY(-1px);
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }

    .btn-secondary:active {
      transform: translateY(0);
      box-shadow: 0 1px 2px rgba(0,0,0,0.1);
    }

    .btn-success { background: var(--success); color: white; }
    .btn-warning { background: var(--warning); color: white; }

    .status-badge {
      display: inline-flex;
      align-items: center;
      padding: 4px 8px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 500;
      background: var(--primary-light);
      color: var(--primary);
    }

    .status-badge.success { background: #e8f5e8; color: var(--success); }
    .status-badge.warning { background: #fff3e0; color: var(--warning); }
    .status-badge.error { background: #ffebee; color: var(--error); }

    .rule-templates {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 16px;
      margin-top: 16px;
    }

    .template-card {
      background: var(--card-bg);
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 16px;
      cursor: pointer;
      transition: all 0.2s;
    }

    .template-card:hover {
      border-color: var(--primary);
      box-shadow: 0 4px 12px rgba(25, 118, 210, 0.15);
      transform: translateY(-2px);
      transition: all 0.3s ease;
    }

    .template-card h4 {
      margin: 0 0 8px 0;
      font-size: 14px;
      font-weight: 600;
    }

    .template-card p {
      margin: 0;
      font-size: 12px;
      color: var(--text-light);
    }

    .wizard-step { display: none; }
    .wizard-step.active { display: block; }

    .condition-row, .action-row {
      display: flex;
      gap: 8px;
      align-items: center;
      margin-bottom: 8px;
      padding: 12px;
      background: #fafafa;
      border-radius: 6px;
    }

    .condition-row select, .action-row select { flex: 1; }
    .condition-row input, .action-row input { flex: 2; }

    .message {
      padding: 12px 16px;
      border-radius: 6px;
      font-size: 14px;
      margin-top: 8px;
      border-left: 4px solid transparent;
      animation: fadeIn 0.3s ease;
    }

    .message.success {
      background: #e8f5e8;
      color: var(--success);
      border-left-color: var(--success);
    }

    .message.error {
      background: #ffebee;
      color: var(--error);
      border-left-color: var(--error);
    }

    .message.info {
      background: #e3f2fd;
      color: var(--primary);
      border-left-color: var(--primary);
    }

    .loading {
      opacity: 0.7;
      pointer-events: none;
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(-10px); }
      to { opacity: 1; transform: translateY(0); }
    }

    .flex { display: flex; gap: 8px; align-items: center; }
    .flex-wrap { flex-wrap: wrap; }
    .justify-between { justify-content: space-between; }
    .mt-16 { margin-top: 16px; }
    .mb-16 { margin-bottom: 16px; }

    @media (max-width: 768px) {
      .condition-row, .action-row { flex-direction: column; align-items: stretch; }
      .condition-row select, .action-row select,
      .condition-row input, .action-row input { width: 100%; }
    }
  </style>
</head>
<body>
  <div class="container">
    <div class="header">
      <h1>🤖 Rules Automation</h1>
      <p>Create simple automation rules for your time entries</p>
      <div class="flex flex-wrap mt-16" style="gap: 12px;">
        <span class="status-badge">Mode: %s</span>
        <span class="status-badge">Signature: %s</span>
        <span class="status-badge">Env: %s</span>
        <span class="status-badge">Base: %s</span>
      </div>
    </div>

    <div class="card">
      <h2>Quick Start</h2>
      <p style="color: var(--text-light); margin-bottom: 20px;">
        Choose a template to get started quickly, or build a custom rule from scratch.
      </p>

      <div class="rule-templates">
        <div class="template-card" data-template="billable">
          <h4>💰 Mark Billable</h4>
          <p>Automatically mark time entries as billable when they contain specific keywords</p>
        </div>

        <div class="template-card" data-template="urgent">
          <h4>🚨 Urgent Tag</h4>
          <p>Add "urgent" tag to time entries that mention urgent or ASAP</p>
        </div>

        <div class="template-card" data-template="meeting">
          <h4>📅 Meeting Notes</h4>
          <p>Standardize meeting descriptions and add meeting tag</p>
        </div>

        <div class="template-card" data-template="custom">
          <h4>⚙️ Custom Rule</h4>
          <p>Build your own rule from scratch</p>
        </div>
      </div>
    </div>

    <div class="card">
      <h2>Create Rule</h2>

      <div class="form-group">
        <label for="workspaceId">Workspace ID</label>
        <input type="text" id="workspaceId" class="form-control" placeholder="Enter your workspace ID" />
      </div>

      <div class="form-group">
        <label for="ruleName">Rule Name</label>
        <input type="text" id="ruleName" class="form-control" placeholder="e.g., Mark billable meetings" />
      </div>

      <div class="form-group">
        <label>
          <input type="checkbox" id="ruleEnabled" checked /> Enable this rule
        </label>
      </div>

      <h3 style="margin: 24px 0 16px 0; font-size: 16px;">Conditions (When to run)</h3>
      <div id="conditions"></div>
      <button class="btn btn-secondary" type="button" id="btnAddCondition">+ Add Condition</button>

      <h3 style="margin: 24px 0 16px 0; font-size: 16px;">Actions (What to do)</h3>
      <div id="actions"></div>
      <button class="btn btn-secondary" type="button" id="btnAddAction">+ Add Action</button>

      <div class="form-group mt-16">
        <button class="btn btn-primary" type="button" id="btnSaveRule">💾 Save Rule</button>
        <button class="btn btn-secondary" type="button" id="btnTestRule">🧪 Test Rule</button>
        <button class="btn btn-secondary" type="button" id="btnShowDebug">🐛 Debug</button>
        <span id="saveMessage" class="message"></span>
      </div>
    </div>

    <div class="card">
      <h2>Your Rules</h2>
      <div class="flex justify-between mb-16">
        <button class="btn btn-secondary" type="button" id="btnRefreshRules">🔄 Refresh Rules</button>
        <span id="rulesCount" class="status-badge">0 rules</span>
      </div>
      <div id="rulesList"></div>
    </div>

    <div class="card">
      <h2>Need Help?</h2>
      <div class="flex flex-wrap" style="gap: 12px;">
        <button class="btn btn-secondary" type="button" id="btnAdvancedBuilder">
          ⚙️ Advanced Builder
        </button>
        <button class="btn btn-secondary" type="button" id="btnIftttBuilder">
          🔗 IFTTT Builder
        </button>
        <button class="btn btn-secondary" type="button" id="btnCopyManifest">
          📋 Copy Manifest
        </button>
      </div>
    </div>
  </div>

  <script nonce="%s">
    const baseUrl = () => {
      const u = new URL(window.location.href);
      let p = u.pathname;
      if (p.endsWith('/simple/')) p = p.slice(0, -7);
      else if (p.endsWith('/simple')) p = p.slice(0, -6);
      if (p.length > 1 && p.endsWith('/')) p = p.slice(0, -1);
      return u.origin + p;
    };

    // Template functions
    function useTemplate(template) {
      const nameInput = document.getElementById('ruleName');
      const conditionsDiv = document.getElementById('conditions');
      const actionsDiv = document.getElementById('actions');

      // Clear existing conditions and actions
      conditionsDiv.innerHTML = '';
      actionsDiv.innerHTML = '';

      switch(template) {
        case 'billable':
          nameInput.value = 'Mark billable meetings';
          addCondition('descriptionContains', 'meeting');
          addAction('set_billable', 'true');
          break;

        case 'urgent':
          nameInput.value = 'Tag urgent items';
          addCondition('descriptionContains', 'urgent');
          addCondition('descriptionContains', 'asap');
          addAction('add_tag', 'urgent');
          break;

        case 'meeting':
          nameInput.value = 'Standardize meetings';
          addCondition('descriptionContains', 'meeting');
          addAction('set_description', 'Meeting: {{original}}');
          addAction('add_tag', 'meeting');
          break;

        case 'custom':
          nameInput.value = 'My custom rule';
          addCondition('descriptionContains', '');
          addAction('add_tag', '');
          break;
      }

      showMessage('Template applied! Customize as needed.', 'success');
    }

    const CONDITION_OPTIONS = [
      { value: 'descriptionContains', label: 'Description contains' },
      { value: 'descriptionEquals', label: 'Description equals' },
      { value: 'hasTag', label: 'Has tag' },
      { value: 'projectIdEquals', label: 'Project ID equals' },
      { value: 'isBillable', label: 'Is billable' }
    ];

    const ACTION_OPTIONS = [
      { value: 'add_tag', label: 'Add tag' },
      { value: 'remove_tag', label: 'Remove tag' },
      { value: 'set_description', label: 'Set description' },
      { value: 'set_billable', label: 'Set billable' }
    ];

    function addCondition(type = 'descriptionContains', value = '') {
      const div = document.createElement('div');
      div.className = 'condition-row';
      const select = document.createElement('select');
      select.className = 'condition-type';
      CONDITION_OPTIONS.forEach(opt => {
        const option = document.createElement('option');
        option.value = opt.value;
        option.textContent = opt.label;
        select.appendChild(option);
      });
      select.value = type;

      const input = document.createElement('input');
      input.type = 'text';
      input.className = 'condition-value';
      input.placeholder = 'Enter value';
      input.value = value || '';

      const remove = document.createElement('button');
      remove.className = 'btn btn-secondary';
      remove.type = 'button';
      remove.textContent = 'Remove';
      remove.addEventListener('click', () => div.remove());

      div.append(select, input, remove);
      document.getElementById('conditions').appendChild(div);
    }

    function addAction(type = 'add_tag', value = '') {
      const div = document.createElement('div');
      div.className = 'action-row';
      const select = document.createElement('select');
      select.className = 'action-type';
      ACTION_OPTIONS.forEach(opt => {
        const option = document.createElement('option');
        option.value = opt.value;
        option.textContent = opt.label;
        select.appendChild(option);
      });
      select.value = type;

      const input = document.createElement('input');
      input.type = 'text';
      input.className = 'action-value';
      input.placeholder = 'Enter value';
      input.value = value || '';

      const remove = document.createElement('button');
      remove.className = 'btn btn-secondary';
      remove.type = 'button';
      remove.textContent = 'Remove';
      remove.addEventListener('click', () => div.remove());

      div.append(select, input, remove);
      document.getElementById('actions').appendChild(div);
    }

    async function saveRule() {
      const saveBtn = document.getElementById('btnSaveRule');
      const originalText = saveBtn.textContent;

      try {
        saveBtn.textContent = '💾 Saving...';
        saveBtn.classList.add('loading');

        const workspaceId = document.getElementById('workspaceId').value.trim();
        const ruleName = document.getElementById('ruleName').value.trim();
        const enabled = document.getElementById('ruleEnabled').checked;

        if (!workspaceId) {
          showMessage('Please enter a workspace ID', 'error');
          return;
        }

        if (!ruleName) {
          showMessage('Please enter a rule name', 'error');
          return;
        }

      // Build conditions
      const conditions = Array.from(document.querySelectorAll('#conditions .condition-row')).map(row => ({
        type: row.querySelector('.condition-type').value,
        operator: 'CONTAINS',
        value: row.querySelector('.condition-value').value
      }));

      // Build actions
      const actions = Array.from(document.querySelectorAll('#actions .action-row')).map(row => {
        const type = row.querySelector('.action-type').value;
        const value = row.querySelector('.action-value').value;

        let args = {};
        if (type === 'add_tag' || type === 'remove_tag') {
          args = { tag: value };
        } else if (type === 'set_description') {
          args = { value: value };
        } else if (type === 'set_billable') {
          args = { value: value };
        }

        return { type, args };
      });

      const payload = {
        name: ruleName,
        enabled,
        conditions,
        actions,
        combinator: 'AND',
        priority: 0
      };

      try {
        const response = await fetch(baseUrl() + `/api/rules?workspaceId=${encodeURIComponent(workspaceId)}`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });

        if (response.ok) {
          showMessage('Rule saved successfully!', 'success');
          loadRules();
        } else {
          const error = await response.text();
          showMessage('Error saving rule: ' + error, 'error');
        }
      } catch (error) {
        showMessage('Error saving rule: ' + error.message, 'error');
      } finally {
        saveBtn.textContent = originalText;
        saveBtn.classList.remove('loading');
      }
    }

    async function testRule() {
      const testBtn = document.getElementById('btnTestRule');
      const originalText = testBtn.textContent;

      try {
        testBtn.textContent = '🧪 Testing...';
        testBtn.classList.add('loading');

        const workspaceId = document.getElementById('workspaceId').value.trim();
        if (!workspaceId) {
          showMessage('Please enter a workspace ID', 'error');
          return;
        }

      // Build test time entry based on current conditions
      const testDescription = 'Test meeting with client';
      const testPayload = {
        workspaceId,
        timeEntry: {
          id: 'test-1',
          description: testDescription,
          tagIds: [],
          billable: false,
          projectId: 'test-project',
          projectName: 'Test Project',
          clientId: 'test-client',
          clientName: 'Test Client'
        }
      };

      try {
        const response = await fetch(baseUrl() + '/api/test', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(testPayload)
        });

        if (response.ok) {
          const result = await response.json();
          let message = `Test completed: ${result.actionsCount} actions would be executed`;

          if (result.actions && result.actions.length > 0) {
            message += '\n\nActions:';
            result.actions.forEach((action, index) => {
              message += `\n${index + 1}. ${action.type}`;
              if (action.args) {
                Object.entries(action.args).forEach(([key, value]) => {
                  message += ` (${key}: ${value})`;
                });
              }
            });
          }

          showMessage(message, 'success');
        } else {
          const error = await response.text();
          showMessage('Error testing rule: ' + error, 'error');
        }
      } catch (error) {
        showMessage('Error testing rule: ' + error.message, 'error');
      } finally {
        testBtn.textContent = originalText;
        testBtn.classList.remove('loading');
      }
    }

    async function loadRules() {
      const refreshBtn = document.getElementById('btnRefreshRules');
      const originalText = refreshBtn.textContent;

      const workspaceId = document.getElementById('workspaceId').value.trim();
      if (!workspaceId) return;

      try {
        refreshBtn.textContent = '🔄 Loading...';
        refreshBtn.classList.add('loading');
        const response = await fetch(baseUrl() + `/api/rules?workspaceId=${encodeURIComponent(workspaceId)}`);
        const rulesList = document.getElementById('rulesList');

        if (response.ok) {
          const rules = await response.json();
          document.getElementById('rulesCount').textContent = `${rules.length} rules`;

          if (rules.length === 0) {
            rulesList.innerHTML = '<p style="color: var(--text-light);">No rules yet. Create your first rule above!</p>';
            return;
          }

          rulesList.innerHTML = '';
          rules.forEach(rule => {
            const wrapper = document.createElement('div');
            wrapper.style.cssText = 'padding:12px;border:1px solid var(--border);border-radius:6px;margin-bottom:8px;';

            const topRow = document.createElement('div');
            topRow.style.cssText = 'display:flex;justify-content:space-between;align-items:center;';

            const info = document.createElement('div');
            const title = document.createElement('strong');
            title.textContent = rule.name || '(untitled)';
            info.appendChild(title);

            const badge = document.createElement('span');
            badge.className = 'status-badge';
            if (rule.enabled) badge.classList.add('success');
            badge.style.marginLeft = '8px';
            badge.textContent = rule.enabled ? 'Enabled' : 'Disabled';
            info.appendChild(badge);

            const deleteBtn = document.createElement('button');
            deleteBtn.className = 'btn btn-secondary';
            deleteBtn.type = 'button';
            deleteBtn.textContent = 'Delete';
            deleteBtn.addEventListener('click', () => deleteRule(rule.id));

            topRow.append(info, deleteBtn);

            const meta = document.createElement('div');
            meta.style.cssText = 'margin-top:8px;font-size:12px;color:var(--text-light);';
            const condCount = (rule.conditions && rule.conditions.length) || 0;
            const actionCount = (rule.actions && rule.actions.length) || 0;
            meta.textContent = `${condCount} conditions • ${actionCount} actions`;

            wrapper.append(topRow, meta);
            rulesList.appendChild(wrapper);
          });
        } else {
          rulesList.innerHTML = '<p style="color: var(--error);">Error loading rules</p>';
        }
      } catch (error) {
        document.getElementById('rulesList').innerHTML = '<p style="color: var(--error);">Error loading rules</p>';
      } finally {
        refreshBtn.textContent = originalText;
        refreshBtn.classList.remove('loading');
      }
    }

    async function deleteRule(ruleId) {
      const workspaceId = document.getElementById('workspaceId').value.trim();
      if (!workspaceId || !ruleId) return;

      try {
        const response = await fetch(baseUrl() + `/api/rules?workspaceId=${encodeURIComponent(workspaceId)}&id=${encodeURIComponent(ruleId)}`, {
          method: 'DELETE'
        });

        if (response.ok) {
          showMessage('Rule deleted successfully!', 'success');
          loadRules();
        } else {
          showMessage('Error deleting rule', 'error');
        }
      } catch (error) {
        showMessage('Error deleting rule: ' + error.message, 'error');
      }
    }

    function showDebug() {
      const workspaceId = document.getElementById('workspaceId').value.trim();
      const ruleName = document.getElementById('ruleName').value.trim();
      const enabled = document.getElementById('ruleEnabled').checked;

      // Build conditions
      const conditions = Array.from(document.querySelectorAll('#conditions .condition-row')).map(row => ({
        type: row.querySelector('.condition-type').value,
        operator: 'CONTAINS',
        value: row.querySelector('.condition-value').value
      }));

      // Build actions
      const actions = Array.from(document.querySelectorAll('#actions .action-row')).map(row => {
        const type = row.querySelector('.action-type').value;
        const value = row.querySelector('.action-value').value;

        let args = {};
        if (type === 'add_tag' || type === 'remove_tag') {
          args = { tag: value };
        } else if (type === 'set_description') {
          args = { value: value };
        } else if (type === 'set_billable') {
          args = { value: value };
        }

        return { type, args };
      });

      const ruleConfig = {
        workspaceId: workspaceId || '(not set)',
        name: ruleName || '(not set)',
        enabled,
        conditions,
        actions,
        combinator: 'AND',
        priority: 0
      };

      const debugMessage = `Current Rule Configuration:\n\n${JSON.stringify(ruleConfig, null, 2)}`;
      showMessage(debugMessage, 'info');
    }

    function showMessage(message, type) {
      const messageEl = document.getElementById('saveMessage');
      messageEl.textContent = message;
      messageEl.className = `message ${type}`;
      setTimeout(() => {
        messageEl.textContent = '';
        messageEl.className = 'message';
      }, 10000); // Longer timeout for debug messages
    }

    async function copyManifest() {
      try {
        const url = baseUrl() + '/manifest.json';
        await navigator.clipboard.writeText(url);
        showMessage('Manifest URL copied to clipboard!', 'success');
      } catch (error) {
        showMessage('Unable to copy to clipboard', 'error');
      }
    }

    function bindButton(id, handler) {
      const el = document.getElementById(id);
      if (el) {
        el.addEventListener('click', handler);
      }
    }

    document.querySelectorAll('[data-template]').forEach(card => {
      const template = card.getAttribute('data-template');
      card.addEventListener('click', () => useTemplate(template));
    });
    bindButton('btnAddCondition', () => addCondition());
    bindButton('btnAddAction', () => addAction());
    bindButton('btnSaveRule', saveRule);
    bindButton('btnTestRule', testRule);
    bindButton('btnShowDebug', showDebug);
    bindButton('btnRefreshRules', loadRules);
    bindButton('btnAdvancedBuilder', () => window.location.href = baseUrl() + '/settings');
    bindButton('btnIftttBuilder', () => window.location.href = baseUrl() + '/ifttt');
    bindButton('btnCopyManifest', copyManifest);

    addCondition();
    addAction();

    // Initialize
    document.addEventListener('DOMContentLoaded', function() {
      // Try to load workspace ID from URL parameters
      const urlParams = new URLSearchParams(window.location.search);
      const workspaceId = urlParams.get('workspaceId') || urlParams.get('ws');
      if (workspaceId) {
        document.getElementById('workspaceId').value = workspaceId;
        loadRules();
      }
      const ruleNameParam = urlParams.get('ruleName');
      if (ruleNameParam) {
        document.getElementById('ruleName').value = ruleNameParam;
      }
      const descriptionPrefill = urlParams.get('prefillDescription');
      if (descriptionPrefill) {
        const firstCondition = document.querySelector('#conditions .condition-row');
        if (firstCondition) {
          const typeSelect = firstCondition.querySelector('.condition-type');
          const valueInput = firstCondition.querySelector('.condition-value');
          if (typeSelect) typeSelect.value = 'descriptionContains';
          if (valueInput) valueInput.value = descriptionPrefill;
        } else {
          addCondition('descriptionContains', descriptionPrefill);
        }
      }
      const projectPrefill = urlParams.get('prefillProjectId');
      if (projectPrefill) {
        addCondition('projectIdEquals', projectPrefill);
      }
      const tagPrefill = urlParams.get('prefillTagIds');
      if (tagPrefill) {
        const uniqueTags = Array.from(new Set(
          tagPrefill
            .split(',')
            .map(tagId => tagId.trim())
            .filter(Boolean)
        ));
        uniqueTags.forEach(tagId => addCondition('hasTag', tagId));
      }
    });
  </script>
</body>
</html>
""";

        html = String.format(html, nonce, nonce, nonce, applyMode, skipSig, envLabel, base, nonce);
        return HttpResponse.ok(html, "text/html; charset=utf-8");
    }
}
