package com.freddy.controldegastos.UTILS;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BillingManager {

    public interface PremiumStatusCallback {
        void onResult(boolean isPremium);
    }

    private static final String ID_PRODUCTO = "premium_version";
    private BillingClient billingClient;
    private ProductDetails premiumProductDetails;
    private boolean yaRespondio = false;
    private final Activity activityRef;
    private final PremiumStatusCallback callback;

    public BillingManager(Activity activity, PremiumStatusCallback callback) {
        this(activity, callback, false);
    }

    public BillingManager(Activity activity, PremiumStatusCallback callback, boolean restaurarAlIniciar) {
        this.activityRef = activity;
        this.callback = callback;

        billingClient = BillingClient.newBuilder(activity)
                .enablePendingPurchases()
                .setListener(this::handlePurchaseUpdate)
                .build();

        if (restaurarAlIniciar) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!yaRespondio && callback != null) {
                    Log.w("BillingManager", "Timeout alcanzado. Continuando sin Billing.");
                    callback.onResult(false);
                    yaRespondio = true;
                }
            }, 5000);
        }

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    if (restaurarAlIniciar) {
                        consultarComprasExistentes(false);
                    }
                    consultarProductoPremium();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.w("BillingManager", "Servicio Billing desconectado");
            }
        });
    }

    private void consultarComprasExistentes(boolean mostrarErrorAlUsuario) {
        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build();

        billingClient.queryPurchasesAsync(params, (result, purchasesList) -> {
            for (Purchase purchase : purchasesList) {
                if (esCompraPremiumActiva(purchase)) {
                    validarCompraPremiumEnBackend(purchase, () -> {
                        reconocerCompraSiHaceFalta(purchase);
                        if (!yaRespondio && callback != null) {
                            callback.onResult(true);
                            yaRespondio = true;
                        }
                    }, mostrarErrorAlUsuario);
                    return;
                }
            }

            if (!yaRespondio && callback != null) {
                callback.onResult(false);
                yaRespondio = true;
            }
        });
    }

    private void consultarProductoPremium() {
        QueryProductDetailsParams.Product product =
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(ID_PRODUCTO)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build();

        QueryProductDetailsParams queryParams =
                QueryProductDetailsParams.newBuilder()
                        .setProductList(Collections.singletonList(product))
                        .build();

        billingClient.queryProductDetailsAsync(queryParams, (billingResult, productDetailsList) -> {
            if (!productDetailsList.isEmpty()) {
                premiumProductDetails = productDetailsList.get(0);
            }
        });
    }

    private void handlePurchaseUpdate(@NonNull BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                if (esCompraPremiumActiva(purchase)) {
                    validarCompraPremiumEnBackend(purchase, () -> {
                        if (callback != null) {
                            callback.onResult(true);
                        }

                        if (activityRef != null) {
                            activityRef.runOnUiThread(() -> Toast.makeText(
                                    activityRef,
                                    "Compra validada. Ya tienes Premium activo.",
                                    Toast.LENGTH_LONG
                            ).show());
                        }

                        reconocerCompraSiHaceFalta(purchase);
                    }, true);
                }
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            consultarComprasExistentes(true);
        }
    }

    private boolean esCompraPremiumActiva(Purchase purchase) {
        return purchase.getProducts().contains(ID_PRODUCTO)
                && purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED;
    }

    private void validarCompraPremiumEnBackend(Purchase purchase, Runnable onValidated, boolean mostrarErrorAlUsuario) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", ID_PRODUCTO);
        data.put("purchaseToken", purchase.getPurchaseToken());

        FirebaseFunctions.getInstance()
                .getHttpsCallable("validatePremiumPurchase")
                .call(data)
                .addOnSuccessListener(result -> {
                    if (onValidated != null) {
                        onValidated.run();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w("BillingManager", "No se pudo validar la compra Premium.", e);
                    if (!yaRespondio && callback != null) {
                        callback.onResult(false);
                        yaRespondio = true;
                    }
                    if (mostrarErrorAlUsuario && activityRef != null) {
                        activityRef.runOnUiThread(() -> Toast.makeText(
                                activityRef,
                                "Esta compra ya esta vinculada a otra cuenta. Inicia sesion con esa cuenta o usa otra cuenta de Google Play.",
                                Toast.LENGTH_LONG
                        ).show());
                    }
                });
    }

    private void reconocerCompraSiHaceFalta(Purchase purchase) {
        if (!purchase.isAcknowledged()) {
            AcknowledgePurchaseParams acknowledgeParams =
                    AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.getPurchaseToken())
                            .build();

            billingClient.acknowledgePurchase(acknowledgeParams, billingResult -> {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d("BillingManager", "Compra reconocida correctamente.");
                }
            });
        }
    }

    public void iniciarCompraPremium(Activity activity) {
        if (premiumProductDetails != null) {
            BillingFlowParams billingFlowParams =
                    BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(Collections.singletonList(
                                    BillingFlowParams.ProductDetailsParams.newBuilder()
                                            .setProductDetails(premiumProductDetails)
                                            .build()
                            ))
                            .build();

            billingClient.launchBillingFlow(activity, billingFlowParams);
        } else {
            Log.e("BillingManager", "Producto premium no cargado aun.");
            Toast.makeText(activity, "Espera unos segundos e intenta de nuevo.", Toast.LENGTH_SHORT).show();
        }
    }
}
