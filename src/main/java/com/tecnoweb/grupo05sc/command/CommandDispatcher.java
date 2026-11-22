package com.tecnoweb.grupo05sc.command;

import com.tecnoweb.grupo05sc.config.Config;
import com.tecnoweb.grupo05sc.config.DatabaseConnection;
import com.tecnoweb.grupo05sc.security.SecurityContext;
import com.tecnoweb.grupo05sc.security.UsuarioAuth;
import com.tecnoweb.grupo05sc.service.UsuarioService;
import com.tecnoweb.grupo05sc.view.HtmlTemplate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Enruta los comandos parseados hacia la logica de negocio correspondiente.
 * Proporciona:
 *   1. Catalogo de ayuda dinamico e integral (HELP[] y HELP["COMANDO"]).
 *   2. Identificacion del remitente (QUIENSOY[]).
 *   3. Deteccion de errores tipograficos con sugerencias semanticas de comandos cercanos.
 *   4. Delegacion modular hacia los 8 Casos de Uso del CRM Educativo.
 */
public final class CommandDispatcher {

    private final UsuarioService usuarioService = new UsuarioService();
    private final com.tecnoweb.grupo05sc.service.LeadService leadService = new com.tecnoweb.grupo05sc.service.LeadService();
    private final com.tecnoweb.grupo05sc.service.AsignacionService asignacionService = new com.tecnoweb.grupo05sc.service.AsignacionService();
    private final com.tecnoweb.grupo05sc.service.PipelineService pipelineService = new com.tecnoweb.grupo05sc.service.PipelineService();

    private static final List<String> COMANDOS_VALIDOS = Arrays.asList(
        "HELP", "AYUDA", "QUIENSOY",
        "LISTARUSUARIOS", "BUSCARUSUARIO", "CREARUSUARIO", "MODIFICARUSUARIO", "TOGGLEUSUARIO", "TOGGLEROUNDROBIN",
        "REGISTRARPROSPECTO", "VINCULARINTERES", "LISTARINTERESES", "LISTARORIGENES", "LISTARPROFESIONES",
        "BOLSACOMUN", "ASIGNARMANUAL", "EJECUTARROUNDROBIN", "TRANSFERIRLEAD", "HISTORIALASIGNACIONES",
        "VERKANBAN", "VEREMBUDO", "LEADSETAPA", "MOVERETAPA", "MISLEADS",
        "LISTARMOTIVOSPERDIDA", "MARCARPERDIDO", "LISTARPERDIDOS",
        "REGISTRARCOMPROBANTE", "PAGOSPENDIENTES", "APROBARPAGO", "RECHAZARPAGO", "COMPLETARDATOSALUMNO", "CONVERTIRAMATRICULA",
        "VERBITACORA", "AGREGARNOTA", "CREARRECORDATORIO", "MISRECORDATORIOS", "COMPLETARRECORDATORIO",
        "REPORTEEMBUDO", "REPORTECONVERSION", "REPORTEORIGENES", "REPORTEMOTIVOSPERDIDA", "DASHBOARDVENDEDOR"
    );

    public CommandResult dispatch(String fromEmail, String subject) {
        String cleanSubject = (subject != null) ? subject.trim() : "";

        try {
            ParsedCommand cmd = CommandParser.parse(cleanSubject);
            System.out.println(">>> [DISPATCHER] Procesando comando: " + cmd.getName() + " con " + cmd.paramCount() + " parametros. Emisor: " + fromEmail);

            switch (cmd.getName()) {
                // =============================================================
                // 1. COMANDOS GENERALES Y AYUDA (HELP, QUIENSOY)
                // =============================================================
                case "HELP":
                case "AYUDA":
                    return handleHelp(cmd, fromEmail);

                case "QUIENSOY":
                    return handleQuienSoy(fromEmail);

                // =============================================================
                // 2. CASOS DE USO CRM EDUCATIVO (Delegacion modular)
                // =============================================================

                // CU1: Gestion de Usuarios (Nataly)
                case "LISTARUSUARIOS":
                case "BUSCARUSUARIO":
                case "CREARUSUARIO":
                case "MODIFICARUSUARIO":
                case "TOGGLEUSUARIO":
                case "TOGGLEROUNDROBIN":
                    return handleModuloUsuarios(cmd, fromEmail);

                // CU2: Captacion de Leads y Oferta (Jael)
                case "REGISTRARPROSPECTO":
                case "VINCULARINTERES":
                case "LISTARINTERESES":
                case "LISTARORIGENES":
                case "LISTARPROFESIONES":
                    return handleModuloCaptacion(cmd, fromEmail);

                // CU3: Asignacion de Leads (Nicol)
                case "BOLSACOMUN":
                case "ASIGNARMANUAL":
                case "EJECUTARROUNDROBIN":
                case "TRANSFERIRLEAD":
                case "HISTORIALASIGNACIONES":
                    return handleModuloAsignacion(cmd, fromEmail);

                // CU4: Embudo de Ventas Kanban (Nicol)
                case "VERKANBAN":
                case "VEREMBUDO":
                case "LEADSETAPA":
                case "MOVERETAPA":
                case "MISLEADS":
                    return handleModuloKanban(cmd, fromEmail);

                // CU5: Motivos de Perdida (Nicol)
                case "LISTARMOTIVOSPERDIDA":
                case "MARCARPERDIDO":
                case "LISTARPERDIDOS":
                    return handleModuloPerdida(cmd, fromEmail);

                // CU6: Pagos y Matricula (Nataly)
                case "REGISTRARCOMPROBANTE":
                case "PAGOSPENDIENTES":
                case "APROBARPAGO":
                case "RECHAZARPAGO":
                case "COMPLETARDATOSALUMNO":
                case "CONVERTIRAMATRICULA":
                    return handleModuloMatricula(cmd, fromEmail);

                // CU7: Bitacora y Recordatorios (Nataly)
                case "VERBITACORA":
                case "AGREGARNOTA":
                case "CREARRECORDATORIO":
                case "MISRECORDATORIOS":
                case "COMPLETARRECORDATORIO":
                    return handleModuloBitacora(cmd, fromEmail);

                // CU8: Reportes y Estadisticas (Jael)
                case "REPORTEEMBUDO":
                case "REPORTECONVERSION":
                case "REPORTEORIGENES":
                case "REPORTEMOTIVOSPERDIDA":
                case "DASHBOARDVENDEDOR":
                    return handleModuloReportes(cmd, fromEmail);

                default: {
                    String sugerencia = sugerirComandoCercano(cmd.getName());
                    String mensaje = "El comando '" + cmd.getName() + "' no existe en el sistema.";
                    String ayuda = (sugerencia != null)
                        ? "Quizas quiso decir: " + sugerencia + "[]? Envie HELP[\"" + sugerencia + "\"] para ver su formato."
                        : "Envie HELP[] en el asunto para consultar la lista completa de comandos.";
                    throw new CommandException(mensaje, ayuda);
                }
            }

        } catch (CommandException e) {
            String respSubject = Config.SUBJECT_PREFIX + " Error en comando: " + cleanSubject;
            String errorHtml = HtmlTemplate.errorCard(e.getMessage(), e.getHelpTip());
            return CommandResult.error(respSubject, errorHtml);

        } catch (Exception e) {
            System.err.println("[ERROR NO CONTROLADO] " + e.getMessage());
            e.printStackTrace();
            String respSubject = Config.SUBJECT_PREFIX + " Error de servidor";
            String errorHtml = HtmlTemplate.errorCard(
                "Ocurrio un error inesperado al procesar la solicitud: " + e.getMessage(),
                "Verifique los parametros enviados o contacte al administrador del sistema."
            );
            return CommandResult.error(respSubject, errorHtml);
        }
    }

    // =========================================================================
    // AYUDA DINAMICA E INTEGRAL (HELP[], HELP["COMANDO"])
    // =========================================================================

    private CommandResult handleHelp(ParsedCommand cmd, String fromEmail) {
        // Caso 1: Detalle especifico de un comando -> HELP["NOMBRE_COMANDO"]
        if (cmd.hasParams()) {
            try {
                String cmdBuscado = cmd.getParam(0).trim().toUpperCase();
                return handleHelpDetallado(cmdBuscado);
            } catch (Exception ignored) {}
        }

        // Caso 2: Catalogo Completo de Comandos (Todos los Casos de Uso)
        StringBuilder sb = new StringBuilder();
        sb.append("<p style='font-size: 14px; margin-top: 0;'><b>Catalogo Oficial de Comandos del Sistema CRM Educativo</b></p>");
        sb.append("<p style='font-size: 13px; color: #444444;'>Los comandos se envian en el <b>Asunto</b> del correo. Formato: <code>COMANDO[\"param1\", \"param2\", ...]</code>. Para comandos sin parametros puede usar <code>COMANDO[]</code>.</p>");

        String[] headers = new String[]{"MODULO", "COMANDO", "PARAMETROS", "DESCRIPCION"};
        List<String[]> rows = new ArrayList<>();

        // 1. GENERALES
        rows.add(new String[]{"GENERAL", "HELP", "[] o [\"COMANDO\"]", "Muestra esta guia completa o la sintaxis de un comando"});
        rows.add(new String[]{"GENERAL", "QUIENSOY", "[]", "Muestra informacion de identidad, rol y estado del remitente"});

        // 2. CU1: GESTION DE USUARIOS (Nataly)
        rows.add(new String[]{"CU1 USUARIOS", "LISTARUSUARIOS", "[]", "Lista usuarios, roles y estados (Solo Administrador)"});
        rows.add(new String[]{"CU1 USUARIOS", "BUSCARUSUARIO", "[\"id_o_correo\"]", "Busca la ficha de un usuario por ID o correo (Admin)"});
        rows.add(new String[]{"CU1 USUARIOS", "CREARUSUARIO", "[\"nom\",\"correo\",\"pass\",\"rol\"]", "Registra nuevo usuario institucional (Admin)"});
        rows.add(new String[]{"CU1 USUARIOS", "MODIFICARUSUARIO", "[\"id\",\"nom\",\"correo\",\"rol\"]", "Actualiza datos institucionales de un usuario (Admin)"});
        rows.add(new String[]{"CU1 USUARIOS", "TOGGLEUSUARIO", "[\"id_usuario\"]", "Activa o desactiva usuario (Proteccion anti-autobloqueo)"});
        rows.add(new String[]{"CU1 USUARIOS", "TOGGLEROUNDROBIN", "[\"id_vendedor\"]", "Incluye o excluye vendedor del reparto automatico"});

        // 3. CU2: CAPTACION Y OFERTA (Jael)
        rows.add(new String[]{"CU2 CAPTACION", "REGISTRARPROSPECTO", "[\"nom\",\"telf\",\"mail\",\"origen\",\"prof\"]", "Registra un nuevo prospecto en etapa 'Nuevo'"});
        rows.add(new String[]{"CU2 CAPTACION", "VINCULARINTERES", "[\"id_lead\",\"tipo\",\"id_ext\",\"curso\"]", "Vincula interes academico (programa o modulo)"});
        rows.add(new String[]{"CU2 CAPTACION", "LISTARINTERESES", "[\"id_lead\"]", "Lista los intereses academicos de un prospecto"});
        rows.add(new String[]{"CU2 CAPTACION", "LISTARORIGENES", "[]", "Lista los canales de captacion (Facebook, Web, etc.)"});
        rows.add(new String[]{"CU2 CAPTACION", "LISTARPROFESIONES", "[]", "Lista el catalogo de profesiones registradas"});

        // 4. CU3: ASIGNACIONES (Nicol)
        rows.add(new String[]{"CU3 ASIGNACION", "BOLSACOMUN", "[]", "Lista prospectos no asignados en espera de atencion"});
        rows.add(new String[]{"CU3 ASIGNACION", "ASIGNARMANUAL", "[\"id_lead\",\"id_vendedor\"]", "Asigna prospecto directamente a un vendedor"});
        rows.add(new String[]{"CU3 ASIGNACION", "EJECUTARROUNDROBIN", "[]", "Distribuye equitativamente prospectos a vendedores activos"});
        rows.add(new String[]{"CU3 ASIGNACION", "TRANSFERIRLEAD", "[\"id_lead\",\"id_nuevo_vend\",\"motivo\"]", "Reasigna prospecto exigiendo motivo formal"});
        rows.add(new String[]{"CU3 ASIGNACION", "HISTORIALASIGNACIONES", "[\"id_lead\"]", "Muestra auditoria de traspasos del prospecto"});

        // 5. CU4: EMBUDO KANBAN (Nicol)
        rows.add(new String[]{"CU4 KANBAN", "VERKANBAN", "[] o [\"id_vendedor\"]", "Genera el Tablero Kanban con las 6 etapas"});
        rows.add(new String[]{"CU4 KANBAN", "LEADSETAPA", "[\"id_etapa\"]", "Lista los prospectos en una etapa especifica (1-6)"});
        rows.add(new String[]{"CU4 KANBAN", "MOVERETAPA", "[\"id_lead\",\"id_nueva_etapa\"]", "Avanza de etapa en el embudo (Audita en bitacora)"});
        rows.add(new String[]{"CU4 KANBAN", "MISLEADS", "[]", "Lista la cartera comercial del vendedor remitente"});

        // 6. CU5: MOTIVOS DE PERDIDA (Nicol)
        rows.add(new String[]{"CU5 PERDIDA", "LISTARMOTIVOSPERDIDA", "[]", "Lista el catalogo de motivos de desistimiento"});
        rows.add(new String[]{"CU5 PERDIDA", "MARCARPERDIDO", "[\"id_lead\",\"id_motivo\",\"comentario\"]", "Pasa a etapa 6 exigiendo motivo y comentario"});
        rows.add(new String[]{"CU5 PERDIDA", "LISTARPERDIDOS", "[]", "Lista prospectos perdidos con sus justificaciones"});

        // 7. CU6: MATRICULA Y PAGOS (Nataly)
        rows.add(new String[]{"CU6 PAGOS", "REGISTRARCOMPROBANTE", "[\"id_lead\",\"banco\",\"nro_trans\",\"monto\"]", "Registra comprobante de deposito bancario"});
        rows.add(new String[]{"CU6 PAGOS", "PAGOSPENDIENTES", "[]", "Lista comprobantes pendientes de verificacion"});
        rows.add(new String[]{"CU6 PAGOS", "APROBARPAGO", "[\"id_comprobante\"]", "Aprueba comprobante y habilita la matriculacion"});
        rows.add(new String[]{"CU6 PAGOS", "RECHAZARPAGO", "[\"id_comprobante\",\"motivo\"]", "Rechaza comprobante por inconsistencia"});
        rows.add(new String[]{"CU6 PAGOS", "COMPLETARDATOSALUMNO", "[\"id_lead\",\"ci\",\"fecha_nac\",\"ciudad\"]", "Registra datos formales de alumno"});
        rows.add(new String[]{"CU6 PAGOS", "CONVERTIRAMATRICULA", "[\"id_lead\"]", "Valida pago y datos, pasa a Convertido y emite constancia"});

        // 8. CU7: BITACORA Y RECORDATORIOS (Nataly)
        rows.add(new String[]{"CU7 BITACORA", "VERBITACORA", "[\"id_lead\"]", "Muestra timeline cronologica de eventos del prospecto"});
        rows.add(new String[]{"CU7 BITACORA", "AGREGARNOTA", "[\"id_lead\",\"contenido_nota\"]", "Registra nota de seguimiento comercial"});
        rows.add(new String[]{"CU7 BITACORA", "CREARRECORDATORIO", "[\"id_lead\",\"yyyy-MM-dd HH:mm\",\"desc\"]", "Programa recordatorio de llamada o tarea"});
        rows.add(new String[]{"CU7 BITACORA", "MISRECORDATORIOS", "[]", "Lista tareas y recordatorios pendientes del vendedor"});
        rows.add(new String[]{"CU7 BITACORA", "COMPLETARRECORDATORIO", "[\"id_recordatorio\"]", "Marca recordatorio como finalizado"});

        // 9. CU8: REPORTES Y ESTADISTICAS (Jael)
        rows.add(new String[]{"CU8 REPORTES", "REPORTEEMBUDO", "[]", "Volumen y porcentaje de prospectos por etapa"});
        rows.add(new String[]{"CU8 REPORTES", "REPORTECONVERSION", "[]", "Tasa de conversion global y por vendedor"});
        rows.add(new String[]{"CU8 REPORTES", "REPORTEORIGENES", "[]", "Distribucion de prospectos segun canal de captacion"});
        rows.add(new String[]{"CU8 REPORTES", "REPORTEMOTIVOSPERDIDA", "[]", "Estadisticas de principales motivos de abandono"});
        rows.add(new String[]{"CU8 REPORTES", "DASHBOARDVENDEDOR", "[]", "Panel de metricas personales del vendedor"});

        sb.append(HtmlTemplate.table(headers, rows));
        sb.append("<p style='font-size: 12px; color: #666666; margin-top: 8px;'>Para consultar la sintaxis y parametros de un comando particular, envie: <code>HELP[\"NOMBRE_COMANDO\"]</code>.</p>");

        String html = HtmlTemplate.wrapContainer("Catalogo Integral de Comandos del Sistema", "GUIA OFICIAL", sb.toString());
        return CommandResult.success(Config.SUBJECT_PREFIX + " Guia de Comandos (HELP)", html);
    }

    private CommandResult handleHelpDetallado(String comando) {
        String info;
        switch (comando) {
            case "CREARUSUARIO":
                info = "<p><b>Comando:</b> CREARUSUARIO</p>" +
                       "<p><b>Sintaxis:</b> <code>CREARUSUARIO[\"nombre_completo\", \"correo\", \"password\", \"rol\"]</code></p>" +
                       "<p><b>Roles permitidos:</b> Administrador</p>" +
                       "<p><b>Ejemplo:</b> <code>CREARUSUARIO[\"Mariana Suarez\", \"mariana@crm.bo\", \"secreto123\", \"Vendedor\"]</code></p>" +
                       "<p><b>Descripcion:</b> Crea una cuenta institucional. La contrasena se almacena con hash SHA-256. Si el rol es Vendedor, se activa automaticamente para Round-Robin.</p>";
                break;
            case "REGISTRARPROSPECTO":
                info = "<p><b>Comando:</b> REGISTRARPROSPECTO</p>" +
                       "<p><b>Sintaxis:</b> <code>REGISTRARPROSPECTO[\"nombre\", \"telefono\", \"correo\", \"origen\", \"profesion\"]</code></p>" +
                       "<p><b>Roles permitidos:</b> Administrador, Coordinador, Vendedor, Invitado</p>" +
                       "<p><b>Ejemplo:</b> <code>REGISTRARPROSPECTO[\"Carlos Andres\", \"70012345\", \"carlos@gmail.com\", \"Facebook Ads\", \"Ingeniero de Sistemas\"]</code></p>" +
                       "<p><b>Descripcion:</b> Da de alta un prospecto en etapa 1 (Nuevo) y lo coloca en la Bolsa Comun sin vendedor asignado.</p>";
                break;
            case "VERKANBAN":
                info = "<p><b>Comando:</b> VERKANBAN</p>" +
                       "<p><b>Sintaxis:</b> <code>VERKANBAN[]</code> o <code>VERKANBAN[\"id_vendedor\"]</code></p>" +
                       "<p><b>Roles permitidos:</b> Administrador, Coordinador, Vendedor</p>" +
                       "<p><b>Descripcion:</b> Renderiza el tablero comercial con las 6 etapas: 1. Nuevo, 2. En Contacto, 3. Seguimiento, 4. Promesa de Pago, 5. Convertido, 6. Perdido.</p>";
                break;
            case "CONVERTIRAMATRICULA":
                info = "<p><b>Comando:</b> CONVERTIRAMATRICULA</p>" +
                       "<p><b>Sintaxis:</b> <code>CONVERTIRAMATRICULA[\"id_lead\"]</code></p>" +
                       "<p><b>Roles permitidos:</b> Administrador, Coordinador</p>" +
                       "<p><b>Descripcion:</b> Verifica que el prospecto tenga comprobante de pago aprobado y datos de alumno completos (CI, fecha nacimiento, ciudad), pasandolo a etapa 5 (Convertido).</p>";
                break;
            default:
                info = "<p><b>Comando:</b> " + comando + "</p>" +
                       "<p>Envie <code>HELP[]</code> para consultar la tabla general con todos los comandos y parametros del sistema.</p>";
                break;
        }

        String html = HtmlTemplate.wrapContainer("Detalle de Comando: " + comando, "AYUDA", info);
        return CommandResult.success(Config.SUBJECT_PREFIX + " Ayuda: " + comando, html);
    }

    private CommandResult handleQuienSoy(String fromEmail) throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT u.id_usuario, u.nombre_completo, u.correo, r.nombre AS rol, u.activo, u.participa_round_robin " +
                         "FROM usuarios u JOIN roles r ON u.id_rol = r.id_rol WHERE LOWER(u.correo) = LOWER(?)";
            ps = conn.prepareStatement(sql);
            ps.setString(1, fromEmail.trim());
            rs = ps.executeQuery();

            if (rs.next()) {
                String nombre = rs.getString("nombre_completo");
                String rol = rs.getString("rol");
                boolean activo = rs.getBoolean("activo");
                boolean rr = rs.getBoolean("participa_round_robin");

                String info = "<p style='font-size: 14px; margin-top: 0;'><b>Identidad Verificada en PostgreSQL:</b></p>" +
                              "<table style='width: 100%; border-collapse: collapse; font-size: 13px;'>" +
                              "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 6px 0; font-weight: bold; width: 180px;'>Nombre Completo:</td><td>" + nombre + "</td></tr>" +
                              "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 6px 0; font-weight: bold;'>Correo Electronico:</td><td>" + fromEmail + "</td></tr>" +
                              "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 6px 0; font-weight: bold;'>Rol Institucional:</td><td><b>" + rol + "</b></td></tr>" +
                              "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 6px 0; font-weight: bold;'>Estado de Cuenta:</td><td>" + (activo ? "<span style='color: #1e7e34; font-weight: bold;'>[ACTIVO]</span>" : "<span style='color: #bd2130; font-weight: bold;'>[INACTIVO]</span>") + "</td></tr>" +
                              "  <tr style='border-bottom: 1px solid #eeeeee;'><td style='padding: 6px 0; font-weight: bold;'>Participa en Round-Robin:</td><td>" + (rr ? "Si" : "No") + "</td></tr>" +
                              "</table>";

                return CommandResult.success(Config.SUBJECT_PREFIX + " Identidad de Usuario", HtmlTemplate.wrapContainer("Perfil del Remitente", "AUTENTICACION", info));
            } else {
                String error = "<p>El correo remitente <b>" + fromEmail + "</b> no corresponde a ningun personal registrado en la base de datos institucional.</p>" +
                               "<p>Para registrarse como prospecto nuevo en la institucion, envie: <code>REGISTRARPROSPECTO[\"su_nombre\",\"su_telefono\",\"" + fromEmail + "\",\"Web\",\"Profesion\"]</code>.</p>";
                return CommandResult.success(Config.SUBJECT_PREFIX + " Usuario No Registrado", HtmlTemplate.wrapContainer("Usuario Externo", "INVITADO", error));
            }

        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }
    }

    // =========================================================================
    // DELEGACION MODULAR HACIA LOS SERVICIOS DE LOS 8 CASOS DE USO
    // =========================================================================

    private CommandResult handleModuloUsuarios(ParsedCommand cmd, String fromEmail) throws Exception {
        UsuarioAuth auth = SecurityContext.obtenerUsuario(fromEmail);
        switch (cmd.getName()) {
            case "LISTARUSUARIOS":
                return usuarioService.listarUsuarios(auth);
            case "BUSCARUSUARIO":
                return usuarioService.buscarUsuario(auth, cmd);
            case "CREARUSUARIO":
                return usuarioService.crearUsuario(auth, cmd);
            case "MODIFICARUSUARIO":
                return usuarioService.modificarUsuario(auth, cmd);
            case "TOGGLEUSUARIO":
                return usuarioService.toggleUsuario(auth, cmd);
            case "TOGGLEROUNDROBIN":
                return usuarioService.toggleRoundRobin(auth, cmd);
            default:
                throw new CommandException("Comando de usuario no implementado: " + cmd.getName());
        }
    }

    private CommandResult handleModuloCaptacion(ParsedCommand cmd, String fromEmail) throws Exception {
        UsuarioAuth auth = SecurityContext.obtenerUsuario(fromEmail);
        switch (cmd.getName()) {
            case "REGISTRARPROSPECTO":
                return leadService.registrarProspecto(auth, cmd);
            case "VINCULARINTERES":
                return leadService.vincularInteres(auth, cmd);
            case "LISTARINTERESES":
                return leadService.listarIntereses(auth, cmd);
            case "LISTARORIGENES":
                return leadService.listarOrigenes(auth);
            case "LISTARPROFESIONES":
                return leadService.listarProfesiones(auth);
            default:
                throw new CommandException("Comando de captacion no implementado: " + cmd.getName());
        }
    }

    private CommandResult handleModuloAsignacion(ParsedCommand cmd, String fromEmail) throws Exception {
        UsuarioAuth auth = SecurityContext.obtenerUsuario(fromEmail);
        switch (cmd.getName()) {
            case "BOLSACOMUN":
                return asignacionService.listarBolsaComun(auth);
            case "ASIGNARMANUAL":
                return asignacionService.asignarManual(auth, cmd);
            case "EJECUTARROUNDROBIN":
                return asignacionService.ejecutarRoundRobin(auth);
            case "TRANSFERIRLEAD":
                return asignacionService.transferirLead(auth, cmd);
            case "HISTORIALASIGNACIONES":
                return asignacionService.historialAsignaciones(auth, cmd);
            default:
                throw new CommandException("Comando de asignacion no implementado: " + cmd.getName());
        }
    }

    private CommandResult handleModuloKanban(ParsedCommand cmd, String fromEmail) throws Exception {
        UsuarioAuth auth = SecurityContext.obtenerUsuario(fromEmail);
        switch (cmd.getName()) {
            case "VERKANBAN":
            case "VEREMBUDO":
                return pipelineService.verKanban(auth, cmd);
            case "LEADSETAPA":
                return pipelineService.leadsEtapa(auth, cmd);
            case "MOVERETAPA":
                return pipelineService.moverEtapa(auth, cmd);
            case "MISLEADS":
                return pipelineService.misLeads(auth);
            default:
                throw new CommandException("Comando de kanban no implementado: " + cmd.getName());
        }
    }

    private CommandResult handleModuloPerdida(ParsedCommand cmd, String fromEmail) throws Exception {
        return CommandResult.success(Config.SUBJECT_PREFIX + " " + cmd.getName(),
            HtmlTemplate.successCard("Modulo Motivos de Perdida (CU5 - Nicol)", "Comando " + cmd.getName() + " reconocido con exito.", "<p>Parametros recibidos: " + cmd.getParams() + "</p>"));
    }

    private CommandResult handleModuloMatricula(ParsedCommand cmd, String fromEmail) throws Exception {
        return CommandResult.success(Config.SUBJECT_PREFIX + " " + cmd.getName(),
            HtmlTemplate.successCard("Modulo Conversion y Pagos (CU6 - Nataly)", "Comando " + cmd.getName() + " reconocido con exito.", "<p>Parametros recibidos: " + cmd.getParams() + "</p>"));
    }

    private CommandResult handleModuloBitacora(ParsedCommand cmd, String fromEmail) throws Exception {
        return CommandResult.success(Config.SUBJECT_PREFIX + " " + cmd.getName(),
            HtmlTemplate.successCard("Modulo Bitacora y Recordatorios (CU7 - Nataly)", "Comando " + cmd.getName() + " reconocido con exito.", "<p>Parametros recibidos: " + cmd.getParams() + "</p>"));
    }

    private CommandResult handleModuloReportes(ParsedCommand cmd, String fromEmail) throws Exception {
        return CommandResult.success(Config.SUBJECT_PREFIX + " " + cmd.getName(),
            HtmlTemplate.successCard("Modulo Reportes y Estadisticas (CU8 - Jael)", "Comando " + cmd.getName() + " reconocido con exito.", "<p>Parametros recibidos: " + cmd.getParams() + "</p>"));
    }

    // =========================================================================
    // DETECCION DE ERRORES TIPOGRAFICOS (Distancia Levenshtein)
    // =========================================================================

    private String sugerirComandoCercano(String input) {
        if (input == null || input.trim().isEmpty()) return null;
        String normalized = input.trim().toUpperCase();

        String bestMatch = null;
        int minDistance = Integer.MAX_VALUE;

        for (String valid : COMANDOS_VALIDOS) {
            int dist = calcularLevenshtein(normalized, valid);
            if (dist < minDistance && dist <= 3) {
                minDistance = dist;
                bestMatch = valid;
            }
        }
        return bestMatch;
    }

    private static int calcularLevenshtein(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[s1.length()][s2.length()];
    }
}
