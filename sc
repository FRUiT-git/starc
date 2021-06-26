#!/bin/sh

# Set script shell flag allexport. Only affects within script scope
set -a

# Search path for some parsing tool
# https://pastebin.com/iEAZJUwM
: ${SEND:="$(which putf)"}

# Procedure parsing useful information on screen
inf () { [ "$SEND" ] && $SEND -L "${2}" "${1}" || printf "  [%04b] %b\n" "${2}" "${1}" ; }

# Procedure handling fatal errors
err () { [ "$SEND" ] && $SEND -e "${2}" "${1}" || printf "  [%04b] %b\n" "${1}" "Error: ${2}" ; exit $1 ; }

# Procedure converting an amount of seconds (duration) in the format HH:MM
_hm () {
  local S=${1} ; local h=$(($S%86400/3600)) ; local m=$(($S%3600/60))
  inf "Played $(printf "%02.0f:%02.0f\n" $h $m)" "time"
}

# Wine prefix path settings
WINEPREFIX=$(readlink -e "$HOME/Games/sctkg") || err $? "Prefix not found"
inf "$WINEPREFIX [prefix]" "wine"

# Define whether or not to launch some external tools/helpers
# 0: disable
# 1: enable

# Enable/disable Kwin Compositor
COMPOS=0

# Enable/disable Feral Game Mode
# https://github.com/FeralInteractive/gamemode
USEGMR=1

# Enable/disable Mango Hud
# https://github.com/flightlessmango/MangoHud
USEMANGO=0

# Enable/disable VKBasalt
# https://github.com/DadSchoorse/vkBasalt
BASALT=0

# Define path to script output (shell log)
logfile="$WINEPREFIX/sc-last.log"

# Define some unified GL shader and DXVK state cache path inside prefix, eventually create it
mkdir -p "${cache:=$WINEPREFIX/cache}"

# Define the DXVK config file path
# https://github.com/doitsujin/dxvk/wiki/Configuration
DXVK_CONFIG_FILE=$(readlink -e "$HOME/.dxvk/dxvk.conf")
inf "${DXVK_CONFIG_FILE:-Config file not found}" "dxvk"

# Define the vulkan ICD config file path
VK_ICD_FILENAMES=$(readlink -e "/usr/share/vulkan/icd.d/nvidia_icd.json")
inf "${VK_ICD_FILENAMES:-ICD config file not found}" "dxvk"

# Define DXVK log level (set to none may fasten execution by reducing disk I/O)
# Values: none|error|warn|info|debug
inf "Log level [dxvk]" "${DXVK_LOG_LEVEL:=none}"

# Whether or not DXVK should use ASYNC
inf "Async [dxvk]" "${DXVK_ASYNC:=1}"

# Specify a directory where to put the DXVK cache files
# Preferably use the unified cache path above
inf "State cache: ${DXVK_STATE_CACHE_PATH:=${cache:-not found}} [dxvk]" "${DXVK_STATE_CACHE:=1}"

# Specify a directory where to put the OpenGL cache files
# Preferably use the unified cache path above
inf "GL: Shader cache: ${__GL_SHADER_DISK_CACHE_PATH:=${cache:-not found}}" "${__GL_SHADER_DISK_CACHE:=1}"
#__GL_SHADER_DISK_CACHE_SIZE=17179869184
#__GL_SHADER_DISK_CACHE_SKIP_CLEANUP=0

# Whether or not threaded optimizations should be used by OpenGL
inf "GL: Threaded Optimizations" "${__GL_THREADED_OPTIMIZATIONS:=1}"

# Specify where's the VKBasalt configuration file
VKBASALT_CONFIG_FILE=$(readlink -e "$WINEPREFIX/vkbasalt.conf")
inf "Config file: ${VKBASALT_CONFIG_FILE:-not found} [vkbasalt]" "${ENABLE_VKBASALT:=${BASALT:=0}}"

# Specify where's the runners repository
rdep=$(readlink -e "$HOME/.local/share/lutris/runners/wine")
inf "Runners repository: ${rdep:-not found}" "wine"

# Select which runner to pick up in the runners repository
# Use command line parameters to change runner
#
# Example to select wine-tkg:
#
# > ./sc -t
#
if [ "^${1#-}" != "^${1}" ]; then while getopts ":atslf" p; do case ${p} in
  a) runr="ackurus"                       ;;
  s) runr="wine-runner-sc-patched"        ;;
  t) runr="wine-tkg"                      ;;
  l) runr="lutris-6.10-2-x86_64"          ;;
  f) runr="wine-runner-6.10-gold-fsync"   ;;
esac ; done ; shift ; fi

# If no specific runner has been explicited with a command line parameter, define a default one
: ${runr:="wine-tkg-staging-fsync-git-6.11.r0.g432c0b5a"}

# Specify the wine runner BIN directory
rbin="$rdep/$runr/bin"

# Specify the wine runner executable and perform a sanity check
wine=$(readlink -e "$rbin/wine") || err $? "Wine executable or runner path not found"
inf "Runner: $runr" "wine"

# Specify some wine environment variables. Link to runner directories
WINELOADER="$wine"
WINEDLLOVERRIDES=dxgi=n
WINEDEBUG=-all
WINEDLLPATH=$(readlink -e "$rdep/$runr/lib64/wine" || readlink -e "$rdep/$runr/lib/wine")
inf "DLL path: ${WINEDLLPATH:-not found}" "wine"

# Whether or not wine ESYNC should be enabled
inf "Wine E-sync" "${WINEESYNC:=1}"

# Eventually include wine runner bin directory into system PATH
case :$PATH: in *:${rbin}:*) ;; *) PATH="$PATH${PATH:+:}${rbin}" ;; esac

# Store the Robert Space Industries and Star Citizen LIVE locations
rsip="$WINEPREFIX/drive_c/Program Files/Roberts Space Industries"
sclp="$rsip/StarCitizen/LIVE"

# Set which executable to use, depending on login infos file existance (loginData.json)
# User must capture this file during game's execution and copy it back
# in the ../StarCitizen/LIVE directory after the launcher closed
[ -f "$sclp/loginData.json" ] && \
  game="$sclp/Bin64/StarCitizen.exe" || \
  game="$rsip/RSI Launcher/RSI Launcher.exe"

# Sanity check
[ -f "$game" ] && inf "${game##*/} found in Wine prefix" "game" || err 2 "Executable not found"

# Lookup Feral Game Mode, use the toggle above to enable/disable it
gmr="$(which gamemoderun)"
[ $USEGMR -eq 1 ] && cmd="$gmr"
inf "Feral Game Mode: ${gmr:-not found}" "${USEGMR:=0}"

# Lookup Mango HUD, use the toggle above to enable/disable it
mango="$(which mangohud)"
[ $USEMANGO -eq 1 ] && cmd="${cmd:+$cmd${mango:+ }}${mango}"
inf "Mango: ${mango:-not found}" "${USEMANGO:=0}"

# If RSI Launcher.exe window appears white/blank, uncomment this line
#args="--use-gl=osmesa"

# Store initial date/time for game uptime calculation
st=$(date +%s)

# Disable Kwin's Compositor depending on above user settings
[ $COMPOS -eq 0 ] && qdbus org.kde.KWin /Compositor suspend >/dev/null 2>&1

# Output log path and time, everything went good so far :)
inf "$logfile" "log"
inf "Launching Star Citizen at $(date +%R)" "game"

# Finally, launch the game
$cmd "$wine" "$game" ${args} >"$logfile" 2>&1

# Re-enable Kwin's Compositor if it has been disabled
[ $COMPOS -eq 0 ] && qdbus org.kde.KWin /Compositor resume >/dev/null 2>&1

# Game uptime calculation
_hm $(( $(date +%s) - $st ))

# Daemon to kill Feral Game Mode a few seconds after script gave hand back to shell
( sleep 10 ; pkill gamemoded 2>/dev/null ; ) &

