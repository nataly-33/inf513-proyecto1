# ⏱️ GUÍA OFICIAL: FLUJO COMPLETO DE DEFENSA EN 15 MINUTOS
### INF513 - Tecnología Web | Grupo 05 SC | Docente: Msc. Ing. Evans Balcázar Veizaga
### Sistema Automatizado vía Correo Electrónico – CRM Educativo UNEXO

---

## 🎯 1. Estrategia y Distribución del Tiempo en la Mesa de Examen

El Ing. Evans Balcázar evalúa de forma sumamente práctica, cronometrada y directa. El tiempo total asignado por grupo es de aproximadamente **20 minutos**:

| Minuto | Actividad | Objetivo de Evaluación | Responsable |
| :---: | :--- | :--- | :---: |
| **0 - 2** | **Revisión de Base de Datos y Documento** | El docente verifica que las tablas desplegadas en PostgreSQL coincidan con el diagrama del documento. | Todo el equipo |
| **2 - 5** | **Pitch Puntual (3 minutos)** | Explicar qué es la empresa (UNEXO - Centro de Capacitación y Postgrados), a qué se dedica y cómo este sistema automatiza el ciclo comercial vía email. | Nataly |
| **5 - 17** | **Demostración en Vivo vía Email (12 min)** | El docente envía correos desde su cuenta personal de Gmail hacia `grupo05sc@tecnoweb.org.bo` ejecutando el flujo comercial completo de punta a punta. | Equipo / Docente |
| **17 - 20** | **Preguntas y Modificación en Vivo** | Preguntas capciosas de bajo nivel o solicitud de ajuste rápido en código. | Todo el equipo |

---

## 🚀 2. El Flujo de Prueba Comercial Oficial (De Prospecto a Alumno Matriculado)

> 💡 **La Regla de Oro del Docente:**  
> Cuando el docente pregunte: *«¿Cuál es su flujo de prueba? ¿Con qué comando comenzamos y cuál es el siguiente?»*, respondan con seguridad:
> 
> *"Ingeniero, nuestro flujo representa el ciclo comercial completo de la institución: El sistema arranca con la verificación de comandos e identidad, luego un nuevo interesado se registra como prospecto y cae en la Bolsa Común, se le asigna un vendedor, avanza por el embudo Kanban comercial, el prospecto realiza su pago bancario, el administrador aprueba el comprobante y formaliza su matrícula emitiendo su constancia académica, impactando finalmente en los reportes estadísticos en tiempo real."*

---

### 📋 GUION DETALLADO PASO A PASO (COMANDO POR COMANDO)

#### PASO 0: Inicio y Verificación de Comandos (Minuto 5)
* **Actor / Remitente:** El Docente (`docente@uagrm.edu.bo` o su Gmail)
* **Asunto (Subject):**
  ```text
  HELP[]
  ```
* **Acción en el Sistema:** El `CommandParser` interpreta el comando general y genera el catálogo de 42 comandos institucionales clasificados por los 8 Casos de Uso.
* **Respuesta Esperada en Bandeja:** Correo con tabla formal rectangular y paleta UNEXO conteniendo la lista de comandos.
* **Comprobación en BD:** Consulta en memoria / metadata.

---

#### PASO 1: Identificación y Control de Acceso (Minuto 6)
* **Actor / Remitente:** Cuenta Administrativa o Docente (`admin@crm.bo` o correo registrado)
* **Asunto (Subject):**
  ```text
  QUIENSOY[]
  ```
* **Acción en el Sistema:** Consulta la tabla `usuarios` y `roles` vinculando el `From:` del remitente.
* **Respuesta Esperada en Bandeja:** Ficha del usuario con: Nombre completo, Correo, Rol (`Administrador`), Estado (`[ACTIVO]`) y Participación en Round-Robin.

---

#### PASO 2: CU1 - Verificación de Personal y Vendedores (Minuto 7)
* **Actor / Remitente:** Administrador (`admin@crm.bo`)
* **Asunto (Subject):**
  ```text
  LISTARUSUARIOS[]
  ```
* **Acción en el Sistema:** `UsuarioDao.listarTodos()` ejecuta consulta parametrizada a PostgreSQL.
* **Respuesta Esperada en Bandeja:** Tabla formal con ID, Nombre, Rol, Estado y Vendedores disponibles para asignación comercial.
* **Prueba de Seguridad Rápida (Si el docente la pide):**
  * Asunto: `TOGGLEUSUARIO["1"]`
  * Respuesta: `[ERROR] Operación denegada por seguridad: El Administrador no puede desactivar ni bloquear su propia cuenta.`

---

#### PASO 3: CU2 - Captación de Nuevo Prospecto Interesado (Minuto 8)
* **Actor / Remitente:** Cualquier remitente / Docente / Web (`docente@uagrm.edu.bo`)
* **Asunto (Subject):**
  ```text
  REGISTRARPROSPECTO["Mario Becerra","70011223","mario.becerra@gmail.com","Facebook Ads","Ingeniero de Sistemas"]
  ```
* **Acción en el Sistema:** Inserta en la tabla `leads` con `id_etapa = 1` (Nuevo), `id_origen = 1` (Facebook Ads) y `id_vendedor = NULL`.
* **Respuesta Esperada en Bandeja:** `[EXITO] Prospecto Mario Becerra registrado satisfactoriamente con ID #15. Etapa: Nuevo. Ubicación: Bolsa Común.`
* **Verificación en PostgreSQL:**
  ```sql
  SELECT id_lead, nombre, telefono, id_etapa, id_vendedor FROM leads WHERE correo = 'mario.becerra@gmail.com';
  ```

---

#### PASO 4: CU3 - Ver Bolsa Común y Asignación a Vendedor (Minuto 9)
* **Actor / Remitente:** Coordinador o Administrador (`admin@crm.bo`)
* **Asunto (Subject):**
  ```text
  BOLSACOMUN[]
  ```
* **Respuesta:** Muestra la tabla de prospectos sin asignar, donde figura Mario Becerra con `id_vendedor = NULL`.
* **Siguiente Asunto (Asignación Directa):**
  ```text
  ASIGNARMANUAL["15","3"]
  ```
  *(Asigna el prospecto #15 al vendedor #3: Vendedor 1)*.
* **Acción en el Sistema:** Actualiza `leads.id_vendedor = 3`, registra auditoría en `asignaciones_lead` y añade hito en `bitacora`.
* **Respuesta:** `[EXITO] Prospecto #15 asignado exitosamente al vendedor Vendedor 1.`

---

#### PASO 5: CU4 - Gestión del Embudo Comercial Kanban (Minuto 11)
* **Actor / Remitente:** Vendedor (`vendedor1@crm.bo`) o Administrador
* **Asunto (Subject):**
  ```text
  VERKANBAN[]
  ```
* **Respuesta:** Tabla con las 6 etapas del embudo comercial:
  * Etapa 1 (Nuevo)
  * Etapa 2 (En Contacto)
  * Etapa 3 (Seguimiento)
  * Etapa 4 (Promesa de Pago)
  * Etapa 5 (Convertido)
  * Etapa 6 (Perdido)
* **Siguiente Asunto (Avanzar Etapa):**
  ```text
  MOVERETAPA["15","4"]
  ```
  *(El vendedor contactó al prospecto y este confirmó que pagará hoy: avanza a Etapa 4 Promesa de Pago)*.
* **Respuesta:** `[EXITO] El prospecto #15 avanzó a la etapa Promesa de Pago. Evento registrado en bitácora.`

---

#### PASO 6: CU7 - Registro de Interacción en Bitácora (Minuto 12)
* **Actor / Remitente:** Vendedor (`vendedor1@crm.bo`)
* **Asunto (Subject):**
  ```text
  AGREGARNOTA["15","El prospecto confirma depósito mediante transferencia Banco Unión para el Diplomado en Ciberseguridad."]
  ```
* **Acción en el Sistema:** Inserta en la tabla `bitacora` tipo `NOTA` con fecha/hora oficial.
* **Respuesta:** `[EXITO] Nota comercial registrada en la bitácora del prospecto #15.`

---

#### PASO 7: CU6 - Registro de Pago y Conversión a Matrícula (Minuto 13 - Clímax Comercial)
* **Actor / Remitente:** Vendedor / Alumno / Admin
* **Asunto (Paso A - Subir Comprobante):**
  ```text
  REGISTRARCOMPROBANTE["15","BANCO_UNION","TRANS-987654","1500.00"]
  ```
* **Respuesta:** `[EXITO] Comprobante de pago registrado con ID #8. Estado: PENDIENTE DE REVISIÓN.`

* **Asunto (Paso B - Aprobación del Administrador):**
  ```text
  APROBARPAGO["8"]
  ```
* **Respuesta:** `[EXITO] Comprobante #8 APROBADO por Administración.`

* **Asunto (Paso C - Completar Datos y Convertir a Alumno):**
  ```text
  COMPLETARDATOSALUMNO["15","6845123","1998-05-14","Santa Cruz"]
  ```
  *(Completa CI, Fecha de Nacimiento y Ciudad requeridos por la institución)*.

* **Asunto (Paso D - Conversión Definitiva):**
  ```text
  CONVERTIRAMATRICULA["15"]
  ```
* **Acción en el Sistema:** Pasa al lead a **Etapa 5 (Convertido)**, marca el cierre de venta exitoso y emite constancia de matrícula.
* **Respuesta:** `[EXITO] ¡Prospecto convertido formalmente en Alumno! Matrícula generada.`

---

#### PASO 8: CU8 - Métricas y Reporte del Embudo Final (Minuto 15)
* **Actor / Remitente:** Administrador (`admin@crm.bo`)
* **Asunto (Subject):**
  ```text
  REPORTEEMBUDO[]
  ```
* **Acción en el Sistema:** Realiza `GROUP BY` sobre `etapas_pipeline` y `leads`.
* **Respuesta:** Reporte estadístico formal reflejando el incremento en la columna **Convertido** y la tasa de conversión actualizada.

---

## 🔀 Alternativa Rápida para CU5 (Motivos de Pérdida):
Si el docente pide: *«¿Y qué pasa si el prospecto decide no inscribirse?»*, se ejecuta inmediatamente:
* **Asunto:**
  ```text
  MARCARPERDIDO["15","1","El cliente indica que se quedó sin presupuesto para esta versión."]
  ```
* **Acción:** Pasa a etapa 6 (Perdido), exige `id_motivo_perdida = 1` y almacena la justificación obligatoria en la base de datos.
