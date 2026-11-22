package com.tecnoweb.grupo05sc.service;

import com.tecnoweb.grupo05sc.command.CommandException;
import com.tecnoweb.grupo05sc.command.CommandResult;
import com.tecnoweb.grupo05sc.command.ParsedCommand;
import com.tecnoweb.grupo05sc.config.Config;
import com.tecnoweb.grupo05sc.dao.LeadDao;
import com.tecnoweb.grupo05sc.dao.MotivoPerdidaDao;
import com.tecnoweb.grupo05sc.security.SecurityContext;
import com.tecnoweb.grupo05sc.security.UsuarioAuth;
import com.tecnoweb.grupo05sc.view.HtmlTemplate;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de Negocio para la Gestion de Perdida y Desistimiento de Prospectos.
 *
 * Implementa las reglas institucionales:
 *   - Consulta del catalogo oficial de motivos de perdida comercial.
 *   - Tipificacion obligatoria de la causa y comentario analitico antes de mover a Etapa 6 (Perdido).
 *   - Verificacion de pertenencia de cartera para vendedores.
 *   - Consulta y auditoria de prospectos perdidos para retroalimentacion gerencial.
 */
public final class MotivoPerdidaService {

    private final MotivoPerdidaDao motivoDao;
    private final LeadDao leadDao;

    public MotivoPerdidaService() {
        this.motivoDao = new MotivoPerdidaDao();
        this.leadDao = new LeadDao();
    }

    public MotivoPerdidaService(MotivoPerdidaDao motivoDao, LeadDao leadDao) {
        this.motivoDao = motivoDao;
        this.leadDao = leadDao;
    }

    /**
     * Comando: LISTARMOTIVOSPERDIDA[]
     * Muestra el catalogo institucional de motivos de perdida.
     */
    public CommandResult listarMotivosPerdida(UsuarioAuth auth) throws Exception {
        validarAdminOVendedor(auth);

        List<MotivoPerdidaDao.MotivoDTO> motivos = motivoDao.listarMotivos();
        String[] headers = new String[]{"ID MOTIVO", "CAUSA DE DESISTIMIENTO / PERDIDA"};
        List<String[]> rows = new ArrayList<>();

        for (MotivoPerdidaDao.MotivoDTO m : motivos) {
            rows.add(new String[]{
                "#" + m.getIdMotivo(),
                "<b>" + m.getNombre() + "</b>"
            });
        }

        String summary = "<p style='font-size: 13px; margin-top: 0;'>Catalogo oficial para tipificacion de desistimientos comerciales en el CRM.</p>";
        String tableHtml = HtmlTemplate.table(headers, rows);
        String html = HtmlTemplate.wrapContainer("Catalogo de Motivos de Perdida", "PERDIDAS", summary + tableHtml);

        return CommandResult.success(Config.SUBJECT_PREFIX + " Motivos de Perdida (" + motivos.size() + ")", html);
    }

    /**
     * Comando: MARCARPERDIDO["id_lead", "id_motivo", "comentario"]
     * Clasifica un prospecto como Perdido (Etapa 6) exigiendo causa formal y comentario justificativo.
     */
    public CommandResult marcarPerdido(UsuarioAuth auth, ParsedCommand cmd) throws Exception {
        validarAdminOVendedor(auth);

        if (cmd.paramCount() < 3) {
            throw new CommandException(
                "Parametros insuficientes para tipificar la perdida del prospecto.",
                "Sintaxis: MARCARPERDIDO[\"id_lead\", \"id_motivo\", \"comentario\"]. " +
                "Ejemplo: MARCARPERDIDO[\"15\", \"1\", \"El prospecto indica que el arancel excede su presupuesto actual\"]"
            );
        }

        long idLead = cmd.getIntParam(0, "id_lead");
        long idMotivo = cmd.getIntParam(1, "id_motivo");
        String comentario = cmd.getParam(2).trim();

        if (comentario.isEmpty()) {
            throw new CommandException(
                "El comentario explicativo no puede estar vacio.",
                "Detalle las razones expuestas por el prospecto para alimentar los informes gerenciales."
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
                    "Solo puede gestionar el desistimiento de prospectos asignados a su cuenta."
                );
            }
        }

        MotivoPerdidaDao.MotivoDTO motivo = motivoDao.buscarPorId(idMotivo);
        if (motivo == null) {
            List<MotivoPerdidaDao.MotivoDTO> lista = motivoDao.listarMotivos();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lista.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append("#").append(lista.get(i).getIdMotivo()).append(" (").append(lista.get(i).getNombre()).append(")");
            }
            throw new CommandException(
                "El ID de motivo #" + idMotivo + " no existe.",
                "Motivos validos: " + sb.toString() + ". Ejecute LISTARMOTIVOSPERDIDA[] para consultar el catalogo."
            );
        }

        boolean ok = motivoDao.marcarPerdido(idLead, idMotivo, comentario, auth.getIdUsuario());
        if (!ok) {
            throw new CommandException("No se pudo clasificar el prospecto como perdido.");
        }

        String details = "<table style='width: 100%; border-collapse: collapse; font-size: 13px;'>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold; width: 180px;'>Prospecto:</td><td><b>" + lead.getNombre() + "</b> (Lead #" + idLead + ")</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Etapa Anterior:</td><td>" + HtmlTemplate.badgeEtapa((int) lead.getIdEtapa(), lead.getEtapaNombre()) + "</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Nueva Etapa:</td><td>" + HtmlTemplate.badgeEtapa(6, "Perdido") + "</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Causa Tipificada:</td><td><span style='color: #dc3545; font-weight: bold;'>#" + motivo.getIdMotivo() + " - " + motivo.getNombre() + "</span></td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Justificacion:</td><td>" + comentario + "</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Responsable:</td><td>" + auth.getNombreCompleto() + " (" + auth.getRolNombre() + ")</td></tr>" +
                         "</table>";

        String html = HtmlTemplate.successCard("Prospecto Clasificado como Perdido", "Se actualizo el expediente comercial y se registro la causa en bitacora.", details);
        return CommandResult.success(Config.SUBJECT_PREFIX + " Lead #" + idLead + " Clasificado como Perdido", html);
    }

    /**
     * Comando: LISTARPERDIDOS[]
     * Lista todos los prospectos clasificados en la Etapa 6 (Perdido).
     */
    public CommandResult listarPerdidos(UsuarioAuth auth) throws Exception {
        validarAdminOVendedor(auth);

        Long filtroVendedor = auth.esVendedor() ? auth.getIdUsuario() : null;
        List<MotivoPerdidaDao.LeadPerdidoDTO> perdidos = motivoDao.listarPerdidos(filtroVendedor);

        String[] headers = new String[]{"ID LEAD", "PROSPECTO", "TELEFONO", "VENDEDOR", "CAUSA DE PERDIDA", "JUSTIFICACION", "FECHA"};
        List<String[]> rows = new ArrayList<>();

        for (MotivoPerdidaDao.LeadPerdidoDTO p : perdidos) {
            rows.add(new String[]{
                "#" + p.getIdLead(),
                "<b>" + p.getNombre() + "</b>",
                p.getTelefono(),
                p.getVendedor(),
                "<span style='color: #721c24; font-weight: bold;'>" + p.getMotivoNombre() + "</span>",
                p.getComentario(),
                p.getFechaPerdida()
            });
        }

        String alcance = auth.esVendedor() ? "Cartera personal de " + auth.getNombreCompleto() : "Consolidado institucional";
        String summary = "<p style='font-size: 13px; margin-top: 0;'>Alcance: <b>" + alcance + "</b> | Total en Etapa Perdido: <b>" + perdidos.size() + " prospectos</b></p>";
        String tableHtml = HtmlTemplate.table(headers, rows);
        String html = HtmlTemplate.wrapContainer("Listado de Prospectos Perdidos", "PERDIDAS", summary + tableHtml);

        return CommandResult.success(Config.SUBJECT_PREFIX + " Prospectos Perdidos (" + perdidos.size() + ")", html);
    }

    private void validarAdminOVendedor(UsuarioAuth auth) throws CommandException {
        SecurityContext.validarPersonal(auth);
        if (!auth.esAdmin() && !auth.esVendedor()) {
            throw new CommandException(
                "Acceso denegado: La gestion de deserciones y motivos de perdida esta reservada a los roles Administrador y Vendedor.",
                "El rol Coordinador se enfoca en la oferta academica y reportes estadisticos."
            );
        }
    }
}
