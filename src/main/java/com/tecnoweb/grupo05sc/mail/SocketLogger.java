package com.tecnoweb.grupo05sc.mail;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Gestor de trazabilidad y logs para la comunicacion de Sockets TCP (POP3 y SMTP).
 *
 * Estrategia de Doble Terminal:
 * 1. Terminal 1 (Defensa / Consola Principal):
 *    Muestra eventos operacionales limpios, claros y concisos sin saturar la pantalla.
 * 2. Terminal 2 (Auditoria en Vivo de Sockets de Bajo Nivel):
 *    Registra cada comando C: y S : en el archivo 'logs/socket_traffic.log'.
 *    Se visualiza en tiempo real ejecutando: tail -f logs/socket_traffic.log
 */
public final class SocketLogger {

    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE = "logs/socket_traffic.log";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static boolean verboseConsole = false;
    private static PrintWriter fileWriter = null;

    static {
        try {
            File dir = new File(LOG_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            // Modo append: true para acumular sesiones de prueba
            fileWriter = new PrintWriter(new FileWriter(LOG_FILE, true), true);
            logSocket(">>> [INICIO DE SESION DE SOCKETS: " + DATE_FORMAT.format(new Date()) + "] <<<");
        } catch (Exception e) {
            System.err.println("[SOCKET_LOGGER] No se pudo inicializar archivo de logs: " + e.getMessage());
        }
    }

    private SocketLogger() {}

    public static void setVerboseConsole(boolean verbose) {
        verboseConsole = verbose;
    }

    public static boolean isVerboseConsole() {
        return verboseConsole;
    }

    /**
     * Registra un comando enviado por el cliente (C: ...)
     */
    public static void client(String command) {
        String entry = "C: " + command;
        logSocket(entry);
        if (verboseConsole) {
            System.out.println(entry);
        }
    }

    /**
     * Registra una respuesta recibida del servidor (S : ...)
     */
    public static void server(String response) {
        String entry = "S : " + response;
        logSocket(entry);
        if (verboseConsole) {
            System.out.println(entry);
        }
    }

    /**
     * Registra un evento operacional de alto nivel para la consola principal (Terminal 1)
     */
    public static void info(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String entry = "[" + timestamp + "] " + message;
        System.out.println(entry);
        logSocket(entry);
    }

    /**
     * Registra una advertencia o error en la consola principal
     */
    public static void error(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String entry = "[" + timestamp + "] [ERROR] " + message;
        System.err.println(entry);
        logSocket(entry);
    }

    private static synchronized void logSocket(String line) {
        if (fileWriter != null) {
            fileWriter.println(line);
        }
    }
}
