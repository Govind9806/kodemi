package com.example.user_service.dto.request;

import org.springframework.web.multipart.MultipartFile;

public record TrainerProfileFiles(
    MultipartFile demo,
    MultipartFile profileImage,
    MultipartFile globalCertifications,
    MultipartFile totRegistration,
    MultipartFile supportingDocuments
) {}
