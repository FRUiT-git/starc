#!/bin/sh

# Set script shell flag allexport. Only affects within script scope
set -a

# User defined configuration, fill those vars according to your system
#
# Vars begining with an underscore (_) require strings (path or args)
# Vars with all caps are toggles that require integer 0 or 1 (or empty)
#

# [Required] Game installation
# Path to RSI-Setup-*.*.**.exe launcher install file
# This is required only to create and configure new prefixes
# i.e. "$HOME/download/RSI-Setup-1.4.11.exe"
_scins=

# [Optional] Game prefix
# Path to the wine prefix folder where Star Citizen is installed
# If prefix doesn't exist, it is created and configured
# If not set, script uses the default location below
# i.e. "$HOME/Games/starcitizen"
_scpre=

# [Optional] Runner
# Full name of the wine runner folder
# If not set, script tries to use the default system wine executable
# i.e. "$HOME/.local/share/lutris/runners/wine/lutris-6.10-2-x86_64"
_scrun=

# [Optional] Vulkan ICD configuration file path
# Link to the icd vulkan descriptor depending on gfcard
# i.e. "/usr/share/vulkan/icd.d/nvidia_icd.json"
_scicd=

# [Optional] DXVK configuration file path
# https://github.com/doitsujin/dxvk/wiki/Configuration
# If not set, script tries to lookup in home default path
# i.e. "$HOME/.dxvk/dxvk.conf"
_scdxc=

# [Optional] VKBasalt configuration file path
# If not set, script tries to lookup inside prefix folder
# i.e. "/path/to/vkbasalt.conf"
_scvkb=

# [Optional] DXVK state and OpenGL shader cache path
# Usefull if cache needs to be stored on another (external) drive
# If not set, a default cache path is created inside prefix folder
# i.e. "/path/to/cache"
_scglc=

# [Optional] Winetricks verbs
# List of verbs Winetricks should install in a new prefix in addition
# to the required arial and dxvk, separated by spaces
# Script looks up at start and installs any new verb found here
# i.e. "vcrun2019 corefonts win10"
_scwtt=

# If RSI Launcher.exe window appears white/blank, override the renderer
# i.e. "--use-gl=osmesa"
_scarg="--use-gl=osmesa"

# Define game options and the use of some external tools/helpers
#
# 0: disable
# 1: enable
#

# Branch selection
# Whether the PTU branch (1) should be used instead of LIVE
# If not set, script uses the LIVE branch (0)
USEPTU=

# Enable/disable 'putf' as the string parser (or use 'printf' instead)
# Parse and format output messages through a fancy bus
# https://github.com/FRUiT-git/putf
USEPUTF=

# Enable/disable Feral Game Mode
# Optimise Linux system performance on demand
# https://github.com/FeralInteractive/gamemode
USEGMR=

# Enable/disable Mango Hud
# A Vulkan and OpenGL overlay for monitoring FPS, temperatures,
# CPU/GPU load and more
# https://github.com/flightlessmango/MangoHud
USEMGO=

# Enable/disable VKBasalt
# vkBasalt is a Vulkan post processing layer to enhance the visual
# graphics of games
# https://github.com/DadSchoorse/vkBasalt
BASALT=

# Enable/disable Open GL threaded optimizations
# If not set, value defaults to 0
USETOPT=

# DXVK log level
# Set to none may fasten execution by reducing disk I/O)
# If not set, value defaults to 'none'
# Values: none|error|warn|info|debug
DXVKLL=

# Enable/disable DXVK ASYNC
# If not set, value defaults to 0
DXVKAS=

# Enable/disable DXVK state cache
# If not set, value defaults to 0
DXVKSC=1

# Enable/disable Open GL shader cache
# If not set, value defaults to 0
USEGLC=1

# Enable/disable wine ESYNC
# If not set, value defaults to 0
WINEESYNC=

# Enable/disable wine FSYNC
# Only effects if FSYNC capable kernel and runner are installed
# Enabling this automatically inactivates ESYNC
# If not set, value defaults to 0
WINEFSYNC=

# Disable loginData.json capture
# By default, script tries to capture this file to allow to
# bypass the launcher and start the game directly
# Set this to 1 prevents this behavior to always start the game
# through the launcher
DNC=

# Enable/disable Kwin Compositor
# If not set or set to 0, disable compositing
COMPOS=

# Set script default output verbosity
# Perm toggle for the parameter -s
# It is not recommended to disable this until being very comfy
# about what happens
# NOTE: The important messages still always show
VERB=1

######################################################################
# DO NOT EDIT BELOW THIS LINE                                        #
######################################################################

# Declare
maj=0
min=2
dpv=1.4.11
dpi="RSI-Setup-${dpv}.exe"
dpu="https://install.robertsspaceindustries.com/star-citizen"

# Lookup putf
[ "${USEPUTF:-0}" != "0" ] && p="$(which putf)"

# Procedure parsing information on screen
inf () {
  [ "${3:-$VERB}" ] && {
    [ "$p" ] && "$p" -c ${3:-32} -L "$2" "$1" || \
    printf "  [\033[${3:-32}m%04b\033[0;0m] %b\n" "$2" "$1"
  }
  return 0
}

# Procedure handling fatal errors
err () {
  [ "$p" ] && "$p" -e "$2" "$1" 1>&2 || \
  printf "  [\033[31m%04b\033[0;0m] %b\n" "$1" "Error: $2" 1>&2
  exit $1
}

# Procedure converting an amount of seconds (duration) in the format HH:MM
_hm () {
  local S=$1 ; local h=$(($S%86400/3600)) ; local m=$(($S%3600/60))
  inf "Played $(printf "%02.0f:%02.0f\n" $h $m)" "time" 32
}

# Output script version
version () {
  cat <<EOF
${0##*/}: version $maj.$min
EOF
  exit 0
}

# Output command line parameters options
synopsis () {
  cat <<EOF
Usage: ${0##*/} [OPTION]
    Unofficial Star Citizen launcher script version $maj.$min
    
Options:
    -i, --install=LIST  Install the quoted LIST of verbs with winetricks
    -c, --config        Launch 'winecfg'
    -p, --panel         Launch wine control panel
    -r, --purge-cache   Purge ALL cached files (game + dxvk + opengl)
    -s                  Silent mode
    -u                  Force renew login infos ../LIVE/loginData.json
    -v, --version       Show version
    -h, --help          Show this help
EOF
  exit 0
}

# Process command line arguments
[ "^${1#-}" != "^${1}" ] && { while getopts ":cprhdi:vsu" a; do case $a in
         h|-help) synopsis                               ;;
      v|-version) version                                ;;
       c|-config) wine_cfg='true'                        ;;
        p|-panel) wine_cpl='true'                        ;;
  r|-purge-cache) rm_cache='true'                        ;;
               u) DNC='true'                             ;;
               d) DEBUG='true'                           ;;
      i|-install) _scwtt="${OPTARG}${_scwtt:+ $_scwtt}"  ;;
               s) unset VERB                             ;;
esac ; done ; shift $(($OPTIND-1)) ; }

# Check VERB
[ "${VERB:-0}" = "0" ] && unset VERB

# Sanity check
[ "$DISPLAY" ] || err 3 "No graphical environment found"

# Wine prefix path settings
mkdir -p "${WINEPREFIX:=${_scpre:-$HOME/Games/starcitizen}}" 2>/dev/null || err $? "Unable to find/create prefix"
inf "Prefix: $WINEPREFIX" "wine" 35

# Check winetricks
which winetricks >/dev/null 2>&1 || inf "Winetricks not found" "conf" 31

# Check kernel map count
vmc=$(cat /proc/sys/vm/max_map_count 2>/dev/null)
[ ${vmc:-16777216} -ge 16777216 ] || {
  inf "Value too low vm.max_map_count = $vmc (recommended 16777216)" "conf" 31
  inf "https://stackoverflow.com/questions/42889241" "conf" 31
}

# Define path to script output (shell log)
> "${logfile:=$WINEPREFIX/sc-last.log}" || err $? "Unable to write inside prefix"

# Specify the runner bin directory
_scrun=$(readlink -e "$_scrun")
rbin="${_scrun:-/usr}/bin"
runr="${_scrun##*/}"

# Setup runner/wine
WINE=$(readlink -e "$rbin/wine") || err $? "Wine executable or runner path not found"
rbin="${WINE%/*}"
WINELOADER="$WINE"
WINEDLLOVERRIDES="dxgi=b,n"
WINEDEBUG="-all"
WINESERVER=$(readlink -e "$rbin/wineserver")
WINEDLLPATH=$(readlink -e "${rbin%/*}/lib64/wine" || readlink -e "${rbin%/*}/lib/wine")
inf "Runner: ${runr:-$WINE}" "wine"
inf "Server: $WINESERVER" "wine"
inf "Version: $($WINE --version)" "wine"
inf "DLL path: ${WINEDLLPATH:-not found}" "wine"

# Eventually include runner bin directory into system PATH
case :$PATH: in *:${rbin}:*) ;; *) PATH="$PATH${PATH:+:}${rbin}" ;; esac

# Configure prefix
[ -f "$WINEPREFIX/system.reg" ] || {
  inf "Configuring the new prefix, please wait" "conf" 31
  wine_cfg='true'
  wine_cpl='true'
  _scwtt="arial dxvk${_scwtt:+ $_scwtt}"
}
for verb in $_scwtt; do
  grep -q -e "$verb" "$WINEPREFIX"/winetricks.log 2>/dev/null || {
    inf "Installing « $verb »" "conf" 31
    winetricks $verb >> "$logfile" 2>&1 && $WINESERVER -w || inf "Failed" "122" 31
  }
done

# Launch wine configuration tools on demand
[ "$wine_cfg" ] || [ "$wine_cpl" ] && inf "Launching wine configuration panel" "conf" 31
[ "$wine_cfg" ] && "$rbin"/winecfg >> "$logfile" 2>&1
[ "$wine_cpl" ] && "$WINE" control >> "$logfile" 2>&1

# Use PTU or LIVE
[ "${USEPTU:-0}" != "0" ] && BRANCH="PTU"

# Store the Roberts Space Industries and Star Citizen LIVE/PTU locations
rsip="$WINEPREFIX/drive_c/Program Files/Roberts Space Industries"
sclp="$rsip/StarCitizen/${BRANCH:=LIVE}"

# Set launcher path
game="$rsip/RSI Launcher/RSI Launcher.exe"

# If launcher is not installed, try to install
[ -f "$game" ] || {
  _scins=$(readlink -e "$_scins")
  : ${_scins:="$(find $HOME -name "RSI-Setup*.exe" -type f -print0 -quit 2>/dev/null)"}
  [ ! -f "$_scins" ] && ping -c 1 www.google.com >/dev/null 2>&1 && {
    inf "Downloading $dpi" "conf" 31
    wget -x -q --show-progress -O "$HOME/download/${dpi}" "${dpu}/${dpi}" && \
      _scins=$(readlink -e "$HOME/download/${dpi}") || \
      err $? "Failed to download or file not found"
  } || inf "No internet connexion" "conf" 31
  [ ! -f "$_scins" ] && inf "Game installation not found" "2" 31 || {
    inf "Executing ${_scins##*/}" "conf" 31
    "$WINE" "$_scins" >> "$logfile" 2>&1 && {
      inf "${_scins##*/} has been installed succesfully" "conf" 31
      sleep 1 ; pkill RSI >/dev/null 2>&1
      $WINESERVER -k
    } || inf "Installation aborted or failed" "conf" 31
  }
}

# Switch executable, depending on login infos file existance (loginData.json)
# FIXME: periodically delete $ldata
ldata="$sclp/loginData.json"
scexe="$sclp/Bin64/StarCitizen.exe"
[ "${DNC:-0}" != "0" ] && [ -f "$ldata" ] && rm -rfv "$ldata" >> "$logfile" 2>&1
[ "${DNC:-0}" != "0" ] || {
  [ -f "$scexe" ] && {
    [ -f "$ldata" ] && game="$scexe" || {
      (
        sleep 300
        [ -f "$ldata" ] && chmod 444 "$ldata" 2>/dev/null && {
          inf "${ldata##*/} has been locked" "conf" 31
          perm=$(find "$sclp" -name "${ldata##*/}" -printf "%m" 2>/dev/null)
          inf "Permissions" "$perm" 31
        }
      ) &
    }
  }
}

# Sanity check
[ -f "$game" ] && inf "${game##*/} found inside prefix" "game" || err 2 "Game executable not found"
ver=$(grep -e "FileVersion" "$sclp/Game.log" 2>/dev/null) && inf "${ver%[[:cntrl:]]} $BRANCH" "game"

# Define GL shader and DXVK state cache path, eventually create it
mkdir -p "${cache:=${_scglc:-$WINEPREFIX/cache}}"

# Purge cache
[ "$rm_cache" ] && {
  rm -rfv "$sclp/USER/Client/0/shaders"/* >> "$logfile" 2>&1
  rm -rfv "$cache"/* >> "logfile" 2>&1
  inf "Wiped all cache files from $BRANCH" "conf" 31
}

# Specify where to put the DXVK cache file
inf "State cache: ${DXVK_STATE_CACHE_PATH:=${cache:-not found}} [dxvk]" "${DXVK_STATE_CACHE:=${DXVKSC:-0}}"

# Specify where to put the OpenGL cache files
inf "GL: Shader cache: ${__GL_SHADER_DISK_CACHE_PATH:=${cache:-not found}}" "${__GL_SHADER_DISK_CACHE:=${USEGLC:-0}}"
__GL_SHADER_DISK_CACHE_SIZE=17179869184
#__GL_SHADER_DISK_CACHE_SKIP_CLEANUP=0

# Define the DXVK config file path
DXVK_CONFIG_FILE=$(readlink -e "${_scdxc:-$HOME/.dxvk/dxvk.conf}")
inf "${DXVK_CONFIG_FILE:-Configuration file not found}" "dxvk"

# Define the vulkan ICD config file path
VK_ICD_FILENAMES=$(readlink -e "${_scicd}")
inf "${VK_ICD_FILENAMES:-ICD configuration file not found}" "dxvk"

# DXVK log level
inf "Log level [dxvk]" "${DXVK_LOG_LEVEL:=${DXVKLL:-none}}"

# Whether or not DXVK should use ASYNC
inf "Async [dxvk]" "${DXVK_ASYNC:=${DXVKAS:-0}}"

# Whether or not threaded optimizations should be used by OpenGL
inf "GL: Threaded Optimizations" "${__GL_THREADED_OPTIMIZATIONS:=${USETOPT:-0}}"

# Define the VKBasalt config file path
VKBASALT_LOG_LEVEL="none"
VKBASALT_CONFIG_FILE=$(readlink -e "${_scvkb:-$WINEPREFIX/vkbasalt.conf}")
inf "Config file: ${VKBASALT_CONFIG_FILE:-not found} [vkbasalt]" "${ENABLE_VKBASALT:=${BASALT:-0}}"

# Whether or not wine ESYNC and FSYNC should be enabled
isfsync=$(\ls /sys/kernel | grep "futex")
[ "$isfsync" ] && [ "${WINEFSYNC:-0}" = "1" ] && unset WINEESYNC
[ "$isfsync" ] || unset WINEFSYNC
inf "Wine E-sync" "${WINEESYNC:=0}"
inf "Wine F-sync (${isfsync:-disabled})" "${WINEFSYNC:=0}"

# Lookup Feral Game Mode
gmr="$(which gamemoderun)" || unset USEGMR
inf "Feral Game Mode: ${gmr:-not found}" "${USEGMR:=0}"
[ ! "${USEGMR:-0}" = "0" ] || unset gmr

# Lookup Mango HUD
mango="$(which mangohud)" || unset USEMGO
inf "Mango: ${mango:-not found}" "${USEMGO:=0}"
[ ! "${USEMGO:-0}" = "0" ] || unset mango

# Store initial date/time for game uptime calculation
st=$(date +%s)

# Output log path
inf "$logfile" "log"

# Disable Kwin's Compositor
kwinState=$(qdbus org.kde.KWin /Compositor active 2>/dev/null)
[ "$DEBUG" ] && COMPOS=1
[ "${COMPOS:-0}" = "0" ] && [ "$kwinState" = "true" ] && qdbus org.kde.KWin /Compositor suspend >/dev/null 2>&1

# Finally, launch the game
[ "$DEBUG" ] || {
  inf "Launching Star Citizen (${game##*/}) at $(date +%R)" "${BRANCH}" 32
  "$gmr"${mango:+${gmr:+ }"$mango"} "$WINE" "$game" $_scarg >> "$logfile" 2>&1
}

# Re-enable Kwin's Compositor if it has been disabled
[ "${COMPOS:-0}" = "0" ] && [ "$kwinState" = "true" ] && qdbus org.kde.KWin /Compositor resume >/dev/null 2>&1

# Script uptime calculation
[ "$DEBUG" ] || _hm $(( $(date +%s) - $st ))

# Daemon to kill Feral Game Mode a few seconds after script gave hand back to shell
[ "$gmr" ] && [ ! "$DEBUG" ] && {
  ( sleep 10 ; pkill gamemoded 2>/dev/null ; ) &
}

# Bye bye
exit 0

