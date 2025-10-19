import tkinter as tk
try:
    from ..client.GuiApp import Frame, BarraMenu
except ImportError:
    import os
    import sys
    sys.path.append(os.path.dirname(os.path.dirname(__file__)))
    from client.GuiApp import Frame, BarraMenu

def main():
    root = tk.Tk()
    root.title("Catálogo de Películas")
    app = Frame(root=root)
    BarraMenu(root)
    app.mainloop()
    
if __name__ == "__main__":
    main()
