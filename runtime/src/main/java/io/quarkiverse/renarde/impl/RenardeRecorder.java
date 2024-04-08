package io.quarkiverse.renarde.impl;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class RenardeRecorder {

    public void configureLoginPage(BeanContainer beanContainer, String uri) {
        RenardeConfigBean config = beanContainer.beanInstance(RenardeConfigBean.class);
        config.setLoginPage(uri);
    }

    public void addLanguageBundle(BeanContainer beanContainer, String language, String bundlePath) {
        RenardeConfigBean config = beanContainer.beanInstance(RenardeConfigBean.class);
        config.addLanguageBundle(language, bundlePath);
    }
}
