from .base import BankParser, ParsedFatura
from .utils import extrair_mes_ano_referencia, extrair_transacoes_generico


class BradescoParser(BankParser):
    banco = "bradesco"

    def matches(self, texto: str) -> bool:
        return "bradesco" in texto.lower()

    def parse(self, texto: str, pdf_bytes: bytes = b"", senha: str = "") -> ParsedFatura:
        mes, ano = extrair_mes_ano_referencia(texto)
        transacoes = extrair_transacoes_generico(texto, ano)
        return ParsedFatura(
            banco=self.banco,
            mes_referencia=mes,
            ano_referencia=ano,
            transacoes=transacoes,
        )
