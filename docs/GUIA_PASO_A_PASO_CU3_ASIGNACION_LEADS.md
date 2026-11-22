# 🎯 GUÍA DE PRUEBA MANUAL PASO A PASO: CASO DE USO 3 (CU3 - ASIGNACIÓN DE LEADS)
### INF513 - Tecnología Web | Grupo 05 SC | Docente: Msc. Ing. Evans Balcázar Veizaga
### Responsable Oficial: Nicol (Dev B)

---

## 📌 1. Instrucciones Previas para la Verificación Manual

1. Inicia el servidor demonio de correos en **Terminal 1**:
   ```bash
   cd /home/nataly/Projects/TECNO/crm_correo/INF513_P1
   java -cp "lib/postgresql.jar:bin" com.tecnoweb.grupo05sc.Main
   ```
2. *(Opcional)* En **Terminal 2**, audita el tráfico de Sockets TCP en vivo:
   ```bash
   tail -f /home/nataly/Projects/TECNO/crm_correo/INF513_P1/logs/socket_traffic.log
   ```
3. En **Terminal 3**, abre `psql` para verificar la persistencia y auditoría en tiempo real:
   ```bash
   psql -U postgres -d crm_educativo
   ```
4. Envía los correos de prueba hacia la cuenta oficial:
   **Destinatario:** `grupo05sc@tecnoweb.org.bo`

---

## 📋 2. Banco Exhaustivo de Pruebas Comando por Comando (CU3)

---

### 🔹 PRUEBA 3.1: `BOLSACOMUN[]` (Monitoreo de Prospectos en Espera)

#### Caso A: Consulta Exitosa de la Bolsa Común
* **Objetivo:** Listar todos los prospectos captados que aún no tienen un vendedor asignado (`id_vendedor IS NULL`).
* **Remitente Requerido (`From:`):** Cualquier usuario registrado en el sistema (Admin, Coordinador, Vendedor).
* **Asunto a Enviar (Exacto):**
  ```text
  BOLSACOMUN[]
  ```
* **Asunto de Respuesta del Correo:**
  ```text
  RE: [CRM-EDUCATIVO] Bolsa Comun (X pendientes)
  ```
* **Contenido HTML que Recibirás en tu Bandeja:**
  * Cabecera institucional azul marino: `CRM EDUCATIVO UNEXO`
  * Badge superior: `ASIGNACION`
  * Título: `Bolsa Comun de Prospectos`
  * Resumen: `Total de prospectos en Bolsa Comun: X`
  * Tabla formal (tipografía Serif, bordes rectangulares) con columnas:
    * `ID LEAD`: #ID
    * `PROSPECTO`: Nombre del lead en negrita
    * `TELEFONO`: Número de contacto
    * `CORREO`: Correo del lead
    * `CANAL ORIGEN`: Facebook, Web, WhatsApp, etc.
    * `REGISTRADO`: Fecha y hora de captación
* **Consulta SQL para Comprobar en PostgreSQL:**
  ```sql
  SELECT id_lead, nombre, telefono, correo, id_origen, created_at 
  FROM leads 
  WHERE id_vendedor IS NULL 
  ORDER BY id_lead ASC;
  ```

#### Caso B: Acceso Denegado a Remitente Externo (RBAC)
* **Remitente Requerido (`From:`):** `desconocido@gmail.com`
* **Asunto a Enviar:** `BOLSACOMUN[]`
* **Contenido HTML que Recibirás:**
  ```text
  [ERROR] El remitente 'desconocido@gmail.com' no se encuentra registrado en el sistema.
  Sugerencia: Solicite acceso formal al administrador institucional.
  ```

---

### 🔹 PRUEBA 3.2: `ASIGNARMANUAL[...]` (Asignación Directa a un Vendedor)

#### Caso A: Asignación Manual Exitosa (Admin / Coordinador)
* **Objetivo:** Asignar un lead de la Bolsa Común a la cartera de un vendedor específico, retirándolo de la bolsa y registrando la auditoría.
* **Remitente Requerido (`From:`):** `admin@crm.bo` o `coordinador@crm.bo`
* **Asunto a Enviar (Exacto):** *(Reemplazar con un ID de lead existente en Bolsa Común y un ID de vendedor, por ejemplo lead #14 a vendedor #3)*
  ```text
  ASIGNARMANUAL["14", "3", "Asignacion directa por especialidad comercial"]
  ```
* **Asunto de Respuesta del Correo:**
  ```text
  RE: [CRM-EDUCATIVO] Lead #14 Asignado a Nicol Flores
  ```
* **Contenido HTML que Recibirás en tu Bandeja:**
  * Tarjeta formal con etiqueta `[EXITO]`
  * Título: `Asignacion Manual Exitosa`
  * Mensaje: `El prospecto ha sido asignado a la cartera comercial.`
  * Ficha rectangular con campos:
    * **Prospecto:** Mario Becerra (Lead #14)
    * **Vendedor Asignado:** Nicol Flores (#3)
    * **Asignado Por:** Nataly Valenzuela (Administrador)
    * **Motivo:** Asignacion directa por especialidad comercial
    * **Estado:** `[ASIGNADO] (Retirado de Bolsa Comun)`
* **Consulta SQL para Comprobar en PostgreSQL:**
  ```sql
  -- 1. Verificar que el lead ya tiene vendedor asignado
  SELECT id_lead, nombre, id_vendedor, id_etapa 
  FROM leads WHERE id_lead = 14;

  -- 2. Verificar el registro en la tabla de asignaciones
  SELECT id_asignacion, id_lead, id_vendedor_anterior, id_vendedor_nuevo, asignado_por, tipo, motivo, created_at 
  FROM asignaciones_lead WHERE id_lead = 14 ORDER BY id_asignacion DESC LIMIT 1;

  -- 3. Verificar el hito en la bitácora
  SELECT id_evento, id_lead, id_usuario, tipo, descripcion, created_at 
  FROM bitacora WHERE id_lead = 14 AND tipo = 'ASIGNACION' ORDER BY id_evento DESC LIMIT 1;
  ```

#### Caso B: Rechazo por Asignar a un No-Vendedor (Regla de Negocio)
* **Objetivo:** Demostrar que el sistema bloquea asignar prospectos a Coordinadores o Administradores.
* **Remitente (`From:`):** `admin@crm.bo`
* **Asunto a Enviar:**
  ```text
  ASIGNARMANUAL["14", "2"]
  ```
  *(El usuario #2 es Coordinador Académico).*
* **Contenido HTML que Recibirás:**
  ```text
  [ERROR] El usuario #2 (Carlos Coordinador) tiene rol 'Coordinador'.
  Sugerencia: Los prospectos solo pueden ser asignados a usuarios con rol 'Vendedor'.
  ```

#### Caso C: Bloqueo de Acceso a Vendedores (RBAC)
* **Objetivo:** Un vendedor no tiene atribución de autoasignarse o asignar leads a otros compañeros.
* **Remitente (`From:`):** `vendedor1@crm.bo`
* **Asunto a Enviar:** `ASIGNARMANUAL["14", "3"]`
* **Contenido HTML que Recibirás:**
  ```text
  [ERROR] Acceso denegado: La asignacion manual es exclusiva para Administradores y Coordinadores.
  Sugerencia: Su rol actual es 'Vendedor'.
  ```

---

### 🔹 PRUEBA 3.3: `EJECUTARROUNDROBIN[]` (Reparto Equitativo y Balanceado)

#### Caso A: Reparto Automático con Prospectos Pendientes
* **Objetivo:** Asignar todos los prospectos en Bolsa Común de manera rotativa y equitativa entre los vendedores activos que tengan habilitado el indicador `participa_round_robin = true`.
* **Remitente Requerido (`From:`):** `admin@crm.bo` o `coordinador@crm.bo`
* **Asunto a Enviar (Exacto):**
  ```text
  EJECUTARROUNDROBIN[]
  ```
* **Asunto de Respuesta del Correo:**
  ```text
  RE: [CRM-EDUCATIVO] Reparto Round-Robin (X asignados)
  ```
* **Contenido HTML que Recibirás en tu Bandeja:**
  * Cabecera institucional azul marino
  * Badge superior: `EXITO`
  * Título: `Reparto Round-Robin Completado`
  * Resumen: `Se distribuyeron exitosamente X prospectos entre Y vendedores activos.`
  * Tabla con el desglose del reparto:
    * `ID LEAD`: #ID
    * `PROSPECTO`: Nombre del lead
    * `VENDEDOR ASIGNADO`: Nombre del vendedor (#ID)
* **Consulta SQL para Comprobar en PostgreSQL:**
  ```sql
  -- 1. Verificar que no queden prospectos en Bolsa Común
  SELECT COUNT(*) AS pendientes_bolsa FROM leads WHERE id_vendedor IS NULL;

  -- 2. Verificar el reparto equitativo en asignaciones_lead
  SELECT id_vendedor_nuevo, COUNT(*) AS leads_asignados 
  FROM asignaciones_lead 
  WHERE tipo = 'ROUND_ROBIN' AND created_at >= NOW() - INTERVAL '5 minutes'
  GROUP BY id_vendedor_nuevo;
  ```

#### Caso B: Reparto Round-Robin sin Pendientes en Bolsa
* **Objetivo:** Si la Bolsa Común está vacía, no revienta ni falla; informa con aviso formal.
* **Asunto a Enviar:** `EJECUTARROUNDROBIN[]`
* **Contenido HTML que Recibirás:**
  * Badge superior: `AVISO`
  * Mensaje: `No existen prospectos pendientes en la Bolsa Comun para asignar en este momento.`

---

### 🔹 PRUEBA 3.4: `TRANSFERIRLEAD[...]` (Reasignación con Motivo Obligatorio)

#### Caso A: Transferencia Exitosa entre Vendedores
* **Objetivo:** Transferir un lead de un vendedor titular a un nuevo vendedor, exigiendo justificación formal para trazabilidad de auditoría.
* **Remitente Requerido (`From:`):** `admin@crm.bo` o `coordinador@crm.bo`
* **Asunto a Enviar (Exacto):**
  ```text
  TRANSFERIRLEAD["14", "4", "Reasignacion por sobrecarga comercial del vendedor titular"]
  ```
  *(Transfiere el lead #14 al Vendedor #4 Carlos Mendoza)*.
* **Asunto de Respuesta del Correo:**
  ```text
  RE: [CRM-EDUCATIVO] Lead #14 Transferido a Carlos Mendoza
  ```
* **Contenido HTML que Recibirás en tu Bandeja:**
  * Tarjeta formal con etiqueta `[EXITO]`
  * Título: `Transferencia de Prospecto`
  * Mensaje: `El prospecto ha sido reasignado satisfactoriamente y registrado en auditoria.`
  * Ficha rectangular con campos:
    * **Prospecto:** Mario Becerra (#14)
    * **Vendedor Anterior:** Nicol Flores
    * **Nuevo Titular:** Carlos Mendoza (#4)
    * **Motivo Formal:** Reasignacion por sobrecarga comercial del vendedor titular
    * **Autorizado Por:** Nataly Valenzuela (Administrador)
* **Consulta SQL para Comprobar en PostgreSQL:**
  ```sql
  -- Verificar cambio de titularidad en leads
  SELECT id_lead, nombre, id_vendedor FROM leads WHERE id_lead = 14;

  -- Verificar registro histórico en asignaciones_lead
  SELECT * FROM asignaciones_lead WHERE id_lead = 14 ORDER BY id_asignacion DESC;
  ```

#### Caso B: Rechazo por Motivo Vacío o Parámetros Insuficientes
* **Asunto a Enviar:**
  ```text
  TRANSFERIRLEAD["14", "4", ""]
  ```
* **Contenido HTML que Recibirás:**
  ```text
  [ERROR] El motivo de la transferencia no puede estar vacio.
  Sugerencia: Indique una justificacion formal para el traspaso de cartera.
  ```

---

### 🔹 PRUEBA 3.5: `HISTORIALASIGNACIONES[...]` (Trazabilidad y Auditoría)

#### Caso A: Consulta Cronológica de Traspasos
* **Objetivo:** Ver la línea de tiempo completa de quién tuvo el lead, a quién se lo pasó, en qué fecha, bajo qué motivo y autorizado por quién.
* **Remitente Requerido (`From:`):** Cualquier personal registrado.
* **Asunto a Enviar (Exacto):**
  ```text
  HISTORIALASIGNACIONES["14"]
  ```
* **Asunto de Respuesta del Correo:**
  ```text
  RE: [CRM-EDUCATIVO] Historial Asignaciones Lead #14
  ```
* **Contenido HTML que Recibirás en tu Bandeja:**
  * Cabecera institucional azul marino
  * Badge superior: `AUDITORIA`
  * Título: `Auditoria de Asignaciones - Lead #14`
  * Tabla cronológica completa con columnas:
    * `#`: ID de asignación
    * `DE (ANTERIOR)`: Vendedor anterior o "Bolsa Comun"
    * `A (NUEVO TITULAR)`: Nuevo vendedor responsable
    * `TIPO`: MANUAL, ROUND_ROBIN o TRANSFERENCIA
    * `MOTIVO`: Justificación registrada
    * `AUTORIZADO POR`: Nombre del usuario que aprobó el traspaso
    * `FECHA`: Timestamp exacto del suceso
* **Consulta SQL para Comprobar en PostgreSQL:**
  ```sql
  SELECT a.id_asignacion, 
         COALESCE(va.nombre || ' ' || va.apellido, 'Bolsa Comun') AS de,
         vn.nombre || ' ' || vn.apellido AS a,
         a.tipo, a.motivo, u.nombre || ' ' || u.apellido AS autorizador, a.created_at
  FROM asignaciones_lead a
  LEFT JOIN usuarios va ON a.id_vendedor_anterior = va.id_usuario
  JOIN usuarios vn ON a.id_vendedor_nuevo = vn.id_usuario
  JOIN usuarios u ON a.asignado_por = u.id_usuario
  WHERE a.id_lead = 14
  ORDER BY a.created_at DESC;
  ```

---

## ⚡ 3. Resumen Rápido de Comandos para la Defensa (CU3)

| Comando | Parámetros | Rol Autorizado | Acción Clave |
|---|---|---|---|
| `BOLSACOMUN[]` | Ninguno | Personal (Admin, Coord, Vend) | Ver prospectos en espera |
| `ASIGNARMANUAL["id_lead","id_vend","motivo"]` | 3 | Admin / Coordinador | Asignación directa y formal |
| `EJECUTARROUNDROBIN[]` | Ninguno | Admin / Coordinador | Reparto automático equitativo |
| `TRANSFERIRLEAD["id_lead","id_nuevo","motivo"]` | 3 | Admin / Coordinador | Traspaso con motivo obligatorio |
| `HISTORIALASIGNACIONES["id_lead"]` | 1 | Personal del CRM | Consulta traza de auditoría |

---

## 🎓 4. Preguntas Típicas del Docente y Respuestas Técnicas

### ❓ P1: "¿Cómo garantiza que el algoritmo Round-Robin sea verdaderamente equitativo?"
> **Respuesta:** En `AsignacionDao.java` en el método `obtenerVendedoresRoundRobinActivos()`, filtramos a los vendedores con `activo = true`, `id_rol = 3` y `participa_round_robin = true`, ordenándolos por `id_usuario`. Al iterar la lista de leads de la Bolsa Común, usamos un apuntador circular modular: `idx = (idx + 1) % vendedores.size()`. Además, toda la operación se ejecuta dentro de una transacción única (`conn.setAutoCommit(false)`) con `conn.commit()` y `rollback()` para evitar inconsistencias en caso de falla de red o desconexión.

### ❓ P2: "¿Por qué no permiten que un vendedor se asigne a sí mismo un lead de la Bolsa?"
> **Respuesta:** Por integridad comercial y control gerencial. En `AsignacionService.java` implementamos la validación `if (!auth.esAdmin() && !auth.esCoordinador()) throw new CommandException(...)`. Si permitiéramos a los vendedores autoasignarse, se generaría competencia desleal (*"cherry-picking"*), donde los vendedores elegirían solo los leads más calientes o fáciles de cerrar.

### ❓ P3: "¿Dónde queda la traza de si un lead fue transferido por favoritismo o por necesidad?"
> **Respuesta:** En la tabla `asignaciones_lead` y en `bitacora`. El comando `TRANSFERIRLEAD` exige como tercer parámetro obligatorio el `motivo`. Si el usuario envía una cadena vacía, el validador semántico lo rechaza inmediatamente. Luego, con `HISTORIALASIGNACIONES["id_lead"]` la jefatura puede auditar el historial completo de traspasos y quién los autorizó.
