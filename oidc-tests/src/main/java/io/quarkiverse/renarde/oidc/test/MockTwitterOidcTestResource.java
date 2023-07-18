package io.quarkiverse.renarde.oidc.test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.UUID;

import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.RoutingContext;
import io.vertx.mutiny.ext.web.handler.BodyHandler;

public class MockTwitterOidcTestResource extends MockOidcTestResource<MockTwitterOidc> {

    public MockTwitterOidcTestResource() {
        super("twitter", 50005);
    }

    @Override
    protected void registerRoutes(Router router) {
        BodyHandler bodyHandler = BodyHandler.create();
        router.get("/i/oauth2/authorize").handler(this::authorize);
        router.post("/2/oauth2/token").handler(bodyHandler).handler(this::accessToken);
        router.get("/2/users/me").handler(this::getUser);
    }

    @Override
    public Map<String, String> start() {
        Map<String, String> ret = super.start();
        ret.put("quarkus.oidc.twitter.auth-server-url", baseURI + "/2/oauth2");
        ret.put("quarkus.oidc.twitter.user-info-path", baseURI + "/2/users/me");
        ret.put("quarkus.oidc.twitter.authorization-path", baseURI + "/i/oauth2/authorize");
        return ret;
    }

    /*
     * First request:
     * GET
     * https://twitter.com/i/oauth2/authorize?
     * response_type=code
     * &client_id=TWCLIENT
     * &scope=offline.access+offline.access
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
                .endAndForget();
    }

    /*
     * OIDC calls POST https://api.twitter.com/2/oauth2/token
     * Authorization: Basic AUTH
     *
     * grant_type=authorization_code
     * code=CODE
     * redirect_uri=http://localhost:8080/_renarde/security/oidc-success
     * code_verifier=VERIFIER
     *
     * returns:
     * {
     * "token_type":"bearer",
     * "expires_in":7200,
     * "access_token":"ATOKEN",
     * "scope":"users.read tweet.read offline.access",
     * "refresh_token":"RTOKEN"
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
                .endAndForget("{\n"
                        + "  \"token_type\":\"bearer\",\n"
                        + "  \"expires_in\":7200,\n"
                        + "  \"access_token\":\"" + atoken + "\",\n"
                        + "  \"scope\":\"users.read tweet.read offline.access\",\n"
                        + "  \"refresh_token\":\"" + rtoken + "\"\n"
                        + "}");
    }

    /*
     * OIDC calls for UserInfo
     * Authorization: Bearer OAUTH-TOKEN
     * GET https://api.twitter.com/2/users/me
     * Returns:
     * {
     * "data":
     * {
     * "id":"USERID",
     * "name":"Foo Bar",
     * "username":"TwitterUser"
     * }
     * }
     */
    private void getUser(RoutingContext rc) {
        rc.response()
                .putHeader("Content-Type", "application/json")
                .endAndForget("{\n"
                        + "  \"data\":\n"
                        + "  {\n"
                        + "   \"id\":\"USERID\",\n"
                        + "   \"name\":\"Foo Bar\",\n"
                        + "   \"username\":\"TwitterUser\"\n"
                        + "  }\n"
                        + " }");
    }
}
