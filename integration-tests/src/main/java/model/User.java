package model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.quarkiverse.renarde.security.RenardeUserWithPassword;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Table(name = "user_entity")
@Entity
public class User extends PanacheEntity implements RenardeUserWithPassword {
    public String username;
    public String password;
    public String roles;
    // this is just for the backoffice UI
    public boolean needsHashing;

    // this is just for the backoffice UI
    @Override
    public void persist() {
        if (needsHashing) {
            needsHashing = false;
            password = BcryptUtil.bcryptHash(password);
        }
        super.persist();
    }

    @Override
    public Set<String> roles() {
        if (roles == null)
            return Collections.emptySet();
        return new HashSet<>(Arrays.asList(roles.split(",")));
    }

    @Override
    public String userId() {
        return username;
    }

    @Override
    public boolean registered() {
        return true;
    }

    public static User findByUsername(String username) {
        return find("username", username).firstResult();
    }

    @Override
    public String password() {
        return password;
    }
}
