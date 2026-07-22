from datetime import date, datetime

from pydantic import BaseModel


class TransacaoOut(BaseModel):
    id: int
    data: date
    descricao: str
    valor: float
    categoria: str
    parcela_atual: int | None = None
    parcela_total: int | None = None

    class Config:
        from_attributes = True


class FaturaOut(BaseModel):
    id: int
    banco: str
    cartao: str = ""
    mes_referencia: int
    ano_referencia: int
    processada_em: datetime
    transacoes: list[TransacaoOut] = []

    class Config:
        from_attributes = True


class ResumoCategoria(BaseModel):
    categoria: str
    total: float


class ResumoMensal(BaseModel):
    mes_referencia: int
    ano_referencia: int
    total_gasto: float
    por_categoria: list[ResumoCategoria]


class ComparacaoMensal(BaseModel):
    meses: list[ResumoMensal]
    variacao_percentual_total: float | None = None


class SenhaPadraoCreate(BaseModel):
    valor: str
    descricao: str | None = None


class SenhaPadraoOut(BaseModel):
    id: int
    valor: str
    descricao: str | None = None

    class Config:
        from_attributes = True
