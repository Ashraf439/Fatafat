package com.ashraf.shared.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Service
public class ImageStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5MB, mirrors multipart limit

    private final Cloudinary cloudinary;

    public ImageStorageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    /**
     * Uploads an image into the given Cloudinary folder (e.g. "restaurants/12", "menu/12").
     */
    public UploadedImage upload(MultipartFile file, String folder) {
        validate(file);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "image"
            ));
            return new UploadedImage((String) result.get("secure_url"), (String) result.get("public_id"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image: " + e.getMessage(), e);
        }
    }

    /**
     * Best-effort delete of a previous image. Safe to call with null/blank publicId.
     */
    public void delete(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete previous image: " + e.getMessage(), e);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Image must be smaller than 5MB.");
        }
        if (file.getContentType() == null || !ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Only JPEG, PNG, or WEBP images are allowed.");
        }
    }

    public record UploadedImage(String url, String publicId) {}
}