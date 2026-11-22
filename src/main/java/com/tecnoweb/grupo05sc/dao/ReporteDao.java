package com.tecnoweb.grupo05sc.dao;

import com.tecnoweb.grupo05sc.config.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para la consolidacion de metricas, embudo de ventas y reportes analiticos.
 */
public class ReporteDao {

    public static class EmbudoItemDTO {
        private final long idEtapa;
        private final String etapaNombre;
        private final int orden;
        private final int totalLeads;
        private final double porcentaje;

        public EmbudoItemDTO(long idEtapa, String etapaNombre, int orden, int totalLeads, double porcentaje) {
            this.idEtapa = idEtapa;
            this.etapaNombre = etapaNombre;
            this.orden = orden;
            this.totalLeads = totalLeads;
            this.porcentaje = porcentaje;
        }

        public long getIdEtapa() { return idEtapa; }
        public String getEtapaNombre() { return etapaNombre; }
        public int getOrden() { return orden; }
        public int getTotalLeads() { return totalLeads; }
        public double getPorcentaje() { return porcentaje; }
    }

    public static class MetricasGlobalesDTO {
        private final int totalLeads;
        private final int convertidos;
        private final int perdidos;
        private final int enProceso;
        private final double tasaConversion;
        private final double tasaDesercion;

        public MetricasGlobalesDTO(int totalLeads, int convertidos, int perdidos, int enProceso) {
            this.totalLeads = totalLeads;
            this.convertidos = convertidos;
            this.perdidos = perdidos;
            this.enProceso = enProceso;
            this.tasaConversion = (totalLeads > 0) ? ((double) convertidos * 100.0 / totalLeads) : 0.0;
            this.tasaDesercion = (totalLeads > 0) ? ((double) perdidos * 100.0 / totalLeads) : 0.0;
        }

        public int getTotalLeads() { return totalLeads; }
        public int getConvertidos() { return convertidos; }
        public int getPerdidos() { return perdidos; }
        public int getEnProceso() { return enProceso; }
        public double getTasaConversion() { return tasaConversion; }
        public double getTasaDesercion() { return tasaDesercion; }
    }

    public static class VendedorRendimientoDTO {
        private final long idUsuario;
        private final String nombreCompleto;
        private final int totalAsignados;
        private final int convertidos;
        private final int perdidos;
        private final int enProceso;
        private final double tasaConversion;

        public VendedorRendimientoDTO(long idUsuario, String nombreCompleto, int totalAsignados,
                                      int convertidos, int perdidos, int enProceso) {
            this.idUsuario = idUsuario;
            this.nombreCompleto = nombreCompleto;
            this.totalAsignados = totalAsignados;
            this.convertidos = convertidos;
            this.perdidos = perdidos;
            this.enProceso = enProceso;
            this.tasaConversion = (totalAsignados > 0) ? ((double) convertidos * 100.0 / totalAsignados) : 0.0;
        }

        public long getIdUsuario() { return idUsuario; }
        public String getNombreCompleto() { return nombreCompleto; }
        public int getTotalAsignados() { return totalAsignados; }
        public int getConvertidos() { return convertidos; }
        public int getPerdidos() { return perdidos; }
        public int getEnProceso() { return enProceso; }
        public double getTasaConversion() { return tasaConversion; }
    }

    public static class OrigenMetricaDTO {
        private final long idOrigen;
        private final String origenNombre;
        private final int totalLeads;
        private final int convertidos;
        private final double porcentajeParticipacion;
        private final double tasaConversion;

        public OrigenMetricaDTO(long idOrigen, String origenNombre, int totalLeads, int convertidos, int totalGeneral) {
            this.idOrigen = idOrigen;
            this.origenNombre = origenNombre;
            this.totalLeads = totalLeads;
            this.convertidos = convertidos;
            this.porcentajeParticipacion = (totalGeneral > 0) ? ((double) totalLeads * 100.0 / totalGeneral) : 0.0;
            this.tasaConversion = (totalLeads > 0) ? ((double) convertidos * 100.0 / totalLeads) : 0.0;
        }

        public long getIdOrigen() { return idOrigen; }
        public String getOrigenNombre() { return origenNombre; }
        public int getTotalLeads() { return totalLeads; }
        public int getConvertidos() { return convertidos; }
        public double getPorcentajeParticipacion() { return porcentajeParticipacion; }
        public double getTasaConversion() { return tasaConversion; }
    }

    public static class MotivoPerdidaMetricaDTO {
        private final long idMotivo;
        private final String motivoNombre;
        private final int totalPerdidos;
        private final double porcentaje;

        public MotivoPerdidaMetricaDTO(long idMotivo, String motivoNombre, int totalPerdidos, int totalGeneralPerdidos) {
            this.idMotivo = idMotivo;
            this.motivoNombre = motivoNombre;
            this.totalPerdidos = totalPerdidos;
            this.porcentaje = (totalGeneralPerdidos > 0) ? ((double) totalPerdidos * 100.0 / totalGeneralPerdidos) : 0.0;
        }

        public long getIdMotivo() { return idMotivo; }
        public String getMotivoNombre() { return motivoNombre; }
        public int getTotalPerdidos() { return totalPerdidos; }
        public double getPorcentaje() { return porcentaje; }
    }

    public static class DashboardVendedorDTO {
        private final int totalCartera;
        private final int activos;
        private final int convertidos;
        private final int perdidos;
        private final double tasaConversion;
        private final int recordatoriosPendientes;
        private final int bolsaComunDisponibles;

        public DashboardVendedorDTO(int totalCartera, int activos, int convertidos, int perdidos,
                                    int recordatoriosPendientes, int bolsaComunDisponibles) {
            this.totalCartera = totalCartera;
            this.activos = activos;
            this.convertidos = convertidos;
            this.perdidos = perdidos;
            this.tasaConversion = (totalCartera > 0) ? ((double) convertidos * 100.0 / totalCartera) : 0.0;
            this.recordatoriosPendientes = recordatoriosPendientes;
            this.bolsaComunDisponibles = bolsaComunDisponibles;
        }

        public int getTotalCartera() { return totalCartera; }
        public int getActivos() { return activos; }
        public int getConvertidos() { return convertidos; }
        public int getPerdidos() { return perdidos; }
        public double getTasaConversion() { return tasaConversion; }
        public int getRecordatoriosPendientes() { return recordatoriosPendientes; }
        public int getBolsaComunDisponibles() { return bolsaComunDisponibles; }
    }

    /**
     * Obtiene la distribucion de prospectos en el embudo comercial.
     */
    public List<EmbudoItemDTO> obtenerMetricasEmbudo() throws Exception {
        List<EmbudoItemDTO> lista = new ArrayList<>();
        Connection conn = null;
        PreparedStatement psTotal = null;
        PreparedStatement ps = null;
        ResultSet rsTotal = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();

            int totalLeads = 0;
            psTotal = conn.prepareStatement("SELECT COUNT(*) AS total FROM leads");
            rsTotal = psTotal.executeQuery();
            if (rsTotal.next()) {
                totalLeads = rsTotal.getInt("total");
            }

            String sql = "SELECT ep.id_etapa, ep.nombre AS etapa_nombre, ep.orden, COUNT(l.id_lead) AS total_leads " +
                         "FROM etapas_pipeline ep " +
                         "LEFT JOIN leads l ON ep.id_etapa = l.id_etapa " +
                         "GROUP BY ep.id_etapa, ep.nombre, ep.orden " +
                         "ORDER BY ep.orden ASC";
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                int count = rs.getInt("total_leads");
                double pct = (totalLeads > 0) ? ((double) count * 100.0 / totalLeads) : 0.0;
                lista.add(new EmbudoItemDTO(
                    rs.getLong("id_etapa"),
                    rs.getString("etapa_nombre"),
                    rs.getInt("orden"),
                    count,
                    pct
                ));
            }
            return lista;
        } finally {
            DatabaseConnection.close(rsTotal);
            DatabaseConnection.close(psTotal);
            DatabaseConnection.close(rs);
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }
    }

    /**
     * Obtiene el resumen consolidado global de conversion y desercion.
     */
    public MetricasGlobalesDTO obtenerMetricasGlobales() throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT COUNT(*) AS total_leads, " +
                         "       COUNT(CASE WHEN id_etapa = 5 THEN 1 END) AS convertidos, " +
                         "       COUNT(CASE WHEN id_etapa = 6 THEN 1 END) AS perdidos, " +
                         "       COUNT(CASE WHEN id_etapa BETWEEN 1 AND 4 THEN 1 END) AS en_proceso " +
                         "FROM leads";
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            if (rs.next()) {
                return new MetricasGlobalesDTO(
                    rs.getInt("total_leads"),
                    rs.getInt("convertidos"),
                    rs.getInt("perdidos"),
                    rs.getInt("en_proceso")
                );
            }
            return new MetricasGlobalesDTO(0, 0, 0, 0);
        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }
    }

    /**
     * Obtiene el desglose de rendimiento comercial por vendedor.
     */
    public List<VendedorRendimientoDTO> obtenerRendimientoVendedores() throws Exception {
        List<VendedorRendimientoDTO> lista = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT u.id_usuario, u.nombre_completo, " +
                         "       COUNT(l.id_lead) AS total_asignados, " +
                         "       COUNT(CASE WHEN l.id_etapa = 5 THEN 1 END) AS convertidos, " +
                         "       COUNT(CASE WHEN l.id_etapa = 6 THEN 1 END) AS perdidos, " +
                         "       COUNT(CASE WHEN l.id_etapa BETWEEN 1 AND 4 THEN 1 END) AS en_proceso " +
                         "FROM usuarios u " +
                         "LEFT JOIN leads l ON u.id_usuario = l.id_vendedor " +
                         "WHERE u.id_rol = 3 AND u.activo = TRUE " +
                         "GROUP BY u.id_usuario, u.nombre_completo " +
                         "ORDER BY convertidos DESC, total_asignados DESC";
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                lista.add(new VendedorRendimientoDTO(
                    rs.getLong("id_usuario"),
                    rs.getString("nombre_completo"),
                    rs.getInt("total_asignados"),
                    rs.getInt("convertidos"),
                    rs.getInt("perdidos"),
                    rs.getInt("en_proceso")
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
     * Obtiene el desglose y efectividad por canal de captacion (origen).
     */
    public List<OrigenMetricaDTO> obtenerMetricasOrigenes() throws Exception {
        List<OrigenMetricaDTO> lista = new ArrayList<>();
        Connection conn = null;
        PreparedStatement psTotal = null;
        PreparedStatement ps = null;
        ResultSet rsTotal = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();

            int totalGeneral = 0;
            psTotal = conn.prepareStatement("SELECT COUNT(*) AS total FROM leads");
            rsTotal = psTotal.executeQuery();
            if (rsTotal.next()) {
                totalGeneral = rsTotal.getInt("total");
            }

            String sql = "SELECT ol.id_origen, ol.nombre AS origen_nombre, " +
                         "       COUNT(l.id_lead) AS total_leads, " +
                         "       COUNT(CASE WHEN l.id_etapa = 5 THEN 1 END) AS convertidos " +
                         "FROM origenes_lead ol " +
                         "LEFT JOIN leads l ON ol.id_origen = l.id_origen " +
                         "GROUP BY ol.id_origen, ol.nombre " +
                         "ORDER BY total_leads DESC";
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                lista.add(new OrigenMetricaDTO(
                    rs.getLong("id_origen"),
                    rs.getString("origen_nombre"),
                    rs.getInt("total_leads"),
                    rs.getInt("convertidos"),
                    totalGeneral
                ));
            }
            return lista;
        } finally {
            DatabaseConnection.close(rsTotal);
            DatabaseConnection.close(psTotal);
            DatabaseConnection.close(rs);
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }
    }

    /**
     * Obtiene el desglose y frecuencia de las causas de perdida comercial.
     */
    public List<MotivoPerdidaMetricaDTO> obtenerMetricasMotivosPerdida() throws Exception {
        List<MotivoPerdidaMetricaDTO> lista = new ArrayList<>();
        Connection conn = null;
        PreparedStatement psTotal = null;
        PreparedStatement ps = null;
        ResultSet rsTotal = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();

            int totalPerdidos = 0;
            psTotal = conn.prepareStatement("SELECT COUNT(*) AS total FROM leads WHERE id_etapa = 6");
            rsTotal = psTotal.executeQuery();
            if (rsTotal.next()) {
                totalPerdidos = rsTotal.getInt("total");
            }

            String sql = "SELECT mp.id_motivo, mp.nombre AS motivo_nombre, " +
                         "       COUNT(l.id_lead) AS total_perdidos " +
                         "FROM motivos_perdida mp " +
                         "LEFT JOIN leads l ON mp.id_motivo = l.id_motivo_perdida AND l.id_etapa = 6 " +
                         "GROUP BY mp.id_motivo, mp.nombre " +
                         "ORDER BY total_perdidos DESC";
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                lista.add(new MotivoPerdidaMetricaDTO(
                    rs.getLong("id_motivo"),
                    rs.getString("motivo_nombre"),
                    rs.getInt("total_perdidos"),
                    totalPerdidos
                ));
            }
            return lista;
        } finally {
            DatabaseConnection.close(rsTotal);
            DatabaseConnection.close(psTotal);
            DatabaseConnection.close(rs);
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }
    }

    /**
     * Obtiene las metricas operativas y comerciales del dashboard de un vendedor.
     */
    public DashboardVendedorDTO obtenerDashboardVendedor(long idVendedor) throws Exception {
        Connection conn = null;
        PreparedStatement psCartera = null;
        PreparedStatement psRec = null;
        PreparedStatement psBolsa = null;
        ResultSet rsCartera = null;
        ResultSet rsRec = null;
        ResultSet rsBolsa = null;

        try {
            conn = DatabaseConnection.getConnection();

            int totalCartera = 0;
            int activos = 0;
            int convertidos = 0;
            int perdidos = 0;

            String sqlCartera = "SELECT COUNT(l.id_lead) AS total_cartera, " +
                                "       COUNT(CASE WHEN l.id_etapa BETWEEN 1 AND 4 THEN 1 END) AS activos, " +
                                "       COUNT(CASE WHEN l.id_etapa = 5 THEN 1 END) AS convertidos, " +
                                "       COUNT(CASE WHEN l.id_etapa = 6 THEN 1 END) AS perdidos " +
                                "FROM leads l WHERE l.id_vendedor = ?";
            psCartera = conn.prepareStatement(sqlCartera);
            psCartera.setLong(1, idVendedor);
            rsCartera = psCartera.executeQuery();
            if (rsCartera.next()) {
                totalCartera = rsCartera.getInt("total_cartera");
                activos = rsCartera.getInt("activos");
                convertidos = rsCartera.getInt("convertidos");
                perdidos = rsCartera.getInt("perdidos");
            }

            int recPendientes = 0;
            String sqlRec = "SELECT COUNT(*) AS total FROM recordatorios WHERE id_usuario = ? AND estado = 'PENDIENTE'";
            psRec = conn.prepareStatement(sqlRec);
            psRec.setLong(1, idVendedor);
            rsRec = psRec.executeQuery();
            if (rsRec.next()) {
                recPendientes = rsRec.getInt("total");
            }

            int bolsaComun = 0;
            String sqlBolsa = "SELECT COUNT(*) AS total FROM leads WHERE id_vendedor IS NULL AND id_etapa NOT IN (5, 6)";
            psBolsa = conn.prepareStatement(sqlBolsa);
            rsBolsa = psBolsa.executeQuery();
            if (rsBolsa.next()) {
                bolsaComun = rsBolsa.getInt("total");
            }

            return new DashboardVendedorDTO(
                totalCartera, activos, convertidos, perdidos, recPendientes, bolsaComun
            );
        } finally {
            DatabaseConnection.close(rsCartera);
            DatabaseConnection.close(psCartera);
            DatabaseConnection.close(rsRec);
            DatabaseConnection.close(psRec);
            DatabaseConnection.close(rsBolsa);
            DatabaseConnection.close(psBolsa);
            DatabaseConnection.close(conn);
        }
    }
}
