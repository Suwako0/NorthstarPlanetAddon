package com.suwako.northstarplanetaddon.mercury.client.model;

import com.suwako.northstarplanetaddon.mercury.MercuryModule;
import com.suwako.northstarplanetaddon.mercury.entity.SunfishAttackMode;
import com.suwako.northstarplanetaddon.mercury.entity.SunfishEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 太阳鱼（Sunfish, sky-fish of Mercury）: the calm, pale-gold breed.
 * <p>
 * Silhouette from the concept sheet: a long serpentine body that dips through the middle and lifts the
 * tail, a horned dragon head with a long tapered snout, eleven pale dorsal spines, a mane of small
 * frills down the nape, two modest three-lobed pectoral fins and a tail that dissolves into four flame
 * streamers. The whole body is stepped integer voxels, so the texture bands read as ribbed scales.
 * <p>
 * Geometry lives in the generated {@link SunfishMesh} (built from {@code tools/entity-specs/sunfish.bbmodel});
 * this class owns the layer, the 128x128 sheet and the calm motion language. Regenerate the mesh with
 * {@code node tools/gen-entity-model.mjs tools/entity-specs/sunfish.bbmodel ... SunfishMesh}.
 */
public class SunfishModel<T extends SunfishEntity> extends SkyfishModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MercuryModule.NAMESPACE, "sunfish"), "main");

    /** 128x128 sheet; the UV slots come from the packed Blockbench project. */
    public static final int TEXTURE_WIDTH = 128;
    public static final int TEXTURE_HEIGHT = 128;

    public SunfishModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        SunfishMesh.build(mesh);
        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    // ---- 缓慢、轻盈的漂浮节奏：整条鱼像浸在水里，波形几乎看不出起伏 ----

    @Override
    protected float bobSpeed() {
        return 0.05F;
    }

    @Override
    protected float bobAmplitude() {
        return 0.5F;
    }

    @Override
    protected float swaySpeed() {
        return 0.045F;
    }

    @Override
    protected float swayAmplitude() {
        return 0.05F;
    }

    @Override
    protected float waveSpeed() {
        return 0.085F;
    }

    @Override
    protected float waveAmplitude() {
        return 0.18F;
    }

    @Override
    protected float finSpeed() {
        return 0.06F;
    }

    @Override
    protected float finAmplitude() {
        return 0.22F;
    }

    @Override
    protected float finRestAngle() {
        return 0.16F;
    }

    @Override
    protected float diveSpinSpeed() {
        return 0.95F;
    }

    /**
     * 太阳鱼的待机细节：角冠与颌部极轻地摆，远端尾焰须的相位已经交给基类；
     * 这里只补一条更慢的“呼吸”波，让静止悬停时也不会像冻结的模型。
     */
    @Override
    protected void animateSpecies(T entity, float time, SunfishAttackMode mode) {
        float breath = Mth.sin(time * 0.035F);
        this.hornLeft.xRot = breath * 0.05F;
        this.hornRight.xRot = breath * 0.05F;
        this.hornLeftMid.xRot = Mth.sin(time * 0.035F - 0.4F) * 0.04F;
        this.hornRightMid.xRot = Mth.sin(time * 0.035F - 0.4F) * 0.04F;
        this.hornLeftTip.zRot = Mth.cos(time * 0.03F) * 0.04F;
        this.hornRightTip.zRot = -Mth.cos(time * 0.03F) * 0.04F;

        // 缓慢眨眼式的颌部开合：很小，但让头部不是死的。
        this.jaw.xRot += Mth.sin(time * 0.05F + 1.3F) * 0.02F;

        if (mode == SunfishAttackMode.WING_FLAP) {
            // 拍翅散射时头微微上仰、角冠后压，和炽热太阳鱼射线蓄力形成对照。
            this.hornLeft.xRot -= 0.12F;
            this.hornRight.xRot -= 0.12F;
            this.jaw.xRot = 0.12F;
        }
    }
}
