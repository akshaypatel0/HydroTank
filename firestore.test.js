const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const GATEWAY_UID = "gateway_user_123";
const OTHER_UID = "random_user_456";

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

test("Public link visitor: can read tank status without authentication", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("tanks").doc("main_tank").set({
      levelPercent: 78.5,
      isMotorOn: false,
      targetLevel: 80.0,
      gatewayUserId: GATEWAY_UID,
      lastUpdated: new Date()
    });
  });

  const publicDb = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(publicDb.collection("tanks").doc("main_tank").get());
});

test("Unauthenticated user: cannot write or tamper with tank status", async () => {
  const publicDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    publicDb.collection("tanks").doc("main_tank").set({
      levelPercent: 99.0,
      isMotorOn: true,
      targetLevel: 90.0,
      gatewayUserId: "hacker",
      lastUpdated: new Date()
    })
  );
});

test("Authenticated gateway user: can write valid tank telemetry", async () => {
  const gatewayDb = testEnv.authenticatedContext(GATEWAY_UID).firestore();
  await assertSucceeds(
    gatewayDb.collection("tanks").doc("main_tank").set({
      levelPercent: 65.0,
      distanceCM: 6.8,
      isMotorOn: true,
      targetLevel: 80.0,
      statusMessage: "Motor Running",
      gatewayUserId: GATEWAY_UID,
      lastUpdated: new Date()
    })
  );
});

test("Authenticated user: cannot impersonate another gateway's userId", async () => {
  const badDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  await assertFails(
    badDb.collection("tanks").doc("main_tank").set({
      levelPercent: 65.0,
      isMotorOn: true,
      targetLevel: 80.0,
      gatewayUserId: GATEWAY_UID,
      lastUpdated: new Date()
    })
  );
});

test("Rejects invalid tank telemetry (out of bounds levelPercent)", async () => {
  const gatewayDb = testEnv.authenticatedContext(GATEWAY_UID).firestore();
  await assertFails(
    gatewayDb.collection("tanks").doc("main_tank").set({
      levelPercent: 150.0, // Invalid: exceeds 100%
      isMotorOn: false,
      targetLevel: 80.0,
      gatewayUserId: GATEWAY_UID,
      lastUpdated: new Date()
    })
  );
});
