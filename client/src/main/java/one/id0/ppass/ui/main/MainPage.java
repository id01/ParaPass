package one.id0.ppass.ui.main;

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
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.SplitPane;
import javafx.scene.Node;
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
import one.id0.ppass.ui.Page;
import one.id0.ppass.ui.popup.DescriptionPage;
import one.id0.ppass.ui.popup.PastPasswordPage;
import one.id0.ppass.server.JSONServer;

public class MainPage extends Page {	
	// Class variables
	private PPassBackend backend;
	private ObservableList<UserAccount> userAccounts;
	private TreeItem<UserAccount> accountRoot;
	private PPassServer server;
	
	// FXML elements that we need to interact with
	@FXML private StackPane everythingPane;
	@FXML private AnchorPane backgroundPane;
	@FXML private JFXTextField searchInput;
	@FXML private JFXTreeTableView<UserAccount> searchTreeTable;

	// Constructor
	public MainPage(Stage stage, PPassBackend backend) throws IOException {
		// Initialize page and logger
		super(stage, "MainPage.fxml", "ParaPass");
		backend.setLogger(logger);
		
		// Copy over backend
		this.backend = backend;
		// Remove focus from inputs if background is clicked
		everythingPane.addEventHandler(MouseEvent.MOUSE_PRESSED, e->{
			everythingPane.requestFocus();
		});
		backgroundPane.addEventHandler(MouseEvent.MOUSE_PRESSED, e->{
			backgroundPane.requestFocus();
		});
		
		// Initialize searchTreeTable
		initSearchTreeTable();
		
		// Show our new scene
		super.enterStage();
		
		// Create JSONServer and initialize. If you don't want to use the webextension, import and use NullServer instead.
		server = new JSONServer();
		server.init(backend, false, logger);
	}
	
	/* ACCOUNT OPTIONS POPUP CREATOR AND ACTIONS */
	// Shows the account options popup for an account at a controller node
	public void showAccountOptionsPopup(Node control, UserAccount account) {
		// Create account options popup actions (copy password, pin password, new password, past passwords)
		JFXListView<Label> popupActions = new JFXListView<Label>();
		Label copyPasswordButton = new Label("Copy password");
		Label pinPasswordButton = new Label("Pin password");
		Label newPasswordButton = new Label("New password");
		if (account.pinned.getValue().equals("pinned")) {
			pinPasswordButton.setText("Unpin password");
		}
		Label pastPasswordsButton = new Label("Past passwords");
		Label accountPropertiesButton = new Label("Properties");
		popupActions.getItems().setAll(copyPasswordButton, pinPasswordButton,
				newPasswordButton, pastPasswordsButton, accountPropertiesButton);
		
		// Create wrappers for JFXListView
		JFXPopup popup = new JFXPopup(popupActions);
		// Set event handler for popup actions to run the selected function and close the popup
		popupActions.setOnMouseClicked(e->{
			Label selected = popupActions.getSelectionModel().getSelectedItem();
			if (selected == copyPasswordButton) {
				copyPassword(account);
			} else if (selected == pinPasswordButton) {
				togglePasswordPin(account);
			} else if (selected == newPasswordButton) {
				createNewPassword(account.accountName.getValue(), false);
			} else if (selected == pastPasswordsButton) {
				showPastPasswords(account);
			} else if (selected == accountPropertiesButton) {
				showAccountProperties(account);
			} else {
				logger.logErr("How is this even possible? You clicked a nonexistent button.");
			}
			popup.hide();
		});
		
		// Show popup
		popup.show(control);
	}
	
	// Copies a specified account's password
	private void copyPassword(UserAccount account) {
		try {
			if (account != null) {
				// Get password and put into clipboard
				String[] accnpass = backend.getPassword(account.accountID);
				ClipboardContent copiedPassword = new ClipboardContent();
				copiedPassword.putString(accnpass[1]);
				Clipboard.getSystemClipboard().setContent(copiedPassword);
			} else {
				logger.logErr("Account specified is null!");
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
	
	// Shows past passwords using a PastPasswordPage on a popup stage
	private void showPastPasswords(UserAccount account) {
		try {
			ArrayList<UserPassword> pastPasswords = backend.getPastPasswords(account.accountID);
			Stage popupStage = new Stage();
			new PastPasswordPage(popupStage, pastPasswords);
			popupStage.show();
		} catch (Exception e) {
			logger.log("Failed to show past passwords: " + e.getMessage());
		}
	}
	
	// Shows properties of an account using a DescriptionPage on a popup stage
	private void showAccountProperties(UserAccount account) {
		try {
			Stage popupStage = new Stage();
			new DescriptionPage(popupStage, backend, account);
			popupStage.show();
		} catch (Exception e) {
			logger.logErr("Failed to show account properties: " + e.getMessage());
		}
	}
	
	/* TREETABLEVIEW FOR SEARCHING PASSWORDS INITIALIZATION UPDATING AND ACTIONS */
	// Initializes searchTreeTable (the JFXTreeTable on the left)
	private void initSearchTreeTable() {
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
			ArrayList<UserAccount> userAccountsArrayList = backend.getAllAccounts();
			userAccounts = FXCollections.observableArrayList(userAccountsArrayList);
			accountRoot = new RecursiveTreeItem<UserAccount>(userAccounts, RecursiveTreeObject::getChildren);
			searchTreeTable.getColumns().setAll(accountNameColumn, timestampColumn, pinnedColumn);
			searchInput.textProperty().addListener((o, oldVal, newVal) -> {
				searchTreeTable.setPredicate(accProp -> {
					final UserAccount account = accProp.getValue();
					return account.accountName.toString().contains(newVal);
				});
			});
			searchTreeTable.setRoot(accountRoot);
			// Style searchTreeTable and add click event that changes value of currently
			// selected account as well as UI elements about it
	//		searchTreeTable.getStyleClass().add("dark");
			searchTreeTable.getSelectionModel().selectedItemProperty().addListener((o, oldVal, newVal) -> {
				if (newVal != null) {
					// Get selected account
					UserAccount account = newVal.getValue();
					// Show account options popup
					showAccountOptionsPopup(searchTreeTable, account);
					// Update last accessed timestamp for this account
					try {
						backend.updateAccountCache(account.accountID, true, null, -1);
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
				userAccounts.add(userAccountToAdd);
				// accountRoot.getChildren().add(new RecursiveTreeItem<UserAccount>(userAccountToAdd, RecursiveTreeObject::getChildren));
			}
		});
	}
	
	// Creates a new password for the account in Input
	@FXML
	protected void createNewAccount() {
		if (!searchInput.getText().equals("")) { // We use toLowerCase() here to remove visual gitches.
			createNewPassword(searchInput.getText().toLowerCase(), true); // Our backend is already not case-sensitive.
		}
	}
}
