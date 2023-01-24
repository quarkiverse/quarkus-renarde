package io.quarkiverse.renarde.backoffice;

import io.quarkiverse.renarde.Controller;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

public abstract class BackofficeController<Entity extends PanacheEntityBase> extends Controller {

}
