import io
import re
from datetime import date

import pdfplumber

MESES = {
    "jan": 1, "fev": 2, "mar": 3, "abr": 4, "mai": 5, "jun": 6,
    "jul": 7, "ago": 8, "set": 9, "out": 10, "nov": 11, "dez": 12,
}

# Casa linhas tipo: "12/07  UBER *TRIP  1x  25,90" ou "12/07 IFOOD 45,00"
LINHA_TRANSACAO_RE = re.compile(
    r"(?P<dia>\d{2})/(?P<mes>\d{2})\s+"
    r"(?P<descricao>.+?)\s+"
    r"(?:(?P<parcela_atual>\d{1,2})/(?P<parcela_total>\d{1,2})\s+)?"
    r"(?P<valor>-?\d{1,3}(?:\.\d{3})*,\d{2})\s*$"
)


def parse_valor_br(valor_str: str) -> float:
    return float(valor_str.replace(".", "").replace(",", "."))


def extrair_texto_pdf_por_colunas(pdf_bytes: bytes, senha: str, fracao_corte: float = 0.5) -> str:
    """
    Algumas faturas (ex: Itau) usam layout de duas colunas lado a lado. O
    extract_text padrao do pdfplumber intercala as colunas linha a linha e
    embaralha a ordem do texto (ex: junta uma transacao da coluna esquerda
    com uma linha de resumo da coluna direita na mesma linha de texto).
    Por isso cortamos cada pagina no ponto de corte informado e extraimos
    cada coluna separadamente. fracao_corte e a posicao do corte como
    fracao da largura da pagina (0.5 = meio exato).
    """
    with pdfplumber.open(io.BytesIO(pdf_bytes), password=senha) as pdf:
        partes = []
        for page in pdf.pages:
            corte = page.width * fracao_corte
            coluna_esquerda = page.within_bbox((0, 0, corte, page.height))
            coluna_direita = page.within_bbox((corte, 0, page.width, page.height))
            partes.append((coluna_esquerda.extract_text() or "") + "\n" + (coluna_direita.extract_text() or ""))
        return "\n".join(partes)


def extrair_transacoes_generico(texto: str, ano_referencia: int):
    from .base import ParsedTransacao

    transacoes = []
    for linha in texto.splitlines():
        match = LINHA_TRANSACAO_RE.search(linha.strip())
        if not match:
            continue
        grupos = match.groupdict()
        dia, mes = int(grupos["dia"]), int(grupos["mes"])
        try:
            data_transacao = date(ano_referencia, mes, dia)
        except ValueError:
            continue
        transacoes.append(
            ParsedTransacao(
                data=data_transacao,
                descricao=grupos["descricao"].strip(),
                valor=parse_valor_br(grupos["valor"]),
                parcela_atual=int(grupos["parcela_atual"]) if grupos["parcela_atual"] else None,
                parcela_total=int(grupos["parcela_total"]) if grupos["parcela_total"] else None,
            )
        )
    return transacoes


def extrair_mes_ano_referencia(texto: str) -> tuple[int, int]:
    """Procura padrao 'MES/ANO' (ex: JULHO/2026) ou 'MM/AAAA' no cabecalho da fatura."""
    match = re.search(
        r"(jan|fev|mar|abr|mai|jun|jul|ago|set|out|nov|dez)\w*[/\s](\d{4})",
        texto.lower(),
    )
    if match:
        return MESES[match.group(1)], int(match.group(2))

    match = re.search(r"(\d{2})/(\d{4})", texto)
    if match:
        return int(match.group(1)), int(match.group(2))

    raise ValueError("Nao foi possivel identificar o mes/ano de referencia da fatura")
