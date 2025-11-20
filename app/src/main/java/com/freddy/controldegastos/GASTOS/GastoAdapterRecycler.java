package com.freddy.controldegastos.GASTOS;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.freddy.controldegastos.R;

import java.util.List;

public class GastoAdapterRecycler extends RecyclerView.Adapter<GastoAdapterRecycler.GastoViewHolder> {

    private List<Gasto> listaGastos;
    private OnGastoClickListener listener;

    public interface OnGastoClickListener {
        void onGastoClick(Gasto gasto);
    }

    public GastoAdapterRecycler(List<Gasto> listaGastos, OnGastoClickListener listener) {
        this.listaGastos = listaGastos;
        this.listener = listener;
    }



    @NonNull
    @Override
    public GastoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gasto, parent, false);
        return new GastoViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull GastoViewHolder holder, int position) {
        Gasto gasto = listaGastos.get(position);

        holder.txtDescripcion.setText(gasto.getDescripcion());
        holder.txtMonto.setText("$" + String.format("%.2f", gasto.getMonto()));
        holder.txtFecha.setText(gasto.getFecha());

        if (gasto.isEsIngreso()) {
            holder.txtCategoria.setVisibility(View.GONE);
            // opcional: verde para ingresos
            holder.txtMonto.setTextColor(0xFF1B5E20); // verde
        } else {
            holder.txtCategoria.setVisibility(View.VISIBLE);
            holder.txtCategoria.setText(gasto.getCategoria());
            holder.txtMonto.setTextColor(0xFF1976D2); // color que ya usas
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onGastoClick(gasto);
        });
    }

    @Override
    public int getItemCount() {
        return listaGastos.size();
    }

    public void removeAt(int position) {
        listaGastos.remove(position);
        notifyItemRemoved(position);
    }

    public static class GastoViewHolder extends RecyclerView.ViewHolder {
        TextView txtDescripcion, txtMonto, txtFecha, txtCategoria;

        public GastoViewHolder(View itemView) {
            super(itemView);
            txtDescripcion = itemView.findViewById(R.id.txtDescripcion);
            txtMonto = itemView.findViewById(R.id.txtMonto);
            txtFecha = itemView.findViewById(R.id.txtFecha);
            txtCategoria = itemView.findViewById(R.id.txtCategoria);
        }
    }
    public void setItems(List<Gasto> nuevos) {
        this.listaGastos.clear();
        if (nuevos != null) this.listaGastos.addAll(nuevos);
        notifyDataSetChanged();
    }


}
