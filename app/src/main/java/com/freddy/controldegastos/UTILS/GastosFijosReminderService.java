package com.freddy.controldegastos.UTILS;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

public class GastosFijosReminderService extends JobIntentService {

    private static final int JOB_ID = 1002;

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        NotificationHelper.mostrarNotificacion(
                this,
                "Recordatorio de Gastos Fijos",
                "¡Revisa y marca tus gastos fijos esta semana!"
        );
    }

    public static void enqueueWork(Context context) {
        enqueueWork(context, GastosFijosReminderService.class, JOB_ID, new Intent());
    }
}
