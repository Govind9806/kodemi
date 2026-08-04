# Bugfix Requirements Document

## Introduction

The UserService Spring Boot microservice has 21 SonarQube code quality issues spanning
critical multi-threading risks, missing test assertions, redundant test duplication,
oversized test methods, unsafe assertThrows lambdas, unused imports, naming convention
violations, and a suboptimal AWS SDK builder pattern. Left unaddressed these issues
degrade maintainability, introduce concurrency bugs, and reduce test reliability.

---

## Bug Analysis

### Current Behavior (Defect)

**Critical — Thread-safety violation**

1.1 WHEN `EncryptionUtil.init()` is called concurrently by multiple threads THEN the
    system writes to the static field `keySpec` from an instance method, creating a
    data race (java:S2696, `EncryptionUtil.java` line 51).

**Blocker — Test with no assertions**

1.2 WHEN `UserServiceApplicationTests.contextLoads()` is executed THEN the system runs
    the test without any assertions, making it impossible to detect regressions
    (java:S2699, `UserServiceApplicationTests.java` line 10).

**Major — Duplicated test groups instead of parameterized tests**

1.3 WHEN `TrainerRequestDTOTest` is compiled THEN the system contains three groups of
    near-identical test methods (5 tests at line 118, 4 tests at line 162, 3 tests at
    line 192) that differ only in input values, violating DRY and inflating test count
    (java:S5976).

1.4 WHEN `TrainerAdminResponseTest.testSettersAndGetters()` is executed THEN the system
    runs a single test method containing 32 assertions, exceeding the recommended
    maximum of 25 and making failures harder to diagnose (java:S5961,
    `TrainerAdminResponseTest.java` line 51).

**Major — Unsafe assertThrows lambdas with multiple throwing invocations**

1.5 WHEN `TrainerServiceImplTest` is compiled THEN the system contains assertThrows
    lambda bodies at lines 173, 180, 187, 199, 320, 326, 338, 344, and 357 where each
    lambda contains multiple method invocations that could each throw, making it
    ambiguous which call actually triggers the expected exception (java:S5778).

**Minor — Unused imports**

1.6 WHEN `TrainerService.java` is compiled THEN the system retains an unused import
    `com.example.user_service.model.Trainer` at line 7 (java:S1128).

1.7 WHEN `RatingRepository.java` is compiled THEN the system retains an unused import
    `com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList` at line 5
    (java:S1128).

1.8 WHEN `TrainerServiceImpl.java` is compiled THEN the system retains an unused import
    `com.example.user_service.feign.AdminClient` at line 11 (java:S1128).

**Minor — Naming convention violations**

1.9 WHEN `localDateConverterTest.java` is loaded by the JUnit runner THEN the system
    uses a class name starting with a lowercase letter, violating the Java test naming
    convention (java:S3577).

1.10 WHEN `LearnerRepositoryTest.java` is compiled THEN the system declares the test
     class with an unnecessary `public` modifier, which is redundant for JUnit 5 test
     classes (java:S5786, line 19).

**Info — Suboptimal AWS SDK builder usage**

1.11 WHEN `FileServiceImpl.uploadMultipart()` builds the `CompleteMultipartUploadRequest`
     THEN the system uses a nested `CompletedMultipartUpload.builder().parts(...).build()`
     call instead of the Consumer Builder overload, adding unnecessary verbosity
     (java:S6244, `FileServiceImpl.java` line 213).

---

### Expected Behavior (Correct)

**Critical — Thread-safety**

2.1 WHEN `EncryptionUtil.init()` is called THEN the system SHALL initialize `keySpec`
    safely, either by making `init()` static or by removing the static field and
    passing the key spec through instance state, eliminating the S2696 violation.

**Blocker — Test assertions**

2.2 WHEN `UserServiceApplicationTests.contextLoads()` is executed THEN the system SHALL
    include at least one assertion (e.g., asserting the application context is not null)
    so the test can meaningfully detect failures.

**Major — Parameterized tests**

2.3 WHEN `TrainerRequestDTOTest` validates phone number formats THEN the system SHALL
    use a single `@ParameterizedTest` with a `@MethodSource` or `@CsvSource` to cover
    all equivalent phone number cases (replacing the 5-test group at line 118).

2.4 WHEN `TrainerRequestDTOTest` validates email formats THEN the system SHALL use a
    single `@ParameterizedTest` to cover all equivalent email cases (replacing the
    4-test group at line 162).

2.5 WHEN `TrainerRequestDTOTest` validates gender values THEN the system SHALL use a
    single `@ParameterizedTest` to cover all equivalent gender cases (replacing the
    3-test group at line 192).

2.6 WHEN `TrainerAdminResponseTest.testSettersAndGetters()` is executed THEN the system
    SHALL split the assertions across multiple focused test methods so that no single
    test method contains more than 25 assertions.

**Major — Isolated assertThrows lambdas**

2.7 WHEN `TrainerServiceImplTest` uses `assertThrows` THEN the system SHALL ensure each
    lambda body contains exactly one method invocation that is expected to throw, so
    that the source of the exception is unambiguous (fixing all 9 occurrences at lines
    173, 180, 187, 199, 320, 326, 338, 344, 357).

**Minor — Clean imports**

2.8 WHEN `TrainerService.java` is compiled THEN the system SHALL NOT contain the unused
    import `com.example.user_service.model.Trainer`.

2.9 WHEN `RatingRepository.java` is compiled THEN the system SHALL NOT contain the
    unused import `com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList`.

2.10 WHEN `TrainerServiceImpl.java` is compiled THEN the system SHALL NOT contain the
     unused import `com.example.user_service.feign.AdminClient`.

**Minor — Naming conventions**

2.11 WHEN `localDateConverterTest.java` is loaded THEN the system SHALL use a class name
     that matches the Java test naming convention (e.g., `LocalDateConverterTest`),
     including renaming the file to match.

2.12 WHEN `LearnerRepositoryTest.java` is compiled THEN the system SHALL declare the
     test class without the `public` modifier, conforming to JUnit 5 conventions.

**Info — Consumer Builder pattern**

2.13 WHEN `FileServiceImpl.uploadMultipart()` builds the `CompleteMultipartUploadRequest`
     THEN the system SHALL use the Consumer Builder overload
     (`.multipartUpload(b -> b.parts(completedParts))`) instead of the nested builder,
     reducing verbosity.

---

### Unchanged Behavior (Regression Prevention)

3.1 WHEN valid plaintext is encrypted via `EncryptionUtil.encrypt()` THEN the system
    SHALL CONTINUE TO produce a Base64-encoded AES-GCM ciphertext that can be
    successfully decrypted by `EncryptionUtil.decrypt()`.

3.2 WHEN `TrainerRequestDTOTest` runs all existing validation scenarios THEN the system
    SHALL CONTINUE TO assert the same pass/fail outcomes for every field constraint
    after the parameterized refactor.

3.3 WHEN `TrainerAdminResponseTest` runs THEN the system SHALL CONTINUE TO verify all
    32 getter/setter pairs after the assertion split refactor.

3.4 WHEN `TrainerServiceImplTest` runs all service-layer scenarios THEN the system SHALL
    CONTINUE TO verify the same exception types and mock interactions after the
    assertThrows lambda isolation refactor.

3.5 WHEN `LearnerRepositoryTest` runs THEN the system SHALL CONTINUE TO execute all
    existing test cases and assertions after the `public` modifier is removed.

3.6 WHEN `localDateConverterTest` tests are run THEN the system SHALL CONTINUE TO pass
    all existing convert/unconvert assertions after the class is renamed.

3.7 WHEN `FileServiceImpl.uploadMultipart()` completes a multipart upload THEN the
    system SHALL CONTINUE TO pass the correct list of `CompletedPart` objects to S3
    after the builder pattern change.

3.8 WHEN `TrainerService`, `RatingRepository`, and `TrainerServiceImpl` are compiled
    THEN the system SHALL CONTINUE TO compile and function correctly after the unused
    imports are removed.
