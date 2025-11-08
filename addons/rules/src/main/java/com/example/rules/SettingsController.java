package com.example.rules;

import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.RequestHandler;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Renders the settings UI (no‑code rule builder) for the Rules add‑on.
 */
public class SettingsController implements RequestHandler {

    @Override
    public HttpResponse handle(HttpServletRequest request) {
        String applyMode = "true".equalsIgnoreCase(System.getenv().getOrDefault("RULES_APPLY_CHANGES", "false")) ? "Apply" : "Log-only";
        String skipSig = "true".equalsIgnoreCase(System.getenv().getOrDefault("ADDON_SKIP_SIGNATURE_VERIFY", "false")) ? "ON" : "OFF";
        String base = System.getenv().getOrDefault("ADDON_BASE_URL", "");
        String html = """
<!DOCTYPE html>
<html>
<head>
  <meta charset=\"UTF-8\" />
  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />
  <title>Rules Add-on</title>
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin:0; padding:20px; background:#f5f5f5; }
    h1 { font-size:20px; margin-top:0; color:#333; }
    h2 { font-size:16px; margin-top:20px; color:#555; }
    .section { background:white; padding:15px; margin-bottom:15px; border-radius:6px; box-shadow:0 1px 3px rgba(0,0,0,0.1); }
    .row { display:flex; gap:8px; align-items:center; margin-bottom:8px; flex-wrap:wrap; }
    label { font-size:13px; color:#444; }
    input[type=text], select { padding:6px 8px; border:1px solid #ccc; border-radius:4px; font-size:13px; }
    button { padding:6px 10px; font-size:13px; cursor:pointer; border:1px solid #1976d2; background:#1976d2; color:white; border-radius:4px; }
    button.secondary { background:#eee; color:#333; border-color:#bbb; }
    .pill { background:#eef6ff; padding:2px 6px; border-radius:10px; font-size:12px; color:#1976d2; }
    .list { font-size:13px; }
    .list li { margin-bottom:4px; }
    .muted { color:#777; font-size:12px; }
    .error { color:#b00020 }
    .ok { color:#1b5e20 }
    code { background:#f3f3f3; padding:2px 6px; border-radius:3px; }
  </style>
</head>
<body>
  <h1>Rules Automation</h1>

  <div class=\"section\" style=\"border-left:4px solid #1976d2\">
    <div class=\"row\">
      <span class=\"pill\">Mode: %s</span>
      <span class=\"pill\">Signature bypass: %s</span>
      <span class=\"pill\">Base: %s</span>
    </div>
    <div class=\"row\"><span id=\"tokenStatus\" class=\"muted\">Token: (checking...)</span></div>
    <div class=\"row\">\n      <button class=\"secondary\" type=\"button\" onclick=\"copyManifest()\">Copy manifest URL</button>\n      <button class=\"secondary\" type=\"button\" onclick=\"openInstall()\">Open install page</button>\n      <span id=\"installMsg\" class=\"muted\"></span>\n    </div>
    <p class=\"muted\">Install the manifest after the server is running so the Developer workspace sends webhooks to this exact URL. For signed webhooks, leave signature bypass OFF; while testing, turn it ON with <code>ADDON_SKIP_SIGNATURE_VERIFY=true</code>.</p>
  </div>

  <div class=\"section\"> 
    <h2>Workspace Data</h2>
    <div class=\"row\">
      <button class=\"secondary\" type=\"button\" onclick=\"loadCache()\">Load Data</button>
      <button class=\"secondary\" type=\"button\" onclick=\"refreshCache()\">Refresh Cache</button>
      <span id=\"cacheMsg\" class=\"muted\"></span>
    </div>
    <div class=\"row\"><span id=\"cacheCounts\" class=\"muted\">(no data)</span></div>
    <datalist id=\"dl-tags\"></datalist>
    <datalist id=\"dl-projects\"></datalist>
    <datalist id=\"dl-project-ids\"></datalist>
    <datalist id=\"dl-clients\"></datalist>
    <datalist id=\"dl-users\"></datalist>
    <datalist id=\"dl-tasks\"></datalist>
    <datalist id=\"dl-task-ids\"></datalist>
  </div>

  <div class=\"section\"> 
    <h2>Create / Update Rule</h2>
    <div class=\"row\">
      <label>Workspace ID</label>
      <input id=\"wsid\" type=\"text\" placeholder=\"workspaceId\" style=\"min-width:260px\" />
      <span class=\"muted\">Required for API calls</span>
    </div>
    <div class=\"row\">
      <label>Name</label>
      <input id=\"ruleName\" type=\"text\" placeholder=\"Rule name\" style=\"min-width:260px\" />
      <label><input id=\"ruleEnabled\" type=\"checkbox\" checked /> Enabled</label>
      <label>Combinator</label>
      <select id=\"ruleComb\">
        <option value=\"\">(auto: OR)</option>
        <option value=\"AND\">AND</option>
        <option value=\"OR\">OR</option>
      </select>
    </div>

    <div class=\"row\"><span class=\"pill\">Conditions</span></div>
    <div id=\"conds\"></div>
    <div class=\"row\"><button class=\"secondary\" onclick=\"addCond()\" type=\"button\">+ Condition</button></div>

    <div class=\"row\"><span class=\"pill\">Actions</span></div>
    <div id=\"acts\"></div>
    <div class=\"row\"><button class=\"secondary\" onclick=\"addAct()\" type=\"button\">+ Action</button></div>

    <div class=\"row\">
      <button onclick=\"saveRule()\" type=\"button\">Save Rule</button>
      <span id=\"saveMsg\" class=\"muted\"></span>
    </div>
  </div>

  <div class=\"section\">
    <h2>Existing Rules</h2>
    <div class=\"row\"><button class=\"secondary\" type=\"button\" onclick=\"loadRules()\">Refresh</button></div>
    <ul id=\"rulesList\" class=\"list\"></ul>
  </div>

  <div class=\"section\">
    <h2>Quick Test (Dry‑run)</h2>
    <div class=\"row\">
      <label>Description</label>
      <input id=\"testDesc\" type=\"text\" placeholder=\"e.g., Client meeting\" style=\"min-width:260px\" />
      <label>Project ID</label>
      <input id=\"testPid\" type=\"text\" placeholder=\"optional\" />
      <button class=\"secondary\" onclick=\"dryRun()\" type=\"button\">Run</button>
      <span id=\"testMsg\" class=\"muted\"></span>
    </div>
    <p class=\"muted\">Dry‑run evaluates rules and shows matched actions without applying changes.</p>
  </div>

  <div class=\"section\">
    <h2>Supported Conditions</h2>
    <ul>
      <li><code>descriptionContains</code>, <code>descriptionEquals</code></li>
      <li><code>hasTag</code> (by ID)</li>
      <li><code>projectIdEquals</code>, <code>projectNameContains</code></li>
      <li><code>clientIdEquals</code>, <code>clientNameContains</code></li>
      <li><code>isBillable</code> (value: true/false)</li>
    </ul>
  </div>

  <div class=\"section\">
    <h2>Supported Actions</h2>
    <ul>
      <li><code>add_tag</code>, <code>remove_tag</code> (arg: tag/name)</li>
      <li><code>set_description</code> (arg: value)</li>
      <li><code>set_billable</code> (arg: value=true|false)</li>
      <li><code>set_project_by_id</code> (arg: projectId), <code>set_project_by_name</code> (arg: name)</li>
      <li><code>set_task_by_id</code> (arg: taskId), <code>set_task_by_name</code> (arg: name)</li>
    </ul>
  </div>

  <script>
    const COND_TYPES = [
      'descriptionContains','descriptionEquals','hasTag','projectIdEquals','projectNameContains','clientIdEquals','clientNameContains','isBillable'
    ];
    const OPS = ['EQUALS','NOT_EQUALS','CONTAINS','NOT_CONTAINS'];
    const ACT_TYPES = [
      'add_tag','remove_tag','set_description','set_billable','set_project_by_id','set_project_by_name','set_task_by_id','set_task_by_name'
    ];
    let CACHE = { tags:[], projects:[], clients:[], users:[], tasks:[] };
    let MAPS = { tagNameToId:{}, projectNameToId:{}, projectIdToName:{}, clientNameToId:{}, taskNameToId:{}, taskIdToName:{} };
    function summarize(){ const el=document.getElementById('cacheCounts'); if(!CACHE||!CACHE.tags){ el.textContent='(no data)'; return;} el.textContent=`Tags: ${CACHE.tags.length} • Projects: ${CACHE.projects.length} • Clients: ${CACHE.clients.length} • Users: ${CACHE.users.length} • Tasks: ${CACHE.tasks.length}`; }
    function fillDatalists(){ const fill=(id,arr,key='name')=>{ const dl=document.getElementById(id); if(!dl)return; dl.innerHTML=''; (arr||[]).forEach(x=>{ const o=document.createElement('option'); o.value=x[key]||''; dl.appendChild(o); }); }; fill('dl-tags',CACHE.tags); fill('dl-projects',CACHE.projects); fill('dl-clients',CACHE.clients); fill('dl-users',CACHE.users); fill('dl-tasks',CACHE.tasks); fill('dl-project-ids',CACHE.projects,'id'); fill('dl-task-ids',CACHE.tasks,'id'); }
    function rebuildMaps(){ MAPS={ tagNameToId:{}, projectNameToId:{}, projectIdToName:{}, clientNameToId:{}, taskNameToId:{}, taskIdToName:{} }; (CACHE.tags||[]).forEach(t=>{ if(t.name) MAPS.tagNameToId[t.name.toLowerCase()]=t.id; }); (CACHE.projects||[]).forEach(p=>{ if(p.name){ MAPS.projectNameToId[p.name.toLowerCase()]=p.id; MAPS.projectIdToName[p.id]=p.name; }}); (CACHE.clients||[]).forEach(c=>{ if(c.name) MAPS.clientNameToId[c.name.toLowerCase()]=c.id; }); (CACHE.tasks||[]).forEach(t=>{ if(t.name){ MAPS.taskNameToId[t.name.toLowerCase()]=t.id; MAPS.taskIdToName[t.id]=t.name; }}); }
    async function loadCache(){ const ws=document.getElementById('wsid').value.trim(); const msg=document.getElementById('cacheMsg'); if(!ws){ msg.textContent='workspaceId required'; msg.className='error'; return;} const r=await fetch(baseUrl()+`/api/cache/data?workspaceId=${encodeURIComponent(ws)}`); if(!r.ok){ msg.textContent='Failed to load data'; msg.className='error'; return;} CACHE=await r.json(); msg.textContent='Loaded'; msg.className='ok'; setTimeout(()=>msg.textContent='',2000); rebuildMaps(); fillDatalists(); summarize(); }
    async function refreshCache(){ const ws=document.getElementById('wsid').value.trim(); const msg=document.getElementById('cacheMsg'); if(!ws){ msg.textContent='workspaceId required'; msg.className='error'; return;} const r=await fetch(baseUrl()+`/api/cache/refresh?workspaceId=${encodeURIComponent(ws)}`,{method:'POST'}); if(r.ok){ msg.textContent='Refreshed'; msg.className='ok'; setTimeout(()=>msg.textContent='',2000); await loadCache(); } else { msg.textContent='Refresh failed'; msg.className='error'; } }

    function addCond(pref={}){
      const el = document.createElement('div');
      el.className = 'row';
      el.innerHTML = `
        <select class=\"cond-type\">${COND_TYPES.map(t=>`<option value=\"${t}\">${t}</option>`).join('')}</select>
        <select class=\"cond-op\">${OPS.map(o=>`<option value=\"${o}\">${o}</option>`).join('')}</select>
        <input class=\"cond-val\" type=\"text\" placeholder=\"value\" list=\"\" />
        <button class=\"secondary\" type=\"button\" onclick=\"this.parentElement.remove()\">Remove</button>
      `;
      const typeSel = el.querySelector('.cond-type'); const valEl = el.querySelector('.cond-val');
      function syncCondAutocomplete(){ const t=typeSel.value; valEl.removeAttribute('list'); if(t==='hasTag'){ valEl.setAttribute('list','dl-tags'); valEl.placeholder='tag name'; } else if(t==='projectNameContains'){ valEl.setAttribute('list','dl-projects'); valEl.placeholder='project name'; } else if(t==='projectIdEquals'){ valEl.setAttribute('list','dl-projects'); valEl.placeholder='project (auto→id)'; } else if(t==='clientNameContains'){ valEl.setAttribute('list','dl-clients'); valEl.placeholder='client name'; } else if(t==='clientIdEquals'){ valEl.placeholder='client id'; } }
      typeSel.addEventListener('change', syncCondAutocomplete); syncCondAutocomplete();
      valEl.addEventListener('change', ()=>{ if(typeSel.value==='projectIdEquals'){ const k=(valEl.value||'').toLowerCase(); if(MAPS.projectNameToId[k]) valEl.value=MAPS.projectNameToId[k]; }});
      if(pref.type) el.querySelector('.cond-type').value = pref.type;
      if(pref.operator) el.querySelector('.cond-op').value = pref.operator;
      if(pref.value) el.querySelector('.cond-val').value = pref.value;
      document.getElementById('conds').appendChild(el);
    }

    function addAct(pref={}){
      const el = document.createElement('div');
      el.className = 'row';
      el.innerHTML = `
        <select class=\"act-type\">${ACT_TYPES.map(t=>`<option value=\"${t}\">${t}</option>`).join('')}</select>
        <input class=\"act-k\" type=\"text\" placeholder=\"arg key (e.g., tag,name,projectId,taskId,value)\" style=\"min-width:240px\"/>
        <input class=\"act-v\" type=\"text\" placeholder=\"arg value\" style=\"min-width:240px\" list=\"\"/>
        <button class=\"secondary\" type=\"button\" onclick=\"this.parentElement.remove()\">Remove</button>
      `;
      // Improve UX: set common arg keys/values when type changes
      const typeSel = el.querySelector('.act-type');
      const kEl = el.querySelector('.act-k');
      const vEl = el.querySelector('.act-v');
      function syncActPlaceholders(){
        const t = typeSel.value;
        if(t==='add_tag' || t==='remove_tag'){ kEl.value = kEl.value||'tag'; kEl.placeholder='tag or name'; vEl.placeholder='e.g., billable'; }
        else if(t==='set_description'){ kEl.value = kEl.value||'value'; vEl.placeholder='new description'; }
        else if(t==='set_billable'){ kEl.value = kEl.value||'value'; vEl.placeholder='true or false'; if(!vEl.value) vEl.value='true'; }
        else if(t==='set_project_by_id'){ kEl.value = kEl.value||'projectId'; vEl.placeholder='project id'; }
        else if(t==='set_project_by_name'){ kEl.value = kEl.value||'name'; vEl.placeholder='project name'; vEl.setAttribute('list','dl-projects'); }
        else if(t==='set_task_by_id'){ kEl.value = kEl.value||'taskId'; vEl.placeholder='task id'; vEl.setAttribute('list','dl-task-ids'); }
        else if(t==='set_task_by_name'){ kEl.value = kEl.value||'name'; vEl.placeholder='task name'; vEl.setAttribute('list','dl-tasks'); }
      }
      typeSel.addEventListener('change', syncActPlaceholders);
      syncActPlaceholders();
      if(pref.type) el.querySelector('.act-type').value = pref.type;
      if(pref.k) el.querySelector('.act-k').value = pref.k;
      if(pref.v) el.querySelector('.act-v').value = pref.v;
      document.getElementById('acts').appendChild(el);
    }

    function baseUrl(){
      const u = new URL(window.location.href);
      let p = u.pathname;
      if (p.endsWith('/settings/')) p = p.slice(0, -10);
      else if (p.endsWith('/settings')) p = p.slice(0, -9);
      if (p.length > 1 && p.endsWith('/')) p = p.slice(0, -1);
      return u.origin + p;
    }

    async function saveRule(){
      const ws = document.getElementById('wsid').value.trim();
      if(!ws){ document.getElementById('saveMsg').textContent = 'workspaceId is required'; document.getElementById('saveMsg').className='error'; return; }
      try { localStorage.setItem('rules.wsid', ws); } catch(e) {}

      const name = document.getElementById('ruleName').value.trim() || 'Untitled';
      const enabled = document.getElementById('ruleEnabled').checked;
      const comb = document.getElementById('ruleComb').value;

      const conditions = Array.from(document.querySelectorAll('#conds .row')).map(row=>{
        return {
          type: row.querySelector('.cond-type').value,
          operator: row.querySelector('.cond-op').value,
          value: row.querySelector('.cond-val').value
        };
      });
      const actions = Array.from(document.querySelectorAll('#acts .row')).map(row=>{
        const t = row.querySelector('.act-type').value;
        const k = row.querySelector('.act-k').value.trim();
        const v = row.querySelector('.act-v').value;
        const args = k ? { [k]: v } : {};
        return { type: t, args };
      });

      const payload = { name, enabled, conditions, actions };
      if(comb) payload.combinator = comb;

      const resp = await fetch(baseUrl()+`/api/rules?workspaceId=${encodeURIComponent(ws)}`,{
        method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(payload)
      });
      const msg = document.getElementById('saveMsg');
      if(resp.ok){ msg.textContent = 'Saved'; msg.className='ok'; loadRules(); }
      else { const t = await resp.text(); msg.textContent = 'Error: '+t; msg.className='error'; }
    }

    async function loadRules(){
      const ws = document.getElementById('wsid').value.trim();
      if(!ws){ return; }
      const ul = document.getElementById('rulesList'); ul.innerHTML='';
      const r = await fetch(baseUrl()+`/api/rules?workspaceId=${encodeURIComponent(ws)}`);
      if(!r.ok){ ul.innerHTML = '<li class=\"error\">Failed to load rules</li>'; return; }
      const arr = await r.json();
      if(!Array.isArray(arr) || arr.length===0){ ul.innerHTML = '<li class=\"muted\">No rules yet.</li>'; return; }
      arr.forEach(rule=>{
        const li = document.createElement('li');
        li.innerHTML = `<strong>${rule.name||'(unnamed)'}</strong> <span class=\"muted\">(${rule.enabled?'enabled':'disabled'})</span>
          <button class=\"secondary\" type=\"button\">Delete</button>`;
        li.querySelector('button').onclick = async ()=>{
          await fetch(baseUrl()+`/api/rules?workspaceId=${encodeURIComponent(ws)}&id=${encodeURIComponent(rule.id||'')}`,{method:'DELETE'});
          loadRules();
        };
        ul.appendChild(li);
      });
    }

    async function dryRun(){
      const ws = document.getElementById('wsid').value.trim();
      if(!ws){ document.getElementById('testMsg').textContent='workspaceId is required'; document.getElementById('testMsg').className='error'; return; }
      const desc = document.getElementById('testDesc').value || 'Client meeting';
      const pid = document.getElementById('testPid').value;
      const body = { workspaceId: ws, timeEntry: { id:'te1', description: desc, tagIds:[], ...(pid?{projectId:pid}:{}) } };
      const r = await fetch(baseUrl()+`/api/test`, { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(body) });
      const msg = document.getElementById('testMsg');
      if(!r.ok){ msg.textContent = 'Error: '+(await r.text()); msg.className='error'; return; }
      const json = await r.json();
      msg.textContent = `Matched actions: ${json.actionsCount}`; msg.className='ok';
    }

    // Seed one row each and init workspaceId from storage/query
    addCond({type:'descriptionContains',operator:'CONTAINS'});
    addAct({type:'add_tag',k:'tag',v:'billable'});
    async function refreshStatus(){
      const ws = document.getElementById('wsid').value.trim();
      if(!ws){ document.getElementById('tokenStatus').textContent='Token: workspaceId not set'; return; }
      const r = await fetch(baseUrl()+`/status?workspaceId=${encodeURIComponent(ws)}`);
      if(!r.ok){ document.getElementById('tokenStatus').textContent='Token: status unavailable'; return; }
      const j = await r.json();
      document.getElementById('tokenStatus').textContent = 'Token: ' + (j.tokenPresent?'PRESENT':'MISSING');
    }

    try {
      const params = new URLSearchParams(location.search);
      const wsQ = params.get('workspaceId') || params.get('ws');
      const wsS = localStorage.getItem('rules.wsid');
      const ws = wsQ || wsS;
      if(ws){ document.getElementById('wsid').value = ws; loadRules(); refreshStatus(); loadCache(); }
    } catch(e) {}

    // UX helpers: copy manifest URL and open install page
    async function copyManifest(){
      try {
        const url = baseUrl() + '/manifest.json';
        await navigator.clipboard.writeText(url);
        const el = document.getElementById('installMsg');
        el.textContent = 'Copied: ' + url; el.className = 'ok';
        setTimeout(()=>{ el.textContent=''; }, 3000);
      } catch(e) {
        const el = document.getElementById('installMsg');
        el.textContent = 'Unable to copy (clipboard denied)'; el.className = 'error';
      }
    }
    function openInstall(){
      const ws = document.getElementById('wsid').value.trim();
      const el = document.getElementById('installMsg');
      if(!ws){ el.textContent='Workspace ID required'; el.className='error'; return; }
      const link = `https://developer.clockify.me/workspaces/${encodeURIComponent(ws)}/settings`;
      window.open(link, '_blank', 'noopener');
      el.textContent = 'Opened Developer workspace settings in a new tab';
      el.className = 'muted';
      setTimeout(()=>{ el.textContent=''; }, 3000);
    }
  </script>
</body>
</html>
""";

        html = String.format(html, applyMode, skipSig, base);
        return HttpResponse.ok(html, "text/html; charset=utf-8");
    }
}
