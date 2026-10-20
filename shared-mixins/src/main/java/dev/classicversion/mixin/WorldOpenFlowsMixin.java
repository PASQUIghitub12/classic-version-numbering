package dev.classicversion.mixin;

import dev.classicversion.version.MappedWorldVersion;
import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Replaces the current-version half of the "this world was last played in a
 * different version" warning shown before a world is upgraded.
 *
 * <p>{@code WorldOpenFlows.openWorldCheckVersionCompatibility} formats the
 * world's own version name together with {@code SharedConstants.getCurrentVersion().name()}.
 * Wrapping the returned version only changes the part that belongs to the running
 * client; the world's recorded version is left untouched.</p>
 */
@Mixin(WorldOpenFlows.class)
public abstract class WorldOpenFlowsMixin {

    @Redirect(
            method = "openWorldCheckVersionCompatibility",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/SharedConstants;getCurrentVersion()Lnet/minecraft/WorldVersion;"))
    private WorldVersion classicVersionNumbering$mapVersion() {
        return new MappedWorldVersion(SharedConstants.getCurrentVersion());
    }
}
