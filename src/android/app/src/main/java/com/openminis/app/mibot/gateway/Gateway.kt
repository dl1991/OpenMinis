package com.openminis.app.mibot.gateway

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.openminis.app.MinisApp
import com.openminis.app.data.model.ProviderInstance
import com.openminis.app.data.model.ProviderType
import com.openminis.app.mibot.agent.MibotTools
import com.openminis.app.mibot.design.ChipKind
import com.openminis.app.mibot.design.MibotButton
import com.openminis.app.mibot.design.MibotCard
import com.openminis.app.mibot.design.MibotFootnote
import com.openminis.app.mibot.design.MibotRow
import com.openminis.app.mibot.design.MibotScaffold
import com.openminis.app.mibot.design.MibotSectionHeader
import com.openminis.app.mibot.design.MibotTokens
import com.openminis.app.mibot.design.StatusChip
import com.openminis.app.mibot.design.t
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * [fork/mibot-v2] AI gateway health, layered on the existing model groups
 * (which already do fallback / load-balance routing). Each provider instance
 * is one account in a pool; a failure puts it on an exponential cooldown
 * (30 s → 10 min) and a 401/403 health check marks it dead until the user
 * signs in again. ProviderRepository.availableMemberEntries skips cooling /
 * dead accounts while any healthy member is left, so routing fails over
 * across accounts and providers automatically.
 */
object GatewayHealth {
    data class State(
        val failures: Int = 0,
        val consecutive: Int = 0,
        val cooldownUntil: Long = 0L,
        val dead: Boolean = false,
        val lastError: String? = null,
        val lastCheckAt: Long = 0L,
        val lastCheck: String? = null,
        val okCount: Int = 0,
    )

    private val map = ConcurrentHashMap<String, State>()
    val version = MutableStateFlow(0L)

    fun state(id: String): State = map[id] ?: State()

    fun isUsable(id: String): Boolean {
        val s = map[id] ?: return true
        return !s.dead && System.currentTimeMillis() >= s.cooldownUntil
    }

    @Suppress("UNUSED_PARAMETER")
    fun recordFailure(ctx: Context?, id: String, reason: String) {
        val s = state(id)
        val n = s.consecutive + 1
        val cool = (30_000L shl (n - 1).coerceAtMost(5)).coerceAtMost(600_000L)
        map[id] = s.copy(failures = s.failures + 1, consecutive = n, cooldownUntil = System.currentTimeMillis() + cool, lastError = reason)
        version.value++
    }

    fun recordSuccess(id: String) {
        val s = state(id)
        map[id] = s.copy(consecutive = 0, cooldownUntil = 0L, dead = false, okCount = s.okCount + 1)
        version.value++
    }

    fun recordCheck(id: String, result: String) {
        val code = Regex("HTTP (\\d+)").find(result)?.groupValues?.get(1)?.toIntOrNull()
        val s = state(id)
        map[id] = when {
            code != null && code in 200..299 -> s.copy(consecutive = 0, cooldownUntil = 0L, dead = false, lastCheckAt = System.currentTimeMillis(), lastCheck = result)
            code == 401 || code == 403 -> s.copy(dead = true, lastCheckAt = System.currentTimeMillis(), lastCheck = result, lastError = result)
            code == 429 -> s.copy(cooldownUntil = System.currentTimeMillis() + 120_000L, lastCheckAt = System.currentTimeMillis(), lastCheck = result, lastError = result)
            else -> s.copy(lastCheckAt = System.currentTimeMillis(), lastCheck = result, lastError = result)
        }
        version.value++
    }

    fun reset(id: String) { map.remove(id); version.value++ }
}

object GatewayRoutes { const val GATEWAY = "mibot_gateway" }

private fun poolKey(i: ProviderInstance): String {
    val host = i.effectiveBaseURL?.let { runCatching { java.net.URI(it).host }.getOrNull() } ?: ""
    return if (host.isNotEmpty()) host else i.providerType.displayName
}

@Composable
fun GatewayScreen(onBack: () -> Unit, onProvider: (String) -> Unit, onModelGroups: () -> Unit, onUsage: () -> Unit, onAddProvider: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { (ctx.applicationContext as MinisApp).providerRepository }
    val config by repo.config.collectAsState()
    @Suppress("UNUSED_VARIABLE") val v by GatewayHealth.version.collectAsState()
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1_000); now = System.currentTimeMillis() } }

    MibotScaffold(title = t("AI 网关", "AI gateway"), onBack = onBack) {
        MibotFootnote(t(
            "每个服务商实例就是池中的一个账号。失败的账号自动冷却（30 秒起，指数退避至 10 分钟），401/403 标记失效并提示重新登录；模型组按“回退/负载均衡”在可用账号和服务商之间自动切换。",
            "Each provider instance is one account in a pool. Failing accounts cool down (30 s, backing off to 10 min); 401/403 marks them dead and asks you to sign in again; model groups fail over / load-balance across the healthy accounts and providers.",
        ))
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MibotButton(if (checking) t("体检中…", "Checking…") else t("全部体检", "Health-check all"), enabled = !checking, modifier = Modifier.weight(1f), onClick = {
                checking = true
                scope.launch {
                    for (inst in config.instances.filter { it.isEnabled }) {
                        val key = repo.usableApiKey(inst)
                        val gemini = inst.providerType == ProviderType.gemini
                        val base = inst.effectiveBaseURL ?: defaultBase(inst.providerType)
                        if (base == null && !gemini) { GatewayHealth.recordCheck(inst.id, "no base URL (OAuth/native provider)"); continue }
                        val r = runCatching { MibotTools.probe(base ?: "", key, gemini) }.getOrElse { "error: ${it.message}" }
                        GatewayHealth.recordCheck(inst.id, com.openminis.app.mibot.vault.VaultBroker.scrub(r, listOfNotNull(key)))
                    }
                    checking = false
                }
            })
            MibotButton(t("添加账号", "Add account"), tonal = true, modifier = Modifier.weight(1f), onClick = onAddProvider)
        }

        val pools = config.instances.groupBy { poolKey(it) }
        if (pools.isEmpty()) {
            MibotFootnote(t("还没有服务商。先添加一个（免费可用的也行）。", "No providers yet — add one (free ones work)."))
        }
        pools.forEach { (pool, list) ->
            MibotSectionHeader("$pool · ${list.size}")
            MibotCard {
                list.forEachIndexed { i, inst ->
                    val s = GatewayHealth.state(inst.id)
                    val (label, kind) = when {
                        !inst.isEnabled -> t("已停用", "Off") to ChipKind.NEUTRAL
                        s.dead -> t("失效", "Dead") to ChipKind.ERR
                        s.cooldownUntil > now -> (t("冷却中 ", "Cooling ") + "${(s.cooldownUntil - now) / 1000}s") to ChipKind.WARN
                        s.lastCheckAt > 0 || s.okCount > 0 -> t("可用", "OK") to ChipKind.OK
                        else -> t("未检测", "Unchecked") to ChipKind.NEUTRAL
                    }
                    val models = config.modelEntries.count { it.providerInstanceId == inst.id && !it.isHidden }
                    val sub = buildString {
                        append(t("模型 ", "models ")).append(models)
                        append(t(" · 失败 ", " · failures ")).append(s.failures)
                        s.lastCheck?.let { append("\n").append(it.take(90)) }
                        if (s.dead) append(t("\n需要重新登录或更换 Key", "\nSign in again or replace the key"))
                    }
                    MibotRow(
                        title = inst.label,
                        subtitle = sub,
                        icon = Icons.Outlined.Hub,
                        color = MibotTokens.Brick,
                        onClick = { onProvider(inst.id) },
                        showDivider = i < list.lastIndex,
                        trailing = { StatusChip(label, kind) },
                    )
                }
            }
        }

        MibotSectionHeader(t("路由与用量", "Routing & usage"))
        MibotCard {
            MibotRow(t("模型组", "Model groups"), t("别名：一个逻辑模型 → 多个账号/服务商；回退或负载均衡", "Aliases: one logical model → many accounts/providers; fallback or load-balance"),
                Icons.Outlined.AccountTree, MibotTokens.Slate, onClick = onModelGroups, showDivider = true)
            MibotRow(t("Token 用量", "Token usage"), t("按服务商统计请求与 Token", "Requests and tokens per provider"),
                Icons.Outlined.BarChart, MibotTokens.Green, onClick = onUsage, showDivider = true)
            MibotRow(t("本地 OpenAI 兼容端点", "Local OpenAI-compatible endpoint"), "127.0.0.1 · " + t("即将支持", "coming soon"),
                Icons.Outlined.Dns, MibotTokens.Steel, chevron = false, trailing = { StatusChip(t("即将支持", "Soon"), ChipKind.NEUTRAL) })
        }
    }
}

private fun defaultBase(type: ProviderType): String? = when (type) {
    ProviderType.openAI -> "https://api.openai.com/v1"
    ProviderType.openRouter -> "https://openrouter.ai/api/v1"
    ProviderType.xAI -> "https://api.x.ai/v1"
    else -> null
}
