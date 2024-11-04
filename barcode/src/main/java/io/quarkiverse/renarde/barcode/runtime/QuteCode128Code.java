package io.quarkiverse.renarde.barcode.runtime;

import io.quarkiverse.barcode.zxing.ZebraCrossing;
import io.quarkus.qute.EngineConfiguration;
import io.quarkus.qute.SectionHelperFactory;

@EngineConfiguration
public class QuteCode128Code extends QuteBarCode implements SectionHelperFactory<QuteBarCode.CustomSectionHelper> {

    public QuteCode128Code() {
        super("code128", ZebraCrossing::code128Img);
    }
}