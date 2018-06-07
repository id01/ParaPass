package one.id0.ppass.utils;

import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;

public class UserAccount extends RecursiveTreeObject<UserAccount> {
	// Final objects
	StringProperty neverstr = new SimpleStringProperty("Never");
	StringProperty emptystr = new SimpleStringProperty("");
	StringProperty pinnedstr = new SimpleStringProperty("pinned");
	
	// Properties
	public byte[] accountID;
	public StringProperty accountName;
	public StringProperty description;
	public StringProperty timestamp;
	public StringProperty pinned;
	
	// Constructor that takes less arguments
	public UserAccount(byte[] accountID, String accountName) {
		this.accountID = accountID;
		this.accountName = new SimpleStringProperty(accountName);
		this.description = emptystr;
		this.timestamp = neverstr;
		this.pinned = emptystr;
	}
	
	// Constructor that initializes everything
	public UserAccount(byte[] accountID, String accountName, String description, long timestamp, boolean pinned) {
		this.accountID = accountID;
		this.accountName = new SimpleStringProperty(accountName);
		// Initialize description to a SimpleStringProperty with our specified String, or to an empty one if it's null
		if (description != null) {
			this.description = new SimpleStringProperty(description);
		} else {
			this.description = new SimpleStringProperty("");
		}
		// Initialize timestamp string and timestamp.
		if (timestamp != 0) {
			ZonedDateTime instant = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault());
			this.timestamp = new SimpleStringProperty(DateTimeFormatter.ofPattern("yyyy-MM-dd").format(instant));
		} else {
			this.timestamp = neverstr;
		}
		// Initialize pinned state.
		if (pinned) {
			this.pinned = pinnedstr;
		} else {
			this.pinned = emptystr;
		}
	}
}
