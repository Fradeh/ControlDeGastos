package com.freddy.controldegastos.GastosFijos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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
        holder.txtEstado.setText(gasto.isPagado() ? "Pagado" : "Pendiente");
        holder.txtEstado.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), gasto.isPagado() ? R.color.fixed_paid_text : R.color.fixed_pending_text));
        holder.txtEstado.setBackgroundResource(gasto.isPagado() ? R.drawable.bg_fixed_status_paid : R.drawable.bg_fixed_status_pending);
        holder.layoutItem.setBackgroundResource(gasto.isPagado() ? R.drawable.bg_fixed_item_paid : R.drawable.bg_fixed_item_pending);

        holder.checkPagado.setOnCheckedChangeListener(null);
        holder.checkPagado.setChecked(gasto.isPagado());
        holder.checkPagado.setOnCheckedChangeListener((buttonView, isChecked) -> {
            gasto.setPagado(isChecked);
            holder.txtEstado.setText(isChecked ? "Pagado" : "Pendiente");
            holder.txtEstado.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), isChecked ? R.color.fixed_paid_text : R.color.fixed_pending_text));
            holder.txtEstado.setBackgroundResource(isChecked ? R.drawable.bg_fixed_status_paid : R.drawable.bg_fixed_status_pending);
            holder.layoutItem.setBackgroundResource(isChecked ? R.drawable.bg_fixed_item_paid : R.drawable.bg_fixed_item_pending);
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
        LinearLayout layoutItem;
        TextView txtNombre, txtMonto, txtEstado;
        CheckBox checkPagado;

        public GastoFijoViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutItem = itemView.findViewById(R.id.layoutGastoFijoItem);
            txtNombre = itemView.findViewById(R.id.txtNombreGastoFijo);
            txtMonto = itemView.findViewById(R.id.txtMontoGastoFijo);
            txtEstado = itemView.findViewById(R.id.txtEstadoGastoFijo);
            checkPagado = itemView.findViewById(R.id.checkPagado);
        }
    }
}
