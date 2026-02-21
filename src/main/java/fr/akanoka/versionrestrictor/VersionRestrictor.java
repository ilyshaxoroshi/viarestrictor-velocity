package fr.akanoka.versionrestrictor;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Plugin(
        id = "versionrestrictor",
        name = "VersionRestrictor",
        version = "1.1.0",
        authors = {"AkaNoka"},
        dependencies = {@Dependency(id = "viaversion")}
)
public class VersionRestrictor {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private Set<Integer> allowedProtocols;
    private String kickMessage;
    private boolean logKicks;

    @Inject
    public VersionRestrictor(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        // Создать директорию если нет
        if (!Files.exists(dataDirectory)) {
            try {
                Files.createDirectories(dataDirectory);
            } catch (IOException e) {
                logger.error("Не удалось создать директорию: " + e.getMessage());
            }
        }

        // Копировать default config если нет
        File configFile = dataDirectory.resolve("config.yml").toFile();
        if (!configFile.exists()) {
            try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                } else {
                    logger.error("config.yml не найден в ресурсах!");
                }
            } catch (IOException e) {
                logger.error("Ошибка копирования config.yml: " + e.getMessage());
            }
        }

        // Загрузить config
        loadConfigData(configFile);

        // Регистрация команды
        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("versionrestrictor").build(),
                new ReloadCommand(this)
        );

        logger.info("VersionRestrictor активирован с " + allowedProtocols.size() + " разрешенными версиями.");
    }

    private void loadConfigData(File configFile) {
        Yaml yaml = new Yaml();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            Map<String, Object> config = yaml.load(fis);
            if (config == null) {
                logger.error("config.yml пуст или невалидный!");
                allowedProtocols = new HashSet<>();
                kickMessage = "§cВаша версия не поддерживается!";
                logKicks = true;
                return;
            }
            allowedProtocols = new HashSet<>();
            
            Object versionsObj = config.get("allowed_versions");
            if (versionsObj instanceof List) {
                List<?> allowedVersions = (List<?>) versionsObj;
                for (Object obj : allowedVersions) {
                    try {
                        if (obj instanceof Integer) {
                            allowedProtocols.add((Integer) obj);
                        } else if (obj instanceof String) {
                            allowedProtocols.add(Integer.parseInt((String) obj));
                        } else {
                            logger.warn("Неверный тип протокола в config.yml: " + obj);
                        }
                    } catch (NumberFormatException e) {
                        logger.warn("Неверный протокол в config.yml: " + obj);
                    }
                }
            }
            
            Object messageObj = config.get("kick_message");
            if (messageObj instanceof String) {
                kickMessage = (String) messageObj;
            } else {
                kickMessage = "§cВаша версия не поддерживается!";
            }
            
            Object logKicksObj = config.get("log_kicks");
            logKicks = logKicksObj instanceof Boolean ? (Boolean) logKicksObj : true;
        } catch (IOException e) {
            logger.error("Ошибка загрузки config.yml: " + e.getMessage());
            allowedProtocols = new HashSet<>();
            kickMessage = "§cВаша версия не поддерживается!";
            logKicks = true;
        }
    }

    public void reloadPluginConfig() {
        File configFile = dataDirectory.resolve("config.yml").toFile();
        loadConfigData(configFile);
        logger.info("Конфигурация VersionRestrictor перезагружена.");
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        if (event == null) {
            return;
        }
        
        Player player = event.getPlayer();
        if (player == null || allowedProtocols == null || allowedProtocols.isEmpty() || kickMessage == null) {
            return;
        }
        
        int protocol = -1;
        try {
            protocol = Via.getAPI().getPlayerVersion(player.getUniqueId());
        } catch (Exception e) {
            logger.warn("Ошибка получения версии игрока " + player.getUsername() + ": " + e.getMessage());
            return;
        }
        
        String mcVersion = "Unknown";

        if (protocol != -1) {
            try {
                ProtocolVersion pv = ProtocolVersion.getProtocol(protocol);
                if (pv != null) {
                    String name = pv.getName();
                    if (name != null && !name.isEmpty()) {
                        mcVersion = name.split("-")[0];
                    }
                }
            } catch (Exception e) {
                logger.debug("Не удалось получить название версии для протокола " + protocol);
            }
        }

        if (!allowedProtocols.contains(protocol)) {
            String message = kickMessage
                    .replace("%version%", String.valueOf(protocol))
                    .replace("%mcversion%", mcVersion);

            try {
                Component kickComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
                player.disconnect(kickComponent);
            } catch (Exception e) {
                logger.error("Ошибка при отправке kick сообщения: " + e.getMessage());
                player.disconnect(Component.text("§cВаша версия не поддерживается!"));
            }

            if (logKicks) {
                logger.info("Игрок " + player.getUsername() + " кикнут за версию " + mcVersion + " (" + protocol + ")");
            }
        }
    }
}

// Класс для команды reload
class ReloadCommand implements com.velocitypowered.api.command.SimpleCommand {

    private final VersionRestrictor plugin;

    public ReloadCommand(VersionRestrictor plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(com.velocitypowered.api.command.SimpleCommand.Invocation invocation) {
        if (invocation == null) {
            return;
        }
        
        com.velocitypowered.api.command.CommandSource source = invocation.source();
        if (source == null) {
            return;
        }
        
        String[] args = invocation.arguments();

        if (!source.hasPermission("versionrestrictor.reload")) {
            source.sendMessage(Component.text("§cНет прав."));
            return;
        }

        if (args == null || args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            source.sendMessage(Component.text("§eИспользование: /versionrestrictor reload"));
            return;
        }

        try {
            plugin.reloadPluginConfig();
            source.sendMessage(Component.text("§aКонфигурация перезагружена."));
        } catch (Exception e) {
            source.sendMessage(Component.text("§cОшибка при перезагрузке конфигурации: " + e.getMessage()));
        }
    }
}
