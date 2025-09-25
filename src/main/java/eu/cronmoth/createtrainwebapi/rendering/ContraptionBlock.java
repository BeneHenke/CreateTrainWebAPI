package eu.cronmoth.createtrainwebapi.rendering;

import de.bluecolored.bluemap.core.world.BlockEntity;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.LightData;
import de.bluecolored.bluemap.core.world.biome.Biome;
import de.bluecolored.bluemap.core.world.block.BlockAccess;
import org.jetbrains.annotations.Nullable;

public class ContraptionBlock implements BlockAccess {
    @Override
    public void set(int x, int y, int z) {

    }

    @Override
    public BlockAccess copy() {
        return null;
    }

    @Override
    public int getX() {
        return 0;
    }

    @Override
    public int getY() {
        return 0;
    }

    @Override
    public int getZ() {
        return 0;
    }

    @Override
    public BlockState getBlockState() {
        return null;
    }

    @Override
    public LightData getLightData() {
        return null;
    }

    @Override
    public Biome getBiome() {
        return null;
    }

    @Override
    public @Nullable BlockEntity getBlockEntity() {
        return null;
    }

    @Override
    public boolean hasOceanFloorY() {
        return false;
    }

    @Override
    public int getOceanFloorY() {
        return 0;
    }
}
