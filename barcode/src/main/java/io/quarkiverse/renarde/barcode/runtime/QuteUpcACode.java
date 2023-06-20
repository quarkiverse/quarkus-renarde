package io.quarkiverse.renarde.barcode.runtime;

import io.quarkiverse.renarde.barcode.Barcode;
import io.quarkus.qute.EngineConfiguration;
import io.quarkus.qute.SectionHelperFactory;

@EngineConfiguration
public class QuteUpcACode extends QuteBarCode implements SectionHelperFactory<QuteBarCode.CustomSectionHelper> {

    public QuteUpcACode() {
        super("upca", Barcode::upcAImg);
    }
}