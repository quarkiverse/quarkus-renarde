package io.quarkiverse.renarde.test;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.MediaType;

import io.restassured.RestAssured;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.filter.OrderedFilter;
import io.restassured.http.Cookie;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import io.restassured.specification.MultiPartSpecification;

public class CSRFFilter implements OrderedFilter {

    public static void install() {
        // only install ourselves once
        for (Filter filter : RestAssured.filters()) {
            // I really tried to find the proper CL, but had up to 3 instances with different CL. This works.
            if (filter.getClass().getName().equals(CSRFFilter.class.getName())) {
                return;
            }
        }
        RestAssured.filters(new CSRFFilter());
    }

    public static void deinstall() throws ClassNotFoundException {
        boolean needsDeinstall = false;
        for (Filter filter : RestAssured.filters()) {
            // I really tried to find the proper CL, but had up to 3 instances with different CL. This works.
            if (filter.getClass().getName().equals(CSRFFilter.class.getName())) {
                needsDeinstall = true;
                break;
            }
        }
        if (needsDeinstall) {
            List<Filter> newFilters = RestAssured.filters().stream()
                    // I really tried to find the proper CL, but had up to 3 instances with different CL. This works.
                    .filter(filter -> !filter.getClass().getName().equals(CSRFFilter.class.getName()))
                    .collect(Collectors.toList());
            RestAssured.replaceFiltersWith(newFilters);
        }
    }

    @Override
    public int getOrder() {
        // we want to be executed after any user-added CookieFilter, in case they handle CSRF themselves
        return OrderedFilter.LOWEST_PRECEDENCE;
    }

    private static boolean requestMethodIsSafe(String method) {
        switch (method) {
            case "GET":
            case "HEAD":
            case "OPTIONS":
                return true;
            default:
                return false;
        }
    }

    @Override
    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec,
            FilterContext ctx) {
        if (!requestMethodIsSafe(requestSpec.getMethod())
                && isValidPayload(requestSpec.getContentType())
                && !isAlreadyHandled(requestSpec)) {
            Cookie previousValue = requestSpec.getCookies().get(CSRF.getTokenCookieName());
            String token = previousValue != null ? previousValue.getValue() : CSRF.makeCSRFToken();
            requestSpec.cookie(CSRF.getTokenCookieName(), token);
            String contentType = requestSpec.getContentType();
            if (contentType == null
                    || isCompatible(MediaType.APPLICATION_FORM_URLENCODED, contentType)) {
                requestSpec.formParam(CSRF.getTokenFormName(), token);
            } else {
                requestSpec.multiPart(CSRF.getTokenFormName(), token);
            }
        }
        return ctx.next(requestSpec, responseSpec);
    }

    private boolean isAlreadyHandled(FilterableRequestSpecification requestSpec) {
        String formName = CSRF.getTokenFormName();
        String previousFormToken = requestSpec.getFormParams().get(formName);
        String previousMultipartToken = null;
        for (MultiPartSpecification part : requestSpec.getMultiPartParams()) {
            if (formName.equals(part.getControlName())) {
                previousMultipartToken = (String) part.getContent();
                break;
            }
        }
        return previousFormToken != null
                || previousMultipartToken != null;
    }

    private boolean isValidPayload(String contentType) {
        // no payload is also valid: we will inject it
        return contentType == null
                || isCompatible(MediaType.APPLICATION_FORM_URLENCODED, contentType)
                || isCompatible(MediaType.MULTIPART_FORM_DATA, contentType);
    }

    private boolean isCompatible(String validContentType, String checkedContentType) {
        return checkedContentType != null
                // exact
                && (validContentType.equals(checkedContentType)
                        // or with parameters
                        || checkedContentType.startsWith(validContentType + ";"));
    }

}
