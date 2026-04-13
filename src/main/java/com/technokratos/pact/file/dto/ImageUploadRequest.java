package com.technokratos.pact.file.dto;

import com.technokratos.pact.file.annotation.ImageFile;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record ImageUploadRequest(

        @NotNull(message = "Файл не выбран")
        @ImageFile
        MultipartFile image
) {
}
