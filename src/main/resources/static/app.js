(() => {
  "use strict";
  const $ = (id) => document.getElementById(id);
  const KEY = "snipit.links";
  const fmt = new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" });
  const date = (iso) => (iso ? fmt.format(new Date(iso)) : "-");
  const full = (c) => location.origin + "/r/" + c;
  const shown = (c) => location.host + "/r/" + c;
  const safe = (u) => /^https?:\/\//i.test(u);

  let links = load();
  const clicks = {};
  let cur = null; // record shown in the details dialog

  function load() {
    try { const l = JSON.parse(localStorage.getItem(KEY) || "[]"); return Array.isArray(l) ? l : []; } catch { return []; }
  }
  function save() { try { localStorage.setItem(KEY, JSON.stringify(links.slice(0, 20))); } catch { /* ignore */ } }

  async function api(method, path, body) {
    const o = { method, headers: {} };
    if (body !== undefined) { o.headers["Content-Type"] = "application/json"; o.body = JSON.stringify(body); }
    const r = await fetch(path, o);
    const t = await r.text();
    let data = null;
    if (t) { try { data = JSON.parse(t); } catch { data = t; } }
    return { ok: r.ok, status: r.status, data };
  }
  const msg = (r) => (r.data && r.data.errors && r.data.errors.length ? r.data.errors[0].message : (r.data && r.data.message) || "Request failed (" + r.status + ")");
  const stats = (c) => api("GET", "/shorten/" + encodeURIComponent(c) + "/stats");

  function toast(text) {
    const t = $("toast"); t.textContent = text; t.classList.add("on");
    clearTimeout(toast.id); toast.id = setTimeout(() => t.classList.remove("on"), 1800);
  }
  async function copy(text) {
    try { await navigator.clipboard.writeText(text); toast("Copied to clipboard"); } catch { toast("Copy failed"); }
  }
  function showError(el, text) { el.textContent = text || ""; el.hidden = !text; }

  function upsert(rec) {
    links = [rec, ...links.filter((l) => l.code !== rec.code)];
    save(); render();
  }
  function remove(code) { links = links.filter((l) => l.code !== code); delete clicks[code]; save(); render(); }

  function render() {
    const list = $("link-list");
    list.replaceChildren();
    $("empty").hidden = links.length > 0;
    links.forEach((l) => {
      const row = document.createElement("div"); row.className = "row";
      const main = document.createElement("div"); main.className = "main";
      const a = document.createElement("a"); a.className = "short"; a.href = full(l.code); a.target = "_blank"; a.rel = "noopener"; a.textContent = shown(l.code);
      const d = document.createElement("p"); d.className = "dest"; d.textContent = l.url;
      const s = document.createElement("small"); s.textContent = "Created " + date(l.createdAt);
      main.append(a, d, s);
      const c = document.createElement("div"); c.className = "clicks";
      const n = document.createElement("b"); n.textContent = clicks[l.code] ?? "-";
      const lab = document.createElement("span"); lab.textContent = "clicks";
      c.append(n, lab);
      const acts = document.createElement("div"); acts.className = "acts";
      const b1 = document.createElement("button"); b1.className = "btn btn-ghost sm"; b1.type = "button"; b1.textContent = "Copy";
      b1.addEventListener("click", () => copy(full(l.code)));
      const b2 = document.createElement("button"); b2.className = "btn btn-ghost sm"; b2.type = "button"; b2.textContent = "Details";
      b2.addEventListener("click", () => openDetails(l.code));
      acts.append(b1, b2);
      row.append(main, c, acts);
      list.appendChild(row);
    });
  }

  async function refresh() {
    const gone = [];
    await Promise.allSettled(links.map(async (l) => {
      const r = await stats(l.code);
      if (r.status === 404) gone.push(l.code);
      else if (r.ok) { clicks[l.code] = r.data.accessCount; l.url = r.data.url; l.createdAt = r.data.createdAt; }
    }));
    links = links.filter((l) => !gone.includes(l.code));
    save(); render();
  }

  // ----- create -----
  $("create-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    showError($("create-error"), ""); $("result").hidden = true;
    try {
      const r = await api("POST", "/shorten", { url: $("url-input").value });
      if (!r.ok) return showError($("create-error"), msg(r));
      const a = $("result-link"); a.href = full(r.data.shortCode); a.textContent = shown(r.data.shortCode);
      $("result").hidden = false; $("result").dataset.code = r.data.shortCode;
      $("url-input").value = "";
      clicks[r.data.shortCode] = 0;
      upsert({ code: r.data.shortCode, url: r.data.url, createdAt: r.data.createdAt });
    } catch { showError($("create-error"), "Could not reach the server."); }
  });
  $("copy-btn").addEventListener("click", () => copy($("result-link").href));
  $("result-details").addEventListener("click", () => openDetails($("result").dataset.code));

  // ----- find a code -----
  function extractCode(input) {
    const s = input.trim();
    if (/^[0-9A-Za-z]+$/.test(s)) return s;
    try {
      const parts = new URL(s.includes("://") ? s : "http://" + s).pathname.split("/").filter((p) => p && p !== "stats");
      const c = parts.pop() || "";
      return /^[0-9A-Za-z]+$/.test(c) ? c : "";
    } catch { return ""; }
  }
  $("lookup-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const err = $("lookup-error"); showError(err, "");
    const code = extractCode($("lookup-input").value);
    if (!code) return showError(err, "Enter a short code or a short link.");
    try {
      const r = await stats(code);
      if (!r.ok) return showError(err, r.status === 404 ? "No link found for that code." : msg(r));
      clicks[code] = r.data.accessCount;
      upsert({ code, url: r.data.url, createdAt: r.data.createdAt });
      $("lookup-input").value = "";
      openDetails(code);
    } catch { showError(err, "Could not reach the server."); }
  });
  $("refresh-btn").addEventListener("click", refresh);
  document.addEventListener("visibilitychange", () => { if (!document.hidden) refresh(); });

  // ----- details dialog -----
  const dlg = $("dlg");
  function fill(d) {
    cur = d;
    const a = $("d-short"); a.href = full(d.shortCode); a.textContent = shown(d.shortCode);
    $("d-url").textContent = d.url;
    $("d-clicks").textContent = d.accessCount;
    $("d-created").textContent = date(d.createdAt);
    $("d-updated").textContent = date(d.updatedAt);
    clicks[d.shortCode] = d.accessCount;
  }
  function resetDialog() {
    $("d-edit").hidden = true; $("d-confirm").hidden = true; $("d-actions").hidden = false; $("d-url").hidden = false;
    showError($("d-error"), "");
  }
  async function openDetails(code) {
    resetDialog();
    try {
      const r = await stats(code);
      if (r.status === 404) { remove(code); return toast("That link no longer exists"); }
      if (!r.ok) return toast(msg(r));
      fill(r.data);
      upsert({ code, url: r.data.url, createdAt: r.data.createdAt });
      if (!dlg.open) dlg.showModal();
    } catch { toast("Could not reach the server"); }
  }
  $("dlg-close").addEventListener("click", () => dlg.close());
  dlg.addEventListener("click", (e) => { if (e.target === dlg) dlg.close(); });
  $("d-copy").addEventListener("click", () => copy($("d-short").href));

  $("d-edit-btn").addEventListener("click", () => {
    $("d-input").value = cur.url; $("d-edit").hidden = false; $("d-url").hidden = true; $("d-actions").hidden = true; $("d-input").focus();
  });
  $("d-cancel").addEventListener("click", resetDialog);
  $("d-edit").addEventListener("submit", async (e) => {
    e.preventDefault(); showError($("d-error"), "");
    try {
      const r = await api("PUT", "/shorten/" + encodeURIComponent(cur.shortCode), { url: $("d-input").value });
      if (!r.ok) return showError($("d-error"), msg(r));
      fill({ ...r.data, accessCount: clicks[cur.shortCode] ?? cur.accessCount });
      upsert({ code: r.data.shortCode, url: r.data.url, createdAt: r.data.createdAt });
      resetDialog(); toast("Destination updated");
    } catch { showError($("d-error"), "Could not reach the server."); }
  });

  $("d-delete-btn").addEventListener("click", () => { $("d-actions").hidden = true; $("d-confirm").hidden = false; });
  $("d-no").addEventListener("click", resetDialog);
  $("d-yes").addEventListener("click", async () => {
    try {
      const r = await api("DELETE", "/shorten/" + encodeURIComponent(cur.shortCode));
      if (r.status === 204 || r.status === 404) { remove(cur.shortCode); dlg.close(); toast("Link deleted"); }
      else showError($("d-error"), msg(r));
    } catch { showError($("d-error"), "Could not reach the server."); }
  });

  render();
  refresh();
})();
