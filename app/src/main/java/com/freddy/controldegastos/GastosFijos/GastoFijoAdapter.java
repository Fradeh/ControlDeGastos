package com.freddy.controldegastos.GastosFijos;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.freddy.controldegastos.R;

import java.util.List;

public class GastoFijoAdapter extends BaseAdapter {
    private Context context;
    private List<GastoFijo> listaGastosFijos;
    private Runnable onCheckChangedCallback;

    public GastoFijoAdapter(Context context, List<GastoFijo> listaGastosFijos) {
        this.context = context;
        this.listaGastosFijos = listaGastosFijos;
    }

    // NUEVO
    public void setOnCheckChangedCallback(Runnable onCheckChangedCallback) {
        this.onCheckChangedCallback = onCheckChangedCallback;
    }

    @Override
    public int getCount() { return listaGastosFijos.size(); }

    @Override
    public Object getItem(int position) { return listaGastosFijos.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_gasto_fijo, parent, false);
        } else {
            view = convertView;
        }

        TextView txtNombre = view.findViewById(R.id.txtNombreGastoFijo);
        TextView txtMonto = view.findViewById(R.id.txtMontoGastoFijo);
        CheckBox checkPagado = view.findViewById(R.id.checkPagado);

        GastoFijo gastoFijo = listaGastosFijos.get(position);

        txtNombre.setText(gastoFijo.getDescripcion());
        txtMonto.setText("$" + String.format("%.2f", gastoFijo.getMonto()));

        // MUY IMPORTANTE para evitar errores visuales
        checkPagado.setOnCheckedChangeListener(null);
        checkPagado.setChecked(gastoFijo.isPagado());

        checkPagado.setOnCheckedChangeListener((buttonView, isChecked) -> {
            gastoFijo.setPagado(isChecked);
            if (onCheckChangedCallback != null) onCheckChangedCallback.run();
        });

        return view;
    }
}
