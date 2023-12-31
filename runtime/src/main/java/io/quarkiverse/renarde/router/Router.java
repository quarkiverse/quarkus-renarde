package io.quarkiverse.renarde.router;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.Arc;

public class Router {

    public static <Target> URI getURI(Method0<Target> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target> URI getURI(Method0V<Target> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1> URI getURI(Method1<Target, P1> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1> URI getURI(Method1V<Target, P1> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2> URI getURI(Method2<Target, P1, P2> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2> URI getURI(Method2V<Target, P1, P2> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3> URI getURI(Method3<Target, P1, P2, P3> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3> URI getURI(Method3V<Target, P1, P2, P3> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4> URI getURI(Method4<Target, P1, P2, P3, P4> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4> URI getURI(Method4V<Target, P1, P2, P3, P4> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5> URI getURI(Method5<Target, P1, P2, P3, P4, P5> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5> URI getURI(Method5V<Target, P1, P2, P3, P4, P5> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5, P6> URI getURI(Method6<Target, P1, P2, P3, P4, P5, P6> method,
            Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5, P6> URI getURI(Method6V<Target, P1, P2, P3, P4, P5, P6> method,
            Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5, P6, P7> URI getURI(Method7<Target, P1, P2, P3, P4, P5, P6, P7> method,
            Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5, P6, P7> URI getURI(Method7V<Target, P1, P2, P3, P4, P5, P6, P7> method,
            Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5, P6, P7, P8> URI getURI(Method8<Target, P1, P2, P3, P4, P5, P6, P7, P8> method,
            Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5, P6, P7, P8> URI getURI(Method8V<Target, P1, P2, P3, P4, P5, P6, P7, P8> method,
            Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5, P6, P7, P8, P9> URI getURI(
            Method9<Target, P1, P2, P3, P4, P5, P6, P7, P8, P9> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5, P6, P7, P8, P9> URI getURI(
            Method9V<Target, P1, P2, P3, P4, P5, P6, P7, P8, P9> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10> URI getURI(
            Method10<Target, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10> URI getURI(
            Method10V<Target, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11> URI getURI(
            Method11<Target, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11> URI getURI(
            Method11V<Target, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12> URI getURI(
            Method12<Target, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12> URI getURI(
            Method12V<Target, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13> URI getURI(
            Method13<Target, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13> URI getURI(
            Method13V<Target, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target> URI getAbsoluteURI(Method0<Target> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target> URI getAbsoluteURI(Method0V<Target> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1> URI getAbsoluteURI(Method1<Target, P1> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1> URI getAbsoluteURI(Method1V<Target, P1> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2> URI getAbsoluteURI(Method2<Target, P1, P2> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2> URI getAbsoluteURI(Method2V<Target, P1, P2> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3> URI getAbsoluteURI(Method3<Target, P1, P2, P3> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3> URI getAbsoluteURI(Method3V<Target, P1, P2, P3> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4> URI getAbsoluteURI(Method4<Target, P1, P2, P3, P4> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4> URI getAbsoluteURI(Method4V<Target, P1, P2, P3, P4> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5> URI getAbsoluteURI(Method5<Target, P1, P2, P3, P4, P5> method,
            Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5> URI getAbsoluteURI(Method5V<Target, P1, P2, P3, P4, P5> method,
            Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5, P6> URI getAbsoluteURI(Method6<Target, P1, P2, P3, P4, P5, P6> method,
            Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5, P6> URI getAbsoluteURI(Method6V<Target, P1, P2, P3, P4, P5, P6> method,
            Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5, P6, P7> URI getAbsoluteURI(Method7<Target, P1, P2, P3, P4, P5, P6, P7> method,
            Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5, P6, P7> URI getAbsoluteURI(Method7V<Target, P1, P2, P3, P4, P5, P6, P7> method,
            Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5, P6, P7, P8> URI getAbsoluteURI(
            Method8<Target, P1, P2, P3, P4, P5, P6, P7, P8> method,
            Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5, P6, P7, P8> URI getAbsoluteURI(
            Method8V<Target, P1, P2, P3, P4, P5, P6, P7, P8> method,
            Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5, P6, P7, P8, P9> URI getAbsoluteURI(
            Method9<Target, P1, P2, P3, P4, P5, P6, P7, P8, P9> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5, P6, P7, P8, P9> URI getAbsoluteURI(
            Method9V<Target, P1, P2, P3, P4, P5, P6, P7, P8, P9> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10> URI getAbsoluteURI(
            Method10<Target, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10> URI getAbsoluteURI(
            Method10V<Target, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11> URI getAbsoluteURI(
            Method11<Target, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11> URI getAbsoluteURI(
            Method11V<Target, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12> URI getAbsoluteURI(
            Method12<Target, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12> URI getAbsoluteURI(
            Method12V<Target, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13> URI getAbsoluteURI(
            Method13<Target, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13> method, Object... params) {
        return findURI(method, params);
    }

    public static <Target, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13> URI getAbsoluteURI(
            Method13V<Target, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13> method, Object... params) {
        return findURI(method, params);
    }

    private static URI findURI(Object method, Object... params) {
        // make sure all the calls are instrumented
        throw new RuntimeException("This call should have been instrumented away");
    }

    public static URI findURI(String route, boolean absolute, Object... params) {
        // This is only used by the views
        RouterMethod routerMethod = routerMethods.get(route);
        if (routerMethod == null)
            throw new RuntimeException("No route defined for " + route);
        return routerMethod.getRoute(absolute, params);
    }

    private static Map<String, RouterMethod> routerMethods = new HashMap<>();

    // Called by generated class __RenardeInit
    public static void clearRoutes() {
        routerMethods.clear();
    }

    // Called by generated class __RenardeInit for each controller route
    public static void registerRoute(String route, RouterMethod method) {
        if (routerMethods.containsKey(route)) {
            System.err.println("WARNING: duplicate route registered for " + route);
        }
        routerMethods.put(route, method);
    }

    // Used by generated bytecode for each controller method, which has a corresponding method to build a URI to it (it's only called in testing)
    public static UriBuilder getTestUriBuilder(boolean absolute) {
        Config config = ConfigProvider.getConfig();
        var uri = "http://" + config.getConfigValue("quarkus.http.host").getValue() + ":"
                + config.getConfigValue("quarkus.http.test-port").getValue();
        UriBuilder uriBuilder = UriBuilder.fromUri(uri);
        return absolute ? uriBuilder : uriBuilder.host(null).port(-1).scheme(null);
    }

    // Used by generated bytecode for each controller method, which has a corresponding method to build a URI to it
    public static UriBuilder getUriBuilder(boolean absolute) {
        UriInfo uriInfo = Arc.container().instance(UriInfo.class).get();
        UriBuilder ret = absolute ? uriInfo.getBaseUriBuilder()
                : uriInfo.getBaseUriBuilder().host(null).port(-1).scheme(null);
        return ret;
    }

    // Called by the URI-generating methods of controllers, for Optional parameters
    public static Optional ofNullable(Object o) {
        if (o instanceof Optional) {
            return (Optional) o;
        }
        return Optional.ofNullable(o);
    }
}
