package util;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.renarde.security.RenardeUser;
import io.quarkiverse.renarde.security.RenardeUserProvider;
import model.User;

@ApplicationScoped
public class UserProvider implements RenardeUserProvider {

    @Override
    public RenardeUser findUser(String tenantId, String authId) {
        if (tenantId == null || tenantId.equals("manual")) {
            return User.findByUsername(authId);
        } else {
            return User.findByOidc(tenantId, authId);
        }
    }

}
