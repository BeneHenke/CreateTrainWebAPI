package eu.cronmoth.createtrainwebapi.rendering;

import com.github.benmanes.caffeine.cache.LoadingCache;
import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.map.hires.TileModelView;
import de.bluecolored.bluemap.core.map.hires.block.BlockRenderer;
import de.bluecolored.bluemap.core.map.hires.block.BlockRendererType;
import de.bluecolored.bluemap.core.map.hires.block.BlockStateModelRenderer;
import de.bluecolored.bluemap.core.map.hires.entity.EntityRenderer;
import de.bluecolored.bluemap.core.map.hires.entity.EntityRendererType;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.entitystate.Part;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.Entity;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;
import eu.cronmoth.createtrainwebapi.rendering.entitymodel.ContraptionEntity;

public class ContraptionEntityRenderer implements EntityRenderer {
    public static final EntityRendererType TYPE = new EntityRendererType.Impl(new Key("create","contraption"), ContraptionEntityRenderer::new);
    private final LoadingCache<BlockRendererType, BlockRenderer> blockRenderers = null;
    private ResourcePack resourcePack;
    private final ThreadLocal<BlockStateModelRenderer> threadLocalBlockRenderer;


    public ContraptionEntityRenderer(ResourcePack resourcePack, TextureGallery textureGallery, RenderSettings renderSettings) {
        this.resourcePack = resourcePack;
//        this.blockRenderers = Caffeine.newBuilder()
//                .build(type -> type.create(resourcePack, textureGallery, renderSettings));
        this.threadLocalBlockRenderer = ThreadLocal.withInitial(() -> new BlockStateModelRenderer(resourcePack, textureGallery, renderSettings));
    }

    @Override
    public void render(Entity entity, BlockNeighborhood block, Part part, TileModelView tileModel) {
        System.out.println("inside ContraptionEntityRenderer");
        if (!(entity instanceof ContraptionEntity)) {
            return;
        }
        ContraptionEntity contraption = (ContraptionEntity) entity;
    }
}
