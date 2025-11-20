package com.freddy.controldegastos.BD;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.freddy.controldegastos.GASTOS.Gasto;
import com.freddy.controldegastos.GastosFijos.GastoFijo;

@Database(entities = {Gasto.class, GastoFijo.class}, version = 3, exportSchema = false)

public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instancia;

    public abstract GastoDao gastoDao();
    public abstract GastoFijoDao gastoFijoDao();

    public static AppDatabase obtenerInstancia(Context context) {
        if (instancia == null) {
            instancia = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "mi_base_datos")
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instancia;
    }
}

