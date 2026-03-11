package br.com.mypersonalmoney.ui;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/login")
public class LoginResource {

    @Inject
    @Location("ui/login.html")
    Template login;

    @GET
    public TemplateInstance login() {
        return login.instance();
    }
}