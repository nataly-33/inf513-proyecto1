package com.tecnoweb.grupo05sc.dao;

import com.tecnoweb.grupo05sc.config.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Capa de Acceso a Datos (DAO) para la gestión de usuarios y roles en PostgreSQL.
 * Implementa consultas parametrizadas con PreparedStatement (anti-inyección SQL)
 * y cierre estricto de recursos en bloques finally.
 */
public final class UsuarioDao {

    public static class UsuarioDTO {
        private final long idUsuario;
        private final long idRol;
        private final String rolNombre;
        private final String nombreCompleto;
        private final String correo;
        private final boolean activo;
        private final boolean participaRoundRobin;
        private final String ultimoAcceso;
        private final String createdAt;

        public UsuarioDTO(long idUsuario, long idRol, String rolNombre, String nombreCompleto, String correo, boolean activo, boolean participaRoundRobin, String ultimoAcceso, String createdAt) {
            this.idUsuario = idUsuario;
            this.idRol = idRol;
            this.rolNombre = rolNombre;
            this.nombreCompleto = nombreCompleto;
            this.correo = correo;
            this.activo = activo;
            this.participaRoundRobin = participaRoundRobin;
            this.ultimoAcceso = ultimoAcceso;
            this.createdAt = createdAt;
        }

        public long getIdUsuario() { return idUsuario; }
        public long getIdRol() { return idRol; }
        public String getRolNombre() { return rolNombre; }
        public String getNombreCompleto() { return nombreCompleto; }
        public String getCorreo() { return correo; }
        public boolean isActivo() { return activo; }
        public boolean isParticipaRoundRobin() { return participaRoundRobin; }
        public String getUltimoAcceso() { return ultimoAcceso != null ? ultimoAcceso : "-"; }
        public String getCreatedAt() { return createdAt != null ? createdAt : "-"; }
    }

    /**
     * Lista todos los usuarios registrados en el CRM con su rol asociado.
     */
    public List<UsuarioDTO> listarTodos() throws SQLException {
        List<UsuarioDTO> lista = new ArrayList<>();
        String sql = "SELECT u.id_usuario, u.id_rol, r.nombre AS rol_nombre, u.nombre_completo, u.correo, " +
                     "       u.activo, u.participa_round_robin, u.ultimo_acceso, u.created_at " +
                     "FROM usuarios u " +
                     "JOIN roles r ON u.id_rol = r.id_rol " +
                     "ORDER BY u.id_usuario ASC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                lista.add(mapResultSet(rs));
            }
        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }

        return lista;
    }

    /**
     * Busca un usuario por ID numérico o por coincidencia exacta/parcial de correo o nombre.
     */
    public UsuarioDTO buscarPorIdOCorreo(String termino) throws SQLException {
        if (termino == null || termino.trim().isEmpty()) return null;
        String t = termino.trim();

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();

            boolean esNumero = t.matches("^\\d+$");
            String sql;

            if (esNumero) {
                sql = "SELECT u.id_usuario, u.id_rol, r.nombre AS rol_nombre, u.nombre_completo, u.correo, " +
                      "       u.activo, u.participa_round_robin, u.ultimo_acceso, u.created_at " +
                      "FROM usuarios u JOIN roles r ON u.id_rol = r.id_rol " +
                      "WHERE u.id_usuario = ? OR LOWER(u.correo) = LOWER(?)";
                ps = conn.prepareStatement(sql);
                ps.setLong(1, Long.parseLong(t));
                ps.setString(2, t);
            } else {
                sql = "SELECT u.id_usuario, u.id_rol, r.nombre AS rol_nombre, u.nombre_completo, u.correo, " +
                      "       u.activo, u.participa_round_robin, u.ultimo_acceso, u.created_at " +
                      "FROM usuarios u JOIN roles r ON u.id_rol = r.id_rol " +
                      "WHERE LOWER(u.correo) = LOWER(?) OR u.nombre_completo ILIKE ? LIMIT 1";
                ps = conn.prepareStatement(sql);
                ps.setString(1, t);
                ps.setString(2, "%" + t + "%");
            }

            rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSet(rs);
            }
        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }

        return null;
    }

    /**
     * Busca un usuario por su ID primario.
     */
    public UsuarioDTO buscarPorId(long idUsuario) throws SQLException {
        String sql = "SELECT u.id_usuario, u.id_rol, r.nombre AS rol_nombre, u.nombre_completo, u.correo, " +
                     "       u.activo, u.participa_round_robin, u.ultimo_acceso, u.created_at " +
                     "FROM usuarios u JOIN roles r ON u.id_rol = r.id_rol " +
                     "WHERE u.id_usuario = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, idUsuario);
            rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSet(rs);
            }
        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }

        return null;
    }

    /**
     * Verifica si un correo ya se encuentra registrado.
     */
    public boolean existeCorreo(String correo) throws SQLException {
        if (correo == null) return false;
        String sql = "SELECT 1 FROM usuarios WHERE LOWER(correo) = LOWER(?) LIMIT 1";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, correo.trim());
            rs = ps.executeQuery();
            return rs.next();
        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }
    }

    /**
     * Verifica si un correo ya está en uso por otro usuario distinto al dado.
     */
    public boolean existeCorreoExcepto(String correo, long idUsuario) throws SQLException {
        if (correo == null) return false;
        String sql = "SELECT 1 FROM usuarios WHERE LOWER(correo) = LOWER(?) AND id_usuario != ? LIMIT 1";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, correo.trim());
            ps.setLong(2, idUsuario);
            rs = ps.executeQuery();
            return rs.next();
        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }
    }

    /**
     * Inserta un nuevo usuario en PostgreSQL devolviendo el ID generado.
     */
    public long insertar(String nombre, String correo, String passwordHash, long idRol) throws SQLException {
        // Por regla de negocio, participa en round-robin por defecto solo si es Vendedor (rol 3)
        boolean participaRoundRobin = (idRol == 3);

        String sql = "INSERT INTO usuarios (nombre_completo, correo, password_hash, id_rol, activo, participa_round_robin, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, true, ?, NOW(), NOW())";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, nombre.trim());
            ps.setString(2, correo.trim());
            ps.setString(3, passwordHash);
            ps.setLong(4, idRol);
            ps.setBoolean(5, participaRoundRobin);

            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getLong(1);
            }
            throw new SQLException("No se pudo obtener el ID del usuario insertado.");
        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }
    }

    /**
     * Actualiza los datos de un usuario existente.
     */
    public boolean actualizar(long idUsuario, String nombre, String correo, long idRol) throws SQLException {
        String sql = "UPDATE usuarios SET nombre_completo = ?, correo = ?, id_rol = ?, updated_at = NOW() WHERE id_usuario = ?";

        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, nombre.trim());
            ps.setString(2, correo.trim());
            ps.setLong(3, idRol);
            ps.setLong(4, idUsuario);

            return ps.executeUpdate() > 0;
        } finally {
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }
    }

    /**
     * Alterna el estado activo/inactivo de una cuenta.
     */
    public boolean toggleActivo(long idUsuario) throws SQLException {
        String sql = "UPDATE usuarios SET activo = NOT activo, updated_at = NOW() WHERE id_usuario = ?";

        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, idUsuario);
            return ps.executeUpdate() > 0;
        } finally {
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }
    }

    /**
     * Alterna la participación en el reparto automático Round-Robin.
     */
    public boolean toggleRoundRobin(long idUsuario) throws SQLException {
        String sql = "UPDATE usuarios SET participa_round_robin = NOT participa_round_robin, updated_at = NOW() WHERE id_usuario = ?";

        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, idUsuario);
            return ps.executeUpdate() > 0;
        } finally {
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }
    }

    /**
     * Mapea un nombre de rol (ej. 'Administrador', 'Coordinador', 'Vendedor') a su id_rol.
     */
    public Long obtenerIdRolPorNombre(String rolNombre) throws SQLException {
        if (rolNombre == null || rolNombre.trim().isEmpty()) return null;
        String r = rolNombre.trim().toLowerCase();

        // Mapeo rápido estándar
        if (r.startsWith("admin")) return 1L;
        if (r.startsWith("coord")) return 2L;
        if (r.startsWith("vend")) return 3L;

        // Búsqueda en base de datos
        String sql = "SELECT id_rol FROM roles WHERE LOWER(nombre) = LOWER(?) LIMIT 1";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, r);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("id_rol");
            }
        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }

        return null;
    }

    private static UsuarioDTO mapResultSet(ResultSet rs) throws SQLException {
        return new UsuarioDTO(
            rs.getLong("id_usuario"),
            rs.getLong("id_rol"),
            rs.getString("rol_nombre"),
            rs.getString("nombre_completo"),
            rs.getString("correo"),
            rs.getBoolean("activo"),
            rs.getBoolean("participa_round_robin"),
            rs.getString("ultimo_acceso"),
            rs.getString("created_at")
        );
    }
}
