package net.xetanth87.campaignsplitter;

import net.xetanth87.campaignsplitter.util.XT87Utils;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

import static net.xetanth87.campaignsplitter.util.XT87Utils.getLocalizedString;

public enum SplitterLanguage {
	DEFAULT("default", Locale.getDefault()),
	ENGLISH("en", Locale.ENGLISH),
	SPANISH("es", new Locale("es")),
	GERMAN("de", Locale.GERMAN),
	//FRENCH("fr", Locale.FRENCH),
	//ITALIAN("it", Locale.ITALIAN),
	//PORTUGUESE("pt", new Locale("pt")),
	//POLISH("pl", new Locale("pl")),
	RUSSIAN("ru", new Locale("ru")),
	ROMANIAN("ro", new Locale("ro")),
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

	public String getDisplayName()
	{
		if (this == SplitterLanguage.DEFAULT)
			return getLocalizedString("language_default");
		return XT87Utils.getLocalizedString("language_name", locale);
	}
}
