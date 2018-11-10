package one.id0.ppass.ui;

import java.applet.Applet;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.PopupMenu;
import java.awt.MenuItem;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.event.ActionListener;

import javafx.application.Platform;
import javafx.stage.Stage;

public class WindowMinimizer extends Applet {
	// Global vars
	private static final long serialVersionUID = 0;
	private boolean firstClose;
	private TrayIcon trayIcon;
	
	// Constructor. NOTE THAT THIS ASSUMES IMPLICIT EXIT IS ALREADY OFF.
	public WindowMinimizer(Stage stage) {
		// Initialize first close
		firstClose = true;
		// Init trayIcon if system tray is supported, otherwise fall back to default
		if (SystemTray.isSupported()) {
			try {
				// Set stage close request action
				stage.setOnCloseRequest(e->{
					if (firstClose) {
						trayIcon.displayMessage("Minimized to System Tray",
								"ParaPass is still running in the background. Click here to open it again",
								TrayIcon.MessageType.INFO);
						firstClose = false;
					}
					stage.hide();
				});
				// Get system tray and image
				SystemTray tray = SystemTray.getSystemTray();
				Toolkit toolkit = Toolkit.getDefaultToolkit();
				MediaTracker tracker = new MediaTracker(this);
				Image image = toolkit.getImage(getClass().getResource("/icons/icon.png"));
				tracker.addImage(image, 0);
				tracker.waitForAll();
				// Create popup menu and menu items
				ActionListener showListener = e->{
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							stage.show();
						}
					});
				};
				PopupMenu popup = new PopupMenu();
				MenuItem showItem = new MenuItem("Show");
				showItem.addActionListener(showListener);
				MenuItem closeItem = new MenuItem("Close");
				closeItem.addActionListener(e->exit());
				popup.add(showItem);
				popup.add(closeItem);
				// Create tray icon and add to tray
				trayIcon = new TrayIcon(image, "ParaPass", popup);
				trayIcon.addActionListener(showListener);
				tray.add(trayIcon);
			} catch (Exception ex) {
				System.out.println("Couldn't init tray icon; falling back to none: " + ex.getMessage());
				stage.setOnCloseRequest(e->exit());
			}
		} else {
			stage.setOnCloseRequest(e->exit());
		}
	}
	
	// Exits the program
	private void exit() {
		Platform.exit();
		System.exit(0);
	}
}
