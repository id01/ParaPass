package one.id0.ppass.ui.main;

import javafx.event.EventHandler;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import one.id0.ppass.ui.HoverTooltip;

public class SearchEntryIcon extends ImageView {
	enum TYPES {
		PIN, DESCRIPTION, HISTORY, NEW, COPY
	}
	private TYPES type;
	private HoverTooltip tooltip;
	
	public SearchEntryIcon(String imageURI, String tooltipText, TYPES type, int size, String style, EventHandler<MouseEvent> handler) {
		super(new Image(imageURI, size, size, false, false));
		tooltip = new HoverTooltip(tooltipText, this);
		super.setOnMouseClicked(handler);
		super.setPickOnBounds(true);
		this.type = type;
		getStyleClass().add(style);
	}
	
	public TYPES getType() {
		return type;
	}
	
	public void updateTooltip(String tooltipText) {
		tooltip.setText(tooltipText);
	}
	
	public void setStyleClass(String style) {
		getStyleClass().clear();
		getStyleClass().add(style);
	}
}
