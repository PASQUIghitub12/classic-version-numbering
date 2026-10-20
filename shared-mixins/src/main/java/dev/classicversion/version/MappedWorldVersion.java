package dev.classicversion.version;

import dev.classicversion.common.ClassicVersion;
import net.minecraft.WorldVersion;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.world.level.storage.DataVersion;

import java.util.Date;

/**
 * A {@link WorldVersion} view that only changes the displayed {@link #name()}.
 *
 * <p>Every other accessor delegates to the real version, so protocol numbers,
 * data/pack formats, build times and the internal id stay untouched. This is the
 * only place where the game's version object is temporary wrapped for display.</p>
 *
 * <p>This class deliberately lives outside {@code dev.classicversion.mixin}: Mixin
 * forbids referencing classes that sit in a package registered as a mixin
 * package, so the helper must be an ordinary class in an ordinary package.</p>
 */
public final class MappedWorldVersion implements WorldVersion {

    private final WorldVersion delegate;

    public MappedWorldVersion(WorldVersion delegate) {
        this.delegate = delegate;
    }

    @Override
    public DataVersion dataVersion() {
        return delegate.dataVersion();
    }

    @Override
    public String id() {
        return delegate.id();
    }

    @Override
    public String name() {
        return ClassicVersion.map(delegate.name());
    }

    @Override
    public int protocolVersion() {
        return delegate.protocolVersion();
    }

    @Override
    public PackFormat packVersion(PackType type) {
        return delegate.packVersion(type);
    }

    @Override
    public Date buildTime() {
        return delegate.buildTime();
    }

    @Override
    public boolean stable() {
        return delegate.stable();
    }
}
