package com.technokratos.pact.article.controller;

import com.technokratos.pact.article.dto.ArticleCreateRequest;
import com.technokratos.pact.article.service.ArticleService;
import com.technokratos.pact.game.service.GameService;
import com.technokratos.pact.security.model.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/article")
public class ArticleController {

    private final ArticleService articleService;
    private final GameService gameService;

    @GetMapping("/create")
    public String createPage(Model model) {
        model.addAttribute("articleCreateRequest", new ArticleCreateRequest(null, null, null));
        model.addAttribute("games", gameService.findAll());
        return "article/create";
    }

    @PostMapping("/create")
    public String createArticle(
            @Valid @ModelAttribute("article") ArticleCreateRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.article",
                    bindingResult
            );
            redirectAttributes.addFlashAttribute("article", request);

            return "redirect:/article/create";
        }

        UUID savedArticleId = articleService.create(request, currentUser.getId());

        return "redirect:/article/%s".formatted(savedArticleId);
    }

    @GetMapping("/{id}")
    public String articlePage(@PathVariable UUID id, Model model) {
        model.addAttribute("article", articleService.getArticle(id));
        return "article/article";
    }
}