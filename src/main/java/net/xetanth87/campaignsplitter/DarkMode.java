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
				UIManager.put( "control", new Color( 0, 0, 0) );
				UIManager.put( "Button.background", new Color(18, 30, 49) );
				UIManager.put( "Button.foreground", new Color( 59, 68, 75) );
				UIManager.put( "info", new Color(128,128,128) );
				UIManager.put( "nimbusBase", new Color( 18, 30, 49) );
				UIManager.put( "nimbusAlertYellow", new Color( 248, 187, 0) );
				UIManager.put( "nimbusDisabledText", new Color( 128, 128, 128) );
				UIManager.put( "nimbusFocus", new Color(115,164,209) );
				UIManager.put( "nimbusGreen", new Color(176,179,50) );
				UIManager.put( "nimbusInfoBlue", new Color( 66, 139, 221) );
				UIManager.put( "nimbusLightBackground", new Color( 18, 30, 49) );
				UIManager.put( "nimbusOrange", new Color(191,98,4) );
				UIManager.put( "nimbusRed", new Color(169,46,34) );
				UIManager.put( "nimbusSelectedText", new Color( 255, 255, 255) );
				UIManager.put( "nimbusSelectionBackground", new Color( 104, 93, 156) );
				UIManager.put( "text", new Color( 255, 255, 255) );
				for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
					if ("Nimbus".equals(info.getName())) {
						UIManager.setLookAndFeel(info.getClassName());
						break;
					}
				}
			} catch (Exception ignored) {
			}
		} else {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
