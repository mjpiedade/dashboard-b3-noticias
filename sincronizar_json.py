"""Copia data/noticias_temas.json (raiz do projeto) para mobile-noticias/data/.

Usar antes de commitar/push para o repo do GitHub Pages, ou chamar depois de cada
corrida do scraper de notícias.
"""
import os
import shutil
import sys

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ORIGEM = os.path.join(RAIZ, "data", "noticias_temas.json")
DESTINO = os.path.join(os.path.dirname(os.path.abspath(__file__)), "data", "noticias_temas.json")


def main() -> int:
    if not os.path.exists(ORIGEM):
        print(f"[sincronizar_json] não encontrei {ORIGEM}", file=sys.stderr)
        return 1
    os.makedirs(os.path.dirname(DESTINO), exist_ok=True)
    shutil.copy2(ORIGEM, DESTINO)
    tam = os.path.getsize(DESTINO)
    print(f"[sincronizar_json] {ORIGEM} -> {DESTINO} ({tam} bytes)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
