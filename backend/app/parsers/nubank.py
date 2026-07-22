import re
from datetime import date

from .base import BankParser, ParsedFatura, ParsedTransacao
from .utils import parse_valor_br

MESES_ABREV = {
    "JAN": 1, "FEV": 2, "MAR": 3, "ABR": 4, "MAI": 5, "JUN": 6,
    "JUL": 7, "AGO": 8, "SET": 9, "OUT": 10, "NOV": 11, "DEZ": 12,
}

_MESES_ALTERNATIVAS = "|".join(MESES_ABREV.keys())

DATA_VENCIMENTO_RE = re.compile(
    rf"Data de vencimento:\s*\d{{1,2}}\s+({_MESES_ALTERNATIVAS})\s+(\d{{4}})",
    re.IGNORECASE,
)

# Ex: "16 JUN •••• 3333 Pg *Universal Music St - Parcela 2/2 R$ 99,90"
# Ex: "16 JUN KaBuM! - NuPay - Parcela 2/8 R$ 28,00" (sem digitos do cartao)
# Ex: "23 JUN Pagamento em 23 JUN −R$ 1.500,00" (pagamento, sinal negativo)
LINHA_TRANSACAO_RE = re.compile(
    rf"^(?P<dia>\d{{2}})\s+(?P<mes>{_MESES_ALTERNATIVAS})\s+"
    r"(?:•{2,6}\s*\d{3,4}\s+)?"
    r"(?P<descricao>.+?)"
    r"(?:\s-\s*Parcela\s+(?P<parcela_atual>\d{1,2})/(?P<parcela_total>\d{1,2}))?"
    r"\s+(?P<sinal>[−-])?R\$\s*(?P<valor>\d{1,3}(?:\.\d{3})*,\d{2})\s*$",
    re.IGNORECASE,
)

# Linhas de "Pagamentos e Financiamentos" que nao sao gastos reais.
DESCRICAO_IGNORAR_RE = re.compile(
    r"^(pagamento em|saldo restante da fatura)", re.IGNORECASE
)


class NubankParser(BankParser):
    banco = "nubank"

    def matches(self, texto: str) -> bool:
        return "nubank" in texto.lower()

    def parse(self, texto: str, pdf_bytes: bytes = b"", senha: str = "") -> ParsedFatura:
        mes, ano = self._extrair_mes_ano_referencia(texto)
        transacoes = self._extrair_transacoes(texto, mes, ano)
        return ParsedFatura(
            banco=self.banco,
            mes_referencia=mes,
            ano_referencia=ano,
            transacoes=transacoes,
        )

    def _extrair_mes_ano_referencia(self, texto: str) -> tuple[int, int]:
        match = DATA_VENCIMENTO_RE.search(texto)
        if not match:
            raise ValueError(
                "Não foi possível identificar o mês/ano de referência da fatura Nubank"
            )
        return MESES_ABREV[match.group(1).upper()], int(match.group(2))

    def _extrair_transacoes(
        self, texto: str, mes_referencia: int, ano_referencia: int
    ) -> list[ParsedTransacao]:
        transacoes = []
        for linha in texto.splitlines():
            match = LINHA_TRANSACAO_RE.match(linha.strip())
            if not match:
                continue

            grupos = match.groupdict()
            descricao = grupos["descricao"].strip()

            if grupos["sinal"] or DESCRICAO_IGNORAR_RE.match(descricao):
                continue

            mes_transacao = MESES_ABREV[grupos["mes"].upper()]
            ano_transacao = ano_referencia
            if mes_transacao == 12 and mes_referencia == 1:
                ano_transacao -= 1

            transacoes.append(
                ParsedTransacao(
                    data=date(ano_transacao, mes_transacao, int(grupos["dia"])),
                    descricao=descricao,
                    valor=parse_valor_br(grupos["valor"]),
                    parcela_atual=int(grupos["parcela_atual"]) if grupos["parcela_atual"] else None,
                    parcela_total=int(grupos["parcela_total"]) if grupos["parcela_total"] else None,
                )
            )
        return transacoes
