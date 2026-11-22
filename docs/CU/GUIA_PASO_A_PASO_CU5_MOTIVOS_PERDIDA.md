# 🎯 GUÍA DE PRUEBA MANUAL PASO A PASO: CASO DE USO 5 (CU5 - GESTIÓN DE PÉRDIDAS Y DESISTIMIENTOS)
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
3. En **Terminal 3**, abre `psql` para verificar la persistencia y trazabilidad en tiempo real:
   ```bash
   psql -U postgres -d crm_educativo
   ```
4. Envía los correos de prueba hacia la cuenta oficial:
   **Destinatario:** `grupo05sc@tecnoweb.org.bo`

---

## 📋 2. Banco Exhaustivo de Pruebas Comando por Comando (CU5)

---

### 🔹 PRUEBA 5.1: `LISTARMOTIVOSPERDIDA[]` (Catálogo Oficial de Causas)

#### Caso A: Consulta Exitosa del Catálogo
* **Objetivo:** Consultar la lista institucional de causas tipificadas por las que un prospecto desiste de su inscripción (factor económico, horarios, falta de respuesta, etc.).
* **Remitente Requerido (`From:`):** Cualquier usuario registrado en el sistema (Admin, Coordinador, Vendedor).
* **Asunto a Enviar (Exacto):**
  ```text
  LISTARMOTIVOSPERDIDA[]
  ```
* **Asunto de Respuesta del Correo:**
  ```text
  RE: [CRM-EDUCATIVO] Motivos de Perdida (4)
  ```
* **Contenido HTML que Recibirás en tu Bandeja:**
  * Cabecera institucional azul marino: `CRM EDUCATIVO UNEXO`
  * Badge superior: `PERDIDAS`
  * Título: `Catalogo de Motivos de Perdida`
  * Resumen: `Catalogo oficial para tipificacion de desistimientos comerciales en el CRM.`
  * Tabla formal (tipografía Serif, bordes rectangulares) con columnas:
    * `ID MOTIVO`: #1, #2, #3, #4
    * `CAUSA DE DESISTIMIENTO / PERDIDA`:
      * `#1`: **Factor económico**
      * `#2`: **Horario no disponible**
      * `#3`: **Ya no interesado**
      * `#4`: **Sin respuesta**
* **Consulta SQL para Comprobar en PostgreSQL:**
  ```sql
  SELECT id_motivo, nombre FROM motivos_perdida ORDER BY id_motivo ASC;
  ```

---

### 🔹 PRUEBA 5.2: `MARCARPERDIDO[...]` (Clasificación de Desistimiento en Etapa 6)

#### Caso A: Tipificación Exitosa de Pérdida (3 Parámetros)
* **Objetivo:** Mover formalmente un prospecto a la Etapa 6 (*Perdido*), registrando el código del motivo, la justificación cualitativa y el evento correspondiente en la bitácora institucional.
* **Remitente Requerido (`From:`):** `admin@crm.bo` o el vendedor titular asignado al prospecto.
* **Asunto a Enviar (Exacto):** *(Reemplazar con un ID de lead existente, ej. #14)*
  ```text
  MARCARPERDIDO["14", "1", "El arancel excede la capacidad presupuestaria actual del interesado"]
  ```
* **Asunto de Respuesta del Correo:**
  ```text
  RE: [CRM-EDUCATIVO] Lead #14 Clasificado como Perdido
  ```
* **Contenido HTML que Recibirás en tu Bandeja:**
  * Cabecera institucional azul marino
  * Tarjeta formal con etiqueta verde: `[EXITO]`
  * Título: `Prospecto Clasificado como Perdido`
  * Mensaje: `Se actualizo el expediente comercial y se registro la causa en bitacora.`
  * Ficha rectangular con campos:
    * **Prospecto:** Mario Becerra (Lead #14)
    * **Etapa Anterior:** `[2] En Contacto` *(o la etapa previa)*
    * **Nueva Etapa:** `Perdido` *(Badge rectangular rojo)*
    * **Causa Tipificada:** `#1 - Factor económico`
    * **Justificación:** El arancel excede la capacidad presupuestaria actual del interesado
    * **Responsable:** Nataly Valenzuela (Administrador)
* **Consulta SQL para Comprobar en PostgreSQL:**
  ```sql
  -- 1. Verificar la etapa 6 y causa registrada en el lead
  SELECT id_lead, nombre, id_etapa, id_motivo_perdida, comentario_perdida, updated_at 
  FROM leads WHERE id_lead = 14;

  -- 2. Verificar el hito cronológico en la bitácora
  SELECT id_evento, id_lead, id_usuario, tipo, id_etapa_anterior, id_etapa_nueva, descripcion, created_at 
  FROM bitacora 
  WHERE id_lead = 14 AND tipo = 'PERDIDA' 
  ORDER BY id_evento DESC LIMIT 1;
  ```

#### Caso B: Rechazo por Comentario Justificativo Vacío
* **Objetivo:** Demostrar que el CRM no permite dar de baja a un lead sin dejar una explicación para los reportes analíticos de gerencia.
* **Asunto a Enviar:**
  ```text
  MARCARPERDIDO["14", "1", ""]
  ```
* **Contenido HTML que Recibirás:**
  ```text
  [ERROR] El comentario explicativo no puede estar vacio.
  Sugerencia: Detalle las razones expuestas por el prospecto para alimentar los informes gerenciales.
  ```

#### Caso C: Rechazo por ID de Motivo Inexistente
* **Asunto a Enviar:**
  ```text
  MARCARPERDIDO["14", "99", "Causa desconocida"]
  ```
* **Contenido HTML que Recibirás:**
  ```text
  [ERROR] El ID de motivo #99 no existe.
  Sugerencia: Motivos validos: #1 (Factor económico), #2 (Horario no disponible), #3 (Ya no interesado), #4 (Sin respuesta). Ejecute LISTARMOTIVOSPERDIDA[] para consultar el catalogo.
  ```

#### Caso D: Rechazo por Cartera Comercial (Vendedor no asignado)
* **Objetivo:** Un vendedor solo puede marcar como perdido a un prospecto que pertenezca a su cartera.
* **Remitente (`From:`):** `vendedor2@crm.bo` (intentando marcar un lead de `vendedor1@crm.bo`)
* **Asunto a Enviar:**
  ```text
  MARCARPERDIDO["14", "1", "El cliente no respondio"]
  ```
* **Contenido HTML que Recibirás:**
  ```text
  [ERROR] Acceso denegado: El prospecto #14 no pertenece a su cartera comercial.
  Sugerencia: Solo puede gestionar el desistimiento de prospectos asignados a su cuenta.
  ```

---

### 🔹 PRUEBA 5.3: `LISTARPERDIDOS[]` (Reporte de Prospectos en Etapa 6)

#### Caso A: Consulta Consolidada Institucional (Administrador / Coordinador)
* **Objetivo:** Visualizar todos los prospectos clasificados en la Etapa 6 con su causa tipificada y justificación.
* **Remitente Requerido (`From:`):** `admin@crm.bo`
* **Asunto a Enviar (Exacto):**
  ```text
  LISTARPERDIDOS[]
  ```
* **Asunto de Respuesta del Correo:**
  ```text
  RE: [CRM-EDUCATIVO] Prospectos Perdidos (X)
  ```
* **Contenido HTML que Recibirás en tu Bandeja:**
  * Badge superior: `PERDIDAS`
  * Título: `Listado de Prospectos Perdidos`
  * Resumen: `Alcance: Consolidado institucional | Total en Etapa Perdido: X prospectos`
  * Tabla formal con columnas:
    * `ID LEAD`: #ID
    * `PROSPECTO`: Nombre completo en negrita
    * `TELEFONO`: Número de contacto
    * `VENDEDOR`: Titular responsable o Bolsa Común
    * `CAUSA DE PERDIDA`: Nombre del motivo (ej. Factor económico)
    * `JUSTIFICACION`: Comentario registrado al momento del desistimiento
    * `FECHA`: Timestamp exacto de la baja
* **Consulta SQL para Comprobar en PostgreSQL:**
  ```sql
  SELECT l.id_lead, l.nombre, l.telefono, 
         COALESCE(u.nombre_completo, 'Bolsa Comun') AS vendedor,
         m.nombre AS motivo, l.comentario_perdida, l.updated_at
  FROM leads l
  LEFT JOIN usuarios u ON l.id_vendedor = u.id_usuario
  JOIN motivos_perdida m ON l.id_motivo_perdida = m.id_motivo
  WHERE l.id_etapa = 6
  ORDER BY l.updated_at DESC;
  ```

#### Caso B: Filtrado Automático para Remitente Vendedor
* **Objetivo:** Si un vendedor envía `LISTARPERDIDOS[]`, el sistema muestra únicamente los prospectos que se perdieron en su cartera.
* **Remitente Requerido (`From:`):** `vendedor1@crm.bo`
* **Asunto a Enviar:** `LISTARPERDIDOS[]`
* **Contenido HTML que Recibirás:**
  * Resumen: `Alcance: Cartera personal de Nicol Flores | Total en Etapa Perdido: Y prospectos`.

---

## ⚡ 3. Resumen Rápido de Comandos para la Defensa (CU5)

| Comando | Parámetros | Rol Autorizado | Acción Clave |
|---|---|---|---|
| `LISTARMOTIVOSPERDIDA[]` | Ninguno | Personal (Admin, Coord, Vend) | Consulta catálogo de causas |
| `MARCARPERDIDO["id_lead","id_motivo","comentario"]` | 3 | Titular / Admin / Coord | Clasifica en Etapa 6 con bitácora |
| `LISTARPERDIDOS[]` | Ninguno | Personal del CRM | Reporte nominal de prospectos perdidos |

---

## 🎓 4. Preguntas Típicas del Docente y Respuestas Técnicas

### ❓ P1: "¿Por qué se prohíbe pasar a Etapa 6 directamente mediante `MOVERETAPA`?"
> **Respuesta:** Porque en una gestión de CRM profesional, clasificar a un interesado como "Perdido" sin especificar por qué se perdió arruina las métricas de la institución. Si permitiéramos mover a la columna 6 sin motivo, la gerencia no sabría si perdimos alumnos por precio, por horario, por mala atención del vendedor o por desinterés. En `PipelineService.java` interceptamos la transición `idNuevaEtapa == 6` y obligamos a usar `MARCARPERDIDO` que exige el `id_motivo` foráneo y un `comentario` explicativo.

### ❓ P2: "¿Dónde se almacena la causa y cómo se evita que un vendedor la sobreescriba después?"
> **Respuesta:** Se almacena en las columnas `id_motivo_perdida` y `comentario_perdida` de la tabla `leads`, y simultáneamente se crea una entrada inmutable en la tabla `bitacora` (`tipo = 'PERDIDA'`, `id_etapa_anterior`, `id_etapa_nueva = 6`, `id_usuario`). Aunque se intentara mover de nuevo, la traza histórica en la bitácora queda intacta con timestamp y responsable.
