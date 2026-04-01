package com.technokratos.pact.auth.controller;

import com.technokratos.pact.auth.service.AuthService;
import com.technokratos.pact.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("user")) {
            model.addAttribute(
                    "user",
                    new RegisterRequest("", "", "", "")
            );
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegistration(
            @Valid @ModelAttribute("user") RegisterRequest registerRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.user",
                    bindingResult
            );
            redirectAttributes.addFlashAttribute("user", registerRequest);

            return "redirect:/auth/register";
        }

        try {
            authService.register(registerRequest);

            redirectAttributes.addFlashAttribute("success",
                    "Регистрация успешна! Теперь вы можете войти в систему.");
            redirectAttributes.addFlashAttribute("username", registerRequest.username());

            return "redirect:/auth/login";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Ошибка регистрации: %s".formatted(e.getMessage()));
            redirectAttributes.addFlashAttribute("user", registerRequest);

            return "redirect:/auth/register";
        }
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }
}
