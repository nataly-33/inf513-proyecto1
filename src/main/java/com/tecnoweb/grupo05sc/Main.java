package com.tecnoweb.grupo05sc;

import com.tecnoweb.grupo05sc.command.CommandDispatcher;
import com.tecnoweb.grupo05sc.command.CommandResult;
import com.tecnoweb.grupo05sc.config.Config;
import com.tecnoweb.grupo05sc.config.DatabaseConnection;
import com.tecnoweb.grupo05sc.mail.MailMessage;
import com.tecnoweb.grupo05sc.mail.POP3Listener;
import com.tecnoweb.grupo05sc.mail.SMTPSender;
import com.tecnoweb.grupo05sc.mail.SocketLogger;
import java.sql.Connection;
import java.util.List;

/**
 * Servidor Demonio Principal del Sistema via Correo Electronico.
 * Ejecuta el ciclo continuo de sondeo (polling) por POP3, interpretacion de comandos
 * y transmision de respuestas HTML por SMTP sobre Sockets TCP puros.
 */
public class Main {

    public static void main(String[] args) {
        boolean runOnce = false;

        for (String arg : args) {
            if (arg.equalsIgnoreCase("--once")) {
                runOnce = true;
            } else if (arg.equalsIgnoreCase("--verbose")) {
                SocketLogger.setVerboseConsole(true);
            }
        }

        printBanner();

        // 1. Verificacion previa de conexion a PostgreSQL
        System.out.println(">>> [INICIO] Verificando conexion a PostgreSQL...");
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println(">>> [DB] [OK] Conexion exitosa a la base de datos: " + Config.DB_URL);
            }
        } catch (Exception e) {
            System.err.println(">>> [ALERTA DB] No se pudo conectar a PostgreSQL: " + e.getMessage());
            System.err.println("    Asegurese de que el servidor PostgreSQL este corriendo en " + Config.DB_HOST + ":" + Config.DB_PORT);
        }

        POP3Listener pop3 = new POP3Listener();
        CommandDispatcher dispatcher = new CommandDispatcher();

        System.out.println("\n>>> [DEMONIO] Servidor de correos activo.");
        System.out.println(">>> Monitoreando buzon: " + Config.MAIL_EMISOR + " cada " + (Config.POLL_INTERVAL_MS / 1000) + " segundos.");
        System.out.println(">>> [CONSEJO DEFENSA] Para ver los Sockets en vivo (C: y S :), ejecute en Terminal 2:");
        System.out.println("    tail -f logs/socket_traffic.log\n");

        // 2. Loop continuo del Daemon
        while (true) {
            try {
                // FASE 1: LECTURA VIA POP3
                List<MailMessage> pendingEmails = pop3.readPendingMessages();

                if (!pendingEmails.isEmpty()) {
                    for (MailMessage msg : pendingEmails) {
                        SocketLogger.info("------------------------------------------------------------");
                        SocketLogger.info("[PETICION RECIBIDA]");
                        SocketLogger.info("  Remitente: " + msg.getFrom());
                        SocketLogger.info("  Asunto:    " + msg.getSubject());
                        SocketLogger.info("------------------------------------------------------------");

                        if (msg.getFrom() == null || msg.getFrom().trim().isEmpty()) {
                            SocketLogger.error("[OMITIDO] Correo sin direccion de remitente valida.");
                            continue;
                        }

                        // FASE 2: INTERPRETACION Y EJECUCION DEL COMANDO
                        CommandResult result = dispatcher.dispatch(msg.getFrom(), msg.getSubject());

                        // FASE 3: ENVIO DE RESPUESTA VIA SMTP
                        SMTPSender.sendHtmlEmail(msg.getFrom(), result.getSubject(), result.getHtmlBody());
                    }
                }

            } catch (Exception e) {
                SocketLogger.error("[ERROR EN CICLO] " + e.getMessage());
            }

            if (runOnce) {
                System.out.println("\n>>> [MODO --once] Finalizando ejecucion tras un ciclo.");
                break;
            }

            try {
                Thread.sleep(Config.POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                System.out.println(">>> [DETENIDO] Hilo interrumpido.");
                break;
            }
        }
    }

    private static void printBanner() {
        System.out.println("==========================================================================");
        System.out.println("   SISTEMA DE GESTION VIA EMAIL - CRM EDUCATIVO (INF513 GRUPO 05 SC)     ");
        System.out.println("==========================================================================");
        System.out.println(" * Servidor Correo: " + Config.MAIL_HOST + " (POP3: " + Config.POP3_PORT + ", SMTP: " + Config.SMTP_PORT + ")");
        System.out.println(" * Cuenta Oficial:  " + Config.MAIL_EMISOR);
        System.out.println(" * Base de Datos:   " + Config.DB_URL);
        System.out.println("==========================================================================\n");
    }
}
