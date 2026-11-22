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
            if (result.isSuccess() && result.getHtmlBody().contains("Captacion de Prospecto (CU2 - Jael)") && result.getHtmlBody().contains("Mario Test")) {
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
        // TEST 28: CU3 - ASIGNARMANUAL rechazado si el usuario no es vendedor (Coordinador #2)
        // -------------------------------------------------------------
        totalTests++;
        try {
            if (idLeadCreado > 0) {
                String cmdAsig = "ASIGNARMANUAL[\"" + idLeadCreado + "\", \"2\", \"Asignacion invalida\"]";
                CommandResult result = dispatcher.dispatch("admin@crm.bo", cmdAsig);
                if (!result.isSuccess() && result.getHtmlBody().contains("solo pueden ser asignados a usuarios con rol 'Vendedor'")) {
                    System.out.println("✔ TEST 28 PASÓ: CU3 ASIGNARMANUAL validó que el destinatario tenga rol 'Vendedor'.");
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

        // Limpieza final de prueba
        limpiarUsuarioPrueba("mariana.suarez@crm.bo");
        limpiarUsuarioPrueba("mariana.modificada@crm.bo");
        limpiarLeadPrueba("mario.test@gmail.com");

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

    private static void limpiarLeadPrueba(String correo) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM leads WHERE LOWER(correo) = LOWER(?)")) {
            ps.setString(1, correo);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }
}
