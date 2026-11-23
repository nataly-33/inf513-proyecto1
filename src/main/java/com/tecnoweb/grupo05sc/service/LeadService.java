package com.tecnoweb.grupo05sc.service;

import com.tecnoweb.grupo05sc.command.CommandException;
import com.tecnoweb.grupo05sc.command.CommandResult;
import com.tecnoweb.grupo05sc.command.ParsedCommand;
import com.tecnoweb.grupo05sc.config.Config;
import com.tecnoweb.grupo05sc.dao.LeadDao;
import com.tecnoweb.grupo05sc.security.UsuarioAuth;
import com.tecnoweb.grupo05sc.view.HtmlTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Servicio de Negocio para el Caso de Uso 2 (CU2: Captacion de Leads y Oferta Academica).
 * Responsable oficial: Jael (Dev C).
 *
 * Implementa las reglas institucionales:
 *   - Captacion de prospectos interesados desde multiples canales (Redes, Web, Referidos).
 *   - Validacion de datos minimos de contacto (Nombre y Telefono obligatorios).
 *   - Asignacion automatica a Etapa 1 (Nuevo) e ingreso inmediato a la Bolsa Comun.
 *   - Vinculacion de intereses de la oferta academica (Programas y Modulos).
 *   - Registro automatico de auditoria en la Bitacora institucional.
 */
public final class LeadService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");

    private final LeadDao leadDao;

    public LeadService() {
        this.leadDao = new LeadDao();
    }

    public LeadService(LeadDao leadDao) {
        this.leadDao = leadDao;
    }

    /**
     * Comando: REGISTRARPROSPECTO["nombre", "telefono", "correo", "origen", "profesion"]
     * Permite dar de alta a un interesado en el sistema y ubicarlo en la Bolsa Comun.
     */
    public CommandResult registrarProspecto(UsuarioAuth auth, ParsedCommand cmd) throws Exception {
        if (cmd.paramCount() < 2) {
            throw new CommandException(
                "Parametros insuficientes. Se requiere como minimo el nombre y telefono del prospecto.",
                "Sintaxis: REGISTRARPROSPECTO[\"nombre\", \"telefono\", \"correo\", \"origen\", \"profesion\"]. " +
                "Ejemplo: REGISTRARPROSPECTO[\"Mario Becerra\", \"70011223\", \"mario@gmail.com\", \"Facebook\", \"Ingeniero de Sistemas\"]"
            );
        }

        String nombre = cmd.getParam(0).trim();
        String telefono = cmd.getParam(1).trim();
        String correo = cmd.getParamOrDefault(2, "").trim();
        String origen = cmd.getParamOrDefault(3, "Web").trim();
        String profesion = cmd.getParamOrDefault(4, "").trim();

        if (nombre.isEmpty() || telefono.isEmpty()) {
            throw new CommandException(
                "El nombre y el telefono del prospecto no pueden estar vacios.",
                "Complete al menos el nombre completo y numero de contacto."
            );
        }

        // Validación estricta de teléfono (7 u 8 dígitos, celulares inician en 6 o 7, fijos en 2, 3 o 4)
        String cleanPhone = telefono.replaceAll("[\\s\\-\\+\\(\\)]", "");
        if (cleanPhone.startsWith("591")) {
            cleanPhone = cleanPhone.substring(3);
        }

        if (!cleanPhone.matches("\\d+")) {
            throw new CommandException(
                "El telefono '" + telefono + "' contiene caracteres invalidos.",
                "El numero telefonico debe contener unicamente digitos numericos."
            );
        }

        if (cleanPhone.length() < 7 || cleanPhone.length() > 8) {
            throw new CommandException(
                "El telefono '" + telefono + "' debe tener 7 u 8 digitos numericos.",
                "Ejemplo: Celular de 8 digitos como 70011223 o telefono fijo de 7 digitos como 3344556."
            );
        }

        if (cleanPhone.length() == 8 && !cleanPhone.startsWith("6") && !cleanPhone.startsWith("7")) {
            throw new CommandException(
                "El numero celular de 8 digitos '" + cleanPhone + "' es invalido.",
                "En Bolivia los telefonos celulares deben iniciar con 6 o 7 (ej. 70011223, 68901234)."
            );
        }

        if (cleanPhone.length() == 7 && !cleanPhone.startsWith("2") && !cleanPhone.startsWith("3") && !cleanPhone.startsWith("4")) {
            throw new CommandException(
                "El telefono fijo de 7 digitos '" + cleanPhone + "' es invalido.",
                "En Bolivia los telefonos fijos inician con 3 (Santa Cruz), 2 (La Paz) o 4 (Cochabamba)."
            );
        }

        if (!correo.isEmpty() && !EMAIL_PATTERN.matcher(correo).matches()) {
            throw new CommandException(
                "El formato del correo electronico '" + correo + "' es invalido.",
                "Ingrese un correo valido o deje el campo vacio \"\" si no dispone de correo."
            );
        }

        // Verificación y resolución de canal de origen válido
        Long idOrigen = leadDao.buscarIdOrigen(origen);
        if (idOrigen == null) {
            List<LeadDao.CatalogoDTO> origenes = leadDao.listarOrigenes();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < origenes.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(origenes.get(i).getNombre());
            }
            throw new CommandException(
                "El canal de origen '" + origen + "' no es valido.",
                "Origenes validos: " + sb.toString() + ". Ejecute el comando LISTARORIGENES[] para consultar el catalogo disponible."
            );
        }

        Long idProfesion = null;
        if (!profesion.isEmpty()) {
            idProfesion = leadDao.obtenerIdProfesionPorNombre(profesion);
            if (idProfesion == null) {
                List<LeadDao.CatalogoDTO> profs = leadDao.listarProfesiones();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < profs.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(profs.get(i).getNombre());
                }
                throw new CommandException(
                    "La profesion '" + profesion + "' no coincide con ninguna del catalogo.",
                    "Profesiones disponibles: " + sb.toString() + ". Ingrese una profesion valida (ej. 'Sistemas', 'Informatica', 'Derecho') o consulte con LISTARPROFESIONES[]."
                );
            }
        }
        long createdBy = (auth != null) ? auth.getIdUsuario() : 1L; // 1 = Admin por defecto si es invitado

        long newLeadId = leadDao.insertarLead(nombre, cleanPhone, correo, idOrigen, idProfesion, createdBy);
        LeadDao.LeadDTO lead = leadDao.buscarPorId(newLeadId);

        String details = "<table style='width: 100%; border-collapse: collapse; font-size: 13px;'>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold; width: 180px;'>ID Prospecto:</td><td><b>#" + lead.getIdLead() + "</b></td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Nombre Completo:</td><td>" + lead.getNombre() + "</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Telefono de Contacto:</td><td>" + lead.getTelefono() + "</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Correo Electronico:</td><td>" + lead.getCorreo() + "</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Canal de Origen:</td><td>" + lead.getOrigenNombre() + "</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Area / Profesion:</td><td>" + lead.getProfesionNombre() + "</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Etapa Inicial:</td><td>" + HtmlTemplate.badgeEtapa(1, "Nuevo") + "</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Estado de Asignacion:</td><td><span style='color: #00264d; font-weight: bold;'>Bolsa Comun (Pendiente de Vendedor)</span></td></tr>" +
                         "</table>" +
                         "<p style='font-size: 12px; color: #555555; margin-top: 10px;'>El prospecto ha sido ingresado exitosamente y registrado en la bitacora institucional.</p>";

        String html = HtmlTemplate.successCard("Captacion de Prospecto", "Prospecto registrado satisfactoriamente en el CRM.", details);
        return CommandResult.success(Config.SUBJECT_PREFIX + " Prospecto #" + newLeadId + " Registrado", html);
    }

    /**
     * Comando: VINCULARINTERES["id_lead", "tipo_oferta", "id_externo", "nombre_curso"]
     * Vincula un programa o modulo de la oferta academica al prospecto.
     */
    public CommandResult vincularInteres(UsuarioAuth auth, ParsedCommand cmd) throws Exception {
        if (cmd.paramCount() < 4) {
            throw new CommandException(
                "Parametros insuficientes para vincular interes academico. Se requieren 4 parametros.",
                "Sintaxis: VINCULARINTERES[\"id_lead\", \"tipo_oferta\", \"id_externo\", \"nombre_curso\"]. " +
                "Ejemplo: VINCULARINTERES[\"15\", \"PROGRAMA\", \"101\", \"Diplomado en Ciberseguridad Defensiva\"]"
            );
        }

        long idLead = cmd.getIntParam(0, "id_lead");
        String tipo = cmd.getParam(1).trim().toUpperCase();
        long idExterno = cmd.getIntParam(2, "id_externo");
        String nombreCurso = cmd.getParam(3).trim();

        if (!"PROGRAMA".equals(tipo) && !"MODULO".equals(tipo)) {
            throw new CommandException(
                "El tipo de oferta '" + tipo + "' es invalido.",
                "Los tipos permitidos son: 'PROGRAMA' o 'MODULO'."
            );
        }

        // Verificación contra el catálogo oficial de oferta académica
        LeadDao.OfertaItem oferta = LeadDao.buscarOferta(tipo, idExterno);
        if (oferta == null) {
            throw new CommandException(
                "El ID externo #" + idExterno + " no corresponde a ningun " + tipo.toLowerCase() + " disponible en la oferta academica.",
                tipo + "s disponibles: " + LeadDao.resumenOferta(tipo) + ". Ejecute el comando LISTAROFERTA[] o HELP[\"VINCULARINTERES\"] para consultar codigos."
            );
        }

        if (nombreCurso.isEmpty() || nombreCurso.equalsIgnoreCase(tipo)) {
            nombreCurso = oferta.getNombre();
        }

        LeadDao.LeadDTO lead = leadDao.buscarPorId(idLead);
        if (lead == null) {
            throw new CommandException("No existe ningun prospecto registrado con el ID #" + idLead + ".");
        }

        // Control de permisos: Personal institucional (Admin, Coord, Vend asignado) o el propio prospecto por correo
        boolean autorizado = false;
        if (auth != null && auth.esPersonal()) {
            if (auth.esVendedor()) {
                if (lead.getIdVendedor() != null && lead.getIdVendedor() == auth.getIdUsuario()) {
                    autorizado = true;
                } else {
                    throw new CommandException(
                        "Acceso denegado: El prospecto #" + idLead + " no pertenece a su cartera comercial.",
                        "Solo puede vincular ofertas a prospectos asignados a su cuenta."
                    );
                }
            } else {
                autorizado = true; // Admin o Coordinador
            }
        } else if (auth != null && lead.getCorreo() != null && !lead.getCorreo().isEmpty() &&
                   auth.getEmail().equalsIgnoreCase(lead.getCorreo().trim())) {
            autorizado = true; // El propio prospecto registrando su interés
        }

        if (!autorizado) {
            throw new CommandException(
                "Acceso denegado: Remitente no autorizado para vincular intereses en este prospecto.",
                "Solo el vendedor asignado, el administrador o el propio interesado (" + lead.getCorreo() + ") pueden gestionar intereses."
            );
        }

        long idInteres = leadDao.vincularInteres(idLead, tipo, idExterno, nombreCurso);

        String details = "<table style='width: 100%; border-collapse: collapse; font-size: 13px;'>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold; width: 180px;'>ID Registro Interes:</td><td>#" + idInteres + "</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Prospecto Beneficiario:</td><td><b>" + lead.getNombre() + "</b> (Lead #" + idLead + ")</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Tipo de Oferta:</td><td>" + tipo + "</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>ID Catalogo Externo:</td><td>#" + idExterno + "</td></tr>" +
                         "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 5px 0; font-weight: bold;'>Programa / Modulo:</td><td><b>" + nombreCurso + "</b></td></tr>" +
                         "</table>";

        String html = HtmlTemplate.successCard("Interes Academico Vinculado", "El interes fue guardado en el expediente del prospecto y registrado en bitacora.", details);
        return CommandResult.success(Config.SUBJECT_PREFIX + " Interes Vinculado a Lead #" + idLead, html);
    }

    /**
     * Comando: LISTARINTERESES["id_lead"]
     * Muestra todos los intereses academicos asociados a un prospecto.
     */
    public CommandResult listarIntereses(UsuarioAuth auth, ParsedCommand cmd) throws Exception {
        if (!cmd.hasParams()) {
            throw new CommandException(
                "Debe especificar el ID del prospecto para consultar sus intereses academicos.",
                "Sintaxis: LISTARINTERESES[\"id_lead\"]. Ejemplo: LISTARINTERESES[\"15\"]"
            );
        }

        long idLead = cmd.getIntParam(0, "id_lead");
        LeadDao.LeadDTO lead = leadDao.buscarPorId(idLead);
        if (lead == null) {
            throw new CommandException("No existe ningun prospecto registrado con el ID #" + idLead + ".");
        }

        List<LeadDao.LeadInteresDTO> intereses = leadDao.listarIntereses(idLead);

        String[] headers = new String[]{"#", "TIPO", "ID EXTERNO", "CURSO / PROGRAMA DE INTERES"};
        List<String[]> rows = new ArrayList<>();

        for (LeadDao.LeadInteresDTO item : intereses) {
            long extId = (item.getIdVersionExterno() != null) ? item.getIdVersionExterno() : (item.getIdModuloExterno() != null ? item.getIdModuloExterno() : 0L);
            rows.add(new String[]{
                String.valueOf(item.getIdInteres()),
                item.getTipo(),
                "#" + extId,
                "<b>" + item.getNombreSnapshot() + "</b>"
            });
        }

        String summary = "<p style='font-size: 13px;'>Intereses academicos registrados para el prospecto: <b>" + lead.getNombre() + "</b> (Lead #" + idLead + ")</p>";
        String tableHtml = HtmlTemplate.table(headers, rows);
        String html = HtmlTemplate.wrapContainer("Oferta de Interes - Lead #" + idLead, "OFERTA", summary + tableHtml);

        return CommandResult.success(Config.SUBJECT_PREFIX + " Intereses Lead #" + idLead, html);
    }

    /**
     * Comando: LISTARORIGENES[]
     * Muestra el catalogo de canales de captacion.
     */
    public CommandResult listarOrigenes(UsuarioAuth auth) throws Exception {
        List<LeadDao.CatalogoDTO> lista = leadDao.listarOrigenes();
        String[] headers = new String[]{"ID ORIGEN", "CANAL DE CAPTACION"};
        List<String[]> rows = new ArrayList<>();

        for (LeadDao.CatalogoDTO c : lista) {
            rows.add(new String[]{String.valueOf(c.getId()), c.getNombre()});
        }

        String html = HtmlTemplate.wrapContainer("Canales de Captacion Institucionales", "CATALOGO", HtmlTemplate.table(headers, rows));
        return CommandResult.success(Config.SUBJECT_PREFIX + " Canales de Origen", html);
    }

    /**
     * Comando: LISTARPROFESIONES[]
     * Muestra el catalogo de profesiones u ocupaciones.
     */
    public CommandResult listarProfesiones(UsuarioAuth auth) throws Exception {
        List<LeadDao.CatalogoDTO> lista = leadDao.listarProfesiones();
        String[] headers = new String[]{"ID", "PROFESION / AREA ACADEMICA"};
        List<String[]> rows = new ArrayList<>();

        for (LeadDao.CatalogoDTO c : lista) {
            rows.add(new String[]{String.valueOf(c.getId()), c.getNombre()});
        }

        String html = HtmlTemplate.wrapContainer("Catalogo de Profesiones Registradas", "CATALOGO", HtmlTemplate.table(headers, rows));
        return CommandResult.success(Config.SUBJECT_PREFIX + " Catalogo de Profesiones", html);
    }

    /**
     * Comando: LISTAROFERTA[]
     * Muestra el catalogo completo de programas y modulos ofertados.
     */
    public CommandResult listarOferta(UsuarioAuth auth) throws Exception {
        List<LeadDao.OfertaItem> items = LeadDao.getCatalogoOferta();
        String[] headers = new String[]{"ID", "TIPO", "CODIGO", "PROGRAMA / MODULO", "DURACION", "INVERSION (BS.)"};
        List<String[]> rows = new ArrayList<>();

        for (LeadDao.OfertaItem item : items) {
            rows.add(new String[]{
                "#" + item.getId(),
                item.getTipo(),
                item.getCodigo(),
                "<b>" + item.getNombre() + "</b>",
                item.getDuracion(),
                String.format("%.2f Bs.", item.getCostoBs())
            });
        }

        String summary = "<p style='font-size: 13px; margin-top: 0;'>Oferta academica oficial de programas y modulos de postgrado y capacitacion continua.</p>";
        String tableHtml = HtmlTemplate.table(headers, rows);
        String html = HtmlTemplate.wrapContainer("Oferta Academica Disponible", "CATALOGO", summary + tableHtml);

        return CommandResult.success(Config.SUBJECT_PREFIX + " Oferta Academica (" + items.size() + " cursos)", html);
    }
}
