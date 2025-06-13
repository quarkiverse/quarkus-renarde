package io.quarkiverse.renarde.oidc.test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.UUID;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class MockGithubOidcTestResource extends MockOidcTestResource<MockGithubOidc> {

    public MockGithubOidcTestResource() {
        super("github");
    }

    @Override
    protected void registerRoutes(Router router) {
        BodyHandler bodyHandler = BodyHandler.create();
        router.get("/login/oauth/authorize").handler(this::authorize);
        router.post("/login/oauth/access_token").handler(bodyHandler).handler(this::accessToken);
        router.get("/user").handler(this::getUser);
        router.get("/user/emails").handler(this::getEmails);
    }

    @Override
    public Map<String, String> start() {
        Map<String, String> ret = super.start();
        ret.put("quarkus.rest-client.RenardeGithubClient.url", baseURI);
        ret.put("quarkus.oidc.github.auth-server-url", baseURI + "/login/oauth");
        ret.put("quarkus.oidc.github.user-info-path", baseURI + "/user");
        return ret;
    }

    /*
     * First request:
     * GET
     * https://github.com/login/oauth/authorize?response_type=code&client_id=GHCLIENT&scope=openid+user:email&redirect_uri=http:
     * //localhost:8080/Login/githubLoginSuccess&state=e956e017-5e13-4c9d-b83b-6dd6337a6a86
     * returns a 302 to
     * GET http://localhost:8080/Login/githubLoginSuccess?code=36e9dd7c003df627de39&state=e956e017-5e13-4c9d-b83b-6dd6337a6a86
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
                .end();
    }

    /*
     * OIDC calls POST https://github.com/login/oauth/access_token
     * client_id string Required. The client ID you received from GitHub for your OAuth App.
     * client_secret string Required. The client secret you received from GitHub for your OAuth App.
     * code string Required. The code you received as a response to Step 1.
     * returns:
     * access_token=gho_16C7e42F292c6912E7710c838347Ae178B4a&scope=repo%2Cgist&token_type=bearer
     */
    private void accessToken(RoutingContext rc) {
        String client_id = rc.request().formAttributes().get("client_id");
        String client_secret = rc.request().formAttributes().get("client_secret");
        String code = rc.request().formAttributes().get("code");
        UUID token = UUID.randomUUID();
        if ("application/x-www-form-urlencoded".equals(rc.request().getHeader("Accept"))) {
            rc.response()
                    .putHeader("Content-Type", "application/x-www-form-urlencoded")
                    .end("access_token=" + token + "&scope=repo%2Cgist&token_type=bearer");
        } else {
            rc.response()
                    .putHeader("Content-Type", "application/json")
                    .end("{\n"
                            + "  \"access_token\":\"" + token + "\",\n"
                            + "  \"scope\":\"repo,gist\",\n"
                            + "  \"token_type\":\"bearer\"\n"
                            + "}");
        }
    }

    /*
     * OIDC calls for UserInfo
     * Authorization: token OAUTH-TOKEN
     * GET https://api.github.com/user
     */
    private void getUser(RoutingContext rc) {
        rc.response()
                .putHeader("Content-Type", "application/json")
                .end("{\n"
                        + "  \"login\": \"GithubUser\",\n"
                        + "  \"id\": 1234,\n"
                        + "  \"node_id\": \"MDQ6VXNlcjE=\",\n"
                        + "  \"avatar_url\": \"https://github.com/images/error/octocat_happy.gif\",\n"
                        + "  \"gravatar_id\": \"\",\n"
                        + "  \"url\": \"https://api.github.com/users/octocat\",\n"
                        + "  \"html_url\": \"https://github.com/octocat\",\n"
                        + "  \"followers_url\": \"https://api.github.com/users/octocat/followers\",\n"
                        + "  \"following_url\": \"https://api.github.com/users/octocat/following{/other_user}\",\n"
                        + "  \"gists_url\": \"https://api.github.com/users/octocat/gists{/gist_id}\",\n"
                        + "  \"starred_url\": \"https://api.github.com/users/octocat/starred{/owner}{/repo}\",\n"
                        + "  \"subscriptions_url\": \"https://api.github.com/users/octocat/subscriptions\",\n"
                        + "  \"organizations_url\": \"https://api.github.com/users/octocat/orgs\",\n"
                        + "  \"repos_url\": \"https://api.github.com/users/octocat/repos\",\n"
                        + "  \"events_url\": \"https://api.github.com/users/octocat/events{/privacy}\",\n"
                        + "  \"received_events_url\": \"https://api.github.com/users/octocat/received_events\",\n"
                        + "  \"type\": \"User\",\n"
                        + "  \"site_admin\": false,\n"
                        + "  \"name\": \"Foo Bar\",\n"
                        + "  \"company\": \"GitHub\",\n"
                        + "  \"blog\": \"https://github.com/blog\",\n"
                        + "  \"location\": \"San Francisco\",\n"
                        // disabled as many people make this private, and we want to fetch it from the /user/emails endpoint
                        // which includes private emails
                        //                + "  \"email\": \"octocat@github.com\",\n"
                        + "  \"hireable\": false,\n"
                        + "  \"bio\": \"There once was...\",\n"
                        + "  \"twitter_username\": \"monatheoctocat\",\n"
                        + "  \"public_repos\": 2,\n"
                        + "  \"public_gists\": 1,\n"
                        + "  \"followers\": 20,\n"
                        + "  \"following\": 0,\n"
                        + "  \"created_at\": \"2008-01-14T04:33:35Z\",\n"
                        + "  \"updated_at\": \"2008-01-14T04:33:35Z\",\n"
                        + "  \"private_gists\": 81,\n"
                        + "  \"total_private_repos\": 100,\n"
                        + "  \"owned_private_repos\": 100,\n"
                        + "  \"disk_usage\": 10000,\n"
                        + "  \"collaborators\": 8,\n"
                        + "  \"two_factor_authentication\": true,\n"
                        + "  \"plan\": {\n"
                        + "    \"name\": \"Medium\",\n"
                        + "    \"space\": 400,\n"
                        + "    \"private_repos\": 20,\n"
                        + "    \"collaborators\": 0\n"
                        + "  }\n"
                        + "}");
    }

    /*
     * other call for emails via github client
     * GET /user/emails
     */
    private void getEmails(RoutingContext rc) {
        rc.response()
                .putHeader("Content-Type", "application/json")
                .end("[\n"
                        + "  {\n"
                        + "    \"email\": \"github@example.com\",\n"
                        + "    \"verified\": true,\n"
                        + "    \"primary\": true,\n"
                        + "    \"visibility\": \"public\"\n"
                        + "  }\n"
                        + "]\n");
    }
}
