# 🎯 GUÍA DE PRUEBA MANUAL PASO A PASO: CASO DE USO 6 (CU6 - PAGOS Y CONVERSIÓN A MATRÍCULA)
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
3. En **Terminal 3**, abre `psql` para verificar la persistencia y estados contables:
   ```bash
   psql -U postgres -d crm_educativo
   ```
4. Envía los correos de prueba hacia la cuenta oficial:
   **Destinatario:** `grupo05sc@tecnoweb.org.bo`

---

## 📋 2. Banco Exhaustivo de Pruebas Comando por Comando (CU6)

---

### 🔹 PRUEBA 6.1: `REGISTRARCOMPROBANTE[...]` (Registro de Depósito Bancario)

#### Caso A: Registro Exitoso de Comprobante (4 Parámetros)
* **Objetivo:** Registrar un comprobante de transferencia o depósito bancario para un prospecto, ingresándolo en estado inicial `PENDIENTE` y avanzando al lead automáticamente a la Etapa 4 (*Promesa de Pago*).
* **Remitente Requerido (`From:`):** `admin@crm.bo` o el vendedor asignado al prospecto.
* **Asunto a Enviar (Exacto):** *(Reemplazar con un ID de lead existente, ej. #14)*
  ```text
  REGISTRARCOMPROBANTE["14", "Banco Mercantil Santa Cruz", "TR-982104", "2500.00"]
  ```
* **Asunto de Respuesta del Correo:**
  ```text
  RE: [CRM-EDUCATIVO] Comprobante #(ID_COMPROBANTE) Registrado para Lead #14
  ```
* **Contenido HTML que Recibirás en tu Bandeja:**
  * Cabecera institucional azul marino: `CRM EDUCATIVO UNEXO`
  * Tarjeta formal con etiqueta verde: `[EXITO]`
  * Título: `Comprobante de Pago Registrado`
  * Mensaje: `El comprobante fue ingresado a la cola de verificacion contable y registrado en bitacora.`
  * Ficha rectangular con campos:
    * **ID Comprobante:** #(ID_COMPROBANTE)
    * **Prospecto Beneficiario:** Mario Becerra (Lead #14)
    * **Entidad Financiera:** Banco Mercantil Santa Cruz
    * **Nro. de Operación:** TR-982104
    * **Monto Declarado:** 2500.00 Bs.
    * **Estado de Revisión:** `[PENDIENTE DE APROBACION]` *(Color ámbar)*
    * **Etapa Comercial:** `Promesa de Pago` *(Badge cian/celeste)*
* **Consulta SQL para Comprobar en PostgreSQL:**
  ```sql
  -- 1. Verificar el comprobante insertado
  SELECT id_comprobante, id_lead, subido_por, archivo_url, monto, estado, created_at 
  FROM comprobantes_pago WHERE id_lead = 14 ORDER BY id_comprobante DESC LIMIT 1;

  -- 2. Verificar que el lead avanzó a Etapa 4 (Promesa de Pago)
  SELECT id_lead, nombre, id_etapa FROM leads WHERE id_lead = 14;

  -- 3. Verificar el hito en la bitácora
  SELECT id_evento, id_lead, id_usuario, tipo, descripcion, created_at 
  FROM bitacora WHERE id_lead = 14 AND tipo = 'PAGO' ORDER BY id_evento DESC LIMIT 1;
  ```

#### Caso B: Rechazo por Monto Negativo o Inválido
* **Asunto a Enviar:**
  ```text
  REGISTRARCOMPROBANTE["14", "Banco Union", "112233", "-500"]
  ```
* **Contenido HTML que Recibirás:**
  ```text
  [ERROR] El monto especificado '-500' es invalido.
  Sugerencia: Debe ser un valor numerico positivo en Bolivianos (ej. 2500.00).
  ```

---

### 🔹 PRUEBA 6.2: `PAGOSPENDIENTES[]` (Cola Contable de Verificación)

#### Caso A: Consulta Exitosa de Comprobantes Pendientes (Exclusivo Administrador)
* **Objetivo:** Visualizar todos los depósitos bancarios que esperan verificación y aprobación por parte de la administración.
* **Remitente Requerido (`From:`):** `admin@crm.bo`
* **Asunto a Enviar (Exacto):**
  ```text
  PAGOSPENDIENTES[]
  ```
* **Asunto de Respuesta del Correo:**
  ```text
  RE: [CRM-EDUCATIVO] Pagos Pendientes (X)
  ```
* **Contenido HTML que Recibirás en tu Bandeja:**
  * Badge superior: `PAGOS`
  * Título: `Cola de Comprobantes de Pago Pendientes`
  * Resumen: `Comprobantes bancarios en cola de aprobacion contable: X`
  * Tabla formal con columnas:
    * `ID`: #ID Comprobante
    * `PROSPECTO`: Nombre del lead (#ID)
    * `VENDEDOR`: Usuario que registró el depósito
    * `BANCO Y REFERENCIA`: Entidad bancaria y número de operación
    * `MONTO (BS.)`: Monto en Bolivianos
    * `FECHA REGISTRO`: Timestamp de carga
* **Consulta SQL para Comprobar en PostgreSQL:**
  ```sql
  SELECT c.id_comprobante, l.nombre AS lead, c.archivo_url, c.monto, c.estado, c.created_at
  FROM comprobantes_pago c
  JOIN leads l ON c.id_lead = l.id_lead
  WHERE c.estado = 'PENDIENTE'
  ORDER BY c.created_at ASC;
  ```

#### Caso B: Bloqueo de Acceso a Vendedores (RBAC)
* **Remitente (`From:`):** `vendedor1@crm.bo`
* **Asunto a Enviar:** `PAGOSPENDIENTES[]`
* **Contenido HTML que Recibirás:**
  ```text
  [ERROR] Acceso denegado: La revision contable de pagos es exclusiva para el Administrador del sistema.
  Sugerencia: Su rol actual es 'Vendedor'.
  ```

---

### 🔹 PRUEBA 6.3: `APROBARPAGO[...]` (Aprobación Contable del Depósito)

#### Caso A: Aprobación Exitosa (Exclusivo Administrador)
* **Objetivo:** Validar y marcar como `APROBADO` un comprobante bancario, habilitando la posterior matriculación del alumno.
* **Remitente Requerido (`From:`):** `admin@crm.bo`
* **Asunto a Enviar (Exacto):** *(Reemplazar con el ID del comprobante generado en la prueba 6.1)*
  ```text
  APROBARPAGO["1"]
  ```
* **Asunto de Respuesta del Correo:**
  ```text
  RE: [CRM-EDUCATIVO] Comprobante #1 Aprobado
  ```
* **Contenido HTML que Recibirás en tu Bandeja:**
  * Tarjeta formal con etiqueta `[EXITO]`
  * Título: `Pago Aprobado Satisfactoriamente`
  * Mensaje: `El comprobante fue validado con exito y habilita la matriculacion formal.`
  * Ficha rectangular con campos:
    * **Comprobante Aprobado:** #1
    * **Prospecto:** Mario Becerra (Lead #14)
    * **Monto Verificado:** 2500.00 Bs.
    * **Detalle Bancario:** Banco Mercantil Santa Cruz - Transacción #TR-982104
    * **Aprobado Por:** Nataly Valenzuela (Administrador)
    * **Nuevo Estado:** `[APROBADO]` *(Color verde institucional)*
* **Consulta SQL para Comprobar en PostgreSQL:**
  ```sql
  SELECT id_comprobante, id_lead, monto, estado, revisado_por, fecha_revision 
  FROM comprobantes_pago WHERE id_comprobante = 1;
  ```

#### Caso B: Rechazo por Comprobante Ya Procesado
* **Asunto a Enviar:** `APROBARPAGO["1"]` *(enviarlo nuevamente)*
* **Contenido HTML que Recibirás:**
  ```text
  [ERROR] El comprobante #1 ya fue procesado anteriormente.
  Sugerencia: Estado actual del comprobante: APROBADO.
  ```

---

### 🔹 PRUEBA 6.4: `RECHAZARPAGO[...]` (Rechazo por Inconsistencia)

#### Caso A: Rechazo Formal de Comprobante con Observación
* **Objetivo:** Si la transferencia no figura en el extracto bancario o el monto no coincide, el administrador rechaza el comprobante indicando el motivo.
* **Remitente Requerido (`From:`):** `admin@crm.bo`
* **Asunto a Enviar (Exacto):**
  ```text
  RECHAZARPAGO["2", "El numero de transferencia no figura en el extracto bancario del dia"]
  ```
* **Asunto de Respuesta del Correo:**
  ```text
  RE: [CRM-EDUCATIVO] Comprobante #2 Rechazado
  ```
* **Contenido HTML que Recibirás en tu Bandeja:**
  * Badge superior: `AVISO`
  * Título: `Comprobante de Pago Rechazado`
  * Detalle con motivo y revisor responsable.
* **Consulta SQL para Comprobar en PostgreSQL:**
  ```sql
  SELECT id_comprobante, estado, comentario_rechazo, revisado_por, fecha_revision 
  FROM comprobantes_pago WHERE id_comprobante = 2;
  ```

---

### 🔹 PRUEBA 6.5: `COMPLETARDATOSALUMNO[...]` (Expediente Académico Formal)

#### Caso A: Registro Exitoso de Datos de Matrícula (4 Parámetros)
* **Objetivo:** Registrar cédula de identidad, fecha de nacimiento y ciudad de residencia, completando el expediente exigido para matricular al alumno.
* **Remitente Requerido (`From:`):** `admin@crm.bo` o vendedor titular.
* **Asunto a Enviar (Exacto):**
  ```text
  COMPLETARDATOSALUMNO["14", "8291043 SC", "1998-05-14", "Santa Cruz de la Sierra"]
  ```
* **Asunto de Respuesta del Correo:**
  ```text
  RE: [CRM-EDUCATIVO] Datos de Alumno Registrados Lead #14
  ```
* **Contenido HTML que Recibirás en tu Bandeja:**
  * Tarjeta formal con etiqueta `[EXITO]`
  * Título: `Datos Academicos Registrados`
  * Ficha con campos:
    * **Prospecto:** Mario Becerra (Lead #14)
    * **Documento de Identidad:** 8291043 SC
    * **Fecha de Nacimiento:** 1998-05-14
    * **Ciudad de Residencia:** Santa Cruz de la Sierra
    * **Estado Expediente:** `[COMPLETO PARA MATRICULA]`
* **Consulta SQL para Comprobar en PostgreSQL:**
  ```sql
  SELECT id_lead, nombre, ci, fecha_nacimiento, ciudad, updated_at 
  FROM leads WHERE id_lead = 14;
  ```

#### Caso B: Rechazo por Formato de Fecha Inválido
* **Asunto a Enviar:**
  ```text
  COMPLETARDATOSALUMNO["14", "8291043 SC", "14/05/1998", "Santa Cruz"]
  ```
* **Contenido HTML que Recibirás:**
  ```text
  [ERROR] El formato de la fecha de nacimiento '14/05/1998' es invalido.
  Sugerencia: El formato requerido es YYYY-MM-DD (ejemplo: 1998-05-14).
  ```

---

### 🔹 PRUEBA 6.6: `CONVERTIRAMATRICULA[...]` (Formalización y Constancia Oficial)

#### Caso A: Conversión Exitosa a Matrícula (Etapa 5: Convertido)
* **Objetivo:** Habiendo verificado el pago aprobado y los datos completos del alumno, formaliza la matrícula, pasa al prospecto a la Etapa 5 (*Convertido*) y emite la **Constancia Oficial de Matrícula Académica**.
* **Remitente Requerido (`From:`):** `admin@crm.bo`
* **Asunto a Enviar (Exacto):**
  ```text
  CONVERTIRAMATRICULA["14"]
  ```
* **Asunto de Respuesta del Correo:**
  ```text
  RE: [CRM-EDUCATIVO] Matricula Oficial #14 Formalizada
  ```
* **Contenido HTML que Recibirás en tu Bandeja:**
  * Cabecera institucional azul marino
  * Badge superior: `MATRICULA`
  * Tarjeta formal de certificación académica:
    * **Título:** `CONSTANCIA OFICIAL DE MATRICULA ACADEMICA`
    * **Subtítulo:** Registro Oficial de Alumno Regular - Postgrado UNEXO
    * **Código de Matrícula:** `MATR-2026-14`
    * **Alumno Regular:** Mario Becerra
    * **Cédula de Identidad:** 8291043 SC
    * **Correo Electrónico:** `mario.becerra@gmail.com`
    * **Ciudad de Residencia:** Santa Cruz de la Sierra
    * **Comprobante Asociado:** Comprobante Aprobado #1
    * **Etapa Actual:** `Convertido` *(Badge verde oscuro institucional)*
    * **Autorizado Por:** Nataly Valenzuela (Administrador)
  * Nota de cierre: `El prospecto culmino exitosamente el embudo comercial y se encuentra matriculado como alumno regular.`
* **Consulta SQL para Comprobar en PostgreSQL:**
  ```sql
  -- 1. Verificar la etapa 5 en leads
  SELECT id_lead, nombre, id_etapa, updated_at FROM leads WHERE id_lead = 14;

  -- 2. Verificar el registro oficial en envios_matricula
  SELECT id_envio, id_lead, id_comprobante, payload, estado, created_at 
  FROM envios_matricula WHERE id_lead = 14;

  -- 3. Verificar la bitácora institucional
  SELECT id_evento, id_lead, id_usuario, tipo, id_etapa_anterior, id_etapa_nueva, descripcion, created_at 
  FROM bitacora WHERE id_lead = 14 AND tipo = 'MATRICULA_CONVERTIDA';
  ```

#### Caso B: Rechazo si el Prospecto NO tiene Pago Aprobado
* **Objetivo:** Demostrar que el sistema PROHÍBE matricular a un alumno sin un depósito bancario verificado por administración.
* **Asunto a Enviar:** *(Lead sin pago aprobado)*
  ```text
  CONVERTIRAMATRICULA["20"]
  ```
* **Contenido HTML que Recibirás:**
  ```text
  [ERROR] No se puede matricular al prospecto: No cuenta con ningun comprobante de pago aprobado.
  Sugerencia: Registre el comprobante con REGISTRARCOMPROBANTE y apruebelo formalmente con APROBARPAGO["id_comprobante"].
  ```

#### Caso C: Rechazo si Faltan Datos del Expediente (CI, Fecha o Ciudad)
* **Objetivo:** Demostrar que el sistema PROHÍBE matricular si faltan requisitos legales del alumno.
* **Asunto a Enviar:**
  ```text
  CONVERTIRAMATRICULA["21"]
  ```
* **Contenido HTML que Recibirás:**
  ```text
  [ERROR] No se puede matricular al prospecto: Faltan datos obligatorios en su expediente academico.
  Sugerencia: Asegurese de registrar CI, fecha de nacimiento y ciudad mediante el comando: COMPLETARDATOSALUMNO["21", "ci", "fecha_nac", "ciudad"].
  ```

---

## ⚡ 3. Resumen Rápido de Comandos para la Defensa (CU6)

| Comando | Parámetros | Rol Autorizado | Acción Clave |
|---|---|---|---|
| `REGISTRARCOMPROBANTE["id","banco","trans","monto"]`| 4 | Titular / Admin | Carga depósito (estado PENDIENTE, Etapa 4) |
| `PAGOSPENDIENTES[]` | Ninguno | Administrador | Lista cola contable por verificar |
| `APROBARPAGO["id_comp"]` | 1 | Administrador | Valida depósito y habilita matrícula |
| `RECHAZARPAGO["id_comp","motivo"]` | 2 | Administrador | Rechaza comprobante inconsistente |
| `COMPLETARDATOSALUMNO["id","ci","nac","ciudad"]` | 4 | Titular / Admin | Completa CI, fecha nac y ciudad |
| `CONVERTIRAMATRICULA["id_lead"]` | 1 | Administrador | Pasa a Etapa 5 (Convertido) y emite constancia |

---

## 🎓 4. Preguntas Típicas del Docente y Respuestas Técnicas

### ❓ P1: "¿Por qué no permitieron que `MOVERETAPA` pase directamente a Etapa 5 (Convertido)?"
> **Respuesta:** Porque en una universidad o escuela de postgrado, un vendedor no puede simplemente declarar a alguien "alumno matriculado" sin haber validado dos condiciones mandatorias: que el depósito bancario haya ingresado efectivamente a las cuentas de la institución (aprobado por finanzas/administración) y que el alumno haya presentado su cédula de identidad y datos legales para el registro de actas. Por eso `PipelineService.java` intercepta el paso a Etapa 5 y redirige a `CONVERTIRAMATRICULA`.

### ❓ P2: "¿Cómo garantiza la integridad transaccional al momento de matricular?"
> **Respuesta:** En `PagoMatriculaDao.java`, el método `formalizarMatricula()` opera bajo una única transacción de base de datos (`conn.setAutoCommit(false)`). En esa misma transacción se actualiza `leads` a `id_etapa = 5`, se inserta en `envios_matricula` con el payload JSON oficial y se crea el hito en `bitacora` (`MATRICULA_CONVERTIDA`). Si cualquiera de las sentencias falla, se ejecuta `DatabaseConnection.rollback(conn)` evitando estados inconsistentes.
