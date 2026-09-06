package com.william.saldocerto.api;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class CotacaoService {
    // AwesomeAPI: retorna a última cotação disponível de USD/BRL com data/hora da fonte.
    public static final String ENDPOINT = "https://economia.awesomeapi.com.br/json/last/USD-BRL";

    public static class Cotacao {
        private final double compra;
        private final double venda;
        private final String atualizadoEm;

        public Cotacao(double compra, double venda, String atualizadoEm) {
            this.compra = compra;
            this.venda = venda;
            this.atualizadoEm = atualizadoEm;
        }

        public double getCompra() { return compra; }
        public double getVenda() { return venda; }
        public String getAtualizadoEm() { return atualizadoEm; }
    }

    public static Cotacao buscarUsdBrl() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(7000);
        connection.setReadTimeout(7000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Cache-Control", "no-cache");

        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) throw new Exception("HTTP " + status);

        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) body.append(line);
        } finally {
            connection.disconnect();
        }

        JSONObject root = new JSONObject(body.toString());
        JSONObject usdBrl = root.optJSONObject("USDBRL");
        if (usdBrl == null) throw new Exception("Resposta inválida");

        double compra = Double.parseDouble(usdBrl.optString("bid", "0"));
        double venda = Double.parseDouble(usdBrl.optString("ask", "0"));
        String atualizadoEm = usdBrl.optString("create_date", "");

        if (compra <= 0) throw new Exception("Cotação indisponível");
        return new Cotacao(compra, venda, atualizadoEm);
    }
}
