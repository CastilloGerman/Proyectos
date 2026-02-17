import smtplib
import json
import os
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from email.mime.base import MIMEBase
from email import encoders
from typing import Optional, Tuple
import tkinter as tk
from tkinter import messagebox, simpledialog, ttk

class EmailSender:
    def __init__(self):
        self.smtp_server = None
        self.smtp_port = None
        self.email = None
        self.password = None
        self.configurado = False
        self.config_file = "config/email_config.json"
        
        # Cargar configuración al inicializar
        self.cargar_configuracion()
    
    def configurar_email(self, smtp_server: str, smtp_port: int, email: str, password: str):
        """Configura los datos del servidor de email"""
        self.smtp_server = smtp_server
        self.smtp_port = smtp_port
        self.email = email
        self.password = password
        self.configurado = True
        
        # Guardar configuración automáticamente
        self.guardar_configuracion()
    
    def guardar_configuracion(self):
        """Guarda la configuración del email en un archivo JSON"""
        try:
            config = {
                "smtp_server": self.smtp_server,
                "smtp_port": self.smtp_port,
                "email": self.email,
                "password": self.password,
                "configurado": self.configurado
            }
            with open(self.config_file, 'w', encoding='utf-8') as f:
                json.dump(config, f, indent=2, ensure_ascii=False)
        except Exception as e:
            print(f"Error guardando configuración de email: {e}")
    
    def cargar_configuracion(self):
        """Carga la configuración del email desde un archivo JSON"""
        try:
            if os.path.exists(self.config_file):
                with open(self.config_file, 'r', encoding='utf-8') as f:
                    config = json.load(f)
                    self.smtp_server = config.get("smtp_server")
                    self.smtp_port = int(config.get("smtp_port")) if config.get("smtp_port") else None
                    self.email = config.get("email")
                    self.password = config.get("password")
                    self.configurado = config.get("configurado", False)
        except Exception as e:
            print(f"Error cargando configuración de email: {e}")
            self.configurado = False
    
    def mostrar_configuracion_email(self, parent_window):
        """Muestra una ventana para configurar el email"""
        config_window = tk.Toplevel(parent_window)
        config_window.title("Configuración de Email")
        config_window.geometry("700x550")
        config_window.configure(bg='#ecf0f1')
        config_window.transient(parent_window)
        config_window.grab_set()
        
        # Centrar la ventana
        config_window.update_idletasks()
        x = (config_window.winfo_screenwidth() // 2) - (600 // 2)
        y = (config_window.winfo_screenheight() // 2) - (550 // 2)
        config_window.geometry(f"600x550+{x}+{y}")
        
        # Frame principal con padding
        main_frame = ttk.Frame(config_window, padding=20)
        main_frame.pack(fill='both', expand=True)
        
        # Título principal
        title_label = ttk.Label(main_frame, text="⚙️ Configuración de Email", 
                               font=('Segoe UI', 16, 'bold'))
        title_label.pack(pady=(0, 20))
        
        # Frame para los campos de configuración
        config_frame = ttk.LabelFrame(main_frame, text="Datos del Servidor SMTP", padding=15)
        config_frame.pack(fill='x', pady=(0, 15))
        
        # Proveedor de email
        ttk.Label(config_frame, text="Proveedor de Email:", font=('Segoe UI', 9, 'bold')).grid(row=0, column=0, sticky='w', pady=(0, 5))
        
        # Configuraciones predefinidas de proveedores
        proveedores = {
            "Gmail": {"servidor": "smtp.gmail.com", "puerto": "587"},
            "Outlook/Hotmail": {"servidor": "smtp-mail.outlook.com", "puerto": "587"},
            "Yahoo Mail": {"servidor": "smtp.mail.yahoo.com", "puerto": "587"},
            "iCloud": {"servidor": "smtp.mail.me.com", "puerto": "587"},
            "Zoho Mail": {"servidor": "smtp.zoho.com", "puerto": "587"},
            "ProtonMail": {"servidor": "smtp.protonmail.com", "puerto": "587"},
            "Personalizado": {"servidor": "", "puerto": ""}
        }
        
        # Servidor SMTP
        ttk.Label(config_frame, text="Servidor SMTP:", font=('Segoe UI', 9, 'bold')).grid(row=1, column=0, sticky='w', pady=(0, 5))
        smtp_entry = ttk.Entry(config_frame, width=40, font=('Segoe UI', 9))
        smtp_entry.grid(row=1, column=1, sticky='ew', pady=(0, 10), padx=(10, 0))
        
        # Puerto
        ttk.Label(config_frame, text="Puerto:", font=('Segoe UI', 9, 'bold')).grid(row=2, column=0, sticky='w', pady=(0, 5))
        port_entry = ttk.Entry(config_frame, width=40, font=('Segoe UI', 9))
        port_entry.grid(row=2, column=1, sticky='ew', pady=(0, 10), padx=(10, 0))
        
        # Función para actualizar servidor y puerto según el proveedor seleccionado
        def actualizar_configuracion_proveedor(event=None):
            proveedor_seleccionado = proveedor_var.get()
            
            if proveedor_seleccionado in proveedores:
                config = proveedores[proveedor_seleccionado]
                
                # Habilitar temporalmente para poder modificar
                smtp_entry.config(state='normal')
                port_entry.config(state='normal')
                
                # Limpiar campos
                smtp_entry.delete(0, tk.END)
                port_entry.delete(0, tk.END)
                
                # Insertar nuevos valores
                smtp_entry.insert(0, config["servidor"])
                port_entry.insert(0, config["puerto"])
                
                # Configurar estado final según selección
                if proveedor_seleccionado == "Personalizado":
                    smtp_entry.focus()
                else:
                    smtp_entry.config(state='readonly')
                    port_entry.config(state='readonly')
        
        # Crear variable y combobox después de definir la función
        proveedor_var = tk.StringVar(value="Gmail")
        proveedor_combo = ttk.Combobox(config_frame, textvariable=proveedor_var, 
                                      values=list(proveedores.keys()), 
                                      state="readonly", width=37, font=('Segoe UI', 9))
        proveedor_combo.grid(row=0, column=1, sticky='ew', pady=(0, 10), padx=(10, 0))
        
        # Bind el evento de selección
        proveedor_combo.bind('<<ComboboxSelected>>', actualizar_configuracion_proveedor)
        
        # También bind cuando se cambie el texto (para mayor compatibilidad)
        proveedor_combo.bind('<FocusOut>', actualizar_configuracion_proveedor)
        
        # Configurar valores iniciales
        actualizar_configuracion_proveedor()
        
        # Email
        ttk.Label(config_frame, text="Email:", font=('Segoe UI', 9, 'bold')).grid(row=3, column=0, sticky='w', pady=(0, 5))
        email_entry = ttk.Entry(config_frame, width=40, font=('Segoe UI', 9))
        email_entry.grid(row=3, column=1, sticky='ew', pady=(0, 10), padx=(10, 0))
        
        # Contraseña
        ttk.Label(config_frame, text="Contraseña:", font=('Segoe UI', 9, 'bold')).grid(row=4, column=0, sticky='w', pady=(0, 5))
        password_entry = ttk.Entry(config_frame, width=40, font=('Segoe UI', 9), show="*")
        password_entry.grid(row=4, column=1, sticky='ew', pady=(0, 10), padx=(10, 0))
        
        # Cargar valores guardados si existen
        if self.configurado:
            email_entry.insert(0, self.email or "")
            password_entry.insert(0, self.password or "")
            
            # Buscar el proveedor correspondiente
            for proveedor, config in proveedores.items():
                if config['servidor'] == self.smtp_server and config['puerto'] == self.smtp_port:
                    proveedor_combo.set(proveedor)
                    break
            else:
                # Si no encuentra coincidencia, usar "Personalizado"
                proveedor_combo.set("Personalizado")
                smtp_entry.config(state='normal')
                port_entry.config(state='normal')
                smtp_entry.insert(0, self.smtp_server or "")
                port_entry.insert(0, str(self.smtp_port) if self.smtp_port else "")
                smtp_entry.config(state='readonly')
                port_entry.config(state='readonly')
        
        # Configurar el grid para que se expanda
        config_frame.columnconfigure(1, weight=1)
        
        # Frame para información adicional
        info_frame = ttk.LabelFrame(main_frame, text="ℹ️ Información Importante", padding=15)
        info_frame.pack(fill='x', pady=(0, 20))
        
        info_text = """🔐 INFORMACIÓN IMPORTANTE:
• Gmail, Yahoo, iCloud: Requieren contraseña de aplicación (NO tu contraseña normal)
• Outlook/Hotmail: Puede usar contraseña normal o de aplicación
• Zoho, ProtonMail: Consulta la configuración específica de tu proveedor

📧 PROVEEDORES SOPORTADOS:
• Selecciona tu proveedor en el desplegable para configuración automática
• Los campos Servidor y Puerto se llenan automáticamente
• Opción "Personalizado" para otros proveedores"""
        
        info_label = ttk.Label(info_frame, text=info_text, font=('Segoe UI', 9), 
                              foreground='#7f8c8d', wraplength=500, justify='left')
        info_label.pack()
        
        # Frame para los botones
        button_frame = ttk.Frame(main_frame)
        button_frame.pack(fill='x', pady=(20, 10))
        
        def guardar_configuracion():
            try:
                smtp = smtp_entry.get().strip()
                port_str = port_entry.get().strip()
                email = email_entry.get().strip()
                password = password_entry.get().strip()
                
                if not all([smtp, port_str, email, password]):
                    messagebox.showerror("Error", "Todos los campos son obligatorios")
                    return
                
                try:
                    port = int(port_str)
                except ValueError:
                    messagebox.showerror("Error", "El puerto debe ser un número válido")
                    return
                
                self.configurar_email(smtp, port, email, password)
                messagebox.showinfo("Éxito", "✅ Configuración de email guardada correctamente")
                config_window.destroy()
                
            except Exception as e:
                messagebox.showerror("Error", f"Error al configurar email: {str(e)}")
        
        def cancelar():
            config_window.destroy()
        
        # Botones con estilos - Primera fila
        button_row1 = ttk.Frame(button_frame)
        button_row1.pack(fill='x', pady=(0, 10))
        
        save_button = ttk.Button(button_row1, text="💾 Guardar Configuración", 
                                command=guardar_configuracion, style='Accent.TButton')
        save_button.pack(side='left', padx=(0, 15))
        
        cancel_button = ttk.Button(button_row1, text="❌ Cancelar", 
                                  command=cancelar, style='TButton')
        cancel_button.pack(side='left', padx=(0, 15))
        
        def eliminar_configuracion():
            if messagebox.askyesno("Confirmar", "¿Estás seguro de que quieres eliminar la configuración de email guardada?"):
                try:
                    if os.path.exists(self.config_file):
                        os.remove(self.config_file)
                    self.smtp_server = None
                    self.smtp_port = None
                    self.email = None
                    self.password = None
                    self.configurado = False
                    
                    # Limpiar campos
                    email_entry.delete(0, tk.END)
                    password_entry.delete(0, tk.END)
                    smtp_entry.config(state='normal')
                    port_entry.config(state='normal')
                    smtp_entry.delete(0, tk.END)
                    port_entry.delete(0, tk.END)
                    smtp_entry.config(state='readonly')
                    port_entry.config(state='readonly')
                    proveedor_combo.set("Gmail")
                    actualizar_configuracion_proveedor()
                    
                    messagebox.showinfo("Éxito", "✅ Configuración eliminada correctamente")
                except Exception as e:
                    messagebox.showerror("Error", f"Error al eliminar configuración: {str(e)}")
        
        delete_button = ttk.Button(button_row1, text="🗑️ Eliminar Configuración", 
                                  command=eliminar_configuracion, style='Warning.TButton')
        delete_button.pack(side='left', padx=(0, 15))
        
        # Botón de prueba (opcional)
        def probar_conexion():
            try:
                smtp = smtp_entry.get().strip()
                port_str = port_entry.get().strip()
                email = email_entry.get().strip()
                password = password_entry.get().strip()
                
                if not all([smtp, port_str, email, password]):
                    messagebox.showwarning("Advertencia", "Complete todos los campos para probar la conexión")
                    return
                
                try:
                    port = int(port_str)
                except ValueError:
                    messagebox.showerror("Error", "El puerto debe ser un número válido")
                    return
                
                # Probar conexión
                server = smtplib.SMTP(smtp, port)
                server.starttls()
                server.login(email, password)
                server.quit()
                
                messagebox.showinfo("Éxito", "✅ Conexión exitosa. La configuración es correcta.")
                
            except Exception as e:
                messagebox.showerror("Error de Conexión", f"❌ No se pudo conectar:\n{str(e)}")
        
        test_button = ttk.Button(button_row1, text="🔍 Probar Conexión", 
                                command=probar_conexion, style='Warning.TButton')
        test_button.pack(side='right', padx=(0, 10))
        
        # Botón de ayuda
        def mostrar_ayuda():
            help_window = tk.Toplevel(config_window)
            help_window.title("Ayuda - Configuración de Email")
            help_window.geometry("700x600")
            help_window.configure(bg='#ecf0f1')
            help_window.transient(config_window)
            help_window.grab_set()
            
            # Centrar la ventana
            help_window.update_idletasks()
            x = (help_window.winfo_screenwidth() // 2) - (700 // 2)
            y = (help_window.winfo_screenheight() // 2) - (600 // 2)
            help_window.geometry(f"700x600+{x}+{y}")
            
            # Frame principal
            main_frame = ttk.Frame(help_window, padding=20)
            main_frame.pack(fill='both', expand=True)
            
            # Título
            ttk.Label(main_frame, text="📧 Guía de Configuración de Email", 
                     font=('Segoe UI', 16, 'bold')).pack(pady=(0, 20))
            
            # Crear notebook para pestañas
            notebook = ttk.Notebook(main_frame)
            notebook.pack(fill='both', expand=True)
            
            # Pestaña Gmail
            gmail_frame = ttk.Frame(notebook, padding=15)
            notebook.add(gmail_frame, text="Gmail")
            
            gmail_text = """🔐 CONFIGURACIÓN PARA GMAIL:

PASO 1: Activar Verificación en 2 Pasos
• Ve a: https://myaccount.google.com/security
• Activa "Verificación en 2 pasos" si no está activada

PASO 2: Crear Contraseña de Aplicación
• En la misma página, busca "Contraseñas de aplicación"
• Selecciona "Correo" como aplicación
• Copia la contraseña de 16 caracteres generada

PASO 3: Configurar en la Aplicación
• Selecciona "Gmail" en el desplegable (se configura automáticamente)
• Email: tu_email@gmail.com
• Contraseña: la contraseña de 16 caracteres (NO tu contraseña normal)

⚠️ IMPORTANTE: 
• La contraseña de aplicación es diferente a tu contraseña de Gmail
• Tiene 16 caracteres sin espacios
• Solo funciona con la verificación en 2 pasos activada"""
            
            gmail_label = ttk.Label(gmail_frame, text=gmail_text, font=('Segoe UI', 10), 
                                   wraplength=650, justify='left')
            gmail_label.pack(anchor='w')
            
            # Pestaña Outlook
            outlook_frame = ttk.Frame(notebook, padding=15)
            notebook.add(outlook_frame, text="Outlook/Hotmail")
            
            outlook_text = """📧 CONFIGURACIÓN PARA OUTLOOK/HOTMAIL:

PASO 1: Configuración Básica
• Selecciona "Outlook/Hotmail" en el desplegable (se configura automáticamente)
• Email: tu_email@outlook.com o @hotmail.com
• Contraseña: tu contraseña normal de Outlook

PASO 2: Si tienes Verificación en 2 Pasos
• Ve a: https://account.microsoft.com/security
• Busca "Contraseñas de aplicación"
• Crea una contraseña de aplicación para "Correo"
• Usa esa contraseña en lugar de la normal

PASO 3: Configuración en la Aplicación
• Los campos Servidor y Puerto se llenan automáticamente
• Email: tu_email@outlook.com
• Contraseña: contraseña de aplicación o normal"""
            
            outlook_label = ttk.Label(outlook_frame, text=outlook_text, font=('Segoe UI', 10), 
                                     wraplength=650, justify='left')
            outlook_label.pack(anchor='w')
            
            # Pestaña Otros
            otros_frame = ttk.Frame(notebook, padding=15)
            notebook.add(otros_frame, text="Otros Proveedores")
            
            otros_text = """🌐 CONFIGURACIÓN PARA OTROS PROVEEDORES:

PROVEEDORES INCLUIDOS EN EL DESPLEGABLE:
• Yahoo Mail: Requiere contraseña de aplicación
• iCloud: Requiere contraseña de aplicación  
• Zoho Mail: Consulta configuración específica
• ProtonMail: Requiere configuración especial

PROVEEDORES EMPRESARIALES:
• Usa la opción "Personalizado" en el desplegable
• Consulta con tu administrador de IT
• Servidor SMTP: proporcionado por tu empresa
• Puerto: generalmente 587 o 465

CONFIGURACIÓN PERSONALIZADA:
• Selecciona "Personalizado" en el desplegable
• Ingresa manualmente el servidor y puerto
• Puerto 587: Usa TLS (recomendado)
• Puerto 465: Usa SSL

🔧 SOLUCIÓN DE PROBLEMAS:
• Error "Application-specific password required": Necesitas contraseña de aplicación
• Error "Username and Password not accepted": Verifica las credenciales
• Error de conexión: Verifica el servidor SMTP y puerto
• Usa el botón "Probar Conexión" antes de guardar"""
            
            otros_label = ttk.Label(otros_frame, text=otros_text, font=('Segoe UI', 10), 
                                   wraplength=650, justify='left')
            otros_label.pack(anchor='w')
            
            # Botón cerrar
            ttk.Button(main_frame, text="Cerrar", command=help_window.destroy).pack(pady=(20, 0))
        
        help_button = ttk.Button(button_row1, text="❓ Ayuda", 
                                command=mostrar_ayuda, style='TButton')
        help_button.pack(side='right')
        
        # Hacer que la ventana sea redimensionable
        config_window.resizable(True, True)
        
        # Enfocar el primer campo
        smtp_entry.focus()
    
    def enviar_presupuesto_por_email(self, presupuesto: dict, archivo_pdf: str, email_destino: str = None) -> Tuple[bool, str]:
        """Envía el presupuesto por email
        
        Returns:
            tuple: (éxito: bool, mensaje: str)
        """
        if not self.configurado:
            return False, "El email no está configurado"
        
        if not email_destino:
            email_destino = presupuesto.get('email', '')
            if not email_destino:
                return False, "No se especificó email de destino"
        
        try:
            # Crear el mensaje
            msg = MIMEMultipart()
            msg['From'] = self.email
            msg['To'] = email_destino
            msg['Subject'] = f"Presupuesto Nº {presupuesto['id']} - {presupuesto['cliente_nombre']}"
            
            # Cuerpo del mensaje
            body = f"""
Estimado/a {presupuesto['cliente_nombre']},

Adjunto encontrará el presupuesto Nº {presupuesto['id']} solicitado.

Detalles del presupuesto:
- Fecha: {presupuesto['fecha_creacion'][:10]}
- Total: €{presupuesto['total']:.2f}

Para cualquier consulta, no dude en contactarnos.

Saludos cordiales,
Sistema de Presupuestos
            """
            
            msg.attach(MIMEText(body, 'plain', 'utf-8'))
            
            # Adjuntar el PDF
            if os.path.exists(archivo_pdf):
                with open(archivo_pdf, "rb") as attachment:
                    part = MIMEBase('application', 'octet-stream')
                    part.set_payload(attachment.read())
                
                encoders.encode_base64(part)
                part.add_header(
                    'Content-Disposition',
                    f'attachment; filename= {os.path.basename(archivo_pdf)}'
                )
                msg.attach(part)
            
            # Enviar el email
            server = smtplib.SMTP(self.smtp_server, self.smtp_port)
            server.starttls()
            server.login(self.email, self.password)
            text = msg.as_string()
            server.sendmail(self.email, email_destino, text)
            server.quit()
            
            return True, "Email enviado correctamente"
            
        except Exception as e:
            error_msg = f"Error al enviar email: {str(e)}"
            print(error_msg)
            return False, error_msg
    
    def enviar_factura(self, destinatario: str, factura: dict, archivo_pdf: str) -> bool:
        """Envía la factura por email"""
        if not self.configurado:
            messagebox.showwarning("Advertencia", "Debe configurar el email primero")
            return False
        
        try:
            # Crear el mensaje
            msg = MIMEMultipart()
            msg['From'] = self.email
            msg['To'] = destinatario
            msg['Subject'] = f"Factura {factura.get('numero_factura', '')} - {factura['cliente_nombre']}"
            
            # Cuerpo del mensaje
            body = f"""
Estimado/a {factura['cliente_nombre']},

Adjunto encontrará la factura {factura.get('numero_factura', '')} correspondiente a los servicios prestados.

Detalles de la factura:
- Número: {factura.get('numero_factura', 'N/A')}
- Fecha: {factura.get('fecha_creacion', '')[:10] if factura.get('fecha_creacion') else 'N/A'}
- Total: €{factura.get('total', 0):.2f}
- Método de pago: {factura.get('metodo_pago', 'No especificado')}

{f'Fecha de vencimiento: {factura.get("fecha_vencimiento", "")[:10]}' if factura.get('fecha_vencimiento') else ''}

Para cualquier consulta sobre esta factura, no dude en contactarnos.

Saludos cordiales,
Sistema de Facturación
            """
            
            msg.attach(MIMEText(body, 'plain', 'utf-8'))
            
            # Adjuntar el PDF
            if os.path.exists(archivo_pdf):
                with open(archivo_pdf, "rb") as attachment:
                    part = MIMEBase('application', 'octet-stream')
                    part.set_payload(attachment.read())
                
                encoders.encode_base64(part)
                part.add_header(
                    'Content-Disposition',
                    f'attachment; filename= {os.path.basename(archivo_pdf)}'
                )
                msg.attach(part)
            
            # Enviar el email
            server = smtplib.SMTP(self.smtp_server, self.smtp_port)
            server.starttls()
            server.login(self.email, self.password)
            server.send_message(msg)
            server.quit()
            
            return True
            
        except Exception as e:
            print(f"Error al enviar factura por email: {str(e)}")
            messagebox.showerror("Error", f"Error al enviar factura por email: {str(e)}")
            return False
    
    def mostrar_dialogo_envio(self, parent_window, presupuesto: dict, archivo_pdf: str) -> bool:
        """Muestra un diálogo para enviar el presupuesto por email"""
        if not self.configurado:
            messagebox.showwarning("Advertencia", "Debe configurar el email primero")
            return False
        
        # Ventana de diálogo para el email
        email_window = tk.Toplevel(parent_window)
        email_window.title("Enviar Presupuesto por Email")
        email_window.geometry("600x550")
        email_window.configure(bg='#ecf0f1')
        email_window.transient(parent_window)
        email_window.grab_set()
        
        # Centrar la ventana
        email_window.update_idletasks()
        x = (email_window.winfo_screenwidth() // 2) - (600 // 2)
        y = (email_window.winfo_screenheight() // 2) - (550 // 2)
        email_window.geometry(f"600x550+{x}+{y}")
        
        # Frame principal
        main_frame = ttk.Frame(email_window, padding=25)
        main_frame.pack(fill='both', expand=True)
        
        # Título
        title_label = ttk.Label(main_frame, text=f"📧 Enviar Presupuesto Nº {presupuesto['id']}", 
                               font=('Segoe UI', 16, 'bold'))
        title_label.pack(pady=(0, 25))
        
        # Frame para información del presupuesto
        info_frame = ttk.LabelFrame(main_frame, text="Información del Presupuesto", padding=18)
        info_frame.pack(fill='x', pady=(0, 20))
        
        ttk.Label(info_frame, text=f"Cliente: {presupuesto['cliente_nombre']}", 
                 font=('Segoe UI', 10)).pack(anchor='w', pady=(0, 8))
        ttk.Label(info_frame, text=f"Total: €{presupuesto['total']:.2f}", 
                 font=('Segoe UI', 10)).pack(anchor='w', pady=(0, 8))
        ttk.Label(info_frame, text=f"Fecha: {presupuesto['fecha_creacion'][:10]}", 
                 font=('Segoe UI', 10)).pack(anchor='w')
        
        # Frame para el email destino
        email_frame = ttk.LabelFrame(main_frame, text="Destinatario", padding=18)
        email_frame.pack(fill='x', pady=(0, 20))
        
        ttk.Label(email_frame, text="Email destino:", font=('Segoe UI', 10, 'bold')).pack(anchor='w', pady=(0, 10))
        email_entry = ttk.Entry(email_frame, width=50, font=('Segoe UI', 11))
        email_entry.pack(fill='x', pady=(0, 5))
        email_entry.insert(0, presupuesto.get('email', ''))
        
        # Frame para los botones
        button_frame = ttk.Frame(main_frame)
        button_frame.pack(fill='x', pady=(15, 0))
        
        resultado = [False]  # Usar lista para poder modificar desde dentro de las funciones
        
        def enviar():
            email_destino = email_entry.get().strip()
            if not email_destino:
                messagebox.showerror("Error", "Ingrese un email destino")
                return
            
            # Validar formato básico de email
            if '@' not in email_destino or '.' not in email_destino.split('@')[1]:
                messagebox.showerror("Error", "El email ingresado no es válido")
                return
            
            # Mostrar mensaje de confirmación
            if messagebox.askyesno("Confirmar Envío", 
                                 f"¿Está seguro de enviar el presupuesto a {email_destino}?"):
                try:
                    exito, mensaje = self.enviar_presupuesto_por_email(presupuesto, archivo_pdf, email_destino)
                    if exito:
                        messagebox.showinfo("Éxito", "✅ Presupuesto enviado correctamente")
                        resultado[0] = True
                        email_window.destroy()
                    else:
                        messagebox.showerror("Error", f"❌ {mensaje}")
                except Exception as e:
                    messagebox.showerror("Error", f"❌ Error al enviar el presupuesto:\n{str(e)}")
        
        def cancelar():
            email_window.destroy()
        
        # Botones con estilos
        send_button = ttk.Button(button_frame, text="📤 Enviar Presupuesto", 
                                command=enviar, style='Accent.TButton')
        send_button.pack(side='left', padx=(0, 15))
        
        cancel_button = ttk.Button(button_frame, text="❌ Cancelar", 
                                  command=cancelar, style='TButton')
        cancel_button.pack(side='left')
        
        # Enfocar el campo de email
        email_entry.focus()
        email_entry.select_range(0, tk.END)
        
        email_window.wait_window()
        return resultado[0]

# Instancia global del sender de email
email_sender = EmailSender()
