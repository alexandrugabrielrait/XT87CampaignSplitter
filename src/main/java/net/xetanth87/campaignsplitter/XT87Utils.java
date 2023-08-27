package net.xetanth87.campaignsplitter;

import com.ibm.icu.impl.Assert;
import net.moonlightflower.wc3libs.bin.ObjMod;
import net.moonlightflower.wc3libs.bin.Wc3BinInputStream;
import net.moonlightflower.wc3libs.bin.app.W3I;
import net.moonlightflower.wc3libs.dataTypes.app.Controller;
import net.moonlightflower.wc3libs.port.JMpqPort;
import net.moonlightflower.wc3libs.port.MpqPort;
import systems.crigges.jmpq3.JMpqEditor;
import systems.crigges.jmpq3.MPQOpenOption;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

public class XT87Utils {
	public static final String STRING_PREFIX = "TRIGSTR_";
	public static final String IMPORT_DELIM = "\r";
	public static final String DATA_DELIM = "\n";
	public static final String PATH_PREFIX = "XT87CampaignSplitter";
	public static final String DISABLE_TRIGGER = "call DisableTrigger(";
	public static final String ENABLE_TRIGGER = "call EnableTrigger(";
	public static final String THIS_TRIGGER = "GetTriggeringTrigger()";
	public static final int MAX_PLAYER_COUNT = 24;
	public static final String BASE62_CHARS = "XlvWV7GmwyH5M9YsNhJkCUzAZT6roxOLnqIe0KjcFiDQdtpaP2S3uE1bRg84fB";
	public static final String GAME_FOLDER = FileSystemView.getFileSystemView().getDefaultDirectory().getAbsolutePath() + "/Warcraft III";
	public static final String MAPS_FOLDER = GAME_FOLDER + "/Maps";
	public static final String CAMPAIGNS_FOLDER = GAME_FOLDER + "/Campaigns";
	public static final String DOWNLOAD_FOLDER = MAPS_FOLDER + "/Download";
	public static final Controller FAKE_PLAYER_CONTROLLER = Controller.COMPUTER;

	static {
		assert BASE62_CHARS.length() == 62 && BASE62_CHARS.length() == BASE62_CHARS.chars().distinct().count();
	}
	public static Stack<File> createdFiles = new Stack<>();

	public enum TriOption {
		NO,
		SMART,
		YES
	}

	public static Set<Integer> getUsedPlayerNumbers(MapInjector mi) throws Exception {
		W3I info = W3I.ofMapFile(mi.mapFile);
		UsedPlayersScriptReader upsr = new UsedPlayersScriptReader(mi, info.getPlayers().stream()
				.map(W3I.Player::getNum).collect(Collectors.toSet()));
		boolean hadEditor = mi.mapEditor != null;
		if (!hadEditor)
			mi.initEditor();
		ScriptRewriter.readScript(mi);
		upsr.modifyScript();
		if (!hadEditor)
			mi.closeEditor();
		return upsr.getUsedSet();
	}

	public static String beautifyEnum(Enum e) {
		String s = e.toString().replace("_", " ");
		return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
	}

	public static void createNewFile(File file) throws IOException {
		file.createNewFile();
		createdFiles.push(file);
	}

	public static void createNewDir(File file) throws IOException {
		file.mkdir();
		createdFiles.push(file);
	}

	public static void deleteNewFiles() {
		while (!createdFiles.empty())
		{
			File file = createdFiles.pop();
			if (file.exists())
				file.delete();
		}
	}

	public static String zeroedNumber(int number, int charCount)
	{
		String buttonIndexText = number + "";
		int addedZeroes = Math.max(charCount - buttonIndexText.length(), 0);
		for (int i = 0; i < addedZeroes; i ++)
			buttonIndexText = "0" + buttonIndexText;
		return buttonIndexText;
	}

	public static String alphabetNumber(int number, int charCount) {
		if (charCount <= 0) {
			throw new IllegalArgumentException("charCount must be greater than 0");
		}

		StringBuilder result = new StringBuilder();

		while (charCount > 0) {
			int remainder = number % 26;
			char letter = (char) ('a' + remainder);
			result.insert(0, letter);

			number /= 26;
			charCount--;
		}

		return result.toString();
	}

	public static String toCampaignPath(String gamePath)
	{
		return gamePath.replace("war3map.", "war3campaign.");
	}

	public static void overwriteFile(JMpqEditor editor, String name, File file, boolean backupFile) throws IOException {
		if (editor.hasFile(name))
			editor.deleteFile(name);
		editor.insertFile(name, file, backupFile);
	}

	public static <T extends ObjMod> T objModOfMapFile(Class<T> clazz, File mapFile, boolean isSkin) throws IOException {
		if (!mapFile.exists()) throw new IOException(String.format("file %s does not exist", mapFile));

		MpqPort.Out port = new JMpqPort.Out();

		try {
			File inFilePath = (File) clazz.getField("GAME_PATH").get(null);
			if (isSkin)
				inFilePath = new File(inFilePath.getPath().replace(".w3", "Skin.w3"));

			port.add(inFilePath);

			MpqPort.Out.Result portResult = port.commit(mapFile);

			if (!portResult.getExports().containsKey(inFilePath)) throw new IOException("could not extract w3a file");

			Wc3BinInputStream inStream = new Wc3BinInputStream(portResult.getInputStream(inFilePath));

			T objMod = ObjMod.create(clazz);

			objMod.read(inStream);

			inStream.close();

			return objMod;
		} catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
			throw new AssertionError(e);
		}
	}

	public static boolean deleteDirectory(File directoryToBeDeleted) {
		File[] allContents = directoryToBeDeleted.listFiles();
		if (allContents != null) {
			for (File file : allContents) {
				deleteDirectory(file);
			}
		}
		return directoryToBeDeleted.delete();
	}

	public static void fixImportFile(File extractedFile, boolean isCampaignFile) throws IOException {
		String defaultPath = isCampaignFile ? "war3campImported\\" : "war3mapImported\\";
		byte[] bytes = Files.readAllBytes(extractedFile.toPath());
		byte[] content = new byte[bytes.length - 8];
		for (int i = 0; i < content.length; i++)
			content[i] = bytes[i + 8];
		String s = new String(content, StandardCharsets.US_ASCII)
				.replace("\u0005", IMPORT_DELIM)
				.replace("\b" + defaultPath, IMPORT_DELIM + defaultPath)
				.replace("\b", IMPORT_DELIM + defaultPath)
				.replace("\0\0", "\0" + IMPORT_DELIM + defaultPath)
				.replace(defaultPath + "\0", "\b\0")
				.replace("\u0015", IMPORT_DELIM)
				.replace("\u001D", IMPORT_DELIM);

		try (FileOutputStream output = new FileOutputStream(extractedFile)) {
			output.write(Arrays.copyOf(bytes, 8));
		}

		FileWriter writer = new FileWriter(extractedFile, true);
		writer.append(s);
		writer.close();
	}

	public static int stringIndexToInt(String s) {
		return Integer.parseInt(s.replaceFirst(STRING_PREFIX, ""));
	}

	public static String buttonText(int buttonIndex, int buttonCount) {
		return alphabetNumber(buttonIndex, ((buttonCount - 1) / 26) + 1) + ". ";
	}

	public static String getWithoutExtension(String path) {
		return path.replaceFirst("[.][^.]+$", "");
	}

	public static String getExtension(String path) {
		return path.substring(path.lastIndexOf('.') + 1);
	}

	public static String offsetCampaignDataString(String string, int campaignKeyOffset) {
		int delimPos = 0;
		while (Character.isDigit(string.charAt(delimPos)))
			delimPos++;
		return XT87Utils.STRING_PREFIX + (Integer.parseInt(string.substring(0, delimPos)) + campaignKeyOffset) + string.substring(delimPos);
	}

	public static W3I.Player clonePlayer(W3I.Player player, int number) {
		W3I.Player result = new W3I.Player();
		result.setNum(number);
		result.setName(player.getName());
		result.setRace(player.getRace());
		result.setType(player.getType());
		result.setAllyHighPrioFlags(player.getAllyHighPrioFlags());
		result.setAllyLowPrioFlags(player.getAllyLowPrioFlags());
		result.setEnemyHighPrioFlags(player.getEnemyHighPrioFlags());
		result.setEnemyLowPrioFlags(player.getEnemyLowPrioFlags());
		result.setStartPos(player.getStartPos());
		result.setStartPosFixed(player.getStartPosFixed());
		return result;
	}

	public static String getLocalizedString(String label)
	{
		try {
			ResourceBundle bundle = ResourceBundle.getBundle("properties/Localization", SplitterLanguage.selectedLanguage.getLocale());
			return bundle.getString(label);
		}
		catch (Throwable ignored)
		{
			return label;
		}
	}

	public static String getUnicodeString(String readable)
	{
		try {
			return new String(readable.getBytes(), StandardCharsets.UTF_8);
		}
		catch (Exception ignored)
		{
			return readable;
		}
	}
}
