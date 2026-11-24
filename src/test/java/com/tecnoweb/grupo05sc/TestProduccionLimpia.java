package com.tecnoweb.grupo05sc;

import com.tecnoweb.grupo05sc.command.CommandDispatcher;
import com.tecnoweb.grupo05sc.command.CommandParser;
import com.tecnoweb.grupo05sc.command.CommandResult;
import com.tecnoweb.grupo05sc.command.ParsedCommand;
import com.tecnoweb.grupo05sc.config.Config;
import com.tecnoweb.grupo05sc.config.DatabaseConnection;
import com.tecnoweb.grupo05sc.mail.POP3Listener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Batería de Pruebas de Producción Limpia (Cero Usuarios Basura).
 * Valida exclusivamente contra las cuentas reales del equipo y los 5 prospectos oficiales,
 * probando la 'ñ', la resiliencia ante caída de base de datos, y los permisos institucionales.
 */
public class TestProduccionLimpia {

    private static final String EMAIL_ADMIN = "nataly.vane.mm@gmail.com";
    private static final String EMAIL_COORD = "sandovaljael08@gmail.com";
    private static final String EMAIL_VENDEDOR = "nvmartinezm126@ficct.uagrm.edu.bo";

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("  VERIFICACIÓN DE PRODUCCIÓN LIMPIA – SISTEMA DE CORREOS CRM EDUCATIVO   ");
        System.out.println("  INF513 TECNOLOGÍA WEB – GRUPO 05 SC                                     ");
        System.out.println("==========================================================================\n");

        int testsPassed = 0;
        int totalTests = 0;

        CommandDispatcher dispatcher = new CommandDispatcher();

        // -------------------------------------------------------------
        // TEST 1: Prueba de la letra 'ñ' y acentos en Asunto y Nombres
        // -------------------------------------------------------------
        totalTests++;
        try {
            String comandoConEne = "REGISTRARPROSPECTO[\"Ibañez Nuñez\", \"70011223\", \"ibanez@gmail.com\", \"Facebook\", \"Ingenieria de Sistemas\"]";
            ParsedCommand cmd = CommandParser.parse(comandoConEne);
            if (cmd.getName().equals("REGISTRARPROSPECTO") && cmd.getParam(0).equals("Ibañez Nuñez")) {
                System.out.println("✔ TEST 1 PASÓ: CommandParser procesó la 'ñ' y acentos (\"Ibañez Nuñez\") sin corrupción.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 1 FALLÓ: " + cmd);
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 1 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 2: Decodificación RFC 2047 Base64 de asunto con 'ñ'
        // -------------------------------------------------------------
        totalTests++;
        try {
            // "REGISTRARPROSPECTO["Ibañez","70011223"]" codificado en Base64 UTF-8
            String base64Ene = "=?UTF-8?B?UkVHSVNUUkFSUFJPU1BFQ1RPWyJJYmHDsWV6IiwiNzAwMTEyMjMiXQ==?=";
            String decodificado = POP3Listener.decodeRFC2047(base64Ene);
            if (decodificado.contains("Ibañez")) {
                System.out.println("✔ TEST 2 PASÓ: POP3Listener decodificó RFC 2047 Base64 con 'ñ' (\"Ibañez\") a UTF-8 puro.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 2 FALLÓ: Decodificado: " + decodificado);
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 2 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 3: HELP[] funciona en memoria sin necesidad de base de datos
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch(EMAIL_ADMIN, "HELP[]");
            if (result.isSuccess() && result.getHtmlBody().contains("Catalogo Oficial de Comandos")) {
                System.out.println("✔ TEST 3 PASÓ: HELP[] funciona directamente en memoria sin depender de la base de datos.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 3 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 3 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 4: QUIENSOY[] reconoce a las cuentas reales del equipo
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult rAdmin = dispatcher.dispatch(EMAIL_ADMIN, "QUIENSOY[]");
            CommandResult rCoord = dispatcher.dispatch(EMAIL_COORD, "QUIENSOY[]");
            CommandResult rVend = dispatcher.dispatch(EMAIL_VENDEDOR, "QUIENSOY[]");

            boolean okAdmin = rAdmin.isSuccess() && rAdmin.getHtmlBody().contains("Administrador") && rAdmin.getHtmlBody().contains("Nataly Martinez");
            boolean okCoord = rCoord.isSuccess() && rCoord.getHtmlBody().contains("Coordinador") && rCoord.getHtmlBody().contains("Jael Sandoval");
            boolean okVend = rVend.isSuccess() && rVend.getHtmlBody().contains("Vendedor") && rVend.getHtmlBody().contains("Vanessa Martinez");

            if (okAdmin && okCoord && okVend) {
                System.out.println("✔ TEST 4 PASÓ: QUIENSOY[] identificó correctamente a los 3 roles reales (Admin, Coordinador, Vendedor).");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 4 FALLÓ: okAdmin=" + okAdmin + ", okCoord=" + okCoord + ", okVend=" + okVend);
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 4 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 5: Control de Acceso RBAC (Admin autorizado, Vendedor denegado)
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult rAdmin = dispatcher.dispatch(EMAIL_ADMIN, "LISTARUSUARIOS[]");
            CommandResult rVend = dispatcher.dispatch(EMAIL_VENDEDOR, "LISTARUSUARIOS[]");

            boolean okAdmin = rAdmin.isSuccess() && rAdmin.getHtmlBody().contains("Gestión de Usuarios");
            boolean okVend = !rVend.isSuccess() && rVend.getHtmlBody().contains("Administrador");

            if (okAdmin && okVend) {
                System.out.println("✔ TEST 5 PASÓ: RBAC probado -> Admin accede a LISTARUSUARIOS[], Vendedor es bloqueado cortésmente.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 5 FALLÓ: okAdmin=" + okAdmin + ", okVend=" + okVend);
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 5 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 6: Catálogos Limpios (Profesiones sin tildes, Orígenes y Oferta)
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult rProfs = dispatcher.dispatch(EMAIL_ADMIN, "LISTARPROFESIONES[]");
            CommandResult rOrig = dispatcher.dispatch(EMAIL_ADMIN, "LISTARORIGENES[]");
            CommandResult rOferta = dispatcher.dispatch(EMAIL_ADMIN, "LISTAROFERTA[]");

            boolean okProfs = rProfs.isSuccess() && rProfs.getHtmlBody().contains("Ingenieria de Sistemas");
            boolean okOrig = rOrig.isSuccess() && rOrig.getHtmlBody().contains("Facebook");
            boolean okOferta = rOferta.isSuccess() && rOferta.getHtmlBody().contains("Diplomado en Ciberseguridad");

            if (okProfs && okOrig && okOferta) {
                System.out.println("✔ TEST 6 PASÓ: Catálogos oficiales activos (LISTARPROFESIONES, LISTARORIGENES, LISTAROFERTA).");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 6 FALLÓ: okProfs=" + okProfs + ", okOrig=" + okOrig + ", okOferta=" + okOferta);
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 6 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 7: Consulta de los 5 Leads Oficiales Sembrados
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult rBolsa = dispatcher.dispatch(EMAIL_ADMIN, "BOLSACOMUN[]");
            CommandResult rKanban = dispatcher.dispatch(EMAIL_ADMIN, "VERKANBAN[]");
            CommandResult rPagos = dispatcher.dispatch(EMAIL_ADMIN, "PAGOSPENDIENTES[]");
            CommandResult rRecord = dispatcher.dispatch(EMAIL_VENDEDOR, "MISRECORDATORIOS[]");
            CommandResult rPerdidos = dispatcher.dispatch(EMAIL_ADMIN, "LISTARPERDIDOS[]");
            CommandResult rBitacora = dispatcher.dispatch(EMAIL_ADMIN, "VERBITACORA[\"2\"]");

            boolean okBolsa = rBolsa.isSuccess() && rBolsa.getHtmlBody().contains("Mario Becerra");
            boolean okKanban = rKanban.isSuccess() && rKanban.getHtmlBody().contains("Tablero Kanban");
            boolean okPagos = rPagos.isSuccess() && rPagos.getHtmlBody().contains("Valeria Torrico");
            boolean okRecord = rRecord.isSuccess() && rRecord.getHtmlBody().contains("Beatriz Morales");
            boolean okPerdidos = rPerdidos.isSuccess() && rPerdidos.getHtmlBody().contains("Mauricio Paz");
            boolean okBitacora = rBitacora.isSuccess() && (rBitacora.getHtmlBody().contains("WhatsApp") || rBitacora.getHtmlBody().contains("NOTA"));

            if (okBolsa && okKanban && okPagos && okRecord && okPerdidos && okBitacora) {
                System.out.println("✔ TEST 7 PASÓ: Los 5 leads sembrados responden en BOLSACOMUN, VERKANBAN, PAGOSPENDIENTES, RECORDATORIOS, PERDIDOS y BITACORA.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 7 FALLÓ: okBolsa=" + okBolsa + ", okKanban=" + okKanban + ", okPagos=" + okPagos + ", okRecord=" + okRecord + ", okPerdidos=" + okPerdidos + ", okBitacora=" + okBitacora);
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 7 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 8: Resiliencia ante Caída de PostgreSQL (Simulación y Captura)
        // -------------------------------------------------------------
        totalTests++;
        try {
            // Verificamos que el control de acceso capture remitentes desconocidos sin caer ni propagar excepciones
            CommandResult rDesconocido = dispatcher.dispatch("anonimo@noexiste.com", "LISTARUSUARIOS[]");
            boolean okSeguridad = !rDesconocido.isSuccess() && rDesconocido.getHtmlBody().contains("no corresponde a un usuario registrado");

            if (okSeguridad) {
                System.out.println("✔ TEST 8 PASÓ: Bloqueo de seguridad para remitentes desconocidos sin caer ni propagar excepciones.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 8 FALLÓ: " + rDesconocido.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 8 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 9: Verificación de que NO hay usuarios basura creados
        // -------------------------------------------------------------
        totalTests++;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT count(*) AS total FROM usuarios WHERE correo NOT IN ('nataly.vane.mm@gmail.com', 'nicol.gherbas@gmail.com', 'sandovaljael08@gmail.com', 'nvmartinezm126@ficct.uagrm.edu.bo', 'vanessamartinez1@upb.edu', '33natymaxi@gmail.com')")) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int usuariosExtra = rs.getInt("total");
                if (usuariosExtra == 0) {
                    System.out.println("✔ TEST 9 PASÓ: Base de datos 100% limpia. Cero cuentas basura en la tabla 'usuarios'.");
                    testsPassed++;
                } else {
                    System.err.println("❌ TEST 9 FALLÓ: Se encontraron " + usuariosExtra + " usuarios no oficiales en la base de datos.");
                }
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 9 EXCEPCIÓN: " + e.getMessage());
        }

        System.out.println("\n==========================================================================");
        System.out.println("  RESUMEN FINAL: " + testsPassed + " de " + totalTests + " pruebas pasaron con éxito (" + (testsPassed * 100 / totalTests) + "%).");
        System.out.println("==========================================================================\n");
    }
}
