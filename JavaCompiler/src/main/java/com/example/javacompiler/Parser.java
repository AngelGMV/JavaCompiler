package com.example.javacompiler;

import java.util.List;
import javafx.collections.ObservableList;

public class Parser {
    private final List<Token> tokens;
    private final ObservableList<ErrorSintactico> erroresSintacticos;
    private int index = 0;
    private String tipoRetornoActual = "";

    public Parser(List<Token> tokens, ObservableList<ErrorSintactico> erroresSintacticos) {
        this.tokens = tokens;
        this.erroresSintacticos = erroresSintacticos;
    }

    private Token current() {
        return index < tokens.size() ? tokens.get(index) : null;
    }

    private Token previousToken() {
        return (index - 1 >= 0 && index - 1 < tokens.size()) ? tokens.get(index - 1) : null;
    }

    private boolean match(String tipoEsperado) {
        if (current() != null && current().getTipo().equals(tipoEsperado)) {
            index++;
            return true;
        }
        return false;
    }

    private boolean expect(String tipoEsperado) {
        if (match(tipoEsperado)) return true;
        error(tipoEsperado);
        return false;
    }

    private void error(String esperado) {
        Token t = current();
        if (t != null) {
            erroresSintacticos.add(new ErrorSintactico(
                    String.format("Se esperaba '%s' pero se encontró '%s' [idx=%d]", esperado, t.getLexema(), index),
                    t.getLinea(), t.getColumna()
            ));
        } else {
            erroresSintacticos.add(new ErrorSintactico(
                    "Fin inesperado del archivo, se esperaba: " + esperado,
                    -1, -1
            ));
        }
    }

    private void sincronizar() {
        Token t = current();
        while (t != null && !t.getTipo().equals("return") && !t.getTipo().equals("brace_c") && !t.getTipo().equals(";")) {
            index++;
            t = current();
        }
    }

    public boolean parse() {
        return program() && index == tokens.size() && erroresSintacticos.isEmpty();
    }

    public boolean program() {
        return mainClass() && classDeclarations();
    }


    private boolean mainClass() {
        return expect("class") && expect("identifier") && expect("brace_a") && expect("public") && expect("static")
                && expect("void") && expect("main") && expect("parentesis_a") && expect("String") && expect("bracket_a")
                && expect("bracket_c") && expect("args") && expect("parentesis_c") && expect("brace_a")
                && statement() && expect("brace_c") && expect("brace_c");
    }

    private boolean classDeclarations() {
        boolean result = true;
        while (current() != null && current().getTipo().equals("class")) {
            if (!classDeclaration()) {
                result = false;
                break;
            }
        }
        return result;
    }

    private boolean classDeclaration() {
        if (!expect("class") || !expect("identifier")) return false;
        if (match("extends") && !expect("identifier")) return false;
        if (!expect("brace_a")) return false;

        while (current() != null && isType()) {
            if (!variableDeclaration()) return false;
        }
        while (current() != null && current().getTipo().equals("public")) {
            if (!methodDeclaration()) return false;
        }
        return expect("brace_c");
    }

    private boolean methodDeclaration() {
        if (!expect("public")) return false;
        if (!type()) return false;

        tipoRetornoActual = previousToken().getTipo();

        if (!expect("identifier") || !formalList() || !expect("brace_a")) return false;

        while (current() != null && (isType() || isStartOfStatement())) {
            boolean ok;
            if (isType()) {
                ok = variableDeclaration();
            } else {
                ok = statement();
            }
            if (!ok) {
                error("Declaración inválida en método");
                sincronizar();
                break;
            }
        }

        if (!expect("return")) return false;

        if (tipoRetornoActual.equals("void")) {
            if (!expect(";")) return false;
        } else {
            if (!expression()) {
                error("Expresión inválida en return (idx=" + index + ")");
                sincronizar();
                return false;
            }
            if (!expect(";")) return false;
        }

        return expect("brace_c");
    }


    private boolean isType() {
        String tipo = current() != null ? current().getTipo() : "";
        return tipo.equals("identifier") || tipo.equals("int") || tipo.equals("boolean") || tipo.equals("String") || tipo.equals("void");
    }

    private boolean isStartOfStatement() {
        if (current() == null) return false;
        String tipo = current().getTipo();
        return tipo.equals("brace_a") || tipo.equals("if") || tipo.equals("while") || tipo.equals("System") || tipo.equals("identifier");
    }

    private boolean statement() {
        Token token = current();
        if (token == null) return false;

        switch (token.getTipo()) {
            case "brace_a":
                expect("brace_a");
                while (current() != null && (isType() || isStartOfStatement())) {
                    if (isType()) {
                        if (!variableDeclaration()) return false;
                    } else if (!statement()) {
                        error("Sentencia dentro del bloque");
                        sincronizar();
                        break;
                    }
                }
                return expect("brace_c");

            case "if":
                return expect("if") && expect("parentesis_a") && expression() && expect("parentesis_c") && statement() && expect("else") && statement();
            case "while":
                return expect("while") && expect("parentesis_a") && expression() && expect("parentesis_c") && statement();
            case "System":
                return expect("System") && expect(".") && expect("out") && expect(".") && expect("println") && expect("parentesis_a") && expressionOrCadena() && expect("parentesis_c") && expect(";");
            case "identifier":
                index++;
                if (expect("=")) {
                    return expression() && expect(";");
                } else if (expect("bracket_a")) {
                    return expression() && expect("bracket_c") && expect("=") && expression() && expect(";");
                } else {
                    error("'=' o '['");
                    return false;
                }
            default:
                return false;
        }
    }

    private boolean variableDeclaration() {
        return type() && expect("identifier") && expect(";");
    }

    private boolean type() {
        return expect("int") || expect("boolean") || expect("String") || expect("identifier") || expect("void");
    }

    private boolean formalList() {
        if (!expect("parentesis_a")) return false;
        if (current() != null && isType()) {
            if (!type() || !expect("identifier")) return false;
            while (current() != null && current().getTipo().equals(",")) {
                expect(",");
                if (!type() || !expect("identifier")) return false;
            }
        }
        return expect("parentesis_c");
    }

    private boolean expression() {
        return parseBinaryExpression();
    }

    private boolean parseBinaryExpression() {
        if (!primary()) return false;

        while (current() != null) {
            String tipo = current().getTipo();
            if (tipo.equals("+") || tipo.equals("-") || tipo.equals("*") || tipo.equals("/") || tipo.equals("&&") || tipo.equals("<") || tipo.equals(">") || tipo.equals("==") || tipo.equals("!=")) {
                index++;
                if (!primary()) {
                    error("Expresión incompleta después de operador '" + tipo + "'");
                    return false;
                }
            } else {
                break;
            }
        }
        return true;
    }

    private boolean primary() {
        Token token = current();
        if (token == null) return false;

        switch (token.getTipo()) {
            case "Numero":
            case "true":
            case "false":
            case "identifier":
            case "this":
                index++;
                return true;
            case "new":
                index++;
                if (expect("int")) {
                    return expect("bracket_a") && expression() && expect("bracket_c");
                } else if (expect("identifier")) {
                    return expect("parentesis_a") && expect("parentesis_c");
                }
                return false;
            case "!":
                index++;
                return expression();
            case "parentesis_a":
                index++;
                boolean inner = expression();
                return inner && expect("parentesis_c");
            default:
                return false;
        }
    }


    private boolean expressionList() {
        if (!expression()) return false;
        while (current() != null && current().getTipo().equals(",")) {
            expect(",");
            if (!expression()) return false;
        }
        return true;
    }

    private boolean expressionOrCadena() {
        if (current() != null && current().getTipo().equals("Cadena")) {
            index++;
            return true;
        }
        return expression();
    }
}
