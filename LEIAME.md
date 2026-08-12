# Notícias · Dashboard B3 (PWA Android)

App-web em modo standalone: mostra os temas do dia (`data/noticias_temas.json`).
Pensada para adicionar ao ecrã inicial do Android via Chrome — abre em ecrã inteiro,
com ícone próprio, e funciona offline (última versão em cache).

## Ver localmente

```bash
python -m http.server 8765 --directory mobile-noticias
```

Abrir `http://localhost:8765/`. Para forçar refresh do JSON, puxar para baixo no topo.

## Colocar online (GitHub Pages)

### 1. Criar repo no GitHub

No `github.com` cria um repo **público** vazio (ex.: `dashboard-b3-noticias`).

### 2. Inicializar git nesta pasta

```bash
cd "C:/Users/migue/Trading/dashboard_financeiro/Dashboard B3/mobile-noticias"
git init
git branch -M main
git add .
git commit -m "PWA notícias inicial"
git remote add origin https://github.com/<utilizador>/dashboard-b3-noticias.git
git push -u origin main
```

### 3. Ligar o Pages

`Settings → Pages → Source: Deploy from a branch → Branch: main / (root) → Save`.

Em ~1 minuto fica em `https://<utilizador>.github.io/dashboard-b3-noticias/`.

### 4. No Android

Abrir esse URL no Chrome → menu ⋮ → **Adicionar ao ecrã principal**.
Fica com ícone e abre como app.

## Atualizar as notícias no telemóvel

O JSON no telemóvel só muda quando fazemos push do repo. Duas opções:

**A. Manual (à velocidade que quiseres):**
```bash
python mobile-noticias/sincronizar_json.py
cd mobile-noticias && git add data/noticias_temas.json && git commit -m "notícias" && git push
```

**B. Automático (recomendado — depois de cada corrida do scraper):**

No fim do `scraper_janela.py` (ou numa tarefa Windows separada logo a seguir),
correr:

```bash
python mobile-noticias/sincronizar_json.py
cd mobile-noticias && git add data/noticias_temas.json && git commit -m "auto: notícias" && git push -q
```

O service worker guarda a versão anterior em cache — se o telemóvel estiver sem
rede, ainda abre a última versão vista.

## Ficheiros

- `index.html` — página única
- `style.css` — dark mode, cores dos sinais iguais à app principal
- `app.js` — fetch + render + pull-to-refresh
- `manifest.webmanifest` — meta PWA (nome, ícones, cores, `display: standalone`)
- `sw.js` — service worker (cache do shell + fallback offline do JSON)
- `icons/` — 192, 512 e 512 maskable
- `data/noticias_temas.json` — cópia do JSON de temas
- `sincronizar_json.py` — copia da raiz do projeto para aqui
