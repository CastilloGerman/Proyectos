package appgestion.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Cliente {

    private final IntegerProperty id = new SimpleIntegerProperty(this, "id");
    private final StringProperty nombre = new SimpleStringProperty(this, "nombre", "");
    private final StringProperty telefono = new SimpleStringProperty(this, "telefono", "");
    private final StringProperty email = new SimpleStringProperty(this, "email", "");
    private final StringProperty direccion = new SimpleStringProperty(this, "direccion", "");
    private final StringProperty dni = new SimpleStringProperty(this, "dni", "");

    public Cliente(int id, String nombre, String telefono, String email, String direccion, String dni) {
        this.id.set(id);
        this.nombre.set(nombre);
        this.telefono.set(telefono);
        this.email.set(email);
        this.direccion.set(direccion);
        this.dni.set(dni);
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

    public String getTelefono() {
        return telefono.get();
    }

    public StringProperty telefonoProperty() {
        return telefono;
    }

    public String getEmail() {
        return email.get();
    }

    public StringProperty emailProperty() {
        return email;
    }

    public String getDireccion() {
        return direccion.get();
    }

    public StringProperty direccionProperty() {
        return direccion;
    }

    public String getDni() {
        return dni.get();
    }

    public StringProperty dniProperty() {
        return dni;
    }
}

