package com.freddy.controldegastos.PREMIUM;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.freddy.controldegastos.GASTOS.MainActivity;
import com.freddy.controldegastos.UTILS.BillingManager;
import com.freddy.controldegastos.R;


public class PromoPremiumActivity extends AppCompatActivity {

    private BillingManager billingManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promo_premium);

        // Inicializar BillingManager
        billingManager = new BillingManager(this, isPremium -> {
            if (isPremium) {
                Toast.makeText(this, "¡Gracias por hacerte Premium!", Toast.LENGTH_LONG).show();
                finish(); // Cierra esta pantalla al convertirse en premium
            }
        });

        Button btnLoQuiero = findViewById(R.id.btnLoQuiero);
        Button btnInicio = findViewById(R.id.btnInicio); // nuevo botón para volver al inicio

        // Acción para el botón de compra
        btnLoQuiero.setOnClickListener(v -> {
            if (billingManager != null) {
                billingManager.iniciarCompraPremium(PromoPremiumActivity.this);
            } else {
                Toast.makeText(this, "Error al iniciar la compra", Toast.LENGTH_SHORT).show();
            }
        });

        // Acción para volver al MainActivity
        btnInicio.setOnClickListener(v -> {
            Intent intent = new Intent(PromoPremiumActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}
