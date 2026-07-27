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
  - **Mensagens com falha ficam numa lista de retry separada
    (`EmailCredentialsRepository.getFailedUids()`/`setFailedUids()`, CSV em
    string), não travam nem são puladas pelo cursor principal.** Primeira
    versão avançava `lastProcessedUid` pra TODA mensagem vista, sucesso ou
    falha — uma fatura real que falhasse por senha errada nunca mais seria
    tentada de novo, mesmo depois do usuário cadastrar a senha certa (bug
    real: 3 das 4 faturas reais do usuário sumiram silenciosamente assim).
    Correção errada #1: fazer o cursor parar logo antes da primeira falha —
    resolve o "nunca mais tenta" mas reprocessa TODAS as mensagens depois
    dela a cada fetch (também observado ao vivo: 56 mensagens reprocessadas
    por causa de 16 falhas). Versão final: `EmailFetcher.fetchPdfAttachments`
    aceita `retryUids: Set<Long>` e busca essas mensagens específicas via
    `getMessagesByUID(longArray)` **além** do range normal — o cursor
    principal sempre avança (nunca trava), e só as mensagens que ainda
    falham continuam na lista de retry (removidas assim que têm sucesso).
    Cada `FetchedAttachment` carrega o `messageUid` de origem, necessário
    pra saber qual mensagem gerou qual resultado.
  - **`EmailCredentialsRepository.save()` só reseta `lastProcessedUid`/lista
    de retry se a conta (`address`/`imapHost`) realmente mudou, não a cada
    chamada.** Bug real: o botão único "Buscar faturas agora" (ver decisão
    de UI única de save+fetch) chama `save()` toda vez que é clicado, então
    resetar incondicionalmente destruía a sincronização incremental a cada
    clique, forçando um re-scan completo de 40+ mensagens sempre — só
    reseta agora quando `address`/`imapHost` mudam de verdade (trocar só a
    senha de app, ex: senha expirada, não invalida os UIDs já processados).
  - Falhas de import (PDF que não é fatura reconhecida, senha errada,
    etc.) são logadas via `Log.w("EmailFetchCoordinator", ...)` com a
    exceção completa — antes só incrementava um contador `failed` sem
    registrar o motivo. Útil pra diferenciar “não reconheceu o banco”
    (anexo que não é fatura de verdade, ex: boleto de condomínio,
    balancete) de um bug real no parser. Falhas especificamente por senha
    incorreta (`IncorrectPasswordException`) são contadas à parte
    (`EmailImportResult.failedPasswords`) e mostradas na UI com uma dica
    acionável ("cadastre a senha certa em Configurações > Senhas padrão")
    em vez de só aparecerem como um número genérico de "falharam".
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

## Fluxo de trabalho (git) — GitFlow

Adotado em 2026-07-27 (pedido explícito do usuário: "implementa gotflow no
repo"), substituindo o fluxo anterior de feature branch direto a partir de
`main`. Duas branches de longa duração:

- **`develop`** — branch de integração, **default branch do repo no
  GitHub**. Todo trabalho do dia a dia (`feature/*`, `fix/*`) nasce daqui e
  volta pra cá via PR. Reflete "tudo que já foi implementado e testado",
  não necessariamente o que está instalado no celular do usuário.
- **`main`** — reflete o que foi de fato buildado/instalado
  (`app-debug.apk` copiado pro Desktop). Só recebe merge via `release/*`
  (arredondando o que já está em `develop`) ou `hotfix/*` (correção urgente
  em cima do que já está em produção, sem esperar o resto de `develop`).

### Mensagens de commit e PR: Gitmoji + Conventional Commits

Adotado em 2026-07-27 (pedido explícito do usuário). As duas convenções
juntas, **nessa ordem** — tipo primeiro, emoji logo depois:

```
<tipo>: <emoji> <descrição>
```

Ex: `feat: ✨ busca automática de fatura por e-mail`,
`fix: 🐛 spinner que não resolvia no fetch de e-mail`.

**Por que o tipo vem antes do emoji, não depois**: `mathieudutour/github-tag-action`
(ver "Release automática" abaixo) calcula o bump de versão com regex
ancorada no *início* da string (`^feat:`, `^fix:`, etc.) — um emoji na
frente quebraria a detecção. Gitmoji "puro" (emoji sozinho, sem o texto
`feat:`/`fix:`) não é usado aqui por esse motivo: a ferramenta de
versionamento não entende código de emoji, só Conventional Commits.

Tabela de emoji (subconjunto do [gitmoji.dev](https://gitmoji.dev) oficial,
mapeado 1:1 pros tipos que já usamos):

| Tipo        | Emoji | Quando usar                                       |
|-------------|-------|----------------------------------------------------|
| `feat:`     | ✨    | funcionalidade nova                                 |
| `fix:`      | 🐛    | correção de bug                                     |
| `docs:`     | 📝    | só documentação (CLAUDE.md, comentários, README)    |
| `refactor:` | ♻️    | reestrutura código sem mudar comportamento          |
| `test:`     | ✅    | adiciona/corrige teste                              |
| `chore:`    | 🔧    | config, CI, tooling, dependências                   |
| `style:`    | 💄    | mudança visual/UI sem lógica nova                   |
| `perf:`     | ⚡️    | melhoria de performance                             |
| `security:` | 🔒️    | correção de segurança                               |
| `revert:`   | ⏪️    | reverte um commit/PR anterior                       |

**Cuidado real já vivido**: a string literal `BREAKING CHANGE` em
qualquer lugar do corpo do commit/PR — mesmo só sendo *mencionada* como
documentação — é lida pela action como um bump major de verdade (ver
"Erro real já cometido" mais abaixo). Nunca escrever esse texto à toa.

**Changelog**: o corpo do GitHub Release já vem gerado automaticamente
pela action a partir dessas mensagens de commit (ver "Release automática"
abaixo) — isso é a base, não precisa reescrever do zero. Se algum ponto
ficar raso ou confuso só pela mensagem do commit, editar o release depois
(`gh release edit`) pra enriquecer *esse* ponto específico, sem duplicar
o que os commits já deixam claro.

Passos, sem pedir confirmação a cada um (autorização padrão já dada pelo
usuário: "vai fazendo e mergeando"):

1. `git checkout -b feature/nome-descritivo` (ou `fix/...`) a partir de
   **`develop`** atualizado — não mais `main`.
2. Implementar, buildar (`./dev.sh build`), testar ao vivo no emulador.
   **A suíte automatizada (`./gradlew testDebugUnitTest
   connectedDebugAndroidTest`) não é rodada localmente em nenhum ponto
   deste fluxo** — nem durante o desenvolvimento, nem antes de commitar.
   Ela roda exclusivamente no CI, um único lugar de verdade (ver "CI
   (GitHub Actions)" abaixo).
3. `git add` arquivos específicos (nunca `-A` sem checar o `git status`
   antes), commit com mensagem explicando o *porquê*.
4. `git push -u origin <branch>`.
5. `gh pr create --base develop` com corpo descrevendo mudança + evidência
   do teste manual no emulador (passo 2).
6. **O CI precisa terminar verde pra `gh pr merge` funcionar** em
   `develop` (branch protection, ver "CI (GitHub Actions)" abaixo). `gh pr
   checks <número> --watch` pra acompanhar antes de tentar mergear.
7. `gh pr merge --squash --delete-branch`.
8. `git fetch origin --prune && git checkout develop && git pull`.

Pra levar o que está em `develop` pro celular de verdade (equivalente a
"cortar uma release"):

9. `git checkout -b release/o-que-mudou develop` (ou pular a branch de
   release pra mudanças pequenas e ir direto de PR `develop -> main`,
   dado que é um projeto solo sem QA formal — usar `release/*` quando fizer
   sentido isolar/testar mais antes de ir pro `main`).
10. `gh pr create --base main --head release/o-que-mudou` (ou `--head
    develop` se pulou o passo 9), **sempre `--squash`** (nunca merge commit
    normal aqui — ver "Erro real já cometido" abaixo pro porquê). Merge
    depois do CI verde.
11. `git checkout main && git pull` — rebuildar e copiar o APK atualizado
    pro Desktop quando o usuário pedir (`cp
    android/app/build/outputs/apk/debug/app-debug.apk` pro OneDrive/Desktop
    — a pasta Desktop real fica em `OneDrive/Desktop`, não em
    `C:\Users\<user>\Desktop`).

**Nunca fazer `git merge main` dentro de `develop`** quando `release/*`
veio de `develop` (o caso normal) — `develop` **já contém** tudo que foi
squash-mergeado pra `main` (ele é a origem), então não existe divergência
de conteúdo real pra "sincronizar". Só existe uma divergência **aparente**
de grafo de commits (squash quebra ancestralidade), que faz o próximo PR
`develop -> main` aparecer como "not mergeable" no GitHub — isso é
esperado e cosmético, não significa que falta algo em `develop`.

Se esse PR aparecer como não-mergeável: resolver num **branch descartável**
a partir de `develop` (nunca no `develop` em si), mantendo sempre o
conteúdo de `develop` nos arquivos em conflito, e dar squash-merge desse
branch descartável pra `main` — nunca um merge commit normal, e nunca
mergear `main` de volta pro `develop` permanente.

**`hotfix/*`**: único caso onde `develop` *realmente* fica sem algo que
`main` tem — branch a partir de `main` (não `develop`) pra corrigir algo
já instalado sem esperar o resto de `develop`. PR `hotfix/* -> main`
(squash), e depois de mergear, **agora sim** precisa levar a correção pra
`develop`: `git checkout develop`, criar um branch novo a partir dele,
reaplicar a mudança do hotfix nesse branch (`git cherry-pick <commit-do-hotfix>`
ou reimplementar manualmente — não `git merge main`), e PR normal desse
branch pra `develop`.

**Erro real já cometido (2026-07-27), pra não repetir**: depois do
primeiro `develop -> main` (PR #57, squash, virou tag `v1.0.0`), tentei
"sincronizar" `main` de volta pro `develop` com `git merge main` achando
que havia divergência real — não havia (ver acima). Isso criou um commit
de merge em `develop` que trouxe de volta os commits *originais*
pré-squash (que só existiam em `develop`, nunca tinham entrado em `main`).
No PR `develop -> main` seguinte, usei merge commit normal (não squash)
"pra preservar ancestralidade" — isso reintroduziu esses commits originais
em `main` pela primeira vez, e a action de versionamento (ver "Release
automática" abaixo) os rescaneou desde a última tag, pegando de novo o
texto "BREAKING CHANGE" de um commit antigo (que já tinha sido processado
uma vez) e forçando um bump major indevido (`v2.0.0` ao invés do `v1.0.1`
esperado). Corrigido manualmente (`gh release delete v2.0.0 --cleanup-tag`
+ `gh release create v1.0.1` apontando pro `main` certo). Lição: squash
pra `main` sempre, nunca merge commit; nunca `git merge main` dentro de
`develop` sem ser hotfix de verdade.

**Limitação conhecida do GitHub free**: não dá pra restringir tecnicamente
"só aceitar PR em `main` vindo de `release/*` ou `hotfix/*`" (isso é
`restrictions` por padrão de branch, feature paga). É convenção documentada
aqui, não bloqueio automático — na dúvida, checar se a branch de origem do
PR contra `main` realmente é `release/*`/`hotfix/*` antes de mergear.

## CI (GitHub Actions)

`.github/workflows/ci.yml` roda em todo PR (contra qualquer branch) e em
push direto em `main` ou `develop`, três jobs em `ubuntu-latest`:

- **`changes`**: `dorny/paths-filter` detecta se o diff toca
  `android/**` ou o próprio `ci.yml` — usado só pra decidir se
  `instrumented-tests` roda (ver abaixo). PR que só mexe em docs
  (`CLAUDE.md`, `README.md`) ou noutro workflow (`release.yml`) pula o
  emulador inteiro, que é o real gargalo de tempo do CI (~3min vs ~1min
  do `unit-tests`).
- **`unit-tests`**: `./gradlew testDebugUnitTest` (Categorizer,
  SummaryAggregator) — sempre roda, sem dependência externa, custo baixo
  o suficiente pra não valer a pena condicionar.
- **`instrumented-tests`**: `if: needs.changes.outputs.android-code ==
  'true'` — quando pulado, GitHub reporta o job como "skipped", que
  conta como passou pra required status checks (comportamento oficial
  do GitHub Actions pra jobs condicionais, não é gambiarra). Emulador
  Android via
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

`develop` tem a mesma proteção espelhada de `main` (PR obrigatório, os
mesmos dois status checks, `enforce_admins: true`, sem force-push, sem
deleção) e é o default branch do repo no GitHub — aplicado manualmente
pelo usuário em 2026-07-27 (mudança de configuração de repo/infraestrutura
compartilhada, bloqueada pro assistente automático via `gh repo edit
--default-branch` e `gh api .../branches/develop/protection`).

## Release automática (GitHub Actions)

`.github/workflows/release.yml` roda em todo push em `main` (ou seja, todo
merge de `release/*`/`hotfix/*`) — versiona e publica sozinho, sem passo
manual:

1. **`mathieudutour/github-tag-action`** calcula o próximo SemVer
   (`vX.Y.Z`) a partir de Conventional Commits nos commits novos:
   `feat:` → sobe minor, `fix:` → sobe patch, `BREAKING CHANGE` (em
   qualquer lugar do corpo) → sobe major. Sem prefixo reconhecido, cai no
   `default_bump: patch`. Não existe input `initial_version` nessa versão
   da action (`v6.2` — testado ao vivo, GitHub Actions avisa "Unexpected
   input" e ignora silenciosamente); a primeira tag (`v1.0.0`) já existe no
   repo desde 2026-07-27, então isso não é mais um problema de bootstrap.
   - **Importante**: como todo merge é squash, o título do PR
     `release/*`/`hotfix/* -> main` vira a mensagem do commit em `main` —
     é ali que o prefixo Conventional Commits importa. Ex:
     `feat: busca automática de fatura por e-mail` (minor),
     `fix: spinner que não resolvia no fetch de e-mail` (patch),
     `feat!: remove suporte a Bradesco` ou corpo com `BREAKING CHANGE:
     ...` (major).
   - **Pegadinha real, já vivida**: a action detecta a string literal
     `BREAKING CHANGE` em qualquer lugar do corpo do commit, mesmo que só
     esteja sendo *mencionada* (ex: um PR descrevendo essa própria regra de
     versionamento). O PR #57 ("feat: adopt GitFlow...") tinha essa string
     no corpo só como documentação e isso forçou bump major sem intenção —
     coincidentemente resultou em `v1.0.0`, o valor certo, mas por acidente.
     Evitar escrever "BREAKING CHANGE" no título/corpo de PR contra `main`
     a menos que seja pra valer.
2. Builda o APK **release assinado e minificado** (`./gradlew
   assembleRelease`, `isMinifyEnabled = true`) — ver "Release assinada
   (keystore)" abaixo pra como a assinatura funciona. Continua sendo
   side-load direto (sem Play Store), mas agora com R8 reduzindo o
   tamanho (~30MB debug → ~9MB release) e sem `debuggable=true`.
3. **`softprops/action-gh-release`** cria o GitHub Release na tag calculada,
   anexando o APK renomeado (`money-hole-vX.Y.Z.apk`, não mais o nome
   genérico do Gradle) e usando o changelog gerado pela action de tag
   como corpo.

Não depende do CI (`ci.yml`) pra rodar — como `main` já é protegido com
required status checks, o commit que chega aqui já passou pela suíte antes
de mergear; essa workflow só versiona e publica o que já está validado.

## Release assinada (keystore)

Adotado em 2026-07-27 (pedido explícito do usuário, depois de perguntar
"pq debug? pq não release?"). Antes disso o app nunca teve
`signingConfigs` pro build type `release` — por isso `assembleRelease`
sempre foi inútil (gerava um APK sem assinatura, impossível de instalar)
e todo build (local e CI) usava `assembleDebug`.

- **A keystore (`money-hole-release.jks`, PKCS12, validade até 2053)
  nunca vai pro repo** — está no `.gitignore` (`*.jks`/`*.keystore`) como
  defesa extra, mas na prática nunca chega a existir dentro do diretório
  do projeto. Ela só existe: (a) como backup do usuário fora do repo, e
  (b) codificada em base64 no GitHub Secret `RELEASE_KEYSTORE_BASE64`,
  decodificada num arquivo temporário só durante o job do
  `release.yml` (deletado no fim do step, `if: always()`).
- **Quatro GitHub Secrets** necessários, criados manualmente pelo usuário
  (não dá pra criar secret via `gh` sem confirmação explícita — mesma
  categoria de "mudança de infraestrutura compartilhada" das outras
  configs de repo neste projeto):
  `RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`,
  `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`.
- **`app/build.gradle.kts`**: `signingConfigs { create("release") { ... } }`
  só é criado se a env `RELEASE_STORE_FILE` existir — build local
  (`./dev.sh`) nunca define essa env, então nunca tenta assinar release
  nem quebra por falta de keystore; só o `release.yml` (via os 4 secrets)
  configura isso.
- **`app/proguard-rules.pro`** — populado pela primeira vez (estava
  vazio). Duas classes de regra descobertas rodando `assembleRelease`
  localmente com a keystore antes de subir pro CI:
  1. `-keep class com.sun.mail.** / javax.mail.** / *.activation.**` —
     Jakarta Mail carrega providers IMAP via reflection
     (`META-INF/javamail.providers`); sem isso o R8 stripa silenciosamente
     e o IMAP quebra em runtime, não em build time. **Testado ao vivo**:
     build release instalado, tentativa de fetch com credenciais falsas
     resultou em "Authentication failed" (erro normal de auth, não crash
     nem `ClassNotFoundException`) — confirma que a reflection sobreviveu.
  2. `-dontwarn com.google.errorprone.annotations.** / javax.annotation.**`
     — o R8 (`minifyReleaseWithR8`) falhou na primeira tentativa com
     "Missing classes" apontando pro Google Tink (motor de criptografia
     por trás do `EncryptedSharedPreferences` de
     `androidx.security:security-crypto`), referenciando anotações que só
     existem em tempo de compilação (errorprone, `javax.annotation`). São
     seguras de silenciar — não afetam comportamento em runtime.
- **Instruções pro usuário rodar uma vez** (`gh secret set` — o
  assistente não pode criar secrets sozinho):
  ```bash
  gh secret set RELEASE_KEYSTORE_BASE64 --repo GuiMartins/money-hole < keystore_base64.txt
  gh secret set RELEASE_STORE_PASSWORD --repo GuiMartins/money-hole --body "..."
  gh secret set RELEASE_KEY_ALIAS --repo GuiMartins/money-hole --body "money-hole-release"
  gh secret set RELEASE_KEY_PASSWORD --repo GuiMartins/money-hole --body "..."
  ```
  A keystore (`.jks`) e as senhas foram entregues ao usuário fora do git
  (arquivo + texto na conversa) — guardar em gerenciador de senhas.
  **Perder a keystore = nunca mais dá pra publicar update assinado com a
  mesma identidade** (só reinstalando do zero, desinstalando a versão
  atual do celular).

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
