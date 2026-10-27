package com.tecnoweb.grupo05sc;

import com.tecnoweb.grupo05sc.command.CommandDispatcher;
import com.tecnoweb.grupo05sc.command.CommandResult;
import com.tecnoweb.grupo05sc.config.Config;
import com.tecnoweb.grupo05sc.config.DatabaseConnection;
import com.tecnoweb.grupo05sc.mail.MailMessage;
import com.tecnoweb.grupo05sc.mail.POP3Listener;
import com.tecnoweb.grupo05sc.mail.SMTPSender;
import java.sql.Connection;
import java.util.List;

/**
 * Servidor Demonio Principal del Sistema vía Correo Electrónico.
 * Ejecuta el ciclo continuo de sondeo (polling) por POP3, interpretación de comandos
 * y transmisión de respuestas HTML por SMTP sobre Sockets TCP puros.
 */
public class Main {

    public static void main(String[] args) {
        printBanner();

        // 1. Verificación previa de conexión a PostgreSQL
        System.out.println(">>> [INICIO] Verificando conexión a PostgreSQL...");
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println(">>> [DB] ¡Conexión exitosa a la base de datos: " + Config.DB_URL + "!");
            }
        } catch (Exception e) {
            System.err.println(">>> [ALERTA DB] No se pudo conectar a PostgreSQL: " + e.getMessage());
            System.err.println("    Asegúrese de que el servidor PostgreSQL esté corriendo en " + Config.DB_HOST + ":" + Config.DB_PORT);
        }

        POP3Listener pop3 = new POP3Listener();
        CommandDispatcher dispatcher = new CommandDispatcher();

        boolean runOnce = (args.length > 0 && args[0].equalsIgnoreCase("--once"));

        System.out.println("\n>>> [DEMONIO] Servidor de correos iniciado en bucle continuo.");
        System.out.println(">>> Monitoreando buzón: " + Config.MAIL_EMISOR + " cada " + (Config.POLL_INTERVAL_MS / 1000) + " segundos...\n");

        // 2. Loop continuo del Daemon
        while (true) {
            try {
                // FASE 1: LECTURA VÍA POP3
                List<MailMessage> pendingEmails = pop3.readPendingMessages();

                if (!pendingEmails.isEmpty()) {
                    System.out.println("\n>>> [NOTIFICACIÓN] Se detectaron " + pendingEmails.size() + " correo(s) nuevo(s) para procesar.");

                    for (MailMessage msg : pendingEmails) {
                        System.out.println("\n------------------------------------------------------------");
                        System.out.println(">>> PROCESANDO PETICIÓN ENTRANTE:");
                        System.out.println("    - Remitente (From): " + msg.getFrom());
                        System.out.println("    - Asunto (Subject): " + msg.getSubject());
                        System.out.println("    - Fecha de envío:   " + msg.getDate());
                        System.out.println("------------------------------------------------------------");

                        if (msg.getFrom() == null || msg.getFrom().trim().isEmpty()) {
                            System.err.println(">>> [OMITIDO] Correo sin dirección de remitente válida.");
                            continue;
                        }

                        // FASE 2: INTERPRETACIÓN Y EJECUCIÓN DEL COMANDO
                        CommandResult result = dispatcher.dispatch(msg.getFrom(), msg.getSubject());

                        // FASE 3: ENVÍO DE RESPUESTA VÍA SMTP
                        SMTPSender.sendHtmlEmail(msg.getFrom(), result.getSubject(), result.getHtmlBody());
                        System.out.println(">>> [COMPLETADO] Respuesta entregada con éxito a: " + msg.getFrom());
                    }
                }

            } catch (Exception e) {
                System.err.println(">>> [ERROR EN CICLO DE CORREO] " + e.getMessage());
            }

            if (runOnce) {
                System.out.println("\n>>> [MODO --once] Finalizando ejecución tras un ciclo.");
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
        System.out.println("   SISTEMA DE GESTIÓN VÍA EMAIL – CRM EDUCATIVO (INF513 GRUPO 05 SC)     ");
        System.out.println("   Msc. Ing. Evans Balcázar Veizaga • Facultad de Cs. de la Computación   ");
        System.out.println("   Integrantes: Nataly Martinez (Dev A), Nicol (Dev B), Jael (Dev C)     ");
        System.out.println("==========================================================================");
        System.out.println(" • Servidor Correo: " + Config.MAIL_HOST + " (POP3: " + Config.POP3_PORT + ", SMTP: " + Config.SMTP_PORT + ")");
        System.out.println(" • Cuenta Oficial:  " + Config.MAIL_EMISOR);
        System.out.println(" • Base de Datos:   " + Config.DB_URL);
        System.out.println("==========================================================================\n");
    }
}
