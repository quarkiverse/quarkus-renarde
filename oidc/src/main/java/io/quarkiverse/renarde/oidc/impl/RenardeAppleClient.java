package io.quarkiverse.renarde.oidc.impl;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(baseUri = "https://appleid.apple.com")
public interface RenardeAppleClient {

    @POST
    @Path("/auth/revoke")
    void revokeAppleUser(@FormParam("client_id") String clientID,
            @FormParam("client_secret") String clientSecret,
            @FormParam("token") String token,
            @FormParam("token_type_hint") String tokenTypeHint);
}
