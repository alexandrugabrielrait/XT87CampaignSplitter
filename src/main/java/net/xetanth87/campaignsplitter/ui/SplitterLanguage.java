package net.xetanth87.campaignsplitter.ui;

import net.xetanth87.campaignsplitter.util.XT87Utils;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

import static net.xetanth87.campaignsplitter.util.XT87Utils.getLocalizedString;

public enum SplitterLanguage {
	DEFAULT("default", null),
	ENGLISH("en", Locale.ENGLISH),
	SPANISH("es", new Locale("es")),
	GERMAN("de", Locale.GERMAN),
	//FRENCH("fr", Locale.FRENCH),
	//ITALIAN("it", Locale.ITALIAN),
	//PORTUGUESE("pt", new Locale("pt")),
	//POLISH("pl", new Locale("pl")),
	ROMANIAN("ro", new Locale("ro")),
	RUSSIAN("ru", new Locale("ru")),
	//UKRAINIAN("ua", new Locale("ua")),
	CHINESE("zh", Locale.CHINESE);
	//KOREAN("ko", Locale.KOREAN);

	public static final String PREF_PREFIX = "SL";
	public static final String PREF_LANGUAGE = PREF_PREFIX + "Language";

	public static SplitterLanguage selectedLanguage = SplitterLanguage.DEFAULT;
	private final String languageCode;
	private final Locale locale;

	SplitterLanguage(String languageCode, Locale locale) {
		this.languageCode = languageCode;
		this.locale = locale;
	}

	public String getLanguageCode() {
		return languageCode;
	}

	public Locale getLocale() {
		if (locale == null) {
			Locale locale = Locale.getDefault();
			String lang = locale.getLanguage();
			if (lang == null || Arrays.stream(SplitterLanguage.values()).noneMatch(x -> x.locale != null && lang.equals(x.locale.getLanguage())))
				return SplitterLanguage.ENGLISH.locale;
			return locale;
		}
		return locale;
	}

	public static void saveLanguage(SplitterLanguage language) {
		XT87Utils.savePreference(PREF_LANGUAGE, language.languageCode);
	}

	public static void loadLanguage() {
		String languageCode = XT87Utils.loadPreference(PREF_LANGUAGE);
		SplitterLanguage.selectedLanguage = Arrays.stream(SplitterLanguage.values())
				.filter(x -> Objects.equals(x.getLanguageCode(), languageCode))
				.findFirst().orElse(DEFAULT);
	}

	public String getDisplayName() {
		if (this == SplitterLanguage.DEFAULT)
			return getLocalizedString("language_default");
		return getLocalizedString("language_name", getLocale());
	}
}
