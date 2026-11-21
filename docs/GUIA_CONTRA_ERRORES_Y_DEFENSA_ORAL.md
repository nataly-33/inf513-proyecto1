# 🛡️ GUÍA CONTRA ERRORES, TRAMPAS DEL DOCENTE Y DEFENSA ORAL
### INF513 - Tecnología Web | Grupo 05 SC | Docente: Msc. Ing. Evans Balcázar Veizaga
### Sistema Automatizado vía Correo Electrónico – CRM Educativo UNEXO

---

## 🎯 Introducción a la Dinámica de Evaluación del Docente

El Ing. Evans Balcázar es conocido por poner a prueba la solidez del sistema ante fallos imprevistos, comandos maliciosos o mal escritos, cortes de servicios y solicitudes de modificación de código en tiempo real mientras se comparte pantalla.

Esta guía recopila los escenarios reales ocurridos en las defensas y proporciona la respuesta técnica exacta, la acción en código y la preparación como grupo para obtener la máxima nota.

---

## 💣 ESCENARIO 1: El Docente detiene el Servidor de Base de Datos

### ¿Qué hace el docente?
A mitad de la exposición o antes de una prueba, el docente suele detener el servicio de PostgreSQL en el servidor (`systemctl stop postgresql`) o cortar el acceso para ver cómo reacciona tu consola. Si la consola se congela sin decir nada o arroja un *crash* feo no controlado, penaliza al grupo.

### ¿Cómo reacciona nuestro sistema?
1. **Detección Inmediata:** En `DatabaseConnection.java`, cuando se invoca `getConnection()`, el driver de PostgreSQL lanza `SQLException: Connection to localhost:5432 refused`.
2. **Registro Claro en Consola (Terminal 1):**
   ```text
   [11:30:15] [ERROR] No se pudo establecer conexión con PostgreSQL en localhost:5432: Connection refused.
   [11:30:15] [ERROR] Verifique que el servicio de base de datos esté activo.
   ```
3. **El Servidor NO Colapsa:** El hilo principal en `Main.java` atrapa la excepción, mantiene vivo el demonio y continúa escuchando.
4. **Respuesta al Usuario:** Si llegó un correo en ese instante, el sistema le responde formalmente por SMTP:
   `[ERROR] El servicio de base de datos no se encuentra disponible temporalmente. Su petición no fue perdida, por favor reintente en unos momentos.`

### 💡 Plan de Contingencia B (Base de Datos en la Nube - Neon / Supabase):
En el archivo `.env` o en `Config.java` tenemos preparadas las variables de entorno:
```properties
# Base de datos local (Default)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=crm_educativo
DB_USER=postgres
DB_PASS=postgres

# PLAN B (Nube): Si el docente tumba el postgres local, solo descomentas la línea de Neon/Supabase
# DB_HOST=ep-crm-edu.neon.tech
# DB_PORT=5432
# DB_NAME=neondb
# DB_USER=neondb_owner
# DB_PASS=tu_password_neon
```
Si el docente apaga el postgres, en 5 segundos cambias el host en el `.env` o levantas tu postgres local con:
```bash
sudo systemctl start postgresql
```
Y ante el docente explicas:  
> *"Ingeniero, como se observa claramente en nuestra consola, el sistema detectó la desconexión del socket TCP del puerto 5432 hacia PostgreSQL, capturó la SQLException sin colapsar el demonio y notificó al usuario que la base de datos estaba inalcanzable."*

---

## ✏️ ESCENARIO 2: Modificación de Código en Tiempo Real (Encabezado del Correo)

### ¿Qué pide el docente?
*«A ver, veo que su correo dice "CRM EDUCATIVO UNEXO". Cámbienlo ahorita mismo en vivo para que diga "CRM_Santa Cruz" y mándenme un correo para comprobar que surte efecto.»*

### ¿Dónde y cómo se cambia en 10 segundos?
El diseño del código fue preparado con **constantes públicas estáticas** exactamente para este momento en `src/main/java/com/tecnoweb/grupo05sc/view/HtmlTemplate.java`:

* **Líneas 18-20 de `HtmlTemplate.java`:**
```java
// Modificar directamente aquí:
public static String HEADER_TITLE = "CRM_Santa Cruz";
```

### Pasos para aplicar el cambio en vivo:
1. Abres `HtmlTemplate.java`, editas `HEADER_TITLE = "CRM_Santa Cruz";` y guardas (`Ctrl + S`).
2. En la terminal compilas y ejecutas en un solo comando:
   ```bash
   javac -encoding UTF-8 -cp "lib/postgresql.jar:." -d bin $(find src/main/java -name "*.java") && java -cp "lib/postgresql.jar:bin" com.tecnoweb.grupo05sc.Main
   ```
3. Le dices al docente: *«Listo Ingeniero, envíe cualquier comando (por ejemplo `HELP[]`) y verá el nuevo título aplicado.»*

---

## 🔤 ESCENARIO 3: El Docente escribe mal el Comando a propósito

### ¿Qué hace el docente?
Escribe en el Asunto:
* `HELLP` o `AYUDA`
* `CREAR_USUARIO` o `CREARUSUARIOS`
* `REGISTRARPROSPETO`

### ¿Cómo responde nuestro sistema?
El `CommandDispatcher` cuenta con un algoritmo de **Distancia de Levenshtein** que mide la similitud fonética y ortográfica entre lo que escribió el docente y los 42 comandos válidos del CRM.

* **Si envía `HELLP`:**
  * El sistema no falla a ciegas ni devuelve un error genérico.
  * Responde al correo:
    ```text
    [ERROR] El comando 'HELLP' no existe en el sistema.
    Sugerencia: ¿Quizás quiso decir: HELP[]? Envíe HELP[] en el asunto para consultar la guía oficial.
    ```
* **Si envía `CREARUSUARIOS`:**
  * Responde:
    ```text
    [ERROR] El comando 'CREARUSUARIOS' no existe en el sistema.
    Sugerencia: ¿Quizás quiso decir: CREARUSUARIO[]? Envíe HELP["CREARUSUARIO"] para ver su formato.
    ```
Esto demuestra una **tolerancia a fallos de nivel profesional**, superando por completo a cualquier otro grupo de la materia.

---

## 🖥️ ESCENARIO 4: Estrategia de Doble Terminal (Evitar Saturación de Consola)

### El Problema de Otros Grupos:
Al imprimir cada byte de `C:` y `S :` en la pantalla principal, la consola se vuelve un pergamino ilegible donde cuesta encontrar qué comando se ejecutó y si hubo errores.

### La Solución de Nuestro Grupo:
Trabajamos con dos terminales abiertas una al lado de la otra:

* **TERMINAL 1 (La que mira el Docente - Consola Limpia):**
  * Corre:
    ```bash
    java -cp "lib/postgresql.jar:bin" com.tecnoweb.grupo05sc.Main
    ```
  * Muestra únicamente eventos limpios y ejecutivos:
    ```text
    [11:00:02] [POP3] 1 correo(s) detectado(s) en buzón. Descargando...
    [11:00:02] [PETICIÓN RECIBIDA]
      Remitente: docente@uagrm.edu.bo
      Asunto:    HELP[]
    [11:00:03] [DISPATCHER] Procesando comando: HELP con 0 parámetros.
    [11:00:03] [SMTP] Correo entregado satisfactoriamente a: docente@uagrm.edu.bo
    ```

* **TERMINAL 2 (Auditoría de Sockets Puros de Bajo Nivel en Vivo):**
  * Corre:
    ```bash
    tail -f logs/socket_traffic.log
    ```
  * Si el docente dice: *«¿Y cómo sé que no usaron JavaMail? ¿Dónde están los sockets?»*, señalas la Terminal 2:
    ```text
    C: USER grupo05sc
    S : +OK User accepted
    C: PASS ********
    S : +OK Password accepted
    C: STAT
    S : +OK 1 450
    C: RETR 1
    S : +OK 450 octets
    C: DELE 1
    S : +OK Message 1 marked for deletion
    C: QUIT
    S : +OK POP3 server signing off
    C: HELO mail.tecnoweb.org.bo
    S : 250 mail.tecnoweb.org.bo
    C: MAIL FROM: <grupo05sc@tecnoweb.org.bo>
    S : 250 2.1.0 Ok
    C: RCPT TO: <docente@uagrm.edu.bo>
    S : 250 2.1.5 Ok
    C: DATA
    S : 354 End data with <CR><LF>.<CR><LF>
    C: QUIT
    S : 221 2.0.0 Bye
    ```
  * Esto deja boquiabierto al docente porque tienes **orden profesional en Terminal 1** y **auditoría estricta de protocolos RFC 1939 y RFC 5321 en Terminal 2**.

---

## 📊 ESCENARIO 5: Mostrar Modificaciones en la Base de Datos en Tiempo Real

### ¿Qué pide el docente?
*«Usted dice que insertó a Mario Becerra. A ver, muéstreme la base de datos para ver si de verdad se guardó.»*

### Preparación para la Defensa:
Tener abierta una tercera pestaña de terminal conectada a PostgreSQL mediante `psql`:

```bash
psql -U postgres -d crm_educativo
```
Tener a mano los comandos SQL rápidos:

1. **Ver último prospecto insertado:**
   ```sql
   SELECT id_lead, nombre, telefono, correo, id_etapa, id_vendedor, created_at FROM leads ORDER BY id_lead DESC LIMIT 3;
   ```
2. **Ver asignación de vendedor:**
   ```sql
   SELECT l.id_lead, l.nombre, u.nombre_completo AS vendedor, a.tipo_asignacion 
   FROM leads l 
   LEFT JOIN usuarios u ON l.id_vendedor = u.id_usuario 
   LEFT JOIN asignaciones_lead a ON l.id_lead = a.id_lead 
   ORDER BY l.id_lead DESC LIMIT 1;
   ```
3. **Ver bitácora de eventos:**
   ```sql
   SELECT id_bitacora, id_lead, tipo_evento, descripcion, created_at FROM bitacora ORDER BY id_bitacora DESC LIMIT 3;
   ```
4. **Ver comprobante y conversión:**
   ```sql
   SELECT c.id_comprobante, c.id_lead, c.banco, c.monto, c.estado, l.id_etapa 
   FROM comprobantes_pago c 
   JOIN leads l ON c.id_lead = l.id_lead 
   ORDER BY c.id_comprobante DESC LIMIT 1;
   ```

Con esta fluidez, demuestras que el correo y la base de datos están sincronizados al 100% en tiempo real.
