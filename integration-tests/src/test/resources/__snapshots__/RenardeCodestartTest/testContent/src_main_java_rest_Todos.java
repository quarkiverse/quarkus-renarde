package rest;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestForm;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.CheckedTemplate;
import io.quarkiverse.renarde.Controller;
import model.Todo;

/**
 * This defines a REST controller, each method will be available under the "Classname/method" URI by convention
 */
public class Todos extends Controller {
    
    /**
     * This defines templates available in src/main/resources/templates/Classname/method.html by convention
     */
    @CheckedTemplate
    public static class Templates {
        /**
         * This specifies that the Todos/index.html template does not take any parameter
         */
        public static native TemplateInstance index();
        /**
         * This specifies that the Todos/todos.html template takes a todos parameter of type List&lt;Todo&gt;
         */
        public static native TemplateInstance todos(List<Todo> todos);
    }

    // This overrides the convention and makes this method available at "/renarde"
    @Path("/renarde")
    public TemplateInstance index() {
        // renders the Todos/index.html template
        return Templates.index();
    }

    public TemplateInstance todos() {
        // renders the Todos/todos.html template with our list of Todo entities
        return Templates.todos(Todo.listAll());
    }
    
    // Creates a POST action at Todos/add taking a form element named task
    @POST
    public void add(@RestForm @NotBlank String task) {
        // If validation fails, redirect to the todos page (with errors propagated)
        if(validationFailed()) {
            // redirect to the todos page by just calling the method: it does not return!
            todos();
        }
        // save the new Todo
        Todo todo = new Todo();
        todo.task = task;
        todo.persist();
        // redirect to the todos page
        todos();
    }
}