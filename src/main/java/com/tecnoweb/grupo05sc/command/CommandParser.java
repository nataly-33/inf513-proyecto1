package com.tecnoweb.grupo05sc.command;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intérprete léxico de comandos recibidos en el Asunto (Subject) del correo electrónico.
 * Soporta la sintaxis oficial del docente:
 *   - LISPER["*"]
 *   - INSPER["4715292","Juan Carlos","Perez Seras","Estudiante","33554433","71055123","juanperez@uagrm.edu.bo"]
 *   - COMANDO["param1", "param2", ...]
 *   - COMANDO[] o COMANDO (sin corchetes para comandos simples como HELP)
 *
 * Es tolerante a espacios en blanco, comillas dobles y caracteres especiales.
 */
public final class CommandParser {

    private static final Pattern PATTERN_WITH_BRACKETS = Pattern.compile("^\\s*([A-Za-z0-9_]+)\\s*\\[(.*)\\]\\s*$", Pattern.DOTALL);
    private static final Pattern PATTERN_WITHOUT_BRACKETS = Pattern.compile("^\\s*([A-Za-z0-9_]+)\\s*$", Pattern.DOTALL);

    private CommandParser() {}

    /**
     * Parsea la línea de asunto extrayendo el nombre del comando y sus parámetros.
     */
    public static ParsedCommand parse(String subject) throws CommandException {
        if (subject == null || subject.trim().isEmpty()) {
            throw new CommandException(
                "El asunto del correo está vacío.",
                "Envíe HELP[] en el asunto para ver la lista de comandos disponibles."
            );
        }

        String trimmed = subject.trim();

        // 1. Caso estándar: COMANDO[...]
        Matcher matcher = PATTERN_WITH_BRACKETS.matcher(trimmed);
        if (matcher.matches()) {
            String commandName = matcher.group(1).trim().toUpperCase();
            String rawParams = matcher.group(2).trim();
            List<String> params = parseParameters(rawParams);
            return new ParsedCommand(commandName, params);
        }

        // 2. Caso simplificado: COMANDO (ej. HELP, QUIENSOY, BOLSACOMUN)
        Matcher matcherSimple = PATTERN_WITHOUT_BRACKETS.matcher(trimmed);
        if (matcherSimple.matches()) {
            String commandName = matcherSimple.group(1).trim().toUpperCase();
            return new ParsedCommand(commandName, new ArrayList<>());
        }

        throw new CommandException(
            "Sintaxis de comando no reconocida: \"" + trimmed + "\".",
            "El formato esperado es: COMANDO[\"param1\", \"param2\"] o COMANDO[]. Ejemplo: LISPER[\"*\"] o HELP[]."
        );
    }

    /**
     * Extrae parámetros tolerando comillas dobles y comas como delimitadores.
     * Soporta tanto parámetros entrecomillados ("valor con espacios") como literales directos (*, 123).
     */
    private static List<String> parseParameters(String raw) throws CommandException {
        List<String> params = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return params;
        }

        int i = 0;
        int len = raw.length();

        while (i < len) {
            // Saltar espacios en blanco previos
            while (i < len && Character.isWhitespace(raw.charAt(i))) {
                i++;
            }
            if (i >= len) break;

            if (raw.charAt(i) == '"') {
                // Caso A: Parámetro delimitado por comillas dobles
                i++; // saltar comilla de apertura
                StringBuilder sb = new StringBuilder();
                boolean closed = false;

                while (i < len) {
                    char c = raw.charAt(i);
                    if (c == '\\' && i + 1 < len) {
                        // Secuencia de escape \"
                        sb.append(raw.charAt(i + 1));
                        i += 2;
                        continue;
                    }
                    if (c == '"') {
                        closed = true;
                        i++;
                        break;
                    }
                    sb.append(c);
                    i++;
                }

                if (!closed) {
                    throw new CommandException(
                        "Error de sintaxis: Comilla de cierre faltante en uno de los parámetros.",
                        "Asegúrese de cerrar todas las comillas dobles. Ejemplo: COMANDO[\"valor\"]"
                    );
                }

                params.add(sb.toString().trim());

            } else {
                // Caso B: Parámetro sin comillas (ej. *, 123, true)
                StringBuilder sb = new StringBuilder();
                while (i < len && raw.charAt(i) != ',') {
                    sb.append(raw.charAt(i));
                    i++;
                }
                params.add(sb.toString().trim());
            }

            // Saltar espacios después del parámetro
            while (i < len && Character.isWhitespace(raw.charAt(i))) {
                i++;
            }

            // Procesar separador de coma
            if (i < len) {
                if (raw.charAt(i) == ',') {
                    i++; // saltar la coma
                } else {
                    throw new CommandException(
                        "Error de sintaxis: Se esperaba una coma ',' para separar parámetros.",
                        "Ejemplo correcto: COMANDO[\"param1\", \"param2\"]"
                    );
                }
            }
        }

        return params;
    }
}
