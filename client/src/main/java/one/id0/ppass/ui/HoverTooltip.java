package one.id0.ppass.ui;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;

public class HoverTooltip extends Tooltip {
	public HoverTooltip(String text, Node parentNode, Stage stage) {
		super(text);
		
		parentNode.setOnMouseEntered(e->{
			Bounds parentBounds = parentNode.localToScene(parentNode.getBoundsInLocal());
			show(parentNode,
					stage.getX()+parentBounds.getMinX()+parentBounds.getWidth()/2-getWidth()/2,
					stage.getY()+parentBounds.getMinY());
		});
		parentNode.setOnMouseExited(e->{
			hide();
		});
	}
}
