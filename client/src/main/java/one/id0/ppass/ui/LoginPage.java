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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.concurrent.Task;
import javafx.application.Platform;

import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXSpinner;

import one.id0.ppass.backend.Logger;
import one.id0.ppass.backend.PPassBackend;

public class LoginPage extends Page {
	// Constant for PPassNetwork address
	final private String thatGrayShade = "#5B5B5B";
	final private String ppassAddress = "0x60dde9edd2a042bf720518aa9f1c78e1b506eb21";
	
	// Class variables
	private String ppassFilePath;
	private HBox loadingBox;
	private Text loadingText;
	
	// FXML elements that we need to interact with
	@FXML private StackPane everythingPane;
	@FXML private Pane loginFormPane;
	@FXML private JFXTextField usernameInput;
	@FXML private JFXPasswordField passwordInput;
	@FXML private JFXButton selectPPassButton;
	@FXML private Label ppassFileLabel;
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
	
	// Function that handles a login event
	@FXML protected void handleLogin(ActionEvent event) {
		// Replace login form with loading text
		loginFormPane.getChildren().clear();
		loginFormPane.getChildren().add(loadingBox);
		// Set backend and create another task for login or account creation
		Task<Void> enterTask = new Task<Void>() {
			@Override
			public Void call() throws Exception {
				backend = new PPassBackend(usernameInput.getText(), passwordInput.getText(),
						ppassFilePath, ppassAddress,
						!loginButton.getText().equals("Login"), logger);
				return null;
			}
		};
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
	}
	
	// Function that toggles the disabled state of loginButton
	protected void toggleLoginButtonState() {
		boolean notAllFieldsFilled = (ppassFilePath == null ||
				usernameInput.getText().equals("") || passwordInput.getText().equals(""));
		loginButton.setDisable(notAllFieldsFilled);
	}
	
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
