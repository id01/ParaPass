package one.id0.ppass.utils;

import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;

import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;

public class UserAccount extends RecursiveTreeObject<UserAccount> {
	public byte[] accountID;
	public StringProperty accountName;
	
	public UserAccount(byte[] accountID, String accountName) {
		this.accountID = accountID;
		this.accountName = new SimpleStringProperty(accountName);
	}
}
