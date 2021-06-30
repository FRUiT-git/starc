#!/bin/sh

# Set script shell flag allexport. Only affects within script scope
set -a

DEBUG=

# User defined configuration, fill those vars according to your system
#
# Vars begining with an underscore (_) require strings (path)
# Vars with all caps are toggles that require integer 0 or 1 (or empty)
#

# [Required] Game installation executable
# Path to RSI-Setup-*.*.**.exe launcher install file
# i.e. "$HOME/download/RSI-Setup-1.4.11.exe"
_scins=

# [Optional] Game prefix
# Path to the wine prefix folder where Star Citizen is installed
# If prefix doesn't exist, it will be created and configured
# If not set, prefix will default to "$HOME/Games/starcitizen"
_scpre=

# [Optional] Runner
# Name of the wine runner folder
# If not set, script will try to use the default system wine executable
# i.e. "$HOME/.local/share/lutris/runners/wine/lutris-6.10-2-x86_64"
_scrun="$HOME/.local/share/lutris/runners/wine/ackurus"

# [Optional] Vulkan ICD configuration file path
# Link to the icd vulkan descriptor depending on gfcard
# i.e. "/usr/share/vulkan/icd.d/nvidia_icd.json"
_scicd=

# [Optional] DXVK configuration file path
# https://github.com/doitsujin/dxvk/wiki/Configuration
# If not set, script will try to lookup in home default path
# i.e. "$HOME/.dxvk/dxvk.conf"
_scdxc=

# [Optional] VKBasalt configuration file path
# If not set, script will try to lookup inside prefix folder
# i.e. "/path/to/vkbasalt.conf"
_scvkb=

# [Optional] DXVK state and OpenGL shader cache path
# If not set, a default cache path will be created inside prefix folder
# i.e. /path/to/cache
_scglc=

# Define whether or not to launch some external tools/helpers
# 0: disable
# 1: enable

# Enable/disable 'putf' as the string parser (or use 'printf' instead)
# https://github.com/FRUiT-git/putf
USEPUTF=

# Enable/disable Feral Game Mode
# https://github.com/FeralInteractive/gamemode
USEGMR=

# Enable/disable Mango Hud
# https://github.com/flightlessmango/MangoHud
USEMANGO=

# Enable/disable VKBasalt
# https://github.com/DadSchoorse/vkBasalt
BASALT=

# Enable/disable Open GL threaded optimizations
# If not set, value defaults to 0
USETOPT=

# Define DXVK log level (set to none may fasten execution by reducing disk I/O)
# If not set, value defaults to 'none'
# Values: none|error|warn|info|debug
DXVKLL=

# Enable/disable DXVK ASYNC
# If not set, value defaults to 0
DXVKAS=

# Enable/disable DXVK state cache
# If not set, value defaults to 0
DXVKSC=

# Enable/disable Open GL shader cache
# If not set, value defaults to 0
USEGLC=

# Enable/disable wine ESYNC
# If not set, value defaults to 0
WINEESYNC=

# Enable/disable Kwin Compositor
# If not set or set to 0, disable compositing
COMPOS=

# If RSI Launcher.exe window appears white/blank, override the renderer
# i.e. "--use-gl=osmesa"
_scarg=

#################################################################
# DO NOT EDIT BELOW THIS LINE                                   #
#################################################################

# Lookup putf
[ "${USEPUTF:-0}" != "0" ] && p="$(which putf)"

# Procedure parsing useful information on screen
inf () { [ "$p" ] && $p -c ${3:-32} -L "${2}" "${1}" || printf "  [%04b] %b\n" "${2}" "${1}" ; }

# Procedure handling fatal errors
err () { [ "$p" ] && $p -e "${2}" "${1}" || printf "  [%04b] %b\n" "${1}" "Error: ${2}" ; exit $1 ; }

# Procedure converting an amount of seconds (duration) in the format HH:MM
_hm () {
  local S=${1} ; local h=$(($S%86400/3600)) ; local m=$(($S%3600/60))
  inf "Played $(printf "%02.0f:%02.0f\n" $h $m)" "time"
}

# Process command line arguments
if [ "^${1#-}" != "^${1}" ]; then while getopts ":cpr" a; do case ${a} in
  c) wine_cfg='true'  ;;
  p) wine_cpl='true'  ;;
  r) rm_cache='true'  ;;
esac ; done ; shift ; fi

# Wine prefix path settings
mkdir -p "${WINEPREFIX:=${_scpre:-$HOME/Games/starcitizen}}"
inf "$WINEPREFIX [prefix]" "wine" 35

# Define path to script output (shell log)
> "${logfile:=$WINEPREFIX/sc-last.log}"

# Specify the wine runner BIN directory
rbin=$(readlink -e "${_scrun:-/usr}/bin")
rdep="${_scrun%/*}"
runr="${_scrun##*/}"
inf "Runners repository: ${rdep:-not found}" "wine"

# Specify the wine runner executable and perform a sanity check
wine=$(readlink -e "$rbin/wine") || err $? "Wine executable or runner path not found"
inf "Runner: ${runr:-$wine}" "wine"
inf "Version: $($wine --version)" "wine"

# Specify some wine environment variables. Link to runner directories
WINELOADER="$wine"
WINEDLLOVERRIDES=dxgi=n
WINEDEBUG=-all
WINEDLLPATH=$(readlink -e "${rbin%/*}/lib64/wine" || readlink -e "${rbin%/*}/lib/wine")
inf "DLL path: ${WINEDLLPATH:-not found}" "wine"

# Prepare new prefix
[ -f "$WINEPREFIX/system.reg" ] || {
  inf "Configuring the new prefix, please wait" "wine" 31
  $rbin/winboot -u >> "$logfile" 2>&1
  winetricks arial dxvk >> "$logfile" 2>&1
  [ -f "$_scins" ] && "$wine" "$_scins" >> "$logfile" 2>&1 || inf "Installation file not found" "game" 31
  sleep 5 && pkill "RSI" >/dev/null 2>&1
  wine_cfg='true'
  wine_cpl='true'
}

# Define GL shader and DXVK state cache path inside prefix, eventually create it
mkdir -p "${cache:=${_scglc:-$WINEPREFIX/cache}}"

# Define the DXVK config file path
DXVK_CONFIG_FILE=$(readlink -e ${_scdxc:-"$HOME/.dxvk/dxvk.conf"})
inf "${DXVK_CONFIG_FILE:-Configuration file not found}" "dxvk"

# Define the vulkan ICD config file path
VK_ICD_FILENAMES=$(readlink -e "${_scicd}")
inf "${VK_ICD_FILENAMES:-ICD configuration file not found}" "dxvk"

# Define DXVK log level
inf "Log level [dxvk]" "${DXVK_LOG_LEVEL:=${DXVKLL:-none}}"

# Whether or not DXVK should use ASYNC
inf "Async [dxvk]" "${DXVK_ASYNC:=${DXVKAS:-0}}"

# Specify a directory where to put the DXVK cache files
inf "State cache: ${DXVK_STATE_CACHE_PATH:=${cache:-not found}} [dxvk]" "${DXVK_STATE_CACHE:=${DXVKSC:-0}}"

# Specify a directory where to put the OpenGL cache files
inf "GL: Shader cache: ${__GL_SHADER_DISK_CACHE_PATH:=${cache:-not found}}" "${__GL_SHADER_DISK_CACHE:=${USEGLC:-0}}"
#__GL_SHADER_DISK_CACHE_SIZE=17179869184
#__GL_SHADER_DISK_CACHE_SKIP_CLEANUP=0

# Whether or not threaded optimizations should be used by OpenGL
inf "GL: Threaded Optimizations" "${__GL_THREADED_OPTIMIZATIONS:=${USETOPT:-0}}"

# Specify where's the VKBasalt configuration file
VKBASALT_CONFIG_FILE=$(readlink -e ${_scvkb:-"$WINEPREFIX/vkbasalt.conf"})
inf "Config file: ${VKBASALT_CONFIG_FILE:-not found} [vkbasalt]" "${ENABLE_VKBASALT:=${BASALT:-0}}"

# Whether or not wine ESYNC should be enabled
inf "Wine E-sync" "${WINEESYNC:=0}"

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

# Purge all cache files
[ "$rm_cache" ] && {
  rm -rf "$sclp/USER/Client/0/shaders"/* >> "$logfile" 2>&1
  rm -rf "$cache"/* >> "logfile" 2>&1
  inf "Wiped all (shader)cache files" "game" 31
}

# Lookup Feral Game Mode
gmr="$(which gamemoderun)"
[ ! "${USEGMR:-0}" = "0" ] && cmd="$gmr"
inf "Feral Game Mode: ${gmr:-not found}" "${USEGMR:=0}"

# Lookup Mango HUD
mango="$(which mangohud)"
[ ! "${USEMANGO:-0}" = "0" ] && cmd="${cmd:+$cmd${mango:+ }}${mango}"
inf "Mango: ${mango:-not found}" "${USEMANGO:=0}"

# Store initial date/time for game uptime calculation
st=$(date +%s)

# Launch wine configuration tools depending on command line parameters
[ "$wine_cfg" -o "$wine_cpl" ] && inf "Launching configuration panel" "wine"
[ "$wine_cfg" ] && "$rbin/winecfg" >> "$logfile" 2>&1
[ "$wine_cpl" ] && "$wine" control >> "$logfile" 2>&1

# Output log path and time, everything went good so far :)
inf "$logfile" "log"
inf "Launching Star Citizen at $(date +%R)" "game"

# Disable Kwin's Compositor
kwinState=$(qdbus org.kde.KWin /Compositor active 2>/dev/null)
[ "$DEBUG" ] && COMPOS=1
[ "${COMPOS:-0}" = "0" -a "$kwinState" = "true" ] && qdbus org.kde.KWin /Compositor suspend >/dev/null 2>&1

# Finally, launch the game
[ ! "$DEBUG" ] && {
  $cmd "$wine" "$game" ${_scarg} >> "$logfile" 2>&1
}

# Re-enable Kwin's Compositor if it has been disabled
[ "${COMPOS:-0}" = "0" -a "$kwinState" = "true" ] && qdbus org.kde.KWin /Compositor resume >/dev/null 2>&1

# Game uptime calculation
_hm $(( $(date +%s) - $st ))

# Daemon to kill Feral Game Mode a few seconds after script gave hand back to shell
( sleep 10 ; pkill gamemoded 2>/dev/null ; ) &

