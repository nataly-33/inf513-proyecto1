package com.tecnoweb.grupo05sc.service;

import com.tecnoweb.grupo05sc.command.CommandException;
import com.tecnoweb.grupo05sc.command.CommandResult;
import com.tecnoweb.grupo05sc.command.ParsedCommand;
import com.tecnoweb.grupo05sc.config.Config;
import com.tecnoweb.grupo05sc.dao.LeadDao;
import com.tecnoweb.grupo05sc.dao.PagoMatriculaDao;
import com.tecnoweb.grupo05sc.security.SecurityContext;
import com.tecnoweb.grupo05sc.security.UsuarioAuth;
import com.tecnoweb.grupo05sc.view.HtmlTemplate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de Negocio para la Validacion de Pagos y Conversion a Matricula.
 *
 * Implementa las reglas institucionales:
 *   - Registro formal de comprobantes bancarios y avance automatico a Etapa 4 (Promesa de Pago).
 *   - Aprobacion o rechazo exclusivo por el Administrador institucional.
 *   - Registro y completitud de datos formales del expediente academico (CI, fecha de nacimiento, ciudad).
 *   - Formalizacion de matriculacion y emision de constancia academica oficial (Etapa 5: Convertido).
 */
public final class PagoMatriculaService {

    private final PagoMatriculaDao pagoDao;
    private final LeadDao leadDao;

    public PagoMatriculaService() {
        this.pagoDao = new PagoMatriculaDao();
        this.leadDao = new LeadDao();
    }

    public PagoMatriculaService(PagoMatriculaDao pagoDao, LeadDao leadDao) {
        this.pagoDao = pagoDao;
        this.leadDao = leadDao;
    }

    /**
     * Comando: REGISTRARCOMPROBANTE["id_lead", "banco", "nro_trans", "monto"]
     * Registra un comprobante de deposito bancario con estado inicial PENDIENTE.
     */
    public CommandResult registrarComprobante(UsuarioAuth auth, ParsedCommand cmd) throws Exception {
        validarAdminOVendedor(auth);

        if (cmd.paramCount() < 4) {
            throw new CommandException(
                "Parametros insuficientes para registrar el comprobante de pago.",
                "Sintaxis: REGISTRARCOMPROBANTE[\"id_lead\", \"banco\", \"nro_trans\", \"monto\"]. " +
                "Ejemplo: REGISTRARCOMPROBANTE[\"15\", \"Banco Mercantil Santa Cruz\", \"8829104\", \"2500.00\"]"
            );
        }

        long idLead = cmd.getIntParam(0, "id_lead");
        String banco = cmd.getParam(1).trim();
        String nroTrans = cmd.getParam(2).trim();
        String montoStr = cmd.getParam(3).trim();

        if (banco.isEmpty() || nroTrans.isEmpty()) {
            throw new CommandException("El nombre del banco y el numero de transaccion no pueden estar vacios.");
        }

        BigDecimal monto;
        try {
            monto = new BigDecimal(montoStr);
            if (monto.compareTo(BigDecimal.ZERO) <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            throw new CommandException(
                "El monto especificado '" + montoStr + "' es invalido.",
                "Debe ser un valor numerico positivo en Bolivianos (ej. 2500.00)."
            );
        }

        LeadDao.LeadDTO lead = leadDao.buscarPorId(idLead);
        if (lead == null) {
            throw new CommandException("No existe ningun prospecto registrado con el ID #" + idLead + ".");
        }

        if (auth.esVendedor()) {
            if (lead.getIdVendedor() == null || lead.getIdVendedor() != auth.getIdUsuario()) {
                throw new CommandException(
                    "Acceso denegado: El prospecto #" + idLead + " no pertenece a su cartera comercial.",
                    "Solo puede registrar comprobantes de prospectos asignados a su cuenta."
                );
            }
        }

        long idComprobante = pagoDao.registrarComprobante(idLead, auth.getIdUsuario(), banco, nroTrans, monto);

        String details = "<table style='width: 100%; border-collapse: collapse; font-size: 13px;'>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold; width: 180px;'>ID Comprobante:</td><td><b>#" + idComprobante + "</b></td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Prospecto Beneficiario:</td><td><b>" + lead.getNombre() + "</b> (Lead #" + idLead + ")</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Entidad Financiera:</td><td>" + banco + "</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Nro. de Operacion:</td><td>" + nroTrans + "</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Monto Declarado:</td><td><b>" + monto + " Bs.</b></td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Estado de Revision:</td><td><span style='color: #856404; font-weight: bold;'>[PENDIENTE DE APROBACION]</span></td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Etapa Comercial:</td><td>" + HtmlTemplate.badgeEtapa(4, "Promesa de Pago") + "</td></tr>" +
                         "</table>";

        String html = HtmlTemplate.successCard("Comprobante de Pago Registrado", "El comprobante fue ingresado a la cola de verificacion contable y registrado en bitacora.", details);
        return CommandResult.success(Config.SUBJECT_PREFIX + " Comprobante #" + idComprobante + " Registrado para Lead #" + idLead, html);
    }

    /**
     * Comando: PAGOSPENDIENTES[]
     * Lista todos los comprobantes pendientes de revision (Exclusivo Administrador).
     */
    public CommandResult listarPagosPendientes(UsuarioAuth auth) throws Exception {
        SecurityContext.validarPersonal(auth);

        if (!auth.esAdmin()) {
            throw new CommandException(
                "Acceso denegado: La revision contable de pagos es exclusiva para el Administrador del sistema.",
                "Su rol actual es '" + auth.getRolNombre() + "'."
            );
        }

        List<PagoMatriculaDao.ComprobanteDTO> pendientes = pagoDao.listarPagosPendientes();

        String[] headers = new String[]{"ID", "PROSPECTO", "VENDEDOR", "BANCO Y REFERENCIA", "MONTO (BS.)", "FECHA REGISTRO"};
        List<String[]> rows = new ArrayList<>();

        for (PagoMatriculaDao.ComprobanteDTO c : pendientes) {
            rows.add(new String[]{
                "#" + c.getIdComprobante(),
                "<b>" + c.getLeadNombre() + "</b> (#" + c.getIdLead() + ")",
                c.getVendedorNombre(),
                c.getArchivoUrl(),
                "<b>" + c.getMonto() + " Bs.</b>",
                c.getCreatedAt()
            });
        }

        String summary = "<p style='font-size: 13px; margin-top: 0;'>Comprobantes bancarios en cola de aprobacion contable: <b>" + pendientes.size() + "</b></p>";
        String tableHtml = HtmlTemplate.table(headers, rows);
        String html = HtmlTemplate.wrapContainer("Cola de Comprobantes de Pago Pendientes", "PAGOS", summary + tableHtml);

        return CommandResult.success(Config.SUBJECT_PREFIX + " Pagos Pendientes (" + pendientes.size() + ")", html);
    }

    /**
     * Comando: APROBARPAGO["id_comprobante"]
     * Valida y aprueba un comprobante bancario (Exclusivo Administrador).
     */
    public CommandResult aprobarPago(UsuarioAuth auth, ParsedCommand cmd) throws Exception {
        SecurityContext.validarPersonal(auth);

        if (!auth.esAdmin()) {
            throw new CommandException(
                "Acceso denegado: La aprobacion contable de pagos es exclusiva para el Administrador del sistema.",
                "Su rol actual es '" + auth.getRolNombre() + "'."
            );
        }

        if (!cmd.hasParams()) {
            throw new CommandException(
                "Debe especificar el ID del comprobante a aprobar.",
                "Sintaxis: APROBARPAGO[\"id_comprobante\"]. Ejemplo: APROBARPAGO[\"5\"]"
            );
        }

        long idComprobante = cmd.getIntParam(0, "id_comprobante");
        PagoMatriculaDao.ComprobanteDTO comp = pagoDao.buscarComprobantePorId(idComprobante);
        if (comp == null) {
            throw new CommandException("No existe ningun comprobante registrado con el ID #" + idComprobante + ".");
        }

        if (!"PENDIENTE".equalsIgnoreCase(comp.getEstado())) {
            throw new CommandException(
                "El comprobante #" + idComprobante + " ya fue procesado anteriormente.",
                "Estado actual del comprobante: " + comp.getEstado() + "."
            );
        }

        boolean ok = pagoDao.aprobarPago(idComprobante, auth.getIdUsuario());
        if (!ok) {
            throw new CommandException("No se pudo aprobar el comprobante de pago.");
        }

        String details = "<table style='width: 100%; border-collapse: collapse; font-size: 13px;'>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold; width: 180px;'>Comprobante Aprobado:</td><td><b>#" + idComprobante + "</b></td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Prospecto:</td><td><b>" + comp.getLeadNombre() + "</b> (Lead #" + comp.getIdLead() + ")</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Monto Verificado:</td><td><b>" + comp.getMonto() + " Bs.</b></td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Detalle Bancario:</td><td>" + comp.getArchivoUrl() + "</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Aprobado Por:</td><td>" + auth.getNombreCompleto() + " (Administrador)</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Nuevo Estado:</td><td><span style='color: #1e7e34; font-weight: bold;'>[APROBADO]</span></td></tr>" +
                         "</table>";

        String html = HtmlTemplate.successCard("Pago Aprobado Satisfactoriamente", "El comprobante fue validado con exito y habilita la matriculacion formal.", details);
        return CommandResult.success(Config.SUBJECT_PREFIX + " Comprobante #" + idComprobante + " Aprobado", html);
    }

    /**
     * Comando: RECHAZARPAGO["id_comprobante", "motivo"]
     * Rechaza un comprobante por inconsistencia contable (Exclusivo Administrador).
     */
    public CommandResult rechazarPago(UsuarioAuth auth, ParsedCommand cmd) throws Exception {
        SecurityContext.validarPersonal(auth);

        if (!auth.esAdmin()) {
            throw new CommandException(
                "Acceso denegado: El rechazo contable de pagos es exclusivo para el Administrador del sistema.",
                "Su rol actual es '" + auth.getRolNombre() + "'."
            );
        }

        if (cmd.paramCount() < 2) {
            throw new CommandException(
                "Parametros insuficientes para rechazar el comprobante.",
                "Sintaxis: RECHAZARPAGO[\"id_comprobante\", \"motivo\"]. Ejemplo: RECHAZARPAGO[\"5\", \"El comprobante no figura en el extracto\"]"
            );
        }

        long idComprobante = cmd.getIntParam(0, "id_comprobante");
        String motivo = cmd.getParam(1).trim();

        if (motivo.isEmpty()) {
            throw new CommandException("Debe ingresar una observacion formal para rechazar el comprobante.");
        }

        PagoMatriculaDao.ComprobanteDTO comp = pagoDao.buscarComprobantePorId(idComprobante);
        if (comp == null) {
            throw new CommandException("No existe ningun comprobante registrado con el ID #" + idComprobante + ".");
        }

        if (!"PENDIENTE".equalsIgnoreCase(comp.getEstado())) {
            throw new CommandException(
                "El comprobante #" + idComprobante + " ya fue procesado anteriormente.",
                "Estado actual: " + comp.getEstado() + "."
            );
        }

        boolean ok = pagoDao.rechazarPago(idComprobante, auth.getIdUsuario(), motivo);
        if (!ok) {
            throw new CommandException("No se pudo rechazar el comprobante.");
        }

        String details = "<table style='width: 100%; border-collapse: collapse; font-size: 13px;'>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold; width: 180px;'>Comprobante Rechazado:</td><td>#" + idComprobante + "</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Prospecto:</td><td>" + comp.getLeadNombre() + " (Lead #" + comp.getIdLead() + ")</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Observacion:</td><td><span style='color: #dc3545;'>" + motivo + "</span></td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Revisado Por:</td><td>" + auth.getNombreCompleto() + "</td></tr>" +
                         "</table>";

        String html = HtmlTemplate.wrapContainer("Comprobante de Pago Rechazado", "AVISO", details);
        return CommandResult.success(Config.SUBJECT_PREFIX + " Comprobante #" + idComprobante + " Rechazado", html);
    }

    /**
     * Comando: COMPLETARDATOSALUMNO["id_lead", "ci", "fecha_nac", "ciudad"]
     * Registra los datos formales exigidos para el expediente academico del alumno.
     */
    public CommandResult completarDatosAlumno(UsuarioAuth auth, ParsedCommand cmd) throws Exception {
        validarAdminOVendedor(auth);

        if (cmd.paramCount() < 4) {
            throw new CommandException(
                "Parametros insuficientes para completar el expediente del alumno.",
                "Sintaxis: COMPLETARDATOSALUMNO[\"id_lead\", \"ci\", \"fecha_nac (YYYY-MM-DD)\", \"ciudad\"]. " +
                "Ejemplo: COMPLETARDATOSALUMNO[\"15\", \"8291043 SC\", \"1998-05-14\", \"Santa Cruz de la Sierra\"]"
            );
        }

        long idLead = cmd.getIntParam(0, "id_lead");
        String ci = cmd.getParam(1).trim();
        String fechaNac = cmd.getParam(2).trim();
        String ciudad = cmd.getParam(3).trim();

        if (ci.isEmpty() || fechaNac.isEmpty() || ciudad.isEmpty()) {
            throw new CommandException("El CI, fecha de nacimiento y ciudad de residencia no pueden estar vacios.");
        }

        try {
            LocalDate.parse(fechaNac);
        } catch (DateTimeParseException e) {
            throw new CommandException(
                "El formato de la fecha de nacimiento '" + fechaNac + "' es invalido.",
                "El formato requerido es YYYY-MM-DD (ejemplo: 1998-05-14)."
            );
        }

        LeadDao.LeadDTO lead = leadDao.buscarPorId(idLead);
        if (lead == null) {
            throw new CommandException("No existe ningun prospecto registrado con el ID #" + idLead + ".");
        }

        if (auth.esVendedor()) {
            if (lead.getIdVendedor() == null || lead.getIdVendedor() != auth.getIdUsuario()) {
                throw new CommandException(
                    "Acceso denegado: El prospecto #" + idLead + " no pertenece a su cartera comercial.",
                    "Solo puede gestionar expedientes de prospectos asignados a su cuenta."
                );
            }
        }

        boolean ok = pagoDao.actualizarDatosAlumno(idLead, ci, fechaNac, ciudad, auth.getIdUsuario());
        if (!ok) {
            throw new CommandException("No se pudieron guardar los datos academicos del alumno.");
        }

        String details = "<table style='width: 100%; border-collapse: collapse; font-size: 13px;'>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold; width: 180px;'>Prospecto:</td><td><b>" + lead.getNombre() + "</b> (Lead #" + idLead + ")</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Documento de Identidad:</td><td><b>" + ci + "</b></td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Fecha de Nacimiento:</td><td>" + fechaNac + "</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Ciudad de Residencia:</td><td>" + ciudad + "</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Estado Expediente:</td><td><span style='color: #1e7e34; font-weight: bold;'>[COMPLETO PARA MATRICULA]</span></td></tr>" +
                         "</table>";

        String html = HtmlTemplate.successCard("Datos Academicos Registrados", "El expediente del prospecto cuenta con todos los requisitos para formalizar la matriculacion.", details);
        return CommandResult.success(Config.SUBJECT_PREFIX + " Datos de Alumno Registrados Lead #" + idLead, html);
    }

    /**
     * Comando: CONVERTIRAMATRICULA["id_lead"]
     * Formaliza la matriculacion academica (Etapa 5: Convertido) verificando pago y expediente completo (Exclusivo Administrador).
     */
    public CommandResult convertirAMatricula(UsuarioAuth auth, ParsedCommand cmd) throws Exception {
        SecurityContext.validarPersonal(auth);

        if (!auth.esAdmin()) {
            throw new CommandException(
                "Acceso denegado: La formalizacion de matricula es exclusiva para el Administrador del sistema.",
                "Su rol actual es '" + auth.getRolNombre() + "'."
            );
        }

        if (!cmd.hasParams()) {
            throw new CommandException(
                "Debe especificar el ID del prospecto a matricular.",
                "Sintaxis: CONVERTIRAMATRICULA[\"id_lead\"]. Ejemplo: CONVERTIRAMATRICULA[\"15\"]"
            );
        }

        long idLead = cmd.getIntParam(0, "id_lead");
        PagoMatriculaDao.DatosAlumnoDTO alumno = pagoDao.obtenerDatosAlumno(idLead);
        if (alumno == null) {
            throw new CommandException("No existe ningun prospecto registrado con el ID #" + idLead + ".");
        }

        if (alumno.getIdEtapa() == 5L) {
            throw new CommandException(
                "El prospecto #" + idLead + " (" + alumno.getNombre() + ") ya se encuentra matriculado como alumno oficial.",
                "Etapa actual: Convertido."
            );
        }

        // 1. Verificación de expediente de datos
        if (!alumno.tieneDatosCompletos()) {
            throw new CommandException(
                "No se puede matricular al prospecto: Faltan datos obligatorios en su expediente academico.",
                "Asegurese de registrar CI, fecha de nacimiento y ciudad mediante el comando: COMPLETARDATOSALUMNO[\"" + idLead + "\", \"ci\", \"fecha_nac\", \"ciudad\"]."
            );
        }

        // 2. Verificación de pago aprobado
        Long idCompAprobado = pagoDao.obtenerIdComprobanteAprobado(idLead);
        if (idCompAprobado == null) {
            throw new CommandException(
                "No se puede matricular al prospecto: No cuenta con ningun comprobante de pago aprobado.",
                "Registre el comprobante con REGISTRARCOMPROBANTE y apruebelo formalmente con APROBARPAGO[\"id_comprobante\"]."
            );
        }

        // Generar payload institucional JSON
        String payloadJson = "{\"crm_lead_id\": \"LEAD-" + idLead + "\", " +
                             "\"name\": \"" + alumno.getNombre().replace("\"", "\\\"") + "\", " +
                             "\"documento\": \"" + alumno.getCi() + "\", " +
                             "\"email\": \"" + alumno.getCorreo() + "\", " +
                             "\"telefono\": \"" + alumno.getTelefono() + "\", " +
                             "\"ciudad\": \"" + alumno.getCiudad() + "\", " +
                             "\"matricula_oficial\": true}";

        boolean ok = pagoDao.formalizarMatricula(idLead, idCompAprobado, payloadJson, auth.getIdUsuario());
        if (!ok) {
            throw new CommandException("No se pudo formalizar la matricula del prospecto.");
        }

        String details = "<div style='border: 2px solid #00264d; padding: 16px; margin: 10px 0; background-color: #fafafa;'>" +
                         "  <div style='text-align: center; margin-bottom: 12px; border-bottom: 1px solid #cccccc; padding-bottom: 8px;'>" +
                         "    <h3 style='margin: 0; color: #00264d; font-size: 16px; text-transform: uppercase;'>Constancia Oficial de Matricula Academica</h3>" +
                         "    <div style='font-size: 11px; color: #555555;'>Registro Oficial de Alumno Regular - Postgrado UNEXO</div>" +
                         "  </div>" +
                         "  <table style='width: 100%; border-collapse: collapse; font-size: 13px;'>" +
                         "    <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold; width: 180px;'>Codigo de Matricula:</td><td><b>MATR-2026-" + idLead + "</b></td></tr>" +
                         "    <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Alumno Regular:</td><td><b>" + alumno.getNombre() + "</b></td></tr>" +
                         "    <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Cedula de Identidad:</td><td>" + alumno.getCi() + "</td></tr>" +
                         "    <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Correo Electronico:</td><td>" + alumno.getCorreo() + "</td></tr>" +
                         "    <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Ciudad de Residencia:</td><td>" + alumno.getCiudad() + "</td></tr>" +
                         "    <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Comprobante Asociado:</td><td>Comprobante Aprobado #" + idCompAprobado + "</td></tr>" +
                         "    <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Etapa Actual:</td><td>" + HtmlTemplate.badgeEtapa(5, "Convertido") + "</td></tr>" +
                         "    <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Autorizado Por:</td><td>" + auth.getNombreCompleto() + " (Administrador)</td></tr>" +
                         "  </table>" +
                         "</div>" +
                         "<p style='font-size: 12px; color: #1e7e34; font-weight: bold;'>El prospecto culmino exitosamente el embudo comercial y se encuentra matriculado como alumno regular.</p>";

        String html = HtmlTemplate.wrapContainer("Formalizacion de Matricula Academica", "MATRICULA", details);
        return CommandResult.success(Config.SUBJECT_PREFIX + " Matricula Oficial #" + idLead + " Formalizada", html);
    }

    private void validarAdminOVendedor(UsuarioAuth auth) throws CommandException {
        SecurityContext.validarPersonal(auth);
        if (!auth.esAdmin() && !auth.esVendedor()) {
            throw new CommandException(
                "Acceso denegado: El registro de comprobantes y expediente estudiantil esta reservado a Administrador y Vendedor.",
                "El rol Coordinador se enfoca en el seguimiento de programas y reportes academicos."
            );
        }
    }
}
