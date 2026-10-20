package dev.classicversion.fabric;

import dev.classicversion.common.ClassicVersion;
import dev.classicversion.common.ConfigLoader;
import dev.classicversion.common.ModConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Fabric client entry point.
 *
 * <p>Loads the configuration once during client initialisation and installs the
 * resulting mapping registry. The mixins themselves never read the file.</p>
 */
public final class ClassicVersionFabricClient implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("ClassicVersionNumbering");

    @Override
    public void onInitializeClient() {
        Path configFile = FabricLoader.getInstance().getConfigDir().resolve(ConfigLoader.FILE_NAME);
        ModConfig config = ConfigLoader.loadOrCreate(configFile, LOGGER::warn);
        ClassicVersion.setRegistry(config.toRegistry());
        LOGGER.info(
                "Classic Version Numbering active (enabled={}, mapPreReleases={}, rules={}).",
                config.enabled(),
                config.mapPreReleases(),
                config.rules().size());
    }
}
