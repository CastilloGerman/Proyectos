package appgestion.model;

import javafx.beans.property.*;

public class Material {

    private final IntegerProperty id = new SimpleIntegerProperty(this, "id");
    private final StringProperty nombre = new SimpleStringProperty(this, "nombre", "");
    private final StringProperty unidadMedida = new SimpleStringProperty(this, "unidadMedida", "");
    private final DoubleProperty precioUnitario = new SimpleDoubleProperty(this, "precioUnitario", 0.0);

    public Material(int id, String nombre, String unidadMedida, double precioUnitario) {
        this.id.set(id);
        this.nombre.set(nombre);
        this.unidadMedida.set(unidadMedida);
        this.precioUnitario.set(precioUnitario);
    }

    public int getId() {
        return id.get();
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public String getNombre() {
        return nombre.get();
    }

    public StringProperty nombreProperty() {
        return nombre;
    }

    public String getUnidadMedida() {
        return unidadMedida.get();
    }

    public StringProperty unidadMedidaProperty() {
        return unidadMedida;
    }

    public double getPrecioUnitario() {
        return precioUnitario.get();
    }

    public DoubleProperty precioUnitarioProperty() {
        return precioUnitario;
    }
}

