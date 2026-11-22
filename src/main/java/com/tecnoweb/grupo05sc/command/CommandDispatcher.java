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
    private final com.tecnoweb.grupo05sc.service.MotivoPerdidaService motivoService = new com.tecnoweb.grupo05sc.service.MotivoPerdidaService();
    private final com.tecnoweb.grupo05sc.service.PagoMatriculaService pagoService = new com.tecnoweb.grupo05sc.service.PagoMatriculaService();
    private final com.tecnoweb.grupo05sc.service.BitacoraService bitacoraService = new com.tecnoweb.grupo05sc.service.BitacoraService();
    private final com.tecnoweb.grupo05sc.service.ReporteService reporteService = new com.tecnoweb.grupo05sc.service.ReporteService();

    private static final List<String> COMANDOS_VALIDOS = Arrays.asList(
        "HELP", "AYUDA", "QUIENSOY",
        "LISTARUSUARIOS", "BUSCARUSUARIO", "CREARUSUARIO", "MODIFICARUSUARIO", "TOGGLEUSUARIO", "TOGGLEROUNDROBIN",
        "REGISTRARPROSPECTO", "VINCULARINTERES", "LISTARINTERESES", "LISTARORIGENES", "LISTARPROFESIONES", "LISTAROFERTA",
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

                // Modulo: Captacion de Leads y Oferta
                case "REGISTRARPROSPECTO":
                case "VINCULARINTERES":
                case "LISTARINTERESES":
                case "LISTARORIGENES":
                case "LISTARPROFESIONES":
                case "LISTAROFERTA":
                    return handleModuloCaptacion(cmd, fromEmail);

                // Modulo: Asignacion de Leads
                case "BOLSACOMUN":
                case "ASIGNARMANUAL":
                case "EJECUTARROUNDROBIN":
                case "TRANSFERIRLEAD":
                case "HISTORIALASIGNACIONES":
                    return handleModuloAsignacion(cmd, fromEmail);

                // Modulo: Embudo de Ventas Kanban
                case "VERKANBAN":
                case "VEREMBUDO":
                case "LEADSETAPA":
                case "MOVERETAPA":
                case "MISLEADS":
                    return handleModuloKanban(cmd, fromEmail);

                // Modulo: Motivos de Perdida
                case "LISTARMOTIVOSPERDIDA":
                case "MARCARPERDIDO":
                case "LISTARPERDIDOS":
                    return handleModuloPerdida(cmd, fromEmail);

                // Modulo: Pagos y Matricula
                case "REGISTRARCOMPROBANTE":
                case "PAGOSPENDIENTES":
                case "APROBARPAGO":
                case "RECHAZARPAGO":
                case "COMPLETARDATOSALUMNO":
                case "CONVERTIRAMATRICULA":
                    return handleModuloMatricula(cmd, fromEmail);

                // Modulo: Bitacora y Recordatorios
                case "VERBITACORA":
                case "AGREGARNOTA":
                case "CREARRECORDATORIO":
                case "MISRECORDATORIOS":
                case "COMPLETARRECORDATORIO":
                    return handleModuloBitacora(cmd, fromEmail);

                // Modulo: Reportes y Estadisticas
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

        // 2. GESTION DE USUARIOS
        rows.add(new String[]{"USUARIOS", "LISTARUSUARIOS", "[]", "Lista usuarios, roles y estados (Solo Administrador)"});
        rows.add(new String[]{"USUARIOS", "BUSCARUSUARIO", "[\"id_o_correo\"]", "Busca la ficha de un usuario por ID o correo (Admin)"});
        rows.add(new String[]{"USUARIOS", "CREARUSUARIO", "[\"nom\",\"correo\",\"pass\",\"rol\"]", "Registra nuevo usuario institucional (Admin)"});
        rows.add(new String[]{"USUARIOS", "MODIFICARUSUARIO", "[\"id\",\"nom\",\"correo\",\"rol\"]", "Actualiza datos institucionales de un usuario (Admin)"});
        rows.add(new String[]{"USUARIOS", "TOGGLEUSUARIO", "[\"id_usuario\"]", "Activa o desactiva usuario (Proteccion anti-autobloqueo)"});
        rows.add(new String[]{"USUARIOS", "TOGGLEROUNDROBIN", "[\"id_vendedor\"]", "Incluye o excluye vendedor del reparto automatico"});

        // 3. CAPTACION Y OFERTA ACADEMICA
        rows.add(new String[]{"PROSPECTOS", "REGISTRARPROSPECTO", "[\"nom\",\"telf\",\"mail\",\"origen\",\"prof\"]", "Registra un nuevo prospecto en etapa 'Nuevo'"});
        rows.add(new String[]{"OFERTA", "LISTAROFERTA", "[]", "Catalogo oficial de programas y modulos ofertados"});
        rows.add(new String[]{"OFERTA", "VINCULARINTERES", "[\"id_lead\",\"tipo\",\"id_ext\",\"curso\"]", "Vincula interes academico (programa o modulo)"});
        rows.add(new String[]{"OFERTA", "LISTARINTERESES", "[\"id_lead\"]", "Lista los intereses academicos de un prospecto"});
        rows.add(new String[]{"CATALOGO", "LISTARORIGENES", "[]", "Lista los canales de captacion (Facebook, Web, etc.)"});
        rows.add(new String[]{"CATALOGO", "LISTARPROFESIONES", "[]", "Lista el catalogo de profesiones registradas"});

        // 4. ASIGNACIONES Y FUERZA DE VENTAS
        rows.add(new String[]{"ASIGNACION", "BOLSACOMUN", "[]", "Lista prospectos no asignados en espera de atencion"});
        rows.add(new String[]{"ASIGNACION", "ASIGNARMANUAL", "[\"id_lead\",\"id_vendedor\"]", "Asigna prospecto directamente a un vendedor (Admin)"});
        rows.add(new String[]{"ASIGNACION", "EJECUTARROUNDROBIN", "[]", "Distribuye equitativamente prospectos a vendedores activos (Admin)"});
        rows.add(new String[]{"ASIGNACION", "TRANSFERIRLEAD", "[\"id_lead\",\"id_nuevo_vend\",\"motivo\"]", "Reasigna prospecto exigiendo motivo formal (Admin)"});
        rows.add(new String[]{"ASIGNACION", "HISTORIALASIGNACIONES", "[\"id_lead\"]", "Muestra auditoria de traspasos del prospecto"});

        // 5. EMBUDO DE VENTAS KANBAN
        rows.add(new String[]{"KANBAN", "VERKANBAN", "[] o [\"id_vendedor\"]", "Genera el Tablero Kanban con las 6 etapas"});
        rows.add(new String[]{"KANBAN", "LEADSETAPA", "[\"id_etapa\"]", "Lista los prospectos en una etapa especifica (1-6)"});
        rows.add(new String[]{"KANBAN", "MOVERETAPA", "[\"id_lead\",\"id_nueva_etapa\"]", "Avanza de etapa en el embudo (Audita en bitacora)"});
        rows.add(new String[]{"KANBAN", "MISLEADS", "[]", "Lista la cartera comercial del vendedor remitente"});

        // 6. GESTION DE PERDIDA Y DESISTIMIENTO
        rows.add(new String[]{"PERDIDA", "LISTARMOTIVOSPERDIDA", "[]", "Lista el catalogo de motivos de desistimiento"});
        rows.add(new String[]{"PERDIDA", "MARCARPERDIDO", "[\"id_lead\",\"id_motivo\",\"comentario\"]", "Pasa a etapa 6 exigiendo motivo y comentario"});
        rows.add(new String[]{"PERDIDA", "LISTARPERDIDOS", "[]", "Lista prospectos perdidos con sus justificaciones"});

        // 7. PAGOS Y MATRICULACION
        rows.add(new String[]{"PAGOS", "REGISTRARCOMPROBANTE", "[\"id_lead\",\"banco\",\"nro_trans\",\"monto\"]", "Registra comprobante de deposito bancario"});
        rows.add(new String[]{"PAGOS", "PAGOSPENDIENTES", "[]", "Lista comprobantes pendientes de verificacion (Admin)"});
        rows.add(new String[]{"PAGOS", "APROBARPAGO", "[\"id_comprobante\"]", "Aprueba comprobante y habilita la matriculacion (Admin)"});
        rows.add(new String[]{"PAGOS", "RECHAZARPAGO", "[\"id_comprobante\",\"motivo\"]", "Rechaza comprobante por inconsistencia (Admin)"});
        rows.add(new String[]{"MATRICULA", "COMPLETARDATOSALUMNO", "[\"id_lead\",\"ci\",\"fecha_nac\",\"ciudad\"]", "Registra datos formales de alumno"});
        rows.add(new String[]{"MATRICULA", "CONVERTIRAMATRICULA", "[\"id_lead\"]", "Valida pago y datos, pasa a Convertido y emite constancia (Admin)"});

        // 8. BITACORA Y RECORDATORIOS
        rows.add(new String[]{"BITACORA", "VERBITACORA", "[\"id_lead\"]", "Muestra timeline cronologica de eventos del prospecto"});
        rows.add(new String[]{"BITACORA", "AGREGARNOTA", "[\"id_lead\",\"contenido_nota\"]", "Registra nota de seguimiento comercial"});
        rows.add(new String[]{"BITACORA", "CREARRECORDATORIO", "[\"id_lead\",\"yyyy-MM-dd HH:mm\",\"desc\"]", "Programa recordatorio de llamada o tarea"});
        rows.add(new String[]{"BITACORA", "MISRECORDATORIOS", "[]", "Lista tareas y recordatorios pendientes del vendedor"});
        rows.add(new String[]{"BITACORA", "COMPLETARRECORDATORIO", "[\"id_recordatorio\"]", "Marca recordatorio como finalizado"});

        // 9. REPORTES ESTADISTICOS
        rows.add(new String[]{"REPORTES", "REPORTEEMBUDO", "[]", "Volumen y porcentaje de prospectos por etapa"});
        rows.add(new String[]{"REPORTES", "REPORTECONVERSION", "[]", "Tasa de conversion global y por vendedor"});
        rows.add(new String[]{"REPORTES", "REPORTEORIGENES", "[]", "Distribucion de prospectos segun canal de captacion"});
        rows.add(new String[]{"REPORTES", "REPORTEMOTIVOSPERDIDA", "[]", "Estadisticas de principales motivos de abandono"});
        rows.add(new String[]{"REPORTES", "DASHBOARDVENDEDOR", "[]", "Panel de metricas personales del vendedor"});

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
                       "<p><b>Validaciones:</b> Telefono de 7 u 8 digitos (celulares inician con 6 o 7). Canal de origen validado contra catalogo.</p>" +
                       "<p><b>Ejemplo:</b> <code>REGISTRARPROSPECTO[\"Carlos Andres\", \"70012345\", \"carlos@gmail.com\", \"Facebook\", \"Ingenieria de Sistemas\"]</code></p>" +
                       "<p><b>Descripcion:</b> Da de alta un prospecto en etapa 1 (Nuevo) y lo coloca en la Bolsa Comun sin vendedor asignado.</p>";
                break;
            case "LISTAROFERTA":
                info = "<p><b>Comando:</b> LISTAROFERTA</p>" +
                       "<p><b>Sintaxis:</b> <code>LISTAROFERTA[]</code></p>" +
                       "<p><b>Roles permitidos:</b> Todos (Publico y Personal)</p>" +
                       "<p><b>Descripcion:</b> Consulta el catalogo de programas y modulos con sus codigos, duracion y arancel en Bolivianos.</p>";
                break;
            case "VINCULARINTERES":
                info = "<p><b>Comando:</b> VINCULARINTERES</p>" +
                       "<p><b>Sintaxis:</b> <code>VINCULARINTERES[\"id_lead\", \"tipo_oferta\", \"id_externo\", \"nombre_curso\"]</code></p>" +
                       "<p><b>Roles permitidos:</b> Administrador, Coordinador, Vendedor asignado o el propio prospecto</p>" +
                       "<p><b>Tipos validos:</b> <code>PROGRAMA</code> o <code>MODULO</code></p>" +
                       "<p><b>Programas disponibles:</b> #101 Ciberseguridad Defensiva, #102 Maestria TI, #103 Inteligencia Artificial, #104 Desarrollo Full Stack.<br/>" +
                       "<b>Modulos disponibles:</b> #201 Hacking Etico, #202 Redes y Respuesta, #203 Cloud AWS/GCP, #204 Machine Learning Python.</p>" +
                       "<p><b>Ejemplo:</b> <code>VINCULARINTERES[\"15\", \"PROGRAMA\", \"101\", \"Diplomado en Ciberseguridad Defensiva\"]</code></p>";
                break;
            case "MARCARPERDIDO":
                info = "<p><b>Comando:</b> MARCARPERDIDO</p>" +
                       "<p><b>Sintaxis:</b> <code>MARCARPERDIDO[\"id_lead\", \"id_motivo\", \"comentario\"]</code></p>" +
                       "<p><b>Roles permitidos:</b> Administrador, Vendedor asignado</p>" +
                       "<p><b>Motivos comunes:</b> #1 Factor economico, #2 Horario no disponible, #3 Ya no interesado, #4 Sin respuesta.</p>" +
                       "<p><b>Ejemplo:</b> <code>MARCARPERDIDO[\"15\", \"1\", \"El arancel mensual excede su capacidad de pago actual\"]</code></p>" +
                       "<p><b>Descripcion:</b> Clasifica el lead en etapa 6 (Perdido) auditando la causa y justificacion en bitacora.</p>";
                break;
            case "REGISTRARCOMPROBANTE":
                info = "<p><b>Comando:</b> REGISTRARCOMPROBANTE</p>" +
                       "<p><b>Sintaxis:</b> <code>REGISTRARCOMPROBANTE[\"id_lead\", \"banco\", \"nro_trans\", \"monto\"]</code></p>" +
                       "<p><b>Roles permitidos:</b> Administrador, Vendedor asignado</p>" +
                       "<p><b>Ejemplo:</b> <code>REGISTRARCOMPROBANTE[\"15\", \"Banco Mercantil\", \"982104\", \"2500.00\"]</code></p>" +
                       "<p><b>Descripcion:</b> Registra deposito en comprobantes_pago con estado PENDIENTE y avanza a etapa 4 (Promesa de Pago).</p>";
                break;
            case "APROBARPAGO":
                info = "<p><b>Comando:</b> APROBARPAGO</p>" +
                       "<p><b>Sintaxis:</b> <code>APROBARPAGO[\"id_comprobante\"]</code></p>" +
                       "<p><b>Roles permitidos:</b> Administrador</p>" +
                       "<p><b>Ejemplo:</b> <code>APROBARPAGO[\"3\"]</code></p>" +
                       "<p><b>Descripcion:</b> Valida el deposito bancario, marcandolo como APROBADO para habilitar la matriculacion.</p>";
                break;
            case "COMPLETARDATOSALUMNO":
                info = "<p><b>Comando:</b> COMPLETARDATOSALUMNO</p>" +
                       "<p><b>Sintaxis:</b> <code>COMPLETARDATOSALUMNO[\"id_lead\", \"ci\", \"fecha_nac\", \"ciudad\"]</code></p>" +
                       "<p><b>Roles permitidos:</b> Administrador, Vendedor asignado</p>" +
                       "<p><b>Formato fecha:</b> YYYY-MM-DD (ejemplo: 1998-05-14)</p>" +
                       "<p><b>Ejemplo:</b> <code>COMPLETARDATOSALUMNO[\"15\", \"8291043 SC\", \"1998-05-14\", \"Santa Cruz\"]</code></p>";
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
                       "<p><b>Roles permitidos:</b> Administrador</p>" +
                       "<p><b>Descripcion:</b> Verifica que el prospecto tenga comprobante de pago aprobado y datos de alumno completos (CI, fecha nacimiento, ciudad), pasandolo a etapa 5 (Convertido).</p>";
                break;
            case "VERBITACORA":
                info = "<p><b>Comando:</b> VERBITACORA</p>" +
                       "<p><b>Sintaxis:</b> <code>VERBITACORA[\"id_lead\"]</code></p>" +
                       "<p><b>Roles permitidos:</b> Administrador, Coordinador, Vendedor</p>" +
                       "<p><b>Descripcion:</b> Despliega la linea de tiempo cronologica con todos los eventos, notas, transiciones de etapa y pagos registrados para un prospecto.</p>";
                break;
            case "AGREGARNOTA":
                info = "<p><b>Comando:</b> AGREGARNOTA</p>" +
                       "<p><b>Sintaxis:</b> <code>AGREGARNOTA[\"id_lead\", \"contenido_nota\"]</code></p>" +
                       "<p><b>Roles permitidos:</b> Administrador, Vendedor asignado</p>" +
                       "<p><b>Ejemplo:</b> <code>AGREGARNOTA[\"1\", \"Cliente solicita envio de temario detallado por WhatsApp\"]</code></p>" +
                       "<p><b>Descripcion:</b> Registra una anotacion cualitativa sobre la interaccion comercial con el cliente en su expediente.</p>";
                break;
            case "CREARRECORDATORIO":
                info = "<p><b>Comando:</b> CREARRECORDATORIO</p>" +
                       "<p><b>Sintaxis:</b> <code>CREARRECORDATORIO[\"id_lead\", \"yyyy-MM-dd HH:mm\", \"descripcion\"]</code></p>" +
                       "<p><b>Roles permitidos:</b> Administrador, Vendedor</p>" +
                       "<p><b>Ejemplo:</b> <code>CREARRECORDATORIO[\"1\", \"2026-11-28 15:30\", \"Llamar para coordinar modalidad de pago\"]</code></p>" +
                       "<p><b>Descripcion:</b> Programa una alarma o compromiso comercial futuro con fecha y hora exacta en la agenda del remitente.</p>";
                break;
            case "MISRECORDATORIOS":
                info = "<p><b>Comando:</b> MISRECORDATORIOS</p>" +
                       "<p><b>Sintaxis:</b> <code>MISRECORDATORIOS[]</code></p>" +
                       "<p><b>Roles permitidos:</b> Administrador, Vendedor</p>" +
                       "<p><b>Descripcion:</b> Muestra la lista de tareas y compromisos pendientes programados en la agenda del usuario que envia el correo.</p>";
                break;
            case "COMPLETARRECORDATORIO":
                info = "<p><b>Comando:</b> COMPLETARRECORDATORIO</p>" +
                       "<p><b>Sintaxis:</b> <code>COMPLETARRECORDATORIO[\"id_recordatorio\"]</code></p>" +
                       "<p><b>Roles permitidos:</b> Administrador, Vendedor</p>" +
                       "<p><b>Ejemplo:</b> <code>COMPLETARRECORDATORIO[\"1\"]</code></p>" +
                       "<p><b>Descripcion:</b> Marca un recordatorio o compromiso como atendido y finalizado, registrando el cierre en la bitacora.</p>";
                break;
            case "REPORTEEMBUDO":
                info = "<p><b>Comando:</b> REPORTEEMBUDO</p>" +
                       "<p><b>Sintaxis:</b> <code>REPORTEEMBUDO[]</code></p>" +
                       "<p><b>Roles permitidos:</b> Administrador, Coordinador</p>" +
                       "<p><b>Descripcion:</b> Despliega el volumen de prospectos y porcentaje de participacion a lo largo de las 6 etapas del embudo comercial.</p>";
                break;
            case "REPORTECONVERSION":
                info = "<p><b>Comando:</b> REPORTECONVERSION</p>" +
                       "<p><b>Sintaxis:</b> <code>REPORTECONVERSION[]</code></p>" +
                       "<p><b>Roles permitidos:</b> Administrador, Coordinador</p>" +
                       "<p><b>Descripcion:</b> Consolida las tasas globales de conversion y desercion, junto con la matriz de desempeno comercial por vendedor.</p>";
                break;
            case "REPORTEORIGENES":
                info = "<p><b>Comando:</b> REPORTEORIGENES</p>" +
                       "<p><b>Sintaxis:</b> <code>REPORTEORIGENES[]</code></p>" +
                       "<p><b>Roles permitidos:</b> Administrador, Coordinador</p>" +
                       "<p><b>Descripcion:</b> Muestra la distribucion de prospectos captados y matriculados agrupados por canal de origen (Facebook, WhatsApp, Web, etc.).</p>";
                break;
            case "REPORTEMOTIVOSPERDIDA":
                info = "<p><b>Comando:</b> REPORTEMOTIVOSPERDIDA</p>" +
                       "<p><b>Sintaxis:</b> <code>REPORTEMOTIVOSPERDIDA[]</code></p>" +
                       "<p><b>Roles permitidos:</b> Administrador, Coordinador</p>" +
                       "<p><b>Descripcion:</b> Analisis estadistico de las causales de desistimiento y perdida comercial de prospectos.</p>";
                break;
            case "DASHBOARDVENDEDOR":
                info = "<p><b>Comando:</b> DASHBOARDVENDEDOR</p>" +
                       "<p><b>Sintaxis:</b> <code>DASHBOARDVENDEDOR[]</code></p>" +
                       "<p><b>Roles permitidos:</b> Administrador, Vendedor</p>" +
                       "<p><b>Descripcion:</b> Cuadro de mando individual con prospectos en cartera, activos, convertidos, perdidos, tasa de exito y recordatorios pendientes.</p>";
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
            case "LISTAROFERTA":
                return leadService.listarOferta(auth);
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
        UsuarioAuth auth = SecurityContext.obtenerUsuario(fromEmail);
        switch (cmd.getName()) {
            case "LISTARMOTIVOSPERDIDA":
                return motivoService.listarMotivosPerdida(auth);
            case "MARCARPERDIDO":
                return motivoService.marcarPerdido(auth, cmd);
            case "LISTARPERDIDOS":
                return motivoService.listarPerdidos(auth);
            default:
                throw new CommandException("Comando de motivos de perdida no implementado: " + cmd.getName());
        }
    }

    private CommandResult handleModuloMatricula(ParsedCommand cmd, String fromEmail) throws Exception {
        UsuarioAuth auth = SecurityContext.obtenerUsuario(fromEmail);
        switch (cmd.getName()) {
            case "REGISTRARCOMPROBANTE":
                return pagoService.registrarComprobante(auth, cmd);
            case "PAGOSPENDIENTES":
                return pagoService.listarPagosPendientes(auth);
            case "APROBARPAGO":
                return pagoService.aprobarPago(auth, cmd);
            case "RECHAZARPAGO":
                return pagoService.rechazarPago(auth, cmd);
            case "COMPLETARDATOSALUMNO":
                return pagoService.completarDatosAlumno(auth, cmd);
            case "CONVERTIRAMATRICULA":
                return pagoService.convertirAMatricula(auth, cmd);
            default:
                throw new CommandException("Comando de pagos y matricula no implementado: " + cmd.getName());
        }
    }

    private CommandResult handleModuloBitacora(ParsedCommand cmd, String fromEmail) throws Exception {
        UsuarioAuth auth = SecurityContext.obtenerUsuario(fromEmail);
        switch (cmd.getName()) {
            case "VERBITACORA":
                return bitacoraService.verBitacora(auth, cmd);
            case "AGREGARNOTA":
                return bitacoraService.agregarNota(auth, cmd);
            case "CREARRECORDATORIO":
                return bitacoraService.crearRecordatorio(auth, cmd);
            case "MISRECORDATORIOS":
                return bitacoraService.misRecordatorios(auth);
            case "COMPLETARRECORDATORIO":
                return bitacoraService.completarRecordatorio(auth, cmd);
            default:
                throw new CommandException("Comando de bitacora no implementado: " + cmd.getName());
        }
    }

    private CommandResult handleModuloReportes(ParsedCommand cmd, String fromEmail) throws Exception {
        UsuarioAuth auth = SecurityContext.obtenerUsuario(fromEmail);
        switch (cmd.getName()) {
            case "REPORTEEMBUDO":
                return reporteService.reporteEmbudo(auth);
            case "REPORTECONVERSION":
                return reporteService.reporteConversion(auth);
            case "REPORTEORIGENES":
                return reporteService.reporteOrigenes(auth);
            case "REPORTEMOTIVOSPERDIDA":
                return reporteService.reporteMotivosPerdida(auth);
            case "DASHBOARDVENDEDOR":
                return reporteService.dashboardVendedor(auth);
            default:
                throw new CommandException("Comando de reportes no implementado: " + cmd.getName());
        }
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
