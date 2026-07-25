# Fatura App

App pessoal Android (Kotlin/Jetpack Compose) pra analisar faturas de cartão de
crédito em PDF (Nubank, Itaú, Mercado Pago). 100% on-device — sem backend, sem
rede, sem servidor pra manter.

## Stack e arquitetura

- **UI**: Jetpack Compose + Material3, tema claro/escuro/sistema.
- **Persistência**: Room (`AppDatabase`, versão incrementada com `Migration`
  explícita — nunca destrutiva, o app guarda dados reais do usuário).
- **Parsing de PDF**: PdfBox-Android (`com.tom-roush:pdfbox-android`),
  `PDFBoxResourceLoader.init()` roda em `FaturaApp.onCreate()` (a
  `Application`, não a `Activity` — cold start via share-intent pode chegar no
  parsing antes de qualquer `Activity.onCreate()`).
- **Anotações**: KSP (não kapt) pro compilador do Room.
- **Sem rede**: não há Retrofit/OkHttp/permissão INTERNET. Removido de
  propósito numa migração anterior — não reintroduzir.
- **SDK**: `compileSdk`/`targetSdk` 34, `minSdk` 26, JVM target 17, Kotlin
  compiler extension 1.5.14 (compose-bom `2024.06.00`).

### Bancos suportados

Nubank, Itaú, Mercado Pago. **Bradesco é deliberadamente não suportado** —
não existe parser nem stub pra ele; não adicionar sem pedido explícito.

## Estrutura de diretórios

```
android/app/src/main/java/com/faturaapp/
  FaturaApp.kt                  Application; init do PDFBox aqui
  MainActivity.kt                única Activity; calcula WindowSizeClass, tema
  categorizer/Categorizer.kt     regras regex de categorização (ordem importa)
  data/
    PreferencesRepository.kt     DataStore (tema)
    SharedFileHolder.kt          ponte Intent (ACTION_SEND) -> UploadScreen
    local/
      AppDatabase.kt, DatabaseProvider.kt   Room + singleton + Migrations
      InvoiceRepository.kt       fachada única sobre os DAOs; regra de negócio
      InvoiceWithTransactions.kt @Relation Invoice + List<Transaction>
      SummaryAggregator.kt       agregação pra tela de Comparação
      entity/                    InvoiceEntity, TransactionEntity, DefaultPasswordEntity,
                                  CategoryOverrideEntity (camelCase idiomático)
      dao/                       um DAO por entidade
  parsing/
    BankParser.kt                interface comum (matches/parse)
    NubankParser.kt, ItauParser.kt, MercadoPagoParser.kt
    InvoiceDispatcher.kt         detecta banco, tenta senhas, orquestra parse
    ParserUtils.kt                parseBrazilianAmount, extração de texto por coluna
  navigation/AppNavigation.kt     rotas + Scaffold raiz com a nav bar flutuante
  ui/
    components/                  composables compartilhados entre telas
      AdaptiveScreen.kt           container responsivo (ver decisões abaixo)
      FloatingNavigationBar.kt    nav bar flutuante persistente
      EditCategoryDialog.kt       diálogo de categoria (usado em 2 telas)
    dashboard/                    tela inicial: card "Resumo geral"
    invoicesbycard/               quebra por banco/cartão/mês
    invoicedetail/                transações de uma fatura, filtros
    comparison/                   comparação entre períodos + gráfico de barras
    upload/                       envio de PDF (picker ou share-intent)
    passwords/                    CRUD de senhas padrão de PDF
    categories/                   tela "Categorização manual" (overrides)
    settings/                     tema, atalhos pra Senhas e Categorização
    theme/                        cores, visual de categoria/banco
```

## Decisões de arquitetura (não reverter sem motivo)

- **Sem backend.** Tudo Room + PdfBox no próprio app. Não recriar
  Retrofit/rede.
- **Nomes de campo em Room são camelCase idiomático** (`mesReferencia`, não
  `mes_referencia`) — decisão explícita do usuário, prioriza Kotlin
  idiomático sobre menor diff.
- **Migrations do Room nunca são destrutivas.** O app é usado com dados
  reais; sempre escrever uma `Migration` explícita ao mudar o schema.
- **Categorização por regex ordenada**: em `Categorizer.kt`, regras mais
  específicas (`amazon prime`, `mercado livre`) vêm antes das genéricas
  (`amazon`, `mercado`) — colisão de substring é o motivo. Ao adicionar
  regra nova, checar a ordem.
- **Aprendizado de categoria**: ao editar a categoria de uma transação
  (`InvoiceRepository.updateCategory`), o app salva um override
  (descrição normalizada -> categoria) em `categoria_overrides` e consulta
  esses overrides antes do regex em uploads futuros. Editável/removível na
  tela "Categorização manual".
- **Navegação por abas sem `saveState`/`restoreState`.** Tocar numa aba da
  `FloatingNavigationBar` sempre leva pra raiz dela (`popUpTo` +
  `launchSingleTop`, sem salvar estado). Testamos o padrão oficial
  (multi-backstack com `saveState`/`restoreState`) e ele reintroduz uma
  tela filha ao trocar de aba e voltar — confuso numa app sem breadcrumb
  visível. Decisão deliberada, não é bug.
- **Barra de navegação é persistente e global**, vive no `Scaffold` de
  `AppNavigation.kt` (no slot `floatingActionButton`, com
  `FabPosition.Center`), não em cada tela — aparece em toda tela do app,
  inclusive telas "filhas" (Upload, Fatura Detalhe, Senhas, Categorização
  manual). Usa `Modifier.selectable(role = Role.Tab)`, não `clickable` —
  necessário pra semântica de acessibilidade (leitor de tela anunciar
  "aba, selecionado").
- **Layout responsivo via `WindowSizeClass` oficial** (`AdaptiveScreen` em
  `ui/components/`), não por breakpoint chutado. `calculateWindowSizeClass`
  roda uma vez em `MainActivity` e é distribuída por `CompositionLocal`
  (`LocalWindowWidthSizeClass`). Em `Compact` (celular) o conteúdo ocupa a
  tela inteira; em `Medium`/`Expanded` (tablet, tela aberta de dobrável)
  limita a 600dp e centraliza. Toda tela nova com lista/formulário deve
  usar `AdaptiveScreen { ... }` em vez de `Modifier.fillMaxSize()` direto.
- **Diálogos e composables repetidos em mais de um lugar vão pra
  `ui/components/`** (ex: `EditCategoryDialog`, usado em Dashboard e
  Invoice Detail). Não duplicar.

## Convenções de código

- **Padrão de código agora é inglês** (decisão tomada após a migração pro
  Room deixar isso seguro de fazer). Identificadores Kotlin (classes,
  funções, variáveis, nomes de arquivo e de pacote) e comentários são em
  inglês. **A UI continua em português** — labels, mensagens de erro
  mostradas ao usuário, nomes de categoria (`"Compras"`, `"Alimentação"`,
  etc.) e códigos de banco (`"nubank"`, `"itau"`) são dados/conteúdo do
  app pessoal, não identificadores de código, e não são traduzidos.
  Strings de schema (nomes de tabela/coluna do Room via `@ColumnInfo`/
  `tableName`, chaves do DataStore) também ficam como estão — são valores
  gravados em disco, renomear quebraria dados existentes sem uma
  `Migration`. Ao adicionar código novo: identificadores em inglês, texto
  visível ao usuário em português.
- **Comentários só quando explicam um "porquê" não óbvio** (uma
  invariante escondida, um workaround, um comportamento que surpreenderia
  quem lê). Não comentar o óbvio.
- **Sem abstração prematura.** Duplicação pequena (2-3 linhas) é aceitável;
  extrair componente compartilhado só quando o mesmo bloco não-trivial
  aparece em 2+ lugares de verdade.
- **Modifier order importa em Compose**: `.widthIn(max=X).fillMaxWidth()`
  funciona (limita, depois preenche até o limite); `.fillMaxWidth().widthIn(max=X)`
  **não funciona** (fillMaxWidth força min=max=largura do pai antes do
  widthIn agir, o limite é ignorado). Já caímos nessa — cuidado ao mexer
  em modifiers de largura.
- Itens clicáveis com texto de tamanho variável numa `Row` sem `weight`
  podem estourar a largura e ter conteúdo cortado pelo clip do `Card`
  (já aconteceu duas vezes: Resumo geral e Categorização manual). Sempre
  dar `Modifier.weight(1f)` + `maxLines`/`TextOverflow.Ellipsis` no texto
  quando o resto da Row tem um elemento de tamanho fixo (ícone de remover,
  valor em R$).

## Fluxo de trabalho (git)

Todo PR segue o mesmo padrão, sem pedir confirmação a cada passo
(autorização padrão já dada pelo usuário: "vai fazendo e mergeando"):

1. `git checkout -b feature/nome-descritivo` (ou `fix/...`) a partir de
   `main` atualizado.
2. Implementar, buildar (`./dev.sh build`), testar ao vivo no emulador.
3. `git add` arquivos específicos (nunca `-A` sem checar o `git status`
   antes), commit com mensagem explicando o *porquê*.
4. `git push -u origin <branch>`.
5. `gh pr create` com corpo descrevendo mudança + evidência de teste.
6. `gh pr merge --squash --delete-branch`.
7. `git fetch origin --prune && git checkout main && git pull`.
8. Rebuildar e copiar o APK atualizado pro Desktop quando o usuário pedir
   (`cp android/app/build/outputs/apk/debug/app-debug.apk` pro
   OneDrive/Desktop — a pasta Desktop real fica em `OneDrive/Desktop`,
   não em `C:\Users\<user>\Desktop`).

## Dev loop / testes

- `android/dev.sh <comando>`: `emulator` (abre o AVD `fatura_test`),
  `build`, `install`, `start`, `run` (build+install+start), `logs`
  (logcat do processo do app).
- Testes JVM puros (`src/test/`) pra lógica sem Android runtime
  (Categorizer): `./gradlew testDebugUnitTest`.
- Testes instrumentados (`src/androidTest/`) pra Room e pipeline de
  parsing contra PDFs reais.
- **PDFs de fatura reais nunca vão pro git** — `android/app/src/androidTest/assets/`
  está no `.gitignore` de propósito. Testados localmente, nunca commitados.
- Pra inspecionar o banco Room ao vivo: puxar `.db`, `.db-wal` e `.db-shm`
  juntos (Room usa WAL, dado recente pode não estar no `.db` principal)
  via `adb exec-out run-as com.faturaapp cat databases/fatura_app.db > arquivo`
  (usar `exec-out`, não `shell ... >`, senão o Git Bash corrompe dados
  binários) e abrir com `sqlite3` local.
- Paths do `adb push`/`pull` no Git Bash: usar barra dupla
  (`//sdcard/arquivo`), senão o Git Bash reescreve o path e falha.
- **Não simular telas grandes via `adb shell wm size`/`wm density` em
  produção de teste** — é gambiarra: derruba o launcher do emulador
  (crash loop) e às vezes mata o processo do emulador inteiro. Serve pra
  um check visual rápido e único, sempre resetando depois
  (`wm size reset && wm density reset`). Pra testar telas grandes/dobráveis
  de verdade, usar o Resizable Emulator do Android Studio ou um AVD com
  perfil de Fold — não configurado neste projeto ainda.

## Limitações conhecidas (aceitas, não são bugs)

- `SharedFileHolder` (objeto com `var` mutável) é a ponte entre o Intent
  de compartilhamento e a tela de Upload. Não sobrevive a morte de
  processo (caso raro). Aceito pra um app pessoal; não vale a complexidade
  de persistir isso pra esse caso de uso.
- Sem layout multi-coluna em `Expanded` (tablets/dobráveis em paisagem) —
  `AdaptiveScreen` só limita e centraliza uma coluna. Um redesenho
  lista-detalhe de duas colunas seria o próximo nível, não implementado.
