# Implementation Plan

- [x] 1. Write bug condition exploration test
  - **Property 1: Bug Condition** - All 11 SonarQube Violations Present in Unfixed Code
  - **CRITICAL**: This test MUST FAIL on unfixed code — failure confirms the violations exist
  - **DO NOT attempt to fix the test or the code when it fails**
  - **NOTE**: This test encodes the expected behavior — it will validate the fix when it passes after implementation
  - **GOAL**: Surface counterexamples that demonstrate each violation exists
  - **Scoped PBT Approach**: Scope to the concrete flagged constructs for reproducibility
  - Run SonarLint / SonarQube scanner on the unfixed codebase and confirm all 11 rule violations are reported at the expected locations:
    - S2696 on `EncryptionUtil.init()` at line 51
    - S2699 on `UserServiceApplicationTests.contextLoads()` at line 10
    - S5976 on the three duplicate test groups in `TrainerRequestDTOTest`
    - S5961 on `TrainerAdminResponseTest.testSettersAndGetters()` at line 51
    - S5778 on `assertThrows` lambdas at lines 173, 180, 187, 199, 320, 326, 338, 344, 357 in `TrainerServiceImplTest`
    - S1128 on unused imports in `TrainerService.java`, `RatingRepository.java`, `TrainerServiceImpl.java`
    - S3577 on `localDateConverterTest.java` class name
    - S5786 on `public class LearnerRepositoryTest`
    - S6244 on nested builder in `FileServiceImpl.uploadMultipart()` at line 213
  - The test assertions should match the Expected Behavior Properties from design (isBugCondition returns true for all 11 constructs)
  - Run scan on UNFIXED code
  - **EXPECTED OUTCOME**: All 11 violations flagged (this is correct — it proves the bugs exist)
  - Document counterexamples found (e.g., exact SonarQube issue keys and locations) to understand root cause
  - Mark task complete when scan is run and all violations are documented
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 1.10, 1.11_

- [x] 2. Write preservation property tests (BEFORE implementing fixes)
  - **Property 2: Preservation** - Runtime and Test Behaviour Unchanged
  - **IMPORTANT**: Follow observation-first methodology
  - Observe behavior on UNFIXED code for all constructs where isBugCondition returns false
  - Observe: `EncryptionUtil.encrypt(p)` followed by `decrypt(ciphertext)` returns `p` for valid plaintexts
  - Observe: all existing `TrainerRequestDTOTest` phone/email/gender assertions pass on unfixed code
  - Observe: all 32 assertions in `TrainerAdminResponseTest.testSettersAndGetters()` pass on unfixed code
  - Observe: all exception-type and mock-interaction assertions in `TrainerServiceImplTest` pass on unfixed code
  - Observe: `LearnerRepositoryTest` all test cases pass on unfixed code
  - Observe: `localDateConverterTest` all convert/unconvert assertions pass on unfixed code
  - Observe: `FileServiceImpl.uploadMultipart()` passes correct `CompletedPart` list to S3 on unfixed code
  - Observe: `TrainerService`, `RatingRepository`, `TrainerServiceImpl` compile cleanly on unfixed code
  - Write property-based tests capturing observed behavior patterns from Preservation Requirements in design:
    - PBT: for random valid plaintexts, `decrypt(encrypt(p)) == p` (encryption round-trip)
    - PBT: for random phone/email/gender inputs, validation outcome matches original individual tests
    - PBT: for random `CompletedPart` lists, Consumer Builder passes them through unchanged to S3 request
  - Run full Maven test suite (`mvn test`) on UNFIXED code
  - **EXPECTED OUTCOME**: All existing tests PASS (confirms baseline behavior to preserve)
  - Mark task complete when property-based tests are written, run, and passing on unfixed code
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8_

- [x] 3. Fix all 11 SonarQube code smell violations

  - [x] 3.1 Fix S2696 — Make `EncryptionUtil.init()` static
    - File: `src/main/java/com/example/user_service/util/EncryptionUtil.java`
    - Extract the static field write from the instance `@PostConstruct init()` into a `static` method accepting `secretKey` as a parameter
    - Add a thin `@PostConstruct` wrapper that calls `EncryptionUtil.init(this.secretKey)` with the injected value
    - Remove the direct static field assignment from the instance method body
    - Verify `encrypt()` and `decrypt()` still function correctly after the refactor
    - _Bug_Condition: isBugCondition(init()) where init().isInstanceMethod AND init().writesStaticField("keySpec")_
    - _Expected_Behavior: init() is static or delegates via static helper; S2696 violation eliminated_
    - _Preservation: EncryptionUtil.encrypt()/decrypt() AES-GCM round-trips unchanged (Requirement 3.1)_
    - _Requirements: 2.1, 3.1_

  - [x] 3.2 Fix S2699 — Add assertion to `contextLoads()`
    - File: `src/test/java/com/example/user_service/UserServiceApplicationTests.java`
    - Inject `ApplicationContext` via `@Autowired`
    - Replace empty body (or trivial `assertTrue(true)`) with `assertNotNull(context)`
    - _Bug_Condition: isBugCondition(contextLoads()) where contextLoads().assertionCount = 0_
    - _Expected_Behavior: contextLoads() contains at least one meaningful assertion_
    - _Preservation: Spring context loads successfully; no other test behaviour changed_
    - _Requirements: 2.2_

  - [x] 3.3 Fix S5976 — Replace duplicate test groups with `@ParameterizedTest` in `TrainerRequestDTOTest`
    - File: `src/test/java/com/example/user_service/dto/request/TrainerRequestDTOTest.java`
    - Replace the 5-method phone number group with a single `@ParameterizedTest @CsvSource` method covering all 5 cases
    - Replace the 4-method email group with a single `@ParameterizedTest @CsvSource` method covering all 4 cases
    - Replace the 3-method gender group with a single `@ParameterizedTest @CsvSource` method covering all 3 cases
    - Keep `phoneNumber_null_violation` and `email_null_violation` as separate `@Test` methods (null cannot be expressed in `@CsvSource`)
    - _Bug_Condition: isBugCondition(group) where group.groupSize >= 3 and methods differ only in input values_
    - _Expected_Behavior: one @ParameterizedTest per group; S5976 violation eliminated_
    - _Preservation: every phone/email/gender input that previously passed or failed validation continues to do so (Requirement 3.2)_
    - _Requirements: 2.3, 2.4, 2.5, 3.2_

  - [x] 3.4 Fix S5961 — Split `testSettersAndGetters()` in `TrainerAdminResponseTest`
    - File: `src/test/java/com/example/user_service/dto/response/TrainerAdminResponseTest.java`
    - Split the 32-assertion method into two focused methods, each with ≤ 25 assertions:
      - `testCoreIdentityAndContactFields()` — userId, fullName, gender, designation, phoneNumber, email, ratingValue, languageKnown, officeName, officeAddress, linkedInOrWebsiteURL, gitHubUrl (≤ 15 assertions)
      - `testProfessionalAndFinancialFields()` — remaining 20 fields (≤ 20 assertions)
    - Delete the original `testSettersAndGetters()` method
    - _Bug_Condition: isBugCondition(testSettersAndGetters()) where assertionCount > 25_
    - _Expected_Behavior: no single test method exceeds 25 assertions; S5961 violation eliminated_
    - _Preservation: all 32 getter/setter assertions still present across the new methods (Requirement 3.3)_
    - _Requirements: 2.6, 3.3_

  - [x] 3.5 Fix S5778 — Isolate `assertThrows` lambdas in `TrainerServiceImplTest`
    - File: `src/test/java/com/example/user_service/service/TrainerServiceImplTest.java`
    - Audit each `assertThrows` lambda at lines 173, 180, 187, 199, 320, 326, 338, 344, 357
    - For any lambda containing more than one method invocation that could throw, extract all setup/stub calls outside the lambda and leave exactly one invocation inside
    - Pattern: move `when(...)` stubs and any setup calls before the `assertThrows` call; keep only the single invocation under test inside the lambda
    - _Bug_Condition: isBugCondition(lambda) where lambda.throwingInvocationCount > 1_
    - _Expected_Behavior: each assertThrows lambda contains exactly one throwing invocation; S5778 eliminated_
    - _Preservation: same exception types thrown for same inputs; all mock interactions unchanged (Requirement 3.4)_
    - _Requirements: 2.7, 3.4_

  - [x] 3.6 Fix S1128 — Remove unused imports
    - `TrainerService.java`: remove `import com.example.user_service.model.Trainer;`
    - `RatingRepository.java`: remove `import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;`
    - `TrainerServiceImpl.java`: remove `import com.example.user_service.feign.AdminClient;`
    - _Bug_Condition: isBugCondition(import) where import is present but never referenced_
    - _Expected_Behavior: no unused imports; S1128 violations eliminated in all three files_
    - _Preservation: all three files compile and function correctly after removal (Requirement 3.8)_
    - _Requirements: 2.8, 2.9, 2.10, 3.8_

  - [x] 3.7 Fix S3577 — Rename `localDateConverterTest` to `LocalDateConverterTest`
    - Rename file from `localDateConverterTest.java` to `LocalDateConverterTest.java`
    - Update the class declaration to `class LocalDateConverterTest` (PascalCase)
    - Update any references to the class name in test runner configuration or imports if present
    - _Bug_Condition: isBugCondition(class) where class.simpleName.charAt(0).isLowerCase()_
    - _Expected_Behavior: class name starts with uppercase; S3577 violation eliminated_
    - _Preservation: JUnit runner discovers and executes all existing convert/unconvert tests (Requirement 3.6)_
    - _Requirements: 2.11, 3.6_

  - [x] 3.8 Fix S5786 — Remove `public` modifier from `LearnerRepositoryTest`
    - File: `src/test/java/com/example/user_service/repository/LearnerRepositoryTest.java`
    - Change `public class LearnerRepositoryTest` to `class LearnerRepositoryTest`
    - _Bug_Condition: isBugCondition(class) where class.hasModifier("public") in JUnit 5 test class_
    - _Expected_Behavior: no public modifier; S5786 violation eliminated_
    - _Preservation: all existing test cases execute and pass without the public modifier (Requirement 3.5)_
    - _Requirements: 2.12, 3.5_

  - [x] 3.9 Fix S6244 — Use Consumer Builder in `FileServiceImpl.uploadMultipart()`
    - File: `src/main/java/com/example/user_service/service/impl/FileServiceImpl.java`
    - At line 213, replace `.multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build())` with `.multipartUpload(b -> b.parts(completedParts))`
    - _Bug_Condition: isBugCondition(call) where call.usesNestedBuilder("CompletedMultipartUpload")_
    - _Expected_Behavior: Consumer Builder lambda used; S6244 violation eliminated_
    - _Preservation: uploadMultipart() passes the correct ordered CompletedPart list to S3 (Requirement 3.7)_
    - _Requirements: 2.13, 3.7_

  - [x] 3.10 Verify bug condition exploration test now passes
    - **Property 1: Expected Behavior** - All 11 SonarQube Violations Eliminated
    - **IMPORTANT**: Re-run the SAME scan from task 1 — do NOT write a new test
    - The scan from task 1 encodes the expected behavior (zero violations for all 11 rules)
    - When this scan passes (reports 0 violations), it confirms the expected behavior is satisfied
    - Run SonarLint / SonarQube scanner on the FIXED codebase
    - **EXPECTED OUTCOME**: 0 violations for S2696, S2699, S5976, S5961, S5778, S1128 (×3), S3577, S5786, S6244
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 2.10, 2.11, 2.12, 2.13_

  - [x] 3.11 Verify preservation tests still pass
    - **Property 2: Preservation** - Runtime and Test Behaviour Unchanged
    - **IMPORTANT**: Re-run the SAME tests from task 2 — do NOT write new tests
    - Run full Maven test suite (`mvn test`) on the FIXED codebase
    - Run property-based tests (encryption round-trip, validation outcomes, S3 upload)
    - **EXPECTED OUTCOME**: All tests PASS (confirms no regressions)
    - Confirm all tests still pass after fixes (no regressions)

- [x] 4. Checkpoint — Ensure all tests pass
  - Run `mvn test` and confirm zero test failures
  - Run SonarQube scanner and confirm the issue count drops from 21 to 0 for all targeted rules
  - Verify the Spring application context loads successfully with the refactored `EncryptionUtil`
  - Ensure all tests pass; ask the user if questions arise
