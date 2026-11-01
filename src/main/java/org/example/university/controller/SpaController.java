package org.example.university.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping(value = {"/{path:^(?!api|assets|index\\.html).*$}", "/{path:^(?!api|assets|index\\.html).*$}/**"})
    public String forward() {
        return "forward:/index.html";
    }
}
