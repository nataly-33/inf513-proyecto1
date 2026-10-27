package com.tecnoweb.grupo05sc.command;

import java.util.Collections;
import java.util.List;

/**
 * Representa un comando extraído del Asunto del correo.
 * Ejemplo: LISPER["*"] o REGISTRARPROSPECTO["Carlos","70012345",...]
 */
public final class ParsedCommand {
    private final String name;
    private final List<String> params;

    public ParsedCommand(String name, List<String> params) {
        this.name = (name != null) ? name.trim().toUpperCase() : "";
        this.params = (params != null) ? params : Collections.emptyList();
    }

    public String getName() {
        return name;
    }

    public List<String> getParams() {
        return Collections.unmodifiableList(params);
    }

    public int paramCount() {
        return params.size();
    }

    public boolean hasParams() {
        return !params.isEmpty();
    }

    public String getParam(int index) throws CommandException {
        if (index < 0 || index >= params.size()) {
            throw new CommandException("Parámetro #" + (index + 1) + " faltante.");
        }
        return params.get(index);
    }

    public String getParamOrDefault(int index, String defaultValue) {
        if (index < 0 || index >= params.size()) {
            return defaultValue;
        }
        return params.get(index);
    }

    public int getIntParam(int index, String paramName) throws CommandException {
        String val = getParam(index);
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            throw new CommandException("El parámetro '" + paramName + "' debe ser un número entero válido. Se recibió: \"" + val + "\".");
        }
    }

    public double getDoubleParam(int index, String paramName) throws CommandException {
        String val = getParam(index);
        try {
            return Double.parseDouble(val.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new CommandException("El parámetro '" + paramName + "' debe ser un valor decimal válido. Se recibió: \"" + val + "\".");
        }
    }

    @Override
    public String toString() {
        return name + params;
    }
}
