package com.freddy.controldegastos.BD;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.freddy.controldegastos.GASTOS.Gasto;
import java.util.List;

@Dao
public interface GastoDao {

    // Lista completa
    @Query("SELECT * FROM gastos ORDER BY fecha DESC")
    List<Gasto> obtenerTodos();

    @Insert
    void insertar(Gasto gasto);

    @Update
    void actualizar(Gasto gasto);

    @Delete
    void eliminar(Gasto gasto);

    @Query("DELETE FROM gastos")
    void eliminarTodos();


    @Query("SELECT IFNULL(SUM(monto), 0) FROM gastos WHERE esIngreso = 0")
    double totalGastosNormales();

    @Query("SELECT IFNULL(SUM(monto), 0) FROM gastos WHERE esIngreso = 1")
    double totalIngresos();


    @Query("SELECT IFNULL(SUM(monto), 0) FROM gastos WHERE esIngreso = 0")
    double sumaGastos();

    @Query("SELECT IFNULL(SUM(monto), 0) FROM gastos WHERE esIngreso = 1")
    double sumaIngresos();

    // Buscar por id
    @Query("SELECT * FROM gastos WHERE id = :id LIMIT 1")
    Gasto obtenerPorId(int id);

    //  Mostrar TODAS las categorías (gastos e ingresos)
    @Query("SELECT DISTINCT categoria FROM gastos " +
            "WHERE categoria IS NOT NULL AND categoria <> '' " +
            "ORDER BY categoria ASC")
    List<String> obtenerCategorias();

    //  Filtrar por categoría (gastos e ingresos)
    @Query("SELECT * FROM gastos WHERE categoria = :categoria ORDER BY fecha DESC")
    List<Gasto> obtenerPorCategoria(String categoria);

    @Query("DELETE FROM gastos WHERE categoria = :categoria")
    void eliminarPorCategoria(String categoria);

}
