package net.xetanth87.campaignsplitter;

import java.util.Locale;

public enum SplitterLanguage {
	DEFAULT(null, Locale.getDefault()),
	ENGLISH("English", Locale.ENGLISH),
	SPANISH("Español", new Locale("es")),
	GERMAN("Deutsch", Locale.GERMAN),
	FRENCH("Français", Locale.FRENCH),
	ITALIAN("Italiano", Locale.ITALIAN),
	PORTUGUESE("Português", new Locale("pt")),
	POLISH("Polski", new Locale("pl")),
	RUSSIAN("Русский", new Locale("ru")),
	ROMANIAN("Română", new Locale("ro")),
	UKRAINIAN("Українська", new Locale("ua")),
	CHINESE("中文", Locale.CHINESE),
	KOREAN("한국어", Locale.KOREAN);

	public static SplitterLanguage selectedLanguage = SplitterLanguage.DEFAULT;
	private final String displayName;
	private final Locale locale;

	SplitterLanguage(String displayName, Locale locale) {
		this.displayName = displayName;
		this.locale = locale;
	}

	public String getDisplayName() {
		if (this == DEFAULT)
			return XT87Utils.getLocalizedString("language_default");
		return XT87Utils.getUnicodeString(displayName);
	}

	public Locale getLocale() {
		return locale;
	}
}
