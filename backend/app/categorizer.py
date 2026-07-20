import re

# Ordem importa: regras mais especificas primeiro.
REGRAS_CATEGORIA: list[tuple[str, re.Pattern]] = [
    ("Alimentação", re.compile(
        r"ifd\*|ifood|rappi|restaurante|pizzaria|padaria|lanchonete|"
        r"mercado|supermercado|redeconomia|abastecedora|hortifruti|"
        r"acougue|padok|empada|sushi|burger|fast food",
        re.I,
    )),
    ("Transporte", re.compile(r"uber|99app|99pop|taxi|posto|combustivel|estacionamento|propark", re.I)),
    ("Streaming/Assinaturas", re.compile(r"netflix|spotify|amazon prime|disney|hbo|youtube premium|icloud|universal music", re.I)),
    ("Saúde", re.compile(r"farmacia|drogaria|clinica|hospital|laboratorio", re.I)),
    ("Compras", re.compile(
        r"amazon|magalu|mercado\s?livre|shopee|shein|americanas|"
        r"aliexpress|kabum|kabum!",
        re.I,
    )),
    ("Educação", re.compile(r"udemy|alura|coursera|faculdade|escola", re.I)),
    ("Contas/Serviços", re.compile(r"energia|luz|agua|telefone|internet|claro|vivo|tim|oi\b", re.I)),
]

CATEGORIA_PADRAO = "Outros"


def categorizar(descricao: str) -> str:
    for categoria, padrao in REGRAS_CATEGORIA:
        if padrao.search(descricao):
            return categoria
    return CATEGORIA_PADRAO
