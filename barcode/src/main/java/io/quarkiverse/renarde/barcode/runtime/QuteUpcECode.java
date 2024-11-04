package io.quarkiverse.renarde.barcode.runtime;

import io.quarkiverse.barcode.zxing.ZebraCrossing;
import io.quarkus.qute.EngineConfiguration;
import io.quarkus.qute.SectionHelperFactory;

@EngineConfiguration
public class QuteUpcECode extends QuteBarCode implements SectionHelperFactory<QuteBarCode.CustomSectionHelper> {

    public QuteUpcECode() {
        super("upce", ZebraCrossing::upcEImg);
    }
}