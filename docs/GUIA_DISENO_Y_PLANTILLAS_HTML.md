# GUÍA TÉCNICA DE DISEÑO Y PLANTILLAS HTML
## Arquitectura de Vista, Estilos y Modificaciones en Vivo para la Defensa
**Materia:** INF513 - Tecnología Web | Grupo 05 SC  
**Docente:** Msc. Ing. Evans Balcázar Veizaga  
**Archivo Central de Estilos:** [HtmlTemplate.java](file:///home/nataly/Projects/TECNO/crm_correo/INF513_P1/src/main/java/com/tecnoweb/grupo05sc/view/HtmlTemplate.java)

---

## 1. Fundamento Arquitectónico y Académico

En la materia **Tecnología Web**, el uso de correos electrónicos bajo el protocolo **SMTP (RFC 5322)** con soporte **MIME (`Content-Type: text/html; charset=UTF-8`)** es el estándar de la industria. 

* **¿Por qué usamos HTML y no solo texto plano?**  
  Un CRM corporativo gestiona expedientes, embudos de ventas (*Kanban*), tablas de usuarios y comprobantes de pago. Enviar tablas desalineadas en texto plano denota falta de manejo del estándar web. El uso de HTML estructurado responde directamente a los objetivos de la materia.
* **Patrón de Diseño MVC (Modelo - Vista - Controlador):**  
  Todo el diseño gráfico, tipografía, colores y márgenes se encuentran desacoplados en una única clase: [HtmlTemplate.java](file:///home/nataly/Projects/TECNO/crm_correo/INF513_P1/src/main/java/com/tecnoweb/grupo05sc/view/HtmlTemplate.java) (la **Vista**). Los controladores y servicios de negocio no contienen estilos embebidos arbitrarios, lo que permite modificar cualquier aspecto visual en menos de 20 segundos durante la defensa.

---

## 2. Desglose Línea por Línea de `HtmlTemplate.java`

El archivo cuenta con **175 líneas** organizadas de forma limpia en 6 bloques lógicos:

```
HtmlTemplate.java
├── Bloque 1: Textos Globales y Paleta de Colores (Líneas 16 - 33)
├── Bloque 2: Contenedor y Armazón General (wrapContainer) (Líneas 38 - 87)
├── Bloque 3: Tarjetas de Error y Validación (errorCard) (Líneas 92 - 105)
├── Bloque 4: Tarjetas de Confirmación Exitosa (successCard) (Líneas 110 - 122)
├── Bloque 5: Generador de Tablas Corporativas (table) (Líneas 127 - 156)
└── Bloque 6: Insignias de Etapas Kanban (badgeEtapa) (Líneas 161 - 174)
```

---

### 🏛️ BLOQUE 1: Textos Globales y Paleta de Colores (Líneas 16 - 33)

Define todas las constantes públicas utilizadas en la plataforma:

```java
16: public final class HtmlTemplate {
17: 
18:     public static String HEADER_TITLE = "CRM EDUCATIVO UNEXO";
19:     public static String HEADER_SUBTITLE = "Plataforma de Gestion Comercial - FICCT UAGRM";
20:     public static String FOOTER_TEXT = "INF513 Tecnologia Web | Grupo 05 SC | Universidad Autonoma Gabriel Rene Moreno";
21: 
22:     // Paleta de Colores Institucional UNEXO
23:     public static final String COLOR_PRIMARY = "#00264d";      // Azul Marino Profundo
24:     public static final String COLOR_ACCENT = "#00a8ab";       // Cian / Teal de acento
25:     public static final String COLOR_TEXT = "#1f1919";         // Charcoal formal (casi negro)
26:     public static final String COLOR_BG_PAGE = "#f7f7f7";      // Fondo general gris tenue
27:     public static final String COLOR_CARD_BG = "#ffffff";      // Fondo blanco del contenedor
28:     public static final String COLOR_BORDER = "#cccccc";       // Gris claro de bordes
29:     public static final String COLOR_SUCCESS = "#1e7e34";      // Verde sobrio
30:     public static final String COLOR_DANGER = "#bd2130";       // Rojo sobrio
31:     public static final String COLOR_WARNING = "#856404";      // Ocre / Amarillo sobrio
32: 
33:     private HtmlTemplate() {}
```

#### Explicación Detallada:
* **Línea 18 (`HEADER_TITLE`):** Texto principal blanco del encabezado. Si el docente solicita: *«Cambie el encabezado por GRUPO 05»*, se modifica esta variable.
* **Línea 19 (`HEADER_SUBTITLE`):** Subtítulo celeste claro bajo el título principal.
* **Línea 20 (`FOOTER_TEXT`):** Texto del pie de página que aparece centrado en gris al final de cada correo.
* **Línea 23 (`COLOR_PRIMARY = "#00264d"`):** Azul marino institucional. Se usa en el banner principal y en los encabezados (`<th>`) de todas las tablas.
* **Línea 24 (`COLOR_ACCENT = "#00a8ab"`):** Color cian/verde azulado. Se usa en la línea divisoria inferior de la cabecera y en el fondo de las etiquetas (*badges*).
* **Línea 25 (`COLOR_TEXT = "#1f1919"`):** Color de texto principal para garantizar contraste y legibilidad óptima.
* **Líneas 29 y 30 (`COLOR_SUCCESS` y `COLOR_DANGER`):** Verde (`#1e7e34`) para confirmaciones y Rojo (`#bd2130`) para alertas de error.

---

### 📦 BLOQUE 2: El Armazón General del Correo (`wrapContainer`) (Líneas 38 - 87)

Método envoltorio que proporciona la estructura HTML5, el contenedor centrado, el banner superior y el pie de página a todos los mensajes del sistema:

```java
38:     public static String wrapContainer(String title, String badgeText, String contentHtml) {
39:         StringBuilder sb = new StringBuilder();
40:         sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head>");
41:         sb.append("<body style='font-family: \"Times New Roman\", Times, Georgia, serif; background-color: ").append(COLOR_BG_PAGE)
42:           .append("; margin: 0; padding: 20px; color: ").append(COLOR_TEXT).append(";'>");
```
* **Línea 40:** Cabecera HTML con `UTF-8` para compatibilidad universal de acentos y caracteres especiales.
* **Líneas 41-42:** Estilos del cuerpo del correo:
  * `font-family: "Times New Roman", Times, Georgia, serif`: Tipografía institucional.
  * `background-color: #f7f7f7`: Fondo exterior gris suave.
  * `padding: 20px`: Margen de separación con los bordes de la ventana del cliente de correo.

```java
44:         // Contenedor principal rectangular
45:         sb.append("  <div style='max-width: 850px; margin: 0 auto; background: ").append(COLOR_CARD_BG)
46:           .append("; border: 1px solid ").append(COLOR_BORDER).append(";'>");
```
* **Líneas 45-46:** Contenedor central tipo tarjeta blanca:
  * `max-width: 850px`: Ancho máximo para evitar que el texto se disperse en pantallas panorámicas.
  * `margin: 0 auto`: Centra la tarjeta horizontalmente en la pantalla.
  * `border: 1px solid #cccccc`: Borde sutil que delimita la tarjeta.

```java
48:         // Cabecera formal
49:         sb.append("    <div style='background-color: ").append(COLOR_PRIMARY)
50:           .append("; padding: 16px 22px; border-bottom: 3px solid ").append(COLOR_ACCENT).append(";'>");
51:         sb.append("      <table style='width: 100%; border-collapse: collapse;'><tr>");
52:         sb.append("        <td style='vertical-align: middle;'>");
53:         sb.append("          <h2 style='margin: 0; color: #ffffff; font-size: 18px; font-weight: bold; letter-spacing: 0.5px;'>")
54:           .append(HEADER_TITLE).append("</h2>");
55:         sb.append("          <div style='color: #d1e2f2; font-size: 12px; margin-top: 3px;'>")
56:           .append(HEADER_SUBTITLE).append("</div>");
57:         sb.append("        </td>");
```
* **Líneas 49-57:** Cabecera azul marino. Emplea una tabla de 1 fila y 2 columnas (`<td>`) para garantizar compatibilidad con clientes de correo que no soportan CSS Flexbox o Grid (como Microsoft Outlook).
  * `padding: 16px 22px`: Relleno interno superior e inferior de 16px, lateral de 22px.
  * `border-bottom: 3px solid #00a8ab`: Línea decorativa inferior cian.

```java
58:         if (badgeText != null && !badgeText.trim().isEmpty()) {
59:             sb.append("        <td style='text-align: right; vertical-align: middle;'>\r\n");
60:             sb.append("          <span style='background: ").append(COLOR_ACCENT)
61:               .append("; color: #ffffff; padding: 4px 10px; font-size: 11px; font-weight: bold; text-transform: uppercase;'>")
62:               .append(badgeText.trim()).append("</span>\r\n");
63:             sb.append("        </td>\r\n");
64:         }
65:         sb.append("      </tr></table>\r\n");
66:         sb.append("    </div>\r\n");
```
* **Líneas 58-64:** Insignia de la esquina superior derecha (`badgeText`). Imprime etiquetas en mayúsculas como `GUIA`, `EXITO`, `ALERTA` con fondo `#00a8ab` y texto blanco.

```java
68:         // Cuerpo de contenido
69:         sb.append("    <div style='padding: 22px;'>\r\n");
70:         if (title != null && !title.trim().isEmpty()) {
71:             sb.append("      <h3 style='color: ").append(COLOR_PRIMARY)
72:               .append("; margin-top: 0; margin-bottom: 14px; font-size: 16px; border-bottom: 1px solid ").append(COLOR_BORDER)
73:               .append("; padding-bottom: 6px;'>").append(title).append("</h3>\r\n");
74:         }
75:         sb.append(contentHtml).append("\r\n");
76:         sb.append("    </div>\r\n");
```
* **Líneas 69-76:** Sección central de contenido.
  * La línea 71 renderiza el título de la sección (`<h3>`) con una línea separadora inferior gris.
  * La línea 75 inyecta el contenido dinámico (`contentHtml`), como tablas o fichas de datos.

```java
78:         // Pie de pagina
79:         sb.append("    <div style='background: #f0f0f0; padding: 10px 22px; border-top: 1px solid ").append(COLOR_BORDER)
80:           .append("; font-size: 11px; color: #555555; text-align: center;'>\r\n");
81:         sb.append("      ").append(FOOTER_TEXT).append("\r\n");
82:         sb.append("    </div>\r\n");
83: 
84:         sb.append("  </div>\r\n");
85:         sb.append("</body></html>\r\n");
86:         return sb.toString();
87:     }
```
* **Líneas 78-83:** Pie de página gris tenue (`#f0f0f0`) con tipografía pequeña de 11px y texto centrado.

---

### 🚨 BLOQUE 3: Tarjetas de Error (`errorCard`) (Líneas 92 - 105)

Genera la tarjeta visual para comandos fallidos, errores de permisos (RBAC) o sintaxis incorrecta:

```java
92:     public static String errorCard(String errorMessage, String helpTip) {
93:         StringBuilder sb = new StringBuilder();
94:         sb.append("<div style='background: #fdf7f7; border: 1px solid ").append(COLOR_DANGER)
95:           .append("; border-left: 5px solid ").append(COLOR_DANGER).append("; padding: 14px; margin-bottom: 14px;'>\r\n");
96:         sb.append("  <p style='color: ").append(COLOR_DANGER)
97:           .append("; font-weight: bold; margin: 0 0 6px 0; font-size: 14px;'>[ERROR] ").append(errorMessage).append("</p>\r\n");
98:         if (helpTip != null && !helpTip.trim().isEmpty()) {
99:             sb.append("  <p style='color: #444444; font-size: 13px; margin: 0;'><b>Sugerencia:</b> ").append(helpTip).append("</p>\r\n");
100:         }
101:         sb.append("</div>\r\n");
102:         sb.append("<p style='font-size: 12px; color: #555555; margin: 8px 0 0 0;'>Para consultar la sintaxis valida de cada comando, envie <b>HELP[]</b> en el asunto.</p>\r\n");
103: 
104:         return wrapContainer("Reporte de Error en Peticion", "ALERTA", sb.toString());
105:     }
```
* **Líneas 94-95:** Fondo rojizo tenue (`#fdf7f7`) con borde izquierdo grueso de 5px en rojo corporativo (`#bd2130`).
* **Línea 97:** Muestra `[ERROR]` en negrita y color rojo.
* **Línea 99:** Muestra la sugerencia didáctica para que el usuario aprenda la sintaxis correcta.
* **Línea 104:** Envuelve la tarjeta con `wrapContainer` agregando la etiqueta `ALERTA` en la cabecera.

---

### ✅ BLOQUE 4: Tarjetas de Éxito (`successCard`) (Líneas 110 - 122)

Genera las respuestas de confirmación de operaciones exitosas:

```java
110:     public static String successCard(String title, String message, String detailsHtml) {
111:         StringBuilder sb = new StringBuilder();
112:         sb.append("<div style='background: #f7faf7; border: 1px solid ").append(COLOR_SUCCESS)
113:           .append("; border-left: 5px solid ").append(COLOR_SUCCESS).append("; padding: 14px; margin-bottom: 14px;'>\r\n");
114:         sb.append("  <p style='color: ").append(COLOR_SUCCESS)
115:           .append("; font-weight: bold; margin: 0 0 6px 0; font-size: 14px;'>[EXITO] Operacion Ejecutada Correctamente</p>\r\n");
116:         sb.append("  <p style='color: ").append(COLOR_TEXT).append("; font-size: 13px; margin: 0;'>").append(message).append("</p>\r\n");
117:         sb.append("</div>\r\n");
118:         if (detailsHtml != null && !detailsHtml.trim().isEmpty()) {
119:             sb.append("<div style='margin-top: 14px;'>\r\n").append(detailsHtml).append("</div>\r\n");
120:         }
121:         return wrapContainer(title, "EXITO", sb.toString());
122:     }
```
* **Líneas 112-113:** Fondo verde claro (`#f7faf7`) con barra lateral verde (`#1e7e34`).
* **Línea 114:** Etiqueta formal `[EXITO] Operacion Ejecutada Correctamente`.
* **Línea 119:** Si la operación incluye datos adicionales (por ejemplo la ficha del lead o del comprobante), los renderiza abajo.

---

### 📊 BLOQUE 5: Generador de Tablas Corporativas (`table`) (Líneas 127 - 156)

Genera dinámicamente las tablas para comandos de listado (`LISTARUSUARIOS`, `LISTAROFERTA`, `BOLSACOMUN`, `LEADSETAPA`, `HELP`):

```java
127:     public static String table(String[] headers, List<String[]> rows) {
128:         StringBuilder sb = new StringBuilder();
129:         sb.append("<div style='overflow-x: auto; margin: 10px 0;'>\r\n");
130:         sb.append("<table style='width: 100%; border-collapse: collapse; font-size: 12px; text-align: left; border: 1px solid ").append(COLOR_BORDER).append(";'>\r\n");
```
* **Línea 129 (`overflow-x: auto`):** Garantiza que si la tabla tiene 5 o más columnas, se pueda desplazar horizontalmente en pantallas pequeñas sin desbordar el correo.
* **Línea 130:** 
  * `width: 100%`: Ocupa el ancho completo disponible.
  * `border-collapse: collapse`: Elimina el espacio entre bordes de celdas adyacentes.
  * `font-size: 12px`: Tamaño compacto y legible.

```java
132:         // Encabezados
133:         sb.append("  <thead><tr style='background-color: ").append(COLOR_PRIMARY).append("; color: #ffffff;'>\r\n");
134:         for (String h : headers) {
135:             sb.append("    <th style='padding: 8px 10px; border: 1px solid #777777; font-weight: bold;'>").append(h).append("</th>\r\n");
136:         }
137:         sb.append("  </tr></thead>\r\n");
```
* **Líneas 133-137:** Encabezados de columna (`<th>`):
  * Fondo azul marino (`COLOR_PRIMARY`).
  * Texto en blanco (`#ffffff`).
  * `padding: 8px 10px`: Relleno vertical de 8px y horizontal de 10px.

```java
139:         // Filas
140:         sb.append("  <tbody>\r\n");
141:         if (rows == null || rows.isEmpty()) {
142:             sb.append("    <tr><td colspan='").append(headers.length).append("' style='padding: 14px; text-align: center; color: #666666;'>No se encontraron registros en el sistema.</td></tr>\r\n");
143:         } else {
144:             for (int i = 0; i < rows.size(); i++) {
145:                 String bg = (i % 2 == 0) ? "#ffffff" : "#fbfbfb";
146:                 sb.append("    <tr style='background-color: ").append(bg).append("; color: ").append(COLOR_TEXT).append(";'>\r\n");
147:                 for (String cell : rows.get(i)) {
148:                     sb.append("      <td style='padding: 7px 10px; border: 1px solid ").append(COLOR_BORDER).append(";'>")
149:                       .append(cell != null ? cell : "").append("</td>\r\n");
150:                 }
151:                 sb.append("    </tr>\r\n");
152:             }
153:         }
154:         sb.append("  </tbody>\r\n</table>\r\n</div>\r\n");
155:         return sb.toString();
156:     }
```
* **Línea 145 (Efecto Cebra / Zebra Striping):** Alterna el color de fondo: filas pares en blanco (`#ffffff`) y filas impares en un gris casi blanco (`#fbfbfb`).
* **Línea 148:** Cada celda (`<td>`) cuenta con borde gris de 1px y padding de `7px 10px`.

---

### 🏷️ BLOQUE 6: Insignias de Etapas Kanban (`badgeEtapa`) (Líneas 161 - 174)

Colorea cada etapa comercial del embudo de ventas:

```java
161:     public static String badgeEtapa(int idEtapa, String nombreEtapa) {
162:         String bg;
163:         String color = "#ffffff";
164:         switch (idEtapa) {
165:             case 1: bg = "#0051f9"; break; // Nuevo (Azul)
166:             case 2: bg = "#17a2b8"; break; // En Contacto (Cyan)
167:             case 3: bg = "#d39e00"; break; // Seguimiento (Ocre/Amarillo)
168:             case 4: bg = "#5a379e"; break; // Promesa de Pago (Purpura)
169:             case 5: bg = "#1e7e34"; break; // Convertido (Verde)
170:             case 6: bg = "#bd2130"; break; // Perdido (Rojo)
171:             default: bg = "#555555"; break;
172:         }
173:         return "<span style='background:" + bg + "; color:" + color + "; padding: 3px 8px; font-size: 11px; font-weight: bold; display: inline-block;'>" + nombreEtapa + "</span>";
174:     }
```
* Genera una etiqueta en línea (`<span>`) con bordes rectos y el color característico de la etapa para identificación visual rápida en el Kanban.

---

## 3. "Chuleta" para Modificaciones en Vivo en la Defensa

Si el Ing. Evans Balcázar pide una modificación visual durante la defensa, consulta esta tabla rápida:

| Solicitud del Docente | Archivo y Línea | Código a Modificar |
|---|---|---|
| **Cambiar Título Superior** | [HtmlTemplate.java#L18](file:///home/nataly/Projects/TECNO/crm_correo/INF513_P1/src/main/java/com/tecnoweb/grupo05sc/view/HtmlTemplate.java#L18) | `HEADER_TITLE = "NUEVO TITULO";` |
| **Cambiar Pie de Página** | [HtmlTemplate.java#L20](file:///home/nataly/Projects/TECNO/crm_correo/INF513_P1/src/main/java/com/tecnoweb/grupo05sc/view/HtmlTemplate.java#L20) | `FOOTER_TEXT = "Defensa Primer Parcial INF513";` |
| **Cambiar Color Principal (Azul)** | [HtmlTemplate.java#L23](file:///home/nataly/Projects/TECNO/crm_correo/INF513_P1/src/main/java/com/tecnoweb/grupo05sc/view/HtmlTemplate.java#L23) | `COLOR_PRIMARY = "#000000";` *(o `#721c24` para concho de vino)* |
| **Cambiar Tipografía** | [HtmlTemplate.java#L41](file:///home/nataly/Projects/TECNO/crm_correo/INF513_P1/src/main/java/com/tecnoweb/grupo05sc/view/HtmlTemplate.java#L41) | Cambiar `"Times New Roman"...` por `Arial, sans-serif;` |
| **Cambiar Padding de Tablas** | [HtmlTemplate.java#L148](file:///home/nataly/Projects/TECNO/crm_correo/INF513_P1/src/main/java/com/tecnoweb/grupo05sc/view/HtmlTemplate.java#L148) | Cambiar `padding: 7px 10px;` por `padding: 14px 18px;` |
| **Cambiar Borde de Tablas** | [HtmlTemplate.java#L130](file:///home/nataly/Projects/TECNO/crm_correo/INF513_P1/src/main/java/com/tecnoweb/grupo05sc/view/HtmlTemplate.java#L130) | Cambiar `1px solid` por `2px solid #000000` |

---

## 4. Ciclo de Compilación y Reinicio en 15 Segundos

Cada vez que realices una modificación en [HtmlTemplate.java](file:///home/nataly/Projects/TECNO/crm_correo/INF513_P1/src/main/java/com/tecnoweb/grupo05sc/view/HtmlTemplate.java) o en cualquier archivo Java:

1. **Compilar el proyecto:**
   ```bash
   javac -encoding UTF-8 -cp "lib/postgresql.jar:bin:." -d bin $(find src/main/java -name "*.java")
   ```
2. **Reiniciar el servidor:**
   * En la terminal donde está corriendo el demonio, presionar: `Ctrl + C`
   * Volver a iniciar el servicio con:
     ```bash
     java -cp "lib/postgresql.jar:bin" com.tecnoweb.grupo05sc.Main
     ```
3. **Comprobar:** Enviar cualquier comando desde Gmail (por ejemplo `HELP[]`) y el correo llegará con el nuevo diseño de inmediato.
