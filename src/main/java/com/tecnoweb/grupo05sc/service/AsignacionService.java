package com.tecnoweb.grupo05sc.service;

import com.tecnoweb.grupo05sc.command.CommandException;
import com.tecnoweb.grupo05sc.command.CommandResult;
import com.tecnoweb.grupo05sc.command.ParsedCommand;
import com.tecnoweb.grupo05sc.config.Config;
import com.tecnoweb.grupo05sc.dao.AsignacionDao;
import com.tecnoweb.grupo05sc.dao.LeadDao;
import com.tecnoweb.grupo05sc.dao.UsuarioDao;
import com.tecnoweb.grupo05sc.security.SecurityContext;
import com.tecnoweb.grupo05sc.security.UsuarioAuth;
import com.tecnoweb.grupo05sc.view.HtmlTemplate;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de Negocio para el Caso de Uso 3 (CU3: Asignacion de Leads a la Fuerza de Ventas).
 * Responsable oficial: Nicol (Dev B).
 *
 * Implementa las reglas institucionales:
 *   - Monitoreo en tiempo real de la Bolsa Comun de prospectos sin vendedor asignado.
 *   - Asignacion manual directa a vendedores activos autorizados.
 *   - Reparto automatico y equitativo mediante algoritmo Round-Robin.
 *   - Transferencias formalizadas exigiendo motivo obligatorio para auditoria.
 *   - Trazabilidad y consulta del historial de traspasos.
 */
public final class AsignacionService {

    private final AsignacionDao asignacionDao;
    private final LeadDao leadDao;
    private final UsuarioDao usuarioDao;

    public AsignacionService() {
        this.asignacionDao = new AsignacionDao();
        this.leadDao = new LeadDao();
        this.usuarioDao = new UsuarioDao();
    }

    public AsignacionService(AsignacionDao asignacionDao, LeadDao leadDao, UsuarioDao usuarioDao) {
        this.asignacionDao = asignacionDao;
        this.leadDao = leadDao;
        this.usuarioDao = usuarioDao;
    }

    /**
     * Comando: BOLSACOMUN[]
     * Lista todos los prospectos en espera de atencion comercial (id_vendedor IS NULL).
     */
    public CommandResult listarBolsaComun(UsuarioAuth auth) throws Exception {
        SecurityContext.validarPersonal(auth);

        List<AsignacionDao.BolsaLeadDTO> leads = asignacionDao.listarBolsaComun();
        String[] headers = new String[]{"ID LEAD", "PROSPECTO", "TELEFONO", "CORREO", "CANAL ORIGEN", "REGISTRADO"};
        List<String[]> rows = new ArrayList<>();

        for (AsignacionDao.BolsaLeadDTO l : leads) {
            rows.add(new String[]{
                "#" + l.getIdLead(),
                "<b>" + l.getNombre() + "</b>",
                l.getTelefono(),
                l.getCorreo(),
                l.getOrigen(),
                l.getCreatedAt()
            });
        }

        String summary = "<p style='font-size: 13px; margin-top: 0;'>Total de prospectos en Bolsa Comun: <b>" + leads.size() + "</b></p>";
        String tableHtml = HtmlTemplate.table(headers, rows);
        String html = HtmlTemplate.wrapContainer("Bolsa Comun de Prospectos", "ASIGNACION", summary + tableHtml);

        return CommandResult.success(Config.SUBJECT_PREFIX + " Bolsa Comun (" + leads.size() + " pendientes)", html);
    }

    /**
     * Comando: ASIGNARMANUAL["id_lead", "id_vendedor", "motivo"]
     * Asigna un prospecto a un vendedor activo.
     */
    public CommandResult asignarManual(UsuarioAuth auth, ParsedCommand cmd) throws Exception {
        SecurityContext.validarPersonal(auth);

        if (!auth.esAdmin() && !auth.esCoordinador()) {
            throw new CommandException(
                "Acceso denegado: La asignacion manual es exclusiva para Administradores y Coordinadores.",
                "Su rol actual es '" + auth.getRolNombre() + "'."
            );
        }

        if (cmd.paramCount() < 2) {
            throw new CommandException(
                "Parametros insuficientes. Se requiere el ID del prospecto y el ID del vendedor.",
                "Sintaxis: ASIGNARMANUAL[\"id_lead\", \"id_vendedor\", \"motivo\"]. Ejemplo: ASIGNARMANUAL[\"15\", \"3\", \"Asignacion inicial\"]"
            );
        }

        long idLead = cmd.getIntParam(0, "id_lead");
        long idVendedor = cmd.getIntParam(1, "id_vendedor");
        String motivo = cmd.getParamOrDefault(2, "Asignacion manual");

        LeadDao.LeadDTO lead = leadDao.buscarPorId(idLead);
        if (lead == null) {
            throw new CommandException("No existe ningun prospecto registrado con el ID #" + idLead + ".");
        }

        UsuarioDao.UsuarioDTO vendedor = usuarioDao.buscarPorId(idVendedor);
        if (vendedor == null) {
            throw new CommandException("No existe ningun usuario registrado con el ID #" + idVendedor + ".");
        }

        if (vendedor.getIdRol() != 3) {
            throw new CommandException(
                "El usuario #" + idVendedor + " (" + vendedor.getNombreCompleto() + ") tiene rol '" + vendedor.getRolNombre() + "'.",
                "Los prospectos solo pueden ser asignados a usuarios con rol 'Vendedor'."
            );
        }

        if (!vendedor.isActivo()) {
            throw new CommandException(
                "El vendedor '" + vendedor.getNombreCompleto() + "' se encuentra inactivo.",
                "Active la cuenta del vendedor antes de asignarle prospectos."
            );
        }

        boolean ok = asignacionDao.asignarManual(idLead, idVendedor, auth.getIdUsuario(), motivo);
        if (!ok) {
            throw new CommandException("No se pudo completar la asignacion del prospecto.");
        }

        String details = "<table style='width: 100%; border-collapse: collapse; font-size: 13px;'>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold; width: 180px;'>Prospecto:</td><td><b>" + lead.getNombre() + "</b> (Lead #" + idLead + ")</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Vendedor Asignado:</td><td><b>" + vendedor.getNombreCompleto() + "</b> (#" + idVendedor + ")</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Asignado Por:</td><td>" + auth.getNombreCompleto() + " (" + auth.getRolNombre() + ")</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Motivo:</td><td>" + motivo + "</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Estado:</td><td><span style='color: #1e7e34; font-weight: bold;'>[ASIGNADO]</span> (Retirado de Bolsa Comun)</td></tr>" +
                         "</table>";

        String html = HtmlTemplate.successCard("Asignacion Manual Exitosa", "El prospecto ha sido asignado a la cartera comercial.", details);
        return CommandResult.success(Config.SUBJECT_PREFIX + " Lead #" + idLead + " Asignado a " + vendedor.getNombreCompleto(), html);
    }

    /**
     * Comando: EJECUTARROUNDROBIN[]
     * Distribuye equitativamente todos los prospectos de la Bolsa Comun entre los vendedores habilitados.
     */
    public CommandResult ejecutarRoundRobin(UsuarioAuth auth) throws Exception {
        SecurityContext.validarPersonal(auth);

        if (!auth.esAdmin() && !auth.esCoordinador()) {
            throw new CommandException(
                "Acceso denegado: La ejecucion del reparto Round-Robin es exclusiva para Administradores y Coordinadores.",
                "Su rol actual es '" + auth.getRolNombre() + "'."
            );
        }

        List<AsignacionDao.VendedorRRDTO> vendedores = asignacionDao.obtenerVendedoresRoundRobinActivos();
        if (vendedores.isEmpty()) {
            throw new CommandException(
                "No hay vendedores activos habilitados para la rotacion Round-Robin.",
                "Asegurese de que existan usuarios con rol 'Vendedor', 'activo = true' y 'participa_round_robin = true'."
            );
        }

        List<AsignacionDao.BolsaLeadDTO> bolsa = asignacionDao.listarBolsaComun();
        if (bolsa.isEmpty()) {
            return CommandResult.success(
                Config.SUBJECT_PREFIX + " Round-Robin Sin Pendientes",
                HtmlTemplate.wrapContainer("Reparto Round-Robin", "AVISO",
                    "<p>No existen prospectos pendientes en la Bolsa Comun para asignar en este momento.</p>")
            );
        }

        List<AsignacionDao.AsignacionResumenDTO> resultado = asignacionDao.ejecutarRoundRobin(auth.getIdUsuario());

        String[] headers = new String[]{"ID LEAD", "PROSPECTO", "VENDEDOR ASIGNADO"};
        List<String[]> rows = new ArrayList<>();
        for (AsignacionDao.AsignacionResumenDTO a : resultado) {
            rows.add(new String[]{
                "#" + a.getIdLead(),
                "<b>" + a.getLeadNombre() + "</b>",
                a.getVendedorNombre() + " (#" + a.getIdVendedor() + ")"
            });
        }

        String summary = "<p style='font-size: 13px;'>Se distribuyeron exitosamente <b>" + resultado.size() + "</b> prospectos entre <b>" + vendedores.size() + "</b> vendedores activos.</p>";
        String tableHtml = HtmlTemplate.table(headers, rows);
        String html = HtmlTemplate.wrapContainer("Reparto Round-Robin Completado", "EXITO", summary + tableHtml);

        return CommandResult.success(Config.SUBJECT_PREFIX + " Reparto Round-Robin (" + resultado.size() + " asignados)", html);
    }

    /**
     * Comando: TRANSFERIRLEAD["id_lead", "id_nuevo_vendedor", "motivo"]
     * Reasigna un prospecto de un vendedor a otro exigiendo justificacion obligatoria.
     */
    public CommandResult transferirLead(UsuarioAuth auth, ParsedCommand cmd) throws Exception {
        SecurityContext.validarPersonal(auth);

        if (!auth.esAdmin() && !auth.esCoordinador()) {
            throw new CommandException(
                "Acceso denegado: La transferencia de prospectos es exclusiva para Administradores y Coordinadores.",
                "Su rol actual es '" + auth.getRolNombre() + "'."
            );
        }

        if (cmd.paramCount() < 3) {
            throw new CommandException(
                "Parametros insuficientes para transferir el prospecto. Se requieren 3 parametros obligatorios.",
                "Sintaxis: TRANSFERIRLEAD[\"id_lead\", \"id_nuevo_vendedor\", \"motivo\"]. " +
                "Ejemplo: TRANSFERIRLEAD[\"15\", \"4\", \"Reasignacion por licencia medica del titular\"]"
            );
        }

        long idLead = cmd.getIntParam(0, "id_lead");
        long idNuevoVendedor = cmd.getIntParam(1, "id_nuevo_vendedor");
        String motivo = cmd.getParam(2).trim();

        if (motivo.isEmpty()) {
            throw new CommandException(
                "El motivo de la transferencia no puede estar vacio.",
                "Indique una justificacion formal para el traspaso de cartera."
            );
        }

        LeadDao.LeadDTO lead = leadDao.buscarPorId(idLead);
        if (lead == null) {
            throw new CommandException("No existe ningun prospecto registrado con el ID #" + idLead + ".");
        }

        UsuarioDao.UsuarioDTO nuevoVend = usuarioDao.buscarPorId(idNuevoVendedor);
        if (nuevoVend == null || nuevoVend.getIdRol() != 3 || !nuevoVend.isActivo()) {
            throw new CommandException(
                "El nuevo vendedor #" + idNuevoVendedor + " no existe, no tiene rol de ventas o esta inactivo.",
                "Seleccione un vendedor activo de la lista institucional."
            );
        }

        boolean ok = asignacionDao.transferirLead(idLead, idNuevoVendedor, auth.getIdUsuario(), motivo);
        if (!ok) {
            throw new CommandException("No se pudo completar la transferencia del prospecto.");
        }

        String details = "<table style='width: 100%; border-collapse: collapse; font-size: 13px;'>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold; width: 180px;'>Prospecto:</td><td><b>" + lead.getNombre() + "</b> (#" + idLead + ")</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Vendedor Anterior:</td><td>" + lead.getVendedorNombre() + "</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Nuevo Titular:</td><td><b>" + nuevoVend.getNombreCompleto() + "</b> (#" + idNuevoVendedor + ")</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Motivo Formal:</td><td>" + motivo + "</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Autorizado Por:</td><td>" + auth.getNombreCompleto() + "</td></tr>" +
                         "</table>";

        String html = HtmlTemplate.successCard("Transferencia de Prospecto", "El prospecto ha sido reasignado satisfactoriamente y registrado en auditoria.", details);
        return CommandResult.success(Config.SUBJECT_PREFIX + " Lead #" + idLead + " Transferido a " + nuevoVend.getNombreCompleto(), html);
    }

    /**
     * Comando: HISTORIALASIGNACIONES["id_lead"]
     * Muestra la traza historica de todas las asignaciones y transferencias que ha tenido un prospecto.
     */
    public CommandResult historialAsignaciones(UsuarioAuth auth, ParsedCommand cmd) throws Exception {
        SecurityContext.validarPersonal(auth);

        if (!cmd.hasParams()) {
            throw new CommandException(
                "Debe especificar el ID del prospecto para consultar su historial de asignaciones.",
                "Sintaxis: HISTORIALASIGNACIONES[\"id_lead\"]. Ejemplo: HISTORIALASIGNACIONES[\"15\"]"
            );
        }

        long idLead = cmd.getIntParam(0, "id_lead");
        LeadDao.LeadDTO lead = leadDao.buscarPorId(idLead);
        if (lead == null) {
            throw new CommandException("No existe ningun prospecto registrado con el ID #" + idLead + ".");
        }

        List<AsignacionDao.AsignacionHistorialDTO> historial = asignacionDao.historialAsignaciones(idLead);

        String[] headers = new String[]{"#", "DE (ANTERIOR)", "A (NUEVO TITULAR)", "TIPO", "MOTIVO", "AUTORIZADO POR", "FECHA"};
        List<String[]> rows = new ArrayList<>();

        for (AsignacionDao.AsignacionHistorialDTO h : historial) {
            rows.add(new String[]{
                String.valueOf(h.getIdAsignacion()),
                h.getVendedorAnterior(),
                "<b>" + h.getVendedorNuevo() + "</b>",
                h.getTipo(),
                h.getMotivo(),
                h.getAsignadoPor(),
                h.getCreatedAt()
            });
        }

        String summary = "<p style='font-size: 13px;'>Historial de traspasos para el prospecto: <b>" + lead.getNombre() + "</b> (Lead #" + idLead + ")</p>";
        String tableHtml = HtmlTemplate.table(headers, rows);
        String html = HtmlTemplate.wrapContainer("Auditoria de Asignaciones - Lead #" + idLead, "AUDITORIA", summary + tableHtml);

        return CommandResult.success(Config.SUBJECT_PREFIX + " Historial Asignaciones Lead #" + idLead, html);
    }
}
