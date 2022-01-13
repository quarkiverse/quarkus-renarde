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

import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.RoutingContext;
import io.vertx.mutiny.ext.web.handler.BodyHandler;

public class MockFacebookOidcTestResource extends MockOidcTestResource<MockFacebookOidc> {

    private KeyPair kp;

    public MockFacebookOidcTestResource() {
        super("facebook", 50004);
    }

    @Override
    protected void registerRoutes(Router router) {
        BodyHandler bodyHandler = BodyHandler.create();
        router.get("/.well-known/openid-configuration").handler(this::configuration);
        router.get("/dialog/oauth/").handler(this::authorize);
        router.post("/v12.0/oauth/access_token").handler(bodyHandler).handler(this::accessTokenJson);
        router.get("/.well-known/oauth/openid/jwks/").handler(this::getKeys);
        router.get("/me/").handler(this::getUser);

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
    public Map<String, String> start() {
        Map<String, String> ret = super.start();
        ret.put("quarkus.oidc.facebook.token-path", baseURI + "/v12.0/oauth/access_token");
        ret.put("quarkus.oidc.facebook.authorization-path", baseURI + "/dialog/oauth/");
        ret.put("quarkus.oidc.facebook.jwks-path", baseURI + "/.well-known/oauth/openid/jwks/");
        ret.put("quarkus.oidc.facebook.user-info-path", baseURI + "/me/?fields=id,name,email,first_name,last_name");
        return ret;
    }

    private void configuration(RoutingContext rc) {
        String data = "{\n"
                + "   \"issuer\": \"https://www.facebook.com\",\n"
                + "   \"authorization_endpoint\": \"" + baseURI + "/dialog/oauth/\",\n"
                + "   \"jwks_uri\": \"" + baseURI + "/.well-known/oauth/openid/jwks/\",\n"
                + "   \"response_types_supported\": [\n"
                + "      \"id_token\",\n"
                + "      \"token id_token\"\n"
                + "   ],\n"
                + "   \"subject_types_supported\": [\n"
                + "      \"pairwise\"\n"
                + "   ],\n"
                + "   \"id_token_signing_alg_values_supported\": [\n"
                + "      \"RS256\"\n"
                + "   ],\n"
                + "   \"claims_supported\": [\n"
                + "      \"iss\",\n"
                + "      \"aud\",\n"
                + "      \"sub\",\n"
                + "      \"iat\",\n"
                + "      \"exp\",\n"
                + "      \"jti\",\n"
                + "      \"nonce\",\n"
                + "      \"at_hash\",\n"
                + "      \"name\",\n"
                + "      \"given_name\",\n"
                + "      \"middle_name\",\n"
                + "      \"family_name\",\n"
                + "      \"email\",\n"
                + "      \"picture\",\n"
                + "      \"user_friends\",\n"
                + "      \"user_birthday\",\n"
                + "      \"user_age_range\",\n"
                + "      \"user_link\",\n"
                + "      \"user_hometown\",\n"
                + "      \"user_location\",\n"
                + "      \"user_gender\"\n"
                + "   ]\n"
                + "}";
        rc.response().putHeader("Content-Type", "application/json");
        rc.endAndForget(data);
    }

    /*
     * First request:
     * GET
     * https://facebook.com/dialog/oauth/?response_type=code&client_id=CLIENT&scope=openid+email+public_profile&redirect_uri=
     * http://localhost:8080/Login/facebookLoginSuccess&state=STATE
     * 
     * returns a 302 to
     * GET http://localhost:8080/Login/facebookLoginSuccess?code=CODE&state=STATE
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
            redirect = new URI(redirect_uri + "?state=" + state + "&code=" + code);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        rc.response()
                .putHeader("Location", redirect.toASCIIString())
                .setStatusCode(302)
                .endAndForget();
    }

    /*
     * OIDC calls POST /v12.0/oauth/access_token
     * grant_type=authorization_code
     * &code=CODE
     * &redirect_uri=http%3A%2F%2Flocalhost%3A8080%2FLogin%2FfacebookLoginSuccess
     * 
     * returns:
     * {
     * "access_token":TOKEN,
     * "token_type":"bearer",
     * "expires_in":5172337
     * }
     * 
     */
    private void accessTokenJson(RoutingContext rc) {
        String authorization_code = rc.request().formAttributes().get("authorization_code");
        String code = rc.request().formAttributes().get("code");
        String redirect_uri = rc.request().formAttributes().get("redirect_uri");

        UUID token = UUID.randomUUID();

        String data = "{\n"
                + " \"access_token\":\"" + token + "\",\n"
                + " \"token_type\":\"bearer\",\n"
                + " \"expires_in\":5172337\n"
                + "} ";
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

    /*
     * GET /me/?fields=id,name,email,first_name,last_name HTTP/1.1
     * authorization: Bearer TOKEN
     * 
     * {
     * "id":"USERID",
     * "name":"Foo Bar",
     * "email":"facebook@example.com",
     * "first_name":"Foo",
     * "last_name":"Bar"
     * }
     */
    private void getUser(RoutingContext rc) {
        rc.response()
                .putHeader("Content-Type", "application/json")
                .endAndForget("{\n"
                        + " \"id\":\"USERID\",\n"
                        + " \"name\":\"Foo Bar\",\n"
                        + " \"email\":\"facebook@example.com\",\n"
                        + " \"first_name\":\"Foo\",\n"
                        + " \"last_name\":\"Bar\"\n"
                        + "} ");
    }

}
