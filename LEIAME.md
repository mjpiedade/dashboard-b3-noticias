# Notícias · Dashboard B3 (PWA Android)

App-web em modo standalone que mostra os temas do dia do Dashboard B3.
Ao vivo em **https://mjpiedade.github.io/dashboard-b3-noticias/**.

## Adicionar ao Android (uma vez)

1. Abre o URL acima no Chrome do Android.
2. Menu ⋮ → **Adicionar ao ecrã principal**.
3. Fica com ícone próprio (pilares risk-off / neutro / risk-on + N) e abre
   em ecrã inteiro como app nativa.

## Atualizações automáticas

O `scraper_janela.py` chama `mobile-noticias/publicar.py` no fim de cada
corrida. Se o `data/noticias_temas.json` mudou, o script:

1. Copia para `mobile-noticias/data/noticias_temas.json`
2. Faz `git commit -m "notícias YYYY-MM-DD HH:MM"`
3. Faz `git push origin main`

O GitHub Pages rebuilda automaticamente em ~40s. O service worker do
telemóvel busca a nova versão na próxima abertura (ou o utilizador puxa
para baixo para forçar refresh).

Se o push falhar (sem rede, token expirado), o scraper NÃO aborta — só
imprime o erro. Na corrida seguinte tenta de novo.

## Correr localmente

```bash
python -m http.server 8765 --directory mobile-noticias
```

Abrir `http://localhost:8765/`.

## Estrutura

- `index.html` · `style.css` · `app.js` — PWA (dark, ~15 KB total)
- `manifest.webmanifest` — meta PWA (nome, ícones, `display: standalone`)
- `sw.js` — service worker (cache do shell + fallback offline do JSON)
- `icons/` — 192, 512, 512 maskable
- `data/noticias_temas.json` — cópia dos temas (repô-la = corre `publicar.py`)
- `publicar.py` — sincroniza + commita + push; chamado pelo scraper
- `sincronizar_json.py` — só copia (útil para debug)

## Repo Git

Este é um repo git independente do repo-mãe (`Dashboard B3/`), que ignora
esta pasta no seu `.gitignore` para não interferir com o autocommit diário.

- Remoto: `https://github.com/mjpiedade/dashboard-b3-noticias`
- Branch: `main`
- Pages: source `main / (root)`
