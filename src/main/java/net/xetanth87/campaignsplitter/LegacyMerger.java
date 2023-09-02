package net.xetanth87.campaignsplitter;

import net.moonlightflower.wc3libs.bin.app.IMP;
import net.xetanth87.campaignsplitter.rewriters.CoopRewriter;
import net.xetanth87.campaignsplitter.util.XT87Utils;

import java.io.File;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class LegacyMerger {
	private static List<String> assetList;
	private static List<String> hdAssetList;
	private static final String PORTRAIT_SUFIX = "_portrait";
	private static final String HD_PREFIX = "_hd.w3mod\\";
	private static final String SD_ARCHIVE_NAME = "legacy.zip";
	private static final String HD_ARCHIVE_NAME = "legacyHD.zip";

	static {
		assetList = Arrays.asList(
				"replaceabletextures\\commandbuttons\\btnsteamtank.blp",
				"replaceabletextures\\commandbuttonsdisabled\\disbtnsteamtank.blp",
				"replaceabletextures\\commandbuttons\\btncatapult.blp",
				"replaceabletextures\\commandbuttonsdisabled\\disbtncatapult.blp",
				"replaceabletextures\\commandbuttons\\btnballista.blp",
				"replaceabletextures\\commandbuttonsdisabled\\disbtnballista.blp",
				"replaceabletextures\\commandbuttons\\btnchaosspaceorc.blp",
				"replaceabletextures\\commandbuttonsdisabled\\disbtnchaosspaceorc.blp",
				"units\\human\\warwagon\\warwagon.mdx",
				"units\\human\\warwagon\\warwagon_portrait.mdx",
				"units\\human\\gyrocopter\\gyrocopter.mdx",
				"units\\human\\gyrocopter\\gyrocopter_portrait.mdx",
				"units\\orc\\catapult\\catapult.mdx",
				"units\\orc\\catapult\\catapult_portrait.mdx",
				"units\\nightelf\\ballista\\ballista.mdx",
				"units\\nightelf\\ballista\\ballista_portrait.mdx",
				"units\\critters\\chaosspaceorc\\chaosspaceorc.mdx",
				XT87Utils.PATH_PREFIX + "\\" + CoopRewriter.SELECTION_INDICATOR_PATH + ".mdx"
		);
		hdAssetList = new ArrayList<>();
		for (String path : assetList) {
			if (path.endsWith(".blp"))
				path = path.substring(0, path.length() - 3) + "dds";
			hdAssetList.add(path);
		}
		assetList.sort(Comparator.comparing(o -> new File(o).getName()));
	}

	public static List<String> newAssetList(boolean isHd) {
		return new ArrayList<>(isHd ? hdAssetList : assetList);
	}

	public static boolean AddLegacyAssets(CampaignSplitter cs, IMP imports) {
		List<String> originalImportList = new ArrayList<>();
		for (IMP.Obj o : imports.getObjs()) {
			originalImportList.add(o.getPath());
		}
		boolean sdValue = false, hdValue = false;
		if (cs.withLegacyAssetsSD)
			sdValue = AddLegacyAssets(cs, imports, originalImportList, false);
		if (cs.withLegacyAssetsHD)
			hdValue = AddLegacyAssets(cs, imports, originalImportList, true);
		return sdValue || hdValue;
	}

	public static boolean AddLegacyAssets(CampaignSplitter cs, IMP imports, List<String> originalImportList, boolean isHd) {
		ZipFile zipFile = null;
		try {
			CodeSource codeSource = CampaignSplitter.class.getProtectionDomain().getCodeSource();
			File jarFile = new File(codeSource.getLocation().toURI().getPath());
			String zipDirPath = jarFile.getParentFile().getPath() + "/" + (isHd ? HD_ARCHIVE_NAME : SD_ARCHIVE_NAME);
			try {
				zipFile = new ZipFile(zipDirPath);
			} catch (Exception e) {
				zipFile = null;
			}
			if (zipFile == null) {
				System.out.println((isHd ? "HD" : "SD") + " Legacy Assets archive was not found at expected path: " + zipDirPath);
				return false;
			}
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			List<String> checkList = newAssetList(isHd);
			for (String path : originalImportList) {
				if (path.startsWith(HD_PREFIX))
					path = path.substring(HD_PREFIX.length());
				if (path.endsWith(".mdl") || path.endsWith(".mdx")) {
					String name = path.substring(0, path.length() - 3);
					checkList.remove(name + ".mdx");
					checkList.remove(name + PORTRAIT_SUFIX + ".mdx");
				} else
					checkList.remove(path);
			}
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				String entryName = entry.getName();
				for (String assetPath : checkList) {
					String realAssetPath = isHd ? HD_PREFIX + assetPath : assetPath;
					if (new File(realAssetPath).getName().equals(entryName)) {
						checkList.remove(assetPath);
						IMP.Obj o = new IMP.Obj();
						o.setPath(realAssetPath);
						imports.addObj(o);
						File importedFile = new File(cs.getImportsPath() + "/" + o.getPath());
						importedFile.getParentFile().mkdirs();
						importedFile.createNewFile();
						InputStream stream = zipFile.getInputStream(entry);
						java.nio.file.Files.copy(stream, importedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
						stream.close();
						break;
					}
				}
			}
//			for (String assetPath : checkList) {
//				System.err.println("Legacy" + (isHd ? "HD" : "") + " asset not found in legacy" + (isHd ? "HD" : "") + " archive: " + new File(assetPath).getName());
//			}
			zipFile.close();
			return true;
		} catch (Exception e) {
			System.err.println(e.getMessage());
			if (zipFile != null) {
				try {
					zipFile.close();
				} catch (Exception ex) {
				}
			}
			return false;
		}
	}

	public static String getNewPath(String oldPath) {
		return XT87Utils.PATH_PREFIX + "/" + oldPath;
	}
}
