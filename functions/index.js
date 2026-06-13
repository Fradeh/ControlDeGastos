"use strict";

const {google} = require("googleapis");
const admin = require("firebase-admin");
const crypto = require("crypto");
const {HttpsError, onCall} = require("firebase-functions/v2/https");

admin.initializeApp();

const PACKAGE_NAME = process.env.PACKAGE_NAME || "com.freddy.controldegastos";
const PREMIUM_PRODUCT_ID = "premium_version";
const ANDROID_PUBLISHER_SCOPE = "https://www.googleapis.com/auth/androidpublisher";
const NEW_PURCHASE_BIND_WINDOW_MS = 15 * 60 * 1000;

function hashPurchaseToken(purchaseToken) {
  return crypto.createHash("sha256").update(purchaseToken).digest("hex");
}

exports.validatePremiumPurchase = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Debes iniciar sesión.");
  }

  const uid = request.auth.uid;
  const productId = String(request.data.productId || "");
  const purchaseToken = String(request.data.purchaseToken || "");

  if (productId !== PREMIUM_PRODUCT_ID) {
    throw new HttpsError("invalid-argument", "Producto Premium inválido.");
  }

  if (!purchaseToken) {
    throw new HttpsError("invalid-argument", "Token de compra requerido.");
  }

  const auth = await google.auth.getClient({
    scopes: [ANDROID_PUBLISHER_SCOPE],
  });
  const androidpublisher = google.androidpublisher({
    version: "v3",
    auth,
  });

  let purchase;
  try {
    const response = await androidpublisher.purchases.products.get({
      packageName: PACKAGE_NAME,
      productId,
      token: purchaseToken,
    });
    purchase = response.data;
  } catch (error) {
    console.error("Google Play purchase validation failed", error);
    throw new HttpsError("permission-denied", "No se pudo validar la compra.");
  }

  if (purchase.purchaseState !== 0) {
    throw new HttpsError("failed-precondition", "La compra no está completada.");
  }

  if (purchase.acknowledgementState === 0) {
    await androidpublisher.purchases.products.acknowledge({
      packageName: PACKAGE_NAME,
      productId,
      token: purchaseToken,
      requestBody: {},
    });
  }

  const tokenHash = hashPurchaseToken(purchaseToken);
  const userRef = admin.database().ref(`usuarios/${uid}`);
  const userSnapshot = await userRef.child("esPremium").get();
  const userAlreadyPremium = userSnapshot.val() === true;
  const purchaseTime = Number(purchase.purchaseTimeMillis || 0);
  const isRecentPurchase =
      purchaseTime > 0 && Date.now() - purchaseTime <= NEW_PURCHASE_BIND_WINDOW_MS;
  const tokenRef = admin.database().ref(`premiumPurchaseTokens/${tokenHash}`);
  const transactionResult = await tokenRef.transaction((current) => {
    if (current === null) {
      if (!userAlreadyPremium && !isRecentPurchase) {
        return;
      }
      return {
        uid,
        productId,
        packageName: PACKAGE_NAME,
        purchaseTimeMillis: purchase.purchaseTimeMillis || null,
        createdAt: Date.now(),
      };
    }
    if (current.uid !== uid && userAlreadyPremium) {
      return {
        ...current,
        uid,
        recoveredFromUid: current.uid || null,
        recoveredAt: Date.now(),
      };
    }
    return current;
  });

  const tokenOwner = transactionResult.snapshot.val();
  if (!tokenOwner || tokenOwner.uid !== uid) {
    throw new HttpsError(
        "permission-denied",
        "Esta compra ya esta vinculada a otra cuenta.",
    );
  }

  await admin.database().ref(`usuarios/${uid}`).update({
    esPremium: true,
    fechaPremium: admin.database.ServerValue.TIMESTAMP,
    premiumPurchaseTokenHash: tokenHash,
  });

  return {
    premium: true,
  };
});
