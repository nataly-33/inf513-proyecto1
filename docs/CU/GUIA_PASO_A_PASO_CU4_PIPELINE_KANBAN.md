# 🎯 GUÍA DE PRUEBA MANUAL PASO A PASO: CASO DE USO 4 (CU4 - EMBUDO DE VENTAS KANBAN)
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
3. En **Terminal 3**, abre `psql` para verificar la persistencia y transiciones de etapas:
   ```bash
   psql -U postgres -d crm_educativo
   ```
4. Envía los correos de prueba hacia la cuenta oficial:
   **Destinatario:** `grupo05sc@tecnoweb.org.bo`

---

## 📋 2. Banco Exhaustivo de Pruebas Comando por Comando (CU4)

---

### 🔹 PRUEBA 4.1: `VERKANBAN[]` (Visualización del Tablero Comercial)

#### Caso A: Tablero Consolidado Institucional (Admin / Coordinador)
* **Objetivo:** Obtener un resumen cuantitativo del embudo de ventas en sus 6 etapas obligatorias.
* **Remitente Requerido (`From:`):** `admin@crm.bo` o `coordinador@crm.bo`
* **Asunto a Enviar (Exacto):**
  ```text
  VERKANBAN[]
  ```
* **Asunto de Respuesta del Correo:**
  ```text
  RE: [CRM-EDUCATIVO] Tablero Kanban (X leads)
  ```
* **Contenido HTML que Recibirás en tu Bandeja:**
  * Cabecera institucional azul marino: `CRM EDUCATIVO UNEXO`
  * Badge superior: `PIPELINE`
  * Título: `Tablero Kanban Comercial`
  * Resumen: `Alcance: Consolidado Institucional | Total en Embudo: X prospectos`
  * Tabla formal con las 6 etapas del embudo:
    * `ORDEN`: 1 al 6
    * `ETAPA DEL EMBUDO COMERCIAL`: Badge de color por etapa:
      * 1: `Nuevo` *(Fondo azul institucional)*
      * 2: `En Contacto` *(Fondo ámbar)*
      * 3: `Seguimiento` *(Fondo morado)*
      * 4: `Promesa de Pago` *(Fondo cian/celeste)*
      * 5: `Convertido` *(Fondo verde oscuro)*
      * 6: `Perdido` *(Fondo rojo)*
    * `PROSPECTOS EN ETAPA`: Cantidad de prospectos en esa etapa
* **Consulta SQL para Comprobar en PostgreSQL:**
  ```sql
  SELECT e.id_etapa, e.orden, e.nombre, COUNT(l.id_lead) AS total_leads
  FROM etapas_pipeline e
  LEFT JOIN leads l ON e.id_etapa = l.id_etapa
  GROUP BY e.id_etapa, e.orden, e.nombre
  ORDER BY e.orden ASC;
  ```

#### Caso B: Filtrado de Kanban por Vendedor Específico
* **Objetivo:** Ver el embudo comercial exclusivo de un vendedor.
* **Asunto a Enviar:**
  ```text
  VERKANBAN["3"]
  ```
* **Contenido HTML que Recibirás:**
  * El resumen indicará: `Alcance: Filtrado por Vendedor #3 | Total en Embudo: Y prospectos`.

#### Caso C: Auto-alcance para Remitente con Rol Vendedor
* **Objetivo:** Si quien envía `VERKANBAN[]` sin parámetros es un vendedor (`vendedor1@crm.bo`), el sistema detecta su identidad y muestra automáticamente solo su cartera personal.
* **Remitente Requerido (`From:`):** `vendedor1@crm.bo`
* **Asunto a Enviar:**
  ```text
  VERKANBAN[]
  ```
* **Contenido HTML que Recibirás:**
  * Resumen: `Alcance: Cartera Personal de Nicol Flores | Total en Embudo: Z prospectos`.

---

### 🔹 PRUEBA 4.2: `LEADSETAPA[...]` (Detalle de Prospectos por Columna)

#### Caso A: Consulta Detallada de una Etapa del Embudo
* **Objetivo:** Ver el listado nominal y datos de contacto de todos los prospectos en una etapa determinada.
* **Remitente Requerido (`From:`):** `admin@crm.bo` o personal del CRM.
* **Asunto a Enviar (Exacto):**
  ```text
  LEADSETAPA["2"]
  ```
  *(Consulta prospectos en etapa 2: 'En Contacto')*.
* **Asunto de Respuesta del Correo:**
  ```text
  RE: [CRM-EDUCATIVO] Leads en En Contacto (X)
  ```
* **Contenido HTML que Recibirás en tu Bandeja:**
  * Badge superior: `PIPELINE`
  * Título: `Columna Kanban: En Contacto`
  * Tabla con columnas:
    * `ID LEAD`: #ID
    * `PROSPECTO`: Nombre completo en negrita
    * `TELEFONO`: Teléfono de contacto
    * `CORREO`: Correo electrónico
    * `VENDEDOR ASIGNADO`: Nombre del vendedor titular o `Bolsa Comun`
    * `ACTUALIZADO`: Timestamp de la última modificación
* **Consulta SQL para Comprobar en PostgreSQL:**
  ```sql
  SELECT l.id_lead, l.nombre, l.telefono, l.correo, 
         COALESCE(u.nombre || ' ' || u.apellido, 'Bolsa Comun') AS vendedor, l.updated_at
  FROM leads l
  LEFT JOIN usuarios u ON l.id_vendedor = u.id_usuario
  WHERE l.id_etapa = 2
  ORDER BY l.updated_at DESC;
  ```

#### Caso B: Rechazo por ID de Etapa Inválido
* **Asunto a Enviar:**
  ```text
  LEADSETAPA["9"]
  ```
* **Contenido HTML que Recibirás:**
  ```text
  [ERROR] El ID de etapa #9 es invalido.
  Sugerencia: Las etapas van del 1 al 6: 1. Nuevo, 2. En Contacto, 3. Seguimiento, 4. Promesa de Pago, 5. Convertido, 6. Perdido.
  ```

---

### 🔹 PRUEBA 4.3: `MOVERETAPA[...]` (Avance Comercial y Auditoría en Bitácora)

#### Caso A: Avance Exitoso en el Embudo (Flujo Ordinario)
* **Objetivo:** Avanzar un prospecto de etapa comercial (por ejemplo, de Etapa 1 *Nuevo* a Etapa 2 *En Contacto* o de Etapa 2 a Etapa 3 *Seguimiento*).
* **Remitente Requerido (`From:`):** `admin@crm.bo` o el vendedor asignado al lead.
* **Asunto a Enviar (Exacto):**
  ```text
  MOVERETAPA["14", "2"]
  ```
* **Asunto de Respuesta del Correo:**
  ```text
  RE: [CRM-EDUCATIVO] Lead #14 ahora en En Contacto
  ```
* **Contenido HTML que Recibirás en tu Bandeja:**
  * Tarjeta formal con etiqueta `[EXITO]`
  * Título: `Etapa Comercial Actualizada`
  * Mensaje: `El prospecto avanzo satisfactoriamente en el Embudo de Ventas.`
  * Ficha rectangular con campos:
    * **Prospecto:** Mario Becerra (Lead #14)
    * **Etapa Anterior:** `[1] Nuevo`
    * **Nueva Etapa:** `[2] En Contacto`
    * **Responsable:** Nataly Valenzuela (Administrador)
    * **Auditoría:** Hito registrado cronológicamente en la Bitácora institucional.
* **Consulta SQL para Comprobar en PostgreSQL:**
  ```sql
  -- 1. Verificar cambio de etapa en leads
  SELECT id_lead, nombre, id_etapa, updated_at FROM leads WHERE id_lead = 14;

  -- 2. Verificar registro automático en la bitácora institucional
  SELECT id_evento, id_lead, id_usuario, tipo, id_etapa_anterior, id_etapa_nueva, descripcion, created_at 
  FROM bitacora 
  WHERE id_lead = 14 AND tipo = 'CAMBIO_ETAPA' 
  ORDER BY id_evento DESC LIMIT 1;
  ```

#### Caso B: Bloqueo de Regla de Negocio al Mover a Etapa 6 (Perdido)
* **Objetivo:** Demostrar que el sistema PROHÍBE mover a *Perdido* sin justificación formal obligando a usar el caso de uso de Motivos de Pérdida (CU5).
* **Asunto a Enviar:**
  ```text
  MOVERETAPA["14", "6"]
  ```
* **Contenido HTML que Recibirás:**
  ```text
  [ERROR] Para mover un prospecto a la etapa 'Perdido' debe tipificar obligatoriamente el motivo formal del desistimiento.
  Sugerencia: Utilice el comando: MARCARPERDIDO["id_lead", "id_motivo", "comentario justificativo"].
  ```

#### Caso C: Bloqueo de Regla de Negocio al Mover a Etapa 5 (Convertido)
* **Objetivo:** Demostrar que el sistema PROHÍBE marcar como matriculado/convertido sin procesar el pago y matrícula formal (CU6).
* **Asunto a Enviar:**
  ```text
  MOVERETAPA["14", "5"]
  ```
* **Contenido HTML que Recibirás:**
  ```text
  [ERROR] Para formalizar la conversion a 'Convertido' debe procesarse la matriculacion academica formal.
  Sugerencia: Asegurese de tener el pago aprobado y utilice el comando: CONVERTIRAMATRICULA["id_lead"].
  ```

#### Caso D: Control de Acceso por Cartera Comercial (Vendedores Ajenos)
* **Objetivo:** Un vendedor solo puede mover prospectos de su propia cartera.
* **Remitente Requerido (`From:`):** `vendedor2@crm.bo` (Carlos Mendoza, usuario #4)
* **Asunto a Enviar:** *(Intentando mover un lead que está asignado al Vendedor #3)*
  ```text
  MOVERETAPA["14", "3"]
  ```
* **Contenido HTML que Recibirás:**
  ```text
  [ERROR] Acceso denegado: El prospecto #14 no pertenece a su cartera comercial.
  Sugerencia: Solo puede gestionar prospectos asignados a su cuenta.
  ```

---

### 🔹 PRUEBA 4.4: `MISLEADS[]` (Cartera Personal del Vendedor)

#### Caso A: Consulta de Cartera Personal
* **Objetivo:** Permitir al vendedor consultar en un solo vistazo todos los prospectos bajo su responsabilidad, con su etapa actual y última fecha de gestión.
* **Remitente Requerido (`From:`):** `vendedor1@crm.bo`
* **Asunto a Enviar (Exacto):**
  ```text
  MISLEADS[]
  ```
* **Asunto de Respuesta del Correo:**
  ```text
  RE: [CRM-EDUCATIVO] Mi Cartera (X leads)
  ```
* **Contenido HTML que Recibirás en tu Bandeja:**
  * Cabecera institucional azul marino
  * Badge superior: `CARTERA`
  * Título: `Mi Cartera de Prospectos`
  * Resumen: `Cartera comercial asignada a: Nicol Flores | Total: X prospectos`
  * Tabla con columnas:
    * `ID LEAD`: #ID
    * `PROSPECTO`: Nombre del lead en negrita
    * `TELEFONO`: Teléfono de contacto
    * `CORREO`: Correo electrónico
    * `ETAPA KANBAN`: Badge estilizado con el nombre de la etapa actual
    * `ULTIMA GESTION`: Timestamp del último seguimiento o actualización
* **Consulta SQL para Comprobar en PostgreSQL:**
  ```sql
  SELECT l.id_lead, l.nombre, l.telefono, l.correo, e.nombre AS etapa, l.updated_at
  FROM leads l
  JOIN etapas_pipeline e ON l.id_etapa = e.id_etapa
  WHERE l.id_vendedor = 3
  ORDER BY l.updated_at DESC;
  ```

---

## ⚡ 3. Resumen Rápido de Comandos para la Defensa (CU4)

| Comando | Parámetros | Rol Autorizado | Acción Clave |
|---|---|---|---|
| `VERKANBAN[]` | Opcional: `id_vendedor` | Personal del CRM | Resumen cuantitativo 6 etapas |
| `LEADSETAPA["id_etapa"]` | 1 (1 al 6) | Personal del CRM | Ver leads de una columna |
| `MOVERETAPA["id_lead","id_etapa"]` | 2 | Asignado / Admin / Coord | Mueve de etapa con bitácora |
| `MISLEADS[]` | Ninguno | Personal (Vendedores) | Ver cartera personal del remitente |

---

## 🎓 4. Preguntas Típicas del Docente y Respuestas Técnicas

### ❓ P1: "¿Por qué no permiten mover directamente a las etapas 5 y 6 con `MOVERETAPA`?"
> **Respuesta:** Porque violaría las políticas institucionales de control. La etapa 6 (*Perdido*) requiere una causa analítica obligatoria (precio, horarios, desinterés) tipificada en `motivos_perdida` para los reportes gerenciales del CU5. Y la etapa 5 (*Convertido*) exige que exista un pago verificado o una matrícula registrada formalmente en `matriculas` (CU6). En `PipelineService.java`, interceptamos las transiciones a 5 y 6 guiando al usuario hacia el comando transaccional correspondiente.

### ❓ P2: "¿Cómo evitan que un vendedor toque los prospectos de otro vendedor?"
> **Respuesta:** Mediante el contexto de seguridad RBAC. En `PipelineService.java` verificamos `if (auth.esVendedor()) { if (lead.getIdVendedor() != auth.getIdUsuario()) throw new CommandException(...) }`. Un vendedor únicamente puede visualizar y mover los prospectos donde `leads.id_vendedor == auth.getIdUsuario()`. Solo el Administrador o Coordinador pueden operar transversalmente.

### ❓ P3: "¿Cómo se registra el movimiento de etapas para que no se pierda la trazabilidad?"
> **Respuesta:** En `PipelineDao.java`, la actualización de la tabla `leads` y la inserción en la tabla `bitacora` se realizan en una sola transacción atómica (`conn.setAutoCommit(false)`). En `bitacora` se inserta el `id_lead`, `id_usuario` que hizo el cambio, `tipo = 'CAMBIO_ETAPA'`, `id_etapa_anterior`, `id_etapa_nueva` y la descripción.
