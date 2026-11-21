package com.tecnoweb.grupo05sc.view;

import java.util.List;

/**
 * Generador de vistas y plantillas HTML para respuestas de correo electrónico.
 *
 * Directrices de diseno institucional UNEXO:
 * - Tipografia sobria y elegante: Times New Roman / Serif academico.
 * - Paleta formal: Azul Marino Profundo (#00264d), Cian/Teal (#00a8ab), Gris Fondo (#f7f7f7).
 * - Estructura rectangular limpia (border-radius: 0).
 * - Cero emojis (etiquetas de texto formales: [ERROR], [EXITO], [AVISO], [SISTEMA]).
 * - Cabecera y pie de pagina centralizados en constantes estaticas publicas
 *   para edicion inmediata en caso de solicitud del docente en la defensa.
 */
public final class HtmlTemplate {

    public static String HEADER_TITLE = "CRM EDUCATIVO UNEXO";
    public static String HEADER_SUBTITLE = "Plataforma de Gestion Comercial - FICCT UAGRM";
    public static String FOOTER_TEXT = "INF513 Tecnologia Web | Grupo 05 SC | Universidad Autonoma Gabriel Rene Moreno";

    // Paleta de Colores Institucional UNEXO
    public static final String COLOR_PRIMARY = "#00264d";      // Azul Marino Profundo
    public static final String COLOR_ACCENT = "#00a8ab";       // Cian / Teal de acento
    public static final String COLOR_TEXT = "#1f1919";         // Charcoal formal
    public static final String COLOR_BG_PAGE = "#f7f7f7";      // Fondo general gris tenue
    public static final String COLOR_CARD_BG = "#ffffff";      // Fondo de tarjeta blanco
    public static final String COLOR_BORDER = "#cccccc";       // Borde rectangular gris
    public static final String COLOR_SUCCESS = "#1e7e34";      // Verde sobrio
    public static final String COLOR_DANGER = "#bd2130";       // Rojo sobrio
    public static final String COLOR_WARNING = "#856404";      // Ocre / Amarillo sobrio

    private HtmlTemplate() {}

    /**
     * Envoltorio general con contenedor rectangular, cabecera formal y pie de pagina.
     */
    public static String wrapContainer(String title, String badgeText, String contentHtml) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head>");
        sb.append("<body style='font-family: \"Times New Roman\", Times, Georgia, serif; background-color: ").append(COLOR_BG_PAGE)
          .append("; margin: 0; padding: 20px; color: ").append(COLOR_TEXT).append(";'>");

        // Contenedor principal rectangular
        sb.append("  <div style='max-width: 850px; margin: 0 auto; background: ").append(COLOR_CARD_BG)
          .append("; border: 1px solid ").append(COLOR_BORDER).append(";'>");

        // Cabecera formal
        sb.append("    <div style='background-color: ").append(COLOR_PRIMARY)
          .append("; padding: 16px 22px; border-bottom: 3px solid ").append(COLOR_ACCENT).append(";'>");
        sb.append("      <table style='width: 100%; border-collapse: collapse;'><tr>");
        sb.append("        <td style='vertical-align: middle;'>");
        sb.append("          <h2 style='margin: 0; color: #ffffff; font-size: 18px; font-weight: bold; letter-spacing: 0.5px;'>")
          .append(HEADER_TITLE).append("</h2>");
        sb.append("          <div style='color: #d1e2f2; font-size: 12px; margin-top: 3px;'>")
          .append(HEADER_SUBTITLE).append("</div>");
        sb.append("        </td>");
        if (badgeText != null && !badgeText.trim().isEmpty()) {
            sb.append("        <td style='text-align: right; vertical-align: middle;'>");
            sb.append("          <span style='background: ").append(COLOR_ACCENT)
              .append("; color: #ffffff; padding: 4px 10px; font-size: 11px; font-weight: bold; text-transform: uppercase;'>")
              .append(badgeText).append("</span>");
            sb.append("        </td>");
        }
        sb.append("      </tr></table>");
        sb.append("    </div>");

        // Cuerpo de contenido
        sb.append("    <div style='padding: 22px;'>");
        if (title != null && !title.trim().isEmpty()) {
            sb.append("      <h3 style='color: ").append(COLOR_PRIMARY)
              .append("; margin-top: 0; margin-bottom: 14px; font-size: 16px; border-bottom: 1px solid ").append(COLOR_BORDER)
              .append("; padding-bottom: 6px;'>").append(title).append("</h3>");
        }
        sb.append(contentHtml);
        sb.append("    </div>");

        // Pie de pagina
        sb.append("    <div style='background: #f0f0f0; padding: 10px 22px; border-top: 1px solid ").append(COLOR_BORDER)
          .append("; font-size: 11px; color: #555555; text-align: center;'>");
        sb.append("      ").append(FOOTER_TEXT);
        sb.append("    </div>");

        sb.append("  </div>");
        sb.append("</body></html>");
        return sb.toString();
    }

    /**
     * Tarjeta rectangular formal para errores semanticos o de sintaxis.
     */
    public static String errorCard(String errorMessage, String helpTip) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background: #fdf7f7; border: 1px solid ").append(COLOR_DANGER)
          .append("; border-left: 5px solid ").append(COLOR_DANGER).append("; padding: 14px; margin-bottom: 14px;'>");
        sb.append("  <p style='color: ").append(COLOR_DANGER)
          .append("; font-weight: bold; margin: 0 0 6px 0; font-size: 14px;'>[ERROR] ").append(errorMessage).append("</p>");
        if (helpTip != null && !helpTip.trim().isEmpty()) {
            sb.append("  <p style='color: #444444; font-size: 13px; margin: 0;'><b>Sugerencia:</b> ").append(helpTip).append("</p>");
        }
        sb.append("</div>");
        sb.append("<p style='font-size: 12px; color: #555555; margin: 8px 0 0 0;'>Para consultar la sintaxis valida de cada comando, envie <b>HELP[]</b> en el asunto.</p>");

        return wrapContainer("Reporte de Error en Peticion", "ALERTA", sb.toString());
    }

    /**
     * Tarjeta rectangular formal de confirmacion exitosa.
     */
    public static String successCard(String title, String message, String detailsHtml) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background: #f7faf7; border: 1px solid ").append(COLOR_SUCCESS)
          .append("; border-left: 5px solid ").append(COLOR_SUCCESS).append("; padding: 14px; margin-bottom: 14px;'>");
        sb.append("  <p style='color: ").append(COLOR_SUCCESS)
          .append("; font-weight: bold; margin: 0 0 6px 0; font-size: 14px;'>[EXITO] Operacion Ejecutada Correctamente</p>");
        sb.append("  <p style='color: ").append(COLOR_TEXT).append("; font-size: 13px; margin: 0;'>").append(message).append("</p>");
        sb.append("</div>");
        if (detailsHtml != null && !detailsHtml.trim().isEmpty()) {
            sb.append("<div style='margin-top: 14px;'>").append(detailsHtml).append("</div>");
        }
        return wrapContainer(title, "EXITO", sb.toString());
    }

    /**
     * Renderiza una tabla rectangular formal y compacta con alto contraste.
     */
    public static String table(String[] headers, List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='overflow-x: auto; margin: 10px 0;'>");
        sb.append("<table style='width: 100%; border-collapse: collapse; font-size: 12px; text-align: left; border: 1px solid ").append(COLOR_BORDER).append(";'>");

        // Encabezados
        sb.append("  <thead><tr style='background-color: ").append(COLOR_PRIMARY).append("; color: #ffffff;'>");
        for (String h : headers) {
            sb.append("    <th style='padding: 8px 10px; border: 1px solid #777777; font-weight: bold;'>").append(h).append("</th>");
        }
        sb.append("  </tr></thead>");

        // Filas
        sb.append("  <tbody>");
        if (rows == null || rows.isEmpty()) {
            sb.append("    <tr><td colspan='").append(headers.length).append("' style='padding: 14px; text-align: center; color: #666666;'>No se encontraron registros en el sistema.</td></tr>");
        } else {
            for (int i = 0; i < rows.size(); i++) {
                String bg = (i % 2 == 0) ? "#ffffff" : "#fbfbfb";
                sb.append("    <tr style='background-color: ").append(bg).append("; color: ").append(COLOR_TEXT).append(";'>");
                for (String cell : rows.get(i)) {
                    sb.append("      <td style='padding: 7px 10px; border: 1px solid ").append(COLOR_BORDER).append(";'>")
                      .append(cell != null ? cell : "").append("</td>");
                }
                sb.append("    </tr>");
            }
        }
        sb.append("  </tbody></table></div>");
        return sb.toString();
    }

    /**
     * Retorna una insignia visual rectangular para las 6 etapas del embudo comercial (Kanban).
     */
    public static String badgeEtapa(int idEtapa, String nombreEtapa) {
        String bg;
        String color = "#ffffff";
        switch (idEtapa) {
            case 1: bg = "#0051f9"; break; // Nuevo (Azul)
            case 2: bg = "#17a2b8"; break; // En Contacto (Cyan)
            case 3: bg = "#d39e00"; break; // Seguimiento (Ocre/Amarillo sobrio)
            case 4: bg = "#5a379e"; break; // Promesa de Pago (Purpura sobrio)
            case 5: bg = "#1e7e34"; break; // Convertido (Verde sobrio)
            case 6: bg = "#bd2130"; break; // Perdido (Rojo sobrio)
            default: bg = "#555555"; break;
        }
        return "<span style='background:" + bg + "; color:" + color + "; padding: 3px 8px; font-size: 11px; font-weight: bold; display: inline-block;'>" + nombreEtapa + "</span>";
    }
}
