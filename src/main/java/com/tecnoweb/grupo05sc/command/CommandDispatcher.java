package com.tecnoweb.grupo05sc.command;

import com.tecnoweb.grupo05sc.config.Config;
import com.tecnoweb.grupo05sc.config.DatabaseConnection;
import com.tecnoweb.grupo05sc.view.HtmlTemplate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Enruta los comandos parseados hacia la lógica de negocio correspondiente.
 * Incluye soporte inmediato para:
 *   1. Ejemplos oficiales del docente: LISPER["*"] e INSPER[...]
 *   2. Sistema de Ayuda Dinámica: HELP[] y HELP["COMANDO"]
 *   3. Identificación del usuario: QUIENSOY[]
 *   4. Delegación modular para los 8 Casos de Uso del CRM Educativo.
 */
public final class CommandDispatcher {

    public CommandResult dispatch(String fromEmail, String subject) {
        String cleanSubject = (subject != null) ? subject.trim() : "";

        try {
            ParsedCommand cmd = CommandParser.parse(cleanSubject);
            System.out.println(">>> [DISPATCHER] Procesando comando: " + cmd.getName() + " con " + cmd.paramCount() + " parámetros. Emisor: " + fromEmail);

            switch (cmd.getName()) {
                // =============================================================
                // 1. COMANDOS DE PRUEBA OFICIALES DEL DOCENTE (LISPER, INSPER)
                // =============================================================
                case "LISPER":
                    return handleLisper(cmd);

                case "INSPER":
                    return handleInsper(cmd);

                // =============================================================
                // 2. COMANDOS GENERALES Y AYUDA (HELP, QUIENSOY)
                // =============================================================
                case "HELP":
                case "AYUDA":
                    return handleHelp(cmd, fromEmail);

                case "QUIENSOY":
                    return handleQuienSoy(fromEmail);

                // =============================================================
                // 3. CASOS DE USO CRM EDUCATIVO (Delegación modular)
                // =============================================================

                // CU1: Gestión de Usuarios (Nataly)
                case "LISTARUSUARIOS":
                case "BUSCARUSUARIO":
                case "CREARUSUARIO":
                case "MODIFICARUSUARIO":
                case "TOGGLEUSUARIO":
                case "TOGGLEROUNDROBIN":
                    return handleModuloUsuarios(cmd, fromEmail);

                // CU2: Captación de Leads y Oferta (Jael)
                case "REGISTRARPROSPECTO":
                case "VINCULARINTERES":
                case "LISTARINTERESES":
                case "LISTARORIGENES":
                case "LISTARPROFESIONES":
                    return handleModuloCaptacion(cmd, fromEmail);

                // CU3: Asignación de Leads (Nicol)
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

                // CU5: Motivos de Pérdida (Nicol)
                case "LISTARMOTIVOSPERDIDA":
                case "MARCARPERDIDO":
                case "LISTARPERDIDOS":
                    return handleModuloPerdida(cmd, fromEmail);

                // CU6: Pagos y Matrícula (Nataly)
                case "REGISTRARCOMPROBANTE":
                case "PAGOSPENDIENTES":
                case "APROBARPAGO":
                case "RECHAZARPAGO":
                case "COMPLETARDATOSALUMNO":
                case "CONVERTIRAMATRICULA":
                    return handleModuloMatricula(cmd, fromEmail);

                // CU7: Bitácora y Recordatorios (Nataly)
                case "VERBITACORA":
                case "AGREGARNOTA":
                case "CREARRECORDATORIO":
                case "MISRECORDATORIOS":
                case "COMPLETARRECORDATORIO":
                    return handleModuloBitacora(cmd, fromEmail);

                // CU8: Reportes y Estadísticas (Jael)
                case "REPORTEEMBUDO":
                case "REPORTECONVERSION":
                case "REPORTEORIGENES":
                case "REPORTEMOTIVOSPERDIDA":
                case "DASHBOARDVENDEDOR":
                    return handleModuloReportes(cmd, fromEmail);

                default:
                    throw new CommandException(
                        "El comando '" + cmd.getName() + "' no existe en el sistema.",
                        "Envíe HELP[] para ver la lista de comandos disponibles."
                    );
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
                "Ocurrió un error inesperado al procesar la solicitud: " + e.getMessage(),
                "Verifique los parámetros enviados o contacte al administrador del sistema."
            );
            return CommandResult.error(respSubject, errorHtml);
        }
    }

    // =========================================================================
    // HANDLERS OFICIALES: LISPER E INSPER (Orden docente Evans Balcázar)
    // =========================================================================

    private CommandResult handleLisper(ParsedCommand cmd) throws Exception {
        String patron = (cmd.hasParams()) ? cmd.getParam(0).trim() : "*";
        boolean buscarTodos = patron.equals("*") || patron.isEmpty();

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            String sql;
            if (buscarTodos) {
                sql = "SELECT per_cod, per_nom, per_appm, per_prof, per_telf, per_cel, per_email FROM persona ORDER BY per_cod ASC";
                ps = conn.prepareStatement(sql);
            } else {
                sql = "SELECT per_cod, per_nom, per_appm, per_prof, per_telf, per_cel, per_email FROM persona " +
                      "WHERE per_cod ILIKE ? OR per_nom ILIKE ? OR per_appm ILIKE ? OR per_prof ILIKE ? OR per_email ILIKE ? " +
                      "ORDER BY per_cod ASC";
                ps = conn.prepareStatement(sql);
                String likeVal = "%" + patron + "%";
                for (int i = 1; i <= 5; i++) {
                    ps.setString(i, likeVal);
                }
            }

            rs = ps.executeQuery();
            List<String[]> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(new String[]{
                    rs.getString("per_cod"),
                    rs.getString("per_nom"),
                    rs.getString("per_appm"),
                    rs.getString("per_prof"),
                    rs.getString("per_telf"),
                    rs.getString("per_cel"),
                    rs.getString("per_email")
                });
            }

            String[] headers = new String[]{"CÓDIGO", "NOMBRE", "APELLIDOS", "PROFESIÓN", "TELÉFONO", "CELULAR", "EMAIL"};
            String tableHtml = HtmlTemplate.table(headers, rows);
            String summary = "<p style='font-size: 13px; color: #6a737b;'>Filtro aplicado: <b>\"" + patron + "\"</b> | Total encontrados: <b>" + rows.size() + "</b></p>";
            String html = HtmlTemplate.wrapContainer("Listado de Personas Registradas", "CONSULTA", summary + tableHtml);

            return CommandResult.success(Config.SUBJECT_PREFIX + " Listando las Personas", html);

        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }
    }

    private CommandResult handleInsper(ParsedCommand cmd) throws Exception {
        // Formato oficial: INSPER["ci","nombre","apellido","profesion","telefono","celular","email"]
        if (cmd.paramCount() < 7) {
            throw new CommandException(
                "Cantidad insuficiente de parámetros para INSPER. Se requieren 7 parámetros.",
                "Sintaxis: INSPER[\"ci\",\"nombre\",\"apellido\",\"profesion\",\"telefono\",\"celular\",\"email\"]"
            );
        }

        String ci = cmd.getParam(0);
        String nombre = cmd.getParam(1);
        String apellido = cmd.getParam(2);
        String profesion = cmd.getParam(3);
        String telefono = cmd.getParam(4);
        String celular = cmd.getParam(5);
        String email = cmd.getParam(6);

        if (ci.isEmpty() || nombre.isEmpty()) {
            throw new CommandException("Los campos CI y Nombre son obligatorios.", "Asegúrese de enviar datos válidos.");
        }

        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DatabaseConnection.getConnection();
            String sql = "INSERT INTO persona (per_cod, per_nom, per_appm, per_prof, per_telf, per_cel, per_email) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                         "ON CONFLICT (per_cod) DO UPDATE SET " +
                         "per_nom = EXCLUDED.per_nom, per_appm = EXCLUDED.per_appm, per_prof = EXCLUDED.per_prof, " +
                         "per_telf = EXCLUDED.per_telf, per_cel = EXCLUDED.per_cel, per_email = EXCLUDED.per_email";

            ps = conn.prepareStatement(sql);
            ps.setString(1, ci);
            ps.setString(2, nombre);
            ps.setString(3, apellido);
            ps.setString(4, profesion);
            ps.setString(5, telefono);
            ps.setString(6, celular);
            ps.setString(7, email);

            ps.executeUpdate();

            String details = "<p style='font-size: 13px;'><b>Datos registrados:</b><br>" +
                             "• <b>CI:</b> " + ci + "<br>" +
                             "• <b>Nombre completo:</b> " + nombre + " " + apellido + "<br>" +
                             "• <b>Profesión:</b> " + profesion + "<br>" +
                             "• <b>Contacto:</b> " + telefono + " / " + celular + "<br>" +
                             "• <b>Correo:</b> " + email + "</p>";

            String html = HtmlTemplate.successCard("Registro de Persona", "La persona fue insertada satisfactoriamente en la base de datos.", details);
            return CommandResult.success(Config.SUBJECT_PREFIX + " Inserción Exitosa (INSPER)", html);

        } finally {
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }
    }

    // =========================================================================
    // AYUDA DINÁMICA E IDENTIDAD (HELP, QUIENSOY)
    // =========================================================================

    private CommandResult handleHelp(ParsedCommand cmd, String fromEmail) {
        StringBuilder sb = new StringBuilder();
        sb.append("<p style='font-size: 14px;'>Bienvenido al sistema automatizado del <b>CRM Educativo (Grupo 05 SC)</b>.</p>");
        sb.append("<p style='font-size: 13px; color: #6a737b;'>Los comandos se envían en el <b>Asunto</b> del correo con el formato: <code>COMANDO[\"param1\", \"param2\"]</code>.</p>");

        String[] headers = new String[]{"COMANDO", "PARÁMETROS", "DESCRIPCIÓN"};
        List<String[]> rows = new ArrayList<>();

        // Pruebas Docente
        rows.add(new String[]{"LISPER", "[\"*\"] o [\"patron\"]", "Lista personas registradas (Ejemplo del docente)"});
        rows.add(new String[]{"INSPER", "[\"ci\",\"nom\",\"app\",\"prof\",\"telf\",\"cel\",\"mail\"]", "Inserta persona en la base de datos"});

        // Generales
        rows.add(new String[]{"HELP", "[] o [\"COMANDO\"]", "Muestra esta guía o el detalle de un comando específico"});
        rows.add(new String[]{"QUIENSOY", "[]", "Muestra información de la cuenta asociada a su correo remitente"});

        // CU1 - Usuarios
        rows.add(new String[]{"LISTARUSUARIOS", "[]", "Lista usuarios, roles y estados (Solo Administrador)"});
        rows.add(new String[]{"CREARUSUARIO", "[\"nom\",\"correo\",\"pass\",\"rol\"]", "Registra nuevo usuario institucional"});
        rows.add(new String[]{"TOGGLEUSUARIO", "[\"id_usuario\"]", "Activa/Desactiva usuario (Protección anti-autobloqueo)"});

        // CU2 - Captación
        rows.add(new String[]{"REGISTRARPROSPECTO", "[\"nom\",\"telf\",\"mail\",\"origen\",\"prof\"]", "Registra un nuevo prospecto en etapa 'Nuevo'"});
        rows.add(new String[]{"VINCULARINTERES", "[\"id_lead\",\"tipo\",\"id_ext\",\"nombre_curso\"]", "Vincula interés de programa o módulo"});

        // CU3 - Asignaciones
        rows.add(new String[]{"BOLSACOMUN", "[]", "Lista prospectos no asignados en espera de vendedor"});
        rows.add(new String[]{"EJECUTARROUNDROBIN", "[]", "Distribuye equitativamente leads de bolsa a vendedores activos"});
        rows.add(new String[]{"TRANSFERIRLEAD", "[\"id_lead\",\"id_nuevo_vend\",\"motivo\"]", "Reasigna prospecto exigiendo motivo formal"});

        // CU4 - Kanban
        rows.add(new String[]{"VERKANBAN", "[] o [\"id_vendedor\"]", "Genera el Tablero Kanban visual con las 6 etapas"});
        rows.add(new String[]{"MOVERETAPA", "[\"id_lead\",\"id_nueva_etapa\"]", "Avanza de etapa en el ciclo comercial (Audita en bitácora)"});

        // CU5 - Motivos Pérdida
        rows.add(new String[]{"LISTARMOTIVOSPERDIDA", "[]", "Lista el catálogo de motivos de desistimiento"});
        rows.add(new String[]{"MARCARPERDIDO", "[\"id_lead\",\"id_motivo\",\"comentario\"]", "Pasa a etapa 6 exigiendo motivo y comentario obligatorio"});

        // CU6 - Matrícula y Pagos
        rows.add(new String[]{"PAGOSPENDIENTES", "[]", "Lista comprobantes de depósito pendientes de revisión"});
        rows.add(new String[]{"APROBARPAGO", "[\"id_comprobante\"]", "Aprueba comprobante y habilita conversión"});
        rows.add(new String[]{"CONVERTIRAMATRICULA", "[\"id_lead\"]", "Valida pago y datos alumno, pasa a Convertido y despacha"});

        // CU7 - Bitácora y Recordatorios
        rows.add(new String[]{"VERBITACORA", "[\"id_lead\"]", "Muestra timeline cronológica de eventos del prospecto"});
        rows.add(new String[]{"CREARRECORDATORIO", "[\"id_lead\",\"yyyy-MM-dd HH:mm\",\"desc\"]", "Programa alerta de tarea para vendedor"});

        // CU8 - Reportes
        rows.add(new String[]{"REPORTEEMBUDO", "[]", "Muestra volumen y porcentaje de leads en cada etapa"});
        rows.add(new String[]{"REPORTECONVERSION", "[]", "Muestra tasa de conversión global y por vendedor"});

        sb.append(HtmlTemplate.table(headers, rows));
        String html = HtmlTemplate.wrapContainer("Catálogo de Comandos del Sistema", "GUÍA OFICIAL", sb.toString());
        return CommandResult.success(Config.SUBJECT_PREFIX + " Guía de Comandos (HELP)", html);
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

                String info = "<p style='font-size: 14px;'><b>Identidad Verificada:</b></p>" +
                              "<ul>" +
                              "  <li><b>Nombre:</b> " + nombre + "</li>" +
                              "  <li><b>Correo:</b> " + fromEmail + "</li>" +
                              "  <li><b>Rol Institucional:</b> <span style='background:#ebf0f9; padding:2px 8px; border-radius:4px; font-weight:bold;'>" + rol + "</span></li>" +
                              "  <li><b>Estado de Cuenta:</b> " + (activo ? "<span style='color:#28a745; font-weight:bold;'>Activo</span>" : "<span style='color:#dc3545;'>Inactivo</span>") + "</li>" +
                              "  <li><b>Participa en Round-Robin:</b> " + (rr ? "Sí" : "No") + "</li>" +
                              "</ul>";

                return CommandResult.success(Config.SUBJECT_PREFIX + " Identidad de Usuario", HtmlTemplate.wrapContainer("Perfil del Remitente", "AUTENTICACIÓN", info));
            } else {
                String error = "<p>El correo remitente <b>" + fromEmail + "</b> no corresponde a ningún personal registrado en la base de datos.</p>" +
                               "<p>Para registrarse como prospecto nuevo, envíe <code>REGISTRARME[\"su_nombre\",\"su_telefono\"]</code>.</p>";
                return CommandResult.success(Config.SUBJECT_PREFIX + " Usuario No Registrado", HtmlTemplate.wrapContainer("Usuario Externo", "INVITADO", error));
            }

        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }
    }

    // =========================================================================
    // DELEGACIÓN MODULAR HACIA LOS SERVICIOS DE LOS 8 CASOS DE USO
    // =========================================================================

    private CommandResult handleModuloUsuarios(ParsedCommand cmd, String fromEmail) throws Exception {
        // Se implementa en UsuarioService.java (Nataly)
        if (cmd.getName().equals("LISTARUSUARIOS")) {
            return listarUsuariosEnBD();
        }
        return CommandResult.success(Config.SUBJECT_PREFIX + " " + cmd.getName(),
            HtmlTemplate.successCard("Módulo Usuarios (CU1 - Nataly)", "Comando " + cmd.getName() + " reconocido con éxito.", "<p>Parámetros recibidos: " + cmd.getParams() + "</p>"));
    }

    private CommandResult handleModuloCaptacion(ParsedCommand cmd, String fromEmail) throws Exception {
        // Se implementa en LeadService.java (Jael)
        return CommandResult.success(Config.SUBJECT_PREFIX + " " + cmd.getName(),
            HtmlTemplate.successCard("Módulo Captación (CU2 - Jael)", "Comando " + cmd.getName() + " reconocido con éxito.", "<p>Parámetros recibidos: " + cmd.getParams() + "</p>"));
    }

    private CommandResult handleModuloAsignacion(ParsedCommand cmd, String fromEmail) throws Exception {
        // Se implementa en AsignacionService.java (Nicol)
        if (cmd.getName().equals("BOLSACOMUN")) {
            return listarBolsaComunEnBD();
        }
        return CommandResult.success(Config.SUBJECT_PREFIX + " " + cmd.getName(),
            HtmlTemplate.successCard("Módulo Asignaciones (CU3 - Nicol)", "Comando " + cmd.getName() + " reconocido con éxito.", "<p>Parámetros recibidos: " + cmd.getParams() + "</p>"));
    }

    private CommandResult handleModuloKanban(ParsedCommand cmd, String fromEmail) throws Exception {
        // Se implementa en PipelineService.java (Nicol)
        if (cmd.getName().equals("VERKANBAN") || cmd.getName().equals("VEREMBUDO")) {
            return renderizarKanbanEnBD();
        }
        return CommandResult.success(Config.SUBJECT_PREFIX + " " + cmd.getName(),
            HtmlTemplate.successCard("Módulo Kanban (CU4 - Nicol)", "Comando " + cmd.getName() + " reconocido con éxito.", "<p>Parámetros recibidos: " + cmd.getParams() + "</p>"));
    }

    private CommandResult handleModuloPerdida(ParsedCommand cmd, String fromEmail) throws Exception {
        // Se implementa en PerdidaService.java (Nicol)
        return CommandResult.success(Config.SUBJECT_PREFIX + " " + cmd.getName(),
            HtmlTemplate.successCard("Módulo Motivos de Pérdida (CU5 - Nicol)", "Comando " + cmd.getName() + " reconocido con éxito.", "<p>Parámetros recibidos: " + cmd.getParams() + "</p>"));
    }

    private CommandResult handleModuloMatricula(ParsedCommand cmd, String fromEmail) throws Exception {
        // Se implementa en ConversionMatriculaService.java (Nataly)
        return CommandResult.success(Config.SUBJECT_PREFIX + " " + cmd.getName(),
            HtmlTemplate.successCard("Módulo Conversión y Pagos (CU6 - Nataly)", "Comando " + cmd.getName() + " reconocido con éxito.", "<p>Parámetros recibidos: " + cmd.getParams() + "</p>"));
    }

    private CommandResult handleModuloBitacora(ParsedCommand cmd, String fromEmail) throws Exception {
        // Se implementa en BitacoraService.java (Nataly)
        return CommandResult.success(Config.SUBJECT_PREFIX + " " + cmd.getName(),
            HtmlTemplate.successCard("Módulo Bitácora y Recordatorios (CU7 - Nataly)", "Comando " + cmd.getName() + " reconocido con éxito.", "<p>Parámetros recibidos: " + cmd.getParams() + "</p>"));
    }

    private CommandResult handleModuloReportes(ParsedCommand cmd, String fromEmail) throws Exception {
        // Se implementa en ReporteService.java (Jael)
        return CommandResult.success(Config.SUBJECT_PREFIX + " " + cmd.getName(),
            HtmlTemplate.successCard("Módulo Reportes y Estadísticas (CU8 - Jael)", "Comando " + cmd.getName() + " reconocido con éxito.", "<p>Parámetros recibidos: " + cmd.getParams() + "</p>"));
    }

    // =========================================================================
    // MÉTODOS DE CONSULTA DIRECTA A LA BASE DE DATOS LOCAL EXISTENTE
    // =========================================================================

    private CommandResult listarUsuariosEnBD() throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement("SELECT u.id_usuario, u.nombre_completo, u.correo, r.nombre as rol, u.activo, u.participa_round_robin " +
                                       "FROM usuarios u JOIN roles r ON u.id_rol = r.id_rol ORDER BY u.id_usuario ASC");
            rs = ps.executeQuery();
            List<String[]> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(new String[]{
                    String.valueOf(rs.getInt("id_usuario")),
                    rs.getString("nombre_completo"),
                    rs.getString("correo"),
                    rs.getString("rol"),
                    rs.getBoolean("activo") ? "Activo" : "Inactivo",
                    rs.getBoolean("participa_round_robin") ? "Sí" : "No"
                });
            }
            String[] headers = new String[]{"ID", "NOMBRE COMPLETO", "CORREO", "ROL", "ESTADO", "ROUND-ROBIN"};
            String html = HtmlTemplate.wrapContainer("Usuarios Registrados en el Sistema", "CU1", HtmlTemplate.table(headers, rows));
            return CommandResult.success(Config.SUBJECT_PREFIX + " Listado de Usuarios", html);
        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }
    }

    private CommandResult listarBolsaComunEnBD() throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement("SELECT l.id_lead, l.nombre, l.telefono, l.correo, o.nombre as origen, l.created_at " +
                                       "FROM leads l JOIN origenes_lead o ON l.id_origen = o.id_origen " +
                                       "WHERE l.id_vendedor IS NULL ORDER BY l.created_at ASC");
            rs = ps.executeQuery();
            List<String[]> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(new String[]{
                    String.valueOf(rs.getInt("id_lead")),
                    rs.getString("nombre"),
                    rs.getString("telefono"),
                    rs.getString("correo") != null ? rs.getString("correo") : "-",
                    rs.getString("origen"),
                    rs.getString("created_at")
                });
            }
            String[] headers = new String[]{"ID", "PROSPECTO", "TELÉFONO", "CORREO", "CANAL ORIGEN", "REGISTRADO"};
            String html = HtmlTemplate.wrapContainer("Bolsa Común (Prospectos No Asignados)", "CU3", HtmlTemplate.table(headers, rows));
            return CommandResult.success(Config.SUBJECT_PREFIX + " Bolsa Común de Leads", html);
        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }
    }

    private CommandResult renderizarKanbanEnBD() throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT e.id_etapa, e.nombre as etapa, COUNT(l.id_lead) as total " +
                         "FROM etapas_pipeline e LEFT JOIN leads l ON e.id_etapa = l.id_etapa " +
                         "GROUP BY e.id_etapa, e.nombre, e.orden ORDER BY e.orden ASC";
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            List<String[]> rows = new ArrayList<>();
            while (rs.next()) {
                int idEtapa = rs.getInt("id_etapa");
                String nombreEtapa = rs.getString("etapa");
                int total = rs.getInt("total");
                rows.add(new String[]{
                    String.valueOf(idEtapa),
                    HtmlTemplate.badgeEtapa(idEtapa, nombreEtapa),
                    "<b>" + total + " leads</b>"
                });
            }
            String[] headers = new String[]{"#", "ETAPA DEL EMBUDO", "TOTAL EN ETAPA"};
            String html = HtmlTemplate.wrapContainer("Resumen Tablero Kanban (Pipeline Comercial)", "CU4", HtmlTemplate.table(headers, rows));
            return CommandResult.success(Config.SUBJECT_PREFIX + " Tablero Kanban", html);
        } finally {
            DatabaseConnection.close(rs);
            DatabaseConnection.close(ps);
            DatabaseConnection.close(conn);
        }
    }
}
