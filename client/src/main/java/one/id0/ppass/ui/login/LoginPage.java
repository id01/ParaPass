package one.id0.ppass.ui.login;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
//import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.concurrent.Task;
import javafx.application.Platform;

import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXDialogLayout;
import com.jfoenix.controls.JFXSpinner;

import one.id0.ppass.backend.Logger;
import one.id0.ppass.backend.PPassBackend;
import one.id0.ppass.ui.HoverTooltip;
import one.id0.ppass.ui.Page;
import one.id0.ppass.ui.main.MainPage;

public class LoginPage extends Page {
	// Constant for PPassNetwork address
	final private String thatGrayShade = "#5B5B5B";
	final private String ppassAddress = "0x24c08fb0f8fa924d49db9713034e26f3a358a7fa";
	
	// Class variables
	private enum FormMode {
		LOGIN, CREATE, PPASS
	}
	private FormMode currentMode;
	private String ppassFilePath;
	private String keystoreFilePath;
	private String keystorePassword;
	private HBox loadingBox;
	private TextFlow loadingTextFlow;
	private Text loadingText;
	
	// FXML elements that we need to interact with
	// Wrapper panes
	@FXML private StackPane everythingPane;
	@FXML private Pane loginFormPane;
	@FXML private GridPane loginFormGridPane;
	// Username and password inputs
	@FXML private JFXTextField usernameInput;
	@FXML private JFXPasswordField passwordInput;
	// PPass selection pane
	@FXML private Pane selectPPassPane;
	@FXML private JFXButton selectPPassButton;
	@FXML private Label ppassFileLabel;
	@FXML private JFXButton createPPassButton;
	// Keystore selection pane
	@FXML private Pane selectKeystorePane;
	@FXML private JFXButton selectKeystoreButton;
	@FXML private Label keystoreFileLabel;
	// Remember me, create account, and login/create button
	@FXML private JFXButton loginButton;
	@FXML private JFXButton toggleCreateAccountButton;
	@FXML private JFXCheckBox rememberMeCheckbox;
	
	// Other global variables
	private PPassBackend backend;
	
	// Constructor
	public LoginPage(Stage stage) throws IOException {
		// Initialize page and logger
		super(stage, "LoginPage.fxml", "ParaPass - Login");
		
		// Construct loading box
		JFXSpinner loadingSpinner = new JFXSpinner();
		loadingText = new Text("Please wait...");
		loadingTextFlow = new TextFlow(loadingText);
		loadingTextFlow.setPrefWidth(200.0);
		loadingTextFlow.setMaxWidth(200.0);
		HBox.setMargin(loadingTextFlow, new Insets(32,32,32,32));
		loadingBox = new HBox(loadingSpinner, loadingTextFlow);
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
								loadingBox.getChildren().setAll(loadingTextFlow, backButton);
							}
						});
					}
				};
			}
		};
		logger = new Logger(onLog, onErr);

		// Create tooltips
		new HoverTooltip("Select your account's PPass file", selectPPassButton, stage);
		new HoverTooltip("Generate a PPass file", createPPassButton, stage);
		new HoverTooltip("Select your ethereum wallet's keystore file", selectKeystoreButton, stage);
		new HoverTooltip("NOT SECURE - Logs you in automatically", rememberMeCheckbox, stage);
		
		// Set current mode to login and hide the keystore file selection pane
		currentMode = FormMode.LOGIN;
		everythingPane.getChildren().remove(selectKeystorePane);
		
		// Take the stage
		super.enterStage();
	}
	
	// LOGIN/CREATE MODE FUNCTIONS
	// Function that selects a ppass file
	@FXML protected void selectPPassFile(ActionEvent event) {
		// Get ppass file path from user
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Select PPass File");
		File file = chooser.showOpenDialog(new Stage());
		if (file == null) { // User didn't pick anything
			ppassFileLabel.setText("No file selected");
			return;
		}
		String chosenFilePath = file.getAbsolutePath();
		// Put it into ppassFilePath and ppassFileLabel
		ppassFilePath = chosenFilePath;
		ppassFileLabel.setText(chosenFilePath);
		toggleLoginButtonState();
	}
	
	// Function that toggles the disabled state of loginButton
	protected void toggleLoginButtonState() {
		boolean notAllFieldsFilled = ((ppassFilePath == null && currentMode != FormMode.PPASS) ||
				(keystoreFilePath == null && currentMode == FormMode.PPASS) ||
				usernameInput.getText().equals("") || passwordInput.getText().equals(""));
		loginButton.setDisable(notAllFieldsFilled);
	}
	
	// Function that toggles our mode - to create an account or to login to one
	@FXML protected void toggleCreateAccount(ActionEvent event) {
		// If the current mode is to create a ppass file using a keystore, switch the selection pane back to selectPPassPane
		if (currentMode == FormMode.PPASS) {
			loginFormGridPane.getChildren().remove(selectKeystorePane);
			loginFormGridPane.add(selectPPassPane, 0, 2);
		}
		// Toggle login or create
		if (loginButton.getText().equals("Login")) {
			loginButton.setText("Create");
			toggleCreateAccountButton.setText("Login to Account");
			currentMode = FormMode.CREATE;
		} else {
			loginButton.setText("Login");
			currentMode = FormMode.LOGIN;
			toggleCreateAccountButton.setText("Create Account");
		}
	}
	
	// PPASS CREATION MODE FUNCTIONS
	// Function that switches to PPass creation mode
	@FXML protected void switchToCreatePPass(ActionEvent event) {
		loginButton.setText("Generate");
		toggleCreateAccountButton.setText("Login to Account");
		loginFormGridPane.getChildren().remove(selectPPassPane);
		loginFormGridPane.add(selectKeystorePane, 0, 2);
		currentMode = FormMode.PPASS;
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
	
	// SUBMIT EVENT FUNCTION
	// Function that handles a login, create, or create PPass event
	@FXML protected void handleLogin(ActionEvent event) {
		// Replace login form with loading text
		loginFormPane.getChildren().clear();
		loginFormPane.getChildren().add(loadingBox);
		// Set our task according to the type of event that is happening
		Task<Void> enterTask;
		if (currentMode == FormMode.PPASS) { // If this is a create PPass file event
			// Set task to create a PPass file and save it (if it doesn't already exist)
			// This will always throw an exception because we want it to log a success as an error so the user can log back in
			enterTask = new Task<Void>() {
				@Override
				public Void call() throws Exception {
					String ppassFileContent = PPassBackend.generatePPassFile(usernameInput.getText(), passwordInput.getText(),
							keystoreFilePath, keystorePassword);
					String keystoreFileDir = new File(keystoreFilePath).getParentFile().getAbsolutePath();
					String ppassFileName = keystoreFileDir + '/' + usernameInput.getText() + ".ppass";
					boolean ppassFileExists = new File(ppassFileName).exists();
					if (!ppassFileExists) {
						PrintWriter ppassFileWriter = new PrintWriter(new FileWriter(ppassFileName));
						ppassFileWriter.write(ppassFileContent);
						ppassFileWriter.close();
						throw new Exception("Created File " + ppassFileName +
								". You may now create an account with this file.");
					} else {
						throw new Exception("File " + ppassFileName + " already exists!");
					}
				}
			};
			// If this failed, log the error. If it succeeds, 
			// log the success as an error so that the user can go back and log in
			enterTask.setOnFailed(ee->{
				logger.logErr(enterTask.getException().getMessage());
			});
			enterTask.setOnSucceeded(ee->{
				logger.logErr("You should never reach this code. This success is actually a failure.\n" + 
						" The function you called should always return an exception.");
			});
			// Set loading text, as the function to generate a PPass file doesn't log anything
			logger.log("Generating PPass file...");
		} else { // If this is a login or create account event
			// Set backend and create another task for login or account creation based on form mode
			enterTask = new Task<Void>() {
				@Override
				public Void call() throws Exception {
					backend = new PPassBackend(usernameInput.getText(), passwordInput.getText(),
							ppassFilePath, ppassAddress, currentMode == FormMode.CREATE, logger);
					return null;
				}
			};
			// If this failed, log the error. If it succeeds, move onto main page
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
		}
		// Launch task
		new Thread(enterTask).start();
	}
}
