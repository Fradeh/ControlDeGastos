package com.freddy.controldegastos.GASTOS;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.freddy.controldegastos.R;

import java.util.List;

public class GastoAdapter extends ArrayAdapter<Gasto> {
    public GastoAdapter(Context context, List<Gasto> gastos) {
        super(context, 0, gastos);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Gasto gasto = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_gasto, parent, false);
        }
        TextView txtCategoria = convertView.findViewById(R.id.txtCategoria);
        txtCategoria.setText(gasto.getCategoria());
        TextView txtDescripcion = convertView.findViewById(R.id.txtDescripcion);
        TextView txtMonto = convertView.findViewById(R.id.txtMonto);
        TextView txtFecha = convertView.findViewById(R.id.txtFecha);

        txtDescripcion.setText(gasto.getDescripcion());
        txtMonto.setText(String.format("$ %.2f", gasto.getMonto()));
        txtFecha.setText(gasto.getFecha());

        return convertView;
    }
}
