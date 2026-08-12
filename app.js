"use strict";

const URL_JSON = "./data/noticias_temas.json";
const ROTULOS = { risk_on: "RISK-ON", risk_off: "RISK-OFF", neutro: "NEUTRO" };

const el = {
  lista: document.getElementById("lista"),
  idade: document.getElementById("idade"),
  contadores: document.getElementById("contadores"),
  ultima: document.getElementById("ultima"),
  refresh: document.getElementById("refresh-indicador"),
};

let ultimoAtualizadoEm = null;
let temporizadorIdade = null;

function escaparHTML(s) {
  if (s == null) return "";
  return String(s)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function parsearISO(s) {
  if (!s) return null;
  // formato do ficheiro: "2026-08-12T14:11" (sem tz — hora local de SP)
  // tratamos como local ao dispositivo; serve para "há X min" com margem
  const d = new Date(s);
  return isNaN(d) ? null : d;
}

function formatarIdade(dt) {
  if (!dt) return { texto: "—", classe: "" };
  const segundos = Math.max(0, Math.floor((Date.now() - dt.getTime()) / 1000));
  const min = Math.floor(segundos / 60);
  if (min < 1) return { texto: "agora mesmo", classe: "" };
  if (min < 60) return { texto: `há ${min} min`, classe: min > 30 ? "velho" : "" };
  const h = Math.floor(min / 60);
  const mr = min % 60;
  if (h < 24) {
    const t = mr === 0 ? `há ${h}h` : `há ${h}h${String(mr).padStart(2, "0")}`;
    return { texto: t, classe: h >= 6 ? "muito-velho" : "velho" };
  }
  const dias = Math.floor(h / 24);
  return { texto: `há ${dias}d`, classe: "muito-velho" };
}

function atualizarIdade() {
  const { texto, classe } = formatarIdade(ultimoAtualizadoEm);
  el.idade.textContent = texto;
  el.idade.classList.remove("velho", "muito-velho");
  if (classe) el.idade.classList.add(classe);
}

function renderContadores(temas) {
  const cont = { risk_on: 0, risk_off: 0, neutro: 0 };
  for (const t of temas) {
    if (cont[t.sinal] != null) cont[t.sinal] += 1;
  }
  el.contadores.innerHTML = `
    <span class="item"><span class="ponto" style="background:var(--risk-on)"></span>${cont.risk_on} risk-on</span>
    <span class="item"><span class="ponto" style="background:var(--risk-off)"></span>${cont.risk_off} risk-off</span>
    <span class="item"><span class="ponto" style="background:var(--neutro)"></span>${cont.neutro} neutros</span>
    <span class="item">· ${temas.length} temas</span>
  `;
}

function renderTemas(temas) {
  if (!temas || temas.length === 0) {
    el.lista.innerHTML = `<div class="erro">Sem temas para mostrar.</div>`;
    return;
  }
  const html = temas.map((t, i) => {
    const sinal = ROTULOS[t.sinal] ? t.sinal : "neutro";
    const rot = ROTULOS[sinal];
    const noticias = Array.isArray(t.noticias) ? t.noticias : [];
    const noticiasHTML = noticias.map(n => `
      <li class="noticia">
        <div class="noticia-titulo">${escaparHTML(n.titulo)}</div>
        <div class="noticia-meta">
          <span class="fonte">${escaparHTML(n.fonte || "")}</span>
          <span>${escaparHTML(n.data || "")}${n.hora ? ` · ${escaparHTML(n.hora)}` : ""}</span>
        </div>
      </li>
    `).join("");

    return `
      <article class="tema ${sinal}" data-i="${i}">
        <div class="tema-cabeca" role="button" aria-expanded="false" tabindex="0">
          <div class="tema-titulo-bloco">
            <span class="selo ${sinal}">${rot}</span>
            <h2>${escaparHTML(t.titulo)}</h2>
            <span class="n-noticias">${t.n_noticias ?? noticias.length} notícias</span>
          </div>
          <svg class="chevron" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <polyline points="5 8 10 13 15 8"></polyline>
          </svg>
        </div>
        <div class="tema-corpo">
          <div class="tema-corpo-interior">
            ${t.resumo ? `<div class="bloco"><div class="bloco-titulo">Resumo</div><div class="bloco-texto">${escaparHTML(t.resumo)}</div></div>` : ""}
            ${t.impacto ? `<div class="bloco"><div class="bloco-titulo">Impacto</div><div class="bloco-texto">${escaparHTML(t.impacto)}</div></div>` : ""}
            ${noticias.length ? `<div class="bloco"><div class="bloco-titulo">Notícias (${noticias.length})</div><ul class="noticias">${noticiasHTML}</ul></div>` : ""}
          </div>
        </div>
      </article>
    `;
  }).join("");

  el.lista.innerHTML = html;
  el.lista.setAttribute("aria-busy", "false");

  el.lista.querySelectorAll(".tema").forEach(art => {
    const cabeca = art.querySelector(".tema-cabeca");
    const alternar = () => {
      const aberto = art.classList.toggle("aberto");
      cabeca.setAttribute("aria-expanded", aberto ? "true" : "false");
    };
    cabeca.addEventListener("click", alternar);
    cabeca.addEventListener("keydown", (e) => {
      if (e.key === "Enter" || e.key === " ") { e.preventDefault(); alternar(); }
    });
  });

  // Deep-link do widget Android: ?tema=N ou #tema=N abre o Nº tema expandido
  const params = new URLSearchParams(location.search);
  const alvo = params.get("tema") ?? (location.hash.startsWith("#tema=") ? location.hash.slice(6) : null);
  if (alvo != null) {
    const i = parseInt(alvo, 10);
    const art = el.lista.querySelector(`.tema[data-i="${i}"]`);
    if (art) {
      art.classList.add("aberto");
      art.querySelector(".tema-cabeca").setAttribute("aria-expanded", "true");
      setTimeout(() => art.scrollIntoView({ behavior: "smooth", block: "start" }), 100);
    }
  }
}

async function carregar({ mostrarIndicador = false } = {}) {
  if (mostrarIndicador) el.refresh.classList.add("visivel");
  try {
    // cache-buster para forçar rede quando o utilizador puxa refresh
    const url = mostrarIndicador ? `${URL_JSON}?t=${Date.now()}` : URL_JSON;
    const resp = await fetch(url, { cache: mostrarIndicador ? "no-store" : "default" });
    if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
    const dados = await resp.json();
    ultimoAtualizadoEm = parsearISO(dados.atualizado_em);
    atualizarIdade();
    renderContadores(dados.temas || []);
    renderTemas(dados.temas || []);
    el.ultima.textContent = dados.atualizado_em
      ? `Notícias atualizadas em ${dados.atualizado_em.replace("T", " ")}`
      : "";
  } catch (err) {
    el.lista.innerHTML = `<div class="erro">Não consegui carregar as notícias.<br><small>${escaparHTML(err.message)}</small></div>`;
    el.lista.setAttribute("aria-busy", "false");
  } finally {
    if (mostrarIndicador) {
      setTimeout(() => el.refresh.classList.remove("visivel"), 300);
    }
  }
}

// ---- Pull-to-refresh (simples, sem dependências) ----
(function ativarPullRefresh() {
  let yInicial = null;
  let ativo = false;
  let deslocamento = 0;
  const LIMIAR = 70;

  document.addEventListener("touchstart", (e) => {
    if (window.scrollY > 0) { yInicial = null; return; }
    yInicial = e.touches[0].clientY;
    ativo = true;
    deslocamento = 0;
  }, { passive: true });

  document.addEventListener("touchmove", (e) => {
    if (!ativo || yInicial == null) return;
    deslocamento = e.touches[0].clientY - yInicial;
    if (deslocamento > 0 && window.scrollY === 0) {
      const prog = Math.min(1, deslocamento / LIMIAR);
      el.refresh.style.transform = `translate(-50%, ${prog * 12}px)`;
      el.refresh.style.opacity = String(prog);
      if (prog >= 1) el.refresh.classList.add("visivel");
    }
  }, { passive: true });

  document.addEventListener("touchend", () => {
    if (!ativo) return;
    el.refresh.style.transform = "";
    el.refresh.style.opacity = "";
    if (deslocamento >= LIMIAR && window.scrollY === 0) {
      carregar({ mostrarIndicador: true });
    } else {
      el.refresh.classList.remove("visivel");
    }
    ativo = false;
    yInicial = null;
    deslocamento = 0;
  });
})();

// ---- Ciclo de vida ----
carregar();
temporizadorIdade = setInterval(atualizarIdade, 30_000);

// re-fetch ao voltar a focar o separador
document.addEventListener("visibilitychange", () => {
  if (document.visibilityState === "visible") carregar();
});

// Service Worker
if ("serviceWorker" in navigator) {
  window.addEventListener("load", () => {
    navigator.serviceWorker.register("./sw.js").catch(() => {});
  });
}
