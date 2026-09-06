package com.william.saldocerto.model;

import org.json.JSONException;
import org.json.JSONObject;

public class MetaFinanceira {
    private final String id;
    private final String nome;
    private final double atual;
    private final double objetivo;

    public MetaFinanceira(String id, String nome, double atual, double objetivo) {
        this.id = id;
        this.nome = nome;
        this.atual = atual;
        this.objetivo = objetivo;
    }

    public String getId() { return id; }
    public String getNome() { return nome; }
    public double getAtual() { return atual; }
    public double getObjetivo() { return objetivo; }
    public int getPercentual() {
        if (objetivo <= 0) return 0;
        return (int) Math.min(100, Math.round((atual / objetivo) * 100));
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("nome", nome);
        o.put("atual", atual);
        o.put("objetivo", objetivo);
        return o;
    }

    public static MetaFinanceira fromJson(JSONObject o) {
        return new MetaFinanceira(o.optString("id"), o.optString("nome"), o.optDouble("atual"), o.optDouble("objetivo"));
    }
}
