"use strict";

// v3 (2026-08-13): app.js agora tem auto-reload em controllerchange e
// registration.update() no arranque/visibilitychange. Bumpo a VERSAO para que os
// telemóveis reinstalem a cache do shell e passem a servir esse app.js novo.
const VERSAO = "v3";
const CACHE_APP = `noticias-b3-app-${VERSAO}`;
const CACHE_DADOS = `noticias-b3-dados-${VERSAO}`;
const FICHEIROS_APP = [
  "./",
  "./index.html",
  "./style.css",
  "./app.js",
  "./manifest.webmanifest",
  "./icons/icon-192.png",
  "./icons/icon-512.png",
];

self.addEventListener("install", (evt) => {
  evt.waitUntil(
    caches.open(CACHE_APP).then((c) => c.addAll(FICHEIROS_APP)).then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", (evt) => {
  evt.waitUntil(
    caches.keys().then((chaves) => Promise.all(
      chaves.filter((k) => !k.endsWith(VERSAO)).map((k) => caches.delete(k))
    )).then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (evt) => {
  const req = evt.request;
  if (req.method !== "GET") return;
  const url = new URL(req.url);

  // dados: rede primeiro (com timeout), cache como fallback.
  if (url.pathname.endsWith("/data/noticias_temas.json") || url.pathname.endsWith("noticias_temas.json")) {
    evt.respondWith((async () => {
      const ctrl = new AbortController();
      const timer = setTimeout(() => ctrl.abort(), 8_000);
      try {
        const resp = await fetch(req.url, { cache: "no-store", signal: ctrl.signal });
        clearTimeout(timer);
        if (resp && resp.ok) {
          const c = await caches.open(CACHE_DADOS);
          c.put("./data/noticias_temas.json", resp.clone());
        }
        return resp;
      } catch (_) {
        clearTimeout(timer);
        const cache = await caches.open(CACHE_DADOS);
        const cached = await cache.match("./data/noticias_temas.json");
        if (cached) return cached;
        return new Response(JSON.stringify({ atualizado_em: "", temas: [] }), {
          status: 200, headers: { "Content-Type": "application/json" },
        });
      }
    })());
    return;
  }

  // shell da app: cache primeiro
  evt.respondWith((async () => {
    const cache = await caches.open(CACHE_APP);
    const cached = await cache.match(req, { ignoreSearch: true });
    if (cached) return cached;
    try {
      const resp = await fetch(req);
      if (resp && resp.ok && url.origin === self.location.origin) {
        cache.put(req, resp.clone());
      }
      return resp;
    } catch (_) {
      return cached || new Response("offline", { status: 503 });
    }
  })());
});
