package dev.classicversion.mixin;

import dev.classicversion.version.MappedWorldVersion;
import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Replaces the version label drawn in the bottom-left corner of the main menu.
 *
 * <p>In Minecraft 26.3 the label is built inside
 * {@code TitleScreen.extractRenderState(...)} from
 * {@code SharedConstants.getCurrentVersion().name()} before being passed to the
 * graphics extractor. Wrapping the returned version changes only that label.</p>
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {

    @Redirect(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/SharedConstants;getCurrentVersion()Lnet/minecraft/WorldVersion;"))
    private WorldVersion classicVersionNumbering$mapVersion() {
        return new MappedWorldVersion(SharedConstants.getCurrentVersion());
    }
}
