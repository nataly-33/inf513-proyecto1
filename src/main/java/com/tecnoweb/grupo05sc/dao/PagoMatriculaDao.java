package com.tecnoweb.grupo05sc.dao;

import com.tecnoweb.grupo05sc.config.DatabaseConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Capa de Acceso a Datos (DAO) para Validacion de Pagos y Conversion a Matricula.
 * Gestiona el registro y auditoria de comprobantes bancarios, verificacion administrativa
 * y formalizacion del expediente academico en PostgreSQL.
 */
public final class PagoMatriculaDao {

    // =========================================================================
    // DTOs DE TRANSFERENCIA DE DATOS
    // =========================================================================

    public static class ComprobanteDTO {
        private final long idComprobante;
        private final long idLead;
        private final String leadNombre;
        private final String vendedorNombre;
        private final String archivoUrl;
        private final BigDecimal monto;
        private final String estado;
        private final String revisadoPor;
        private final String comentarioRechazo;
        private final String createdAt;

        public ComprobanteDTO(long idComprobante, long idLead, String leadNombre, String vendedorNombre,
                              String archivoUrl, BigDecimal monto, String estado, String revisadoPor,
                              String comentarioRechazo, String createdAt) {
            this.idComprobante = idComprobante;
            this.idLead = idLead;
            this.leadNombre = leadNombre;
            this.vendedorNombre = vendedorNombre;
            this.archivoUrl = archivoUrl;
            this.monto = monto;
            this.estado = estado;
            this.revisadoPor = revisadoPor;
            this.comentarioRechazo = comentarioRechazo;
            this.createdAt = createdAt;
        }

        public long getIdComprobante() { return idComprobante; }
        public long getIdLead() { return idLead; }
        public String getLeadNombre() { return leadNombre; }
        public String getVendedorNombre() { return vendedorNombre; }
        public String getArchivoUrl() { return archivoUrl; }
        public BigDecimal getMonto() { return monto; }
        public String getEstado() { return estado; }
        public String getRevisadoPor() { return revisadoPor != null ? revisadoPor : "-"; }
        public String getComentarioRechazo() { return comentarioRechazo != null ? comentarioRechazo : "-"; }
        public String getCreatedAt() { return createdAt != null ? createdAt : "-"; }
    }

    public static class DatosAlumnoDTO {
        private final long idLead;
        private final String nombre;
        private final String telefono;
        private final String correo;
        private final String ci;
        private final String fechaNacimiento;
        private final String ciudad;
        private final long idEtapa;
        private final String etapaNombre;

        public DatosAlumnoDTO(long idLead, String nombre, String telefono, String correo,
                              String ci, String fechaNacimiento, String ciudad,
                              long idEtapa, String etapaNombre) {
            this.idLead = idLead;
            this.nombre = nombre;
            this.telefono = telefono;
            this.correo = correo;
            this.ci = ci;
            this.fechaNacimiento = fechaNacimiento;
            this.ciudad = ciudad;
            this.idEtapa = idEtapa;
            this.etapaNombre = etapaNombre;
        }

        public long getIdLead() { return idLead; }
        public String getNombre() { return nombre; }
        public String getTelefono() { return telefono; }
        public String getCorreo() { return correo != null ? correo : "-"; }
        public String getCi() { return ci != null ? ci : ""; }
        public String getFechaNacimiento() { return fechaNacimiento != null ? fechaNacimiento : ""; }
        public String getCiudad() { return ciudad != null ? ciudad : ""; }
        public long getIdEtapa() { return idEtapa; }
        public String getEtapaNombre() { return etapaNombre != null ? etapaNombre : "Nuevo"; }

        public boolean tieneDatosCompletos() {
            return ci != null && !ci.trim().isEmpty() &&
                   fechaNacimiento != null && !fechaNacimiento.trim().isEmpty() &&
                   ciudad != null && !ciudad.trim().isEmpty();
        }
    }

    // =========================================================================
    // OPERACIONES DE BASE DE DATOS
    // =========================================================================

    /**
     * Registra un nuevo comprobante bancario para un prospecto.
     * Si el lead se encuentra en etapa 1, 2 o 3, lo avanza a etapa 4 (Promesa de Pago).
     */
    public long registrarComprobante(long idLead, long subidoPor, String banco, String nroTrans, BigDecimal monto) throws SQLException {
        String sqlComp = "INSERT INTO comprobantes_pago (id_lead, subido_por, archivo_url, tipo_archivo, monto, estado, created_at, updated_at) " +
                         "VALUES (?, ?, ?, 'TRANSF', ?, 'PENDIENTE', NOW(), NOW())";
        String sqlLead = "SELECT id_etapa FROM leads WHERE id_lead = ? FOR UPDATE";
        String sqlUpdLead = "UPDATE leads SET id_etapa = 4, updated_at = NOW() WHERE id_lead = ?";
        String sqlBit = "INSERT INTO bitacora (id_lead, id_usuario, tipo, id_etapa_anterior, id_etapa_nueva, descripcion, created_at) " +
                        "VALUES (?, ?, 'PAGO', ?, ?, ?, NOW())";

        Connection conn = null;
        PreparedStatement psComp = null;
        PreparedStatement psLead = null;
        PreparedStatement psUpdLead = null;
        PreparedStatement psBit = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Insertar comprobante
            String ref = banco.trim() + " - Transaccion #" + nroTrans.trim();
            psComp = conn.prepareStatement(sqlComp, Statement.RETURN_GENERATED_KEYS);
            psComp.setLong(1, idLead);
            psComp.setLong(2, subidoPor);
            psComp.setString(3, ref);
            psComp.setBigDecimal(4, monto);
            psComp.executeUpdate();

            rs = psComp.getGeneratedKeys();
            if (!rs.next()) {
                throw new SQLException("No se pudo registrar el comprobante de pago.");
            }
            long idComprobante = rs.getLong(1);

            // 2. Evaluar avance de etapa a Promesa de Pago (Etapa 4)
            psLead = conn.prepareStatement(sqlLead);
            psLead.setLong(1, idLead);
            ResultSet rsLead = psLead.executeQuery();
            long idEtapaAnt = 1L;
            if (rsLead.next()) {
                idEtapaAnt = rsLead.getLong("id_etapa");
            }
            rsLead.close();

            Long etapaNueva = null;
            if (idEtapaAnt < 4) {
                psUpdLead = conn.prepareStatement(sqlUpdLead);
                psUpdLead.setLong(1, idLead);
                psUpdLead.executeUpdate();
                etapaNueva = 4L;
            }

            // 3. Registrar en bitacora
            psBit = conn.prepareStatement(sqlBit);
            psBit.setLong(1, idLead);
            psBit.setLong(2, subidoPor);
            psBit.setLong(3, idEtapaAnt);
            if (etapaNueva != null) {
                psBit.setLong(4, etapaNueva);
            } else {
                psBit.setNull(4, Types.BIGINT);
            }
            psBit.setString(5, "Comprobante de pago #" + idComprobante + " registrado: " + ref + " por " + monto + " Bs.");
            psBit.executeUpdate();

            conn.commit();
            return idComprobante;

        } catch (SQLException e) {
            DatabaseConnection.rollback(conn);
            throw e;
        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(psBit);
            DatabaseConnection.close(psUpdLead);
            DatabaseConnection.close(psLead);
            DatabaseConnection.close(psComp);
            DatabaseConnection.close(conn);
        }
    }

    /**
     * Lista todos los comprobantes con estado PENDIENTE de revision.
     */
    public List<ComprobanteDTO> listarPagosPendientes() throws SQLException {
        List<ComprobanteDTO> lista = new ArrayList<>();
        String sql = "SELECT c.id_comprobante, c.id_lead, l.nombre AS lead_nombre, " +
                     "       COALESCE(u.nombre_completo, 'Sistema') AS vendedor_nombre, " +
                     "       c.archivo_url, c.monto, c.estado, NULL AS revisado_por, NULL AS comentario_rechazo, " +
                     "       TO_CHAR(c.created_at, 'YYYY-MM-DD HH24:MI:SS') AS fecha " +
                     "FROM comprobantes_pago c " +
                     "JOIN leads l ON c.id_lead = l.id_lead " +
                     "JOIN usuarios u ON c.subido_por = u.id_usuario " +
                     "WHERE c.estado = 'PENDIENTE' " +
                     "ORDER BY c.created_at ASC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new ComprobanteDTO(
                    rs.getLong("id_comprobante"),
                    rs.getLong("id_lead"),
                    rs.getString("lead_nombre"),
                    rs.getString("vendedor_nombre"),
                    rs.getString("archivo_url"),
                    rs.getBigDecimal("monto"),
                    rs.getString("estado"),
                    null,
                    null,
                    rs.getString("fecha")
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
     * Busca un comprobante por su ID.
     */
    public ComprobanteDTO buscarComprobantePorId(long idComprobante) throws SQLException {
        String sql = "SELECT c.id_comprobante, c.id_lead, l.nombre AS lead_nombre, " +
                     "       COALESCE(u.nombre_completo, 'Sistema') AS vendedor_nombre, " +
                     "       c.archivo_url, c.monto, c.estado, " +
                     "       COALESCE(ur.nombre_completo, '-') AS revisor_nombre, " +
                     "       c.comentario_rechazo, " +
                     "       TO_CHAR(c.created_at, 'YYYY-MM-DD HH24:MI:SS') AS fecha " +
                     "FROM comprobantes_pago c " +
                     "JOIN leads l ON c.id_lead = l.id_lead " +
                     "JOIN usuarios u ON c.subido_por = u.id_usuario " +
                     "LEFT JOIN usuarios ur ON c.revisado_por = ur.id_usuario " +
                     "WHERE c.id_comprobante = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, idComprobante);
            rs = ps.executeQuery();
            if (rs.next()) {
                return new ComprobanteDTO(
                    rs.getLong("id_comprobante"),
                    rs.getLong("id_lead"),
                    rs.getString("lead_nombre"),
                    rs.getString("vendedor_nombre"),
                    rs.getString("archivo_url"),
                    rs.getBigDecimal("monto"),
                    rs.getString("estado"),
                    rs.getString("revisor_nombre"),
                    rs.getString("comentario_rechazo"),
                    rs.getString("fecha")
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
     * Aprueba un comprobante bancario.
     */
    public boolean aprobarPago(long idComprobante, long revisadoPor) throws SQLException {
        String sqlGet = "SELECT id_lead, monto FROM comprobantes_pago WHERE id_comprobante = ? AND estado = 'PENDIENTE' FOR UPDATE";
        String sqlUpd = "UPDATE comprobantes_pago SET estado = 'APROBADO', revisado_por = ?, fecha_revision = NOW(), updated_at = NOW() WHERE id_comprobante = ?";
        String sqlBit = "INSERT INTO bitacora (id_lead, id_usuario, tipo, descripcion, created_at) VALUES (?, ?, 'PAGO_APROBADO', ?, NOW())";

        Connection conn = null;
        PreparedStatement psGet = null;
        PreparedStatement psUpd = null;
        PreparedStatement psBit = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            psGet = conn.prepareStatement(sqlGet);
            psGet.setLong(1, idComprobante);
            rs = psGet.executeQuery();
            if (!rs.next()) {
                conn.rollback();
                return false;
            }
            long idLead = rs.getLong("id_lead");
            BigDecimal monto = rs.getBigDecimal("monto");

            psUpd = conn.prepareStatement(sqlUpd);
            psUpd.setLong(1, revisadoPor);
            psUpd.setLong(2, idComprobante);
            psUpd.executeUpdate();

            psBit = conn.prepareStatement(sqlBit);
            psBit.setLong(1, idLead);
            psBit.setLong(2, revisadoPor);
            psBit.setString(3, "Comprobante de pago #" + idComprobante + " por " + monto + " Bs. fue APROBADO satisfactoriamente.");
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
     * Rechaza un comprobante bancario registrando motivo.
     */
    public boolean rechazarPago(long idComprobante, long revisadoPor, String motivoRechazo) throws SQLException {
        String sqlGet = "SELECT id_lead FROM comprobantes_pago WHERE id_comprobante = ? AND estado = 'PENDIENTE' FOR UPDATE";
        String sqlUpd = "UPDATE comprobantes_pago SET estado = 'RECHAZADO', revisado_por = ?, comentario_rechazo = ?, fecha_revision = NOW(), updated_at = NOW() WHERE id_comprobante = ?";
        String sqlBit = "INSERT INTO bitacora (id_lead, id_usuario, tipo, descripcion, created_at) VALUES (?, ?, 'PAGO_RECHAZADO', ?, NOW())";

        Connection conn = null;
        PreparedStatement psGet = null;
        PreparedStatement psUpd = null;
        PreparedStatement psBit = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            psGet = conn.prepareStatement(sqlGet);
            psGet.setLong(1, idComprobante);
            rs = psGet.executeQuery();
            if (!rs.next()) {
                conn.rollback();
                return false;
            }
            long idLead = rs.getLong("id_lead");

            psUpd = conn.prepareStatement(sqlUpd);
            psUpd.setLong(1, revisadoPor);
            psUpd.setString(2, motivoRechazo.trim());
            psUpd.setLong(3, idComprobante);
            psUpd.executeUpdate();

            psBit = conn.prepareStatement(sqlBit);
            psBit.setLong(1, idLead);
            psBit.setLong(2, revisadoPor);
            psBit.setString(3, "Comprobante #" + idComprobante + " RECHAZADO. Observacion: " + motivoRechazo.trim());
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
     * Obtiene los datos del expediente del alumno para un lead.
     */
    public DatosAlumnoDTO obtenerDatosAlumno(long idLead) throws SQLException {
        String sql = "SELECT l.id_lead, l.nombre, l.telefono, l.correo, l.ci, " +
                     "       TO_CHAR(l.fecha_nacimiento, 'YYYY-MM-DD') AS fecha_nac, " +
                     "       l.ciudad, l.id_etapa, e.nombre AS etapa_nombre " +
                     "FROM leads l " +
                     "JOIN etapas_pipeline e ON l.id_etapa = e.id_etapa " +
                     "WHERE l.id_lead = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, idLead);
            rs = ps.executeQuery();
            if (rs.next()) {
                return new DatosAlumnoDTO(
                    rs.getLong("id_lead"),
                    rs.getString("nombre"),
                    rs.getString("telefono"),
                    rs.getString("correo"),
                    rs.getString("ci"),
                    rs.getString("fecha_nac"),
                    rs.getString("ciudad"),
                    rs.getLong("id_etapa"),
                    rs.getString("etapa_nombre")
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
     * Actualiza los datos formales de matricula (CI, fecha de nacimiento, ciudad).
     */
    public boolean actualizarDatosAlumno(long idLead, String ci, String fechaNac, String ciudad, long idUsuario) throws SQLException {
        String sqlUpd = "UPDATE leads SET ci = ?, fecha_nacimiento = ?::date, ciudad = ?, updated_at = NOW() WHERE id_lead = ?";
        String sqlBit = "INSERT INTO bitacora (id_lead, id_usuario, tipo, descripcion, created_at) VALUES (?, ?, 'DATOS_ALUMNO', ?, NOW())";

        Connection conn = null;
        PreparedStatement psUpd = null;
        PreparedStatement psBit = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            psUpd = conn.prepareStatement(sqlUpd);
            psUpd.setString(1, ci.trim());
            psUpd.setDate(2, Date.valueOf(fechaNac.trim()));
            psUpd.setString(3, ciudad.trim());
            psUpd.setLong(4, idLead);
            int rows = psUpd.executeUpdate();
            if (rows == 0) {
                conn.rollback();
                return false;
            }

            psBit = conn.prepareStatement(sqlBit);
            psBit.setLong(1, idLead);
            psBit.setLong(2, idUsuario);
            psBit.setString(3, "Expediente academico completado: CI " + ci.trim() + ", Ciudad: " + ciudad.trim());
            psBit.executeUpdate();

            conn.commit();
            return true;

        } catch (SQLException e) {
            DatabaseConnection.rollback(conn);
            throw e;
        } finally {
            DatabaseConnection.close(psBit);
            DatabaseConnection.close(psUpd);
            DatabaseConnection.close(conn);
        }
    }

    /**
     * Verifica si un prospecto tiene al menos un pago aprobado.
     */
    public Long obtenerIdComprobanteAprobado(long idLead) throws SQLException {
        String sql = "SELECT id_comprobante FROM comprobantes_pago WHERE id_lead = ? AND estado = 'APROBADO' ORDER BY id_comprobante DESC LIMIT 1";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, idLead);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("id_comprobante");
            }
            return null;
        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }
    }

    /**
     * Formaliza la conversion a matricula (Etapa 5):
     * Actualiza lead a etapa 5, registra en envios_matricula y audita en bitacora.
     */
    public boolean formalizarMatricula(long idLead, long idComprobante, String jsonPayload, long idUsuario) throws SQLException {
        String sqlLead = "UPDATE leads SET id_etapa = 5, updated_at = NOW() WHERE id_lead = ?";
        String sqlEnvio = "INSERT INTO envios_matricula (id_lead, id_comprobante, payload, estado, created_at, updated_at) " +
                          "VALUES (?, ?, ?::jsonb, 'COMPLETADO', NOW(), NOW())";
        String sqlBit = "INSERT INTO bitacora (id_lead, id_usuario, tipo, id_etapa_anterior, id_etapa_nueva, descripcion, created_at) " +
                        "VALUES (?, ?, 'MATRICULA_CONVERTIDA', 4, 5, 'Prospecto formalizado exitosamente a alumno matriculado oficial.', NOW())";

        Connection conn = null;
        PreparedStatement psLead = null;
        PreparedStatement psEnvio = null;
        PreparedStatement psBit = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Actualizar lead a etapa 5
            psLead = conn.prepareStatement(sqlLead);
            psLead.setLong(1, idLead);
            psLead.executeUpdate();

            // 2. Registrar en envios_matricula
            psEnvio = conn.prepareStatement(sqlEnvio);
            psEnvio.setLong(1, idLead);
            psEnvio.setLong(2, idComprobante);
            psEnvio.setString(3, jsonPayload);
            psEnvio.executeUpdate();

            // 3. Registrar en bitacora
            psBit = conn.prepareStatement(sqlBit);
            psBit.setLong(1, idLead);
            psBit.setLong(2, idUsuario);
            psBit.executeUpdate();

            conn.commit();
            return true;

        } catch (SQLException e) {
            DatabaseConnection.rollback(conn);
            throw e;
        } finally {
            DatabaseConnection.close(psBit);
            DatabaseConnection.close(psEnvio);
            DatabaseConnection.close(psLead);
            DatabaseConnection.close(conn);
        }
    }
}
