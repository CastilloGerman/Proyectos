# Columnas sin uso en `presupuestos`

La migración V38 (`V38__presupuesto_foto_firma.sql`) añadió las columnas
`presupuestos.foto_trabajo` y `presupuestos.firma_cliente` como almacenamiento
`BYTEA` para una función de foto del trabajo y firma del cliente. La función se
retiró en el commit `4d8ec15` (`advances in IA implementation`), así que estas
columnas no tienen uso en el código actual.

La implementación y el test originales se incorporaron en el commit
`b4834c3` (`pwa movil`). Este documento registra el esquema sin uso; no propone
modificar V38 ni borrar datos o columnas.

## Consulta de solo lectura

Esta consulta cuenta los presupuestos con datos no vacíos en cada columna y en
al menos una de ellas:

```sql
SELECT
    COUNT(*) FILTER (
        WHERE foto_trabajo IS NOT NULL AND octet_length(foto_trabajo) > 0
    ) AS filas_con_foto,
    COUNT(*) FILTER (
        WHERE firma_cliente IS NOT NULL AND octet_length(firma_cliente) > 0
    ) AS filas_con_firma,
    COUNT(*) FILTER (
        WHERE (foto_trabajo IS NOT NULL AND octet_length(foto_trabajo) > 0)
           OR (firma_cliente IS NOT NULL AND octet_length(firma_cliente) > 0)
    ) AS filas_con_foto_o_firma
FROM public.presupuestos;
```

Las imágenes y firmas pueden contener datos personales. Si hay datos almacenados,
antes de eliminar estas columnas debe decidirse su retención o borrado. Cualquier
eliminación requiere una futura migración; queda fuera de este cambio.

## Si se reintroduce la función

- Validar los bytes reales mediante la firma binaria PNG, JPEG o WebP; no confiar
  en el `Content-Type` declarado por el cliente.
- Decodificar la imagen y limitar sus dimensiones, además de imponer un tamaño
  máximo de archivo.
- Mantener la autorización y la consulta filtrada por usuario para cada carga y
  lectura, con un test IDOR que compruebe el aislamiento entre usuarios.
