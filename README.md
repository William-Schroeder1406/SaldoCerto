# Saldo Certo

Aplicativo mobile de controle financeiro pessoal desenvolvido como continuidade da **Atividade Avaliativa 1** da disciplina de **Desenvolvimento para Dispositivos Móveis**, do curso de Análise e Desenvolvimento de Sistemas da UNOESC.

O objetivo do Saldo Certo é oferecer uma forma simples e intuitiva de registrar receitas e despesas, acompanhar o saldo disponível, consultar o histórico financeiro e organizar metas de economia.

## Desenvolvedor

**William Schroeder**

Curso: Análise e Desenvolvimento de Sistemas  
Instituição: UNOESC  
Disciplina: Desenvolvimento para Dispositivos Móveis  
Professor: Alysson Borges  
Ano: 2026

---

## Tecnologias utilizadas

- Android Studio
- Java
- Gradle
- XML para construção das interfaces
- SharedPreferences para persistência local
- Comunicação HTTP com API externa
- AwesomeAPI para consulta da cotação USD/BRL

---

## Funcionalidades implementadas

### Dashboard

A tela inicial apresenta um resumo das informações financeiras do usuário, incluindo:

- saldo atual;
- total de receitas;
- total de despesas;
- gastos organizados por categoria;
- últimas movimentações;
- consulta da cotação atual do dólar em relação ao real.

Os valores do Dashboard são recalculados automaticamente conforme novas movimentações são cadastradas, editadas ou excluídas.

### Movimentações financeiras

O aplicativo permite cadastrar:

- receitas;
- despesas.

Cada movimentação pode possuir informações como:

- descrição;
- valor;
- categoria;
- tipo da movimentação;
- forma de pagamento.

Também é possível:

- editar uma movimentação existente;
- excluir uma movimentação;
- consultar todas as movimentações no histórico.

### Histórico

A tela de histórico apresenta todas as movimentações cadastradas.

Também estão disponíveis filtros para visualizar:

- todas as movimentações;
- somente receitas;
- somente despesas.

Receitas são destacadas como valores positivos e despesas como valores negativos, facilitando a identificação das movimentações.

### Metas financeiras

O aplicativo possui uma área destinada ao gerenciamento de metas financeiras.

O usuário pode:

- criar uma nova meta;
- definir o nome da meta;
- definir o valor objetivo;
- adicionar dinheiro à meta;
- retirar dinheiro da meta;
- editar a meta;
- excluir a meta;
- acompanhar o percentual de conclusão.

Ao adicionar dinheiro em uma meta, o valor é retirado do saldo disponível e uma movimentação automática é registrada no histórico:

`Aporte para meta: Nome da meta`

Ao retirar dinheiro de uma meta, o valor retorna ao saldo disponível e também é registrada uma movimentação:

`Resgate da meta: Nome da meta`

As movimentações automáticas relacionadas às metas não podem ser editadas ou excluídas diretamente pelo histórico, evitando inconsistências entre o saldo disponível e o valor acumulado da meta.

O sistema também impede que o usuário retire da meta um valor maior que o acumulado.

### Persistência local

As movimentações e metas são armazenadas localmente no dispositivo.

Dessa forma, os dados permanecem disponíveis mesmo após o fechamento e a reabertura do aplicativo.

A persistência local também permite que as principais funcionalidades continuem disponíveis sem conexão com a internet.

---

## Integração com API

O Saldo Certo possui integração com uma API externa para demonstrar comunicação entre o aplicativo e um servidor.

Nesta versão, o Dashboard consulta a **AwesomeAPI** para obter a cotação entre dólar americano e real brasileiro.

Endpoint utilizado:

`https://economia.awesomeapi.com.br/json/last/USD-BRL`

O aplicativo utiliza o campo `bid` retornado pela API e apresenta também a data e a hora da atualização da cotação.

Existe ainda a opção **Atualizar cotação**, permitindo realizar uma nova consulta ao servidor.

Os valores apresentados podem possuir pequenas diferenças em relação a outras plataformas, como o Google, pois cada provedor pode trabalhar com diferentes horários de atualização, referências, spreads e metodologias.

Caso não exista conexão com a internet ou ocorra algum problema durante a consulta, o restante das funcionalidades financeiras continua disponível normalmente.

---

## Regras implementadas

Entre as regras utilizadas pelo aplicativo estão:

- movimentações devem possuir valor válido;
- receitas aumentam o saldo;
- despesas reduzem o saldo;
- movimentações excluídas deixam de fazer parte dos cálculos;
- alterações nas movimentações atualizam automaticamente o Dashboard;
- aportes em metas reduzem o saldo disponível;
- resgates de metas devolvem dinheiro ao saldo;
- não é possível retirar de uma meta um valor maior que o acumulado;
- os dados cadastrados são mantidos localmente no dispositivo.

---

## Experiência do usuário

A interface foi desenvolvida buscando manter uma experiência simples e intuitiva.

Entre os princípios utilizados estão:

- identidade visual baseada na cor verde;
- destaque visual para receitas e despesas;
- organização das informações através de cards;
- navegação inferior entre as principais áreas;
- confirmação antes de operações de exclusão;
- validação de campos;
- mensagens de retorno após determinadas ações;
- apresentação clara do saldo e dos principais valores financeiros;
- separação das funcionalidades em telas específicas.

A navegação principal possui as opções:

- Início;
- Histórico;
- Adicionar;
- Metas;
- Perfil.

---

## Estrutura principal do projeto

A aplicação está organizada em diferentes arquivos e diretórios para facilitar sua manutenção.

Entre os principais componentes estão:

- `MainActivity.java` — responsável pela navegação e por parte da lógica principal do aplicativo;
- `model/` — contém os modelos utilizados pelo sistema;
- `data/` — contém a lógica relacionada à persistência local;
- `api/CotacaoService.java` — responsável pela comunicação com a API de cotação;
- `res/layout/` — contém os arquivos XML utilizados na construção das interfaces;
- `res/drawable/` — contém recursos visuais utilizados pelas telas;
- `AndroidManifest.xml` — contém configurações e permissões da aplicação.

---

## Como executar o projeto

### Requisitos

Para executar o aplicativo é necessário possuir:

- Android Studio;
- Android SDK instalado;
- Java/JDK configurado;
- emulador Android ou dispositivo Android compatível.

O projeto foi testado utilizando um emulador **Pixel 7**.

### Execução

1. Clone ou faça o download deste repositório.
2. Abra a pasta do projeto no Android Studio.
3. Aguarde a sincronização do Gradle.
4. Selecione um dispositivo Android ou emulador.
5. Clique em **Run (▶)**.
6. Aguarde a instalação e abertura do aplicativo.

---

## Como testar

1. Abra o aplicativo.
2. Confira os valores apresentados no Dashboard.
3. Cadastre uma nova receita.
4. Verifique se o saldo e o total de receitas foram atualizados.
5. Cadastre uma nova despesa.
6. Confira se o saldo e o total de despesas foram atualizados.
7. Abra o Histórico.
8. Teste os filtros de receitas e despesas.
9. Edite uma movimentação e confirme a alteração no Dashboard.
10. Exclua uma movimentação e confirme a atualização dos valores.
11. Acesse a tela de Metas.
12. Crie uma nova meta financeira.
13. Utilize a opção de adicionar valor à meta.
14. Confirme que o saldo disponível diminuiu.
15. Confira no Histórico a movimentação `Aporte para meta`.
16. Utilize a opção de retirar valor da meta.
17. Confirme que o dinheiro retornou ao saldo.
18. Confira no Histórico a movimentação `Resgate da meta`.
19. Teste a edição e exclusão de metas.
20. Retorne ao Dashboard e utilize **Atualizar cotação**.
21. Feche completamente o aplicativo.
22. Abra novamente e confirme que movimentações e metas continuam armazenadas.

---

## Relação com a Atividade Avaliativa 1

O Saldo Certo foi inicialmente planejado na Atividade Avaliativa 1 como um aplicativo para organização financeira pessoal.

Nesta segunda etapa, parte dos requisitos projetados anteriormente foi transformada em funcionalidades reais.

Entre os recursos já implementados estão:

- cadastro de receitas;
- cadastro de despesas;
- edição de movimentações;
- exclusão de movimentações;
- histórico financeiro;
- cálculo do saldo;
- organização por categorias;
- metas financeiras;
- funcionamento local;
- comunicação com servidor através de API.

Algumas funcionalidades previstas no planejamento inicial possuem maior complexidade e poderão ser adicionadas em versões futuras.

---

## Possíveis melhorias futuras

O projeto pode receber novas funcionalidades, como:

- autenticação de usuários;
- recuperação de senha;
- acesso por biometria;
- integração completa das movimentações com uma API REST;
- sincronização entre armazenamento local e servidor;
- cadastro de contas a pagar;
- notificações de vencimentos;
- gráficos financeiros avançados;
- relatórios mensais;
- anexos de comprovantes;
- utilização da câmera;
- categorias personalizadas;
- limites mensais de gastos;
- modo escuro;
- exportação de relatórios;
- integração com contas bancárias.

---

## Status do projeto

**Versão funcional desenvolvida para a Atividade Avaliativa 2.**

A versão atual já permite executar as principais operações de controle financeiro, trabalhar com metas, manter os dados localmente e realizar comunicação com uma API externa.

A versão atual foi consolidada para entrega da Atividade Avaliativa 2, mantendo como melhorias futuras as funcionalidades listadas anteriormente.

---

## Licença

Projeto desenvolvido para fins acadêmicos.