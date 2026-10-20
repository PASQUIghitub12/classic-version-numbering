package dev.classicversion.mixin;

import dev.classicversion.version.MappedWorldVersion;
import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.minecraft.client.gui.components.debug.DebugEntryVersion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Replaces the version shown on the first line of the F3 debug overlay.
 *
 * <p>Since the debug screen became modular, that line is produced by
 * {@link DebugEntryVersion#display}. It is built from
 * {@code SharedConstants.getCurrentVersion().name()} together with the launched
 * version and the client brand; only the version name is wrapped.</p>
 */
@Mixin(DebugEntryVersion.class)
public abstract class DebugEntryVersionMixin {

    @Redirect(
            method = "display",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/SharedConstants;getCurrentVersion()Lnet/minecraft/WorldVersion;"))
    private WorldVersion classicVersionNumbering$mapVersion() {
        return new MappedWorldVersion(SharedConstants.getCurrentVersion());
    }
}
