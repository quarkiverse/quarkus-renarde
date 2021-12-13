package io.quarkiverse.renarde.util;

import java.util.Set;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.executable.ExecutableValidator;

import io.quarkus.hibernate.validator.runtime.interceptor.AbstractMethodValidationInterceptor;
import io.quarkus.hibernate.validator.runtime.jaxrs.JaxrsEndPointValidated;
import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyReactiveViolationException;

@JaxrsEndPointValidated
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_AFTER + 700)
public class MyValidationInterceptor extends AbstractMethodValidationInterceptor {

    @Inject
    Validator validator;

    @Inject
    Validation validation;

    @AroundInvoke
    @Override
    public Object validateMethodInvocation(InvocationContext ctx) throws Exception {
        ExecutableValidator executableValidator = validator.forExecutables();
        Set<ConstraintViolation<Object>> violations = executableValidator.validateParameters(ctx.getTarget(),
                ctx.getMethod(), ctx.getParameters());

        if (!violations.isEmpty()) {
            // just collect them and go on
            validation.addErrors(violations);
        }

        Object result = ctx.proceed();

        violations = executableValidator.validateReturnValue(ctx.getTarget(), ctx.getMethod(), result);

        if (!violations.isEmpty()) {
            throw new ResteasyReactiveViolationException(violations);
        }

        return result;
    }

    @AroundConstruct
    @Override
    public void validateConstructorInvocation(InvocationContext ctx) throws Exception {
        super.validateConstructorInvocation(ctx);
    }
}
