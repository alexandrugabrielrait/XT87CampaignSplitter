package net.xetanth87.campaignsplitter.ui;

import net.xetanth87.campaignsplitter.util.XT87Utils;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class AppThemeHandler {
	public static final String PREF_PREFIX = "DM";
	public static final String PREF_THEME = PREF_PREFIX + "Theme";

	public static AppTheme selectedTheme = AppTheme.OS;

	private static final Map<String, Object> originalValues = new HashMap<>();
	private static final String[] THEME_PROPERTIES = {
		"control", "info", "nimbusBase", "nimbusAlertYellow", "nimbusDisabledText",
		"nimbusFocus", "nimbusGreen", "nimbusInfoBlue", "nimbusLightBackground",
		"nimbusOrange", "nimbusRed", "nimbusSelectedText", "nimbusSelectionBackground", "text"
	};

	public static void saveStyle(AppTheme appStyle) {
		XT87Utils.savePreference(PREF_THEME, String.valueOf(appStyle));
	}

	public static void loadTheme() {
		String pref = XT87Utils.loadPreference(PREF_THEME);
		selectedTheme = pref != null ? AppTheme.valueOf(pref) : AppTheme.OS;
	}

	private static void saveOriginalValues() {
		if (originalValues.isEmpty()) {
			for (String property : THEME_PROPERTIES) {
				originalValues.put(property, UIManager.get(property));
			}
		}
	}

	public static void applyNimbusLookAndFeel()
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

	public static void applyDarkMode()
	{
		applyNimbusLookAndFeel();
		saveOriginalValues();
		UIManager.put("control", new Color( 0, 0, 0) );
		UIManager.put("info", new Color(128,128,128) );
		UIManager.put("nimbusBase", new Color( 18, 30, 49) );
		UIManager.put("nimbusAlertYellow", new Color( 248, 187, 0) );
		UIManager.put("nimbusDisabledText", new Color( 128, 128, 128) );
		UIManager.put("nimbusFocus", new Color(115,164,209) );
		UIManager.put("nimbusGreen", new Color(176,179,50) );
		UIManager.put("nimbusInfoBlue", new Color( 66, 139, 221) );
		UIManager.put("nimbusLightBackground", new Color( 18, 30, 49) );
		UIManager.put("nimbusOrange", new Color(191,98,4) );
		UIManager.put("nimbusRed", new Color(169,46,34) );
		UIManager.put("nimbusSelectedText", new Color( 255, 255, 255) );
		UIManager.put("nimbusSelectionBackground", new Color( 104, 93, 156) );
		UIManager.put("text", new Color( 255, 255, 255) );
	}

	private static void restoreNimbusValues() {
		for (String property : THEME_PROPERTIES) {
			Object value = originalValues.get(property);
			if (value != null) {
				UIManager.put(property, value);
			}
		}
	}

	public static void applyNimbusMode()
	{
		restoreNimbusValues();
		applyNimbusLookAndFeel();
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
