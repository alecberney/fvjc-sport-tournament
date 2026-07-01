package abe.fvjc.tournament.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
class HelloController {

    @GetMapping("/hello")
    Map<String, String> hello() {
        return Map.of("message", "Hello FVJC!");
    }
}
