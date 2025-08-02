package com.example.javacompiler;

public class ErrorSintactico {
    private final String mensaje;
    private final int linea;
    private final int columna;

    public ErrorSintactico(String mensaje, int linea, int columna) {
        this.mensaje = mensaje;
        this.linea = linea;
        this.columna = columna;
    }

    public String getMensaje() { return mensaje; }
    public int getLinea() { return linea; }
    public int getColumna() { return columna; }
}
