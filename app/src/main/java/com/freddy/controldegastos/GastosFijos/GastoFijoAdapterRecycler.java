package com.freddy.controldegastos.GastosFijos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.freddy.controldegastos.R;

import java.util.List;

public class GastoFijoAdapterRecycler extends RecyclerView.Adapter<GastoFijoAdapterRecycler.GastoFijoViewHolder> {

    public interface OnGastoChangedListener {
        void onGastoCheckChanged();
    }

    private List<GastoFijo> listaGastosFijos;
    private OnGastoChangedListener callback;

    public GastoFijoAdapterRecycler(List<GastoFijo> listaGastosFijos, OnGastoChangedListener callback) {
        this.listaGastosFijos = listaGastosFijos;
        this.callback = callback;
    }

    @NonNull
    @Override
    public GastoFijoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gasto_fijo, parent, false);
        return new GastoFijoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GastoFijoViewHolder holder, int position) {
        GastoFijo gasto = listaGastosFijos.get(position);
        holder.txtNombre.setText(gasto.getDescripcion());
        holder.txtMonto.setText("$" + String.format("%.2f", gasto.getMonto()));

        holder.checkPagado.setOnCheckedChangeListener(null);
        holder.checkPagado.setChecked(gasto.isPagado());
        holder.checkPagado.setOnCheckedChangeListener((buttonView, isChecked) -> {
            gasto.setPagado(isChecked);
            if (callback != null) callback.onGastoCheckChanged();
        });
    }

    @Override
    public int getItemCount() {
        return listaGastosFijos.size();
    }

    public void removeAt(int position) {
        listaGastosFijos.remove(position);
        notifyItemRemoved(position);
        if (callback != null) callback.onGastoCheckChanged();
    }

    public static class GastoFijoViewHolder extends RecyclerView.ViewHolder {
        TextView txtNombre, txtMonto;
        CheckBox checkPagado;

        public GastoFijoViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNombre = itemView.findViewById(R.id.txtNombreGastoFijo);
            txtMonto = itemView.findViewById(R.id.txtMontoGastoFijo);
            checkPagado = itemView.findViewById(R.id.checkPagado);
        }
    }
}
