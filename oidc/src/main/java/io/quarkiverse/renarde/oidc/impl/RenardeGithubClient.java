package io.quarkiverse.renarde.oidc.impl;

import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;

@RegisterRestClient(baseUri = "https://api.github.com")
@Path("/")
public interface RenardeGithubClient {

    @GET
    @Path("user/emails")
    public List<Email> getEmails(@HeaderParam("Authorization") String auth);

    public static class Email {
        public String email;
        public boolean primary;
        public boolean verified;
        public String visibility;
    }
}
