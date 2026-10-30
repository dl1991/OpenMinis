#!/usr/bin/env bash
# Emulator smoke test: install, launch, capture key screens.
# Usage: smoke.sh <apk> <outdir>
set -uo pipefail
APK="$1"; OUT="$2"; PKG=com.openminis.app
mkdir -p "$OUT"
HERE="$(cd "$(dirname "$0")" && pwd)"
fail=0

shot() { sleep "${2:-3}"; adb exec-out screencap -p > "$OUT/$1.png"; echo "shot: $1 ($(stat -c%s "$OUT/$1.png") bytes)"; }
alive() { adb shell pidof $PKG >/dev/null 2>&1; }

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
adb shell am start -W -n $PKG/.MainActivity --ez glass_demo true | tee "$OUT/start.txt"
sleep 25
alive || { echo "::error::app not running after launch"; fail=1; }
shot 01-home 2

# Open the sidebar via its menu button (found by content-desc).
python3 "$HERE/tap.py" desc "Open sidebar" && shot 02-sidebar 3 || { echo "::warning::sidebar button not found"; fail=1; }
python3 "$HERE/tap.py" desc "Account menu" && shot 03-account-menu 2 || echo "::warning::account menu not found"
adb shell input keyevent KEYCODE_BACK; sleep 1
python3 "$HERE/tap.py" desc "Bots" && shot 04-bots 3 || echo "::warning::Bots tab not found"
adb shell input keyevent KEYCODE_BACK; sleep 2

# Backdrop picker via deep link.
adb shell am start -W -a android.intent.action.VIEW -d "minis://settings/backdrop" $PKG >/dev/null
shot 05-backdrop-settings 4
python3 "$HERE/tap.py" text "Ambient" && shot 06-ambient-selected 3 || true
adb shell am start -W -a android.intent.action.VIEW -d "minis://settings" $PKG >/dev/null
shot 07-settings 4

alive || { echo "::error::app died during smoke test"; fail=1; }
adb logcat -d -v time > "$OUT/logcat.txt" || true
grep -E "FATAL EXCEPTION|AndroidRuntime" -A20 "$OUT/logcat.txt" | head -80 > "$OUT/crash.txt" || true
[ -s "$OUT/crash.txt" ] && { echo "::error::crash in logcat"; cat "$OUT/crash.txt"; fail=1; }
exit $fail
