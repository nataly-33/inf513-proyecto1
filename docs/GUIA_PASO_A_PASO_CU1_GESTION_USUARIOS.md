# 🛡️ GUÍA DE PRUEBA MANUAL PASO A PASO: CASO DE USO 1 (CU1 - GESTIÓN DE USUARIOS)
### INF513 - Tecnología Web | Grupo 05 SC | Docente: Msc. Ing. Evans Balcázar Veizaga
### Responsable Oficial: Nataly Vanessa Martinez Martinez (Dev A)

---

## 📌 1. Instrucciones Previas para la Verificación Manual

Para realizar la verificación manual de cada comando:
1. Asegúrate de que el servidor PostgreSQL esté corriendo en tu máquina local (`crm_educativo`).
2. Abre **Terminal 1** en `/home/nataly/Projects/TECNO/crm_correo/INF513_P1` e inicia el servidor de correos:
   ```bash
   cd /home/nataly/Projects/TECNO/crm_correo/INF513_P1
   java -cp "lib/postgresql.jar:bin" com.tecnoweb.grupo05sc.Main
   ```
3. *(Opcional)* Abre **Terminal 2** para auditar los Sockets TCP en vivo:
   ```bash
   tail -f /home/nataly/Projects/TECNO/crm_correo/INF513_P1/logs/socket_traffic.log
   ```
4. Abre **Terminal 3** con `psql` para verificar la base de datos en tiempo real:
   ```bash
   psql -U postgres -d crm_educativo
   ```
5. Desde tu cliente de correo (Gmail, Thunderbird, etc.), envía los correos a la cuenta oficial:
   **Destinatario:** `grupo05sc@tecnoweb.org.bo`

---

## 📋 2. Banco Exhaustivo de Pruebas Comando por Comando

---

### 🔹 PRUEBA 1.1: `LISTARUSUARIOS[]` (Flujo Exitoso)
* **Objetivo:** Obtener la tabla general de todo el personal institucional registrado.
* **Remitente Requerido (`From:`):** `admin@crm.bo` *(o correo registrado con rol Administrador)*.
* **Asunto a Enviar (Exacto):**
  ```text
  LISTARUSUARIOS[]
  ```
  *(También se admite sin corchetes: `LISTARUSUARIOS`)*.
* **Asunto de Respuesta del Correo:**
  ```text
  RE: [CRM-EDUCATIVO] Listado de Usuarios
  ```
* **Contenido HTML que Recibirás en tu Bandeja:**
  * Cabecera institucional azul marino: `CRM EDUCATIVO UNEXO`
  * Título: `Gestión de Usuarios (CU1 – Nataly)`
  * Resumen: `Total de usuarios registrados en el sistema: X`
  * Tabla rectangular con bordes limpios y columnas:
    `ID | NOMBRE COMPLETO | CORREO | ROL | ESTADO | ROUND-ROBIN | ÚLTIMO ACCESO`
    * Fila 1: `1 | Nataly Martinez | admin@crm.bo | Administrador | Activo | No | ...`
    * Fila 2: `2 | Coordinador Academico | coordinador@crm.bo | Coordinador | Activo | No | ...`
    * Fila 3: `3 | Vendedor 1 | vendedor1@crm.bo | Vendedor | Activo | Sí | ...`
* **Consulta SQL para Comprobar en PostgreSQL:**
  ```sql
  SELECT u.id_usuario, u.nombre_completo, u.correo, r.nombre AS rol, u.activo, u.participa_round_robin 
  FROM usuarios u JOIN roles r ON u.id_rol = r.id_rol ORDER BY u.id_usuario ASC;
  ```

---

### 🔹 PRUEBA 1.2: `BUSCARUSUARIO[...]` (Búsqueda por Correo y por ID)

#### Caso A: Búsqueda por Correo Institucional
* **Remitente (`From:`):** `admin@crm.bo`
* **Asunto a Enviar:**
  ```text
  BUSCARUSUARIO["vendedor1@crm.bo"]
  ```
* **Asunto de Respuesta:**
  ```text
  RE: [CRM-EDUCATIVO] Usuario Vendedor 1
  ```
* **Contenido HTML que Recibirás:**
  * Tarjeta rectangular con título `Detalle de Usuario` e insignia `CONSULTA`.
  * Ficha con los campos:
    * **ID Usuario:** 3
    * **Nombre Completo:** Vendedor 1
    * **Correo Institucional:** `vendedor1@crm.bo`
    * **Rol Asignado:** Vendedor
    * **Estado Operativo:** Cuenta Activa
    * **Participación en Round-Robin:** Activa
    * **Fecha de Alta:** *(Fecha de creación)*

#### Caso B: Búsqueda por ID Numérico
* **Asunto a Enviar:**
  ```text
  BUSCARUSUARIO["1"]
  ```
* **Respuesta:** Ficha del Administrador del sistema.

#### Caso C: Búsqueda de Usuario Inexistente (Manejo de Error)
* **Asunto a Enviar:**
  ```text
  BUSCARUSUARIO["noexiste@crm.bo"]
  ```
* **Asunto de Respuesta:**
  ```text
  RE: [CRM-EDUCATIVO] Error en comando: BUSCARUSUARIO["noexiste@crm.bo"]
  ```
* **Contenido HTML que Recibirás:**
  * Tarjeta roja formal:
    ```text
    [ERROR] No se encontró ningún usuario con el criterio de búsqueda: "noexiste@crm.bo".
    Sugerencia: Verifique el correo o ID ingresado o envíe LISTARUSUARIOS[] para ver todos los usuarios.
    ```

---

### 🔹 PRUEBA 1.3: `CREARUSUARIO[...]` (Alta de Usuario y Hash SHA-256)

#### Caso A: Registro Exitoso de un Nuevo Vendedor
* **Remitente (`From:`):** `admin@crm.bo`
* **Asunto a Enviar:**
  ```text
  CREARUSUARIO["Mariana Suarez", "mariana.suarez@crm.bo", "PasswordSeguro2026*", "Vendedor"]
  ```
* **Asunto de Respuesta:**
  ```text
  RE: [CRM-EDUCATIVO] Alta de Usuario Exitosa
  ```
* **Contenido HTML que Recibirás:**
  * Tarjeta verde formal con etiqueta `[EXITO]`:
    ```text
    [EXITO] Operación Ejecutada Correctamente
    La cuenta de usuario ha sido persistida en PostgreSQL.

    • ID Asignado: (Nuevo ID autogenerado, ej. 12)
    • Nombre Completo: Mariana Suarez
    • Correo Registrado: mariana.suarez@crm.bo
    • Rol: Vendedor
    • Estado Inicial: Activo
    • Round-Robin: Habilitado (Vendedor)
    ```
* **Consulta SQL para Comprobar en PostgreSQL:**
  ```sql
  SELECT id_usuario, nombre_completo, correo, password_hash, id_rol, activo, participa_round_robin 
  FROM usuarios WHERE correo = 'mariana.suarez@crm.bo';
  ```
  *(Observa que `password_hash` es un hash hexadecimal de 64 caracteres SHA-256, garantizando seguridad criptográfica)*.

#### Caso B: Rechazo por Correo Duplicado
* **Asunto a Enviar:**
  ```text
  CREARUSUARIO["Mariana Clon", "mariana.suarez@crm.bo", "123456", "Vendedor"]
  ```
* **Contenido HTML que Recibirás:**
  ```text
  [ERROR] El correo institucional 'mariana.suarez@crm.bo' ya se encuentra registrado para otra cuenta.
  Sugerencia: Utilice una dirección de correo diferente o verifique el listado existente con LISTARUSUARIOS[].
  ```

#### Caso C: Rechazo por Formato de Correo Inválido
* **Asunto a Enviar:**
  ```text
  CREARUSUARIO["Carlos", "carlos_sin_arroba", "pass123", "Vendedor"]
  ```
* **Contenido HTML que Recibirás:**
  ```text
  [ERROR] El formato del correo electrónico 'carlos_sin_arroba' es inválido.
  Sugerencia: Ingrese un correo válido con formato usuario@dominio.com
  ```

#### Caso D: Rechazo por Rol Desconocido
* **Asunto a Enviar:**
  ```text
  CREARUSUARIO["Pedro", "pedro@crm.bo", "pass123", "GerenteGeneral"]
  ```
* **Contenido HTML que Recibirás:**
  ```text
  [ERROR] El rol 'GerenteGeneral' no existe en el sistema.
  Sugerencia: Los roles institucionales permitidos son: 'Administrador', 'Coordinador', 'Vendedor'.
  ```

---

### 🔹 PRUEBA 1.4: `MODIFICARUSUARIO[...]` (Actualización de Datos)
* **Remitente (`From:`):** `admin@crm.bo`
* **Asunto a Enviar:** *(Reemplazar el primer parámetro con el ID del usuario creado)*
  ```text
  MODIFICARUSUARIO["12", "Mariana Suarez de Perez", "mariana.modificada@crm.bo", "Vendedor"]
  ```
* **Asunto de Respuesta:**
  ```text
  RE: [CRM-EDUCATIVO] Modificación de Usuario #12
  ```
* **Contenido HTML que Recibirás:**
  ```text
  [EXITO] Operación Ejecutada Correctamente
  Los datos institucionales han sido actualizados satisfactoriamente.

  • ID Usuario: 12
  • Nuevo Nombre: Mariana Suarez de Perez
  • Nuevo Correo: mariana.modificada@crm.bo
  • Nuevo Rol: Vendedor
  ```
* **Consulta SQL para Comprobar en PostgreSQL:**
  ```sql
  SELECT id_usuario, nombre_completo, correo, id_rol, updated_at FROM usuarios WHERE id_usuario = 12;
  ```

---

### 🔹 PRUEBA 1.5: `TOGGLEUSUARIO[...]` (Activar/Desactivar y Regla de Oro)

#### Caso A: Demostración de la Regla de Oro Anti-Autobloqueo (¡Vital para el Docente!)
* **Remitente (`From:`):** `admin@crm.bo` *(cuyo `id_usuario` es `1`)*.
* **Asunto a Enviar:**
  ```text
  TOGGLEUSUARIO["1"]
  ```
* **Asunto de Respuesta:**
  ```text
  RE: [CRM-EDUCATIVO] Error en comando: TOGGLEUSUARIO["1"]
  ```
* **Contenido HTML que Recibirás:**
  ```text
  [ERROR] Operación denegada por seguridad: El Administrador no puede desactivar ni bloquear su propia cuenta.
  Sugerencia: Para dar de baja esta cuenta administrativa, otro usuario con rol Administrador debe ejecutar la acción.
  ```
* **Verificación en PostgreSQL:**
  ```sql
  SELECT id_usuario, correo, activo FROM usuarios WHERE id_usuario = 1;
  ```
  *(El valor de `activo` permanece intacto en `true`)*.

#### Caso B: Desactivar a Otro Usuario
* **Asunto a Enviar:**
  ```text
  TOGGLEUSUARIO["12"]
  ```
* **Contenido HTML que Recibirás:**
  ```text
  [EXITO] Operación Ejecutada Correctamente
  Se alternó con éxito la disponibilidad de la cuenta.

  • Usuario: Mariana Suarez (mariana.modificada@crm.bo)
  • Rol: Vendedor
  • Nuevo Estado: DESACTIVADA (El acceso al sistema ha sido revocado)
  ```
* **Verificación en PostgreSQL:**
  ```sql
  SELECT id_usuario, activo FROM usuarios WHERE id_usuario = 12;
  -- Resultado: activo = false
  ```

#### Caso C: Reactivar al Usuario
* **Asunto a Enviar:**
  ```text
  TOGGLEUSUARIO["12"]
  ```
* **Contenido HTML que Recibirás:**
  ```text
  [EXITO] Operación Ejecutada Correctamente
  • Nuevo Estado: ACTIVADA (El usuario ahora puede acceder al sistema)
  ```
* **Verificación en PostgreSQL:**
  ```sql
  SELECT id_usuario, activo FROM usuarios WHERE id_usuario = 12;
  -- Resultado: activo = true
  ```

---

### 🔹 PRUEBA 1.6: `TOGGLEROUNDROBIN[...]` (Regulación de Reparto Comercial)

#### Caso A: Excluir a un Vendedor de la Rotación Automática
* **Remitente (`From:`):** `admin@crm.bo`
* **Asunto a Enviar:**
  ```text
  TOGGLEROUNDROBIN["3"]
  ```
* **Contenido HTML que Recibirás:**
  ```text
  [EXITO] Operación Ejecutada Correctamente
  Se actualizó la participación en la bolsa comercial.

  • Vendedor: Vendedor 1
  • Correo: vendedor1@crm.bo
  • Participación Round-Robin: EXCLUIDO (No recibirá prospectos automáticos hasta nuevo aviso)
  ```
* **Verificación en PostgreSQL:**
  ```sql
  SELECT id_usuario, participa_round_robin FROM usuarios WHERE id_usuario = 3;
  -- Resultado: participa_round_robin = false
  ```

#### Caso B: Reincorporar al Vendedor
* **Asunto a Enviar:**
  ```text
  TOGGLEROUNDROBIN["3"]
  ```
* **Contenido HTML que Recibirás:**
  ```text
  [EXITO] Operación Ejecutada Correctamente
  • Participación Round-Robin: HABILITADO (Recibirá nuevos prospectos automáticamente en la rotación)
  ```

#### Caso C: Intento de Aplicar Round-Robin a un No-Vendedor (Validación de Negocio)
* **Asunto a Enviar:** *(El usuario #2 es Coordinador)*
  ```text
  TOGGLEROUNDROBIN["2"]
  ```
* **Contenido HTML que Recibirás:**
  ```text
  [ERROR] El usuario #2 (Coordinador Academico) tiene rol 'Coordinador'.
  Sugerencia: La asignación automática Round-Robin solo es aplicable a usuarios con rol 'Vendedor'.
  ```

---

### 🔹 PRUEBA 1.7: Control de Acceso Basado en Roles (RBAC)

#### Caso A: Vendedor Intenta Listar Usuarios
* **Remitente (`From:`):** `vendedor1@crm.bo`
* **Asunto a Enviar:**
  ```text
  LISTARUSUARIOS[]
  ```
* **Contenido HTML que Recibirás:**
  ```text
  [ERROR] Acceso denegado: Esta acción es exclusiva para usuarios con rol 'Administrador'.
  Sugerencia: Su rol actual es 'Vendedor'. Envíe HELP[] para ver los comandos permitidos para su rol.
  ```

#### Caso B: Remitente Externo Desconocido Intenta Comandos Administrativos
* **Remitente (`From:`):** `hacker@externo.com`
* **Asunto a Enviar:**
  ```text
  CREARUSUARIO["Infiltrado", "infiltrado@crm.bo", "123", "Administrador"]
  ```
* **Contenido HTML que Recibirás:**
  ```text
  [ERROR] Acceso denegado: El correo remitente no corresponde a un usuario registrado en el sistema.
  Sugerencia: Para solicitar una cuenta institucional, comuníquese con el administrador del CRM o envíe HELP[] para asistencia.
  ```

---

## 🎯 Resumen de Comprobaciones para la Defensa

| Comando Probado | Asunto Enviado | Código de Estado | Resultado Visible en Correo | Comprobación en BD |
| :--- | :--- | :---: | :--- | :--- |
| `LISTARUSUARIOS` | `LISTARUSUARIOS[]` | `250 OK` | Tabla HTML con usuarios y roles | `SELECT * FROM usuarios;` |
| `BUSCARUSUARIO` | `BUSCARUSUARIO["vendedor1@crm.bo"]` | `250 OK` | Ficha formal del usuario | `SELECT * FROM usuarios WHERE correo = ...;` |
| `CREARUSUARIO` | `CREARUSUARIO["Nombre","mail","pass","rol"]` | `250 OK` | `[EXITO]` con ID asignado | `password_hash` en SHA-256 |
| `MODIFICARUSUARIO`| `MODIFICARUSUARIO["id","nom","mail","rol"]` | `250 OK` | `[EXITO]` con datos actualizados | `updated_at` actualizado |
| `TOGGLEUSUARIO` | `TOGGLEUSUARIO["1"]` *(mismo admin)* | `250 OK` | `[ERROR]` Anti-Autobloqueo | `activo` se mantiene en `true` |
| `TOGGLEUSUARIO` | `TOGGLEUSUARIO["3"]` *(otro usuario)* | `250 OK` | `[EXITO]` Cambia Activo/Inactivo | `activo` alterna `true/false` |
| `TOGGLEROUNDROBIN`| `TOGGLEROUNDROBIN["3"]` | `250 OK` | `[EXITO]` Habilitado/Excluido | `participa_round_robin` alterna |
| `TOGGLEROUNDROBIN`| `TOGGLEROUNDROBIN["2"]` *(no vendedor)* | `250 OK` | `[ERROR]` Solo para Vendedores | Sin cambios en BD |
