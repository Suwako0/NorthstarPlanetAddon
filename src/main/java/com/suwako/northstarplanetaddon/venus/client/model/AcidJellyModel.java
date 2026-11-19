package com.suwako.northstarplanetaddon.venus.client.model;

import com.suwako.northstarplanetaddon.venus.entity.AcidJellyEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 酸潮水母模型：伞盖 + 四根触手。
 * 贴图 64×64：{@code assets/phosphoros/textures/entity/acid_jelly.png}。
 */
public class AcidJellyModel extends EntityModel<AcidJellyEntity> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("northstar_planet_addon", "acid_jelly"), "main");

    private final ModelPart root;
    private final ModelPart bell;
    private final ModelPart[] tentacles;

    public AcidJellyModel(ModelPart root) {
        this.root = root;
        this.bell = root.getChild("bell");
        this.tentacles = new ModelPart[]{
                this.bell.getChild("tentacle_1"),
                this.bell.getChild("tentacle_2"),
                this.bell.getChild("tentacle_3"),
                this.bell.getChild("tentacle_4")
        };
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition bell = root.addOrReplaceChild("bell",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-5.0F, -8.0F, -5.0F, 10.0F, 8.0F, 10.0F),
                PartPose.offset(0.0F, 8.0F, 0.0F));

        int[][] offsets = {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}};
        for (int i = 0; i < offsets.length; i++) {
            bell.addOrReplaceChild("tentacle_" + (i + 1),
                    CubeListBuilder.create().texOffs(0, 20)
                            .addBox(-0.5F, 0.0F, -0.5F, 1.0F, 9.0F, 1.0F),
                    PartPose.offset(offsets[i][0], 0.0F, offsets[i][1]));
        }

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(AcidJellyEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float pulse = 1.0F + Mth.sin(ageInTicks * 0.2F) * 0.08F;
        this.bell.xScale = pulse;
        this.bell.zScale = pulse;
        this.bell.yScale = 2.0F - pulse;

        for (int i = 0; i < this.tentacles.length; i++) {
            this.tentacles[i].xRot = Mth.sin(ageInTicks * 0.15F + i) * 0.25F;
            this.tentacles[i].zRot = Mth.cos(ageInTicks * 0.13F + i) * 0.2F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
