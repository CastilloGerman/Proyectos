package appgestion.config;

import com.google.gson.annotations.SerializedName;

/**
 * Configuración de plantilla para PDFs (empresa, logo) y carpetas por defecto.
 * Compatible con el JSON de la app Python (plantilla_config.json).
 */
public class PlantillaConfig {

    public static class Empresa {
        public String nombre = "Mi Empresa S.L.";
        public String direccion = "Calle Principal 123";
        @SerializedName("codigo_postal")
        public String codigoPostal = "28001";
        public String ciudad = "Madrid";
        public String provincia = "Madrid";
        public String pais = "España";
        public String telefono = "+34 123 456 789";
        public String email = "info@miempresa.com";
        public String web = "www.miempresa.com";
        public String cif = "B12345678";
    }

    public static class Logo {
        @SerializedName("usar_logo")
        public boolean usarLogo = false;
        @SerializedName("ruta_logo")
        public String rutaLogo = "";
        @SerializedName("texto_logo")
        public String textoLogo = "PRESUPUESTOS";
        @SerializedName("tamaño_logo")
        public int tamanoLogo = 24;
    }

    public Empresa empresa = new Empresa();
    public Logo logo = new Logo();

    @SerializedName("carpeta_presupuestos_pdf")
    public String carpetaPresupuestosPdf;

    @SerializedName("carpeta_facturas_pdf")
    public String carpetaFacturasPdf;

    /** Compatible con la app Python: si viene carpeta_pdfs, se usa para ambos. */
    @SerializedName("carpeta_pdfs")
    public String carpetaPdfs;
}
