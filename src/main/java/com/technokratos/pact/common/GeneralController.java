package com.technokratos.pact.common;

import com.technokratos.pact.security.model.UserDetailsImpl;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class GeneralController {

    @GetMapping
    public String indexPage() {
        return "index";
    }

    @GetMapping("/egg/scp")
    public String scpPage(@AuthenticationPrincipal UserDetailsImpl currentUser) {
        if (currentUser == null) {
            return "egg/forbidden-scp";
        } else {
            return "egg/scp";
        }
    }
}
