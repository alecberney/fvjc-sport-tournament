package abe.fvjc.tournament.api;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    // Catch all non-API, non-static-file routes and serve index.html for Angular SPA routing
    @GetMapping(value = {
        "/",
        "/{path:^(?!api)(?!.*\\..+$).*$}",
        "/{path:^(?!api)(?!.*\\..+$).*$}/**"
    })
    public ResponseEntity<Resource> index() {
        Resource resource = new ClassPathResource("static/index.html");
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(resource);
    }
}