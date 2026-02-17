from .utils import db
from typing import List, Dict, Any, Optional
from datetime import datetime

class PresupuestoManager:
    def __init__(self):
        self.db = db
        self.iva_porcentaje = 21.0  # 21% de IVA
    
    def crear_presupuesto(self, cliente_id: int, items: List[Dict[str, Any]], iva_habilitado: bool = True,
                         descuento_global_porcentaje: float = 0, descuento_global_fijo: float = 0,
                         descuento_antes_iva: bool = True) -> int:
        """Crea un nuevo presupuesto con sus items"""
        # Calcular totales con nuevos campos
        totales = self.calcular_totales_completo(items, descuento_global_porcentaje, descuento_global_fijo, descuento_antes_iva, iva_habilitado)
        
        # Crear presupuesto
        query = """
            INSERT INTO presupuestos (cliente_id, subtotal, iva, total, iva_habilitado, 
                                    descuento_global_porcentaje, descuento_global_fijo, descuento_antes_iva)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """
        presupuesto_id = self.db.execute_update(query, (
            cliente_id, totales['subtotal'], totales['iva'], totales['total'], 
            1 if iva_habilitado else 0, descuento_global_porcentaje, descuento_global_fijo,
            1 if descuento_antes_iva else 0
        ))
        
        # Crear items del presupuesto
        for item in items:
            item_subtotal = item['cantidad'] * item['precio_unitario']
            item_query = """
                INSERT INTO presupuesto_items (presupuesto_id, material_id, tarea_manual, cantidad, precio_unitario, subtotal, 
                                              visible_pdf, es_tarea_manual, aplica_iva, descuento_porcentaje, descuento_fijo)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """
            self.db.execute_update(item_query, (
                presupuesto_id, 
                item.get('material_id'), 
                item.get('tarea_manual', ''),
                item['cantidad'], 
                item['precio_unitario'], 
                item_subtotal,
                item.get('visible_pdf', 1),
                item.get('es_tarea_manual', 0),
                item.get('aplica_iva', 1),
                item.get('descuento_porcentaje', 0),
                item.get('descuento_fijo', 0)
            ))
        
        return presupuesto_id
    
    def obtener_presupuestos(self, anio: Optional[int] = None, mes: Optional[int] = None) -> List[Dict[str, Any]]:
        """Obtiene presupuestos con filtros opcionales por año y mes"""
        where_clauses = []
        params: List[Any] = []

        if anio is not None:
            where_clauses.append("strftime('%Y', p.fecha_creacion) = ?")
            params.append(str(anio))

        if mes is not None:
            where_clauses.append("strftime('%m', p.fecha_creacion) = ?")
            params.append(f"{int(mes):02d}")

        where_sql = f"WHERE {' AND '.join(where_clauses)}" if where_clauses else ""

        query = f"""
            SELECT p.*, c.nombre as cliente_nombre, c.telefono, c.email
            FROM presupuestos p
            JOIN clientes c ON p.cliente_id = c.id
            {where_sql}
            ORDER BY p.fecha_creacion DESC
        """
        return self.db.execute_query(query, tuple(params))
    
    def obtener_presupuesto_por_id(self, presupuesto_id: int) -> Optional[Dict[str, Any]]:
        """Obtiene un presupuesto específico con sus items"""
        # Obtener presupuesto
        query = """
            SELECT p.*, c.nombre as cliente_nombre, c.telefono, c.email, c.direccion
            FROM presupuestos p
            JOIN clientes c ON p.cliente_id = c.id
            WHERE p.id = ?
        """
        presupuesto = self.db.execute_query(query, (presupuesto_id,))
        if not presupuesto:
            return None
        
        # Obtener items del presupuesto
        items_query = """
            SELECT pi.*, m.nombre as material_nombre, m.unidad_medida
            FROM presupuesto_items pi
            LEFT JOIN materiales m ON pi.material_id = m.id
            WHERE pi.presupuesto_id = ?
            ORDER BY pi.id
        """
        items = self.db.execute_query(items_query, (presupuesto_id,))
        
        presupuesto[0]['items'] = items
        return presupuesto[0]
    
    def eliminar_presupuesto(self, presupuesto_id: int) -> bool:
        """Elimina un presupuesto y sus items"""
        try:
            # Eliminar items primero
            items_query = "DELETE FROM presupuesto_items WHERE presupuesto_id = ?"
            self.db.execute_update(items_query, (presupuesto_id,))
            
            # Eliminar presupuesto
            presupuesto_query = "DELETE FROM presupuestos WHERE id = ?"
            self.db.execute_update(presupuesto_query, (presupuesto_id,))
            
            return True
        except:
            return False
    
    def calcular_totales(self, items: List[Dict[str, Any]]) -> Dict[str, float]:
        """Calcula subtotal, IVA y total para una lista de items (método legacy)"""
        return self.calcular_totales_completo(items, 0, 0, True, True)
    
    def calcular_totales_completo(self, items: List[Dict[str, Any]], descuento_global_porcentaje: float = 0,
                                 descuento_global_fijo: float = 0, descuento_antes_iva: bool = True,
                                 iva_habilitado: bool = True) -> Dict[str, float]:
        """Calcula totales completos con descuentos por item y globales"""
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
        
        # Calcular IVA primero (sin descuento global aplicado aún)
        iva = iva_base
        
        # Aplicar descuento global según configuración
        descuento_global = 0.0
        if descuento_antes_iva:
            # Descuento se aplica antes de calcular IVA
            if descuento_global_porcentaje > 0:
                descuento_global = subtotal * (descuento_global_porcentaje / 100)
            elif descuento_global_fijo > 0:
                descuento_global = min(descuento_global_fijo, subtotal)
            
            # Recalcular IVA sobre la base después del descuento
            base_iva = subtotal - descuento_global
            if subtotal > 0:
                iva = iva_base * (base_iva / subtotal)
            else:
                iva = 0.0
            total = base_iva + iva
        else:
            # Descuento se aplica después de calcular IVA (sobre subtotal + IVA)
            total_sin_descuento = subtotal + iva
            if descuento_global_porcentaje > 0:
                descuento_global = total_sin_descuento * (descuento_global_porcentaje / 100)
            elif descuento_global_fijo > 0:
                descuento_global = min(descuento_global_fijo, total_sin_descuento)
            
            total = total_sin_descuento - descuento_global
        
        return {
            'subtotal': subtotal,
            'descuentos_items': descuentos_items,
            'descuento_global': descuento_global,
            'iva': iva,
            'total': total,
            'iva_porcentaje': self.iva_porcentaje,
            'descuento_antes_iva': descuento_antes_iva
        }
    
    def actualizar_visibilidad_item(self, item_id: int, visible: bool) -> bool:
        """Actualiza la visibilidad de un item en el PDF"""
        query = "UPDATE presupuesto_items SET visible_pdf = ? WHERE id = ?"
        try:
            self.db.execute_update(query, (1 if visible else 0, item_id))
            return True
        except:
            return False
    
    def obtener_items_visibles_pdf(self, presupuesto_id: int) -> List[Dict[str, Any]]:
        """Obtiene solo los items marcados como visibles para el PDF"""
        query = """
            SELECT pi.*, m.nombre as material_nombre, m.unidad_medida
            FROM presupuesto_items pi
            LEFT JOIN materiales m ON pi.material_id = m.id
            WHERE pi.presupuesto_id = ? AND pi.visible_pdf = 1
            ORDER BY pi.id
        """
        return self.db.execute_query(query, (presupuesto_id,))
    
    def buscar_presupuestos(self, termino: str, anio: Optional[int] = None, mes: Optional[int] = None) -> List[Dict[str, Any]]:
        """Busca presupuestos por cliente, ID o fecha con filtros opcionales"""
        where_clauses = [
            "(c.nombre LIKE ? OR CAST(p.id AS TEXT) LIKE ? OR p.fecha_creacion LIKE ?)"
        ]
        params: List[Any] = []

        termino_busqueda = f"%{termino}%"
        params.extend([termino_busqueda, termino_busqueda, termino_busqueda])

        if anio is not None:
            where_clauses.append("strftime('%Y', p.fecha_creacion) = ?")
            params.append(str(anio))

        if mes is not None:
            where_clauses.append("strftime('%m', p.fecha_creacion) = ?")
            params.append(f"{int(mes):02d}")

        where_sql = " AND ".join(where_clauses)

        query = f"""
            SELECT p.*, c.nombre as cliente_nombre, c.telefono, c.email
            FROM presupuestos p
            JOIN clientes c ON p.cliente_id = c.id
            WHERE {where_sql}
            ORDER BY p.fecha_creacion DESC
        """
        return self.db.execute_query(query, tuple(params))

    def obtener_anios_presupuestos(self) -> List[str]:
        """Obtiene la lista de años disponibles en los registros de presupuestos"""
        query = """
            SELECT DISTINCT strftime('%Y', fecha_creacion) as anio
            FROM presupuestos
            WHERE fecha_creacion IS NOT NULL
            ORDER BY anio DESC
        """
        result = self.db.execute_query(query)
        return [row['anio'] for row in result if row.get('anio')]

    def obtener_meses_presupuestos(self, anio: Optional[int] = None) -> List[str]:
        """Obtiene la lista de meses disponibles, opcionalmente filtrados por año"""
        where_clauses = ["fecha_creacion IS NOT NULL"]
        params: List[Any] = []

        if anio is not None:
            where_clauses.append("strftime('%Y', fecha_creacion) = ?")
            params.append(str(anio))

        where_sql = " WHERE " + " AND ".join(where_clauses) if where_clauses else ""

        query = f"""
            SELECT DISTINCT strftime('%m', fecha_creacion) as mes
            FROM presupuestos
            {where_sql}
            ORDER BY mes ASC
        """

        result = self.db.execute_query(query, tuple(params))
        return [row['mes'] for row in result if row.get('mes')]
    
    def actualizar_estado_presupuesto(self, presupuesto_id: int, estado: str) -> bool:
        """Actualiza el estado de un presupuesto"""
        query = "UPDATE presupuestos SET estado = ? WHERE id = ?"
        try:
            self.db.execute_update(query, (estado, presupuesto_id))
            return True
        except:
            return False
    
    def obtener_estadisticas_presupuestos(self, fecha_inicio: str = None, fecha_fin: str = None) -> Dict[str, Any]:
        """Obtiene estadísticas de presupuestos con filtros de fecha opcionales"""
        # Construir query base
        where_clauses = []
        params = []
        
        if fecha_inicio:
            where_clauses.append("DATE(p.fecha_creacion) >= ?")
            params.append(fecha_inicio)
        
        if fecha_fin:
            where_clauses.append("DATE(p.fecha_creacion) <= ?")
            params.append(fecha_fin)
        
        where_sql = "WHERE " + " AND ".join(where_clauses) if where_clauses else ""
        
        # Total emitidos
        query_total = f"""
            SELECT COUNT(*) as total
            FROM presupuestos p
            {where_sql}
        """
        result_total = self.db.execute_query(query_total, tuple(params))
        total_emitidos = result_total[0]['total'] if result_total else 0
        
        # Contadores por estado
        query_estados = f"""
            SELECT 
                COALESCE(estado, 'Pendiente') as estado,
                COUNT(*) as cantidad
            FROM presupuestos p
            {where_sql}
            GROUP BY COALESCE(estado, 'Pendiente')
        """
        result_estados = self.db.execute_query(query_estados, tuple(params))
        
        # Inicializar contadores
        pendientes = 0
        aprobados = 0
        rechazados = 0
        
        for row in result_estados:
            estado = row['estado']
            cantidad = row['cantidad']
            if estado == 'Pendiente':
                pendientes = cantidad
            elif estado == 'Aprobado':
                aprobados = cantidad
            elif estado == 'Rechazado':
                rechazados = cantidad
        
        # Cálculos monetarios
        query_valores = f"""
            SELECT 
                COALESCE(SUM(total), 0) as total_valor_emitidos,
                COALESCE(AVG(total), 0) as promedio_presupuesto,
                COALESCE(MAX(total), 0) as presupuesto_max,
                COALESCE(MIN(total), 0) as presupuesto_min,
                COALESCE(SUM(descuento_global_porcentaje * total / 100), 0) + 
                COALESCE(SUM(descuento_global_fijo), 0) as total_descuentos
            FROM presupuestos p
            {where_sql}
        """
        result_valores = self.db.execute_query(query_valores, tuple(params))
        valores = result_valores[0] if result_valores else {}
        total_valor_emitidos = valores.get('total_valor_emitidos', 0) or 0
        promedio_presupuesto = valores.get('promedio_presupuesto', 0) or 0
        presupuesto_max = valores.get('presupuesto_max', 0) or 0
        presupuesto_min = valores.get('presupuesto_min', 0) or 0
        total_descuentos = valores.get('total_descuentos', 0) or 0
        
        # Valores por estado
        query_valor_estados = f"""
            SELECT 
                COALESCE(estado, 'Pendiente') as estado,
                COALESCE(SUM(total), 0) as valor_total
            FROM presupuestos p
            {where_sql}
            GROUP BY COALESCE(estado, 'Pendiente')
        """
        result_valor_estados = self.db.execute_query(query_valor_estados, tuple(params))
        
        total_valor_pendientes = 0
        total_valor_aprobados = 0
        total_valor_rechazados = 0
        
        for row in result_valor_estados:
            estado = row['estado']
            valor = row['valor_total'] or 0
            if estado == 'Pendiente':
                total_valor_pendientes = valor
            elif estado == 'Aprobado':
                total_valor_aprobados = valor
            elif estado == 'Rechazado':
                total_valor_rechazados = valor
        
        # Datos agrupados por mes para evolución
        query_mensual = f"""
            SELECT 
                strftime('%Y-%m', p.fecha_creacion) as mes,
                COUNT(*) as cantidad,
                COALESCE(SUM(total), 0) as valor_total,
                COALESCE(estado, 'Pendiente') as estado
            FROM presupuestos p
            {where_sql}
            GROUP BY strftime('%Y-%m', p.fecha_creacion), COALESCE(estado, 'Pendiente')
            ORDER BY mes
        """
        result_mensual = self.db.execute_query(query_mensual, tuple(params))
        
        return {
            'total_emitidos': total_emitidos,
            'pendientes': pendientes,
            'aprobados': aprobados,
            'rechazados': rechazados,
            'total_valor_emitidos': total_valor_emitidos,
            'total_valor_pendientes': total_valor_pendientes,
            'total_valor_aprobados': total_valor_aprobados,
            'total_valor_rechazados': total_valor_rechazados,
            'promedio_presupuesto': promedio_presupuesto,
            'presupuesto_max': presupuesto_max,
            'presupuesto_min': presupuesto_min,
            'total_descuentos': total_descuentos,
            'evolucion_mensual': result_mensual
        }

    def obtener_tasa_conversion_presupuestos(self, fecha_inicio: str = None, fecha_fin: str = None) -> Dict[str, Any]:
        """Calcula la tasa de conversión de presupuestos a facturas"""
        where_clauses = []
        params = []
        
        if fecha_inicio:
            where_clauses.append("DATE(p.fecha_creacion) >= ?")
            params.append(fecha_inicio)
        
        if fecha_fin:
            where_clauses.append("DATE(p.fecha_creacion) <= ?")
            params.append(fecha_fin)
        
        # Construir WHERE clause
        if where_clauses:
            where_sql = "WHERE " + " AND ".join(where_clauses)
            where_sql_aprobados = where_sql + " AND COALESCE(estado, 'Pendiente') = 'Aprobado'"
        else:
            where_sql = ""
            where_sql_aprobados = "WHERE COALESCE(estado, 'Pendiente') = 'Aprobado'"
        
        # Presupuestos aprobados
        query_aprobados = f"""
            SELECT COUNT(*) as total
            FROM presupuestos p
            {where_sql_aprobados}
        """
        result_aprobados = self.db.execute_query(query_aprobados, tuple(params))
        total_aprobados = result_aprobados[0]['total'] if result_aprobados else 0
        
        # Presupuestos que tienen factura asociada
        base_query_con_factura = """
            SELECT COUNT(DISTINCT p.id) as total
            FROM presupuestos p
            INNER JOIN facturas f ON f.presupuesto_id = p.id
        """
        query_con_factura = base_query_con_factura + where_sql
        result_con_factura = self.db.execute_query(query_con_factura, tuple(params))
        total_con_factura = result_con_factura[0]['total'] if result_con_factura else 0
        
        # Calcular tasa de conversión
        tasa_conversion = 0.0
        if total_aprobados > 0:
            tasa_conversion = (total_con_factura / total_aprobados) * 100
        
        return {
            'total_aprobados': total_aprobados,
            'total_con_factura': total_con_factura,
            'tasa_conversion': tasa_conversion
        }
    
    def obtener_top_clientes_presupuestos(self, fecha_inicio: str = None, fecha_fin: str = None, limite: int = 10) -> List[Dict[str, Any]]:
        """Obtiene los top clientes por valor de presupuestos"""
        where_clauses = []
        params = []
        
        if fecha_inicio:
            where_clauses.append("DATE(p.fecha_creacion) >= ?")
            params.append(fecha_inicio)
        
        if fecha_fin:
            where_clauses.append("DATE(p.fecha_creacion) <= ?")
            params.append(fecha_fin)
        
        where_sql = "WHERE " + " AND ".join(where_clauses) if where_clauses else ""
        
        query = f"""
            SELECT 
                c.id as cliente_id,
                c.nombre as cliente_nombre,
                COUNT(p.id) as cantidad_presupuestos,
                COALESCE(SUM(p.total), 0) as total_valor
            FROM presupuestos p
            JOIN clientes c ON p.cliente_id = c.id
            {where_sql}
            GROUP BY c.id, c.nombre
            ORDER BY total_valor DESC
            LIMIT ?
        """
        params.append(limite)
        return self.db.execute_query(query, tuple(params))

# Instancia global del manager de presupuestos
presupuesto_manager = PresupuestoManager()
