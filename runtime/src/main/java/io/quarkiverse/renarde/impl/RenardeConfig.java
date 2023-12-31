package io.quarkiverse.renarde.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import jakarta.enterprise.context.ApplicationScoped;

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
        // let's not add a leading slash because this fails in native-image
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = RenardeConfig.class.getClassLoader();
        }
        try {
            Enumeration<URL> resources = cl.getResources(bundlePath);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                try (Reader reader = new BufferedReader(
                        new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                    bundle.load(reader);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                Properties properties = bundles.get(language);
                if (properties == null || properties.isEmpty()) {
                    bundles.put(language, bundle);
                } else {
                    properties.putAll(bundle);
                    bundles.put(language, bundle);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

}
