package com.example.kodemilabs.repository;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.example.kodemilabs.model.OTP;
import org.springframework.stereotype.Repository;

@Repository
public class OTPRepo {

    private final DynamoDBMapper dynamoDBMapper;

    public OTPRepo(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public void save(OTP otp) {
        dynamoDBMapper.save(otp);
    }

    public OTP getOtpData(String userId) {
        return dynamoDBMapper.load(OTP.class, userId);
    }

    public void delete(OTP otp) {
        dynamoDBMapper.delete(otp);
    }
}
