from datetime import datetime

from sqlalchemy import (
    Column,
    Date,
    DateTime,
    Float,
    ForeignKey,
    Integer,
    String,
    UniqueConstraint,
)
from sqlalchemy.orm import relationship

from .database import Base


class Fatura(Base):
    __tablename__ = "faturas"

    id = Column(Integer, primary_key=True)
    banco = Column(String, nullable=False)
    cartao = Column(String, nullable=False, default="")
    mes_referencia = Column(Integer, nullable=False)
    ano_referencia = Column(Integer, nullable=False)
    arquivo_hash = Column(String, nullable=False, unique=True)
    processada_em = Column(DateTime, default=datetime.utcnow)

    transacoes = relationship(
        "Transacao", back_populates="fatura", cascade="all, delete-orphan"
    )

    __table_args__ = (
        UniqueConstraint(
            "banco", "cartao", "mes_referencia", "ano_referencia", name="uq_fatura_periodo"
        ),
    )


class Transacao(Base):
    __tablename__ = "transacoes"

    id = Column(Integer, primary_key=True)
    fatura_id = Column(Integer, ForeignKey("faturas.id"), nullable=False)
    data = Column(Date, nullable=False)
    descricao = Column(String, nullable=False)
    valor = Column(Float, nullable=False)
    categoria = Column(String, nullable=False, default="Outros")
    parcela_atual = Column(Integer, nullable=True)
    parcela_total = Column(Integer, nullable=True)

    fatura = relationship("Fatura", back_populates="transacoes")


class SenhaPadrao(Base):
    __tablename__ = "senhas_padrao"

    id = Column(Integer, primary_key=True)
    valor = Column(String, nullable=False, unique=True)
    descricao = Column(String, nullable=True)
