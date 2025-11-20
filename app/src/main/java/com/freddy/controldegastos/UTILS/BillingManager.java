package com.freddy.controldegastos.UTILS;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.billingclient.api.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.*;

public class BillingManager {

    public interface PremiumStatusCallback {
        void onResult(boolean isPremium);
    }

    private static final String ID_PRODUCTO = "premium_version";
    private BillingClient billingClient;
    private ProductDetails premiumProductDetails;
    private boolean yaRespondio = false;
    private Activity activityRef; // referencia para mostrar Toast

    public BillingManager(Activity activity, PremiumStatusCallback callback) {
        this.activityRef = activity; // guardar referencia

        billingClient = BillingClient.newBuilder(activity)
                .enablePendingPurchases()
                .setListener(this::handlePurchaseUpdate)
                .build();

        // ⏳ Timeout de seguridad
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!yaRespondio && callback != null) {
                Log.w("BillingManager", "⏰ Timeout alcanzado. Continuando sin Billing.");
                callback.onResult(false);
                yaRespondio = true;
            }
        }, 5000);

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // Verificar si ya se compró
                    QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build();

                    billingClient.queryPurchasesAsync(params, (result, purchasesList) -> {
                        boolean esPremium = false;
                        for (Purchase purchase : purchasesList) {
                            if (purchase.getProducts().contains(ID_PRODUCTO)
                                    && purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                esPremium = true;
                                break;
                            }
                        }

                        if (!yaRespondio && callback != null) {
                            callback.onResult(esPremium);
                            yaRespondio = true;
                        }
                    });

                    // Obtener detalles del producto
                    QueryProductDetailsParams.Product product =
                            QueryProductDetailsParams.Product.newBuilder()
                                    .setProductId(ID_PRODUCTO)
                                    .setProductType(BillingClient.ProductType.INAPP)
                                    .build();

                    QueryProductDetailsParams queryParams =
                            QueryProductDetailsParams.newBuilder()
                                    .setProductList(Collections.singletonList(product))
                                    .build();

                    billingClient.queryProductDetailsAsync(queryParams, (billingResult1, productDetailsList) -> {
                        if (!productDetailsList.isEmpty()) {
                            premiumProductDetails = productDetailsList.get(0);
                        }
                    });
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.w("BillingManager", "Servicio Billing desconectado");
            }
        });
    }

    private void handlePurchaseUpdate(@NonNull BillingResult billingResult, @NonNull List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                if (purchase.getProducts().contains(ID_PRODUCTO)
                        && purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {

                    // Guardar en Firebase
                    FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
                    if (usuario != null) {
                        String uid = usuario.getUid();
                        DatabaseReference ref = FirebaseDatabase.getInstance()
                                .getReference("usuarios")
                                .child(uid);

                        Map<String, Object> datos = new HashMap<>();
                        datos.put("esPremium", true);
                        datos.put("fechaPremium", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));

                        ref.updateChildren(datos);
                    }

                    // Mensaje para el usuario
                    if (activityRef != null) {
                        activityRef.runOnUiThread(() -> Toast.makeText(
                                activityRef,
                                "Compra realizada con éxito. Reinicia la app para activar Premium.",
                                Toast.LENGTH_LONG
                        ).show());
                    }

                    // Aceptar compra
                    if (!purchase.isAcknowledged()) {
                        AcknowledgePurchaseParams acknowledgeParams =
                                AcknowledgePurchaseParams.newBuilder()
                                        .setPurchaseToken(purchase.getPurchaseToken())
                                        .build();

                        billingClient.acknowledgePurchase(acknowledgeParams, billingResult1 -> {
                            if (billingResult1.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                Log.d("BillingManager", "Compra reconocida correctamente.");
                            }
                        });
                    }
                }
            }
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
            Log.e("BillingManager", "⚠️ Producto premium no cargado aún.");
        }
    }
}
