package com.tecnoweb.grupo05sc.dao;

import com.tecnoweb.grupo05sc.config.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Capa de Acceso a Datos (DAO) para la Captacion de Leads y Oferta Academica (CU2).
 * Persistencia en PostgreSQL mediante consultas 100% parametrizadas (PreparedStatement)
 * y cierre seguro de recursos en bloques finally.
 */
public final class LeadDao {

    // =========================================================================
    // DTOs DE TRANSFERENCIA DE DATOS
    // =========================================================================

    public static class LeadDTO {
        private final long idLead;
        private final String nombre;
        private final String telefono;
        private final String correo;
        private final long idOrigen;
        private final String origenNombre;
        private final long idEtapa;
        private final String etapaNombre;
        private final Long idVendedor;
        private final String vendedorNombre;
        private final Long idProfesion;
        private final String profesionNombre;
        private final String createdAt;

        public LeadDTO(long idLead, String nombre, String telefono, String correo,
                       long idOrigen, String origenNombre, long idEtapa, String etapaNombre,
                       Long idVendedor, String vendedorNombre, Long idProfesion, String profesionNombre,
                       String createdAt) {
            this.idLead = idLead;
            this.nombre = nombre;
            this.telefono = telefono;
            this.correo = correo;
            this.idOrigen = idOrigen;
            this.origenNombre = origenNombre;
            this.idEtapa = idEtapa;
            this.etapaNombre = etapaNombre;
            this.idVendedor = idVendedor;
            this.vendedorNombre = vendedorNombre;
            this.idProfesion = idProfesion;
            this.profesionNombre = profesionNombre;
            this.createdAt = createdAt;
        }

        public long getIdLead() { return idLead; }
        public String getNombre() { return nombre; }
        public String getTelefono() { return telefono; }
        public String getCorreo() { return correo != null ? correo : "-"; }
        public long getIdOrigen() { return idOrigen; }
        public String getOrigenNombre() { return origenNombre != null ? origenNombre : "No especificado"; }
        public long getIdEtapa() { return idEtapa; }
        public String getEtapaNombre() { return etapaNombre != null ? etapaNombre : "Nuevo"; }
        public Long getIdVendedor() { return idVendedor; }
        public String getVendedorNombre() { return vendedorNombre != null ? vendedorNombre : "Bolsa Comun (Sin Asignar)"; }
        public Long getIdProfesion() { return idProfesion; }
        public String getProfesionNombre() { return profesionNombre != null ? profesionNombre : "No especificada"; }
        public String getCreatedAt() { return createdAt != null ? createdAt : "-"; }
    }

    public static class LeadInteresDTO {
        private final long idInteres;
        private final long idLead;
        private final String tipo;
        private final Long idVersionExterno;
        private final Long idModuloExterno;
        private final String nombreSnapshot;

        public LeadInteresDTO(long idInteres, long idLead, String tipo, Long idVersionExterno, Long idModuloExterno, String nombreSnapshot) {
            this.idInteres = idInteres;
            this.idLead = idLead;
            this.tipo = tipo;
            this.idVersionExterno = idVersionExterno;
            this.idModuloExterno = idModuloExterno;
            this.nombreSnapshot = nombreSnapshot;
        }

        public long getIdInteres() { return idInteres; }
        public long getIdLead() { return idLead; }
        public String getTipo() { return tipo; }
        public Long getIdVersionExterno() { return idVersionExterno; }
        public Long getIdModuloExterno() { return idModuloExterno; }
        public String getNombreSnapshot() { return nombreSnapshot; }
    }

    public static class CatalogoDTO {
        private final long id;
        private final String nombre;

        public CatalogoDTO(long id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        public long getId() { return id; }
        public String getNombre() { return nombre; }
    }

    // =========================================================================
    // OPERACIONES DE PERSISTENCIA
    // =========================================================================

    /**
     * Inserta un nuevo prospecto en la tabla leads, ubicandolo en etapa 1 (Nuevo)
     * y en la Bolsa Comun (id_vendedor = NULL). Tambien genera el hito inicial en bitacora.
     */
    public long insertarLead(String nombre, String telefono, String correo, long idOrigen, Long idProfesion, long createdBy) throws SQLException {
        String sqlLead = "INSERT INTO leads (nombre, telefono, correo, id_origen, id_etapa, id_vendedor, created_by, id_profesion, created_at, updated_at) " +
                         "VALUES (?, ?, ?, ?, 1, NULL, ?, ?, NOW(), NOW())";

        String sqlBitacora = "INSERT INTO bitacora (id_lead, id_usuario, tipo, descripcion, id_etapa_nueva, created_at) " +
                            "VALUES (?, ?, 'CREACION', 'Prospecto captado e ingresado a Bolsa Comun.', 1, NOW())";

        Connection conn = null;
        PreparedStatement psLead = null;
        PreparedStatement psBit = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Transaccion atomica

            psLead = conn.prepareStatement(sqlLead, Statement.RETURN_GENERATED_KEYS);
            psLead.setString(1, nombre.trim());
            psLead.setString(2, telefono.trim());
            if (correo != null && !correo.trim().isEmpty()) {
                psLead.setString(3, correo.trim().toLowerCase());
            } else {
                psLead.setNull(3, Types.VARCHAR);
            }
            psLead.setLong(4, idOrigen);
            psLead.setLong(5, createdBy);

            if (idProfesion != null && idProfesion > 0) {
                psLead.setLong(6, idProfesion);
            } else {
                psLead.setNull(6, Types.BIGINT);
            }

            psLead.executeUpdate();
            rs = psLead.getGeneratedKeys();

            if (!rs.next()) {
                throw new SQLException("No se pudo obtener el ID del prospecto registrado.");
            }
            long idLead = rs.getLong(1);

            // Registrar en bitacora institucional
            psBit = conn.prepareStatement(sqlBitacora);
            psBit.setLong(1, idLead);
            psBit.setLong(2, createdBy);
            psBit.executeUpdate();

            conn.commit();
            return idLead;

        } catch (SQLException e) {
            DatabaseConnection.rollback(conn);
            throw e;
        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(psBit);
            DatabaseConnection.close(psLead);
            DatabaseConnection.close(conn);
        }
    }

    /**
     * Busca un prospecto por su ID primario.
     */
    public LeadDTO buscarPorId(long idLead) throws SQLException {
        String sql = "SELECT l.id_lead, l.nombre, l.telefono, l.correo, l.id_origen, o.nombre AS origen_nombre, " +
                     "       l.id_etapa, e.nombre AS etapa_nombre, l.id_vendedor, u.nombre_completo AS vendedor_nombre, " +
                     "       l.id_profesion, p.nombre AS profesion_nombre, l.created_at " +
                     "FROM leads l " +
                     "JOIN origenes_lead o ON l.id_origen = o.id_origen " +
                     "JOIN etapas_pipeline e ON l.id_etapa = e.id_etapa " +
                     "LEFT JOIN usuarios u ON l.id_vendedor = u.id_usuario " +
                     "LEFT JOIN profesiones p ON l.id_profesion = p.id_profesion " +
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
                return mapLead(rs);
            }
            return null;
        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }
    }

    /**
     * Vincula un interes academico a un prospecto en lead_interes.
     * Respeta la restriccion check: chk_tipo_interes (PROGRAMA -> version, MODULO -> modulo).
     */
    public long vincularInteres(long idLead, String tipo, Long idExterno, String nombreSnapshot) throws SQLException {
        String sqlInteres = "INSERT INTO lead_interes (id_lead, tipo, id_version_externo, id_modulo_externo, nombre_snapshot) " +
                            "VALUES (?, ?, ?, ?, ?)";

        String sqlBitacora = "INSERT INTO bitacora (id_lead, id_usuario, tipo, descripcion, created_at) " +
                             "VALUES (?, 1, 'INTERES', ?, NOW())";

        Connection conn = null;
        PreparedStatement psInt = null;
        PreparedStatement psBit = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            psInt = conn.prepareStatement(sqlInteres, Statement.RETURN_GENERATED_KEYS);
            psInt.setLong(1, idLead);
            psInt.setString(2, tipo.toUpperCase());

            if ("PROGRAMA".equalsIgnoreCase(tipo)) {
                psInt.setLong(3, idExterno);
                psInt.setNull(4, Types.BIGINT);
            } else {
                psInt.setNull(3, Types.BIGINT);
                psInt.setLong(4, idExterno);
            }
            psInt.setString(5, nombreSnapshot.trim());
            psInt.executeUpdate();

            rs = psInt.getGeneratedKeys();
            if (!rs.next()) {
                throw new SQLException("No se pudo generar el registro de interes.");
            }
            long idInteres = rs.getLong(1);

            // Registro en bitacora
            psBit = conn.prepareStatement(sqlBitacora);
            psBit.setLong(1, idLead);
            psBit.setString(2, "Vinculo interes academico: " + nombreSnapshot + " (" + tipo + " #" + idExterno + ")");
            psBit.executeUpdate();

            conn.commit();
            return idInteres;

        } catch (SQLException e) {
            DatabaseConnection.rollback(conn);
            throw e;
        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(psBit);
            DatabaseConnection.close(psInt);
            DatabaseConnection.close(conn);
        }
    }

    /**
     * Lista los intereses academicos de un prospecto.
     */
    public List<LeadInteresDTO> listarIntereses(long idLead) throws SQLException {
        List<LeadInteresDTO> lista = new ArrayList<>();
        String sql = "SELECT id_interes, id_lead, tipo, id_version_externo, id_modulo_externo, nombre_snapshot " +
                     "FROM lead_interes WHERE id_lead = ? ORDER BY id_interes ASC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, idLead);
            rs = ps.executeQuery();

            while (rs.next()) {
                Long idVer = rs.getObject("id_version_externo") != null ? rs.getLong("id_version_externo") : null;
                Long idMod = rs.getObject("id_modulo_externo") != null ? rs.getLong("id_modulo_externo") : null;
                lista.add(new LeadInteresDTO(
                    rs.getLong("id_interes"),
                    rs.getLong("id_lead"),
                    rs.getString("tipo"),
                    idVer,
                    idMod,
                    rs.getString("nombre_snapshot")
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
     * Lista todos los origenes de captacion.
     */
    public List<CatalogoDTO> listarOrigenes() throws SQLException {
        List<CatalogoDTO> lista = new ArrayList<>();
        String sql = "SELECT id_origen, nombre FROM origenes_lead ORDER BY id_origen ASC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new CatalogoDTO(rs.getLong("id_origen"), rs.getString("nombre")));
            }
            return lista;
        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }
    }

    /**
     * Normaliza cadenas de texto eliminando acentos, caracteres especiales, y exceso de espacios.
     */
    public static String normalizeText(String s) {
        if (s == null) return "";
        return s.toLowerCase()
                .replace('á', 'a').replace('é', 'e').replace('í', 'i').replace('ó', 'o').replace('ú', 'u')
                .replace('ñ', 'n').replace('/', ' ').replace('-', ' ').replace('.', ' ').replace(',', ' ')
                .replaceAll("\\s+", " ").trim();
    }

    /**
     * Valida si un canal de origen existe en la base de datos (por ID o por nombre).
     * Soporta coincidencias flexibles sin distincion de mayusculas ni acentos.
     * Devuelve el id_origen correspondiente, o null si no existe.
     */
    public Long buscarIdOrigen(String origen) throws SQLException {
        if (origen == null || origen.trim().isEmpty()) return 3L; // Web por defecto si viene vacio
        String clean = origen.trim();

        // 1. Si es numerico, verificar por id_origen
        if (clean.matches("\\d+")) {
            long id = Long.parseLong(clean);
            String sqlId = "SELECT id_origen FROM origenes_lead WHERE id_origen = ?";
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                conn = DatabaseConnection.getConnection();
                ps = conn.prepareStatement(sqlId);
                ps.setLong(1, id);
                rs = ps.executeQuery();
                if (rs.next()) return rs.getLong("id_origen");
            } finally {
                DatabaseConnection.close(rs);
                DatabaseConnection.close(ps);
                DatabaseConnection.close(conn);
            }
        }

        // 2. Coincidencia flexible contra catalogo de origenes
        String inputNorm = normalizeText(clean);
        List<CatalogoDTO> todos = listarOrigenes();
        for (CatalogoDTO cat : todos) {
            String dbNorm = normalizeText(cat.getNombre());
            if (dbNorm.equals(inputNorm) || dbNorm.contains(inputNorm) || inputNorm.contains(dbNorm)) {
                return cat.getId();
            }
        }

        return null; // No existe
    }

    /**
     * Resuelve el id_origen a partir de un nombre (ej. "Facebook", "WhatsApp", "Web").
     */
    public long obtenerIdOrigenPorNombre(String origen) throws SQLException {
        Long id = buscarIdOrigen(origen);
        return id != null ? id : 3L;
    }

    /**
     * Lista todas las profesiones registradas.
     */
    public List<CatalogoDTO> listarProfesiones() throws SQLException {
        List<CatalogoDTO> lista = new ArrayList<>();
        String sql = "SELECT id_profesion, nombre FROM profesiones ORDER BY id_profesion ASC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new CatalogoDTO(rs.getLong("id_profesion"), rs.getString("nombre")));
            }
            return lista;
        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }
    }

    /**
     * Resuelve el id_profesion de forma inteligente y resiliente a partir de cualquier cadena
     * ingresada por el usuario (ej. "Ingenieria en Sistemas", "Ing. Sistemas", "Sistemas", "Auditoria", "Derecho").
     */
    public Long obtenerIdProfesionPorNombre(String profesion) throws SQLException {
        if (profesion == null || profesion.trim().isEmpty()) return null;
        String inputNorm = normalizeText(profesion);
        if (inputNorm.isEmpty()) return null;

        List<CatalogoDTO> todas = listarProfesiones();

        // 1. Coincidencia directa o contenida normalizada
        for (CatalogoDTO cat : todas) {
            String dbNorm = normalizeText(cat.getNombre());
            if (dbNorm.equals(inputNorm) || dbNorm.contains(inputNorm) || inputNorm.contains(dbNorm)) {
                return cat.getId();
            }
        }

        // 2. Coincidencia por palabras clave representativas (ignorando conectores y abreviaturas comunes)
        String[] tokens = inputNorm.split(" ");
        for (CatalogoDTO cat : todas) {
            String dbNorm = normalizeText(cat.getNombre());
            for (String token : tokens) {
                if (token.equals("de") || token.equals("en") || token.equals("la") ||
                    token.equals("el") || token.equals("y") || token.equals("ing") ||
                    token.equals("lic") || token.length() < 3) {
                    continue;
                }
                if (dbNorm.contains(token)) {
                    return cat.getId();
                }
            }
        }

        // 3. Mapeo de terminos genericos o sinonimos comunes
        if (inputNorm.contains("abogad")) return 6L; // Derecho
        if (inputNorm.contains("medic") || inputNorm.contains("doctor")) return 7L; // Medicina
        if (inputNorm.contains("profesor") || inputNorm.contains("docente") || inputNorm.contains("maestr")) return 8L; // Educacion
        if (inputNorm.contains("colegio")) return 10L; // Bachiller
        if (inputNorm.contains("ingenier")) return 1L; // Por defecto ingenieria si no se especifico otra rama

        return null;
    }

    private static LeadDTO mapLead(ResultSet rs) throws SQLException {
        Long idVend = rs.getObject("id_vendedor") != null ? rs.getLong("id_vendedor") : null;
        Long idProf = rs.getObject("id_profesion") != null ? rs.getLong("id_profesion") : null;

        return new LeadDTO(
            rs.getLong("id_lead"),
            rs.getString("nombre"),
            rs.getString("telefono"),
            rs.getString("correo"),
            rs.getLong("id_origen"),
            rs.getString("origen_nombre"),
            rs.getLong("id_etapa"),
            rs.getString("etapa_nombre"),
            idVend,
            rs.getString("vendedor_nombre"),
            idProf,
            rs.getString("profesion_nombre"),
            rs.getString("created_at")
        );
    }

    // =========================================================================
    // CATÁLOGO INSTITUCIONAL DE OFERTA ACADÉMICA
    // =========================================================================

    public static class OfertaItem {
        private final long id;
        private final String tipo; // "PROGRAMA" o "MODULO"
        private final String codigo;
        private final String nombre;
        private final String duracion;
        private final double costoBs;

        public OfertaItem(long id, String tipo, String codigo, String nombre, String duracion, double costoBs) {
            this.id = id;
            this.tipo = tipo;
            this.codigo = codigo;
            this.nombre = nombre;
            this.duracion = duracion;
            this.costoBs = costoBs;
        }

        public long getId() { return id; }
        public String getTipo() { return tipo; }
        public String getCodigo() { return codigo; }
        public String getNombre() { return nombre; }
        public String getDuracion() { return duracion; }
        public double getCostoBs() { return costoBs; }
    }

    private static final List<OfertaItem> CATALOGO_OFERTA = new ArrayList<>();
    static {
        // Programas
        CATALOGO_OFERTA.add(new OfertaItem(101, "PROGRAMA", "PROG-101", "Diplomado en Ciberseguridad Defensiva y Peritaje Forense", "6 meses", 4500.00));
        CATALOGO_OFERTA.add(new OfertaItem(102, "PROGRAMA", "PROG-102", "Maestria en Gestion Estrategica de Tecnologias de Informacion", "18 meses", 14000.00));
        CATALOGO_OFERTA.add(new OfertaItem(103, "PROGRAMA", "PROG-103", "Diplomado en Inteligencia Artificial Aplicada y Machine Learning", "6 meses", 4800.00));
        CATALOGO_OFERTA.add(new OfertaItem(104, "PROGRAMA", "PROG-104", "Diplomado en Desarrollo Full Stack y Cloud Architecture", "6 meses", 4200.00));
        // Modulos
        CATALOGO_OFERTA.add(new OfertaItem(201, "MODULO", "MOD-201", "Hacking Etico y Auditoria Ofensiva de Sistemas", "4 semanas", 1200.00));
        CATALOGO_OFERTA.add(new OfertaItem(202, "MODULO", "MOD-202", "Seguridad en Redes y Respuesta ante Incidentes", "4 semanas", 1200.00));
        CATALOGO_OFERTA.add(new OfertaItem(203, "MODULO", "MOD-203", "Arquitectura Cloud Empresarial en AWS y Google Cloud", "4 semanas", 1300.00));
        CATALOGO_OFERTA.add(new OfertaItem(204, "MODULO", "MOD-204", "Modelos Predictivos y Deep Learning con Python", "4 semanas", 1350.00));
    }

    public static List<OfertaItem> getCatalogoOferta() {
        return Collections.unmodifiableList(CATALOGO_OFERTA);
    }

    public static OfertaItem buscarOferta(String tipo, long id) {
        if (tipo == null) return null;
        for (OfertaItem item : CATALOGO_OFERTA) {
            if (item.getTipo().equalsIgnoreCase(tipo.trim()) && item.getId() == id) {
                return item;
            }
        }
        return null;
    }

    public static String resumenOferta(String tipo) {
        StringBuilder sb = new StringBuilder();
        for (OfertaItem item : CATALOGO_OFERTA) {
            if (tipo == null || item.getTipo().equalsIgnoreCase(tipo.trim())) {
                if (sb.length() > 0) sb.append(", ");
                sb.append("#").append(item.getId()).append(" '").append(item.getNombre()).append("'");
            }
        }
        return sb.toString();
    }
}
