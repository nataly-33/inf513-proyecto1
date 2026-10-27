package com.tecnoweb.grupo05sc.mail;

/**
 * Representa un correo electrónico leído del servidor POP3.
 */
public final class MailMessage {
    private final int messageNumber;
    private final String uid;
    private final String from;
    private final String rawFrom;
    private final String subject;
    private final String body;
    private final String date;

    public MailMessage(int messageNumber, String uid, String from, String rawFrom, String subject, String body, String date) {
        this.messageNumber = messageNumber;
        this.uid = uid;
        this.from = from;
        this.rawFrom = rawFrom;
        this.subject = subject;
        this.body = body;
        this.date = date;
    }

    public int getMessageNumber() {
        return messageNumber;
    }

    public String getUid() {
        return uid;
    }

    public String getFrom() {
        return from;
    }

    public String getRawFrom() {
        return rawFrom;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public String getDate() {
        return date;
    }

    @Override
    public String toString() {
        return "MailMessage{" +
                "msg#" + messageNumber +
                ", from='" + from + '\'' +
                ", subject='" + subject + '\'' +
                '}';
    }
}
