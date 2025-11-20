package com.freddy.controldegastos.GastosFijos;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "gastos_fijos")
public class GastoFijo {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String descripcion;
    private double monto;
    private String categoria;
    private boolean pagado;

    public GastoFijo() {
    }

    @Ignore
    public GastoFijo(String descripcion, double monto, String categoria, boolean pagado) {
        this.descripcion = descripcion;
        this.monto = monto;
        this.categoria = categoria;
        this.pagado = pagado;
    }

    // Getters y setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public boolean isPagado() { return pagado; }
    public void setPagado(boolean pagado) { this.pagado = pagado; }
}
