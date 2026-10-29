# 🛡️ Guía Paso a Paso y Explicación Detallada: Caso de Uso 1 (CU1 - Gestión de Usuarios)
## Sistema vía Correo Electrónico — CRM Educativo (Grupo 05 SC)

> **Materia:** Tecnología Web (INF513 - SC)  
> **Docente:** Msc. Ing. Evans Balcázar Veizaga  
> **Responsable del Caso de Uso 1:** Nataly Vanessa Martinez Martinez (Dev A)  
> **Carpeta Oficial del Proyecto (Git):** [**`INF513_P1/`**](file:///home/nataly/Projects/TECNO/crm_correo/INF513_P1)

---

## 📌 1. Organización del Proyecto y Carpeta Oficial

* **Eliminación del enlace:** Se eliminó por completo el enlace simbólico `INF513_P1_05sc`.
* **Carpeta Oficial Única:** Todo el código fuente, configuración, dependencias y pruebas se encuentran centralizados en tu repositorio Git oficial: **`INF513_P1/`**.
* **Configuración Local:** El archivo [**`INF513_P1/.env`**](file:///home/nataly/Projects/TECNO/crm_correo/INF513_P1/.env) mantiene activas las credenciales locales de PostgreSQL (`crm_educativo`, usuario `postgres`, puerto `5432`).

---

## 📂 2. ¿Qué archivos se crearon y para qué sirve cada uno?

Para implementar el **Caso de Uso 1 (Gestión de Usuarios)** con arquitectura profesional en capas limpias y seguras, se añadieron los siguientes componentes:

```
INF513_P1/src/main/java/com/tecnoweb/grupo05sc/
├── security/
│   ├── UsuarioAuth.java        # DTO de identidad y permisos del remitente
│   └── SecurityContext.java    # Control de Acceso Basado en Roles (RBAC)
├── dao/
│   └── UsuarioDao.java         # Consultas SQL con PreparedStatement (anti-inyección)
├── service/
│   └── UsuarioService.java     # Lógica de Negocio y Reglas Institucionales (Nataly)
└── command/
    └── CommandDispatcher.java  # Conexión entre los comandos por correo y el servicio
```

---

### 🔹 2.1. `UsuarioAuth.java` (Capa de Seguridad)
📁 [**`INF513_P1/src/main/java/com/tecnoweb/grupo05sc/security/UsuarioAuth.java`**](file:///home/nataly/Projects/TECNO/crm_correo/INF513_P1/src/main/java/com/tecnoweb/grupo05sc/security/UsuarioAuth.java)

* **¿Para qué sirve?**  
  Representa al usuario del sistema una vez resuelta su identidad a partir del remitente del correo (`From:`).
* **Atributos:** `idUsuario`, `nombreCompleto`, `correo`, `idRol`, `rolNombre`, `activo`, `participaRoundRobin`.
* **Métodos clave:**
  * `esAdmin()`: Retorna `true` si `idRol == 1` o el nombre es `"Administrador"`.
  * `esCoordinador()`: Retorna `true` si `idRol == 2`.
  * `esVendedor()`: Retorna `true` si `idRol == 3`.
  * `isActivo()`: Indica si la cuenta está habilitada para operar.

---

### 🔹 2.2. `SecurityContext.java` (Control de Acceso RBAC)
📁 [**`INF513_P1/src/main/java/com/tecnoweb/grupo05sc/security/SecurityContext.java`**](file:///home/nataly/Projects/TECNO/crm_correo/INF513_P1/src/main/java/com/tecnoweb/grupo05sc/security/SecurityContext.java)

* **¿Para qué sirve?**  
  Es el guardián de seguridad del sistema. Extrae la dirección limpia de correo y consulta en PostgreSQL para validar qué acciones tiene permitido ejecutar el remitente.
* **Métodos principales:**
  * `obtenerUsuario(String fromEmail)`: Ejecuta una consulta segura con `PreparedStatement` sobre la tabla `usuarios` uniéndola con `roles`. Retorna el objeto `UsuarioAuth` correspondiente o `null` si es un remitente no registrado.
  * `validarAdmin(UsuarioAuth auth)`:
    * Si el usuario no existe: Lanza `CommandException("Acceso denegado: El correo remitente no corresponde a un usuario registrado...")`.
    * Si el usuario está inactivo: Lanza `CommandException("Acceso denegado: Su cuenta de usuario se encuentra desactivada.")`.
    * Si no es Administrador: Lanza `CommandException("Acceso denegado: Esta acción es exclusiva para usuarios con rol 'Administrador'. Su rol actual es '...'.")`.
  * `validarPersonal(UsuarioAuth auth)`: Valida que pertenezca al personal activo de la facultad.

---

### 🔹 2.3. `UsuarioDao.java` (Capa de Acceso a Datos - JDBC)
📁 [**`INF513_P1/src/main/java/com/tecnoweb/grupo05sc/dao/UsuarioDao.java`**](file:///home/nataly/Projects/TECNO/crm_correo/INF513_P1/src/main/java/com/tecnoweb/grupo05sc/dao/UsuarioDao.java)

* **¿Para qué sirve?**  
  Encapsula todas las operaciones SQL sobre las tablas `usuarios` y `roles` garantizando que **ningún dato se concatene directamente** (prevención total de inyecciones SQL).
* **Métodos implementados:**
  1. `listarTodos()`: `SELECT` con `JOIN roles` ordenado por `id_usuario ASC`.
  2. `buscarPorIdOCorreo(String termino)`: Detecta automáticamente si el término es numérico (busca por `id_usuario`) o alfanumérico (busca por coincidencia de correo o nombre).
  3. `buscarPorId(long idUsuario)`: Obtiene la ficha de un usuario específico.
  4. `existeCorreo(String correo)`: Valida si una cuenta ya existe para evitar duplicados en la base de datos.
  5. `existeCorreoExcepto(String correo, long idUsuario)`: Valida que al modificar un usuario, su nuevo correo no colisione con el de otro usuario registrado.
  6. `insertar(nombre, correo, passwordHash, idRol)`: Inserta un nuevo usuario y devuelve su `id_usuario` autogenerado con `RETURN_GENERATED_KEYS`. Si el rol es `Vendedor` (rol 3), asigna `participa_round_robin = true` por defecto.
  7. `actualizar(idUsuario, nombre, correo, idRol)`: Modifica los datos institucionales del personal.
  8. `toggleActivo(idUsuario)`: Ejecuta `UPDATE usuarios SET activo = NOT activo WHERE id_usuario = ?`.
  9. `toggleRoundRobin(idUsuario)`: Ejecuta `UPDATE usuarios SET participa_round_robin = NOT participa_round_robin WHERE id_usuario = ?`.
  10. `obtenerIdRolPorNombre(String rolNombre)`: Convierte cadenas como `"Administrador"`, `"Coordinador"` o `"Vendedor"` a su correspondiente ID foráneo en PostgreSQL.

---

### 🔹 2.4. `UsuarioService.java` (Capa de Negocio - Módulo de Nataly)
📁 [**`INF513_P1/src/main/java/com/tecnoweb/grupo05sc/service/UsuarioService.java`**](file:///home/nataly/Projects/TECNO/crm_correo/INF513_P1/src/main/java/com/tecnoweb/grupo05sc/service/UsuarioService.java)

* **¿Para qué sirve?**  
  Contiene la lógica de negocio y las validaciones de las reglas institucionales exigidas por el docente para el Caso de Uso 1.
* **Comandos implementados:**
  1. `LISTARUSUARIOS[]`:
     * Valida permisos de Administrador.
     * Genera una tabla HTML corporativa con columnas: `ID`, `NOMBRE COMPLETO`, `CORREO`, `ROL`, `ESTADO` (con color verde para Activo y rojo para Inactivo), `ROUND-ROBIN` y `ÚLTIMO ACCESO`.
  2. `BUSCARUSUARIO["correo_o_id"]`:
     * Permite localizar a un usuario por su número de ID o por su dirección de correo.
     * Retorna una ficha estilizada con todos sus atributos y fecha de alta.
  3. `CREARUSUARIO["nombre", "correo", "password", "rol"]`:
     * Valida que ninguno de los 4 campos esté vacío.
     * Valida el formato del correo mediante expresión regular.
     * Valida unicidad (rechaza si el correo ya está registrado).
     * Valida que el rol exista en la institución.
     * **Hashea la contraseña con algoritmo SHA-256 estándar** (`hashPassword`), sin usar librerías externas.
     * Inserta y retorna tarjeta verde de confirmación con el ID asignado.
  4. `MODIFICARUSUARIO["id", "nombre", "correo", "rol"]`:
     * Valida existencia previa del usuario.
     * Valida que el nuevo correo no esté en uso por otra persona.
     * Actualiza y devuelve confirmación.
  5. `TOGGLEUSUARIO["id"]`:
     * **REGLA DE ORO DE SEGURIDAD (PROTECCIÓN DE AUTOBLOQUEO):**  
       Si el Administrador autenticado intenta desactivar o bloquear su propia cuenta (`idUsuario == auth.getIdUsuario()`), el sistema lo bloquea en seco y lanza:  
       *"Operación denegada por seguridad: El Administrador no puede desactivar ni bloquear su propia cuenta. Para dar de baja esta cuenta, otro Administrador debe realizar la solicitud."*
     * Si es otro usuario: Invierte su estado (Activo ➔ Inactivo o viceversa) y devuelve una tarjeta informando el nuevo estado operativo de la cuenta.
  6. `TOGGLEROUNDROBIN["id"]`:
     * Valida que el usuario tenga rol `Vendedor` (rol 3). Si un Administrador intenta aplicarlo a un Coordinador, el sistema le informa que la rotación de prospectos solo aplica a agentes comerciales.
     * Alterna la participación y responde informando si el vendedor ahora recibirá o no prospectos automáticos.

---

### 🔹 2.5. `CommandDispatcher.java` (Enrutador de Comandos)
📁 [**`INF513_P1/src/main/java/com/tecnoweb/grupo05sc/command/CommandDispatcher.java`**](file:///home/nataly/Projects/TECNO/crm_correo/INF513_P1/src/main/java/com/tecnoweb/grupo05sc/command/CommandDispatcher.java)

* Conecta los comandos recibidos por correo con `UsuarioService` pasando el `UsuarioAuth` autenticado:
  ```java
  private CommandResult handleModuloUsuarios(ParsedCommand cmd, String fromEmail) throws Exception {
      UsuarioAuth auth = SecurityContext.obtenerUsuario(fromEmail);
      switch (cmd.getName()) {
          case "LISTARUSUARIOS":   return usuarioService.listarUsuarios(auth);
          case "BUSCARUSUARIO":    return usuarioService.buscarUsuario(auth, cmd);
          case "CREARUSUARIO":     return usuarioService.crearUsuario(auth, cmd);
          case "MODIFICARUSUARIO": return usuarioService.modificarUsuario(auth, cmd);
          case "TOGGLEUSUARIO":    return usuarioService.toggleUsuario(auth, cmd);
          case "TOGGLEROUNDROBIN": return usuarioService.toggleRoundRobin(auth, cmd);
          default: throw new CommandException("Comando no implementado: " + cmd.getName());
      }
  }
  ```

---

## 🧪 3. Batería de Pruebas: 18 de 18 Pruebas Pasadas (100% de Éxito)

Ampliamos la clase de pruebas automatizadas en [**`INF513_P1/src/test/java/com/tecnoweb/grupo05sc/TestRunner.java`**](file:///home/nataly/Projects/TECNO/crm_correo/INF513_P1/src/test/java/com/tecnoweb/grupo05sc/TestRunner.java).

### Para ejecutar las pruebas en tu máquina:
```bash
cd /home/nataly/Projects/TECNO/crm_correo/INF513_P1
javac -encoding UTF-8 -cp "lib/postgresql.jar:." -d bin $(find src/main/java -name "*.java") src/test/java/com/tecnoweb/grupo05sc/TestRunner.java
java -cp "lib/postgresql.jar:bin" com.tecnoweb.grupo05sc.TestRunner
```

### Resultados verificados en la terminal:
```text
==========================================================================
  INICIANDO BATERÍA DE PRUEBAS AUTOMATIZADAS – SISTEMA DE CORREOS        
  INF513 TECNOLOGÍA WEB – GRUPO 05 SC                                     
==========================================================================

✔ TEST 1 PASÓ: LISPER["*"] parseado correctamente.
✔ TEST 2 PASÓ: INSPER parseado con 7 parámetros y soporte de espacios.
✔ TEST 3 PASÓ: Comando 'HELP' sin corchetes parseado como HELP[].
✔ TEST 4 PASÓ: Captura error semántico para asunto vacío.
✔ TEST 5 PASÓ: LISPER["*"] ejecutado sobre PostgreSQL con tabla HTML generada.
✔ TEST 6 PASÓ: INSPER ejecutado exitosamente con PreparedStatement.
✔ TEST 7 PASÓ: QUIENSOY autenticó correctamente al Administrador (Nataly).
✔ TEST 8 PASÓ: CU1 LISTARUSUARIOS[] ejecutado por Administrador con tabla HTML.
✔ TEST 9 PASÓ: CU1 Control de Acceso (RBAC) bloqueó correctamente a Vendedor.
✔ TEST 10 PASÓ: CU1 Bloqueó correctamente petición de remitente no registrado.
✔ TEST 11 PASÓ: CU1 CREARUSUARIO dio de alta al usuario #5 con Round-Robin activo.
✔ TEST 12 PASÓ: CU1 Bloqueó duplicidad de correo con error semántico explicativo.
✔ TEST 13 PASÓ: CU1 BUSCARUSUARIO encontró y formateó la ficha del usuario.
✔ TEST 14 PASÓ: CU1 MODIFICARUSUARIO actualizó datos y correo en PostgreSQL.
✔ TEST 15 PASÓ: CU1 Regla de Oro demostrada -> Bloqueó el autobloqueo del Administrador.
✔ TEST 16 PASÓ: CU1 TOGGLEUSUARIO alternó exitosamente de Activo a Inactivo y de regreso.
✔ TEST 17 PASÓ: CU1 TOGGLEROUNDROBIN excluyó al vendedor de la asignación rotativa.
✔ TEST 18 PASÓ: CU1 Validó regla de negocio -> Round-Robin solo aplica a Vendedores.

==========================================================================
  RESUMEN FINAL: 18 de 18 pruebas pasaron con éxito (100%).
==========================================================================
```

---

## 📬 4. Cómo Probar el Caso de Uso 1 Enviando Correos Reales

### 1. Inicia el Demonio Servidor en tu terminal:
```bash
cd /home/nataly/Projects/TECNO/crm_correo/INF513_P1
java -cp "lib/postgresql.jar:bin" com.tecnoweb.grupo05sc.Main
```

### 2. Envía correos de prueba hacia `grupo05sc@tecnoweb.org.bo`:

| Escenario que probarás | Correo desde el que debes enviar | Asunto (Subject) a escribir | Qué responderá el sistema por correo |
| :--- | :---: | :--- | :--- |
| **Listar todo el personal** | `admin@crm.bo` (o tu Gmail) | `LISTARUSUARIOS[]` | Tabla HTML con todos los usuarios registrados, sus roles y estados. |
| **Buscar un usuario específico** | `admin@crm.bo` | `BUSCARUSUARIO["vendedor1@crm.bo"]` | Ficha detallada de Nicol Flores con su estado comercial. |
| **Dar de alta un nuevo vendedor** | `admin@crm.bo` | `CREARUSUARIO["Roberto Carlos", "roberto@crm.bo", "claveSegura123", "Vendedor"]` | Tarjeta verde confirmando el alta y asignándole ID en la base de datos. |
| **Probar la Protección Anti-Autobloqueo** | `admin@crm.bo` | `TOGGLEUSUARIO["1"]` | **Alerta roja de seguridad:** *"Operación denegada por seguridad: El Administrador no puede desactivar ni bloquear su propia cuenta."* |
| **Desactivar/Reactivar otro usuario** | `admin@crm.bo` | `TOGGLEUSUARIO["3"]` | Confirma que la cuenta de Nicol Flores fue desactivada / reactivada. |
| **Regular Round-Robin** | `admin@crm.bo` | `TOGGLEROUNDROBIN["3"]` | Confirma que el vendedor fue excluido o reincorporado a la rotación automática. |
| **Probar Control de Acceso (RBAC)** | `vendedor1@crm.bo` | `LISTARUSUARIOS[]` | **Rechazo semántico:** *"Acceso denegado: Esta acción es exclusiva para usuarios con rol 'Administrador'."* |

---

## 🎯 5. Argumentario para la Defensa Oral (Ing. Evans Balcázar)

* **¿Cómo garantizan que un vendedor no cree usuarios?**  
  *"Ingeniero, cada orden entrante pasa primero por el `SecurityContext`. Se consulta la identidad biunívoca del remitente (`From:`) contra la tabla `usuarios` y `roles` en PostgreSQL. Si el usuario no tiene rol `Administrador`, el sistema rechaza la ejecución y devuelve un correo semántico explicando que la acción está restringida."*
* **¿Qué sucede si un Administrador intenta desactivarse a sí mismo?**  
  *"En la clase `UsuarioService`, método `toggleUsuario`, implementamos una validación estricta: `if (idUsuario == auth.getIdUsuario())`. Si el ID coincide con el usuario autenticado, la transacción se aborta de inmediato con una alerta de seguridad, garantizando que el sistema nunca quede sin un administrador activo."*
* **¿Cómo almacenan las contraseñas sin librerías externas?**  
  *"Utilizamos `java.security.MessageDigest` de la biblioteca estándar de Java para generar un hash criptográfico SHA-256 en formato hexadecimal. Esto asegura que ninguna contraseña se almacene en texto plano en la base de datos sin depender de frameworks externos."*
* **¿Por qué `PreparedStatement` previene inyección SQL?**  
  *"Porque separa la compilación de la consulta de los datos de entrada. PostgreSQL analiza la estructura del SQL primero y asigna los comodines `?` como valores literales puros, neutralizando cualquier intento de inyectar comillas como `' OR 1=1 --`."*
