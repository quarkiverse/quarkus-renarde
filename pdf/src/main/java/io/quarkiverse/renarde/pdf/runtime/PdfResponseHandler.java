package io.quarkiverse.renarde.pdf.runtime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;

import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.resteasy.reactive.server.core.BlockingOperationSupport;

import com.openhtmltopdf.pdfboxout.PdfBoxRenderer;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.util.Diagnostic;
import com.openhtmltopdf.util.XRLog;
import com.openhtmltopdf.util.XRLogger;

import io.quarkiverse.renarde.pdf.Pdf;
import io.quarkus.runtime.ExecutorRecorder;
import io.smallrye.mutiny.Uni;

// NOTE: name used by reflection in RenardeProcessor
public class PdfResponseHandler {
    private final static Logger logger = Logger.getLogger(Pdf.class);

    /*
     * For some reason, this is not automatically caught by JBoss Logging, but we also want to downgrade its info
     * level which is too verbose for us.
     */
    static {
        XRLog.setLoggerImpl(new XRLogger() {
            @Override
            public void log(String where, Level level, String msg) {
                // FIXME: dropped where because if I set it, it appears as <unknown>" in the logs and I can't even enable the logs
                logger.log(translate(level), msg);
            }

            @Override
            public void log(String where, Level level, String msg, Throwable th) {
                // FIXME: dropped where because if I set it, it appears as <unknown>" in the logs and I can't even enable the logs
                logger.log(translate(level), msg, th);
            }

            private org.jboss.logging.Logger.Level translate(Level level) {
                // downgrade INFO which is too verbose
                if (level == Level.INFO)
                    return Logger.Level.DEBUG;
                // the rest is a best guess
                if (level == Level.WARNING)
                    return Logger.Level.WARN;
                if (level == Level.SEVERE)
                    return Logger.Level.ERROR;
                if (level == Level.FINE
                        || level == Level.FINER)
                    return Logger.Level.TRACE;
                if (level == Level.FINEST)
                    return Logger.Level.DEBUG;
                return Logger.Level.INFO;
            }

            @Override
            public void setLevel(String logger, Level level) {
            }

            @Override
            public boolean isLogLevelEnabled(Diagnostic diagnostic) {
                return logger.isEnabled(translate(diagnostic.getLevel()));
            }
        });
    }

    @ServerResponseFilter
    public Uni<Void> filter(ContainerResponseContext responseContext, UriInfo uriInfo) throws IOException {
        Object entity = responseContext.getEntity();
        if (responseContext.getStatus() == 200
                && entity instanceof String
                && responseContext.getMediaType() != null
                && responseContext.getMediaType().equals(Pdf.APPLICATION_PDF_TYPE)) {
            String baseURI = uriInfo.getBaseUri().toString();
            // this is all blocking because it can do blocking HTTP calls
            if (BlockingOperationSupport.isBlockingAllowed()) {
                renderPdf((String) entity, baseURI, responseContext);
                return Uni.createFrom().nullItem();
            } else {
                return Uni.createFrom().<Void> item(() -> {
                    renderPdf((String) entity, baseURI, responseContext);
                    return null;
                }).runSubscriptionOn(ExecutorRecorder.getCurrent());
            }
        } else {
            return Uni.createFrom().nullItem();
        }
    }

    private void renderPdf(String entity, String baseURI, ContainerResponseContext responseContext) {
        PdfRendererBuilder builder = new PdfRendererBuilder();
        // FIXME: use this to avoid going over HTTP for static files?
        //            builder.useHttpStreamImplementation(new FSStreamFactory() {
        //			@Override
        //			public FSStream getUrl(String url) {
        //				// TODO Auto-generated method stub
        //				return null;
        //			}
        //		});
        builder.withHtmlContent(entity, baseURI);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        builder.toStream(out);
        PdfBoxRenderer renderer = builder.buildPdfRenderer();
        try {
            renderer.createPDF();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        responseContext.setEntity(out.toByteArray(), null, Pdf.APPLICATION_PDF_TYPE);
    }
}
