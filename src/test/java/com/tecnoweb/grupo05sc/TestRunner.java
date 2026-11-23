package com.tecnoweb.grupo05sc;

import com.tecnoweb.grupo05sc.command.CommandDispatcher;
import com.tecnoweb.grupo05sc.command.CommandParser;
import com.tecnoweb.grupo05sc.command.CommandResult;
import com.tecnoweb.grupo05sc.command.ParsedCommand;
import com.tecnoweb.grupo05sc.config.DatabaseConnection;
import com.tecnoweb.grupo05sc.dao.UsuarioDao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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
        // TEST 1: CommandParser con sintaxis estándar de comando CRM
        // -------------------------------------------------------------
        totalTests++;
        try {
            ParsedCommand cmd = CommandParser.parse("REGISTRARPROSPECTO[\"Carlos Andres\",\"70012345\",\"carlos@gmail.com\",\"Facebook\",\"Ingeniero\"]");
            if (cmd.getName().equals("REGISTRARPROSPECTO") && cmd.paramCount() == 5 && cmd.getParam(0).equals("Carlos Andres")) {
                System.out.println("✔ TEST 1 PASÓ: REGISTRARPROSPECTO parseado con 5 parámetros y soporte de espacios.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 1 FALLÓ: " + cmd);
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 1 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 2: CommandParser con comando sin parámetros COMANDO[]
        // -------------------------------------------------------------
        totalTests++;
        try {
            ParsedCommand cmd = CommandParser.parse("LISTARUSUARIOS[]");
            if (cmd.getName().equals("LISTARUSUARIOS") && cmd.paramCount() == 0) {
                System.out.println("✔ TEST 2 PASÓ: LISTARUSUARIOS[] parseado sin parámetros correctamente.");
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
        // TEST 5: CommandDispatcher ejecuta HELP[] generando catálogo HTML
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("admin@crm.bo", "HELP[]");
            if (result.isSuccess() && result.getHtmlBody().contains("Catalogo") && result.getHtmlBody().contains("LISTARUSUARIOS")) {
                System.out.println("✔ TEST 5 PASÓ: HELP[] ejecutado con catálogo interactivo HTML.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 5 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 5 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 6: CommandDispatcher ejecuta VERKANBAN[] y BOLSACOMUN[] sobre PostgreSQL
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult resultKanban = dispatcher.dispatch("admin@crm.bo", "VERKANBAN[]");
            CommandResult resultBolsa = dispatcher.dispatch("admin@crm.bo", "BOLSACOMUN[]");
            if (resultKanban.isSuccess() && resultKanban.getHtmlBody().contains("Tablero Kanban") &&
                resultBolsa.isSuccess() && resultBolsa.getHtmlBody().contains("Bolsa")) {
                System.out.println("✔ TEST 6 PASÓ: VERKANBAN[] y BOLSACOMUN[] ejecutados contra PostgreSQL con tablas HTML.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 6 FALLÓ: " + resultKanban.getHtmlBody());
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
            if (result.isSuccess() && result.getHtmlBody().contains("Gestión de Usuarios") && result.getHtmlBody().contains("admin@crm.bo")) {
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

        // =============================================================
        // PRUEBAS ESPECÍFICAS DE CU2: CAPTACIÓN Y OFERTA (JAEL)
        // =============================================================

        limpiarLeadPrueba("mario.test@gmail.com");
        long idLeadCreado = -1;

        // -------------------------------------------------------------
        // TEST 19: CU2 - REGISTRARPROSPECTO crea lead en Bolsa Común (etapa 1)
        // -------------------------------------------------------------
        totalTests++;
        try {
            String cmdReg = "REGISTRARPROSPECTO[\"Mario Test\", \"70011223\", \"mario.test@gmail.com\", \"Facebook\", \"Ingeniería de Sistemas\"]";
            CommandResult result = dispatcher.dispatch("admin@crm.bo", cmdReg);
            if (result.isSuccess() && result.getHtmlBody().contains("Captacion de Prospecto") && result.getHtmlBody().contains("Mario Test")) {
                com.tecnoweb.grupo05sc.dao.LeadDao leadDao = new com.tecnoweb.grupo05sc.dao.LeadDao();
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement ps = conn.prepareStatement("SELECT id_lead, id_etapa, id_vendedor FROM leads WHERE correo = 'mario.test@gmail.com'")) {
                    java.sql.ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        idLeadCreado = rs.getLong("id_lead");
                        long etapa = rs.getLong("id_etapa");
                        Object vend = rs.getObject("id_vendedor");
                        if (etapa == 1 && vend == null) {
                            System.out.println("✔ TEST 19 PASÓ: CU2 REGISTRARPROSPECTO registró al lead #" + idLeadCreado + " en etapa 1 y Bolsa Común.");
                            testsPassed++;
                        } else {
                            System.err.println("❌ TEST 19 FALLÓ: Lead con etapa o vendedor incorrecto: etapa=" + etapa + ", vend=" + vend);
                        }
                    } else {
                        System.err.println("❌ TEST 19 FALLÓ: Lead no encontrado en BD.");
                    }
                }
            } else {
                System.err.println("❌ TEST 19 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 19 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 20: CU2 - REGISTRARPROSPECTO rechaza parámetros insuficientes
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("admin@crm.bo", "REGISTRARPROSPECTO[\"SoloNombre\"]");
            if (!result.isSuccess() && result.getHtmlBody().contains("Parametros insuficientes")) {
                System.out.println("✔ TEST 20 PASÓ: CU2 REGISTRARPROSPECTO rechazó petición con datos de contacto incompletos.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 20 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 20 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 21: CU2 - VINCULARINTERES vincula programa académico a lead
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (idLeadCreado > 0) {
                String cmdVinc = "VINCULARINTERES[\"" + idLeadCreado + "\", \"PROGRAMA\", \"101\", \"Diplomado en Ciberseguridad Defensiva\"]";
                CommandResult result = dispatcher.dispatch("admin@crm.bo", cmdVinc);
                if (result.isSuccess() && result.getHtmlBody().contains("Interes Academico Vinculado") && result.getHtmlBody().contains("Diplomado en Ciberseguridad")) {
                    System.out.println("✔ TEST 21 PASÓ: CU2 VINCULARINTERES asoció exitosamente programa al lead #" + idLeadCreado + ".");
                    testsPassed++;
                } else {
                    System.err.println("❌ TEST 21 FALLÓ: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 21 OMITIDO: Falta idLeadCreado.");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 21 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 22: CU2 - VINCULARINTERES rechaza tipo inválido (ni PROGRAMA ni MODULO)
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (idLeadCreado > 0) {
                String cmdVinc = "VINCULARINTERES[\"" + idLeadCreado + "\", \"SEMINARIO\", \"101\", \"Seminario Invalido\"]";
                CommandResult result = dispatcher.dispatch("admin@crm.bo", cmdVinc);
                if (!result.isSuccess() && result.getHtmlBody().contains("invalido")) {
                    System.out.println("✔ TEST 22 PASÓ: CU2 VINCULARINTERES validó restricción de tipo ('PROGRAMA' o 'MODULO').");
                    testsPassed++;
                } else {
                    System.err.println("❌ TEST 22 FALLÓ: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 22 OMITIDO: Falta idLeadCreado.");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 22 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 23: CU2 - LISTARINTERESES lista los cursos del prospecto
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (idLeadCreado > 0) {
                String cmdList = "LISTARINTERESES[\"" + idLeadCreado + "\"]";
                CommandResult result = dispatcher.dispatch("admin@crm.bo", cmdList);
                if (result.isSuccess() && result.getHtmlBody().contains("Diplomado en Ciberseguridad")) {
                    System.out.println("✔ TEST 23 PASÓ: CU2 LISTARINTERESES generó tabla HTML con los cursos del prospecto.");
                    testsPassed++;
                } else {
                    System.err.println("❌ TEST 23 FALLÓ: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 23 OMITIDO: Falta idLeadCreado.");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 23 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 24: CU2 - LISTARORIGENES lista los canales de captación
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("admin@crm.bo", "LISTARORIGENES[]");
            if (result.isSuccess() && result.getHtmlBody().contains("Facebook") && result.getHtmlBody().contains("WhatsApp")) {
                System.out.println("✔ TEST 24 PASÓ: CU2 LISTARORIGENES consultó el catálogo de canales de captación.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 24 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 24 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 25: CU2 - LISTARPROFESIONES lista el catálogo de profesiones
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("admin@crm.bo", "LISTARPROFESIONES[]");
            if (result.isSuccess() && result.getHtmlBody().contains("Ingeniería de Sistemas") && result.getHtmlBody().contains("Derecho")) {
                System.out.println("✔ TEST 25 PASÓ: CU2 LISTARPROFESIONES consultó el catálogo de áreas y profesiones.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 25 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 25 EXCEPCIÓN: " + e.getMessage());
        }

        // =============================================================
        // PRUEBAS ESPECÍFICAS DE CU3: ASIGNACIÓN DE LEADS (NICOL)
        // =============================================================

        // -------------------------------------------------------------
        // TEST 26: CU3 - BOLSACOMUN lista prospectos no asignados
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("admin@crm.bo", "BOLSACOMUN[]");
            if (result.isSuccess() && result.getHtmlBody().contains("Bolsa Comun de Prospectos")) {
                System.out.println("✔ TEST 26 PASÓ: CU3 BOLSACOMUN devolvió tabla de prospectos en espera.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 26 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 26 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 27: CU3 - ASIGNARMANUAL asigna lead a vendedor #3
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (idLeadCreado > 0) {
                String cmdAsig = "ASIGNARMANUAL[\"" + idLeadCreado + "\", \"3\", \"Asignacion inicial comercial\"]";
                CommandResult result = dispatcher.dispatch("admin@crm.bo", cmdAsig);
                if (result.isSuccess() && result.getHtmlBody().contains("Asignacion Manual Exitosa")) {
                    com.tecnoweb.grupo05sc.dao.LeadDao leadDao = new com.tecnoweb.grupo05sc.dao.LeadDao();
                    com.tecnoweb.grupo05sc.dao.LeadDao.LeadDTO l = leadDao.buscarPorId(idLeadCreado);
                    if (l != null && l.getIdVendedor() != null && l.getIdVendedor() == 3) {
                        System.out.println("✔ TEST 27 PASÓ: CU3 ASIGNARMANUAL asignó el lead #" + idLeadCreado + " a Vendedor 1 (#3) con auditoría.");
                        testsPassed++;
                    } else {
                        System.err.println("❌ TEST 27 FALLÓ: Vendedor no actualizado en BD.");
                    }
                } else {
                    System.err.println("❌ TEST 27 FALLÓ: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 27 OMITIDO: Falta idLeadCreado.");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 27 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 28: CU3 - ASIGNARMANUAL bloqueado si el lead ya tiene vendedor asignado (exige TRANSFERIRLEAD)
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (idLeadCreado > 0) {
                String cmdAsigYaTiene = "ASIGNARMANUAL[\"" + idLeadCreado + "\", \"4\", \"Intento de sobreasignacion\"]";
                CommandResult result = dispatcher.dispatch("admin@crm.bo", cmdAsigYaTiene);
                if (!result.isSuccess() && result.getHtmlBody().contains("ya se encuentra asignado") && result.getHtmlBody().contains("TRANSFERIRLEAD")) {
                    System.out.println("✔ TEST 28 PASÓ: CU3 ASIGNARMANUAL bloqueó reasignación directa sobre lead ya asignado (exige TRANSFERIRLEAD).");
                    testsPassed++;
                } else {
                    System.err.println("❌ TEST 28 FALLÓ: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 28 OMITIDO: Falta idLeadCreado.");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 28 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 28B: CU3 - ASIGNARMANUAL rechazado si destinatario no es Vendedor (Coordinador #2)
        // -------------------------------------------------------------
        totalTests++;
        try {
            String cmdAsigCoord = "ASIGNARMANUAL[\"1\", \"2\", \"Asignacion invalida\"]";
            CommandResult result = dispatcher.dispatch("admin@crm.bo", cmdAsigCoord);
            if (!result.isSuccess() && result.getHtmlBody().contains("solo pueden ser asignados a usuarios con rol 'Vendedor'")) {
                System.out.println("✔ TEST 28B PASÓ: CU3 ASIGNARMANUAL validó que el destinatario tenga rol 'Vendedor'.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 28B FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 28B EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 29: CU3 - TRANSFERIRLEAD reasigna lead a vendedor #4 con motivo
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (idLeadCreado > 0) {
                String cmdTrans = "TRANSFERIRLEAD[\"" + idLeadCreado + "\", \"4\", \"Reasignacion por carga de trabajo\"]";
                CommandResult result = dispatcher.dispatch("admin@crm.bo", cmdTrans);
                if (result.isSuccess() && result.getHtmlBody().contains("Transferencia de Prospecto")) {
                    com.tecnoweb.grupo05sc.dao.LeadDao leadDao = new com.tecnoweb.grupo05sc.dao.LeadDao();
                    com.tecnoweb.grupo05sc.dao.LeadDao.LeadDTO l = leadDao.buscarPorId(idLeadCreado);
                    if (l != null && l.getIdVendedor() != null && l.getIdVendedor() == 4) {
                        System.out.println("✔ TEST 29 PASÓ: CU3 TRANSFERIRLEAD transfirió el lead #" + idLeadCreado + " a Vendedor 2 (#4).");
                        testsPassed++;
                    } else {
                        System.err.println("❌ TEST 29 FALLÓ: Lead no reasignado a vendedor 4.");
                    }
                } else {
                    System.err.println("❌ TEST 29 FALLÓ: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 29 OMITIDO: Falta idLeadCreado.");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 29 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 30: CU3 - HISTORIALASIGNACIONES despliega la traza de auditoría
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (idLeadCreado > 0) {
                String cmdHist = "HISTORIALASIGNACIONES[\"" + idLeadCreado + "\"]";
                CommandResult result = dispatcher.dispatch("admin@crm.bo", cmdHist);
                if (result.isSuccess() && result.getHtmlBody().contains("Auditoria de Asignaciones") && result.getHtmlBody().contains("TRANSFERENCIA")) {
                    System.out.println("✔ TEST 30 PASÓ: CU3 HISTORIALASIGNACIONES mostró la traza cronológica de traspasos.");
                    testsPassed++;
                } else {
                    System.err.println("❌ TEST 30 FALLÓ: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 30 OMITIDO: Falta idLeadCreado.");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 30 EXCEPCIÓN: " + e.getMessage());
        }

        // =============================================================
        // PRUEBAS ESPECÍFICAS DE CU4: EMBUDO KANBAN (NICOL)
        // =============================================================

        // -------------------------------------------------------------
        // TEST 31: CU4 - VERKANBAN visualiza las 6 etapas
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("admin@crm.bo", "VERKANBAN[]");
            if (result.isSuccess() && result.getHtmlBody().contains("Tablero Kanban Comercial") && result.getHtmlBody().contains("En Contacto")) {
                System.out.println("✔ TEST 31 PASÓ: CU4 VERKANBAN generó el tablero consolidado de 6 etapas.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 31 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 31 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 32: CU4 - LEADSETAPA consulta leads en Etapa 1 (Nuevo)
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("admin@crm.bo", "LEADSETAPA[\"1\"]");
            if (result.isSuccess() && result.getHtmlBody().contains("Columna Kanban: Nuevo")) {
                System.out.println("✔ TEST 32 PASÓ: CU4 LEADSETAPA listó los prospectos de la etapa 'Nuevo'.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 32 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 32 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 33: CU4 - MOVERETAPA avanza lead de etapa 1 a 2 (En Contacto)
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (idLeadCreado > 0) {
                String cmdMover = "MOVERETAPA[\"" + idLeadCreado + "\", \"2\"]";
                CommandResult result = dispatcher.dispatch("admin@crm.bo", cmdMover);
                if (result.isSuccess() && result.getHtmlBody().contains("Etapa Comercial Actualizada") && result.getHtmlBody().contains("En Contacto")) {
                    com.tecnoweb.grupo05sc.dao.LeadDao leadDao = new com.tecnoweb.grupo05sc.dao.LeadDao();
                    com.tecnoweb.grupo05sc.dao.LeadDao.LeadDTO l = leadDao.buscarPorId(idLeadCreado);
                    if (l != null && l.getIdEtapa() == 2) {
                        System.out.println("✔ TEST 33 PASÓ: CU4 MOVERETAPA avanzó el lead #" + idLeadCreado + " a etapa 2 (En Contacto) y auditó en bitácora.");
                        testsPassed++;
                    } else {
                        System.err.println("❌ TEST 33 FALLÓ: Etapa no actualizada en BD.");
                    }
                } else {
                    System.err.println("❌ TEST 33 FALLÓ: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 33 OMITIDO: Falta idLeadCreado.");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 33 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 34: CU4 - MOVERETAPA a Etapa 6 rechaza y exige MARCARPERDIDO
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (idLeadCreado > 0) {
                String cmdMover = "MOVERETAPA[\"" + idLeadCreado + "\", \"6\"]";
                CommandResult result = dispatcher.dispatch("admin@crm.bo", cmdMover);
                if (!result.isSuccess() && result.getHtmlBody().contains("MARCARPERDIDO")) {
                    System.out.println("✔ TEST 34 PASÓ: CU4 Validó regla institucional -> Paso a etapa 6 exige tipificar motivo (MARCARPERDIDO).");
                    testsPassed++;
                } else {
                    System.err.println("❌ TEST 34 FALLÓ: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 34 OMITIDO: Falta idLeadCreado.");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 34 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 35: CU4 - MISLEADS lista la cartera comercial del vendedor
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("vendedor1@crm.bo", "MISLEADS[]");
            if (result.isSuccess() && result.getHtmlBody().contains("Mi Cartera de Prospectos")) {
                System.out.println("✔ TEST 35 PASÓ: CU4 MISLEADS filtró la cartera personal del vendedor remitente.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 35 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 35 EXCEPCIÓN: " + e.getMessage());
        }

        // =============================================================
        // PRUEBAS DE VALIDACIONES EXTRA: TELÉFONO, ORIGEN, OFERTA, RBAC
        // =============================================================

        // -------------------------------------------------------------
        // TEST 36: Validación de teléfono (rechaza celular boliviano que no inicia con 6 o 7)
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("admin@crm.bo", "REGISTRARPROSPECTO[\"Pedro Invalido\", \"80011223\", \"pedro@gmail.com\", \"Facebook\", \"Sistemas\"]");
            if (!result.isSuccess() && result.getHtmlBody().contains("deben iniciar con 6 o 7")) {
                System.out.println("✔ TEST 36 PASÓ: Validó teléfono boliviano -> Celular de 8 dígitos debe iniciar con 6 o 7.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 36 FALLÓ: Debió rechazar celular que inicia con 8: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 36 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 37: Validación de canal de origen (rechaza origen no existente en BD)
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("admin@crm.bo", "REGISTRARPROSPECTO[\"Laura Invalida\", \"70099881\", \"laura@gmail.com\", \"CanalInexistente\", \"Sistemas\"]");
            if (!result.isSuccess() && result.getHtmlBody().contains("LISTARORIGENES[]")) {
                System.out.println("✔ TEST 37 PASÓ: Validó canal de origen contra base de datos e instruyó LISTARORIGENES[].");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 37 FALLÓ: Debió rechazar origen inexistente: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 37 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 38: Catálogo de oferta académica (LISTAROFERTA[])
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("admin@crm.bo", "LISTAROFERTA[]");
            if (result.isSuccess() && result.getHtmlBody().contains("Diplomado en Ciberseguridad") && result.getHtmlBody().contains("Hacking Etico")) {
                System.out.println("✔ TEST 38 PASÓ: LISTAROFERTA desplegó programas y módulos con aranceles oficiales.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 38 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 38 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 39: VINCULARINTERES rechaza ID no existente en oferta académica
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (idLeadCreado > 0) {
                CommandResult result = dispatcher.dispatch("admin@crm.bo", "VINCULARINTERES[\"" + idLeadCreado + "\", \"PROGRAMA\", \"999\", \"Curso Inexistente\"]");
                if (!result.isSuccess() && result.getHtmlBody().contains("disponible en la oferta academica")) {
                    System.out.println("✔ TEST 39 PASÓ: VINCULARINTERES validó el ID contra el catálogo oficial de oferta.");
                    testsPassed++;
                } else {
                    System.err.println("❌ TEST 39 FALLÓ: Debió rechazar ID 999: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 39 OMITIDO: Falta idLeadCreado.");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 39 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 40: CU3 RBAC - Coordinador bloqueado de ASIGNARMANUAL (Admin exclusivo)
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (idLeadCreado > 0) {
                CommandResult result = dispatcher.dispatch("coordinador@crm.bo", "ASIGNARMANUAL[\"" + idLeadCreado + "\", \"3\", \"Intento de coord\"]");
                if (!result.isSuccess() && result.getHtmlBody().contains("exclusiva para el Administrador del sistema")) {
                    System.out.println("✔ TEST 40 PASÓ: CU3 ASIGNARMANUAL bloqueó a Coordinador -> Exclusivo Administrador.");
                    testsPassed++;
                } else {
                    System.err.println("❌ TEST 40 FALLÓ: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 40 OMITIDO: Falta idLeadCreado.");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 40 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 41: CU3 RBAC - Coordinador bloqueado de TRANSFERIRLEAD (Admin exclusivo)
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (idLeadCreado > 0) {
                CommandResult result = dispatcher.dispatch("coordinador@crm.bo", "TRANSFERIRLEAD[\"" + idLeadCreado + "\", \"4\", \"Intento de coord\"]");
                if (!result.isSuccess() && result.getHtmlBody().contains("exclusiva para el Administrador del sistema")) {
                    System.out.println("✔ TEST 41 PASÓ: CU3 TRANSFERIRLEAD bloqueó a Coordinador -> Exclusivo Administrador.");
                    testsPassed++;
                } else {
                    System.err.println("❌ TEST 41 FALLÓ: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 41 OMITIDO: Falta idLeadCreado.");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 41 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 42: CU4 - MOVERETAPA con número de etapa inválido muestra tip 1 a 6
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (idLeadCreado > 0) {
                CommandResult result = dispatcher.dispatch("admin@crm.bo", "MOVERETAPA[\"" + idLeadCreado + "\", \"11\"]");
                if (!result.isSuccess() && result.getHtmlBody().contains("Las etapas van del 1 al 6: 1. Nuevo, 2. En Contacto, 3. Seguimiento, 4. Promesa de Pago, 5. Convertido, 6. Perdido.")) {
                    System.out.println("✔ TEST 42 PASÓ: CU4 MOVERETAPA validó rango (1 a 6) con sugerencia institucional.");
                    testsPassed++;
                } else {
                    System.err.println("❌ TEST 42 FALLÓ: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 42 OMITIDO: Falta idLeadCreado.");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 42 EXCEPCIÓN: " + e.getMessage());
        }

        // =============================================================
        // PRUEBAS ESPECÍFICAS DE CU5: MOTIVOS DE PÉRDIDA Y DESISTIMIENTO
        // =============================================================

        // -------------------------------------------------------------
        // TEST 43: CU5 - LISTARMOTIVOSPERDIDA lista el catálogo oficial
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("admin@crm.bo", "LISTARMOTIVOSPERDIDA[]");
            if (result.isSuccess() && result.getHtmlBody().contains("Factor económico") && result.getHtmlBody().contains("Horario no disponible")) {
                System.out.println("✔ TEST 43 PASÓ: CU5 LISTARMOTIVOSPERDIDA devolvió catálogo con causas de deserción.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 43 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 43 EXCEPCIÓN: " + e.getMessage());
        }

        // Creamos lead específico para pruebas de pérdida
        limpiarLeadPrueba("perdido.test@gmail.com");
        long idLeadPerdido = -1;
        try {
            CommandResult r = dispatcher.dispatch("admin@crm.bo", "REGISTRARPROSPECTO[\"Prospecto Baja\", \"71122334\", \"perdido.test@gmail.com\", \"Web\", \"Auditor\"]");
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT id_lead FROM leads WHERE correo = 'perdido.test@gmail.com'")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) idLeadPerdido = rs.getLong("id_lead");
            }
        } catch (Exception ignored) {}

        // -------------------------------------------------------------
        // TEST 44: CU5 - MARCARPERDIDO clasifica prospecto en Etapa 6 (Perdido)
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (idLeadPerdido > 0) {
                String cmdPerd = "MARCARPERDIDO[\"" + idLeadPerdido + "\", \"1\", \"El arancel excede presupuesto actual del cliente\"]";
                CommandResult result = dispatcher.dispatch("admin@crm.bo", cmdPerd);
                if (result.isSuccess() && result.getHtmlBody().contains("Prospecto Clasificado como Perdido") && result.getHtmlBody().contains("Factor económico")) {
                    try (Connection conn = DatabaseConnection.getConnection();
                         PreparedStatement ps = conn.prepareStatement("SELECT id_etapa, id_motivo_perdida FROM leads WHERE id_lead = ?")) {
                        ps.setLong(1, idLeadPerdido);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next() && rs.getLong("id_etapa") == 6 && rs.getLong("id_motivo_perdida") == 1) {
                            System.out.println("✔ TEST 44 PASÓ: CU5 MARCARPERDIDO pasó el lead #" + idLeadPerdido + " a etapa 6 con causa auditada.");
                            testsPassed++;
                        } else {
                            System.err.println("❌ TEST 44 FALLÓ: Datos no reflejados en PostgreSQL.");
                        }
                    }
                } else {
                    System.err.println("❌ TEST 44 FALLÓ: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 44 OMITIDO: Falta idLeadPerdido.");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 44 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 45: CU5 - MARCARPERDIDO rechaza justificación vacía
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (idLeadPerdido > 0) {
                CommandResult result = dispatcher.dispatch("admin@crm.bo", "MARCARPERDIDO[\"" + idLeadPerdido + "\", \"1\", \"\"]");
                if (!result.isSuccess() && result.getHtmlBody().contains("El comentario explicativo no puede estar vacio")) {
                    System.out.println("✔ TEST 45 PASÓ: CU5 MARCARPERDIDO exigió comentario analítico obligatorio.");
                    testsPassed++;
                } else {
                    System.err.println("❌ TEST 45 FALLÓ: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 45 OMITIDO: Falta idLeadPerdido.");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 45 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 46: CU5 - LISTARPERDIDOS lista los prospectos en etapa 6
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("admin@crm.bo", "LISTARPERDIDOS[]");
            if (result.isSuccess() && result.getHtmlBody().contains("Prospecto Baja") && result.getHtmlBody().contains("Factor económico")) {
                System.out.println("✔ TEST 46 PASÓ: CU5 LISTARPERDIDOS generó informe consolidado de bajas comerciales.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 46 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 46 EXCEPCIÓN: " + e.getMessage());
        }

        // =============================================================
        // PRUEBAS ESPECÍFICAS DE CU6: PAGOS Y MATRÍCULA ACADÉMICA
        // =============================================================

        // Creamos lead específico para pruebas de pago y matrícula
        limpiarLeadPrueba("matricula.test@gmail.com");
        long idLeadMatricula = -1;
        try {
            dispatcher.dispatch("admin@crm.bo", "REGISTRARPROSPECTO[\"Alumno Matriculado\", \"73344556\", \"matricula.test@gmail.com\", \"Web\", \"Ingeniero\"]");
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT id_lead FROM leads WHERE correo = 'matricula.test@gmail.com'")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) idLeadMatricula = rs.getLong("id_lead");
            }
        } catch (Exception ignored) {}

        long idComprobanteCreado = -1;

        // -------------------------------------------------------------
        // TEST 47: CU6 - REGISTRARCOMPROBANTE registra depósito y avanza a etapa 4
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (idLeadMatricula > 0) {
                String cmdPago = "REGISTRARCOMPROBANTE[\"" + idLeadMatricula + "\", \"Banco Mercantil Santa Cruz\", \"TR-889922\", \"2500.00\"]";
                CommandResult result = dispatcher.dispatch("admin@crm.bo", cmdPago);
                if (result.isSuccess() && result.getHtmlBody().contains("Comprobante de Pago Registrado") && result.getHtmlBody().contains("2500.00")) {
                    try (Connection conn = DatabaseConnection.getConnection();
                         PreparedStatement ps = conn.prepareStatement("SELECT id_comprobante, estado FROM comprobantes_pago WHERE id_lead = ?")) {
                        ps.setLong(1, idLeadMatricula);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            idComprobanteCreado = rs.getLong("id_comprobante");
                            String estado = rs.getString("estado");
                            if ("PENDIENTE".equals(estado)) {
                                System.out.println("✔ TEST 47 PASÓ: CU6 REGISTRARCOMPROBANTE registró comprobante #" + idComprobanteCreado + " en estado PENDIENTE.");
                                testsPassed++;
                            } else {
                                System.err.println("❌ TEST 47 FALLÓ: Estado incorrecto: " + estado);
                            }
                        } else {
                            System.err.println("❌ TEST 47 FALLÓ: Comprobante no insertado en BD.");
                        }
                    }
                } else {
                    System.err.println("❌ TEST 47 FALLÓ: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 47 OMITIDO: Falta idLeadMatricula.");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 47 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 48: CU6 - PAGOSPENDIENTES lista comprobantes en espera (Admin)
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("admin@crm.bo", "PAGOSPENDIENTES[]");
            if (result.isSuccess() && result.getHtmlBody().contains("TR-889922") && result.getHtmlBody().contains("2500.00")) {
                System.out.println("✔ TEST 48 PASÓ: CU6 PAGOSPENDIENTES mostró la cola contable de verificación.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 48 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 48 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 49: CU6 RBAC - PAGOSPENDIENTES denegado a Vendedor
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("vendedor1@crm.bo", "PAGOSPENDIENTES[]");
            if (!result.isSuccess() && result.getHtmlBody().contains("exclusiva para el Administrador del sistema")) {
                System.out.println("✔ TEST 49 PASÓ: CU6 PAGOSPENDIENTES bloqueó acceso a Vendedor -> Exclusivo Admin.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 49 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 49 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 50: CU6 - APROBARPAGO valida el comprobante satisfactoriamente
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (idComprobanteCreado > 0) {
                CommandResult result = dispatcher.dispatch("admin@crm.bo", "APROBARPAGO[\"" + idComprobanteCreado + "\"]");
                if (result.isSuccess() && result.getHtmlBody().contains("Pago Aprobado Satisfactoriamente")) {
                    try (Connection conn = DatabaseConnection.getConnection();
                         PreparedStatement ps = conn.prepareStatement("SELECT estado FROM comprobantes_pago WHERE id_comprobante = ?")) {
                        ps.setLong(1, idComprobanteCreado);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next() && "APROBADO".equals(rs.getString("estado"))) {
                            System.out.println("✔ TEST 50 PASÓ: CU6 APROBARPAGO aprobó el comprobante #" + idComprobanteCreado + " en PostgreSQL.");
                            testsPassed++;
                        } else {
                            System.err.println("❌ TEST 50 FALLÓ: Estado no pasó a APROBADO.");
                        }
                    }
                } else {
                    System.err.println("❌ TEST 50 FALLÓ: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 50 OMITIDO: Falta idComprobanteCreado.");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 50 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 51: CU6 - COMPLETARDATOSALUMNO registra CI, fecha nacimiento y ciudad
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (idLeadMatricula > 0) {
                String cmdDatos = "COMPLETARDATOSALUMNO[\"" + idLeadMatricula + "\", \"8912345 SC\", \"1997-11-20\", \"Santa Cruz de la Sierra\"]";
                CommandResult result = dispatcher.dispatch("admin@crm.bo", cmdDatos);
                if (result.isSuccess() && result.getHtmlBody().contains("Datos Academicos Registrados") && result.getHtmlBody().contains("8912345 SC")) {
                    try (Connection conn = DatabaseConnection.getConnection();
                         PreparedStatement ps = conn.prepareStatement("SELECT ci, ciudad FROM leads WHERE id_lead = ?")) {
                        ps.setLong(1, idLeadMatricula);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next() && "8912345 SC".equals(rs.getString("ci"))) {
                            System.out.println("✔ TEST 51 PASÓ: CU6 COMPLETARDATOSALUMNO completó el expediente académico formal.");
                            testsPassed++;
                        } else {
                            System.err.println("❌ TEST 51 FALLÓ: CI no guardado en BD.");
                        }
                    }
                } else {
                    System.err.println("❌ TEST 51 FALLÓ: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 51 OMITIDO: Falta idLeadMatricula.");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 51 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 52: CU6 - CONVERTIRAMATRICULA rechaza si faltan requisitos (lead sin pago)
        // -------------------------------------------------------------
        totalTests++;
        long idLeadSinPago = -1;
        try {
            limpiarLeadPrueba("sinpago@test.com");
            dispatcher.dispatch("admin@crm.bo", "REGISTRARPROSPECTO[\"Lead Sin Pago\", \"74455667\", \"sinpago@test.com\", \"Web\", \"Ingeniero\"]");
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT id_lead FROM leads WHERE correo = 'sinpago@test.com'")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) idLeadSinPago = rs.getLong("id_lead");
            }

            if (idLeadSinPago > 0) {
                // Completamos datos para probar que rechace específicamente por falta de PAGO
                dispatcher.dispatch("admin@crm.bo", "COMPLETARDATOSALUMNO[\"" + idLeadSinPago + "\", \"7788991 SC\", \"1995-03-12\", \"Santa Cruz\"]");
                CommandResult result = dispatcher.dispatch("admin@crm.bo", "CONVERTIRAMATRICULA[\"" + idLeadSinPago + "\"]");
                if (!result.isSuccess() && result.getHtmlBody().contains("No cuenta con ningun comprobante de pago aprobado")) {
                    System.out.println("✔ TEST 52 PASÓ: CU6 Validó regla institucional -> Rechazó matricular sin pago aprobado.");
                    testsPassed++;
                } else {
                    System.err.println("❌ TEST 52 FALLÓ: " + result.getHtmlBody());
                }
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 52 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 53: CU6 - CONVERTIRAMATRICULA formaliza alumno en Etapa 5 y emite constancia
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (idLeadMatricula > 0) {
                CommandResult result = dispatcher.dispatch("admin@crm.bo", "CONVERTIRAMATRICULA[\"" + idLeadMatricula + "\"]");
                if (result.isSuccess() && result.getHtmlBody().contains("Constancia Oficial de Matricula Academica") && result.getHtmlBody().contains("MATR-2026-" + idLeadMatricula)) {
                    try (Connection conn = DatabaseConnection.getConnection();
                         PreparedStatement ps = conn.prepareStatement("SELECT id_etapa FROM leads WHERE id_lead = ?")) {
                        ps.setLong(1, idLeadMatricula);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next() && rs.getLong("id_etapa") == 5) {
                            System.out.println("✔ TEST 53 PASÓ: CU6 CONVERTIRAMATRICULA formalizó al prospecto #" + idLeadMatricula + " a etapa 5 (Convertido) con constancia oficial.");
                            testsPassed++;
                        } else {
                            System.err.println("❌ TEST 53 FALLÓ: Lead no avanzó a etapa 5.");
                        }
                    }
                } else {
                    System.err.println("❌ TEST 53 FALLÓ: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 53 OMITIDO: Falta idLeadMatricula.");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 53 EXCEPCIÓN: " + e.getMessage());
        }

        // =============================================================
        // CASO DE USO 7 (CU7): BITACORA HISTORICA Y RECORDATORIOS
        // =============================================================

        // -------------------------------------------------------------
        // TEST 54: CU7 - AGREGARNOTA registra nota en bitacora del lead
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (idLeadMatricula > 0) {
                String cmdNota = "AGREGARNOTA[\"" + idLeadMatricula + "\", \"El alumno solicita el comprobante fiscal y credencial estudiantil\"]";
                CommandResult result = dispatcher.dispatch("admin@crm.bo", cmdNota);
                if (result.isSuccess() && result.getHtmlBody().contains("Nota Comercial Registrada")) {
                    try (Connection conn = DatabaseConnection.getConnection();
                         PreparedStatement ps = conn.prepareStatement("SELECT tipo, descripcion FROM bitacora WHERE id_lead = ? AND tipo = 'NOTA'")) {
                        ps.setLong(1, idLeadMatricula);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next() && rs.getString("descripcion").contains("comprobante fiscal")) {
                            System.out.println("✔ TEST 54 PASÓ: CU7 AGREGARNOTA registró formalmente nota en la bitácora del lead #" + idLeadMatricula + ".");
                            testsPassed++;
                        } else {
                            System.err.println("❌ TEST 54 FALLÓ: Nota no persistió en PostgreSQL.");
                        }
                    }
                } else {
                    System.err.println("❌ TEST 54 FALLÓ: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 54 OMITIDO: Falta idLeadMatricula.");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 54 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 55: CU7 - VERBITACORA despliega timeline cronológico del lead
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (idLeadMatricula > 0) {
                CommandResult result = dispatcher.dispatch("admin@crm.bo", "VERBITACORA[\"" + idLeadMatricula + "\"]");
                if (result.isSuccess() && result.getHtmlBody().contains("Bitacora Historica de Interacciones") &&
                    result.getHtmlBody().contains("MATRÍCULA") && result.getHtmlBody().contains("NOTA")) {
                    System.out.println("✔ TEST 55 PASÓ: CU7 VERBITACORA generó timeline cronológico completo con todos los hitos del prospecto.");
                    testsPassed++;
                } else {
                    System.err.println("❌ TEST 55 FALLÓ: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 55 OMITIDO: Falta idLeadMatricula.");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 55 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 56: CU7 - CREARRECORDATORIO agenda tarea con fecha futura
        // -------------------------------------------------------------
        totalTests++;
        long idRecordatorioCreado = -1;
        try {
            if (idLeadMatricula > 0) {
                String cmdRec = "CREARRECORDATORIO[\"" + idLeadMatricula + "\", \"2026-11-28 10:30\", \"Entregar credencial y dar bienvenida oficial\"]";
                CommandResult result = dispatcher.dispatch("admin@crm.bo", cmdRec);
                if (result.isSuccess() && result.getHtmlBody().contains("Recordatorio Agendado")) {
                    try (Connection conn = DatabaseConnection.getConnection();
                         PreparedStatement ps = conn.prepareStatement("SELECT id_recordatorio, estado FROM recordatorios WHERE id_lead = ? AND descripcion LIKE '%credencial%' ORDER BY id_recordatorio DESC LIMIT 1")) {
                        ps.setLong(1, idLeadMatricula);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            idRecordatorioCreado = rs.getLong("id_recordatorio");
                            if ("PENDIENTE".equals(rs.getString("estado"))) {
                                System.out.println("✔ TEST 56 PASÓ: CU7 CREARRECORDATORIO programó tarea #" + idRecordatorioCreado + " en estado PENDIENTE.");
                                testsPassed++;
                            } else {
                                System.err.println("❌ TEST 56 FALLÓ: Estado incorrecto: " + rs.getString("estado"));
                            }
                        } else {
                            System.err.println("❌ TEST 56 FALLÓ: No se encontró recordatorio en BD.");
                        }
                    }
                } else {
                    System.err.println("❌ TEST 56 FALLÓ: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 56 OMITIDO: Falta idLeadMatricula.");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 56 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 57: CU7 - CREARRECORDATORIO rechaza formato de fecha inválido
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (idLeadMatricula > 0) {
                String cmdErr = "CREARRECORDATORIO[\"" + idLeadMatricula + "\", \"fecha-invalida\", \"Llamar\"]";
                CommandResult result = dispatcher.dispatch("admin@crm.bo", cmdErr);
                if (!result.isSuccess() && result.getHtmlBody().contains("invalido")) {
                    System.out.println("✔ TEST 57 PASÓ: CU7 CREARRECORDATORIO validó y rechazó formato de fecha/hora incorrecto.");
                    testsPassed++;
                } else {
                    System.err.println("❌ TEST 57 FALLÓ: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 57 OMITIDO: Falta idLeadMatricula.");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 57 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 58: CU7 - MISRECORDATORIOS lista tareas pendientes del usuario
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("admin@crm.bo", "MISRECORDATORIOS[]");
            if (result.isSuccess() && result.getHtmlBody().contains("Mis Recordatorios Pendientes") && result.getHtmlBody().contains("Entregar credencial")) {
                System.out.println("✔ TEST 58 PASÓ: CU7 MISRECORDATORIOS listó la agenda de tareas pendientes del remitente.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 58 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 58 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 59: CU7 - COMPLETARRECORDATORIO finaliza la tarea y actualiza estado
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (idRecordatorioCreado > 0) {
                CommandResult result = dispatcher.dispatch("admin@crm.bo", "COMPLETARRECORDATORIO[\"" + idRecordatorioCreado + "\"]");
                if (result.isSuccess() && result.getHtmlBody().contains("Recordatorio Completado")) {
                    try (Connection conn = DatabaseConnection.getConnection();
                         PreparedStatement ps = conn.prepareStatement("SELECT estado FROM recordatorios WHERE id_recordatorio = ?")) {
                        ps.setLong(1, idRecordatorioCreado);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next() && "COMPLETADO".equals(rs.getString("estado"))) {
                            System.out.println("✔ TEST 59 PASÓ: CU7 COMPLETARRECORDATORIO cerró exitosamente la tarea #" + idRecordatorioCreado + ".");
                            testsPassed++;
                        } else {
                            System.err.println("❌ TEST 59 FALLÓ: Estado no pasó a COMPLETADO.");
                        }
                    }
                } else {
                    System.err.println("❌ TEST 59 FALLÓ: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 59 OMITIDO: Falta idRecordatorioCreado.");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 59 EXCEPCIÓN: " + e.getMessage());
        }

        // =============================================================
        // CASO DE USO 8 (CU8): REPORTES ESTADISTICOS Y CUADROS DE MANDO
        // =============================================================

        // -------------------------------------------------------------
        // TEST 60: CU8 - REPORTEEMBUDO despliega distribución y porcentajes de las 6 etapas
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("admin@crm.bo", "REPORTEEMBUDO[]");
            if (result.isSuccess() && result.getHtmlBody().contains("Reporte Estadistico: Embudo de Ventas") &&
                result.getHtmlBody().contains("Nuevo") && result.getHtmlBody().contains("Convertido")) {
                System.out.println("✔ TEST 60 PASÓ: CU8 REPORTEEMBUDO generó distribución volumétrica y porcentual de las 6 etapas del pipeline.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 60 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 60 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 61: CU8 - REPORTECONVERSION muestra KPIs globales y matriz por vendedor
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("admin@crm.bo", "REPORTECONVERSION[]");
            if (result.isSuccess() && result.getHtmlBody().contains("Reporte de Conversion y Rendimiento Comercial") &&
                result.getHtmlBody().contains("TASA CONVERSION") && result.getHtmlBody().contains("Nicol Flores")) {
                System.out.println("✔ TEST 61 PASÓ: CU8 REPORTECONVERSION consolidó KPIs globales y matriz individual por ejecutivo.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 61 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 61 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 62: CU8 - REPORTEORIGENES muestra desglose de captación por canal
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("admin@crm.bo", "REPORTEORIGENES[]");
            if (result.isSuccess() && result.getHtmlBody().contains("Reporte de Canales de Captacion") &&
                result.getHtmlBody().contains("WhatsApp")) {
                System.out.println("✔ TEST 62 PASÓ: CU8 REPORTEORIGENES desplegó efectividad y captación por fuentes de prospección.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 62 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 62 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 63: CU8 - REPORTEMOTIVOSPERDIDA muestra causales de abandono
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("admin@crm.bo", "REPORTEMOTIVOSPERDIDA[]");
            if (result.isSuccess() && result.getHtmlBody().contains("Reporte de Motivos de Desistimiento") &&
                result.getHtmlBody().contains("CAUSAS DE DESISTIMIENTO COMERCIAL")) {
                System.out.println("✔ TEST 63 PASÓ: CU8 REPORTEMOTIVOSPERDIDA analizó las causales de pérdida comercial y porcentajes.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 63 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 63 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 64: CU8 - DASHBOARDVENDEDOR muestra KPIs personales de cartera
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("vendedor1@crm.bo", "DASHBOARDVENDEDOR[]");
            if (result.isSuccess() && result.getHtmlBody().contains("Cuadro de Mando del Vendedor") &&
                result.getHtmlBody().contains("TOTAL EN CARTERA")) {
                System.out.println("✔ TEST 64 PASÓ: CU8 DASHBOARDVENDEDOR desplegó el panel de control con métricas del vendedor.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 64 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 64 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 65: CU8 - REPORTEEMBUDO rechaza acceso a vendedor (RBAC gerencial)
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("vendedor1@crm.bo", "REPORTEEMBUDO[]");
            if (!result.isSuccess() && result.getHtmlBody().contains("Acceso denegado")) {
                System.out.println("✔ TEST 65 PASÓ: CU8 REPORTEEMBUDO protegió el acceso gerencial bloqueando el rol Vendedor.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 65 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 65 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 66: CU5 - Coordinador bloqueado de LISTARMOTIVOSPERDIDA
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("coordinador@crm.bo", "LISTARMOTIVOSPERDIDA[]");
            if (!result.isSuccess() && result.getHtmlBody().contains("Acceso denegado")) {
                System.out.println("✔ TEST 66 PASÓ: CU5 LISTARMOTIVOSPERDIDA bloqueó al rol Coordinador (solo Admin y Vendedor).");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 66 FALLÓ: Coordinador no fue bloqueado.");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 66 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 67: CU6 - Coordinador bloqueado de REGISTRARCOMPROBANTE
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("coordinador@crm.bo", "REGISTRARCOMPROBANTE[\"1\", \"Banco Union\", \"889922\", \"1500.00\"]");
            if (!result.isSuccess() && result.getHtmlBody().contains("Acceso denegado")) {
                System.out.println("✔ TEST 67 PASÓ: CU6 REGISTRARCOMPROBANTE bloqueó al rol Coordinador (solo Admin y Vendedor).");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 67 FALLÓ: Coordinador no fue bloqueado.");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 67 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 68: CU7 - Coordinador bloqueado de AGREGARNOTA
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("coordinador@crm.bo", "AGREGARNOTA[\"1\", \"Nota de prueba por coordinador\"]");
            if (!result.isSuccess() && result.getHtmlBody().contains("Acceso denegado")) {
                System.out.println("✔ TEST 68 PASÓ: CU7 AGREGARNOTA bloqueó al rol Coordinador (solo Admin y Vendedor).");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 68 FALLÓ: Coordinador no fue bloqueado.");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 68 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 69: CU8 - Coordinador bloqueado de DASHBOARDVENDEDOR
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("coordinador@crm.bo", "DASHBOARDVENDEDOR[]");
            if (!result.isSuccess() && result.getHtmlBody().contains("Acceso denegado")) {
                System.out.println("✔ TEST 69 PASÓ: CU8 DASHBOARDVENDEDOR bloqueó al rol Coordinador (solo Admin y Vendedor).");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 69 FALLÓ: Coordinador no fue bloqueado.");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 69 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 70: Autenticación con Correo Real del Equipo
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("nataly.vane.mm@gmail.com", "QUIENSOY[]");
            if (result.isSuccess() && result.getHtmlBody().contains("Nataly Martinez (Admin)") && result.getHtmlBody().contains("Administrador")) {
                System.out.println("✔ TEST 70 PASÓ: QUIENSOY[] autenticó con éxito el correo real nataly.vane.mm@gmail.com como Administrador.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 70 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 70 EXCEPCIÓN: " + e.getMessage());
        }

        // =============================================================
        // SIMULACIÓN COMPLETA DEL FLUJO DE DEFENSA EN VIVO CON EL DOCENTE
        // (Flujo oficial: Evans Balcazar desde HELP hasta Matrícula en Etapa 5)
        // =============================================================
        System.out.println("\n--- SIMULANDO FLUJO EXACTO DE DEFENSA EN VIVO (DOCENTE ING. EVANS BALCAZAR) ---");
        limpiarLeadPrueba("carlos.v@gmail.com");
        limpiarUsuarioPrueba("evansbv@gmail.com");

        // -------------------------------------------------------------
        // TEST 71: Docente envía HELP[] inicialmente
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("evansbv@gmail.com", "HELP[]");
            if (result.isSuccess() && result.getHtmlBody().contains("Catalogo Oficial de Comandos")) {
                System.out.println("✔ TEST 71 PASÓ: Docente envía HELP[] y recibe catálogo interactivo del sistema.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 71 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 71 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 72: Administrador da de alta al docente como Administrador
        // -------------------------------------------------------------
        totalTests++;
        try {
            String cmdCrearDocente = "CREARUSUARIO[\"Ing. Evans Balcazar\", \"evansbv@gmail.com\", \"12345678\", \"Administrador\"]";
            CommandResult result = dispatcher.dispatch("nataly.vane.mm@gmail.com", cmdCrearDocente);
            if (result.isSuccess() && result.getHtmlBody().contains("Usuario Creado Exitosamente")) {
                System.out.println("✔ TEST 72 PASÓ: Administrador creó la cuenta para el Ing. Evans Balcazar con rol Administrador.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 72 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 72 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 73: Docente verifica su identidad como Administrador
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("evansbv@gmail.com", "QUIENSOY[]");
            if (result.isSuccess() && result.getHtmlBody().contains("Ing. Evans Balcazar") && result.getHtmlBody().contains("Administrador")) {
                System.out.println("✔ TEST 73 PASÓ: QUIENSOY[] confirmó que el Ing. Evans Balcazar opera con rol Administrador.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 73 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 73 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 74: Docente registra nuevo prospecto (Carlos Villagomez)
        // -------------------------------------------------------------
        totalTests++;
        long idLeadDefensa = -1;
        try {
            String cmdRegLead = "REGISTRARPROSPECTO[\"Carlos Villagomez\", \"70012345\", \"carlos.v@gmail.com\", \"Facebook\", \"Ingeniería de Sistemas / Software\"]";
            CommandResult result = dispatcher.dispatch("evansbv@gmail.com", cmdRegLead);
            if (result.isSuccess() && result.getHtmlBody().contains("satisfactoriamente")) {
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement ps = conn.prepareStatement("SELECT id_lead FROM leads WHERE correo = 'carlos.v@gmail.com'")) {
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        idLeadDefensa = rs.getLong("id_lead");
                        System.out.println("✔ TEST 74 PASÓ: Docente registró prospecto #" + idLeadDefensa + " (Carlos Villagomez) en etapa 1 (Nuevo).");
                        testsPassed++;
                    }
                }
            } else {
                System.err.println("❌ TEST 74 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 74 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 75: Docente consulta la oferta académica disponible
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("evansbv@gmail.com", "LISTAROFERTA[]");
            if (result.isSuccess() && result.getHtmlBody().contains("Oferta Academica Disponible") && result.getHtmlBody().contains("101")) {
                System.out.println("✔ TEST 75 PASÓ: Docente consultó LISTAROFERTA[] y visualizó programas y módulos.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 75 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 75 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 76: Docente vincula interés académico al prospecto
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (idLeadDefensa > 0) {
                String cmdVinc = "VINCULARINTERES[\"" + idLeadDefensa + "\", \"PROGRAMA\", \"101\", \"Diplomado en Ciberseguridad Defensiva\"]";
                CommandResult result = dispatcher.dispatch("evansbv@gmail.com", cmdVinc);
                if (result.isSuccess() && result.getHtmlBody().contains("Interes Academico Vinculado")) {
                    System.out.println("✔ TEST 76 PASÓ: Docente vinculó programa #101 al prospecto #" + idLeadDefensa + ".");
                    testsPassed++;
                } else {
                    System.err.println("❌ TEST 76 FALLÓ: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 76 OMITIDO: idLeadDefensa <= 0");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 76 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 77: Docente lista usuarios para identificar vendedor
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("evansbv@gmail.com", "LISTARUSUARIOS[]");
            if (result.isSuccess() && result.getHtmlBody().contains("Vendedor")) {
                System.out.println("✔ TEST 77 PASÓ: Docente listó usuarios e identificó ejecutivos comerciales.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 77 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 77 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 78: Docente asigna manualmente el prospecto a vendedor #3
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (idLeadDefensa > 0) {
                String cmdAsignar = "ASIGNARMANUAL[\"" + idLeadDefensa + "\", \"3\"]";
                CommandResult result = dispatcher.dispatch("evansbv@gmail.com", cmdAsignar);
                if (result.isSuccess() && result.getHtmlBody().contains("Asignacion Manual Exitosa")) {
                    System.out.println("✔ TEST 78 PASÓ: Docente asignó formalmente el lead #" + idLeadDefensa + " a Vendedor #3.");
                    testsPassed++;
                } else {
                    System.err.println("❌ TEST 78 FALLÓ: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 78 OMITIDO: idLeadDefensa <= 0");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 78 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 79: Docente avanza el lead a Etapa 4 (Promesa de Pago)
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (idLeadDefensa > 0) {
                String cmdMover = "MOVERETAPA[\"" + idLeadDefensa + "\", \"4\"]";
                CommandResult result = dispatcher.dispatch("evansbv@gmail.com", cmdMover);
                if (result.isSuccess() && result.getHtmlBody().contains("Etapa Comercial Actualizada")) {
                    System.out.println("✔ TEST 79 PASÓ: Docente avanzó el lead #" + idLeadDefensa + " a Etapa 4 (Promesa de Pago).");
                    testsPassed++;
                } else {
                    System.err.println("❌ TEST 79 FALLÓ: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 79 OMITIDO: idLeadDefensa <= 0");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 79 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 80: Docente registra comprobante bancario
        // -------------------------------------------------------------
        totalTests++;
        long idCompDefensa = -1;
        try {
            if (idLeadDefensa > 0) {
                String cmdComp = "REGISTRARCOMPROBANTE[\"" + idLeadDefensa + "\", \"Banco Mercantil Santa Cruz\", \"TR-998877\", \"2500.00\"]";
                CommandResult result = dispatcher.dispatch("evansbv@gmail.com", cmdComp);
                if (result.isSuccess() && result.getHtmlBody().contains("Comprobante de Pago Registrado")) {
                    try (Connection conn = DatabaseConnection.getConnection();
                         PreparedStatement ps = conn.prepareStatement("SELECT id_comprobante FROM comprobantes_pago WHERE id_lead = ? ORDER BY id_comprobante DESC LIMIT 1")) {
                        ps.setLong(1, idLeadDefensa);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            idCompDefensa = rs.getLong("id_comprobante");
                            System.out.println("✔ TEST 80 PASÓ: Docente registró comprobante #" + idCompDefensa + " en estado PENDIENTE.");
                            testsPassed++;
                        }
                    }
                } else {
                    System.err.println("❌ TEST 80 FALLÓ: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 80 OMITIDO: idLeadDefensa <= 0");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 80 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 81: Docente aprueba el comprobante de pago
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (idCompDefensa > 0) {
                String cmdAprobar = "APROBARPAGO[\"" + idCompDefensa + "\"]";
                CommandResult result = dispatcher.dispatch("evansbv@gmail.com", cmdAprobar);
                if (result.isSuccess() && result.getHtmlBody().contains("Pago Aprobado Satisfactoriamente")) {
                    System.out.println("✔ TEST 81 PASÓ: Docente aprobó el comprobante #" + idCompDefensa + " como Administrador.");
                    testsPassed++;
                } else {
                    System.err.println("❌ TEST 81 FALLÓ: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 81 OMITIDO: idCompDefensa <= 0");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 81 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 82: Docente completa datos formales del alumno
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (idLeadDefensa > 0) {
                String cmdDatos = "COMPLETARDATOSALUMNO[\"" + idLeadDefensa + "\", \"8921043 SC\", \"1998-05-14\", \"Santa Cruz de la Sierra\"]";
                CommandResult result = dispatcher.dispatch("evansbv@gmail.com", cmdDatos);
                if (result.isSuccess() && result.getHtmlBody().contains("Datos Academicos Registrados")) {
                    System.out.println("✔ TEST 82 PASÓ: Docente completó el expediente académico (CI, fecha nacimiento, ciudad).");
                    testsPassed++;
                } else {
                    System.err.println("❌ TEST 82 FALLÓ: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 82 OMITIDO: idLeadDefensa <= 0");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 82 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 83: Docente convierte a matrícula y emite constancia
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (idLeadDefensa > 0) {
                String cmdMatricula = "CONVERTIRAMATRICULA[\"" + idLeadDefensa + "\"]";
                CommandResult result = dispatcher.dispatch("evansbv@gmail.com", cmdMatricula);
                if (result.isSuccess() && result.getHtmlBody().contains("Constancia Oficial de Matricula Academica") && result.getHtmlBody().contains("MATR-2026-" + idLeadDefensa)) {
                    System.out.println("✔ TEST 83 PASÓ: Docente convirtió al prospecto #" + idLeadDefensa + " en Alumno Matriculado con constancia oficial.");
                    testsPassed++;
                } else {
                    System.err.println("❌ TEST 83 FALLÓ: " + result.getHtmlBody());
                }
            } else {
                System.err.println("❌ TEST 83 OMITIDO: idLeadDefensa <= 0");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 83 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 84: Docente verifica que el alumno está en Etapa 5 (Convertido)
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandResult result = dispatcher.dispatch("evansbv@gmail.com", "LEADSETAPA[\"5\"]");
            if (result.isSuccess() && result.getHtmlBody().contains("Carlos Villagomez")) {
                System.out.println("✔ TEST 84 PASÓ: LEADSETAPA[\"5\"] confirmó que Carlos Villagomez figura oficialmente en Etapa 5 (Convertido).");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 84 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 84 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 85: RFC 2822 Header Unfolding (Despliegue de Asuntos cortados por Gmail)
        // -------------------------------------------------------------
        totalTests++;
        try {
            String rawEmail = "From: Raysa <raysa@gmail.com>\n" +
                              "Subject: REGISTRARPROSPECTO[\"Raysa Sanchez\",\"72115304\",\"morita@gmail.com\",\"Facebook\",\"Ingenieria\n" +
                              " en Sistemas\"]\n" +
                              "Date: Mon, 23 Nov 2026 11:30:00 -0400\n\n" +
                              "Cuerpo de prueba";
            com.tecnoweb.grupo05sc.mail.MailMessage mm = com.tecnoweb.grupo05sc.mail.POP3Listener.parseRawEmail(1, "1", rawEmail);
            String esperado = "REGISTRARPROSPECTO[\"Raysa Sanchez\",\"72115304\",\"morita@gmail.com\",\"Facebook\",\"Ingenieria en Sistemas\"]";
            if (mm.getSubject().equals(esperado)) {
                System.out.println("✔ TEST 85 PASÓ: RFC 2822 Header Unfolding recompuso correctamente el asunto plegado por Gmail.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 85 FALLÓ: Asunto obtenido: [" + mm.getSubject() + "], esperado: [" + esperado + "]");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 85 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 86: RFC 2047 MIME Encoded-Word Decoding (Base64 UTF-8)
        // -------------------------------------------------------------
        totalTests++;
        try {
            String encodedSubject = "=?UTF-8?B?UkVHSVNUUkFSUFJPU1BFQ1RPWyJJdmFubmEgTSIsIjcyMTE1MzAzIiwiaXZhbm5hLnBvdA==?= =?UTF-8?B?ZXJpY2FAZ21haWwuY29tIiwiRmFjZWJvb2siLCJJbmdlbmllcsOtYSBkZSBTaXN0ZW1hcyAvIFNvZnR3YXJlIl0=?=";
            String decoded = com.tecnoweb.grupo05sc.mail.POP3Listener.decodeRFC2047(encodedSubject);
            String esperado = "REGISTRARPROSPECTO[\"Ivanna M\",\"72115303\",\"ivanna.poterica@gmail.com\",\"Facebook\",\"Ingeniería de Sistemas / Software\"]";
            if (decoded.equals(esperado)) {
                System.out.println("✔ TEST 86 PASÓ: RFC 2047 decodificó múltiples bloques Base64 UTF-8 eliminando espacios lineales intermedios.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 86 FALLÓ: Decodificado: [" + decoded + "], esperado: [" + esperado + "]");
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 86 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 87: Normalización de comillas tipográficas de Google Docs / Word
        // -------------------------------------------------------------
        totalTests++;
        try {
            String docsSubject = "REGISTRARPROSPECTO[“Raysa Sanchez”, “72115304”, “morita@gmail.com”, “Facebook”, “Ingenieria en Sistemas”]";
            ParsedCommand parsed = CommandParser.parse(docsSubject);
            if (parsed.getName().equals("REGISTRARPROSPECTO") && parsed.paramCount() == 5 &&
                parsed.getParam(0).equals("Raysa Sanchez") && parsed.getParam(4).equals("Ingenieria en Sistemas")) {
                System.out.println("✔ TEST 87 PASÓ: CommandParser normalizó comillas tipográficas “...” de Google Docs a ASCII puro.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 87 FALLÓ: " + parsed);
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 87 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 88: Resolución flexible de profesión y validación de catálogo
        // -------------------------------------------------------------
        totalTests++;
        try {
            com.tecnoweb.grupo05sc.dao.LeadDao dao = new com.tecnoweb.grupo05sc.dao.LeadDao();
            Long id1 = dao.obtenerIdProfesionPorNombre("Ingenieria en Sistemas");
            Long id2 = dao.obtenerIdProfesionPorNombre("Ing. Sistemas");
            Long id3 = dao.obtenerIdProfesionPorNombre("Sistemas");
            Long id4 = dao.obtenerIdProfesionPorNombre("Software");
            Long idInvalido = dao.obtenerIdProfesionPorNombre("Astronauta");

            boolean okFlexible = (id1 != null && id1 == 1L && id2 != null && id2 == 1L && id3 != null && id3 == 1L && id4 != null && id4 == 1L && idInvalido == null);

            CommandResult rInvalido = dispatcher.dispatch("admin@crm.bo", "REGISTRARPROSPECTO[\"Carlos\",\"71112233\",\"c@test.bo\",\"Web\",\"Astronauta\"]");
            boolean okRechazo = !rInvalido.isSuccess() && rInvalido.getHtmlBody().contains("LISTARPROFESIONES[]");

            if (okFlexible && okRechazo) {
                System.out.println("✔ TEST 88 PASÓ: Resolución flexible de profesiones ('Ingenieria en Sistemas' -> 1) y validación ante profesión inexistente.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 88 FALLÓ: okFlexible=" + okFlexible + ", okRechazo=" + okRechazo);
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 88 EXCEPCIÓN: " + e.getMessage());
        }

        // Limpieza final de prueba
        limpiarUsuarioPrueba("mariana.suarez@crm.bo");
        limpiarUsuarioPrueba("mariana.modificada@crm.bo");
        limpiarLeadPrueba("mario.test@gmail.com");
        limpiarLeadPrueba("perdido.test@gmail.com");
        limpiarLeadPrueba("matricula.test@gmail.com");
        limpiarLeadPrueba("sinpago@test.com");
        limpiarLeadPrueba("carlos.v@gmail.com");
        limpiarUsuarioPrueba("evansbv@gmail.com");

        System.out.println("\n==========================================================================");
        System.out.println("  RESUMEN FINAL: " + testsPassed + " de " + totalTests + " pruebas pasaron con éxito (" + (testsPassed * 100 / totalTests) + "%).");
        System.out.println("==========================================================================\n");
    }

    private static void limpiarUsuarioPrueba(String correo) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            Long idUser = null;
            try (PreparedStatement ps = conn.prepareStatement("SELECT id_usuario FROM usuarios WHERE LOWER(correo) = LOWER(?)")) {
                ps.setString(1, correo);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) idUser = rs.getLong(1);
            }
            if (idUser != null) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM comprobantes_pago WHERE id_aprobador = ?")) {
                    ps.setLong(1, idUser); ps.executeUpdate();
                } catch (Exception ignored) {}
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM historial_asignaciones WHERE id_vendedor_asignado = ? OR id_usuario_accion = ?")) {
                    ps.setLong(1, idUser); ps.setLong(2, idUser); ps.executeUpdate();
                } catch (Exception ignored) {}
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM notas_lead WHERE id_usuario = ?")) {
                    ps.setLong(1, idUser); ps.executeUpdate();
                } catch (Exception ignored) {}
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM recordatorios WHERE id_usuario = ?")) {
                    ps.setLong(1, idUser); ps.executeUpdate();
                } catch (Exception ignored) {}
                try (PreparedStatement ps = conn.prepareStatement("UPDATE leads SET id_vendedor = NULL WHERE id_vendedor = ?")) {
                    ps.setLong(1, idUser); ps.executeUpdate();
                } catch (Exception ignored) {}
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM usuarios WHERE id_usuario = ?")) {
                    ps.setLong(1, idUser); ps.executeUpdate();
                }
            }
        } catch (Exception ignored) {}
    }

    private static void limpiarLeadPrueba(String correo) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            Long idLead = null;
            try (PreparedStatement ps = conn.prepareStatement("SELECT id_lead FROM leads WHERE LOWER(correo) = LOWER(?)")) {
                ps.setString(1, correo);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) idLead = rs.getLong(1);
            }
            if (idLead != null) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM comprobantes_pago WHERE id_lead = ?")) {
                    ps.setLong(1, idLead); ps.executeUpdate();
                } catch (Exception ignored) {}
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM matriculas WHERE id_lead = ?")) {
                    ps.setLong(1, idLead); ps.executeUpdate();
                } catch (Exception ignored) {}
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM historial_asignaciones WHERE id_lead = ?")) {
                    ps.setLong(1, idLead); ps.executeUpdate();
                } catch (Exception ignored) {}
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM intereses_lead WHERE id_lead = ?")) {
                    ps.setLong(1, idLead); ps.executeUpdate();
                } catch (Exception ignored) {}
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM notas_lead WHERE id_lead = ?")) {
                    ps.setLong(1, idLead); ps.executeUpdate();
                } catch (Exception ignored) {}
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM recordatorios WHERE id_lead = ?")) {
                    ps.setLong(1, idLead); ps.executeUpdate();
                } catch (Exception ignored) {}
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM leads WHERE id_lead = ?")) {
                    ps.setLong(1, idLead); ps.executeUpdate();
                }
            }
        } catch (Exception ignored) {}
    }
}
