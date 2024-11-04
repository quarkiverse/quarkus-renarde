package io.quarkiverse.renarde.barcode.runtime;

import io.quarkiverse.barcode.zxing.ZebraCrossing;
import io.quarkus.qute.EngineConfiguration;
import io.quarkus.qute.SectionHelperFactory;

@EngineConfiguration
public class QuteCode39Code extends QuteBarCode implements SectionHelperFactory<QuteBarCode.CustomSectionHelper> {

    public QuteCode39Code() {
        super("code39", ZebraCrossing::code39Img);
    }
}