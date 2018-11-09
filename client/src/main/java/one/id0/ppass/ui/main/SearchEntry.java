package one.id0.ppass.ui.main;

import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SearchEntry extends VBox {
	// Directory where icons are
	private final String ICONDIR = "../../../../../../../icons";
	
	// Constructor
	public SearchEntry(String accountName, Stage stage, EventHandler<MouseEvent> handler) {
		// Create wrappers
		super();
		BorderPane wrapper = new BorderPane();
		HBox leftWrapper = new HBox();
		HBox rightWrapper = new HBox();
		wrapper.setCenter(leftWrapper);
		wrapper.setRight(rightWrapper);
		this.getChildren().add(wrapper);
		
		// Create everything in the left wrapper
		// Pin Icon
		SearchEntryIcon pinIcon = new SearchEntryIcon(new Image(ICONDIR + "/pin.png"),
				"Pin Password", SearchEntryIcon.TYPES.PIN, stage, handler);
		// Account name
		Label accountLabel = new Label(accountName);
		// Add elements to the left wrapper
		leftWrapper.getChildren().addAll(pinIcon, accountLabel);
		
		// Create everything in the right wrapper
		// Description icon
		SearchEntryIcon descIcon = new SearchEntryIcon(new Image(ICONDIR + "/info.png"),
				"Description", SearchEntryIcon.TYPES.DESCRIPTION, stage, handler);
		// History icon
		SearchEntryIcon histIcon = new SearchEntryIcon(new Image(ICONDIR + "/time.png"),
				"History", SearchEntryIcon.TYPES.HISTORY, stage, handler);
		// Generate new password icon
		SearchEntryIcon newIcon = new SearchEntryIcon(new Image(ICONDIR + "/redo.png"),
				"Generate New Password", SearchEntryIcon.TYPES.NEW, stage, handler);
		// Copy password icon
		SearchEntryIcon copyIcon = new SearchEntryIcon(new Image(ICONDIR + "/next.png"),
				"Copy Password", SearchEntryIcon.TYPES.COPY, stage, handler);
		// Add elements to the right wrapper
		rightWrapper.getChildren().addAll(descIcon, histIcon, newIcon, copyIcon);
	}
}
