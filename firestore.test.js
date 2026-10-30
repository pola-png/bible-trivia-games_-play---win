const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// --- SECURITY BOUNDS TESTS ---

test("Unauthenticated user: cannot read quizSessions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("quizSessions").get());
});

test("Unauthenticated user: cannot create quizSession", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("quizSessions").doc("session_1").set({
    sessionId: "session_1",
    userId: ALICE_UID,
    category: "Old Testament",
    totalQuestions: 30,
    correctAnswers: 20,
    timedOutAnswers: 2,
    isPassed: true,
    pointsAwarded: 50,
    status: "completed",
    createdAt: new Date(),
  }));
});

test("Authenticated user: cannot read another user's quizSession", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("quizSessions").doc("bob_session").set({
      sessionId: "bob_session",
      userId: BOB_UID,
      category: "New Testament",
      totalQuestions: 30,
      correctAnswers: 22,
      timedOutAnswers: 1,
      isPassed: true,
      pointsAwarded: 50,
      status: "completed",
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("quizSessions").doc("bob_session").get());
});

test("Authenticated user: can read their own quizSession", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("quizSessions").doc("alice_session").set({
      sessionId: "alice_session",
      userId: ALICE_UID,
      category: "New Testament",
      totalQuestions: 30,
      correctAnswers: 20,
      timedOutAnswers: 2,
      isPassed: true,
      pointsAwarded: 50,
      status: "completed",
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("quizSessions").doc("alice_session").get());
});

test("Authenticated user: can query their own sessions with matching filter", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("quizSessions").doc("alice_session").set({
      sessionId: "alice_session",
      userId: ALICE_UID,
      category: "Bible Characters",
      totalQuestions: 30,
      correctAnswers: 25,
      timedOutAnswers: 0,
      isPassed: true,
      pointsAwarded: 50,
      status: "completed",
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("quizSessions").where("userId", "==", ALICE_UID).get()
  );
});

test("Authenticated user: cannot directly modify pointTransactions (immutable ledger)", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("pointTransactions").doc("tx_1").set({
      transactionId: "tx_1",
      userId: ALICE_UID,
      type: "quiz_reward",
      amount: 50,
      description: "Quiz reward",
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("pointTransactions").doc("tx_1").update({
    amount: 1000,
  }));
});

test("Cash Redemption: 50,000 points min required, rejected under 50k", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  // Attempt with 40,000 points (must fail)
  await assertFails(aliceDb.collection("cashRedemptionRequests").doc("req_under_50k").set({
    requestId: "req_under_50k",
    userId: ALICE_UID,
    pointsRedeemed: 40000,
    usdAmount: 40,
    accountName: "Alice Walker",
    accountNumber: "0123456789",
    bankName: "Access Bank",
    status: "pending",
    createdAt: new Date(),
  }));

  // Attempt with 50,000 points (must succeed)
  await assertSucceeds(aliceDb.collection("cashRedemptionRequests").doc("req_50k").set({
    requestId: "req_50k",
    userId: ALICE_UID,
    pointsRedeemed: 50000,
    usdAmount: 50,
    accountName: "Alice Walker",
    accountNumber: "0123456789",
    bankName: "Access Bank",
    status: "pending",
    createdAt: new Date(),
  }));
});

test("Cash Redemption: Bob cannot view Alice's cash redemption requests", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("cashRedemptionRequests").doc("alice_cash_req").set({
      requestId: "alice_cash_req",
      userId: ALICE_UID,
      pointsRedeemed: 50000,
      usdAmount: 50,
      accountName: "Alice Walker",
      accountNumber: "0123456789",
      bankName: "Access Bank",
      status: "pending",
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("cashRedemptionRequests").doc("alice_cash_req").get());
});
