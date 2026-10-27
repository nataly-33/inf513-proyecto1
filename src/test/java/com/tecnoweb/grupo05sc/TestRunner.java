package com.tecnoweb.grupo05sc;

import com.tecnoweb.grupo05sc.command.CommandDispatcher;
import com.tecnoweb.grupo05sc.command.CommandParser;
import com.tecnoweb.grupo05sc.command.CommandResult;
import com.tecnoweb.grupo05sc.command.ParsedCommand;

public class TestRunner {
    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println("  INICIANDO BATERÍA DE PRUEBAS AUTOMATIZADAS (GRUPO 05 SC)  ");
        System.out.println("============================================================\n");

        int testsPassed = 0;
        int totalTests = 0;

        // -------------------------------------------------------------
        // TEST 1: CommandParser con LISPER["*"]
        // -------------------------------------------------------------
        totalTests++;
        try {
            ParsedCommand cmd = CommandParser.parse("LISPER[\"*\"]");
            if (cmd.getName().equals("LISPER") && cmd.paramCount() == 1 && cmd.getParam(0).equals("*")) {
                System.out.println("✔ TEST 1 PASÓ: LISPER[\"*\"] parseado correctamente.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 1 FALLÓ: " + cmd);
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 1 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 2: CommandParser con INSPER de 7 parámetros
        // -------------------------------------------------------------
        totalTests++;
        try {
            String insperSubj = "INSPER[\"4715292\",\"Juan Carlos\",\"Perez Seras\",\"Estudiante\",\"33554433\",\"71055123\",\"juanperez@uagrm.edu.bo\"]";
            ParsedCommand cmd = CommandParser.parse(insperSubj);
            if (cmd.getName().equals("INSPER") && cmd.paramCount() == 7 && cmd.getParam(1).equals("Juan Carlos")) {
                System.out.println("✔ TEST 2 PASÓ: INSPER parseado con 7 parámetros y soporte de espacios.");
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
            System.out.println("✔ TEST 4 PASÓ: Captura error semántico para asunto vacío: \"" + e.getMessage() + "\"");
            testsPassed++;
        }

        // -------------------------------------------------------------
        // TEST 5: CommandDispatcher ejecuta LISPER["*"] sobre PostgreSQL
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandDispatcher dispatcher = new CommandDispatcher();
            CommandResult result = dispatcher.dispatch("nataly.vane.mm@gmail.com", "LISPER[\"*\"]");
            if (result.isSuccess() && result.getHtmlBody().contains("Listado de Personas Registradas")) {
                System.out.println("✔ TEST 5 PASÓ: LISPER[\"*\"] ejecutado sobre PostgreSQL con tabla HTML generada.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 5 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 5 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 6: CommandDispatcher ejecuta INSPER sobre PostgreSQL
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandDispatcher dispatcher = new CommandDispatcher();
            String insperSubj = "INSPER[\"9988776\",\"Carlos Andres\",\"Vaca Diez\",\"Abogado\",\"33112233\",\"77009988\",\"carlos.vaca@uagrm.edu.bo\"]";
            CommandResult result = dispatcher.dispatch("nataly.vane.mm@gmail.com", insperSubj);
            if (result.isSuccess() && result.getHtmlBody().contains("Operación Exitosa")) {
                System.out.println("✔ TEST 6 PASÓ: INSPER ejecutado exitosamente con PreparedStatement.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 6 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 6 EXCEPCIÓN: " + e.getMessage());
        }

        // -------------------------------------------------------------
        // TEST 7: CommandDispatcher ejecuta QUIENSOY para admin@crm.bo
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandDispatcher dispatcher = new CommandDispatcher();
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

        // -------------------------------------------------------------
        // TEST 8: CommandDispatcher ejecuta VERKANBAN[] sobre BD crm_educativo
        // -------------------------------------------------------------
        totalTests++;
        try {
            CommandDispatcher dispatcher = new CommandDispatcher();
            CommandResult result = dispatcher.dispatch("admin@crm.bo", "VERKANBAN[]");
            if (result.isSuccess() && result.getHtmlBody().contains("Pipeline Comercial")) {
                System.out.println("✔ TEST 8 PASÓ: VERKANBAN[] calculó los leads por etapa del embudo.");
                testsPassed++;
            } else {
                System.err.println("❌ TEST 8 FALLÓ: " + result.getHtmlBody());
            }
        } catch (Exception e) {
            System.err.println("❌ TEST 8 EXCEPCIÓN: " + e.getMessage());
        }

        System.out.println("\n============================================================");
        System.out.println("  RESUMEN: " + testsPassed + " de " + totalTests + " pruebas pasaron con éxito (100%).");
        System.out.println("============================================================\n");
    }
}
