package com.tecnoweb.grupo05sc.command;

/**
 * Resultado devuelto tras procesar un comando, listo para ser despachado por SMTP.
 */
public final class CommandResult {
    private final String subject;
    private final String htmlBody;
    private final boolean success;

    public CommandResult(String subject, String htmlBody, boolean success) {
        this.subject = subject;
        this.htmlBody = htmlBody;
        this.success = success;
    }

    public static CommandResult success(String subject, String htmlBody) {
        return new CommandResult(subject, htmlBody, true);
    }

    public static CommandResult error(String subject, String htmlBody) {
        return new CommandResult(subject, htmlBody, false);
    }

    public String getSubject() {
        return subject;
    }

    public String getHtmlBody() {
        return htmlBody;
    }

    public boolean isSuccess() {
        return success;
    }
}
