package com.tecnoweb.grupo05sc.dao;

import com.tecnoweb.grupo05sc.config.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Capa de Acceso a Datos (DAO) para la Gestion de Perdida y Desistimiento de Prospectos.
 * Maneja el catalogo de motivos de perdida, la transicion a Etapa 6 y la consulta analitica de bajas.
 */
public final class MotivoPerdidaDao {

    // =========================================================================
    // DTOs DE TRANSFERENCIA DE DATOS
    // =========================================================================

    public static class MotivoDTO {
        private final long idMotivo;
        private final String nombre;

        public MotivoDTO(long idMotivo, String nombre) {
            this.idMotivo = idMotivo;
            this.nombre = nombre;
        }

        public long getIdMotivo() { return idMotivo; }
        public String getNombre() { return nombre; }
    }

    public static class LeadPerdidoDTO {
        private final long idLead;
        private final String nombre;
        private final String telefono;
        private final String correo;
        private final String vendedor;
        private final long idMotivo;
        private final String motivoNombre;
        private final String comentario;
        private final String fechaPerdida;

        public LeadPerdidoDTO(long idLead, String nombre, String telefono, String correo,
                              String vendedor, long idMotivo, String motivoNombre,
                              String comentario, String fechaPerdida) {
            this.idLead = idLead;
            this.nombre = nombre;
            this.telefono = telefono;
            this.correo = correo;
            this.vendedor = vendedor;
            this.idMotivo = idMotivo;
            this.motivoNombre = motivoNombre;
            this.comentario = comentario;
            this.fechaPerdida = fechaPerdida;
        }

        public long getIdLead() { return idLead; }
        public String getNombre() { return nombre; }
        public String getTelefono() { return telefono; }
        public String getCorreo() { return correo != null ? correo : "-"; }
        public String getVendedor() { return vendedor != null ? vendedor : "Bolsa Comun"; }
        public long getIdMotivo() { return idMotivo; }
        public String getMotivoNombre() { return motivoNombre; }
        public String getComentario() { return comentario != null ? comentario : "-"; }
        public String getFechaPerdida() { return fechaPerdida != null ? fechaPerdida : "-"; }
    }

    // =========================================================================
    // OPERACIONES DE BASE DE DATOS
    // =========================================================================

    /**
     * Lista todos los motivos oficiales de perdida registrados en el sistema.
     */
    public List<MotivoDTO> listarMotivos() throws SQLException {
        List<MotivoDTO> lista = new ArrayList<>();
        String sql = "SELECT id_motivo, nombre FROM motivos_perdida ORDER BY id_motivo ASC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new MotivoDTO(rs.getLong("id_motivo"), rs.getString("nombre")));
            }
            return lista;
        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }
    }

    /**
     * Busca un motivo por su ID.
     */
    public MotivoDTO buscarPorId(long idMotivo) throws SQLException {
        String sql = "SELECT id_motivo, nombre FROM motivos_perdida WHERE id_motivo = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, idMotivo);
            rs = ps.executeQuery();
            if (rs.next()) {
                return new MotivoDTO(rs.getLong("id_motivo"), rs.getString("nombre"));
            }
            return null;
        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }
    }

    /**
     * Marca un prospecto como perdido en la Etapa 6, registrando motivo formal,
     * comentario explicativo y auditoria en bitacora dentro de una transaccion atomica.
     */
    public boolean marcarPerdido(long idLead, long idMotivo, String comentario, long idUsuario) throws SQLException {
        String sqlGet = "SELECT id_etapa FROM leads WHERE id_lead = ? FOR UPDATE";
        String sqlUpd = "UPDATE leads SET id_etapa = 6, id_motivo_perdida = ?, comentario_perdida = ?, updated_at = NOW() WHERE id_lead = ?";
        String sqlBit = "INSERT INTO bitacora (id_lead, id_usuario, tipo, id_etapa_anterior, id_etapa_nueva, descripcion, created_at) " +
                        "VALUES (?, ?, 'PERDIDA', ?, 6, ?, NOW())";

        Connection conn = null;
        PreparedStatement psGet = null;
        PreparedStatement psUpd = null;
        PreparedStatement psBit = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Obtener etapa actual
            psGet = conn.prepareStatement(sqlGet);
            psGet.setLong(1, idLead);
            rs = psGet.executeQuery();
            if (!rs.next()) {
                conn.rollback();
                return false;
            }
            long idEtapaAnt = rs.getLong("id_etapa");

            // 2. Actualizar lead a etapa 6
            psUpd = conn.prepareStatement(sqlUpd);
            psUpd.setLong(1, idMotivo);
            psUpd.setString(2, comentario.trim());
            psUpd.setLong(3, idLead);
            psUpd.executeUpdate();

            // 3. Registrar auditoria en bitacora
            psBit = conn.prepareStatement(sqlBit);
            psBit.setLong(1, idLead);
            psBit.setLong(2, idUsuario);
            psBit.setLong(3, idEtapaAnt);
            psBit.setString(4, "Lead clasificado como Perdido. Motivo #" + idMotivo + ". Justificacion: " + comentario.trim());
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
     * Lista todos los prospectos clasificados en la Etapa 6 (Perdido).
     * Si idVendedorFiltro no es nulo, filtra a la cartera del vendedor.
     */
    public List<LeadPerdidoDTO> listarPerdidos(Long idVendedorFiltro) throws SQLException {
        List<LeadPerdidoDTO> lista = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT l.id_lead, l.nombre, l.telefono, l.correo, ")
          .append("       COALESCE(u.nombre_completo, 'Bolsa Comun') AS vendedor_nombre, ")
          .append("       COALESCE(m.id_motivo, 0) AS id_motivo, ")
          .append("       COALESCE(m.nombre, 'No especificado') AS motivo_nombre, ")
          .append("       l.comentario_perdida, ")
          .append("       TO_CHAR(l.updated_at, 'YYYY-MM-DD HH24:MI:SS') AS fecha_perdida ")
          .append("FROM leads l ")
          .append("LEFT JOIN usuarios u ON l.id_vendedor = u.id_usuario ")
          .append("LEFT JOIN motivos_perdida m ON l.id_motivo_perdida = m.id_motivo ")
          .append("WHERE l.id_etapa = 6 ");

        if (idVendedorFiltro != null) {
            sb.append("AND l.id_vendedor = ? ");
        }
        sb.append("ORDER BY l.updated_at DESC");

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sb.toString());
            if (idVendedorFiltro != null) {
                ps.setLong(1, idVendedorFiltro);
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new LeadPerdidoDTO(
                    rs.getLong("id_lead"),
                    rs.getString("nombre"),
                    rs.getString("telefono"),
                    rs.getString("correo"),
                    rs.getString("vendedor_nombre"),
                    rs.getLong("id_motivo"),
                    rs.getString("motivo_nombre"),
                    rs.getString("comentario_perdida"),
                    rs.getString("fecha_perdida")
                ));
            }
            return lista;
        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }
    }
}
