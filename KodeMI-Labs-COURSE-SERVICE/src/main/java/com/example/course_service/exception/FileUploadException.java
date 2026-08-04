package com.example.course_service.exception;
 
import lombok.Getter;
import lombok.ToString;
 
@Getter
@ToString
public class FileUploadException extends RuntimeException {
    private final String fileName;
    private final String s3Key;
    private final int retryCount;
 
    public FileUploadException(String message, String fileName, String s3Key, int retryCount) {
        super(message);
        this.fileName = fileName;
        this.s3Key = s3Key;
        this.retryCount = retryCount;
    }
 
    public FileUploadException(String message, Throwable cause, String fileName, String s3Key, int retryCount) {
        super(message, cause);
        this.fileName = fileName;
        this.s3Key = s3Key;
        this.retryCount = retryCount;
    }
}
