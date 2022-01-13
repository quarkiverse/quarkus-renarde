package io.quarkiverse.renarde.oidc.impl;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
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
