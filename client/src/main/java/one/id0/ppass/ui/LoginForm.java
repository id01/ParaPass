package one.id0.ppass.ui;

import java.io.File;
import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.concurrent.Task;
import javafx.application.Platform;

import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXDialogLayout;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXSpinner;

import one.id0.ppass.backend.Logger;
import one.id0.ppass.backend.PPassBackend;

public class LoginForm {
	// Constant for PPassNetwork address
	final private String ppassAddress = "0xf09d7f2623d109a0567fc377f080200c61f7bad7";
	
	// Class variables
	private Stage stage;
	private String keystoreFilePath;
	private String keystorePassword;
	private Logger logger;
	private HBox loadingBox;
	private Text loadingText;
	
	// FXML elements that we need to interact with
	@FXML private StackPane everythingPane;
	@FXML private Pane loginFormPane;
	@FXML private JFXTextField usernameInput;
	@FXML private JFXPasswordField passwordInput;
	@FXML private JFXButton selectKeystoreButton;
	@FXML private Label keystoreFileLabel;
	@FXML private JFXButton loginButton;
	
	// Other global variables
	private PPassBackend backend;
	
	// Constructor
	public LoginForm(Stage stage) throws IOException {
		// Copy over stage
		this.stage = stage;
		// Initialize login form
		FXMLLoader loader = new FXMLLoader(getClass().getResource("LoginForm.fxml"));
		loader.setController(this);
		Scene scene = new Scene(loader.load());
		stage.setTitle("ParaPass - Login");
		stage.setScene(scene);
		// Construct loading box
		JFXSpinner loadingSpinner = new JFXSpinner();
		loadingText = new Text("Please wait...");
		loadingBox = new HBox(loadingSpinner, loadingText);
		// Initialize logger. This is SO UGLY!!! But it works :D
		Logger.Handler onLog = new Logger.Handler() {
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
		logger = new Logger(onLog, onLog);
	}
	
	// Function that creates PPassBackend initialization task template without onSuccess
	protected Task<PPassBackend> createInitTaskTemplate() {
		// Initialization task: Initialize PPassBackend with specified parameters
		Task<PPassBackend> initTask = new Task<PPassBackend>() {
			@Override
			public PPassBackend call() throws Exception {
				return new PPassBackend(keystorePassword, keystoreFilePath, ppassAddress, logger);
			}
		};
		// Upon failure, log error
		initTask.setOnFailed(e->{
			logger.logErr(initTask.getException().getMessage());
		});
		// Return unfinished task
		return initTask;
	}
	
	// Function that handles a login event
	@FXML protected void handleLogin(ActionEvent event) {
		// Replace login form with loading text
		loginFormPane.getChildren().clear();
		loginFormPane.getChildren().add(loadingBox);
		// Initialization task: Initialize PPassBackend with specified parameters
		Task<PPassBackend> initTask = createInitTaskTemplate();
		// If successful, set backend and create another task for login
		initTask.setOnSucceeded(e->{
			backend = initTask.getValue();
			Task<Void> loginTask = new Task<Void>() {
				@Override
				public Void call() throws Exception {
					backend.userLogin(usernameInput.getText(), passwordInput.getText());
					return null;
				}
			};
			loginTask.setOnFailed(ee->{
				logger.logErr(loginTask.getException().getMessage());
			});
			loginTask.setOnSucceeded(ee->{
				try {
					new MainPage(stage, backend);
				} catch (IOException eee) {
					logger.logErr(eee.getMessage());
				}
			});
			// Launch login task
			new Thread(loginTask).start();
		});
		// Launch initTask
		new Thread(initTask).start();
	}
	
	// Function that selects a keystore file
	@FXML protected void selectKeystoreFile(ActionEvent event) {
		// Get keystore file path from user
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Select Keystore");
		File file = chooser.showOpenDialog(new Stage());
		if (file == null) { // User didn't pick anything
			keystoreFileLabel.setText("No file selected");
			return;
		}
		String chosenFilePath = file.getAbsolutePath();
		// Task to get whether wallet file has a password
		keystoreFileLabel.setText("Please wait...");
		Task<Boolean> checkWalletFileTask = new Task<Boolean>() {
			public Boolean call() throws IOException {
				// Check whether the wallet file is encrypted and whether it can be opened.
				boolean walletFileNotEncrypted = false;
				walletFileNotEncrypted = PPassBackend.checkWalletFile(chosenFilePath, "");
				return walletFileNotEncrypted;
			}
		};
		// On failure (IOException), tell the user we couldn't open the file
		checkWalletFileTask.setOnFailed(e->{
			keystoreFileLabel.setText("Couldn't open file");
		});
		// On success (no IOException), process result
		checkWalletFileTask.setOnSucceeded(e->{
			// Get whether the wallet file is encrypted. Act accordingly.
			boolean walletFileNotEncrypted = checkWalletFileTask.getValue();
			if (!walletFileNotEncrypted) { // If it's encrypted, prompt the user for the password.
				// Construct dialog and dialog layout, set heading
				JFXDialogLayout dialogLayout = new JFXDialogLayout();
				JFXDialog dialog = new JFXDialog(everythingPane, dialogLayout, JFXDialog.DialogTransition.CENTER);
				dialogLayout.setHeading(new Text("Wallet Encrypted"));
				// Construct dialog body
				TextFlow dialogBodyText = new TextFlow(new Text("This keystore file is encrypted. " +
						"Please type the password below to decrypt the file."));
				Text emptyLine = new Text(" ");
				JFXTextField keystorePasswordInput = new JFXTextField();
				keystorePasswordInput.setLabelFloat(true);
				keystorePasswordInput.setPromptText("Password");
				VBox bodyContainer = new VBox(dialogBodyText, emptyLine, keystorePasswordInput);
				dialogLayout.setBody(bodyContainer);
				// Construct dialog action to update the keystore file path and label content
				JFXButton continueButton = new JFXButton("Continue");
				continueButton.setOnAction((action)->{
					// Check if we can decrypt the wallet file
					boolean canDecryptWalletFile = false;
					try {
						canDecryptWalletFile = PPassBackend.checkWalletFile(chosenFilePath, keystorePasswordInput.getText());
					} catch (IOException ee) {
						keystoreFileLabel.setText("Couldn't open file");
						return;
					}
					// If we can, set keystoreFilePath and update label. Otherwise, show the user that the password is invalid
					if (canDecryptWalletFile) {
						keystoreFilePath = chosenFilePath;
						keystoreFileLabel.setText(chosenFilePath);
						keystorePassword = keystorePasswordInput.getText();
					} else {
						keystoreFileLabel.setText("Invalid Password");
					}
					// Close the dialog
					dialog.close();
				});
				dialogLayout.setActions(continueButton);
				// Create dialog and show
				dialog.show();
			} else { // If keystore doesn't have a password, update the keystore information directly.
				keystoreFilePath = chosenFilePath;
				keystoreFileLabel.setText(chosenFilePath);
				keystorePassword = "";
			}
		});
		new Thread(checkWalletFileTask).start();
	}
}
