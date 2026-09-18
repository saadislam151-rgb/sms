/**
 * MFS Payment Logger — receiving endpoint.
 *
 * Deploy this from the Apps Script editor bound to your target Google
 * Sheet (Extensions > Apps Script), then Deploy > New deployment >
 * type "Web app". See README.md for the full step-by-step.
 *
 * Expected incoming JSON body (POST), sent by the Android app:
 * {
 *   "secret": "...",
 *   "senderId": "bKash",
 *   "payerNumber": "01712345678",
 *   "transactionId": "9A7B3C4D5E",
 *   "amount": "500.00",
 *   "timestamp": "2026-09-19 10:30:00"
 * }
 *
 * SETUP (do this before deploying):
 * 1. Project Settings (gear icon) > Script Properties > add a property
 *    named SHARED_SECRET with a value you choose. Put the same value
 *    in the Android app's "Shared secret" field. This stops random
 *    people who guess your URL from writing junk into your sheet.
 * 2. Change SHEET_NAME below if your tab isn't called "Payments".
 */

const SHEET_NAME = "Payments";

// Column order written to the sheet. Adjust if you want a different layout —
// just keep it in sync with the row built in appendPaymentRow().
// A | Timestamp | B | Sender/Provider | C | Payer Number | D | Transaction ID | E | Amount

function doPost(e) {
  try {
    const data = JSON.parse(e.postData.contents);

    const expectedSecret = PropertiesService.getScriptProperties().getProperty("SHARED_SECRET");
    if (expectedSecret && data.secret !== expectedSecret) {
      return jsonResponse({ ok: false, error: "Invalid secret" });
    }

    const sheet = getOrCreateSheet();

    // Skip duplicates: if this transaction ID is already in the sheet
    // (e.g. the phone retried the request), don't add it twice.
    if (data.transactionId && transactionExists(sheet, data.transactionId)) {
      return jsonResponse({ ok: true, skipped: true, reason: "Duplicate transaction ID" });
    }

    appendPaymentRow(sheet, data);

    return jsonResponse({ ok: true });
  } catch (err) {
    return jsonResponse({ ok: false, error: err.toString() });
  }
}

function appendPaymentRow(sheet, data) {
  sheet.appendRow([
    data.timestamp || new Date(),
    data.senderId || "",
    data.payerNumber || "",
    data.transactionId || "",
    data.amount || ""
  ]);
}

function transactionExists(sheet, transactionId) {
  const lastRow = sheet.getLastRow();
  if (lastRow < 2) return false;
  // Transaction ID is column D (4th column).
  const ids = sheet.getRange(2, 4, lastRow - 1, 1).getValues().flat();
  return ids.indexOf(transactionId) !== -1;
}

function getOrCreateSheet() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  let sheet = ss.getSheetByName(SHEET_NAME);
  if (!sheet) {
    sheet = ss.insertSheet(SHEET_NAME);
    sheet.appendRow(["Timestamp", "Provider", "Payer Number", "Transaction ID", "Amount"]);
    sheet.setFrozenRows(1);
  }
  return sheet;
}

function jsonResponse(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}

/** Optional: run manually from the editor to sanity-check the sheet setup. */
function testAppendRow() {
  const sheet = getOrCreateSheet();
  appendPaymentRow(sheet, {
    timestamp: new Date().toISOString(),
    senderId: "bKash",
    payerNumber: "01700000000",
    transactionId: "MANUALTEST1",
    amount: "10.00"
  });
}
