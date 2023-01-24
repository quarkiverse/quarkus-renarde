package rest;

import io.quarkiverse.renarde.security.ControllerWithUser;
import io.quarkus.security.Authenticated;
import model.User;

@Authenticated
public class SecureController extends ControllerWithUser<User> {

    public String hello() {
        return "Hello Security from " + getUser().username;
    }
}
