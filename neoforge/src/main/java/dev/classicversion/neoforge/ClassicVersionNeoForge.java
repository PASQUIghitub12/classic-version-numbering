package dev.classicversion.neoforge;

import dev.classicversion.common.ClassicVersion;
import dev.classicversion.common.ConfigLoader;
import dev.classicversion.common.ModConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * NeoForge client entry point.
 *
 * <p>The mod is declared as client-only, so this class is never loaded on a
 * dedicated server. Configuration is read once during construction.</p>
 */
@Mod(value = "classic_version_numbering", dist = Dist.CLIENT)
public final class ClassicVersionNeoForge {

    private static final Logger LOGGER = LoggerFactory.getLogger("ClassicVersionNumbering");

    public ClassicVersionNeoForge() {
        Path configFile = FMLPaths.CONFIGDIR.get().resolve(ConfigLoader.FILE_NAME);
        ModConfig config = ConfigLoader.loadOrCreate(configFile, LOGGER::warn);
        ClassicVersion.setRegistry(config.toRegistry());
        LOGGER.info(
                "Classic Version Numbering active (enabled={}, mapPreReleases={}, rules={}).",
                config.enabled(),
                config.mapPreReleases(),
                config.rules().size());
    }
}
