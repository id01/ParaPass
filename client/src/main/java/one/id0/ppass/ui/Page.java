package one.id0.ppass.ui;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Scene;

import one.id0.ppass.backend.Logger;

public abstract class Page {
	// Class variables
	public final String fxmlFile;
	public final String title;
	protected Logger logger;
	protected Scene scene;
	protected Stage stage;
	
	// Constructor.
	public Page(Stage stage, String fxmlFile, String title) throws IOException {
		// Copy over fxml file name and title
		this.fxmlFile = fxmlFile;
		this.title = title;
		// Create default logger
		logger = new Logger();
		// Initialize FXML
		FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
		loader.setController(this);
		scene = new Scene(loader.load());
		// Copy over stage
		this.stage = stage;
	}
	
	// Function to set stage to this scene
	protected void enterStage() {
		stage.setScene(scene);
	}
}
