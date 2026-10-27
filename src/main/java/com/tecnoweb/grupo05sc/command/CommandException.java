package com.tecnoweb.grupo05sc.command;

/**
 * Excepción semántica para errores en la interpretación y ejecución de comandos.
 * Proporciona mensajes claros y consejos de sintaxis para el usuario.
 */
public class CommandException extends Exception {
    private final String helpTip;

    public CommandException(String message) {
        super(message);
        this.helpTip = "Envíe HELP[] para ver la lista de comandos disponibles y su sintaxis.";
    }

    public CommandException(String message, String helpTip) {
        super(message);
        this.helpTip = helpTip;
    }

    public String getHelpTip() {
        return helpTip;
    }
}
