package one.id0.ppass.ui.popup;

import java.io.IOException;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.Stage;
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
	
	// Class vars
	private UserAccount account;
	private PPassBackend backend;
	
	public DescriptionPage(Stage stage, PPassBackend backend, UserAccount account) throws IOException {
		// Init page and logger
		super(stage, "DescriptionPage.fxml", "ParaPass - Viewing " + account.accountName.get());
		
		// Copy over account and backend to class variables
		this.account = account;
		this.backend = backend;
		
		// Initialize saving mechanism for accountDescription
		accountDescription.focusedProperty().addListener((o, oldVal, newVal)->{
			if (newVal) { // We just got focus. Don't do anything
			} else { // We just lost focus. Save our new description to the backend.
				// MainPage will update the account upon this window closing
				// Create task to update and run it
				byte[] currentSelectedAccountID = account.accountID;
				String currentSelectedDescription = accountDescription.getText(); 
				Task<Void> updateTask = new Task<Void>() {
					public Void call() throws Exception {
						backend.updateAccountCache(currentSelectedAccountID, true, currentSelectedDescription, -1);
						return null;
					}
				};
				new Thread(updateTask).start();
			}
		});
		
		// Initialize title and description
		accountTitle.setText(account.accountName.getValue());
		accountDescription.setText(account.description.getValue());
		
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
