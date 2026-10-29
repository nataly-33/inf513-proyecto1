package com.tecnoweb.grupo05sc.service;

import com.tecnoweb.grupo05sc.command.CommandException;
import com.tecnoweb.grupo05sc.command.CommandResult;
import com.tecnoweb.grupo05sc.command.ParsedCommand;
import com.tecnoweb.grupo05sc.config.Config;
import com.tecnoweb.grupo05sc.dao.UsuarioDao;
import com.tecnoweb.grupo05sc.security.SecurityContext;
import com.tecnoweb.grupo05sc.security.UsuarioAuth;
import com.tecnoweb.grupo05sc.view.HtmlTemplate;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Servicio de Negocio para el Caso de Uso 1 (CU1: Gestión de Usuarios).
 * Responsable oficial: Nataly (Dev A).
 *
 * Implementa las reglas de negocio institucionales:
 *   - Control de acceso exclusivo para rol Administrador.
 *   - Regla de oro de seguridad: Protección contra autobloqueo del Administrador.
 *   - Validación de formato y unicidad de correo institucional.
 *   - Hasheo seguro de contraseñas con estándar criptográfico SHA-256 (sin dependencias externas).
 *   - Regulación de participación en asignaciones equitativas por Round-Robin.
 */
public final class UsuarioService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");

    private final UsuarioDao usuarioDao;

    public UsuarioService() {
        this.usuarioDao = new UsuarioDao();
    }

    public UsuarioService(UsuarioDao usuarioDao) {
        this.usuarioDao = usuarioDao;
    }

    /**
     * Comando: LISTARUSUARIOS[]
     * Devuelve la lista completa de usuarios con sus roles, estados y participación en Round-Robin.
     */
    public CommandResult listarUsuarios(UsuarioAuth auth) throws Exception {
        SecurityContext.validarAdmin(auth);

        List<UsuarioDao.UsuarioDTO> usuarios = usuarioDao.listarTodos();

        String[] headers = new String[]{"ID", "NOMBRE COMPLETO", "CORREO", "ROL", "ESTADO", "ROUND-ROBIN", "ÚLTIMO ACCESO"};
        List<String[]> rows = new ArrayList<>();

        for (UsuarioDao.UsuarioDTO u : usuarios) {
            String badgeEstado = u.isActivo()
                ? "<span style='color:#28a745; font-weight:bold;'>Activo</span>"
                : "<span style='color:#dc3545; font-weight:bold;'>Inactivo</span>";

            String badgeRR = u.isParticipaRoundRobin()
                ? "<span style='color:#0051f9; font-weight:bold;'>Sí</span>"
                : "<span style='color:#6a737b;'>No</span>";

            rows.add(new String[]{
                String.valueOf(u.getIdUsuario()),
                u.getNombreCompleto(),
                u.getCorreo(),
                "<b>" + u.getRolNombre() + "</b>",
                badgeEstado,
                badgeRR,
                u.getUltimoAcceso()
            });
        }

        String summary = "<p style='font-size:13px; color:#6a737b;'>Total de usuarios registrados en el sistema: <b>" + usuarios.size() + "</b></p>";
        String tableHtml = HtmlTemplate.table(headers, rows);
        String html = HtmlTemplate.wrapContainer("Gestión de Usuarios (CU1 – Nataly)", "ADMIN", summary + tableHtml);

        return CommandResult.success(Config.SUBJECT_PREFIX + " Listado de Usuarios", html);
    }

    /**
     * Comando: BUSCARUSUARIO["correo_o_id"]
     */
    public CommandResult buscarUsuario(UsuarioAuth auth, ParsedCommand cmd) throws Exception {
        SecurityContext.validarAdmin(auth);

        if (!cmd.hasParams()) {
            throw new CommandException(
                "Debe especificar el ID numérico o correo del usuario a buscar.",
                "Sintaxis: BUSCARUSUARIO[\"criterio_busqueda\"]. Ejemplo: BUSCARUSUARIO[\"admin@crm.bo\"]"
            );
        }

        String criterio = cmd.getParam(0);
        UsuarioDao.UsuarioDTO u = usuarioDao.buscarPorIdOCorreo(criterio);

        if (u == null) {
            throw new CommandException(
                "No se encontró ningún usuario con el criterio de búsqueda: \"" + criterio + "\".",
                "Verifique el correo o ID ingresado o envíe LISTARUSUARIOS[] para ver todos los usuarios."
            );
        }

        String badgeEstado = u.isActivo()
            ? "<span style='background:#d4edda; color:#155724; padding:3px 8px; border-radius:4px; font-weight:bold;'>Cuenta Activa</span>"
            : "<span style='background:#f8d7da; color:#721c24; padding:3px 8px; border-radius:4px; font-weight:bold;'>Cuenta Desactivada</span>";

        String details = "<div style='font-size:14px; line-height:1.6;'>" +
                         "  <p><b>Ficha de Usuario:</b></p>" +
                         "  <ul>" +
                         "    <li><b>ID Usuario:</b> " + u.getIdUsuario() + "</li>" +
                         "    <li><b>Nombre Completo:</b> " + u.getNombreCompleto() + "</li>" +
                         "    <li><b>Correo Institucional:</b> " + u.getCorreo() + "</li>" +
                         "    <li><b>Rol Asignado:</b> <span style='background:#ebf0f9; padding:2px 8px; border-radius:4px; font-weight:bold;'>" + u.getRolNombre() + "</span></li>" +
                         "    <li><b>Estado Operativo:</b> " + badgeEstado + "</li>" +
                         "    <li><b>Participación en Round-Robin:</b> " + (u.isParticipaRoundRobin() ? "Activa" : "Excluida") + "</li>" +
                         "    <li><b>Último Acceso Registrado:</b> " + u.getUltimoAcceso() + "</li>" +
                         "    <li><b>Fecha de Alta:</b> " + u.getCreatedAt() + "</li>" +
                         "  </ul>" +
                         "</div>";

        String html = HtmlTemplate.wrapContainer("Detalle de Usuario", "CONSULTA", details);
        return CommandResult.success(Config.SUBJECT_PREFIX + " Usuario " + u.getNombreCompleto(), html);
    }

    /**
     * Comando: CREARUSUARIO["nombre", "correo", "password", "rol"]
     */
    public CommandResult crearUsuario(UsuarioAuth auth, ParsedCommand cmd) throws Exception {
        SecurityContext.validarAdmin(auth);

        if (cmd.paramCount() < 4) {
            throw new CommandException(
                "Parámetros insuficientes para dar de alta un usuario. Se requieren 4 parámetros.",
                "Sintaxis: CREARUSUARIO[\"nombre_completo\", \"correo\", \"password\", \"rol\"]"
            );
        }

        String nombre = cmd.getParam(0).trim();
        String correo = cmd.getParam(1).trim();
        String password = cmd.getParam(2);
        String rol = cmd.getParam(3).trim();

        if (nombre.isEmpty() || correo.isEmpty() || password.isEmpty() || rol.isEmpty()) {
            throw new CommandException(
                "Ninguno de los parámetros (nombre, correo, contraseña, rol) puede estar vacío.",
                "Complete todos los campos requeridos."
            );
        }

        if (!EMAIL_PATTERN.matcher(correo).matches()) {
            throw new CommandException(
                "El formato del correo electrónico '" + correo + "' es inválido.",
                "Ingrese un correo válido con formato usuario@dominio.com"
            );
        }

        if (usuarioDao.existeCorreo(correo)) {
            throw new CommandException(
                "El correo institucional '" + correo + "' ya se encuentra registrado para otra cuenta.",
                "Utilice una dirección de correo diferente o verifique el listado existente con LISTARUSUARIOS[]."
            );
        }

        Long idRol = usuarioDao.obtenerIdRolPorNombre(rol);
        if (idRol == null) {
            throw new CommandException(
                "El rol '" + rol + "' no existe en el sistema.",
                "Los roles institucionales permitidos son: 'Administrador', 'Coordinador', 'Vendedor'."
            );
        }

        String passwordHash = hashPassword(password);
        long newId = usuarioDao.insertar(nombre, correo, passwordHash, idRol);

        String details = "<p style='font-size:13px;'>" +
                         "• <b>ID Asignado:</b> " + newId + "<br>" +
                         "• <b>Nombre Completo:</b> " + nombre + "<br>" +
                         "• <b>Correo Registrado:</b> " + correo + "<br>" +
                         "• <b>Rol:</b> " + rol + "<br>" +
                         "• <b>Estado Inicial:</b> Activo<br>" +
                         "• <b>Round-Robin:</b> " + (idRol == 3 ? "Habilitado (Vendedor)" : "Deshabilitado (Personal no comercial)") + "</p>";

        String html = HtmlTemplate.successCard("Usuario Creado Exitosamente", "La cuenta de usuario ha sido persistida en PostgreSQL.", details);
        return CommandResult.success(Config.SUBJECT_PREFIX + " Alta de Usuario Exitosa", html);
    }

    /**
     * Comando: MODIFICARUSUARIO["id", "nombre", "correo", "rol"]
     */
    public CommandResult modificarUsuario(UsuarioAuth auth, ParsedCommand cmd) throws Exception {
        SecurityContext.validarAdmin(auth);

        if (cmd.paramCount() < 4) {
            throw new CommandException(
                "Parámetros insuficientes para modificar el usuario. Se requieren 4 parámetros.",
                "Sintaxis: MODIFICARUSUARIO[\"id_usuario\", \"nombre_completo\", \"correo\", \"rol\"]"
            );
        }

        long idUsuario = cmd.getIntParam(0, "id_usuario");
        String nombre = cmd.getParam(1).trim();
        String correo = cmd.getParam(2).trim();
        String rol = cmd.getParam(3).trim();

        if (nombre.isEmpty() || correo.isEmpty() || rol.isEmpty()) {
            throw new CommandException("Los campos nombre, correo y rol no pueden estar vacíos.");
        }

        UsuarioDao.UsuarioDTO actual = usuarioDao.buscarPorId(idUsuario);
        if (actual == null) {
            throw new CommandException("No existe ningún usuario registrado con el ID #" + idUsuario + ".");
        }

        if (!EMAIL_PATTERN.matcher(correo).matches()) {
            throw new CommandException("El formato del correo electrónico '" + correo + "' es inválido.");
        }

        if (usuarioDao.existeCorreoExcepto(correo, idUsuario)) {
            throw new CommandException("El correo '" + correo + "' ya está siendo utilizado por otro usuario.");
        }

        Long idRol = usuarioDao.obtenerIdRolPorNombre(rol);
        if (idRol == null) {
            throw new CommandException("Rol '" + rol + "' inválido. Roles válidos: Administrador, Coordinador, Vendedor.");
        }

        usuarioDao.actualizar(idUsuario, nombre, correo, idRol);

        String details = "<p style='font-size:13px;'>" +
                         "• <b>ID Usuario:</b> " + idUsuario + "<br>" +
                         "• <b>Nuevo Nombre:</b> " + nombre + "<br>" +
                         "• <b>Nuevo Correo:</b> " + correo + "<br>" +
                         "• <b>Nuevo Rol:</b> " + rol + "</p>";

        String html = HtmlTemplate.successCard("Usuario Modificado", "Los datos institucionales han sido actualizados satisfactoriamente.", details);
        return CommandResult.success(Config.SUBJECT_PREFIX + " Modificación de Usuario #" + idUsuario, html);
    }

    /**
     * Comando: TOGGLEUSUARIO["id"]
     * Alterna entre Activo e Inactivo.
     * REGLA DE ORO DE SEGURIDAD: El Administrador autenticado no puede autobloquearse.
     */
    public CommandResult toggleUsuario(UsuarioAuth auth, ParsedCommand cmd) throws Exception {
        SecurityContext.validarAdmin(auth);

        if (!cmd.hasParams()) {
            throw new CommandException(
                "Debe especificar el ID del usuario a activar o desactivar.",
                "Sintaxis: TOGGLEUSUARIO[\"id_usuario\"]. Ejemplo: TOGGLEUSUARIO[\"3\"]"
            );
        }

        long idUsuario = cmd.getIntParam(0, "id_usuario");

        // REGLA DE SEGURIDAD VITAL DEL DOCENTE:
        if (idUsuario == auth.getIdUsuario()) {
            throw new CommandException(
                "Operación denegada por seguridad: El Administrador no puede desactivar ni bloquear su propia cuenta.",
                "Para dar de baja esta cuenta administrativa, otro usuario con rol Administrador debe ejecutar la acción."
            );
        }

        UsuarioDao.UsuarioDTO target = usuarioDao.buscarPorId(idUsuario);
        if (target == null) {
            throw new CommandException("No existe ningún usuario registrado con el ID #" + idUsuario + ".");
        }

        boolean ok = usuarioDao.toggleActivo(idUsuario);
        if (!ok) {
            throw new CommandException("No se pudo alternar el estado operativo del usuario.");
        }

        UsuarioDao.UsuarioDTO updated = usuarioDao.buscarPorId(idUsuario);
        boolean nuevoEstado = updated.isActivo();

        String mensajeEstado = nuevoEstado
            ? "<span style='color:#28a745; font-weight:bold;'>ACTIVADA</span> (El usuario ahora puede acceder al sistema)"
            : "<span style='color:#dc3545; font-weight:bold;'>DESACTIVADA</span> (El acceso al sistema ha sido revocado)";

        String details = "<p style='font-size:13px;'>" +
                         "• <b>Usuario:</b> " + updated.getNombreCompleto() + " (" + updated.getCorreo() + ")<br>" +
                         "• <b>Rol:</b> " + updated.getRolNombre() + "<br>" +
                         "• <b>Nuevo Estado:</b> " + mensajeEstado + "</p>";

        String html = HtmlTemplate.successCard("Estado de Usuario Actualizado", "Se alternó con éxito la disponibilidad de la cuenta.", details);
        return CommandResult.success(Config.SUBJECT_PREFIX + " Estado Usuario #" + idUsuario, html);
    }

    /**
     * Comando: TOGGLEROUNDROBIN["id"]
     * Incluye o excluye a un vendedor del reparto automático equitativo.
     */
    public CommandResult toggleRoundRobin(UsuarioAuth auth, ParsedCommand cmd) throws Exception {
        SecurityContext.validarAdmin(auth);

        if (!cmd.hasParams()) {
            throw new CommandException(
                "Debe especificar el ID del usuario para alternar su participación en Round-Robin.",
                "Sintaxis: TOGGLEROUNDROBIN[\"id_usuario\"]. Ejemplo: TOGGLEROUNDROBIN[\"3\"]"
            );
        }

        long idUsuario = cmd.getIntParam(0, "id_usuario");
        UsuarioDao.UsuarioDTO target = usuarioDao.buscarPorId(idUsuario);

        if (target == null) {
            throw new CommandException("No existe ningún usuario registrado con el ID #" + idUsuario + ".");
        }

        // Validación de lógica de negocio: Round-Robin solo tiene efecto comercial sobre Vendedores
        if (target.getIdRol() != 3) {
            throw new CommandException(
                "El usuario #" + idUsuario + " (" + target.getNombreCompleto() + ") tiene rol '" + target.getRolNombre() + "'.",
                "La asignación automática Round-Robin solo es aplicable a usuarios con rol 'Vendedor'."
            );
        }

        usuarioDao.toggleRoundRobin(idUsuario);
        UsuarioDao.UsuarioDTO updated = usuarioDao.buscarPorId(idUsuario);

        String rrMsg = updated.isParticipaRoundRobin()
            ? "<span style='color:#0051f9; font-weight:bold;'>HABILITADO</span> (Recibirá nuevos prospectos automáticamente en la rotación)"
            : "<span style='color:#6a737b; font-weight:bold;'>EXCLUIDO</span> (No recibirá prospectos automáticos hasta nuevo aviso)";

        String details = "<p style='font-size:13px;'>" +
                         "• <b>Vendedor:</b> " + updated.getNombreCompleto() + "<br>" +
                         "• <b>Correo:</b> " + updated.getCorreo() + "<br>" +
                         "• <b>Participación Round-Robin:</b> " + rrMsg + "</p>";

        String html = HtmlTemplate.successCard("Configuración Round-Robin Actualizada", "Se actualizó la participación en la bolsa comercial.", details);
        return CommandResult.success(Config.SUBJECT_PREFIX + " Round-Robin Vendedor #" + idUsuario, html);
    }

    /**
     * Genera un hash criptográfico SHA-256 estándar para almacenar contraseñas.
     * Cero librerías externas (utiliza java.security.MessageDigest).
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error en algoritmo de hash: " + e.getMessage());
        }
    }
}
