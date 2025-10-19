import tkinter as tk
from tkinter import messagebox
import math
# Lógica de la calculadora
historial = []

def agregar_a_historial(operacion):
    historial.append(operacion)

def mostrar_historial():
    if not historial:
        messagebox.showinfo("Historial", "No hay operaciones registradas aún.")
    else:
        texto = "\n".join(historial[-10:])  # últimas 10
        messagebox.showinfo("Historial", texto)

# UI de la calculadora
ventana = tk.Tk()
ventana.title("Calculadora Básica")
ventana.geometry("289x483")
ventana.config(bg="#f4f6f7")

# Pantalla de entrada
entrada = tk.Entry(ventana, font=("Arial", 24), borderwidth=8, relief="flat",
                   justify="left", bg="#ecf0f1", fg="#2c3e50")
entrada.grid(row=0, column=0, columnspan=1200, padx=5, pady=5, ipady=10)  # Menos separación

# Funciones de botones
def presionar(valor):
    entrada.insert(tk.END, valor)

def limpiar():
    entrada.delete(0, tk.END)

def calcular():
    try:
        expresion = entrada.get().replace("^", "**")  # permitir ^
        resultado = eval(expresion)
        entrada.delete(0, tk.END)
        entrada.insert(0, str(resultado))
        agregar_a_historial(f"{expresion} = {resultado}")
    except ZeroDivisionError:
        entrada.delete(0, tk.END)
        entrada.insert(0, "Error (div/0)")
    except:
        entrada.delete(0, tk.END)
        entrada.insert(0, "Error")

def calcular_raiz():
    try:
        valor = float(entrada.get())
        if valor < 0:
            raise ValueError("Número negativo")
        resultado = math.sqrt(valor)
        entrada.delete(0, tk.END)
        entrada.insert(0, str(resultado))
        agregar_a_historial(f"√{valor} = {resultado}")
    except:
        entrada.delete(0, tk.END)
        entrada.insert(0, "Error")

def calcular_porcentaje():
    try:
        partes = entrada.get().split("%")
        if len(partes) == 2 and partes[0] and partes[1]:
            porc = float(partes[0])
            num = float(partes[1])
            resultado = (porc * num) / 100
            entrada.delete(0, tk.END)
            entrada.insert(0, str(resultado))
            agregar_a_historial(f"{porc}% de {num} = {resultado}")
        else:
            entrada.delete(0, tk.END)
            entrada.insert(0, "Error")
    except Exception:
        entrada.delete(0, tk.END)
        entrada.insert(0, "Error")

# Estilos de botones
estilo_num = {"bg": "#ffffff", "fg": "#2c3e50", "font": ("Arial", 16), "width": 5, "height": 2, "relief": "flat"}
estilo_op = {"bg": "#3498db", "fg": "white", "font": ("Arial", 16), "width": 5, "height": 2, "relief": "flat"}
estilo_func = {"bg": "#2ecc71", "fg": "white", "font": ("Arial", 16), "width": 5, "height": 2, "relief": "flat"}
estilo_hist = {"bg": "#e67e22", "fg": "white", "font": ("Arial", 16), "width": 23, "height": 2, "relief": "flat"}

# Botones numéricos y operadores
botones = [
    ("7", 1, 0, estilo_num), ("8", 1, 1, estilo_num), ("9", 1, 2, estilo_num), ("/", 1, 3, estilo_op),
    ("4", 2, 0, estilo_num), ("5", 2, 1, estilo_num), ("6", 2, 2, estilo_num), ("*", 2, 3, estilo_op),
    ("1", 3, 0, estilo_num), ("2", 3, 1, estilo_num), ("3", 3, 2, estilo_num), ("-", 3, 3, estilo_op),
    ("0", 4, 0, estilo_num), (".", 4, 1, estilo_num), ("^", 4, 2, estilo_op), ("+", 4, 3, estilo_op),
]

for (texto, fila, columna, estilo) in botones:
    b = tk.Button(ventana, text=texto, **estilo, command=lambda t=texto: presionar(t))
    b.grid(row=fila, column=columna, padx=1, pady=0)  # Menos separación lateral y vertical

# Botones especiales
tk.Button(ventana, text="C", **estilo_op, command=limpiar).grid(row=5, column=0, padx=1, pady=0)
tk.Button(ventana, text="=", **estilo_func, command=calcular).grid(row=5, column=1, padx=1, pady=0)
tk.Button(ventana, text="√", **estilo_func, command=calcular_raiz).grid(row=5, column=2, padx=1, pady=0)
tk.Button(ventana, text="%", **estilo_func, command=calcular_porcentaje).grid(row=5, column=3, padx=1, pady=0)

# Botón historial
tk.Button(ventana, text="📜 Ver historial", **estilo_hist, command=mostrar_historial).grid(row=6, column=0, columnspan=4, padx=2, pady=2)  # Menos separación
ventana.mainloop()