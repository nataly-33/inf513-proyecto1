# 🎯 GUÍA DE PRUEBA MANUAL PASO A PASO: CASO DE USO 8 (CU8 - REPORTES ESTADÍSTICOS Y CUADROS DE MANDO)
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
3. En **Terminal 3**, abre `psql` para contrastar las métricas calculadas con la base de datos:
   ```bash
   psql -U postgres -d crm_educativo
   ```
4. Envía los correos de prueba hacia la cuenta oficial:
   **Destinatario:** `grupo05sc@tecnoweb.org.bo`

---

## 📋 2. Banco Exhaustivo de Pruebas Comando por Comando (CU8)

---

### 🔹 PRUEBA 8.1: `REPORTEEMBUDO[]` (Distribución Volumétrica y Porcentual del Pipeline)

#### Caso A: Ejecución Exitosa por Administrador o Coordinador
* **Objetivo:** Visualizar el volumen de prospectos en cada una de las 6 etapas del embudo comercial, con su correspondiente porcentaje de concentración y barra visual.
* **Remitente Requerido (`From:`):** `admin@crm.bo` o `coord.sistemas@crm.bo`.
* **Asunto a Enviar (Exacto):**
  ```text
  REPORTEEMBUDO[]
  ```
* **Asunto de Respuesta del Correo:**
  ```text
  RE: [CRM-EDUCATIVO] Reporte de Embudo de Ventas
  ```
* **Contenido HTML que Recibirás en tu Bandeja:**
  * Contenedor formal con etiqueta: `[METRICAS]`
  * Título: `Reporte Estadistico: Embudo de Ventas`
  * Tarjeta superior: `DISTRIBUCION GENERAL DEL EMBUDO COMERCIAL | Total de prospectos registrados: N`
  * Tabla institucional con 5 columnas:
    * `ETAPA`: #1 a #6
    * `NOMBRE DE ETAPA`: Nuevo, En Contacto, Seguimiento, Promesa de Pago, Convertido, Perdido
    * `VOLUMEN`: Cantidad absoluta de prospectos
    * `PARTICIPACIÓN`: Porcentaje respecto al total del CRM (`%`)
    * `DISTRIBUCIÓN VISUAL`: Barra textual institucional (ej. `[========      ]`)
* **Consulta SQL para Comprobar en PostgreSQL:**
  ```sql
  SELECT ep.id_etapa, ep.nombre, COUNT(l.id_lead) AS total,
         ROUND(COUNT(l.id_lead) * 100.0 / (SELECT COUNT(*) FROM leads), 1) AS porcentaje
  FROM etapas_pipeline ep
  LEFT JOIN leads l ON ep.id_etapa = l.id_etapa
  GROUP BY ep.id_etapa, ep.nombre, ep.orden
  ORDER BY ep.orden ASC;
  ```

#### Caso B: Rechazo por Control de Acceso (Vendedor Bloqueado)
* **Remitente Requerido (`From:`):** `vendedor1@crm.bo`.
* **Asunto a Enviar:**
  ```text
  REPORTEEMBUDO[]
  ```
* **Asunto de Respuesta:**
  ```text
  RE: [CRM-EDUCATIVO] Error: Acceso denegado: Los reportes estadisticos estan reservados a los roles Administrador y Coordinador.
  ```
* **Sugerencia en el Cuerpo:** `Los ejecutivos comerciales pueden consultar sus metricas individuales con: DASHBOARDVENDEDOR[].`

---

### 🔹 PRUEBA 8.2: `REPORTECONVERSION[]` (KPIs Globales y Rendimiento por Vendedor)

#### Caso A: Ejecución Exitosa de Matriz de Rendimiento Comercial
* **Objetivo:** Analizar la tasa global de conversión de prospectos a alumnos matriculados y el desempeño individual por cada ejecutivo de ventas activo.
* **Remitente Requerido (`From:`):** `admin@crm.bo` o `coord.sistemas@crm.bo`.
* **Asunto a Enviar (Exacto):**
  ```text
  REPORTECONVERSION[]
  ```
* **Asunto de Respuesta del Correo:**
  ```text
  RE: [CRM-EDUCATIVO] Reporte de Conversion Comercial
  ```
* **Contenido HTML que Recibirás en tu Bandeja:**
  * Fila de tarjetas KPI consolidadas:
    * **TOTAL PROSPECTOS:** Número global
    * **EN PROCESO ACTIVO:** Prospectos en etapas 1 a 4
    * **MATRICULADOS (CONV.):** Alumnos confirmados (Etapa 5) en verde
    * **DESISTIDOS (PERD.):** Bajas comerciales (Etapa 6) en rojo
    * **TASA CONVERSIÓN:** Porcentaje institucional `(Convertidos / Total) * 100`
  * Tabla de desempeño por vendedor:
    * `EJECUTIVO COMERCIAL`: Nombre completo
    * `ASIGNADOS`: Cartera total
    * `MATRICULADOS`: Cierres exitosos
    * `PERDIDOS`: Bajas
    * `EN PROCESO`: Cartera viva
    * `% CONVERSIÓN`: Efectividad personal
* **Consulta SQL para Comprobar en PostgreSQL:**
  ```sql
  -- Rendimiento por vendedor
  SELECT u.nombre_completo,
         COUNT(l.id_lead) AS asignados,
         COUNT(CASE WHEN l.id_etapa = 5 THEN 1 END) AS convertidos,
         COUNT(CASE WHEN l.id_etapa = 6 THEN 1 END) AS perdidos,
         ROUND(COUNT(CASE WHEN l.id_etapa = 5 THEN 1 END) * 100.0 / NULLIF(COUNT(l.id_lead), 0), 1) AS pct_conversion
  FROM usuarios u
  LEFT JOIN leads l ON u.id_usuario = l.id_vendedor
  WHERE u.id_rol = 3 AND u.activo = TRUE
  GROUP BY u.id_usuario, u.nombre_completo;
  ```

---

### 🔹 PRUEBA 8.3: `REPORTEORIGENES[]` (Efectividad por Canal de Prospección)

#### Caso A: Análisis de Fuentes de Entrada
* **Objetivo:** Conocer qué canales de atracción digital o presencial aportan mayor volumen y cuáles generan mayor tasa de conversión a matrícula.
* **Remitente Requerido (`From:`):** `admin@crm.bo` o `coord.sistemas@crm.bo`.
* **Asunto a Enviar (Exacto):**
  ```text
  REPORTEORIGENES[]
  ```
* **Asunto de Respuesta del Correo:**
  ```text
  RE: [CRM-EDUCATIVO] Reporte de Origenes de Leads
  ```
* **Contenido HTML que Recibirás en tu Bandeja:**
  * Contenedor institucional con badge: `[METRICAS]`
  * Título: `Reporte de Canales de Captacion`
  * Tabla formal con 5 columnas:
    * `CANAL / FUENTE`: Facebook, WhatsApp, Web, Referido, etc.
    * `TOTAL CAPTADOS`: Prospectos ingresados por ese medio
    * `MATRICULADOS`: Alumnos convertidos provenientes del canal
    * `% PARTICIPACIÓN`: Peso del canal en la captación total
    * `EFECTIVIDAD CANAL`: Tasa de conversión propia del canal
* **Consulta SQL para Comprobar en PostgreSQL:**
  ```sql
  SELECT ol.nombre AS origen, COUNT(l.id_lead) AS total_captados,
         COUNT(CASE WHEN l.id_etapa = 5 THEN 1 END) AS matriculados,
         ROUND(COUNT(CASE WHEN l.id_etapa = 5 THEN 1 END) * 100.0 / NULLIF(COUNT(l.id_lead), 0), 1) AS efectividad_pct
  FROM origenes_lead ol
  LEFT JOIN leads l ON ol.id_origen = l.id_origen
  GROUP BY ol.id_origen, ol.nombre
  ORDER BY total_captados DESC;
  ```

---

### 🔹 PRUEBA 8.4: `REPORTEMOTIVOSPERDIDA[]` (Causales de Abandono y Desistimiento)

#### Caso A: Análisis Estadístico de Bajas Comerciales
* **Objetivo:** Evaluar las causas frecuentes por las cuales los prospectos no concretaron su inscripción (factor económico, horarios, falta de respuesta, etc.).
* **Remitente Requerido (`From:`):** `admin@crm.bo` o `coord.sistemas@crm.bo`.
* **Asunto a Enviar (Exacto):**
  ```text
  REPORTEMOTIVOSPERDIDA[]
  ```
* **Asunto de Respuesta del Correo:**
  ```text
  RE: [CRM-EDUCATIVO] Reporte de Motivos de Perdida
  ```
* **Contenido HTML que Recibirás en tu Bandeja:**
  * Contenedor con encabezado: `CAUSAS DE DESISTIMIENTO COMERCIAL | Total en Etapa Perdido: N`
  * Tabla analítica con:
    * `CAUSA DE DESISTIMIENTO`: Factor económico, Horario no disponible, Sin respuesta, etc.
    * `TOTAL CASOS`: Casos atribuidos a esa causa
    * `% INCIDENCIA`: Porcentaje sobre el total de deserciones
    * `REPRESENTACIÓN`: Barra gráfica textual
* **Consulta SQL para Comprobar en PostgreSQL:**
  ```sql
  SELECT mp.nombre AS motivo, COUNT(l.id_lead) AS total_perdidos,
         ROUND(COUNT(l.id_lead) * 100.0 / NULLIF((SELECT COUNT(*) FROM leads WHERE id_etapa = 6), 0), 1) AS incidencia_pct
  FROM motivos_perdida mp
  LEFT JOIN leads l ON mp.id_motivo = l.id_motivo_perdida AND l.id_etapa = 6
  GROUP BY mp.id_motivo, mp.nombre
  ORDER BY total_perdidos DESC;
  ```

---

### 🔹 PRUEBA 8.5: `DASHBOARDVENDEDOR[]` (Cuadro de Mando Operativo Individual)

#### Caso A: Despliegue de Métricas Personales del Ejecutivo
* **Objetivo:** Proporcionar al vendedor una visión sintética de su desempeño comercial, cartera asignada y tareas pendientes sin requerir privilegios de administrador.
* **Remitente Requerido (`From:`):** `vendedor1@crm.bo` (o cualquier personal del CRM).
* **Asunto a Enviar (Exacto):**
  ```text
  DASHBOARDVENDEDOR[]
  ```
* **Asunto de Respuesta del Correo:**
  ```text
  RE: [CRM-EDUCATIVO] Dashboard Comercial - Nicol Flores (Vendedora)
  ```
* **Contenido HTML que Recibirás en tu Bandeja:**
  * Tarjeta de encabezado con nombre y rol del ejecutivo
  * Fila de indicadores personales:
    * **TOTAL EN CARTERA:** Prospectos asignados
    * **PROSPECTOS ACTIVOS:** Leads en etapas 1 a 4 requiriendo atención
    * **MATRICULADOS:** Cierres logrados
    * **PERDIDOS:** Deserciones
    * **% CONVERSIÓN:** Efectividad personal del vendedor
  * Ficha de acciones operativas prioritarias:
    * **Recordatorios Pendientes:** Número de tareas con tip para ejecutar `MISRECORDATORIOS[]`
    * **Bolsa Común Disponible:** Prospectos sin asignar listos para tomar con `BOLSACOMUN[]`
    * **Acciones Recomendadas:** Vínculos para consultar `MISLEADS[]` o `VERKANBAN[]`
* **Consulta SQL para Validar los Números del Vendedor:**
  ```sql
  -- Sustituir id_vendedor con el ID del vendedor (ej. 3 para Nicol)
  SELECT 
    COUNT(id_lead) AS total_cartera,
    COUNT(CASE WHEN id_etapa BETWEEN 1 AND 4 THEN 1 END) AS activos,
    COUNT(CASE WHEN id_etapa = 5 THEN 1 END) AS convertidos,
    COUNT(CASE WHEN id_etapa = 6 THEN 1 END) AS perdidos
  FROM leads WHERE id_vendedor = 3;
  ```

---

## 📊 3. Resumen Rápido de Comandos del Caso de Uso 8

| Comando | Sintaxis Exacta | Roles | Propósito Principal |
| :--- | :--- | :---: | :--- |
| **`REPORTEEMBUDO`** | `REPORTEEMBUDO[]` | Admin, Coordinador | Distribución volumétrica y porcentual de las 6 etapas del embudo comercial. |
| **`REPORTECONVERSION`** | `REPORTECONVERSION[]` | Admin, Coordinador | Tasa global de conversión institucional y ranking de desempeño por vendedor. |
| **`REPORTEORIGENES`** | `REPORTEORIGENES[]` | Admin, Coordinador | Efectividad y captación según canal de procedencia (Facebook, WhatsApp, Web). |
| **`REPORTEMOTIVOSPERDIDA`**| `REPORTEMOTIVOSPERDIDA[]` | Admin, Coordinador | Análisis de razones de deserción de clientes para retroalimentación académica. |
| **`DASHBOARDVENDEDOR`** | `DASHBOARDVENDEDOR[]` | Admin, Coord, Vend | Panel individual con cartera personal, tareas pendientes y efectividad. |
