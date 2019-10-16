package net.minecraftforge.client.model.generators;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import net.minecraft.resources.ResourcePackType;
import net.minecraft.resources.VanillaPack;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.nio.file.*;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class ExistingFileHelper {
    private final List<Function<Path, Path>> resolve;
    private final boolean enable;
    private final VanillaPack vanillaResources;

    public ExistingFileHelper(Collection<Path> inputs, boolean enable) {
        this.enable = enable;
        this.vanillaResources = new VanillaPack("minecraft");
        ImmutableList.Builder<Function<Path, Path>> resolve = ImmutableList.builder();
        for (Path base:inputs) {
            if (Files.isRegularFile(base)) {
                try {
                    FileSystem jarSystem = FileSystems.newFileSystem(base, null);
                    resolve.add(p -> jarSystem.getPath('/'+p.toString()));
                } catch (IOException x) {
                    throw new RuntimeException("Failed to create file system for "+base, x);
                }
            } else {
                Preconditions.checkArgument(Files.isDirectory(base));
                resolve.add(base::resolve);
            }
        }
        this.resolve = resolve.build();
    }

    public boolean exists(ResourceLocation loc, ResourcePackType type, String pathSuffix, String pathPrefix) {
        if (!enable) {
            return true;
        }
        ResourceLocation fullLoc = new ResourceLocation(loc.getNamespace(), pathPrefix+"/"+loc.getPath()+pathSuffix);
        if (vanillaResources.resourceExists(type, fullLoc)) {
            return true;
        }
        Path subPath = Paths.get(type.getDirectoryName(), loc.getNamespace(), pathPrefix, loc.getPath()+pathSuffix);
        for (Function<Path, Path> resolver:resolve) {
            Path inBase = resolver.apply(subPath);
            if (Files.exists(inBase)) {
                return true;
            }
        }
        return false;
    }
}
