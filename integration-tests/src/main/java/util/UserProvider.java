package util;

import javax.enterprise.context.ApplicationScoped;

import io.quarkiverse.renarde.security.RenardeUser;
import io.quarkiverse.renarde.security.RenardeUserProvider;
import model.User;

@ApplicationScoped
public class UserProvider implements RenardeUserProvider {

    @Override
    public RenardeUser findUser(String tenantId, String authId) {
        return User.findByUsername(authId);
    }

}
