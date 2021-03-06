/*
 * Copyright (c) 2021 - present Pethum Jeewantha. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 */

package controller;

import javafx.animation.FadeTransition;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import util.FXUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class EditorFormController {
    private final List<Index> searchList = new ArrayList<>();
    public TextArea txtEditor;
    public AnchorPane pneFind;
    public TextField txtSearch;
    public AnchorPane pneReplace;
    public TextField txtSearch1;
    public TextField txtReplace;
    public Label lblWords;
    String text;
    double font_size;
    private int findOffset = -1;
    private PrinterJob printerJob;

    public void initialize() {
        pneFind.setOpacity(0);
        pneReplace.setOpacity(0);
        this.printerJob = PrinterJob.createPrinterJob();
        this.text = txtEditor.getText();

        wordCount(txtEditor.getText());

        font_size = Preferences.userRoot().node("TextEditor-DEP7").getDouble("font size", 18);
        txtEditor.setStyle("-fx-font-size: " + font_size);

        ChangeListener textListener = (ChangeListener<String>) (observable, oldValue, newValue) -> {
            searchMatches(newValue);
        };

        txtSearch.textProperty().addListener(textListener);
        txtSearch1.textProperty().addListener(textListener);

        txtEditor.textProperty().addListener((observable, oldValue, newValue) -> {
            wordCount(newValue);
        });

    }

    private void wordCount(String newValue) {
        if (txtEditor.getText().trim().isEmpty()) lblWords.setText("0");
        else {
            String[] words = newValue.split("\\s+");
            lblWords.setText(String.valueOf(words.length));
        }
    }

    private void searchMatches(String query) {
        FXUtil.highlightOnTextArea(txtEditor, query, Color.web("yellow", 0.8));

        try {
            Pattern regExp = Pattern.compile(query);
            Matcher matcher = regExp.matcher(txtEditor.getText());

            searchList.clear();

            while (matcher.find()) {
                searchList.add(new Index(matcher.start(), matcher.end()));
            }

            if (searchList.isEmpty()) {
                findOffset = -1;
            }
        } catch (PatternSyntaxException ignored) {

        }
    }

    public void mnuItemNew_OnAction(ActionEvent actionEvent) {
        txtEditor.clear();
        txtEditor.requestFocus();
    }

    public void mnuItemExit_OnAction(ActionEvent actionEvent) {
        System.exit(0);
    }

    public void mnuItemFind_OnAction(ActionEvent actionEvent) {
        findOffset = -1;
        if (pneReplace.getOpacity() == 1) {
            pneReplace.setOpacity(0);
        }
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(250), pneFind);
        fadeTransition.setFromValue(0);
        fadeTransition.setToValue(1);
        fadeTransition.play();
        txtSearch.requestFocus();
    }

    public void mnuItemReplace_OnAction(ActionEvent actionEvent) {
        findOffset = -1;
        if (pneFind.getOpacity() == 1) {
            pneFind.setOpacity(0);
        }
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(250), pneReplace);
        fadeTransition.setFromValue(0);
        fadeTransition.setToValue(1);
        fadeTransition.play();
        txtSearch1.requestFocus();
    }

    public void mnuItemSelectAll_OnAction(ActionEvent actionEvent) {
        txtEditor.selectAll();
    }

    public void btnFindNext_OnAction(ActionEvent actionEvent) {
        if (!searchList.isEmpty()) {
            findOffset++;
            if (findOffset >= searchList.size()) {
                findOffset = 0;
            }
            txtEditor.selectRange(searchList.get(findOffset).startingIndex, searchList.get(findOffset).endIndex);
        }
    }

    public void btnFindPrevious_OnAction(ActionEvent actionEvent) {
        if (!searchList.isEmpty()) {
            findOffset--;
            if (findOffset < 0) {
                findOffset = searchList.size() - 1;
            }
            txtEditor.selectRange(searchList.get(findOffset).startingIndex, searchList.get(findOffset).endIndex);
        }
    }

    public void btnReplaceAll_OnAction(ActionEvent actionEvent) {
        while (!searchList.isEmpty()) {
            txtEditor.replaceText(searchList.get(0).startingIndex, searchList.get(0).endIndex, txtReplace.getText());
            searchMatches(txtSearch1.getText());
        }
    }

    public void btnReplace_OnAction(ActionEvent actionEvent) {
        if (findOffset == -1) return;
        txtEditor.replaceText(searchList.get(findOffset).startingIndex, searchList.get(findOffset).endIndex, txtReplace.getText());
        searchMatches(txtSearch1.getText());
    }

    public void mnuFileOpen_OnAction(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("All Text Files", "*.txt", "*.html"));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("All Files", "*")
        );
        File file = fileChooser.showOpenDialog(txtEditor.getScene().getWindow());
        if (file == null) return;
        fileOpen(file);
    }

    private void fileOpen(File file) {

        txtEditor.clear();

        try (FileReader fileReader = new FileReader(file);
             BufferedReader bufferedReader = new BufferedReader(fileReader)) {
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                txtEditor.appendText(line + '\n');
            }
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Can't open the file", ButtonType.CLOSE).show();
        }
    }

    public void mnuSave_OnAction(ActionEvent actionEvent) {
        if (!text.equals(txtEditor.getText())) {
            text = txtEditor.getText();
            saveFile();
        }
    }

    public void mnuSaveAs_OnAction(ActionEvent actionEvent) {
        saveFile();
    }

    private void saveFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save File");
        File file = fileChooser.showSaveDialog(txtEditor.getScene().getWindow());

        if (file == null) return;

        try (FileWriter fileWriter = new FileWriter(file);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
            bufferedWriter.write(txtEditor.getText());
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Can't save the file", ButtonType.CLOSE).show();
        }
    }

    public void mnuPageSetup_OnAction(ActionEvent actionEvent) {
        printerJob.showPageSetupDialog(txtEditor.getScene().getWindow());
    }

    public void mnuPrint_OnAction(ActionEvent actionEvent) {
        boolean printDialog = printerJob.showPrintDialog(txtEditor.getScene().getWindow());

        if (printDialog) {
            printerJob.printPage(txtEditor.lookup("Text"));
        }
    }

    public void mnuItemCut_OnAction(ActionEvent actionEvent) {
        txtEditor.cut();
    }

    public void mnuItemCopy_OnAction(ActionEvent actionEvent) {
        txtEditor.copy();
    }

    public void mnuItemPaste_OnAction(ActionEvent actionEvent) {
        txtEditor.paste();
    }

    public void mnuItemFontSize_OnAction(ActionEvent actionEvent) {
        TextInputDialog textInputDialog = new TextInputDialog(String.valueOf(Preferences.userRoot().node("TextEditor-DEP7").getDouble("font size", 18)));
        textInputDialog.setHeaderText("Font Size");
        Stage dialogStage = (Stage) textInputDialog.getEditor().getScene().getWindow();
        dialogStage.initStyle(StageStyle.TRANSPARENT);

        try {
            String fontSize = textInputDialog.showAndWait().get();
            Preferences.userRoot().node("TextEditor-DEP7").putDouble("font size", Double.parseDouble(textInputDialog.getEditor().getText()));
            txtEditor.setStyle("-fx-font-size: " + fontSize);
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR, "Invalid size", ButtonType.CLOSE).show();
        } catch (NoSuchElementException ignored) {

        }
    }

    public void txtEditorDragOver_OnAction(DragEvent dragEvent) {
        if (dragEvent.getDragboard().hasFiles()) {
            dragEvent.acceptTransferModes(TransferMode.ANY);
        }
    }

    public void txtEditorDragDropped_OnAction(DragEvent dragEvent) {
        if (dragEvent.getDragboard().hasFiles()) {
            File file = dragEvent.getDragboard().getFiles().get(0);
            fileOpen(file);
        }
    }

    public void openAbout_OnAction(ActionEvent actionEvent) throws IOException {
        Stage aboutStage = new Stage();
        aboutStage.setScene(new Scene(FXMLLoader.load(this.getClass().getResource("/view/AboutForm.fxml"))));
        aboutStage.show();
    }
}

class Index {
    int startingIndex;
    int endIndex;

    public Index(int startingIndex, int endIndex) {
        this.startingIndex = startingIndex;
        this.endIndex = endIndex;
    }
}
