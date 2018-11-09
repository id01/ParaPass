package one.id0.ppass.ui.main;

import javafx.event.EventHandler;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import one.id0.ppass.ui.HoverTooltip;

public class SearchEntryIcon extends ImageView {
	enum TYPES {
		PIN, DESCRIPTION, HISTORY, NEW, COPY
	}
	private TYPES type;
	
	public SearchEntryIcon(Image image, String tooltipText, TYPES type, Stage stage, EventHandler<MouseEvent> handler) {
		super(image);
		new HoverTooltip(tooltipText, this, stage);
		super.setOnMouseClicked(handler);
		this.type = type;
	}
	
	public TYPES getType() {
		return type;
	}
}
