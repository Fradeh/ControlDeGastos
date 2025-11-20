package com.freddy.controldegastos.GASTOS;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "gastos")
public class Gasto {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String descripcion;
    private double monto;
    private String fecha;
    private String categoria;

    // NUEVO -> marca si es un ingreso (true) o un gasto (false)
    private boolean esIngreso = false;

    public Gasto() { }

    @Ignore
    public Gasto(String descripcion, double monto, String fecha, String categoria) {
        this.descripcion = descripcion;
        this.monto = monto;
        this.fecha = fecha;
        this.categoria = categoria;
    }

    // Getters y setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    // NUEVOS getters/setters
    public boolean isEsIngreso() { return esIngreso; }
    public void setEsIngreso(boolean esIngreso) { this.esIngreso = esIngreso; }
}
