package com.freddy.controldegastos.GASTOS;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;
import com.freddy.controldegastos.UTILS.GraficasUtils;
import android.graphics.Bitmap;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import com.freddy.controldegastos.UTILS.BackupHelper;
import com.freddy.controldegastos.UTILS.GastosFijosReceiver;
import android.content.pm.PackageManager;
import android.os.Build;
import com.freddy.controldegastos.UTILS.EditarPerfilActivity;
import java.util.Locale;

import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.freddy.controldegastos.AUTH.LoginActivity;
import com.freddy.controldegastos.BD.AppDatabase;
import com.freddy.controldegastos.BD.GastoDao;
import com.freddy.controldegastos.GastosFijos.GastoFijo;
import com.freddy.controldegastos.GastosFijos.GastosFijosActivity;
import com.freddy.controldegastos.PREMIUM.ExportarPdfHelper;
import com.freddy.controldegastos.PREMIUM.PromoPremiumActivity;
import com.freddy.controldegastos.R;
import com.freddy.controldegastos.UTILS.BillingManager;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import android.net.Uri;
import java.util.Locale;

import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

public class MainActivity extends AppCompatActivity {

    private static final int CODIGO_AGREGAR_GASTO = 1;
    private static final int RC_EDITAR_PERFIL = 2001;
    private static final int LIMITE_GASTOS_MENSUALES = 20;

    // Header del Drawer
    private ImageView imgNavFoto;
    private TextView txtNombreUsuario, txtCorreoUsuario;

    private RecyclerView recyclerViewGastos;
    private GastoAdapterRecycler adapter;

    private List<Gasto> listaGastos;
    private GastoDao gastoDao;
    private TextView txtIngresoResumen, txtGastadoResumen, txtSaldoResumen;

    private SharedPreferences prefs;
    private BillingManager billingManager;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private ActionBarDrawerToggle toggle;

    private InterstitialAd interstitialAdEditar;


    // Premium
    private boolean esPremium = false;
    private boolean premiumResuelto = false;

    private Spinner spnCategoria;
    private ArrayAdapter<String> categoriasAdapter;
    private final List<String> categorias = new ArrayList<>();
    private String categoriaSeleccionada = "Todos";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }

        prefs = getSharedPreferences("mis_datos", MODE_PRIVATE);
        esPremium = prefs.getBoolean("es_premium", false);

        billingManager = new BillingManager(this, isUserPremium -> {
            esPremium = isUserPremium;
            prefs.edit().putBoolean("es_premium", esPremium).apply();
            if (premiumResuelto) aplicarUIporPremium();
        });

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // ---- Header del NavigationView ----
        View headerView = navigationView.getHeaderView(0);
        imgNavFoto       = headerView.findViewById(R.id.imgNavFoto);
        txtNombreUsuario = headerView.findViewById(R.id.txtNombreUsuario);
        txtCorreoUsuario = headerView.findViewById(R.id.txtCorreoUsuario);

        TextView tvVersion = headerView.findViewById(R.id.tvVersionApp);
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            if (tvVersion != null) tvVersion.setText("Versión " + versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario != null) {
            txtCorreoUsuario.setText(usuario.getEmail());
        }
        cargarPerfilHeader(); // pinta nombre y foto desde Firebase

        // ---- Menú lateral ----
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_manual_usuario) {
                mostrarManualUsuario();
            } else if (id == R.id.nav_cerrar_sesion) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(MainActivity.this, LoginActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            } else if (id == R.id.nav_beneficios_premium) {
                mostrarDialogoBeneficios();
            } else if (id == R.id.nav_grafica_categorias) {
                if (!ensurePremiumOrWarn()) return true;
                startActivity(new Intent(MainActivity.this, GraficasActivity.class));
            } else if (id == R.id.nav_exportar_pdf) {
                if (!ensurePremiumOrWarn()) return true;

                AppDatabase db = AppDatabase.obtenerInstancia(MainActivity.this);
                List<Gasto> gastos = db.gastoDao().obtenerTodos();
                List<com.freddy.controldegastos.GastosFijos.GastoFijo> fijos = db.gastoFijoDao().obtenerTodos();

                Bitmap graficoPastel = GraficasUtils.generarGraficoPastel(MainActivity.this, gastos);
                Bitmap graficoBarras = GraficasUtils.generarGraficoBarras(MainActivity.this, gastos);

                String fechaParaFijos = null;    // usa la de hoy
                boolean soloPagados = true;      // igual que tu saldo

                double ingresoMensual = prefs.getFloat("ingreso_mensual", 0f);

                // Llamada al helper nuevo (mismo formato de tabla)
                ExportarPdfHelper.exportarPDF(
                        MainActivity.this,
                        gastos,
                        fechaParaFijos,
                        fijos,
                        soloPagados,
                        ingresoMensual,
                        graficoPastel,
                        graficoBarras);
            } else if (id == R.id.nav_guardar_backup) {
                if (!ensurePremiumOrWarn()) return true;
                BackupHelper.hacerBackup(this);
            } else if (id == R.id.nav_restaurar_backup) {
                if (!ensurePremiumOrWarn()) return true;
                BackupHelper.confirmarYRestaurarBackup(this, adapter, listaGastos, this::actualizarResumen);
            } else if (id == R.id.nav_editar_perfil) {
                // Abrir esperando resultado para refrescar al volver
                Intent i = new Intent(MainActivity.this, EditarPerfilActivity.class);
                startActivityForResult(i, RC_EDITAR_PERFIL);
            } else if (id == R.id.nav_agregar_ingreso) {
                // Nueva opción para agregar ingreso
                Intent i = new Intent(MainActivity.this, AgregarGastoActivity.class);
                i.putExtra("modo_ingreso", true);
                startActivityForResult(i, CODIGO_AGREGAR_GASTO);
            }
            // 👇👇👇 Caso "Contáctenos"
            else if (id == R.id.nav_contactenos) {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:")); // solo apps de correo
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"controldegastosoficial@gmail.com"});
                intent.putExtra(Intent.EXTRA_SUBJECT, "Sugerencias / Reporte de errores - Control de Gastos");
                intent.putExtra(Intent.EXTRA_TEXT,
                        "Hola, quería reportar lo siguiente (describe el problema, pasos y pantalla):\n\n");
                try {
                    startActivity(Intent.createChooser(intent, "Enviar correo con..."));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(this, "No tienes una app de correo instalada", Toast.LENGTH_SHORT).show();
                }
            }
            // ☝️☝️☝️ FIN del caso

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });




        // ---- Listado y lógica de gastos ----
        gastoDao = AppDatabase.obtenerInstancia(this).gastoDao();
        listaGastos = gastoDao.obtenerTodos();

        recyclerViewGastos = findViewById(R.id.recyclerViewGastos);
        recyclerViewGastos.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GastoAdapterRecycler(listaGastos, gasto -> {
            Runnable abrirEdicion = () -> {
                Intent intent = new Intent(MainActivity.this, AgregarGastoActivity.class);
                intent.putExtra("modo_edicion", true);
                intent.putExtra("modo_ingreso", gasto.isEsIngreso()); // ← clave
                intent.putExtra("id", gasto.getId());
                intent.putExtra("descripcion", gasto.getDescripcion());
                intent.putExtra("monto", gasto.getMonto());
                intent.putExtra("fecha", gasto.getFecha());
                intent.putExtra("categoria", gasto.getCategoria());
                startActivityForResult(intent, CODIGO_AGREGAR_GASTO);
            };


            if (!premiumResuelto) {
                Toast.makeText(this, "Comprobando tu estado Premium…", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!esPremium && interstitialAdEditar != null) {
                interstitialAdEditar.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        interstitialAdEditar = null;
                        cargarAnuncioEditar();
                        abrirEdicion.run();
                    }
                });
                interstitialAdEditar.show(MainActivity.this);
            } else {
                abrirEdicion.run();
            }
        });
        recyclerViewGastos.setAdapter(adapter);

        //  Referencia al Spinner
        spnCategoria = findViewById(R.id.spnCategoria);

        ImageButton btnBorrarFiltro = findViewById(R.id.btnBorrarFiltro);
        btnBorrarFiltro.setOnClickListener(v -> confirmarBorradoSegunFiltro());


// Listener del Spinner (se configura una sola vez)
        spnCategoria.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                categoriaSeleccionada = categorias.get(position);
                cargarGastosFiltrados(categoriaSeleccionada);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

// Primera carga al abrir la app
        recargarCategoriasYReseleccionar();
        cargarGastosFiltrados(categoriaSeleccionada);


// Listener: aplicar filtro al cambiar selección
        spnCategoria.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                categoriaSeleccionada = categorias.get(position);
                cargarGastosFiltrados(categoriaSeleccionada);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { /* no-op */ }
        });

// Carga inicial
        cargarGastosFiltrados(categoriaSeleccionada);


// Carga inicial de la lista según selección
        cargarGastosFiltrados(categoriaSeleccionada);


        txtIngresoResumen = findViewById(R.id.txtIngresoResumen);
        txtGastadoResumen = findViewById(R.id.txtGastadoResumen);
        txtSaldoResumen = findViewById(R.id.txtSaldoResumen);

        findViewById(R.id.btnAgregar).setOnClickListener(v -> {
            if (!premiumResuelto) {
                Toast.makeText(this, "Comprobando tu estado Premium…", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!esPremium && contarGastosDelMesActual() >= LIMITE_GASTOS_MENSUALES) {
                Toast.makeText(this, "Límite mensual alcanzado (20). Hazte Premium para agregar más.", Toast.LENGTH_LONG).show();
            } else {
                startActivityForResult(new Intent(this, AgregarGastoActivity.class), CODIGO_AGREGAR_GASTO);
            }
        });

        findViewById(R.id.btnIngresoMensual).setOnClickListener(v -> {
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            input.setHint("Ej: 1000.00");

            new AlertDialog.Builder(this)
                    .setTitle("Ingreso Mensual")
                    .setMessage("Ingresa el monto de tu ingreso mensual:")
                    .setView(input)
                    .setPositiveButton("Guardar", (dialog, which) -> {
                        try {
                            float ingreso = Float.parseFloat(input.getText().toString().trim());
                            prefs.edit().putFloat("ingreso_mensual", ingreso).apply();
                            actualizarResumen();
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        findViewById(R.id.btnVerGastosFijos).setOnClickListener(v -> {
            startActivity(new Intent(this, GastosFijosActivity.class));
        });

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            int pos;

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;

                // Tomamos el gasto según la lista visible (respeta el filtro actual)
                Gasto seleccionado = listaGastos.get(pos);

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Eliminar gasto")
                        .setMessage("¿Deseas eliminar este gasto?")
                        .setPositiveButton("Eliminar", (dialog, which) -> {
                            // Eliminar en BD
                            gastoDao.eliminar(seleccionado);

                            recargarCategoriasYReseleccionar();
                            cargarGastosFiltrados(categoriaSeleccionada);
                            actualizarResumen();

                            // Mensaje opcional
                            Toast.makeText(MainActivity.this, "Gasto eliminado", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancelar", (dialog, which) -> {
                            // Restaurar el ítem si cancelan
                            adapter.notifyItemChanged(pos);
                        })
                        .setOnCancelListener(dialog -> {
                            // Si cierran el diálogo sin elegir, también restaurar
                            adapter.notifyItemChanged(pos);
                        })
                        .show();
            }
        }).attachToRecyclerView(recyclerViewGastos);

        actualizarResumen();
        refrescarPremium(); // ← clave
    }


    private void confirmarBorradoSegunFiltro() {
        final String cat = categoriaSeleccionada; // por si cambia durante el diálogo
        String titulo, mensaje;

        if ("Todos".equals(cat)) {
            titulo = "Borrar todos";
            mensaje = "¿Seguro que deseas borrar TODOS los registros (gastos e ingresos)?";
        } else {
            titulo = "Borrar categoría";
            mensaje = "¿Borrar todos los registros de \"" + cat + "\"?";
        }

        new AlertDialog.Builder(this)
                .setTitle(titulo)
                .setMessage(mensaje)
                .setPositiveButton("Borrar", (d, w) -> {
                    if ("Todos".equals(cat)) {
                        gastoDao.eliminarTodos();
                    } else {
                        gastoDao.eliminarPorCategoria(cat);
                    }
                    // Refrescar UI
                    recargarCategoriasYReseleccionar();            // actualiza opciones del Spinner
                    cargarGastosFiltrados(categoriaSeleccionada);  // respeta selección (cambiará a "Todos" si desaparece)
                    actualizarResumen();
                    Toast.makeText(this, "Registros eliminados", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }


    private void recargarCategoriasYReseleccionar() {
        new Thread(() -> {
            // 1) Consultar categorías
            List<String> result = gastoDao.obtenerCategorias();
            List<String> lista = new ArrayList<>();
            lista.add("Todos");
            if (result != null) lista.addAll(result);

            // 2) Calcular selección nueva
            String tmpSeleccion = "Todos";
            if (categoriaSeleccionada != null && lista.contains(categoriaSeleccionada)) {
                tmpSeleccion = categoriaSeleccionada;
            }

            // 3) Copias FINALES para usarlas dentro del lambda
            final List<String> listaFinal = new ArrayList<>(lista);
            final String seleccionFinal = tmpSeleccion;

            // 4) Actualizar UI
            runOnUiThread(() -> {
                categorias.clear();
                categorias.addAll(listaFinal);

                if (categoriasAdapter == null) {
                    categoriasAdapter = new ArrayAdapter<>(
                            MainActivity.this,
                            android.R.layout.simple_spinner_item,
                            categorias
                    );
                    categoriasAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spnCategoria.setAdapter(categoriasAdapter);
                } else {
                    categoriasAdapter.notifyDataSetChanged();
                }

                int idx = categorias.indexOf(seleccionFinal);
                spnCategoria.setSelection(Math.max(idx, 0), false);
                categoriaSeleccionada = seleccionFinal;
            });
        }).start();
    }

    private void cargarGastosFiltrados(String categoria) {
        new Thread(() -> {
            List<Gasto> data;
            if (categoria == null || "Todos".equals(categoria)) {
                // Lista completa
                data = gastoDao.obtenerTodos();
            } else {
                // Solo la categoría elegida
                data = gastoDao.obtenerPorCategoria(categoria);
            }

            runOnUiThread(() -> {
                // Actualiza el adapter y la lista local (que usas en otras funciones)
                adapter.setItems(data);
                listaGastos.clear();
                if (data != null) listaGastos.addAll(data);
            });
        }).start();
    }



    // ---------- Perfil: cargar header ----------

    private void cargarPerfilHeader() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) {
            txtNombreUsuario.setText("Usuario");
            imgNavFoto.setImageResource(R.drawable.ic_person);
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference("usuarios")
                .child(u.getUid())
                .get()
                .addOnSuccessListener(s -> {
                    String nombre = "Usuario";
                    String fotoUrl = null;

                    Object n = s.child("nombre").getValue();
                    if (n != null) nombre = String.valueOf(n);
                    Object f = s.child("fotoPerfil").getValue();
                    if (f != null) fotoUrl = String.valueOf(f);

                    txtNombreUsuario.setText(nombre);

                    if (fotoUrl != null && !fotoUrl.isEmpty()) {
                        Glide.with(this)
                                .load(fotoUrl)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .placeholder(R.drawable.ic_person)
                                .error(R.drawable.ic_person)
                                .circleCrop()
                                .into(imgNavFoto);
                    } else {
                        imgNavFoto.setImageResource(R.drawable.ic_person);
                    }
                });
    }

    // ---------- Premium: resolver, aplicar y gating ----------

    private void refrescarPremium() {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            esPremium = false;
            premiumResuelto = true;
            aplicarUIporPremium();
            return;
        }

        DatabaseReference refPremium = FirebaseDatabase.getInstance()
                .getReference("usuarios")
                .child(usuario.getUid())
                .child("esPremium");

        refPremium.get().addOnSuccessListener(snapshot -> {
            Boolean server = snapshot.exists() ? snapshot.getValue(Boolean.class) : Boolean.FALSE;
            esPremium = server != null && server;
            prefs.edit().putBoolean("es_premium", esPremium).apply();
            premiumResuelto = true;
            aplicarUIporPremium();
        }).addOnFailureListener(e -> {
            esPremium = prefs.getBoolean("es_premium", false);
            premiumResuelto = true;
            aplicarUIporPremium();
        });
    }

    private void aplicarUIporPremium() {
        if (esPremium) {
            interstitialAdEditar = null;
            programarRecordatorioLunes();
        } else {
            cargarAnuncioEditar();
        }
        if (!esPremium) {
            mostrarPromoSiEsNecesario();
        }
    }

    private boolean ensurePremiumOrWarn() {
        if (!premiumResuelto) {
            Toast.makeText(this, "Comprobando tu estado Premium…", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!esPremium) {
            Toast.makeText(this, "Función disponible solo en versión Premium.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void programarRecordatorioLunes() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, GastosFijosReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1);
        }

        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY * 7,
                pendingIntent
        );
    }

    private void cargarAnuncioEditar() {
        if (esPremium) { interstitialAdEditar = null; return; }

        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, "ca-app-pub-2503649416779224/1348229276", adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        interstitialAdEditar = ad;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                        interstitialAdEditar = null;
                    }
                });
    }

    private int contarGastosDelMesActual() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        String mesActual = sdf.format(Calendar.getInstance().getTime()); // ej: 2025-08
        int conteo = 0;
        for (Gasto g : listaGastos) {
            if (!g.isEsIngreso() && g.getFecha() != null && g.getFecha().startsWith(mesActual)) {
                conteo++;
            }
        }
        return conteo;
    }


    private void actualizarResumen() {
        float ingresoMensual = prefs.getFloat("ingreso_mensual", 0f);

        // Totales de Room
        GastoDao gdao = AppDatabase.obtenerInstancia(this).gastoDao();
        double totalGastosNormales = gdao.sumaGastos();   // esIngreso = 0
        double ingresosExtra = gdao.sumaIngresos();       // esIngreso = 1

        // Gastos fijos pagados
        double gastadoFijos = 0;
        List<GastoFijo> listaFijos = AppDatabase.obtenerInstancia(this).gastoFijoDao().obtenerTodos();
        for (GastoFijo gf : listaFijos) {
            if (gf.isPagado()) gastadoFijos += gf.getMonto();
        }

        double totalGastado = totalGastosNormales + gastadoFijos;
        double saldo = ingresoMensual + ingresosExtra - totalGastado;

        //  Aquí va el formato con paréntesis para los ingresos extra
        if (ingresosExtra > 0.0001) {
            txtIngresoResumen.setText(String.format(Locale.getDefault(),
                    "Ingreso mensual: $%.2f (+%.2f)", ingresoMensual, ingresosExtra));
        } else {
            txtIngresoResumen.setText(String.format(Locale.getDefault(),
                    "Ingreso mensual: $%.2f", ingresoMensual));
        }

        txtGastadoResumen.setText(String.format(Locale.getDefault(),
                "Gastado: $%.2f", totalGastado));
        txtSaldoResumen.setText(String.format(Locale.getDefault(),
                "Saldo disponible: $%.2f", saldo));
    }


    private void mostrarPromoSiEsNecesario() {
        if (!esPremium) {
            long ultimaVez = prefs.getLong("promo_mostrada", 0);
            long ahora = System.currentTimeMillis();
            long unDia = 24 * 60 * 60 * 1000;

            if (ahora - ultimaVez > unDia) {
                prefs.edit().putLong("promo_mostrada", ahora).apply();
                startActivity(new Intent(this, PromoPremiumActivity.class));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //  Ahora respeta la categoría elegida en el Spinner
        cargarGastosFiltrados(categoriaSeleccionada);
        actualizarResumen();
        // (Opcional) refrescar header siempre que vuelves
        // cargarPerfilHeader();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //  Volviste de AgregarGastoActivity (agregar o editar)
        if (requestCode == CODIGO_AGREGAR_GASTO && resultCode == RESULT_OK) {
            // Ya NO leemos extras ni reinsertamos aquí: eso lo hizo AgregarGastoActivity
            recargarCategoriasYReseleccionar();            // actualiza opciones del Spinner
            cargarGastosFiltrados(categoriaSeleccionada);  // respeta la categoría actual
            actualizarResumen();                           // recalcula totales
            return;
        }

        //  Volviste de Editar Perfil (deja tu lógica tal cual)
        if (requestCode == RC_EDITAR_PERFIL && resultCode == RESULT_OK) {
            String nombre = data != null ? data.getStringExtra("nombre_actualizado") : null;
            String foto = data != null ? data.getStringExtra("foto_actualizada") : null;

            if (nombre != null) txtNombreUsuario.setText(nombre);
            if (foto != null) {
                if (!foto.isEmpty()) {
                    Glide.with(this)
                            .load(foto)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .circleCrop()
                            .into(imgNavFoto);
                } else {
                    imgNavFoto.setImageResource(R.drawable.ic_person);
                }
            }
            if (nombre == null && foto == null) {
                cargarPerfilHeader(); // fallback
            }
        }
    }

    private void mostrarManualUsuario() {
        new AlertDialog.Builder(this)
                .setTitle("Ayuda de la aplicación")
                .setMessage(
                        "⭐ Bienvenido a Control de Gastos\n\n" +
                                "🔢 Agrega tu ingreso mensual con el botón correspondiente.\n" +
                                "💵 Añades tus ingresos extras en el menú.\n" +
                                "📝 Agrega gastos diarios con 'Agregar Gasto'.\n" +
                                "📃 Usa 'Ver Gastos Fijos' para pagos recurrentes (alquiler, luz, etc.).\n" +
                                "✅ Marca como pagados los gastos fijos cuando corresponda.\n" +
                                "✏️ Toca un gasto para editarlo.\n" +
                                "🗑️ Desliza un gasto para eliminarlo.\n" +
                                "💰 El saldo se actualiza automáticamente.\n" +
                                "👑 Revisa las ventajas de la versión Premium.\n\n" +
                                "✨ ¡Administra tu dinero con facilidad!")
                .setPositiveButton("ENTENDIDO", null)
                .show();
    }

    private void mostrarDialogoBeneficios() {
        new AlertDialog.Builder(this)
                .setTitle("Beneficios de la Versión Premium")
                .setMessage(
                        "🎉 ¡Con Premium obtienes mucho más!\n\n" +
                                "✅ Elimina todos los anuncios\n" +
                                "📊 Gráficas por categoría\n" +
                                "☁️ Backup en la nube\n" +
                                "🔔 Notificaciones de gastos fijos\n" +
                                "📄 Exportación a PDF\n" +
                                "🔗 Sincronización entre dispositivos\n\n" +
                                "¿Quieres más detalles o adquirir Premium?")
                .setNegativeButton("Ahora no", null)
                .setPositiveButton("Obtener beneficios Premium", (d, w) -> {
                    if (billingManager != null) {
                        billingManager.iniciarCompraPremium(MainActivity.this);
                    } else {
                        Toast.makeText(this, "Error al iniciar la compra", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

}
