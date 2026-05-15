package net.xetanth87.campaignsplitter.ui;

public enum AppTheme {
	OS("OS"),
	NIMBUS("Nimbus"),
	DARK("Dark");

	private final String displayName;

	AppTheme(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
