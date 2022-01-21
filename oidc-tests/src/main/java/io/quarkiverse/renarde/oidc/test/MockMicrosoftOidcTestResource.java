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

public class MockMicrosoftOidcTestResource extends MockOidcTestResource<MockMicrosoftOidc> {

    private KeyPair kp;

    public MockMicrosoftOidcTestResource() {
        super("microsoft", 50002);
    }

    @Override
    protected void registerRoutes(Router router) {
        BodyHandler bodyHandler = BodyHandler.create();
        router.get("/.well-known/openid-configuration").handler(this::configuration);
        router.get("/common/oauth2/v2.0/authorize").handler(this::authorize);
        router.post("/common/oauth2/v2.0/token").handler(bodyHandler).handler(this::accessTokenJson);
        router.get("/common/discovery/v2.0/keys").handler(this::getKeys);

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
                + "   \"token_endpoint\":\"" + baseURI + "/common/oauth2/v2.0/token\",\n"
                + "   \"token_endpoint_auth_methods_supported\":[\n"
                + "      \"client_secret_post\",\n"
                + "      \"private_key_jwt\",\n"
                + "      \"client_secret_basic\"\n"
                + "   ],\n"
                + "   \"jwks_uri\":\"" + baseURI + "/common/discovery/v2.0/keys\",\n"
                + "   \"response_modes_supported\":[\n"
                + "      \"query\",\n"
                + "      \"fragment\",\n"
                + "      \"form_post\"\n"
                + "   ],\n"
                + "   \"subject_types_supported\":[\n"
                + "      \"pairwise\"\n"
                + "   ],\n"
                + "   \"id_token_signing_alg_values_supported\":[\n"
                + "      \"RS256\"\n"
                + "   ],\n"
                + "   \"response_types_supported\":[\n"
                + "      \"code\",\n"
                + "      \"id_token\",\n"
                + "      \"code id_token\",\n"
                + "      \"id_token token\"\n"
                + "   ],\n"
                + "   \"scopes_supported\":[\n"
                + "      \"openid\",\n"
                + "      \"profile\",\n"
                + "      \"email\",\n"
                + "      \"offline_access\"\n"
                + "   ],\n"
                + "   \"issuer\":\"" + baseURI + "/{tenantid}/v2.0\",\n"
                + "   \"request_uri_parameter_supported\":false,\n"
                + "   \"userinfo_endpoint\":\"" + baseURI + "/oidc/userinfo\",\n"
                + "   \"authorization_endpoint\":\"" + baseURI + "/common/oauth2/v2.0/authorize\",\n"
                + "   \"device_authorization_endpoint\":\"" + baseURI + "/common/oauth2/v2.0/devicecode\",\n"
                + "   \"http_logout_supported\":true,\n"
                + "   \"frontchannel_logout_supported\":true,\n"
                + "   \"end_session_endpoint\":\"" + baseURI + "/common/oauth2/v2.0/logout\",\n"
                + "   \"claims_supported\":[\n"
                + "      \"sub\",\n"
                + "      \"iss\",\n"
                + "      \"cloud_instance_name\",\n"
                + "      \"cloud_instance_host_name\",\n"
                + "      \"cloud_graph_host_name\",\n"
                + "      \"msgraph_host\",\n"
                + "      \"aud\",\n"
                + "      \"exp\",\n"
                + "      \"iat\",\n"
                + "      \"auth_time\",\n"
                + "      \"acr\",\n"
                + "      \"nonce\",\n"
                + "      \"preferred_username\",\n"
                + "      \"name\",\n"
                + "      \"tid\",\n"
                + "      \"ver\",\n"
                + "      \"at_hash\",\n"
                + "      \"c_hash\",\n"
                + "      \"email\"\n"
                + "   ],\n"
                + "   \"kerberos_endpoint\":\"" + baseURI + "/common/kerberos\",\n"
                + "   \"tenant_region_scope\":null,\n"
                + "   \"cloud_instance_name\":\"microsoftonline.com\",\n"
                + "   \"cloud_graph_host_name\":\"graph.windows.net\",\n"
                + "   \"msgraph_host\":\"graph.microsoft.com\",\n"
                + "   \"rbac_url\":\"https://pas.windows.net\"\n"
                + "}";
        rc.response().putHeader("Content-Type", "application/json");
        rc.endAndForget(data);
    }

    /*
     * First request:
     * GET
     * https://login.microsoftonline.com/common/oauth2/v2.0/authorize?response_type=code&client_id=SECRET&scope=openid+openid+
     * email+profile&redirect_uri=http://localhost:8080/Login/oidcLoginSuccess&state=STATE
     * 
     * returns a 302 to
     * GET http://localhost:8080/Login/oidcLoginSuccess?code=CODE&state=STATE
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
     * OIDC calls POST /token
     * grant_type=authorization_code&code=CODE&redirect_uri=http%3A%2F%2Flocalhost%3A8080%2FLogin%2FoidcLoginSuccess
     * returns:
     * 
     * {
     * "token_type":"Bearer",
     * "scope":"openid email profile",
     * "expires_in":3600,
     * "ext_expires_in":3600,
     * "access_token":TOKEN,
     * "id_token":JWT
     * }
     * 
     * ID token:
     * {
     * "ver": "2.0",
     * "iss": "https://login.microsoftonline.com/TENANTID/v2.0",
     * "sub": "USERID",
     * "aud": "OPAQUE",
     * "exp": 1641906214,
     * "iat": 1641819514,
     * "nbf": 1641819514,
     * "name": "Foo Bar",
     * "preferred_username": "microsoft@example.com",
     * "oid": "OPAQUE",
     * "email": "microsoft@example.com",
     * "tid": "TENANTID",
     * "aio": "AZURE_OPAQUE"
     * }
     */
    private void accessTokenJson(RoutingContext rc) {
        String authorization_code = rc.request().formAttributes().get("authorization_code");
        String code = rc.request().formAttributes().get("code");
        String redirect_uri = rc.request().formAttributes().get("redirect_uri");

        UUID token = UUID.randomUUID();
        UUID tenant = UUID.randomUUID();
        String hashedToken = hashAccessToken(token.toString());
        String idToken = Jwt.issuer("https://accounts.google.com")
                .audience("SOMETHING")
                .claim("ver", "2.0")
                .issuer(baseURI + "/" + tenant + "/v2.0")
                .subject("USERID")
                .audience(UUID.randomUUID().toString())
                .expiresIn(Duration.ofDays(1))
                .issuedAt(Instant.now())
                .claim(Claims.nbf, Instant.now())
                .claim("name", "Foo Bar")
                .claim(Claims.preferred_username, "microsoft@example.com")
                .claim("oid", UUID.randomUUID().toString())
                .claim(Claims.email, "microsoft@example.com")
                .claim("tid", tenant.toString())
                .claim("aio", UUID.randomUUID().toString())
                .jws()
                .keyId("KEYID")
                .sign(kp.getPrivate());

        String data = "{\n"
                + " \"token_type\":\"Bearer\",\n"
                + " \"scope\":\"openid email profile\",\n"
                + " \"expires_in\":3600,\n"
                + " \"ext_expires_in\":3600,\n"
                + " \"access_token\":\"" + token + "\",\n"
                + " \"id_token\":\"" + idToken + "\"\n"
                + " }  ";
        rc.response()
                .putHeader("Content-Type", "application/json")
                .endAndForget(data);
    }

    /*
     * {"kty":"RSA",
     * "use":"sig",
     * "kid":"nOo3ZDrODXEK1jKWhXslHR_KXEg",
     * "x5t":"nOo3ZDrODXEK1jKWhXslHR_KXEg",
     * "n":
     * "oaLLT9hkcSj2tGfZsjbu7Xz1Krs0qEicXPmEsJKOBQHauZ_kRM1HdEkgOJbUznUspE6xOuOSXjlzErqBxXAu4SCvcvVOCYG2v9G3-uIrLF5dstD0sYHBo1VomtKxzF90Vslrkn6rNQgUGIWgvuQTxm1uRklYFPEcTIRw0LnYknzJ06GC9ljKR617wABVrZNkBuDgQKj37qcyxoaxIGdxEcmVFZXJyrxDgdXh9owRmZn6LIJlGjZ9m59emfuwnBnsIQG7DirJwe9SXrLXnexRQWqyzCdkYaOqkpKrsjuxUj2-MHX31FqsdpJJsOAvYXGOYBKJRjhGrGdONVrZdUdTBQ",
     * "e":"AQAB",
     * "x5c":[
     * "MIIDBTCCAe2gAwIBAgIQN33ROaIJ6bJBWDCxtmJEbjANBgkqhkiG9w0BAQsFADAtMSswKQYDVQQDEyJhY2NvdW50cy5hY2Nlc3Njb250cm9sLndpbmRvd3MubmV0MB4XDTIwMTIyMTIwNTAxN1oXDTI1MTIyMDIwNTAxN1owLTErMCkGA1UEAxMiYWNjb3VudHMuYWNjZXNzY29udHJvbC53aW5kb3dzLm5ldDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKGiy0/YZHEo9rRn2bI27u189Sq7NKhInFz5hLCSjgUB2rmf5ETNR3RJIDiW1M51LKROsTrjkl45cxK6gcVwLuEgr3L1TgmBtr/Rt/riKyxeXbLQ9LGBwaNVaJrSscxfdFbJa5J+qzUIFBiFoL7kE8ZtbkZJWBTxHEyEcNC52JJ8ydOhgvZYykete8AAVa2TZAbg4ECo9+6nMsaGsSBncRHJlRWVycq8Q4HV4faMEZmZ+iyCZRo2fZufXpn7sJwZ7CEBuw4qycHvUl6y153sUUFqsswnZGGjqpKSq7I7sVI9vjB199RarHaSSbDgL2FxjmASiUY4RqxnTjVa2XVHUwUCAwEAAaMhMB8wHQYDVR0OBBYEFI5mN5ftHloEDVNoIa8sQs7kJAeTMA0GCSqGSIb3DQEBCwUAA4IBAQBnaGnojxNgnV4+TCPZ9br4ox1nRn9tzY8b5pwKTW2McJTe0yEvrHyaItK8KbmeKJOBvASf+QwHkp+F2BAXzRiTl4Z+gNFQULPzsQWpmKlz6fIWhc7ksgpTkMK6AaTbwWYTfmpKnQw/KJm/6rboLDWYyKFpQcStu67RZ+aRvQz68Ev2ga5JsXlcOJ3gP/lE5WC1S0rjfabzdMOGP8qZQhXk4wBOgtFBaisDnbjV5pcIrjRPlhoCxvKgC/290nZ9/DLBH3TbHk8xwHXeBAnAjyAqOZij92uksAv7ZLq4MODcnQshVINXwsYshG1pQqOLwMertNaY5WtrubMRku44Dw7R"
     * ],
     * "issuer":"https://login.microsoftonline.com/{tenantid}/v2.0"},
     */
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
                + "      \"k5t\": \"KEYID\",\n"
                + "      \"issuer\": \"" + baseURI + "/{tenantid}/v2.0\",\n"
                + "      \"e\": \"" + exponent + "\"\n"
                + "    },\n"
                + "  ]\n"
                + "}";
        rc.response()
                .putHeader("Content-Type", "application/json")
                .endAndForget(data);
    }
}
