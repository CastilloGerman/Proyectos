package appgestion.model;

import javafx.beans.property.*;

/**
 * Fila de item de presupuesto usada en la tabla de JavaFX.
 * Contiene la información necesaria para cálculos de totales (cantidad, precio, descuentos, IVA, etc.).
 */
public class PresupuestoItemRow {

    private final IntegerProperty materialId = new SimpleIntegerProperty(this, "materialId");
    private final StringProperty descripcion = new SimpleStringProperty(this, "descripcion", "");
    private final BooleanProperty esTareaManual = new SimpleBooleanProperty(this, "esTareaManual", false);
    private final DoubleProperty cantidad = new SimpleDoubleProperty(this, "cantidad", 1.0);
    private final DoubleProperty precioUnitario = new SimpleDoubleProperty(this, "precioUnitario", 0.0);
    private final BooleanProperty aplicaIva = new SimpleBooleanProperty(this, "aplicaIva", true);
    private final DoubleProperty descuentoPorcentaje = new SimpleDoubleProperty(this, "descuentoPorcentaje", 0.0);
    private final DoubleProperty descuentoFijo = new SimpleDoubleProperty(this, "descuentoFijo", 0.0);
    private final BooleanProperty visiblePdf = new SimpleBooleanProperty(this, "visiblePdf", true);
    private final DoubleProperty subtotal = new SimpleDoubleProperty(this, "subtotal", 0.0);

    public int getMaterialId() {
        return materialId.get();
    }

    public void setMaterialId(int id) {
        this.materialId.set(id);
    }

    public IntegerProperty materialIdProperty() {
        return materialId;
    }

    public String getDescripcion() {
        return descripcion.get();
    }

    public void setDescripcion(String value) {
        this.descripcion.set(value);
    }

    public StringProperty descripcionProperty() {
        return descripcion;
    }

    public boolean isEsTareaManual() {
        return esTareaManual.get();
    }

    public void setEsTareaManual(boolean value) {
        this.esTareaManual.set(value);
    }

    public BooleanProperty esTareaManualProperty() {
        return esTareaManual;
    }

    public double getCantidad() {
        return cantidad.get();
    }

    public void setCantidad(double value) {
        this.cantidad.set(value);
    }

    public DoubleProperty cantidadProperty() {
        return cantidad;
    }

    public double getPrecioUnitario() {
        return precioUnitario.get();
    }

    public void setPrecioUnitario(double value) {
        this.precioUnitario.set(value);
    }

    public DoubleProperty precioUnitarioProperty() {
        return precioUnitario;
    }

    public boolean isAplicaIva() {
        return aplicaIva.get();
    }

    public void setAplicaIva(boolean value) {
        this.aplicaIva.set(value);
    }

    public BooleanProperty aplicaIvaProperty() {
        return aplicaIva;
    }

    public double getDescuentoPorcentaje() {
        return descuentoPorcentaje.get();
    }

    public void setDescuentoPorcentaje(double value) {
        this.descuentoPorcentaje.set(value);
    }

    public DoubleProperty descuentoPorcentajeProperty() {
        return descuentoPorcentaje;
    }

    public double getDescuentoFijo() {
        return descuentoFijo.get();
    }

    public void setDescuentoFijo(double value) {
        this.descuentoFijo.set(value);
    }

    public DoubleProperty descuentoFijoProperty() {
        return descuentoFijo;
    }

    public boolean isVisiblePdf() {
        return visiblePdf.get();
    }

    public void setVisiblePdf(boolean value) {
        this.visiblePdf.set(value);
    }

    public BooleanProperty visiblePdfProperty() {
        return visiblePdf;
    }

    public double getSubtotal() {
        return subtotal.get();
    }

    public void setSubtotal(double value) {
        this.subtotal.set(value);
    }

    public DoubleProperty subtotalProperty() {
        return subtotal;
    }
}

