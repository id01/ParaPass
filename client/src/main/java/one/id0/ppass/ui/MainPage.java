package one.id0.ppass.ui;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainPage {
	// Class variables
	private Stage stage;
	
	// Constructor
	public MainPage(Stage stage) throws IOException {
		// Copy over stage
		this.stage = stage;
		// Initialize main page
		FXMLLoader loader = new FXMLLoader(getClass().getResource("MainPage.fxml"));
		loader.setController(this);
		Scene scene = new Scene(loader.load());
		stage.setTitle("ParaPass");
		stage.setScene(scene);
	}
}
