package com.freddy.controldegastos.AUTH;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.freddy.controldegastos.R;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {
    private final Handler handler = new Handler();
    private Runnable task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        task = () -> {
            Intent intent;
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                //  Ir a InicioActivity para que ahí decidas: anuncio si no es premium, o directo a Main si es premium.
                intent = new Intent(this, InicioActivity.class);
            } else {
                intent = new Intent(this, LoginActivity.class);
            }
            startActivity(intent);
            finish();
        };
        handler.postDelayed(task, 1500);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (task != null) handler.removeCallbacks(task);
    }
}
