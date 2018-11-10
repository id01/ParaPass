package one.id0.ppass.ui;

import javafx.scene.Node;
import javafx.scene.control.Tooltip;

public class HoverTooltip extends Tooltip {
	boolean mouseOverTooltip;
	
	public HoverTooltip(String text, Node parentNode) {
		super(text);
		
		parentNode.setOnMouseEntered(e->{
			show(parentNode, e.getScreenX()-getWidth()/2, e.getScreenY()-2-getHeight());
		});
		parentNode.setOnMouseExited(e->{
			hide();
		});
	}
}
