package net.xetanth87.campaignsplitter.ui;

import net.xetanth87.campaignsplitter.util.XT87Utils;

import javax.swing.*;
import java.awt.*;

public class AppThemeHandler {
	public static final String PREF_PREFIX = "DM";
	public static final String PREF_THEME = PREF_PREFIX + "Theme";

	public static AppTheme selectedTheme = AppTheme.OS;

	public static void saveStyle(AppTheme appStyle) {
		XT87Utils.savePreference(PREF_THEME, String.valueOf(appStyle));
	}

	public static void loadTheme() {
		String pref = XT87Utils.loadPreference(PREF_THEME);
		selectedTheme = pref != null ? AppTheme.valueOf(pref) : AppTheme.OS;
	}

	public static void applyDarkMode()
	{
		UIManager.put( "control", new Color( 0, 0, 0) );
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
		applyNimbusMode();
	}

	public static void applyNimbusMode()
	{
		try {
			for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void applySystemMode()
	{
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void applyPreference() {
		switch (selectedTheme) {
			case NIMBUS -> applyNimbusMode();
			case DARK -> applyDarkMode();
			default -> applySystemMode();
		}
	}
}
