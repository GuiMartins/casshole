# Money Hole

App Android pessoal (Kotlin + Jetpack Compose) pra analisar faturas de
cartão de crédito em PDF — Nubank, Itaú e Mercado Pago. 100% on-device: sem
backend próprio, sem servidor pra manter. A única exceção de rede é a busca
automática de fatura por e-mail via IMAP, com a senha de app ficando só no
seu aparelho.

## Funcionalidades

- **Upload de fatura**: selecione o PDF no app ou compartilhe de qualquer
  outro app pra ele — reconhece o banco e organiza tudo automaticamente.
- **Busca automática por e-mail (IMAP)**: configure seu e-mail e uma senha
  de app, e o app busca faturas novas direto da caixa de entrada — uma vez
  ao abrir o app e sob demanda, com sincronização incremental (não
  reprocessa e-mails já vistos) e nova tentativa automática de mensagens
  que falharam por senha incorreta assim que a senha certa é cadastrada.
- **Senhas padrão de PDF**: cadastre senhas comuns (ex: dígitos do CPF)
  pro app tentar abrir faturas protegidas sem digitar toda vez.
- **Categorização automática** por regras, com aprendizado: corrigir a
  categoria de uma transação ensina o app a aplicar a mesma correção
  automaticamente em faturas futuras do mesmo estabelecimento.
- **Resumo do mês** (números ou gráfico de pizza), **comparação entre
  períodos**, e visão por cartão/banco.
- **Onboarding configurável**: primeira execução já deixa configurar tema,
  senhas padrão e e-mail, não é só um tour de telas.
- **Tema claro/escuro/automático**, **PT/EN/ES**.

## Stack

- Kotlin + Jetpack Compose + Material3
- Room (persistência local)
- PdfBox-Android (parsing de PDF, extração de texto por coluna)
- `com.sun.mail:android-mail` (IMAP)
- DataStore + `EncryptedSharedPreferences` (preferências e credencial de
  e-mail, respectivamente)

Bancos suportados: Nubank, Itaú, Mercado Pago. Bradesco é deliberadamente
não suportado.

## Rodando localmente

```bash
cd android
./dev.sh emulator   # abre o AVD de teste
./dev.sh run        # build + install + start
```

Outros comandos úteis em `android/dev.sh`: `build`, `install`, `start`,
`logs`.

## Contribuindo

Fluxo GitFlow: `feature/*`/`fix/*` a partir de `develop` (branch padrão),
PR de volta pra `develop`. `main` só recebe `release/*`/`hotfix/*` e
reflete o que foi de fato buildado — cada push em `main` gera
automaticamente uma tag SemVer (Conventional Commits) e uma
[release](https://github.com/GuiMartins/money-hole/releases) com o APK
anexado.

Detalhes completos do fluxo de trabalho, decisões de arquitetura e
convenções de código em [`CLAUDE.md`](CLAUDE.md).

## Licença

Projeto pessoal, sem licença formal definida.
