package com.technokratos.pact.file.service;

import com.technokratos.pact.file.dto.FileInfo;
import com.technokratos.pact.file.exception.FileDeleteException;
import com.technokratos.pact.file.exception.FileNotFoundException;
import com.technokratos.pact.file.exception.FileUploadException;
import com.technokratos.pact.file.exception.FileValidationException;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucket;

    @Value("${minio.public-url-expiry}")
    private int publicUrlExpiryDays;

    @Value("${minio.allowed-extensions}")
    private List<String> allowedExtensions;

    public static class Folders {
        public static final String AVATARS = "avatars";

    }

    @PostConstruct
    public void init() {
        try {
            boolean isExists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucket)
                            .build()
            );

            if (!isExists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucket)
                                .region("us-east-1")
                                .build()
                );

                String policy = """
                    {
                        "Version": "2012-10-17",
                        "Statement": [
                            {
                                "Effect": "Allow",
                                "Principal": "*",
                                "Action": ["s3:GetObject"],
                                "Resource": ["arn:aws:s3:::%s/*"]
                            }
                        ]
                    }
                    """.formatted(bucket);

                minioClient.setBucketPolicy(
                        SetBucketPolicyArgs.builder()
                                .bucket(bucket)
                                .config(policy)
                                .build()
                );

                log.info("Bucket '{}' was successfully created", bucket);
            } else {
                log.info("Bucket '{}' is already exists", bucket);
            }
        } catch (Exception e) {
            log.error("Error while initializing MinIO: {}", e.getMessage(), e);
            throw new RuntimeException("Cannot initialize MinIO", e);
        }
    }

    public FileInfo uploadFile(MultipartFile file, String folder) {
        validateFile(file);
        
        String originalFilename = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(originalFilename);
        String uniqueFilename = generateUniqueFilename(originalFilename);
        String objectName = "%s/%s".formatted(folder, uniqueFilename);

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("File '{}' was successfully uploaded to '{}'", originalFilename, objectName);

            return FileInfo.builder()
                    .originalName(originalFilename)
                    .storedName(uniqueFilename)
                    .path(objectName)
                    .size(file.getSize())
                    .contentType(file.getContentType())
                    .extension(extension)
                    .uploadedAt(ZonedDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Error while uploading file '{}': {}", originalFilename, e.getMessage());
            throw new FileUploadException("Cannot upload the file", e);
        }
    }

    public String getFileUrl(String filePath) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(filePath)
                            .expiry(publicUrlExpiryDays)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error while getting URL for file '{}': {}", filePath, e.getMessage());
            return null;
        }
    }

    public InputStream downloadFile(String filePath) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(filePath)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error while downloading file '{}': {}", filePath, e.getMessage());
            throw new FileNotFoundException("File not found: %s".formatted(filePath), e);
        }
    }

    public void deleteFile(String filePath) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(filePath)
                            .build()
            );
            log.info("File '{}' was successfully deleted", filePath);
        } catch (Exception e) {
            log.error("Error while deleting file '{}': {}", filePath, e.getMessage());
            throw new FileDeleteException("Cannot delete the file", e);
        }
    }

    public boolean fileExists(String filePath) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(filePath)
                            .build()
            );
            return true;
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return false;
            }
            throw new RuntimeException("Error while checking the file", e);
        } catch (Exception e) {
            throw new RuntimeException("Error while checking the file", e);
        }
    }

    public List<FileInfo> listFiles(String folder) {
        List<FileInfo> files = new ArrayList<>();

        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucket)
                            .prefix(folder + "/")
                            .recursive(true)
                            .build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();
                if (!item.isDir()) {
                    files.add(FileInfo.builder()
                            .storedName(FilenameUtils.getName(item.objectName()))
                            .path(item.objectName())
                            .size(item.size())
                            .lastModified(item.lastModified())
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("Error while getting list of files: {}", e.getMessage());
        }

        return files;
    }

    private String generateUniqueFilename(String originalFilename) {
        String extension = FilenameUtils.getExtension(originalFilename);
        String baseName = FilenameUtils.getBaseName(originalFilename);
        
        baseName = transliterate(baseName);
        baseName = baseName.replaceAll("[^a-zA-Z0-9-_]", "");

        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0,8);

        return "%s_%s_%s.%s".formatted(baseName, timestamp, uuid, extension);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileValidationException("File is empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new FileValidationException("File name not specified");
        }

        String extension = FilenameUtils.getExtension(originalFilename).toLowerCase();
        if (!allowedExtensions.contains(extension)) {
            throw new FileValidationException(
                    "Invalid file extension. Allowed: " + allowedExtensions
            );
        }

        if (file.getSize() > 10 * 1024 * 1024) { // 10MB
            throw new FileValidationException("The file is too large. Maximum size: 10MB");
        }
    }

    private String transliterate(String text) {
        Map<Character, String> translitMap = Map.ofEntries(
                Map.entry('а', "a"), Map.entry('б', "b"), Map.entry('в', "v"),
                Map.entry('г', "g"), Map.entry('д', "d"), Map.entry('е', "e"),
                Map.entry('ё', "yo"), Map.entry('ж', "zh"), Map.entry('з', "z"),
                Map.entry('и', "i"), Map.entry('й', "y"), Map.entry('к', "k"),
                Map.entry('л', "l"), Map.entry('м', "m"), Map.entry('н', "n"),
                Map.entry('о', "o"), Map.entry('п', "p"), Map.entry('р', "r"),
                Map.entry('с', "s"), Map.entry('т', "t"), Map.entry('у', "u"),
                Map.entry('ф', "f"), Map.entry('х', "kh"), Map.entry('ц', "ts"),
                Map.entry('ч', "ch"), Map.entry('ш', "sh"), Map.entry('щ', "shch"),
                Map.entry('ы', "y"), Map.entry('э', "e"), Map.entry('ю', "yu"),
                Map.entry('я', "ya")
        );

        StringBuilder result = new StringBuilder();
        for (char c : text.toLowerCase().toCharArray()) {
            result.append(translitMap.getOrDefault(c, String.valueOf(c)));
        }
        return result.toString();
    }

    public String getAvatarUrl(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }

        try {
            String objectPath = Folders.AVATARS + "/" + filename;

            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectPath)
                            .expiry(7, TimeUnit.DAYS)
                            .build()
            );

        } catch (Exception e) {
            log.error("Error generating avatar URL for {}: {}", filename, e.getMessage());
            return null;
        }
    }
}
