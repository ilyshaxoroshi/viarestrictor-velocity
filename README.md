# ViaRestrictor for Velocity
This is ViaRestrictor, a fork of the original akanoka/viarestrictor project, adapted specifically for Velocity proxies. It helps manage client versions on your Minecraft proxy setup by integrating with ViaVersion, allowing you to restrict access from incompatible or undesired clients and provide informative messages if needed.

### What It Does

Detects the player's client version through ViaVersion (which must be installed).
Supports blacklisting restricted versions or whitelisting allowed ones, based on your preference.
Customizable kick messages with color codes (&) and line breaks (\n).
Option to automatically kick players after displaying the message.
Logs blocked connections to the console for reference, if enabled.

### Why This Fork?

The original plugin was designed for Spigot servers (or forks like Paper). This version has been fully ported to work with Velocity proxies, keeping all the core features intact but optimized for the proxy environment.
Setup

Place the plugin in your Velocity plugins directory.
Requires Java 17 or higher and ViaVersion.
The configuration file is generated at plugins/ViaRestrictor/config.yml upon first launch.
allowed_versions: Specify protocol numbers (e.g., 759 for Minecraft 1.20.1).


```
kick_message: Customize with placeholders like %mcversion% or %version%.

mode: Use whitelist to allow only specified versions.

kick: Set to true to enable automatic kicks.

log_kicks: Set to true to log blocked players in the console.
```

Example for kick:  ![kick](<img width="1058" height="293" alt="286ad13cb50cc24033281b2d4bd9fe695335777a" src="https://github.com/user-attachments/assets/896aea06-b9c2-440b-9405-2805db30fb3a" />

You can reload the configuration using Velocity's commands (like /velocity reload) or restart 
