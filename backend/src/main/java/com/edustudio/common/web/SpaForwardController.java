package com.edustudio.common.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards browser routes to the bundled Vue entry page in release builds.
 */
@Controller
public class SpaForwardController {

    @GetMapping({
            "/", "/login", "/register", "/dashboard", "/spaces", "/knowledge",
            "/resource-generation", "/resource-generator", "/resources", "/quiz",
            "/reports", "/report", "/chat", "/roles", "/profile", "/models",
            "/agents", "/graph", "/paths", "/help"
    })
    public String forwardToApplication() {
        return "forward:/index.html";
    }
}
