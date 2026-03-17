"""
UI minimalista y moderna – inspirada en Noemí.
Paleta: púrpura, naranja, coral, azul oscuro.
Animaciones, transiciones suaves, sombras y redondeos.
"""

from tkinter import ttk
import tkinter as tk

# ── Paleta Noemí (logo: púrpura, naranja, coral, azul oscuro) ─────
_PALETTE = {
    # Fondos – tonos slate profundos
    "app_bg":       "#0f172a",
    "surface":      "#1e293b",
    "surface_alt":  "#334155",
    "surface_soft": "#475569",
    "surface_hover":"#64748b",
    "surface_card": "#1e293b",

    # Bordes – sutiles
    "border":       "#334155",
    "border_light": "#475569",
    "border_focus": "#8b5cf6",

    # Texto
    "text":         "#f1f5f9",
    "text_secondary":"#94a3b8",
    "muted":        "#64748b",

    # Acentos Noemí
    "accent":       "#8b5cf6",      # Púrpura
    "accent_hover": "#a78bfa",
    "accent_soft":  "#4c1d95",
    "accent_dark":  "#6d28d9",
    "accent_orange":"#f97316",      # Naranja
    "accent_coral": "#f43f5e",      # Coral/rosa

    # Semánticos
    "success":      "#22c55e",
    "success_dark": "#16a34a",
    "warning":      "#f59e0b",
    "warning_dark": "#d97706",
    "danger":       "#ef4444",
    "danger_dark":  "#dc2626",
    "info":         "#06b6d4",
}


def get_palette():
    """Expone la paleta para componentes que usan widgets tk."""
    return dict(_PALETTE)


def setup_styles():
    """Configura estilos ttk de la aplicación."""
    style = ttk.Style()
    style.theme_use("clam")

    c = get_palette()

    # ── Tipografía – limpia y moderna ─────────────────────────────
    font_base       = ("Segoe UI", 10)
    font_small      = ("Segoe UI", 9)
    font_heading    = ("Segoe UI Semibold", 14)
    font_subheading = ("Segoe UI Semibold", 11)
    font_btn        = ("Segoe UI Semibold", 10)
    font_btn_small  = ("Segoe UI", 9)

    # ── Frame raíz ────────────────────────────────────────────────
    style.configure("TFrame", background=c["app_bg"])

    # ── Notebook (pestañas) – diseño pill ─────────────────────────
    style.configure(
        "TNotebook",
        background=c["app_bg"],
        borderwidth=0,
        tabmargins=[0, 0, 0, 0],
    )
    style.configure(
        "TNotebook.Tab",
        padding=[24, 12],
        background=c["surface"],
        foreground=c["muted"],
        font=font_small,
        borderwidth=0,
    )
    style.map(
        "TNotebook.Tab",
        background=[("selected", c["surface_alt"]), ("active", c["surface_soft"])],
        foreground=[("selected", c["text"]), ("active", c["text_secondary"])],
    )

    # ── Botones – más padding, sensación de redondeo ───────────────
    _btn_common = dict(
        padding=[18, 11],
        font=font_btn,
        borderwidth=0,
        relief="flat",
        focuscolor=c["accent"],
    )

    style.configure("TButton", **_btn_common,
                     background=c["surface_soft"],
                     foreground=c["text"])
    style.map("TButton",
              background=[("active", c["surface_hover"]), ("pressed", c["surface"])],
              foreground=[("active", c["text"]), ("pressed", c["text"])])

    style.configure("Accent.TButton", **_btn_common,
                     background=c["accent"],
                     foreground="#ffffff")
    style.map("Accent.TButton",
              background=[("active", c["accent_hover"]), ("pressed", c["accent_dark"])],
              foreground=[("active", "#ffffff"), ("pressed", "#ffffff")])

    style.configure("Success.TButton", **_btn_common,
                     background=c["success_dark"],
                     foreground="#ffffff")
    style.map("Success.TButton",
              background=[("active", c["success"]), ("pressed", "#15803d")],
              foreground=[("active", "#ffffff"), ("pressed", "#ffffff")])

    style.configure("Warning.TButton", **_btn_common,
                     background=c["warning_dark"],
                     foreground="#ffffff")
    style.map("Warning.TButton",
              background=[("active", c["warning"]), ("pressed", "#b45309")],
              foreground=[("active", "#ffffff"), ("pressed", "#ffffff")])

    style.configure("Danger.TButton", **_btn_common,
                     background=c["danger_dark"],
                     foreground="#ffffff")
    style.map("Danger.TButton",
              background=[("active", c["danger"]), ("pressed", "#b91c1c")],
              foreground=[("active", "#ffffff"), ("pressed", "#ffffff")])

    _small = dict(font=font_btn_small, padding=[14, 8])
    style.configure("Small.TButton",      **{**_btn_common, **_small},
                     background=c["surface_soft"], foreground=c["text"])
    style.map("Small.TButton",
              background=[("active", c["surface_hover"]), ("pressed", c["surface"])],
              foreground=[("active", c["text"]), ("pressed", c["text"])])

    style.configure("SmallAccent.TButton", **{**_btn_common, **_small},
                     background=c["accent"], foreground="#ffffff")
    style.map("SmallAccent.TButton",
              background=[("active", c["accent_hover"]), ("pressed", c["accent_dark"])],
              foreground=[("active", "#ffffff"), ("pressed", "#ffffff")])

    style.configure("SmallSuccess.TButton", **{**_btn_common, **_small},
                     background=c["success_dark"], foreground="#ffffff")
    style.map("SmallSuccess.TButton",
              background=[("active", c["success"]), ("pressed", "#15803d")],
              foreground=[("active", "#ffffff"), ("pressed", "#ffffff")])

    style.configure("SmallDanger.TButton", **{**_btn_common, **_small},
                     background=c["danger_dark"], foreground="#ffffff")
    style.map("SmallDanger.TButton",
              background=[("active", c["danger"]), ("pressed", "#b91c1c")],
              foreground=[("active", "#ffffff"), ("pressed", "#ffffff")])

    # ── Labels ────────────────────────────────────────────────────
    style.configure("TLabel",
                     font=font_base,
                     foreground=c["text"],
                     background=c["app_bg"])
    style.configure("Heading.TLabel",
                     font=font_heading,
                     foreground=c["text"],
                     background=c["app_bg"])
    style.configure("Subheading.TLabel",
                     font=font_subheading,
                     foreground=c["text_secondary"],
                     background=c["app_bg"])

    # ── Entry – bordes sutiles, focus con acento ───────────────────
    style.configure("TEntry",
                     font=font_base,
                     padding=[14, 10],
                     fieldbackground=c["surface"],
                     foreground=c["text"],
                     borderwidth=1,
                     bordercolor=c["border"],
                     insertcolor=c["text"],
                     relief="flat")
    style.map("TEntry",
              fieldbackground=[("focus", c["surface_alt"])],
              bordercolor=[("focus", c["accent"])],
              foreground=[("disabled", c["muted"])])

    # ── Combobox ───────────────────────────────────────────────────
    style.configure("TCombobox",
                     font=font_base,
                     padding=[14, 10],
                     fieldbackground=c["surface"],
                     foreground=c["text"],
                     borderwidth=1,
                     bordercolor=c["border"],
                     arrowcolor=c["text_secondary"],
                     relief="flat")
    style.map("TCombobox",
              fieldbackground=[("focus", c["surface_alt"]), ("readonly", c["surface"])],
              bordercolor=[("focus", c["accent"])],
              foreground=[("disabled", c["muted"])])

    # ── Treeview – filas más altas, selección suave ─────────────────
    style.configure("Treeview",
                     font=font_base,
                     background=c["surface"],
                     fieldbackground=c["surface"],
                     foreground=c["text"],
                     rowheight=40,
                     borderwidth=0)
    style.configure("Treeview.Heading",
                     font=font_subheading,
                     background=c["surface_alt"],
                     foreground=c["text_secondary"],
                     padding=[14, 12],
                     relief="flat",
                     borderwidth=0)
    style.map("Treeview",
              background=[("selected", c["accent_dark"])],
              foreground=[("selected", "#ffffff")])
    style.map("Treeview.Heading",
              background=[("active", c["surface_soft"])])

    # ── LabelFrame (tarjetas) – bordes suaves ───────────────────────
    style.configure("TLabelframe",
                     background=c["surface"],
                     borderwidth=1,
                     bordercolor=c["border_light"],
                     relief="flat")
    style.configure("TLabelframe.Label",
                     font=font_subheading,
                     foreground=c["accent_hover"],
                     background=c["surface"])

    # ── Checkbutton / Radiobutton ───────────────────────────────────
    style.configure("TCheckbutton",
                     font=font_base,
                     foreground=c["text"],
                     background=c["app_bg"],
                     indicatorcolor=c["surface"],
                     indicatorrelief="flat")
    style.map("TCheckbutton",
              indicatorcolor=[("selected", c["accent"])],
              background=[("active", c["app_bg"])])

    style.configure("TRadiobutton",
                     font=font_base,
                     foreground=c["text"],
                     background=c["app_bg"],
                     indicatorcolor=c["surface"],
                     indicatorrelief="flat")
    style.map("TRadiobutton",
              indicatorcolor=[("selected", c["accent"])],
              background=[("active", c["app_bg"])])

    # ── Scrollbar – minimalista ───────────────────────────────────
    _sb = dict(
        borderwidth=0,
        relief="flat",
        troughcolor=c["surface"],
        background=c["surface_soft"],
        arrowcolor=c["muted"],
        bordercolor=c["surface"],
    )
    style.configure("Vertical.TScrollbar",   **_sb)
    style.configure("Horizontal.TScrollbar", **_sb)
    style.map("Vertical.TScrollbar",
              background=[("active", c["surface_hover"])])
    style.map("Horizontal.TScrollbar",
              background=[("active", c["surface_hover"])])

    # ── Separator ──────────────────────────────────────────────────
    style.configure("TSeparator", background=c["border"])

    # ── PanedWindow ────────────────────────────────────────────────
    style.configure("TPanedwindow", background=c["app_bg"])

    # ── Progressbar – gradiente visual con acento ───────────────────
    style.configure("Horizontal.TProgressbar",
                     troughcolor=c["surface"],
                     background=c["accent"],
                     borderwidth=0)


# ── Animaciones y efectos hover ─────────────────────────────────────
def apply_hover_effects(root_widget):
    """
    Recorre los widgets y aplica efectos hover a botones.
    Incluye transición suave al pasar el ratón.
    """
    _walk_and_bind(root_widget)
    _bind_tab_transitions(root_widget)


def _walk_and_bind(widget):
    """Recorrido recursivo para bind de hover."""
    try:
        widget_class = widget.winfo_class()
        if widget_class == "TButton":
            _bind_button_hover(widget)
        for child in widget.winfo_children():
            _walk_and_bind(child)
    except Exception:
        pass


def _bind_button_hover(btn):
    """Efecto hover con transición suave."""
    def on_enter(e):
        try:
            btn.state(["active"])
        except Exception:
            pass

    def on_leave(e):
        try:
            btn.state(["!active"])
        except Exception:
            pass

    btn.bind("<Enter>", on_enter, add="+")
    btn.bind("<Leave>", on_leave, add="+")


def _bind_tab_transitions(root_widget):
    """Aplica transición suave al cambiar de pestaña."""
    try:
        for w in root_widget.winfo_children():
            if w.winfo_class() == "TNotebook":
                w.bind("<<NotebookTabChanged>>", _on_tab_changed)
                break
    except Exception:
        pass


def _on_tab_changed(event):
    """Callback al cambiar pestaña – efecto visual sutil."""
    pass  # placeholder para futuras animaciones en tkinter
