package com.tecnoweb.grupo05sc.view;

import java.util.List;

/**
 * Generador de vistas y plantillas HTML para respuestas de correo electrónico.
 * Diseñado con variables visuales corporativas (#3d5598 azul institucional, #da3038 rojo institucional)
 * y formateo limpio de tablas, insignias (badges) y mensajes de alerta.
 */
public final class HtmlTemplate {

    private HtmlTemplate() {}

    /**
     * Envoltorio general con contenedor de tarjeta, cabecera corporativa y pie de página.
     */
    public static String wrapContainer(String title, String badgeText, String contentHtml) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head>");
        sb.append("<body style='font-family: -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, Helvetica, Arial, sans-serif; background-color: #f5f9fd; margin: 0; padding: 20px; color: #4c5a73;'>");
        sb.append("  <div style='max-width: 850px; margin: 0 auto; background: #ffffff; border-radius: 8px; border: 1px solid #ebf0f9; box-shadow: 0 4px 12px rgba(0,0,0,0.06); overflow: hidden;'>");

        // Cabecera institucional
        sb.append("    <div style='background-color: #3d5598; padding: 16px 24px; border-bottom: 4px solid #da3038; display: flex; align-items: center; justify-content: space-between;'>");
        sb.append("      <div>");
        sb.append("        <h2 style='margin: 0; color: #ffffff; font-size: 19px; letter-spacing: 0.5px;'>CRM EDUCATIVO – FICCT UAGRM</h2>");
        sb.append("        <small style='color: #ebf0f9; font-size: 11px;'>Sistema Automatizado de Negocio vía Correo Electrónico</small>");
        sb.append("      </div>");
        if (badgeText != null && !badgeText.trim().isEmpty()) {
            sb.append("      <span style='background: #da3038; color: #ffffff; padding: 4px 12px; border-radius: 4px; font-size: 11px; font-weight: bold; text-transform: uppercase;'>").append(badgeText).append("</span>");
        }
        sb.append("    </div>");

        // Cuerpo principal
        sb.append("    <div style='padding: 24px;'>");
        if (title != null && !title.trim().isEmpty()) {
            sb.append("      <h3 style='color: #313848; margin-top: 0; margin-bottom: 16px; font-size: 17px; border-bottom: 1px solid #ebf0f9; padding-bottom: 8px;'>").append(title).append("</h3>");
        }
        sb.append(contentHtml);
        sb.append("    </div>");

        // Pie de página
        sb.append("    <div style='background: #f8f9fa; padding: 12px 24px; border-top: 1px solid #ebf0f9; font-size: 11px; color: #6a737b; text-align: center;'>");
        sb.append("      INF513 Tecnología Web • Grupo 05 SC • Universidad Autónoma Gabriel René Moreno");
        sb.append("    </div>");

        sb.append("  </div>");
        sb.append("</body></html>");
        return sb.toString();
    }

    /**
     * Tarjeta estilizada para errores semánticos con sugerencia de sintaxis.
     */
    public static String errorCard(String errorMessage, String helpTip) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background: #fff5f5; border-left: 4px solid #dc3545; padding: 16px; border-radius: 4px; margin-bottom: 16px;'>");
        sb.append("  <p style='color: #dc3545; font-weight: bold; margin: 0 0 8px 0; font-size: 15px;'>❌ ").append(errorMessage).append("</p>");
        if (helpTip != null && !helpTip.trim().isEmpty()) {
            sb.append("  <p style='color: #6a737b; font-size: 13px; margin: 0;'>💡 <b>Sugerencia:</b> ").append(helpTip).append("</p>");
        }
        sb.append("</div>");
        sb.append("<p style='font-size: 13px; color: #4c5a73;'>Si necesita conocer el formato exacto de cada comando, envíe <b>HELP[]</b> en el asunto.</p>");

        return wrapContainer("Error al Procesar la Petición", "ALERTA", sb.toString());
    }

    /**
     * Tarjeta de confirmación exitosa con mensaje descriptivo.
     */
    public static String successCard(String title, String message, String detailsHtml) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background: #f0fff4; border-left: 4px solid #28a745; padding: 16px; border-radius: 4px; margin-bottom: 16px;'>");
        sb.append("  <p style='color: #28a745; font-weight: bold; margin: 0 0 6px 0; font-size: 15px;'>✔ Operación Exitosa</p>");
        sb.append("  <p style='color: #2d3748; font-size: 14px; margin: 0;'>").append(message).append("</p>");
        sb.append("</div>");
        if (detailsHtml != null && !detailsHtml.trim().isEmpty()) {
            sb.append("<div style='margin-top: 16px;'>").append(detailsHtml).append("</div>");
        }
        return wrapContainer(title, "ÉXITO", sb.toString());
    }

    /**
     * Renderiza una tabla HTML estilizada.
     */
    public static String table(String[] headers, List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='overflow-x: auto; margin: 12px 0;'>");
        sb.append("<table style='width: 100%; border-collapse: collapse; font-size: 13px; text-align: left; border: 1px solid #d5ddea;'>");

        // Headers
        sb.append("  <thead><tr style='background-color: #ebf0f9; color: #313848;'>");
        for (String h : headers) {
            sb.append("    <th style='padding: 10px 12px; border: 1px solid #d5ddea; font-weight: 600;'>").append(h).append("</th>");
        }
        sb.append("  </tr></thead>");

        // Rows
        sb.append("  <tbody>");
        if (rows == null || rows.isEmpty()) {
            sb.append("    <tr><td colspan='").append(headers.length).append("' style='padding: 16px; text-align: center; color: #6a737b;'>No se encontraron registros.</td></tr>");
        } else {
            for (int i = 0; i < rows.size(); i++) {
                String bg = (i % 2 == 0) ? "#ffffff" : "#fbfcfe";
                sb.append("    <tr style='background-color: ").append(bg).append("; color: #4c5a73;'>");
                for (String cell : rows.get(i)) {
                    sb.append("      <td style='padding: 9px 12px; border: 1px solid #ebf0f9;'>").append(cell != null ? cell : "").append("</td>");
                }
                sb.append("    </tr>");
            }
        }
        sb.append("  </tbody></table></div>");
        return sb.toString();
    }

    /**
     * Retorna una insignia (badge) visual para las 6 etapas del Kanban.
     */
    public static String badgeEtapa(int idEtapa, String nombreEtapa) {
        String bg;
        String color = "#ffffff";
        switch (idEtapa) {
            case 1: bg = "#0051f9"; break; // Nuevo (Azul)
            case 2: bg = "#17a2b8"; break; // En Contacto (Cyan)
            case 3: bg = "#ffc107"; color = "#313848"; break; // Seguimiento (Amarillo)
            case 4: bg = "#6f42c1"; break; // Promesa de Pago (Púrpura)
            case 5: bg = "#28a745"; break; // Convertido (Verde)
            case 6: bg = "#dc3545"; break; // Perdido (Rojo)
            default: bg = "#6a737b"; break;
        }
        return "<span style='background:" + bg + "; color:" + color + "; padding: 3px 8px; border-radius: 4px; font-size: 11px; font-weight: bold;'>" + nombreEtapa + "</span>";
    }
}
