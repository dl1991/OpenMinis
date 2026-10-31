#!/usr/bin/env bash
# Emulator smoke test: install, launch, capture key screens of the glass UI.
# Usage: smoke.sh <apk> <outdir>
set -uo pipefail
APK="$1"; OUT="$2"; PKG=com.dl1991.mibot  # applicationId; activity classes stay in namespace com.openminis.app
mkdir -p "$OUT"
HERE="$(cd "$(dirname "$0")" && pwd)"
fail=0

shot() { sleep "${2:-3}"; adb exec-out screencap -p > "$OUT/$1.png"; echo "shot: $1 ($(stat -c%s "$OUT/$1.png") bytes)"; }
alive() { adb shell pidof $PKG >/dev/null 2>&1; }
tap() { python3 "$HERE/tap.py" "$@"; }
launch() { adb shell am start -W -n $PKG/com.openminis.app.MainActivity --ez glass_demo true > "$OUT/start-$1.txt"; }

adb shell getprop ro.product.cpu.abilist | tee "$OUT/abilist.txt"
adb logcat -c || true
adb install -r -g "$APK" 2>&1 | tee "$OUT/install.txt"
grep -q Success "$OUT/install.txt" || { echo "::error::install failed"; exit 1; }

adb shell settings put system screen_off_timeout 1800000 || true
adb shell svc power stayon true || true
adb shell input keyevent KEYCODE_WAKEUP || true
adb shell wm dismiss-keyguard || true

# glass_demo (debug builds only) fills the sidebar's recent list with sample
# chats/groups so a fresh emulator has something to render.
launch en
sleep 25
alive || { echo "::error::app not running after launch"; fail=1; }
shot 01-chats 2

tap desc "Open sidebar" && shot 02-sidebar 3 || { echo "::warning::sidebar button not found"; fail=1; }
adb shell input keyevent KEYCODE_BACK; sleep 2

for t in Feed:03-feed Ideas:04-ideas Bots:05-bots Settings:06-settings; do
  name="${t%%:*}"; file="${t##*:}"
  if tap desc "$name"; then shot "$file" 3; else echo "::warning::tab $name not found"; fail=1; fi
done
# Settings page scrolled to the Appearance / Feedback rows.
adb shell input swipe 540 1700 540 900 400; shot 07-settings-scrolled 2

adb shell am start -W -a android.intent.action.VIEW -d "minis://settings/backdrop" $PKG >/dev/null
shot 08-backdrop-settings 4

# Style B (warm white): switch, look at Chats + sidebar, then switch back to A.
if tap text "Warm white"; then
  shot 08b-backdrop-warm 2
  adb shell input keyevent KEYCODE_BACK; sleep 2
  tap desc "Chats" && shot 12-chats-warm 3
  tap desc "Open sidebar" && shot 13-sidebar-warm 3
  adb shell input keyevent KEYCODE_BACK; sleep 2
  adb shell am start -W -a android.intent.action.VIEW -d "minis://settings/backdrop" $PKG >/dev/null; sleep 3
  tap text "Dusk glass" || true
  adb shell input keyevent KEYCODE_BACK; sleep 2
fi
tap desc "Chats" >/dev/null 2>&1 || true

# Mibot v2 Chinese pass, light + dark. ci_lang/ci_theme/ci_vault_demo are
# debug-only launch extras (MainActivity); language applies from the next
# process start on API < 33, so seed once, then relaunch per theme.
ZH="$OUT/zh"; mkdir -p "$ZH"
zlaunch() { adb shell am force-stop $PKG; sleep 1
  adb shell am start -W -n $PKG/com.openminis.app.MainActivity --ez glass_demo true \
    --es ci_lang zh-CN --ez ci_vault_demo true --ei ci_theme "$1" > "$ZH/start-$2.txt"; sleep "${3:-12}"; }
zshot() { sleep "${2:-3}"; adb exec-out screencap -p > "$ZH/$1.png"; echo "zh shot: $1 ($(stat -c%s "$ZH/$1.png") bytes)"; }
link() { adb shell am start -W -a android.intent.action.VIEW -d "minis://settings/$1" $PKG >/dev/null; }
home() { adb shell input keyevent KEYCODE_BACK; sleep 1; adb shell input keyevent KEYCODE_BACK; sleep 1; tap desc "对话" >/dev/null 2>&1 || true; sleep 1; }
adb shell am force-stop $PKG; adb shell pm clear $PKG >/dev/null 2>&1 || true
zlaunch 1 seed 15
for th in 1:light 2:dark; do
  m="${th%%:*}"; n="${th##*:}"
  zlaunch "$m" "$n" 14
  alive || { echo "::error::app not running (zh $n)"; fail=1; }
  zshot "$n-01-onboarding" 1
  if tap desc "打开侧边栏"; then
    zshot "$n-03-sidebar" 3
    if tap desc "新对话"; then zshot "$n-02-chat" 3; else echo "::warning::zh new-chat not found ($n)"; fi
  else echo "::warning::zh sidebar button not found ($n)"; fi
  home
  tap desc "Bots" && zshot "$n-04-bots" 3 || echo "::warning::zh Bots tab not found ($n)"
  tap desc "设置" && zshot "$n-05-settings" 3 || echo "::warning::zh 设置 tab not found ($n)"
  link vault;        zshot "$n-06-vault" 4;    home
  link gateway;      zshot "$n-07-gateway" 4;  home
  link sandbox;      zshot "$n-08-sandbox" 4;  home
  link add-provider; zshot "$n-09-add-provider" 4; home
  alive || { echo "::error::app died during zh $n pass"; fail=1; }
done
adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 && adb exec-out cat /sdcard/ui.xml > "$ZH/last-ui.xml" || true

alive || { echo "::error::app died during smoke test"; fail=1; }
adb logcat -d -v time > "$OUT/logcat.txt" || true
grep -E "FATAL EXCEPTION" -A30 "$OUT/logcat.txt" | head -120 > "$OUT/crash.txt" || true
if [ -s "$OUT/crash.txt" ]; then echo "::error::crash in logcat"; cat "$OUT/crash.txt"; fail=1; fi
exit $fail
