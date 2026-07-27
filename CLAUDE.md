# Money Hole

App pessoal Android (Kotlin/Jetpack Compose) pra analisar faturas de cartão de
crédito em PDF (Nubank, Itaú, Mercado Pago). 100% on-device, sem backend
próprio — mas **tem rede** desde 2026-07-25 (busca de fatura por e-mail via
IMAP, ver "Decisões de arquitetura"), reversão deliberada e explicitamente
pedida pelo usuário da decisão anterior "sem rede".

**Rebrand completo em 2026-07-25** (nome antigo: Fatura App). Renomeado de
propósito até o fim, a pedido explícito do usuário — repo GitHub
(`GuiMartins/money-hole`), pacote Kotlin (`com.moneyhole`, era
`com.faturaapp`), `applicationId`, classe `Application` (`MoneyHoleApp`, era
`FaturaApp`), tema (`Theme.MoneyHole`/`MoneyHoleTheme`, era
`Theme.FaturaApp`/`FaturaAppTheme`) e o nome do banco Room local
(`money_hole.db`, era `fatura_app.db`). **Consequência aceita conscientemente**:
mudar o `applicationId` faz o Android tratar como um app novo — qualquer
instalação real anterior perde os dados (não há como preservar isso, o app
antigo e o novo coexistem como pacotes diferentes do ponto de vista do
Android). Diferente do rename PT→EN anterior (esse sim preservou dados via
`@ColumnInfo`/`tableName` do Room), aqui não tinha como evitar a perda.

## Princípios gerais

- **Nada de gambiarra.** Toda escolha de ferramenta, biblioteca ou padrão
  arquitetural segue recomendação oficial (documentação do Android/Google,
  docs oficiais da lib) ou um padrão já consolidado de mercado — não uma
  solução improvisada só porque "funciona agora". Vale tanto pra decisões
  grandes (Room em vez de storage caseiro, `WindowSizeClass` oficial em vez
  de breakpoint chutado, KSP em vez de kapt) quanto pra atalhos de
  teste/debug (por isso simular telas grandes via `wm size`/`wm density` em
  produção de teste é evitado — ver "Dev loop / testes"). Na dúvida entre o
  jeito oficial/consolidado e um atalho mais rápido, o oficial vence, mesmo
  que dê mais trabalho. As decisões abaixo em "Decisões de arquitetura" são
  a aplicação concreta desse princípio; ao adicionar algo novo, justificar
  contra ele.

## Stack e arquitetura

- **UI**: Jetpack Compose + Material3, tema claro/escuro/sistema,
  i18n (PT/EN/ES) com troca de idioma por-app (ver "Decisões de
  arquitetura").
- **Persistência**: Room (`AppDatabase`, versão incrementada com `Migration`
  explícita — nunca destrutiva, o app guarda dados reais do usuário).
- **Parsing de PDF**: PdfBox-Android (`com.tom-roush:pdfbox-android`),
  `PDFBoxResourceLoader.init()` roda em `MoneyHoleApp.onCreate()` (a
  `Application`, não a `Activity` — cold start via share-intent pode chegar no
  parsing antes de qualquer `Activity.onCreate()`).
- **Anotações**: KSP (não kapt) pro compilador do Room.
- **Rede**: só pra IMAP (busca de fatura por e-mail, `com.sun.mail:android-mail`).
  Continua sem Retrofit/OkHttp/backend próprio — permissão INTERNET existe
  exclusivamente pra essa conexão IMAP com o provedor de e-mail do usuário.
- **SDK**: `compileSdk`/`targetSdk` 34, `minSdk` 26, JVM target 17, Kotlin
  compiler extension 1.5.14 (compose-bom `2024.06.00`).

### Bancos suportados

Nubank, Itaú, Mercado Pago. **Bradesco é deliberadamente não suportado** —
não existe parser nem stub pra ele; não adicionar sem pedido explícito.

## Estrutura de diretórios

```
android/app/src/main/java/com/moneyhole/
  MoneyHoleApp.kt                Application; init do PDFBox aqui
  MainActivity.kt                única Activity; calcula WindowSizeClass, tema
  categorizer/Categorizer.kt     regras regex de categorização (ordem importa)
  data/
    PreferencesRepository.kt     DataStore (tema, modo de resumo)
    SharedFileHolder.kt          ponte Intent (ACTION_SEND) -> UploadScreen
    local/
      AppDatabase.kt, DatabaseProvider.kt   Room + singleton + Migrations
      InvoiceRepository.kt       fachada única sobre os DAOs; regra de negócio
      InvoiceWithTransactions.kt @Relation Invoice + List<Transaction>
      SummaryAggregator.kt       agregação pra Dashboard/Comparação
      entity/                    InvoiceEntity, TransactionEntity, DefaultPasswordEntity,
                                  CategoryOverrideEntity (camelCase idiomático)
      dao/                       um DAO por entidade
    email/
      EmailCredentialsRepository.kt  EncryptedSharedPreferences (senha de app real + lastProcessedUid)
      EmailFetcher.kt            IMAP puro (javax.mail), sem Context - testável
      EmailFetchCoordinator.kt   singleton: dispara/compartilha o fetch (auto no start + botão manual)
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
      PasswordFieldWithReveal.kt  campo de senha com revelar (senhas PDF + e-mail)
    dashboard/                    tela inicial: card "Resumo geral"
    invoicesbycard/               quebra por banco/cartão/mês
    invoicedetail/                transações de uma fatura, filtros
    comparison/                   comparação entre períodos + gráfico de barras
    upload/                       envio de PDF (picker ou share-intent)
    passwords/                    CRUD de senhas padrão de PDF
    categories/                   tela "Categorização manual" (overrides)
    email/                        configurar IMAP + botão "Buscar faturas agora"
    settings/                     tema, idioma, resumo, atalhos pras telas acima
    theme/                        cores, visual de categoria/banco
```

## Decisões de arquitetura (não reverter sem motivo)

- **Sem backend próprio.** Tudo Room + PdfBox no próprio app. Rede existe
  só pra conexão IMAP direta do usuário com o provedor dele — não recriar
  Retrofit/API própria.
- **Busca de fatura por e-mail é IMAP + senha de app, não OAuth.** Decisão
  explícita do usuário: evita todo o setup de Google Cloud Console/tela de
  consentimento/verificação de app que o OAuth exigiria. Funciona com
  qualquer provedor com IMAP (não só Gmail). Trade-off aceito conscientemente:
  a senha de app fica armazenada no dispositivo (nunca sai dele), diferente
  de OAuth que nunca guarda segredo nenhum.
  - `EmailCredentialsRepository` usa `EncryptedSharedPreferences`
    (`androidx.security:security-crypto`), não Room/DataStore em texto puro
    — é uma credencial de conta de verdade, diferente das senhas de abrir
    PDF (baixo risco, ficam em Room sem criptografia mesmo).
  - `EmailFetcher` é um `object` sem `Context`, só `javax.mail` — mesma
    filosofia do `InvoiceDispatcher` (parsing puro, testável sem
    Android/Room, sem precisar de emulador pra testar a lógica de extrair
    anexo PDF do MIME).
  - `com.sun.mail:android-mail` + `com.sun.mail:android-activation` geram
    conflito de `META-INF/NOTICE.md`/`LICENSE.md` duplicado entre os dois
    jars — resolvido com bloco `packaging { resources { excludes += ... } }`
    no `build.gradle.kts` (conflito conhecido/documentado dessas libs, não
    peculiaridade deste projeto).
- **Busca de e-mail roda automaticamente uma vez por cold start, e também
  manualmente** (2026-07-27, revisão da decisão original de "só botão
  manual" — pedido explícito do usuário). `EmailFetchCoordinator`
  (`data/email/`) é um singleton com `CoroutineScope` próprio (não escopado
  a nenhuma tela): `MoneyHoleApp.onCreate()` chama
  `EmailFetchCoordinator.fetchOnAppStart()`, que só dispara se já existe
  e-mail configurado e só roda uma vez por processo (flag em memória, não
  `WorkManager` — continua sem polling periódico em background, só no
  momento em que o app abre). O botão "Buscar faturas agora" chama
  `fetchNow()` no mesmo coordinator, então os dois caminhos compartilham o
  mesmo `StateFlow<EmailFetchState>` — a Dashboard também observa esse
  state e mostra uma barra de progresso ("Buscando faturas por
  e-mail...") enquanto uma busca (automática ou manual) está rolando,
  além de recarregar a lista de faturas assim que ela termina.
  - **Sincronização incremental via IMAP UID**, não por data. Refazer a
    busca por `ReceivedDateTerm` a cada vez rebaixava os mesmos e-mails do
    servidor toda vez (~45 mensagens, ~1/segundo) mesmo já tendo
    processado todos antes. `EmailCredentialsRepository` guarda
    `lastProcessedUid`; `EmailFetcher.fetchPdfAttachments` usa
    `(IMAPFolder).getMessagesByUID(lastProcessedUid + 1, UIDFolder.LASTUID)`
    pra pegar só mensagens novas, caindo pro filtro por
    `sinceDays` (60 dias) só na primeira busca (`lastProcessedUid == 0`).
    `save()` em `EmailCredentialsRepository` reseta `lastProcessedUid` pra
    0 — trocar a conta/senha refaz a janela de 60 dias do zero, evitando
    UID de uma conta antiga vazar pra outra.
  - Falhas de import (PDF que não é fatura reconhecida, senha errada,
    etc.) são logadas via `Log.w("EmailFetchCoordinator", ...)` com a
    exceção completa — antes só incrementava um contador `failed` sem
    registrar o motivo. Útil pra diferenciar “não reconheceu o banco”
    (anexo que não é fatura de verdade, ex: boleto de condomínio,
    balancete) de um bug real no parser.
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
- **`MainActivity` é `AppCompatActivity`, não `ComponentActivity`** — só
  por causa da troca de idioma por-app (item abaixo). O app é 100%
  Compose/Material3, `AppCompatActivity` aqui não traz Views nem o
  visual "Material 2" do AppCompat: o único efeito é o tema da activity
  ter que herdar de `Theme.AppCompat.*` em vez de `android:Theme.*`
  (`themes.xml`) — toda UI real continua vindo do `MoneyHoleTheme`
  (Material3) via `setContent`. Não trocar de volta pra
  `ComponentActivity` sem entender a troca de idioma primeiro.
- **Troca de idioma via `AppCompatDelegate.setApplicationLocales()`**
  (Settings > Idioma). Duas pegadinhas não óbvias descobertas na prática,
  testando ao vivo (`adb shell cmd locale get-app-locales com.moneyhole`
  ficava `[]` mesmo depois de selecionar um idioma):
  1. Sem `AndroidManifest.xml` declarar
     `<service android:name="androidx.appcompat.app.AppLocalesMetadataHolderService" android:enabled="false" android:exported="false"><meta-data android:name="autoStoreLocales" android:value="true" /></service>`,
     a chamada não sincroniza com o `LocaleManager` da plataforma (API
     33+) nem persiste nada — não é automático só de ter a dependência
     `androidx.appcompat:appcompat` no Gradle.
  2. Sem a Activity ser `AppCompatActivity` (item acima), não existe
     nenhum `AppCompatDelegate` de verdade registrado pra receber a
     notificação de mudança — `setApplicationLocales` roda mas não tem
     efeito nenhum (nem no rádio de idioma, nem no locale persistido).
     Com os dois em vigor, a troca funciona e persiste sozinha; **não**
     chamar `activity.recreate()` manualmente depois de
     `setApplicationLocales()` — isso causa uma corrida de fato observada
     (o relaunch manual chega antes do sistema persistir o locale, e a
     troca não pega) — deixar o próprio framework recriar a Activity.

## Convenções de código

- **Padrão de código é inglês** (decisão tomada após a migração pro Room
  deixar isso seguro de fazer). Identificadores Kotlin (classes, funções,
  variáveis, nomes de arquivo e de pacote) e comentários são em inglês.
  Strings de schema (nomes de tabela/coluna do Room via `@ColumnInfo`/
  `tableName`, chaves do DataStore) ficam como estão — são valores
  gravados em disco, renomear quebraria dados existentes sem uma
  `Migration`. Nomes de categoria (`"Compras"`, `"Alimentação"`, etc.) e
  códigos de banco (`"nubank"`, `"itau"`) são dados/conteúdo do app
  pessoal, não identificadores de código nem texto de UI — não são
  traduzidos, não viram `strings.xml`.
- **UI é internacionalizada (i18n)** — decisão de 2026-07-25, superando a
  antiga regra "UI sempre em português". Todo texto visível ao usuário
  vive em `res/values/strings.xml` (português, idioma base/fallback),
  `res/values-en/strings.xml` e `res/values-es/strings.xml`. Ao adicionar
  UI nova: nunca hardcodar string visível, sempre `stringResource(R.string...)`
  (Compose) ou `context.getString(...)`/`getApplication<Application>().getString(...)`
  (ViewModel) — e adicionar a entrada correspondente nos 3 arquivos.
  `./gradlew lintDebug` falha silenciosamente em avisar (não é erro fatal)
  se um idioma ficar com string faltando (`MissingTranslation`) — rodar e
  checar o report depois de mexer em strings. Troca de idioma é via
  `AppCompatDelegate.setApplicationLocales()` (Configurações > Idioma:
  Automático/Português/English/Español) — ver "Troca de idioma" abaixo
  pro porquê `MainActivity` precisa ser `AppCompatActivity`, não
  `ComponentActivity`. "R$" (símbolo de Real) nunca é traduzido/trocado
  por `NumberFormat` de moeda — o app só lida com faturas em reais,
  independente do idioma da UI.
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
   **A suíte automatizada (`./gradlew testDebugUnitTest
   connectedDebugAndroidTest`) não é rodada localmente em nenhum ponto
   deste fluxo** — nem durante o desenvolvimento, nem antes de commitar.
   Ela roda exclusivamente no CI, um único lugar de verdade (ver "CI
   (GitHub Actions)" abaixo).
3. `git add` arquivos específicos (nunca `-A` sem checar o `git status`
   antes), commit com mensagem explicando o *porquê*.
4. `git push -u origin <branch>`.
5. `gh pr create` com corpo descrevendo mudança + evidência do teste
   manual no emulador (passo 2).
6. **O CI precisa terminar verde pra `gh pr merge` funcionar** — não é
   mais um lembrete, é bloqueio real do GitHub (branch protection, ver "CI
   (GitHub Actions)" abaixo). `gh pr checks <número> --watch` pra
   acompanhar antes de tentar mergear.
7. `gh pr merge --squash --delete-branch`.
8. `git fetch origin --prune && git checkout main && git pull`.
9. Rebuildar e copiar o APK atualizado pro Desktop quando o usuário pedir
   (`cp android/app/build/outputs/apk/debug/app-debug.apk` pro
   OneDrive/Desktop — a pasta Desktop real fica em `OneDrive/Desktop`,
   não em `C:\Users\<user>\Desktop`).

## CI (GitHub Actions)

`.github/workflows/ci.yml` roda em todo PR (e push em `main`), dois jobs
independentes, ambos em `ubuntu-latest`:

- **`unit-tests`**: `./gradlew testDebugUnitTest` (Categorizer,
  SummaryAggregator) — sempre roda, sem dependência externa.
- **`instrumented-tests`**: emulador Android via
  `reactivecircus/android-emulator-runner` (API 30, AVD sempre criado do
  zero — **sem** cache de snapshot, ver nota no próprio `ci.yml`: a
  combinação de snapshot salvo + recarregado bateu num bug real de
  compatibilidade com a versão atual do emulator, `adb` nunca saía de
  "device offline" e o job estourava os 10min de timeout; um boot único
  por execução é ~1min mais lento mas confiável) rodando `./gradlew
  connectedDebugAndroidTest`, **excluindo `InvoiceDispatcherTest`** via
  `-Pandroid.testInstrumentationRunnerArguments.notClass=...` — esse
  teste precisa dos PDFs reais de `src/androidTest/assets/`, que são
  gitignored de propósito (dado pessoal, nunca commitados) e por isso
  não existem no runner do CI. `RoomFoundationTest` e
  `InvoiceRepositoryTest` não dependem deles e rodam normalmente.

**Enforcement real, não só informativo.** O repo é público (decisão do
usuário, 2026-07-25) especificamente pra habilitar branch protection com
required status checks — feature paga em repo privado no plano free do
GitHub. `main` tem, via `gh api .../branches/main/protection`:

- PR obrigatório pra mergear (bloqueia push direto em `main`).
- `required_approving_review_count: 0` — ainda força passar por PR, mas
  não exige um segundo humano aprovando (repo solo; `count: 1` +
  `enforce_admins: true` cria deadlock onde nem o dono consegue mergear
  o próprio PR — não usar `count: 1` aqui sem adicionar um segundo
  colaborador antes).
- Os dois jobs do CI (`JVM unit tests`, `Instrumented tests (Room,
  InvoiceRepository)`) como required status checks — `gh pr merge`
  falha de verdade se algum estiver vermelho ou ainda rodando.
- `enforce_admins: true` (a regra vale até pro dono), sem force-push,
  sem deleção da branch.
- `delete_branch_on_merge: true` no repo (branches de PR mergeado somem
  sozinhas).

## Dev loop / testes

- `android/dev.sh <comando>`: `emulator` (abre o AVD `money_hole_test`),
  `build`, `install`, `start`, `run` (build+install+start), `logs`
  (logcat do processo do app).
- **Testes automatizados rodam só no CI, nunca localmente.** Não fazem
  parte do desenvolvimento iterativo nem de um passo manual antes do
  commit — o único lugar onde a suíte executa é o GitHub Actions, ao abrir
  o PR (ver "CI (GitHub Actions)"). Localmente, a verificação é o teste
  manual ao vivo no emulador (passo 2 do fluxo de git). Ao implementar
  feature nova, sempre que fizer sentido (lógica de negócio, não Compose
  puro), escrever o teste automatizado junto do código — ele só vai ser
  executado depois, no PR, mas a cobertura entra no mesmo commit.
- Testes JVM puros (`src/test/`), sem Android runtime — rodados pelo job
  `unit-tests` do CI via `./gradlew testDebugUnitTest`:
  - `CategorizerTest` — regras de categorização por regex.
  - `SummaryAggregatorTest` — agregação/comparação mensal (arredondamento,
    variação %, casos vazios).
- Testes instrumentados (`src/androidTest/`), precisam de emulador/device
  — rodados pelo job `instrumented-tests` do CI via `./gradlew
  connectedDebugAndroidTest`:
  - `RoomFoundationTest` — schema Room (cascade delete, unique constraints).
  - `InvoiceRepositoryTest` — regras de negócio do `InvoiceRepository`
    (rejeição de reenvio duplicado, aprendizado/remoção de categoria) contra
    um banco Room em memória; usa o parâmetro `database` do construtor de
    `InvoiceRepository` pra injetar esse banco de teste em vez do singleton
    real (`DatabaseProvider`).
  - `InvoiceDispatcherTest` — pipeline de parsing contra PDFs reais dos 3
    bancos suportados. **Não roda no CI** (ver próximo bullet).
- **PDFs de fatura reais nunca vão pro git** — `android/app/src/androidTest/assets/`
  está no `.gitignore` de propósito (dado pessoal). Por isso
  `InvoiceDispatcherTest` é excluído explicitamente do job `instrumented-tests`
  do CI (`-Pandroid.testInstrumentationRunnerArguments.notClass=...`) — o
  runner nunca tem esses arquivos. `RoomFoundationTest` e
  `InvoiceRepositoryTest` não dependem deles e rodam normalmente no CI.
- Pra inspecionar o banco Room ao vivo: puxar `.db`, `.db-wal` e `.db-shm`
  juntos (Room usa WAL, dado recente pode não estar no `.db` principal)
  via `adb exec-out run-as com.moneyhole cat databases/money_hole.db > arquivo`
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
