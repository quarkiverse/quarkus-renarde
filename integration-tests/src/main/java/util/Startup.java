package util;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
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
