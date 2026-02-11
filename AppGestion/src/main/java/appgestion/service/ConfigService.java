package appgestion.service;

import appgestion.config.PlantillaConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Carga y guarda la configuración de plantilla (empresa, logo, carpetas PDF).
 * Busca config/plantilla_config.json en el directorio de trabajo o en la carpeta del usuario.
 */
public class ConfigService {

    private static final Logger log = Logger.getLogger(ConfigService.class.getName());
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final String CONFIG_DIR = "config";
    private static final String CONFIG_FILE = "plantilla_config.json";

    private final Path configPath;
    private PlantillaConfig config;

    public ConfigService() {
        String userDir = System.getProperty("user.dir", ".");
        Path dir = Paths.get(userDir).resolve(CONFIG_DIR);
        this.configPath = dir.resolve(CONFIG_FILE);
        this.config = load();
    }

    public PlantillaConfig getConfig() {
        return config;
    }

    /**
     * Carga la configuración desde JSON. Si no existe o falla, devuelve valores por defecto
     * y asegura carpetas output/presupuestos y output/facturas.
     */
    public final PlantillaConfig load() {
        if (configPath != null && Files.isRegularFile(configPath)) {
            try {
                String json = Files.readString(configPath);
                PlantillaConfig c = GSON.fromJson(json, PlantillaConfig.class);
                if (c != null) {
                    if (c.empresa == null) c.empresa = new PlantillaConfig.Empresa();
                    if (c.logo == null) c.logo = new PlantillaConfig.Logo();
                    config = c;
                    return config;
                }
            } catch (IOException e) {
                log.log(Level.WARNING, "Error leyendo configuración: {0}", e.getMessage());
            }
        }
        config = new PlantillaConfig();
        String base = System.getProperty("user.dir", ".");
        config.carpetaPresupuestosPdf = Paths.get(base, "output", "presupuestos").toString();
        config.carpetaFacturasPdf = Paths.get(base, "output", "facturas").toString();
        save(); // Crear config/plantilla_config.json con valores por defecto para que el usuario pueda editarlo
        return config;
    }

    /**
     * Guarda la configuración actual en JSON y crea el directorio config si no existe.
     */
    public void save() {
        try {
            if (configPath != null) {
                Files.createDirectories(configPath.getParent());
                Files.writeString(configPath, GSON.toJson(config));
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Error guardando configuración", e);
        }
    }

    private static String defaultBase() {
        return Paths.get(System.getProperty("user.dir", "."), "output").toString();
    }

    /**
     * Devuelve la ruta de la carpeta donde guardar PDFs de presupuestos.
     * Crea la carpeta si no existe.
     * Si existe carpeta_pdfs (config Python), se usa como carpeta única.
     */
    public Path getCarpetaPresupuestosPdf() {
        String ruta = null;
        if (config != null) {
            if (config.carpetaPresupuestosPdf != null && !config.carpetaPresupuestosPdf.isEmpty())
                ruta = config.carpetaPresupuestosPdf;
            else if (config.carpetaPdfs != null && !config.carpetaPdfs.isEmpty())
                ruta = config.carpetaPdfs;
        }
        if (ruta == null) ruta = Paths.get(defaultBase(), "presupuestos").toString();
        Path p = Paths.get(ruta);
        try {
            Files.createDirectories(p);
        } catch (IOException ignored) {
        }
        return p;
    }

    /**
     * Devuelve la ruta de la carpeta donde guardar PDFs de facturas.
     */
    public Path getCarpetaFacturasPdf() {
        String ruta = null;
        if (config != null) {
            if (config.carpetaFacturasPdf != null && !config.carpetaFacturasPdf.isEmpty())
                ruta = config.carpetaFacturasPdf;
            else if (config.carpetaPdfs != null && !config.carpetaPdfs.isEmpty())
                ruta = config.carpetaPdfs;
        }
        if (ruta == null) ruta = Paths.get(defaultBase(), "facturas").toString();
        Path p = Paths.get(ruta);
        try {
            Files.createDirectories(p);
        } catch (IOException ignored) {
        }
        return p;
    }
}
