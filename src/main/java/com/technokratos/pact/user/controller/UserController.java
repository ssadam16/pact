package com.technokratos.pact.user.controller;

import com.technokratos.pact.file.dto.ImageUploadRequest;
import com.technokratos.pact.file.service.AvatarService;
import com.technokratos.pact.security.model.UserDetailsImpl;
import com.technokratos.pact.user.dto.ProfileEditRequest;
import com.technokratos.pact.user.dto.UserProfileResponse;
import com.technokratos.pact.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final AvatarService avatarService;

    @GetMapping("/{username}")
    public String getProfilePage(@PathVariable String username,
                                 @AuthenticationPrincipal UserDetailsImpl currentUser,
                                 Model model) {

        UserProfileResponse profile = userService.getProfile(username);

        model.addAttribute("isMy", profile.getId().equals(currentUser.getId()));
        model.addAttribute("profile", profile);

        return "user/profile";
    }

    @GetMapping("/{username}/edit")
    public String editProfilePage(@PathVariable String username,
                                  @AuthenticationPrincipal UserDetailsImpl currentUser,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {

        if (!username.equals(currentUser.getUsername())) {
            redirectAttributes.addFlashAttribute("error", "Вы не можете редактировать чужой профиль");
            return "redirect:/user/" + username;
        }

        UserProfileResponse profile = userService.getProfile(username);
        ProfileEditRequest editRequest = new ProfileEditRequest();
        editRequest.setName(profile.getName());

        model.addAttribute("editRequest", editRequest);
        model.addAttribute("profile", profile);

        return "user/profile-edit";
    }

    @PostMapping("/{username}/edit")
    public String updateProfile(@PathVariable String username,
                                @Valid @ModelAttribute("editRequest") ProfileEditRequest editRequest,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal UserDetailsImpl currentUser,
                                RedirectAttributes redirectAttributes,
                                Model model) {

        if (!username.equals(currentUser.getUsername())) {
            redirectAttributes.addFlashAttribute("error", "Вы не можете редактировать чужой профиль");
            return "redirect:/user/" + username;
        }

        if (bindingResult.hasErrors()) {
            UserProfileResponse profile = userService.getProfile(username);
            model.addAttribute("profile", profile);
            return "user/profile-edit";
        }

        try {
            userService.updateProfile(currentUser.getId(), editRequest);
            redirectAttributes.addFlashAttribute("success", "Профиль успешно обновлен!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/user/%s/edit".formatted(username);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении профиля: " + e.getMessage());
            return "redirect:/user/%s/edit".formatted(username);
        }

        return "redirect:/user/" + username;
    }

    @PostMapping("/avatar/delete")
    public String deleteAvatar(@AuthenticationPrincipal UserDetailsImpl currentUser,
                               RedirectAttributes redirectAttributes) {
        try {
            avatarService.deleteAvatar(currentUser.getId());
            redirectAttributes.addFlashAttribute("success", "Фото профиля успешно удалено!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка удаления: " + e.getMessage());
        }
        return "redirect:/user/%s/edit".formatted(currentUser.getUsername());
    }

    @PostMapping("/avatar/upload")
    public String uploadAvatar(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @Valid @ModelAttribute("avatar") ImageUploadRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.avatar",
                    bindingResult
            );
            return "redirect:/user/%s".formatted(currentUser.getUsername());
        }

        try {
            avatarService.uploadAvatar(currentUser.getId(), request.image());
            redirectAttributes.addFlashAttribute("success", "Фото профиля успешно обновлено!");

        } catch (Exception e) {
            log.error("Failed to upload image for user {}: {}", currentUser.getId(), e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Ошибка загрузки: " + e.getMessage());
        }

        return "redirect:/user/%s".formatted(currentUser.getUsername());
    }
}