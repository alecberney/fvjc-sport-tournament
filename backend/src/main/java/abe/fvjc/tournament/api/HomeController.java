package abe.fvjc.tournament.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping({"/", "/{path:^(?!api).*$}", "/{path:^(?!api).*$}/**"})
    public String index() {
        return "forward:/static/index.html";
    }
}