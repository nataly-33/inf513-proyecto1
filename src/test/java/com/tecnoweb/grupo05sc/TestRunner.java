package com.tecnoweb.grupo05sc;

import com.tecnoweb.grupo05sc.command.CommandDispatcher;
import com.tecnoweb.grupo05sc.command.CommandParser;
import com.tecnoweb.grupo05sc.command.CommandResult;
import com.tecnoweb.grupo05sc.command.ParsedCommand;
import com.tecnoweb.grupo05sc.config.DatabaseConnection;
import com.tecnoweb.grupo05sc.dao.UsuarioDao;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class TestRunner {
    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("  INICIANDO BATERÍA DE PRUEBAS AUTOMATIZADAS – SISTEMA DE CORREOS        ");
        System.out.println("  INF513 TECNOLOGÍA WEB – GRUPO 05 SC                                     ");
        System.out.println("==========================================================================\n");

        int testsPassed = 0;
        int totalTests = 0;

        CommandDispatcher dispatcher = new CommandDispatcher();
        UsuarioDao usuarioDao = new UsuarioDao();

        // Limpieza previa de usuario de prueba si existiera
        limpiarUsuarioPrueba("mariana.suarez@crm.bo");
        limpiarUsuarioPrueba("mariana.modificada@crm.bo");

        // -------------------------------------------------------------
        // TEST 1: CommandParser con LISPER["*"]
        // -------------------------------------------------------------
        totalTests++;
        try {
            ParsedCommand cmd = CommandParser.parse("LISPER[\"*\"]");
            if (cmd.getName().equals("LISPER") && cmd.paramCount() == 1 && cmd.getParam(0).equals("*")) {
                System.out.println("✔ TEST 1 PASÓ: LISPER[\"*\"] parseado correctamente.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 1 FALLÓ: " + cmd);
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 1 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 2: CommandParser con INSPER de 7 parámetros
        // -------------------------------------------------------------
        totalTests++;
        try {
            String insperSubj = "INSPER[\"4715292\",\"Juan Carlos\",\"Perez Seras\",\"Estudiante\",\"33554433\",\"71055123\",\"juanperez@uagrm.edu.bo\"]";
            ParsedCommand cmd = CommandParser.parse(insperSubj);
            if (cmd.getName().equals("INSPER") && cmd.paramCount() == 7 && cmd.getParam(1).equals("Juan Carlos")) {
                System.out.println("✔ TEST 2 PASÓ: INSPER parseado con 7 parámetros y soporte de espacios.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 2 FALLÓ: " + cmd);
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 2 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 3: CommandParser tolerante a COMANDO sin corchetes (HELP)
        // -------------------------------------------------------------
        totalTests++;
        try {
            ParsedCommand cmd = CommandParser.parse("  HELP  ");
            if (cmd.getName().equals("HELP") && cmd.paramCount() == 0) {
                System.out.println("✔ TEST 3 PASÓ: Comando 'HELP' sin corchetes parseado como HELP[].");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 3 FALLÓ: " + cmd);
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 3 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 4: CommandParser validación de Asunto Vacío
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandParser.parse("   ");
            System.err.println("❌ TEST 4 FALLÓ: Debió lanzar CommandException para asunto vacío.");
        } catch (Exception e) {
            System.out.println("✔ TEST 4 PASÓ: Captura error semántico para asunto vacío.");
            testsPassed++;
        }

        // -------------------------------------------------------------
        // TEST 5: CommandDispatcher ejecuta LISPER["*"] sobre PostgreSQL
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("nataly.vane.mm@gmail.com", "LISPER[\"*\"]");
            if (result.isSuccess() && result.getHtmlBody().contains("Listado de Personas Registradas")) {
                System.out.println("✔ TEST 5 PASÓ: LISPER[\"*\"] ejecutado sobre PostgreSQL con tabla HTML generada.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 5 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 5 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 6: CommandDispatcher ejecuta INSPER sobre PostgreSQL
        // -------------------------------------------------------------
        totalTests++;
        try {
            String insperSubj = "INSPER[\"9988776\",\"Carlos Andres\",\"Vaca Diez\",\"Abogado\",\"33112233\",\"77009988\",\"carlos.vaca@uagrm.edu.bo\"]";
            CommandResult result = dispatcher.dispatch("nataly.vane.mm@gmail.com", insperSubj);
            if (result.isSuccess() && result.getHtmlBody().contains("Operación Exitosa")) {
                System.out.println("✔ TEST 6 PASÓ: INSPER ejecutado exitosamente con PreparedStatement.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 6 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 6 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 7: QUIENSOY[] para admin@crm.bo
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("admin@crm.bo", "QUIENSOY[]");
            if (result.isSuccess() && result.getHtmlBody().contains("Administrador")) {
                System.out.println("✔ TEST 7 PASÓ: QUIENSOY autenticó correctamente al Administrador (Nataly).");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 7 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 7 EXCEPCIÓN: " + e.getMessage());
        }

        // =============================================================
        // PRUEBAS ESPECÍFICAS DE CU1: GESTIÓN DE USUARIOS (NATALY)
        // =============================================================

        // -------------------------------------------------------------
        // TEST 8: CU1 - LISTARUSUARIOS[] por Administrador
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("admin@crm.bo", "LISTARUSUARIOS[]");
            if (result.isSuccess() && result.getHtmlBody().contains("Gestión de Usuarios (CU1 – Nataly)") && result.getHtmlBody().contains("admin@crm.bo")) {
                System.out.println("✔ TEST 8 PASÓ: CU1 LISTARUSUARIOS[] ejecutado por Administrador con tabla HTML.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 8 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 8 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 9: CU1 - RBAC: LISTARUSUARIOS[] denegado a Vendedor
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("vendedor1@crm.bo", "LISTARUSUARIOS[]");
            if (!result.isSuccess() && result.getHtmlBody().contains("Acceso denegado: Esta acción es exclusiva para usuarios con rol 'Administrador'")) {
                System.out.println("✔ TEST 9 PASÓ: CU1 Control de Acceso (RBAC) bloqueó correctamente a Vendedor.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 9 FALLÓ: Debió denegar acceso. Respuesta: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 9 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 10: CU1 - RBAC: LISTARUSUARIOS[] denegado a remitente desconocido
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("hacker@anonimo.com", "LISTARUSUARIOS[]");
            if (!result.isSuccess() && result.getHtmlBody().contains("Acceso denegado: El correo remitente no corresponde a un usuario registrado")) {
                System.out.println("✔ TEST 10 PASÓ: CU1 Bloqueó correctamente petición de remitente no registrado.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 10 FALLÓ: Debió rechazar. Respuesta: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 10 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 11: CU1 - CREARUSUARIO["Mariana Suarez","mariana.suarez@crm.bo","pass123","Vendedor"]
        // -------------------------------------------------------------
        totalTests++;
        long nuevoIdCreado = -1;
        try {
            CommandResult result = dispatcher.dispatch("admin@crm.bo", "CREARUSUARIO[\"Mariana Suarez\", \"mariana.suarez@crm.bo\", \"pass123\", \"Vendedor\"]");
            if (result.isSuccess() && result.getHtmlBody().contains("Usuario Creado Exitosamente")) {
                UsuarioDao.UsuarioDTO u = usuarioDao.buscarPorIdOCorreo("mariana.suarez@crm.bo");
                if (u != null && u.isActivo() && u.isParticipaRoundRobin()) {
                    nuevoIdCreado = u.getIdUsuario();
                    System.out.println("✔ TEST 11 PASÓ: CU1 CREARUSUARIO dio de alta al usuario #" + nuevoIdCreado + " con Round-Robin activo.");
                    testsPassed++;
                } else {
                    System.err.println("❌ TEST 11 FALLÓ: Usuario no encontrado en BD.");
                }
            } else {
                System.err.println("❌ TEST 11 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 11 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 12: CU1 - Rechazo de CREARUSUARIO por correo duplicado
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("admin@crm.bo", "CREARUSUARIO[\"Mariana Clon\", \"mariana.suarez@crm.bo\", \"pass123\", \"Vendedor\"]");
            if (!result.isSuccess() && result.getHtmlBody().contains("ya se encuentra registrado para otra cuenta")) {
                System.out.println("✔ TEST 12 PASÓ: CU1 Bloqueó duplicidad de correo con error semántico explicativo.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 12 FALLÓ: Debió rechazar correo duplicado: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 12 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 13: CU1 - BUSCARUSUARIO por correo
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("admin@crm.bo", "BUSCARUSUARIO[\"mariana.suarez@crm.bo\"]");
            if (result.isSuccess() && result.getHtmlBody().contains("Mariana Suarez") && result.getHtmlBody().contains("Cuenta Activa")) {
                System.out.println("✔ TEST 13 PASÓ: CU1 BUSCARUSUARIO encontró y formateó la ficha del usuario.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 13 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 13 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 14: CU1 - MODIFICARUSUARIO
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (nuevoIdCreado > 0) {
                String cmdMod = "MODIFICARUSUARIO[\"" + nuevoIdCreado + "\", \"Mariana Modificada\", \"mariana.modificada@crm.bo\", \"Vendedor\"]";
                CommandResult result = dispatcher.dispatch("admin@crm.bo", cmdMod);
                if (result.isSuccess() && result.getHtmlBody().contains("Usuario Modificado")) {
                    UsuarioDao.UsuarioDTO u = usuarioDao.buscarPorId(nuevoIdCreado);
                    if (u != null && u.getNombreCompleto().equals("Mariana Modificada") && u.getCorreo().equals("mariana.modificada@crm.bo")) {
                        System.out.println("✔ TEST 14 PASÓ: CU1 MODIFICARUSUARIO actualizó datos y correo en PostgreSQL.");
                        testsPassed++;
                    } else {
                        System.err.println("❌ TEST 14 FALLÓ: Datos no actualizados en BD.");
                    }
                } else {
                    System.err.println("❌ TEST 14 FALLÓ: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 14 OMITIDO: Falta nuevoIdCreado.");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 14 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 15: CU1 - REGLA DE ORO DE SEGURIDAD: Protección anti-autobloqueo
        // El Administrador (admin@crm.bo, id=1) intenta desactivarse a sí mismo
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("admin@crm.bo", "TOGGLEUSUARIO[\"1\"]");
            if (!result.isSuccess() && result.getHtmlBody().contains("El Administrador no puede desactivar ni bloquear su propia cuenta")) {
                System.out.println("✔ TEST 15 PASÓ: CU1 Regla de Oro demostrada -> Bloqueó el autobloqueo del Administrador.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 15 FALLÓ: Debió bloquear la auto-desactivación: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 15 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 16: CU1 - TOGGLEUSUARIO a otro usuario (Desactivar y Reactivar)
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (nuevoIdCreado > 0) {
                // Desactivar
                CommandResult resDesact = dispatcher.dispatch("admin@crm.bo", "TOGGLEUSUARIO[\"" + nuevoIdCreado + "\"]");
                UsuarioDao.UsuarioDTO u1 = usuarioDao.buscarPorId(nuevoIdCreado);
                boolean quedoInactivo = (u1 != null && !u1.isActivo());

                // Reactivar
                CommandResult resAct = dispatcher.dispatch("admin@crm.bo", "TOGGLEUSUARIO[\"" + nuevoIdCreado + "\"]");
                UsuarioDao.UsuarioDTO u2 = usuarioDao.buscarPorId(nuevoIdCreado);
                boolean quedoActivo = (u2 != null && u2.isActivo());

                if (resDesact.isSuccess() && quedoInactivo && resAct.isSuccess() && quedoActivo) {
                    System.out.println("✔ TEST 16 PASÓ: CU1 TOGGLEUSUARIO alternó exitosamente de Activo a Inactivo y de regreso.");
                    testsPassed++;
                } else {
                    System.err.println("❌ TEST 16 FALLÓ en alternar estados.");
                }
            } else {
                System.err.println("❌ TEST 16 OMITIDO: Falta nuevoIdCreado.");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 16 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 17: CU1 - TOGGLEROUNDROBIN para Vendedor
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (nuevoIdCreado > 0) {
                CommandResult result = dispatcher.dispatch("admin@crm.bo", "TOGGLEROUNDROBIN[\"" + nuevoIdCreado + "\"]");
                UsuarioDao.UsuarioDTO u = usuarioDao.buscarPorId(nuevoIdCreado);
                if (result.isSuccess() && u != null && !u.isParticipaRoundRobin()) {
                    System.out.println("✔ TEST 17 PASÓ: CU1 TOGGLEROUNDROBIN excluyó al vendedor de la asignación rotativa.");
                    testsPassed++;
                } else {
                    System.err.println("❌ TEST 17 FALLÓ: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 17 OMITIDO: Falta nuevoIdCreado.");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 17 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 18: CU1 - TOGGLEROUNDROBIN rechazado para no-vendedor (Coordinador id=2)
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("admin@crm.bo", "TOGGLEROUNDROBIN[\"2\"]");
            if (!result.isSuccess() && result.getHtmlBody().contains("solo es aplicable a usuarios con rol 'Vendedor'")) {
                System.out.println("✔ TEST 18 PASÓ: CU1 Validó regla de negocio -> Round-Robin solo aplica a Vendedores.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 18 FALLÓ: Debió rechazar round-robin a Coordinador: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 18 EXCEPCIÓN: " + e.getMessage());
        }

        // Limpieza final de prueba
        limpiarUsuarioPrueba("mariana.suarez@crm.bo");
        limpiarUsuarioPrueba("mariana.modificada@crm.bo");

        System.out.println("\n==========================================================================");
        System.out.println("  RESUMEN FINAL: " + testsPassed + " de " + totalTests + " pruebas pasaron con éxito (" + (testsPassed * 100 / totalTests) + "%).");
        System.out.println("==========================================================================\n");
    }

    private static void limpiarUsuarioPrueba(String correo) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM usuarios WHERE LOWER(correo) = LOWER(?)")) {
            ps.setString(1, correo);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }
}
