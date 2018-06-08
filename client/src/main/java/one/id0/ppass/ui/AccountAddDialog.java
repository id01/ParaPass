package one.id0.ppass.ui;

import com.jfoenix.controls.JFXButton;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXSlider;
import com.jfoenix.controls.JFXSpinner;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXDialogLayout;

import one.id0.ppass.backend.Logger;
import one.id0.ppass.backend.PPassBackend;

public class AccountAddDialog {
	// Character type string constants
	private final String charTypeLowercase = "abcdefghijklmnopqrstuvwxyz";
	private final String charTypeUppercase = "ACDFEFGHIJKLMNOPQRSTUVWXYZ";
	private final String charTypeDigits = "0123456789";
	private final String charTypeSymbols = "!@#$%^&*()_+-=`~{}[]|\\\'\";:/<>,.";
	
	// Global vars
	private JFXDialog dialog;
	private VBox bodyContainer;
	
	// Inputs
	JFXCheckBox lowercaseCheckbox;
	JFXCheckBox uppercaseCheckbox;
	JFXCheckBox digitsCheckbox;
	JFXCheckBox symbolsCheckbox;
	JFXSlider passLengthSlider;
	
	public AccountAddDialog(PPassBackend backend, StackPane everythingPane, boolean addingAccount, String accountToAdd, Runnable callback) {
		// Create usedStrings array
		String[] usedStrings;
		if (addingAccount) {
			usedStrings = new String[]{"Add Account", "This will create a new account in the blockchain. "};
		} else {
			usedStrings = new String[]{"Change Account Password", "This will generate a new password for your account. "};
		}
		
		// Construct dialog and dialog layout, set heading
		JFXDialogLayout dialogLayout = new JFXDialogLayout();
		dialog = new JFXDialog(everythingPane, dialogLayout, JFXDialog.DialogTransition.CENTER);
		dialogLayout.setHeading(new Text(usedStrings[0]));
		
		// Construct dialog body
		TextFlow dialogBodyText = new TextFlow(new Text(usedStrings[1] +
				"Please note that this operation will have a gas cost. Select the character types you " +
				"want to include in your password below."));
		Text emptyLine = new Text(" ");
		// Character type inputs and password length slider for dialog body
		lowercaseCheckbox = new JFXCheckBox("Lowercase Letters"); lowercaseCheckbox.setSelected(true);
		uppercaseCheckbox = new JFXCheckBox("Uppercase Letters"); uppercaseCheckbox.setSelected(true);
		digitsCheckbox = new JFXCheckBox("Digits"); digitsCheckbox.setSelected(true);
		symbolsCheckbox = new JFXCheckBox("Symbols"); symbolsCheckbox.setSelected(true);
		HBox.setMargin(lowercaseCheckbox, new Insets(10,10,10,10));
		HBox.setMargin(uppercaseCheckbox, new Insets(10,10,10,10));
		HBox.setMargin(digitsCheckbox, new Insets(10,10,10,10));
		HBox.setMargin(symbolsCheckbox, new Insets(10,10,10,10));
		HBox characterTypeInputs = new HBox(lowercaseCheckbox, uppercaseCheckbox, digitsCheckbox, symbolsCheckbox);
		passLengthSlider = new JFXSlider(12, 64, 32);
		// Finalize dialog body
		bodyContainer = new VBox(dialogBodyText, emptyLine, characterTypeInputs, passLengthSlider);
		dialogLayout.setBody(bodyContainer);
		
		// Construct two actions - one to finalize add and one to cancel
		JFXButton continueButton = new JFXButton("Continue");
		JFXButton cancelButton = new JFXButton("Cancel");
		
		// Set action for continueButton to put password
		continueButton.setOnAction((action)->{
			// Disable both buttons
			cancelButton.setDisable(true);
			continueButton.setDisable(true);
			// Run put password
			runPutPassword(backend, accountToAdd, new Runnable() {
				@Override
				public void run() {
					// Do some UI cleanup
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							// Change cancelButton text to "close" and remove continue button.
							cancelButton.setText("Close");
							cancelButton.setDisable(false);
							dialogLayout.setActions(cancelButton);
							// Run callback
							callback.run();
						}
					});
				}
			});
		});
		
		// Create button to cancel, then set actions for dialogLayout
		cancelButton.setOnAction((action)->{
			dialog.close();
		});
		dialogLayout.setActions(cancelButton, continueButton);
	}
	
	// Run put password
	private void runPutPassword(PPassBackend backend, String accountToAdd, Runnable callback) {
		// Create loading screen
		JFXSpinner spinner = new JFXSpinner();
		Text loadingText = new Text("Please wait...");
		HBox.setMargin(loadingText, new Insets(16, 16, 16, 16));
		HBox loadingBox = new HBox(spinner, loadingText);
		loadingBox.setAlignment(Pos.CENTER);
		// It's too late to turn back...
		dialog.setOverlayClose(false);
		// Replace bodyContainer children with loadingBox
		bodyContainer.getChildren().clear();
		bodyContainer.getChildren().add(loadingBox);
		// Create a log handler for the dialog (to use just for now)
		Logger.Handler dialogLoggerHandler = new Logger.Handler() {
			public Runnable getRunnable(String toLog) {
				return new Runnable() {
					public void run() {
						Platform.runLater(new Runnable() {
							public void run() {
								System.out.println(toLog);
								loadingText.setText(toLog);
							}
						});
					}
				};
			}
		};
		// Create a new task to put the password
		Task<Void> putPasswordTask = new Task<Void>() {
			@Override
			public Void call() {
				// Set the backend logger to a new logger with our new handler
				Logger tempLogger = new Logger(dialogLoggerHandler, dialogLoggerHandler);
				backend.setLogger(tempLogger);
				// Get input from user input nodes
				int passLength = (int)passLengthSlider.getValue();
				StringBuilder charlistBuilder = new StringBuilder();
				if (lowercaseCheckbox.isSelected()) {
					charlistBuilder.append(charTypeLowercase);
				}
				if (uppercaseCheckbox.isSelected()) {
					charlistBuilder.append(charTypeUppercase);
				}
				if (digitsCheckbox.isSelected()) {
					charlistBuilder.append(charTypeDigits);
				}
				if (symbolsCheckbox.isSelected()) {
					charlistBuilder.append(charTypeSymbols);
				}
				// Run putPassword with inputs
				try {
					backend.putPassword(accountToAdd, charlistBuilder.toString(), passLength);
				} catch (Exception e) {
					tempLogger.logErr(e.getMessage());
				}
				Platform.runLater(callback);
				return null;
			}
		};
		// Run our task
		new Thread(putPasswordTask).start();
	}
	
	// Show dialog
	public void show() {
		dialog.show();
	}
}
