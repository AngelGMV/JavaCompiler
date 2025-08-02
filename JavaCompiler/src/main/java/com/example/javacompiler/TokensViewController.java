package com.example.javacompiler;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class TokensViewController implements Initializable {

    @FXML
    private TableView<Token> tablaTokens;

    @FXML
    private TableColumn<Token, String> columnaLexema;

    @FXML
    private TableColumn<Token, String> columnaTipo;

    @FXML
    private TableColumn<Token, Integer> columnaLinea;

    @FXML
    private TableColumn<Token, Integer> columnaColumna;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        columnaLexema.setCellValueFactory(new PropertyValueFactory<>("lexema"));
        columnaTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        columnaLinea.setCellValueFactory(new PropertyValueFactory<>("linea"));
        columnaColumna.setCellValueFactory(new PropertyValueFactory<>("columna"));
    }

    public void setTokens(ObservableList<Token> tokens) {
        tablaTokens.setItems(tokens);
    }
}