package com.tecnoweb.grupo05sc.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Centraliza la configuración de red y base de datos.
 * Carga automáticamente las variables desde el archivo .env si existe,
 * y en su defecto utiliza los valores por defecto (PostgreSQL Local).
 *
 * Cero librerías externas: utiliza exclusivamente clases estándar de Java SE.
 */
public final class Config {

    private Config() {}

    private static final Map<String, String> ENV = loadEnv();

    private static Map<String, String> loadEnv() {
        Map<String, String> map = new HashMap<>();
        File envFile = new File(".env");
        if (!envFile.exists()) {
            envFile = new File("INF513_P1/.env");
        }
        if (envFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(envFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int eq = line.indexOf('=');
                    if (eq > 0) {
                        String key = line.substring(0, eq).trim();
                        String val = line.substring(eq + 1).trim();
                        map.put(key, val);
                    }
                }
            } catch (Exception ignored) {}
        }
        return map;
    }

    private static String get(String key, String defaultValue) {
        String val = ENV.get(key);
        return (val != null && !val.isEmpty()) ? val : defaultValue;
    }

    private static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // =========================================================================
    // 1. CONFIGURACIÓN DEL SERVIDOR DE CORREO ELECTRÓNICO (UAGRM - FICCT)
    // =========================================================================
    public static final String MAIL_HOST = get("MAIL_HOST", "mail.tecnoweb.org.bo");
    public static final int POP3_PORT = getInt("POP3_PORT", 110);
    public static final int SMTP_PORT = getInt("SMTP_PORT", 25);

    public static final String POP3_USER = get("POP3_USER", "grupo05sc");
    public static final String POP3_PASS = get("POP3_PASS", "grup005grup005*");
    public static final String MAIL_EMISOR = get("MAIL_EMISOR", "grupo05sc@tecnoweb.org.bo");

    // Prefijo de respuesta para prevenir bucles infinitos de correos automáticos
    public static final String SUBJECT_PREFIX = "RE: [CRM-EDUCATIVO]";

    // Intervalo de revisión del buzón POP3 (3 segundos)
    public static final int POLL_INTERVAL_MS = 3000;

    // Timeout de sockets en lectura y escritura (10 segundos)
    public static final int SOCKET_TIMEOUT_MS = 10000;

    // =========================================================================
    // 2. CONFIGURACIÓN DE BASE DE DATOS (POSTGRESQL)
    // =========================================================================
    public static final String DB_HOST = get("DB_HOST", "localhost");
    public static final String DB_PORT = get("DB_PORT", "5432");
    public static final String DB_NAME = get("DB_NAME", "crm_educativo");
    public static final String DB_USER = get("DB_USER", "postgres");
    public static final String DB_PASS = get("DB_PASS", "postgres");

    public static final String DB_URL = "jdbc:postgresql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
    public static final String DB_DRIVER = "org.postgresql.Driver";
}
