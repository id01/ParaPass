package one.id0.ppass.ui;

import java.io.File;
import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
//import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.input.MouseEvent;
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

public class LoginPage extends Page {
	// Constant for PPassNetwork address
	final private String thatGrayShade = "#5B5B5B";
	final private String ppassAddress = "0xcfc2a3d81adf61571e5a83a96a49fe82189259d9";
	
	// Class variables
	private String keystoreFilePath;
	private String keystorePassword;
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
	@FXML private JFXButton toggleCreateAccountButton;
	
	// Other global variables
	private PPassBackend backend;
	
	// Constructor
	public LoginPage(Stage stage) throws IOException {
		// Initialize page and logger
		super(stage, "LoginPage.fxml", "ParaPass - Login");
		
		// Construct loading box
		JFXSpinner loadingSpinner = new JFXSpinner();
		loadingText = new Text("Please wait...");
		HBox.setMargin(loadingText, new Insets(32,32,32,32));
		loadingBox = new HBox(loadingSpinner, loadingText);
		loadingBox.setAlignment(Pos.CENTER);
		
		// Toggle login button status on username or password input change
		usernameInput.textProperty().addListener((o, oldVal, newVal)->toggleLoginButtonState());
		passwordInput.textProperty().addListener((o, oldVal, newVal)->toggleLoginButtonState());
		// Remove focus from inputs if background is clicked
		everythingPane.addEventHandler(MouseEvent.MOUSE_PRESSED, e->{
			everythingPane.requestFocus();
		});
		
		// Initialize logger. This is SO UGLY!!! But it works :D
		Logger.Handler onLog = new Logger.Handler() {
			public Runnable getRunnable(String toLog) {
				return new Runnable() {
					public void run() {
						Platform.runLater(new Runnable() {
							public void run() {
								// Update UI with log string
								System.out.println(toLog);
								loadingText.setText(toLog);
							}
						});
					}
				};
			}
		};
		Logger.Handler onErr = new Logger.Handler() {
			public Runnable getRunnable(String toLog) {
				return new Runnable() {
					public void run() {
						Platform.runLater(new Runnable() {
							public void run() {
								// Error! Remove loading circle and allow the user to escape
								System.out.println(toLog);
								loadingText.setText(toLog);
								loadingBox.getChildren().clear();
								JFXButton backButton = new JFXButton("Return to Login");
								backButton.setTextFill(Color.web(thatGrayShade));
								backButton.setOnAction(e->{
									try {
										new LoginPage(stage);
									} catch (IOException ee) {
										System.out.println("Unexpected exception: " + ee.getMessage() +
												"\nStack trace: \n" + ee.getStackTrace());
									}
								});
								loadingBox.getChildren().setAll(loadingText, backButton);
							}
						});
					}
				};
			}
		};
		logger = new Logger(onLog, onErr);
		
		// Take the stage
		super.enterStage();
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
		// If successful, set backend and create another task for login or account creation
		initTask.setOnSucceeded(e->{
			backend = initTask.getValue();
			Task<Void> enterTask;
			if (loginButton.getText().equals("Login")) {
				enterTask = new Task<Void>() {
					@Override
					public Void call() throws Exception {
						backend.userLogin(usernameInput.getText(), passwordInput.getText());
						return null;
					}
				};
			} else {
				enterTask = new Task<Void>() {
					@Override
					public Void call() throws Exception {
						backend.userSetup(usernameInput.getText(), passwordInput.getText());
						return null;
					}
				};
			}
			enterTask.setOnFailed(ee->{
				logger.logErr(enterTask.getException().getMessage());
			});
			enterTask.setOnSucceeded(ee->{
				try {
					new MainPage(stage, backend);
				} catch (IOException eee) {
					logger.logErr(eee.getMessage());
				}
			});
			// Launch login task
			new Thread(enterTask).start();
		});
		// Launch initTask
		new Thread(initTask).start();
	}
	
	// Function that toggles the disabled state of loginButton
	protected void toggleLoginButtonState() {
		boolean notAllFieldsFilled = (keystoreFilePath == null || keystorePassword == null ||
				usernameInput.getText().equals("") || passwordInput.getText().equals(""));
		loginButton.setDisable(notAllFieldsFilled);
	}
	
	// Function that selects a keystore file
	@FXML protected void selectKeystoreFile(ActionEvent event) {
		// Reset keystoreFilePath and keystorePassword
		keystoreFilePath = null;
		keystorePassword = null;
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
					// Check if we can decrypt the wallet file and act accordingly
					try {
						boolean canDecryptWalletFile = PPassBackend.checkWalletFile(chosenFilePath,
								keystorePasswordInput.getText());
						// If we can, set keystoreFilePath, update label, and enable login button.
						// Otherwise, show the user that the password is invalid.
						if (canDecryptWalletFile) {
							keystoreFilePath = chosenFilePath;
							keystoreFileLabel.setText(chosenFilePath);
							keystorePassword = keystorePasswordInput.getText();
						} else {
							keystoreFileLabel.setText("Invalid Password");
						}
					} catch (IOException ee) {
						keystoreFileLabel.setText("Couldn't open file");
					}
					// Close the dialog, toggle login button state
					dialog.close();
					toggleLoginButtonState();
				});
				dialogLayout.setActions(continueButton);
				// Create dialog and show
				dialog.show();
			} else { // If keystore doesn't have a password, update the keystore information directly. Toggle login button state
				keystoreFilePath = chosenFilePath;
				keystoreFileLabel.setText(chosenFilePath);
				keystorePassword = "";
				toggleLoginButtonState();
			}
		});
		new Thread(checkWalletFileTask).start();
	}
	
	// Function that toggles our mode - to create an account or to login to one
	@FXML protected void toggleCreateAccount(ActionEvent event) {
		if (loginButton.getText().equals("Login")) {
			loginButton.setText("Create");
			toggleCreateAccountButton.setText("Login to Account");
		} else {
			loginButton.setText("Login");
			toggleCreateAccountButton.setText("Create Account");
		}
	}
}
