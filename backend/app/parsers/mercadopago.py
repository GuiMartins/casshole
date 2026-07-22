import re
from datetime import date

from .base import BankParser, ParsedFatura, ParsedTransacao
from .utils import parse_valor_br

VENCIMENTO_RE = re.compile(r"Vencimento:\s*(\d{2})/(\d{2})/(\d{4})", re.IGNORECASE)

# Ex: "28/11 MERCADOLIVRE*ESHOPIMPORTA Parcela 19 de 24 R$ 116,21"
# Ex: "15/06 Pagamento da fatura de junho/2026 R$ 1.200,00" (pagamento, ignorar)
LINHA_TRANSACAO_RE = re.compile(
    r"^(?P<dia>\d{2})/(?P<mes>\d{2})\s+"
    r"(?P<descricao>.+?)"
    r"(?:\s+Parcela\s+(?P<parcela_atual>\d{1,2})\s+de\s+(?P<parcela_total>\d{1,2}))?"
    r"\s+R\$\s*(?P<valor>\d{1,3}(?:\.\d{3})*,\d{2})\s*$",
    re.IGNORECASE,
)

DESCRICAO_IGNORAR_RE = re.compile(r"^pagamento da fatura", re.IGNORECASE)


class MercadoPagoParser(BankParser):
    banco = "mercadopago"

    def matches(self, texto: str) -> bool:
        texto_lower = texto.lower()
        return "mercado pago" in texto_lower or "mercadopago" in texto_lower

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
        match = VENCIMENTO_RE.search(texto)
        if not match:
            raise ValueError(
                "Não foi possível identificar o mês/ano de referência da fatura Mercado Pago"
            )
        return int(match.group(2)), int(match.group(3))

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
            if DESCRICAO_IGNORAR_RE.match(descricao):
                continue

            mes_transacao = int(grupos["mes"])
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
