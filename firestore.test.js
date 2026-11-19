const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

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

test("Unauthenticated users cannot read or write any Life OS collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = new Date(Date.now() - 2000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("userPreferences").doc("dashboard").set({
      userId: ALICE_UID,
      enabledWidgets: ["DAILY_PRODUCTIVITY_SCORE"],
      themeMode: "SYSTEM",
      weightTasks: 30,
      weightHabits: 30,
      weightChallenges: 20,
      weightWater: 10,
      weightSteps: 10,
      waterTargetMl: 2500,
      stepsTarget: 8000,
      focusTargetMinutes: 90,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Owner can create and update customizable dashboard widgets in userPreferences", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const prefRef = aliceDb.collection("users").doc(ALICE_UID).collection("userPreferences").doc("dashboard");

  await assertSucceeds(
    prefRef.set({
      userId: ALICE_UID,
      enabledWidgets: [
        "DAILY_PRODUCTIVITY_SCORE",
        "TASKS_SUMMARY",
        "HABIT_PROGRESS",
        "ACTIVE_CHALLENGES",
        "WATER_INTAKE",
        "AUTOMATIC_STEPS",
        "FOCUS_TIME",
        "UPCOMING_REMINDERS",
        "GOAL_PROGRESS",
      ],
      themeMode: "SYSTEM",
      weightTasks: 30,
      weightHabits: 30,
      weightChallenges: 20,
      weightWater: 10,
      weightSteps: 10,
      waterTargetMl: 2500,
      stepsTarget: 8000,
      focusTargetMinutes: 120,
      quietHoursStart: 22,
      quietHoursEnd: 7,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reorder and remove widgets via partial update
  await assertSucceeds(
    prefRef.update({
      enabledWidgets: ["FOCUS_TIME", "DAILY_PRODUCTIVITY_SCORE", "WATER_INTAKE"],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field shadow update
  await assertFails(
    prefRef.update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject immutable createdAt overwrite
  await assertFails(
    prefRef.update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Owner can create, list, and partially update tasks, waterLogs, stepLogs, and challengeLogs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  const taskRef = aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_01");
  await assertSucceeds(
    taskRef.set({
      userId: ALICE_UID,
      title: "Ship customizable dashboard widgets",
      description: "Implement drag and reorder controls",
      priority: "HIGH",
      status: "PENDING",
      dueDateIso: "2026-11-19",
      category: "Engineering",
      recurrence: "NONE",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    taskRef.update({
      status: "COMPLETED",
      completedDateIso: "2026-11-19",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("tasks").get());

  // Water log
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("waterLogs").doc("w_01").set({
      userId: ALICE_UID,
      dateIso: "2026-11-19",
      amountMl: 500,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Step log
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("stepLogs").doc("s_2026-11-19").set({
      userId: ALICE_UID,
      dateIso: "2026-11-19",
      steps: 6420,
      source: "MANUAL",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject future timestamp tampering
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("waterLogs").doc("w_future").set({
      userId: ALICE_UID,
      dateIso: "2026-11-19",
      amountMl: 250,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Reject DocumentId "id" key inside body
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_bad").set({
      id: "task_bad",
      userId: ALICE_UID,
      title: "Bad payload",
      priority: "LOW",
      status: "PENDING",
      dueDateIso: "2026-11-19",
      category: "General",
      recurrence: "NONE",
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Cross-user isolation blocks Bob from reading or modifying Alice's data", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const ts = new Date(Date.now() - 10000);
    await ctx.firestore().collection("users").doc(ALICE_UID).collection("tasks").doc("alice_task").set({
      userId: ALICE_UID,
      title: "Private task",
      priority: "HIGH",
      status: "PENDING",
      dueDateIso: "2026-11-19",
      category: "Personal",
      recurrence: "NONE",
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("tasks").doc("alice_task").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("tasks").doc("alice_task").update({
      title: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner can create measurable custom-unit habits with autoSource and progressValue habitLogs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  const habitRef = aliceDb.collection("users").doc(ALICE_UID).collection("habits").doc("habit_water_auto");
  await assertSucceeds(
    habitRef.set({
      userId: ALICE_UID,
      title: "Hydrate Daily",
      scheduleType: "DAILY",
      weekdaysMask: 127,
      startDateIso: "2026-11-19",
      targetValue: 8,
      unit: "Glasses",
      autoSource: "WATER",
      reminderTime: "09:00",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("habitLogs").doc("habit_water_auto_20261119").set({
      userId: ALICE_UID,
      habitId: "habit_water_auto",
      dateIso: "2026-11-19",
      completed: false,
      progressValue: 5,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("habitLogs").doc("habit_water_auto_20261119").update({
      completed: true,
      progressValue: 8,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject invalid autoSource
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("habits").doc("habit_bad").set({
      userId: ALICE_UID,
      title: "Bad Source",
      scheduleType: "DAILY",
      weekdaysMask: 127,
      startDateIso: "2026-11-19",
      targetValue: 10,
      unit: "Minutes",
      autoSource: "INVALID_SOURCE",
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Owner can manage private memories, code notes, finance transactions, STEP_SENSOR logs, and notificationPrefs; cross-user blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 10000);

  // Memory (strictly private, separate from Notes)
  const memRef = aliceDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_01");
  await assertSucceeds(
    memRef.set({
      userId: ALICE_UID,
      title: "Graduation Day",
      description: "Celebrated with family",
      memoryDateIso: "2026-11-19",
      memoryTimeText: "18:30",
      category: "Personal",
      tagsCsv: "family,milestone",
      favorite: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_01").get());

  // Upgraded Note with folder, favorite, isCodeNote
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_java_01").set({
      userId: ALICE_UID,
      title: "Spring Boot REST",
      content: "@RestController class Demo {}",
      folder: "Java",
      pinned: true,
      favorite: true,
      isCodeNote: true,
      archived: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Finance Transaction
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_01").set({
      userId: ALICE_UID,
      type: "EXPENSE",
      amount: 249.5,
      category: "Food",
      note: "Lunch",
      dateIso: "2026-11-19",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_bad").set({
      userId: ALICE_UID,
      type: "EXPENSE",
      amount: -50,
      category: "Food",
      dateIso: "2026-11-19",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Hardware STEP_SENSOR step log
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("stepLogs").doc("s_sensor_1").set({
      userId: ALICE_UID,
      dateIso: "2026-11-19",
      steps: 4520,
      source: "STEP_SENSOR",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Smart Notification Preferences
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notificationPrefs").doc("default").set({
      userId: ALICE_UID,
      pauseAll: false,
      waterEnabled: true,
      stepsEnabled: true,
      goalsEnabled: true,
      tasksEnabled: true,
      habitsEnabled: true,
      techNewsEnabled: true,
      youtubeVideosEnabled: true,
      learningEnabled: true,
      classesEnabled: true,
      interviewsEnabled: true,
      financeEnabled: true,
      motivationEnabled: true,
      dailySummaryEnabled: true,
      waterIntervalMinutes: 60,
      dailySummaryHour: 21,
      dailySummaryMinute: 0,
      quietHoursEnabled: true,
      quietStartHour: 22,
      quietEndHour: 7,
      soundEnabled: true,
      vibrationEnabled: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("notificationPrefs").doc("default").get());
});


