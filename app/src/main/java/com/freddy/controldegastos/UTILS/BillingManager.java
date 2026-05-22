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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Collections;
import java.util.List;

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
        this.activityRef = activity;
        this.callback = callback;

        billingClient = BillingClient.newBuilder(activity)
                .enablePendingPurchases()
                .setListener(this::handlePurchaseUpdate)
                .build();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!yaRespondio && callback != null) {
                Log.w("BillingManager", "Timeout alcanzado. Continuando sin Billing.");
                callback.onResult(false);
                yaRespondio = true;
            }
        }, 5000);

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    consultarComprasExistentes();
                    consultarProductoPremium();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.w("BillingManager", "Servicio Billing desconectado");
            }
        });
    }

    private void consultarComprasExistentes() {
        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build();

        billingClient.queryPurchasesAsync(params, (result, purchasesList) -> {
            boolean esPremium = false;
            for (Purchase purchase : purchasesList) {
                if (esCompraPremiumActiva(purchase)) {
                    esPremium = true;
                    guardarPremiumConfirmadoEnFirebase();
                    break;
                }
            }

            if (!yaRespondio && callback != null) {
                callback.onResult(esPremium);
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
                    guardarPremiumConfirmadoEnFirebase();
                    if (callback != null) {
                        callback.onResult(true);
                    }

                    if (activityRef != null) {
                        activityRef.runOnUiThread(() -> Toast.makeText(
                                activityRef,
                                "Compra registrada. Tu estado Premium se validara de forma segura.",
                                Toast.LENGTH_LONG
                        ).show());
                    }

                    reconocerCompraSiHaceFalta(purchase);
                }
            }
        }
    }

    private boolean esCompraPremiumActiva(Purchase purchase) {
        return purchase.getProducts().contains(ID_PRODUCTO)
                && purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED;
    }

    private void guardarPremiumConfirmadoEnFirebase() {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference("usuarios")
                .child(usuario.getUid())
                .child("esPremium")
                .setValue(true)
                .addOnFailureListener(e -> Log.w("BillingManager", "No se pudo sincronizar Premium.", e));
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
        }
    }
}
