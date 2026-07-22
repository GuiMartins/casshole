import io

import pdfplumber
from pdfminer.pdfdocument import PDFEncryptionError, PDFPasswordIncorrect

from .base import ParsedFatura
from .bradesco import BradescoParser
from .itau import ItauParser
from .mercadopago import MercadoPagoParser
from .nubank import NubankParser

PARSERS = [NubankParser(), ItauParser(), BradescoParser(), MercadoPagoParser()]


class BancoNaoIdentificadoError(Exception):
    pass


class SenhaIncorretaError(Exception):
    pass


def _encontrar_senha_correta(pdf_bytes: bytes, senhas_candidatas: list[str]) -> str:
    """Retorna a primeira senha (dentre '' + candidatas) que abre o PDF."""
    candidatos = list(dict.fromkeys([""] + senhas_candidatas))
    for senha in candidatos:
        try:
            with pdfplumber.open(io.BytesIO(pdf_bytes), password=senha):
                return senha
        except (PDFPasswordIncorrect, PDFEncryptionError):
            continue

    raise SenhaIncorretaError(
        "Esta fatura esta protegida por senha e nenhuma das senhas informadas "
        "ou cadastradas como padrao conseguiu abri-la."
    )


def extrair_texto_pdf(pdf_bytes: bytes, senhas_candidatas: list[str]) -> str:
    senha = _encontrar_senha_correta(pdf_bytes, senhas_candidatas)
    with pdfplumber.open(io.BytesIO(pdf_bytes), password=senha) as pdf:
        return "\n".join(page.extract_text() or "" for page in pdf.pages)


def processar_fatura(pdf_bytes: bytes, senhas_candidatas: list[str] | None = None) -> ParsedFatura:
    senha = _encontrar_senha_correta(pdf_bytes, senhas_candidatas or [])
    with pdfplumber.open(io.BytesIO(pdf_bytes), password=senha) as pdf:
        texto = "\n".join(page.extract_text() or "" for page in pdf.pages)

    for parser in PARSERS:
        if parser.matches(texto):
            return parser.parse(texto, pdf_bytes=pdf_bytes, senha=senha)

    raise BancoNaoIdentificadoError(
        "Nao foi possivel identificar o banco desta fatura. "
        "Bancos suportados: Nubank, Itau, Bradesco, Mercado Pago."
    )
