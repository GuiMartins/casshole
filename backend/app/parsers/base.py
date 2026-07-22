from abc import ABC, abstractmethod
from dataclasses import dataclass
from datetime import date


@dataclass
class ParsedTransacao:
    data: date
    descricao: str
    valor: float
    parcela_atual: int | None = None
    parcela_total: int | None = None


@dataclass
class ParsedFatura:
    banco: str
    mes_referencia: int
    ano_referencia: int
    transacoes: list[ParsedTransacao]


class BankParser(ABC):
    banco: str

    @abstractmethod
    def matches(self, texto: str) -> bool:
        """Retorna True se o texto extraido do PDF pertence a este banco."""

    @abstractmethod
    def parse(self, texto: str, pdf_bytes: bytes, senha: str) -> ParsedFatura:
        """
        Extrai mes/ano de referencia e lista de transacoes do texto do PDF.
        pdf_bytes/senha sao fornecidos para parsers que precisam reabrir o PDF
        com uma extracao diferente (ex: layout de duas colunas do Itau).
        """
