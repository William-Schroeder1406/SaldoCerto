package com.william.saldocerto.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Transacao {
    private String id;
    private String descricao;
    private double valor;
    private String tipo;
    private String categoria;
    private String data;
    private String formaPagamento;
    private String observacao;

    public Transacao(String id, String descricao, double valor, String tipo, String categoria,
                     String data, String formaPagamento, String observacao) {
        this.id = id;
        this.descricao = descricao;
        this.valor = valor;
        this.tipo = tipo;
        this.categoria = categoria;
        this.data = data;
        this.formaPagamento = formaPagamento;
        this.observacao = observacao;
    }

    public String getId() { return id; }
    public String getDescricao() { return descricao; }
    public double getValor() { return valor; }
    public String getTipo() { return tipo; }
    public String getCategoria() { return categoria; }
    public String getData() { return data; }
    public String getFormaPagamento() { return formaPagamento; }
    public String getObservacao() { return observacao; }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("descricao", descricao);
        o.put("valor", valor);
        o.put("tipo", tipo);
        o.put("categoria", categoria);
        o.put("data", data);
        o.put("formaPagamento", formaPagamento);
        o.put("observacao", observacao);
        return o;
    }

    public static Transacao fromJson(JSONObject o) {
        return new Transacao(
                o.optString("id"),
                o.optString("descricao"),
                o.optDouble("valor"),
                o.optString("tipo"),
                o.optString("categoria"),
                o.optString("data"),
                o.optString("formaPagamento"),
                o.optString("observacao")
        );
    }
}
