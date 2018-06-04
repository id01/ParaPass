package one.id0.ppass.ui;

import javafx.application.Application;
import javafx.stage.Stage;

public class PPassUI extends Application {
	// Initializer
	@Override
	public void start(Stage primaryStage) throws Exception {
		// Start a LoginForm and show the stage
		new LoginForm(primaryStage);
		primaryStage.show();
	}

	public static void main(String[] args) {
		launch(args);
	}
}
