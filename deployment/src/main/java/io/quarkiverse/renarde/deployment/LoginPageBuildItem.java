package io.quarkiverse.renarde.deployment;

import java.util.List;

import io.quarkiverse.renarde.deployment.ControllerVisitor.QueryParamUriPart;
import io.quarkiverse.renarde.deployment.ControllerVisitor.StaticUriPart;
import io.quarkiverse.renarde.deployment.ControllerVisitor.UriPart;
import io.quarkus.builder.item.SimpleBuildItem;

public final class LoginPageBuildItem extends SimpleBuildItem {

    public final String uri;

    public LoginPageBuildItem(List<UriPart> parts) {
        StringBuilder sb = new StringBuilder();
        for (UriPart part : parts) {
            if (part instanceof StaticUriPart) {
                String p = ((StaticUriPart) part).part;
                if (p.isEmpty())
                    continue;
                if (!p.startsWith("/")) {
                    sb.append("/");
                }
                sb.append(p);
            } else if (part instanceof QueryParamUriPart) {
                // ignore
            } else {
                throw new RuntimeException("@LoginPage can only be placed on controller methods without any path parameter");
            }
        }
        this.uri = sb.toString();
    }

}
