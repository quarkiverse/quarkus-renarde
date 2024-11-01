package rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestQuery;

import io.quarkiverse.renarde.Controller;

public class ValidationController {
    @Path("/ValidationController")
    public static class RenardController extends Controller {
        @GET
        @Path("/RenardController")
        public void renardValidate(@Valid @NotNull @RestQuery String input) {

        }
    }

    @Path("/ValidationController")
    public static class RestController {
        @GET
        @Path("/RestController")
        public void restValidate(@Valid @NotNull @RestQuery String input) {

        }
    }
}
