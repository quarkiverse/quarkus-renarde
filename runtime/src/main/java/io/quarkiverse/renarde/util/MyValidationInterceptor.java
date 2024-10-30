package io.quarkiverse.renarde.util;

import java.util.Set;

import io.quarkiverse.renarde.Controller;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.executable.ExecutableValidator;

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
            if(ctx.getTarget() instanceof Controller){
                // just collect them and go on
                validation.addErrors(violations);
            }else{
                throw new ResteasyReactiveViolationException(violations);
            }
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
