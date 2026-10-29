# 📖 Guía Paso a Paso, Explicación Detallada y Manual de Pruebas
## 1er Parcial: Sistema vía Correo Electrónico — Grupo 05 SC (CRM Educativo)

> **Materia:** Tecnología Web (INF513 - SC)  
> **Docente:** Msc. Ing. Evans Balcázar Veizaga  
> **Estudiante:** Nataly Vanessa Martinez Martinez (Dev A)  
> **Ubicación de tu proyecto aislado:** `INF513_P1_05sc/`

---

## 🎯 1. La Gran Duda: ¿De dónde salió la Base de Datos? ¿Ya está creada y poblada?

### La realidad técnica en tu máquina:
1. **PostgreSQL ya estaba corriendo en tu computadora:**  
   En tu sistema Linux, el motor de PostgreSQL (puerto `5432`) está activo y listo para conexiones.
2. **La base de datos `crm_educativo` YA EXISTÍA:**  
   Cuando ustedes estuvieron trabajando en el 2do parcial con Laravel 12 + Inertia + React, corrieron las migraciones y seeders (`php artisan migrate:fresh --seed`). Esa base de datos **se llama `crm_educativo`** y ya contenía las **25 tablas oficiales del sistema**.
3. **Ya tenía datos reales poblados:**  
   Al consultar la tabla `usuarios` en tu PostgreSQL local, encontramos a todo tu equipo ya registrado:
   * `admin@crm.bo` ➔ Nataly Valenzuela (Admin) — Rol 1 (`Administrador`)
   * `coordinador@crm.bo` ➔ Jael Sandoval (Coordinadora) — Rol 2 (`Coordinador`)
   * `vendedor1@crm.bo` ➔ Nicol Flores (Vendedora) — Rol 3 (`Vendedor`)
   * `vendedor2@crm.bo` ➔ Carlos Mendoza (Vendedor) — Rol 3 (`Vendedor`)
   * Y la tabla `leads` ya tenía prospectos cargados (María Laura Rojas, Roberto Gómez Bolaños, etc.).
4. **La tabla extra que agregamos (`persona`):**  
   En la orden que subió el Ing. Evans al Moodle, él puso como ejemplos: `LISPER["*"]` e `INSPER["4715292", ...]`. Esa tabla `persona` era la del práctico de la U3 (`db_agenda`). Para que el día de tu examen, si el docente se le ocurre probar con esos mismos comandos de su orden, tu sistema **no falle**, creamos también la tabla `persona` dentro de tu base `crm_educativo` local con registros de ejemplo.

---

## 🔐 2. El Archivo `.env` y la Configuración de Conexión

Siguiendo tu petición, creamos el archivo de configuración por entorno en:  
📁 [**`INF513_P1_05sc/.env`**](file:///home/nataly/Projects/TECNO/crm_correo/INF513_P1_05sc/.env)

Contiene exactamente lo siguiente:

```properties
# =========================================================================
# CONFIGURACIÓN LOCAL (PostgreSQL en tu máquina para pruebas y desarrollo)
# =========================================================================
DB_HOST=localhost
DB_PORT=5432
DB_NAME=crm_educativo
DB_USER=postgres
DB_PASS=postgres

# =========================================================================
# CONFIGURACIÓN DEL SERVIDOR DE CORREO (UAGRM - FICCT)
# =========================================================================
MAIL_HOST=mail.tecnoweb.org.bo
POP3_PORT=110
SMTP_PORT=25
POP3_USER=grupo05sc
POP3_PASS=grup005grup005*
MAIL_EMISOR=grupo05sc@tecnoweb.org.bo

# =========================================================================
# CONFIGURACIÓN SERVIDOR FACULTAD (Para la defensa oral)
# =========================================================================
# DB_HOST=db.tecnoweb.org.bo
# DB_PORT=5432
# DB_NAME=db_grupo05sc
# DB_USER=grupo05sc
# DB_PASS=grup005grup005*
```

### ¿Cómo lee Java este archivo sin librerías externas?
El Ing. Evans prohíbe librerías raras. Por eso, en [`Config.java`](file:///home/nataly/Projects/TECNO/crm_correo/INF513_P1_05sc/src/main/java/com/tecnoweb/grupo05sc/config/Config.java) programamos un lector nativo ultra ligero (con `BufferedReader` estándar de Java) que busca el archivo `.env`, extrae las claves y valores, y si no lo encuentra, utiliza los valores por defecto locales.  
👉 **Ventaja para ti:** Si quieres cambiar la contraseña o la base de datos, solo editas `.env` sin tener que recompilar código.

---

## 📁 3. Estructura de tu Carpeta Aislada: `INF513_P1_05sc/`

Para no mezclar tu trabajo con las carpetas de los otros grupos, **todo lo tuyo está concentrado y aislado en `INF513_P1_05sc/`**:

```
INF513_P1_05sc/
├── .env                             # Credenciales locales y de red editables
├── .gitignore                       # Ignora bin/, .class y temporales de git
├── pom.xml                          # Para compilar en Maven, VS Code o IntelliJ
├── lib/
│   └── postgresql.jar               # Driver JDBC oficial descargado para Java 17
├── bin/                             # Carpeta donde se guardan los .class compilados
└── src/
    ├── main/java/com/tecnoweb/grupo05sc/
    │   ├── Main.java                # Demonio principal que corre en bucle
    │   ├── config/
    │   │   ├── Config.java          # Lector de .env y constantes del sistema
    │   │   └── DatabaseConnection.java # Conexión a PostgreSQL con PreparedStatement y finally
    │   ├── mail/
    │   │   ├── MailMessage.java     # Guarda From, Subject, Body, Fecha
    │   │   ├── POP3Listener.java    # Socket TCP puerto 110 (USER, PASS, STAT, RETR, DELE, QUIT)
    │   │   └── SMTPSender.java      # Socket TCP puerto 25 (HELO, MAIL FROM, RCPT TO, DATA, QUIT)
    │   ├── command/
    │   │   ├── CommandParser.java   # Extrae COMANDO["param1","param2"] con Regex
    │   │   ├── ParsedCommand.java   # Facilita obtener getParam(0), getIntParam(1), etc.
    │   │   ├── CommandException.java# Errores explicativos con sugerencias (helpTip)
    │   │   ├── CommandResult.java   # Contenedor del asunto y cuerpo HTML resultante
    │   │   └── CommandDispatcher.java # Enruta a LISPER, INSPER, HELP, QUIENSOY y los 8 CU
    │   └── view/
    │       └── HtmlTemplate.java    # Genera tablas HTML corporativas y tarjetas de alerta
    └── test/java/com/tecnoweb/grupo05sc/
        └── TestRunner.java          # Batería de 8 pruebas unitarias automatizadas
```

---

## 🔬 4. ¿Qué hace exactamente cada archivo clave? (Explicación para la Defensa)

### 1. `POP3Listener.java` (Descargar correos a bajo nivel)
* Abre un `Socket` hacia `mail.tecnoweb.org.bo` en el puerto `110`.
* Configura `socket.setSoTimeout(10000)` para que, si se cae el internet, no se cuelgue el hilo.
* Envía `USER grupo05sc` y `PASS grup005grup005*`.
* Envía `STAT` para saber cuántos correos nuevos hay.
* Por cada correo:
  * Envía `RETR i` y lee línea por línea hasta encontrar un punto solitario `.` (RFC 1939).
  * Aplica des-escape de puntos (*Byte-stuffing*).
  * Extrae el Asunto (`Subject:`) y el Remitente (`From:`).
  * Envía `DELE i` para marcarlo como eliminado del servidor y no volverlo a procesar en el siguiente ciclo.
* Finaliza con `QUIT` y cierra todo en el bloque `finally`.
* **Muestra en pantalla cada `C:` (lo que manda tu cliente) y cada `S :` (lo que responde el servidor).**

### 2. `SMTPSender.java` (Enviar respuestas HTML a bajo nivel)
* Abre un `Socket` hacia `mail.tecnoweb.org.bo` en el puerto `25`.
* Lee el saludo `220`.
* Envía `HELO mail.tecnoweb.org.bo` (recibe `250`).
* Envía `MAIL FROM: <grupo05sc@tecnoweb.org.bo>` (recibe `250`).
* Envía `RCPT TO: <correo_del_remitente>` (recibe `250`).
* Envía `DATA` (recibe `354 Enter mail, end with "."`).
* Envía las cabeceras MIME RFC 5322:
  * `Content-Type: text/html; charset=UTF-8`
  * `Subject: =?UTF-8?B?...?=` (codificado en Base64 para que soporte tildes y caracteres especiales sin usar librerías externas).
  * **Línea en blanco obligatoria `\r\n`** que separa cabeceras del cuerpo.
  * El código HTML con la tabla o mensaje de respuesta.
  * Finaliza los datos con `\r\n.\r\n`.
* Envía `QUIT` y cierra streams en `finally`.

### 3. `CommandParser.java` (El Intérprete)
* Recibe el texto del Asunto.
* Usa la expresión regular: `^\s*([A-Za-z0-9_]+)\s*\[(.*)\]\s*$`
* Si mandas `LISPER["*"]`:
  * Nombre: `LISPER`
  * Parámetro 0: `*`
* Si mandas `INSPER["4715292","Juan Carlos",...]`:
  * Extrae cada parámetro entre comillas dobles, respetando espacios internos y comas separadoras.
* Si el asunto viene vacío o mal escrito, lanza un `CommandException` semántico para que el sistema le devuelva un correo con la guía de cómo escribirlo bien.

### 4. `CommandDispatcher.java` (El Router a la Base de Datos)
* Toma el comando y decide qué hacer:
  * Si es `LISPER`: Ejecuta un `SELECT` con `PreparedStatement` sobre la tabla `persona` y devuelve una tabla HTML con todos los datos.
  * Si es `INSPER`: Ejecuta un `INSERT INTO persona (...) VALUES (?, ?, ...)` usando `PreparedStatement` (blindado contra inyección SQL) y devuelve una tarjeta verde de éxito.
  * Si es `HELP`: Devuelve la lista completa de comandos y su sintaxis.
  * Si es `QUIENSOY`: Busca en la tabla `usuarios` quién es el dueño del correo `From:` y le responde con su nombre, rol y estado.
  * Si es `VERKANBAN`: Consulta los leads por cada una de las 6 etapas del embudo comercial en PostgreSQL y arma el tablero visual.

---

## 🚀 5. Manual de Pruebas: Cómo Probar Todo Paso a Paso

### 🧪 Paso 1: Ejecutar la Batería de Pruebas Automatizadas (Offline)
Para verificar que el parser y la base de datos local responden al 100% sin necesidad de enviar correos todavía:

1. Abre tu terminal.
2. Entra a la carpeta de tu proyecto:
   ```bash
   cd /home/nataly/Projects/TECNO/crm_correo/INF513_P1_05sc
   ```
3. Compila el código:
   ```bash
   javac -encoding UTF-8 -cp "lib/postgresql.jar:." -d bin $(find src/main/java -name "*.java") src/test/java/com/tecnoweb/grupo05sc/TestRunner.java
   ```
4. Ejecuta las pruebas:
   ```bash
   java -cp "lib/postgresql.jar:bin" com.tecnoweb.grupo05sc.TestRunner
   ```
   **Resultado esperado en tu pantalla:**
   ```text
   ✔ TEST 1 PASÓ: LISPER["*"] parseado correctamente.
   ✔ TEST 2 PASÓ: INSPER parseado con 7 parámetros y soporte de espacios.
   ✔ TEST 3 PASÓ: Comando 'HELP' sin corchetes parseado como HELP[].
   ✔ TEST 4 PASÓ: Captura error semántico para asunto vacío.
   ✔ TEST 5 PASÓ: LISPER["*"] ejecutado sobre PostgreSQL con tabla HTML generada.
   ✔ TEST 6 PASÓ: INSPER ejecutado exitosamente con PreparedStatement.
   ✔ TEST 7 PASÓ: QUIENSOY autenticó correctamente al Administrador (Nataly).
   ✔ TEST 8 PASÓ: VERKANBAN[] calculó los leads por etapa del embudo.
   RESUMEN: 8 de 8 pruebas pasaron con éxito (100%).
   ```

---

### 📬 Paso 2: Probar el Sistema Real Enviando Correos desde tu Celular / Gmail

Esta es la prueba real de fuego, idéntica a lo que harás en la defensa frente al Ing. Evans Balcázar:

#### A. Iniciar el Servidor de Correos en tu terminal:
```bash
cd /home/nataly/Projects/TECNO/crm_correo/INF513_P1_05sc
java -cp "lib/postgresql.jar:bin" com.tecnoweb.grupo05sc.Main
```
Verás que el demonio arranca y dice:
```text
>>> [INICIO] Verificando conexión a PostgreSQL...
>>> [DB] ¡Conexión exitosa a la base de datos: jdbc:postgresql://localhost:5432/crm_educativo!
>>> [DEMONIO] Servidor de correos iniciado en bucle continuo.
>>> Monitoreando buzón: grupo05sc@tecnoweb.org.bo cada 5 segundos...
```

#### B. Enviar un correo desde tu Gmail:
* **Para:** `grupo05sc@tecnoweb.org.bo`
* **Asunto:** Prueba cualquiera de estos comandos:
  * `HELP[]` ➔ Para que te devuelva la lista completa de comandos y sintaxis.
  * `LISPER["*"]` ➔ Para que te liste en una tabla HTML todas las personas de la BD.
  * `QUIENSOY[]` ➔ Para que verifique tu correo y te diga quién eres en el sistema.
  * `VERKANBAN[]` ➔ Para que te devuelva el resumen de las 6 etapas del embudo con tus leads reales.
  * `LISTARUSUARIOS[]` ➔ Para ver los usuarios del CRM (Nataly Admin, Nicol, Jael).
  * `INSPER["1234567","Carlos","Mendoza","Ingeniero","33221100","77112233","carlos@uagrm.edu.bo"]` ➔ Para insertar una persona nueva.
* **Cuerpo:** Puedes dejarlo en blanco o poner lo que quieras (el sistema lee el comando desde el **Asunto**).

#### C. ¿Qué verás en tu terminal?
En menos de 5 segundos, verás cómo tu terminal cobra vida mostrando:
1. `C: USER grupo05sc` / `S : +OK`
2. `C: PASS ...` / `S : +OK Logged in`
3. `C: RETR 1` y descarga tu correo.
4. `[DISPATCHER]` reconoce el comando y consulta PostgreSQL.
5. Conecta al puerto `25` por SMTP:
   * `C: HELO mail.tecnoweb.org.bo`
   * `C: MAIL FROM: <grupo05sc@tecnoweb.org.bo>`
   * `C: RCPT TO: <nataly.vane.mm@gmail.com>`
   * `C: DATA` (transmite el HTML corporativo con `#3d5598` y `#da3038`).
   * `S : 250 Message accepted for delivery`

#### D. ¿Qué verás en tu bandeja de entrada de Gmail?
Te llegará un correo formal titulado:  
`RE: [CRM-EDUCATIVO] ...`  
Con una tarjeta institucional moderna, la tabla de resultados o la tarjeta de confirmación de tu orden.

---

## 💡 6. Preguntas Clave para Defender con Confianza ante el Ing. Evans

1. **"¿Por qué no usaron librerías de alto nivel como JavaMail?"**  
   *Respuesta:* *"Ingeniero, el sistema fue desarrollado a bajo nivel mediante la doctrina Unix/Java Streams. Usamos exclusivamente `java.net.Socket`, flujos de bytes `BufferedReader` y `DataOutputStream`, implementando a mano los comandos del RFC 1939 para POP3 y RFC 5321 para SMTP."*
2. **"¿Cómo evitan que el hilo se congele si corto el cable de red?"**  
   *Respuesta:* *"En la clase `POP3Listener` y `SMTPSender` configuramos `socket.setSoTimeout(10000)`. Si el servidor remoto deja de responder, a los 10 segundos salta un `SocketTimeoutException` controlado y no dejamos procesos colgados."*
3. **"¿Cómo garantizan que no queden sockets o conexiones abiertas?"**  
   *Respuesta:* *"Todo descriptor se libera en bloques `finally` en orden inverso: primero los flujos `out.close()` e `in.close()`, luego el `socket.close()`, y en base de datos cerramos `ResultSet`, `PreparedStatement` y `Connection`."*
4. **"¿Por qué no hay inyección SQL?"**  
   *Respuesta:* *"El 100% de las consultas utilizan `PreparedStatement` con comodines `?`. El motor de PostgreSQL compila el plan de ejecución primero y cualquier comilla o carácter malicioso enviado en el asunto se procesa como texto literal puro."*
5. **"¿Cómo sabe el sistema quién es el usuario si no pide contraseña?"**  
   *Respuesta:* *"Implementamos autenticación biunívoca por remitente. El sistema extrae el correo del encabezado RFC 5322 `From:`, lo limpia y consulta en la tabla `usuarios` de PostgreSQL para verificar su rol y privilegios antes de despachar la orden."*
