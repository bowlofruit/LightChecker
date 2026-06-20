const test = require("node:test");
const assert = require("node:assert/strict");
const { applyScheduleUpdate } = require("../lib/schedulePipeline.js");

/** Фіксований момент: 23 березня 2026 у календарі Europe/Kyiv (завтра = 24-е). */
const NOW_KYIV_MAR23 = new Date("2026-03-23T15:00:00+02:00");

function createMockFirestore() {
  const store = Object.create(null);
  const db = {
    collection(col) {
      return {
        doc(id) {
          const path = `${col}/${id}`;
          return {
            get: async () => ({
              exists: Object.hasOwn(store, path),
              data: () => (Object.hasOwn(store, path) ? store[path] : undefined),
            }),
            set: async (data) => {
              store[path] = data;
            },
          };
        },
      };
    },
    // Minimal transaction shim mirroring firebase-admin: get() returns a snapshot,
    // set() is a buffered write applied as the callback runs (sufficient for single-doc tests).
    async runTransaction(fn) {
      const tx = {
        get: (ref) => ref.get(),
        set: (ref, data) => {
          ref.set(data);
          return tx;
        },
      };
      return fn(tx);
    },
    _peek(path) {
      return store[path];
    },
  };
  return db;
}

function createMockMessaging(sent) {
  return {
    send: async (msg) => {
      sent.push(msg);
    },
  };
}

test("first write sets v=1 and sends FCM (f=2 days)", async () => {
  const db = createMockFirestore();
  const sent = [];
  const messaging = createMockMessaging(sent);
  const r = await applyScheduleUpdate(
    db,
    messaging,
    "kyiv",
    "1",
    20260323,
    [[480, 600]],
    NOW_KYIV_MAR23,
  );
  assert.equal(r.skipped, false);
  assert.equal(sent.length, 1);
  assert.equal(sent[0].topic, "lc_kyiv_1");
  assert.equal(sent[0].data.v, "1");
  assert.equal(sent[0].data.d, "20260323");
  const doc = db._peek("schedules/kyiv__1");
  assert.equal(doc.f, 2);
  assert.equal(doc.days["20260323"].v, 1);
  assert.deepEqual(doc.days["20260323"].s, [480, 600]);
});

test("unchanged payload skips FCM", async () => {
  const db = createMockFirestore();
  const sent = [];
  const messaging = createMockMessaging(sent);
  const pairs = [
    [0, 60],
    [120, 180],
  ];
  await applyScheduleUpdate(db, messaging, "r", "q", 20260323, pairs, NOW_KYIV_MAR23);
  await applyScheduleUpdate(db, messaging, "r", "q", 20260323, pairs, NOW_KYIV_MAR23);
  assert.equal(sent.length, 1);
  const r = await applyScheduleUpdate(
    db,
    messaging,
    "r",
    "q",
    20260323,
    pairs,
    NOW_KYIV_MAR23,
  );
  assert.equal(r.skipped, true);
});

test("same day changed slots bumps v", async () => {
  const db = createMockFirestore();
  const sent = [];
  const messaging = createMockMessaging(sent);
  await applyScheduleUpdate(db, messaging, "r", "q", 20260323, [[0, 60]], NOW_KYIV_MAR23);
  await applyScheduleUpdate(db, messaging, "r", "q", 20260323, [[0, 30]], NOW_KYIV_MAR23);
  assert.equal(sent.length, 2);
  const doc = db._peek("schedules/r__q");
  assert.equal(doc.days["20260323"].v, 2);
  assert.deepEqual(doc.days["20260323"].s, [0, 30]);
});

test("second calendar day adds second key; v resets for new day", async () => {
  const db = createMockFirestore();
  const sent = [];
  const messaging = createMockMessaging(sent);
  await applyScheduleUpdate(db, messaging, "r", "q", 20260323, [[10, 20]], NOW_KYIV_MAR23);
  await applyScheduleUpdate(db, messaging, "r", "q", 20260324, [[10, 20]], NOW_KYIV_MAR23);
  const doc = db._peek("schedules/r__q");
  assert.equal(doc.f, 2);
  assert.equal(doc.days["20260323"].v, 1);
  assert.equal(doc.days["20260324"].v, 1);
  assert.ok(doc.days["20260323"]);
  assert.ok(doc.days["20260324"]);
});
