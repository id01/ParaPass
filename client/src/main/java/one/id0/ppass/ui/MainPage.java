package one.id0.ppass.ui;

import java.io.IOException;
import java.util.ArrayList;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
//import javafx.geometry.Pos;
//import javafx.geometry.Insets;
//import javafx.scene.Scene;
//import javafx.scene.text.Text;
//import javafx.scene.text.TextFlow;
//import javafx.scene.layout.HBox;
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
import com.jfoenix.controls.JFXHamburger;
import com.jfoenix.controls.JFXRippler;
import com.jfoenix.controls.JFXPopup;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXTreeTableView;
import com.jfoenix.controls.JFXTreeTableColumn;
import com.jfoenix.controls.RecursiveTreeItem;
import com.jfoenix.controls.cells.editors.TextFieldEditorBuilder;
import com.jfoenix.controls.cells.editors.base.GenericEditableTreeTableCell;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;

import one.id0.ppass.backend.PPassBackend;
import one.id0.ppass.utils.UserAccount;
import one.id0.ppass.utils.UserPassword;
import one.id0.ppass.server.PPassServer;
import one.id0.ppass.server.JSONServer;

public class MainPage extends Page {	
	// Class variables
	private PPassBackend backend;
	private TreeItem<UserAccount> accountRoot;
	private JFXTreeTableView<UserAccount> searchTreeTable;
	private UserAccount selectedAccount;
	private JFXHamburger accountHamburger;
	private JFXListView<Label> accountHamburgerActions;
	private PPassServer server;
	
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
		// Initialize page and logger
		super(stage, "MainPage.fxml", "ParaPass");
		backend.setLogger(logger);
		
		// Copy over backend
		this.backend = backend;
		// Current account selected is none
		selectedAccount = null;
		// Remove focus from inputs if background is clicked
		everythingPane.addEventHandler(MouseEvent.MOUSE_PRESSED, e->{
			everythingPane.requestFocus();
		});
		containerPane.addEventHandler(MouseEvent.MOUSE_PRESSED, e->{
			containerPane.requestFocus();
		});
		
		// Initialize accountHamburger and searchTreeTable
		initAccountHamburger();
		initSearchTreeTable();
		
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
		super.enterStage();
		
		// Create JSONServer and initialize. If you don't want to use the webextension, import and use NullServer instead.
		server = new JSONServer();
		server.init(backend, false, logger);
	}
	
	// Initializes accountHamburger
	public void initAccountHamburger() {
		// Create accountHamburger actions (new password, pin password, past passwords)
		accountHamburgerActions = new JFXListView<Label>();
		Label newPasswordButton = new Label("New password");
		Label pinPasswordButton = new Label("Pin password"); // Note: This can also become the "Unpin password" button
		Label pastPasswordsButton = new Label("Past passwords");
		accountHamburgerActions.getItems().setAll(newPasswordButton, pinPasswordButton, pastPasswordsButton);
		// Create wrappers for JFXListView
		accountHamburger = new JFXHamburger();
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
	}
	
	// Initializes searchTreeTable (the JFXTreeTable on the left)
	private void initSearchTreeTable() {
		// Get pinPasswordButton
		Label pinPasswordButton = accountHamburgerActions.getItems().get(1);
		
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
	//		searchTreeTable.getStyleClass().add("dark");
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
		AccountAddDialog accountAddDialog = new AccountAddDialog(backend, everythingPane, addingAccount, accountToAdd, new Runnable() {
			@Override
			public void run() {
				// Set backend logger back
				backend.setLogger(logger);
				// Update search tree
				if (addingAccount) {
					try {
						updateSearchTreeWithAccountAsync(accountToAdd);
					} catch (Exception e) {
						logger.log("Couldn't update search tree: " + e.getMessage());
					}
				}
			}
		});
		accountAddDialog.show();
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
