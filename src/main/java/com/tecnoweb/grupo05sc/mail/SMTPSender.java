package com.tecnoweb.grupo05sc.mail;

import com.tecnoweb.grupo05sc.config.Config;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Emisor de respuestas mediante protocolo SMTP (RFC 5321) sobre Sockets TCP puros.
 * Genera correos en formato HTML (RFC 5322 / MIME) con codificacion UTF-8.
 * Registra el dialogo de bajo nivel en 'logs/socket_traffic.log' (Terminal 2) y eventos clave en Terminal 1.
 */
public final class SMTPSender {

    /**
     * Envia un correo con contenido HTML estilizado al destinatario indicado.
     */
    public static void sendHtmlEmail(String toEmail, String subject, String htmlContent) throws IOException {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("El destinatario no puede ser nulo o vacio.");
        }

        Socket socket = null;
        BufferedReader in = null;
        DataOutputStream out = null;

        try {
            SocketLogger.info("[SMTP] Enviando respuesta a " + toEmail + " via " + Config.MAIL_HOST + ":" + Config.SMTP_PORT + "...");
            socket = new Socket(Config.MAIL_HOST, Config.SMTP_PORT);
            socket.setSoTimeout(Config.SOCKET_TIMEOUT_MS);

            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
            out = new DataOutputStream(socket.getOutputStream());

            // 1. Saludo inicial del servidor SMTP (220 ...)
            String banner = in.readLine();
            SocketLogger.server(banner);
            if (banner == null || !banner.startsWith("220")) {
                throw new IOException("Servidor SMTP no disponible o rechazo la conexion: " + banner);
            }

            // 2. HELO (Estandar de la materia)
            String cmdHelo = "HELO " + Config.MAIL_HOST;
            SocketLogger.client(cmdHelo);
            out.writeBytes(cmdHelo + "\r\n");
            out.flush();
            String respHelo = in.readLine();
            SocketLogger.server(respHelo);
            if (respHelo == null || !respHelo.startsWith("250")) {
                throw new IOException("Error en comando HELO: " + respHelo);
            }

            // 3. MAIL FROM
            String cmdMail = "MAIL FROM: <" + Config.MAIL_EMISOR + ">";
            SocketLogger.client(cmdMail);
            out.writeBytes(cmdMail + "\r\n");
            out.flush();
            String respMail = in.readLine();
            SocketLogger.server(respMail);
            if (respMail == null || !respMail.startsWith("250")) {
                throw new IOException("Error en comando MAIL FROM: " + respMail);
            }

            // 4. RCPT TO
            String cmdRcpt = "RCPT TO: <" + toEmail.trim() + ">";
            SocketLogger.client(cmdRcpt);
            out.writeBytes(cmdRcpt + "\r\n");
            out.flush();
            String respRcpt = in.readLine();
            SocketLogger.server(respRcpt);
            if (respRcpt == null || !respRcpt.startsWith("250")) {
                throw new IOException("Error en comando RCPT TO para <" + toEmail + ">: " + respRcpt);
            }

            // 5. DATA
            SocketLogger.client("DATA");
            out.writeBytes("DATA\r\n");
            out.flush();
            String respData = in.readLine();
            SocketLogger.server(respData);
            if (respData == null || !respData.startsWith("354")) {
                throw new IOException("Error en comando DATA: " + respData);
            }

            // 6. Transmision del mensaje (Cabeceras RFC 5322 + Linea en blanco + Cuerpo HTML)
            // Codificacion segura Base64 del Asunto segun RFC 2047
            String encodedSubject = "=?UTF-8?B?" + Base64.getEncoder().encodeToString(subject.getBytes(StandardCharsets.UTF_8)) + "?=";

            StringBuilder rfcMessage = new StringBuilder();
            rfcMessage.append("From: ").append(Config.MAIL_EMISOR).append("\r\n");
            rfcMessage.append("To: <").append(toEmail.trim()).append(">\r\n");
            rfcMessage.append("Subject: ").append(encodedSubject).append("\r\n");
            rfcMessage.append("MIME-Version: 1.0\r\n");
            rfcMessage.append("Content-Type: text/html; charset=UTF-8\r\n");
            rfcMessage.append("Content-Transfer-Encoding: 8bit\r\n");
            rfcMessage.append("\r\n"); // Linea en blanco que separa cabeceras del cuerpo (OBLIGATORIA)
            rfcMessage.append(htmlContent).append("\r\n");
            rfcMessage.append(".\r\n"); // Fin del bloque DATA: <CRLF>.<CRLF>

            SocketLogger.client("[Cabeceras MIME y cuerpo HTML]");
            SocketLogger.client(".");

            // Escritura en UTF-8 hacia el flujo de salida
            out.write(rfcMessage.toString().getBytes(StandardCharsets.UTF_8));
            out.flush();

            String respEnvio = in.readLine();
            SocketLogger.server(respEnvio);
            if (respEnvio == null || !respEnvio.startsWith("250")) {
                throw new IOException("Error al transmitir datos del correo: " + respEnvio);
            }

            // 7. QUIT
            SocketLogger.client("QUIT");
            out.writeBytes("QUIT\r\n");
            out.flush();
            String respQuit = in.readLine();
            SocketLogger.server(respQuit);

            SocketLogger.info("[SMTP] Correo entregado satisfactoriamente a: " + toEmail);

        } finally {
            // Doctrina de la materia: Cierre garantizado en orden inverso en bloque finally
            try { if (out != null) out.close(); } catch (Exception ignored) {}
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            try { if (socket != null && !socket.isClosed()) socket.close(); } catch (Exception ignored) {}
        }
    }
}
