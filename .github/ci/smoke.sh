#!/usr/bin/env bash
# Emulator smoke test: install, launch, capture key screens of the glass UI.
# Usage: smoke.sh <apk> <outdir>
set -uo pipefail
APK="$1"; OUT="$2"; PKG=com.dl1991.mibot
mkdir -p "$OUT"
HERE="$(cd "$(dirname "$0")" && pwd)"
fail=0

shot() { sleep "${2:-3}"; adb exec-out screencap -p > "$OUT/$1.png"; echo "shot: $1 ($(stat -c%s "$OUT/$1.png") bytes)"; }
alive() { adb shell pidof $PKG >/dev/null 2>&1; }
tap() { python3 "$HERE/tap.py" "$@"; }
launch() { adb shell am start -W -n $PKG/.MainActivity --ez glass_demo true > "$OUT/start-$1.txt"; }

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

# Chinese UI pass (per-app locale, API 33+; ignored on older images).
if adb shell cmd locale set-app-locales $PKG --locales zh-CN 2>/dev/null; then
  adb shell am force-stop $PKG; sleep 1; launch zh; sleep 15
  shot 09-chats-zh 2
  tap desc "打开侧边栏" && shot 10-sidebar-zh 3 || echo "::warning::zh sidebar button not found"
  adb shell input keyevent KEYCODE_BACK; sleep 2
  tap desc "动态" && shot 11-feed-zh 3 || true
fi

alive || { echo "::error::app died during smoke test"; fail=1; }
adb logcat -d -v time > "$OUT/logcat.txt" || true
grep -E "FATAL EXCEPTION" -A30 "$OUT/logcat.txt" | head -120 > "$OUT/crash.txt" || true
if [ -s "$OUT/crash.txt" ]; then echo "::error::crash in logcat"; cat "$OUT/crash.txt"; fail=1; fi
exit $fail
