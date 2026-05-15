package net.xetanth87.campaignsplitter;

import net.moonlightflower.wc3libs.bin.app.IMP;
import net.xetanth87.campaignsplitter.rewriters.CoopRewriter;
import net.xetanth87.campaignsplitter.util.XT87Utils;

import java.io.File;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ImportsMerger {
	private static final String PORTRAIT_SUFFIX = "_portrait";
	private static final String HD_PREFIX = "_hd.w3mod\\";

	public static List<String> newArchonArrowsAssetList() {
		return Stream.of(
				XT87Utils.PATH_PREFIX + "\\" + CoopRewriter.SELECTION_INDICATOR_PATH + ".mdx"
		).sorted().collect(Collectors.toList());
	}

	private static List<String> newLegacyAssetList(boolean isHd) {
		Stream<String> stream = Stream.of(
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
				"units\\nightelf\\furion\\furion.mdx",
				"units\\nightelf\\furion\\furion_portrait.mdx",
				"units\\nightelf\\malfurion\\malfurionnostag.mdx",
				"units\\nightelf\\malfurion\\malfurionnostag_portrait.mdx",
				"units\\critters\\chaosspaceorc\\chaosspaceorc.mdx"
		);
		if (isHd)
			stream = stream.map(x -> HD_PREFIX + (x.endsWith(".blp") ? x.substring(0, x.length() - 3) + "dds" : x));
		return stream.sorted().collect(Collectors.toList());
	}

	public enum AssetListType {
		ARCHON_ARROWS("aa", true),
		SD("sd", true),
		HD("hd", true);

		public final String prefix;
		public final boolean justName;

		AssetListType(String prefix, boolean justName) {
			this.prefix = prefix;
			this.justName = justName;
		}

		List<String> newAssetList() {
			return switch (this) {
				case ARCHON_ARROWS -> newArchonArrowsAssetList();
				case SD -> newLegacyAssetList(false);
				case HD -> newLegacyAssetList(true);
			};
		}
	}

	private static boolean AddAssets(AssetListType listType, CampaignSplitter cs, IMP imports, List<String> originalImportList) {
		String prefix = "/imports/";
		if (listType.prefix != null)
			prefix += listType.prefix + "/";
		try {
			List<String> checkList = listType.newAssetList();
			for (String path : originalImportList) {
				if (path.endsWith(".mdl") || path.endsWith(".mdx")) {
					String name = path.substring(0, path.length() - 3);
					checkList.remove(name + ".mdx");
					checkList.remove(name + PORTRAIT_SUFFIX + ".mdx");
				} else
					checkList.remove(path);
			}
			for (String importPath : checkList) {
				IMP.Obj o = new IMP.Obj();
				o.setPath(importPath);
				imports.addObj(o);
				File importedFile = new File(cs.getImportsPath() + "/" + o.getPath());
				importedFile.getParentFile().mkdirs();
				importedFile.createNewFile();
				String assetPath = importPath.replace("\\", "/");
				if (listType.justName)
					assetPath = new File(assetPath).getName();
				InputStream stream = ImportsMerger.class.getResourceAsStream(prefix + assetPath);
				if (stream != null) {
					java.nio.file.Files.copy(stream, importedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
					stream.close();
				}
			}
			return true;
		} catch (Exception e) {
			System.err.println(e.getMessage());
			return false;
		}
	}

	public static boolean AddAssets(AssetListType listType, CampaignSplitter cs, IMP imports) {
		List<String> originalImportList = new ArrayList<>();
		for (IMP.Obj o : imports.getObjs())
			originalImportList.add(o.getPath());
		return AddAssets(listType, cs, imports, originalImportList);
	}

	public static String getNewPath(String oldPath) {
		return XT87Utils.PATH_PREFIX + "/" + oldPath;
	}
}
