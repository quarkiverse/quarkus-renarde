package io.quarkiverse.renarde.barcode.runtime;

import io.quarkiverse.barcode.zxing.ZebraCrossing;
import io.quarkus.qute.EngineConfiguration;
import io.quarkus.qute.SectionHelperFactory;

@EngineConfiguration
public class QuteEan13Code extends QuteBarCode implements SectionHelperFactory<QuteBarCode.CustomSectionHelper> {

    public QuteEan13Code() {
        super("ean13", ZebraCrossing::ean13Img);
    }
}