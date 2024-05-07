package io.quarkiverse.renarde.oidc.test;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.eclipse.microprofile.jwt.Claims;

import io.smallrye.jwt.build.Jwt;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.RoutingContext;
import io.vertx.mutiny.ext.web.handler.BodyHandler;

public class MockGoogleOidcTestResource extends MockOidcTestResource<MockGoogleOidc> {

    private KeyPair kp;

    public MockGoogleOidcTestResource() {
        super("google");
    }

    @Override
    protected void registerRoutes(Router router) {
        BodyHandler bodyHandler = BodyHandler.create();
        router.get("/.well-known/openid-configuration").handler(this::configuration);
        router.get("/o/oauth2/v2/auth").handler(this::authorize);
        router.post("/token").handler(bodyHandler).handler(this::accessTokenJson);
        router.get("/oauth2/v3/certs").handler(this::getKeys);
        router.get("/v1/userinfo").handler(this::userinfo);

        KeyPairGenerator kpg;
        try {
            kpg = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        kpg.initialize(2048);
        kp = kpg.generateKeyPair();
    }

    private void configuration(RoutingContext rc) {
        String data = "{\n"
                + " \"issuer\": \"https://accounts.google.com\",\n"
                + " \"authorization_endpoint\": \"" + baseURI + "/o/oauth2/v2/auth\",\n"
                + " \"device_authorization_endpoint\": \"" + baseURI + "/device/code\",\n"
                + " \"token_endpoint\": \"" + baseURI + "/token\",\n"
                + " \"userinfo_endpoint\": \"" + baseURI + "/v1/userinfo\",\n"
                + " \"revocation_endpoint\": \"" + baseURI + "/revoke\",\n"
                + " \"jwks_uri\": \"" + baseURI + "/oauth2/v3/certs\",\n"
                + " \"response_types_supported\": [\n"
                + "  \"code\",\n"
                + "  \"token\",\n"
                + "  \"id_token\",\n"
                + "  \"code token\",\n"
                + "  \"code id_token\",\n"
                + "  \"token id_token\",\n"
                + "  \"code token id_token\",\n"
                + "  \"none\"\n"
                + " ],\n"
                + " \"subject_types_supported\": [\n"
                + "  \"public\"\n"
                + " ],\n"
                + " \"id_token_signing_alg_values_supported\": [\n"
                + "  \"RS256\"\n"
                + " ],\n"
                + " \"scopes_supported\": [\n"
                + "  \"openid\",\n"
                + "  \"email\",\n"
                + "  \"profile\"\n"
                + " ],\n"
                + " \"token_endpoint_auth_methods_supported\": [\n"
                + "  \"client_secret_post\",\n"
                + "  \"client_secret_basic\"\n"
                + " ],\n"
                + " \"claims_supported\": [\n"
                + "  \"aud\",\n"
                + "  \"email\",\n"
                + "  \"email_verified\",\n"
                + "  \"exp\",\n"
                + "  \"family_name\",\n"
                + "  \"given_name\",\n"
                + "  \"iat\",\n"
                + "  \"iss\",\n"
                + "  \"locale\",\n"
                + "  \"name\",\n"
                + "  \"picture\",\n"
                + "  \"sub\"\n"
                + " ],\n"
                + " \"code_challenge_methods_supported\": [\n"
                + "  \"plain\",\n"
                + "  \"S256\"\n"
                + " ],\n"
                + " \"grant_types_supported\": [\n"
                + "  \"authorization_code\",\n"
                + "  \"refresh_token\",\n"
                + "  \"urn:ietf:params:oauth:grant-type:device_code\",\n"
                + "  \"urn:ietf:params:oauth:grant-type:jwt-bearer\"\n"
                + " ]\n"
                + "}";
        rc.response().putHeader("Content-Type", "application/json");
        rc.endAndForget(data);
    }

    /*
     * First request:
     * GET https://accounts.google.com/o/oauth2/v2/auth?response_type=code&client_id=SECRET&scope=openid+openid+email+profile&
     * redirect_uri=http%3A%2F%2Flocalhost%3A8080%2FLogin%2FoidcLoginSuccess&state=STATE
     *
     * returns a 302 to
     * GET
     * http://localhost:8080/Login/oidcLoginSuccess?state=STATE&code=CODE&scope=email+profile+openid+https://www.googleapis.com/
     * auth/userinfo.email+https://www.googleapis.com/auth/userinfo.profile&authuser=0&prompt=consent
     */
    private void authorize(RoutingContext rc) {
        String response_type = rc.request().params().get("response_type");
        String client_id = rc.request().params().get("client_id");
        String scope = rc.request().params().get("scope");
        String state = rc.request().params().get("state");
        String redirect_uri = rc.request().params().get("redirect_uri");
        UUID code = UUID.randomUUID();
        URI redirect;
        try {
            redirect = new URI(redirect_uri + "?state=" + state + "&code=" + code
                    + "&scope=email+profile+openid+https://www.googleapis.com/auth/userinfo.email+https://www.googleapis.com/auth/userinfo.profile&authuser=0&prompt=consent");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        rc.response()
                .putHeader("Location", redirect.toASCIIString())
                .setStatusCode(302)
                .endAndForget();
    }

    /*
     * OIDC calls POST /token
     * grant_type=authorization_code&code=CODE&redirect_uri=http%3A%2F%2Flocalhost%3A8080%2FLogin%2FoidcLoginSuccess
     * returns:
     * {
     * "access_token": "..token..",
     * "expires_in": 3599,
     * "scope": "https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email openid",
     * "token_type": "Bearer",
     * "id_token": "...JWT..."
     * }
     *
     * ID token:
     * {
     * "iss": "https://accounts.google.com",
     * "azp": "SOMETHING",
     * "aud": "SOMETHING",
     * "sub": "USERID",
     * "email": "google@example.com",
     * "email_verified": true,
     * "at_hash": "AT_HASH",
     * "name": "Foo Bar",
     * "picture": "https://example.com/picture",
     * "given_name": "Foo",
     * "family_name": "Bar",
     * "locale": "en-GB",
     * "iat": 1641566612,
     * "exp": 1641570212
     * }
     */
    private void accessTokenJson(RoutingContext rc) {
        String authorization_code = rc.request().formAttributes().get("authorization_code");
        String code = rc.request().formAttributes().get("code");
        String redirect_uri = rc.request().formAttributes().get("redirect_uri");

        UUID token = UUID.randomUUID();
        String hashedToken = hashAccessToken(token.toString());
        String idToken = Jwt.issuer("https://accounts.google.com")
                .audience("GGLCLIENT")
                .claim(Claims.azp, "SOMETHING")
                .subject("USERID")
                .claim(Claims.email, "google@example.com")
                .claim(Claims.email_verified, true)
                .claim(Claims.at_hash, hashedToken)
                .claim("name", "Foo Bar")
                .claim("picture", "https://example.com/picture")
                .claim(Claims.given_name, "Foo")
                .claim(Claims.family_name, "Bar")
                .claim(Claims.locale, "en-GB")
                .issuedAt(Instant.now())
                .expiresIn(Duration.ofDays(1))
                .jws()
                .keyId("KEYID")
                .sign(kp.getPrivate());

        String data = "{\n"
                + "  \"access_token\": \"" + token + "\",\n"
                + "  \"expires_in\": 3599,\n"
                + "  \"scope\": \"https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email openid\",\n"
                + "  \"token_type\": \"Bearer\",\n"
                + "  \"id_token\": \"" + idToken + "\"\n"
                + "}\n";
        rc.response()
                .putHeader("Content-Type", "application/json")
                .endAndForget(data);
    }

    private void getKeys(RoutingContext rc) {
        RSAPublicKey pub = (RSAPublicKey) kp.getPublic();
        String modulus = Base64.getUrlEncoder().encodeToString(pub.getModulus().toByteArray());
        String exponent = Base64.getUrlEncoder().encodeToString(pub.getPublicExponent().toByteArray());
        String data = "{\n"
                + "  \"keys\": [\n"
                + "    {\n"
                + "      \"alg\": \"RS256\",\n"
                + "      \"kty\": \"RSA\",\n"
                + "      \"n\": \"" + modulus + "\",\n"
                + "      \"use\": \"sig\",\n"
                + "      \"kid\": \"KEYID\",\n"
                + "      \"e\": \"" + exponent + "\"\n"
                + "    },\n"
                + "  ]\n"
                + "}";
        rc.response()
                .putHeader("Content-Type", "application/json")
                .endAndForget(data);
    }

    private void userinfo(RoutingContext rc) {
        String data = "{\n"
                + "  \"sub\": \"USERID\",\n"
                + "  \"name\": \"Foo Bar\",\n"
                + "  \"given_name\": \"Foo\",\n"
                + "  \"family_name\": \"Bar\",\n"
                + "  \"picture\": \"https://example.com/picture\",\n"
                + "  \"email\": \"google@example.com\",\n"
                + "  \"email_verified\": true,\n"
                + "  \"locale\": \"en-GB\"\n"
                + "}";
        rc.response()
                .putHeader("Content-Type", "application/json")
                .endAndForget(data);
    }
}
