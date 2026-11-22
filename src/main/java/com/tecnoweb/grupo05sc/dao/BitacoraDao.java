package com.tecnoweb.grupo05sc.dao;

import com.tecnoweb.grupo05sc.config.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para la persistencia y consulta de la Bitacora Historica de Eventos y Notas.
 * Tabla: bitacora
 */
public class BitacoraDao {

    public static class EventoBitacoraDTO {
        private final long idEvento;
        private final long idLead;
        private final Long idUsuario;
        private final String usuarioNombre;
        private final String tipo;
        private final String descripcion;
        private final Long idEtapaAnterior;
        private final String etapaAnteriorNombre;
        private final Long idEtapaNueva;
        private final String etapaNuevaNombre;
        private final Timestamp createdAt;

        public EventoBitacoraDTO(long idEvento, long idLead, Long idUsuario, String usuarioNombre,
                                 String tipo, String descripcion, Long idEtapaAnterior,
                                 String etapaAnteriorNombre, Long idEtapaNueva,
                                 String etapaNuevaNombre, Timestamp createdAt) {
            this.idEvento = idEvento;
            this.idLead = idLead;
            this.idUsuario = idUsuario;
            this.usuarioNombre = usuarioNombre;
            this.tipo = tipo;
            this.descripcion = descripcion;
            this.idEtapaAnterior = idEtapaAnterior;
            this.etapaAnteriorNombre = etapaAnteriorNombre;
            this.idEtapaNueva = idEtapaNueva;
            this.etapaNuevaNombre = etapaNuevaNombre;
            this.createdAt = createdAt;
        }

        public long getIdEvento() { return idEvento; }
        public long getIdLead() { return idLead; }
        public Long getIdUsuario() { return idUsuario; }
        public String getUsuarioNombre() { return usuarioNombre; }
        public String getTipo() { return tipo; }
        public String getDescripcion() { return descripcion; }
        public Long getIdEtapaAnterior() { return idEtapaAnterior; }
        public String getEtapaAnteriorNombre() { return etapaAnteriorNombre; }
        public Long getIdEtapaNueva() { return idEtapaNueva; }
        public String getEtapaNuevaNombre() { return etapaNuevaNombre; }
        public Timestamp getCreatedAt() { return createdAt; }
    }

    /**
     * Consulta cronologica de todos los eventos registrados para un prospecto.
     */
    public List<EventoBitacoraDTO> listarPorLead(long idLead) throws Exception {
        List<EventoBitacoraDTO> lista = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT b.id_evento, b.id_lead, b.id_usuario, " +
                         "       COALESCE(u.nombre_completo, 'Sistema Automático') AS usuario_nombre, " +
                         "       b.tipo, b.descripcion, " +
                         "       b.id_etapa_anterior, ea.nombre AS etapa_anterior_nombre, " +
                         "       b.id_etapa_nueva, en.nombre AS etapa_nueva_nombre, " +
                         "       b.created_at " +
                         "FROM bitacora b " +
                         "LEFT JOIN usuarios u ON b.id_usuario = u.id_usuario " +
                         "LEFT JOIN etapas_pipeline ea ON b.id_etapa_anterior = ea.id_etapa " +
                         "LEFT JOIN etapas_pipeline en ON b.id_etapa_nueva = en.id_etapa " +
                         "WHERE b.id_lead = ? " +
                         "ORDER BY b.created_at ASC, b.id_evento ASC";

            ps = conn.prepareStatement(sql);
            ps.setLong(1, idLead);
            rs = ps.executeQuery();

            while (rs.next()) {
                Long idUser = rs.getObject("id_usuario") != null ? rs.getLong("id_usuario") : null;
                Long idAnt = rs.getObject("id_etapa_anterior") != null ? rs.getLong("id_etapa_anterior") : null;
                Long idNue = rs.getObject("id_etapa_nueva") != null ? rs.getLong("id_etapa_nueva") : null;

                lista.add(new EventoBitacoraDTO(
                    rs.getLong("id_evento"),
                    rs.getLong("id_lead"),
                    idUser,
                    rs.getString("usuario_nombre"),
                    rs.getString("tipo"),
                    rs.getString("descripcion"),
                    idAnt,
                    rs.getString("etapa_anterior_nombre"),
                    idNue,
                    rs.getString("etapa_nueva_nombre"),
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
     * Agrega una nota de seguimiento comercial a la bitacora del prospecto.
     */
    public long agregarNota(long idLead, long idUsuario, String nota) throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            String sql = "INSERT INTO bitacora (id_lead, id_usuario, tipo, descripcion, created_at) " +
                         "VALUES (?, ?, 'NOTA', ?, CURRENT_TIMESTAMP) RETURNING id_evento";
            ps = conn.prepareStatement(sql);
            ps.setLong(1, idLead);
            ps.setLong(2, idUsuario);
            ps.setString(3, nota);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("id_evento");
            }
            throw new Exception("No se pudo obtener el identificador de la nota registrada.");
        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }
    }

    /**
     * Registra un evento de auditoria general en la bitacora.
     */
    public void registrarEvento(long idLead, Long idUsuario, String tipo, String descripcion,
                                Long idEtapaAnt, Long idEtapaNue) throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DatabaseConnection.getConnection();
            String sql = "INSERT INTO bitacora (id_lead, id_usuario, tipo, descripcion, id_etapa_anterior, id_etapa_nueva, created_at) " +
                         "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
            ps = conn.prepareStatement(sql);
            ps.setLong(1, idLead);
            if (idUsuario != null) ps.setLong(2, idUsuario); else ps.setNull(2, java.sql.Types.BIGINT);
            ps.setString(3, tipo);
            ps.setString(4, descripcion);
            if (idEtapaAnt != null) ps.setLong(5, idEtapaAnt); else ps.setNull(5, java.sql.Types.BIGINT);
            if (idEtapaNue != null) ps.setLong(6, idEtapaNue); else ps.setNull(6, java.sql.Types.BIGINT);
            ps.executeUpdate();
        } finally {
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }
    }
}
