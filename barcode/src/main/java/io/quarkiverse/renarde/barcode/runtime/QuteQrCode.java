package io.quarkiverse.renarde.barcode.runtime;

import io.quarkiverse.renarde.barcode.Barcode;
import io.quarkus.qute.EngineConfiguration;
import io.quarkus.qute.SectionHelperFactory;

@EngineConfiguration
public class QuteQrCode extends QuteBarCode implements SectionHelperFactory<QuteBarCode.CustomSectionHelper> {

    public QuteQrCode() {
        super("qrcode", Barcode::qrCodeImg);
    }
}