"""
Punto de entrada principal de la aplicación
Sistema de Gestión de Presupuestos
"""

import tkinter as tk
from ui.app import AppPresupuestos

def main():
    """Función principal que inicia la aplicación"""
    root = tk.Tk()
    app = AppPresupuestos(root)
    root.mainloop()

if __name__ == "__main__":
    main()
