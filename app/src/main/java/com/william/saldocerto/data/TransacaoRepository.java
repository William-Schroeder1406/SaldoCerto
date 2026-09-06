package com.william.saldocerto.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.william.saldocerto.model.Transacao;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransacaoRepository {
    private static final String PREFS = "saldo_certo_prefs";
    private static final String KEY = "transacoes";
    private final SharedPreferences prefs;

    public TransacaoRepository(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.contains(KEY)) resetDemoData();
    }

    public List<Transacao> getAll() {
        List<Transacao> lista = new ArrayList<>();
        String raw = prefs.getString(KEY, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) lista.add(Transacao.fromJson(arr.getJSONObject(i)));
        } catch (JSONException ignored) { }
        return lista;
    }

    public void add(String descricao, double valor, String tipo, String categoria, String data,
                    String formaPagamento, String observacao) {
        List<Transacao> lista = getAll();
        lista.add(0, new Transacao(UUID.randomUUID().toString(), descricao, valor, tipo, categoria,
                data, formaPagamento, observacao));
        save(lista);
    }

    public void update(Transacao transacao) {
        List<Transacao> lista = getAll();
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).getId().equals(transacao.getId())) {
                lista.set(i, transacao);
                break;
            }
        }
        save(lista);
    }

    public void delete(String id) {
        List<Transacao> lista = getAll();
        lista.removeIf(t -> t.getId().equals(id));
        save(lista);
    }

    public void resetDemoData() {
        List<Transacao> demo = new ArrayList<>();
        demo.add(new Transacao(UUID.randomUUID().toString(), "Salário", 4500.00, "receita", "Salário", "05/09/2026", "PIX", "Receita mensal"));
        demo.add(new Transacao(UUID.randomUUID().toString(), "Supermercado", 650.00, "despesa", "Alimentação", "04/09/2026", "Cartão de crédito", "Compras do mês"));
        demo.add(new Transacao(UUID.randomUUID().toString(), "Aluguel", 900.00, "despesa", "Moradia", "02/09/2026", "PIX", ""));
        demo.add(new Transacao(UUID.randomUUID().toString(), "Combustível", 420.00, "despesa", "Transporte", "01/09/2026", "Cartão de crédito", ""));
        demo.add(new Transacao(UUID.randomUUID().toString(), "Internet", 99.90, "despesa", "Moradia", "31/08/2026", "Débito", ""));
        save(demo);
    }

    private void save(List<Transacao> lista) {
        JSONArray arr = new JSONArray();
        for (Transacao t : lista) {
            try { arr.put(t.toJson()); } catch (JSONException ignored) { }
        }
        prefs.edit().putString(KEY, arr.toString()).apply();
    }
}
