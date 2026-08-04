package com.example.course_service.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MultipartUploadPartETag {
    @JsonProperty("partNumber")
    @JsonAlias({"part_number", "partnum", "part_num"})
    @NotNull(message = "partNumber is required")
    @Min(value = 1, message = "partNumber must be a positive integer")
    private Integer partNumber;

    @JsonProperty("eTag")
    @JsonAlias({"etag", "ETag"})
    @NotBlank(message = "eTag is required")
    private String eTag;

    public MultipartUploadPartETag() {
    }

    public MultipartUploadPartETag(Integer partNumber, String eTag) {
        this.partNumber = partNumber;
        this.eTag = eTag;
    }

    public Integer getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(Integer partNumber) {
        this.partNumber = partNumber;
    }

    public String getETag() {
        return eTag;
    }

    public void setETag(String eTag) {
        this.eTag = eTag;
    }
}
