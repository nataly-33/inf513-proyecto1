# 🎯 GUÍA DE PRUEBA MANUAL PASO A PASO: CASO DE USO 2 (CU2 - CAPTACIÓN Y OFERTA ACADÉMICA)
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

#### Caso B: Registro Mínimo Obligatorio (Solo Nombre y Teléfono)
* **Asunto a Enviar:**
  ```text
  REGISTRARPROSPECTO["Claudia Mendez", "77334455"]
  ```
* **Contenido HTML que Recibirás:**
  * Alta exitosa. Asigna automáticamente el canal de origen por defecto (`Web`) y profesión `No especificada`.

#### Caso C: Rechazo por Teléfono Móvil Inválido (Regla Nacional Bolivia)
* **Objetivo:** Demostrar que los números celulares de 8 dígitos deben iniciar obligatoriamente con 6 o 7, y los fijos de 7 dígitos con 2, 3 o 4.
* **Asunto a Enviar:**
  ```text
  REGISTRARPROSPECTO["Pedro", "80011223", "pedro@gmail.com", "Facebook", "Sistemas"]
  ```
* **Contenido HTML que Recibirás:**
  ```text
  [ERROR] El numero celular de 8 digitos '80011223' es invalido.
  Sugerencia: En Bolivia los telefonos celulares deben iniciar con 6 o 7 (ej. 70011223, 68901234).
  ```

#### Caso D: Rechazo por Canal de Origen Inexistente
* **Objetivo:** Validar que el canal de captación exista en la tabla `origenes_lead` de la base de datos, instruyendo consultar los válidos.
* **Asunto a Enviar:**
  ```text
  REGISTRARPROSPECTO["Laura", "70099881", "laura@gmail.com", "TikTok", "Sistemas"]
  ```
* **Contenido HTML que Recibirás:**
  ```text
  [ERROR] El canal de origen 'TikTok' no es valido.
  Sugerencia: Origenes validos: Facebook, WhatsApp, Web, Referido, Pinterest. Ejecute el comando LISTARORIGENES[] para consultar el catalogo disponible.
  ```

#### Caso E: Rechazo por Formato de Correo Inválido
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

### 🔹 PRUEBA 2.2: `LISTAROFERTA[]` (Catálogo Oficial de Oferta Académica)

#### Caso A: Consulta General de Programas y Módulos
* **Objetivo:** Permitir a cualquier usuario o interesado consultar el catálogo oficial de postgrado con sus códigos, duración y arancel en Bolivianos antes de vincular un interés.
* **Remitente Requerido (`From:`):** Cualquier remitente.
* **Asunto a Enviar (Exacto):**
  ```text
  LISTAROFERTA[]
  ```
* **Asunto de Respuesta del Correo:**
  ```text
  RE: [CRM-EDUCATIVO] Oferta Academica (8 cursos)
  ```
* **Contenido HTML que Recibirás en tu Bandeja:**
  * Cabecera institucional azul marino
  * Badge superior: `CATALOGO`
  * Título: `Oferta Academica Disponible`
  * Tabla formal con columnas:
    * `ID`: #101, #102, #103, #104 (Programas) / #201, #202, #203, #204 (Módulos)
    * `TIPO`: PROGRAMA / MODULO
    * `CODIGO`: PROG-101, MOD-201, etc.
    * `PROGRAMA / MODULO`: Nombre completo del diplomado o curso
    * `DURACION`: Meses o semanas
    * `INVERSION (BS.)`: Costo oficial en Bolivianos

---

### 🔹 PRUEBA 2.3: `VINCULARINTERES[...]` (Vinculación de Oferta Académica)

#### Caso A: Vinculación Exitosa de un Programa Académico (`PROGRAMA`)
* **Remitente (`From:`):** `admin@crm.bo`, vendedor asignado al lead, **o el propio prospecto interesado**.
* **Asunto a Enviar (Exacto):**
  ```text
  VINCULARINTERES["14", "PROGRAMA", "101", "Diplomado en Ciberseguridad Defensiva"]
  ```
* **Asunto de Respuesta:**
  ```text
  RE: [CRM-EDUCATIVO] Interes Vinculado a Lead #14
  ```
* **Contenido HTML que Recibirás:**
  * Tarjeta formal con etiqueta `[EXITO]`
  * Título: `Interes Academico Vinculado`
  * Ficha con campos:
    * **ID Registro Interés:** #(ID_GENERADO)
    * **Prospecto Beneficiario:** Mario Becerra (Lead #14)
    * **Tipo de Oferta:** PROGRAMA
    * **ID Catálogo Externo:** #101
    * **Programa / Módulo:** Diplomado en Ciberseguridad Defensiva y Peritaje Forense
* **Consulta SQL para Comprobar en PostgreSQL:**
  ```sql
  SELECT id_interes, id_lead, tipo, id_version_externo, id_modulo_externo, nombre_snapshot 
  FROM lead_interes WHERE id_lead = 14;
  ```

#### Caso B: Rechazo por ID Inexistente en Oferta Académica
* **Asunto a Enviar:**
  ```text
  VINCULARINTERES["14", "PROGRAMA", "999", "Curso Inventado"]
  ```
* **Contenido HTML que Recibirás:**
  ```text
  [ERROR] El ID externo #999 no corresponde a ningun programa disponible en la oferta academica.
  Sugerencia: PROGRAMAs disponibles: #101 'Diplomado en Ciberseguridad...', #102 'Maestria en Gestion...', etc. Ejecute el comando LISTAROFERTA[] o HELP["VINCULARINTERES"] para consultar codigos.
  ```

---

### 🔹 PRUEBA 2.4: `LISTARINTERESES[...]` (Expediente de Cursos de Interés)

#### Caso A: Consulta Nominal de Intereses del Prospecto
* **Asunto a Enviar:**
  ```text
  LISTARINTERESES["14"]
  ```
* **Contenido HTML que Recibirás:**
  * Tabla con el listado de todos los diplomados y módulos vinculados al lead.
* **Consulta SQL:**
  ```sql
  SELECT id_interes, tipo, COALESCE(id_version_externo, id_modulo_externo) AS id_curso, nombre_snapshot 
  FROM lead_interes WHERE id_lead = 14;
  ```

---

### 🔹 PRUEBA 2.5: `LISTARORIGENES[]` y `LISTARPROFESIONES[]`

#### Caso A: Consulta del Catálogo de Canales
* **Asunto a Enviar:** `LISTARORIGENES[]`
* **Contenido HTML que Recibirás:** Tabla con Facebook, WhatsApp, Web, Referido, Pinterest.

#### Caso B: Consulta del Catálogo de Profesiones
* **Asunto a Enviar:** `LISTARPROFESIONES[]`
* **Contenido HTML que Recibirás:** Tabla con Ingeniería de Sistemas, Auditoría, Derecho, etc.

---

## ⚡ 3. Resumen Rápido de Comandos para la Defensa (CU2)

| Comando | Parámetros | Rol Autorizado | Acción Clave |
|---|---|---|---|
| `REGISTRARPROSPECTO[...]` | 2 a 5 | Todos (Público / Personal) | Alta en Etapa 1 (Nuevo) y Bolsa Común |
| `LISTAROFERTA[]` | Ninguno | Todos (Público / Personal) | Catálogo de diplomados y módulos con precios |
| `VINCULARINTERES[...]` | 4 | Titular / Admin / El Prospecto | Asocia curso respetando chk_tipo_interes |
| `LISTARINTERESES["id"]` | 1 | Personal del CRM | Consulta cursos de interés del prospecto |
| `LISTARORIGENES[]` | Ninguno | Personal del CRM | Catálogo de canales de captación |
| `LISTARPROFESIONES[]` | Ninguno | Personal del CRM | Catálogo de áreas profesionales |

---

## 🎓 4. Preguntas Típicas del Docente y Respuestas Técnicas

### ❓ P1: "¿Cómo validan que el teléfono y el correo sean correctos al captar un lead?"
> **Respuesta:** En `LeadService.java`, el correo se valida con una expresión regular RFC 5322 (`EMAIL_PATTERN`). Para el teléfono, se remueven caracteres de formato y se comprueba que tenga 7 u 8 dígitos numéricos: si tiene 8 dígitos (celular) debe iniciar estrictamente con 6 o 7 según el Plan Nacional de Numeración de Bolivia; si tiene 7 dígitos (fijo), debe iniciar con 2 (La Paz), 3 (Santa Cruz) o 4 (Cochabamba).

### ❓ P2: "¿Quién puede ejecutar `VINCULARINTERES`? ¿Puede hacerlo el propio prospecto?"
> **Respuesta:** Sí. El sistema implementa doble vía de acceso:
> 1. **Vía Administrativa/Comercial:** Si el remitente es un usuario registrado con rol Administrador, Coordinador o el Vendedor asignado al lead, se le permite gestionar el expediente.
> 2. **Vía Autoservicio del Cliente:** Si el remitente no es usuario interno, se valida si su correo (`auth.getEmail()`) coincide con el correo registrado del prospecto (`lead.getCorreo()`). De ser así, el interesado tiene plena potestad de autoasociarse los diplomados o módulos que desea cursar.
