#!/usr/bin/env python3
"""Activa premium en PostgreSQL para un email. Requiere psycopg2-binary."""
from __future__ import annotations

import os
import sys
from urllib.parse import urlparse, unquote

try:
    import psycopg2
except ImportError:
    print("Instala: pip install psycopg2-binary", file=sys.stderr)
    sys.exit(1)

EMAIL_DEFAULT = "germancastillodkno@gmail.com"


def parse_database_url(url: str) -> dict:
    p = urlparse(url)
    if p.scheme not in ("postgresql", "postgres"):
        raise ValueError("DATABASE_URL debe ser postgresql://...")
    return {
        "host": p.hostname,
        "port": p.port or 5432,
        "dbname": (p.path or "/").lstrip("/").split("?")[0],
        "user": unquote(p.username or ""),
        "password": unquote(p.password or ""),
        "sslmode": "require" if "sslmode=require" in (p.query or "") else None,
    }


def main() -> int:
    email = sys.argv[1] if len(sys.argv) > 1 else EMAIL_DEFAULT
    url = os.environ.get("DATABASE_URL") or os.environ.get("SPRING_DATASOURCE_URL", "")
    if url.startswith("jdbc:postgresql://"):
        url = url.replace("jdbc:postgresql://", "postgresql://", 1)

    if url:
        params = parse_database_url(url)
    else:
        params = {
            "host": os.environ.get("PGHOST", "localhost"),
            "port": int(os.environ.get("PGPORT", "5433")),
            "dbname": os.environ.get("PGDATABASE", "appgestion"),
            "user": os.environ.get("PGUSER", os.environ.get("DB_USERNAME", "postgres")),
            "password": os.environ.get("PGPASSWORD", os.environ.get("DB_PASSWORD", "postgres")),
            "sslmode": os.environ.get("PGSSLMODE"),
        }

    conn_kw = {k: v for k, v in params.items() if v is not None and k != "sslmode"}
    if params.get("sslmode"):
        conn_kw["sslmode"] = params["sslmode"]

    conn = psycopg2.connect(**conn_kw)
    conn.autocommit = False
    cur = conn.cursor()

    cur.execute(
        "SELECT id, email, subscription_status, trial_end_date FROM usuarios WHERE email = %s",
        (email,),
    )
    before = cur.fetchone()
    if not before:
        print(f"No existe usuario con email: {email}")
        conn.close()
        return 1
    print("ANTES:", before)

    cur.execute(
        """
        UPDATE usuarios SET
          subscription_status = 'ACTIVE',
          stripe_customer_id = NULL,
          stripe_subscription_id = NULL,
          subscription_current_period_end = NULL,
          subscription_cancel_at_period_end = FALSE,
          subscription_requires_payment_action = FALSE
        WHERE email = %s
        """,
        (email,),
    )

    cur.execute(
        """
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'usuarios' AND column_name = 'stripe_price_id'
        """
    )
    if cur.fetchone():
        cur.execute("UPDATE usuarios SET stripe_price_id = NULL WHERE email = %s", (email,))

    cur.execute(
        "SELECT id, email, subscription_status, trial_end_date FROM usuarios WHERE email = %s",
        (email,),
    )
    after = cur.fetchone()
    conn.commit()
    cur.close()
    conn.close()
    print("DESPUÉS:", after)
    print("Listo. Cierra sesión en la web y vuelve a entrar (o recarga /auth/me).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
