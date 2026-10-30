# Quiz Timer to 15 Seconds & 20/30 Passing Threshold for 50 Points & Stage Progression

Increase the per-question countdown timer from 10 seconds to 15 seconds, and lower the stage progression and reward passing threshold from 25 to 20 correct answers out of 30.

## User Review & Critical Decisions

- **Timer Duration**: Increased to 15,000 ms (15 seconds) with scaled circular progress indicator and warning stages (green > 9s, amber 5–9s, red < 5s).
- **Passing Threshold**: Lowered from 25/30 (83.3%) to 20/30 (66.7%).
- **Rewards & Progression**: Answering at least 20 correct questions awards +50 points to the user's wallet, records the category as completed in the permutation cycle, and progresses the user to the next stage. Answering fewer than 20 awards 0 points and requires replaying the category.
- **Rules & Feedback UI**: All countdown badges, score chips, result banners, explanation dialogs, category selection headers, and unit tests will be updated to display 20/30 as the pass target.

---

## 1. Overview & Core Concept

- **What It Does**:
  1. Gives users 15 seconds per question (up from 10s), providing more comfortable reading time for detailed Scripture questions.
  2. Sets a more achievable, motivating pass requirement of 20 out of 30 correct answers (66.7% score), rewarding users with 50 points and stage progression upon reaching 20+ correct.
- **Key Value**: Improves user retention and game feel by reducing time pressure and offering a fair, rewarding path to earn wallet points and unlock all 15 stages in the cycle.

---

## 2. User Experience & Visual Design

- **Quiz Screen (`QuizScreen.kt`)**:
  - Top AppBar score chip updates from `"$correctCount / 25 to Pass"` to `"$correctCount / 20 to Pass"`, highlighting in emerald green as soon as 20 is reached.
  - Circular countdown timer displays 15s to 0s:
    - 15s – 10s: Fresh emerald green (`EmeraldGreen`)
    - 9s – 5s: Warm warning amber (`WarmGold`)
    - 4s – 0s: Urgent alert red (`QuizRed`) with buzzer sound if expired.
- **Quiz Result Screen (`QuizResultScreen.kt`)**:
  - `isPassed = correctCount >= 20`.
  - Passed Banner (20–30 correct): Congratulations badge, "+50 Points Earned! Added to your wallet • Next stage unlocked", and celebration gradient.
  - Failed Banner (0–19 correct): "Keep Learning and Try Again!", "You need at least 20 correct answers out of 30 to earn 50 points and progress to another stage."
- **Category Screen (`QuizCategoryScreen.kt`)**:
  - Rules banner updates to: `"100,000+ Questions in Bank • 10-Day Non-Repeat Guarantee • Score 20+ to pass (+50 pts & progress)"`.

---

## 3. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                      QuizScreen                        │
│  - timerDurationMs = 15000L                            │
│  - Progress = remainingMillis / 15000f                 │
│  - Score Chip = "$correctCount / 20 to Pass"           │
└───────────────────────────┬────────────────────────────┘
                            │ On Quiz Complete
                            ▼
┌────────────────────────────────────────────────────────┐
│                     QuizRepository                     │
│  - isPassed = correctAnswers >= 20                     │
│  - pointsAwarded = if (isPassed) 50L else 0L           │
│  - if (isPassed) CategoryCycleManager.recordPlayed()   │
│  - updates Firestore user points & stage progression   │
└───────────────────────────┬────────────────────────────┘
                            │ Navigates to
                            ▼
┌────────────────────────────────────────────────────────┐
│                   QuizResultScreen                     │
│  - isPassed = correctCount >= 20                       │
│  - Visual cards: +50 Points & Next Stage Unlocked      │
└────────────────────────────────────────────────────────┘
```

---

## 4. Verification Plan

1. **Unit Tests**:
   - Update `QuizRepositoryRuleTest` to assert that 20 correct answers awards 50 points and advances the category cycle, while 19 correct answers awards 0 points.
   - Run `gradle :app:testDebugUnitTest`.
2. **Compilation**:
   - Run `compile_applet` to verify full compilation and resource consistency.
