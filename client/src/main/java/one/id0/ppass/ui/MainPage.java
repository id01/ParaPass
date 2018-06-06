package one.id0.ppass.ui;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.BorderPane;
//import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
//import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;
import javafx.application.Platform;
//import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXSlider;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXDialogLayout;
import com.jfoenix.controls.JFXHamburger;
import com.jfoenix.controls.JFXRippler;
import com.jfoenix.controls.JFXSpinner;
import com.jfoenix.controls.JFXPopup;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXTreeTableView;
import com.jfoenix.controls.JFXTreeTableColumn;
import com.jfoenix.controls.RecursiveTreeItem;
import com.jfoenix.controls.cells.editors.TextFieldEditorBuilder;
import com.jfoenix.controls.cells.editors.base.GenericEditableTreeTableCell;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import com.jfoenix.transitions.hamburger.HamburgerBasicCloseTransition;

import one.id0.ppass.backend.Logger;
import one.id0.ppass.backend.PPassBackend;
import one.id0.ppass.utils.UserAccount;

public class MainPage {
	// Character type string constants
	final String charTypeLowercase = "abcdefghijklmnopqrstuvwxyz";
	final String charTypeUppercase = "ACDFEFGHIJKLMNOPQRSTUVWXYZ";
	final String charTypeDigits = "0123456789";
	final String charTypeSymbols = "!@#$%^&*()_+-=`~{}[]|\\\'\";:/<>,.";
	
	// Class variables
	private PPassBackend backend;
	private Stage stage;
	private UserAccount selectedAccount;
	private JFXTreeTableView<UserAccount> searchTreeTable;
	private Logger logger;
	
	// FXML elements that we need to interact with
	@FXML private StackPane everythingPane;
	@FXML private VBox searchMenu;
	@FXML private BorderPane accountTitlePane;
	@FXML private Label accountTitle;
	@FXML private Text accountDescription;
	@FXML private JFXButton accountCopyButton;
	@FXML private JFXTextField searchInput;

	// Constructor
	public MainPage(Stage stage, PPassBackend backend) throws IOException {
		// Copy over stage and backend
		this.stage = stage;
		this.backend = backend;
		// Initialize main page
		FXMLLoader loader = new FXMLLoader(getClass().getResource("MainPage.fxml"));
		loader.setController(this);
		Scene scene = new Scene(loader.load());
		// Current account selected is none
		selectedAccount = null;
		
		// Initialize logger. This is SO UGLY!!! But it works :D
		Logger.Handler onLog = new Logger.Handler() {
			public Runnable getRunnable(String toLog) {
				return new Runnable() {
					public void run() {
						Platform.runLater(new Runnable() {
							public void run() {
								System.out.println(toLog);
							}
						});
					}
				};
			}
		};
		logger = new Logger(onLog, onLog);
		backend.setLogger(logger);
		
		// Create accountHamburger actions (new password and past passwords)
		JFXListView<Label> accountHamburgerActions = new JFXListView<Label>();
		Label newPasswordButton = new Label("New password");
		Label pastPasswordsButton = new Label("Past passwords");
		accountHamburgerActions.getItems().setAll(newPasswordButton, pastPasswordsButton);
		// Create wrappers for JFXListView
		JFXHamburger accountHamburger = new JFXHamburger();
		accountHamburger.getStyleClass().add("darkHamburger");
		accountHamburger.setDisable(true);
		JFXPopup accountHamburgerPopup = new JFXPopup(accountHamburgerActions);
		JFXRippler accountHamburgerRippler = new JFXRippler(accountHamburger, JFXRippler.RipplerMask.CIRCLE,
				JFXRippler.RipplerPos.BACK);
		// Initialize accountHamburger and add to accountTitlePane
		accountHamburgerRippler.setOnMouseClicked(e -> accountHamburgerPopup.show(accountHamburgerRippler,
				JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.RIGHT));
		accountTitlePane.setRight(accountHamburgerRippler);
		// Set event handler for accountHamburger actions to run the selected function and close the popup
		accountHamburgerActions.setOnMouseClicked(e->{
			Label selected = accountHamburgerActions.getSelectionModel().getSelectedItem();
			if (selected == newPasswordButton) {
				createNewPassword();
			} else if (selected == pastPasswordsButton) {
				showPastPasswords();
			} else {
				logger.logErr("How is this even possible? You clicked a nonexistent button.");
			}
			accountHamburgerPopup.hide();
		});
		
		// Initialize searchTreeTable columns
		JFXTreeTableColumn<UserAccount, String> accountNameColumn = new JFXTreeTableColumn<UserAccount, String>("Name");
		accountNameColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<UserAccount, String> param) -> {
			if (param.getValue().getValue() == null) {
				return new SimpleStringProperty("null");
			}
            return param.getValue().getValue().accountName;
        });
		accountNameColumn.setCellFactory((TreeTableColumn<UserAccount, String> param) ->
        	new GenericEditableTreeTableCell<UserAccount, String>(new TextFieldEditorBuilder()));
		// Initialize searchTreeTable content
		try {
			ObservableList<UserAccount> accounts = FXCollections.observableArrayList(backend.getAllAccounts());
			final TreeItem<UserAccount> root = new RecursiveTreeItem<UserAccount>(accounts, RecursiveTreeObject::getChildren);
			searchTreeTable = new JFXTreeTableView<UserAccount>(root);
			searchTreeTable.getColumns().setAll(accountNameColumn);
			searchInput.textProperty().addListener((o, oldVal, newVal) -> {
				searchTreeTable.setPredicate(accProp -> {
					final UserAccount account = accProp.getValue();
					return account.accountName.toString().contains(newVal);
				});
			});
			searchTreeTable.setShowRoot(false);
			searchMenu.getChildren().add(searchTreeTable);
			// Style searchTreeTable and add click event that changes value of currently
			// selected account as well as UI elements about it
			searchTreeTable.getStyleClass().add("dark");
			searchTreeTable.getSelectionModel().selectedItemProperty().addListener((o, oldVal, newVal) -> {
				if (newVal != null) {
					selectedAccount = newVal.getValue();
					accountTitle.setText(selectedAccount.accountName.getValue());
					accountDescription.setText("Descriptions not implemented yet");
					accountCopyButton.setText("Copy Password");
					accountCopyButton.setDisable(false);
					accountHamburger.setDisable(false);
				}
			});
		} catch (Exception e) {
			logger.log("Non-Fatal error on generating account name table: " + e.getMessage()
					+ ". No table will be added.");
			// Do nothing. Our accountNameColumn won't be populated or added.
			// The user will still have a semi-functional UI
		}
		
		// Show our new scene
		stage.setTitle("ParaPass");
		stage.setScene(scene);
	}
	
	// Copies the currently selected account's password if an account is selected
	@FXML
	protected void copyPassword(ActionEvent event) {
		try {
			if (selectedAccount != null) {
				// Get password and put into clipboard
				String[] accnpass = backend.getPassword(selectedAccount.accountID);
				ClipboardContent copiedPassword = new ClipboardContent();
				copiedPassword.putString(accnpass[1]);
				Clipboard.getSystemClipboard().setContent(copiedPassword);
				// Change accountCopyButton text without changing the width
				accountCopyButton.setText("Copied!");
				accountCopyButton.setMinWidth(126);
				accountCopyButton.setPrefWidth(126);
			}
		} catch (Exception e) {
			logger.logErr("Couldn't copy password: " + e.getMessage());
		}
	}
	
	protected void createNewPassword() {
		// Construct dialog and dialog layout, set heading
		JFXDialogLayout dialogLayout = new JFXDialogLayout();
		JFXDialog dialog = new JFXDialog(everythingPane, dialogLayout, JFXDialog.DialogTransition.CENTER);
		dialogLayout.setHeading(new Text("Change Account Password"));
		
		// Construct dialog body
		TextFlow dialogBodyText = new TextFlow(new Text("This will generate a new password for your account. " +
				"Please note that this operation will have a gas cost. Select the character types you " +
				"want to include in your password below."));
		Text emptyLine = new Text(" ");
		// Character type inputs and password length slider for dialog body
		JFXCheckBox lowercaseCheckbox = new JFXCheckBox("Lowercase Letters"); lowercaseCheckbox.setSelected(true);
		JFXCheckBox uppercaseCheckbox = new JFXCheckBox("Uppercase Letters"); uppercaseCheckbox.setSelected(true);
		JFXCheckBox digitsCheckbox = new JFXCheckBox("Digits"); digitsCheckbox.setSelected(true);
		JFXCheckBox symbolsCheckbox = new JFXCheckBox("Symbols"); symbolsCheckbox.setSelected(true);
		HBox.setMargin(lowercaseCheckbox, new Insets(10,10,10,10));
		HBox.setMargin(uppercaseCheckbox, new Insets(10,10,10,10));
		HBox.setMargin(digitsCheckbox, new Insets(10,10,10,10));
		HBox.setMargin(symbolsCheckbox, new Insets(10,10,10,10));
		HBox characterTypeInputs = new HBox(lowercaseCheckbox, uppercaseCheckbox, digitsCheckbox, symbolsCheckbox);
		JFXSlider passLengthSlider = new JFXSlider(12, 64, 32);
		// Finalize dialog body
		VBox bodyContainer = new VBox(dialogBodyText, emptyLine, characterTypeInputs, passLengthSlider);
		dialogLayout.setBody(bodyContainer);
		
		// Construct two actions - one to finalize add and one to cancel
		JFXButton continueButton = new JFXButton("Continue");
		JFXButton cancelButton = new JFXButton("Cancel");
		// Create loading screen
		JFXSpinner spinner = new JFXSpinner();
		Text loadingText = new Text("Please wait...");
		HBox.setMargin(loadingText, new Insets(16, 16, 16, 16));
		HBox loadingBox = new HBox(spinner, loadingText);
		loadingBox.setAlignment(Pos.CENTER);
		
		// Set action for continueButton to get password
		continueButton.setOnAction((action)->{
			// Disable both buttons
			cancelButton.setDisable(true);
			continueButton.setDisable(true);
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
			// Set the backend logger to a new logger with our new handler
			backend.setLogger(new Logger(dialogLoggerHandler, dialogLoggerHandler));
			// Create a new task to put the password
			Task<Void> putPasswordTask = new Task<Void>() {
				@Override
				public Void call() throws Exception {
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
					backend.putPassword(selectedAccount.accountName.getValue(),
							charlistBuilder.toString(), passLength);
					// Set logger back and remove this button. Change cancelButton text to "close"
					backend.setLogger(logger);
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							cancelButton.setText("Close");
							cancelButton.setDisable(false);
							dialogLayout.setActions(cancelButton);
						}
					});
					return null;
				}
			};
			// Run our task
			new Thread(putPasswordTask).start();
		});
		
		// Create button to cancel, then set actions for dialogLayout
		cancelButton.setOnAction((action)->{
			dialog.close();
		});
		dialogLayout.setActions(cancelButton, continueButton);
		// Create dialog and show
		dialog.show();
	}
	
	protected void showPastPasswords() {
		
	}
}
