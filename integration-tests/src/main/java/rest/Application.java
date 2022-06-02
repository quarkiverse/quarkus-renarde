/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package rest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.router.Router;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

public class Application extends Controller {

    @CheckedTemplate
    static class Templates {
        public static native TemplateInstance index();

        public static native TemplateInstance routingTags();
    }

    public static class DamnMultiPart {
        @RestForm
        File file;
        @RestForm
        public FileUpload fileUpload;
    }

    // FIXME: https://github.com/quarkusio/quarkus/issues/22205
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @POST
    public String form(@RestForm String param,
            @MultipartForm DamnMultiPart damnit) throws IOException {
        return "param: " + param + ", file: " + Files.readString(damnit.file.toPath()) + ", fileUpload: "
                + damnit.fileUpload.fileName();
    }

    public String hello() {
        return "Hello Renarde";
    }

    public TemplateInstance index() {
        return Templates.index();
    }

    public TemplateInstance routingTags() {
        return Templates.routingTags();
    }

    @Path("/absolute")
    public String absolutePath() {
        return "Absolute";
    }

    public String params(@RestPath String a, @RestPath Long id, @RestQuery String q) {
        return "Got params: " + a + "/" + id + "/" + q;
    }

    public String primitiveParams(@RestQuery boolean b,
            @RestQuery char c,
            @RestQuery byte bite,
            @RestQuery short s,
            @RestQuery int i,
            @RestQuery long l,
            @RestQuery float f,
            @RestQuery double d) {
        return "Got params: " + b + "/" + c + "/" + bite + "/" + s + "/" + i + "/" + l + "/" + f + "/" + d;
    }

    public String router() {
        return Router.getURI(Application::absolutePath)
                + "\n" + Router.getURI(Application::index)
                + "\n" + Router.getURI(Application::params, "first", 42l, "search")
                + "\n" + Router.getURI(Application::primitiveParams, true, 'a', (byte) 2, (short) 3, 4, 5l, 6.0f, 7.0d);
    }
}
