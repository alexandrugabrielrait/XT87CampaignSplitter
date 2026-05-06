package net.xetanth87.campaignsplitter.rewriters;

import net.xetanth87.campaignsplitter.MapInjector;
import net.xetanth87.campaignsplitter.util.XT87Utils;

import static net.xetanth87.campaignsplitter.util.XT87Utils.END_FUNCTION;

public class MinimapRewriter extends ScriptRewriter {
	public final String oldPath;

	public MinimapRewriter(MapInjector mi, String oldPath) {
		super(mi, "MM");
		this.oldPath = oldPath;
	}

	public static String getNewPath(String oldPath) {
		return XT87Utils.PATH_PREFIX + "/" + oldPath;
	}

	@Override
	public void onStartRead(String text, StringBuffer sb) {
	}

	@Override
	public void onReadLine(String line, StringBuffer sb) {
		if ("main".equals(insideFunction))
			if (line.equals(END_FUNCTION))
				append(sb, "call BlzChangeMinimapTerrainTex(\"" + getNewPath(oldPath) + "\")");
		// replaces the path to the original image with its new path
		append(sb, line.replace(oldPath, getNewPath(oldPath)));
	}
}
