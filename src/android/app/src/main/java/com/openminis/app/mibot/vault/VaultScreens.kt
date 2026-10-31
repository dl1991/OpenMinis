package com.openminis.app.mibot.vault

import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openminis.app.mibot.design.ChipKind
import com.openminis.app.mibot.design.MibotButton
import com.openminis.app.mibot.design.MibotCard
import com.openminis.app.mibot.design.MibotFilterChips
import com.openminis.app.mibot.design.MibotFootnote
import com.openminis.app.mibot.design.MibotRow
import com.openminis.app.mibot.design.MibotScaffold
import com.openminis.app.mibot.design.MibotSectionHeader
import com.openminis.app.mibot.design.MibotSpacer
import com.openminis.app.mibot.design.MibotTokens
import com.openminis.app.mibot.design.StatusChip
import com.openminis.app.mibot.design.t
import com.openminis.app.mibot.design.tc
import kotlinx.coroutines.launch

object VaultRoutes {
    const val LIST = "mibot_vault"
    const val EDIT = "mibot_vault_edit/{id}"
    const val AUDIT = "mibot_vault_audit"
    fun edit(id: String) = "mibot_vault_edit/$id"
}

private fun categoryIcon(cat: String): ImageVector = when (cat) {
    "AI 服务" -> Icons.Outlined.SmartToy
    "邮箱" -> Icons.Outlined.Email
    "云服务器" -> Icons.Outlined.Cloud
    "社交" -> Icons.Outlined.People
    else -> Icons.Outlined.Key
}

private fun categoryColor(cat: String) = when (cat) {
    "AI 服务" -> MibotTokens.Brick
    "邮箱" -> MibotTokens.Red
    "云服务器" -> MibotTokens.Green
    "社交" -> MibotTokens.Purple
    else -> MibotTokens.Steel
}

@Composable
private fun permLabel(p: VaultPermission): Pair<String, ChipKind> = when (p) {
    VaultPermission.AUTO -> t("自动使用", "Auto") to ChipKind.OK
    VaultPermission.ASK -> t("每次询问", "Ask") to ChipKind.WARN
    VaultPermission.DISABLED -> t("禁用", "Off") to ChipKind.NEUTRAL
}

@Composable
private fun statusLabels(): Map<String, String> = mapOf(
    "balance" to t("余额", "Balance"),
    "points" to t("积分", "Points"),
    "credits" to t("额度", "Credits"),
    "quota" to t("剩余", "Quota"),
    "expiry" to t("到期", "Expires"),
)

private fun ago(ts: Long): String =
    if (ts <= 0) "" else DateUtils.getRelativeTimeSpanString(ts, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()

@Composable
fun VaultListScreen(onBack: () -> Unit, onOpen: (String) -> Unit, onNew: () -> Unit, onAudit: () -> Unit) {
    val ctx = LocalContext.current
    val entries by remember { VaultStore.entries(ctx) }.collectAsState()
    val scope = rememberCoroutineScope()
    val all = t("全部", "All")
    var filter by remember { mutableStateOf("") }
    val sel = filter.ifEmpty { all }
    var busy by remember { mutableStateOf(false) }
    val categories = (VaultCategories.defaults + entries.map { it.category }).distinct()
    val shown = if (sel == all) entries else entries.filter { it.category == sel }
    val labels = statusLabels()

    MibotScaffold(
        title = t("账号台账", "Account Vault"),
        onBack = onBack,
        actions = {
            IconButton(onClick = onAudit) { Icon(Icons.Outlined.History, t("使用记录", "Audit log"), tint = MaterialTheme.colorScheme.onBackground) }
            IconButton(onClick = onNew) { Icon(Icons.Outlined.Add, t("添加账号", "Add account"), tint = MaterialTheme.colorScheme.onBackground) }
        },
    ) {
        MibotFilterChips(listOf(all) + categories, sel) { filter = it }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MibotButton(t("全部签到", "Check in all"), enabled = !busy && shown.isNotEmpty(), modifier = Modifier.weight(1f), onClick = {
                busy = true
                scope.launch {
                    var ok = 0; var manual = 0; var err = 0
                    var firstManualUrl: String? = null
                    for (e in shown) {
                        val r = VaultSites.checkin(ctx, e, "user")
                        when (r.state) {
                            "ok" -> ok++
                            "manual" -> { manual++; if (firstManualUrl == null) firstManualUrl = r.url }
                            "error" -> err++
                        }
                    }
                    busy = false
                    Toast.makeText(ctx, tc(ctx, "签到完成：成功 $ok · 需手动 $manual · 失败 $err", "Check-in: $ok ok · $manual manual · $err failed"), Toast.LENGTH_LONG).show()
                }
            })
            MibotButton(t("刷新状态", "Refresh status"), tonal = true, enabled = !busy && shown.isNotEmpty(), modifier = Modifier.weight(1f), onClick = {
                busy = true
                scope.launch {
                    shown.forEach { VaultSites.refreshStatus(ctx, it, "user") }
                    busy = false
                }
            })
        }
        if (shown.isEmpty()) {
            MibotSpacer(24.dp)
            MibotCard(padding = PaddingValues(20.dp)) {
                Text(t("还没有账号", "No accounts yet"), fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.size(6.dp))
                Text(
                    t("添加网站或服务的登录信息。密码用 Android Keystore 加密保存，AI 只能按你设定的权限请求使用，永远看不到明文。",
                        "Add logins for sites and services. Secrets are encrypted with the Android Keystore; the AI can only request to use them under the permission you set and never sees plaintext."),
                    fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(12.dp))
                MibotButton(t("添加账号", "Add account"), onClick = onNew)
            }
        }
        shown.groupBy { it.category }.forEach { (cat, list) ->
            MibotSectionHeader("$cat · ${list.size}")
            MibotCard {
                list.forEachIndexed { i, e ->
                    val summary = e.status?.summary(labels).orEmpty().joinToString("  ") { "${it.first} ${it.second}" }
                    val updated = e.status?.updatedAt?.takeIf { it > 0 }?.let { " · " + ago(it) } ?: ""
                    val checkin = e.status?.lastCheckinState?.let { st ->
                        when (st) { "ok" -> t(" · 已签到", " · checked in"); "manual" -> t(" · 需要手动", " · manual"); "error" -> t(" · 签到失败", " · check-in failed"); else -> "" }
                    } ?: ""
                    val (pl, pk) = permLabel(e.permission)
                    MibotRow(
                        title = e.site,
                        subtitle = e.username + (if (summary.isNotEmpty()) "\n$summary$updated" else updated) + checkin,
                        icon = categoryIcon(cat),
                        color = categoryColor(cat),
                        onClick = { onOpen(e.id) },
                        showDivider = i < list.lastIndex,
                        trailing = { StatusChip(pl, pk) },
                    )
                }
            }
        }
        MibotFootnote(t(
            "AI 通过 {{vault:ID}} 占位符或沙箱里的 vault 命令请求使用账号；每次使用都记入使用记录。支付、删除、以你名义发送等高风险操作总是需要确认。",
            "The AI requests accounts via {{vault:ID}} placeholders or the `vault` CLI in the sandbox; every use is logged. Paying, deleting and sending as you always need confirmation.",
        ))
    }
}

@Composable
fun VaultEditScreen(id: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val existing = remember(id) { if (id == "new") null else VaultStore.get(ctx, id) }
    var site by remember { mutableStateOf(existing?.site ?: "") }
    var username by remember { mutableStateOf(existing?.username ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: "AI 服务") }
    var secret by remember { mutableStateOf("") }
    var showSecret by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var permission by remember { mutableStateOf(existing?.permission ?: VaultPermission.ASK) }
    var manifestId by remember { mutableStateOf(existing?.manifestId) }
    var confirmDelete by remember { mutableStateOf(false) }
    val manifests = remember { VaultSites.all(ctx) }
    val live by remember { VaultStore.entries(ctx) }.collectAsState()
    val current = live.find { it.id == existing?.id }

    fun save(): VaultEntry? {
        if (site.isBlank()) {
            Toast.makeText(ctx, tc(ctx, "请填写网站或服务", "Site is required"), Toast.LENGTH_SHORT).show()
            return null
        }
        val base = existing ?: VaultEntry(id = VaultStore.newId(), site = "", username = "")
        return VaultStore.upsert(
            ctx,
            base.copy(site = site.trim(), username = username.trim(), category = category.trim().ifEmpty { "其他" },
                notes = notes, permission = permission, manifestId = manifestId),
            newSecret = secret.ifEmpty { null },
        ).also { VaultStore.log(ctx, it, "user", if (existing == null) "create" else "edit", "ok") }
    }

    MibotScaffold(title = if (existing == null) t("添加账号", "Add account") else t("编辑账号", "Edit account"), onBack = onBack) {
        MibotSectionHeader(t("账号", "Account"))
        MibotCard(padding = PaddingValues(16.dp)) {
            OutlinedTextField(site, { site = it }, Modifier.fillMaxWidth(), label = { Text(t("网站 / 服务", "Site / service")) }, singleLine = true)
            Spacer(Modifier.size(8.dp))
            OutlinedTextField(username, { username = it }, Modifier.fillMaxWidth(), label = { Text(t("用户名 / 邮箱 / 手机号", "Username / email / phone")) }, singleLine = true)
            Spacer(Modifier.size(8.dp))
            OutlinedTextField(
                secret, { secret = it }, Modifier.fillMaxWidth(),
                label = { Text(t("密码 / Token / Cookie", "Password / token / cookie")) },
                placeholder = { if (existing?.hasSecret == true) Text(t("已保存，留空则不修改", "Saved — leave blank to keep")) },
                singleLine = true,
                visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showSecret = !showSecret }) {
                        Icon(if (showSecret) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null)
                    }
                },
            )
            Spacer(Modifier.size(8.dp))
            OutlinedTextField(notes, { notes = it }, Modifier.fillMaxWidth(), label = { Text(t("备注", "Notes")) }, minLines = 2)
        }

        MibotSectionHeader(t("分类", "Category"))
        MibotFilterChips(VaultCategories.defaults, category) { category = it }
        OutlinedTextField(
            category, { category = it },
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            label = { Text(t("自定义分类", "Custom category")) }, singleLine = true,
        )

        MibotSectionHeader(t("AI 使用权限", "AI permission"))
        MibotCard {
            VaultPermission.entries.forEachIndexed { i, p ->
                val (title, desc) = when (p) {
                    VaultPermission.AUTO -> t("自动使用", "Use automatically") to t("AI 可直接使用（高风险操作仍需确认）", "AI may use it directly (high-risk actions still ask)")
                    VaultPermission.ASK -> t("每次询问", "Ask every time") to t("每次使用前弹窗确认", "Confirm before each use")
                    VaultPermission.DISABLED -> t("禁用", "Disabled") to t("AI 不能使用此账号", "The AI cannot use this account")
                }
                MibotRow(
                    title = title, subtitle = desc, onClick = { permission = p }, showDivider = i < 2, chevron = false,
                    trailing = { if (permission == p) StatusChip("✓", ChipKind.ACCENT) },
                )
            }
        }

        MibotSectionHeader(t("状态与签到", "Status & check-in"))
        MibotFilterChips(
            listOf(t("自动匹配", "Auto-match")) + manifests.map { it.id },
            manifestId ?: t("自动匹配", "Auto-match"),
        ) { manifestId = if (manifests.any { m -> m.id == it }) it else null }
        val labels = statusLabels()
        current?.status?.let { st ->
            MibotCard(padding = PaddingValues(16.dp)) {
                st.summary(labels).forEach { (k, v) -> Text("$k  $v", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface) }
                if (st.updatedAt > 0) Text(t("更新于 ", "Updated ") + ago(st.updatedAt), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                st.error?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error) }
                st.lastCheckinMessage?.let { Text(t("签到：", "Check-in: ") + it + " · " + ago(st.lastCheckinAt), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            MibotSpacer(8.dp)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MibotButton(t("刷新状态", "Refresh"), tonal = true, modifier = Modifier.weight(1f), onClick = {
                val e = save() ?: return@MibotButton
                scope.launch {
                    val r = VaultSites.refreshStatus(ctx, e, "user")
                    Toast.makeText(ctx, r, Toast.LENGTH_SHORT).show()
                }
            })
            MibotButton(t("签到", "Check in"), tonal = true, modifier = Modifier.weight(1f), onClick = {
                val e = save() ?: return@MibotButton
                scope.launch {
                    val r = VaultSites.checkin(ctx, e, "user")
                    Toast.makeText(ctx, r.message, Toast.LENGTH_SHORT).show()
                    if (r.state == "manual" && r.url != null) {
                        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(r.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    }
                }
            })
        }
        MibotFootnote(t(
            "状态/签到由站点清单定义（HTTP 接口 + JSON 路径，或网页手动签到）。AI 可用 vault site add 添加新站点。遇到验证码会标记为需要手动，绝不自动破解。",
            "Status/check-in come from site manifests (HTTP endpoint + JSON path, or manual web check-in). The AI can add sites with `vault site add`. Captchas are marked manual and never solved automatically.",
        ))

        MibotSpacer(20.dp)
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (existing != null) {
                MibotButton(if (confirmDelete) t("确认删除", "Confirm delete") else t("删除", "Delete"), tonal = true, modifier = Modifier.weight(1f), onClick = {
                    if (!confirmDelete) { confirmDelete = true; return@MibotButton }
                    VaultStore.log(ctx, existing, "user", "delete", "ok")
                    VaultStore.delete(ctx, existing.id)
                    onBack()
                })
            }
            MibotButton(t("保存", "Save"), modifier = Modifier.weight(1f), onClick = { if (save() != null) onBack() })
        }
    }
}

@Composable
fun VaultAuditScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val events by remember { VaultStore.audit(ctx) }.collectAsState()
    MibotScaffold(title = t("使用记录", "Audit log"), onBack = onBack) {
        if (events.isEmpty()) {
            MibotSpacer(20.dp)
            MibotFootnote(t("还没有使用记录。", "Nothing logged yet."))
        } else {
            MibotSpacer(8.dp)
            MibotCard {
                events.take(200).forEachIndexed { i, ev ->
                    val kind = when (ev.result) {
                        "allowed", "ok" -> ChipKind.OK
                        "denied", "disabled" -> ChipKind.WARN
                        "error" -> ChipKind.ERR
                        else -> ChipKind.NEUTRAL
                    }
                    MibotRow(
                        title = ev.label,
                        subtitle = "${ev.actor} · ${ev.action}\n${ago(ev.at)}",
                        showDivider = i < events.size - 1,
                        chevron = false,
                        trailing = { StatusChip(ev.result, kind) },
                    )
                }
            }
        }
    }
}

