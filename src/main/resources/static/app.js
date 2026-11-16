(() => {
  "use strict";

  const $ = (id) => document.getElementById(id);
  const STORAGE_KEY = "urlshortener.recent";
  const MAX_RECENT = 10;

  // ---------- API helper ----------
  async function api(method, path, body) {
    const options = { method, headers: {} };
    if (body !== undefined) {
      options.headers["Content-Type"] = "application/json";
      options.body = JSON.stringify(body);
    }
    const res = await fetch(path, options);
    const text = await res.text();
    let data = null;
    if (text) {
      try { data = JSON.parse(text); } catch { data = text; }
    }
    return { status: res.status, ok: res.ok, data };
  }

  function errorMessage(result) {
    const d = result.data;
    if (d && d.errors && d.errors.length) return d.errors[0].message;
    if (d && d.message) return d.message;
    return "Request failed (" + result.status + ")";
  }

  const shortLink = (code) => location.origin + "/r/" + code;

  // ---------- recent links (kept in this browser only) ----------
  function loadRecent() {
    try {
      const list = JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]");
      return Array.isArray(list) ? list : [];
    } catch { return []; }
  }

  function saveRecent(list) {
    try { localStorage.setItem(STORAGE_KEY, JSON.stringify(list.slice(0, MAX_RECENT))); } catch { /* storage unavailable */ }
  }

  function upsertRecent(code, url) {
    const list = loadRecent().filter((item) => item.code !== code);
    list.unshift({ code, url });
    saveRecent(list);
    renderRecent();
  }

  function removeRecent(code) {
    saveRecent(loadRecent().filter((item) => item.code !== code));
    renderRecent();
  }

  function renderRecent() {
    const list = loadRecent();
    const ul = $("recent-list");
    ul.replaceChildren();
    $("recent-empty").hidden = list.length > 0;

    list.forEach((item) => {
      const li = document.createElement("li");
      const btn = document.createElement("button");
      btn.type = "button";
      btn.className = "recent";
      const code = document.createElement("span");
      code.className = "code";
      code.textContent = item.code;
      const target = document.createElement("span");
      target.className = "target";
      target.textContent = item.url;
      btn.append(code, target);
      btn.addEventListener("click", () => { $("code-input").value = item.code; });
      li.appendChild(btn);
      ul.appendChild(li);
    });
  }

  // ---------- create ----------
  $("create-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const errorEl = $("create-error");
    errorEl.hidden = true;
    $("result").hidden = true;

    try {
      const result = await api("POST", "/shorten", { url: $("url-input").value });
      if (!result.ok) {
        errorEl.textContent = errorMessage(result);
        errorEl.hidden = false;
        return;
      }
      const link = shortLink(result.data.shortCode);
      const a = $("result-link");
      a.href = link;
      a.textContent = link;
      $("result").hidden = false;
      $("code-input").value = result.data.shortCode;
      $("url-input").value = "";
      upsertRecent(result.data.shortCode, result.data.url);
    } catch {
      errorEl.textContent = "Could not reach the server.";
      errorEl.hidden = false;
    }
  });

  $("copy-btn").addEventListener("click", async () => {
    const btn = $("copy-btn");
    try {
      await navigator.clipboard.writeText($("result-link").textContent);
      btn.textContent = "Copied";
    } catch {
      btn.textContent = "Copy failed";
    }
    setTimeout(() => { btn.textContent = "Copy"; }, 1500);
  });

  // ---------- manage ----------
  function show(method, path, result) {
    const body = result.data === null ? "(no content)" : (typeof result.data === "string" ? result.data : JSON.stringify(result.data, null, 2));
    $("output").textContent = method + " " + path + "  ->  " + result.status + "\n\n" + body;
  }

  async function run(method, suffix, body, after) {
    const code = $("code-input").value.trim();
    if (!code) {
      $("output").textContent = "Enter a short code first.";
      return;
    }
    const path = "/shorten/" + encodeURIComponent(code) + suffix;
    try {
      const result = await api(method, path, body);
      show(method, path, result);
      if (after) after(code, result);
    } catch {
      $("output").textContent = "Could not reach the server.";
    }
  }

  $("btn-get").addEventListener("click", () => run("GET", ""));
  $("btn-stats").addEventListener("click", () => run("GET", "/stats"));
  $("btn-update").addEventListener("click", () =>
    run("PUT", "", { url: $("new-url-input").value }, (code, result) => {
      if (result.ok) {
        upsertRecent(code, result.data.url);
        $("new-url-input").value = "";
      }
    }));
  $("btn-delete").addEventListener("click", () => {
    const code = $("code-input").value.trim();
    if (code && !confirm("Delete short code '" + code + "'?")) return;
    run("DELETE", "", undefined, (c, result) => {
      if (result.status === 204 || result.status === 404) removeRecent(c);
    });
  });

  renderRecent();
})();
