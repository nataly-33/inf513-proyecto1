package com.tecnoweb.grupo05sc.dao;

import com.tecnoweb.grupo05sc.config.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Capa de Acceso a Datos (DAO) para el Embudo de Ventas Kanban (CU4).
 * Gestiona el avance de prospectos a lo largo de las 6 etapas comerciales y auditoria en bitacora.
 */
public final class PipelineDao {

    // =========================================================================
    // DTOs
    // =========================================================================

    public static class EtapaResumenDTO {
        private final int idEtapa;
        private final String nombre;
        private final int orden;
        private final int totalLeads;

        public EtapaResumenDTO(int idEtapa, String nombre, int orden, int totalLeads) {
            this.idEtapa = idEtapa;
            this.nombre = nombre;
            this.orden = orden;
            this.totalLeads = totalLeads;
        }

        public int getIdEtapa() { return idEtapa; }
        public String getNombre() { return nombre; }
        public int getOrden() { return orden; }
        public int getTotalLeads() { return totalLeads; }
    }

    public static class LeadEtapaDTO {
        private final long idLead;
        private final String nombre;
        private final String telefono;
        private final String correo;
        private final String vendedor;
        private final String updatedAt;

        public LeadEtapaDTO(long idLead, String nombre, String telefono, String correo, String vendedor, String updatedAt) {
            this.idLead = idLead;
            this.nombre = nombre;
            this.telefono = telefono;
            this.correo = correo != null ? correo : "-";
            this.vendedor = vendedor != null ? vendedor : "Bolsa Comun";
            this.updatedAt = updatedAt;
        }

        public long getIdLead() { return idLead; }
        public String getNombre() { return nombre; }
        public String getTelefono() { return telefono; }
        public String getCorreo() { return correo; }
        public String getVendedor() { return vendedor; }
        public String getUpdatedAt() { return updatedAt; }
    }

    public static class MiLeadDTO {
        private final long idLead;
        private final String nombre;
        private final String telefono;
        private final String correo;
        private final int idEtapa;
        private final String etapaNombre;
        private final String updatedAt;

        public MiLeadDTO(long idLead, String nombre, String telefono, String correo, int idEtapa, String etapaNombre, String updatedAt) {
            this.idLead = idLead;
            this.nombre = nombre;
            this.telefono = telefono;
            this.correo = correo != null ? correo : "-";
            this.idEtapa = idEtapa;
            this.etapaNombre = etapaNombre;
            this.updatedAt = updatedAt;
        }

        public long getIdLead() { return idLead; }
        public String getNombre() { return nombre; }
        public String getTelefono() { return telefono; }
        public String getCorreo() { return correo; }
        public int getIdEtapa() { return idEtapa; }
        public String getEtapaNombre() { return etapaNombre; }
        public String getUpdatedAt() { return updatedAt; }
    }

    // =========================================================================
    // METODOS DE PERSISTENCIA
    // =========================================================================

    /**
     * Obtiene el resumen del Kanban con el total de leads por cada una de las 6 etapas.
     * Si idVendedor != null, filtra por ese vendedor en particular.
     */
    public List<EtapaResumenDTO> obtenerResumenKanban(Long idVendedor) throws SQLException {
        List<EtapaResumenDTO> lista = new ArrayList<>();
        String sql;

        if (idVendedor != null) {
            sql = "SELECT e.id_etapa, e.nombre, e.orden, COUNT(l.id_lead) AS total " +
                  "FROM etapas_pipeline e " +
                  "LEFT JOIN leads l ON e.id_etapa = l.id_etapa AND l.id_vendedor = ? " +
                  "GROUP BY e.id_etapa, e.nombre, e.orden ORDER BY e.orden ASC";
        } else {
            sql = "SELECT e.id_etapa, e.nombre, e.orden, COUNT(l.id_lead) AS total " +
                  "FROM etapas_pipeline e " +
                  "LEFT JOIN leads l ON e.id_etapa = l.id_etapa " +
                  "GROUP BY e.id_etapa, e.nombre, e.orden ORDER BY e.orden ASC";
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            if (idVendedor != null) {
                ps.setLong(1, idVendedor);
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new EtapaResumenDTO(
                    rs.getInt("id_etapa"),
                    rs.getString("nombre"),
                    rs.getInt("orden"),
                    rs.getInt("total")
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
     * Lista los prospectos que se encuentran en una etapa especifica.
     */
    public List<LeadEtapaDTO> listarLeadsPorEtapa(long idEtapa, Long idVendedor) throws SQLException {
        List<LeadEtapaDTO> lista = new ArrayList<>();
        String sql;

        if (idVendedor != null) {
            sql = "SELECT l.id_lead, l.nombre, l.telefono, l.correo, u.nombre_completo AS vendedor, l.updated_at " +
                  "FROM leads l " +
                  "LEFT JOIN usuarios u ON l.id_vendedor = u.id_usuario " +
                  "WHERE l.id_etapa = ? AND l.id_vendedor = ? " +
                  "ORDER BY l.updated_at DESC";
        } else {
            sql = "SELECT l.id_lead, l.nombre, l.telefono, l.correo, u.nombre_completo AS vendedor, l.updated_at " +
                  "FROM leads l " +
                  "LEFT JOIN usuarios u ON l.id_vendedor = u.id_usuario " +
                  "WHERE l.id_etapa = ? " +
                  "ORDER BY l.updated_at DESC";
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, idEtapa);
            if (idVendedor != null) {
                ps.setLong(2, idVendedor);
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new LeadEtapaDTO(
                    rs.getLong("id_lead"),
                    rs.getString("nombre"),
                    rs.getString("telefono"),
                    rs.getString("correo"),
                    rs.getString("vendedor"),
                    rs.getString("updated_at")
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
     * Actualiza la etapa de un prospecto y genera el hito cronologico en bitacora.
     */
    public boolean moverEtapa(long idLead, long idNuevaEtapa, long idUsuarioResponsable) throws SQLException {
        Connection conn = null;
        PreparedStatement psGet = null;
        PreparedStatement psUpd = null;
        PreparedStatement psBit = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Obtener etapa anterior y nombre de etapas
            psGet = conn.prepareStatement(
                "SELECT l.id_etapa AS etapa_ant, ea.nombre AS nombre_ant, en.nombre AS nombre_nueva " +
                "FROM leads l " +
                "JOIN etapas_pipeline ea ON l.id_etapa = ea.id_etapa " +
                "JOIN etapas_pipeline en ON en.id_etapa = ? " +
                "WHERE l.id_lead = ?"
            );
            psGet.setLong(1, idNuevaEtapa);
            psGet.setLong(2, idLead);
            rs = psGet.executeQuery();

            if (!rs.next()) {
                DatabaseConnection.rollback(conn);
                return false;
            }

            long idEtapaAnt = rs.getLong("etapa_ant");
            String nombreAnt = rs.getString("nombre_ant");
            String nombreNueva = rs.getString("nombre_nueva");

            // 2. Actualizar etapa en leads
            psUpd = conn.prepareStatement("UPDATE leads SET id_etapa = ?, updated_at = NOW() WHERE id_lead = ?");
            psUpd.setLong(1, idNuevaEtapa);
            psUpd.setLong(2, idLead);
            psUpd.executeUpdate();

            // 3. Registrar en bitacora institucional
            psBit = conn.prepareStatement(
                "INSERT INTO bitacora (id_lead, id_usuario, tipo, descripcion, id_etapa_anterior, id_etapa_nueva, created_at) " +
                "VALUES (?, ?, 'ETAPA', ?, ?, ?, NOW())"
            );
            psBit.setLong(1, idLead);
            psBit.setLong(2, idUsuarioResponsable);
            psBit.setString(3, "Transicion comercial: de '" + nombreAnt + "' a '" + nombreNueva + "'.");
            psBit.setLong(4, idEtapaAnt);
            psBit.setLong(5, idNuevaEtapa);
            psBit.executeUpdate();

            conn.commit();
            return true;

        } catch (SQLException e) {
            DatabaseConnection.rollback(conn);
            throw e;
        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(psBit);
            DatabaseConnection.close(psUpd);
            DatabaseConnection.close(psGet);
            DatabaseConnection.close(conn);
        }
    }

    /**
     * Lista los prospectos asignados a la cartera de un vendedor especifico.
     */
    public List<MiLeadDTO> listarMisLeads(long idVendedor) throws SQLException {
        List<MiLeadDTO> lista = new ArrayList<>();
        String sql = "SELECT l.id_lead, l.nombre, l.telefono, l.correo, e.id_etapa, e.nombre AS etapa_nombre, l.updated_at " +
                     "FROM leads l " +
                     "JOIN etapas_pipeline e ON l.id_etapa = e.id_etapa " +
                     "WHERE l.id_vendedor = ? " +
                     "ORDER BY e.orden ASC, l.updated_at DESC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, idVendedor);
            rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new MiLeadDTO(
                    rs.getLong("id_lead"),
                    rs.getString("nombre"),
                    rs.getString("telefono"),
                    rs.getString("correo"),
                    rs.getInt("id_etapa"),
                    rs.getString("etapa_nombre"),
                    rs.getString("updated_at")
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
     * Obtiene el nombre de una etapa a partir de su ID.
     */
    public String obtenerNombreEtapa(long idEtapa) throws SQLException {
        String sql = "SELECT nombre FROM etapas_pipeline WHERE id_etapa = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, idEtapa);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("nombre");
            }
            return null;
        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }
    }
}
