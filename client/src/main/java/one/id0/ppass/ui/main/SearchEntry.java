package one.id0.ppass.ui.main;

import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import one.id0.ppass.utils.UserAccount;

public class SearchEntry extends VBox {
	// Directory where icons are
	private final String ICONDIR = "/icons/";
	// Icon sizes
	private static final int SMALL = 16;
	private static final int MEDIUM = 20;
	
	// Pin icon
	private SearchEntryIcon pinIcon;
	
	// Account label and UserAccount
	private UserAccount account;
	private Label accountLabel;
	
	// Constructor
	public SearchEntry(UserAccount account, EventHandler<MouseEvent> handler) {
		// Create wrappers
		super();
		BorderPane wrapper = new BorderPane();
		HBox leftWrapper = new HBox(8);
		HBox rightWrapper = new HBox(4);
		leftWrapper.setAlignment(Pos.CENTER_LEFT);
		rightWrapper.setAlignment(Pos.CENTER_RIGHT);
		wrapper.setCenter(leftWrapper);
		wrapper.setRight(rightWrapper);
		this.getChildren().add(wrapper);
		
		// Create everything in the left wrapper
		// Pin Icon
		pinIcon = new SearchEntryIcon(ICONDIR + "/pin.png",
				"Pin Password", SearchEntryIcon.TYPES.PIN, MEDIUM, "medium-icon-light", handler);
		// Account name
		accountLabel = new Label(account.accountName.getValue());
		// Add elements to the left wrapper
		leftWrapper.getChildren().addAll(pinIcon, accountLabel);
		
		// Create everything in the right wrapper
		// Description icon
		SearchEntryIcon descIcon = new SearchEntryIcon(ICONDIR + "/info.png",
				"Description", SearchEntryIcon.TYPES.DESCRIPTION, SMALL, "small-icon", handler);
		// History icon
		SearchEntryIcon histIcon = new SearchEntryIcon(ICONDIR + "/time.png",
				"History", SearchEntryIcon.TYPES.HISTORY, SMALL, "small-icon", handler);
		// Generate new password icon
		SearchEntryIcon newIcon = new SearchEntryIcon(ICONDIR + "/redo.png",
				"Generate New Password", SearchEntryIcon.TYPES.NEW, SMALL, "small-icon", handler);
		// Copy password icon
		SearchEntryIcon copyIcon = new SearchEntryIcon(ICONDIR + "/next.png",
				"Copy Password", SearchEntryIcon.TYPES.COPY, MEDIUM, "medium-icon", handler);
		// Add elements to the right wrapper
		rightWrapper.getChildren().addAll(descIcon, histIcon, newIcon, copyIcon);
		
		// Copy over UserAccount
		updateAccount(account);
	}
	
	// Update account name
	public void updateAccount(UserAccount account) {
		this.account = account;
		accountLabel.setText(account.accountName.getValue());
		setPinnedStatus(account.pinned.getValue());
	}
	
	// Get account name
	public String getAccountName() {
		return account.accountName.getValue();
	}
	
	// Set pinned status
	public void setPinnedStatus(boolean status) {
		if (status) {
			pinIcon.updateTooltip("Unpin Password");
			pinIcon.setStyleClass("medium-icon-reverse");
		} else {
			pinIcon.updateTooltip("Pin Password");
			pinIcon.setStyleClass("medium-icon-light");
		}
	}
}
