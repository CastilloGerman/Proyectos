from .utils import db
from .presupuestos import presupuesto_manager
from typing import List, Dict, Any, Optional
from datetime import datetime, timedelta

class FacturaManager:
    def __init__(self):
        self.db = db
        self.iva_porcentaje = 21.0  # 21% de IVA
    
    def generar_numero_factura(self) -> str:
        """Genera un número de factura secuencial automático"""
        query = "SELECT numero_factura FROM facturas ORDER BY id DESC LIMIT 1"
        result = self.db.execute_query(query)
        
        if result and result[0]['numero_factura']:
            ultimo_numero = result[0]['numero_factura']
            # Formato: FXXXX-YYYY (F0001-2025)
            try:
                partes = ultimo_numero.split('-')
                if len(partes) == 2:
                    numero = int(partes[0][1:])  # Quitar la 'F' y convertir a int
                    año = partes[1]
                    año_actual = datetime.now().year
                    
                    if int(año) == año_actual:
                        # Incrementar el número
                        nuevo_numero = numero + 1
                    else:
                        # Nuevo año, reiniciar contador
                        nuevo_numero = 1
                    
                    return f"F{nuevo_numero:04d}-{año_actual}"
            except:
                pass
        
        # Si no hay facturas o hay error, empezar desde F0001
        año_actual = datetime.now().year
        return f"F0001-{año_actual}"
    
    def crear_factura(self, cliente_id: int, items: List[Dict[str, Any]], 
                     numero_factura: str = None, fecha_vencimiento: str = None,
                     metodo_pago: str = 'Transferencia', estado_pago: str = 'No Pagada',
                     notas: str = '', iva_habilitado: bool = True,
                     presupuesto_id: int = None, descuento_global_porcentaje: float = 0,
                     descuento_global_fijo: float = 0, descuento_antes_iva: bool = True,
                     retencion_irpf: float = None) -> int:
        """Crea una nueva factura con sus items"""
        # Generar número de factura si no se proporciona
        if not numero_factura:
            numero_factura = self.generar_numero_factura()
        
        # Calcular totales con nuevos campos
        totales = self.calcular_totales_completo(items, descuento_global_porcentaje, descuento_global_fijo, descuento_antes_iva, iva_habilitado, retencion_irpf)
        
        # Crear factura
        query = """
            INSERT INTO facturas (numero_factura, cliente_id, presupuesto_id, fecha_vencimiento, 
                                subtotal, iva, total, iva_habilitado, metodo_pago, estado_pago, notas,
                                descuento_global_porcentaje, descuento_global_fijo, descuento_antes_iva,
                                retencion_irpf)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """
        factura_id = self.db.execute_update(query, (
            numero_factura, cliente_id, presupuesto_id, fecha_vencimiento,
            totales['subtotal'], totales['iva'], totales['total'], 1 if iva_habilitado else 0,
            metodo_pago, estado_pago, notas, descuento_global_porcentaje, descuento_global_fijo,
            1 if descuento_antes_iva else 0, retencion_irpf
        ))
        
        # Crear items de la factura
        for item in items:
            item_subtotal = item['cantidad'] * item['precio_unitario']
            
            # Aplicar descuentos por item
            item_descuento = 0.0
            if item.get('descuento_porcentaje', 0) > 0:
                item_descuento = item_subtotal * (item['descuento_porcentaje'] / 100)
            elif item.get('descuento_fijo', 0) > 0:
                item_descuento = min(item['descuento_fijo'], item_subtotal)
            
            # Calcular subtotal después de descuentos
            item_subtotal_final = item_subtotal - item_descuento
            
            # Calcular cuota de IVA por línea (21% si aplica IVA)
            cuota_iva = 0.0
            if item.get('aplica_iva', True) and iva_habilitado:
                cuota_iva = item_subtotal_final * (self.iva_porcentaje / 100)
            
            item_query = """
                INSERT INTO factura_items (factura_id, material_id, tarea_manual, cantidad, 
                                         precio_unitario, subtotal, visible_pdf, es_tarea_manual,
                                         aplica_iva, descuento_porcentaje, descuento_fijo, cuota_iva)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """
            self.db.execute_update(item_query, (
                factura_id,
                item.get('material_id'),
                item.get('tarea_manual', ''),
                item['cantidad'],
                item['precio_unitario'],
                item_subtotal_final,
                item.get('visible_pdf', 1),
                item.get('es_tarea_manual', 0),
                item.get('aplica_iva', 1),
                item.get('descuento_porcentaje', 0),
                item.get('descuento_fijo', 0),
                cuota_iva
            ))
        
        return factura_id
    
    def crear_factura_desde_presupuesto(self, presupuesto_id: int, 
                                       numero_factura: str = None,
                                       fecha_vencimiento: str = None,
                                       metodo_pago: str = 'Transferencia',
                                       estado_pago: str = 'No Pagada',
                                       notas: str = '') -> int:
        """Crea una factura a partir de un presupuesto existente"""
        # Obtener el presupuesto
        presupuesto = presupuesto_manager.obtener_presupuesto_por_id(presupuesto_id)
        
        if not presupuesto:
            raise ValueError(f"Presupuesto {presupuesto_id} no encontrado")
        
        # Crear factura con los mismos items
        factura_id = self.crear_factura(
            cliente_id=presupuesto['cliente_id'],
            items=presupuesto['items'],
            numero_factura=numero_factura,
            fecha_vencimiento=fecha_vencimiento,
            metodo_pago=metodo_pago,
            estado_pago=estado_pago,
            notas=notas,
            iva_habilitado=presupuesto.get('iva_habilitado', True),
            presupuesto_id=presupuesto_id
        )
        
        return factura_id
    
    def obtener_facturas(self) -> List[Dict[str, Any]]:
        """Obtiene todas las facturas con información del cliente"""
        query = """
            SELECT f.*, c.nombre as cliente_nombre, c.telefono, c.email
            FROM facturas f
            JOIN clientes c ON f.cliente_id = c.id
            ORDER BY f.fecha_creacion DESC
        """
        return self.db.execute_query(query)
    
    def obtener_factura_por_id(self, factura_id: int) -> Optional[Dict[str, Any]]:
        """Obtiene una factura específica con sus items"""
        # Obtener factura
        query = """
            SELECT f.*, c.nombre as cliente_nombre, c.telefono, c.email, c.direccion, c.dni
            FROM facturas f
            JOIN clientes c ON f.cliente_id = c.id
            WHERE f.id = ?
        """
        factura = self.db.execute_query(query, (factura_id,))
        if not factura:
            return None
        
        # Obtener items de la factura
        items_query = """
            SELECT fi.*, m.nombre as material_nombre, m.unidad_medida
            FROM factura_items fi
            LEFT JOIN materiales m ON fi.material_id = m.id
            WHERE fi.factura_id = ?
            ORDER BY fi.id
        """
        items = self.db.execute_query(items_query, (factura_id,))
        
        factura[0]['items'] = items
        return factura[0]
    
    def obtener_factura_por_numero(self, numero_factura: str) -> Optional[Dict[str, Any]]:
        """Obtiene una factura por su número de factura"""
        query = """
            SELECT f.*, c.nombre as cliente_nombre, c.telefono, c.email, c.direccion
            FROM facturas f
            JOIN clientes c ON f.cliente_id = c.id
            WHERE f.numero_factura = ?
        """
        factura = self.db.execute_query(query, (numero_factura,))
        if not factura:
            return None
        
        # Obtener items
        items_query = """
            SELECT fi.*, m.nombre as material_nombre, m.unidad_medida
            FROM factura_items fi
            LEFT JOIN materiales m ON fi.material_id = m.id
            WHERE fi.factura_id = ?
            ORDER BY fi.id
        """
        items = self.db.execute_query(items_query, (factura[0]['id'],))
        
        factura[0]['items'] = items
        return factura[0]
    
    def buscar_facturas(self, termino: str) -> List[Dict[str, Any]]:
        """Busca facturas por cliente, número de factura o fecha"""
        query = """
            SELECT f.*, c.nombre as cliente_nombre, c.telefono, c.email
            FROM facturas f
            JOIN clientes c ON f.cliente_id = c.id
            WHERE c.nombre LIKE ? OR f.numero_factura LIKE ? OR f.fecha_creacion LIKE ?
            ORDER BY f.fecha_creacion DESC
        """
        termino_busqueda = f"%{termino}%"
        return self.db.execute_query(query, (termino_busqueda, termino_busqueda, termino_busqueda))
    
    def actualizar_estado_pago(self, factura_id: int, estado_pago: str) -> bool:
        """Actualiza el estado de pago de una factura"""
        query = "UPDATE facturas SET estado_pago = ? WHERE id = ?"
        try:
            self.db.execute_update(query, (estado_pago, factura_id))
            return True
        except:
            return False
    
    def actualizar_factura(self, factura_id: int, fecha_vencimiento: str = None,
                          metodo_pago: str = None, estado_pago: str = None,
                          notas: str = None) -> bool:
        """Actualiza campos específicos de una factura"""
        updates = []
        params = []
        
        if fecha_vencimiento is not None:
            updates.append("fecha_vencimiento = ?")
            params.append(fecha_vencimiento)
        
        if metodo_pago is not None:
            updates.append("metodo_pago = ?")
            params.append(metodo_pago)
        
        if estado_pago is not None:
            updates.append("estado_pago = ?")
            params.append(estado_pago)
        
        if notas is not None:
            updates.append("notas = ?")
            params.append(notas)
        
        if not updates:
            return False
        
        params.append(factura_id)
        query = f"UPDATE facturas SET {', '.join(updates)} WHERE id = ?"
        
        try:
            self.db.execute_update(query, tuple(params))
            return True
        except:
            return False
    
    def eliminar_factura(self, factura_id: int) -> bool:
        """Elimina una factura y sus items"""
        try:
            # Eliminar items primero
            items_query = "DELETE FROM factura_items WHERE factura_id = ?"
            self.db.execute_update(items_query, (factura_id,))
            
            # Eliminar factura
            factura_query = "DELETE FROM facturas WHERE id = ?"
            self.db.execute_update(factura_query, (factura_id,))
            
            return True
        except:
            return False
    
    def obtener_facturas_por_estado(self, estado_pago: str) -> List[Dict[str, Any]]:
        """Obtiene facturas filtradas por estado de pago"""
        query = """
            SELECT f.*, c.nombre as cliente_nombre, c.telefono, c.email
            FROM facturas f
            JOIN clientes c ON f.cliente_id = c.id
            WHERE f.estado_pago = ?
            ORDER BY f.fecha_creacion DESC
        """
        return self.db.execute_query(query, (estado_pago,))
    
    def calcular_totales(self, items: List[Dict[str, Any]]) -> Dict[str, float]:
        """Calcula subtotal, IVA y total para una lista de items (método legacy)"""
        return self.calcular_totales_completo(items, 0, 0, True, True)
    
    def calcular_totales_completo(self, items: List[Dict[str, Any]], descuento_global_porcentaje: float = 0,
                                 descuento_global_fijo: float = 0, descuento_antes_iva: bool = True,
                                 iva_habilitado: bool = True, retencion_irpf: float = None) -> Dict[str, float]:
        """Calcula totales completos con descuentos por item y globales, incluyendo retención IRPF"""
        subtotal = 0.0
        descuentos_items = 0.0
        iva_base = 0.0
        
        # Verificar si hay items y si alguno tiene IVA habilitado
        items_con_iva = [item for item in items if item.get('aplica_iva', True)]
        
        # Si no hay items con IVA habilitado, forzar iva_habilitado a False
        if not items_con_iva and items:
            iva_habilitado = False
        
        # Calcular subtotal y descuentos por item
        for item in items:
            item_subtotal = item['cantidad'] * item['precio_unitario']
            
            # Aplicar descuentos por item (porcentaje tiene prioridad)
            item_descuento = 0.0
            if item.get('descuento_porcentaje', 0) > 0:
                item_descuento = item_subtotal * (item['descuento_porcentaje'] / 100)
            elif item.get('descuento_fijo', 0) > 0:
                item_descuento = min(item['descuento_fijo'], item_subtotal)
            
            subtotal += item_subtotal - item_descuento
            descuentos_items += item_descuento
            
            # Calcular IVA solo para items que lo tienen habilitado
            if item.get('aplica_iva', True) and iva_habilitado:
                iva_base += (item_subtotal - item_descuento) * (self.iva_porcentaje / 100)
        
        # Aplicar descuento global
        descuento_global = 0.0
        if descuento_global_porcentaje > 0:
            descuento_global = subtotal * (descuento_global_porcentaje / 100)
        elif descuento_global_fijo > 0:
            descuento_global = min(descuento_global_fijo, subtotal)
        
        # Determinar base para IVA según configuración
        if descuento_antes_iva:
            base_iva = subtotal - descuento_global
            iva = iva_base * (base_iva / subtotal) if subtotal > 0 else 0
            base_imponible = base_iva
        else:
            iva = iva_base
            base_imponible = subtotal - descuento_global
        
        # Calcular retención IRPF sobre la base imponible (antes de IVA según AEAT)
        retencion_irpf_importe = 0.0
        if retencion_irpf is not None and retencion_irpf > 0:
            retencion_irpf_importe = base_imponible * (retencion_irpf / 100)
        
        # Total final: base imponible + IVA - retención IRPF
        total = base_imponible + iva - retencion_irpf_importe
        
        return {
            'subtotal': subtotal,
            'descuentos_items': descuentos_items,
            'descuento_global': descuento_global,
            'base_imponible': base_imponible,
            'iva': iva,
            'retencion_irpf': retencion_irpf_importe,
            'retencion_irpf_porcentaje': retencion_irpf if retencion_irpf else 0,
            'total': total,
            'iva_porcentaje': self.iva_porcentaje,
            'descuento_antes_iva': descuento_antes_iva
        }
    
    def calcular_fecha_vencimiento(self, dias: int) -> str:
        """Calcula fecha de vencimiento sumando días a la fecha actual"""
        fecha_vencimiento = datetime.now() + timedelta(days=dias)
        return fecha_vencimiento.strftime("%Y-%m-%d")

# Instancia global del manager de facturas
factura_manager = FacturaManager()

