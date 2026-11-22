# 🎯 GUÍA DE PRUEBA MANUAL PASO A PASO: CASO DE USO 7 (CU7 - BITÁCORA HISTÓRICA, INTERACCIONES Y RECORDATORIOS)
### INF513 - Tecnología Web | Grupo 05 SC | Docente: Msc. Ing. Evans Balcázar Veizaga

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
3. En **Terminal 3**, abre `psql` para auditar la persistencia de eventos y agenda comercial:
   ```bash
   psql -U postgres -d crm_educativo
   ```
4. Envía los correos de prueba hacia la cuenta oficial:
   **Destinatario:** `grupo05sc@tecnoweb.org.bo`

---

## 📋 2. Banco Exhaustivo de Pruebas Comando por Comando (CU7)

---

### 🔹 PRUEBA 7.1: `AGREGARNOTA[...]` (Registro de Minuta Comercial)

#### Caso A: Registro Exitoso de Nota Cualitativa (2 Parámetros)
* **Objetivo:** Asentar una minuta u observación de seguimiento sobre la interacción con el cliente en la bitácora histórica.
* **Remitente Requerido (`From:`):** `admin@crm.bo` o vendedor asignado.
* **Asunto a Enviar (Exacto):** *(Reemplazar con un ID de lead existente, ej. #14)*
  ```text
  AGREGARNOTA["14", "El prospecto solicita reunión por Zoom el viernes para revisar financiamiento."]
  ```
* **Asunto de Respuesta del Correo:**
  ```text
  RE: [CRM-EDUCATIVO] Nota Registrada - Lead #14
  ```
* **Contenido HTML que Recibirás en tu Bandeja:**
  * Cabecera institucional: `CRM EDUCATIVO UNEXO`
  * Tarjeta formal con etiqueta verde: `[EXITO]`
  * Título: `Nota Comercial Registrada`
  * Mensaje: `Interaccion documentada satisfactoriamente en el historial del lead.`
  * Ficha rectangular con campos:
    * **Nro. Evento:** #(ID_EVENTO)
    * **Prospecto:** #14 - Mario Becerra
    * **Etapa Actual:** Seguimiento
    * **Registrado Por:** Nataly Fuentes (Administradora)
    * **Contenido de la Nota:** *"El prospecto solicita reunión por Zoom el viernes para revisar financiamiento."*
* **Consulta SQL para Comprobar en PostgreSQL:**
  ```sql
  SELECT id_evento, id_lead, id_usuario, tipo, descripcion, created_at
  FROM bitacora
  WHERE id_lead = 14 AND tipo = 'NOTA'
  ORDER BY id_evento DESC LIMIT 1;
  ```

#### Caso B: Rechazo por Nota Vacía o Demasiado Breve (< 3 caracteres)
* **Asunto a Enviar:**
  ```text
  AGREGARNOTA["14", "ok"]
  ```
* **Asunto de Respuesta:**
  ```text
  RE: [CRM-EDUCATIVO] Error: El contenido de la nota es demasiado breve. Especifique al menos 3 caracteres.
  ```
* **Etiqueta:** `[ERROR]`

---

### 🔹 PRUEBA 7.2: `VERBITACORA[...]` (Timeline Cronológico Completo)

#### Caso A: Consulta Integral de la Línea de Tiempo del Lead
* **Objetivo:** Visualizar en orden cronológico ascendente todos los hitos del prospecto (creación, asignaciones, notas, transiciones de etapa, pagos y recordatorios).
* **Remitente Requerido (`From:`):** `admin@crm.bo` o vendedor asignado.
* **Asunto a Enviar (Exacto):**
  ```text
  VERBITACORA["14"]
  ```
* **Asunto de Respuesta del Correo:**
  ```text
  RE: [CRM-EDUCATIVO] Bitacora Lead #14 - Mario Becerra
  ```
* **Contenido HTML que Recibirás en tu Bandeja:**
  * Contenedor institucional con badge azul: `[AUDITORIA]`
  * Título: `Bitacora Historica de Interacciones`
  * Tarjeta informativa del prospecto:
    * **PROSPECTO:** #14 - Mario Becerra
    * **CONTACTO:** Tel: 70012345 | Correo: mario@test.com
    * **ETAPA ACTUAL:** Seguimiento (Etapa #3)
    * **EJECUTIVO ASIGNADO:** Nicol Flores (Vendedora)
  * Tabla cronológica con columnas:
    * `FECHA / HORA`: Formato `yyyy-MM-dd HH:mm`
    * `TIPO`: Badges estilizados: `[CREACIÓN]`, `[ASIGNACIÓN]`, `[CAMBIO ETAPA]`, `[NOTA]`, `[MATRÍCULA]`
    * `RESPONSABLE`: Nombre del personal o sistema automático
    * `DETALLE / TRANSICIÓN`: Explicación del hito, notas y flechas de transición (ej. `Nuevo -> En Contacto`)
* **Consulta SQL para Validar Coherencia:**
  ```sql
  SELECT b.id_evento, b.created_at, b.tipo, u.nombre_completo, b.descripcion 
  FROM bitacora b
  LEFT JOIN usuarios u ON b.id_usuario = u.id_usuario
  WHERE b.id_lead = 14
  ORDER BY b.created_at ASC;
  ```

#### Caso B: Rechazo a Vendedor que Intenta Ver Lead Ajeno
* **Remitente Requerido (`From:`):** `vendedor2@crm.bo` (ej. Carlos Mendoza intentando ver lead asignado a Nicol).
* **Asunto a Enviar:**
  ```text
  VERBITACORA["14"]
  ```
* **Asunto de Respuesta:**
  ```text
  RE: [CRM-EDUCATIVO] Error: Acceso denegado: El prospecto #14 esta asignado a la cartera de otro ejecutivo comercial.
  ```
* **Etiqueta:** `[ERROR]`

---

### 🔹 PRUEBA 7.3: `CREARRECORDATORIO[...]` (Agendamiento de Tarea Comercial)

#### Caso A: Programación Exitosa de Recordatorio (3 Parámetros)
* **Objetivo:** Crear una alarma comercial con fecha y hora programada, vinculada al prospecto y asignada a la agenda del vendedor.
* **Remitente Requerido (`From:`):** `admin@crm.bo` o vendedor asignado.
* **Asunto a Enviar (Exacto):**
  ```text
  CREARRECORDATORIO["14", "2026-11-28 15:30", "Llamar para coordinar modalidad de pago del Diplomado"]
  ```
* **Asunto de Respuesta del Correo:**
  ```text
  RE: [CRM-EDUCATIVO] Recordatorio Agendado #(ID_RECORDATORIO)
  ```
* **Contenido HTML que Recibirás en tu Bandeja:**
  * Tarjeta formal con etiqueta verde: `[EXITO]`
  * Título: `Recordatorio Agendado`
  * Mensaje: `Tarea comercial programada correctamente en el sistema.`
  * Ficha rectangular con campos:
    * **Nro. Recordatorio:** #(ID_RECORDATORIO)
    * **Prospecto Asociado:** #14 - Mario Becerra
    * **Teléfono de Contacto:** 70012345
    * **Fecha y Hora Programada:** 2026-11-28 15:30
    * **Tarea / Compromiso:** Llamar para coordinar modalidad de pago del Diplomado
    * **Responsable:** Nataly Fuentes (Administradora)
* **Consulta SQL para Comprobar en PostgreSQL:**
  ```sql
  -- 1. Verificar el recordatorio en estado PENDIENTE
  SELECT id_recordatorio, id_lead, id_usuario, fecha_hora, descripcion, estado 
  FROM recordatorios WHERE id_lead = 14 ORDER BY id_recordatorio DESC LIMIT 1;

  -- 2. Verificar que se auditó en la bitácora
  SELECT id_evento, tipo, descripcion FROM bitacora WHERE id_lead = 14 AND tipo = 'RECORDATORIO' ORDER BY id_evento DESC LIMIT 1;
  ```

#### Caso B: Rechazo por Formato de Fecha Inválido
* **Asunto a Enviar:**
  ```text
  CREARRECORDATORIO["14", "el-lunes-en-la-tarde", "Llamar"]
  ```
* **Asunto de Respuesta:**
  ```text
  RE: [CRM-EDUCATIVO] Error: Formato de fecha u hora invalido: 'el-lunes-en-la-tarde'.
  ```
* **Sugerencia en el Cuerpo:** `Utilice el formato estandar: yyyy-MM-dd HH:mm (ejemplo: 2026-11-28 15:30) o yyyy-MM-dd HH:mm:ss.`

---

### 🔹 PRUEBA 7.4: `MISRECORDATORIOS[]` (Agenda de Compromisos Pendientes)

#### Caso A: Consulta de Agenda con Tareas Pendientes
* **Objetivo:** Consultar la lista de compromisos y llamadas pendientes agendadas por el remitente.
* **Remitente Requerido (`From:`):** Remitente que posea tareas pendientes (ej. `admin@crm.bo` o `vendedor1@crm.bo`).
* **Asunto a Enviar (Exacto):**
  ```text
  MISRECORDATORIOS[]
  ```
* **Asunto de Respuesta del Correo:**
  ```text
  RE: [CRM-EDUCATIVO] Mis Recordatorios (N)
  ```
* **Contenido HTML que Recibirás en tu Bandeja:**
  * Contenedor institucional con badge cian: `[AGENDA]`
  * Título: `Mis Recordatorios Pendientes`
  * Tabla rectangular formal con columnas:
    * `ID`: Identificador del recordatorio (ej. `#1`, `#2`)
    * `FECHA / HORA`: Fecha programada
    * `PROSPECTO`: #ID y Nombre del Lead
    * `TELÉFONO`: Teléfono de contacto directo
    * `TAREA / DESCRIPCIÓN`: Detalle de la acción requerida
    * `ACCIÓN`: Comando sugerido listo para copiar: `COMPLETARRECORDATORIO["ID"]`

#### Caso B: Consulta de Agenda sin Tareas Pendientes
* **Remitente Requerido (`From:`):** Usuario sin compromisos agendados.
* **Asunto a Enviar:** `MISRECORDATORIOS[]`
* **Contenido HTML:**
  * Mensaje en verde: `[AL DIA] No registra tareas o llamadas pendientes en su agenda comercial.`

---

### 🔹 PRUEBA 7.5: `COMPLETARRECORDATORIO[...]` (Cierre y Cumplimiento de Tarea)

#### Caso A: Cierre Exitoso de Recordatorio
* **Objetivo:** Marcar como completada una tarea comercial agendada, liberando la agenda y auditando el cierre en la bitácora.
* **Remitente Requerido (`From:`):** Usuario titular de la tarea (o Administrador).
* **Asunto a Enviar (Exacto):** *(Reemplazar con ID de recordatorio pendiente)*
  ```text
  COMPLETARRECORDATORIO["1"]
  ```
* **Asunto de Respuesta del Correo:**
  ```text
  RE: [CRM-EDUCATIVO] Recordatorio #1 Completado
  ```
* **Contenido HTML que Recibirás en tu Bandeja:**
  * Tarjeta formal con etiqueta verde: `[EXITO]`
  * Título: `Recordatorio Completado`
  * Mensaje: `Tarea comercial concluida satisfactoriamente.`
  * Ficha rectangular:
    * **Nro. Recordatorio:** #1
    * **Prospecto:** #14 - Mario Becerra
    * **Tarea Atendida:** Llamar para coordinar modalidad de pago del Diplomado
    * **Nuevo Estado:** `[COMPLETADO]` *(Verde)*
    * **Cerrado Por:** Nataly Fuentes (Administradora)
* **Consulta SQL para Comprobar en PostgreSQL:**
  ```sql
  SELECT id_recordatorio, estado, updated_at FROM recordatorios WHERE id_recordatorio = 1;
  ```

#### Caso B: Rechazo por Intento de Cierre Duplicado
* **Asunto a Enviar:**
  ```text
  COMPLETARRECORDATORIO["1"]
  ```
* **Asunto de Respuesta:**
  ```text
  RE: [CRM-EDUCATIVO] Error: El recordatorio #1 ya se encuentra en estado COMPLETADO.
  ```
* **Etiqueta:** `[ERROR]`

---

## 📊 3. Resumen Rápido de Comandos del Caso de Uso 7

| Comando | Sintaxis Exacta | Roles | Propósito Principal |
| :--- | :--- | :---: | :--- |
| **`VERBITACORA`** | `VERBITACORA["id_lead"]` | Admin, Coord, Vend | Línea de tiempo cronológica con todos los hitos y eventos del prospecto. |
| **`AGREGARNOTA`** | `AGREGARNOTA["id_lead", "nota"]` | Admin, Coord, Vend | Registra una minuta u observación de seguimiento en la bitácora. |
| **`CREARRECORDATORIO`**| `CREARRECORDATORIO["id_lead", "yyyy-MM-dd HH:mm", "desc"]` | Admin, Coord, Vend | Agenda una tarea comercial futura con fecha y hora exacta. |
| **`MISRECORDATORIOS`** | `MISRECORDATORIOS[]` | Admin, Coord, Vend | Consulta la lista de compromisos pendientes en la agenda del remitente. |
| **`COMPLETARRECORDATORIO`**| `COMPLETARRECORDATORIO["id_recordatorio"]` | Admin, Coord, Vend | Marca como atendida y finalizada una tarea comercial programada. |
