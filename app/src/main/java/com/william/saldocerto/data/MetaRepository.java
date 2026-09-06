package com.william.saldocerto.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.william.saldocerto.model.MetaFinanceira;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MetaRepository {
    private static final String PREFS = "saldo_certo_prefs";
    private static final String KEY = "metas";
    private final SharedPreferences prefs;

    public MetaRepository(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.contains(KEY)) resetDemoData();
    }

    public List<MetaFinanceira> getAll() {
        List<MetaFinanceira> lista = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs.getString(KEY, "[]"));
            for (int i = 0; i < arr.length(); i++) lista.add(MetaFinanceira.fromJson(arr.getJSONObject(i)));
        } catch (JSONException ignored) { }
        return lista;
    }

    public void add(String nome, double atual, double objetivo) {
        List<MetaFinanceira> lista = getAll();
        lista.add(0, new MetaFinanceira(UUID.randomUUID().toString(), nome, Math.max(0, atual), objetivo));
        save(lista);
    }

    public void update(MetaFinanceira updated) {
        List<MetaFinanceira> lista = getAll();
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).getId().equals(updated.getId())) {
                lista.set(i, updated);
                save(lista);
                return;
            }
        }
    }

    public void adjustCurrent(String id, double delta) {
        List<MetaFinanceira> lista = getAll();
        for (int i = 0; i < lista.size(); i++) {
            MetaFinanceira m = lista.get(i);
            if (m.getId().equals(id)) {
                double novoAtual = Math.max(0, m.getAtual() + delta);
                lista.set(i, new MetaFinanceira(m.getId(), m.getNome(), novoAtual, m.getObjetivo()));
                save(lista);
                return;
            }
        }
    }

    public void delete(String id) {
        List<MetaFinanceira> lista = getAll();
        lista.removeIf(m -> m.getId().equals(id));
        save(lista);
    }

    public void resetDemoData() {
        List<MetaFinanceira> demo = new ArrayList<>();
        demo.add(new MetaFinanceira(UUID.randomUUID().toString(), "Reserva de emergência", 3200, 10000));
        demo.add(new MetaFinanceira(UUID.randomUUID().toString(), "Computador novo", 2500, 5000));
        demo.add(new MetaFinanceira(UUID.randomUUID().toString(), "Viagem", 1200, 4000));
        save(demo);
    }

    private void save(List<MetaFinanceira> lista) {
        JSONArray arr = new JSONArray();
        for (MetaFinanceira m : lista) {
            try { arr.put(m.toJson()); } catch (JSONException ignored) { }
        }
        prefs.edit().putString(KEY, arr.toString()).apply();
    }
}
