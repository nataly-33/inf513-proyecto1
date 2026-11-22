package com.tecnoweb.grupo05sc.dao;

import com.tecnoweb.grupo05sc.config.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para la gestion de tareas y recordatorios de seguimiento comercial.
 * Tabla: recordatorios
 */
public class RecordatorioDao {

    public static class RecordatorioDTO {
        private final long idRecordatorio;
        private final long idLead;
        private final String leadNombre;
        private final String leadTelefono;
        private final long idUsuario;
        private final String usuarioNombre;
        private final Timestamp fechaHora;
        private final String descripcion;
        private final String estado;
        private final Timestamp createdAt;

        public RecordatorioDTO(long idRecordatorio, long idLead, String leadNombre,
                               String leadTelefono, long idUsuario, String usuarioNombre,
                               Timestamp fechaHora, String descripcion, String estado,
                               Timestamp createdAt) {
            this.idRecordatorio = idRecordatorio;
            this.idLead = idLead;
            this.leadNombre = leadNombre;
            this.leadTelefono = leadTelefono;
            this.idUsuario = idUsuario;
            this.usuarioNombre = usuarioNombre;
            this.fechaHora = fechaHora;
            this.descripcion = descripcion;
            this.estado = estado;
            this.createdAt = createdAt;
        }

        public long getIdRecordatorio() { return idRecordatorio; }
        public long getIdLead() { return idLead; }
        public String getLeadNombre() { return leadNombre; }
        public String getLeadTelefono() { return leadTelefono; }
        public long getIdUsuario() { return idUsuario; }
        public String getUsuarioNombre() { return usuarioNombre; }
        public Timestamp getFechaHora() { return fechaHora; }
        public String getDescripcion() { return descripcion; }
        public String getEstado() { return estado; }
        public Timestamp getCreatedAt() { return createdAt; }
    }

    /**
     * Crea un nuevo recordatorio comercial asignado a un usuario.
     */
    public long crearRecordatorio(long idLead, long idUsuario, Timestamp fechaHora, String descripcion) throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            String sql = "INSERT INTO recordatorios (id_lead, id_usuario, fecha_hora, descripcion, estado, created_at, updated_at) " +
                         "VALUES (?, ?, ?, ?, 'PENDIENTE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) RETURNING id_recordatorio";
            ps = conn.prepareStatement(sql);
            ps.setLong(1, idLead);
            ps.setLong(2, idUsuario);
            ps.setTimestamp(3, fechaHora);
            ps.setString(4, descripcion);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("id_recordatorio");
            }
            throw new Exception("No se pudo obtener el identificador del recordatorio programado.");
        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }
    }

    /**
     * Consulta los recordatorios de un usuario filtrados opcionalmente por estado.
     */
    public List<RecordatorioDTO> listarPorUsuario(long idUsuario, String estadoFiltro) throws Exception {
        List<RecordatorioDTO> lista = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            StringBuilder sql = new StringBuilder(
                "SELECT r.id_recordatorio, r.id_lead, l.nombre AS lead_nombre, l.telefono AS lead_telefono, " +
                "       r.id_usuario, u.nombre_completo AS usuario_nombre, r.fecha_hora, r.descripcion, " +
                "       r.estado, r.created_at " +
                "FROM recordatorios r " +
                "JOIN leads l ON r.id_lead = l.id_lead " +
                "JOIN usuarios u ON r.id_usuario = u.id_usuario " +
                "WHERE r.id_usuario = ? "
            );

            if (estadoFiltro != null && !estadoFiltro.trim().isEmpty()) {
                sql.append("AND r.estado = ? ");
            }
            sql.append("ORDER BY r.fecha_hora ASC");

            ps = conn.prepareStatement(sql.toString());
            ps.setLong(1, idUsuario);
            if (estadoFiltro != null && !estadoFiltro.trim().isEmpty()) {
                ps.setString(2, estadoFiltro.trim());
            }

            rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new RecordatorioDTO(
                    rs.getLong("id_recordatorio"),
                    rs.getLong("id_lead"),
                    rs.getString("lead_nombre"),
                    rs.getString("lead_telefono"),
                    rs.getLong("id_usuario"),
                    rs.getString("usuario_nombre"),
                    rs.getTimestamp("fecha_hora"),
                    rs.getString("descripcion"),
                    rs.getString("estado"),
                    rs.getTimestamp("created_at")
                ));
            }
            return lista;
        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }
    }

    /**
     * Busca un recordatorio especifico por su identificador primario.
     */
    public RecordatorioDTO buscarPorId(long idRecordatorio) throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT r.id_recordatorio, r.id_lead, l.nombre AS lead_nombre, l.telefono AS lead_telefono, " +
                         "       r.id_usuario, u.nombre_completo AS usuario_nombre, r.fecha_hora, r.descripcion, " +
                         "       r.estado, r.created_at " +
                         "FROM recordatorios r " +
                         "JOIN leads l ON r.id_lead = l.id_lead " +
                         "JOIN usuarios u ON r.id_usuario = u.id_usuario " +
                         "WHERE r.id_recordatorio = ?";
            ps = conn.prepareStatement(sql);
            ps.setLong(1, idRecordatorio);
            rs = ps.executeQuery();

            if (rs.next()) {
                return new RecordatorioDTO(
                    rs.getLong("id_recordatorio"),
                    rs.getLong("id_lead"),
                    rs.getString("lead_nombre"),
                    rs.getString("lead_telefono"),
                    rs.getLong("id_usuario"),
                    rs.getString("usuario_nombre"),
                    rs.getTimestamp("fecha_hora"),
                    rs.getString("descripcion"),
                    rs.getString("estado"),
                    rs.getTimestamp("created_at")
                );
            }
            return null;
        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }
    }

    /**
     * Marca un recordatorio como completado.
     */
    public boolean marcarCompletado(long idRecordatorio) throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DatabaseConnection.getConnection();
            String sql = "UPDATE recordatorios SET estado = 'COMPLETADO', updated_at = CURRENT_TIMESTAMP WHERE id_recordatorio = ?";
            ps = conn.prepareStatement(sql);
            ps.setLong(1, idRecordatorio);
            return ps.executeUpdate() > 0;
        } finally {
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }
    }
}
