package util;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.transaction.Transactional;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.runtime.StartupEvent;
import model.User;

@ApplicationScoped
public class Startup {
    @Transactional
    public void start(@Observes StartupEvent evt) {
        User user = new User();
        user.username = "FroMage";
        user.password = BcryptUtil.bcryptHash("1q2w3e");
        user.persist();
    }
}