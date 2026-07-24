package com.faturaapp.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.faturaapp.data.local.entity.FaturaEntity
import com.faturaapp.data.local.entity.SenhaPadraoEntity
import com.faturaapp.data.local.entity.TransacaoEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomFoundationTest {

    private lateinit var db: AppDatabase

    @Before
    fun criarBanco() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun fecharBanco() {
        db.close()
    }

    @Test
    fun inserirFaturaComTransacoes_apareceNaConsulta() = runBlocking {
        val faturaId = db.faturaDao().inserir(
            FaturaEntity(
                banco = "itau",
                cartao = "2222",
                mesReferencia = 7,
                anoReferencia = 2026,
                arquivoHash = "hash-1",
                processadaEm = "2026-07-24T00:00:00",
            )
        )
        db.transacaoDao().inserirTodas(
            listOf(
                TransacaoEntity(faturaId = faturaId, data = "2026-06-07", descricao = "IFOOD", valor = 51.88),
                TransacaoEntity(faturaId = faturaId, data = "2026-06-08", descricao = "WELLHUB", valor = 69.99),
            )
        )

        val resultado = db.faturaDao().obterComTransacoes(faturaId)
        assertEquals(2, resultado?.transacoes?.size)
        assertEquals(121.87, resultado!!.transacoes.sumOf { it.valor }, 0.001)
    }

    @Test
    fun deletarFatura_apagaTransacoesEmCascata() = runBlocking {
        val fatura = FaturaEntity(
            banco = "nubank",
            mesReferencia = 7,
            anoReferencia = 2026,
            arquivoHash = "hash-cascade",
            processadaEm = "2026-07-24T00:00:00",
        )
        val faturaId = db.faturaDao().inserir(fatura)
        db.transacaoDao().inserirTodas(
            listOf(TransacaoEntity(faturaId = faturaId, data = "2026-06-01", descricao = "X", valor = 10.0))
        )

        db.faturaDao().deletar(fatura.copy(id = faturaId))

        assertNull(db.faturaDao().obterComTransacoes(faturaId))
        assertTrue(db.transacaoDao().listarPorPeriodo(7, 2026).isEmpty())
    }

    @Test(expected = SQLiteConstraintException::class)
    fun arquivoHashDuplicado_lancaExcecao(): Unit = runBlocking {
        val base = FaturaEntity(
            banco = "itau",
            mesReferencia = 7,
            anoReferencia = 2026,
            arquivoHash = "hash-repetido",
            processadaEm = "2026-07-24T00:00:00",
        )
        db.faturaDao().inserir(base)
        db.faturaDao().inserir(base.copy(cartao = "outro-cartao"))
        Unit
    }

    @Test(expected = SQLiteConstraintException::class)
    fun periodoDuplicadoMesmoBancoECartao_lancaExcecao(): Unit = runBlocking {
        db.faturaDao().inserir(
            FaturaEntity(
                banco = "mercadopago", cartao = "1111", mesReferencia = 7, anoReferencia = 2026,
                arquivoHash = "hash-a", processadaEm = "2026-07-24T00:00:00",
            )
        )
        db.faturaDao().inserir(
            FaturaEntity(
                banco = "mercadopago", cartao = "1111", mesReferencia = 7, anoReferencia = 2026,
                arquivoHash = "hash-b", processadaEm = "2026-07-24T00:00:00",
            )
        )
        Unit
    }

    @Test(expected = SQLiteConstraintException::class)
    fun senhaPadraoDuplicada_lancaExcecao(): Unit = runBlocking {
        db.senhaPadraoDao().inserir(SenhaPadraoEntity(valor = "12345"))
        db.senhaPadraoDao().inserir(SenhaPadraoEntity(valor = "12345"))
        Unit
    }
}
