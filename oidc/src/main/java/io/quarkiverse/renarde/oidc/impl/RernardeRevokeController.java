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
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.build.Jwt;

@Path("_renarde/security")
public class RernardeRevokeController extends Controller {

    @Inject
    AccessTokenCredential accessToken;

    @RestClient
    RenardeAppleClient renardeAppleClient;

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
    public Response revokeApple() {
        String clientSecret = Jwt.audience("https://appleid.apple.com")
                .subject(appleClientId)
                .issuer(appleOidcIssuer)
                .issuedAt(Instant.now().getEpochSecond())
                .expiresIn(Duration.ofHours(1))
                .jws()
                .keyId(appleOidcKeyId)
                .algorithm(SignatureAlgorithm.ES256)
                .sign(getPrivateKey(String.format("src/main/resources/%s", appleKeyFile)));
        renardeAppleClient.revokeAppleUser(appleClientId, clientSecret, accessToken.getToken(), "access_token");
        return security.makeLogoutResponse();
    }

    private static PrivateKey getPrivateKey(String filename) {
        try {
            String content = Files.readString(Paths.get(filename));
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
