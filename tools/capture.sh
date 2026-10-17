#!/usr/bin/env bash
# Takes the raw screenshots for the README and Stillcraft from app.hoard.shots (a demo build made with
# -Pdemo -Pshots) on a 1080x2400 phone. Usage: tools/capture.sh <out_dir> <en|ru>
# Every tap checks first that no call is going and that the demo is in front.
set -e
OUT=$1; LANG=$2; SUF=$([ "$LANG" = ru ] && echo .ru || echo "")
ADB="${ADB:-$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe}"
P=app.hoard.shots
mkdir -p "$OUT"

guard() {
  c=$("$ADB" shell dumpsys telephony.registry | grep -m1 -o "mCallState=[0-9]")
  f=$("$ADB" shell dumpsys window | grep -m1 mCurrentFocus)
  if [ "$c" != "mCallState=0" ] || ! echo "$f" | grep -q "$P/"; then echo "ABORT: $c $f"; exit 1; fi
}
tap() { guard; "$ADB" shell input tap "$1" "$2"; sleep "${3:-2}"; }
back() { guard; "$ADB" shell input keyevent BACK; sleep 2; }
shot() { sleep 1; "$ADB" exec-out screencap -p > "$OUT/$1$SUF.png"; echo "$1$SUF"; }

"$ADB" shell am force-stop $P
"$ADB" shell monkey -p $P -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
sleep 10
guard
shot cave
tap 331 2220            # Halls
tap 236 533             # Treasures view
shot halls
tap 200 1270 3          # a coin
shot coin
back
tap 540 1270 3          # the ruby heart
shot gem
back
tap 540 790 4           # a mirror
shot mirror
back
tap 602 533             # Photos view
shot photos
tap 746 2220            # Chronicle
shot chronicle
tap 124 2220            # back to the cave
