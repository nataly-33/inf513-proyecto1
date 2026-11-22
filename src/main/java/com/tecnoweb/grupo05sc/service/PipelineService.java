package com.tecnoweb.grupo05sc.service;

import com.tecnoweb.grupo05sc.command.CommandException;
import com.tecnoweb.grupo05sc.command.CommandResult;
import com.tecnoweb.grupo05sc.command.ParsedCommand;
import com.tecnoweb.grupo05sc.config.Config;
import com.tecnoweb.grupo05sc.dao.LeadDao;
import com.tecnoweb.grupo05sc.dao.PipelineDao;
import com.tecnoweb.grupo05sc.security.SecurityContext;
import com.tecnoweb.grupo05sc.security.UsuarioAuth;
import com.tecnoweb.grupo05sc.view.HtmlTemplate;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de Negocio para el Embudo de Ventas en Tablero Kanban Comercial.
 *
 * Implementa las reglas institucionales:
 *   - Visualizacion del Tablero Kanban comercial con las 6 etapas obligatorias.
 *   - Consulta detallada de prospectos por columna del embudo.
 *   - Transicion fluida entre etapas con auditoria automatica en Bitacora.
 *   - Intercepcion de etapas especiales:
 *       * Paso a Etapa 6 (Perdido) -> Redirige al CU5 (exige motivo y comentario).
 *       * Paso a Etapa 5 (Convertido) -> Redirige al CU6 (exige comprobante y datos de alumno).
 *   - Cartera personal del vendedor (MISLEADS).
 */
public final class PipelineService {

    private final PipelineDao pipelineDao;
    private final LeadDao leadDao;

    public PipelineService() {
        this.pipelineDao = new PipelineDao();
        this.leadDao = new LeadDao();
    }

    public PipelineService(PipelineDao pipelineDao, LeadDao leadDao) {
        this.pipelineDao = pipelineDao;
        this.leadDao = leadDao;
    }

    /**
     * Comando: VERKANBAN[] o VERKANBAN["id_vendedor"]
     * Muestra el resumen cuantitativo de las 6 etapas del embudo comercial.
     */
    public CommandResult verKanban(UsuarioAuth auth, ParsedCommand cmd) throws Exception {
        SecurityContext.validarPersonal(auth);

        Long filtroVendedor = null;
        String alcanceTexto = "Consolidado Institucional";

        // Si se paso parametro de vendedor o si quien consulta es Vendedor
        if (cmd.hasParams()) {
            filtroVendedor = (long) cmd.getIntParam(0, "id_vendedor");
            alcanceTexto = "Filtrado por Vendedor #" + filtroVendedor;
        } else if (auth.esVendedor()) {
            filtroVendedor = auth.getIdUsuario();
            alcanceTexto = "Cartera Personal de " + auth.getNombreCompleto();
        }

        List<PipelineDao.EtapaResumenDTO> resumen = pipelineDao.obtenerResumenKanban(filtroVendedor);

        String[] headers = new String[]{"ORDEN", "ETAPA DEL EMBUDO COMERCIAL", "PROSPECTOS EN ETAPA"};
        List<String[]> rows = new ArrayList<>();
        int granTotal = 0;

        for (PipelineDao.EtapaResumenDTO e : resumen) {
            granTotal += e.getTotalLeads();
            rows.add(new String[]{
                String.valueOf(e.getOrden()),
                HtmlTemplate.badgeEtapa(e.getIdEtapa(), e.getNombre()),
                "<b>" + e.getTotalLeads() + " leads</b>"
            });
        }

        String summary = "<p style='font-size: 13px; margin-top: 0;'>Alcance: <b>" + alcanceTexto + "</b> | Total en Embudo: <b>" + granTotal + " prospectos</b></p>";
        String tableHtml = HtmlTemplate.table(headers, rows);
        String html = HtmlTemplate.wrapContainer("Tablero Kanban Comercial", "PIPELINE", summary + tableHtml);

        return CommandResult.success(Config.SUBJECT_PREFIX + " Tablero Kanban (" + granTotal + " leads)", html);
    }

    /**
     * Comando: LEADSETAPA["id_etapa"]
     * Lista todos los prospectos ubicados en una columna especifica del embudo.
     */
    public CommandResult leadsEtapa(UsuarioAuth auth, ParsedCommand cmd) throws Exception {
        SecurityContext.validarPersonal(auth);

        if (!cmd.hasParams()) {
            throw new CommandException(
                "Debe especificar el numero de etapa (1 a 6).",
                "Sintaxis: LEADSETAPA[\"id_etapa\"]. Ejemplo: LEADSETAPA[\"2\"] para ver leads 'En Contacto'."
            );
        }

        int idEtapa = cmd.getIntParam(0, "id_etapa");
        if (idEtapa < 1 || idEtapa > 6) {
            throw new CommandException(
                "El ID de etapa #" + idEtapa + " es invalido.",
                "Las etapas van del 1 al 6: 1. Nuevo, 2. En Contacto, 3. Seguimiento, 4. Promesa de Pago, 5. Convertido, 6. Perdido."
            );
        }

        String nombreEtapa = pipelineDao.obtenerNombreEtapa(idEtapa);
        Long filtroVendedor = auth.esVendedor() ? auth.getIdUsuario() : null;

        List<PipelineDao.LeadEtapaDTO> leads = pipelineDao.listarLeadsPorEtapa(idEtapa, filtroVendedor);

        String[] headers = new String[]{"ID LEAD", "PROSPECTO", "TELEFONO", "CORREO", "VENDEDOR ASIGNADO", "ACTUALIZADO"};
        List<String[]> rows = new ArrayList<>();

        for (PipelineDao.LeadEtapaDTO l : leads) {
            rows.add(new String[]{
                "#" + l.getIdLead(),
                "<b>" + l.getNombre() + "</b>",
                l.getTelefono(),
                l.getCorreo(),
                l.getVendedor(),
                l.getUpdatedAt()
            });
        }

        String summary = "<p style='font-size: 13px;'>Prospectos en Etapa: " + HtmlTemplate.badgeEtapa(idEtapa, nombreEtapa) +
                         " | Total: <b>" + leads.size() + " leads</b></p>";
        String tableHtml = HtmlTemplate.table(headers, rows);
        String html = HtmlTemplate.wrapContainer("Columna Kanban: " + nombreEtapa, "PIPELINE", summary + tableHtml);

        return CommandResult.success(Config.SUBJECT_PREFIX + " Leads en " + nombreEtapa + " (" + leads.size() + ")", html);
    }

    /**
     * Comando: MOVERETAPA["id_lead", "id_nueva_etapa"]
     * Avanza o retrocede de etapa al prospecto, aplicando reglas de validacion y auditoria en bitacora.
     */
    public CommandResult moverEtapa(UsuarioAuth auth, ParsedCommand cmd) throws Exception {
        SecurityContext.validarPersonal(auth);

        if (cmd.paramCount() < 2) {
            throw new CommandException(
                "Parametros insuficientes. Se requiere el ID del prospecto y el ID de la nueva etapa.",
                "Sintaxis: MOVERETAPA[\"id_lead\", \"id_nueva_etapa\"]. Ejemplo: MOVERETAPA[\"15\", \"3\"]"
            );
        }

        long idLead = cmd.getIntParam(0, "id_lead");
        long idNuevaEtapa = cmd.getIntParam(1, "id_nueva_etapa");

        if (idNuevaEtapa < 1 || idNuevaEtapa > 6) {
            throw new CommandException(
                "La etapa destino #" + idNuevaEtapa + " no existe.",
                "Las etapas van del 1 al 6: 1. Nuevo, 2. En Contacto, 3. Seguimiento, 4. Promesa de Pago, 5. Convertido, 6. Perdido."
            );
        }

        // REGLA INSTITUCIONAL: Paso a etapa Perdido (Etapa 6) exige motivo y comentario (CU5)
        if (idNuevaEtapa == 6) {
            throw new CommandException(
                "Para mover un prospecto a la etapa 'Perdido' debe tipificar obligatoriamente el motivo formal del desistimiento.",
                "Utilice el comando: MARCARPERDIDO[\"id_lead\", \"id_motivo\", \"comentario justificativo\"]."
            );
        }

        // REGLA INSTITUCIONAL: Paso a etapa Convertido (Etapa 5) exige comprobante y datos de alumno (CU6)
        if (idNuevaEtapa == 5) {
            throw new CommandException(
                "Para formalizar la conversion a 'Convertido' debe procesarse la matriculacion academica formal.",
                "Asegurese de tener el pago aprobado y utilice el comando: CONVERTIRAMATRICULA[\"id_lead\"]."
            );
        }

        LeadDao.LeadDTO lead = leadDao.buscarPorId(idLead);
        if (lead == null) {
            throw new CommandException("No existe ningun prospecto registrado con el ID #" + idLead + ".");
        }

        // Si es vendedor, verificar que el lead pertenezca a su cartera
        if (auth.esVendedor()) {
            if (lead.getIdVendedor() == null || lead.getIdVendedor() != auth.getIdUsuario()) {
                throw new CommandException(
                    "Acceso denegado: El prospecto #" + idLead + " no pertenece a su cartera comercial.",
                    "Solo puede gestionar prospectos asignados a su cuenta."
                );
            }
        }

        String nombreEtapaAnt = lead.getEtapaNombre();
        String nombreEtapaNueva = pipelineDao.obtenerNombreEtapa(idNuevaEtapa);

        boolean ok = pipelineDao.moverEtapa(idLead, idNuevaEtapa, auth.getIdUsuario());
        if (!ok) {
            throw new CommandException("No se pudo actualizar la etapa comercial del prospecto.");
        }

        String details = "<table style='width: 100%; border-collapse: collapse; font-size: 13px;'>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold; width: 180px;'>Prospecto:</td><td><b>" + lead.getNombre() + "</b> (Lead #" + idLead + ")</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Etapa Anterior:</td><td>" + HtmlTemplate.badgeEtapa((int) lead.getIdEtapa(), nombreEtapaAnt) + "</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Nueva Etapa:</td><td>" + HtmlTemplate.badgeEtapa((int) idNuevaEtapa, nombreEtapaNueva) + "</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Responsable:</td><td>" + auth.getNombreCompleto() + " (" + auth.getRolNombre() + ")</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Auditoria:</td><td>Hito registrado cronologicamente en la Bitacora institucional.</td></tr>" +
                         "</table>";

        String html = HtmlTemplate.successCard("Etapa Comercial Actualizada", "El prospecto avanzo satisfactoriamente en el Embudo de Ventas.", details);
        return CommandResult.success(Config.SUBJECT_PREFIX + " Lead #" + idLead + " ahora en " + nombreEtapaNueva, html);
    }

    /**
     * Comando: MISLEADS[]
     * Lista todos los prospectos pertenecientes a la cartera del vendedor que envia el correo.
     */
    public CommandResult misLeads(UsuarioAuth auth) throws Exception {
        SecurityContext.validarPersonal(auth);

        List<PipelineDao.MiLeadDTO> leads = pipelineDao.listarMisLeads(auth.getIdUsuario());

        String[] headers = new String[]{"ID LEAD", "PROSPECTO", "TELEFONO", "CORREO", "ETAPA KANBAN", "ULTIMA GESTION"};
        List<String[]> rows = new ArrayList<>();

        for (PipelineDao.MiLeadDTO l : leads) {
            rows.add(new String[]{
                "#" + l.getIdLead(),
                "<b>" + l.getNombre() + "</b>",
                l.getTelefono(),
                l.getCorreo(),
                HtmlTemplate.badgeEtapa(l.getIdEtapa(), l.getEtapaNombre()),
                l.getUpdatedAt()
            });
        }

        String summary = "<p style='font-size: 13px;'>Cartera comercial asignada a: <b>" + auth.getNombreCompleto() + "</b> | Total: <b>" + leads.size() + " prospectos</b></p>";
        String tableHtml = HtmlTemplate.table(headers, rows);
        String html = HtmlTemplate.wrapContainer("Mi Cartera de Prospectos", "CARTERA", summary + tableHtml);

        return CommandResult.success(Config.SUBJECT_PREFIX + " Mi Cartera (" + leads.size() + " leads)", html);
    }
}
