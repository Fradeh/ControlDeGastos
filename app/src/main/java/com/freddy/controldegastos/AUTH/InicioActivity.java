package com.freddy.controldegastos.AUTH;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.freddy.controldegastos.GASTOS.MainActivity;
import com.freddy.controldegastos.R;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class InicioActivity extends AppCompatActivity {

    private static final String TAG = "InicioActivity";

    private InterstitialAd mInterstitialAd;
    private boolean navegadoAMain = false;
    private Handler handler = new Handler();
    private Runnable fallbackRunnable;

    private DatabaseReference usuariosRef;
    private String uidUsuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio);

        animarLogoYTexto();

        // Inicializa Mobile Ads
        MobileAds.initialize(this, initializationStatus -> {});

        uidUsuario = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uidUsuario == null) {
            // Sin sesión
            irAMainSeguro();
            return;
        }

        usuariosRef = FirebaseDatabase.getInstance()
                .getReference("usuarios")
                .child(uidUsuario);

        // Lee si es Premium
        usuariosRef.child("esPremium").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean esPremium = snapshot.getValue(Boolean.class);
                if (Boolean.TRUE.equals(esPremium)) {
                    // Premium → sin anuncio
                    irAMainSeguro();
                } else {
                    // Gratis → intenta cargar y mostrar intersticial
                    cargarYMostrarIntersticial();
                    // Fallback: si en 8s no se pudo mostrar ni falló, avanza
                    fallbackRunnable = () -> {
                        if (!navegadoAMain && mInterstitialAd == null) {
                            Log.w(TAG, "Tiempo de espera agotado (8s) sin anuncio, navegando al Main.");
                            irAMainSeguro();
                        }
                    };
                    handler.postDelayed(fallbackRunnable, 8000);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error al leer esPremium: " + error.getMessage());
                irAMainSeguro();
            }
        });
    }

    private void cargarYMostrarIntersticial() {

        final String adUnitIdPrueba = "ca-app-pub-2503649416779224/1348229276";
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(this, adUnitIdPrueba, adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                Log.d(TAG, "Intersticial cargado.");
                mInterstitialAd = interstitialAd;

                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Intersticial cerrado por el usuario.");
                        // Al cerrarse el anuncio → navega
                        irAMainSeguro();
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                        Log.e(TAG, "No se pudo mostrar el anuncio: " + adError.getMessage());
                        irAMainSeguro();
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        Log.d(TAG, "Intersticial mostrado.");
                        // Para evitar volver a usar el mismo objeto
                        mInterstitialAd = null;
                        // Cancela el fallback si estaba en cola
                        cancelarFallback();
                    }
                });

                // Si la Activity sigue viva, muéstralo
                if (!navegadoAMain) {
                    mInterstitialAd.show(InicioActivity.this);
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "No se pudo cargar el anuncio: " + loadAdError.getMessage());
                // Falló la carga → sigue al Main
                irAMainSeguro();
            }
        });
    }

    private void cancelarFallback() {
        if (fallbackRunnable != null) {
            handler.removeCallbacks(fallbackRunnable);
            fallbackRunnable = null;
        }
    }

    private void irAMainSeguro() {
        if (navegadoAMain) return;
        navegadoAMain = true;
        cancelarFallback();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void animarLogoYTexto() {
        ImageView logo = findViewById(R.id.logoApp);
        TextView texto = findViewById(R.id.textoBienvenida);

        logo.setAlpha(0f);
        logo.setTranslationY(50f);
        texto.setAlpha(0f);

        logo.animate().alpha(1f).translationY(0).setDuration(600).start();
        texto.animate().alpha(1f).setStartDelay(500).setDuration(700).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelarFallback();
    }
}
