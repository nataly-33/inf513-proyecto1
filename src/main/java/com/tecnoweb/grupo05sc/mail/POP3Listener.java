package com.tecnoweb.grupo05sc.mail;

import com.tecnoweb.grupo05sc.config.Config;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Lector de correos entrantes mediante protocolo POP3 (RFC 1939) sobre Sockets TCP puros.
 * Registra el dialogo de bajo nivel en 'logs/socket_traffic.log' (Terminal 2) y eventos clave en Terminal 1.
 */
public final class POP3Listener {

    /**
     * Conecta al servidor POP3, autentica al usuario, descarga los mensajes pendientes,
     * los marca para eliminacion con DELE para no reprocesarlos, y retorna la lista de mensajes.
     */
    public List<MailMessage> readPendingMessages() throws IOException {
        List<MailMessage> messages = new ArrayList<>();
        Socket socket = null;
        BufferedReader in = null;
        DataOutputStream out = null;

        try {
            SocketLogger.info("[POP3] Conectando a servidor de correo " + Config.MAIL_HOST + ":" + Config.POP3_PORT + "...");
            socket = new Socket(Config.MAIL_HOST, Config.POP3_PORT);
            socket.setSoTimeout(Config.SOCKET_TIMEOUT_MS);

            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
            out = new DataOutputStream(socket.getOutputStream());

            // 1. Saludo inicial del servidor POP3 (+OK ...)
            String banner = in.readLine();
            SocketLogger.server(banner);
            if (banner == null || !banner.startsWith("+OK")) {
                throw new IOException("Servidor POP3 rechazo la conexion: " + banner);
            }

            // 2. USER
            String cmdUser = "USER " + Config.POP3_USER;
            SocketLogger.client(cmdUser);
            out.writeBytes(cmdUser + "\r\n");
            out.flush();
            String respUser = in.readLine();
            SocketLogger.server(respUser);
            if (respUser == null || !respUser.startsWith("+OK")) {
                throw new IOException("Error en comando USER: " + respUser);
            }

            // 3. PASS
            String cmdPass = "PASS " + Config.POP3_PASS;
            SocketLogger.client("PASS ********");
            out.writeBytes(cmdPass + "\r\n");
            out.flush();
            String respPass = in.readLine();
            SocketLogger.server(respPass);
            if (respPass == null || !respPass.startsWith("+OK")) {
                throw new IOException("Error de autenticacion PASS: " + respPass);
            }

            // 4. STAT (Obtener total de mensajes y tamano en bytes)
            SocketLogger.client("STAT");
            out.writeBytes("STAT\r\n");
            out.flush();
            String respStat = in.readLine();
            SocketLogger.server(respStat);
            if (respStat == null || !respStat.startsWith("+OK")) {
                throw new IOException("Error en comando STAT: " + respStat);
            }

            String[] partesStat = respStat.split(" ");
            if (partesStat.length < 2) {
                throw new IOException("Respuesta STAT invalida: " + respStat);
            }

            int totalMensajes = Integer.parseInt(partesStat[1]);

            if (totalMensajes == 0) {
                // Sin correos pendientes
                SocketLogger.client("QUIT");
                out.writeBytes("QUIT\r\n");
                out.flush();
                String respQuit = in.readLine();
                SocketLogger.server(respQuit);
                return messages;
            }

            SocketLogger.info("[POP3] " + totalMensajes + " correo(s) detectado(s) en buzon. Descargando...");

            // 5. Descargar cada mensaje con RETR y marcarlo para eliminar con DELE
            for (int i = 1; i <= totalMensajes; i++) {
                String cmdRetr = "RETR " + i;
                SocketLogger.client(cmdRetr);
                out.writeBytes(cmdRetr + "\r\n");
                out.flush();

                String respRetr = in.readLine();
                SocketLogger.server(respRetr);
                if (respRetr == null || !respRetr.startsWith("+OK")) {
                    SocketLogger.error("[POP3] No se pudo leer el mensaje #" + i + ": " + respRetr);
                    continue;
                }

                // Lectura multilinea terminada en un solo punto "." (RFC 1939)
                String contenidoCrudo = readMultilinePOP(in);

                // Parsear encabezados y cuerpo del correo
                MailMessage msg = parseRawEmail(i, String.valueOf(i), contenidoCrudo);

                if (msg != null) {
                    // Evitar bucles infinitos: ignorar correos de auto-respuesta
                    if (msg.getSubject().toUpperCase().startsWith(Config.SUBJECT_PREFIX.toUpperCase())) {
                        SocketLogger.info("[POP3] Omitiendo correo de auto-respuesta (" + msg.getSubject() + ")");
                    } else {
                        messages.add(msg);
                    }
                }

                // 6. DELE (Marcar como eliminado para no reprocesarlo en la siguiente ronda)
                String cmdDele = "DELE " + i;
                SocketLogger.client(cmdDele);
                out.writeBytes(cmdDele + "\r\n");
                out.flush();
                String respDele = in.readLine();
                SocketLogger.server(respDele);
            }

            // 7. QUIT (Aplica las eliminaciones y cierra la sesion)
            SocketLogger.client("QUIT");
            out.writeBytes("QUIT\r\n");
            out.flush();
            String respQuit = in.readLine();
            SocketLogger.server(respQuit);

            return messages;

        } finally {
            // Doctrina de la materia: Cierre garantizado en orden inverso en bloque finally
            try { if (out != null) out.close(); } catch (Exception ignored) {}
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            try { if (socket != null && !socket.isClosed()) socket.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * Lectura multilinea segun RFC 1939: Lee lineas hasta encontrar "." solitario.
     * Implementa des-escape de puntos (Byte-Stuffing).
     */
    private static String readMultilinePOP(BufferedReader in) throws IOException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            String line = in.readLine();
            if (line == null) {
                throw new IOException("El servidor cerro abruptamente la conexion POP3.");
            }
            if (line.equals(".")) {
                break; // Fin del mensaje
            }
            // Byte-stuffing: si la linea comienza con un punto, se retira el primer punto
            if (line.length() > 0 && line.charAt(0) == '.') {
                line = line.substring(1);
            }
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    /**
     * Parsea el texto crudo del correo separando cabeceras RFC 5322 del cuerpo del mensaje.
     */
    private static MailMessage parseRawEmail(int messageNumber, String uid, String raw) {
        String[] lines = raw.split("\n");
        String subject = "";
        String rawFrom = "";
        String fromEmail = "";
        String date = "";
        StringBuilder bodyBuilder = new StringBuilder();

        boolean inHeaders = true;

        for (String line : lines) {
            if (inHeaders) {
                // Una linea en blanco marca el fin de las cabeceras
                if (line.trim().isEmpty()) {
                    inHeaders = false;
                    continue;
                }

                String lower = line.toLowerCase();
                if (lower.startsWith("subject:")) {
                    subject = line.substring(8).trim();
                } else if (lower.startsWith("from:")) {
                    rawFrom = line.substring(5).trim();
                    fromEmail = extractEmailAddress(rawFrom);
                } else if (lower.startsWith("date:")) {
                    date = line.substring(5).trim();
                }
            } else {
                bodyBuilder.append(line).append("\n");
            }
        }

        return new MailMessage(messageNumber, uid, fromEmail, rawFrom, subject, bodyBuilder.toString().trim(), date);
    }

    /**
     * Extrae el email puro de cadenas como: "Juan Perez <juanperez@uagrm.edu.bo>" o "juanperez@uagrm.edu.bo".
     */
    public static String extractEmailAddress(String rawFrom) {
        if (rawFrom == null || rawFrom.trim().isEmpty()) return "";
        String trimmed = rawFrom.trim();
        if (trimmed.contains("<") && trimmed.contains(">")) {
            int start = trimmed.indexOf('<') + 1;
            int end = trimmed.indexOf('>');
            if (start < end) {
                return trimmed.substring(start, end).trim();
            }
        }
        return trimmed;
    }
}
