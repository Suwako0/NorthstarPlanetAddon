package com.suwako.northstarplanetaddon.moon.client.animation;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.suwako.northstarplanetaddon.moon.MoonModule;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Client resource loader for the mod's editable entity-animation JSON files.
 * <p>
 * Minecraft's native {@link AnimationDefinition} is a Java object and does not automatically load
 * JSON files. This listener turns JSON files below {@code assets/<namespace>/animations} into the
 * same animation definitions consumed by {@code HierarchicalModel#animate}, so resource packs and F3+T
 * can replace the cute rabbit action without code changes.
 */
public final class NMAAnimationResources extends SimpleJsonResourceReloadListener {

    public static final ResourceLocation MOON_RABBIT_GOLDEN_CARROT = ResourceLocation.fromNamespaceAndPath(
            MoonModule.NAMESPACE, "moon_rabbit/golden_carrot_cute");
    public static final ResourceLocation MOON_RABBIT_GOLDEN_CARROT_EAT = ResourceLocation.fromNamespaceAndPath(
            MoonModule.NAMESPACE, "moon_rabbit/golden_carrot_eat");

    private static volatile Map<ResourceLocation, AnimationDefinition> animations = Map.of();

    public NMAAnimationResources() {
        super(new Gson(), "animations");
    }

    public static AnimationDefinition get(ResourceLocation id) {
        return animations.get(id);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        ImmutableMap.Builder<ResourceLocation, AnimationDefinition> loaded = ImmutableMap.builder();
        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            try {
                loaded.put(entry.getKey(), parse(entry.getValue().getAsJsonObject()));
            } catch (RuntimeException exception) {
                MoonModule.LOGGER.error("Could not load entity animation {}.", entry.getKey(), exception);
            }
        }
        animations = loaded.buildKeepingLast();
        MoonModule.LOGGER.debug("Loaded {} editable entity animations.", animations.size());
    }

    private static AnimationDefinition parse(JsonObject root) {
        float length = requiredFloat(root, "length");
        if (length <= 0.0F) {
            throw new IllegalArgumentException("Animation length must be positive");
        }

        AnimationDefinition.Builder builder = AnimationDefinition.Builder.withLength(length);
        if (root.has("looping") && root.get("looping").getAsBoolean()) {
            builder.looping();
        }

        JsonObject bones = requiredObject(root, "bones");
        for (Map.Entry<String, JsonElement> boneEntry : bones.entrySet()) {
            JsonObject channels = boneEntry.getValue().getAsJsonObject();
            parseChannel(builder, boneEntry.getKey(), channels, "rotation", AnimationChannel.Targets.ROTATION);
            parseChannel(builder, boneEntry.getKey(), channels, "position", AnimationChannel.Targets.POSITION);
            parseChannel(builder, boneEntry.getKey(), channels, "scale", AnimationChannel.Targets.SCALE);
        }
        return builder.build();
    }

    private static void parseChannel(AnimationDefinition.Builder builder, String bone, JsonObject channels,
                                     String name, AnimationChannel.Target target) {
        if (!channels.has(name)) {
            return;
        }

        JsonArray entries = channels.getAsJsonArray(name);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("Animation channel " + bone + "/" + name + " is empty");
        }

        List<Keyframe> keyframes = new ArrayList<>(entries.size());
        for (JsonElement entry : entries) {
            JsonObject frame = entry.getAsJsonObject();
            float time = requiredFloat(frame, "time");
            float x = requiredFloat(frame, "x");
            float y = requiredFloat(frame, "y");
            float z = requiredFloat(frame, "z");
            AnimationChannel.Interpolation interpolation = frame.has("interpolation")
                    && "linear".equalsIgnoreCase(frame.get("interpolation").getAsString())
                    ? AnimationChannel.Interpolations.LINEAR
                    : AnimationChannel.Interpolations.CATMULLROM;
            keyframes.add(new Keyframe(time, vectorFor(target, x, y, z), interpolation));
        }
        builder.addAnimation(bone, new AnimationChannel(target, keyframes.toArray(Keyframe[]::new)));
    }

    private static org.joml.Vector3f vectorFor(AnimationChannel.Target target, float x, float y, float z) {
        if (target == AnimationChannel.Targets.ROTATION) {
            return KeyframeAnimations.degreeVec(x, y, z);
        }
        if (target == AnimationChannel.Targets.POSITION) {
            return KeyframeAnimations.posVec(x, y, z);
        }
        return KeyframeAnimations.scaleVec(x, y, z);
    }

    private static JsonObject requiredObject(JsonObject object, String name) {
        if (!object.has(name) || !object.get(name).isJsonObject()) {
            throw new IllegalArgumentException("Missing object field " + name);
        }
        return object.getAsJsonObject(name);
    }

    private static float requiredFloat(JsonObject object, String name) {
        if (!object.has(name) || !object.get(name).isJsonPrimitive()) {
            throw new IllegalArgumentException("Missing numeric field " + name);
        }
        return object.get(name).getAsFloat();
    }
}
