package com.example.user_service.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import java.time.LocalDate;

public class LocalDateConverter implements DynamoDBTypeConverter<String, LocalDate> {

    @Override
    public String convert(LocalDate date) {
        return date != null ? date.toString() : null;
    }

    @Override
    public LocalDate unconvert(String value) {
        return value != null ? LocalDate.parse(value) : null;
    }
}
