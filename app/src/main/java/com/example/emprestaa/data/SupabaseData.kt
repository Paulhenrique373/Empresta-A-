package com.example.emprestaa.data

import com.example.emprestaa.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object SupabaseProvider {
    val client by lazy {
        require(BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) { "Configure SUPABASE_URL e SUPABASE_ANON_KEY em local.properties." }
        createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY) {
            install(Auth)
            install(Postgrest)
        }
    }
}

@Serializable data class UsuarioRow(@SerialName("id_usuario") val id: String, @SerialName("nome_completo") val nome: String, val ativo: Boolean)
@Serializable data class PerfilRow(@SerialName("nome_perfil") val nome: String)
@Serializable data class UsuarioPerfilRow(@SerialName("id_perfil") val perfilId: Long)
@Serializable data class EquipamentoRow(@SerialName("id_equipamento") val id: Long, val marca: String, val modelo: String, @SerialName("numero_etiqueta") val etiqueta: String, @SerialName("tipo_equipamento") val tipo: String, @SerialName("uso_tecnico") val usoTecnico: Boolean, val status: String, val observacao: String? = null)
@Serializable data class EmprestimoRow(@SerialName("id_emprestimo") val id: Long, @SerialName("id_usuario") val usuarioId: String, @SerialName("id_equipamento") val equipamentoId: Long, @SerialName("data_retirada") val retirada: String, @SerialName("data_devolucao") val devolucao: String? = null, val status: String)
@Serializable data class DefeitoInsert(@SerialName("id_equipamento") val equipamentoId: Long, @SerialName("id_usuario") val usuarioId: String, @SerialName("tipo_defeito") val tipo: String, val descricao: String)
@Serializable data class EquipamentoInsert(val marca: String, val modelo: String, @SerialName("numero_etiqueta") val etiqueta: String, @SerialName("tipo_equipamento") val tipo: String, @SerialName("uso_tecnico") val usoTecnico: Boolean, val status: String = "Disponível", val observacao: String? = null)
@Serializable data class EquipamentoPatch(val marca: String, val modelo: String, @SerialName("tipo_equipamento") val tipo: String, @SerialName("uso_tecnico") val usoTecnico: Boolean, val status: String, val observacao: String? = null)
@Serializable data class DefeitoRow(@SerialName("id_defeito") val id: Long, @SerialName("id_equipamento") val equipamentoId: Long, @SerialName("id_usuario") val usuarioId: String, @SerialName("tipo_defeito") val tipo: String, val descricao: String, @SerialName("data_defeito") val data: String, val status: String)
@Serializable data class UsuarioAdminRow(@SerialName("id_usuario") val id: String, @SerialName("nome_completo") val nome: String, val ativo: Boolean, @SerialName("usuario_perfil") val perfis: List<UsuarioPerfilAdmin> = emptyList())
@Serializable data class UsuarioPerfilAdmin(@SerialName("perfis") val perfil: PerfilRow)

enum class PerfilAcesso(val label: String) { PROFESSOR_FGB("Professor FGB"), PROFESSOR_TECNICO("Professor Técnico"), PAEET("PAEET"), GESTAO("Gestão") }
data class SessaoUsuario(val id: String, val nome: String, val perfis: Set<PerfilAcesso>)

class AuthRepository {
    suspend fun entrar(email: String, senha: String): SessaoUsuario {
        val client = SupabaseProvider.client
        client.auth.signInWith(Email) { this.email = email; password = senha }
        val id = client.auth.currentUserOrNull()?.id ?: error("A sessão não foi criada.")
        val usuario = client.from("usuarios").select { filter { eq("id_usuario", id) } }.decodeList<UsuarioRow>().firstOrNull() ?: error("Usuário sem cadastro escolar.")
        check(usuario.ativo) { "Conta desativada. Procure a gestão escolar." }
        val relacoes = client.from("usuario_perfil").select { filter { eq("id_usuario", id) } }.decodeList<UsuarioPerfilRow>()
        val perfisEncontrados = relacoes.mapNotNull { relacao ->
            client.from("perfis").select { filter { eq("id_perfil", relacao.perfilId) } }
                .decodeList<PerfilRow>().firstOrNull()?.nome
        }
        val perfis = perfisEncontrados.mapNotNull { nome -> PerfilAcesso.entries.firstOrNull { it.label.equals(nome, true) } }.toSet()
        check(perfis.isNotEmpty()) { "Nenhum perfil válido foi atribuído à conta." }
        return SessaoUsuario(id, usuario.nome, perfis)
    }
    suspend fun sair() = SupabaseProvider.client.auth.signOut()
    suspend fun recuperar(email: String) = SupabaseProvider.client.auth.resetPasswordForEmail(email)
}

class EquipamentoRepository {
    suspend fun listar() = SupabaseProvider.client.from("equipamentos").select().decodeList<EquipamentoRow>()
    suspend fun cadastrar(item: EquipamentoInsert) { SupabaseProvider.client.from("equipamentos").insert(item) }
    suspend fun alterar(id: Long, item: EquipamentoPatch) { SupabaseProvider.client.from("equipamentos").update(item) { filter { eq("id_equipamento", id) } } }
}
class EmprestimoRepository {
    suspend fun meusAtivos(usuarioId: String) = SupabaseProvider.client.from("emprestimos").select { filter { eq("id_usuario", usuarioId); eq("status", "Ativo") } }.decodeList<EmprestimoRow>()
    suspend fun historico(usuarioId: String) = SupabaseProvider.client.from("emprestimos").select { filter { eq("id_usuario", usuarioId) } }.decodeList<EmprestimoRow>()
    suspend fun listarGestao() = SupabaseProvider.client.from("emprestimos").select().decodeList<EmprestimoRow>()
    suspend fun retirar(equipamentoId: Long) { SupabaseProvider.client.postgrest.rpc("emprestar_equipamento", kotlinx.serialization.json.buildJsonObject { put("p_id_equipamento", kotlinx.serialization.json.JsonPrimitive(equipamentoId)) }) }
    suspend fun devolver(emprestimoId: Long) { SupabaseProvider.client.postgrest.rpc("devolver_equipamento", kotlinx.serialization.json.buildJsonObject { put("p_id_emprestimo", kotlinx.serialization.json.JsonPrimitive(emprestimoId)) }) }
}
class DefeitoRepository {
    suspend fun registrar(item: DefeitoInsert) { SupabaseProvider.client.from("defeitos").insert(item) }
    suspend fun listar() = SupabaseProvider.client.from("defeitos").select().decodeList<DefeitoRow>()
    suspend fun atualizarStatus(id: Long, status: String) { SupabaseProvider.client.from("defeitos").update(kotlinx.serialization.json.buildJsonObject { put("status", kotlinx.serialization.json.JsonPrimitive(status)) }) { filter { eq("id_defeito", id) } }
}
}
class UsuarioRepository { suspend fun listar() = SupabaseProvider.client.from("usuarios").select(Columns.raw("*,usuario_perfil(perfis(nome_perfil))")).decodeList<UsuarioAdminRow>() }
class AuditoriaRepository { suspend fun listar() = SupabaseProvider.client.from("auditoria").select().decodeList<kotlinx.serialization.json.JsonObject>() }
