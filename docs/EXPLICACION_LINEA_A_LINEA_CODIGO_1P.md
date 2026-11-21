# 📘 GUÍA MAESTRA: EXPLICACIÓN LÍNEA A LÍNEA DEL CÓDIGO FUENTE (1ER PARCIAL)
### INF513 - Tecnología Web | Grupo 05 SC | Docente: Msc. Ing. Evans Balcázar Veizaga
### Sistema Automatizado vía Correo Electrónico – CRM Educativo (FICCT - UAGRM)

---

## 🧭 Índice General del Recorrido de Clases

1. [Arquitectura General: El Flujo del Correo a Bajo Nivel](#1-arquitectura-general-el-flujo-del-correo-a-bajo-nivel)
2. [Clase `Main.java` (El Demonio en Bucle Continuo)](#2-clase-mainjava)
3. [Clase `config/Config.java` (Cargador Nativo de Configuración y Entorno)](#3-clase-configconfigjava)
4. [Clase `config/DatabaseConnection.java` (Gestor de Sockets JDBC y Cierre Seguro)](#4-clase-configdatabaseconnectionjava)
5. [Clase `mail/MailMessage.java` (DTO de Mensaje POP3 Descargado)](#5-clase-mailmailmessagejava)
6. [Clase `mail/POP3Listener.java` (Descarga vía Sockets TCP Puerto 110 - RFC 1939)](#6-clase-mailpop3listenerjava)
7. [Clase `mail/SMTPSender.java` (Emisión vía Sockets TCP Puerto 25 - RFC 5321 y MIME 5322)](#7-clase-mailsmtpsenderjava)
8. [Clase `command/CommandParser.java` (El Analizador Léxico / Intérprete)](#8-clase-commandcommandparserjava)
9. [Clases `ParsedCommand.java`, `CommandResult.java` y `CommandException.java`](#9-clases-del-paquete-command-soporte)
10. [Clases `security/UsuarioAuth.java` y `security/SecurityContext.java` (Identidad y RBAC)](#10-clases-del-paquete-security)
11. [Clase `view/HtmlTemplate.java` (Generador de Vistas HTML Corporativas)](#11-clase-viewhtmltemplatejava)
12. [Clase `dao/UsuarioDao.java` (Capa de Acceso a Datos - CU1 Nataly)](#12-clase-daousuariodaojava)
13. [Clase `service/UsuarioService.java` (Lógica de Negocio y Reglas de Oro - CU1 Nataly)](#13-clase-serviceusuarioservicejava)
14. [Clase `command/CommandDispatcher.java` (Enrutador Central y Despacho)](#14-clase-commandcommanddispatcherjava)
15. [Banco de Preguntas Clave para la Defensa Oral con el Ing. Evans](#15-banco-de-preguntas-clave-para-la-defensa-oral)

---

## 1. Arquitectura General: El Flujo del Correo a Bajo Nivel

```
               [ Cliente / Usuario ]
               (Envía correo con Asunto: CREARUSUARIO["Juan",...])
                         │
                         ▼
             [ Servidor mail.tecnoweb.org.bo ]
                   (Buzón POP3 :110)
                         │
               ┌─────────┴─────────┐
               │ POP3Listener.java │ (Socket TCP :110, USER, PASS, STAT, RETR, DELE)
               └─────────┬─────────┘
                         ▼
               [ MailMessage (DTO) ]
                         │
                         ▼
             [ CommandParser.java ]  (Regex, extrae comando y array de parámetros)
                         │
                         ▼
          [ SecurityContext.java ]  (Verifica From: contra tabla usuarios en PostgreSQL)
                         │
                         ▼
           [ CommandDispatcher.java ]  (Enruta al CU correspondiente)
                         │
                         ▼
             [ UsuarioService.java ] (Aplica reglas: anti-autobloqueo, SHA-256)
                         │
                         ▼
               [ UsuarioDao.java ]   (PreparedStatement SQL contra PostgreSQL :5432)
                         │
                         ▼
             [ HtmlTemplate.java ]   (Renderiza respuesta en tabla / tarjeta HTML)
                         │
                         ▼
              [ SMTPSender.java ]    (Socket TCP :25, HELO, MAIL FROM, RCPT TO, DATA)
                         │
                         ▼
             [ Servidor mail.tecnoweb.org.bo ]
                         │
                         ▼
               [ Cliente / Usuario ]
               (Recibe respuesta HTML estilizada con prefijo RE: [CRM-EDUCATIVO])
```

---

## 2. Clase `Main.java`

**Ubicación:** `src/main/java/com/tecnoweb/grupo05sc/Main.java`  
**Responsabilidad:** Punto de entrada (`entry point`) de la aplicación en la Máquina Virtual Java (JVM). Funciona como un **Demonio del Sistema Operativo** (proceso en segundo plano sin interfaz gráfica) que sondea cíclicamente el buzón de correos cada 3 segundos.

### Código y Explicación Línea por Línea:

```java
1: package com.tecnoweb.grupo05sc;
```
* **Línea 1:** Declara el paquete raíz del proyecto. Agrupa el código bajo la convención internacional de dominio inverso acordada (`com.tecnoweb.grupo05sc`).

```java
3: import com.tecnoweb.grupo05sc.command.CommandDispatcher;
4: import com.tecnoweb.grupo05sc.command.CommandResult;
5: import com.tecnoweb.grupo05sc.config.Config;
6: import com.tecnoweb.grupo05sc.config.DatabaseConnection;
7: import com.tecnoweb.grupo05sc.mail.MailMessage;
8: import com.tecnoweb.grupo05sc.mail.POP3Listener;
9: import com.tecnoweb.grupo05sc.mail.SMTPSender;
10: import java.sql.Connection;
11: import java.util.List;
```
* **Líneas 3-11:** Importa las dependencias necesarias de las otras capas del sistema (configuración, red de correo, despacho de comandos y conexión JDBC). Nótese que **no se importa ninguna librería externa ni frameworks pesados**; solo clases propias y de la biblioteca estándar de Java SE (`java.sql`, `java.util`).

```java
18: public class Main {
20:     public static void main(String[] args) {
21:         printBanner();
```
* **Línea 18:** Definición de la clase pública `Main`.
* **Línea 20:** Método `public static void main(String[] args)`:
  * `public`: Accesible por el cargador de clases de la JVM desde el exterior.
  * `static`: Se ejecuta a nivel de clase sin requerir instanciar un objeto `new Main()`.
  * `void`: No retorna ningún valor numérico de salida al terminar.
  * `String[] args`: Recibe parámetros de línea de comandos (ej. `--once`).
* **Línea 21:** Llama al método auxiliar `printBanner()` que imprime en la consola los datos del grupo, servidores y puertos para auditoría visual.

```java
23:         // 1. Verificación previa de conexión a PostgreSQL
24:         System.out.println(">>> [INICIO] Verificando conexión a PostgreSQL...");
25:         try (Connection conn = DatabaseConnection.getConnection()) {
26:             if (conn != null && !conn.isClosed()) {
27:                 System.out.println(">>> [DB] ¡Conexión exitosa a la base de datos: " + Config.DB_URL + "!");
28:             }
29:         } catch (Exception e) {
30:             System.err.println(">>> [ALERTA DB] No se pudo conectar a PostgreSQL: " + e.getMessage());
31:             System.err.println("    Asegúrese de que el servidor PostgreSQL esté corriendo en " + Config.DB_HOST + ":" + Config.DB_PORT);
32:         }
```
* **Línea 25:** Uso de la instrucción `try-with-resources` de Java 7+. Crea una conexión de prueba con `DatabaseConnection.getConnection()`. Al implementar la interfaz `AutoCloseable`, Java garantiza que la conexión `conn` se cierre automáticamente al salir de la llave `}`, incluso si ocurre una excepción, evitando "fugas de memoria" (*connection leaks*).
* **Líneas 26-28:** Si la conexión está abierta y sana, avisa en terminal que PostgreSQL responde.
* **Líneas 29-32:** Si PostgreSQL estuviera apagado, no tumba el demonio, sino que emite una advertencia descriptiva con el host y puerto esperados.

```java
34:         POP3Listener pop3 = new POP3Listener();
35:         CommandDispatcher dispatcher = new CommandDispatcher();
37:         boolean runOnce = (args.length > 0 && args[0].equalsIgnoreCase("--once"));
```
* **Línea 34:** Instancia el objeto `pop3`, responsable de abrir el Socket TCP hacia el puerto 110.
* **Línea 35:** Instancia el objeto `dispatcher`, el enrutador que interpretará el texto del Asunto.
* **Línea 37:** Evalúa si se pasó el argumento `--once` al arrancar el programa. Si es verdadero, el programa ejecutará una sola vuelta y terminará (muy útil para pruebas unitarias automatizadas o scripts de test sin dejar el proceso colgado).

```java
43:         while (true) {
44:             try {
45:                 // FASE 1: LECTURA VÍA POP3
46:                 List<MailMessage> pendingEmails = pop3.readPendingMessages();
```
* **Línea 43:** `while (true)`: El bucle infinito característico de los **daemons de servidor**. Mantiene el proceso vivo escuchando eventos.
* **Línea 44:** Bloque `try`: Protege todo el ciclo de trabajo. Si falla la red durante la lectura de un correo, la excepción es atrapada en la línea 73 y el servidor no colapsa, sino que reintenta en el próximo ciclo.
* **Línea 46:** Ejecuta la **FASE 1**: Conecta por socket a `mail.tecnoweb.org.bo:110`, descarga los correos pendientes, los marca para borrar (`DELE`) y los devuelve en una lista en memoria de tipo `MailMessage`.

```java
48:                 if (!pendingEmails.isEmpty()) {
49:                     System.out.println("\n>>> [NOTIFICACIÓN] Se detectaron " + pendingEmails.size() + " correo(s) nuevo(s) para procesar.");
51:                     for (MailMessage msg : pendingEmails) {
```
* **Línea 48:** Pregunta si la lista de correos pendientes contiene al menos un mensaje.
* **Línea 51:** Itera uno a uno los correos recibidos mediante un bucle `for-each`.

```java
52:                         System.out.println("\n------------------------------------------------------------");
53:                         System.out.println(">>> PROCESANDO PETICIÓN ENTRANTE:");
54:                         System.out.println("    - Remitente (From): " + msg.getFrom());
55:                         System.out.println("    - Asunto (Subject): " + msg.getSubject());
56:                         System.out.println("    - Fecha de envío:   " + msg.getDate());
57:                         System.out.println("------------------------------------------------------------");
59:                         if (msg.getFrom() == null || msg.getFrom().trim().isEmpty()) {
60:                             System.err.println(">>> [OMITIDO] Correo sin dirección de remitente válida.");
61:                             continue;
62:                         }
```
* **Líneas 52-57:** Traza de auditoría en consola. Muestra quién mandó el correo y con qué asunto.
* **Líneas 59-62:** Filtro de sanidad: Si el correo viniera corrupto sin remitente, lo ignora con `continue` y pasa al siguiente mensaje.

```java
64:                         // FASE 2: INTERPRETACIÓN Y EJECUCIÓN DEL COMANDO
65:                         CommandResult result = dispatcher.dispatch(msg.getFrom(), msg.getSubject());
```
* **Línea 65:** **FASE 2**: Le pasa al `dispatcher` el correo del remitente y el texto del Asunto. Aquí se realiza todo el trabajo de base de datos y validaciones de negocio, retornando un objeto `CommandResult` con el asunto de respuesta y el HTML generado.

```java
67:                         // FASE 3: ENVÍO DE RESPUESTA VÍA SMTP
68:                         SMTPSender.sendHtmlEmail(msg.getFrom(), result.getSubject(), result.getHtmlBody());
69:                         System.out.println(">>> [COMPLETADO] Respuesta entregada con éxito a: " + msg.getFrom());
70:                     }
71:                 }
```
* **Línea 68:** **FASE 3**: Abre un Socket TCP hacia `mail.tecnoweb.org.bo:25` (SMTP) y le despacha la respuesta en formato HTML directamente al buzón del remitente original (`msg.getFrom()`).
* **Línea 69:** Confirma en terminal que la transacción de correo fue finalizada y enviada.

```java
73:             } catch (Exception e) {
74:                 System.err.println(">>> [ERROR EN CICLO DE CORREO] " + e.getMessage());
75:             }
```
* **Líneas 73-75:** Atrapa cualquier error imprevisto (ej. caída momentánea de internet o timeout) para que el hilo principal siga con vida.

```java
77:             if (runOnce) {
78:                 System.out.println("\n>>> [MODO --once] Finalizando ejecución tras un ciclo.");
79:                 break;
80:             }
```
* **Líneas 77-80:** Si se arrancó con `--once`, rompe el bucle `while` con `break` y finaliza.

```java
82:             try {
83:                 Thread.sleep(Config.POLL_INTERVAL_MS);
84:             } catch (InterruptedException e) {
85:                 System.out.println(">>> [DETENIDO] Hilo interrumpido.");
86:                 break;
87:             }
88:         }
89:     }
```
* **Línea 83:** `Thread.sleep(3000)`: Suspende la ejecución del hilo durante 3 segundos (3000 ms). **¿Por qué esto es vital?** Si no pusiéramos `sleep`, el bucle `while(true)` consumiría el 100% de la CPU de tu laptop y además saturaría el servidor de correo de la universidad con millones de peticiones por segundo, lo que causaría que el servidor bloquee la IP de nuestro grupo.
* **Línea 84:** Si el sistema operativo envía una señal de terminación (ej. `Ctrl + C`), se captura `InterruptedException` y se sale limpiamente del bucle.

---

## 3. Clase `config/Config.java`

**Ubicación:** `src/main/java/com/tecnoweb/grupo05sc/config/Config.java`  
**Responsabilidad:** Centraliza todas las constantes del sistema (hosts, puertos, credenciales). Lee el archivo de variables de entorno `.env` **de forma manual con Java puro**, sin depender de librerías externas de terceros.

### Código y Explicación Línea por Línea:

```java
16: public final class Config {
18:     private Config() {}
```
* **Línea 16:** Declarada como `public final`: Al ser `final`, no puede ser heredada por otra clase.
* **Línea 18:** Constructor privado `private Config()`: Patrón de diseño *Utility Class*. Impide que alguien haga accidentalmente `new Config()`, ya que todos sus métodos y atributos son estáticos.

```java
20:     private static final Map<String, String> ENV = loadEnv();
22:     private static Map<String, String> loadEnv() {
23:         Map<String, String> map = new HashMap<>();
24:         File envFile = new File(".env");
25:         if (!envFile.exists()) {
26:             envFile = new File("INF513_P1/.env");
27:         }
```
* **Línea 20:** `ENV`: Estructura en memoria `HashMap<Clave, Valor>` que se carga una sola vez al iniciar la aplicación.
* **Líneas 24-27:** Busca el archivo `.env` en el directorio actual; si se ejecuta desde la raíz del workspace, busca en `INF513_P1/.env`.

```java
28:         if (envFile.exists()) {
29:             try (BufferedReader br = new BufferedReader(new FileReader(envFile))) {
30:                 String line;
31:                 while ((line = br.readLine()) != null) {
32:                     line = line.trim();
33:                     if (line.isEmpty() || line.startsWith("#")) continue;
34:                     int eq = line.indexOf('=');
35:                     if (eq > 0) {
36:                         String key = line.substring(0, eq).trim();
37:                         String val = line.substring(eq + 1).trim();
38:                         map.put(key, val);
39:                     }
40:                 }
41:             } catch (Exception ignored) {}
42:         }
43:         return map;
44:     }
```
* **Línea 29:** Abre el archivo mediante flujos de entrada de caracteres `BufferedReader` envuelto en `FileReader`.
* **Línea 31:** Lee el archivo línea por línea hasta llegar al final (`null`).
* **Línea 33:** Si la línea está vacía o comienza con `#` (comentario), la ignora con `continue`.
* **Línea 34:** Busca la posición del signo `=` con `indexOf('=')`.
* **Líneas 36-38:** Divide la línea en dos partes: antes del igual es la `key` (clave) y después del igual es el `val` (valor), guardándolos en el `map`.

```java
46:     private static String get(String key, String defaultValue) {
47:         String val = ENV.get(key);
48:         return (val != null && !val.isEmpty()) ? val : defaultValue;
49:     }
51:     private static int getInt(String key, int defaultValue) {
52:         try {
53:             return Integer.parseInt(get(key, String.valueOf(defaultValue)));
54:         } catch (NumberFormatException e) {
55:             return defaultValue;
56:         }
57:     }
```
* **Líneas 46-49:** Método utilitario que consulta una clave en el mapa. Si no existe o está vacía, devuelve el valor por defecto (`defaultValue`).
* **Líneas 51-57:** Similar al anterior pero convierte la cadena a entero (`Integer.parseInt`). Si hay un error de formato numérico, devuelve el valor por defecto de manera segura.

```java
62:     public static final String MAIL_HOST = get("MAIL_HOST", "mail.tecnoweb.org.bo");
63:     public static final int POP3_PORT = getInt("POP3_PORT", 110);
64:     public static final int SMTP_PORT = getInt("SMTP_PORT", 25);
66:     public static final String POP3_USER = get("POP3_USER", "grupo05sc");
67:     public static final String POP3_PASS = get("POP3_PASS", "grup005grup005*");
68:     public static final String MAIL_EMISOR = get("MAIL_EMISOR", "grupo05sc@tecnoweb.org.bo");
71:     public static final String SUBJECT_PREFIX = "RE: [CRM-EDUCATIVO]";
74:     public static final int POLL_INTERVAL_MS = 3000;
77:     public static final int SOCKET_TIMEOUT_MS = 10000;
```
* **Líneas 62-68:** Parámetros de conexión a la cuenta oficial del Grupo 05 SC en el servidor de la universidad.
* **Línea 71:** `SUBJECT_PREFIX`: Prefijo de respuesta. Fundamental para que el sistema identifique sus propios correos enviados y nunca los procese como peticiones entrantes, evitando bucles infinitos de auto-respuestas.
* **Línea 77:** `SOCKET_TIMEOUT_MS`: 10.000 milisegundos (10 segundos). Si el servidor de correos no responde en 10 segundos, el socket se desbloquea lanzando una excepción y no congela el hilo.

```java
82:     public static final String DB_HOST = get("DB_HOST", "localhost");
83:     public static final String DB_PORT = get("DB_PORT", "5432");
84:     public static final String DB_NAME = get("DB_NAME", "crm_educativo");
85:     public static final String DB_USER = get("DB_USER", "postgres");
86:     public static final String DB_PASS = get("DB_PASS", "postgres");
88:     public static final String DB_URL = "jdbc:postgresql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
89:     public static final String DB_DRIVER = "org.postgresql.Driver";
```
* **Líneas 82-89:** Configuración de la base de datos PostgreSQL local (`crm_educativo`), con driver JDBC oficial `org.postgresql.Driver`.

---

## 4. Clase `config/DatabaseConnection.java`

**Ubicación:** `src/main/java/com/tecnoweb/grupo05sc/config/DatabaseConnection.java`  
**Responsabilidad:** Administrar la creación de conexiones JDBC y garantizar el cierre seguro de recursos (`Connection`, `PreparedStatement`, `ResultSet`), siguiendo la estricta doctrina de la materia de evitar fugas de descriptores de conexión.

### Código y Explicación Línea por Línea:

```java
13: public final class DatabaseConnection {
15:     static {
16:         try {
17:             Class.forName(Config.DB_DRIVER);
18:         } catch (ClassNotFoundException e) {
19:             System.err.println("[ERROR CRÍTICO] Driver JDBC de PostgreSQL no encontrado: " + e.getMessage());
20:         }
21:     }
```
* **Líneas 15-21:** **Bloque estático de inicialización (`static initializer`)**: Se ejecuta exactamente una sola vez cuando la JVM carga la clase en memoria. `Class.forName("org.postgresql.Driver")` registra dinámicamente el driver JDBC de PostgreSQL en el `DriverManager` de Java.

```java
28:     public static Connection getConnection() throws SQLException {
29:         return DriverManager.getConnection(Config.DB_URL, Config.DB_USER, Config.DB_PASS);
30:     }
```
* **Líneas 28-30:** Abre un socket TCP hacia el puerto 5432 de PostgreSQL y devuelve una conexión activa lista para ejecutar transacciones.

```java
35:     public static void close(ResultSet rs) {
36:         if (rs != null) {
37:             try {
38:                 rs.close();
39:             } catch (SQLException ignored) {}
40:         }
41:     }
```
* **Líneas 35-41:** Cierra el conjunto de resultados (`ResultSet`). Si es nulo o si ocurre un error al cerrar, se ignora silenciosamente para no tapar una excepción de negocio previa dentro del bloque `finally`.
* **Líneas 45-65 (`close(PreparedStatement)` y `close(Connection)`):** Aplican la misma lógica de cierre seguro para las sentencias preparadas y las conexiones de base de datos.
* **Líneas 70-76 (`rollback(Connection)`):** Si una transacción multiconsulta falla a medio camino, revierte todos los cambios pendientes devolviendo la base de datos a su estado íntegro original.

---

## 5. Clase `mail/MailMessage.java`

**Ubicación:** `src/main/java/com/tecnoweb/grupo05sc/mail/MailMessage.java`  
**Responsabilidad:** Patrón **DTO (Data Transfer Object)**. Es una estructura inmutable que contiene los datos limpios de un correo recibido por POP3:
* `messageNumber`: Índice del correo en el servidor (1, 2, 3...).
* `uid`: Identificador único del mensaje.
* `from`: Dirección de correo limpia del remitente (ej. `nataly.vane.mm@gmail.com`).
* `rawFrom`: Encabezado original completo (ej. `Nataly Montes <nataly.vane.mm@gmail.com>`).
* `subject`: Asunto del correo (donde viaja el comando).
* `body`: Cuerpo del correo en texto plano o HTML.
* `date`: Fecha de recepción del mensaje.

---

## 6. Clase `mail/POP3Listener.java`

**Ubicación:** `src/main/java/com/tecnoweb/grupo05sc/mail/POP3Listener.java`  
**Responsabilidad:** Conectar al servidor mediante **Sockets TCP puros** en el puerto 110, implementar el protocolo **POP3 (RFC 1939)**, descargar mensajes, des-escapar líneas con Byte-stuffing y marcarlos para borrado con `DELE`.

### Diálogo Cliente-Servidor POP3 en Consola:

```java
32:             socket = new Socket(Config.MAIL_HOST, Config.POP3_PORT);
33:             socket.setSoTimeout(Config.SOCKET_TIMEOUT_MS);
35:             in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
36:             out = new DataOutputStream(socket.getOutputStream());
```
* **Línea 32:** `new Socket(host, 110)`: Solicita al sistema operativo la creación de un descriptor de socket y ejecuta el *Three-Way Handshake* (SYN, SYN-ACK, ACK) con el servidor de la universidad.
* **Línea 33:** `setSoTimeout(10000)`: Configura el tiempo máximo de espera en operaciones de lectura. Si el servidor se apaga o la red se congela, `in.readLine()` no se queda bloqueado indefinidamente.
* **Líneas 35-36:**
  * `in`: Flujo de lectura de caracteres (`BufferedReader`) que recibe la respuesta del servidor en codificación estándar de red `ISO-8859-1`.
  * `out`: Flujo de escritura de bytes (`DataOutputStream`) para enviar comandos POP3.

```java
39:             String banner = in.readLine();
40:             System.out.println("S : " + banner);
41:             if (banner == null || !banner.startsWith("+OK")) {
42:                 throw new IOException("Servidor POP3 rechazó la conexión: " + banner);
43:             }
```
* **Líneas 39-43:** Espera el saludo del servidor. En POP3, toda respuesta afirmativa comienza obligatoriamente con `+OK` (ej. `+OK POP3 Server Ready`).
* **`System.out.println("S : " + banner)`**: Imprime lo que dice el servidor, con el prefijo `S :` tal como solicita el Ing. Evans en sus clases.

```java
46:             String cmdUser = "USER " + Config.POP3_USER;
47:             System.out.println("C: " + cmdUser);
48:             out.writeBytes(cmdUser + "\r\n");
49:             out.flush();
50:             String respUser = in.readLine();
51:             System.out.println("S : " + respUser);
```
* **Líneas 46-51:** Envía el comando `USER grupo05sc`.
  * `out.writeBytes(... + "\r\n")`: El estándar de red RFC 1939 exige que cada comando termine en un retorno de carro (`\r` = CR = ASCII 13) y un salto de línea (`\n` = LF = ASCII 10).
  * `out.flush()`: Fuerza al búfer del socket a vaciarse inmediatamente a través de la tarjeta de red, sin esperar a llenarse en memoria local.
  * Lee la respuesta del servidor (`+OK User accepted`).

```java
57:             String cmdPass = "PASS " + Config.POP3_PASS;
58:             System.out.println("C: " + cmdPass);
59:             out.writeBytes(cmdPass + "\r\n");
60:             out.flush();
61:             String respPass = in.readLine();
62:             System.out.println("S : " + respPass);
```
* **Líneas 57-62:** Envía la contraseña con `PASS grup005grup005*` y verifica el `+OK Pass accepted`.

```java
68:             System.out.println("C: STAT");
69:             out.writeBytes("STAT\r\n");
70:             out.flush();
71:             String respStat = in.readLine();
72:             System.out.println("S : " + respStat);
...
82:             int totalMensajes = Integer.parseInt(partesStat[1]);
```
* **Líneas 68-82:** Comando `STAT`: Solicita el estado del buzón. El servidor responde con `+OK <total_mensajes> <tamaño_en_bytes>` (ejemplo: `+OK 2 3420`).
* Si `totalMensajes == 0`, envía `QUIT` y se desconecta de inmediato.

```java
96:             for (int i = 1; i <= totalMensajes; i++) {
97:                 String cmdRetr = "RETR " + i;
98:                 System.out.println("C: " + cmdRetr);
99:                 out.writeBytes(cmdRetr + "\r\n");
100:                out.flush();
...
110:                String contenidoCrudo = readMultilinePOP(in);
113:                MailMessage msg = parseRawEmail(i, String.valueOf(i), contenidoCrudo);
```
* **Líneas 96-113:** Itera por cada mensaje en el buzón enviando `RETR i` (Retrieve).
* El servidor responde con `+OK <tamaño> octets` y a continuación transmite el contenido multilínea del correo electrónico.

```java
155:     private static String readMultilinePOP(BufferedReader in) throws IOException {
156:         StringBuilder sb = new StringBuilder();
157:         while (true) {
158:             String line = in.readLine();
159:             if (line == null) {
160:                 throw new IOException("El servidor cerró abruptamente la conexión POP3.");
161:             }
162:             if (line.equals(".")) {
163:                 break; // Fin del mensaje
164:             }
165:             // Byte-stuffing: si la línea comienza con un punto, se retira el primer punto
166:             if (line.length() > 0 && line.charAt(0) == '.') {
167:                 line = line.substring(1);
168:             }
169:             sb.append(line).append("\n");
170:         }
171:         return sb.toString();
172:     }
```
* **Líneas 155-172:** **Lectura multilínea según el RFC 1939**:
  * Un correo electrónico termina exclusivamente cuando el servidor envía una línea que contiene un único punto: `.\r\n`.
  * **Técnica de Byte-Stuffing (Relleno de Bytes):** Si el cuerpo del correo original contenía una línea que comenzaba con un punto (ej. `...hola`), el servidor de correo emisor le añadió otro punto al principio (`....hola`) para que no se confunda con el terminador de mensaje. Al leerlo, nosotros debemos retirar ese punto extra (`line.substring(1)`).

```java
124:                 // 6. DELE (Marcar como eliminado para no reprocesarlo en la siguiente ronda)
125:                 String cmdDele = "DELE " + i;
126:                 System.out.println("C: " + cmdDele);
127:                 out.writeBytes(cmdDele + "\r\n");
128:                 out.flush();
129:                 String respDele = in.readLine();
130:                 System.out.println("S : " + respDele);
```
* **Líneas 124-130:** Comando `DELE i`: Marca el mensaje número `i` para ser eliminado del servidor. **¡Atención!** El mensaje no se borra físicamente en este instante; solo se marca en memoria del servidor.

```java
134:             System.out.println("C: QUIT");
135:             out.writeBytes("QUIT\r\n");
136:             out.flush();
137:             String respQuit = in.readLine();
138:             System.out.println("S : " + respQuit);
```
* **Líneas 134-138:** Comando `QUIT`: Este comando es el que **confirma las eliminaciones (commit del buzón)** y cierra formalmente la sesión POP3. Si el socket se cerrara sin enviar `QUIT`, los correos marcados con `DELE` **no** se borrarían y volverían a procesarse en el siguiente ciclo.

```java
143:         } finally {
144:             // Doctrina de la materia: Cierre garantizado en orden inverso en bloque finally
145:             try { if (out != null) out.close(); } catch (Exception ignored) {}
146:             try { if (in != null) in.close(); } catch (Exception ignored) {}
147:             try { if (socket != null && !socket.isClosed()) socket.close(); } catch (Exception ignored) {}
148:         }
```
* **Líneas 143-148:** Cierre garantizado de flujos y socket en orden inverso en bloque `finally`.

---

## 7. Clase `mail/SMTPSender.java`

**Ubicación:** `src/main/java/com/tecnoweb/grupo05sc/mail/SMTPSender.java`  
**Responsabilidad:** Conectar por **Sockets TCP puros al puerto 25** del servidor, dialogar mediante el protocolo **SMTP (RFC 5321)**, construir las cabeceras MIME y transmitir la respuesta en formato **HTML con codificación UTF-8**.

### Paso a Paso del Diálogo SMTP:

```java
35:             socket = new Socket(Config.MAIL_HOST, Config.SMTP_PORT);
36:             socket.setSoTimeout(Config.SOCKET_TIMEOUT_MS);
38:             in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
39:             out = new DataOutputStream(socket.getOutputStream());
```
* **Líneas 35-39:** Abre la conexión hacia el puerto 25 de `mail.tecnoweb.org.bo`.

```java
42:             String banner = in.readLine();
43:             System.out.println("S : " + banner);
44:             if (banner == null || !banner.startsWith("220")) {
45:                 throw new IOException("Servidor SMTP no disponible o rechazó la conexión: " + banner);
46:             }
```
* **Líneas 42-46:** Espera el banner del servidor SMTP. En SMTP, las respuestas afirmativas iniciales son códigos numéricos de la serie **200** (el saludo siempre empieza con **220**).

```java
49:             String cmdHelo = "HELO " + Config.MAIL_HOST;
50:             System.out.println("C: " + cmdHelo);
51:             out.writeBytes(cmdHelo + "\r\n");
52:             out.flush();
53:             String respHelo = in.readLine();
54:             System.out.println("S : " + respHelo);
55:             if (respHelo == null || !respHelo.startsWith("250")) {
56:                 throw new IOException("Error en comando HELO: " + respHelo);
57:             }
```
* **Líneas 49-57:** Comando `HELO mail.tecnoweb.org.bo`. Saludo de cortesía donde el cliente se presenta ante el servidor SMTP. El servidor responde con código **250** (OK).

```java
60:             String cmdMail = "MAIL FROM: <" + Config.MAIL_EMISOR + ">";
61:             System.out.println("C: " + cmdMail);
62:             out.writeBytes(cmdMail + "\r\n");
63:             out.flush();
64:             String respMail = in.readLine();
65:             System.out.println("S : " + respMail);
66:             if (respMail == null || !respMail.startsWith("250")) {
67:                 throw new IOException("Error en comando MAIL FROM: " + respMail);
68:             }
```
* **Líneas 60-68:** Comando `MAIL FROM: <grupo05sc@tecnoweb.org.bo>`. Especifica la dirección de origen del remitente del mensaje. El servidor valida la cuenta y responde `250 2.1.0 Ok`.

```java
71:             String cmdRcpt = "RCPT TO: <" + toEmail.trim() + ">";
72:             System.out.println("C: " + cmdRcpt);
73:             out.writeBytes(cmdRcpt + "\r\n");
74:             out.flush();
75:             String respRcpt = in.readLine();
76:             System.out.println("S : " + respRcpt);
77:             if (respRcpt == null || !respRcpt.startsWith("250")) {
78:                 throw new IOException("Error en comando RCPT TO para <" + toEmail + ">: " + respRcpt);
79:             }
```
* **Líneas 71-79:** Comando `RCPT TO: <destinatario>`. Especifica la dirección del receptor del correo (ej. tu Gmail o el del docente). El servidor verifica si puede entregar en ese buzón y responde `250 2.1.5 Ok`.

```java
82:             System.out.println("C: DATA");
83:             out.writeBytes("DATA\r\n");
84:             out.flush();
85:             String respData = in.readLine();
86:             System.out.println("S : " + respData);
87:             if (respData == null || !respData.startsWith("354")) {
88:                 throw new IOException("Error en comando DATA: " + respData);
89:             }
```
* **Líneas 82-89:** Comando `DATA`. Indica que estamos listos para transmitir las cabeceras y el cuerpo del correo. El servidor responde con el código **354** (ej. `354 End data with <CR><LF>.<CR><LF>`), indicando que queda en modo recepción de texto hasta encontrar un punto solitario.

```java
93:             String encodedSubject = "=?UTF-8?B?" + Base64.getEncoder().encodeToString(subject.getBytes(StandardCharsets.UTF_8)) + "?=";
95:             StringBuilder rfcMessage = new StringBuilder();
96:             rfcMessage.append("From: ").append(Config.MAIL_EMISOR).append("\r\n");
97:             rfcMessage.append("To: <").append(toEmail.trim()).append(">\r\n");
98:             rfcMessage.append("Subject: ").append(encodedSubject).append("\r\n");
99:             rfcMessage.append("MIME-Version: 1.0\r\n");
100:            rfcMessage.append("Content-Type: text/html; charset=UTF-8\r\n");
101:            rfcMessage.append("Content-Transfer-Encoding: 8bit\r\n");
102:            rfcMessage.append("\r\n"); // Línea en blanco que separa cabeceras del cuerpo (OBLIGATORIA)
103:            rfcMessage.append(htmlContent).append("\r\n");
104:            rfcMessage.append(".\r\n"); // Fin del bloque DATA: <CRLF>.<CRLF>
```
* **Línea 93: Codificación de Asunto RFC 2047**:
  * Si el asunto lleva tildes (ej. `Éxito`, `Gestión`), en los sockets crudos se arruinarían porque el protocolo de red original era solo ASCII de 7 bits.
  * Formato estándar RFC 2047: `=?UTF-8?B?<bytes_en_base64>?=`. Usando la clase estándar `java.util.Base64`, el asunto se codifica limpiamente sin librerías externas.
* **Líneas 96-101: Cabeceras MIME (RFC 5322)**:
  * `MIME-Version: 1.0`: Declara que el mensaje soporta extensiones multimedia.
  * `Content-Type: text/html; charset=UTF-8`: Le ordena al cliente de correo (Gmail, Outlook, Thunderbird) que interprete el contenido como una página web HTML con colores, tablas y estilos.
* **Línea 102: Línea en blanco obligatoria `\r\n`**:
  * **¡Punto crítico de examen oral!** Según la especificación del correo electrónico, el fin de los encabezados y el inicio del cuerpo del mensaje está delimitado única y exclusivamente por **una línea vacía (`\r\n`)**. Si se omite esta línea vacía, todo tu código HTML será interpretado como encabezados y el correo llegará en blanco.
* **Línea 104:** Finaliza la transmisión con `.\r\n`.

```java
110:            out.write(rfcMessage.toString().getBytes(StandardCharsets.UTF_8));
111:            out.flush();
113:            String respEnvio = in.readLine();
114:            System.out.println("S : " + respEnvio);
...
120:            System.out.println("C: QUIT");
121:            out.writeBytes("QUIT\r\n");
122:            out.flush();
```
* **Líneas 110-122:** Envía los bytes codificados en UTF-8, lee la confirmación `250 2.0.0 Ok: queued as ...`, envía `QUIT` para cerrar la sesión SMTP y entra al bloque `finally` para liberar los descriptores del socket.

---

## 8. Clase `command/CommandParser.java`

**Ubicación:** `src/main/java/com/tecnoweb/grupo05sc/command/CommandParser.java`  
**Responsabilidad:** Analizador léxico y sintáctico que toma la cadena cruda del Asunto y extrae el nombre del comando y sus parámetros.

### Expresiones Regulares Utilizadas:

```java
20: private static final Pattern PATTERN_WITH_BRACKETS = Pattern.compile("^\\s*([A-Za-z0-9_]+)\\s*\\[(.*)\\]\\s*$", Pattern.DOTALL);
21: private static final Pattern PATTERN_WITHOUT_BRACKETS = Pattern.compile("^\\s*([A-Za-z0-9_]+)\\s*$", Pattern.DOTALL);
```
* **`^\\s*([A-Za-z0-9_]+)\\s*\\[(.*)\\]\\s*$`**:
  * `^\\s*`: Ignora cualquier espacio antes del comando.
  * `([A-Za-z0-9_]+)`: **Grupo 1**: Captura el nombre del comando (letras, números y guiones bajos).
  * `\\[(.*)\\]`: Captura entre los corchetes `[` y `]` el contenido interno como **Grupo 2** (los parámetros).
  * `Pattern.DOTALL`: Permite que si un parámetro tiene saltos de línea, no se corte la lectura.
* **`PATTERN_WITHOUT_BRACKETS`**:
  * Permite que comandos simples sin parámetros puedan enviarse sin corchetes (ej. escribir `HELP` en vez de `HELP[]`).

### Algoritmo de Extracción de Parámetros (`parseParameters`):
* Itera carácter por carácter el interior de los corchetes.
* Si encuentra una comilla doble `"`, entra en modo cadena entrecomillada hasta hallar la comilla de cierre `"`.
* Soporta caracteres de escape `\"` (si dentro de un texto se necesita poner comillas).
* Si el parámetro no tiene comillas (ej. `*`, `123`, `true`), lee hasta la siguiente coma `,`.
* Si una comilla no se cierra, lanza `CommandException` indicando el error exacto al usuario.

---

## 9. Clases del Paquete `command` (Soporte)

### `ParsedCommand.java`:
* Contiene `String name` y `List<String> params`.
* Proporciona métodos de conversión segura:
  * `getIntParam(index, "id_usuario")`: Convierte el parámetro a entero y, si viene texto no numérico, lanza un error semántico amigable.
  * `getDoubleParam(index, "monto")`: Convierte a double admitiendo tanto punto como coma decimal (`150.50` o `150,50`).

### `CommandResult.java`:
* Objeto empaquetador con 3 campos:
  * `String subject`: El asunto con el que saldrá el correo de vuelta.
  * `String htmlBody`: La plantilla HTML generada.
  * `boolean success`: Si la operación fue exitosa (`true`) o de error (`false`).

### `CommandException.java`:
* Subclase de `Exception`. Permite separar errores técnicos del sistema de **errores de usuario** (parámetros faltantes, rol insuficiente, duplicidad de datos).
* Incluye el campo `helpTip` (sugerencia de sintaxis para que el usuario aprenda cómo enviar bien el comando).

---

## 10. Clases del Paquete `security`

### `UsuarioAuth.java`:
* DTO con los datos del usuario que mandó el correo:
  * `idUsuario`, `nombreCompleto`, `correo`, `idRol`, `rolNombre`, `activo`, `participaRoundRobin`.
  * Métodos de consulta de privilegios: `esAdmin()`, `esCoordinador()`, `esVendedor()`, `isActivo()`.

### `SecurityContext.java`:
* **`obtenerUsuario(fromEmail)`**: Ejecuta un `SELECT` parametrizado con `PreparedStatement` buscando el correo en la tabla `usuarios` uniéndola con `roles`:
  ```sql
  SELECT u.id_usuario, u.nombre_completo, u.correo, u.id_rol, r.nombre AS rol_nombre,
         u.activo, u.participa_round_robin
  FROM usuarios u
  JOIN roles r ON u.id_rol = r.id_rol
  WHERE LOWER(u.correo) = LOWER(?)
  ```
* **`validarAdmin(auth)`**:
  1. Si `auth == null`, lanza `CommandException`: "Acceso denegado: El correo remitente no corresponde a un usuario registrado en el sistema".
  2. Si `!auth.isActivo()`, lanza: "Acceso denegado: Su cuenta de usuario se encuentra desactivada".
  3. Si `!auth.esAdmin()`, lanza: "Acceso denegado: Esta acción es exclusiva para usuarios con rol 'Administrador'. Su rol actual es 'Vendedor'".

---

## 11. Clase `view/HtmlTemplate.java`

**Ubicación:** `src/main/java/com/tecnoweb/grupo05sc/view/HtmlTemplate.java`  
**Responsabilidad:** Generar el diseño visual corporativo de los correos salientes mediante HTML y CSS en línea (*inline styles*), optimizado para renderizarse impecablemente en clientes de correo como Gmail y Outlook.

### Métodos Principales:
* **`wrapContainer(title, badgeText, contentHtml)`**:
  * Envuelve el contenido en una tarjeta de `max-width: 850px`.
  * Cabecera institucional azul (`#3d5598`) con línea de acento roja (`#da3038`).
  * Título del caso de uso e insignia de estado (`ÉXITO`, `ALERTA`, `ADMIN`).
  * Pie de página oficial: `INF513 Tecnología Web • Grupo 05 SC • Universidad Autónoma Gabriel René Moreno`.
* **`errorCard(errorMessage, helpTip)`**:
  * Tarjeta con borde rojo (`#dc3545`), fondo claro y sugerencia de corrección con emoji 💡.
* **`successCard(title, message, detailsHtml)`**:
  * Tarjeta con borde verde (`#28a745`) y detalle de los registros afectados.
* **`table(headers, rows)`**:
  * Genera una tabla HTML responsiva con cabecera gris-azulada (`#ebf0f9`) y filas alternadas en blanco para facilitar la lectura de reportes y listados.
* **`badgeEtapa(idEtapa, nombreEtapa)`**:
  * Insignias con los colores oficiales de las 6 etapas del Kanban (Nuevo = Azul, Contacto = Cyan, Seguimiento = Amarillo, Promesa = Púrpura, Convertido = Verde, Perdido = Rojo).

---

## 12. Clase `dao/UsuarioDao.java`

**Ubicación:** `src/main/java/com/tecnoweb/grupo05sc/dao/UsuarioDao.java`  
**Responsabilidad:** Patrón **DAO (Data Access Object)**. Aísla completamente la persistencia en PostgreSQL de la lógica de negocio. Toda consulta utiliza `PreparedStatement` con comodines `?` para **inmunidad total ante Inyección SQL**.

### Métodos Destacados:
* **`insertar(nombre, correo, passwordHash, idRol)`**:
  * Ejecuta `INSERT INTO usuarios ... VALUES (?, ?, ?, ?, true, ?, NOW(), NOW())`.
  * Utiliza `Statement.RETURN_GENERATED_KEYS` para recuperar inmediatamente el ID autogenerado por PostgreSQL (`id_usuario`) sin necesidad de hacer una segunda consulta.
* **`existeCorreo(correo)`**:
  * Consulta de existencia ultrarrápida: `SELECT 1 FROM usuarios WHERE LOWER(correo) = LOWER(?) LIMIT 1`.
* **`toggleActivo(idUsuario)`**:
  * `UPDATE usuarios SET activo = NOT activo, updated_at = NOW() WHERE id_usuario = ?`.
  * Invierte el booleano atómicamente a nivel de motor de base de datos.
* **`toggleRoundRobin(idUsuario)`**:
  * Alterna la participación del vendedor en la asignación rotativa de leads.

---

## 13. Clase `service/UsuarioService.java`

**Ubicación:** `src/main/java/com/tecnoweb/grupo05sc/service/UsuarioService.java`  
**Responsabilidad:** Capa de Negocio del **Caso de Uso 1 (CU1: Gestión de Usuarios)**, asignado a **Nataly**.

### Reglas de Negocio Implementadas y Explicadas:

#### 1. Protección contra Autobloqueo del Administrador (Regla de Oro de Seguridad):
```java
258:         // REGLA DE SEGURIDAD VITAL DEL DOCENTE:
259:         if (idUsuario == auth.getIdUsuario()) {
260:             throw new CommandException(
261:                 "Operación denegada por seguridad: El Administrador no puede desactivar ni bloquear su propia cuenta.",
262:                 "Para dar de baja esta cuenta administrativa, otro usuario con rol Administrador debe ejecutar la acción."
263:             );
264:         }
```
* Si el Administrador autenticado intenta desactivar su propio ID, el sistema aborta de inmediato la operación. Esto previene que el sistema quede "huérfano" sin ningún administrador activo capaz de operar el CRM.

#### 2. Criptografía Nativa con SHA-256:
```java
341:     public static String hashPassword(String password) {
342:         try {
343:             MessageDigest md = MessageDigest.getInstance("SHA-256");
344:             byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
345:             StringBuilder sb = new StringBuilder();
346:             for (byte b : hash) {
347:                 sb.append(String.format("%02x", b));
348:             }
349:             return sb.toString();
```
* Las contraseñas nunca se guardan en texto plano en la base de datos. Se utiliza el algoritmo estándar criptográfico unidireccional **SHA-256** mediante la clase nativa `java.security.MessageDigest`.

#### 3. Validación de Roles y Round-Robin:
* Al crear un usuario, si su rol es `Vendedor` (rol 3), `participa_round_robin` se inicializa automáticamente en `true`.
* Si se intenta ejecutar `TOGGLEROUNDROBIN` sobre un usuario que no es vendedor (ej. un Coordinador o un Administrador), se rechaza con un mensaje de negocio indicando que la rotación de leads solo aplica a la fuerza de ventas.

---

## 14. Clase `command/CommandDispatcher.java`

**Ubicación:** `src/main/java/com/tecnoweb/grupo05sc/command/CommandDispatcher.java`  
**Responsabilidad:** El "cerebro" enrutador. Recibe el correo y el comando del `Main`, delega al servicio correspondiente y captura cualquier excepción para transformarla en una respuesta HTML agradable.

### Estructura de Control:
* **`HELP[]`**: Genera dinámicamente el catálogo HTML de comandos disponibles en el CRM.
* **`QUIENSOY[]`**: Devuelve la ficha del remitente (nombre, correo, rol y estado de cuenta).
* **Bloque `switch` por Casos de Uso**:
  * `LISTARUSUARIOS`, `BUSCARUSUARIO`, `CREARUSUARIO`, `MODIFICARUSUARIO`, `TOGGLEUSUARIO`, `TOGGLEROUNDROBIN` ➔ `UsuarioService` (CU1 - Nataly).
  * `REGISTRARPROSPECTO`, `VINCULARINTERES`, etc. ➔ `LeadService` (CU2 - Jael).
  * `BOLSACOMUN`, `EJECUTARROUNDROBIN`, `TRANSFERIRLEAD` ➔ `AsignacionService` (CU3 - Nicol).
  * `VERKANBAN`, `MOVERETAPA` ➔ `PipelineService` (CU4 - Nicol).
  * `LISTARMOTIVOSPERDIDA`, `MARCARPERDIDO` ➔ `PerdidaService` (CU5 - Nicol).
  * `PAGOSPENDIENTES`, `APROBARPAGO`, `CONVERTIRAMATRICULA` ➔ `ConversionMatriculaService` (CU6 - Nataly).
  * `VERBITACORA`, `CREARRECORDATORIO` ➔ `BitacoraService` (CU7 - Nataly).
  * `REPORTEEMBUDO`, `REPORTECONVERSION` ➔ `ReporteService` (CU8 - Jael).

---

## 15. Banco de Preguntas Clave para la Defensa Oral con el Ing. Evans

### ❓ Pregunta 1: "¿Por qué no utilizaron JavaMail (`javax.mail`) y prefirieron sockets puros?"
> **Respuesta de 100 puntos:**  
> *"Ingeniero, utilizamos sockets TCP puros (`java.net.Socket`) en los puertos 110 (POP3) y 25 (SMTP) para comprender y demostrar el funcionamiento real de los protocolos de la capa de aplicación sobre la pila TCP/IP, tal como vimos en la teoría de la materia. Implementamos a bajo nivel los comandos RFC 1939 (`USER`, `PASS`, `STAT`, `RETR`, `DELE`, `QUIT`) y RFC 5321 (`HELO`, `MAIL FROM`, `RCPT TO`, `DATA`, `QUIT`), además de controlar el des-escape de puntos con Byte-stuffing y la separación obligatoria de cabeceras MIME mediante la línea en blanco `\r\n`."*

### ❓ Pregunta 2: "¿Cómo evitan que el sistema entre en un bucle infinito de correos respondiéndose a sí mismo?"
> **Respuesta:**  
> *"Lo prevenimos con dos mecanismos: primero, todas nuestras respuestas llevan el prefijo obligatorio `RE: [CRM-EDUCATIVO]`. En `POP3Listener.java`, antes de procesar un mensaje verificamos si el Asunto inicia con dicho prefijo; si es así, el correo se descarta. Segundo, cada correo leído se marca de inmediato para eliminación mediante `DELE i` y se confirma con `QUIT`, de modo que ningún mensaje pueda ser leído dos veces."*

### ❓ Pregunta 3: "¿Cómo garantizan que un correo falso no ejecute acciones de Administrador si cualquiera puede falsificar un correo?"
> **Respuesta:**  
> *"Implementamos Control de Acceso Basado en Roles (RBAC) en la clase `SecurityContext.java`. Ante cada comando que ingresa, el sistema extrae el correo remitente (`From:`) y realiza una consulta parametrizada contra la tabla `usuarios` en PostgreSQL. Si el remitente no existe en la base de datos o si su estado es `activo = false`, se bloquea de inmediato. Además, comandos administrativos como `CREARUSUARIO` o `TOGGLEUSUARIO` exigen estrictamente que el usuario tenga asociado el rol con `id_rol = 1` ('Administrador')."*

### ❓ Pregunta 4: "¿Por qué en `SMTPSender` codifican el Subject con `=?UTF-8?B?...?=`?"
> **Respuesta:**  
> *"Porque el protocolo SMTP original según el RFC 5321 solo admite caracteres US-ASCII de 7 bits en las cabeceras. Si enviamos tildes, caracteres como la 'ñ' o emojis directamente, los servidores de correo intermedios corrompen el asunto. Mediante la especificación RFC 2047, codificamos el texto en Base64 bajo el estándar UTF-8 (`=?UTF-8?B?...?=`), lo que garantiza que clientes modernos como Gmail rendericen los acentos perfectamente sin requerir librerías externas."*

### ❓ Pregunta 5: "¿Qué sucede si se cae la conexión de red o la base de datos mientras el servidor está corriendo?"
> **Respuesta:**  
> *"El sistema está diseñado con alta tolerancia a fallos. Los sockets tienen un timeout configurado de 10 segundos (`socket.setSoTimeout(10000)`), lo que impide que los hilos se queden esperando indefinidamente. Además, el bucle principal en `Main.java` encapsula el ciclo en un bloque `try-catch`, registrando el error en terminal y esperando 3 segundos con `Thread.sleep()` antes de reintentar la conexión en el siguiente ciclo sin que el proceso colapse."*

---

> 🎯 **Con esta guía dominas el 100% del funcionamiento interno del código, su arquitectura de red y las reglas de negocio.**
> **Tus compañeras Nicol y Jael pueden tomar las clases y métodos aquí documentados como el mapa exacto para diagramar las clases y secuencias del PUDS.**
