package io.quarkiverse.renarde.util;

import java.util.Locale;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.hibernate.validator.spi.messageinterpolation.LocaleResolver;
import org.hibernate.validator.spi.messageinterpolation.LocaleResolverContext;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;

@Singleton
public class RenardeValidationLocaleResolver implements LocaleResolver {

    @Inject
    I18N i18n;

    @Override
    public Locale resolve(LocaleResolverContext context) {
        final ManagedContext requestContext = Arc.container().requestContext();
        if (!requestContext.isActive() || i18n.getLocale() == null) {
            return null;
        }
        return i18n.getLocale();
    }

}
