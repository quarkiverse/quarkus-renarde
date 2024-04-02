package io.quarkiverse.renarde.oidc.test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import org.eclipse.microprofile.jwt.Claims;

import io.smallrye.jwt.build.Jwt;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.RoutingContext;
import io.vertx.mutiny.ext.web.handler.BodyHandler;

public class MockAppleOidcTestResource extends MockOidcTestResource<MockAppleOidc> {

    private KeyPair kp;

    public MockAppleOidcTestResource() {
        super("apple");
    }

    @Override
    protected void registerRoutes(Router router) {
        BodyHandler bodyHandler = BodyHandler.create();
        router.get("/.well-known/openid-configuration").handler(this::configuration);
        router.get("/auth/authorize").handler(this::authorize);
        router.post("/auth/token").handler(bodyHandler).handler(this::accessTokenJson);
        router.get("/auth/keys").handler(this::getKeys);
        router.post("/auth/revoke").handler(this::revoke);

        KeyPairGenerator kpg;
        try {
            kpg = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        kpg.initialize(2048);
        kp = kpg.generateKeyPair();

        // FIXME: folder
        File privateKey = new File("target/classes/test.oidc-apple-key.pem");
        if (privateKey.exists())
            privateKey.delete();
        KeyPairGenerator ecKpg;
        try {
            // Apple hands out EC keys
            ecKpg = KeyPairGenerator.getInstance("EC");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        ecKpg.initialize(256);
        KeyPair kp = ecKpg.generateKeyPair();

        try (FileWriter fw = new FileWriter(privateKey)) {
            fw.append("-----BEGIN PRIVATE KEY-----\n");
            fw.append(Base64.getMimeEncoder().encodeToString(kp.getPrivate().getEncoded()));
            fw.append("\n");
            fw.append("-----END PRIVATE KEY-----\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, String> start() {
        Map<String, String> ret = super.start();
        ret.put("quarkus.oidc.apple.credentials.jwt.key-file", "test.oidc-apple-key.pem");
        ret.put("quarkus.rest-client.RenardeAppleClient.url", baseURI);
        return ret;
    }

    private void configuration(RoutingContext rc) {
        String data = "{\n"
                + " \"issuer\": \"https://appleid.apple.com\",\n"
                + " \"authorization_endpoint\": \"" + baseURI + "/auth/authorize\",\n"
                + " \"token_endpoint\": \"" + baseURI + "/auth/token\",\n"
                + " \"jwks_uri\": \"" + baseURI + "/auth/keys\",\n"
                + " \"response_types_supported\": [\n"
                + "  \"code\"\n"
                + " ],\n"
                + " \"response_modes_supported\": [\n"
                + "  \"query\",\n"
                + "  \"fragment\",\n"
                + "  \"form_post\"\n"
                + " ],\n"
                + "  \"subject_types_supported\": [\n"
                + "  \"pairwise\"\n"
                + " ],\n"
                + " \"id_token_signing_alg_values_supported\": [\n"
                + "  \"RS256\"\n"
                + " ],\n"
                + " \"scopes_supported\": [\n"
                + "  \"openid\",\n"
                + "  \"email\",\n"
                + "  \"name\"\n"
                + " ],\n"
                + " \"token_endpoint_auth_methods_supported\": [\n"
                + "  \"client_secret_post\"\n"
                + " ],\n"
                + " \"claims_supported\": [\n"
                + "  \"aud\",\n"
                + "  \"email\",\n"
                + "  \"email_verified\",\n"
                + "  \"exp\",\n"
                + "  \"iat\",\n"
                + "  \"iss\",\n"
                + "  \"sub\"\n"
                + " ]\n"
                + "}";
        rc.response().putHeader("Content-Type", "application/json");
        rc.endAndForget(data);
    }

    /*
     * First request:
     * GET
     * https://appleid.apple.com/auth/authorize?response_type=code&client_id=CLIENT&scope=openid+openid+email+name&redirect_uri=
     * https://ab7d-81-185-173-5.ngrok.io/Login/oidcLoginSuccess&state=STATE&response_mode=form_post
     *
     * causes a POST but from the client, so let's return that to the client and make-pretend in the test
     * state: STATE
     * code: CODE
     */
    private void authorize(RoutingContext rc) {
        String response_type = rc.request().params().get("response_type");
        String client_id = rc.request().params().get("client_id");
        String scope = rc.request().params().get("scope");
        String state = rc.request().params().get("state");
        String redirect_uri = rc.request().params().get("redirect_uri");
        // make sure we'ret getting HTTPS (required by apple)
        if (!redirect_uri.startsWith("https://")) {
            rc.response().setStatusCode(400).sendAndForget("HTTPS is required");
            return;
        }
        UUID code = UUID.randomUUID();
        rc.response()
                .putHeader("Content-Type", "application/json")
                .endAndForget("{\n"
                        + "  \"code\":\"" + code + "\",\n"
                        + "  \"state\":\"" + state + "\"\n"
                        + "}");
    }

    /*
     * OIDC calls POST /auth/token
     * POST /auth/token
     *
     * grant_type=authorization_code
     * &code=CODE
     * &redirect_uri=https%3A%2F%2Fab7d-81-185-173-5.ngrok.io%2FLogin%2FoidcLoginSuccess
     * &client_id=CLIENT
     * &client_secret=GENERATED_JWT
     *
     *
     * {
     * "access_token":"TOKEN",
     * "token_type":"Bearer",
     * "expires_in":3600,
     * "refresh_token":"TOKEN2",
     * "id_token":"JWT"
     * }
     *
     * {
     * "iss": "https://appleid.apple.com",
     * "aud": "CLIENT",
     * "exp": 1641996866,
     * "iat": 1641910466,
     * "sub": "USERID",
     * "at_hash": "HASHED_TOKEN",
     * "email": "apple@example.com",
     * "email_verified": "true",
     * "auth_time": 1641910465,
     * "nonce_supported": true
     * }
     */
    private void accessTokenJson(RoutingContext rc) {
        String authorization_code = rc.request().formAttributes().get("authorization_code");
        String code = rc.request().formAttributes().get("code");
        String redirect_uri = rc.request().formAttributes().get("redirect_uri");

        UUID token = UUID.randomUUID();
        UUID token2 = UUID.randomUUID();
        String hashedToken = hashAccessToken(token.toString());
        String idToken = Jwt.issuer("https://appleid.apple.com")
                .audience("APLCLIENT")
                .expiresIn(Duration.ofDays(1))
                .issuedAt(Instant.now())
                .subject("USERID")
                .claim(Claims.at_hash, hashedToken)
                .claim(Claims.email, "apple@example.com")
                .claim(Claims.email_verified, true)
                .claim(Claims.auth_time, Instant.now())
                .claim("nonce_supported", true)
                .jws()
                .keyId("KEYID")
                .sign(kp.getPrivate());

        String data = "{\n"
                + " \"token_type\":\"Bearer\",\n"
                + " \"expires_in\":3600,\n"
                + " \"access_token\":\"" + token + "\",\n"
                + " \"refresh_token\":\"" + token2 + "\",\n"
                + " \"id_token\":\"" + idToken + "\"\n"
                + " }  ";
        rc.response()
                .putHeader("Content-Type", "application/json")
                .endAndForget(data);
    }

    /*
     * {
     * "kty": "RSA",
     * "kid": "eXaunmL",
     * "use": "sig",
     * "alg": "RS256",
     * "n":
     * "4dGQ7bQK8LgILOdLsYzfZjkEAoQeVC_aqyc8GC6RX7dq_KvRAQAWPvkam8VQv4GK5T4ogklEKEvj5ISBamdDNq1n52TpxQwI2EqxSk7I9fKPKhRt4F8-2yETlYvye-2s6NeWJim0KBtOVrk0gWvEDgd6WOqJl_yt5WBISvILNyVg1qAAM8JeX6dRPosahRVDjA52G2X-Tip84wqwyRpUlq2ybzcLh3zyhCitBOebiRWDQfG26EH9lTlJhll-p_Dg8vAXxJLIJ4SNLcqgFeZe4OfHLgdzMvxXZJnPp_VgmkcpUdRotazKZumj6dBPcXI_XID4Z4Z3OM1KrZPJNdUhxw",
     * "e": "AQAB"
     * },
     */
    private void getKeys(RoutingContext rc) {
        RSAPublicKey pub = (RSAPublicKey) kp.getPublic();
        String modulus = Base64.getUrlEncoder().encodeToString(pub.getModulus().toByteArray());
        String exponent = Base64.getUrlEncoder().encodeToString(pub.getPublicExponent().toByteArray());
        String data = "{\n"
                + "  \"keys\": [\n"
                + "    {\n"
                + "      \"kty\": \"RSA\",\n"
                + "      \"kid\": \"KEYID\",\n"
                + "      \"use\": \"sig\",\n"
                + "      \"alg\": \"RS256\",\n"
                + "      \"n\": \"" + modulus + "\",\n"
                + "      \"e\": \"" + exponent + "\"\n"
                + "    },\n"
                + "  ]\n"
                + "}";
        rc.response()
                .putHeader("Content-Type", "application/json")
                .endAndForget(data);
    }

    /**
     * POST /auth/revoke
     * Host: appleid.apple.com
     * Content-Type: application/x-www-form-urlencoded
     *
     * client_id=$1
     * &client_secret=$2
     * &token=$3
     * &token_type_hint=access_token
     *
     * https://developer.apple.com/documentation/sign_in_with_apple/revoke_tokens/
     */
    private void revoke(RoutingContext rc) {
        rc.response().setStatusCode(200).endAndForget();
    }
}
