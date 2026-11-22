package com.tecnoweb.grupo05sc.service;

import com.tecnoweb.grupo05sc.command.CommandException;
import com.tecnoweb.grupo05sc.command.CommandResult;
import com.tecnoweb.grupo05sc.config.Config;
import com.tecnoweb.grupo05sc.dao.ReporteDao;
import com.tecnoweb.grupo05sc.security.SecurityContext;
import com.tecnoweb.grupo05sc.security.UsuarioAuth;
import com.tecnoweb.grupo05sc.view.HtmlTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Servicio de Negocio para Reportes Estadisticos y Cuadros de Mando Analiticos.
 *
 * Implementa las operaciones gerenciales y comerciales:
 *   - Distribucion porcentual del embudo de ventas institucional.
 *   - Tasa global de conversion y matriz de rendimiento por vendedor.
 *   - Participacion y efectividad por canal de captacion (origen).
 *   - Analisis de causales de desercion y perdida comercial.
 *   - Panel de control operativo y KPIs individuales para ejecutivos de ventas.
 */
public final class ReporteService {

    private final ReporteDao reporteDao;

    public ReporteService() {
        this.reporteDao = new ReporteDao();
    }

    public ReporteService(ReporteDao reporteDao) {
        this.reporteDao = reporteDao;
    }

    /**
     * Comando: REPORTEEMBUDO[]
     * Genera la distribucion volumetrica y porcentual a lo largo de las 6 etapas del embudo.
     */
    public CommandResult reporteEmbudo(UsuarioAuth auth) throws Exception {
        validarAccesoGerencial(auth);

        List<ReporteDao.EmbudoItemDTO> items = reporteDao.obtenerMetricasEmbudo();
        int totalLeads = 0;
        for (ReporteDao.EmbudoItemDTO it : items) {
            totalLeads += it.getTotalLeads();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background: #fdfdfd; border: 1px solid #d0d7de; padding: 12px; margin-bottom: 16px;'>");
        sb.append("  <span style='font-size: 14px; font-weight: bold; color: #00264d;'>DISTRIBUCION GENERAL DEL EMBUDO COMERCIAL</span><br/>");
        sb.append("  <span style='font-size: 12px; color: #555555;'>Total de prospectos registrados en la institucion: <b>").append(totalLeads).append("</b></span>");
        sb.append("</div>");

        String[] headers = new String[]{"ETAPA", "NOMBRE DE ETAPA", "VOLUMEN", "PARTICIPACION", "DISTRIBUCION VISUAL"};
        List<String[]> rows = new ArrayList<>();

        for (ReporteDao.EmbudoItemDTO it : items) {
            String barra = generarBarraPorcentual(it.getPorcentaje());
            rows.add(new String[]{
                "#" + it.getIdEtapa(),
                "<b>" + it.getEtapaNombre() + "</b>",
                String.valueOf(it.getTotalLeads()),
                String.format(Locale.US, "%.1f %%", it.getPorcentaje()),
                "<code style='font-size: 11px;'>" + barra + "</code>"
            });
        }

        sb.append(HtmlTemplate.table(headers, rows));
        sb.append("<p style='font-size: 12px; color: #666666; margin-top: 10px;'>Para consultar la lista detallada de prospectos en una etapa especifica, envie: <code>LEADSETAPA[\"id_etapa\"]</code>.</p>");

        String html = HtmlTemplate.wrapContainer("Reporte Estadistico: Embudo de Ventas", "METRICAS", sb.toString());
        return CommandResult.success(Config.SUBJECT_PREFIX + " Reporte de Embudo de Ventas", html);
    }

    /**
     * Comando: REPORTECONVERSION[]
     * Muestra la tasa global de conversion institucional y el rendimiento por ejecutivo comercial.
     */
    public CommandResult reporteConversion(UsuarioAuth auth) throws Exception {
        validarAccesoGerencial(auth);

        ReporteDao.MetricasGlobalesDTO global = reporteDao.obtenerMetricasGlobales();
        List<ReporteDao.VendedorRendimientoDTO> vendedores = reporteDao.obtenerRendimientoVendedores();

        StringBuilder sb = new StringBuilder();

        // Tarjetas de resumen global
        sb.append("<div style='margin-bottom: 16px;'>");
        sb.append("<table style='width: 100%; border-collapse: collapse; font-size: 13px; text-align: center;'>");
        sb.append("  <tr>");
        sb.append("    <td style='border: 1px solid #d0d7de; padding: 10px; background: #fafbfc;'>TOTAL PROSPECTOS<br/><b style='font-size: 18px; color: #00264d;'>").append(global.getTotalLeads()).append("</b></td>");
        sb.append("    <td style='border: 1px solid #d0d7de; padding: 10px; background: #fafbfc;'>EN PROCESO ACTIVO<br/><b style='font-size: 18px; color: #00a8ab;'>").append(global.getEnProceso()).append("</b></td>");
        sb.append("    <td style='border: 1px solid #d0d7de; padding: 10px; background: #fafbfc;'>MATRICULADOS (CONV.)<br/><b style='font-size: 18px; color: #1e7e34;'>").append(global.getConvertidos()).append("</b></td>");
        sb.append("    <td style='border: 1px solid #d0d7de; padding: 10px; background: #fafbfc;'>DESISTIDOS (PERD.)<br/><b style='font-size: 18px; color: #bd2130;'>").append(global.getPerdidos()).append("</b></td>");
        sb.append("    <td style='border: 1px solid #d0d7de; padding: 10px; background: #fafbfc;'>TASA CONVERSION<br/><b style='font-size: 18px; color: #1e7e34;'>").append(String.format(Locale.US, "%.1f %%", global.getTasaConversion())).append("</b></td>");
        sb.append("  </tr>");
        sb.append("</table>");
        sb.append("</div>");

        sb.append("<p style='font-size: 13px; font-weight: bold; margin-bottom: 6px;'>Matriz de Desempeño Comercial por Ejecutivo:</p>");

        String[] headers = new String[]{"EJECUTIVO COMERCIAL", "ASIGNADOS", "MATRICULADOS", "PERDIDOS", "EN PROCESO", "% CONVERSION"};
        List<String[]> rows = new ArrayList<>();

        for (ReporteDao.VendedorRendimientoDTO v : vendedores) {
            rows.add(new String[]{
                "<b>" + v.getNombreCompleto() + "</b>",
                String.valueOf(v.getTotalAsignados()),
                "<span style='color: #1e7e34; font-weight: bold;'>" + v.getConvertidos() + "</span>",
                "<span style='color: #bd2130;'>" + v.getPerdidos() + "</span>",
                String.valueOf(v.getEnProceso()),
                "<b>" + String.format(Locale.US, "%.1f %%", v.getTasaConversion()) + "</b>"
            });
        }

        sb.append(HtmlTemplate.table(headers, rows));

        String html = HtmlTemplate.wrapContainer("Reporte de Conversion y Rendimiento Comercial", "METRICAS", sb.toString());
        return CommandResult.success(Config.SUBJECT_PREFIX + " Reporte de Conversion Comercial", html);
    }

    /**
     * Comando: REPORTEORIGENES[]
     * Desglosa el volumen y efectividad segun el canal de entrada del prospecto.
     */
    public CommandResult reporteOrigenes(UsuarioAuth auth) throws Exception {
        validarAccesoGerencial(auth);

        List<ReporteDao.OrigenMetricaDTO> origenes = reporteDao.obtenerMetricasOrigenes();

        StringBuilder sb = new StringBuilder();
        sb.append("<p style='font-size: 13px; margin-top: 0;'>Analisis comparativo de atraccion y conversion por fuente de prospeccion:</p>");

        String[] headers = new String[]{"CANAL / FUENTE", "TOTAL CAPTADOS", "MATRICULADOS", "% PARTICIPACION", "EFECTIVIDAD CANAL"};
        List<String[]> rows = new ArrayList<>();

        for (ReporteDao.OrigenMetricaDTO o : origenes) {
            rows.add(new String[]{
                "<b>" + o.getOrigenNombre() + "</b>",
                String.valueOf(o.getTotalLeads()),
                String.valueOf(o.getConvertidos()),
                String.format(Locale.US, "%.1f %%", o.getPorcentajeParticipacion()),
                "<b>" + String.format(Locale.US, "%.1f %%", o.getTasaConversion()) + "</b>"
            });
        }

        sb.append(HtmlTemplate.table(headers, rows));

        String html = HtmlTemplate.wrapContainer("Reporte de Canales de Captacion", "METRICAS", sb.toString());
        return CommandResult.success(Config.SUBJECT_PREFIX + " Reporte de Origenes de Leads", html);
    }

    /**
     * Comando: REPORTEMOTIVOSPERDIDA[]
     * Analisis de causales de descarte o abandono de prospectos.
     */
    public CommandResult reporteMotivosPerdida(UsuarioAuth auth) throws Exception {
        validarAccesoGerencial(auth);

        List<ReporteDao.MotivoPerdidaMetricaDTO> motivos = reporteDao.obtenerMetricasMotivosPerdida();
        int totalPerdidos = 0;
        for (ReporteDao.MotivoPerdidaMetricaDTO m : motivos) {
            totalPerdidos += m.getTotalPerdidos();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background: #fdfdfd; border: 1px solid #d0d7de; padding: 12px; margin-bottom: 16px;'>");
        sb.append("  <span style='font-size: 14px; font-weight: bold; color: #bd2130;'>CAUSAS DE DESISTIMIENTO COMERCIAL</span><br/>");
        sb.append("  <span style='font-size: 12px; color: #555555;'>Total de prospectos clasificados en Etapa Perdido: <b>").append(totalPerdidos).append("</b></span>");
        sb.append("</div>");

        String[] headers = new String[]{"CAUSA DE DESISTIMIENTO", "TOTAL CASOS", "% INCIDENCIA", "REPRESENTACION"};
        List<String[]> rows = new ArrayList<>();

        for (ReporteDao.MotivoPerdidaMetricaDTO m : motivos) {
            String barra = generarBarraPorcentual(m.getPorcentaje());
            rows.add(new String[]{
                "<b>" + m.getMotivoNombre() + "</b>",
                String.valueOf(m.getTotalPerdidos()),
                String.format(Locale.US, "%.1f %%", m.getPorcentaje()),
                "<code style='font-size: 11px;'>" + barra + "</code>"
            });
        }

        sb.append(HtmlTemplate.table(headers, rows));
        sb.append("<p style='font-size: 12px; color: #666666; margin-top: 10px;'>Para listar los expedientes perdidos con sus comentarios detallados envie: <code>LISTARPERDIDOS[]</code>.</p>");

        String html = HtmlTemplate.wrapContainer("Reporte de Motivos de Desistimiento", "METRICAS", sb.toString());
        return CommandResult.success(Config.SUBJECT_PREFIX + " Reporte de Motivos de Perdida", html);
    }

    /**
     * Comando: DASHBOARDVENDEDOR[]
     * Panel individual con los indicadores comerciales del ejecutivo remitente.
     */
    public CommandResult dashboardVendedor(UsuarioAuth auth) throws Exception {
        validarAdminOVendedor(auth);

        ReporteDao.DashboardVendedorDTO d = reporteDao.obtenerDashboardVendedor(auth.getIdUsuario());

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background: #fdfdfd; border: 1px solid #d0d7de; padding: 12px; margin-bottom: 16px;'>");
        sb.append("  <span style='font-size: 14px; font-weight: bold; color: #00264d;'>PANEL DE CONTROL COMERCIAL INDIVIDUAL</span><br/>");
        sb.append("  <span style='font-size: 12px; color: #555555;'>Ejecutivo: <b>").append(auth.getNombreCompleto()).append("</b> | Rol: <b>").append(auth.getRolNombre()).append("</b></span>");
        sb.append("</div>");

        sb.append("<table style='width: 100%; border-collapse: collapse; font-size: 13px; text-align: center; margin-bottom: 16px;'>");
        sb.append("  <tr>");
        sb.append("    <td style='border: 1px solid #d0d7de; padding: 10px; background: #fafbfc;'>TOTAL EN CARTERA<br/><b style='font-size: 18px; color: #00264d;'>").append(d.getTotalCartera()).append("</b></td>");
        sb.append("    <td style='border: 1px solid #d0d7de; padding: 10px; background: #fafbfc;'>PROSPECTOS ACTIVOS<br/><b style='font-size: 18px; color: #00a8ab;'>").append(d.getActivos()).append("</b></td>");
        sb.append("    <td style='border: 1px solid #d0d7de; padding: 10px; background: #fafbfc;'>MATRICULADOS<br/><b style='font-size: 18px; color: #1e7e34;'>").append(d.getConvertidos()).append("</b></td>");
        sb.append("    <td style='border: 1px solid #d0d7de; padding: 10px; background: #fafbfc;'>PERDIDOS<br/><b style='font-size: 18px; color: #bd2130;'>").append(d.getPerdidos()).append("</b></td>");
        sb.append("    <td style='border: 1px solid #d0d7de; padding: 10px; background: #fafbfc;'>% CONVERSION<br/><b style='font-size: 18px; color: #1e7e34;'>").append(String.format(Locale.US, "%.1f %%", d.getTasaConversion())).append("</b></td>");
        sb.append("  </tr>");
        sb.append("</table>");

        sb.append("<table style='width: 100%; border-collapse: collapse; font-size: 13px;'>");
        sb.append("  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 6px 0; font-weight: bold; width: 230px;'>Recordatorios Pendientes:</td><td><b>").append(d.getRecordatoriosPendientes()).append("</b> tareas &nbsp; (Consulte con <code>MISRECORDATORIOS[]</code>)</td></tr>");
        sb.append("  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 6px 0; font-weight: bold;'>Bolsa Comun Disponible:</td><td><b>").append(d.getBolsaComunDisponibles()).append("</b> prospectos sin asignar &nbsp; (Consulte con <code>BOLSACOMUN[]</code>)</td></tr>");
        sb.append("  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 6px 0; font-weight: bold;'>Acciones Recomendadas:</td><td>Consulte su embudo personal con <code>MISLEADS[]</code> o el tablero con <code>VERKANBAN[]</code>.</td></tr>");
        sb.append("</table>");

        String html = HtmlTemplate.wrapContainer("Cuadro de Mando del Vendedor", "DASHBOARD", sb.toString());
        return CommandResult.success(Config.SUBJECT_PREFIX + " Dashboard Comercial - " + auth.getNombreCompleto(), html);
    }

    private void validarAccesoGerencial(UsuarioAuth auth) throws CommandException {
        SecurityContext.validarPersonal(auth);
        if (!auth.esAdmin() && !auth.esCoordinador()) {
            throw new CommandException(
                "Acceso denegado: Los reportes estadisticos estan reservados a los roles Administrador y Coordinador.",
                "Los ejecutivos comerciales pueden consultar sus metricas individuales con: DASHBOARDVENDEDOR[]."
            );
        }
    }

    private void validarAdminOVendedor(UsuarioAuth auth) throws CommandException {
        SecurityContext.validarPersonal(auth);
        if (!auth.esAdmin() && !auth.esVendedor()) {
            throw new CommandException(
                "Acceso denegado: El dashboard comercial individual esta reservado a Administrador y Vendedor.",
                "Los coordinadores academicos consultan los reportes de gestion mediante: REPORTEEMBUDO[], REPORTECONVERSION[], etc."
            );
        }
    }

    private String generarBarraPorcentual(double pct) {
        int totalChars = 15;
        int filled = (int) Math.round((pct / 100.0) * totalChars);
        if (filled > totalChars) filled = totalChars;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < filled; i++) sb.append("=");
        for (int i = filled; i < totalChars; i++) sb.append(" ");
        sb.append("]");
        return sb.toString();
    }
}
