package com.tecnoweb.grupo05sc.service;

import com.tecnoweb.grupo05sc.command.CommandException;
import com.tecnoweb.grupo05sc.command.CommandResult;
import com.tecnoweb.grupo05sc.command.ParsedCommand;
import com.tecnoweb.grupo05sc.config.Config;
import com.tecnoweb.grupo05sc.dao.BitacoraDao;
import com.tecnoweb.grupo05sc.dao.LeadDao;
import com.tecnoweb.grupo05sc.dao.RecordatorioDao;
import com.tecnoweb.grupo05sc.security.SecurityContext;
import com.tecnoweb.grupo05sc.security.UsuarioAuth;
import com.tecnoweb.grupo05sc.view.HtmlTemplate;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Servicio de Negocio para la Bitacora Historica, Interacciones y Recordatorios.
 *
 * Implementa las operaciones comerciales:
 *   - Auditoria cronologica y visualizacion de la linea de tiempo de eventos por prospecto.
 *   - Registro formal de minutas y notas cualitativas de interaccion comercial.
 *   - Agendamiento y programacion de recordatorios y compromisos comerciales.
 *   - Consulta de tareas pendientes en la agenda del vendedor.
 *   - Cumplimiento y cierre de recordatorios programados.
 */
public final class BitacoraService {

    private final BitacoraDao bitacoraDao;
    private final RecordatorioDao recordatorioDao;
    private final LeadDao leadDao;

    public BitacoraService() {
        this.bitacoraDao = new BitacoraDao();
        this.recordatorioDao = new RecordatorioDao();
        this.leadDao = new LeadDao();
    }

    public BitacoraService(BitacoraDao bitacoraDao, RecordatorioDao recordatorioDao, LeadDao leadDao) {
        this.bitacoraDao = bitacoraDao;
        this.recordatorioDao = recordatorioDao;
        this.leadDao = leadDao;
    }

    /**
     * Comando: VERBITACORA["id_lead"]
     * Despliega la linea de tiempo cronologica con todos los eventos y notas de un prospecto.
     */
    public CommandResult verBitacora(UsuarioAuth auth, ParsedCommand cmd) throws Exception {
        SecurityContext.validarPersonal(auth);

        if (cmd.paramCount() < 1) {
            throw new CommandException(
                "Faltan parametros obligatorios para consultar la bitacora.",
                "Sintaxis correcta: VERBITACORA[\"id_lead\"]. Ejemplo: VERBITACORA[\"1\"]"
            );
        }

        long idLead;
        try {
            idLead = Long.parseLong(cmd.getParam(0).trim());
        } catch (NumberFormatException e) {
            throw new CommandException("El parametro 'id_lead' debe ser un numero entero valido.");
        }

        LeadDao.LeadDTO lead = leadDao.buscarPorId(idLead);
        if (lead == null) {
            throw new CommandException("No existe ningun prospecto registrado con el identificador #" + idLead + ".");
        }

        // Si es vendedor, verificar que pertenezca a su cartera o este en bolsa comun
        if (auth.esVendedor()) {
            if (lead.getIdVendedor() != null && lead.getIdVendedor().longValue() != auth.getIdUsuario()) {
                throw new CommandException(
                    "Acceso denegado: El prospecto #" + idLead + " esta asignado a la cartera de otro ejecutivo comercial.",
                    "Solo puede auditar prospectos asignados a su persona o disponibles en la bolsa comun."
                );
            }
        }

        List<BitacoraDao.EventoBitacoraDTO> eventos = bitacoraDao.listarPorLead(idLead);

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background: #fdfdfd; border: 1px solid #d0d7de; padding: 12px; margin-bottom: 16px;'>");
        sb.append("<table style='width: 100%; border-collapse: collapse; font-size: 13px;'>");
        sb.append("  <tr><td style='width: 130px; font-weight: bold; color: #555555;'>PROSPECTO:</td><td><b>#").append(lead.getIdLead()).append(" - ").append(lead.getNombre()).append("</b></td></tr>");
        sb.append("  <tr><td style='font-weight: bold; color: #555555;'>CONTACTO:</td><td>Tel: ").append(lead.getTelefono()).append(" | Correo: ").append(lead.getCorreo() != null ? lead.getCorreo() : "N/D").append("</td></tr>");
        sb.append("  <tr><td style='font-weight: bold; color: #555555;'>ETAPA ACTUAL:</td><td><b>").append(lead.getEtapaNombre()).append("</b> (Etapa #").append(lead.getIdEtapa()).append(")</td></tr>");
        sb.append("  <tr><td style='font-weight: bold; color: #555555;'>EJECUTIVO ASIGNADO:</td><td>").append(lead.getVendedorNombre() != null ? lead.getVendedorNombre() : "<span style='color: #888888;'>[Bolsa Común - Sin Asignar]</span>").append("</td></tr>");
        sb.append("</table>");
        sb.append("</div>");

        if (eventos.isEmpty()) {
            sb.append("<p style='color: #666666; font-style: italic;'>No se registran eventos previos en la bitacora de este prospecto.</p>");
        } else {
            String[] headers = new String[]{"FECHA / HORA", "TIPO", "RESPONSABLE", "DETALLE / TRANSICION"};
            List<String[]> rows = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

            for (BitacoraDao.EventoBitacoraDTO ev : eventos) {
                String badgeTipo;
                String t = ev.getTipo().toUpperCase();
                if (t.contains("CREACION")) {
                    badgeTipo = "<span style='background: #e6f4ea; color: #137333; padding: 2px 6px; font-weight: bold; font-size: 11px;'>[CREACIÓN]</span>";
                } else if (t.contains("ASIGNACION")) {
                    badgeTipo = "<span style='background: #e8f0fe; color: #1a73e8; padding: 2px 6px; font-weight: bold; font-size: 11px;'>[ASIGNACIÓN]</span>";
                } else if (t.contains("ESTADO") || t.contains("ETAPA")) {
                    badgeTipo = "<span style='background: #fef7e0; color: #b06000; padding: 2px 6px; font-weight: bold; font-size: 11px;'>[CAMBIO ETAPA]</span>";
                } else if (t.contains("NOTA")) {
                    badgeTipo = "<span style='background: #f3e8fd; color: #7627bb; padding: 2px 6px; font-weight: bold; font-size: 11px;'>[NOTA]</span>";
                } else if (t.contains("PAGO") || t.contains("MATRICULA")) {
                    badgeTipo = "<span style='background: #e6f4ea; color: #0d652d; padding: 2px 6px; font-weight: bold; font-size: 11px;'>[MATRÍCULA]</span>";
                } else if (t.contains("PERDIDA")) {
                    badgeTipo = "<span style='background: #fce8e6; color: #c5221f; padding: 2px 6px; font-weight: bold; font-size: 11px;'>[PÉRDIDA]</span>";
                } else {
                    badgeTipo = "<span style='background: #f1f3f4; color: #3c4043; padding: 2px 6px; font-weight: bold; font-size: 11px;'>[" + ev.getTipo() + "]</span>";
                }

                String fechaStr = (ev.getCreatedAt() != null) ? sdf.format(ev.getCreatedAt()) : "-";
                String transicion = "";
                if (ev.getEtapaAnteriorNombre() != null || ev.getEtapaNuevaNombre() != null) {
                    transicion = "<div style='font-size: 11px; color: #666666; margin-top: 3px;'>" +
                                 (ev.getEtapaAnteriorNombre() != null ? ev.getEtapaAnteriorNombre() : "Inicio") +
                                 " &rarr; <b>" + (ev.getEtapaNuevaNombre() != null ? ev.getEtapaNuevaNombre() : "N/A") + "</b></div>";
                }

                rows.add(new String[]{
                    fechaStr,
                    badgeTipo,
                    ev.getUsuarioNombre(),
                    "<div>" + ev.getDescripcion() + transicion + "</div>"
                });
            }
            sb.append(HtmlTemplate.table(headers, rows));
        }

        String html = HtmlTemplate.wrapContainer(
            "Bitacora Historica de Interacciones",
            "AUDITORIA",
            sb.toString()
        );

        return CommandResult.success(Config.SUBJECT_PREFIX + " Bitacora Lead #" + lead.getIdLead() + " - " + lead.getNombre(), html);
    }

    /**
     * Comando: AGREGARNOTA["id_lead", "contenido_nota"]
     * Inserta una nota comercial en la bitacora del prospecto.
     */
    public CommandResult agregarNota(UsuarioAuth auth, ParsedCommand cmd) throws Exception {
        validarAdminOVendedor(auth);

        if (cmd.paramCount() < 2) {
            throw new CommandException(
                "Faltan parametros obligatorios para registrar la nota comercial.",
                "Sintaxis correcta: AGREGARNOTA[\"id_lead\", \"contenido_nota\"]. Ejemplo: AGREGARNOTA[\"1\", \"El cliente solicita facilidades de pago en 2 cuotas\"]"
            );
        }

        long idLead;
        try {
            idLead = Long.parseLong(cmd.getParam(0).trim());
        } catch (NumberFormatException e) {
            throw new CommandException("El parametro 'id_lead' debe ser un numero entero valido.");
        }

        String nota = cmd.getParam(1).trim();
        if (nota.length() < 3) {
            throw new CommandException("El contenido de la nota es demasiado breve. Especifique al menos 3 caracteres.");
        }

        LeadDao.LeadDTO lead = leadDao.buscarPorId(idLead);
        if (lead == null) {
            throw new CommandException("No existe el prospecto especificado con identificador #" + idLead + ".");
        }

        if (auth.esVendedor()) {
            if (lead.getIdVendedor() != null && lead.getIdVendedor().longValue() != auth.getIdUsuario()) {
                throw new CommandException(
                    "Acceso denegado: El prospecto #" + idLead + " pertenece a la cartera de otro ejecutivo.",
                    "Solo puede asentar notas sobre prospectos asignados a su persona o en bolsa comun."
                );
            }
        }

        long idNota = bitacoraDao.agregarNota(idLead, auth.getIdUsuario(), nota);

        StringBuilder info = new StringBuilder();
        info.append("<p style='font-size: 13px;'>Se registro exitosamente la nota en el expediente comercial del prospecto:</p>");
        info.append("<table style='width: 100%; border-collapse: collapse; font-size: 13px;'>");
        info.append("  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 6px 0; font-weight: bold; width: 160px;'>Nro. Evento:</td><td>#").append(idNota).append("</td></tr>");
        info.append("  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 6px 0; font-weight: bold;'>Prospecto:</td><td><b>#").append(lead.getIdLead()).append(" - ").append(lead.getNombre()).append("</b></td></tr>");
        info.append("  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 6px 0; font-weight: bold;'>Etapa Actual:</td><td>").append(lead.getEtapaNombre()).append("</td></tr>");
        info.append("  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 6px 0; font-weight: bold;'>Registrado Por:</td><td>").append(auth.getNombreCompleto()).append("</td></tr>");
        info.append("  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 6px 0; font-weight: bold;'>Contenido de la Nota:</td><td style='background: #fdfdfd; padding: 6px; font-style: italic;'>&ldquo;").append(nota).append("&rdquo;</td></tr>");
        info.append("</table>");
        info.append("<p style='font-size: 12px; color: #666666; margin-top: 12px;'>Para consultar la linea de tiempo completa de este cliente, envie: <code>VERBITACORA[\"").append(idLead).append("\"]</code>.</p>");

        String html = HtmlTemplate.successCard("Nota Comercial Registrada", "Interaccion documentada satisfactoriamente en el historial del lead.", info.toString());
        return CommandResult.success(Config.SUBJECT_PREFIX + " Nota Registrada - Lead #" + lead.getIdLead(), html);
    }

    /**
     * Comando: CREARRECORDATORIO["id_lead", "yyyy-MM-dd HH:mm", "desc"]
     * Agenda una tarea o recordatorio futuro para seguimiento comercial.
     */
    public CommandResult crearRecordatorio(UsuarioAuth auth, ParsedCommand cmd) throws Exception {
        validarAdminOVendedor(auth);

        if (cmd.paramCount() < 3) {
            throw new CommandException(
                "Faltan parametros obligatorios para agendar el recordatorio.",
                "Sintaxis correcta: CREARRECORDATORIO[\"id_lead\", \"yyyy-MM-dd HH:mm\", \"descripcion\"]. " +
                "Ejemplo: CREARRECORDATORIO[\"1\", \"2026-11-28 16:30\", \"Llamar para confirmar envío de comprobante\"]"
            );
        }

        long idLead;
        try {
            idLead = Long.parseLong(cmd.getParam(0).trim());
        } catch (NumberFormatException e) {
            throw new CommandException("El parametro 'id_lead' debe ser un numero entero valido.");
        }

        String fechaStr = cmd.getParam(1).trim();
        Timestamp fechaHora = parsearFechaHora(fechaStr);
        if (fechaHora == null) {
            throw new CommandException(
                "Formato de fecha u hora invalido: '" + fechaStr + "'.",
                "Utilice el formato estandar: yyyy-MM-dd HH:mm (ejemplo: 2026-11-28 15:30) o yyyy-MM-dd HH:mm:ss."
            );
        }

        String descripcion = cmd.getParam(2).trim();
        if (descripcion.length() < 3) {
            throw new CommandException("La descripcion del recordatorio es demasiado corta. Especifique al menos 3 caracteres.");
        }

        LeadDao.LeadDTO lead = leadDao.buscarPorId(idLead);
        if (lead == null) {
            throw new CommandException("No existe el prospecto especificado con identificador #" + idLead + ".");
        }

        if (auth.esVendedor()) {
            if (lead.getIdVendedor() != null && lead.getIdVendedor().longValue() != auth.getIdUsuario()) {
                throw new CommandException(
                    "Acceso denegado: El prospecto #" + idLead + " pertenece a la cartera de otro ejecutivo.",
                    "Solo puede agendar tareas sobre prospectos asignados a su persona o en bolsa comun."
                );
            }
        }

        long idRecordatorio = recordatorioDao.crearRecordatorio(idLead, auth.getIdUsuario(), fechaHora, descripcion);

        // Registro de auditoria en la bitacora del prospecto
        bitacoraDao.registrarEvento(
            idLead,
            auth.getIdUsuario(),
            "RECORDATORIO",
            "Recordatorio #" + idRecordatorio + " programado para " + fechaStr + ": " + descripcion,
            null,
            null
        );

        StringBuilder info = new StringBuilder();
        info.append("<p style='font-size: 13px;'>Se programo la siguiente alerta de seguimiento en su agenda comercial:</p>");
        info.append("<table style='width: 100%; border-collapse: collapse; font-size: 13px;'>");
        info.append("  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 6px 0; font-weight: bold; width: 170px;'>Nro. Recordatorio:</td><td><b>#").append(idRecordatorio).append("</b></td></tr>");
        info.append("  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 6px 0; font-weight: bold;'>Prospecto Asociado:</td><td><b>#").append(lead.getIdLead()).append(" - ").append(lead.getNombre()).append("</b></td></tr>");
        info.append("  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 6px 0; font-weight: bold;'>Telefono de Contacto:</td><td>").append(lead.getTelefono()).append("</td></tr>");
        info.append("  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 6px 0; font-weight: bold;'>Fecha y Hora Programada:</td><td><b style='color: #00264d;'>").append(fechaStr).append("</b></td></tr>");
        info.append("  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 6px 0; font-weight: bold;'>Tarea / Compromiso:</td><td>").append(descripcion).append("</td></tr>");
        info.append("  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 6px 0; font-weight: bold;'>Responsable:</td><td>").append(auth.getNombreCompleto()).append("</td></tr>");
        info.append("</table>");
        info.append("<p style='font-size: 12px; color: #666666; margin-top: 12px;'>Para consultar todas sus tareas pendientes, envie: <code>MISRECORDATORIOS[]</code>.</p>");

        String html = HtmlTemplate.successCard("Recordatorio Agendado", "Tarea comercial programada correctamente en el sistema.", info.toString());
        return CommandResult.success(Config.SUBJECT_PREFIX + " Recordatorio Agendado #" + idRecordatorio, html);
    }

    /**
     * Comando: MISRECORDATORIOS[]
     * Muestra la agenda de tareas pendientes asignadas al usuario remitente.
     */
    public CommandResult misRecordatorios(UsuarioAuth auth) throws Exception {
        validarAdminOVendedor(auth);

        List<RecordatorioDao.RecordatorioDTO> recordatorios = recordatorioDao.listarPorUsuario(auth.getIdUsuario(), "PENDIENTE");

        StringBuilder sb = new StringBuilder();
        sb.append("<p style='font-size: 13px; margin-top: 0;'><b>Agenda de Compromisos Comerciales:</b> ").append(auth.getNombreCompleto()).append("</p>");

        if (recordatorios.isEmpty()) {
            sb.append("<p style='color: #1e7e34; font-weight: bold;'>[AL DIA] No registra tareas o llamadas pendientes en su agenda comercial.</p>");
            sb.append("<p style='font-size: 12px; color: #666666;'>Para agendar un nuevo compromiso, envie: <code>CREARRECORDATORIO[\"id_lead\", \"yyyy-MM-dd HH:mm\", \"descripcion\"]</code>.</p>");
        } else {
            String[] headers = new String[]{"ID", "FECHA / HORA", "PROSPECTO", "TELEFONO", "TAREA / DESCRIPCION", "ACCION"};
            List<String[]> rows = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

            for (RecordatorioDao.RecordatorioDTO r : recordatorios) {
                String fechaStr = (r.getFechaHora() != null) ? sdf.format(r.getFechaHora()) : "-";
                rows.add(new String[]{
                    "#" + r.getIdRecordatorio(),
                    "<b>" + fechaStr + "</b>",
                    "#" + r.getIdLead() + " " + r.getLeadNombre(),
                    r.getLeadTelefono(),
                    r.getDescripcion(),
                    "<code>COMPLETARRECORDATORIO[\"" + r.getIdRecordatorio() + "\"]</code>"
                });
            }
            sb.append(HtmlTemplate.table(headers, rows));
            sb.append("<p style='font-size: 12px; color: #666666; margin-top: 10px;'>Total de tareas pendientes: <b>").append(recordatorios.size()).append("</b>. Para marcar como atendida una tarea envie el comando de la columna ACCION.</p>");
        }

        String html = HtmlTemplate.wrapContainer("Mis Recordatorios Pendientes", "AGENDA", sb.toString());
        return CommandResult.success(Config.SUBJECT_PREFIX + " Mis Recordatorios (" + recordatorios.size() + ")", html);
    }

    /**
     * Comando: COMPLETARRECORDATORIO["id_recordatorio"]
     * Marca un recordatorio como completado o atendido.
     */
    public CommandResult completarRecordatorio(UsuarioAuth auth, ParsedCommand cmd) throws Exception {
        validarAdminOVendedor(auth);

        if (cmd.paramCount() < 1) {
            throw new CommandException(
                "Falta el identificador del recordatorio a completar.",
                "Sintaxis correcta: COMPLETARRECORDATORIO[\"id_recordatorio\"]. Ejemplo: COMPLETARRECORDATORIO[\"1\"]"
            );
        }

        long idRecordatorio;
        try {
            idRecordatorio = Long.parseLong(cmd.getParam(0).trim());
        } catch (NumberFormatException e) {
            throw new CommandException("El parametro 'id_recordatorio' debe ser un numero entero valido.");
        }

        RecordatorioDao.RecordatorioDTO rec = recordatorioDao.buscarPorId(idRecordatorio);
        if (rec == null) {
            throw new CommandException("No existe ningun recordatorio registrado con identificador #" + idRecordatorio + ".");
        }

        // Si es vendedor, verificar que le pertenezca a el
        if (auth.esVendedor() && rec.getIdUsuario() != auth.getIdUsuario()) {
            throw new CommandException(
                "Acceso denegado: El recordatorio #" + idRecordatorio + " pertenece a la agenda de otro ejecutivo.",
                "Solo puede completar tareas asignadas formalmente a su cuenta."
            );
        }

        if ("COMPLETADO".equalsIgnoreCase(rec.getEstado())) {
            throw new CommandException("El recordatorio #" + idRecordatorio + " ya se encuentra en estado COMPLETADO.");
        }

        recordatorioDao.marcarCompletado(idRecordatorio);

        // Auditoria en la bitacora del prospecto
        bitacoraDao.registrarEvento(
            rec.getIdLead(),
            auth.getIdUsuario(),
            "RECORDATORIO",
            "Recordatorio #" + idRecordatorio + " COMPLETADO: " + rec.getDescripcion(),
            null,
            null
        );

        StringBuilder info = new StringBuilder();
        info.append("<p style='font-size: 13px;'>El recordatorio fue finalizado y marcado como atendido satisfactoriamente:</p>");
        info.append("<table style='width: 100%; border-collapse: collapse; font-size: 13px;'>");
        info.append("  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 6px 0; font-weight: bold; width: 170px;'>Nro. Recordatorio:</td><td>#").append(rec.getIdRecordatorio()).append("</td></tr>");
        info.append("  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 6px 0; font-weight: bold;'>Prospecto:</td><td>#").append(rec.getIdLead()).append(" - ").append(rec.getLeadNombre()).append("</td></tr>");
        info.append("  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 6px 0; font-weight: bold;'>Tarea Atendida:</td><td>").append(rec.getDescripcion()).append("</td></tr>");
        info.append("  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 6px 0; font-weight: bold;'>Nuevo Estado:</td><td><b style='color: #1e7e34;'>[COMPLETADO]</b></td></tr>");
        info.append("  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 6px 0; font-weight: bold;'>Cerrado Por:</td><td>").append(auth.getNombreCompleto()).append("</td></tr>");
        info.append("</table>");
        info.append("<p style='font-size: 12px; color: #666666; margin-top: 12px;'>Para revisar su agenda restante envie: <code>MISRECORDATORIOS[]</code>.</p>");

        String html = HtmlTemplate.successCard("Recordatorio Completado", "Tarea comercial concluida satisfactoriamente.", info.toString());
        return CommandResult.success(Config.SUBJECT_PREFIX + " Recordatorio #" + idRecordatorio + " Completado", html);
    }

    private void validarAdminOVendedor(UsuarioAuth auth) throws CommandException {
        SecurityContext.validarPersonal(auth);
        if (!auth.esAdmin() && !auth.esVendedor()) {
            throw new CommandException(
                "Acceso denegado: Esta accion comercial esta reservada a Administrador y Vendedor.",
                "El rol Coordinador tiene acceso de lectura para auditoria de programas pero no gestiona tareas comerciales directas."
            );
        }
    }

    private Timestamp parsearFechaHora(String input) {
        if (input == null) return null;
        String val = input.trim();
        String[] patterns = new String[]{
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy/MM/dd HH:mm",
            "yyyy/MM/dd HH:mm:ss",
            "dd-MM-yyyy HH:mm",
            "dd/MM/yyyy HH:mm"
        };
        for (String pat : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pat);
                sdf.setLenient(false);
                Date d = sdf.parse(val);
                return new Timestamp(d.getTime());
            } catch (Exception ignored) {}
        }
        return null;
    }
}
