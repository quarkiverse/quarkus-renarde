package com.openhtmltopdf.java2d;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.sheet.FontFaceRule;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.FSDerivedValue;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.outputdevice.helper.ExternalResourceType;
import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.XRLog;

/**
 * Extends Java2DFontResolver to cache Font objects created from @font-face
 * rules. Without caching, each render calls Font.createFont() which registers
 * a new TrueTypeFont in JDK's SunFontManager that is never released, causing
 * a memory leak in native image (and to a lesser extent on the JVM).
 */
public class CachingJava2DFontResolver extends Java2DFontResolver {

    private static final ConcurrentHashMap<String, Font> fontCache = new ConcurrentHashMap<>();
    private final SharedContext sharedCtx;

    public CachingJava2DFontResolver(SharedContext sharedContext, boolean useEnvironmentFonts) {
        super(sharedContext, useEnvironmentFonts);
        this.sharedCtx = sharedContext;
    }

    @Override
    public void importFontFaces(List<FontFaceRule> fontFaces) {
        for (FontFaceRule rule : fontFaces) {
            CalculatedStyle style = rule.getCalculatedStyle();

            FSDerivedValue src = style.valueByName(CSSName.SRC);
            if (src == IdentValue.NONE) {
                continue;
            }

            if (!rule.hasFontFamily()) {
                continue;
            }

            String fontFamily = style.valueByName(CSSName.FONT_FAMILY).asString();
            String uri = src.asString();

            Font cachedFont = fontCache.computeIfAbsent(uri, k -> loadFont(k));

            if (cachedFont != null) {
                setFontMapping(fontFamily, cachedFont);
            }
        }
    }

    private Font loadFont(String uri) {
        byte[] fontBytes = sharedCtx.getUserAgentCallback()
                .getBinaryResource(uri, ExternalResourceType.FONT);
        if (fontBytes == null) {
            XRLog.log(Level.WARNING,
                    LogMessageId.LogMessageId1Param.EXCEPTION_COULD_NOT_LOAD_FONT_FACE, uri);
            return null;
        }
        try {
            return Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(fontBytes));
        } catch (FontFormatException | IOException e) {
            XRLog.log(Level.WARNING,
                    LogMessageId.LogMessageId0Param.EXCEPTION_JAVA2D_COULD_NOT_LOAD_FONT, e);
            return null;
        }
    }
}
