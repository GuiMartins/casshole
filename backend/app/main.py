import hashlib

from fastapi import Depends, FastAPI, Form, HTTPException, UploadFile
from sqlalchemy.orm import Session

from . import models, schemas
from .categorizer import CATEGORIAS_DISPONIVEIS, categorizar
from .database import Base, engine, get_db
from .parsers import BancoNaoIdentificadoError, SenhaIncorretaError, processar_fatura

Base.metadata.create_all(bind=engine)

app = FastAPI(title="Fatura App API")


@app.post("/faturas/upload", response_model=schemas.FaturaOut)
async def upload_fatura(
    arquivo: UploadFile,
    senha: str | None = Form(None),
    db: Session = Depends(get_db),
):
    if arquivo.content_type != "application/pdf":
        raise HTTPException(400, "Envie um arquivo PDF")

    pdf_bytes = await arquivo.read()
    arquivo_hash = hashlib.sha256(pdf_bytes).hexdigest()

    existente = db.query(models.Fatura).filter_by(arquivo_hash=arquivo_hash).first()
    if existente:
        raise HTTPException(409, "Esta fatura ja foi processada anteriormente")

    senhas_padrao = [s.valor for s in db.query(models.SenhaPadrao).all()]
    senhas_candidatas = ([senha] if senha else []) + senhas_padrao

    try:
        fatura_parseada = processar_fatura(pdf_bytes, senhas_candidatas)
    except BancoNaoIdentificadoError as exc:
        raise HTTPException(422, str(exc))
    except SenhaIncorretaError as exc:
        raise HTTPException(401, str(exc))

    fatura = models.Fatura(
        banco=fatura_parseada.banco,
        cartao=fatura_parseada.cartao,
        mes_referencia=fatura_parseada.mes_referencia,
        ano_referencia=fatura_parseada.ano_referencia,
        arquivo_hash=arquivo_hash,
    )

    ja_existe_periodo = (
        db.query(models.Fatura)
        .filter_by(
            banco=fatura.banco,
            cartao=fatura.cartao,
            mes_referencia=fatura.mes_referencia,
            ano_referencia=fatura.ano_referencia,
        )
        .first()
    )
    if ja_existe_periodo:
        detalhe_cartao = f" (cartão final {fatura.cartao})" if fatura.cartao else ""
        raise HTTPException(
            409,
            f"Ja existe uma fatura de {fatura.banco}{detalhe_cartao} para "
            f"{fatura.mes_referencia}/{fatura.ano_referencia}",
        )

    for transacao in fatura_parseada.transacoes:
        fatura.transacoes.append(
            models.Transacao(
                data=transacao.data,
                descricao=transacao.descricao,
                valor=transacao.valor,
                categoria=categorizar(transacao.descricao),
                parcela_atual=transacao.parcela_atual,
                parcela_total=transacao.parcela_total,
                titular=transacao.titular,
                cidade=transacao.cidade,
                cartao=transacao.cartao,
            )
        )

    db.add(fatura)
    db.commit()
    db.refresh(fatura)
    return fatura


@app.get("/faturas", response_model=list[schemas.FaturaOut])
def listar_faturas(db: Session = Depends(get_db)):
    return (
        db.query(models.Fatura)
        .order_by(models.Fatura.ano_referencia, models.Fatura.mes_referencia)
        .all()
    )


@app.get("/faturas/{fatura_id}", response_model=schemas.FaturaOut)
def obter_fatura(fatura_id: int, db: Session = Depends(get_db)):
    fatura = db.query(models.Fatura).get(fatura_id)
    if not fatura:
        raise HTTPException(404, "Fatura nao encontrada")
    return fatura


def _montar_resumo_mensal(db: Session, mes: int, ano: int) -> schemas.ResumoMensal | None:
    transacoes = (
        db.query(models.Transacao)
        .join(models.Fatura)
        .filter(
            models.Fatura.mes_referencia == mes,
            models.Fatura.ano_referencia == ano,
        )
        .all()
    )
    if not transacoes:
        return None

    por_categoria: dict[str, float] = {}
    total = 0.0
    for t in transacoes:
        por_categoria[t.categoria] = por_categoria.get(t.categoria, 0.0) + t.valor
        total += t.valor

    return schemas.ResumoMensal(
        mes_referencia=mes,
        ano_referencia=ano,
        total_gasto=round(total, 2),
        por_categoria=[
            schemas.ResumoCategoria(categoria=cat, total=round(valor, 2))
            for cat, valor in sorted(por_categoria.items(), key=lambda kv: -kv[1])
        ],
    )


@app.get("/resumo/{ano}/{mes}", response_model=schemas.ResumoMensal)
def resumo_mensal(ano: int, mes: int, db: Session = Depends(get_db)):
    resumo = _montar_resumo_mensal(db, mes, ano)
    if resumo is None:
        raise HTTPException(404, "Nenhuma fatura encontrada para este periodo")
    return resumo


@app.get("/categorias", response_model=list[str])
def listar_categorias():
    return CATEGORIAS_DISPONIVEIS


@app.patch("/transacoes/{transacao_id}", response_model=schemas.TransacaoOut)
def atualizar_transacao(
    transacao_id: int, payload: schemas.TransacaoUpdate, db: Session = Depends(get_db)
):
    transacao = db.query(models.Transacao).get(transacao_id)
    if not transacao:
        raise HTTPException(404, "Transacao nao encontrada")
    transacao.categoria = payload.categoria
    db.commit()
    db.refresh(transacao)
    return transacao


@app.get("/senhas-padrao", response_model=list[schemas.SenhaPadraoOut])
def listar_senhas_padrao(db: Session = Depends(get_db)):
    return db.query(models.SenhaPadrao).all()


@app.post("/senhas-padrao", response_model=schemas.SenhaPadraoOut)
def criar_senha_padrao(payload: schemas.SenhaPadraoCreate, db: Session = Depends(get_db)):
    existente = db.query(models.SenhaPadrao).filter_by(valor=payload.valor).first()
    if existente:
        raise HTTPException(409, "Esta senha ja esta cadastrada")

    senha_padrao = models.SenhaPadrao(valor=payload.valor, descricao=payload.descricao)
    db.add(senha_padrao)
    db.commit()
    db.refresh(senha_padrao)
    return senha_padrao


@app.delete("/senhas-padrao/{senha_id}", status_code=204)
def remover_senha_padrao(senha_id: int, db: Session = Depends(get_db)):
    senha_padrao = db.query(models.SenhaPadrao).get(senha_id)
    if not senha_padrao:
        raise HTTPException(404, "Senha nao encontrada")
    db.delete(senha_padrao)
    db.commit()


@app.get("/comparacao", response_model=schemas.ComparacaoMensal)
def comparar_meses(periodos: str, db: Session = Depends(get_db)):
    """periodos no formato 'MM-AAAA,MM-AAAA,...' ex: '05-2026,06-2026,07-2026'"""
    resumos = []
    for periodo in periodos.split(","):
        mes_str, ano_str = periodo.strip().split("-")
        resumo = _montar_resumo_mensal(db, int(mes_str), int(ano_str))
        if resumo:
            resumos.append(resumo)

    variacao = None
    if len(resumos) >= 2 and resumos[0].total_gasto > 0:
        variacao = round(
            (resumos[-1].total_gasto - resumos[0].total_gasto)
            / resumos[0].total_gasto
            * 100,
            2,
        )

    return schemas.ComparacaoMensal(meses=resumos, variacao_percentual_total=variacao)
