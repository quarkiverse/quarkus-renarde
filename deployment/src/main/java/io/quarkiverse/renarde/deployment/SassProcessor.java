package io.quarkiverse.renarde.deployment;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import de.larsgrefer.sass.embedded.SassCompilationFailedException;
import de.larsgrefer.sass.embedded.SassCompiler;
import de.larsgrefer.sass.embedded.SassCompilerFactory;
import de.larsgrefer.sass.embedded.importer.CustomImporter;
import io.quarkiverse.renarde.devmode.SassDevModeHandler;
// import io.bit3.jsass.CompilationException;
// import io.bit3.jsass.Compiler;
// import io.bit3.jsass.Options;
// import io.bit3.jsass.Output;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.vertx.http.deployment.spi.AdditionalStaticResourceBuildItem;
import io.quarkus.vertx.http.runtime.StaticResourcesRecorder;
import sass.embedded_protocol.EmbeddedSass.InboundMessage.ImportResponse.ImportSuccess;
import sass.embedded_protocol.EmbeddedSass.OutboundMessage.CompileResponse.CompileSuccess;

public class SassProcessor {
    @BuildStep
    public void processSassFiles(BuildProducer<GeneratedResourceBuildItem> resources,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<AdditionalStaticResourceBuildItem> staticResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> watchedFiles,
            OutputTargetBuildItem outputTargetBuildItem,
            ApplicationArchivesBuildItem archives,
            CurateOutcomeBuildItem curateOutcomeBuildItem) {
        archives.getRootArchive().accept(tree -> {
            tree.walk(pv -> {
                String relativePath = pv.getRelativePath("/");
                if (relativePath.startsWith("META-INF/resources/")
                        && pv.getPath().getFileName().toString().toLowerCase().endsWith(".scss")
                        && !pv.getPath().getFileName().toString().startsWith("_")) {
                    watchedFiles.produce(new HotDeploymentWatchedFileBuildItem(relativePath, false));
                    System.err.println("path visit: " + relativePath + " " + pv.getPath());
                    String result = convertScss(pv.getPath());
                    // scss files depend on themselves
                    SassDevModeHandler.addDependency(relativePath, relativePath);
                    String generatedFile = relativePath.substring(0, relativePath.length() - 5) + ".css";
                    System.err.println("Result in " + generatedFile + ": " + result);
                    byte[] bytes = result.getBytes(StandardCharsets.UTF_8);
                    resources.produce(new GeneratedResourceBuildItem(generatedFile,
                            bytes, true));
                    nativeImageResources.produce(new NativeImageResourceBuildItem(generatedFile));
                    String additionalPath = generatedFile.substring(StaticResourcesRecorder.META_INF_RESOURCES.length());
                    staticResources.produce(new AdditionalStaticResourceBuildItem(additionalPath, false));

                    File buildDir = RenardeProcessor.getBuildDirectory(curateOutcomeBuildItem);
                    Path targetPath = buildDir.toPath().resolve(generatedFile);
                    try {
                        Files.createDirectories(targetPath.getParent());
                        Files.write(targetPath, bytes);
                        System.err.println("Wrote to " + targetPath + " add path: " + additionalPath);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            });
        });
    }

    private String convertScss(Path path) {
        try (SassCompiler sassCompiler = SassCompilerFactory.bundled()) {
            Path parent = path.getParent();
            sassCompiler.registerImporter(new CustomImporter() {

                @Override
                public String canonicalize(String url, boolean fromImport) throws Exception {
                    System.err.println("canonicalize " + url + " fromImport: " + fromImport);
                    // add extension if missing
                    if (!url.toLowerCase().endsWith(".scss")) {
                        url += ".scss";
                    }
                    Path resolved = parent.resolve(url);
                    // prefix with _ for partials
                    if (!resolved.getFileName().toString().startsWith("_")) {
                        resolved = resolved.getParent().resolve("_" + resolved.getFileName().toString());
                    }
                    return "sass:" + resolved;
                }

                @Override
                public ImportSuccess handleImport(String url) throws Exception {
                    System.err.println("handleImport: " + url);
                    if (url.startsWith("sass:")) {
                        String path = url.substring(5);
                        String contents = Files.readString(Path.of(path), StandardCharsets.UTF_8);
                        return ImportSuccess.newBuilder().setContents(contents).buildPartial();
                    }
                    return null;
                }
            });
            String contents = Files.readString(path, StandardCharsets.UTF_8);
            CompileSuccess compileSuccess = sassCompiler.compileScssString(contents);

            //get compiled css
            return compileSuccess.getCss();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (SassCompilationFailedException e) {
            // FIXME: provide better error reporting mechanism to display on front page in dev mode
            throw new RuntimeException(e);
        }
    }
    //    private String convertScss(Path path) {
    //        try {
    //            String contents = Files.readString(path, StandardCharsets.UTF_8);
    //            Compiler compiler = new Compiler();
    //            Options options = new Options();
    //            Output result = compiler.compileString(contents, options);
    //            return result.getCss();
    //        } catch (IOException e) {
    //            throw new UncheckedIOException(e);
    //        } catch (CompilationException e) {
    //            // FIXME: provide better error reporting mechanism to display on front page in dev mode
    //            throw new RuntimeException(e);
    //        }
    //    }
}
