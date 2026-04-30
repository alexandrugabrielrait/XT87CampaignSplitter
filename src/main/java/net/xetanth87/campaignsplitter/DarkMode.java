package net.xetanth87.campaignsplitter;

import net.xetanth87.campaignsplitter.util.XT87Utils;

import javax.swing.*;
import java.awt.*;

public class DarkMode {
	public static final String PREF_PREFIX = "DM";
	public static final String PREF_DARK_MODE = PREF_PREFIX + "Enabled";

	public static boolean darkModeEnabled = false;

	public static void saveDarkMode(boolean enabled) {
		XT87Utils.savePreference(PREF_DARK_MODE, String.valueOf(enabled));
	}

	public static void loadDarkMode() {
		String pref = XT87Utils.loadPreference(PREF_DARK_MODE);
		darkModeEnabled = pref != null && Boolean.parseBoolean(pref);
	}

	public static void applyDarkMode() {
		if (darkModeEnabled) {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception ignored) {
			}
			
			UIManager.put("control", new Color(60, 60, 60));
			UIManager.put("info", new Color(60, 60, 60));
			UIManager.put("nimbusBase", new Color(18, 30, 36));
			UIManager.put("nimbusAlertYellow", new Color(248, 187, 0));
			UIManager.put("nimbusDisabledText", new Color(178, 178, 178));
			UIManager.put("nimbusFocus", new Color(115, 164, 209));
			UIManager.put("nimbusGreen", new Color(176, 179, 50));
			UIManager.put("nimbusInfoBlue", new Color(66, 139, 221));
			UIManager.put("nimbusLightBackground", new Color(50, 50, 50));
			UIManager.put("nimbusOrange", new Color(191, 98, 4));
			UIManager.put("nimbusRed", new Color(169, 0, 0));
			UIManager.put("nimbusSelectedText", new Color(255, 255, 255));
			UIManager.put("nimbusSelectionBackground", new Color(104, 93, 156));
			UIManager.put("text", new Color(230, 230, 230));

			UIManager.put("Panel.background", new Color(50, 50, 50));
			UIManager.put("Panel.foreground", new Color(230, 230, 230));
			UIManager.put("Label.foreground", new Color(230, 230, 230));
			UIManager.put("Button.background", new Color(60, 60, 60));
			UIManager.put("Button.foreground", new Color(230, 230, 230));
			UIManager.put("TextArea.background", new Color(40, 40, 40));
			UIManager.put("TextArea.foreground", new Color(230, 230, 230));
			UIManager.put("TextArea.caretForeground", new Color(230, 230, 230));
			UIManager.put("TextField.background", new Color(40, 40, 40));
			UIManager.put("TextField.foreground", new Color(230, 230, 230));
			UIManager.put("TextField.caretForeground", new Color(230, 230, 230));
			UIManager.put("ProgressBar.background", new Color(60, 60, 60));
			UIManager.put("ProgressBar.foreground", new Color(80, 120, 180));
			UIManager.put("ScrollPane.background", new Color(50, 50, 50));
			UIManager.put("JScrollBar.background", new Color(60, 60, 60));
			UIManager.put("JScrollBar.thumbBackground", new Color(100, 100, 100));
			UIManager.put("JScrollBar.thumbDarkShadow", new Color(80, 80, 80));

			UIManager.put("Menu.background", new Color(50, 50, 50));
			UIManager.put("Menu.foreground", new Color(230, 230, 230));
			UIManager.put("MenuBar.background", new Color(40, 40, 40));
			UIManager.put("MenuBar.foreground", new Color(230, 230, 230));
			UIManager.put("MenuItem.background", new Color(50, 50, 50));
			UIManager.put("MenuItem.foreground", new Color(230, 230, 230));
			UIManager.put("MenuItem.selectionBackground", new Color(100, 100, 100));
			UIManager.put("MenuItem.selectionForeground", new Color(255, 255, 255));
			UIManager.put("PopupMenu.background", new Color(50, 50, 50));
			UIManager.put("PopupMenu.foreground", new Color(230, 230, 230));

			UIManager.put("CheckBox.background", new Color(50, 50, 50));
			UIManager.put("CheckBox.foreground", new Color(230, 230, 230));
			UIManager.put("RadioButton.background", new Color(50, 50, 50));
			UIManager.put("RadioButton.foreground", new Color(230, 230, 230));

			UIManager.put("ComboBox.background", new Color(60, 60, 60));
			UIManager.put("ComboBox.foreground", new Color(230, 230, 230));
			UIManager.put("ComboBox.buttonBackground", new Color(60, 60, 60));

			UIManager.put("OptionPane.background", new Color(50, 50, 50));
			UIManager.put("OptionPane.messageForeground", new Color(230, 230, 230));
			UIManager.put("OptionPane.foreground", new Color(230, 230, 230));
		} else {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
