package com.mengsama.mod.mengsamanetmusic.compat;

import net.minecraftforge.fml.ModList;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;

public class SophisticatedBackpacksCompat {
    public static final String SC = "sophisticatedbackpacks";
    public static final VersionRange SC_VERSION_RANGE;

    static {
        VersionRange range;
        try {
            range = VersionRange.createFromVersionSpec("[1.0.0,)");
        } catch (org.apache.maven.artifact.versioning.InvalidVersionSpecificationException e) {
            range = VersionRange.createFromVersion("1.0.0");
        }
        SC_VERSION_RANGE = range;
    }

    public static void register() {
        ModList.get().getModContainerById(SC).ifPresent(modContainer -> {
            ArtifactVersion version = modContainer.getModInfo().getVersion();
            if (SC_VERSION_RANGE.containsVersion(version)) {

            }
        });
    }
}
