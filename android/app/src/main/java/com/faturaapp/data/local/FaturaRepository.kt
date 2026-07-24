package com.faturaapp.data.local

import android.content.Context
import androidx.room.withTransaction
import com.faturaapp.categorizer.categorizar
import com.faturaapp.data.local.entity.FaturaEntity
import com.faturaapp.data.local.entity.SenhaPadraoEntity
import com.faturaapp.data.local.entity.TransacaoEntity
import com.faturaapp.parsing.FaturaDispatcher
import java.security.MessageDigest
import java.time.Instant

class ArquivoDuplicadoException(message: String) : Exception(message)
class PeriodoDuplicadoException(message: String) : Exception(message)

class FaturaRepository(context: Context) {
    private val db = DatabaseProvider.getDatabase(context)
    private val faturaDao = db.faturaDao()
    private val transacaoDao = db.transacaoDao()
    private val senhaPadraoDao = db.senhaPadraoDao()

    suspend fun temSenhasCadastradas(): Boolean = senhaPadraoDao.listar().isNotEmpty()

    suspend fun listarFaturas(): List<FaturaComTransacoes> = faturaDao.listarComTransacoes()

    suspend fun obterFatura(id: Long): FaturaComTransacoes? = faturaDao.obterComTransacoes(id)

    suspend fun atualizarCategoria(transacaoId: Long, categoria: String) {
        transacaoDao.atualizarCategoria(transacaoId, categoria)
    }

    suspend fun listarSenhasPadrao(): List<SenhaPadraoEntity> = senhaPadraoDao.listar()

    suspend fun adicionarSenhaPadrao(valor: String): SenhaPadraoEntity {
        val id = senhaPadraoDao.inserir(SenhaPadraoEntity(valor = valor))
        return SenhaPadraoEntity(id = id, valor = valor)
    }

    suspend fun removerSenhaPadrao(id: Long) {
        senhaPadraoDao.remover(id)
    }

    suspend fun transacoesPorPeriodo(mes: Int, ano: Int): List<TransacaoEntity> =
        transacaoDao.listarPorPeriodo(mes, ano)

    suspend fun processarEArmazenar(bytes: ByteArray, senhaDigitada: String?): FaturaComTransacoes {
        val hash = calcularHash(bytes)
        if (faturaDao.buscarPorHash(hash) != null) {
            throw ArquivoDuplicadoException("Esta fatura já foi processada anteriormente")
        }

        val senhasPadrao = senhaPadraoDao.listar().map { it.valor }
        val senhaDigitadaLimpa = senhaDigitada?.trim()?.ifBlank { null }
        val candidatos = listOfNotNull(senhaDigitadaLimpa) + senhasPadrao
        val faturaParseada = FaturaDispatcher.processarFatura(bytes, candidatos)

        val existente = faturaDao.buscarPorPeriodo(
            faturaParseada.banco,
            faturaParseada.cartao,
            faturaParseada.mesReferencia,
            faturaParseada.anoReferencia,
        )
        if (existente != null) {
            val detalheCartao = if (faturaParseada.cartao.isNotBlank()) {
                " (cartão final ${faturaParseada.cartao})"
            } else {
                ""
            }
            throw PeriodoDuplicadoException(
                "Já existe uma fatura de ${faturaParseada.banco}$detalheCartao para " +
                    "${faturaParseada.mesReferencia}/${faturaParseada.anoReferencia}"
            )
        }

        val faturaId = db.withTransaction {
            val id = faturaDao.inserir(
                FaturaEntity(
                    banco = faturaParseada.banco,
                    cartao = faturaParseada.cartao,
                    mesReferencia = faturaParseada.mesReferencia,
                    anoReferencia = faturaParseada.anoReferencia,
                    arquivoHash = hash,
                    processadaEm = Instant.now().toString(),
                )
            )
            transacaoDao.inserirTodas(
                faturaParseada.transacoes.map { transacao ->
                    TransacaoEntity(
                        faturaId = id,
                        data = transacao.data,
                        descricao = transacao.descricao,
                        valor = transacao.valor,
                        categoria = categorizar(transacao.descricao),
                        parcelaAtual = transacao.parcelaAtual,
                        parcelaTotal = transacao.parcelaTotal,
                        titular = transacao.titular,
                        cidade = transacao.cidade,
                        cartao = transacao.cartao,
                    )
                }
            )
            id
        }

        return faturaDao.obterComTransacoes(faturaId)!!
    }

    private fun calcularHash(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
