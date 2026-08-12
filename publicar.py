"""Publica a última versão de data/noticias_temas.json no GitHub Pages.

Fluxo: copia o JSON da raiz do projeto -> mobile-noticias/data/, e se mudou
(hash diferente), faz commit + push para o repo dashboard-b3-noticias.

Silencioso quando não há mudança. Nunca lança exceção — o scraper que chama
este script não deve morrer se a rede/git falhar.
"""
from __future__ import annotations

import hashlib
import os
import shutil
import subprocess
import sys
from datetime import datetime

PASTA_PWA = os.path.dirname(os.path.abspath(__file__))
RAIZ = os.path.dirname(PASTA_PWA)
ORIGEM = os.path.join(RAIZ, "data", "noticias_temas.json")
DESTINO = os.path.join(PASTA_PWA, "data", "noticias_temas.json")


def _hash(caminho: str) -> str | None:
    if not os.path.exists(caminho):
        return None
    h = hashlib.sha256()
    with open(caminho, "rb") as f:
        for bloco in iter(lambda: f.read(65536), b""):
            h.update(bloco)
    return h.hexdigest()


def _correr(cmd: list[str]) -> tuple[int, str]:
    r = subprocess.run(cmd, cwd=PASTA_PWA, capture_output=True, text=True, encoding="utf-8", errors="replace")
    saida = (r.stdout or "") + (r.stderr or "")
    return r.returncode, saida.strip()


def publicar() -> int:
    if not os.path.exists(ORIGEM):
        print(f"[publicar] origem não existe: {ORIGEM}")
        return 0

    hash_origem = _hash(ORIGEM)
    hash_destino = _hash(DESTINO)

    if hash_origem == hash_destino:
        print("[publicar] sem mudanças")
        return 0

    os.makedirs(os.path.dirname(DESTINO), exist_ok=True)
    shutil.copy2(ORIGEM, DESTINO)
    print(f"[publicar] copiado ({os.path.getsize(DESTINO)} bytes)")

    rc, saida = _correr(["git", "add", "data/noticias_temas.json"])
    if rc != 0:
        print(f"[publicar] git add falhou: {saida}")
        return 1

    stamp = datetime.now().strftime("%Y-%m-%d %H:%M")
    # Se o `git add` do JSON não introduziu diferenças vs HEAD (ex.: deletado e re-copiado
    # igual), não há nada para commitar. Detetamos antes do commit para evitar exit 1.
    rc_diff, _ = _correr(["git", "diff", "--cached", "--quiet"])
    if rc_diff == 0:
        print("[publicar] nada para commitar")
        return 0

    rc, saida = _correr(["git", "commit", "-m", f"notícias {stamp}"])
    if rc != 0:
        print(f"[publicar] git commit falhou: {saida}")
        return 1

    rc, saida = _correr(["git", "push", "-q", "origin", "main"])
    if rc != 0:
        print(f"[publicar] git push falhou: {saida}")
        return 1

    print(f"[publicar] push OK — notícias {stamp}")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(publicar())
    except Exception as exc:  # noqa: BLE001 — deliberado: nunca abortar o scraper
        print(f"[publicar] erro inesperado: {exc}")
        sys.exit(0)
