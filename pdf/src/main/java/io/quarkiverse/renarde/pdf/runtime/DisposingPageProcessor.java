package io.quarkiverse.renarde.pdf.runtime;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import com.openhtmltopdf.java2d.api.FSPage;
import com.openhtmltopdf.java2d.api.FSPageOutputStreamSupplier;
import com.openhtmltopdf.java2d.api.FSPageProcessor;
import com.openhtmltopdf.util.OpenUtil;

/**
 * Like {@link com.openhtmltopdf.java2d.api.DefaultPageProcessor} but explicitly
 * flushes the BufferedImage after saving to release native surface resources.
 * Without this, JNI global refs held by AWT/Java2D surface data accumulate
 * and leak memory in GraalVM native images.
 */
class DisposingPageProcessor implements FSPageProcessor {

    private final FSPageOutputStreamSupplier _osFactory;
    private final int _imageType;
    private final String _imageFormat;

    DisposingPageProcessor(FSPageOutputStreamSupplier osFactory, int imageType, String imageFormat) {
        _osFactory = osFactory;
        _imageType = imageType;
        _imageFormat = imageFormat;
    }

    @Override
    public FSPage createPage(int zeroBasedPageNumber, int width, int height) {
        return new DisposingPage(zeroBasedPageNumber, width, height, _osFactory, _imageType, _imageFormat);
    }

    @Override
    public void finishPage(FSPage pg) {
        DisposingPage page = (DisposingPage) pg;
        page.getGraphics().dispose();
        page.save();
        page.flush();
    }

    static class DisposingPage implements FSPage {
        private final BufferedImage _img;
        private final Graphics2D _g2d;
        private final int _pgNo;
        private final FSPageOutputStreamSupplier _osf;
        private final String _imgFrmt;

        DisposingPage(int pgNo, int w, int h, FSPageOutputStreamSupplier osFactory, int imageType,
                String imageFormat) {
            _img = new BufferedImage(w, h, imageType);
            _g2d = _img.createGraphics();

            try {
                if (_img.getColorModel().hasAlpha()) {
                    _g2d.setBackground(new Color(255, 255, 255, 0));
                    _g2d.clearRect(0, 0, _img.getWidth(), _img.getHeight());
                } else {
                    _g2d.setColor(Color.WHITE);
                    _g2d.fillRect(0, 0, _img.getWidth(), _img.getHeight());
                }

                _pgNo = pgNo;
                _osf = osFactory;
                _imgFrmt = imageFormat;
            } catch (Throwable e) {
                _g2d.dispose();
                throw e;
            }
        }

        @Override
        public Graphics2D getGraphics() {
            return _g2d;
        }

        void save() {
            OutputStream os = null;
            try {
                os = _osf.supply(_pgNo);
                ImageIO.write(_img, _imgFrmt, os);
            } catch (IOException e) {
                throw new RuntimeException("Couldn't write page image to output stream", e);
            } finally {
                OpenUtil.closeQuietly(os);
            }
        }

        void flush() {
            _img.flush();
        }
    }
}
