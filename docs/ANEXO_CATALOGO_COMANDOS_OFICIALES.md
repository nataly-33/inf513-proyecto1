# 📑 ANEXO TÉCNICO: CATÁLOGO OFICIAL DE COMANDOS DEL SISTEMA VÍA EMAIL
### INF513 - Tecnología Web | Grupo 05 SC | Docente: Msc. Ing. Evans Balcázar Veizaga
### Sistema Automatizado vía Correo Electrónico – CRM Educativo UNEXO

---

## 📌 Protocolo de Comunicación y Sintaxis de Petición

El sistema opera bajo una arquitectura asíncrona guiada por eventos de correo electrónico:
* **Entrada (Request):** Se envía en el **Asunto (Subject)** del correo electrónico dirigido a `grupo05sc@tecnoweb.org.bo`.
* **Formato Estándar:** `COMANDO["parámetro_1", "parámetro_2", ...]`
* **Comandos sin parámetros:** Admiten tanto `COMANDO[]` como `COMANDO` (tolerante a espacios).
* **Parámetros de texto con espacios:** Deben ir obligatoriamente delimitados por comillas dobles `"`.
* **Salida (Response):** El sistema responde al buzón del remitente con un correo HTML estructurado bajo el prefijo `RE: [CRM-EDUCATIVO]`.

---

## 🏛️ Catálogo Completo por Casos de Uso (42 Comandos Oficiales)

### 1. Comandos Generales y Autenticación

| Comando | Sintaxis Exacta | Roles Permitidos | Descripción / Resultado Esperado |
| :--- | :--- | :---: | :--- |
| **`HELP`** | `HELP[]` o `HELP["COMANDO"]` | Todos / Público | Devuelve la tabla completa con los 42 comandos institucionales o la ficha detallada de uno en particular. |
| **`QUIENSOY`** | `QUIENSOY[]` | Todos / Registrados | Consulta en PostgreSQL la identidad, nombre completo, rol institucional y estado de cuenta del remitente del correo. |

---

### 2. CU1: Gestión de Usuarios Institucionales

| Comando | Sintaxis Exacta | Roles Permitidos | Descripción / Resultado Esperado |
| :--- | :--- | :---: | :--- |
| **`LISTARUSUARIOS`** | `LISTARUSUARIOS[]` | Administrador | Lista todos los usuarios con su ID, nombre, correo institucional, rol, estado operativo y participación en Round-Robin. |
| **`BUSCARUSUARIO`** | `BUSCARUSUARIO["termino"]` | Administrador | Busca un usuario por coincidencia de ID numérico, correo exacto o coincidencia parcial de nombre. |
| **`CREARUSUARIO`** | `CREARUSUARIO["nombre", "correo", "pass", "rol"]` | Administrador | Da de alta una nueva cuenta institucional. Cifra la contraseña con algoritmo SHA-256 nativo. |
| **`MODIFICARUSUARIO`** | `MODIFICARUSUARIO["id", "nombre", "correo", "rol"]` | Administrador | Actualiza nombre, correo y rol de un usuario existente validando unicidad de correo institucional. |
| **`TOGGLEUSUARIO`** | `TOGGLEUSUARIO["id_usuario"]` | Administrador | Alterna el estado operativo (Activo $\leftrightarrow$ Inactivo). Aplica la **Regla de Oro Anti-Autobloqueo** del Administrador. |
| **`TOGGLEROUNDROBIN`** | `TOGGLEROUNDROBIN["id_usuario"]` | Administrador | Incluye o excluye a un vendedor del reparto automático equitativo de nuevos prospectos. |

---

### 3. CU2: Captación de Leads y Oferta Académica

| Comando | Sintaxis Exacta | Roles Permitidos | Descripción / Resultado Esperado |
| :--- | :--- | :---: | :--- |
| **`REGISTRARPROSPECTO`** | `REGISTRARPROSPECTO["nom", "telf", "mail", "origen", "prof"]` | Todos / Público | Da de alta un prospecto en etapa 1 (*Nuevo*) y lo ubica en la Bolsa Común (`id_vendedor = NULL`). Valida teléfono boliviano (7-8 dígitos) y canal de origen. |
| **`LISTAROFERTA`** | `LISTAROFERTA[]` | Admin, Coord, Vend | Consulta el catálogo académico oficial de programas postgraduales y módulos disponibles para inscripción. |
| **`VINCULARINTERES`** | `VINCULARINTERES["id_lead", "tipo", "id_ext", "curso"]` | Admin, Coord, Vend, Prospecto | Registra en `lead_interes` el interés por un programa o módulo validado contra el catálogo oficial. |
| **`LISTARINTERESES`** | `LISTARINTERESES["id_lead"]` | Admin, Coord, Vend | Lista todos los cursos, versiones o diplomados en los que ha mostrado interés un prospecto. |
| **`LISTARORIGENES`** | `LISTARORIGENES[]` | Admin, Coord, Vend | Lista el catálogo institucional de canales de captación (Facebook Ads, WhatsApp, Web, Referido, etc.). |
| **`LISTARPROFESIONES`** | `LISTARPROFESIONES[]` | Admin, Coord, Vend | Lista el catálogo de profesiones u ocupaciones registradas en el sistema. |

---

### 4. CU3: Asignación de Prospectos a la Fuerza de Ventas

| Comando | Sintaxis Exacta | Roles Permitidos | Descripción / Resultado Esperado |
| :--- | :--- | :---: | :--- |
| **`BOLSACOMUN`** | `BOLSACOMUN[]` | Admin, Coordinador | Muestra la lista de prospectos entrantes pendientes de atención comercial (`id_vendedor IS NULL`). |
| **`ASIGNARMANUAL`** | `ASIGNARMANUAL["id_lead", "id_vendedor"]` | Administrador | Asigna formalmente un prospecto a un vendedor específico, registrando el traspaso en auditoría (Exclusivo Admin). |
| **`EJECUTARROUNDROBIN`**| `EJECUTARROUNDROBIN[]` | Administrador | Distribuye de manera equitativa y rotativa los prospectos de la bolsa común entre los vendedores activos (Exclusivo Admin). |
| **`TRANSFERIRLEAD`** | `TRANSFERIRLEAD["id_lead", "id_nuevo_vend", "motivo"]` | Administrador | Reasigna un prospecto de un vendedor a otro, exigiendo obligatoriamente el motivo formal del cambio (Exclusivo Admin). |
| **`HISTORIALASIGNACIONES`**| `HISTORIALASIGNACIONES["id_lead"]` | Admin, Coordinador | Despliega la traza histórica completa de todas las asignaciones y transferencias que ha tenido el lead. |

---

### 5. CU4: Embudo de Ventas Comercial en Tablero Kanban

| Comando | Sintaxis Exacta | Roles Permitidos | Descripción / Resultado Esperado |
| :--- | :--- | :---: | :--- |
| **`VERKANBAN`** | `VERKANBAN[]` o `VERKANBAN["id_vendedor"]` | Admin, Coord, Vend | Renderiza el tablero comercial consolidado con el total de prospectos en cada una de las 6 etapas. |
| **`LEADSETAPA`** | `LEADSETAPA["id_etapa"]` | Admin, Coord, Vend | Lista con detalle los prospectos que se encuentran en una etapa específica (1 a 6). |
| **`MOVERETAPA`** | `MOVERETAPA["id_lead", "id_nueva_etapa"]` | Admin, Coord, Vend | Avanza o retrocede de etapa al prospecto en el embudo comercial (1 al 6), auditando el cambio en la bitácora. |
| **`MISLEADS`** | `MISLEADS[]` | Vendedor | Lista de forma exclusiva los prospectos que pertenecen a la cartera comercial del vendedor remitente. |

---

### 6. CU5: Gestión de Pérdida y Desistimiento de Prospectos

| Comando | Sintaxis Exacta | Roles Permitidos | Descripción / Resultado Esperado |
| :--- | :--- | :---: | :--- |
| **`LISTARMOTIVOSPERDIDA`**| `LISTARMOTIVOSPERDIDA[]` | Admin, Vendedor | Consulta el catálogo de motivos de pérdida (Factor económico, Horario no disponible, Sin respuesta, etc.). |
| **`MARCARPERDIDO`** | `MARCARPERDIDO["id_lead", "id_motivo", "comentario"]` | Admin, Vendedor | Pasa el lead a la Etapa 6 (*Perdido*), exigiendo obligatoriamente un motivo formal y comentario justificativo. |
| **`LISTARPERDIDOS`** | `LISTARPERDIDOS[]` | Admin, Vendedor | Lista todos los prospectos clasificados como perdidos con sus correspondientes razones y fechas. |

---

### 7. CU6: Validación de Datos, Pagos y Conversión a Matrícula

| Comando | Sintaxis Exacta | Roles Permitidos | Descripción / Resultado Esperado |
| :--- | :--- | :---: | :--- |
| **`REGISTRARCOMPROBANTE`**| `REGISTRARCOMPROBANTE["id_lead", "banco", "nro_trans", "monto"]` | Admin, Vendedor | Registra un comprobante de transferencia bancaria con estado inicial `PENDIENTE` y mueve a Etapa 4 (*Promesa de Pago*). |
| **`PAGOSPENDIENTES`** | `PAGOSPENDIENTES[]` | Administrador | Lista la cola de comprobantes bancarios en espera de revisión contable (Exclusivo Administrador). |
| **`APROBARPAGO`** | `APROBARPAGO["id_comprobante"]` | Administrador | Valida y aprueba el pago realizado, habilitando la conversión oficial del prospecto a alumno (Exclusivo Administrador). |
| **`RECHAZARPAGO`** | `RECHAZARPAGO["id_comprobante", "motivo"]` | Administrador | Rechaza un comprobante inconsistente registrando la observación contable (Exclusivo Administrador). |
| **`COMPLETARDATOSALUMNO`**| `COMPLETARDATOSALUMNO["id_lead", "ci", "fecha_nac", "ciudad"]` | Admin, Vendedor | Registra los datos formales exigidos para el expediente académico del nuevo alumno. |
| **`CONVERTIRAMATRICULA`**| `CONVERTIRAMATRICULA["id_lead"]` | Administrador | Valida doble candado (pago aprobado + datos de alumno completos), pasa a Etapa 5 (*Convertido*) y emite constancia de matrícula. |

---

### 8. CU7: Bitácora Histórica, Interacciones y Recordatorios

| Comando | Sintaxis Exacta | Roles Permitidos | Descripción / Resultado Esperado |
| :--- | :--- | :---: | :--- |
| **`VERBITACORA`** | `VERBITACORA["id_lead"]` | Admin, Coord, Vend | Genera la línea de tiempo (timeline) cronológica completa de eventos, notas y cambios de etapa del prospecto. |
| **`AGREGARNOTA`** | `AGREGARNOTA["id_lead", "contenido_nota"]` | Admin, Vendedor | Inserta una nota cualitativa sobre una reunión o conversación comercial en la bitácora del prospecto. |
| **`CREARRECORDATORIO`**| `CREARRECORDATORIO["id_lead", "yyyy-MM-dd HH:mm", "desc"]` | Admin, Vendedor | Programa una alerta o tarea futura (ej. llamar para confirmar depósito) con fecha y hora exacta. |
| **`MISRECORDATORIOS`** | `MISRECORDATORIOS[]` | Admin, Vendedor | Lista las tareas y compromisos pendientes asignados exclusivamente al remitente. |
| **`COMPLETARRECORDATORIO`**| `COMPLETARRECORDATORIO["id_recordatorio"]` | Admin, Vendedor | Marca un recordatorio o compromiso como atendido y finalizado. |

---

### 9. CU8: Reportes Estadísticos y Cuadros de Mando

| Comando | Sintaxis Exacta | Roles Permitidos | Descripción / Resultado Esperado |
| :--- | :--- | :---: | :--- |
| **`REPORTEEMBUDO`** | `REPORTEEMBUDO[]` | Administrador, Coord | Muestra el volumen total y porcentaje de distribución de prospectos a lo largo de las 6 etapas del pipeline. |
| **`REPORTECONVERSION`** | `REPORTECONVERSION[]` | Administrador, Coord | Calcula la tasa global de conversión de prospectos a alumnos matriculados y el rendimiento por vendedor. |
| **`REPORTEORIGENES`** | `REPORTEORIGENES[]` | Administrador, Coord | Muestra el ranking y volumen de prospectos captados agrupados por canal de origen. |
| **`REPORTEMOTIVOSPERDIDA`**| `REPORTEMOTIVOSPERDIDA[]` | Administrador, Coord | Desglosa estadísticamente las principales causas de deserción de clientes para retroalimentación académica. |
| **`DASHBOARDVENDEDOR`** | `DASHBOARDVENDEDOR[]` | Admin, Vendedor | Despliega el panel de control individual con métricas personales de cartera, prospectos activos y tareas pendientes. |
