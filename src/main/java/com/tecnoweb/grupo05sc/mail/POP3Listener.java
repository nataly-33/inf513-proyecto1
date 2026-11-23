package com.tecnoweb.grupo05sc.mail;

import com.tecnoweb.grupo05sc.config.Config;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * Implementa:
     *   - RFC 2822 / RFC 5322 Section 2.2.3 Header Unfolding (despliega lineas continuadas que inician con espacio o tab).
     *   - RFC 2047 MIME Encoded-Word Decoding (=?UTF-8?B?...?= y =?UTF-8?Q?...?=) para soportar acentos y caracteres especiales.
     */
    public static MailMessage parseRawEmail(int messageNumber, String uid, String raw) {
        String[] lines = raw.split("\n");
        List<String> headerLines = new ArrayList<>();
        StringBuilder currentHeader = null;
        boolean inHeaders = true;
        StringBuilder bodyBuilder = new StringBuilder();

        for (String rawLine : lines) {
            String line = rawLine.endsWith("\r") ? rawLine.substring(0, rawLine.length() - 1) : rawLine;
            if (inHeaders) {
                // Una linea en blanco marca el fin de las cabeceras
                if (line.trim().isEmpty()) {
                    if (currentHeader != null) {
                        headerLines.add(currentHeader.toString());
                        currentHeader = null;
                    }
                    inHeaders = false;
                    continue;
                }

                // Despliegue de cabeceras plegadas (Header Unfolding - RFC 2822 Seccion 2.2.3)
                // Si la linea comienza con espacio ' ' o tabulador '\t', es continuacion de la cabecera anterior.
                if ((line.startsWith(" ") || line.startsWith("\t")) && currentHeader != null) {
                    currentHeader.append(" ").append(line.trim());
                } else {
                    if (currentHeader != null) {
                        headerLines.add(currentHeader.toString());
                    }
                    currentHeader = new StringBuilder(line);
                }
            } else {
                bodyBuilder.append(rawLine).append("\n");
            }
        }
        if (currentHeader != null) {
            headerLines.add(currentHeader.toString());
        }

        String subject = "";
        String rawFrom = "";
        String fromEmail = "";
        String date = "";

        for (String header : headerLines) {
            String lower = header.toLowerCase();
            if (lower.startsWith("subject:")) {
                subject = header.substring(8).trim();
                subject = decodeRFC2047(subject);
            } else if (lower.startsWith("from:")) {
                rawFrom = header.substring(5).trim();
                rawFrom = decodeRFC2047(rawFrom);
                fromEmail = extractEmailAddress(rawFrom);
            } else if (lower.startsWith("date:")) {
                date = header.substring(5).trim();
            }
        }

        // Respaldo de seguridad: si el asunto viene vacio pero el cuerpo inicia con un comando
        if (subject.isEmpty() && !bodyBuilder.toString().trim().isEmpty()) {
            String firstLine = bodyBuilder.toString().trim().split("\n")[0].trim();
            if (firstLine.matches("^[A-Za-z0-9_]+\\s*(\\[.*\\])?$")) {
                subject = firstLine;
            }
        }

        return new MailMessage(messageNumber, uid, fromEmail, rawFrom, subject, bodyBuilder.toString().trim(), date);
    }

    /**
     * Decodifica palabras codificadas segun RFC 2047 (=?charset?encoding?encoded_text?=).
     * Soporta Base64 ('B') y Quoted-Printable ('Q'), uniendo fragmentos adyacentes
     * e ignorando espacios lineales intermedios segun la Seccion 6.2 del RFC.
     */
    public static String decodeRFC2047(String text) {
        if (text == null || !text.contains("=?")) {
            return text;
        }

        try {
            // Eliminar espacios lineales entre encoded-words adyacentes segun RFC 2047 Seccion 6.2
            String cleaned = text.replaceAll("(?i)(\\?=\\s+=\\?)", "?==?");

            Pattern p = Pattern.compile("=\\?([^?]+)\\?([BbQq])\\?([^?]+)\\?=");
            Matcher m = p.matcher(cleaned);
            StringBuffer sb = new StringBuffer();

            while (m.find()) {
                String charsetName = m.group(1);
                String encoding = m.group(2).toUpperCase();
                String encodedData = m.group(3);

                Charset charset;
                try {
                    charset = Charset.forName(charsetName);
                } catch (Exception e) {
                    charset = StandardCharsets.UTF_8;
                }

                String decodedPart;
                if ("B".equals(encoding)) {
                    byte[] bytes = Base64.getDecoder().decode(encodedData.trim());
                    decodedPart = new String(bytes, charset);
                } else if ("Q".equals(encoding)) {
                    byte[] bytes = decodeQuotedPrintable(encodedData);
                    decodedPart = new String(bytes, charset);
                } else {
                    decodedPart = m.group(0);
                }

                m.appendReplacement(sb, Matcher.quoteReplacement(decodedPart));
            }
            m.appendTail(sb);
            return sb.toString();
        } catch (Exception e) {
            SocketLogger.error("[POP3] Excepcion al decodificar RFC 2047 (" + text + "): " + e.getMessage());
            return text;
        }
    }

    private static byte[] decodeQuotedPrintable(String s) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '_') {
                baos.write(' ');
            } else if (c == '=' && i + 2 < s.length()) {
                String hex = s.substring(i + 1, i + 3);
                try {
                    int b = Integer.parseInt(hex, 16);
                    baos.write(b);
                    i += 2;
                } catch (NumberFormatException e) {
                    baos.write('=');
                }
            } else {
                baos.write((byte) c);
            }
        }
        return baos.toByteArray();
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
