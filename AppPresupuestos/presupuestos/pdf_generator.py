from reportlab.lib.pagesizes import A4, letter
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import inch, cm
from reportlab.lib import colors
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, Image, Frame
from reportlab.lib.enums import TA_CENTER, TA_LEFT, TA_RIGHT
from reportlab.pdfgen import canvas
from reportlab.lib.utils import ImageReader
import io
import os
from datetime import datetime
from typing import Dict, Any, List

class PDFGenerator:
    def __init__(self, plantilla_config=None):
        self.styles = getSampleStyleSheet()
        self.plantilla_config = plantilla_config or self.get_default_config()
        self.setup_custom_styles()
    
    def get_default_config(self):
        """Retorna la configuración por defecto"""
        return {
            "empresa": {
                "nombre": "Mi Empresa",
                "direccion": "Calle Principal 123, Ciudad",
                "telefono": "+34 123 456 789",
                "email": "info@miempresa.com",
                "web": "www.miempresa.com",
                "cif": "B12345678"
            },
            "logo": {
                "usar_logo": False,
                "ruta_logo": "",
                "texto_logo": "PRESUPUESTOS",
                "tamaño_logo": 24
            },
            "colores": {
                "color_principal": "#2c3e50",
                "color_secundario": "#3498db",
                "color_texto": "#2c3e50"
            },
            "texto_personalizado": {
                "titulo_principal": "PRESUPUESTO",
                "notas_pie": "• Este presupuesto tiene una validez de 30 días.\n• Los precios incluyen IVA.\n• Para cualquier consulta, contacte con nosotros.",
                "texto_iva_incluido": "Los precios incluyen IVA.",
                "texto_iva_no_incluido": "Los precios NO incluyen IVA."
            },
            "margenes": {
                "superior": 72,
                "inferior": 18,
                "izquierdo": 72,
                "derecho": 72
            }
        }
    
    def setup_custom_styles(self):
        """Configura estilos personalizados para el PDF"""
        # Obtener colores de la configuración
        color_principal = self.plantilla_config['colores']['color_principal']
        color_secundario = self.plantilla_config['colores']['color_secundario']
        color_texto = self.plantilla_config['colores']['color_texto']
        
        # Convertir colores hex a RGB
        def hex_to_rgb(hex_color):
            hex_color = hex_color.lstrip('#')
            return tuple(int(hex_color[i:i+2], 16)/255.0 for i in (0, 2, 4))
        
        try:
            color_principal_rgb = hex_to_rgb(color_principal)
            color_secundario_rgb = hex_to_rgb(color_secundario)
            color_texto_rgb = hex_to_rgb(color_texto)
        except:
            # Colores por defecto si hay error
            color_principal_rgb = (0.17, 0.24, 0.31)  # darkblue
            color_secundario_rgb = (0.20, 0.60, 0.86)  # blue
            color_texto_rgb = (0.17, 0.24, 0.31)  # darkblue
        
        # Estilo para el título principal
        custom_title = ParagraphStyle(
            name='CustomTitle',
            parent=self.styles['Heading1'],
            fontSize=self.plantilla_config['logo']['tamaño_logo'],
            spaceAfter=30,
            alignment=TA_CENTER,
            textColor=colors.Color(*color_principal_rgb)
        )
        self.styles.add(custom_title)
        
        # Estilo para subtítulos
        custom_heading = ParagraphStyle(
            name='CustomHeading',
            parent=self.styles['Heading2'],
            fontSize=14,
            spaceAfter=12,
            textColor=colors.Color(*color_principal_rgb)
        )
        self.styles.add(custom_heading)
        
        # Estilo para información del cliente
        client_info = ParagraphStyle(
            name='ClientInfo',
            parent=self.styles['Normal'],
            fontSize=10,
            spaceAfter=6,
            leftIndent=20,
            textColor=colors.Color(*color_texto_rgb)
        )
        self.styles.add(client_info)
        
        # Estilo para totales
        total_style = ParagraphStyle(
            name='TotalStyle',
            parent=self.styles['Normal'],
            fontSize=12,
            spaceAfter=6,
            textColor=colors.Color(*color_principal_rgb),
            alignment=TA_RIGHT
        )
        self.styles.add(total_style)
    
    def create_logo(self):
        """Crea un logo usando imagen o texto según la configuración"""
        if self.plantilla_config['logo']['usar_logo'] and self.plantilla_config['logo']['ruta_logo']:
            # Usar imagen de logo
            try:
                if os.path.exists(self.plantilla_config['logo']['ruta_logo']):
                    return Image(self.plantilla_config['logo']['ruta_logo'], width=2*inch, height=1*inch)
            except Exception as e:
                print(f"Error cargando logo: {e}")
        
        # Usar texto como logo
        logo_text = self.plantilla_config['logo']['texto_logo']
        return logo_text
    
    def generate_presupuesto_pdf(self, presupuesto: Dict[str, Any], output_path: str = None) -> str:
        """Genera un PDF del presupuesto con la nueva plantilla corporativa"""
        if not output_path:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            output_path = f"presupuesto_{presupuesto['id']}_{timestamp}.pdf"
        
        # Crear el documento PDF con márgenes personalizados
        margenes = self.plantilla_config['margenes']
        doc = SimpleDocTemplate(
            output_path,
            pagesize=A4,
            rightMargin=margenes['derecho'],
            leftMargin=margenes['izquierdo'],
            topMargin=margenes['superior'],
            bottomMargin=margenes['inferior']
        )
        
        # Contenido del PDF
        story = []
        
        # HEADER - Logo y nombre de empresa
        header_frame = self.create_header_section(presupuesto)
        story.append(header_frame)
        story.append(Spacer(1, 20))
        
        # BANDA GRIS CON TÍTULO
        titulo_principal = self.plantilla_config['texto_personalizado']['titulo_principal']
        banda_titulo = self.create_title_band(titulo_principal, presupuesto['id'])
        story.append(banda_titulo)
        story.append(Spacer(1, 20))
        
        # INFORMACIÓN DEL CLIENTE Y EMPRESA
        info_frame = self.create_info_section(presupuesto)
        story.append(info_frame)
        story.append(Spacer(1, 20))
        
        # TABLA DE MATERIALES
        tabla_materiales = self.create_materials_table(presupuesto)
        story.append(tabla_materiales)
        story.append(Spacer(1, 20))
        
        # TOTALES
        totales_section = self.create_totals_section(presupuesto)
        story.append(totales_section)
        story.append(Spacer(1, 30))
        
        # FOOTER - Términos de pago
        footer_section = self.create_footer_section()
        story.append(footer_section)
        
        # Construir el PDF
        doc.build(story)
        
        return output_path
    
    def create_header_section(self, presupuesto):
        """Crea la sección del header con logo y nombre de empresa"""
        # Crear frame para el header
        header_data = [
            [self.create_logo_element(), self.create_company_name()]
        ]
        
        header_table = Table(header_data, colWidths=[2*inch, 4*inch])
        header_table.setStyle(TableStyle([
            ('ALIGN', (0, 0), (0, 0), 'LEFT'),
            ('ALIGN', (1, 0), (1, 0), 'LEFT'),
            ('VALIGN', (0, 0), (-1, -1), 'TOP'),
            ('FONTSIZE', (0, 0), (-1, -1), 12),
            ('FONTNAME', (1, 0), (1, 0), 'Helvetica-Bold'),
            ('TEXTCOLOR', (1, 0), (1, 0), colors.HexColor('#2c3e50')),
        ]))
        
        return header_table
    
    def create_logo_element(self):
        """Crea el elemento del logo"""
        if self.plantilla_config['logo']['usar_logo'] and self.plantilla_config['logo']['ruta_logo']:
            try:
                if os.path.exists(self.plantilla_config['logo']['ruta_logo']):
                    return Image(self.plantilla_config['logo']['ruta_logo'], width=1.5*inch, height=0.8*inch)
            except Exception as e:
                print(f"Error cargando logo: {e}")
        
        # Crear logo de texto con estilo
        logo_text = self.plantilla_config['logo']['texto_logo']
        return Paragraph(f"<b>{logo_text}</b>", self.styles['CustomTitle'])
    
    def create_company_name(self):
        """Crea el nombre de la empresa"""
        empresa = self.plantilla_config['empresa']
        return Paragraph(f"<b>{empresa['nombre'].upper()}</b>", self.styles['CustomTitle'])
    
    def create_title_band(self, titulo, presupuesto_id):
        """Crea la banda gris con el título del presupuesto"""
        # Crear banda gris con título centrado
        banda_data = [[f"{titulo} #{presupuesto_id}"]]
        banda_table = Table(banda_data, colWidths=[6*inch])
        banda_table.setStyle(TableStyle([
            ('BACKGROUND', (0, 0), (-1, -1), colors.HexColor('#f8f9fa')),
            ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
            ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
            ('FONTSIZE', (0, 0), (-1, -1), 18),
            ('FONTNAME', (0, 0), (-1, -1), 'Helvetica-Bold'),
            ('TEXTCOLOR', (0, 0), (-1, -1), colors.HexColor('#2c3e50')),
            ('BOTTOMPADDING', (0, 0), (-1, -1), 15),
            ('TOPPADDING', (0, 0), (-1, -1), 15),
        ]))
        
        return banda_table
    
    def create_info_section(self, presupuesto):
        """Crea la sección de información del cliente y empresa"""
        # Información del cliente (izquierda)
        # Formatear el ID del presupuesto de manera segura
        presupuesto_id = presupuesto['id']
        if isinstance(presupuesto_id, str):
            id_formateado = presupuesto_id
        else:
            id_formateado = f"{presupuesto_id:03d}"
        
        cliente_info = f"""
        <b>Presupuesto {id_formateado}</b><br/>
        {presupuesto.get('web', 'www.ejemplo.com')}<br/>
        <b>{presupuesto['cliente_nombre']}</b><br/>
        {presupuesto.get('direccion', 'Dirección no especificada')}<br/>
        {presupuesto.get('telefono', 'Teléfono no especificado')}
        """
        
        # Información de la empresa (derecha)
        empresa = self.plantilla_config['empresa']
        empresa_info = f"""
        <b>{empresa['nombre']}</b><br/>
        {empresa['direccion']}<br/>
        {empresa.get('ciudad', 'Ciudad')}<br/>
        T. {empresa['telefono']}
        """
        
        info_data = [
            [Paragraph(cliente_info, self.styles['Normal']), 
             Paragraph(empresa_info, self.styles['Normal'])]
        ]
        
        info_table = Table(info_data, colWidths=[3*inch, 3*inch])
        info_table.setStyle(TableStyle([
            ('ALIGN', (0, 0), (0, 0), 'LEFT'),
            ('ALIGN', (1, 0), (1, 0), 'RIGHT'),
            ('VALIGN', (0, 0), (-1, -1), 'TOP'),
            ('FONTSIZE', (0, 0), (-1, -1), 10),
            ('TEXTCOLOR', (0, 0), (-1, -1), colors.HexColor('#2c3e50')),
        ]))
        
        return info_table
    
    def create_materials_table(self, presupuesto):
        """Crea la tabla de materiales con el nuevo diseño"""
        # Crear datos de la tabla con nuevas columnas
        table_data = [['DESCRIPCIÓN', 'CANT.', 'PRECIO', 'IVA', 'DESC.', 'TOTAL']]
        
        for item in presupuesto['items']:
            if item.get('visible_pdf', 1):  # Solo mostrar items visibles
                if item.get('es_tarea_manual', 0):
                    # Es una tarea manual
                    nombre_item = item.get('tarea_manual', 'Tarea manual')
                else:
                    # Es un material
                    nombre_item = f"{item['material_nombre']} ({item['unidad_medida']})"
                
                # Calcular descuentos
                descuento_pct = item.get('descuento_porcentaje', 0)
                descuento_fijo = item.get('descuento_fijo', 0)
                descuento_texto = ""
                if descuento_pct > 0:
                    descuento_texto = f"-{descuento_pct:.1f}%"
                elif descuento_fijo > 0:
                    descuento_texto = f"-€{descuento_fijo:.0f}"
                
                # IVA indicator
                iva_indicator = "✓" if item.get('aplica_iva', True) else "✗"
                
                table_data.append([
                    nombre_item,
                    f"{item['cantidad']:.0f}",
                    f"{item['precio_unitario']:.0f}€",
                    iva_indicator,
                    descuento_texto,
                    f"{item['subtotal']:.0f}€"
                ])
        
        # Agregar filas vacías para completar el diseño
        for _ in range(5):
            table_data.append(['', '', '', '', '', ''])
        
        # Crear la tabla con nuevas columnas
        table = Table(table_data, colWidths=[2.8*inch, 0.6*inch, 0.7*inch, 0.4*inch, 0.6*inch, 0.8*inch])
        table.setStyle(TableStyle([
            # Header
            ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#f8f9fa')),
            ('TEXTCOLOR', (0, 0), (-1, 0), colors.HexColor('#2c3e50')),
            ('ALIGN', (0, 0), (-1, 0), 'CENTER'),
            ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
            ('FONTSIZE', (0, 0), (-1, 0), 10),
            ('BOTTOMPADDING', (0, 0), (-1, 0), 12),
            ('TOPPADDING', (0, 0), (-1, 0), 12),
            
            # Data rows
            ('BACKGROUND', (0, 1), (-1, -1), colors.white),
            ('ALIGN', (0, 1), (0, -1), 'LEFT'),  # Descripción a la izquierda
            ('ALIGN', (1, 1), (-1, -1), 'CENTER'),  # Cantidad, precio, total centrados
            ('FONTSIZE', (0, 1), (-1, -1), 9),
            ('TEXTCOLOR', (0, 1), (-1, -1), colors.HexColor('#2c3e50')),
            
            # Grid lines
            ('LINEBELOW', (0, 0), (-1, 0), 1, colors.HexColor('#e9ecef')),
            ('LINEBELOW', (0, 1), (-1, -1), 0.5, colors.HexColor('#e9ecef')),
            ('LINEABOVE', (0, 1), (-1, 1), 0.5, colors.HexColor('#e9ecef')),
        ]))
        
        return table
    
    def create_totals_section(self, presupuesto):
        """Crea la sección de totales alineada a la derecha con descuentos"""
        iva_habilitado = presupuesto.get('iva_habilitado', True)
        
        # Calcular descuentos desde la base de datos
        descuentos_items = 0
        descuento_global = 0
        
        # Calcular descuentos de items
        for item in presupuesto.get('items', []):
            item_subtotal = item['cantidad'] * item['precio_unitario']
            if item.get('descuento_porcentaje', 0) > 0:
                descuentos_items += item_subtotal * (item['descuento_porcentaje'] / 100)
            elif item.get('descuento_fijo', 0) > 0:
                descuentos_items += min(item['descuento_fijo'], item_subtotal)
        
        # Descuento global desde la base de datos
        descuento_global_porcentaje = presupuesto.get('descuento_global_porcentaje', 0)
        descuento_global_fijo = presupuesto.get('descuento_global_fijo', 0)
        
        if descuento_global_porcentaje > 0:
            descuento_global = presupuesto['subtotal'] * (descuento_global_porcentaje / 100)
        elif descuento_global_fijo > 0:
            descuento_global = min(descuento_global_fijo, presupuesto['subtotal'])
        
        # Construir datos de totales
        totales_data = []
        
        # Subtotal original (antes de descuentos)
        subtotal_original = presupuesto['subtotal'] + descuentos_items
        totales_data.append(['SUBTOTAL ORIGINAL', f"{subtotal_original:.0f}€"])
        
        # Descuentos de items
        if descuentos_items > 0:
            totales_data.append(['DESCUENTO ITEMS', f"-{descuentos_items:.0f}€"])
        
        # Subtotal después de descuentos de items
        if descuentos_items > 0:
            totales_data.append(['SUBTOTAL', f"{presupuesto['subtotal']:.0f}€"])
        
        # Descuento global
        if descuento_global > 0:
            totales_data.append(['DESCUENTO GLOBAL', f"-{descuento_global:.0f}€"])
        
        # IVA
        if iva_habilitado:
            totales_data.append(['IVA (21%)', f"{presupuesto['iva']:.0f}€"])
        
        # TOTAL
        totales_data.append(['TOTAL', f"{presupuesto['total']:.0f}€"])
        
        totales_table = Table(totales_data, colWidths=[1.5*inch, 1*inch])
        totales_table.setStyle(TableStyle([
            ('ALIGN', (0, 0), (-1, -1), 'RIGHT'),
            ('FONTSIZE', (0, 0), (-1, -2), 10),
            ('FONTSIZE', (0, -1), (-1, -1), 12),  # Total más grande
            ('FONTNAME', (0, 0), (-1, -2), 'Helvetica'),
            ('FONTNAME', (0, -1), (-1, -1), 'Helvetica-Bold'),  # Total en negrita
            ('TEXTCOLOR', (0, 0), (-1, -1), colors.HexColor('#2c3e50')),
            ('BOTTOMPADDING', (0, 0), (-1, -1), 5),
            ('TOPPADDING', (0, 0), (-1, -1), 5),
        ]))
        
        return totales_table
    
    def create_footer_section(self):
        """Crea la sección del footer con términos de pago"""
        notas_pie = self.plantilla_config['texto_personalizado'].get('notas_pie', '')
        if not notas_pie:
            notas_pie = "Modo de pago 50% a la aceptación del encargo y 50% a su finalización"
        
        footer_text = f"<para align='center'>{notas_pie}</para>"
        return Paragraph(footer_text, self.styles['Normal'])
    
    def generate_simple_presupuesto(self, presupuesto: Dict[str, Any], output_path: str = None) -> str:
        """Genera una versión simplificada del presupuesto"""
        if not output_path:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            output_path = f"presupuesto_simple_{presupuesto['id']}_{timestamp}.pdf"
        
        doc = SimpleDocTemplate(output_path, pagesize=A4)
        story = []
        
        # Título
        story.append(Paragraph("PRESUPUESTO", self.styles['CustomTitle']))
        story.append(Spacer(1, 20))
        
        # Información básica
        info_text = f"""
        <b>Presupuesto Nº:</b> {presupuesto['id']}<br/>
        <b>Fecha:</b> {presupuesto['fecha_creacion'][:10]}<br/>
        <b>Cliente:</b> {presupuesto['cliente_nombre']}<br/>
        <b>Total:</b> €{presupuesto['total']:.2f}
        """
        story.append(Paragraph(info_text, self.styles['Normal']))
        story.append(Spacer(1, 20))
        
        # Items
        story.append(Paragraph("<b>Materiales:</b>", self.styles['CustomHeading']))
        for item in presupuesto['items']:
            item_text = f"• {item['material_nombre']} - {item['cantidad']} {item['unidad_medida']} - €{item['subtotal']:.2f}"
            story.append(Paragraph(item_text, self.styles['Normal']))
        
        doc.build(story)
        return output_path
    
    def generate_factura_pdf(self, factura: Dict[str, Any], output_path: str = None) -> str:
        """Genera un PDF de la factura con la nueva plantilla de diseño"""
        if not output_path:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            numero_limpio = factura.get('numero_factura', factura['id']).replace('/', '_').replace('\\', '_')
            output_path = f"factura_{numero_limpio}_{timestamp}.pdf"
        
        # Crear el documento PDF con márgenes personalizados
        margenes = self.plantilla_config['margenes']
        doc = SimpleDocTemplate(
            output_path,
            pagesize=A4,
            rightMargin=margenes['derecho'],
            leftMargin=margenes['izquierdo'],
            topMargin=margenes['superior'],
            bottomMargin=margenes['inferior']
        )
        
        # Contenido del PDF
        story = []
        
        # HEADER - Título FACTURA y logo según plantilla
        header_template = self.create_factura_header_template(factura)
        story.append(header_template)
        story.append(Spacer(1, 20))
        
        # INFORMACIÓN DEL CLIENTE Y EMPRESA - Dos columnas con bordes
        client_company_info = self.create_factura_client_company_info(factura)
        story.append(client_company_info)
        story.append(Spacer(1, 20))
        
        # TABLA DE ITEMS/SERVICIOS según plantilla
        items_table = self.create_factura_items_table_template(factura)
        story.append(items_table)
        story.append(Spacer(1, 20))
        
        # TOTALES según plantilla (IVA y TOTAL en caja)
        totals_section = self.create_factura_totals_template(factura)
        story.append(totals_section)
        story.append(Spacer(1, 30))
        
        # INFORMACIÓN DE PAGO según plantilla (con fondo beige)
        payment_section = self.create_factura_payment_info_template(factura)
        story.append(payment_section)
        story.append(Spacer(1, 20))
        
        # FOOTER - Notas de la factura
        footer_section = self.create_factura_footer_section(factura)
        story.append(footer_section)
        
        # Construir el PDF
        doc.build(story)
        
        return output_path
    
    def create_factura_title_band(self, factura):
        """Crea la banda con el título FACTURA y el número"""
        numero_factura = factura.get('numero_factura', f"F{str(factura['id']).zfill(4)}")
        banda_data = [[f"FACTURA {numero_factura}"]]
        banda_table = Table(banda_data, colWidths=[6*inch])
        banda_table.setStyle(TableStyle([
            ('BACKGROUND', (0, 0), (-1, -1), colors.HexColor('#f8f9fa')),
            ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
            ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
            ('FONTSIZE', (0, 0), (-1, -1), 20),
            ('FONTNAME', (0, 0), (-1, -1), 'Helvetica-Bold'),
            ('TEXTCOLOR', (0, 0), (-1, -1), colors.HexColor('#2c3e50')),
            ('BOTTOMPADDING', (0, 0), (-1, -1), 15),
            ('TOPPADDING', (0, 0), (-1, -1), 15),
        ]))
        
        return banda_table
    
    def create_factura_info_section(self, factura):
        """Crea la sección de información de la factura, cliente y empresa"""
        # Formatear fechas
        fecha_creacion = factura.get('fecha_creacion', datetime.now().strftime('%Y-%m-%d'))
        if len(fecha_creacion) > 10:
            fecha_creacion = fecha_creacion[:10]
        
        fecha_vencimiento = factura.get('fecha_vencimiento', '')
        if fecha_vencimiento and len(fecha_vencimiento) > 10:
            fecha_vencimiento = fecha_vencimiento[:10]
        
        # Información de la factura y cliente (izquierda)
        cliente_info = f"""
        <b>DATOS DE LA FACTURA</b><br/>
        Fecha: {fecha_creacion}<br/>
        {f'Vencimiento: {fecha_vencimiento}<br/>' if fecha_vencimiento else ''}
        <br/>
        <b>FACTURAR A:</b><br/>
        <b>{factura['cliente_nombre']}</b><br/>
        {factura.get('direccion', 'Dirección no especificada')}<br/>
        {f"DNI/CIF: {factura.get('dni', 'N/A')}<br/>" if factura.get('dni') else ''}
        Tel: {factura.get('telefono', 'N/A')}<br/>
        Email: {factura.get('email', 'N/A')}
        """
        
        # Información de la empresa (derecha)
        empresa = self.plantilla_config['empresa']
        empresa_info = f"""
        <b>{empresa['nombre'].upper()}</b><br/>
        {empresa['direccion']}<br/>
        {empresa.get('ciudad', 'Ciudad')}<br/>
        CIF: {empresa.get('cif', 'N/A')}<br/>
        Tel: {empresa['telefono']}<br/>
        Email: {empresa['email']}
        """
        
        info_data = [
            [Paragraph(cliente_info, self.styles['Normal']), 
             Paragraph(empresa_info, self.styles['Normal'])]
        ]
        
        info_table = Table(info_data, colWidths=[3*inch, 3*inch])
        info_table.setStyle(TableStyle([
            ('ALIGN', (0, 0), (0, 0), 'LEFT'),
            ('ALIGN', (1, 0), (1, 0), 'RIGHT'),
            ('VALIGN', (0, 0), (-1, -1), 'TOP'),
            ('FONTSIZE', (0, 0), (-1, -1), 9),
            ('TEXTCOLOR', (0, 0), (-1, -1), colors.HexColor('#2c3e50')),
        ]))
        
        return info_table
    
    def create_payment_info_section(self, factura):
        """Crea la sección de información de pago"""
        metodo_pago = factura.get('metodo_pago', 'No especificado')
        estado_pago = factura.get('estado_pago', 'No Pagada')
        
        # Color según estado
        color_estado = '#27ae60' if estado_pago == 'Pagada' else '#e74c3c'
        
        pago_info = f"""
        <b>MÉTODO DE PAGO:</b> {metodo_pago}<br/>
        <b>ESTADO:</b> <font color="{color_estado}"><b>{estado_pago.upper()}</b></font>
        """
        
        pago_data = [[Paragraph(pago_info, self.styles['Normal'])]]
        pago_table = Table(pago_data, colWidths=[6*inch])
        pago_table.setStyle(TableStyle([
            ('BACKGROUND', (0, 0), (-1, -1), colors.HexColor('#f8f9fa')),
            ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
            ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
            ('FONTSIZE', (0, 0), (-1, -1), 10),
            ('BOTTOMPADDING', (0, 0), (-1, -1), 10),
            ('TOPPADDING', (0, 0), (-1, -1), 10),
        ]))
        
        return pago_table
    
    def create_factura_footer_section(self, factura):
        """Crea la sección del footer con notas de la factura"""
        notas = factura.get('notas', '')
        if not notas:
            notas = self.plantilla_config['texto_personalizado'].get('notas_pie', 
                'Gracias por su confianza. Para cualquier consulta, contacte con nosotros.')
        
        # Agregar subtítulo "Notas:"
        footer_text = f"<para align='left'><b>Notas:</b><br/><br/>{notas}</para>"
        return Paragraph(footer_text, self.styles['Normal'])
    
    # === NUEVOS MÉTODOS PARA PLANTILLA DE FACTURA ===
    
    def create_factura_header_template(self, factura):
        """Crea el header de la factura según la plantilla con título FACTURA y logo"""
        numero_factura = factura.get('numero_factura', f"F{str(factura['id']).zfill(4)}")
        
        # Título FACTURA a la izquierda
        titulo = Paragraph("<b>FACTURA</b>", ParagraphStyle(
            name='FacturaTitle',
            fontSize=24,
            alignment=TA_LEFT,
            textColor=colors.HexColor('#000000')
        ))
        
        # Logo a la derecha
        logo_element = self.create_factura_logo()
        
        # Número de factura en caja
        numero_caja = Paragraph(f"<b>Nº: {numero_factura.split('-')[0]}</b>", ParagraphStyle(
            name='FacturaNumber',
            fontSize=12,
            alignment=TA_LEFT,
            textColor=colors.HexColor('#000000'),
            borderWidth=1,
            borderColor=colors.HexColor('#000000'),
            leftPadding=8,
            rightPadding=8,
            topPadding=4,
            bottomPadding=4
        ))
        
        # Crear tabla con el layout
        header_data = [
            [titulo, logo_element],
            [numero_caja, '']
        ]
        
        header_table = Table(header_data, colWidths=[4*inch, 2*inch])
        header_table.setStyle(TableStyle([
            ('ALIGN', (0, 0), (0, 0), 'LEFT'),
            ('ALIGN', (1, 0), (1, 0), 'RIGHT'),
            ('VALIGN', (0, 0), (-1, -1), 'TOP'),
            ('LEFTPADDING', (0, 0), (-1, -1), 0),
            ('RIGHTPADDING', (0, 0), (-1, -1), 0),
            ('TOPPADDING', (0, 0), (-1, -1), 0),
            ('BOTTOMPADDING', (0, 0), (-1, -1), 0),
        ]))
        
        return header_table
    
    def create_factura_logo(self):
        """Crea el logo para la factura"""
        if self.plantilla_config['logo']['usar_logo'] and self.plantilla_config['logo']['ruta_logo']:
            try:
                if os.path.exists(self.plantilla_config['logo']['ruta_logo']):
                    return Image(self.plantilla_config['logo']['ruta_logo'], width=1*inch, height=1*inch)
            except Exception as e:
                print(f"Error cargando logo: {e}")
        
        # Crear logo de texto simple
        logo_text = self.plantilla_config['logo']['texto_logo'][0] if self.plantilla_config['logo']['texto_logo'] else 'A'
        return Paragraph(f"<font size='24'><b>{logo_text}</b></font>", ParagraphStyle(
            name='SimpleLogo',
            fontSize=24,
            alignment=TA_CENTER,
            textColor=colors.HexColor('#000000')
        ))
    
    def create_factura_client_company_info(self, factura):
        """Crea la sección de información del cliente y empresa en dos columnas"""
        # Información del cliente (izquierda)
        cliente_info = f"""
        <b>DATOS DEL CLIENTE</b><br/>
        <br/>
        <b>{factura['cliente_nombre']}</b><br/>
        {factura.get('email', 'Email no especificado')}<br/>
        {factura.get('telefono', 'Teléfono no especificado')}<br/>
        {factura.get('direccion', 'Dirección no especificada')}
        """
        
        # Información de la empresa (derecha)
        empresa = self.plantilla_config['empresa']
        empresa_info = f"""
        <b>DATOS DE LA EMPRESA</b><br/>
        <br/>
        <b>{empresa['nombre']}</b><br/>
        {empresa['email']}<br/>
        {empresa['telefono']}<br/>
        {empresa['direccion']}, {empresa.get('ciudad', '')}
        """
        
        # Crear párrafos con bordes
        cliente_para = Paragraph(cliente_info, ParagraphStyle(
            name='ClientInfo',
            fontSize=10,
            alignment=TA_LEFT,
            textColor=colors.HexColor('#000000'),
            borderWidth=1,
            borderColor=colors.HexColor('#000000'),
            leftPadding=10,
            rightPadding=10,
            topPadding=10,
            bottomPadding=10
        ))
        
        empresa_para = Paragraph(empresa_info, ParagraphStyle(
            name='CompanyInfo',
            fontSize=10,
            alignment=TA_LEFT,
            textColor=colors.HexColor('#000000'),
            borderWidth=1,
            borderColor=colors.HexColor('#000000'),
            leftPadding=10,
            rightPadding=10,
            topPadding=10,
            bottomPadding=10
        ))
        
        # Crear tabla de dos columnas
        info_data = [[cliente_para, empresa_para]]
        info_table = Table(info_data, colWidths=[3*inch, 3*inch])
        info_table.setStyle(TableStyle([
            ('VALIGN', (0, 0), (-1, -1), 'TOP'),
            ('LEFTPADDING', (0, 0), (-1, -1), 0),
            ('RIGHTPADDING', (0, 0), (-1, -1), 0),
            ('TOPPADDING', (0, 0), (-1, -1), 0),
            ('BOTTOMPADDING', (0, 0), (-1, -1), 0),
        ]))
        
        return info_table
    
    def create_factura_items_table_template(self, factura):
        """Crea la tabla de items de la factura según la plantilla"""
        # Crear datos de la tabla con headers y nuevas columnas
        table_data = [['Detalle', 'Cant.', 'Precio', 'IVA', 'Desc.', 'Total']]
        
        for item in factura['items']:
            if item.get('visible_pdf', 1):  # Solo mostrar items visibles
                if item.get('es_tarea_manual', 0):
                    # Es una tarea manual
                    nombre_item = item.get('tarea_manual', 'Tarea manual')
                else:
                    # Es un material
                    nombre_item = f"{item['material_nombre']}"
                
                # Calcular descuentos
                descuento_pct = item.get('descuento_porcentaje', 0)
                descuento_fijo = item.get('descuento_fijo', 0)
                descuento_texto = ""
                if descuento_pct > 0:
                    descuento_texto = f"-{descuento_pct:.1f}%"
                elif descuento_fijo > 0:
                    descuento_texto = f"-€{descuento_fijo:.0f}"
                
                # IVA indicator
                iva_indicator = "✓" if item.get('aplica_iva', True) else "✗"
                
                table_data.append([
                    nombre_item,
                    f"{item['cantidad']:.0f}",
                    f"{item['precio_unitario']:.0f} €",
                    iva_indicator,
                    descuento_texto,
                    f"{item['subtotal']:.0f} €"
                ])
        
        # Crear la tabla con nuevas columnas
        table = Table(table_data, colWidths=[2.8*inch, 0.6*inch, 0.7*inch, 0.4*inch, 0.6*inch, 0.8*inch])
        table.setStyle(TableStyle([
            # Header
            ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#F5F5DC')),
            ('TEXTCOLOR', (0, 0), (-1, 0), colors.HexColor('#000000')),
            ('ALIGN', (0, 0), (-1, 0), 'CENTER'),
            ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
            ('FONTSIZE', (0, 0), (-1, 0), 10),
            ('BOTTOMPADDING', (0, 0), (-1, 0), 12),
            ('TOPPADDING', (0, 0), (-1, 0), 12),
            
            # Data rows
            ('BACKGROUND', (0, 1), (-1, -1), colors.white),
            ('ALIGN', (0, 1), (0, -1), 'LEFT'),  # Descripción a la izquierda
            ('ALIGN', (1, 1), (-1, -1), 'CENTER'),  # Cantidad, precio, total centrados
            ('FONTSIZE', (0, 1), (-1, -1), 9),
            ('TEXTCOLOR', (0, 1), (-1, -1), colors.HexColor('#000000')),
            
            # Grid lines
            ('LINEBELOW', (0, 0), (-1, 0), 1, colors.HexColor('#000000')),
            ('LINEBELOW', (0, 1), (-1, -1), 0.5, colors.HexColor('#000000')),
            ('LINEABOVE', (0, 1), (-1, 1), 0.5, colors.HexColor('#000000')),
            ('LINEAFTER', (0, 0), (-1, -1), 0.5, colors.HexColor('#000000')),
            ('LINEBEFORE', (0, 0), (-1, -1), 0.5, colors.HexColor('#000000')),
        ]))
        
        return table
    
    def create_factura_totals_template(self, factura):
        """Crea la sección de totales según la plantilla con descuentos"""
        iva_habilitado = factura.get('iva_habilitado', True)
        
        # Calcular descuentos desde la base de datos
        descuentos_items = 0
        descuento_global = 0
        
        # Calcular descuentos de items
        for item in factura.get('items', []):
            item_subtotal = item['cantidad'] * item['precio_unitario']
            if item.get('descuento_porcentaje', 0) > 0:
                descuentos_items += item_subtotal * (item['descuento_porcentaje'] / 100)
            elif item.get('descuento_fijo', 0) > 0:
                descuentos_items += min(item['descuento_fijo'], item_subtotal)
        
        # Descuento global desde la base de datos
        descuento_global_porcentaje = factura.get('descuento_global_porcentaje', 0)
        descuento_global_fijo = factura.get('descuento_global_fijo', 0)
        
        if descuento_global_porcentaje > 0:
            descuento_global = factura['subtotal'] * (descuento_global_porcentaje / 100)
        elif descuento_global_fijo > 0:
            descuento_global = min(descuento_global_fijo, factura['subtotal'])
        
        # Crear tabla de totales
        totales_data = []
        
        # Subtotal original (antes de descuentos)
        subtotal_original = factura['subtotal'] + descuentos_items
        totales_data.append(['SUBTOTAL ORIGINAL', f"{subtotal_original:.0f} €"])
        
        # Descuentos de items
        if descuentos_items > 0:
            totales_data.append(['DESCUENTO ITEMS', f"-{descuentos_items:.0f} €"])
        
        # Subtotal después de descuentos de items
        if descuentos_items > 0:
            totales_data.append(['SUBTOTAL', f"{factura['subtotal']:.0f} €"])
        
        # Descuento global
        if descuento_global > 0:
            totales_data.append(['DESCUENTO GLOBAL', f"-{descuento_global:.0f} €"])
        
        # IVA
        if iva_habilitado:
            iva_porcentaje = 21.0  # Por defecto
            totales_data.append([f'IVA {iva_porcentaje:.0f}%', f"{factura['iva']:.0f} €"])
        
        # TOTAL
        totales_data.append(['TOTAL', f"{factura['total']:.0f} €"])
        
        # Crear tabla de totales
        totales_table = Table(totales_data, colWidths=[1.5*inch, 1*inch])
        totales_table.setStyle(TableStyle([
            ('ALIGN', (0, 0), (-1, -1), 'RIGHT'),
            ('FONTSIZE', (0, 0), (-1, -2), 10),
            ('FONTSIZE', (0, -1), (-1, -1), 12),  # Total más grande
            ('FONTNAME', (0, -1), (-1, -1), 'Helvetica-Bold'),  # Total en negrita
            ('TEXTCOLOR', (0, -1), (-1, -1), colors.HexColor('#000000')),
            ('LINEABOVE', (0, -1), (-1, -1), 1, colors.HexColor('#000000')),  # Línea sobre el total
            ('LINEBELOW', (0, -1), (-1, -1), 1, colors.HexColor('#000000')),  # Línea bajo el total
        ]))
        
        # Simplemente devolver la tabla sin usar Frame
        # El Frame estaba causando problemas, mejor usar la tabla directamente
        return totales_table
    
    def create_factura_payment_info_template(self, factura):
        """Crea la sección de información de pago según la plantilla"""
        pago_config = self.plantilla_config.get('pago', {})
        
        # Usar el método de pago específico de la factura si está disponible
        metodo_pago_factura = factura.get('metodo_pago', pago_config.get('metodo_pago', 'Transferencia bancaria'))
        
        payment_info = f"""
        <b>INFORMACIÓN DE PAGO</b><br/>
        <br/>
        {metodo_pago_factura}<br/>
        {pago_config.get('banco', 'Banco Borrelle')}<br/>
        Nombre: {pago_config.get('titular_cuenta', self.plantilla_config['empresa']['nombre'])}<br/>
        {pago_config.get('numero_cuenta', 'Número de cuenta')}
        """
        
        payment_para = Paragraph(payment_info, ParagraphStyle(
            name='PaymentInfo',
            fontSize=10,
            alignment=TA_LEFT,
            textColor=colors.HexColor('#000000'),
            borderWidth=1,
            borderColor=colors.HexColor('#000000'),
            leftPadding=10,
            rightPadding=10,
            topPadding=10,
            bottomPadding=10
        ))
        
        # Crear tabla con fondo beige
        payment_data = [[payment_para]]
        payment_table = Table(payment_data, colWidths=[6*inch])
        payment_table.setStyle(TableStyle([
            ('BACKGROUND', (0, 0), (-1, -1), colors.HexColor('#F5F5DC')),
            ('LEFTPADDING', (0, 0), (-1, -1), 0),
            ('RIGHTPADDING', (0, 0), (-1, -1), 0),
            ('TOPPADDING', (0, 0), (-1, -1), 0),
            ('BOTTOMPADDING', (0, 0), (-1, -1), 0),
        ]))
        
        return payment_table

# Instancia global del generador de PDF
pdf_generator = PDFGenerator()
