const test = require("node:test");
const assert = require("node:assert/strict");
const { applyScheduleUpdate } = require("../lib/schedulePipeline.js");

function createMockFirestore() {
  const store = Object.create(null);
  return {
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
    _peek(path) {
      return store[path];
    },
  };
}

function createMockMessaging(sent) {
  return {
    send: async (msg) => {
      sent.push(msg);
    },
  };
}

test("first write sets v=1 and sends FCM", async () => {
  const db = createMockFirestore();
  const sent = [];
  const messaging = createMockMessaging(sent);
  const r = await applyScheduleUpdate(db, messaging, "kyiv", "1", 20260323, [
    [480, 600],
  ]);
  assert.equal(r.skipped, false);
  assert.equal(sent.length, 1);
  assert.equal(sent[0].topic, "lc_kyiv_1");
  assert.equal(sent[0].data.v, "1");
  assert.equal(sent[0].data.d, "20260323");
  const doc = db._peek("schedules/kyiv__1");
  assert.equal(doc.v, 1);
  assert.deepEqual(doc.s, [480, 600]);
});

test("unchanged payload skips FCM", async () => {
  const db = createMockFirestore();
  const sent = [];
  const messaging = createMockMessaging(sent);
  const pairs = [
    [0, 60],
    [120, 180],
  ];
  await applyScheduleUpdate(db, messaging, "r", "q", 20260323, pairs);
  await applyScheduleUpdate(db, messaging, "r", "q", 20260323, pairs);
  assert.equal(sent.length, 1);
  const r = await applyScheduleUpdate(db, messaging, "r", "q", 20260323, pairs);
  assert.equal(r.skipped, true);
});

test("same day changed slots bumps v", async () => {
  const db = createMockFirestore();
  const sent = [];
  const messaging = createMockMessaging(sent);
  await applyScheduleUpdate(db, messaging, "r", "q", 20260323, [[0, 60]]);
  await applyScheduleUpdate(db, messaging, "r", "q", 20260323, [[0, 30]]);
  assert.equal(sent.length, 2);
  const doc = db._peek("schedules/r__q");
  assert.equal(doc.v, 2);
  assert.deepEqual(doc.s, [0, 30]);
});

test("new calendar day resets v to 1", async () => {
  const db = createMockFirestore();
  const sent = [];
  const messaging = createMockMessaging(sent);
  await applyScheduleUpdate(db, messaging, "r", "q", 20260323, [[10, 20]]);
  await applyScheduleUpdate(db, messaging, "r", "q", 20260324, [[10, 20]]);
  const doc = db._peek("schedules/r__q");
  assert.equal(doc.d, 20260324);
  assert.equal(doc.v, 1);
});
