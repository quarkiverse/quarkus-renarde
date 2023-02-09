package io.quarkiverse.renarde.oidc;

import java.util.List;

import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkiverse.renarde.oidc.impl.RenardeGithubClient;
import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.IdToken;
import io.quarkus.oidc.OidcSession;
import io.quarkus.oidc.UserInfo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RenardeOidcSecurity {

    @RestClient
    RenardeGithubClient client;

    @Inject
    AccessTokenCredential accessToken;

    @Inject
    @IdToken
    JsonWebToken idToken;

    @Inject
    OidcSession oidcSession;

    @Inject
    UserInfo userInfo;

    public String getOidcEmail() {
        String tenantId = oidcSession.getTenantId();
        switch (tenantId) {
            case "github":
                List<RenardeGithubClient.Email> emails = client.getEmails("Bearer " + accessToken.getToken());
                for (RenardeGithubClient.Email emailStruct : emails) {
                    if (emailStruct.primary) {
                        return emailStruct.email;
                    }
                }
                return null;
            case "twitter":
                return null;
            default:
                return idToken.getClaim(Claims.email);
        }
    }

    public String getOidcFirstName() {
        String tenantId = oidcSession.getTenantId();
        switch (tenantId) {
            case "github":
                return firstPart(userInfo.getString("name"));
            case "twitter":
                return firstPart(userInfo.getObject("data").getString("name"));
            case "microsoft":
                return firstPart(idToken.getClaim("name"));
            default:
                return idToken.getClaim(Claims.given_name);
        }
    }

    private String firstPart(String name) {
        if (name == null)
            return null;
        int firstSpace = name.indexOf(' ');
        if (firstSpace != -1) {
            return name.substring(0, firstSpace);
        } else {
            return name;
        }
    }

    private String secondPart(String name) {
        if (name == null)
            return null;
        int firstSpace = name.indexOf(' ');
        if (firstSpace != -1) {
            return name.substring(firstSpace + 1);
        } else {
            return null;
        }
    }

    public String getOidcLastName() {
        String tenantId = oidcSession.getTenantId();
        switch (tenantId) {
            case "github":
                return secondPart(userInfo.getString("name"));
            case "twitter":
                return secondPart(userInfo.getObject("data").getString("name"));
            case "microsoft":
                return secondPart(idToken.getClaim("name"));
            default:
                return idToken.getClaim(Claims.family_name);
        }
    }

    public String getOidcUserName() {
        String tenantId = oidcSession.getTenantId();
        switch (tenantId) {
            case "github":
                return userInfo.getString("login");
            case "twitter":
                return firstPart(userInfo.getObject("data").getString("username"));
            case "facebook":
                return null;
            default:
                return idToken.getClaim(Claims.preferred_username);
        }
    }
}
