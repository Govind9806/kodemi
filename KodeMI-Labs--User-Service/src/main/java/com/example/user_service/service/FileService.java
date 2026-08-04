package com.example.user_service.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    String uploadFile(String userId, MultipartFile file);
    public boolean fileExists(String fileKey);
    public void deleteFile(String fileKey);

}
