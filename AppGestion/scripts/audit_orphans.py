#!/usr/bin/env python3
"""
Auditoría de tipos Java: nombre simple vs archivos que lo mencionan.

El listado restrictivo (recomendado para posible código muerto) solo incluye
tipos bajo dto/, domain/ y util/, donde una única aparición suele indicar DTO
o utilidad realmente sin uso. Controladores, @Configuration, @Service, etc.
aparecen como 'huérfanos' en un barrido ingenuo porque Spring no referencia
el nombre de la clase en otro fichero fuente.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

API_SRC = Path(__file__).resolve().parent.parent / "api" / "src"
JAVA = list(API_SRC.rglob("*.java"))
TOP_PUBLIC = re.compile(
    r"^public\s+(?:final\s+|abstract\s+)?(class|interface|enum|record)\s+(\w+)",
    re.MULTILINE,
)


def files_with_simple_name(contents: dict[Path, str], name: str) -> list[Path]:
    pat = re.compile(r"\b" + re.escape(name) + r"\b")
    return [f for f, t in contents.items() if pat.search(t)]


def main() -> None:
    mode = sys.argv[1] if len(sys.argv) > 1 else "dto-domain-util"
    contents: dict[Path, str] = {}
    for f in JAVA:
        contents[f] = f.read_text(encoding="utf-8", errors="replace")

    orphans: list[tuple[str, str]] = []
    skipped: list[tuple[str, str, str]] = []

    for f in sorted(JAVA):
        rel = f.relative_to(API_SRC)
        if mode == "dto-domain-util":
            if not any(p in rel.parts for p in ("dto", "domain", "util")):
                continue
            if "test" in rel.parts:
                continue
        text = contents[f]
        m = TOP_PUBLIC.search(text)
        if not m:
            continue
        type_name = m.group(2)
        if type_name != f.stem:
            skipped.append((str(rel), type_name, f.stem))
            continue
        refs = files_with_simple_name(contents, type_name)
        if len(refs) == 1:
            orphans.append((type_name, str(rel)))

    label = "dto/domain/util only" if mode == "dto-domain-util" else "all main sources"
    print(f"=== Orphans ({label}): public type == filename, name in single file ===")
    for name, rel in sorted(orphans):
        print(f"{name}\t{rel}")
    print(f"Total: {len(orphans)}")
    if skipped:
        print("\n=== public type name != filename ===")
        for row in sorted(skipped)[:50]:
            print("\t".join(row))
        if len(skipped) > 50:
            print(f"... +{len(skipped) - 50} more")


if __name__ == "__main__":
    main()
