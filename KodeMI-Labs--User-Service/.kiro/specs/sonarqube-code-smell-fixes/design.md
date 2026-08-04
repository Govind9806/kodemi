# SonarQube Code Smell Fixes — Bugfix Design

## Overview

The UserService Spring Boot microservice has 11 distinct SonarQube code quality issues
(covering 21 flagged locations) ranging from a critical thread-safety violation in
`EncryptionUtil` to minor naming convention and import hygiene issues. The fix strategy
is surgical: each change targets only the flagged construct, with no collateral
modifications. Preservation of all existing runtime and test behaviour is verified
through the testing strategy below.

---

## Glossary

- **Bug_Condition (C)**: The set of source-code constructs that trigger a SonarQube
  rule violation — e.g., an instance method writing to a static field, a test method
  with no assertions, duplicate test methods differing only in input values.
- **Property (P)**: The desired post-fix state — the violation is gone and the
  observable runtime/test behaviour is identical to before.
- **Preservation**: All runtime behaviour (encryption round-trips, S3 uploads, service
  logic) and all existing test assertions that must remain unchanged after each fix.
- **isBugCondition(construct)**: Pseudocode predicate that returns `true` when a given
  source construct still exhibits the SonarQube violation.
- **EncryptionUtil**: `src/main/java/.../util/EncryptionUtil.java` — AES-GCM
  encrypt/decrypt utility initialised via `@PostConstruct`.
- **TrainerRequestDTOTest**: Test class with duplicate phone/email/gender test groups
  that should be collapsed into `@ParameterizedTest` methods.
- **TrainerAdminResponseTest**: Test class whose single `testSettersAndGetters()` method
  contains 32 assertions, exceeding the SonarQube S5961 limit of 25.
- **TrainerServiceImplTest**: Test class with `assertThrows` lambdas that each contain
  multiple method invocations, making the exception source ambiguous (S5778).
- **localDateConverterTest**: Test file whose name starts with a lowercase letter,
  violating the Java test naming convention (S3577).
- **LearnerRepositoryTest**: Test class declared with an unnecessary `public` modifier
  for a JUnit 5 class (S5786).
- **FileServiceImpl**: Service class whose `uploadMultipart()` method uses a nested
  builder instead of the Consumer Builder overload (S6244).

---

## Bug Details

### Bug Condition

The 11 issues share a common structure: a source construct deviates from a well-defined
coding rule. The composite bug condition is:

```
FUNCTION isBugCondition(construct)
  INPUT:  construct — a Java source element (method, import, class declaration, etc.)
  OUTPUT: boolean

  RETURN (
    -- S2696: instance method writes to static field
    (construct IS init() IN EncryptionUtil
     AND construct.isInstanceMethod
     AND construct.writesStaticField("keySpec"))

    OR

    -- S2699: test method has no assertions
    (construct IS contextLoads() IN UserServiceApplicationTests
     AND construct.assertionCount = 0)

    OR

    -- S5976: group of near-identical test methods differing only in inputs
    (construct IS phoneNumber_* group IN TrainerRequestDTOTest  AND construct.groupSize >= 3)
    OR (construct IS email_* group IN TrainerRequestDTOTest     AND construct.groupSize >= 3)
    OR (construct IS gender_* group IN TrainerRequestDTOTest    AND construct.groupSize >= 3)

    OR

    -- S5961: single test method with more than 25 assertions
    (construct IS testSettersAndGetters() IN TrainerAdminResponseTest
     AND construct.assertionCount > 25)

    OR

    -- S5778: assertThrows lambda with multiple throwing invocations
    (construct IS assertThrows lambda IN TrainerServiceImplTest
     AND construct.throwingInvocationCount > 1)

    OR

    -- S1128: unused import
    (construct IS import("com.example.user_service.model.Trainer") IN TrainerService)
    OR (construct IS import("com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList")
        IN RatingRepository)
    OR (construct IS import("com.example.user_service.feign.AdminClient")
        IN TrainerServiceImpl)

    OR

    -- S3577: test class name starts with lowercase
    (construct IS class localDateConverterTest
     AND construct.simpleName.charAt(0).isLowerCase())

    OR

    -- S5786: unnecessary public modifier on JUnit 5 test class
    (construct IS class LearnerRepositoryTest AND construct.hasModifier("public"))

    OR

    -- S6244: nested builder instead of Consumer Builder
    (construct IS completeMultipartUpload call IN FileServiceImpl.uploadMultipart
     AND construct.usesNestedBuilder("CompletedMultipartUpload"))
  )
END FUNCTION
```

### Examples

| # | Construct | Current (buggy) | Expected (fixed) |
|---|-----------|-----------------|------------------|
| 1 | `EncryptionUtil.init()` | Instance `@PostConstruct` method assigns `keySpec = new SecretKeySpec(...)` directly | `init()` is made `static` (or delegates to the existing `setKeySpec` helper without the S2696 flag) |
| 2 | `contextLoads()` | Empty body — no assertions | Contains `assertNotNull(context)` or equivalent |
| 3 | `phoneNumber_valid_noViolation()` + 4 siblings | 5 separate `@Test` methods | 1 `@ParameterizedTest` with `@CsvSource` covering all 5 cases |
| 4 | `testSettersAndGetters()` | 32 assertions in one method | Split into 2–3 focused methods, each ≤ 25 assertions |
| 5 | `assertThrows` at line 173 | Lambda calls `trainerService.createTrainerProfile(null, ...)` — only one invocation, but pattern must be verified for all 9 sites | Each lambda contains exactly one invocation that is expected to throw |
| 6 | `import com.example.user_service.model.Trainer` in `TrainerService.java` | Present, never referenced | Removed |
| 7 | `import ...PaginatedQueryList` in `RatingRepository.java` | Present, never referenced | Removed |
| 8 | `import ...AdminClient` in `TrainerServiceImpl.java` | Present, never referenced | Removed |
| 9 | File `localDateConverterTest.java` | Class name `localDateConverterTest` (lowercase `l`) | Renamed to `LocalDateConverterTest` (file + class) |
| 10 | `public class LearnerRepositoryTest` | `public` modifier present | `class LearnerRepositoryTest` (no `public`) |
| 11 | `completeMultipartUpload(...)` in `FileServiceImpl` | `.multipartUpload(CompletedMultipartUpload.builder().parts(...).build())` | `.multipartUpload(b -> b.parts(completedParts))` |

---

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**

- `EncryptionUtil.encrypt()` and `EncryptionUtil.decrypt()` must continue to produce
  correct AES-GCM round-trips for any valid plaintext after `init()` is refactored.
- All 32 getter/setter assertions in `TrainerAdminResponseTest` must continue to pass
  after the method is split.
- All phone/email/gender validation outcomes in `TrainerRequestDTOTest` must continue
  to pass after the parameterized refactor.
- All exception-type and mock-interaction assertions in `TrainerServiceImplTest` must
  continue to pass after the `assertThrows` lambda isolation.
- `LearnerRepositoryTest` must continue to execute all existing test cases after the
  `public` modifier is removed.
- `localDateConverterTest` (renamed `LocalDateConverterTest`) must continue to pass all
  convert/unconvert assertions.
- `FileServiceImpl.uploadMultipart()` must continue to pass the correct ordered list of
  `CompletedPart` objects to S3 after the builder pattern change.
- `TrainerService`, `RatingRepository`, and `TrainerServiceImpl` must continue to
  compile and function correctly after unused imports are removed.

**Scope:**

All source constructs that do NOT match `isBugCondition` must be completely unaffected.
This includes all controller, repository, model, and other service classes not listed
above, as well as all test methods not in the targeted groups.

---

## Hypothesized Root Cause

Each issue has a distinct root cause:

1. **S2696 — Static field written from instance method**: `EncryptionUtil` was designed
   with `encrypt()`/`decrypt()` as `static` methods (for call-site convenience) but
   `init()` as an instance `@PostConstruct` method. The `keySpec` field was made
   `static` to bridge the two, but SonarQube correctly flags the instance-to-static
   write. The existing `private static synchronized setKeySpec()` helper was added as a
   partial mitigation but does not remove the S2696 flag because `init()` itself is
   still an instance method that triggers the write path.

2. **S2699 — No assertions**: The `contextLoads()` test was scaffolded as a smoke test
   with only a comment, never updated to include a real assertion.

3. **S5976 — Duplicate test methods**: Phone, email, and gender validation tests were
   written as individual `@Test` methods before `@ParameterizedTest` was adopted in the
   project. Each group tests the same field with different input values.

4. **S5961 — Too many assertions**: `testSettersAndGetters()` was written as a single
   comprehensive setter/getter round-trip test covering all 32 fields of
   `TrainerAdminResponse` in one method, exceeding the 25-assertion limit.

5. **S5778 — Multiple throwing invocations in assertThrows lambda**: Some `assertThrows`
   lambdas may contain setup calls (e.g., `when(...)` stubs) alongside the actual
   invocation under test, or the invocation itself calls a method that internally
   delegates to another throwing method. The lambda body must be reduced to exactly the
   one call expected to throw.

6. **S1128 — Unused imports (×3)**: Imports were left behind after refactoring removed
   the usages — `Trainer` from `TrainerService` (interface no longer references the
   model directly), `PaginatedQueryList` from `RatingRepository` (query result type
   changed), and `AdminClient` from `TrainerServiceImpl` (replaced by `AuthClient`).

7. **S3577 — Lowercase class name**: The file `localDateConverterTest.java` was created
   with a lowercase first letter, likely a typo during initial scaffolding.

8. **S5786 — Unnecessary public on test class**: JUnit 5 does not require `public` on
   test classes (unlike JUnit 4). The modifier was carried over from a JUnit 4 style.

9. **S6244 — Nested builder**: The `CompleteMultipartUploadRequest` was built using the
   explicit nested builder (`CompletedMultipartUpload.builder().parts(...).build()`)
   instead of the AWS SDK v2 Consumer Builder lambda overload.

---

## Correctness Properties

Property 1: Bug Condition — All SonarQube Violations Eliminated

_For any_ source construct where `isBugCondition(construct)` returns `true` (i.e., the
construct matches one of the 11 flagged patterns), the fixed codebase SHALL no longer
trigger the corresponding SonarQube rule, and the construct SHALL conform to the
expected coding standard described in the Expected Behavior section.

**Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 2.10, 2.11, 2.12, 2.13**

Property 2: Preservation — Runtime and Test Behaviour Unchanged

_For any_ source construct where `isBugCondition(construct)` returns `false` (i.e., all
constructs not targeted by the fixes), the fixed codebase SHALL produce exactly the same
runtime behaviour and test outcomes as the original codebase, preserving all existing
encryption round-trips, S3 upload logic, service-layer exception handling, and
validation test assertions.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8**

---

## Fix Implementation

### Changes Required

#### Fix 1 — `EncryptionUtil.java` (S2696)

**File**: `src/main/java/com/example/user_service/util/EncryptionUtil.java`

**Specific Changes**:
1. Add `static` modifier to `init()` so it is no longer an instance method writing to a
   static field. Remove `@PostConstruct` (which requires an instance method) and replace
   with a `static` initializer block or a `static` factory method called from a
   `@PostConstruct` on a thin wrapper, OR
2. Alternative: remove the `static` keyword from `keySpec` and pass it as an instance
   field to the `encrypt`/`decrypt` methods (requires making those instance methods too).
   The simpler path is option 1: annotate `init()` as `static` and use a Spring
   `@EventListener(ApplicationReadyEvent.class)` on a separate `@Component` that calls
   `EncryptionUtil.init(secretKey)` with the injected value.

**Recommended approach**: Keep `@PostConstruct` on the instance method but extract the
static field write into the already-present `private static synchronized setKeySpec()`
helper — which is already done in the current code. The remaining S2696 flag is because
`init()` itself is an instance method. The cleanest fix is to make `init()` package-private
and `static`, accepting the `secretKey` as a parameter, and call it from a thin
`@PostConstruct` wrapper that passes `this.secretKey`.

#### Fix 2 — `UserServiceApplicationTests.java` (S2699)

**File**: `src/test/java/com/example/user_service/UserServiceApplicationTests.java`

**Specific Changes**:
1. Inject `ApplicationContext` via `@Autowired` and assert `assertNotNull(context)` in
   `contextLoads()`, replacing the trivial `assertTrue(true)`.

#### Fix 3 — `TrainerRequestDTOTest.java` (S5976)

**File**: `src/test/java/com/example/user_service/dto/request/TrainerRequestDTOTest.java`

**Specific Changes**:
1. Replace the 5-method phone number group (`phoneNumber_valid_noViolation`,
   `phoneNumber_lessThan10Digits_violation`, `phoneNumber_moreThan10Digits_violation`,
   `phoneNumber_withLetters_violation`, `phoneNumber_blank_violation`) with a single
   `@ParameterizedTest @CsvSource` method.
2. Replace the 4-method email group (`email_valid_noViolation`,
   `email_invalidFormat_violation`, `email_missingAtSymbol_violation`,
   `email_blank_violation`) with a single `@ParameterizedTest @CsvSource` method.
3. Replace the 3-method gender group (`gender_male_noViolation`,
   `gender_female_noViolation`, `gender_other_noViolation`) with a single
   `@ParameterizedTest @CsvSource` method.
4. Keep `phoneNumber_null_violation` and `email_null_violation` as separate `@Test`
   methods since null cannot be expressed in `@CsvSource`.

#### Fix 4 — `TrainerAdminResponseTest.java` (S5961)

**File**: `src/test/java/com/example/user_service/dto/response/TrainerAdminResponseTest.java`

**Specific Changes**:
1. Split `testSettersAndGetters()` (32 assertions) into two focused methods:
   - `testCoreIdentityAndContactFields()` — userId, fullName, gender, designation,
     phoneNumber, email, ratingValue, languageKnown, officeName, officeAddress,
     linkedInOrWebsiteURL, gitHubUrl (≤ 15 assertions)
   - `testProfessionalAndFinancialFields()` — trainingSpecialization, yearsOfExperience,
     qualification, modesOfTrainingPreferred, clientsTrainedBefore, profileImageURL,
     createdAt, updatedAt, panNumber, dateOfBirth, bankName, branchName, accountNumber,
     ifscCode, globalCertifications, topRegistration, supportingDocumentsChecklist,
     anyLegalDisputesInPast5Years, demoContentKey, resumeKey (≤ 20 assertions)

#### Fix 5 — `TrainerServiceImplTest.java` (S5778)

**File**: `src/test/java/com/example/user_service/service/TrainerServiceImplTest.java`

**Specific Changes**:
1. Audit each `assertThrows` lambda at lines 173, 180, 187, 199, 320, 326, 338, 344,
   and 357. For any lambda containing more than one method invocation that could throw,
   extract all setup/stub calls outside the lambda and leave exactly one invocation
   inside.
2. Example pattern:
   ```java
   // Before (S5778 — two invocations inside lambda)
   assertThrows(SomeException.class, () -> {
       service.setupSomething(arg);
       service.methodUnderTest(arg);
   });

   // After (one invocation inside lambda)
   service.setupSomething(arg);
   assertThrows(SomeException.class, () -> service.methodUnderTest(arg));
   ```

#### Fix 6, 7, 8 — Unused Imports (S1128)

- **`TrainerService.java`**: Remove `import com.example.user_service.model.Trainer;`
- **`RatingRepository.java`**: Remove
  `import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;`
- **`TrainerServiceImpl.java`**: Remove
  `import com.example.user_service.feign.AdminClient;`

#### Fix 9 — `localDateConverterTest.java` (S3577)

**Specific Changes**:
1. Rename the file from `localDateConverterTest.java` to `LocalDateConverterTest.java`.
2. Rename the class declaration from `class LocalDateConverterTest` — the class body
   already uses `LocalDateConverterTest` (PascalCase), so only the file name needs
   updating.

#### Fix 10 — `LearnerRepositoryTest.java` (S5786)

**File**: `src/test/java/com/example/user_service/repository/LearnerRepositoryTest.java`

**Specific Changes**:
1. Remove the `public` modifier from the class declaration:
   `public class LearnerRepositoryTest` → `class LearnerRepositoryTest`

#### Fix 11 — `FileServiceImpl.java` (S6244)

**File**: `src/main/java/com/example/user_service/service/impl/FileServiceImpl.java`

**Specific Changes**:
1. In `uploadMultipart()`, replace the nested builder:
   ```java
   // Before
   .multipartUpload(CompletedMultipartUpload.builder()
       .parts(completedParts)
       .build())

   // After (Consumer Builder)
   .multipartUpload(b -> b.parts(completedParts))
   ```

---

## Testing Strategy

### Validation Approach

The testing strategy follows a two-phase approach: first, surface counterexamples that
demonstrate each violation on unfixed code, then verify the fix works correctly and
preserves existing behaviour.

### Exploratory Bug Condition Checking

**Goal**: Confirm each SonarQube violation exists in the unfixed code and understand
the exact construct that triggers it. Run these checks on the UNFIXED code.

**Test Plan**: Use SonarQube scanner or SonarLint IDE plugin to scan the unfixed code
and confirm all 11 rule violations are reported at the expected locations.

**Test Cases**:
1. **S2696 Exploration**: Run SonarLint on `EncryptionUtil.java` — expect flag on
   `init()` at line 51 (will fail/flag on unfixed code).
2. **S2699 Exploration**: Run SonarLint on `UserServiceApplicationTests.java` — expect
   flag on `contextLoads()` at line 10 (will flag on unfixed code).
3. **S5976 Exploration**: Run SonarLint on `TrainerRequestDTOTest.java` — expect flags
   on the three duplicate test groups (will flag on unfixed code).
4. **S5961 Exploration**: Run SonarLint on `TrainerAdminResponseTest.java` — expect
   flag on `testSettersAndGetters()` at line 51 (will flag on unfixed code).
5. **S5778 Exploration**: Run SonarLint on `TrainerServiceImplTest.java` — expect flags
   at lines 173, 180, 187, 199, 320, 326, 338, 344, 357 (will flag on unfixed code).
6. **S1128 Exploration**: Run SonarLint on the three files — expect unused import flags
   (will flag on unfixed code).
7. **S3577 Exploration**: Run SonarLint on `localDateConverterTest.java` — expect
   naming convention flag (will flag on unfixed code).
8. **S5786 Exploration**: Run SonarLint on `LearnerRepositoryTest.java` — expect
   unnecessary `public` flag (will flag on unfixed code).
9. **S6244 Exploration**: Run SonarLint on `FileServiceImpl.java` — expect nested
   builder flag at line 213 (will flag on unfixed code).

**Expected Counterexamples**:
- SonarQube reports exactly 21 issues across the 11 rule violations listed above.
- Root cause confirmed: each violation matches the construct identified in Bug Details.

### Fix Checking

**Goal**: Verify that for all constructs where `isBugCondition` holds, the fixed code
no longer triggers the SonarQube rule.

```
FOR ALL construct WHERE isBugCondition(construct) DO
  result := sonarScan(fixedConstruct)
  ASSERT result.violations.isEmpty()
  ASSERT expectedBehavior(fixedConstruct)
END FOR
```

### Preservation Checking

**Goal**: Verify that for all constructs where `isBugCondition` does NOT hold, the
fixed code produces the same runtime and test outcomes as the original.

```
FOR ALL construct WHERE NOT isBugCondition(construct) DO
  ASSERT runtimeBehavior(original(construct)) = runtimeBehavior(fixed(construct))
END FOR
```

**Testing Approach**: The existing test suite acts as the preservation oracle. All
existing tests must continue to pass after each fix. Property-based testing is
recommended for the encryption round-trip and parameterized validation cases because:
- It generates many input combinations automatically.
- It catches edge cases (e.g., boundary-length AES keys, unusual phone formats) that
  manual unit tests might miss.
- It provides strong guarantees that behaviour is unchanged across the full input domain.

**Test Plan**: Run the full test suite before and after each fix. Any regression is a
preservation failure.

**Test Cases**:
1. **Encryption Round-Trip Preservation**: After fixing `EncryptionUtil`, verify that
   `encrypt(plaintext)` followed by `decrypt(ciphertext)` returns the original plaintext
   for a representative set of inputs.
2. **Validation Outcome Preservation**: After the `@ParameterizedTest` refactor, verify
   that every phone/email/gender input that previously passed or failed validation
   continues to do so.
3. **Assertion Coverage Preservation**: After splitting `testSettersAndGetters()`, verify
   that all 32 field assertions are still present across the new methods.
4. **Exception Behaviour Preservation**: After isolating `assertThrows` lambdas, verify
   that the same exception types are thrown for the same inputs.
5. **Compile Preservation**: After removing unused imports, verify that all three files
   compile without errors.
6. **Test Discovery Preservation**: After renaming `localDateConverterTest`, verify that
   the JUnit runner discovers and executes all 7 tests in the renamed class.
7. **S3 Upload Preservation**: After the Consumer Builder change, verify that
   `uploadMultipart()` still passes the correct sorted `CompletedPart` list to S3.

### Unit Tests

- Verify `EncryptionUtil.encrypt()` / `decrypt()` round-trip after `init()` refactor.
- Verify `contextLoads()` contains a non-trivial assertion (context not null).
- Verify each parameterized phone/email/gender test covers all original input cases.
- Verify `TrainerAdminResponseTest` split methods together cover all 32 fields.
- Verify each `assertThrows` lambda in `TrainerServiceImplTest` contains exactly one
  throwing invocation.
- Verify the three files compile cleanly after import removal.
- Verify `LocalDateConverterTest` (renamed) passes all 7 existing test cases.
- Verify `LearnerRepositoryTest` passes all existing tests without `public` modifier.
- Verify `FileServiceImpl.uploadMultipart()` calls `completeMultipartUpload` with the
  correct parts list after the builder change.

### Property-Based Tests

- Generate random valid plaintexts and verify `decrypt(encrypt(p)) == p` after the
  `EncryptionUtil` refactor (Property 2 — preservation of encryption behaviour).
- Generate random phone number strings (valid 10-digit, short, long, with letters) and
  verify the parameterized test produces the same violation outcome as the original
  individual tests (Property 1 — fix correctness for S5976).
- Generate random email strings and verify the same pass/fail outcomes as before the
  parameterized refactor.
- Generate random lists of `CompletedPart` objects and verify that the Consumer Builder
  call passes them through unchanged to the S3 request (Property 2 — preservation of
  S3 upload behaviour).

### Integration Tests

- Run the full Maven test suite (`mvn test`) after all fixes and verify zero test
  failures and zero SonarQube violations remain.
- Run SonarQube scanner post-fix and confirm the issue count drops from 21 to 0 for the
  targeted rules.
- Verify that the Spring application context loads successfully with the refactored
  `EncryptionUtil` (end-to-end smoke test).
