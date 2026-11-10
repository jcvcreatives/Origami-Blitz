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

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function buildValidProfile(uid, ts, overrides = {}) {
  return {
    userId: uid,
    username: "jcv",
    role: "gm",
    level: 5,
    highestUnlockedLevel: 50,
    highScore: 12500,
    scissorsCount: 10,
    glueCount: 10,
    hammerCount: 10,
    starsEncoded: "1:3,2:3,3:3",
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

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

test("Unauthenticated user: cannot read or write player profiles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .set(buildValidProfile(ALICE_UID, new Date()))
  );
});

test("Authenticated owner: can create, read, query, and update own profile", async () => {
  const aliceContext = testEnv.authenticatedContext(ALICE_UID);
  const aliceDb = aliceContext.firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .set(buildValidProfile(ALICE_UID, now))
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  await assertSucceeds(
    aliceDb.collection("users").where("userId", "==", ALICE_UID).get()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      highScore: 25000,
      level: 10,
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: cannot read or modify another user's profile", async () => {
  const now = new Date(Date.now() - 1000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .set(buildValidProfile(BOB_UID, now));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).update({ highScore: 999999 })
  );
});

test("Shadow update & value poisoning: rejects extra keys or out-of-range values", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);

  // Rejects shadow field on create
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .set(buildValidProfile(ALICE_UID, now, { isSuperAdmin: true }))
  );

  // Rejects invalid level > 50
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .set(buildValidProfile(ALICE_UID, now, { level: 999 }))
  );

  // Rejects invalid role enum
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .set(buildValidProfile(ALICE_UID, now, { role: "hacker" }))
  );
});

test("Email spoofing guard: unverified admin email cannot read another user's profile", async () => {
  const now = new Date(Date.now() - 1000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .set(buildValidProfile(BOB_UID, now));
  });

  const spoofAdminDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: "jcv.ai.use@gmail.com",
      email_verified: false,
    })
    .firestore();

  await assertFails(spoofAdminDb.collection("users").doc(BOB_UID).get());
});
