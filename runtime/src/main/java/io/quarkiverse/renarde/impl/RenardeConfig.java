package io.quarkiverse.renarde.impl;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RenardeConfig {

    private String loginPage;

    public String getLoginPage() {
        return loginPage;
    }

    public void setLoginPage(String loginPage) {
        this.loginPage = loginPage;
    }

}
