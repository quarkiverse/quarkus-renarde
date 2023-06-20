package io.quarkiverse.renarde.barcode.runtime;

import io.quarkiverse.renarde.barcode.Barcode;
import io.quarkus.qute.EngineConfiguration;
import io.quarkus.qute.SectionHelperFactory;

@EngineConfiguration
public class QuteCode93Code extends QuteBarCode implements SectionHelperFactory<QuteBarCode.CustomSectionHelper> {

    public QuteCode93Code() {
        super("code93", Barcode::code93Img);
    }
}