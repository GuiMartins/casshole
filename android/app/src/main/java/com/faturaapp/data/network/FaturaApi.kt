package com.faturaapp.data.network

import com.faturaapp.data.model.ComparacaoMensal
import com.faturaapp.data.model.Fatura
import com.faturaapp.data.model.ResumoMensal
import com.faturaapp.data.model.SenhaPadrao
import com.faturaapp.data.model.SenhaPadraoCreate
import com.faturaapp.data.model.Transacao
import com.faturaapp.data.model.TransacaoUpdate
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface FaturaApi {

    @GET("faturas")
    suspend fun listarFaturas(): List<Fatura>

    @GET("faturas/{id}")
    suspend fun obterFatura(@Path("id") id: Int): Fatura

    @Multipart
    @POST("faturas/upload")
    suspend fun uploadFatura(
        @Part arquivo: MultipartBody.Part,
        @Part("senha") senha: RequestBody?,
    ): Fatura

    @GET("resumo/{ano}/{mes}")
    suspend fun resumoMensal(@Path("ano") ano: Int, @Path("mes") mes: Int): ResumoMensal

    @GET("comparacao")
    suspend fun compararMeses(@Query("periodos") periodos: String): ComparacaoMensal

    @GET("senhas-padrao")
    suspend fun listarSenhasPadrao(): List<SenhaPadrao>

    @POST("senhas-padrao")
    suspend fun criarSenhaPadrao(@Body senha: SenhaPadraoCreate): SenhaPadrao

    @DELETE("senhas-padrao/{id}")
    suspend fun removerSenhaPadrao(@Path("id") id: Int)

    @GET("categorias")
    suspend fun listarCategorias(): List<String>

    @PATCH("transacoes/{id}")
    suspend fun atualizarTransacao(@Path("id") id: Int, @Body payload: TransacaoUpdate): Transacao
}
