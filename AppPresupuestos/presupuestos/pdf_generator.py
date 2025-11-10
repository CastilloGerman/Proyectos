from reportlab.lib.pagesizes import A4, letter
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import inch, cm
from reportlab.lib import colors
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, Image, Frame, KeepTogether
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
                "nombre": "Mi Empresa S.L.",
                "direccion": "Calle Principal 123",
                "codigo_postal": "28001",
                "ciudad": "Madrid",
                "provincia": "Madrid",
                "pais": "España",
                "telefono": "+34 123 456 789",
                "email": "info@miempresa.com",
                "web": "www.miempresa.com",
                "cif": "B12345678",
                "registro_mercantil": {
                    "provincia": "Madrid",
                    "tomo": "12345",
                    "folio": "67",
                    "seccion": "8",
                    "hoja": "M-123456",
                    "inscripcion": "1ª"
                }
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
                "texto_iva_no_incluido": "Los precios NO incluyen IVA.",
                "nota_factura_iva_incluido": "Los importes de esta factura incluyen IVA al tipo correspondiente.",
                "nota_factura_iva_exento": "Operación exenta de IVA conforme a la normativa vigente.",
                "nota_factura_general": "Pago mediante transferencia en un plazo máximo de 30 días."
            },
            "margenes": {
                "superior": 72,
                "inferior": 18,
                "izquierdo": 72,
                "derecho": 72
            },
            "legal": {
                "mostrar_firma": False,
                "firma_texto": "Firma y sello de la empresa",
                "nota_exencion": "Operación exenta según art. 20 de la Ley 37/1992 del IVA."
            },
            "opciones_pdf": {
                "mostrar_registro_mercantil": True
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
        story.append(Spacer(1, 16))
        
        # TOTALES según plantilla (IVA y TOTAL en caja)
        totals_section = self.create_factura_totals_template(factura)
        story.append(totals_section)
        story.append(Spacer(1, 18))
        
        # INFORMACIÓN DE PAGO Y NOTAS en una sola sección compacta
        additional_sections = self.create_factura_additional_sections(factura)
        story.append(additional_sections)
        story.append(Spacer(1, 15))
        
        # Firma opcional
        signature_block = self.create_factura_signature_block(factura)
        if signature_block:
            story.append(Spacer(1, 12))
            story.append(signature_block)
        
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
        notas_custom = (factura.get('notas') or '').strip()
        notas_config = self.plantilla_config.get('texto_personalizado', {})
        legal_config = self.plantilla_config.get('legal', {})
        notas_generadas: List[str] = []
        
        if notas_custom:
            notas_generadas.append(notas_custom)
        else:
            iva_breakdown = factura.get('iva_breakdown', {})
            tiene_iva = factura.get('iva_habilitado', True) and any(
                float(porcentaje) > 0 and (datos.get('cuota', 0) or 0) > 0
                for porcentaje, datos in iva_breakdown.items()
            )
            
            if tiene_iva:
                nota_iva = notas_config.get('nota_factura_iva_incluido')
            else:
                nota_iva = notas_config.get('nota_factura_iva_exento') or legal_config.get('nota_exencion')
            
            if nota_iva:
                notas_generadas.append(nota_iva)
            
            nota_general = notas_config.get('nota_factura_general')
            if nota_general:
                notas_generadas.append(nota_general)
        
        if not notas_generadas:
            notas_generadas.append('Gracias por su confianza. Para cualquier consulta, contacte con nosotros.')
        
        notas_formateadas = "<br/>".join(f"• {nota.strip()}" for nota in notas_generadas if nota.strip())
        
        footer_text = f"<para align='left'><b>Notas:</b><br/><br/>{notas_formateadas}</para>"
        return Paragraph(footer_text, self.styles['Normal'])
    
    def create_factura_notes_block(self, factura, width=3*inch, reference_height=None):
        """Crea un bloque compacto de notas para ubicarlo junto a la información de pago"""
        notes_para = self.create_factura_footer_section(factura)
        heights = [reference_height] if reference_height else None
        notes_table = Table([[notes_para]], colWidths=[width], rowHeights=heights)
        notes_table.setStyle(TableStyle([
            ('BACKGROUND', (0, 0), (-1, -1), colors.HexColor('#FDFBF6')),
            ('BOX', (0, 0), (-1, -1), 0.5, colors.HexColor('#000000')),
            ('LEFTPADDING', (0, 0), (-1, -1), 8),
            ('RIGHTPADDING', (0, 0), (-1, -1), 8),
            ('TOPPADDING', (0, 0), (-1, -1), 8),
            ('BOTTOMPADDING', (0, 0), (-1, -1), 8),
        ]))
        return notes_table
    
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
        
        # Formatear fecha de emisión
        fecha_creacion = factura.get('fecha_creacion', datetime.now().strftime('%Y-%m-%d'))
        if len(fecha_creacion) > 10:
            fecha_creacion = fecha_creacion[:10]
        try:
            fecha_formateada = datetime.strptime(fecha_creacion, '%Y-%m-%d').strftime('%d/%m/%Y')
        except:
            fecha_formateada = fecha_creacion
        
        # Número de factura y fecha de emisión en caja
        numero_caja = Paragraph(
            f"<b>Nº: {numero_factura}</b><br/>Fecha de emisión: {fecha_formateada}", 
            ParagraphStyle(
                name='FacturaNumber',
                fontSize=11,
                alignment=TA_LEFT,
                textColor=colors.HexColor('#000000'),
                borderWidth=1,
                borderColor=colors.HexColor('#000000'),
                leftPadding=8,
                rightPadding=8,
                topPadding=6,
                bottomPadding=6
            )
        )
        
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
    
    def _format_cif(self, cif: str) -> str:
        if not cif:
            return "N/A"
        return cif.strip().upper()
    
    def _format_registro_mercantil(self, registro) -> str:
        if not registro:
            return ""
        if isinstance(registro, dict):
            partes = [
                registro.get('provincia'),
                f"Tomo {registro.get('tomo')}" if registro.get('tomo') else None,
                f"Folio {registro.get('folio')}" if registro.get('folio') else None,
                f"Sección {registro.get('seccion')}" if registro.get('seccion') else None,
                f"Hoja {registro.get('hoja')}" if registro.get('hoja') else None,
                f"Inscripción {registro.get('inscripcion')}" if registro.get('inscripcion') else None,
            ]
            return ', '.join([p for p in partes if p])
        return str(registro).strip()
    
    def _format_direccion_empresa(self, empresa: Dict[str, Any]) -> str:
        direccion = empresa.get('direccion', '')
        codigo_postal = empresa.get('codigo_postal', '')
        ciudad = empresa.get('ciudad', '')
        provincia = empresa.get('provincia', '')
        pais = empresa.get('pais', '')
        
        linea_principal = direccion
        linea_localidad = ", ".join([part for part in [codigo_postal, ciudad, provincia] if part])
        linea_pais = pais
        
        partes = [linea_principal]
        if linea_localidad:
            partes.append(linea_localidad)
        if linea_pais:
            partes.append(linea_pais)
        return "<br/>".join(partes)
    
    def _build_iva_breakdown_from_items(self, factura: Dict[str, Any]) -> Dict[float, Dict[str, float]]:
        breakdown: Dict[float, Dict[str, float]] = {}
        iva_habilitado = factura.get('iva_habilitado', True)
        if not iva_habilitado:
            return breakdown
        
        for item in factura.get('items', []):
            base_linea = item.get('subtotal', item.get('subtotal_linea', 0.0)) or 0.0
            iva_porcentaje = float(item.get('iva_porcentaje', factura.get('iva_porcentaje', 0.0)) or 0.0)
            aplica_iva = item.get('aplica_iva', True) and iva_porcentaje > 0
            
            if aplica_iva:
                datos = breakdown.setdefault(iva_porcentaje, {'base': 0.0, 'cuota': 0.0})
                datos['base'] += base_linea
                cuota_linea = item.get('cuota_iva')
                if cuota_linea is None:
                    cuota_linea = base_linea * (iva_porcentaje / 100)
                datos['cuota'] += cuota_linea or 0.0
        
        return breakdown
    
    def create_factura_client_company_info(self, factura):
        """Crea la sección de información del cliente y empresa en dos columnas"""
        # Información del cliente (izquierda)
        nif_cliente = factura.get('dni', 'No especificado')
        cliente_info = f"""
        <b>DATOS DEL CLIENTE</b><br/>
        <br/>
        <b>{factura['cliente_nombre']}</b><br/>
        NIF/NIE/IVA Intracomunitario: {nif_cliente}<br/>
        {factura.get('direccion', 'Dirección no especificada')}<br/>
        {factura.get('email', 'Email no especificado')}<br/>
        {factura.get('telefono', 'Teléfono no especificado')}
        """
        
        # Información de la empresa (derecha)
        empresa = self.plantilla_config['empresa']
        registro_mercantil = empresa.get('registro_mercantil', '')
        mostrar_registro = self.plantilla_config.get('opciones_pdf', {}).get('mostrar_registro_mercantil', True)
        registro_formateado = self._format_registro_mercantil(registro_mercantil) if mostrar_registro else ""
        cif_formateado = self._format_cif(empresa.get('cif', 'N/A'))
        domicilio_formateado = self._format_direccion_empresa(empresa)
        empresa_info = f"""
        <b>DATOS DE LA EMPRESA</b><br/>
        <br/>
        <b>{empresa['nombre']}</b><br/>
        CIF: {cif_formateado}<br/>
        {f'Registro Mercantil: {registro_formateado}<br/>' if registro_formateado else ''}
        {domicilio_formateado}<br/>
        {empresa['email']}<br/>
        {empresa['telefono']}
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
        """Crea la tabla de items de la factura según la plantilla con todos los campos AEAT"""
        # Crear headers con Paragraph para permitir saltos de línea
        headers = [
            Paragraph('Descripción', self.styles['Normal']),
            Paragraph('Cant.', self.styles['Normal']),
            Paragraph('Precio Unit.<br/>(sin IVA)', self.styles['Normal']),
            Paragraph('% IVA', self.styles['Normal']),
            Paragraph('Cuota IVA', self.styles['Normal']),
            Paragraph('Total<br/>(con IVA)', self.styles['Normal'])
        ]
        table_data = [headers]
        
        iva_habilitado = factura.get('iva_habilitado', True)
        
        for item in factura['items']:
            if item.get('visible_pdf', 1):  # Solo mostrar items visibles
                if item.get('es_tarea_manual', 0):
                    # Es una tarea manual
                    nombre_item = item.get('tarea_manual', 'Tarea manual')
                else:
                    # Es un material
                    nombre_item = item.get('material_nombre')
                
                if not nombre_item:
                    nombre_item = "Concepto sin descripción"
                else:
                    nombre_item = str(nombre_item)
                
                # Calcular subtotal después de descuentos
                item_subtotal_bruto = item['cantidad'] * item['precio_unitario']
                descuento_pct = item.get('descuento_porcentaje', 0)
                descuento_fijo = item.get('descuento_fijo', 0)
                item_descuento = 0.0
                if descuento_pct > 0:
                    item_descuento = item_subtotal_bruto * (descuento_pct / 100)
                elif descuento_fijo > 0:
                    item_descuento = min(descuento_fijo, item_subtotal_bruto)
                
                subtotal_sin_iva = item_subtotal_bruto - item_descuento
                
                # Obtener cuota de IVA (ya calculada en BD o calcular si no existe)
                cuota_iva = item.get('cuota_iva', 0) or 0.0
                iva_porcentaje_valor = float(item.get('iva_porcentaje', factura.get('iva_porcentaje', 0)))
                
                if cuota_iva == 0 and item.get('aplica_iva', True) and iva_habilitado and iva_porcentaje_valor:
                    cuota_iva = subtotal_sin_iva * (iva_porcentaje_valor / 100)
                
                if item.get('aplica_iva', True) and iva_habilitado and iva_porcentaje_valor:
                    iva_porcentaje = f"{iva_porcentaje_valor:.0f}%"
                else:
                    iva_porcentaje = "0%"
                
                # Total con IVA
                total_con_iva = subtotal_sin_iva + cuota_iva
                
                # Crear Paragraph para descripción que pueda ajustarse en múltiples líneas
                desc_style = ParagraphStyle(
                    name='ItemDesc',
                    parent=self.styles['Normal'],
                    fontSize=8,
                    alignment=TA_LEFT,
                    leading=9
                )
                desc_para = Paragraph(nombre_item, desc_style)
                
                # Crear Paragraph para otros campos numéricos
                num_style = ParagraphStyle(
                    name='ItemNum',
                    parent=self.styles['Normal'],
                    fontSize=8,
                    alignment=TA_CENTER,
                    leading=9
                )
                
                table_data.append([
                    desc_para,
                    Paragraph(f"{item['cantidad']:.2f}", num_style),
                    Paragraph(f"{item['precio_unitario']:.2f} €", num_style),
                    Paragraph(iva_porcentaje, num_style),
                    Paragraph(f"{cuota_iva:.2f} €" if cuota_iva > 0 else "-", num_style),
                    Paragraph(f"{total_con_iva:.2f} €", num_style)
                ])
        
        # Crear la tabla con columnas según requisitos AEAT (ajustadas para mejor visualización)
        # Anchos optimizados: Descripción más ancha, otras columnas ajustadas
        # Total aproximado: 6.4 inch (deja espacio para márgenes de ~1 inch cada lado en A4)
        table = Table(table_data, colWidths=[2.8*inch, 0.6*inch, 1.1*inch, 0.5*inch, 0.8*inch, 0.6*inch], repeatRows=1)
        table.setStyle(TableStyle([
            # Header
            ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#F5F5DC')),
            ('TEXTCOLOR', (0, 0), (-1, 0), colors.HexColor('#000000')),
            ('ALIGN', (0, 0), (-1, 0), 'CENTER'),
            ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
            ('FONTSIZE', (0, 0), (-1, 0), 8),
            ('BOTTOMPADDING', (0, 0), (-1, 0), 8),
            ('TOPPADDING', (0, 0), (-1, 0), 8),
            ('VALIGN', (0, 0), (-1, 0), 'MIDDLE'),
            ('LEFTPADDING', (0, 0), (-1, 0), 4),
            ('RIGHTPADDING', (0, 0), (-1, 0), 4),
            
            # Data rows
            ('BACKGROUND', (0, 1), (-1, -1), colors.white),
            ('ALIGN', (0, 1), (0, -1), 'LEFT'),  # Descripción a la izquierda
            ('ALIGN', (1, 1), (-1, -1), 'CENTER'),  # Cantidad, precio, total centrados
            ('FONTSIZE', (0, 1), (-1, -1), 8),
            ('TEXTCOLOR', (0, 1), (-1, -1), colors.HexColor('#000000')),
            ('VALIGN', (0, 1), (-1, -1), 'MIDDLE'),
            ('TOPPADDING', (0, 1), (-1, -1), 6),
            ('BOTTOMPADDING', (0, 1), (-1, -1), 6),
            ('LEFTPADDING', (0, 1), (-1, -1), 4),
            ('RIGHTPADDING', (0, 1), (-1, -1), 4),
            
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
        iva_breakdown = factura.get('iva_breakdown', {})
        if (not iva_breakdown) and iva_habilitado:
            iva_breakdown = self._build_iva_breakdown_from_items(factura)
        
        subtotal = factura.get('subtotal', 0.0)
        descuento_global = factura.get('descuento_global_calculado', 0.0)
        descuentos_items = factura.get('descuentos_items_calculados', 0.0)
        base_exenta = factura.get('base_exenta', 0.0)
        base_imponible = factura.get('base_imponible_calculada', subtotal - descuento_global)
        iva_total = sum(data.get('cuota', 0.0) for data in iva_breakdown.values()) if iva_habilitado else 0.0
        retencion_irpf_porcentaje = factura.get('retencion_irpf')
        retencion_irpf_importe = factura.get('retencion_irpf_importe', 0.0)
        total_factura = factura.get('total', base_imponible + iva_total - retencion_irpf_importe)
        
        breakdown_rows = []
        header_style = ParagraphStyle(
            name='TotalesHeader',
            parent=self.styles['Normal'],
            fontSize=9,
            alignment=TA_CENTER,
            textColor=colors.HexColor('#000000')
        )
        cell_style = ParagraphStyle(
            name='TotalesCell',
            parent=self.styles['Normal'],
            fontSize=9,
            alignment=TA_RIGHT,
            textColor=colors.HexColor('#000000')
        )
        
        breakdown_rows.append([
            Paragraph("Tipo IVA", header_style),
            Paragraph("Base (€)", header_style),
            Paragraph("Cuota (€)", header_style)
        ])
        
        if iva_habilitado and iva_breakdown:
            for porcentaje in sorted(iva_breakdown.keys(), key=lambda x: float(x)):
                datos = iva_breakdown[porcentaje]
                porcentaje_valor = float(porcentaje)
                tipo_texto = f"{porcentaje_valor:.0f}%" if porcentaje_valor > 0 else "Exento"
                breakdown_rows.append([
                    Paragraph(tipo_texto, self.styles['Normal']),
                    Paragraph(f"{datos.get('base', 0.0):.2f} €", cell_style),
                    Paragraph(f"{datos.get('cuota', 0.0):.2f} €", cell_style)
                ])
        else:
            breakdown_rows.append([
                Paragraph("Exento", self.styles['Normal']),
                Paragraph(f"{base_imponible:.2f} €", cell_style),
                Paragraph("0.00 €", cell_style)
            ])
        
        if base_exenta > 0:
            breakdown_rows.append([
                Paragraph("Base exenta (0%)", self.styles['Normal']),
                Paragraph(f"{base_exenta:.2f} €", cell_style),
                Paragraph("0.00 €", cell_style)
            ])
        
        breakdown_table = Table(breakdown_rows, colWidths=[1.6*inch, 1.2*inch, 1.1*inch])
        breakdown_table.setStyle(TableStyle([
            ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#F5F5DC')),
            ('TEXTCOLOR', (0, 0), (-1, 0), colors.HexColor('#000000')),
            ('ALIGN', (0, 0), (-1, 0), 'CENTER'),
            ('ALIGN', (0, 1), (0, -1), 'LEFT'),
            ('ALIGN', (1, 1), (-1, -1), 'RIGHT'),
            ('FONTSIZE', (0, 0), (-1, -1), 9),
            ('INNERGRID', (0, 0), (-1, -1), 0.25, colors.HexColor('#000000')),
            ('BOX', (0, 0), (-1, -1), 0.25, colors.HexColor('#000000')),
        ]))
        
        resumen_rows = []
        resumen_rows.append(['Subtotal', f"{subtotal:.2f} €"])
        if descuentos_items > 0:
            resumen_rows.append(['Descuentos por línea', f"-{descuentos_items:.2f} €"])
        if descuento_global > 0:
            resumen_rows.append(['Descuento global', f"-{descuento_global:.2f} €"])
        
        resumen_rows.append(['Base neta', f"{base_imponible:.2f} €"])
        if iva_total > 0:
            resumen_rows.append(['IVA total', f"{iva_total:.2f} €"])
        else:
            resumen_rows.append(['IVA total', "0.00 €"])
        
        if retencion_irpf_importe > 0 and retencion_irpf_porcentaje:
            resumen_rows.append([f"Retención IRPF ({retencion_irpf_porcentaje:.1f}%)", f"-{retencion_irpf_importe:.2f} €"])
        
        resumen_rows.append(['TOTAL FACTURA', f"{total_factura:.2f} €"])
        
        resumen_table = Table(resumen_rows, colWidths=[2.0*inch, 1.5*inch])
        resumen_table.setStyle(TableStyle([
            ('ALIGN', (0, 0), (-1, -2), 'RIGHT'),
            ('FONTSIZE', (0, 0), (-1, -2), 10),
            ('FONTSIZE', (0, -1), (-1, -1), 12),
            ('FONTNAME', (0, -1), (-1, -1), 'Helvetica-Bold'),
            ('TEXTCOLOR', (0, -1), (-1, -1), colors.HexColor('#000000')),
            ('LINEABOVE', (0, -1), (-1, -1), 1, colors.HexColor('#000000')),
            ('LINEBELOW', (0, -1), (-1, -1), 1, colors.HexColor('#000000')),
        ]))
        
        return KeepTogether([breakdown_table, Spacer(1, 10), resumen_table])
    
    def create_factura_payment_info_template(self, factura, width=6*inch):
        """Crea la sección de información de pago según la plantilla"""
        pago_config = self.plantilla_config.get('pago', {})
        
        # Usar el método de pago específico de la factura si está disponible
        metodo_pago_factura = factura.get('metodo_pago', pago_config.get('metodo_pago', 'Transferencia bancaria'))
        
        # Obtener IBAN (requerido por AEAT)
        iban = pago_config.get('iban', pago_config.get('numero_cuenta', ''))
        iban_texto = f"IBAN: {iban}" if iban else "IBAN no especificado"
        
        # Fecha de vencimiento si existe
        fecha_vencimiento_texto = ""
        fecha_vencimiento = factura.get('fecha_vencimiento')
        if fecha_vencimiento:
            if len(fecha_vencimiento) > 10:
                fecha_vencimiento = fecha_vencimiento[:10]
            try:
                fecha_vto_formateada = datetime.strptime(fecha_vencimiento, '%Y-%m-%d').strftime('%d/%m/%Y')
                fecha_vencimiento_texto = f"<br/>Fecha de vencimiento: {fecha_vto_formateada}"
            except:
                fecha_vencimiento_texto = f"<br/>Fecha de vencimiento: {fecha_vencimiento}"
        
        payment_info = f"""
        <b>INFORMACIÓN DE PAGO</b><br/>
        <br/>
        Forma de pago: {metodo_pago_factura}{fecha_vencimiento_texto}<br/>
        {iban_texto}<br/>
        {pago_config.get('banco', 'Banco')}<br/>
        Titular: {pago_config.get('titular_cuenta', self.plantilla_config['empresa']['nombre'])}
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
        payment_table = Table(payment_data, colWidths=[width])
        payment_table.setStyle(TableStyle([
            ('BACKGROUND', (0, 0), (-1, -1), colors.HexColor('#F5F5DC')),
            ('LEFTPADDING', (0, 0), (-1, -1), 0),
            ('RIGHTPADDING', (0, 0), (-1, -1), 0),
            ('TOPPADDING', (0, 0), (-1, -1), 0),
            ('BOTTOMPADDING', (0, 0), (-1, -1), 0),
        ]))
        
        return payment_table
    
    def create_factura_additional_sections(self, factura):
        """Crea un bloque con la información de pago y las notas en una sola fila"""
        payment_block = self.create_factura_payment_info_template(factura, width=3*inch)
        payment_width, payment_height = payment_block.wrap(3*inch, 0)
        notes_block = self.create_factura_notes_block(factura, width=3*inch, reference_height=payment_height)
        
        combined_table = Table([[payment_block, notes_block]], colWidths=[3*inch, 3*inch])
        combined_table.setStyle(TableStyle([
            ('VALIGN', (0, 0), (-1, -1), 'TOP'),
            ('LEFTPADDING', (0, 0), (-1, -1), 0),
            ('RIGHTPADDING', (0, 0), (-1, -1), 0),
            ('TOPPADDING', (0, 0), (-1, -1), 0),
            ('BOTTOMPADDING', (0, 0), (-1, -1), 0),
        ]))
        
        return combined_table
    
    def create_factura_signature_block(self, factura):
        """Crea un bloque de firma opcional según configuración"""
        legal_config = self.plantilla_config.get('legal', {})
        if not legal_config.get('mostrar_firma'):
            return None
        
        firma_texto = legal_config.get('firma_texto', 'Firma y sello')
        firma_para = Paragraph(
            f"<para align='center'><br/><br/><br/>{firma_texto}</para>",
            ParagraphStyle(
                name='FirmaFactura',
                parent=self.styles['Normal'],
                fontSize=10,
                alignment=TA_CENTER,
                textColor=colors.HexColor('#000000')
            )
        )
        
        signature_table = Table([[firma_para]], colWidths=[6*inch])
        signature_table.setStyle(TableStyle([
            ('LINEABOVE', (0, 0), (-1, 0), 0.5, colors.HexColor('#000000')),
            ('TOPPADDING', (0, 0), (-1, -1), 12),
            ('BOTTOMPADDING', (0, 0), (-1, -1), 0),
        ]))
        
        return signature_table

# Instancia global del generador de PDF
pdf_generator = PDFGenerator()
