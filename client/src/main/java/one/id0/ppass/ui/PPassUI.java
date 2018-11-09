package one.id0.ppass.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import one.id0.ppass.ui.login.LoginPage;

public class PPassUI extends Application {
	// Initializer
	@Override
	public void start(Stage primaryStage) throws Exception {
		// Start a LoginForm and show the stage
		new LoginPage(primaryStage);
		primaryStage.show();
		// Set implicit exit to false and create WindowMinimizer
		Platform.setImplicitExit(false);
		new WindowMinimizer(primaryStage);
	}

	public static void main(String[] args) {
		launch(args);
	}
}
