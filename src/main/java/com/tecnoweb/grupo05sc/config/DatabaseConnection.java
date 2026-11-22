package com.tecnoweb.grupo05sc.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Gestiona la conexión JDBC transaccional hacia PostgreSQL.
 * Implementa cierre seguro de recursos en bloques finally (Doctrina de la materia).
 */
public final class DatabaseConnection {

    static {
        try {
            Class.forName(Config.DB_DRIVER);
        } catch (ClassNotFoundException e) {
            System.err.println("[ERROR CRÍTICO] Driver JDBC de PostgreSQL no encontrado: " + e.getMessage());
        }
    }

    private static volatile boolean estuveCaido = false;

    private DatabaseConnection() {}

    /**
     * Obtiene una nueva conexión lista para operaciones de base de datos.
     * Si la base de datos estuvo caída y vuelve a responder, notifica la reconexión automática.
     */
    public static Connection getConnection() throws SQLException {
        try {
            Connection conn = DriverManager.getConnection(Config.DB_URL, Config.DB_USER, Config.DB_PASS);
            if (estuveCaido) {
                System.out.println(">>> [DB RESTABLECIDA] Conexión a PostgreSQL recuperada exitosamente en " + Config.DB_HOST + ":" + Config.DB_PORT);
                estuveCaido = false;
            }
            return conn;
        } catch (SQLException e) {
            estuveCaido = true;
            throw e;
        }
    }

    /**
     * Cierre seguro de ResultSet sin arrojar excepciones en finally.
     */
    public static void close(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException ignored) {}
        }
    }

    /**
     * Cierre seguro de PreparedStatement sin arrojar excepciones en finally.
     */
    public static void close(PreparedStatement ps) {
        if (ps != null) {
            try {
                ps.close();
            } catch (SQLException ignored) {}
        }
    }

    /**
     * Cierre seguro de Connection sin arrojar excepciones en finally.
     */
    public static void close(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException ignored) {}
        }
    }

    /**
     * Rollback seguro en caso de fallos transaccionales.
     */
    public static void rollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {}
        }
    }
}
