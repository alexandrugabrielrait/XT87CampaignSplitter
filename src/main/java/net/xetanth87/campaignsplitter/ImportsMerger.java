package net.xetanth87.campaignsplitter;

import net.moonlightflower.wc3libs.bin.app.IMP;

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

public class ImportsMerger {
	private static List<String> assetList;
	private static final String PORTRAIT_SUFIX = "_portrait";

	static {
		assetList = Arrays.asList(
				XT87Utils.PATH_PREFIX + "\\" + CoopRewriter.SELECTION_INDICATOR_PATH + ".mdx"
		);
		assetList.sort(Comparator.comparing(o -> new File(o).getName()));
	}

	public static List<String> newAssetList() {
		return new ArrayList<>(assetList);
	}

	public static boolean AddLegacyAssets(CampaignSplitter cs, IMP imports) {
		List<String> originalImportList = new ArrayList<>();
		for (IMP.Obj o : imports.getObjs()) {
			originalImportList.add(o.getPath());
		}
		return AddLegacyAssets(cs, imports, originalImportList);
	}

	public static boolean AddLegacyAssets(CampaignSplitter cs, IMP imports, List<String> originalImportList) {
		try {
			List<String> checkList = newAssetList();
			for (String path : originalImportList) {
				if (path.endsWith(".mdl") || path.endsWith(".mdx")) {
					String name = path.substring(0, path.length() - 3);
					checkList.remove(name + ".mdx");
					checkList.remove(name + PORTRAIT_SUFIX + ".mdx");
				} else
					checkList.remove(path);
			}
			for (String assetPath : checkList) {
				IMP.Obj o = new IMP.Obj();
				o.setPath(assetPath);
				imports.addObj(o);
				File importedFile = new File(cs.getImportsPath() + "/" + o.getPath());
				importedFile.getParentFile().mkdirs();
				importedFile.createNewFile();
				InputStream stream = ImportsMerger.class.getResourceAsStream("/imports/" + assetPath.replace("\\", "/"));
				java.nio.file.Files.copy(stream, importedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				stream.close();
			}
			return true;
		} catch (Exception e) {
			System.err.println(e.getMessage());
			return false;
		}
	}

	public static String getNewPath(String oldPath) {
		return XT87Utils.PATH_PREFIX + "/" + oldPath;
	}
}
