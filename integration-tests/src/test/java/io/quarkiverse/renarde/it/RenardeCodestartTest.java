package io.quarkiverse.renarde.it;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language;
import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;

public class RenardeCodestartTest {

    @RegisterExtension
    public static QuarkusCodestartTest codestartTest = QuarkusCodestartTest.builder()
            .languages(Language.JAVA)
            .setupStandaloneExtensionTest("io.quarkiverse.renarde:quarkus-renarde")
            .packageName("")
            .build();

    @Test
    void testContent() throws Throwable {
        codestartTest.checkGeneratedSource("rest.Todos");
        codestartTest.checkGeneratedSource("model.Todo");
        codestartTest.checkGeneratedSource("util.Startup");
        codestartTest.checkGeneratedSource("util.JavaExtensions");
        codestartTest.assertThatGeneratedTreeMatchSnapshots(Language.JAVA, "src/main/resources/templates");
    }

    @Test
    void buildAllProjects() throws Throwable {
        codestartTest.buildAllProjects();
    }
}