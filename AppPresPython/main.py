"""
Punto de entrada principal de la aplicación
Sistema de Gestión de Presupuestos
"""

import sys
import traceback
import tkinter as tk
from ui.app import AppPresupuestos

def main():
    """Función principal que inicia la aplicación"""
    try:
        root = tk.Tk()
    except Exception as e:
        print("No se pudo inicializar Tkinter:", e)
        traceback.print_exc()
        return

    try:
        app = AppPresupuestos(root)
    except Exception as e:
        print("Error iniciando la aplicación:", e)
        traceback.print_exc()
        try:
            root.destroy()
        except Exception:
            pass
        return

    try:
        root.mainloop()
    except KeyboardInterrupt:
        print("Aplicación detenida por el usuario.")
    except Exception as e:
        print("Error en el bucle principal:", e)
        traceback.print_exc()

if __name__ == "__main__":
    main()
