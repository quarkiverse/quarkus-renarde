package io.quarkiverse.renarde.oidc.test;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class MockSpotifyOidcTestResource extends MockOidcTestResource<MockTwitterOidc> {

    private KeyPair kp;

    public MockSpotifyOidcTestResource() {
        super("spotify");

        KeyPairGenerator kpg;
        try {
            kpg = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        kpg.initialize(2048);
        kp = kpg.generateKeyPair();
    }

    @Override
    protected void registerRoutes(Router router) {
        BodyHandler bodyHandler = BodyHandler.create();
        // KnownOidcProviders disables auto-detection, not sure why
        router.get("/.well-known/openid-configuration").handler(this::configuration);
        // route overriden in KnownOidcProviders (for some reason)
        router.get("/authorize").handler(this::authorize);
        // route from .well-known
        router.get("/oauth2/v2/auth").handler(this::authorize);
        router.post("/api/token").handler(bodyHandler).handler(this::accessToken);
        // route overriden in KnownOidcProviders (for some reason)
        router.get("/v1/me").handler(this::getUser);
        // route from .well-known
        router.get("/oidc/userinfo/v1").handler(this::getUser);
        router.get("/oidc/certs/v1").handler(this::getKeys);
    }

    @Override
    public Map<String, String> start() {
        Map<String, String> ret = super.start();
        ret.put("quarkus.oidc.spotify.user-info-path", baseURI + "/v1/me");
        return ret;
    }

    private void configuration(RoutingContext rc) {
        String data = "{\n"
                + "   \"issuer\":\"https://accounts.spotify.com\",\n"
                + "   \"authorization_endpoint\":\"" + baseURI + "/oauth2/v2/auth\",\n"
                + "   \"token_endpoint\":\"" + baseURI + "/api/token\",\n"
                + "   \"userinfo_endpoint\":\"" + baseURI + "/oidc/userinfo/v1\",\n"
                + "   \"revocation_endpoint\":\"" + baseURI + "/oauth2/revoke/v1\",\n"
                + "   \"scopes_supported\":[\n"
                + "      \"email\",\n"
                + "      \"openid\",\n"
                + "      \"profile\"\n"
                + "   ],\n"
                + "   \"jwks_uri\":\"" + baseURI + "/oidc/certs/v1\",\n"
                + "   \"response_types_supported\":[\n"
                + "      \"code\",\n"
                + "      \"none\"\n"
                + "   ],\n"
                + "   \"response_modes_supported\":[\n"
                + "      \"query\"\n"
                + "   ],\n"
                + "   \"code_challenge_methods_supported\":[\n"
                + "      \"S256\"\n"
                + "   ],\n"
                + "   \"grant_types_supported\":[\n"
                + "      \"authorization_code\",\n"
                + "      \"refresh_token\",\n"
                + "      \"urn:ietf:params:oauth:grant-type:device_code\",\n"
                + "      \"urn:ietf:params:oauth:grant-type:jwt-bearer\"\n"
                + "   ],\n"
                + "   \"acr_values_supported\":[\n"
                + "      \"urn:spotify:sso:acr:legacy\",\n"
                + "      \"urn:spotify:sso:acr:bronze:v1\",\n"
                + "      \"urn:spotify:sso:acr:silver:v1\",\n"
                + "      \"urn:spotify:sso:acr:artist:2fa\"\n"
                + "   ],\n"
                + "   \"subject_types_supported\":[\n"
                + "      \"pairwise\"\n"
                + "   ],\n"
                + "   \"id_token_signing_alg_values_supported\":[\n"
                + "      \"RS256\"\n"
                + "   ],\n"
                + "   \"claims_supported\":[\n"
                + "      \"aud\",\n"
                + "      \"email\",\n"
                + "      \"email_verified\",\n"
                + "      \"exp\",\n"
                + "      \"iat\",\n"
                + "      \"iss\",\n"
                + "      \"name\",\n"
                + "      \"picture\",\n"
                + "      \"preferred_username\",\n"
                + "      \"sub\"\n"
                + "   ],\n"
                + "   \"token_endpoint_auth_methods_supported\":[\n"
                + "      \"client_secret_basic\",\n"
                + "      \"client_secret_post\"\n"
                + "   ],\n"
                + "   \"ui_locales_supported\":[\n"
                + "      \"af-ZA\",\n"
                + "      \"am-ET\",\n"
                + "      \"ar\",\n"
                + "      \"az-AZ\",\n"
                + "      \"bg-BG\",\n"
                + "      \"bn-IN\",\n"
                + "      \"bp\",\n"
                + "      \"cs\",\n"
                + "      \"da-DK\",\n"
                + "      \"de\",\n"
                + "      \"el\",\n"
                + "      \"en\",\n"
                + "      \"es\",\n"
                + "      \"es-ES\",\n"
                + "      \"et-EE\",\n"
                + "      \"fa-IR\",\n"
                + "      \"fi\",\n"
                + "      \"tl\",\n"
                + "      \"fr\",\n"
                + "      \"fr-CA\",\n"
                + "      \"gu-IN\",\n"
                + "      \"he-IL\",\n"
                + "      \"hi-IN\",\n"
                + "      \"hr-HR\",\n"
                + "      \"hu\",\n"
                + "      \"id\",\n"
                + "      \"is-IS\",\n"
                + "      \"it\",\n"
                + "      \"ja\",\n"
                + "      \"kn-IN\",\n"
                + "      \"ko\",\n"
                + "      \"lv-LV\",\n"
                + "      \"lt-LT\",\n"
                + "      \"ml-IN\",\n"
                + "      \"mr-IN\",\n"
                + "      \"ms\",\n"
                + "      \"nb-NO\",\n"
                + "      \"ne-NP\",\n"
                + "      \"nl\",\n"
                + "      \"or-IN\",\n"
                + "      \"pa-IN\",\n"
                + "      \"pa-PK\",\n"
                + "      \"pl\",\n"
                + "      \"pt-BR\",\n"
                + "      \"pt-PT\",\n"
                + "      \"ro-RO\",\n"
                + "      \"ru\",\n"
                + "      \"sk-SK\",\n"
                + "      \"sl-SI\",\n"
                + "      \"sr-RS\",\n"
                + "      \"sv\",\n"
                + "      \"sw\",\n"
                + "      \"ta-IN\",\n"
                + "      \"te-IN\",\n"
                + "      \"th-TH\",\n"
                + "      \"tr\",\n"
                + "      \"uk-UA\",\n"
                + "      \"ur\",\n"
                + "      \"vi-VN\",\n"
                + "      \"zh-CN\",\n"
                + "      \"zh-TW\",\n"
                + "      \"zu-ZA\"\n"
                + "   ]\n"
                + "}";
        rc.response().putHeader("Content-Type", "application/json");
        rc.end(data);
    }

    /*
     * First request (.well-known advertises another URI):
     * GET
     * https://accounts.spotify.com/authorize?
     * response_type=code
     * &client_id=SPCLIENT
     * &scope=user-read-email
     * &redirect_uri=http%3A%2F%2Flocalhost%3A8080%2F_renarde%2Fsecurity%2Foidc-success
     * &state=STATE
     * &code_challenge=CHALLENGE
     * &code_challenge_method=S256
     * returns a 302 to
     * GET http://localhost:8080/_renarde/security/oidc-success?code=CODE&state=STATE
     */
    private void authorize(RoutingContext rc) {
        String response_type = rc.request().params().get("response_type");
        String client_id = rc.request().params().get("client_id");
        String scope = rc.request().params().get("scope");
        String state = rc.request().params().get("state");
        String redirect_uri = rc.request().params().get("redirect_uri");
        String code_challenge = rc.request().params().get("code_challenge");
        String code_challenge_method = rc.request().params().get("code_challenge_method");
        UUID code = UUID.randomUUID();
        URI redirect;
        try {
            redirect = new URI(redirect_uri + "?state=" + state + "&code=" + code);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        rc.response()
                .putHeader("Location", redirect.toASCIIString())
                .setStatusCode(302)
                .end();
    }

    /*
     * OIDC calls POST https://accounts.spotify.com/api/token
     * Authorization: Basic AUTH
     *
     * grant_type=authorization_code
     * code=CODE
     * redirect_uri=http://localhost:8080/_renarde/security/oidc-success
     * code_verifier=VERIFIER
     *
     * returns:
     * {
     * "access_token":"ATOKEN",
     * "token_type":"Bearer",
     * "expires_in":3600,
     * "refresh_token":"RTOKEN"
     * "scope":"user-read-email",
     * }
     */
    private void accessToken(RoutingContext rc) {
        String grant_type = rc.request().formAttributes().get("grant_type");
        String code_verifier = rc.request().formAttributes().get("code_verifier");
        String client_secret = rc.request().formAttributes().get("client_secret");
        String code = rc.request().formAttributes().get("code");
        UUID atoken = UUID.randomUUID();
        UUID rtoken = UUID.randomUUID();
        rc.response()
                .putHeader("Content-Type", "application/json")
                .end("{\n"
                        + "   \"access_token\":\"" + atoken + "\",\n"
                        + "   \"token_type\":\"Bearer\",\n"
                        + "   \"expires_in\":3600,\n"
                        + "   \"refresh_token\":\"" + rtoken + "\",\n"
                        + "   \"scope\":\"user-read-email\"\n"
                        + "}");
    }

    /*
     * OIDC calls for UserInfo
     * Authorization: Bearer OAUTH-TOKEN
     * GET https://api.spotify.com/v1/me
     * Returns:
     * {
     * "display_name" : "Foo Bar",
     * "external_urls" : {
     * "spotify" : "https://open.spotify.com/user/USERID"
     * },
     * "href" : "https://api.spotify.com/v1/users/USERID",
     * "id" : "USERID",
     * "images" : [ ],
     * "type" : "user",
     * "uri" : "spotify:user:USERID",
     * "followers" : {
     * "href" : null,
     * "total" : 0
     * },
     * "email" : "spotify@example.com"
     * }
     */
    private void getUser(RoutingContext rc) {
        rc.response()
                .putHeader("Content-Type", "application/json")
                .end("{\n"
                        + "  \"display_name\" : \"Foo Bar\",\n"
                        + "  \"external_urls\" : {\n"
                        + "    \"spotify\" : \"https://open.spotify.com/user/USERID\"\n"
                        + "  },\n"
                        + "  \"href\" : \"https://api.spotify.com/v1/users/USERID\",\n"
                        + "  \"id\" : \"USERID\",\n"
                        + "  \"images\" : [ ],\n"
                        + "  \"type\" : \"user\",\n"
                        + "  \"uri\" : \"spotify:user:USERID\",\n"
                        + "  \"followers\" : {\n"
                        + "    \"href\" : null,\n"
                        + "    \"total\" : 0\n"
                        + "  },\n"
                        + "  \"email\" : \"spotify@example.com\"\n"
                        + "}\n"
                        + "");
    }

    private void getKeys(RoutingContext rc) {
        RSAPublicKey pub = (RSAPublicKey) kp.getPublic();
        String modulus = Base64.getUrlEncoder().encodeToString(pub.getModulus().toByteArray());
        String exponent = Base64.getUrlEncoder().encodeToString(pub.getPublicExponent().toByteArray());
        String data = "{\n"
                + "   \"keys\":[\n"
                + "      {\n"
                + "         \"kty\":\"RSA\",\n"
                + "         \"e\":\"" + exponent + "\",\n"
                + "         \"use\":\"sig\",\n"
                + "         \"kid\":\"sig-1689577269\",\n"
                + "         \"alg\":\"RS256\",\n"
                + "         \"n\":\"" + modulus + "\"\n"
                + "      },\n"
                + "      {\n"
                + "         \"kty\":\"RSA\",\n"
                + "         \"e\":\"" + exponent + "\",\n"
                + "         \"use\":\"sig\",\n"
                + "         \"kid\":\"sig-1688972789\",\n"
                + "         \"alg\":\"RS256\",\n"
                + "         \"n\":\"" + modulus + "\"\n"
                + "      },\n"
                + "      {\n"
                + "         \"kty\":\"RSA\",\n"
                + "         \"e\":\"" + exponent + "\",\n"
                + "         \"use\":\"sig\",\n"
                + "         \"kid\":\"sig-1688367727\",\n"
                + "         \"alg\":\"RS256\",\n"
                + "         \"n\":\"" + modulus + "\"\n"
                + "      }\n"
                + "   ]\n"
                + "}";
        rc.response()
                .putHeader("Content-Type", "application/json")
                .end(data);
    }
}
