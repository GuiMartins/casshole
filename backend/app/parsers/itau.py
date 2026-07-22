import re
from datetime import date

from .base import BankParser, ParsedFatura, ParsedTransacao
from .utils import extrair_texto_pdf_por_colunas, parse_valor_br

VENCIMENTO_RE = re.compile(r"Vencimento:\s*(\d{2})/(\d{2})/(\d{4})", re.IGNORECASE)
CARTAO_RE = re.compile(r"Cart[aã]o\s+\d{4}\.XXXX\.XXXX\.(\d{4})", re.IGNORECASE)

# Ex: "30/12 SHOPEE *LOJAPI 07/07 65,58"
# Ex: "07/06 IFD*SORVETERIA EXEMPLO 51,88" (sem parcela)
# Ex: "16/06 PAGAMENTO -1.100,00" (pagamento, sinal negativo, deve ser ignorado)
LINHA_TRANSACAO_RE = re.compile(
    r"^(?P<dia>\d{2})/(?P<mes>\d{2})\s+"
    r"(?P<descricao>.+?)"
    r"(?:\s+(?P<parcela_atual>\d{2})/(?P<parcela_total>\d{2}))?"
    r"\s+(?P<sinal>[-−])?(?:R\$\s*)?(?P<valor>\d{1,3}(?:\.\d{3})*,\d{2})\s*$"
)


class ItauParser(BankParser):
    banco = "itau"

    def matches(self, texto: str) -> bool:
        texto_lower = texto.lower()
        return "itau" in texto_lower or "itaú" in texto_lower

    def parse(self, texto: str, pdf_bytes: bytes = b"", senha: str = "") -> ParsedFatura:
        mes, ano = self._extrair_mes_ano_referencia(texto)
        cartao = self._extrair_cartao(texto)
        # Corte tunado empiricamente: a coluna esquerda (data/estabelecimento/valor)
        # desta fatura vai ate ~x=330 e a coluna direita comeca em ~x=351 (pagina
        # de ~595pt de largura), entao 0.57 fica bem no meio desse intervalo.
        texto_colunas = extrair_texto_pdf_por_colunas(pdf_bytes, senha, fracao_corte=0.57) if pdf_bytes else texto
        titular = self._extrair_titular(texto_colunas)
        transacoes = self._extrair_transacoes(texto_colunas, mes, ano, titular)
        return ParsedFatura(
            banco=self.banco,
            cartao=cartao,
            mes_referencia=mes,
            ano_referencia=ano,
            transacoes=transacoes,
        )

    def _extrair_mes_ano_referencia(self, texto: str) -> tuple[int, int]:
        match = VENCIMENTO_RE.search(texto)
        if not match:
            raise ValueError(
                "Não foi possível identificar o mês/ano de referência da fatura Itaú"
            )
        return int(match.group(2)), int(match.group(3))

    def _extrair_cartao(self, texto: str) -> str:
        match = CARTAO_RE.search(texto)
        return match.group(1) if match else ""

    def _extrair_titular(self, texto: str) -> str:
        """O nome do titular aparece uma vez, logo apos o cabecalho da secao
        de lancamentos (ex: "Lançamentos: compras e saques\nFULANO DE TAL")."""
        match_header = re.search(r"Lançamentos:\s*compras e saques", texto, re.IGNORECASE)
        if not match_header:
            return ""
        for linha in texto[match_header.end():].splitlines():
            linha_limpa = linha.strip()
            if linha_limpa and re.fullmatch(r"[A-ZÀ-Ú][A-ZÀ-Ú ]+", linha_limpa):
                return linha_limpa.title()
        return ""

    def _extrair_secao_lancamentos_atuais(self, texto: str) -> str:
        """
        A fatura lista, alem dos lancamentos do periodo atual, uma secao de
        "Compras parceladas - proximas faturas" com parcelas futuras que NAO
        fazem parte do total desta fatura. Por isso recortamos o texto entre
        o inicio dos lancamentos e o total, ignorando o que vem depois.
        """
        inicio = re.search(r"Lançamentos:\s*compras e saques", texto, re.IGNORECASE)
        if not inicio:
            return texto
        fim = re.search(r"Total dos lançamentos atuais", texto, re.IGNORECASE)
        fim_pos = fim.start() if fim else len(texto)
        return texto[inicio.start():fim_pos]

    def _extrair_cidade(self, proxima_linha: str) -> str:
        """
        A linha seguinte a uma transacao costuma trazer uma tag de categoria
        do proprio Itau e a cidade da compra, ex: "restaurante TERESOPOLIS".
        Se a primeira palavra for minuscula, e a tag de categoria; o resto e
        a cidade. Caso contrario (ex: nome de estabelecimento que quebrou de
        linha), guardamos a linha inteira como melhor esforco.
        """
        linha_limpa = proxima_linha.strip()
        if not linha_limpa or LINHA_TRANSACAO_RE.match(linha_limpa):
            return ""
        palavras = linha_limpa.split(maxsplit=1)
        if len(palavras) == 2 and palavras[0].islower():
            return palavras[1].strip()
        return linha_limpa

    def _extrair_transacoes(
        self, texto: str, mes_referencia: int, ano_referencia: int, titular: str
    ) -> list[ParsedTransacao]:
        secao = self._extrair_secao_lancamentos_atuais(texto)
        linhas = secao.splitlines()
        transacoes = []

        for indice, linha in enumerate(linhas):
            match = LINHA_TRANSACAO_RE.match(linha.strip())
            if not match:
                continue

            grupos = match.groupdict()
            if grupos["sinal"]:
                continue

            mes_transacao = int(grupos["mes"])
            ano_transacao = ano_referencia
            if mes_transacao > mes_referencia:
                # Compras parceladas mostram a data da compra original, que
                # pode ser de meses ou anos atras (nao a data de cobranca).
                ano_transacao -= 1

            cidade = self._extrair_cidade(linhas[indice + 1]) if indice + 1 < len(linhas) else ""

            transacoes.append(
                ParsedTransacao(
                    data=date(ano_transacao, mes_transacao, int(grupos["dia"])),
                    descricao=grupos["descricao"].strip(),
                    valor=parse_valor_br(grupos["valor"]),
                    parcela_atual=int(grupos["parcela_atual"]) if grupos["parcela_atual"] else None,
                    parcela_total=int(grupos["parcela_total"]) if grupos["parcela_total"] else None,
                    titular=titular,
                    cidade=cidade,
                )
            )
        return transacoes
