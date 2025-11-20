package com.freddy.controldegastos.BD;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.freddy.controldegastos.GastosFijos.GastoFijo;
import java.util.List;

@Dao
public interface GastoFijoDao {

    @Query("SELECT * FROM gastos_fijos")
    List<GastoFijo> obtenerTodos();

    @Insert
    void insertar(GastoFijo gastoFijo);

    @Update
    void actualizar(GastoFijo gastoFijo);

    @Delete
    void eliminar(GastoFijo gastoFijo);

    @Query("DELETE FROM gastos_fijos")
    void eliminarTodos();

    // --- total de fijos pagados ---
    @Query("SELECT IFNULL(SUM(monto), 0) FROM gastos_fijos WHERE pagado = 1")
    double totalFijosPagados();
}
