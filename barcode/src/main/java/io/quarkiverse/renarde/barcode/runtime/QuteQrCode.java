package io.quarkiverse.renarde.barcode.runtime;

import io.quarkiverse.barcode.zxing.ZebraCrossing;
import io.quarkus.qute.EngineConfiguration;
import io.quarkus.qute.SectionHelperFactory;

@EngineConfiguration
public class QuteQrCode extends QuteBarCode implements SectionHelperFactory<QuteBarCode.CustomSectionHelper> {

    public QuteQrCode() {
        super("qrcode", ZebraCrossing::qrCodeImg);
    }
}