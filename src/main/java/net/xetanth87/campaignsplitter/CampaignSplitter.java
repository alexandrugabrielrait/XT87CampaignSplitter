package net.xetanth87.campaignsplitter;

import net.moonlightflower.wc3libs.bin.app.IMP;
import net.moonlightflower.wc3libs.bin.app.W3F;
import net.moonlightflower.wc3libs.bin.app.objMod.W3A;
import net.moonlightflower.wc3libs.bin.app.objMod.W3B;
import net.moonlightflower.wc3libs.bin.app.objMod.W3D;
import net.moonlightflower.wc3libs.bin.app.objMod.W3H;
import net.moonlightflower.wc3libs.bin.app.objMod.W3Q;
import net.moonlightflower.wc3libs.bin.app.objMod.W3T;
import net.moonlightflower.wc3libs.bin.app.objMod.W3U;
import net.moonlightflower.wc3libs.txt.WTS;
import net.moonlightflower.wc3libs.txt.app.MiscTXT;
import net.moonlightflower.wc3libs.txt.app.SkinTXT;
import systems.crigges.jmpq3.JMpqEditor;
import systems.crigges.jmpq3.MPQOpenOption;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.xetanth87.campaignsplitter.XT87Utils.DOWNLOAD_FOLDER;
import static net.xetanth87.campaignsplitter.XT87Utils.STRING_PREFIX;
import static net.xetanth87.campaignsplitter.XT87Utils.buttonText;
import static net.xetanth87.campaignsplitter.XT87Utils.getWithoutExtension;
import static net.xetanth87.campaignsplitter.XT87Utils.stringIndexToInt;

public class CampaignSplitter {
	public static final String IMPORTS_DIR_NAME = "imports";
	private static final int STEPS_CAMP_DATA = 4;
	public static final String MAP_COUNT_FORMAT = "Maps converted: {0}/{1}.";
	public final File campFile;
	public final String splitPath;
	public final String tempDirName;
	public int buttonCount, archonModeAddedSlots, archonModeRealAddedSlots;
	public XT87Utils.TriOption difficultySelectorOption = XT87Utils.TriOption.SMART;
	public boolean withDifficultySelector = false, withCampaignPreview = false, withUpkeepRemoval = false,
			withLegacyAssetsSD = true, withLegacyAssetsHD = true, withNextLevel = true, hasSkins = false, toDownloadFolder = false;
	public W3F campaignData;
	protected boolean initializedProgressBar = false;
	public WTS campStrings = null;
	public Map<String, String> buttonNameMap = new HashMap<>();
	private int stepsPerMap = 0;

	public static String getVersion() {
		return "2.00";
	}

	public void InitializeProgressBar(int i) {
		initializedProgressBar = true;
	}

	public void SetValueProgressBar(int i) {
	}

	public void IncrementValueProgressBar(int i) {
		if (!initializedProgressBar)
			stepsPerMap += i;
	}

	public void CompleteProgressBar() {
	}

	public String getTempPath() {
		return getSplitPath() + "/" + tempDirName;
	}

	public String getTempPath(String filename) {
		return getTempPath() + "/" + filename;
	}

	public String getImportsPath() {
		return getTempPath() + "/" + IMPORTS_DIR_NAME;
	}

	public String getImportsPath(String filename) {
		return getImportsPath() + "/" + filename;
	}

	public CampaignSplitter(File campFile) throws IOException, IllegalArgumentException {
		if (!campFile.getName().endsWith(".w3n"))
			throw new IllegalArgumentException("Argument must be a campaign file!");
		if (!campFile.exists())
			throw new FileNotFoundException("Campaign file not found!");
		this.campFile = campFile;
		String campaignName = getWithoutExtension(campFile.getName());
		splitPath = campFile.getParentFile().getAbsolutePath() + "/" + campaignName;
		tempDirName = "temp_" + campaignName;
	}

	public String getSplitPath() {
		if (toDownloadFolder)
			return DOWNLOAD_FOLDER;
		return splitPath;
	}

	public void removeTemporaryFiles() {
		try {
			String importsPath = getSplitPath() + "/" + tempDirName;
			File importsDir = new File(importsPath);
			if (importsDir.exists())
				XT87Utils.deleteDirectory(importsDir);
		} catch (Exception e) {
		}
	}

	public String getFeatureDescription() {
		String featureDescription = "Xetanth87's Campaign Splitter v" + getVersion();// getClass().getPackage().getImplementationVersion();
		if (archonModeAddedSlots > 0)
			featureDescription += "\n- Archon Mode (" + (archonModeRealAddedSlots + 1) + " slots)";
		if (withDifficultySelector)
			featureDescription += "\n- Difficulty Selector";
		if (withCampaignPreview)
			featureDescription += "\n- Campaign Preview";
		if (withNextLevel)
			featureDescription += "\n- Next Level Name";
		if (withLegacyAssetsSD)
			featureDescription += "\n- SD Legacy Assets";
		if (withLegacyAssetsHD)
			featureDescription += "\n- HD Legacy Assets";
		if (withUpkeepRemoval)
			featureDescription += "\n-Upkeep removed";
		return featureDescription;
	}

	public void extractCampaignFiles(JMpqEditor campEditor) throws IOException {
		File importsDir = new File(getImportsPath());
		importsDir.mkdirs();
		List<String> extractedFileNames = Stream.of(WTS.GAME_PATH, MiscTXT.GAME_PATH,
						SkinTXT.GAME_PATH, W3U.GAME_PATH, W3T.GAME_PATH, W3B.GAME_PATH,
						W3D.GAME_PATH, W3A.GAME_PATH, W3H.GAME_PATH, W3Q.GAME_PATH)
				.map(x -> XT87Utils.toCampaignPath(x.getName())).collect(Collectors.toList());
		System.out.println("Extracting campaign configuration files.");
		for (String fileName : extractedFileNames) {
			List<String> subfileNames = new ArrayList<>(Collections.singletonList(fileName));
			if (fileName.contains(".w3"))
				subfileNames.add(fileName.replace(".w3", "Skin.w3"));
			for (String subfileName : subfileNames) {
				if (!campEditor.hasFile(subfileName))
					continue;
				if (!hasSkins && subfileName.contains("campaignSkin."))
					hasSkins = true;
				File extractedFile = new File(getTempPath() + "/" + subfileName);
				extractedFile.createNewFile();
				campEditor.extractFile(subfileName, extractedFile);
			}
		}
		File importsListFile = null;
		IMP imports = null;

		File wtsFile = new File(getTempPath(WTS.CAMPAIGN_PATH.getName()));
		if (wtsFile.exists()) {
			campStrings = new WTS(wtsFile);
			wtsFile.delete();
		}

		if (campEditor.hasFile(IMP.CAMPAIGN_PATH.getName())) {
			importsListFile = new File(getTempPath() + "/" + IMP.CAMPAIGN_PATH);
			importsListFile.createNewFile();
			campEditor.extractFile(IMP.CAMPAIGN_PATH.getName(), importsListFile);
			XT87Utils.fixImportFile(importsListFile, true);
			imports = new IMP(importsListFile);
			System.out.println("Extracting campaign imported files.");
			for (IMP.Obj o : imports.getObjs()) {
				if (o.getPath().isEmpty())
					continue;
				if (!campEditor.hasFile(o.getPath())) {
					System.err.println("Missing campaign import with path \"" + o.getPath() + "\"!");
					continue;
				}
				File importedFile = new File(getImportsPath() + "/" + o.getPath().trim());
				importedFile.getParentFile().mkdirs();
				importedFile.createNewFile();
				campEditor.extractFile(o.getPath(), importedFile);
			}
			System.out.println("Finished extracting campaign imported files.");
		}

		if (true) {
			System.out.println("Extracting auxiliary asset files.");
			boolean noCampaignImports = importsListFile == null;
			if (noCampaignImports) {
				importsListFile = new File(getTempPath() + "/" + IMP.CAMPAIGN_PATH);
				importsListFile.createNewFile();
			}

			if (imports == null)
				imports = new IMP();

			if (ImportsMerger.AddLegacyAssets(this, imports)) {
				System.out.println("Finished extracting auxiliary asset files.");
				imports.write(importsListFile);
			} else if (noCampaignImports) {
				importsListFile.delete();
			}
		}

		if (withLegacyAssetsSD || withLegacyAssetsHD) {
			System.out.println("Extracting legacy asset files.");
			boolean noCampaignImports = importsListFile == null;
			if (noCampaignImports) {
				importsListFile = new File(getTempPath() + "/" + IMP.CAMPAIGN_PATH);
				importsListFile.createNewFile();
			}

			if (imports == null)
				imports = new IMP();

			if (LegacyMerger.AddLegacyAssets(this, imports)) {
				System.out.println("Finished extracting legacy asset files.");
				imports.write(importsListFile);
			} else if (noCampaignImports) {
				importsListFile.delete();
			}
		}
	}

	public void splitCampaign() throws Exception {
		System.out.println(campFile.getAbsolutePath());
		campaignData = W3F.ofCampaignFile(campFile);
		buttonCount = campaignData.getMaps().size();
		SetValueProgressBar(0);
		{
			File splitDir = new File(getSplitPath());
			splitDir.mkdirs();
		}
		withDifficultySelector = difficultySelectorOption.equals(XT87Utils.TriOption.YES) ||
				(difficultySelectorOption.equals(XT87Utils.TriOption.SMART) && campaignData.getFlag(W3F.Flags.Flag.VAR_DIFFICULTY));
		List<File> mapFiles = new ArrayList<>();
		archonModeRealAddedSlots = archonModeAddedSlots;
		// region extract files from campaign
		{
			JMpqEditor campEditor = new JMpqEditor(campFile, MPQOpenOption.READ_ONLY);
			extractCampaignFiles(campEditor);
			System.out.println("Difficulty selector: " + (withDifficultySelector ? "enabled" : "disabled") + ".");
			for (int i = 0; i < buttonCount; i++) {
				W3F.MapEntry mapEntry = campaignData.getMaps().get(i);
				String title = mapEntry.getChapterTitle();
				if (title == null || !title.contains(STRING_PREFIX))
					throw new Exception("Invalid chapter title for map " + mapEntry.getMapPath() + ": \"" + title + "\" (Broken campaign)");
				int chapterTitleIndex = stringIndexToInt(title);
				title = mapEntry.getMapTitle();
				if (title == null || !title.contains(STRING_PREFIX))
					throw new Exception("Invalid map title for map " + mapEntry.getMapPath() + ": \"" + title + "\" (Broken campaign)");
				int mapTitleIndex = stringIndexToInt(title);
				String buttonTitle = buttonText(i, buttonCount) + campStrings.getKeyedEntries().get(chapterTitleIndex) + ": " + campStrings.getKeyedEntries().get(mapTitleIndex);
				campStrings.addEntry(mapTitleIndex, buttonTitle);
				buttonNameMap.put(mapEntry.getMapPath().toLowerCase(), buttonTitle);
			}
			for (int i = 0; i < buttonCount; i++) {
				W3F.MapEntry mapEntry = campaignData.getMaps().get(i);
				String fileName = mapEntry.getMapPath();
				String mapPath = getSplitPath() + "/" + fileName;
//			if (i != 0)
//				campEditor = new JMpqEditor(campFile, MPQOpenOption.READ_ONLY);
				if (!campEditor.hasFile(fileName))
					continue;
				File mapFile = new File(mapPath);
				mapFile.createNewFile();
				campEditor.extractFile(fileName, mapFile);
				mapFiles.add(mapFile);
				System.out.println("Extracted map \"" + mapFile.getName() + "\".");
				Set<Integer> usedPlayerNumbers = XT87Utils.getUsedPlayerNumbers(new MapInjector(this, mapFile, i));
				archonModeRealAddedSlots = Math.min(archonModeRealAddedSlots, XT87Utils.MAX_PLAYER_COUNT - usedPlayerNumbers.size());
			}
			campEditor.close();
		}
		// endregion
		if (archonModeAddedSlots > 0)
			System.out.println("Number of coop slots to be added: " + archonModeRealAddedSlots + " (" + (archonModeRealAddedSlots + 1) + " total slots).");
		for (int i = 0; i < buttonCount; i++) {
			File mapFile = mapFiles.get(i);
			MapInjector mi = new MapInjector(this, mapFile, i);
			mi.withDifficultySelector = withDifficultySelector;
			mi.addedCoopPlayers = archonModeRealAddedSlots;
			try {
				mi.addCampaignData();
				System.out.println(MessageFormat.format(MAP_COUNT_FORMAT, i + 1, buttonCount));
			} catch (NonReadableChannelException | NonWritableChannelException e) {
				throw new Exception("Map \"" + mapFile.getName() + "\" is protected and cannot be read/edited.");
			}
			if (i == 0) {
				InitializeProgressBar(STEPS_CAMP_DATA + stepsPerMap * buttonCount);
				SetValueProgressBar(STEPS_CAMP_DATA + stepsPerMap);
			}
		}
		XT87Utils.deleteNewFiles();
		removeTemporaryFiles();
		CompleteProgressBar();
	}

	public static void main(String[] args) {
		String campaignPath = null;
		boolean acceptZeroParam = false;
		int acceptedArgsLength = 1;
		if (args.length != acceptedArgsLength && (args.length != 0 || !acceptZeroParam))
			throw new IllegalArgumentException("Wrong number of arguments! Expected: " + acceptedArgsLength + ", Got: " + args.length);
		if (args.length > 0) {
			campaignPath = args[0];
		}
		CampaignSplitter cs = null;
		try {
			cs = new CampaignSplitter(new File(campaignPath));
			cs.splitCampaign();
		} catch (Exception e) {
			e.printStackTrace();
			XT87Utils.deleteNewFiles();
			cs.removeTemporaryFiles();
		}
	}
}