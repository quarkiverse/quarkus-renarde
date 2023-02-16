package io.quarkiverse.renarde.impl;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RenardeConfig {

    private String loginPage;
    private Map<String, Properties> bundles = new HashMap<>();

    public String getLoginPage() {
        return loginPage;
    }

    void setLoginPage(String loginPage) {
        this.loginPage = loginPage;
    }

    public Properties getMessageBundle(String language) {
        return bundles.get(language);
    }

    public String getMessage(String language, String key) {
        Properties bundle = getMessageBundle(language);
        return bundle != null ? bundle.getProperty(key) : null;
    }

    void addLanguageBundle(String language, String bundlePath) {
        Properties bundle = new Properties();
        try (Reader reader = Files.newBufferedReader(Path.of(bundlePath), StandardCharsets.UTF_8)) {
            bundle.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        bundles.put(language, bundle);
    }

}
