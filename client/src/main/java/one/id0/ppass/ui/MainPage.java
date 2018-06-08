package one.id0.ppass.ui;

import java.io.IOException;
import java.util.ArrayList;

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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.SplitPane;
//import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextArea;
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

import one.id0.ppass.backend.Logger;
import one.id0.ppass.backend.PPassBackend;
import one.id0.ppass.utils.UserAccount;
import one.id0.ppass.utils.UserPassword;

public class MainPage {
	// Character type string constants
	private final String charTypeLowercase = "abcdefghijklmnopqrstuvwxyz";
	private final String charTypeUppercase = "ACDFEFGHIJKLMNOPQRSTUVWXYZ";
	private final String charTypeDigits = "0123456789";
	private final String charTypeSymbols = "!@#$%^&*()_+-=`~{}[]|\\\'\";:/<>,.";
	
	// Class variables
	private PPassBackend backend;
	private Stage stage;
	private Logger logger;
	private TreeItem<UserAccount> accountRoot;
	private JFXTreeTableView<UserAccount> searchTreeTable;
	private UserAccount selectedAccount;
	
	// FXML elements that we need to interact with
	@FXML private StackPane everythingPane;
	@FXML private SplitPane containerPane;
	@FXML private VBox searchMenu;
	@FXML private JFXTextField searchInput;
	@FXML private BorderPane accountTitlePane;
	@FXML private Label accountTitle;
	@FXML private JFXTextArea accountDescription;
	@FXML private JFXButton accountCopyButton;

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
		// Remove focus from inputs if background is clicked
		everythingPane.addEventHandler(MouseEvent.MOUSE_PRESSED, e->{
			everythingPane.requestFocus();
		});
		containerPane.addEventHandler(MouseEvent.MOUSE_PRESSED, e->{
			containerPane.requestFocus();
		});
		
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
		
		// Create accountHamburger actions (new password, pin password, past passwords)
		JFXListView<Label> accountHamburgerActions = new JFXListView<Label>();
		Label newPasswordButton = new Label("New password");
		Label pinPasswordButton = new Label("Pin password"); // Note: This can also become the "Unpin password" button
		Label pastPasswordsButton = new Label("Past passwords");
		accountHamburgerActions.getItems().setAll(newPasswordButton, pinPasswordButton, pastPasswordsButton);
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
				createNewPassword(selectedAccount.accountName.getValue(), false);
			} else if (selected == pinPasswordButton) {
				togglePasswordPin(selectedAccount);
			} else if (selected == pastPasswordsButton) {
				showPastPasswords();
			} else {
				logger.logErr("How is this even possible? You clicked a nonexistent button.");
			}
			accountHamburgerPopup.hide();
		});
		
		// Initialize searchTreeTable columns
		// Account name column
		JFXTreeTableColumn<UserAccount, String> accountNameColumn = new JFXTreeTableColumn<UserAccount, String>("Name");
		accountNameColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<UserAccount, String> param) -> {
			if (param.getValue().getValue() == null) {
				return new SimpleStringProperty("null");
			}
            return param.getValue().getValue().accountName;
        });
		accountNameColumn.setCellFactory((TreeTableColumn<UserAccount, String> param) ->
        	new GenericEditableTreeTableCell<UserAccount, String>(new TextFieldEditorBuilder()));
		// Timestamp column
		JFXTreeTableColumn<UserAccount, String> timestampColumn = new JFXTreeTableColumn<UserAccount, String>("Last Accessed");
		timestampColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<UserAccount, String> param) -> {
			if (param.getValue().getValue() == null) {
				return new SimpleStringProperty("Never");
			}
            return param.getValue().getValue().timestamp;
        });
		timestampColumn.setCellFactory((TreeTableColumn<UserAccount, String> param) ->
        	new GenericEditableTreeTableCell<UserAccount, String>(new TextFieldEditorBuilder()));
		// Pinned column
		JFXTreeTableColumn<UserAccount, String> pinnedColumn = new JFXTreeTableColumn<UserAccount, String>("Pinned");
		pinnedColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<UserAccount, String> param) -> {
			if (param.getValue().getValue() == null) {
				return new SimpleStringProperty("");
			}
            return param.getValue().getValue().pinned;
        });
		pinnedColumn.setCellFactory((TreeTableColumn<UserAccount, String> param) ->
        	new GenericEditableTreeTableCell<UserAccount, String>(new TextFieldEditorBuilder()));
		
		// Initialize searchTreeTable content and listeners
		try {
			ArrayList<UserAccount> allAccounts = backend.getAllAccounts();
			ObservableList<UserAccount> accounts = FXCollections.observableArrayList(allAccounts);
			accountRoot = new RecursiveTreeItem<UserAccount>(accounts, RecursiveTreeObject::getChildren);
			searchTreeTable = new JFXTreeTableView<UserAccount>(accountRoot);
			searchTreeTable.getColumns().setAll(accountNameColumn, timestampColumn, pinnedColumn);
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
					// Change selectedAccount and right panel
					selectedAccount = newVal.getValue();
					accountTitle.setText(selectedAccount.accountName.getValue());
					accountDescription.setText(selectedAccount.description.getValue());
					accountDescription.setEditable(true);
					accountCopyButton.setText("Copy Password");
					accountCopyButton.setDisable(false);
					accountHamburger.setDisable(false);
					// Toggle pinPasswordButton text based on whether the selected password is currently pinned
					if (selectedAccount.pinned.getValue().equals("")) {
						pinPasswordButton.setText("Pin password");
					} else {
						pinPasswordButton.setText("Unpin password");
					}
					// Update last accessed timestamp for selectedAccount
					try {
						backend.updateAccountCache(selectedAccount.accountID, true, null, -1);
					} catch (Exception e) {
						logger.log("Failed to update last accessed timestamp: " + e.getMessage());
					}
				}
			});
		} catch (Exception e) {
			logger.log("Non-Fatal error on generating account name table: " + e.getMessage()
					+ ". No table will be added.");
			// Do nothing. Our accountNameColumn won't be populated or added.
			// The user will still have a semi-functional UI
		}
		
		// Initialize saving mechanism for accountDescription
		accountDescription.focusedProperty().addListener((o, oldVal, newVal)->{
			if (newVal) { // We just got focus. Don't do anything
			} else { // We just lost focus. Save our new description in the background and then update the sidebar.
				// First, save all vars just in case they change
				String currentSelectedAccountName = selectedAccount.accountName.getValue();
				byte[] currentSelectedAccountID = selectedAccount.accountID;
				String currentSelectedDescription = accountDescription.getText(); 
				// Create task to update and run it
				Task<Void> updateTask = new Task<Void>() {
					public Void call() throws Exception {
						backend.updateAccountCache(currentSelectedAccountID, true, currentSelectedDescription, -1);
						try {
							updateSearchTreeWithAccountAsync(currentSelectedAccountName);
						} catch (Exception e) {
							logger.log("Couldn't update search tree: " + e.getMessage());
						}
						return null;
					}
				};
				new Thread(updateTask).start();
			}
		});
		
		// Show our new scene
		stage.setTitle("ParaPass");
		stage.setScene(scene);
	}
	
	// Updates searchTreeTable (the sidebar JFXTreeTable)
	private void updateSearchTreeWithAccountAsync(String accountNameToAdd) throws Exception {
		// Add our new UserAccount and regenerate root 
		UserAccount userAccountToAdd = backend.getUserAccountObject(accountNameToAdd);
		// Add this child in the UI thread
		Platform.runLater(new Runnable() {
			public void run() {
				// Loop through the items in accountRoot. If we are updating a past value, update it and return.
				for (TreeItem<UserAccount> item : accountRoot.getChildren()) {
					if (item.getValue().accountName.getValue().equals(accountNameToAdd)) {
						item.setValue(userAccountToAdd);
						return;
					}
				}
				// This is a new value. Add to accountRoot.
				accountRoot.getChildren().add(new TreeItem<UserAccount>(userAccountToAdd));
			}
		});
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
	
	// Creates a new password for a specified account. Takes a UserAccount instance with the account to add. 
	// Setting addingAccount to true adds the account to the search tree and changes a little of the prompt text.
	protected void createNewPassword(String accountToAdd, boolean addingAccount) {
		// Create usedStrings array
		String[] usedStrings;
		if (addingAccount) {
			usedStrings = new String[]{"Add Account", "This will create a new account in the blockchain. "};
		} else {
			usedStrings = new String[]{"Change Account Password", "This will generate a new password for your account. "};
		}
		
		// Construct dialog and dialog layout, set heading
		JFXDialogLayout dialogLayout = new JFXDialogLayout();
		JFXDialog dialog = new JFXDialog(everythingPane, dialogLayout, JFXDialog.DialogTransition.CENTER);
		dialogLayout.setHeading(new Text(usedStrings[0]));
		
		// Construct dialog body
		TextFlow dialogBodyText = new TextFlow(new Text(usedStrings[1] +
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
		
		// Set action for continueButton to put password
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
					backend.putPassword(accountToAdd, charlistBuilder.toString(), passLength);
					// Set logger back to normal and do some UI cleanup.
					backend.setLogger(logger);
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							// Change cancelButton text to "close" and remove this button.
							cancelButton.setText("Close");
							cancelButton.setDisable(false);
							dialogLayout.setActions(cancelButton);
						}
					});
					// Add new account to search tree if it is a new one (note: we need to re-generate the root because it's read only)
					if (addingAccount) {
						try {
							updateSearchTreeWithAccountAsync(accountToAdd);
						} catch (Exception e) {
							logger.log("Couldn't update search tree: " + e.getMessage());
						}
					}
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
	
	// Toggles a password's pin state
	protected void togglePasswordPin(UserAccount account) {
		try {
			if (account.pinned.getValue().equals("")) {
				backend.updateAccountCache(account.accountID, true, null, 1);
				account.pinned.set("pinned");
			} else {
				backend.updateAccountCache(account.accountID, true, null, 0);
				account.pinned.set("");
			}
		} catch (Exception e) {
			logger.log("Failed to toggle password pin: " + e.getMessage());
		}
	}
	
	// Creates a new password for the account in Input
	@FXML
	protected void createNewAccount() {
		if (!searchInput.getText().equals("")) { // We use toLowerCase() here to remove visual gitches.
			createNewPassword(searchInput.getText().toLowerCase(), true); // Our backend is already not case-sensitive.
		}
	}
	
	// Shows past passwords using a PastPasswordPage on a popup stage
	protected void showPastPasswords() {
		try {
			ArrayList<UserPassword> pastPasswords = backend.getPastPasswords(selectedAccount.accountID);
			Stage popupStage = new Stage();
			new PastPasswordPage(popupStage, pastPasswords);
			popupStage.show();
		} catch (Exception e) {
			logger.log("Failed to show past passwords: " + e.getMessage());
		}
	}
}
