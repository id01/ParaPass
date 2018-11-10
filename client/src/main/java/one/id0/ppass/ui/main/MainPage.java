package one.id0.ppass.ui.main;

import java.io.IOException;
import java.util.ArrayList;

import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;

import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListCell;
import com.jfoenix.controls.JFXListView;

import one.id0.ppass.backend.PPassBackend;
import one.id0.ppass.utils.UserAccount;
import one.id0.ppass.utils.UserPassword;
import one.id0.ppass.server.PPassServer;
import one.id0.ppass.ui.HoverTooltip;
import one.id0.ppass.ui.Page;
import one.id0.ppass.ui.popup.DescriptionPage;
import one.id0.ppass.ui.popup.PastPasswordPage;
import one.id0.ppass.server.JSONServer;

public class MainPage extends Page {
	// Class variables
	private PPassBackend backend;
	private ObservableList<UserAccount> userAccounts;
	private FilteredList<UserAccount> filteredAccounts;
	private SortedList<UserAccount> sortedAccounts;
	private PPassServer server;
	
	// FXML elements that we need to interact with
	@FXML private StackPane everythingPane;
	@FXML private VBox backgroundPane;
	@FXML private JFXTextField searchInput;
	@FXML private JFXButton createNewAccountButton;
	@FXML private JFXListView<UserAccount> resultsListView;

	// Constructor
	public MainPage(Stage stage, PPassBackend backend) throws IOException {
		// Initialize page and logger
		super(stage, "MainPage.fxml", "ParaPass");
		backend.setLogger(logger);
		
		// Style icons in scene
		scene.getStylesheets().add(getClass().getResource("/css/icons.css").toExternalForm());
		
		// Copy over backend
		this.backend = backend;
		// Remove focus from inputs if background is clicked
		everythingPane.addEventHandler(MouseEvent.MOUSE_PRESSED, e->{
			everythingPane.requestFocus();
		});
		backgroundPane.addEventHandler(MouseEvent.MOUSE_PRESSED, e->{
			backgroundPane.requestFocus();
		});
		
		// Initialize resultsListView
		initResultsListView();
		
		// Add tooltip for createNewAccountButton
		new HoverTooltip("Create This Password", createNewAccountButton);
		
		// Show our new scene
		super.enterStage();
		
		// Create JSONServer and initialize. If you don't want to use the webextension, import and use NullServer instead.
		server = new JSONServer();
		server.init(backend, false, logger);
	}
	
	/* SEARCH RESULT ACTIONS */
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
						updateUserAccountsAsync(accountToAdd);
					} catch (Exception e) {
						logger.log("Exception when updating user accounts list: " + e.getMessage());
					}
				}
			}
		});
		accountAddDialog.show();
	}
	
	// Toggles a password's pin state
	protected void togglePasswordPin(UserAccount account) {
		try {
			boolean newPinned = !account.pinned.getValue();
			backend.updateAccountCache(account.accountID, true, null, newPinned ? 1:0);
			account.pinned.set(newPinned);
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
			new DescriptionPage(popupStage, backend, account, (o, oldVal, newVal)->{ // Listener for when the description changes
				// Create task to update the description to the new value (both in the backend and frontend) and run it
				Task<Void> updateTask = new Task<Void>() {
					public Void call() throws Exception {
						backend.updateAccountCache(account.accountID, true, newVal, -1);
						account.description.set(newVal);
						Platform.runLater(() -> {
							try {
								updateUserAccountsAsync(account);
							} catch (Exception e) {
								logger.logErr("Exception when updating user accounts list: " + e.getMessage());
							}
						});
						return null;
					}
				};
				new Thread(updateTask).start();
			});
			popupStage.show();
		} catch (Exception e) {
			logger.logErr("Failed to show account properties: " + e.getMessage());
		}
	}
	
	/* INITIALIZATION UPDATING AND ACTIONS FOR LISTVIEW FOR SEARCHING PASSWORDS */
	// Initializes resultsListView (the listView at the center of the page with all the accounts)
	private void initResultsListView() {
		// Initialize resultsListView content and listeners
		try {
			// Create observable list, filtered list, and sorted list for resultsListView to display
			ArrayList<UserAccount> userAccountsArrayList = backend.getAllAccounts();
			userAccounts = FXCollections.observableArrayList(userAccountsArrayList);
			filteredAccounts = new FilteredList<UserAccount>(userAccounts, acc -> true);
			sortedAccounts = new SortedList<UserAccount>(filteredAccounts, (acc1, acc2)->{
				// Value pins the most, timestamps the next, then names the least.
				// This will return any positive number if acc2 is first or any negative number if acc1 is first
				// Check pin difference first
				int pinDiff = 0;
				if (acc1.pinned.getValue()) {
					pinDiff -= 1;
				}
				if (acc2.pinned.getValue()) {
					pinDiff += 1;
				}
				if (pinDiff != 0) {
					return pinDiff;
				}
				// Check time difference next (note: we can compare these as strings because it's in the format YYYY-MM-DD)
				int timeDiff = acc1.timestamp.getValue().compareTo(acc2.timestamp.getValue());
				if (timeDiff != 0) {
					return timeDiff;
				}
				// Check name difference last
				int nameDiff = acc1.accountName.getValue().compareTo(acc2.accountName.getValue());
				return nameDiff;
			});
			// Set search listener to change filter predicate and resort
			searchInput.textProperty().addListener((o, oldVal, newVal) -> {
				String filter = searchInput.getText();
				if (filter == null || filter.length() == 0) {
					filteredAccounts.setPredicate(acc -> true);
				} else {
					filteredAccounts.setPredicate(acc -> acc.accountName.getValue().contains(filter));
				}
			});
			// Set resultsListView cell factory
			resultsListView.setCellFactory(param -> new JFXListCell<UserAccount>() {
				// Each cell stores the UserAccount it represents and a SearchEntry object to display it 
				private UserAccount account;
				private SearchEntry searchEntry;
				
				@Override
				protected void updateItem(UserAccount account, boolean empty) {
					super.updateItem(account, empty);
					super.setText("");
					// Create content
					if (!empty) {
						// If this cell is not empty, manipulate our new information and set our graphic to our searchEntry
						if (searchEntry == null) {
							// If a search entry hasn't been created yet, create it
							this.account = account;
							searchEntry = new SearchEntry(account, e->{
								// When we get an event, determine the source. If it's a SearchEntryIcon, get its type and act accordingly
								Object sourceObject = e.getSource();
								if (sourceObject instanceof SearchEntryIcon) {
									SearchEntryIcon source = (SearchEntryIcon)sourceObject;
									switch (source.getType()) {
										case PIN: togglePasswordPin(this.account); searchEntry.setPinnedStatus(this.account.pinned.getValue()); break;
										case DESCRIPTION: showAccountProperties(this.account); break;
										case HISTORY: showPastPasswords(this.account); break;
										case NEW: createNewPassword(this.account.accountName.getValue(), false); break;
										case COPY: copyPassword(this.account); break;
									}
								}
							});
						} else if (this.account != account) {
							// Otherwise, if the account changed, update the search entry and the cell's UserAccount object
							this.account = account;
							searchEntry.updateAccount(account);
						}
						// Draw our entry
						super.setGraphic(searchEntry);
					} else {
						// If this is now empty, set both account and searchEntry to null and make the ListCell blank if it isn't already
						this.account = null;
						this.searchEntry = null;
						super.setGraphic(new Label(""));
					}
				}
			});
			// Set resultsListView data
			resultsListView.setItems(sortedAccounts);
			resultsListView.setFocusTraversable(false);
		} catch (Exception e) {
			logger.log("Non-Fatal error on generating account name table: " + e.getMessage()
					+ ". No table will be added.");
			throw new RuntimeException(e);
			// Do nothing. Our accountNameColumn won't be populated or added.
			// The user will still have a semi-functional UI
		}
	}
	
	// Updates userAccounts for resultsList using an account name
	private void updateUserAccountsAsync(String accountName) throws Exception {
		updateUserAccountsAsync(backend.getUserAccountObject(accountName));
	}
	
	// Updates userAccounts for resultsList using a UserAccount
	private void updateUserAccountsAsync(UserAccount account) throws Exception {
		// Add this child in the UI thread
		Platform.runLater(new Runnable() {
			public void run() {
				// Loop through the items in userAccounts, removing any old versions of this UserAccount.
				for (int i=0; i<userAccounts.size(); i++) {
					if (userAccounts.get(i).accountName.getValue().equals(account.accountName.getValue())) {
						userAccounts.remove(i);
					}
				}
				// Add the new UserAccount
				userAccounts.add(account);
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
