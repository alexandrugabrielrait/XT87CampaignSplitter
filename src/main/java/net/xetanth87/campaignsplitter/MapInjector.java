package net.xetanth87.campaignsplitter;

import net.moonlightflower.wc3libs.app.Minimap;
import net.moonlightflower.wc3libs.bin.ObjMod;
import net.moonlightflower.wc3libs.bin.Wc3BinInputStream;
import net.moonlightflower.wc3libs.bin.Wc3BinOutputStream;
import net.moonlightflower.wc3libs.bin.app.IMP;
import net.moonlightflower.wc3libs.bin.app.MMP;
import net.moonlightflower.wc3libs.bin.app.W3I;
import net.moonlightflower.wc3libs.bin.app.objMod.W3A;
import net.moonlightflower.wc3libs.bin.app.objMod.W3B;
import net.moonlightflower.wc3libs.bin.app.objMod.W3D;
import net.moonlightflower.wc3libs.bin.app.objMod.W3H;
import net.moonlightflower.wc3libs.bin.app.objMod.W3Q;
import net.moonlightflower.wc3libs.bin.app.objMod.W3T;
import net.moonlightflower.wc3libs.bin.app.objMod.W3U;
import net.moonlightflower.wc3libs.dataTypes.app.Controller;
import net.moonlightflower.wc3libs.txt.TXT;
import net.moonlightflower.wc3libs.txt.TXTSectionId;
import net.moonlightflower.wc3libs.txt.WTS;
import net.moonlightflower.wc3libs.txt.app.MiscTXT;
import net.moonlightflower.wc3libs.txt.app.SkinTXT;
import net.xetanth87.campaignsplitter.rewriters.CallbackRewriter;
import net.xetanth87.campaignsplitter.rewriters.CheatsRewriter;
import net.xetanth87.campaignsplitter.rewriters.CoopRewriter;
import net.xetanth87.campaignsplitter.rewriters.DifficultySelectorRewriter;
import net.xetanth87.campaignsplitter.rewriters.FakeUserPlayerRewriter;
import net.xetanth87.campaignsplitter.rewriters.GlobalsRewriter;
import net.xetanth87.campaignsplitter.rewriters.MinimapRewriter;
import net.xetanth87.campaignsplitter.rewriters.NeutralPlayerRewriter;
import net.xetanth87.campaignsplitter.rewriters.ScriptRewriter;
import net.xetanth87.campaignsplitter.rewriters.TimerRewriter;
import net.xetanth87.campaignsplitter.rewriters.VictoryRewriter;
import net.xetanth87.campaignsplitter.util.XT87Utils;
import systems.crigges.jmpq3.JMpqEditor;
import systems.crigges.jmpq3.JMpqException;
import systems.crigges.jmpq3.MPQOpenOption;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.channels.NonReadableChannelException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static net.xetanth87.campaignsplitter.util.XT87Utils.FAKE_PLAYER_CONTROLLER;
import static net.xetanth87.campaignsplitter.util.XT87Utils.STRING_PREFIX;
import static net.xetanth87.campaignsplitter.util.XT87Utils.getWithoutExtension;
import static net.xetanth87.campaignsplitter.util.XT87Utils.offsetCampaignDataString;
import static net.xetanth87.campaignsplitter.util.XT87Utils.stringIndexToInt;

public class MapInjector {
	private static final int MIN_EDITOR_VERSION = 6072;

	public CampaignSplitter cs;
	public final File mapFile;
	public final int mapIndex;
	public boolean withDifficultySelector = false;
	public int addedCoopPlayers = 0;
	public JMpqEditor mapEditor;
	public File tempFile;
	public StringBuffer scriptBuffer = null;
	public String scriptPath;
	public boolean isLua;
	public boolean wasTwelvePlayerMap = false;

	public MapInjector(CampaignSplitter cs, File mapFile, int mapIndex) throws IOException {
		if (!mapFile.getName().endsWith(".w3m") && !mapFile.getName().endsWith(".w3x"))
			throw new IllegalArgumentException("Argument must be a map file!");
		this.mapFile = mapFile;
		if (!mapFile.exists())
			throw new FileNotFoundException("Map file not found!");
		this.cs = cs;
		this.mapIndex = mapIndex;
	}

	public void insertDataFile(Object data, String fileName) throws IOException {
		if (data instanceof WTS)
			((WTS) data).write(tempFile);
		else if (data instanceof IMP)
			((IMP) data).write(tempFile);
		else if (data instanceof TXT)
			((TXT) data).write(tempFile);
		else if (data instanceof W3I)
			((W3I) data).write(tempFile);
		else if (data instanceof ObjMod) {
			Wc3BinOutputStream bos = new Wc3BinOutputStream(tempFile);
			((ObjMod) data).write(bos);
			bos.close();
		}
		XT87Utils.overwriteFile(mapEditor, fileName, tempFile, true);
	}

	public void insertImports(IMP imports, String fileName) throws IOException {
		insertDataFile(imports, fileName);
		for (IMP.Obj o : imports.getObjs()) {
			String importPath = o.getPath().trim();
			if (importPath.isEmpty())
				continue;
			String localPath = cs.getImportsPath() + "/" + importPath;
			File importedFile = new File(localPath);
			if (mapEditor.hasFile(importPath) || !importedFile.exists())
				continue;
			mapEditor.insertFile(importPath, importedFile, true);
		}
	}

	public void offsetCampaignStrings(File extractedFile, int campaignKeyOffset) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(extractedFile), StandardCharsets.ISO_8859_1));
		StringBuffer sb = new StringBuffer();
		int s1 = 0;
		while ((s1 = br.read()) != -1) {
			sb.append((char) s1);
		}
		br.close();
		String s = sb.toString();

		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.ISO_8859_1);
		String[] splitStrings = s.split(XT87Utils.STRING_PREFIX);
		if (s.startsWith(XT87Utils.STRING_PREFIX))
			splitStrings[0] = offsetCampaignDataString(splitStrings[0], campaignKeyOffset);
		for (int i = 1; i < splitStrings.length; i++)
			splitStrings[i] = offsetCampaignDataString(splitStrings[i], campaignKeyOffset);
		writer.append(String.join("", splitStrings));
		writer.close();
	}

	public void mergeData(ObjMod data, int campaignKeyOffset, boolean isSkin) throws IOException, NoSuchFieldException, IllegalAccessException {
		String gamePath = ((File) data.getClass().getField("GAME_PATH").get(null)).getName();
		String campaignPath = ((File) data.getClass().getField("CAMPAIGN_PATH").get(null)).getName();
		if (isSkin) {
			gamePath = gamePath.replace(".w3", "Skin.w3");
			campaignPath = campaignPath.replace(".w3", "Skin.w3");
		}
		ObjMod mapData = null;
		Wc3BinInputStream bis = null;
		try {
			offsetCampaignStrings(new File(cs.getTempPath(campaignPath)), campaignKeyOffset);
			bis = new Wc3BinInputStream(tempFile);
			data.read(bis);
			bis.close();
			if (data == null)
				throw new IOException();
		} catch (Throwable e) {
			if (bis != null)
				bis.close();
			if (!(e instanceof IOException))
				System.err.println(e.getMessage());
			return;
		}
		try {
			mapData = XT87Utils.objModOfMapFile(data.getClass(), mapFile, isSkin);
			List<ObjMod.Obj> campaignList = data.getObjsList();
			List<ObjMod.Obj> tempList = new ArrayList<>(campaignList);
			boolean found;
			for (Object object : mapData.getObjsList()) {
				found = false;
				ObjMod.Obj obj = (ObjMod.Obj) object;
				for (ObjMod.Obj value : tempList) {
					if (!value.getId().equals(obj.getId()))
						continue;
					found = true;
					for (ObjMod.Obj.Mod mod : obj.getMods()) {
						for (ObjMod.Obj.Mod cmod : value.getMods())
							if (cmod.getId().equals(mod.getId())) {
								value.getMods().remove(cmod);
								break;
							}
						value.addMod(mod);
					}
					break;
				}
				if (!found)
					campaignList.add(obj);
			}
		} catch (Throwable e) {
			if (!(e instanceof IOException))
				System.err.println(e.getMessage());
			if (e instanceof JMpqException) {
				String message = e.getMessage();
				if (message != null && message.contains("Bad header"))
					throw new NonReadableChannelException();
			}
		}

		insertDataFile(data, gamePath);
	}

	public void mergeData(ObjMod data, int campaignKeyOffset) throws IOException, NoSuchFieldException, IllegalAccessException {
		mergeData(data, campaignKeyOffset, false);
		if (cs.hasSkins)
			mergeData(data, campaignKeyOffset, true);
	}

	public void mergeImports() throws IOException {
		IMP imports;
		try {
			imports = new IMP(new File(cs.getTempPath() + "/" + IMP.CAMPAIGN_PATH));
		} catch (Throwable e) {
			if (!(e instanceof IOException))
				System.err.println(e.getMessage());
			return;
		}
		try {
			mapEditor.extractFile(IMP.GAME_PATH, tempFile);
			XT87Utils.fixImportFile(tempFile, false);
			IMP mapImports = new IMP(tempFile);
			for (IMP.Obj o : mapImports.getObjs())
				if (o.getPath() != null && !o.getPath().isEmpty() && o.getStdFlag() != null)
					imports.addObj(o);
		} catch (Throwable e) {
			if (!(e instanceof IOException))
				System.err.println(e.getMessage());
		}

		insertImports(imports, IMP.GAME_PATH);
	}

	public void changeMapStrings(File extractedFile, Map<String, String> mapStrings) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(extractedFile), StandardCharsets.ISO_8859_1));
		StringBuilder sb = new StringBuilder();
		int s1 = 0;
		while ((s1 = br.read()) != -1) {
			sb.append((char) s1);
		}
		br.close();
		String s = sb.toString();

		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.ISO_8859_1);
		for (String key : new ArrayList<>(mapStrings.keySet().stream().sorted().collect(Collectors.toList())))
			s = s.replaceFirst(key, mapStrings.get(key));

		writer.append(s);
		writer.close();
	}

	static class StringOffsets {
		int campaignKeyOffset = 0;
	}

	public StringOffsets mergeStrings(W3I info) throws Exception {
		WTS strings;
		StringOffsets offsets = new StringOffsets();
		try {
			mapEditor.extractFile(WTS.GAME_PATH.getPath(), tempFile);
			strings = new WTS(tempFile);
			offsets.campaignKeyOffset = Collections.max(strings.getKeyedEntries().keySet()) + 1;
		} catch (Throwable e) {
			strings = null;
			offsets.campaignKeyOffset = 0;
			if (!(e instanceof IOException))
				System.err.println(e.getMessage());
		}

		try {
			if (strings == null)
				strings = cs.campStrings;
			else
				for (int i : cs.campStrings.getKeyedEntries().keySet())
					strings.addEntry(offsets.campaignKeyOffset + i, cs.campStrings.getEntry(i));

			Map<String, String> mapStrings = new HashMap<>();
			info.write(tempFile);

			if (mapIndex >= 0) {
				int buttonStringIndex = cs.chapterNameKeysList.get(mapIndex) + offsets.campaignKeyOffset;
				mapStrings.put(W3I.ofMapFile(mapFile).getMapName(), STRING_PREFIX + buttonStringIndex);
			}

			int campaignAuthorIndex = stringIndexToInt(cs.campaignData.getCampaignAuthor()) + offsets.campaignKeyOffset;
			mapStrings.put(info.getMapAuthor(), STRING_PREFIX + campaignAuthorIndex);

			String mapDescription = cs.getFeatureDescription() + "[" + mapFile.getName() + "]";
			// in Archon Mode, we don't have enough time to read the loading screen text, so we add it here if it exists
			if (addedCoopPlayers > 0) {
				try {
					String loadingScreenText = WTS.ofMapFile(mapFile).getEntry(stringIndexToInt(info.getLoadingScreen().getText()));
					loadingScreenText = loadingScreenText.replace("\r\n", "\n");
					while (loadingScreenText.contains("\n "))
						loadingScreenText = loadingScreenText.replace("\n ", "\n");
					mapDescription = mapDescription + "\n\n" + loadingScreenText;
				} catch (Throwable ignored) {
				}
			}
			int mapDescriptionIndex = Collections.max(strings.getKeyedEntries().keySet()) + 1;
			strings.addEntry(mapDescriptionIndex, mapDescription);
			mapStrings.put(info.getMapDescription(), STRING_PREFIX + mapDescriptionIndex);

			changeMapStrings(tempFile, mapStrings);
			insertDataFile(tempFile, W3I.GAME_PATH.getPath());

			insertDataFile(strings, WTS.GAME_PATH.getPath());
		} catch (Throwable e) {
			if (!(e instanceof IOException))
				System.err.println(e.getMessage());
		}

		return offsets;
	}

	public void mergeMisc() {
		MiscTXT txt = null;
		boolean withModification = cs.withUpkeepRemoval || addedCoopPlayers > 0;
		try {
			txt = new MiscTXT();
			txt.read(new File(XT87Utils.toCampaignPath(cs.getTempPath(MiscTXT.GAME_PATH.getName()))));
		} catch (Throwable e) {
			if (!(e instanceof IOException))
				System.err.println(e.getMessage());
			if (!withModification)
				return;
		}
		try {
			if (!mapEditor.hasFile(MiscTXT.GAME_PATH.getPath()))
				throw new IOException();
			mapEditor.extractFile(MiscTXT.GAME_PATH.getPath(), tempFile);
			if (txt == null) {
				txt = new MiscTXT();
				txt.read(tempFile);
			} else {
				MiscTXT mapTxt = new MiscTXT();
				mapTxt.read(tempFile);
				txt.merge(mapTxt, true);
			}
		} catch (Throwable e) {
			if (!(e instanceof IOException))
				System.err.println(e.getMessage());
		} finally {
			try {
				if (withModification) {
					if (txt == null)
						txt = new MiscTXT();
					final TXTSectionId sectionId = TXTSectionId.valueOf("Misc");
					TXT.Section section = txt.getSection(sectionId);
					if (section == null)
						section = txt.addSection(sectionId);
					if (cs.withUpkeepRemoval) {
						String tax = "0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00";
						String usage = "10000,10000,10000,10000,10000,10000,10000,10000,10000,10000";
						section.setLine(MiscTXT.State.RESOURCES_SUPPLY_UPKEEP_COSTS_GOLD.getFieldId(), tax);
						section.setLine(MiscTXT.State.RESOURCES_SUPPLY_UPKEEP_COSTS_LUMBER.getFieldId(), tax);
						section.setLine(MiscTXT.State.RESOURCES_SUPPLY_UPKEEP_LEVELS.getFieldId(), usage);
					}
					if (cs.archonModeAddedSlots > 0) {
						section.setLine(MiscTXT.State.TRADING_INC_SMALL.getFieldId(), "0");
						section.setLine(MiscTXT.State.TRADING_INC_LARGE.getFieldId(), "0");
					}
				}
				insertDataFile(txt, MiscTXT.GAME_PATH.getPath());
			} catch (Throwable e) {
				if (!(e instanceof IOException))
					System.err.println(e.getMessage());
			}
		}
	}

	public void mergeSkin(int campaignKeyOffset) throws IOException {
		SkinTXT txt = null;
		try {
			txt = new SkinTXT();
			offsetCampaignStrings(new File(XT87Utils.toCampaignPath(cs.getTempPath(SkinTXT.GAME_PATH.getName()))), campaignKeyOffset);
			txt.read(tempFile);
		} catch (Throwable e) {
			if (!(e instanceof IOException))
				System.err.println(e.getMessage());
			return;
		}
		try {
			if (!mapEditor.hasFile(SkinTXT.GAME_PATH.getPath()))
				throw new IOException();
			mapEditor.extractFile(SkinTXT.GAME_PATH.getPath(), tempFile);
			if (txt == null) {
				txt = new SkinTXT();
				txt.read(tempFile);
			} else {
				SkinTXT mapTxt = new SkinTXT();
				mapTxt.read(tempFile);
				txt.merge(mapTxt, true);
			}
		} catch (Throwable e) {
			if (!(e instanceof IOException))
				System.err.println(e.getMessage());
		} finally {
			insertDataFile(txt, SkinTXT.GAME_PATH.getPath());
		}
	}

	public void addSecondaryPlayers(int campaignKeyOffset) throws IOException {
		SkinTXT txt = null;
		try {
			txt = new SkinTXT();
			offsetCampaignStrings(new File(cs.getTempPath(XT87Utils.toCampaignPath(SkinTXT.GAME_PATH.getName()))), campaignKeyOffset);
			txt.read(tempFile);
		} catch (Throwable e) {
			if (!(e instanceof IOException))
				System.err.println(e.getMessage());
			return;
		}
		try {
			if (!mapEditor.hasFile(SkinTXT.GAME_PATH.getPath()))
				throw new IOException();
			mapEditor.extractFile(SkinTXT.GAME_PATH.getPath(), tempFile);
			if (txt == null) {
				txt = new SkinTXT();
				txt.read(tempFile);
			} else {
				SkinTXT mapTxt = new SkinTXT();
				mapTxt.read(tempFile);
				txt.merge(mapTxt, true);
			}
		} catch (Throwable e) {
			if (!(e instanceof IOException))
				System.err.println(e.getMessage());
		} finally {
			insertDataFile(txt, SkinTXT.GAME_PATH.getPath());
		}
	}

	public void changePreview() throws IOException {
		String campaignPreviewPath = cs.campaignData.getMinimapPath().getPath();
		if (campaignPreviewPath == null || campaignPreviewPath.isEmpty())
			return;
		File campaignPreviewFile = new File(cs.getImportsPath() + "/" + campaignPreviewPath);
		String previewImagePath = null;
		boolean hasPreviewImage = false;
		if (mapEditor.hasFile(previewImagePath = Minimap.PREVIEW_BLP_GAME_PATH.getPath()))
			hasPreviewImage = true;
		else if (mapEditor.hasFile(previewImagePath = Minimap.PREVIEW_TGA_GAME_PATH.getPath()))
			hasPreviewImage = true;
		if (hasPreviewImage || campaignPreviewFile.exists()) {
			System.out.println("Changing preview for map \"" + mapFile.getName() + "\".");
			String minimapImagePath = null;
			boolean hasMinimapImage = false;
			if (mapEditor.hasFile(minimapImagePath = Minimap.BACKGROUND_BLP_GAME_PATH.getPath()))
				hasMinimapImage = true;
			else if (mapEditor.hasFile(minimapImagePath = Minimap.BACKGROUND_TGA_GAME_PATH.getPath()))
				hasMinimapImage = true;
			String mmpPath = MMP.GAME_PATH.getPath();
			if (mapEditor.hasFile(mmpPath))
				mapEditor.deleteFile(mmpPath);
			if (hasMinimapImage) {
				mapEditor.extractFile(minimapImagePath, tempFile);
				mapEditor.deleteFile(minimapImagePath);
				mapEditor.insertFile(MinimapRewriter.getNewPath(minimapImagePath), tempFile, true);
				new MinimapRewriter(this, minimapImagePath).modifyScript();
			}
			minimapImagePath = XT87Utils.getExtension(hasPreviewImage ? previewImagePath : campaignPreviewPath).equals("tga") ? Minimap.BACKGROUND_TGA_GAME_PATH.getPath() : Minimap.BACKGROUND_BLP_GAME_PATH.getPath();
			if (hasPreviewImage) {
				mapEditor.extractFile(previewImagePath, tempFile);
				mapEditor.deleteFile(previewImagePath);
			}
			mapEditor.insertFile(minimapImagePath, hasPreviewImage ? tempFile : campaignPreviewFile, true);
			System.out.println("Finished changing preview for map \"" + mapFile.getName() + "\".");
			cs.IncrementValueProgressBar(1);
		}
	}

	private class MapInfo {
		public W3I info;
		public W3I.Player mainPlayer;
		public List<W3I.Player> secondaryPlayers;
		public Set<Integer> fakeUserPlayersSet;

		public MapInfo(W3I info, W3I.Player mainPlayer, List<W3I.Player> secondaryPlayers, Set<Integer> fakeUserPlayersSet) {
			this.info = info;
			this.mainPlayer = mainPlayer;
			this.secondaryPlayers = secondaryPlayers;
			this.fakeUserPlayersSet = fakeUserPlayersSet;
		}
	}

	private MapInfo modifyInfo() throws Exception {
		W3I info = W3I.ofMapFile(mapFile);
		W3I.Player mainPlayer = null;
		Set<Integer> fakeUserPlayersSet = new HashSet<>();
		for (W3I.Player p : info.getPlayers())
			if (p.getType().equals(Controller.USER)) {
				if (mainPlayer == null)
					mainPlayer = p;
				else {
					p.setType(FAKE_PLAYER_CONTROLLER);
					fakeUserPlayersSet.add(p.getNum());
				}
			}

		List<W3I.Player> secondaryPlayers = null;
		if (addedCoopPlayers > 0) {
			// to enable 24 players in the lobby
			if (info.getEditorVersion() < MIN_EDITOR_VERSION) {
				wasTwelvePlayerMap = true;
				info.setEditorVersion(MIN_EDITOR_VERSION);
			}
			W3I.Player finalMainPlayer = mainPlayer;
			W3I.Force force = info.getForces().stream().filter(f -> f.getPlayerNums().contains(finalMainPlayer.getNum())).findFirst().get();
			List<Integer> unusedPlayerNumbers = new ArrayList<>();
			for (int i = mainPlayer.getNum() + 1; i < XT87Utils.MAX_PLAYER_COUNT; i++)
				unusedPlayerNumbers.add(i);
			for (int i = 0; i < mainPlayer.getNum(); i++)
				unusedPlayerNumbers.add(i);
			for (Integer id : XT87Utils.getUsedPlayerNumbers(this))
				unusedPlayerNumbers.remove(id);
			unusedPlayerNumbers = unusedPlayerNumbers.subList(0, addedCoopPlayers);
			secondaryPlayers = new ArrayList<>();
			for (int i : unusedPlayerNumbers) {
				W3I.Player secondaryPlayer = XT87Utils.clonePlayer(mainPlayer, i);
				info.getForces().forEach(f -> f.removePlayerNums(i));
				force.addPlayerNums(i);
				secondaryPlayers.add(secondaryPlayer);
				info.getPlayers().add(secondaryPlayer);
			}
		}
		return new MapInfo(info, mainPlayer, secondaryPlayers, fakeUserPlayersSet);
	}

	public void initEditor() throws IOException {
		mapEditor = new JMpqEditor(mapFile, MPQOpenOption.FORCE_V0);
		tempFile = new File(cs.getTempPath() + "/" + getWithoutExtension(mapFile.getName()) + ".temp.txt");
		tempFile.getParentFile().mkdirs();
		tempFile.createNewFile();
	}

	public void closeEditor() throws IOException {
		tempFile.delete();
		mapEditor.close();
		mapEditor = null;
	}

	public void addCampaignData() throws Exception {
		initEditor();

		// imports
		System.out.println("Adding imports to map \"" + mapFile.getName() + "\".");
		mergeImports();
		cs.IncrementValueProgressBar(1);
		MapInfo mapInfo = modifyInfo();
		cs.IncrementValueProgressBar(1);
		// strings
		System.out.println("Adding strings to map \"" + mapFile.getName() + "\".");
		StringOffsets offsets = mergeStrings(mapInfo.info);
		cs.IncrementValueProgressBar(1);
		// misc/constants
		System.out.println("Adding misc/constants to map \"" + mapFile.getName() + "\".");
		mergeMisc();
		cs.IncrementValueProgressBar(1);
		// skin/interface
		System.out.println("Adding skin/interface to map \"" + mapFile.getName() + "\".");
		mergeSkin(offsets.campaignKeyOffset);
		cs.IncrementValueProgressBar(1);

		System.out.println("Adding data to map \"" + mapFile.getName() + "\".");
		// units
		mergeData(new W3U(), offsets.campaignKeyOffset);
		cs.IncrementValueProgressBar(1);
		// items
		mergeData(new W3T(), offsets.campaignKeyOffset);
		cs.IncrementValueProgressBar(1);
		// destructibles
		mergeData(new W3B(), offsets.campaignKeyOffset);
		cs.IncrementValueProgressBar(1);
		// doodads
		mergeData(new W3D(), offsets.campaignKeyOffset);
		cs.IncrementValueProgressBar(1);
		// abilities
		mergeData(new W3A(), offsets.campaignKeyOffset);
		cs.IncrementValueProgressBar(1);
		// buffs
		mergeData(new W3H(), offsets.campaignKeyOffset);
		cs.IncrementValueProgressBar(1);
		// upgrades
		mergeData(new W3Q(), offsets.campaignKeyOffset);
		cs.IncrementValueProgressBar(1);

		boolean editScript = withDifficultySelector || cs.withCampaignPreview
				|| cs.withLegacyAssetsSD || cs.withLegacyAssetsHD
				|| cs.withNextLevel || addedCoopPlayers > 0 || wasTwelvePlayerMap;

		if (editScript) {
			ScriptRewriter.readScript(this);

			if (wasTwelvePlayerMap)
				new NeutralPlayerRewriter(this).modifyScript();

			if (mapInfo.fakeUserPlayersSet != null && !mapInfo.fakeUserPlayersSet.isEmpty())
				new FakeUserPlayerRewriter(this, mapInfo.fakeUserPlayersSet).modifyScript();

			if (withDifficultySelector || addedCoopPlayers > 0)
				new TimerRewriter(this).modifyScript();

			if (withDifficultySelector) {
				System.out.println("Adding Difficulty Selector to map \"" + mapFile.getName() + "\".");
				new DifficultySelectorRewriter(this, mapInfo.mainPlayer.getNum()).modifyScript();
				System.out.println("Finished adding Difficulty Selector to map \"" + mapFile.getName() + "\".");
				cs.IncrementValueProgressBar(1);
			}

			if (cs.withCampaignPreview)
				changePreview();

			if (cs.withNextLevel)
				System.out.println("Adding next level message to map \"" + mapFile.getName() + "\".");
			new VictoryRewriter(this, offsets.campaignKeyOffset, cs.withNextLevel).modifyScript();
			if (cs.withNextLevel) {
				System.out.println("Finished adding next level message to map \"" + mapFile.getName() + "\".");
				cs.IncrementValueProgressBar(1);
			}

			new CheatsRewriter(this, mapInfo.mainPlayer).modifyScript();

			if (addedCoopPlayers > 0) {
				System.out.println("Adding coop support to map \"" + mapFile.getName() + "\".");
				new CallbackRewriter(this).modifyScript();
				new CoopRewriter(this, mapInfo.info, mapInfo.mainPlayer, mapInfo.secondaryPlayers, offsets.campaignKeyOffset, cs.archonArrows).modifyScript();
				System.out.println("Finished adding coop support to map \"" + mapFile.getName() + "\".");
				cs.IncrementValueProgressBar(1);
			}

			new GlobalsRewriter(this).modifyScript();

			ScriptRewriter.insertScript(this);
		}

		System.out.println("Saving map \"" + mapFile.getName() + "\".");
		closeEditor();
		cs.IncrementValueProgressBar(10);
		System.out.println("Finished saving map \"" + mapFile.getName() + "\".");
	}
}
