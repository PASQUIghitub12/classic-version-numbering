package dev.classicversion.mixin;

import dev.classicversion.version.MappedWorldVersion;
import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Replaces the version shown in the operating-system window title.
 *
 * <p>{@code Minecraft.createTitle()} builds the title bar text from
 * {@code SharedConstants.getCurrentVersion().name()}. Because the window is
 * created during client startup, this hook relies on the default rules until the
 * configuration file has been read (see {@code ClassicVersion}).</p>
 */
@Mixin(Minecraft.class)
public abstract class MinecraftWindowTitleMixin {

    @Redirect(
            method = "createTitle",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/SharedConstants;getCurrentVersion()Lnet/minecraft/WorldVersion;"))
    private WorldVersion classicVersionNumbering$mapVersion() {
        return new MappedWorldVersion(SharedConstants.getCurrentVersion());
    }
}
