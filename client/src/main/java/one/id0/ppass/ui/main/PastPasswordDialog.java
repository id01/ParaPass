package one.id0.ppass.ui.main;

import java.io.IOException;
import java.util.ArrayList;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTreeTableView;
import com.jfoenix.controls.JFXTreeTableColumn;
import com.jfoenix.controls.RecursiveTreeItem;
import com.jfoenix.controls.cells.editors.TextFieldEditorBuilder;
import com.jfoenix.controls.cells.editors.base.GenericEditableTreeTableCell;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXDialogLayout;

import one.id0.ppass.backend.Logger;
import one.id0.ppass.utils.UserPassword;

public class PastPasswordDialog {
	// JavaFX elements
	private JFXDialog dialog;
	private JFXButton backButton;
	private JFXButton copyButton;
	private JFXTreeTableView<UserPassword> pastPasswordTable;
	
	// Class variables
	private TreeItem<UserPassword> passwordsRoot;
	private UserPassword selectedPassword;
	private Logger logger;
	
	public PastPasswordDialog(StackPane everythingPane, ArrayList<UserPassword> passwords, Logger logger) throws IOException {
		// Init dialog
		JFXDialogLayout dialogLayout = new JFXDialogLayout();
		dialog = new JFXDialog(everythingPane, dialogLayout, JFXDialog.DialogTransition.CENTER);
		dialogLayout.setHeading(new Text("Past Passwords"));

		// Initialize pastPasswordTable
		// Account name column
		JFXTreeTableColumn<UserPassword, String> accountNameColumn = new JFXTreeTableColumn<UserPassword, String>("Account Name");
		accountNameColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<UserPassword, String> param) -> {
			if (param.getValue().getValue() == null) {
				return new SimpleStringProperty("null");
			}
            return param.getValue().getValue().accountName;
        });
		accountNameColumn.setCellFactory((TreeTableColumn<UserPassword, String> param) ->
        	new GenericEditableTreeTableCell<UserPassword, String>(new TextFieldEditorBuilder()));
		// Timestamp column
		JFXTreeTableColumn<UserPassword, String> timestampColumn = new JFXTreeTableColumn<UserPassword, String>("First Seen");
		timestampColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<UserPassword, String> param) -> {
			if (param.getValue().getValue() == null) {
				return new SimpleStringProperty("Never");
			}
            return param.getValue().getValue().getTimestamp();
        });
		timestampColumn.setCellFactory((TreeTableColumn<UserPassword, String> param) ->
        	new GenericEditableTreeTableCell<UserPassword, String>(new TextFieldEditorBuilder()));
		// Masked password column
		JFXTreeTableColumn<UserPassword, String> passwordColumn = new JFXTreeTableColumn<UserPassword, String>("Password");
		passwordColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<UserPassword, String> param) -> {
			if (param.getValue().getValue() == null) {
				return new SimpleStringProperty("???");
			}
            return new SimpleStringProperty(param.getValue().getValue().pass.replaceAll(".", "*"));
        });
		passwordColumn.setCellFactory((TreeTableColumn<UserPassword, String> param) ->
        	new GenericEditableTreeTableCell<UserPassword, String>(new TextFieldEditorBuilder()));
		
		// Initialize searchTreeTable content and listeners
		try {
			ObservableList<UserPassword> passwordObservable = FXCollections.observableArrayList(passwords);
			passwordsRoot = new RecursiveTreeItem<UserPassword>(passwordObservable, RecursiveTreeObject::getChildren);
			pastPasswordTable = new JFXTreeTableView<UserPassword>(passwordsRoot);
			pastPasswordTable.getColumns().add(accountNameColumn);
			pastPasswordTable.getColumns().add(timestampColumn);
			pastPasswordTable.getColumns().add(passwordColumn);
			pastPasswordTable.setShowRoot(false);
			// Style searchTreeTable and add click event that changes value of currently
			// selected account as well as UI elements about it
			pastPasswordTable.getSelectionModel().selectedItemProperty().addListener((o, oldVal, newVal) -> {
				if (newVal != null) {
					// Change selectedPassword
					selectedPassword = newVal.getValue();
				}
			});
		} catch (Exception e) {
			logger.log("Non-Fatal error on generating past passwords table: " + e.getMessage()
					+ ". No table will be added.");
			// Do nothing. Our accountNameColumn won't be populated or added.
			// The user will still have a semi-functional UI
		}
		// Add columns to body
		dialogLayout.setBody(pastPasswordTable);
		
		// Construct two actions - one to copy password and one to go back
		copyButton = new JFXButton("Copy Password");
		backButton = new JFXButton("Back");
		copyButton.setOnAction(e->copyPassword(e));
		backButton.setOnAction(e->dialog.close());
		// Add buttons to actions
		dialogLayout.setActions(backButton, copyButton);
		
		// Copy over logger
		this.logger = logger;
	}
	
	// Triggered by "Copy Password" button\
	private void copyPassword(ActionEvent event) {
		try {
			if (selectedPassword != null) {
				// Copy password onto clipboard
				ClipboardContent copiedPassword = new ClipboardContent();
				copiedPassword.putString(selectedPassword.pass);
				Clipboard.getSystemClipboard().setContent(copiedPassword);
				// Change copyButton text without changing the width
				copyButton.setText("Copied!");
				copyButton.setMinWidth(124);
				copyButton.setPrefWidth(124);
			}
		} catch (Exception e) {
			logger.logErr("Couldn't copy password: " + e.getMessage());
		}
	}
	
	// Shows dialog
	public void show() {
		dialog.show();
	}
}
