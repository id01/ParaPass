package one.id0.ppass.ui.popup;

import java.io.IOException;

import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import javafx.scene.layout.AnchorPane;
//import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextArea;

import one.id0.ppass.backend.PPassBackend;
import one.id0.ppass.ui.Page;
import one.id0.ppass.utils.UserAccount;

public class DescriptionPage extends Page {
	// FXML elements
	@FXML private VBox containerBox;
	@FXML private JFXButton copyButton;
	@FXML private Label accountTitle;
	@FXML private JFXTextArea accountDescription;
	@FXML private AnchorPane backgroundPane;
	
	// Class vars
	private UserAccount account;
	private PPassBackend backend;
	private String descPrevious; // Previous description value
	
	public DescriptionPage(Stage stage, PPassBackend backend, UserAccount account, ChangeListener<String> listener) throws IOException {
		// Init page and logger
		super(stage, "DescriptionPage.fxml", "ParaPass - Viewing " + account.accountName.get());
		
		// Copy over account, backend, and listener to class variables
		this.account = account;
		this.backend = backend;
		
		// Initialize saving mechanism for accountDescription
		descPrevious = account.description.getValue();
		accountDescription.focusedProperty().addListener((o, oldVal, newVal) -> {
			// If we just lost focus and the account description has been changed, fire off a change to the listener
			if (!newVal && !descPrevious.equals(accountDescription.getText())) {
				listener.changed(accountDescription.textProperty(), descPrevious, accountDescription.getText());
			}
		});
		
		// Save the account description one last time on stage close (we might not lose focus here)
		stage.setOnCloseRequest(e->{
			// Check if our description has been changed one last time just in case, and if so, trigger the change listener
			if (!descPrevious.equals(accountDescription.getText())) {
				listener.changed(accountDescription.textProperty(), descPrevious, accountDescription.getText());
			}
		});
		
		// Initialize title and description
		accountTitle.setText(account.accountName.getValue());
		accountDescription.setText(account.description.getValue());
		
		// Allow background to request focus
		backgroundPane.setOnMouseClicked(e->{
			backgroundPane.requestFocus();
		});
		
		// Show scene
		stage.setTitle("ParaPass - Viewing " + account.accountName.get());
		stage.setScene(scene);
	}
	
	// Triggered by "Copy Password" button
	@FXML
	protected void copyPassword(ActionEvent event) {
		try {
			// Copy password onto clipboard
			ClipboardContent copiedPassword = new ClipboardContent();
			copiedPassword.putString(backend.getPassword(account.accountID)[1]);
			Clipboard.getSystemClipboard().setContent(copiedPassword);
			// Change copyButton text without changing the width
			copyButton.setText("Copied!");
			copyButton.setMinWidth(124);
			copyButton.setPrefWidth(124);
		} catch (Exception e) {
			logger.logErr("Couldn't copy password: " + e.getMessage());
		}
	}
	
	// Triggered by "Back" button
	@FXML
	protected void closeStage(ActionEvent event) {
		stage.close();
	}
}
