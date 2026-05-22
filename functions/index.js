"use strict";

const {google} = require("googleapis");
const admin = require("firebase-admin");
const {HttpsError, onCall} = require("firebase-functions/v2/https");

admin.initializeApp();

const PACKAGE_NAME = process.env.PACKAGE_NAME || "com.freddy.controldegastos";
const PREMIUM_PRODUCT_ID = "premium_version";
const ANDROID_PUBLISHER_SCOPE = "https://www.googleapis.com/auth/androidpublisher";

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

  await admin.database().ref(`usuarios/${uid}`).update({
    esPremium: true,
    fechaPremium: admin.database.ServerValue.TIMESTAMP,
  });

  return {
    premium: true,
  };
});
