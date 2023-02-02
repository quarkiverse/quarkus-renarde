package io.quarkiverse.renarde.devmode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.dev.spi.HotReplacementContext;
import io.quarkus.dev.spi.HotReplacementSetup;

public class SassDevModeHandler implements HotReplacementSetup {

	// source file -> depends on source files
	private final static Map<String, List<String>> reverseDependencies = new HashMap<>();
	
	public static void clear() {
		reverseDependencies.clear();
	}
	
	public static void addDependency(String source, String affectedFile) {
		System.err.println("source file "+source+" will affect "+affectedFile);
		List<String> sources = reverseDependencies.get(affectedFile);
		if(sources == null) {
			sources = new ArrayList<>();
			reverseDependencies.put(affectedFile, sources);
		}
		sources.add(source);
	}
	
    @Override
    public void setupHotDeployment(HotReplacementContext context) {
        System.err.println("Setting up SASS hot deploy classes: "+context.getClassesDir()+" resources dirs: "+context.getResourcesDir());
        context.consumeNoRestartChanges(this::noRestartChanges);
    }

    public void noRestartChanges(Set<String> changes) {
        System.err.println("No restart changes: " + changes);
        Set<String> needRebuild = new HashSet<>();
        for (String change : changes) {
			List<String> affectedFiles = reverseDependencies.get(change);
			if(affectedFiles != null) {
				needRebuild.addAll(affectedFiles);
			}
		}
        System.err.println("Need to rebuild: " + needRebuild);
    }
}
