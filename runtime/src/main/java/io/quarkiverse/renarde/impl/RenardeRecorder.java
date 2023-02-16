package io.quarkiverse.renarde.impl;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class RenardeRecorder {

    public void configureLoginPage(BeanContainer beanContainer, String uri) {
        RenardeConfig config = beanContainer.beanInstance(RenardeConfig.class);
        config.setLoginPage(uri);
    }

    public void addLanguageBundle(BeanContainer beanContainer, String language, String bundlePath) {
        RenardeConfig config = beanContainer.beanInstance(RenardeConfig.class);
        config.addLanguageBundle(language, bundlePath);
    }
}
