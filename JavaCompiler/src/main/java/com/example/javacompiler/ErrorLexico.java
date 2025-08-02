package com.example.javacompiler;

public class ErrorLexico {
    private final String lexema;
    private final int linea;
    private final int columna;

    public ErrorLexico(String lexema, int linea, int columna) {
        this.lexema = lexema;
        this.linea = linea;
        this.columna = columna;
    }

    public String getLexema() { return lexema; }
    public int getLinea() { return linea; }
    public int getColumna() { return columna; }
}
