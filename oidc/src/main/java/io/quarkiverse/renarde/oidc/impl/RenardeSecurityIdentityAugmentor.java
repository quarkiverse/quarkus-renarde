package io.quarkiverse.renarde.oidc.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.json.JsonNumber;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.eclipse.microprofile.jwt.Claims;
import org.jose4j.jwt.JwtClaims;

import io.quarkus.oidc.UserInfo;
import io.quarkus.oidc.runtime.OidcUtils;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal;
import io.smallrye.mutiny.Uni;

/**
 * GitHub and Facebook don't send a upn so we must extract it from the UserInfo.id
 */
@ApplicationScoped
public class RenardeSecurityIdentityAugmentor implements SecurityIdentityAugmentor {

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        if (identity.isAnonymous())
            return Uni.createFrom().item(identity);
        DefaultJWTCallerPrincipal oldPrincipal = (DefaultJWTCallerPrincipal) identity.getPrincipal();
        String tenantId = identity.getAttribute(OidcUtils.TENANT_ID_ATTRIBUTE);
        // only do this for github and facebook
        if ("github".equals(tenantId)
                || "facebook".equals(tenantId)) {
            String rawToken = oldPrincipal.getClaim(Claims.raw_token);
            JwtClaims claims;
            claims = new JwtClaims();
            for (String claim : oldPrincipal.getClaimNames()) {
                claims.setClaim(claim, oldPrincipal.getClaim(claim));
            }
            UserInfo userInfo = identity.getAttribute(OidcUtils.USER_INFO_ATTRIBUTE);
            JsonValue id = (JsonValue) userInfo.get("id");
            String idString;
            switch (id.getValueType()) {
                case NUMBER:
                    idString = Long.toString(((JsonNumber) id).longValue());
                    break;
                case STRING:
                    idString = ((JsonString) id).getString();
                    break;
                default:
                    // bail out
                    throw new RuntimeException("Don't know how to handle userinfo id: " + id);
            }
            claims.setClaim(Claims.upn.name(), idString);
            QuarkusSecurityIdentity newIdentity = QuarkusSecurityIdentity.builder(identity)
                    .setPrincipal(new DefaultJWTCallerPrincipal(rawToken, "JWT", claims)).build();
            return Uni.createFrom().item(newIdentity);
        } else {
            return Uni.createFrom().item(identity);
        }
    }

}
