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
 * Capa de Acceso a Datos (DAO) para la Asignacion de Leads a la Fuerza de Ventas (CU3).
 * Maneja la Bolsa Comun, asignacion manual, reparto rotativo Round-Robin y transferencias con auditoria.
 */
public final class AsignacionDao {

    // =========================================================================
    // DTOs
    // =========================================================================

    public static class BolsaLeadDTO {
        private final long idLead;
        private final String nombre;
        private final String telefono;
        private final String correo;
        private final String origen;
        private final String createdAt;

        public BolsaLeadDTO(long idLead, String nombre, String telefono, String correo, String origen, String createdAt) {
            this.idLead = idLead;
            this.nombre = nombre;
            this.telefono = telefono;
            this.correo = correo != null ? correo : "-";
            this.origen = origen;
            this.createdAt = createdAt;
        }

        public long getIdLead() { return idLead; }
        public String getNombre() { return nombre; }
        public String getTelefono() { return telefono; }
        public String getCorreo() { return correo; }
        public String getOrigen() { return origen; }
        public String getCreatedAt() { return createdAt; }
    }

    public static class VendedorRRDTO {
        private final long idUsuario;
        private final String nombreCompleto;
        private final String correo;

        public VendedorRRDTO(long idUsuario, String nombreCompleto, String correo) {
            this.idUsuario = idUsuario;
            this.nombreCompleto = nombreCompleto;
            this.correo = correo;
        }

        public long getIdUsuario() { return idUsuario; }
        public String getNombreCompleto() { return nombreCompleto; }
        public String getCorreo() { return correo; }
    }

    public static class AsignacionHistorialDTO {
        private final long idAsignacion;
        private final long idLead;
        private final String vendedorAnterior;
        private final String vendedorNuevo;
        private final String asignadoPor;
        private final String tipo;
        private final String motivo;
        private final String createdAt;

        public AsignacionHistorialDTO(long idAsignacion, long idLead, String vendedorAnterior, String vendedorNuevo, String asignadoPor, String tipo, String motivo, String createdAt) {
            this.idAsignacion = idAsignacion;
            this.idLead = idLead;
            this.vendedorAnterior = vendedorAnterior != null ? vendedorAnterior : "Bolsa Comun";
            this.vendedorNuevo = vendedorNuevo;
            this.asignadoPor = asignadoPor;
            this.tipo = tipo;
            this.motivo = motivo != null ? motivo : "-";
            this.createdAt = createdAt;
        }

        public long getIdAsignacion() { return idAsignacion; }
        public long getIdLead() { return idLead; }
        public String getVendedorAnterior() { return vendedorAnterior; }
        public String getVendedorNuevo() { return vendedorNuevo; }
        public String getAsignadoPor() { return asignadoPor; }
        public String getTipo() { return tipo; }
        public String getMotivo() { return motivo; }
        public String getCreatedAt() { return createdAt; }
    }

    public static class AsignacionResumenDTO {
        private final long idLead;
        private final String leadNombre;
        private final long idVendedor;
        private final String vendedorNombre;

        public AsignacionResumenDTO(long idLead, String leadNombre, long idVendedor, String vendedorNombre) {
            this.idLead = idLead;
            this.leadNombre = leadNombre;
            this.idVendedor = idVendedor;
            this.vendedorNombre = vendedorNombre;
        }

        public long getIdLead() { return idLead; }
        public String getLeadNombre() { return leadNombre; }
        public long getIdVendedor() { return idVendedor; }
        public String getVendedorNombre() { return vendedorNombre; }
    }

    // =========================================================================
    // METODOS DE BASE DE DATOS
    // =========================================================================

    /**
     * Lista todos los prospectos en la Bolsa Comun (id_vendedor IS NULL).
     */
    public List<BolsaLeadDTO> listarBolsaComun() throws SQLException {
        List<BolsaLeadDTO> lista = new ArrayList<>();
        String sql = "SELECT l.id_lead, l.nombre, l.telefono, l.correo, o.nombre AS origen, l.created_at " +
                     "FROM leads l " +
                     "JOIN origenes_lead o ON l.id_origen = o.id_origen " +
                     "WHERE l.id_vendedor IS NULL " +
                     "ORDER BY l.created_at ASC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new BolsaLeadDTO(
                    rs.getLong("id_lead"),
                    rs.getString("nombre"),
                    rs.getString("telefono"),
                    rs.getString("correo"),
                    rs.getString("origen"),
                    rs.getString("created_at")
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
     * Obtiene todos los vendedores activos que participan en Round-Robin (rol=3, activo=true, participa_rr=true).
     */
    public List<VendedorRRDTO> obtenerVendedoresRoundRobinActivos() throws SQLException {
        List<VendedorRRDTO> lista = new ArrayList<>();
        String sql = "SELECT id_usuario, nombre_completo, correo " +
                     "FROM usuarios " +
                     "WHERE id_rol = 3 AND activo = true AND participa_round_robin = true " +
                     "ORDER BY id_usuario ASC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new VendedorRRDTO(
                    rs.getLong("id_usuario"),
                    rs.getString("nombre_completo"),
                    rs.getString("correo")
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
     * Asignacion manual de un lead a un vendedor con transaccion atomica y doble auditoria.
     */
    public boolean asignarManual(long idLead, long idVendedorNuevo, long asignadoPor, String motivo) throws SQLException {
        Connection conn = null;
        PreparedStatement psLead = null;
        PreparedStatement psGet = null;
        PreparedStatement psAsig = null;
        PreparedStatement psBit = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Obtener vendedor anterior y validar existencia
            psGet = conn.prepareStatement("SELECT id_vendedor, nombre FROM leads WHERE id_lead = ?");
            psGet.setLong(1, idLead);
            rs = psGet.executeQuery();
            if (!rs.next()) {
                DatabaseConnection.rollback(conn);
                return false;
            }
            Long idVendedorAnt = rs.getObject("id_vendedor") != null ? rs.getLong("id_vendedor") : null;
            String leadNombre = rs.getString("nombre");

            // 2. Actualizar lead
            psLead = conn.prepareStatement("UPDATE leads SET id_vendedor = ?, fecha_asignacion = NOW(), updated_at = NOW() WHERE id_lead = ?");
            psLead.setLong(1, idVendedorNuevo);
            psLead.setLong(2, idLead);
            psLead.executeUpdate();

            // 3. Registrar en asignaciones_lead
            psAsig = conn.prepareStatement("INSERT INTO asignaciones_lead (id_lead, id_vendedor_anterior, id_vendedor_nuevo, asignado_por, tipo, motivo, created_at) " +
                                           "VALUES (?, ?, ?, ?, 'MANUAL', ?, NOW())");
            psAsig.setLong(1, idLead);
            if (idVendedorAnt != null) {
                psAsig.setLong(2, idVendedorAnt);
            } else {
                psAsig.setNull(2, Types.BIGINT);
            }
            psAsig.setLong(3, idVendedorNuevo);
            psAsig.setLong(4, asignadoPor);
            psAsig.setString(5, (motivo != null && !motivo.trim().isEmpty()) ? motivo.trim() : "Asignacion manual");
            psAsig.executeUpdate();

            // 4. Registrar en bitacora
            psBit = conn.prepareStatement("INSERT INTO bitacora (id_lead, id_usuario, tipo, descripcion, created_at) VALUES (?, ?, 'ASIGNACION', ?, NOW())");
            psBit.setLong(1, idLead);
            psBit.setLong(2, asignadoPor);
            String descMotivo = (motivo != null && !motivo.trim().isEmpty()) ? motivo.trim() : "Asignacion manual";
            psBit.setString(3, "Lead asignado manualmente a vendedor #" + idVendedorNuevo + ". Motivo: " + descMotivo);
            psBit.executeUpdate();

            conn.commit();
            return true;

        } catch (SQLException e) {
            DatabaseConnection.rollback(conn);
            throw e;
        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(psBit);
            DatabaseConnection.close(psAsig);
            DatabaseConnection.close(psLead);
            DatabaseConnection.close(psGet);
            DatabaseConnection.close(conn);
        }
    }

    /**
     * Ejecuta el reparto equitativo Round-Robin de todos los leads pendientes en la Bolsa Comun.
     */
    public List<AsignacionResumenDTO> ejecutarRoundRobin(long asignadoPor) throws SQLException {
        List<AsignacionResumenDTO> asignaciones = new ArrayList<>();
        Connection conn = null;
        PreparedStatement psLeads = null;
        PreparedStatement psVend = null;
        PreparedStatement psUpd = null;
        PreparedStatement psAsig = null;
        PreparedStatement psBit = null;
        ResultSet rsLeads = null;
        ResultSet rsVend = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Obtener vendedores habilitados
            psVend = conn.prepareStatement("SELECT id_usuario, nombre_completo FROM usuarios WHERE id_rol = 3 AND activo = true AND participa_round_robin = true ORDER BY id_usuario ASC");
            rsVend = psVend.executeQuery();
            List<VendedorRRDTO> vendedores = new ArrayList<>();
            while (rsVend.next()) {
                vendedores.add(new VendedorRRDTO(rsVend.getLong("id_usuario"), rsVend.getString("nombre_completo"), ""));
            }

            if (vendedores.isEmpty()) {
                DatabaseConnection.rollback(conn);
                return asignaciones; // Vacio
            }

            // 2. Obtener leads de bolsa comun (FIFO)
            psLeads = conn.prepareStatement("SELECT id_lead, nombre FROM leads WHERE id_vendedor IS NULL ORDER BY created_at ASC");
            rsLeads = psLeads.executeQuery();
            List<BolsaLeadDTO> leadsPendientes = new ArrayList<>();
            while (rsLeads.next()) {
                leadsPendientes.add(new BolsaLeadDTO(rsLeads.getLong("id_lead"), rsLeads.getString("nombre"), "", "", "", ""));
            }

            if (leadsPendientes.isEmpty()) {
                DatabaseConnection.rollback(conn);
                return asignaciones;
            }

            // 3. Preparar sentencias en lote
            psUpd = conn.prepareStatement("UPDATE leads SET id_vendedor = ?, fecha_asignacion = NOW(), updated_at = NOW() WHERE id_lead = ?");
            psAsig = conn.prepareStatement("INSERT INTO asignaciones_lead (id_lead, id_vendedor_anterior, id_vendedor_nuevo, asignado_por, tipo, motivo, created_at) VALUES (?, NULL, ?, ?, 'ROUND_ROBIN', 'Reparto automatico equitativo', NOW())");
            psBit = conn.prepareStatement("INSERT INTO bitacora (id_lead, id_usuario, tipo, descripcion, created_at) VALUES (?, ?, 'ASIGNACION', ?, NOW())");

            int vendIndex = 0;
            int numVendedores = vendedores.size();

            for (BolsaLeadDTO lead : leadsPendientes) {
                VendedorRRDTO vend = vendedores.get(vendIndex % numVendedores);

                // Update lead
                psUpd.setLong(1, vend.getIdUsuario());
                psUpd.setLong(2, lead.getIdLead());
                psUpd.executeUpdate();

                // Auditoria asignacion
                psAsig.setLong(1, lead.getIdLead());
                psAsig.setLong(2, vend.getIdUsuario());
                psAsig.setLong(3, asignadoPor);
                psAsig.executeUpdate();

                // Auditoria bitacora
                psBit.setLong(1, lead.getIdLead());
                psBit.setLong(2, asignadoPor);
                psBit.setString(3, "Asignado por Round-Robin a " + vend.getNombreCompleto());
                psBit.executeUpdate();

                asignaciones.add(new AsignacionResumenDTO(lead.getIdLead(), lead.getNombre(), vend.getIdUsuario(), vend.getNombreCompleto()));
                vendIndex++;
            }

            conn.commit();
            return asignaciones;

        } catch (SQLException e) {
            DatabaseConnection.rollback(conn);
            throw e;
        } finally {
            DatabaseConnection.close(rsVend);
            DatabaseConnection.close(rsLeads);
            DatabaseConnection.close(psBit);
            DatabaseConnection.close(psAsig);
            DatabaseConnection.close(psUpd);
            DatabaseConnection.close(psVend);
            DatabaseConnection.close(psLeads);
            DatabaseConnection.close(conn);
        }
    }

    /**
     * Transfiere un lead de un vendedor a otro exigiendo motivo formal de traspaso.
     */
    public boolean transferirLead(long idLead, long idNuevoVendedor, long transferidoPor, String motivo) throws SQLException {
        Connection conn = null;
        PreparedStatement psGet = null;
        PreparedStatement psUpd = null;
        PreparedStatement psAsig = null;
        PreparedStatement psBit = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            psGet = conn.prepareStatement("SELECT id_vendedor, nombre FROM leads WHERE id_lead = ?");
            psGet.setLong(1, idLead);
            rs = psGet.executeQuery();
            if (!rs.next()) {
                DatabaseConnection.rollback(conn);
                return false;
            }
            Long idVendedorAnt = rs.getObject("id_vendedor") != null ? rs.getLong("id_vendedor") : null;
            String leadNombre = rs.getString("nombre");

            // Actualizar lead
            psUpd = conn.prepareStatement("UPDATE leads SET id_vendedor = ?, fecha_asignacion = NOW(), updated_at = NOW() WHERE id_lead = ?");
            psUpd.setLong(1, idNuevoVendedor);
            psUpd.setLong(2, idLead);
            psUpd.executeUpdate();

            // Auditoria asignacion
            psAsig = conn.prepareStatement("INSERT INTO asignaciones_lead (id_lead, id_vendedor_anterior, id_vendedor_nuevo, asignado_por, tipo, motivo, created_at) " +
                                           "VALUES (?, ?, ?, ?, 'TRANSFERENCIA', ?, NOW())");
            psAsig.setLong(1, idLead);
            if (idVendedorAnt != null) {
                psAsig.setLong(2, idVendedorAnt);
            } else {
                psAsig.setNull(2, Types.BIGINT);
            }
            psAsig.setLong(3, idNuevoVendedor);
            psAsig.setLong(4, transferidoPor);
            psAsig.setString(5, motivo.trim());
            psAsig.executeUpdate();

            // Auditoria bitacora
            psBit = conn.prepareStatement("INSERT INTO bitacora (id_lead, id_usuario, tipo, descripcion, created_at) VALUES (?, ?, 'TRANSFERENCIA', ?, NOW())");
            psBit.setLong(1, idLead);
            psBit.setLong(2, transferidoPor);
            psBit.setString(3, "Lead transferido a vendedor #" + idNuevoVendedor + ". Motivo: " + motivo.trim());
            psBit.executeUpdate();

            conn.commit();
            return true;

        } catch (SQLException e) {
            DatabaseConnection.rollback(conn);
            throw e;
        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(psBit);
            DatabaseConnection.close(psAsig);
            DatabaseConnection.close(psUpd);
            DatabaseConnection.close(psGet);
            DatabaseConnection.close(conn);
        }
    }

    /**
     * Obtiene el historial cronologico de asignaciones de un prospecto.
     */
    public List<AsignacionHistorialDTO> historialAsignaciones(long idLead) throws SQLException {
        List<AsignacionHistorialDTO> lista = new ArrayList<>();
        String sql = "SELECT a.id_asignacion, a.id_lead, " +
                     "       va.nombre_completo AS vendedor_ant, " +
                     "       vn.nombre_completo AS vendedor_nuevo, " +
                     "       u.nombre_completo AS asignador, " +
                     "       a.tipo, a.motivo, a.created_at " +
                     "FROM asignaciones_lead a " +
                     "LEFT JOIN usuarios va ON a.id_vendedor_anterior = va.id_usuario " +
                     "JOIN usuarios vn ON a.id_vendedor_nuevo = vn.id_usuario " +
                     "JOIN usuarios u ON a.asignado_por = u.id_usuario " +
                     "WHERE a.id_lead = ? " +
                     "ORDER BY a.id_asignacion DESC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, idLead);
            rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new AsignacionHistorialDTO(
                    rs.getLong("id_asignacion"),
                    rs.getLong("id_lead"),
                    rs.getString("vendedor_ant"),
                    rs.getString("vendedor_nuevo"),
                    rs.getString("asignador"),
                    rs.getString("tipo"),
                    rs.getString("motivo"),
                    rs.getString("created_at")
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
