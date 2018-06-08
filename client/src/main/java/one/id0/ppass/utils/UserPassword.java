package one.id0.ppass.utils;

import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;

// Class for storing a timestamp (as a string), account name, and password
public class UserPassword extends RecursiveTreeObject<UserPassword> {
	// Variables to store
	private StringProperty timestamp;
	public StringProperty accountName;
	public String pass;
	
	// Constructor. Stores an account name, password, and converts a timestamp to a string and sets it
	public UserPassword(long timestamp, String accountName, String pass) {
		this.accountName = new SimpleStringProperty(accountName);
		this.pass = pass;
		setTimestamp(timestamp);
	}
	
	// Sets the timestamp
	public void setTimestamp(long timestamp) {
		ZonedDateTime instant = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault());
		this.timestamp = new SimpleStringProperty(DateTimeFormatter.ofPattern("yyyy-MM-dd").format(instant));
	}
	
	// Gets the timestamp
	public StringProperty getTimestamp() {
		return timestamp;
	}
}
