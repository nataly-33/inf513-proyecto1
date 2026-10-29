package com.tecnoweb.grupo05sc.security;

import com.tecnoweb.grupo05sc.command.CommandException;
import com.tecnoweb.grupo05sc.config.DatabaseConnection;
import com.tecnoweb.grupo05sc.mail.POP3Listener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Gestiona el contexto de seguridad y el Control de Acceso Basado en Roles (RBAC)
 * a partir de la identidad biunívoca del remitente del correo.
 */
public final class SecurityContext {

    private SecurityContext() {}

    /**
     * Resuelve la identidad del usuario en PostgreSQL a partir del correo remitente.
     * Retorna null si el correo no está registrado en la institución.
     */
    public static UsuarioAuth obtenerUsuario(String fromEmail) {
        if (fromEmail == null || fromEmail.trim().isEmpty()) {
            return null;
        }

        String emailLimpio = POP3Listener.extractEmailAddress(fromEmail).trim();

        String sql = "SELECT u.id_usuario, u.nombre_completo, u.correo, u.id_rol, r.nombre AS rol_nombre, " +
                     "       u.activo, u.participa_round_robin " +
                     "FROM usuarios u " +
                     "JOIN roles r ON u.id_rol = r.id_rol " +
                     "WHERE LOWER(u.correo) = LOWER(?)";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, emailLimpio);
            rs = ps.executeQuery();

            if (rs.next()) {
                return new UsuarioAuth(
                    rs.getLong("id_usuario"),
                    rs.getString("nombre_completo"),
                    rs.getString("correo"),
                    rs.getLong("id_rol"),
                    rs.getString("rol_nombre"),
                    rs.getBoolean("activo"),
                    rs.getBoolean("participa_round_robin")
                );
            }
        } catch (SQLException e) {
            System.err.println("[SECURITY] Error al consultar usuario por correo: " + e.getMessage());
        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }

        return null;
    }

    /**
     * Valida que el remitente pertenezca a un Administrador activo.
     * Lanza CommandException con mensaje semántico claro si no cumple los requisitos.
     */
    public static void validarAdmin(UsuarioAuth auth) throws CommandException {
        if (auth == null) {
            throw new CommandException(
                "Acceso denegado: El correo remitente no corresponde a un usuario registrado en el sistema.",
                "Para solicitar una cuenta institucional, comuníquese con el administrador del CRM o envíe HELP[] para asistencia."
            );
        }

        if (!auth.isActivo()) {
            throw new CommandException(
                "Acceso denegado: Su cuenta de usuario se encuentra desactivada.",
                "Comuníquese con el administrador institucional para reactivar sus permisos."
            );
        }

        if (!auth.esAdmin()) {
            throw new CommandException(
                "Acceso denegado: Esta acción es exclusiva para usuarios con rol 'Administrador'.",
                "Su rol actual es '" + auth.getRolNombre() + "'. Envíe HELP[] para ver los comandos permitidos para su rol."
            );
        }
    }

    /**
     * Valida que el remitente sea un usuario institucional registrado y activo.
     */
    public static void validarPersonal(UsuarioAuth auth) throws CommandException {
        if (auth == null) {
            throw new CommandException(
                "Acceso denegado: El correo remitente no corresponde a ningún personal registrado.",
                "Para registrarse como prospecto nuevo, envíe: REGISTRARME[\"su_nombre\",\"su_telefono\"]."
            );
        }

        if (!auth.isActivo()) {
            throw new CommandException(
                "Acceso denegado: Su cuenta de usuario institucional se encuentra desactivada.",
                "Comuníquese con el administrador del CRM."
            );
        }
    }
}
