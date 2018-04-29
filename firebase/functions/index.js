/*
 * Copyright (C) 2018 Gregory S. Meiste  <http://gregmeiste.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// See https://firebase.google.com/docs/functions/write-firebase-functions

const functions = require('firebase-functions');

// Cut off time. Child nodes older than this will be deleted.
const CUT_OFF_TIME = 2 * 24 * 60 * 60 * 1000; // 2 days in milliseconds

exports.sensorData = functions.https.onRequest((req, res) => {
  const data = req.query.data;
  if (data) {
    let obj = JSON.parse(data);
    console.info("temperature is %s, humidity is %s", obj.tempF.toFixed(1), obj.humid.toFixed(1));
    res.send(`temperature is ${obj.tempF.toFixed(1)}, humidity is ${obj.humid.toFixed(1)}`);
  } else {
    console.error("data is empty!");
    res.send("data is empty!");
  }
});

/**
 * This database triggered function will check for child nodes that are older than the
 * cut-off time.
 */
exports.deleteOldTemps = functions.database.ref('/shoptemps/{tempId}').onCreate((snap) => {
  const ref = snap.ref.parent; // reference to the parent
  const now = Date.now();
  const cutoff = now - CUT_OFF_TIME;
  const oldItemsQuery = ref.orderByChild('timestamp').endAt(cutoff);
  return oldItemsQuery.once('value').then((snapshot) => {
    // create a map with all children that need to be removed
    const updates = {};
    snapshot.forEach(child => {
      updates[child.key] = null;
    });
    console.info("deleting %d records", snapshot.numChildren());

    // execute all updates in one go and return the result to end the function
    return ref.update(updates);
  });
});