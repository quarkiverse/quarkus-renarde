package io.quarkiverse.renarde.oidc.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.security.RenardeSecurity;
import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.security.Authenticated;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.build.Jwt;
import io.vertx.ext.web.RoutingContext;

@Path("_renarde/security")
public class RenardeRevokeController extends Controller {

    @Inject
    AccessTokenCredential accessToken;

    @RestClient
    RenardeAppleClient renardeAppleClient;

    @Inject
    RoutingContext context;

    @ConfigProperty(name = "quarkus.oidc.apple.client-id")
    String appleClientId;

    @ConfigProperty(name = "quarkus.oidc.apple.credentials.jwt.issuer")
    String appleOidcIssuer;

    @ConfigProperty(name = "quarkus.oidc.apple.credentials.jwt.token-key-id")
    String appleOidcKeyId;

    @ConfigProperty(name = "quarkus.oidc.apple.credentials.jwt.key-file", defaultValue = "AuthKey_XXX.p8")
    String appleKeyFile;

    @Inject
    public RenardeSecurity security;

    @Path("apple-revoke")
    @Authenticated
    public Response revokeApple() {
        String tenant = context.get("tenant-id");
        if ("apple".equalsIgnoreCase(tenant)) {
            // Build secret using apple PK
            String clientSecret = Jwt.audience("https://appleid.apple.com")
                    .subject(appleClientId)
                    .issuer(appleOidcIssuer)
                    .issuedAt(Instant.now().getEpochSecond())
                    .expiresIn(Duration.ofHours(1))
                    .jws()
                    .keyId(appleOidcKeyId)
                    .algorithm(SignatureAlgorithm.ES256)
                    .sign(getPrivateKey(appleKeyFile));
            // Invalid refresh token
            if (null != accessToken.getRefreshToken()) {
                renardeAppleClient.revokeAppleUser(appleClientId, clientSecret, accessToken.getRefreshToken().getToken(),
                        "refresh_token");
            }
            // Invalid access token
            renardeAppleClient.revokeAppleUser(appleClientId, clientSecret, accessToken.getToken(), "access_token");
        }
        // Invalid cookies
        return security.makeLogoutResponse();
    }

    private PrivateKey getPrivateKey(String filename) {
        try {
            String content = Files.readString(Paths.get("target/classes/" + filename));
            String privateKey = content.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            KeyFactory kf = KeyFactory.getInstance("EC");
            return kf.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKey)));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
            throw new RuntimeException(e);
        }
    }

}
