package com.example.javacompiler;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HelloController implements Initializable {

    @FXML private TextArea editorArea;
    @FXML private TableView<Token> tablaDeSimbolos;
    @FXML private TableView<ErrorLexico> tablaErroresLexicos;
    @FXML private Button AnalizadorLexico;
    @FXML private Button AnalizadorSintactico;

    @FXML private TableColumn<Token, String> identificadorColumn;
    @FXML private TableColumn<ErrorLexico, String> columnaError;
    @FXML private TableColumn<ErrorLexico, Integer> columnaLinea;
    @FXML private TableColumn<ErrorLexico, Integer> columnaColumna;

    @FXML private TableView<ErrorSintactico> tablaErroresSintacticos;
    @FXML private TableColumn<ErrorSintactico, String> columnaErrorSintactico;
    @FXML private TableColumn<ErrorSintactico, Integer> columnaLineaSintactico;
    @FXML private TableColumn<ErrorSintactico, Integer> columnaColumnaSintactico;



    private final ObservableList<Token> tokens = FXCollections.observableArrayList();
    private final ObservableList<ErrorLexico> erroresLexicos = FXCollections.observableArrayList();
    private final ObservableList<ErrorSintactico> erroresSintacticos = FXCollections.observableArrayList();
    private static final ObservableList<Token> identificadores = FXCollections.observableArrayList();


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        columnaError.setCellValueFactory(new PropertyValueFactory<>("lexema"));
        columnaLinea.setCellValueFactory(new PropertyValueFactory<>("linea"));
        columnaColumna.setCellValueFactory(new PropertyValueFactory<>("columna"));
        tablaErroresLexicos.setItems(erroresLexicos);

        columnaErrorSintactico.setCellValueFactory(new PropertyValueFactory<>("mensaje"));
        columnaLineaSintactico.setCellValueFactory(new PropertyValueFactory<>("linea"));
        columnaColumnaSintactico.setCellValueFactory(new PropertyValueFactory<>("columna"));
        tablaErroresSintacticos.setItems(erroresSintacticos);

        identificadorColumn.setCellValueFactory(new PropertyValueFactory<>("lexema"));
        AnalizadorLexico.setOnAction(event -> onAnalizadorLexicoButtonClick());
        AnalizadorSintactico.setOnAction(event -> onAnalizadorSintacticoButtonClick());
    }

    public void onExaminarButtonClick(ActionEvent e) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo de texto");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home") + "/Desktop"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos de texto", "*.txt"));

        Stage stage = (Stage) editorArea.getScene().getWindow();

        File archivoSeleccionado = fileChooser.showOpenDialog(stage);

        if (archivoSeleccionado != null) {
            StringBuilder contenido = new StringBuilder();
            try (BufferedReader lector = new BufferedReader(new FileReader(archivoSeleccionado))) {
                String linea;
                while ((linea = lector.readLine()) != null) {
                    contenido.append(linea).append("\n");
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            editorArea.setText(contenido.toString());
        }
    }

    @FXML
    protected void onAnalizadorLexicoButtonClick() {
        erroresLexicos.clear();
        erroresSintacticos.clear();
        tokens.clear();
        String codigo = editorArea.getText();
        String[] lineas = codigo.split("\n");

        String patronIdentificador = "[a-zA-Z_][a-zA-Z0-9_]*";
        String patronNumero = "\\d+";
        String patronOperador = "[+\\-*/=&<>]+";
        String patronSimbolo = "[()\\[\\]{},.;_]";
        String patronEspacio = "\\s+";
        String patronCadena = "\"[^\"]*\"";
        String patronIdentificadorInvalido = "\\d+[a-zA-Z_][a-zA-Z0-9_]*";

        String patronCombinado = String.format("(%s)|(%s)|(%s)|(%s)|(%s)|(%s)",
                patronCadena, patronIdentificadorInvalido, patronIdentificador, patronNumero, patronOperador, patronSimbolo);
        Pattern pattern = Pattern.compile(patronCombinado);

        for (int numLinea = 0; numLinea < lineas.length; numLinea++) {
            String linea = lineas[numLinea];
            Matcher matcher = pattern.matcher(linea);

            int posAnterior = 0;
            while (matcher.find()) {
                if (matcher.start() > posAnterior) {
                    String caracteresPerdidos = linea.substring(posAnterior, matcher.start()).trim();
                    if (!caracteresPerdidos.isEmpty() && !caracteresPerdidos.matches(patronEspacio)) {
                        ErrorLexico error = new ErrorLexico(caracteresPerdidos, numLinea + 1, posAnterior + 1);
                        erroresLexicos.add(error);
                        tokens.add(new Token(caracteresPerdidos, "Error", numLinea + 1, posAnterior + 1));
                    }
                }

                String lexema = matcher.group();
                if (lexema.matches(patronIdentificadorInvalido)) {
                    ErrorLexico error = new ErrorLexico(lexema, numLinea + 1, matcher.start() + 1);
                    erroresLexicos.add(error);
                    tokens.add(new Token(lexema, "Error", numLinea + 1, matcher.start() + 1));
                } else {

                    String tipo =  clasificarLexema(lexema);
                    Token token = new Token(lexema, tipo, numLinea + 1, matcher.start() + 1);
                    tokens.add(token);
                    if (tipo.equals("identifier")) {
                        boolean yaExiste = identificadores.stream()
                                .anyMatch(t -> t.getLexema().equals(lexema));

                        if (!yaExiste) {
                            identificadores.add(token);
                        }
                    } else if (tipo.equals("Error")) {
                        erroresLexicos.add(new ErrorLexico(lexema, numLinea + 1, matcher.start() + 1));
                    }
                }

                posAnterior = matcher.end();
            }

            if (posAnterior < linea.length()) {
                String caracteresPerdidos = linea.substring(posAnterior).trim();
                if (!caracteresPerdidos.isEmpty() && !caracteresPerdidos.matches(patronEspacio)) {
                    ErrorLexico error = new ErrorLexico(caracteresPerdidos, numLinea + 1, posAnterior + 1);
                    erroresLexicos.add(error);
                    tokens.add(new Token(caracteresPerdidos, "Error", numLinea + 1, posAnterior + 1));
                }
            }
        }
        tablaDeSimbolos.setItems(identificadores);
        abrirVentanaTokens();
    }
    @FXML
    protected void onAnalizadorSintacticoButtonClick() {
        erroresSintacticos.clear();

        if (!erroresLexicos.isEmpty()){
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Análisis sintáctico");
            alert.setHeaderText("Error");
            alert.setContentText("Corrija los errores lexicos para poder seguir con el analisis sintactico");
            alert.showAndWait();
            tokens.clear();
            return;

        }
        Parser parser = new Parser(tokens, erroresSintacticos);
        boolean resultado = parser.parse();

        if (resultado) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Análisis sintáctico");
            alert.setHeaderText("Análisis correcto");
            alert.setContentText("No se encontraron errores sintácticos.");
            alert.showAndWait();
        }
    }

    private void abrirVentanaTokens() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("tokens-view.fxml"));
            Parent root = loader.load();

            TokensViewController controller = loader.getController();
            controller.setTokens(tokens);

            Stage stage = new Stage();
            stage.setTitle("Tokens Detectados");
            stage.setScene(new Scene(root, 800, 600));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error al abrir la ventana de tokens");
            alert.setContentText("No se pudo abrir la ventana de tokens: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private String clasificarLexema(String lexema) {
            if (lexema.startsWith("\"") && lexema.endsWith("\"")) {
                return "Cadena";
            }
            if (lexema.equals("class")) return "class";
            if (lexema.equals("public")) return "public";
            if (lexema.equals("static")) return "static";
            if (lexema.equals("void")) return "void";
            if (lexema.equals("main")) return "main";
            if (lexema.equals("args")) return "args";
            if (lexema.equals("length")) return "length";
            if (lexema.equals("System")) return "System";
            if (lexema.equals("out")) return "out";
            if (lexema.equals("println")) return "println";
            if (lexema.equals("true")) return "true";
            if (lexema.equals("false")) return "false";
            if (lexema.equals("this")) return "this";
            if (lexema.equals("new")) return "new";
            if (lexema.equals("if")) return "if";
            if (lexema.equals("else")) return "else";
            if (lexema.equals("while")) return "while";
            if (lexema.equals("extends")) return "extends";
            if (lexema.equals("return")) return "return";
            if (lexema.equals("(")) return "parentesis_a";
            if (lexema.equals(")")) return "parentesis_c";
            if (lexema.equals("[")) return "bracket_a";
            if (lexema.equals("]")) return "bracket_c";
            if (lexema.equals("{")) return "brace_a";
            if (lexema.equals("}")) return "brace_c";
            if (lexema.equals(",")) return ",";
            if (lexema.equals(".")) return ".";
            if (lexema.equals(";")) return ";";
            if (lexema.equals("_")) return "_";
            if (lexema.equals("+")) return "+";
            if (lexema.equals("-")) return "-";
            if (lexema.equals("*")) return "*";
            if (lexema.equals("=")) return "=";
            if (lexema.equals("&&")) return "&&";
            if (lexema.equals("<")) return "<";
            if (lexema.equals(">")) return ">";
            if (lexema.equals("int")) return "int";
            if (lexema.equals("boolean")) return "boolean";
            if (lexema.equals("String")) return "String";

            if (lexema.matches("\\d+")) return "Numero";
            if (lexema.matches("[a-zA-Z_][a-zA-Z0-9_]*")) return "identifier";
            return "Error";
    }
    public static void removeIdentifierFromSymbolTable(String nombre) {
        identificadores.removeIf(t -> t.getLexema().equals(nombre));
    }
}