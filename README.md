# starc
Cloud Imperium Games Star Citizen launcher script

Updated 3.13
This script is intended to run Star Citizen using the less possible user knowledge and interaction.

_This script requires_
- [winetricks](https://github.com/Winetricks/winetricks)

_Recommended runners_
- [snatella](https://github.com/snatella/wine-runner-sc/releases/tag/wine-v6.6)
- [wine-sc-lug](https://github.com/gort818/wine-sc-lug/releases/tag/6.11)
- [Ackurus](https://github.com/Ackurus/wine/releases/tag/lutris-6.4-sc)

## Highlights

### Basic configuration
While this script could potentially run 'as is', as long as the user downloaded the **RSI-Setup-x.x.xx.exe** somewhere in their home, it is recommended to edit it and fill a few vars in the configuration part. The most important being the **prefix path**, the **game's installation path** and the **runner path**.

This script can run from wherever. Preferably put it somewhere in your $PATH.

### Variables
The script can toggle various linux / nividia / wine options to help adjusting things for a better experience, such as
- __GL_THREADED_OPTIMIZATIONS
- DXVK ASYNC
- WINE ESYNC
- WINE FSYNC
- Enable Feral Game Mode
- Enable Mango HUD

### Prefix creation
If the mentioned prefix path doesn't exist, it is configured and the two required verbs 'arial' and 'dxvk' are installed automatically using winetricks. Then a game installation is attempted, if the user described their game install path, or ultimately if some game install is found within the $HOME folder.

If the prefix path is not provided, the following default location is used : `$HOME/Games/starcitizen`

### Knowledge
The configuration part in the beginning of the script has been highly commented and documented, with links to the tools and helpers involved, and some short descriptions.

### No runner
In case the user hasn't provided a runner's path, the script is trying to use the distribution default wine binary. While there's a chance Star Citizen could run with a vanilla wine, it is much recommended to install and provide a custom runner (as of date).


## Command line parameters
In order to ease the use of runners / wine vars, the script accepts a few parameters to enable things during the game's launching. The benefit of this is to ensure that the wine tools are launched using the runner's binaries (as opposed to vanilla wine).

```
> sc -c
```
This parameter launches the wine configuration window (**wine_cfg**) using the runner's wine binary.

```
> sc -p
```
This parameter launches the wine's control panel (**control.exe**) using the runner's wine binary.
This is usefull for configuring joysticks in wine before playing the game.

```
> sc -r
```
This parameter removes all cached files and shader from :
- Game (../LIVE/USER/client/0/shaders)
- DXVK
- OpenGL

```
> sc -i "list of winetricks verbs"
```
There's more than one way to add some verbs to the wine's prefix with this script. With this parameter any additional winetricks verb can be installed on the fly.

Examples : `sc -i "corefonts"`
           `sc -i "vcrun2019 win10 vlc mspaint"`

## Useful links and documentation

### LUG Helper
[LUG-Helper](https://github.com/the-sane/lug-helper) Star Citizen's Linux Users Group Helper Script 

