# 🎯 GUÍA DE PRUEBA MANUAL PASO A PASO: CASO DE USO 2 (CU2 - CAPTACIÓN Y OFERTA ACADÉMICA)
### INF513 - Tecnología Web | Grupo 05 SC | Docente: Msc. Ing. Evans Balcázar Veizaga
### Responsable Oficial: Jael (Dev C)

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
3. En **Terminal 3**, abre `psql` para verificar la persistencia en tiempo real:
   ```bash
   psql -U postgres -d crm_educativo
   ```
4. Envía los correos de prueba hacia la cuenta oficial:
   **Destinatario:** `grupo05sc@tecnoweb.org.bo`

---

## 📋 2. Banco Exhaustivo de Pruebas Comando por Comando (CU2)

---

### 🔹 PRUEBA 2.1: `REGISTRARPROSPECTO[...]` (Captación de Prospecto)

#### Caso A: Registro Exitoso Completo (5 Parámetros)
* **Objetivo:** Dar de alta a un interesado en el CRM, ubicarlo en la Etapa 1 (*Nuevo*) y dejarlo en la **Bolsa Común** (`id_vendedor = NULL`).
* **Remitente Requerido (`From:`):** Cualquier correo (Personal institucional o el propio interesado).
* **Asunto a Enviar (Exacto):**
  ```text
  REGISTRARPROSPECTO["Mario Becerra", "70011223", "mario.becerra@gmail.com", "Facebook", "Ingeniería de Sistemas"]
  ```
* **Asunto de Respuesta del Correo:**
  ```text
  RE: [CRM-EDUCATIVO] Prospecto #(ID_GENERADO) Registrado
  ```
* **Contenido HTML que Recibirás en tu Bandeja:**
  * Cabecera institucional azul marino: `CRM EDUCATIVO UNEXO`
  * Tarjeta formal con título: `Captacion de Prospecto` y etiqueta `[EXITO]`
  * Mensaje: `Prospecto registrado satisfactoriamente en el CRM.`
  * Ficha rectangular con campos:
    * **ID Prospecto:** #(ID_GENERADO)
    * **Nombre Completo:** Mario Becerra
    * **Teléfono de Contacto:** 70011223
    * **Correo Electrónico:** `mario.becerra@gmail.com`
    * **Canal de Origen:** Facebook
    * **Área / Profesión:** Ingeniería de Sistemas / Software
    * **Etapa Inicial:** `Nuevo` *(Badge azul)*
    * **Estado de Asignación:** `Bolsa Comun (Pendiente de Vendedor)`
* **Consulta SQL para Comprobar en PostgreSQL:**
  ```sql
  SELECT id_lead, nombre, telefono, correo, id_origen, id_etapa, id_vendedor, id_profesion, created_at 
  FROM leads WHERE correo = 'mario.becerra@gmail.com';
  ```
  *(Verifica que `id_etapa = 1` y `id_vendedor IS NULL`)*.
* **Comprobación de Auditoría en Bitácora:**
  ```sql
  SELECT id_evento, id_lead, tipo, descripcion, id_etapa_nueva, created_at 
  FROM bitacora WHERE id_lead = (SELECT id_lead FROM leads WHERE correo = 'mario.becerra@gmail.com');
  ```
  *(Verifica que se insertó el evento de tipo `CREACION`: "Prospecto captado e ingresado a Bolsa Comun")*.

#### Caso B: Registro Mínimo Obligatorio (Solo Nombre y Teléfono)
* **Asunto a Enviar:**
  ```text
  REGISTRARPROSPECTO["Claudia Mendez", "77334455"]
  ```
* **Contenido HTML que Recibirás:**
  * Alta exitosa. Asigna automáticamente el canal de origen por defecto (`Web`) y profesión `No especificada`.

#### Caso C: Rechazo por Parámetros Insuficientes (Manejo de Error)
* **Asunto a Enviar:**
  ```text
  REGISTRARPROSPECTO["SoloNombreSinTelefono"]
  ```
* **Contenido HTML que Recibirás:**
  ```text
  [ERROR] Parametros insuficientes. Se requiere como minimo el nombre y telefono del prospecto.
  Sugerencia: Sintaxis: REGISTRARPROSPECTO["nombre", "telefono", "correo", "origen", "profesion"].
  ```

#### Caso D: Rechazo por Formato de Correo Inválido
* **Asunto a Enviar:**
  ```text
  REGISTRARPROSPECTO["Carlos", "70099881", "correo_sin_arroba", "Web", "Auditor"]
  ```
* **Contenido HTML que Recibirás:**
  ```text
  [ERROR] El formato del correo electronico 'correo_sin_arroba' es invalido.
  Sugerencia: Ingrese un correo valido o deje el campo vacio "" si no dispone de correo.
  ```

---

### 🔹 PRUEBA 2.2: `VINCULARINTERES[...]` (Vinculación de Oferta Académica)

#### Caso A: Vinculación Exitosa de un Programa Académico (`PROGRAMA`)
* **Remitente (`From:`):** `admin@crm.bo` *(o cualquier personal registrado)*.
* **Asunto a Enviar:** *(Reemplazar el primer parámetro con el ID del lead captado)*
  ```text
  VINCULARINTERES["11", "PROGRAMA", "101", "Diplomado en Ciberseguridad Defensiva"]
  ```
* **Asunto de Respuesta:**
  ```text
  RE: [CRM-EDUCATIVO] Interes Vinculado a Lead #11
  ```
* **Contenido HTML que Recibirás:**
  ```text
  [EXITO] Operación Ejecutada Correctamente
  El interes fue guardado en el expediente del prospecto y registrado en bitacora.

  • ID Registro Interés: #(ID_INTERES)
  • Prospecto Beneficiario: Mario Becerra (Lead #11)
  • Tipo de Oferta: PROGRAMA
  • ID Catálogo Externo: #101
  • Programa / Módulo: Diplomado en Ciberseguridad Defensiva
  ```
* **Consulta SQL para Comprobar en PostgreSQL:**
  ```sql
  SELECT id_interes, id_lead, tipo, id_version_externo, id_modulo_externo, nombre_snapshot 
  FROM lead_interes WHERE id_lead = 11;
  ```
  *(Verifica que `tipo = 'PROGRAMA'`, `id_version_externo = 101` y `id_modulo_externo IS NULL`)*.

#### Caso B: Vinculación Exitosa de un Módulo Individual (`MODULO`)
* **Asunto a Enviar:**
  ```text
  VINCULARINTERES["11", "MODULO", "205", "Modulo 1: Analisis Forense Digital"]
  ```
* **Consulta SQL para Comprobar en PostgreSQL:**
  ```sql
  SELECT * FROM lead_interes WHERE id_lead = 11 AND tipo = 'MODULO';
  ```
  *(Verifica que `tipo = 'MODULO'`, `id_modulo_externo = 205` y `id_version_externo IS NULL`)*.

#### Caso C: Rechazo por Tipo Inválido (Restricción Check de BD)
* **Asunto a Enviar:**
  ```text
  VINCULARINTERES["11", "TALLER", "50", "Taller de Hacking"]
  ```
* **Contenido HTML que Recibirás:**
  ```text
  [ERROR] El tipo de oferta 'TALLER' es invalido.
  Sugerencia: Los tipos permitidos son: 'PROGRAMA' o 'MODULO'.
  ```

#### Caso D: Rechazo por Prospecto Inexistente
* **Asunto a Enviar:**
  ```text
  VINCULARINTERES["99999", "PROGRAMA", "101", "Diplomado"]
  ```
* **Contenido HTML que Recibirás:**
  ```text
  [ERROR] No existe ningun prospecto registrado con el ID #99999.
  ```

---

### 🔹 PRUEBA 2.3: `LISTARINTERESES[...]` (Consulta de Cursos de un Lead)
* **Remitente (`From:`):** `admin@crm.bo`
* **Asunto a Enviar:**
  ```text
  LISTARINTERESES["11"]
  ```
* **Asunto de Respuesta:**
  ```text
  RE: [CRM-EDUCATIVO] Intereses Lead #11
  ```
* **Contenido HTML que Recibirás:**
  * Tabla formal rectangular:
    `# | TIPO | ID EXTERNO | CURSO / PROGRAMA DE INTERÉS`
    * Fila 1: `1 | PROGRAMA | #101 | Diplomado en Ciberseguridad Defensiva`
    * Fila 2: `2 | MODULO | #205 | Modulo 1: Analisis Forense Digital`

---

### 🔹 PRUEBA 2.4: `LISTARORIGENES[]` (Catálogo de Canales de Captación)
* **Remitente (`From:`):** `admin@crm.bo`
* **Asunto a Enviar:**
  ```text
  LISTARORIGENES[]
  ```
* **Asunto de Respuesta:**
  ```text
  RE: [CRM-EDUCATIVO] Canales de Origen
  ```
* **Contenido HTML que Recibirás:**
  * Tabla formal:
    `ID ORIGEN | CANAL DE CAPTACIÓN`
    * `1 | Facebook`
    * `2 | WhatsApp`
    * `3 | Web`
    * `4 | Referido`
    * `5 | Pinterest`

---

### 🔹 PRUEBA 2.5: `LISTARPROFESIONES[]` (Catálogo de Áreas Académicas)
* **Remitente (`From:`):** `admin@crm.bo`
* **Asunto a Enviar:**
  ```text
  LISTARPROFESIONES[]
  ```
* **Asunto de Respuesta:**
  ```text
  RE: [CRM-EDUCATIVO] Catalogo de Profesiones
  ```
* **Contenido HTML que Recibirás:**
  * Tabla formal con las profesiones registradas en PostgreSQL:
    * `1 | Ingeniería de Sistemas / Software`
    * `2 | Ingeniería Informática / Redes`
    * `3 | Licenciatura en Administración de Empresas`
    * `4 | Contaduría Pública / Auditoría`
    * `...`

---

## 🎯 Resumen de Comprobaciones Rápidas para la Defensa

| Comando | Asunto Enviado | Código | Respuesta Visible en Correo | Comprobación en BD |
| :--- | :--- | :---: | :--- | :--- |
| `REGISTRARPROSPECTO` | `REGISTRARPROSPECTO["Mario","70011","mario@gm.com","Facebook","Sistemas"]` | `250 OK` | `[EXITO]` Lead creado en Bolsa Común | `SELECT * FROM leads WHERE correo = ...;` |
| `REGISTRARPROSPECTO` | `REGISTRARPROSPECTO["SinTelf"]` *(faltan datos)* | `250 OK` | `[ERROR]` Parámetros insuficientes | Sin cambios en BD |
| `VINCULARINTERES` | `VINCULARINTERES["11","PROGRAMA","101","Diplomado"]` | `250 OK` | `[EXITO]` Interés vinculado a lead | `SELECT * FROM lead_interes;` |
| `VINCULARINTERES` | `VINCULARINTERES["11","TALLER",...]` *(tipo inválido)* | `250 OK` | `[ERROR]` Solo PROGRAMA o MODULO | Check constraint de BD intacta |
| `LISTARINTERESES` | `LISTARINTERESES["11"]` | `250 OK` | Tabla HTML con cursos del lead | Consulta a `lead_interes` |
| `LISTARORIGENES` | `LISTARORIGENES[]` | `250 OK` | Tabla HTML de canales de captación | `SELECT * FROM origenes_lead;` |
| `LISTARPROFESIONES` | `LISTARPROFESIONES[]` | `250 OK` | Tabla HTML de profesiones | `SELECT * FROM profesiones;` |
