package com.freddy.controldegastos.UTILS;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class GastosFijosReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        GastosFijosReminderService.enqueueWork(context);
    }
}
