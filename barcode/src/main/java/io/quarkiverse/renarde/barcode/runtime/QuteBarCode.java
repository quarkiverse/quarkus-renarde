package io.quarkiverse.renarde.barcode.runtime;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import io.quarkus.qute.Expression;
import io.quarkus.qute.Parameter;
import io.quarkus.qute.ResultNode;
import io.quarkus.qute.Scope;
import io.quarkus.qute.SectionHelper;
import io.quarkus.qute.SectionHelperFactory;
import io.quarkus.qute.SingleResultNode;
import io.quarkus.qute.TemplateException;

public abstract class QuteBarCode implements SectionHelperFactory<QuteBarCode.CustomSectionHelper> {

    @FunctionalInterface
    public static interface BarCodeEncoder {
        String encode(String value, int width, int height);
    }

    private String name;
    private BarCodeEncoder encoder;

    public QuteBarCode(String name, BarCodeEncoder encoder) {
        this.name = name;
        this.encoder = encoder;
    }

    @Override
    public List<String> getDefaultAliases() {
        return List.of(name);
    }

    @Override
    public ParametersInfo getParameters() {
        return ParametersInfo.builder()
                .addParameter(Parameter.builder("value"))
                .addParameter(Parameter.builder("size").optional())
                .addParameter(Parameter.builder("width").optional())
                .addParameter(Parameter.builder("height").optional())
                .build();
    }

    @Override
    public Scope initializeBlock(Scope outerScope, BlockInfo block) {
        Util.declareBlock(block, "value", "size", "width", "height");
        return SectionHelperFactory.super.initializeBlock(outerScope, block);
    }

    @Override
    public CustomSectionHelper initialize(SectionInitContext context) {
        Util.requireParameter(context, "value");
        // FIXME: support compile-time type-checking when Qute supports it
        Map<String, Expression> params = Util.collectExpressions(context, "value", "size", "width", "height");
        if (context.hasParameter("size")) {
            if (params.containsKey("width") || params.containsKey("height")) {
                throw new TemplateException("Cannot set both size and (width or height): choose one the others");
            }
        }
        return new CustomSectionHelper(params, encoder);
    }

    static class CustomSectionHelper implements SectionHelper {

        private Map<String, Expression> params;
        private BarCodeEncoder encoder;

        public CustomSectionHelper(Map<String, Expression> params, BarCodeEncoder encoder) {
            this.params = params;
            this.encoder = encoder;
        }

        @Override
        public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
            return Futures.evaluateParams(params, context.resolutionContext())
                    .thenApply(values -> {
                        String value = Util.typecheckValue(values, "value", String.class);
                        Integer size = Util.typecheckValue(values, "size", Integer.class, 200);
                        Integer width = Util.typecheckValue(values, "width", Integer.class, size);
                        Integer height = Util.typecheckValue(values, "height", Integer.class, size);

                        return new SingleResultNode(encoder.encode(value, width, height));
                    });
        }
    }
}