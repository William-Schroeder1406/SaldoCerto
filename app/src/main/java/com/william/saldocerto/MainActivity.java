package com.william.saldocerto;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.william.saldocerto.api.CotacaoService;
import com.william.saldocerto.data.MetaRepository;
import com.william.saldocerto.data.TransacaoRepository;
import com.william.saldocerto.model.MetaFinanceira;
import com.william.saldocerto.model.Transacao;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private final Locale localeBr = new Locale("pt", "BR");
    private final NumberFormat moeda = NumberFormat.getCurrencyInstance(localeBr);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private FrameLayout contentFrame;
    private BottomNavigationView bottomNavigation;
    private TransacaoRepository transacaoRepository;
    private MetaRepository metaRepository;
    private Transacao editingTransaction;
    private String historyFilter = "todas";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        transacaoRepository = new TransacaoRepository(this);
        metaRepository = new MetaRepository(this);
        contentFrame = findViewById(R.id.contentFrame);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                editingTransaction = null;
                showDashboard();
                return true;
            }
            if (id == R.id.nav_history) {
                editingTransaction = null;
                showHistory();
                return true;
            }
            if (id == R.id.nav_add) {
                showAddTransaction();
                return true;
            }
            if (id == R.id.nav_goals) {
                editingTransaction = null;
                showGoals();
                return true;
            }
            if (id == R.id.nav_profile) {
                editingTransaction = null;
                showProfile();
                return true;
            }
            return false;
        });

        bottomNavigation.getMenu().findItem(R.id.nav_home).setChecked(true);
        showDashboard();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private View inflate(int layout) {
        View view = LayoutInflater.from(this).inflate(layout, contentFrame, false);
        contentFrame.removeAllViews();
        contentFrame.addView(view);
        return view;
    }

    private void showDashboard() {
        View root = inflate(R.layout.view_dashboard);
        List<Transacao> transacoes = transacaoRepository.getAll();

        TextView tvMonth = root.findViewById(R.id.tvMonth);
        TextView tvBalance = root.findViewById(R.id.tvBalance);
        TextView tvIncome = root.findViewById(R.id.tvIncome);
        TextView tvExpense = root.findViewById(R.id.tvExpense);
        LinearLayout categoryContainer = root.findViewById(R.id.categoryContainer);
        LinearLayout recentContainer = root.findViewById(R.id.recentContainer);

        String mes = new SimpleDateFormat("MMMM 'de' yyyy", localeBr).format(new Date());
        tvMonth.setText("Resumo de " + capitalize(mes));

        double receitas = 0;
        double despesas = 0;
        Map<String, Double> gastosCategoria = new LinkedHashMap<>();

        for (Transacao t : transacoes) {
            if ("receita".equals(t.getTipo())) {
                receitas += t.getValor();
            } else {
                despesas += t.getValor();
                gastosCategoria.put(t.getCategoria(), gastosCategoria.getOrDefault(t.getCategoria(), 0.0) + t.getValor());
            }
        }

        tvIncome.setText(moeda.format(receitas));
        tvExpense.setText(moeda.format(despesas));
        tvBalance.setText(moeda.format(receitas - despesas));

        renderCategoryBars(categoryContainer, gastosCategoria);

        if (transacoes.isEmpty()) {
            recentContainer.addView(emptyMessage("Nenhuma movimentação cadastrada."));
        } else {
            int limit = Math.min(4, transacoes.size());
            for (int i = 0; i < limit; i++) {
                recentContainer.addView(createTransactionCard(transacoes.get(i), false));
            }
        }

        TextView tvApiRate = root.findViewById(R.id.tvApiRate);
        TextView tvApiStatus = root.findViewById(R.id.tvApiStatus);
        MaterialButton btnRefreshRate = root.findViewById(R.id.btnRefreshRate);
        btnRefreshRate.setOnClickListener(v -> loadExchangeRate(tvApiRate, tvApiStatus));
        loadExchangeRate(tvApiRate, tvApiStatus);
    }

    private void renderCategoryBars(LinearLayout container, Map<String, Double> gastos) {
        container.removeAllViews();
        if (gastos.isEmpty()) {
            container.addView(emptyMessage("Cadastre despesas para visualizar os gastos por categoria."));
            return;
        }

        double max = 1;
        for (double valor : gastos.values()) max = Math.max(max, valor);

        for (Map.Entry<String, Double> entry : gastos.entrySet()) {
            LinearLayout block = new LinearLayout(this);
            block.setOrientation(LinearLayout.VERTICAL);
            block.setPadding(0, dp(6), 0, dp(7));

            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            TextView name = text(entry.getKey(), 13, R.color.sc_text, true);
            TextView value = text(moeda.format(entry.getValue()), 13, R.color.sc_text, true);
            line.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            line.addView(value);
            block.addView(line);

            ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            progress.setMax(100);
            progress.setProgress((int) Math.round((entry.getValue() / max) * 100));
            progress.setProgressTintList(ColorStateList.valueOf(color(R.color.sc_primary)));
            progress.setProgressBackgroundTintList(ColorStateList.valueOf(color(R.color.sc_border)));
            LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(8));
            pp.topMargin = dp(5);
            block.addView(progress, pp);
            container.addView(block);
        }
    }

    private void loadExchangeRate(TextView rate, TextView status) {
        rate.setText("Consultando...");
        status.setText("Buscando a última cotação disponível...");
        executor.execute(() -> {
            try {
                CotacaoService.Cotacao cotacao = CotacaoService.buscarUsdBrl();
                mainHandler.post(() -> {
                    rate.setText(String.format(localeBr, "US$ 1 = R$ %.4f", cotacao.getCompra()));
                    String atualizado = cotacao.getAtualizadoEm();
                    if (atualizado == null || atualizado.trim().isEmpty()) {
                        status.setText("Cotação de compra (bid) • toque em Atualizar para consultar novamente");
                    } else {
                        status.setText("Compra (bid) • última atualização da fonte: " + atualizado);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    rate.setText("Cotação indisponível");
                    status.setText("Sem conexão ou servidor indisponível. O restante do app continua funcionando offline.");
                });
            }
        });
    }

    private void showHistory() {
        View root = inflate(R.layout.view_history);
        MaterialButton all = root.findViewById(R.id.btnFilterAll);
        MaterialButton income = root.findViewById(R.id.btnFilterIncome);
        MaterialButton expense = root.findViewById(R.id.btnFilterExpense);
        LinearLayout container = root.findViewById(R.id.historyContainer);

        all.setOnClickListener(v -> { historyFilter = "todas"; showHistory(); });
        income.setOnClickListener(v -> { historyFilter = "receita"; showHistory(); });
        expense.setOnClickListener(v -> { historyFilter = "despesa"; showHistory(); });
        styleFilterButtons(all, income, expense);

        List<Transacao> allTransactions = transacaoRepository.getAll();
        List<Transacao> filtered = new ArrayList<>();
        for (Transacao t : allTransactions) {
            if ("todas".equals(historyFilter) || historyFilter.equals(t.getTipo())) filtered.add(t);
        }

        if (filtered.isEmpty()) {
            container.addView(emptyMessage("Nenhuma movimentação encontrada nesse filtro."));
        } else {
            for (Transacao t : filtered) container.addView(createTransactionCard(t, true));
        }
    }

    private void styleFilterButtons(MaterialButton all, MaterialButton income, MaterialButton expense) {
        MaterialButton[] buttons = {all, income, expense};
        String[] types = {"todas", "receita", "despesa"};
        for (int i = 0; i < buttons.length; i++) {
            boolean selected = historyFilter.equals(types[i]);
            buttons[i].setBackgroundTintList(ColorStateList.valueOf(color(selected ? R.color.sc_primary : R.color.sc_primary_light)));
            buttons[i].setTextColor(color(selected ? R.color.white : R.color.sc_primary_dark));
        }
    }

    private MaterialCardView createTransactionCard(Transacao t, boolean actions) {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(color(R.color.sc_surface));
        card.setRadius(dp(14));
        card.setStrokeColor(color(R.color.sc_border));
        card.setStrokeWidth(dp(1));
        card.setCardElevation(0);

        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setPadding(dp(15), dp(14), dp(15), dp(12));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        TextView desc = text(t.getDescricao(), 15, R.color.sc_text, true);
        String prefix = "receita".equals(t.getTipo()) ? "+ " : "- ";
        TextView amount = text(prefix + moeda.format(t.getValor()), 15,
                "receita".equals(t.getTipo()) ? R.color.sc_income : R.color.sc_expense, true);
        top.addView(desc, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        top.addView(amount);
        wrapper.addView(top);

        String secondary = t.getCategoria() + " • " + t.getData();
        if (t.getFormaPagamento() != null && !t.getFormaPagamento().isEmpty()) secondary += " • " + t.getFormaPagamento();
        TextView sub = text(secondary, 12, R.color.sc_text_secondary, false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sp.topMargin = dp(4);
        wrapper.addView(sub, sp);

        // Movimentações de metas são geradas automaticamente. Elas aparecem no histórico,
        // mas não podem ser editadas/excluídas por aqui para não desalinhar o saldo da meta.
        if (actions && !"Metas".equals(t.getCategoria())) {
            LinearLayout actionRow = new LinearLayout(this);
            actionRow.setGravity(android.view.Gravity.END);
            actionRow.setOrientation(LinearLayout.HORIZONTAL);
            actionRow.setPadding(0, dp(8), 0, 0);

            MaterialButton edit = new MaterialButton(this);
            edit.setText("Editar");
            edit.setTextColor(color(R.color.white));
            edit.setBackgroundTintList(ColorStateList.valueOf(color(R.color.sc_primary)));
            edit.setCornerRadius(dp(18));
            edit.setOnClickListener(v -> {
                editingTransaction = t;
                bottomNavigation.setSelectedItemId(R.id.nav_add);
            });

            MaterialButton delete = new MaterialButton(this);
            delete.setText("Excluir");
            delete.setTextColor(color(R.color.sc_text_secondary));
            delete.setBackgroundTintList(ColorStateList.valueOf(color(R.color.sc_surface)));
            delete.setStrokeColor(ColorStateList.valueOf(color(R.color.sc_border)));
            delete.setStrokeWidth(dp(1));
            delete.setCornerRadius(dp(18));
            delete.setOnClickListener(v -> confirmDeleteTransaction(t));

            LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            deleteParams.leftMargin = dp(8);

            actionRow.addView(edit, editParams);
            actionRow.addView(delete, deleteParams);
            wrapper.addView(actionRow);
        }

        card.addView(wrapper);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cp.bottomMargin = dp(9);
        card.setLayoutParams(cp);
        return card;
    }

    private void confirmDeleteTransaction(Transacao t) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir movimentação")
                .setMessage("Deseja excluir \"" + t.getDescricao() + "\"? O valor deixará de ser considerado no saldo.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir", (dialog, which) -> {
                    transacaoRepository.delete(t.getId());
                    Toast.makeText(this, "Movimentação excluída", Toast.LENGTH_SHORT).show();
                    showHistory();
                })
                .show();
    }

    private void showAddTransaction() {
        View root = inflate(R.layout.view_add);
        TextView title = root.findViewById(R.id.tvAddTitle);
        RadioGroup rgType = root.findViewById(R.id.rgType);
        RadioButton rbIncome = root.findViewById(R.id.rbIncome);
        RadioButton rbExpense = root.findViewById(R.id.rbExpense);
        TextInputEditText etDescription = root.findViewById(R.id.etDescription);
        TextInputEditText etValue = root.findViewById(R.id.etValue);
        Spinner spCategory = root.findViewById(R.id.spCategory);
        TextInputEditText etDate = root.findViewById(R.id.etDate);
        Spinner spPayment = root.findViewById(R.id.spPayment);
        TextInputEditText etNote = root.findViewById(R.id.etNote);
        MaterialButton btnSave = root.findViewById(R.id.btnSaveTransaction);
        MaterialButton btnClear = root.findViewById(R.id.btnClearTransaction);

        String[] categories = {"Alimentação", "Moradia", "Transporte", "Saúde", "Lazer", "Salário", "Educação", "Outros"};
        String[] payments = {"PIX", "Cartão de crédito", "Cartão de débito", "Dinheiro", "Transferência", "Boleto", "Outro"};
        spCategory.setAdapter(spinnerAdapter(categories));
        spPayment.setAdapter(spinnerAdapter(payments));
        etDate.setText(new SimpleDateFormat("dd/MM/yyyy", localeBr).format(new Date()));

        if (editingTransaction != null) {
            title.setText("Editar movimentação");
            btnSave.setText("Salvar alterações");
            etDescription.setText(editingTransaction.getDescricao());
            etValue.setText(String.format(localeBr, "%.2f", editingTransaction.getValor()));
            etDate.setText(editingTransaction.getData());
            etNote.setText(editingTransaction.getObservacao());
            if ("receita".equals(editingTransaction.getTipo())) rbIncome.setChecked(true); else rbExpense.setChecked(true);
            selectSpinner(spCategory, editingTransaction.getCategoria());
            selectSpinner(spPayment, editingTransaction.getFormaPagamento());
        }

        btnSave.setOnClickListener(v -> {
            String descricao = textOf(etDescription);
            String valueRaw = textOf(etValue);
            String data = textOf(etDate);
            String observacao = textOf(etNote);
            String tipo = rgType.getCheckedRadioButtonId() == R.id.rbIncome ? "receita" : "despesa";
            String categoria = String.valueOf(spCategory.getSelectedItem());
            String pagamento = String.valueOf(spPayment.getSelectedItem());

            if (descricao.length() < 2) {
                etDescription.setError("Informe uma descrição");
                return;
            }
            double valor;
            try {
                valor = parseNumber(valueRaw);
            } catch (Exception e) {
                etValue.setError("Informe um valor válido");
                return;
            }
            if (valor <= 0) {
                etValue.setError("O valor deve ser maior que zero");
                return;
            }
            if (data.trim().isEmpty()) {
                etDate.setError("Informe a data");
                return;
            }

            if (editingTransaction == null) {
                transacaoRepository.add(descricao, valor, tipo, categoria, data, pagamento, observacao);
                Toast.makeText(this, "Movimentação cadastrada com sucesso", Toast.LENGTH_SHORT).show();
            } else {
                Transacao updated = new Transacao(editingTransaction.getId(), descricao, valor, tipo, categoria, data, pagamento, observacao);
                transacaoRepository.update(updated);
                editingTransaction = null;
                Toast.makeText(this, "Movimentação atualizada", Toast.LENGTH_SHORT).show();
            }
            bottomNavigation.setSelectedItemId(R.id.nav_history);
        });

        btnClear.setOnClickListener(v -> {
            editingTransaction = null;
            showAddTransaction();
        });
    }

    private ArrayAdapter<String> spinnerAdapter(String[] values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private void selectSpinner(Spinner spinner, String value) {
        if (value == null) return;
        for (int i = 0; i < spinner.getCount(); i++) {
            if (value.equalsIgnoreCase(String.valueOf(spinner.getItemAtPosition(i)))) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void showGoals() {
        View root = inflate(R.layout.view_goals);
        LinearLayout container = root.findViewById(R.id.goalsContainer);
        MaterialButton btnAdd = root.findViewById(R.id.btnAddGoal);
        btnAdd.setOnClickListener(v -> showGoalDialog());

        List<MetaFinanceira> metas = metaRepository.getAll();
        if (metas.isEmpty()) {
            container.addView(emptyMessage("Você ainda não criou nenhuma meta financeira."));
            return;
        }
        for (MetaFinanceira meta : metas) container.addView(createGoalCard(meta));
    }

    private MaterialCardView createGoalCard(MetaFinanceira meta) {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(color(R.color.sc_surface));
        card.setRadius(dp(14));
        card.setStrokeColor(color(R.color.sc_border));
        card.setStrokeWidth(dp(1));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(15), dp(16), dp(12));

        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        TextView name = text(meta.getNome(), 15, R.color.sc_text, true);
        TextView pct = text(meta.getPercentual() + "%", 14, R.color.sc_primary, true);
        line.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        line.addView(pct);
        content.addView(line);

        TextView values = text(moeda.format(meta.getAtual()) + " de " + moeda.format(meta.getObjetivo()), 12, R.color.sc_text_secondary, false);
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        vp.topMargin = dp(4);
        content.addView(values, vp);

        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgress(meta.getPercentual());
        progress.setProgressTintList(ColorStateList.valueOf(color(R.color.sc_primary)));
        progress.setProgressBackgroundTintList(ColorStateList.valueOf(color(R.color.sc_border)));
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(9));
        pp.topMargin = dp(10);
        content.addView(progress, pp);

        LinearLayout valueActions = new LinearLayout(this);
        valueActions.setOrientation(LinearLayout.HORIZONTAL);
        valueActions.setPadding(0, dp(10), 0, 0);

        MaterialButton removeValue = new MaterialButton(this);
        removeValue.setText("− Retirar");
        removeValue.setTextColor(color(R.color.sc_primary_dark));
        removeValue.setBackgroundTintList(ColorStateList.valueOf(color(R.color.sc_primary_light)));
        removeValue.setCornerRadius(dp(18));
        removeValue.setOnClickListener(v -> showGoalAmountDialog(meta, false));

        MaterialButton addValue = new MaterialButton(this);
        addValue.setText("+ Adicionar");
        addValue.setTextColor(color(R.color.white));
        addValue.setBackgroundTintList(ColorStateList.valueOf(color(R.color.sc_primary)));
        addValue.setCornerRadius(dp(18));
        addValue.setOnClickListener(v -> showGoalAmountDialog(meta, true));

        LinearLayout.LayoutParams half1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        half1.rightMargin = dp(5);
        LinearLayout.LayoutParams half2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        half2.leftMargin = dp(5);
        valueActions.addView(removeValue, half1);
        valueActions.addView(addValue, half2);
        content.addView(valueActions);

        LinearLayout secondaryActions = new LinearLayout(this);
        secondaryActions.setGravity(android.view.Gravity.END);
        secondaryActions.setOrientation(LinearLayout.HORIZONTAL);
        secondaryActions.setPadding(0, dp(4), 0, 0);

        MaterialButton editGoal = new MaterialButton(this);
        editGoal.setText("Editar meta");
        editGoal.setTextColor(color(R.color.sc_primary));
        editGoal.setBackgroundTintList(ColorStateList.valueOf(color(R.color.sc_surface)));
        editGoal.setCornerRadius(dp(18));
        editGoal.setOnClickListener(v -> showEditGoalDialog(meta));

        MaterialButton delete = new MaterialButton(this);
        delete.setText("Excluir");
        delete.setTextColor(color(R.color.sc_text_secondary));
        delete.setBackgroundTintList(ColorStateList.valueOf(color(R.color.sc_surface)));
        delete.setStrokeColor(ColorStateList.valueOf(color(R.color.sc_border)));
        delete.setStrokeWidth(dp(1));
        delete.setCornerRadius(dp(18));
        delete.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Excluir meta")
                .setMessage(meta.getAtual() > 0
                        ? "Deseja excluir a meta \"" + meta.getNome() + "\"? O valor acumulado de " + moeda.format(meta.getAtual()) + " será devolvido ao saldo."
                        : "Deseja excluir a meta \"" + meta.getNome() + "\"?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir", (d, w) -> {
                    if (meta.getAtual() > 0) {
                        registerGoalMovement(meta.getNome(), meta.getAtual(), false);
                    }
                    metaRepository.delete(meta.getId());
                    Toast.makeText(this, "Meta excluída", Toast.LENGTH_SHORT).show();
                    showGoals();
                })
                .show());

        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        deleteParams.leftMargin = dp(6);
        secondaryActions.addView(editGoal);
        secondaryActions.addView(delete, deleteParams);
        content.addView(secondaryActions);

        card.addView(content);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cp.bottomMargin = dp(10);
        card.setLayoutParams(cp);
        return card;
    }

    private void showGoalAmountDialog(MetaFinanceira meta, boolean adicionar) {
        EditText amount = new EditText(this);
        amount.setHint("Valor");
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amount.setPadding(dp(22), dp(8), dp(22), 0);

        String titulo = adicionar ? "Adicionar valor à meta" : "Retirar valor da meta";
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(titulo)
                .setMessage(meta.getNome() + " • acumulado atual: " + moeda.format(meta.getAtual()))
                .setView(amount)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton(adicionar ? "Adicionar" : "Retirar", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            double valor;
            try {
                valor = parseNumber(amount.getText().toString());
            } catch (Exception e) {
                amount.setError("Informe um valor válido");
                return;
            }
            if (valor <= 0) {
                amount.setError("O valor deve ser maior que zero");
                return;
            }
            if (!adicionar && valor > meta.getAtual()) {
                amount.setError("Não é possível retirar mais do que o valor acumulado");
                return;
            }

            if (adicionar && valor > getAvailableBalance()) {
                amount.setError("Saldo disponível insuficiente");
                return;
            }

            metaRepository.adjustCurrent(meta.getId(), adicionar ? valor : -valor);
            registerGoalMovement(meta.getNome(), valor, adicionar);
            dialog.dismiss();
            Toast.makeText(this,
                    adicionar ? "Valor reservado na meta e descontado do saldo" : "Valor retirado da meta e devolvido ao saldo",
                    Toast.LENGTH_SHORT).show();
            showGoals();
        }));
        dialog.show();
    }

    private void showEditGoalDialog(MetaFinanceira meta) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(4), dp(22), 0);

        EditText name = new EditText(this);
        name.setHint("Nome da meta");
        name.setText(meta.getNome());

        EditText target = new EditText(this);
        target.setHint("Valor objetivo");
        target.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        target.setText(String.format(localeBr, "%.2f", meta.getObjetivo()));

        EditText current = new EditText(this);
        current.setHint("Valor acumulado");
        current.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        current.setText(String.format(localeBr, "%.2f", meta.getAtual()));

        box.addView(name);
        box.addView(target);
        box.addView(current);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Editar meta")
                .setView(box)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nome = name.getText().toString().trim();
            double objetivo;
            double atual;
            try { objetivo = parseNumber(target.getText().toString()); } catch (Exception e) { objetivo = 0; }
            try { atual = parseNumber(current.getText().toString()); } catch (Exception e) { atual = -1; }

            if (nome.length() < 2) { name.setError("Informe um nome"); return; }
            if (objetivo <= 0) { target.setError("Informe um valor objetivo válido"); return; }
            if (atual < 0) { current.setError("Informe um valor acumulado válido"); return; }

            double diferenca = atual - meta.getAtual();
            if (diferenca > 0 && diferenca > getAvailableBalance()) {
                current.setError("Saldo disponível insuficiente para aumentar o valor acumulado");
                return;
            }

            metaRepository.update(new MetaFinanceira(meta.getId(), nome, atual, objetivo));
            if (Math.abs(diferenca) > 0.0001) {
                registerGoalMovement(nome, Math.abs(diferenca), diferenca > 0);
            }
            dialog.dismiss();
            Toast.makeText(this, "Meta atualizada", Toast.LENGTH_SHORT).show();
            showGoals();
        }));
        dialog.show();
    }

    private void showGoalDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(4), dp(22), 0);

        EditText name = new EditText(this);
        name.setHint("Nome da meta");
        EditText target = new EditText(this);
        target.setHint("Valor objetivo");
        target.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText current = new EditText(this);
        current.setHint("Valor já acumulado (opcional)");
        current.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        box.addView(name);
        box.addView(target);
        box.addView(current);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Nova meta")
                .setView(box)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nome = name.getText().toString().trim();
            double objetivo;
            double atual = 0;
            try { objetivo = parseNumber(target.getText().toString()); } catch (Exception e) { objetivo = 0; }
            try { if (!current.getText().toString().trim().isEmpty()) atual = parseNumber(current.getText().toString()); } catch (Exception ignored) { }
            if (nome.length() < 2) { name.setError("Informe um nome"); return; }
            if (objetivo <= 0) { target.setError("Informe um valor objetivo válido"); return; }
            atual = Math.max(0, atual);
            if (atual > getAvailableBalance()) {
                current.setError("Saldo disponível insuficiente para esse valor inicial");
                return;
            }
            metaRepository.add(nome, atual, objetivo);
            if (atual > 0) {
                registerGoalMovement(nome, atual, true);
            }
            dialog.dismiss();
            showGoals();
        }));
        dialog.show();
    }

    private double getAvailableBalance() {
        double receitas = 0;
        double despesas = 0;
        for (Transacao t : transacaoRepository.getAll()) {
            if ("receita".equals(t.getTipo())) receitas += t.getValor();
            else despesas += t.getValor();
        }
        return receitas - despesas;
    }

    private void registerGoalMovement(String goalName, double value, boolean contribution) {
        String today = new SimpleDateFormat("dd/MM/yyyy", localeBr).format(new Date());
        String description = contribution
                ? "Aporte para meta: " + goalName
                : "Resgate da meta: " + goalName;
        String type = contribution ? "despesa" : "receita";
        String observation = contribution
                ? "Valor reservado automaticamente para a meta financeira."
                : "Valor devolvido automaticamente da meta financeira ao saldo disponível.";

        transacaoRepository.add(
                description,
                value,
                type,
                "Metas",
                today,
                "Transferência interna",
                observation
        );
    }

    private void showProfile() {
        View root = inflate(R.layout.view_profile);
        MaterialButton reset = root.findViewById(R.id.btnResetDemo);
        reset.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Restaurar demonstração")
                .setMessage("Isso apagará as movimentações e metas atuais deste aparelho e restaurará os dados usados na demonstração.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Restaurar", (dialog, which) -> {
                    transacaoRepository.resetDemoData();
                    metaRepository.resetDemoData();
                    Toast.makeText(this, "Dados de demonstração restaurados", Toast.LENGTH_SHORT).show();
                    bottomNavigation.setSelectedItemId(R.id.nav_home);
                })
                .show());
    }

    private TextView emptyMessage(String message) {
        TextView v = text(message, 13, R.color.sc_text_secondary, false);
        v.setPadding(dp(2), dp(16), dp(2), dp(16));
        return v;
    }

    private TextView text(String value, int size, int colorRes, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color(colorRes));
        if (bold) text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return text;
    }

    private int color(int res) {
        return ContextCompat.getColor(this, res);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String textOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private double parseNumber(String raw) {
        String value = raw.trim();
        if (value.contains(",")) value = value.replace(".", "").replace(",", ".");
        return Double.parseDouble(value);
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) return value;
        return value.substring(0, 1).toUpperCase(localeBr) + value.substring(1);
    }
}
