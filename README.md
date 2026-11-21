# CRM Educativo UNEXO – Sistema Automatizado vía Correo Electrónico
### INF513 Tecnología Web | 1er Parcial | Grupo 05 SC
### Universidad Autónoma Gabriel René Moreno (FICCT - UAGRM)
**Docente:** Msc. Ing. Evans Balcázar Veizaga

---

## Equipo de Desarrollo y Responsabilidades

| Integrante | Rol | Casos de Uso Asignados |
| :--- | :---: | :--- |
| **Nataly (Dev A)** | Líder Técnico & Backend | **CU1:** Gestión de Usuarios e Identidad<br>**CU6:** Matrícula, Pagos y Conversión<br>**CU7:** Bitácora y Recordatorios |
| **Nicol (Dev B)** | Backend & Analista | **CU3:** Asignación de Prospectos (Bolsa Común y Round-Robin)<br>**CU4:** Embudo de Ventas Kanban<br>**CU5:** Gestión de Pérdida de Prospectos |
| **Jael (Dev C)** | Backend & Analista | **CU2:** Captación de Prospectos y Oferta Académica<br>**CU8:** Reportes Estadísticos y Dashboards |

---

## Arquitectura Técnica de Bajo Nivel

El sistema implementa un demonio de servidor en Java SE puro que interactúa mediante **Sockets TCP puros** (sin frameworks ni librerías de alto nivel como `javax.mail`):

```
[ Cliente / Usuario ] ──(Envía correo con comando en el Asunto)──► [ mail.tecnoweb.org.bo ]
                                                                             │
                                                                   (POP3 Puerto 110)
                                                                             ▼
                                                                     [ POP3Listener ]
                                                                             │
                                                                             ▼
                                                                    [ CommandParser ]
                                                                             │
                                                                             ▼
                                                                   [ SecurityContext ]
                                                               (RBAC contra PostgreSQL)
                                                                             │
                                                                             ▼
                                                                  [ CommandDispatcher ]
                                                               (Enruta a CU1 hasta CU8)
                                                                             │
                                                                             ▼
                                                                    [ HtmlTemplate ]
                                                               (Estilo Formal UNEXO)
                                                                             │
                                                                             ▼
                                                                     [ SMTPSender ]
                                                                   (SMTP Puerto 25)
                                                                             │
[ Cliente / Usuario ] ◄──(Recibe respuesta HTML: RE: [CRM-EDUCATIVO])────────┘
```

### Protocolos Soportados a Bajo Nivel:
1. **POP3 (RFC 1939):**
   * Diálogo cliente-servidor: `USER`, `PASS`, `STAT`, `RETR`, `DELE`, `QUIT`.
   * Des-escape de puntos (*Byte-stuffing*).
   * Timeout de lectura y escritura (`socket.setSoTimeout(10000)`).
2. **SMTP (RFC 5321):**
   * Diálogo cliente-servidor: `HELO`, `MAIL FROM`, `RCPT TO`, `DATA`, `QUIT`.
   * Cabeceras MIME RFC 5322 (`MIME-Version: 1.0`, `Content-Type: text/html; charset=UTF-8`).
   * Codificación segura de Asunto en Base64 según **RFC 2047** (`=?UTF-8?B?...?=`).
   * Delimitación estricta de fin de cabeceras mediante línea en blanco `\r\n`.
3. **Persistencia Transaccional (PostgreSQL):**
   * Conexiones JDBC con consultas 100% parametrizadas (`PreparedStatement`) para inmunidad absoluta contra **Inyección SQL**.
   * Cierre garantizado de descriptores en bloques `finally`.
   * Criptografía nativa con hash **SHA-256** para contraseñas.

---

## Estrategia de Monitoreo en Doble Terminal

Para evitar la saturación de la consola durante la defensa y al mismo tiempo disponer de la evidencia de sockets de bajo nivel requerida por el docente:

* **Terminal 1 (Consola Principal / Ejecución Limpia):**
  Muestra únicamente eventos operacionales ejecutivos (correo recibido, comando procesado, respuesta entregada).
* **Terminal 2 (Auditoría de Sockets en Vivo):**
  Muestra en tiempo real cada comando `C:` y respuesta `S :` del diálogo de red leyendo el log:
  ```bash
  tail -f logs/socket_traffic.log
  ```

---

## Estructura del Repositorio

```text
INF513_P1/
├── .env                                       # Configuración de red y base de datos
├── lib/
│   └── postgresql.jar                         # Driver JDBC de PostgreSQL
├── bin/                                       # Clases compiladas (.class)
├── logs/
│   └── socket_traffic.log                     # Registro en vivo de tráfico C: y S :
├── docs/
│   ├── FLUJO_DEFENSA_15MIN.md                 # Guion del flujo comercial de 15 minutos
│   ├── ANEXO_CATALOGO_COMANDOS_OFICIALES.md   # Catálogo de los 42 comandos para anexos
│   ├── GUIA_CONTRA_ERRORES_Y_DEFENSA_ORAL.md  # Respuestas ante trampas y contingencias
│   ├── EXPLICACION_LINEA_A_LINEA_CODIGO_1P.md # Explicación línea a línea de las clases
│   └── GUIA_PASO_A_PASO_CU1_GESTION_USUARIOS.md # Pruebas detalladas de CU1
└── src/
    ├── main/java/com/tecnoweb/grupo05sc/
    │   ├── Main.java                          # Demonio servidor en bucle continuo
    │   ├── config/
    │   │   ├── Config.java                    # Cargador nativo de .env
    │   │   └── DatabaseConnection.java        # Gestor de conexiones JDBC y cierres seguros
    │   ├── mail/
    │   │   ├── MailMessage.java               # DTO de mensaje recibido
    │   │   ├── POP3Listener.java              # Lector sockets puerto 110 (RFC 1939)
    │   │   ├── SMTPSender.java                # Emisor sockets puerto 25 (RFC 5321)
    │   │   └── SocketLogger.java              # Logger para estrategia de doble terminal
    │   ├── command/
    │   │   ├── CommandParser.java             # Analizador léxico y regex
    │   │   ├── ParsedCommand.java             # Objeto comando parseado
    │   │   ├── CommandResult.java             # Resultado empaquetado para SMTP
    │   │   ├── CommandException.java          # Excepciones semánticas con sugerencias
    │   │   └── CommandDispatcher.java         # Enrutador central con Levenshtein
    │   ├── security/
    │   │   ├── UsuarioAuth.java               # DTO de usuario autenticado
    │   │   └── SecurityContext.java           # Control de Acceso Basado en Roles (RBAC)
    │   ├── view/
    │   │   └── HtmlTemplate.java              # Plantillas HTML sobrias (Estilo UNEXO)
    │   ├── dao/
    │   │   └── UsuarioDao.java                # DAO de PostgreSQL para CU1
    │   └── service/
    │       └── UsuarioService.java            # Lógica de negocio de CU1
    └── test/java/com/tecnoweb/grupo05sc/
        └── TestRunner.java                    # Batería automatizada de 18 pruebas unitarias
```

---

## Configuración y Requisitos

1. **Java:** Java SE Development Kit (JDK) 8 o superior.
2. **PostgreSQL:** PostgreSQL 14+ (Local o Servidor Facultad `db.tecnoweb.org.bo`).
3. **Variables de Entorno (`.env`):**
   ```properties
   MAIL_HOST=mail.tecnoweb.org.bo
   POP3_PORT=110
   SMTP_PORT=25
   POP3_USER=grupo05sc
   POP3_PASS=grup005grup005*
   MAIL_EMISOR=grupo05sc@tecnoweb.org.bo

   DB_HOST=localhost
   DB_PORT=5432
   DB_NAME=crm_educativo
   DB_USER=postgres
   DB_PASS=postgres
   ```

---

## 🚀 Compilación y Ejecución

### 1. Compilar todo el proyecto:
```bash
javac -encoding UTF-8 -cp "lib/postgresql.jar:." -d bin $(find src/main/java -name "*.java") src/test/java/com/tecnoweb/grupo05sc/TestRunner.java
```

### 2. Ejecutar la Batería de Pruebas Automatizadas (18 tests):
```bash
java -cp "lib/postgresql.jar:bin" com.tecnoweb.grupo05sc.TestRunner
```

### 3. Iniciar el Demonio de Correos en Vivo:
* **Terminal 1:**
  ```bash
  java -cp "lib/postgresql.jar:bin" com.tecnoweb.grupo05sc.Main
  ```
* **Terminal 2 (Auditoría de sockets en vivo):**
  ```bash
  tail -f logs/socket_traffic.log
  ```

---

## Documentación para la Defensa
* [Guion Oficial de Defensa en 15 Minutos](docs/FLUJO_DEFENSA_15MIN.md)
* [Anexo Técnico: Catálogo de 42 Comandos Oficiales](docs/ANEXO_CATALOGO_COMANDOS_OFICIALES.md)
* [Guía de Contingencia ante Fallas y Modificaciones en Vivo](docs/GUIA_CONTRA_ERRORES_Y_DEFENSA_ORAL.md)
* [Explicación Línea a Línea del Código Fuente](docs/EXPLICACION_LINEA_A_LINEA_CODIGO_1P.md)
