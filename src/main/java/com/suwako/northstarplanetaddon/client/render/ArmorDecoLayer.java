package com.suwako.northstarplanetaddon.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.suwako.northstarplanetaddon.NorthstarPlanetAddon;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * 玩家身上的 3D 盔甲装饰层。
 *
 * <p>原版 {@code HumanoidArmorLayer} 每个部位只画一次、只绑一张贴图，所以它没法同时画
 * 「64x32 的 layer 甲面」和「128x128 的装饰件」。这里在它之后追加一层：甲面继续由原版画，
 * 装饰件（冠、角、肩甲、披风、腕箍、腰甲、胫甲）由本层用 {@code <set>_armor.png} 画。</p>
 *
 * <p>这样脸和手仍然是玩家皮肤（甲面贴图的开口），而 3D 部件挂在
 * {@code head/body/rightArm/leftArm/rightLeg/leftLeg} 骨头上，跟着转头和摆臂一起动。</p>
 */
public final class ArmorDecoLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final Map<Item, Deco> DECOS = new HashMap<>();

    /** 一件 3D 盔甲：模型 + 128x128 图集。 */
    private record Deco(HumanoidModel<LivingEntity> model, ResourceLocation texture) {
    }

    public ArmorDecoLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer) {
        super(renderer);
    }

    /** 把某个部件的物品登记到装饰层；同一套的 4 件共用一个模型实例。 */
    public static void register(Item item, HumanoidModel<LivingEntity> model, String set) {
        DECOS.put(item, new Deco(model, ResourceLocation.fromNamespaceAndPath(
                NorthstarPlanetAddon.MODID, "textures/models/armor/" + set + "_armor.png")));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (DECOS.isEmpty()) {
            return;
        }
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            Deco deco = DECOS.get(stack.getItem());
            if (deco == null || !(deco.model() instanceof ArmorDecoModel decoModel)) {
                continue;
            }
            decoModel.setSlot(typeOf(slot));
            copyPose(this.getParentModel(), deco.model());
            VertexConsumer consumer = buffer.getBuffer(RenderType.armorCutoutNoCull(deco.texture()));
            deco.model().renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        }
    }

    /** 把玩家模型的骨骼姿态（含潜行、骑乘、游泳）复制到装饰模型上。 */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void copyPose(HumanoidModel<AbstractClientPlayer> from, HumanoidModel<LivingEntity> to) {
        ((HumanoidModel) from).copyPropertiesTo((HumanoidModel) to);
    }

    private static ArmorItem.Type typeOf(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> ArmorItem.Type.HELMET;
            case CHEST -> ArmorItem.Type.CHESTPLATE;
            case LEGS -> ArmorItem.Type.LEGGINGS;
            case FEET -> ArmorItem.Type.BOOTS;
            default -> ArmorItem.Type.BODY;
        };
    }
}
