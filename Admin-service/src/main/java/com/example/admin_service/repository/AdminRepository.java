package com.example.admin_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.example.admin_service.enums.AdminRole;
import com.example.admin_service.model.Admin;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AdminRepository {

    private final DynamoDBMapper dynamoDBMapper;

    public AdminRepository(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public String save(Admin admin) {
        dynamoDBMapper.save(admin);
        return "Admin Created Successfully";
    }

    public Admin findById(String adminId) {
        return dynamoDBMapper.load(Admin.class, adminId);
    }

    public Admin findByRole(AdminRole adminRole) {

        Admin admin = new Admin();

        admin.setAdminRole(adminRole);

        DynamoDBQueryExpression<Admin> queryExpression =
                new DynamoDBQueryExpression<Admin>()
                        .withIndexName("adminRole-index")
                        .withHashKeyValues(admin)
                        .withConsistentRead(false)
                        .withLimit(1);

        List<Admin> results =
                dynamoDBMapper.query(Admin.class, queryExpression);

        return results.isEmpty()
                ? null
                : results.get(0);
    }
    public Admin findByEmail(String email) {

        Admin admin = new Admin();

        admin.setEmail(email);

        DynamoDBQueryExpression<Admin> queryExpression =
                new DynamoDBQueryExpression<Admin>()
                        .withIndexName("email-index")
                        .withHashKeyValues(admin)
                        .withConsistentRead(false)
                        .withLimit(1);

        List<Admin> results =
                dynamoDBMapper.query(Admin.class, queryExpression);

        return results.isEmpty()
                ? null
                : results.get(0);
    }

    public void delete(String adminId) {
        Admin admin = dynamoDBMapper.load(Admin.class, adminId);
        if(admin != null){
            dynamoDBMapper.delete(admin);
        }
    }
}