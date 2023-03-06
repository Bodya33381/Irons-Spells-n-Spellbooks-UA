package com.example.testmod.player;

import com.example.testmod.TestMod;
import com.example.testmod.spells.AbstractSpell;
import com.example.testmod.spells.CastSource;
import com.example.testmod.spells.SpellType;
import com.example.testmod.spells.ender.TeleportSpell;
import com.example.testmod.util.ParticleHelper;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

import static com.example.testmod.spells.AbstractSpell.ANIMATION_RESOURCE;

public class ClientSpellCastHelper {
    /**
     * Network Handling Wrapper
     */
    public static void handleClientboundOnClientCast(int spellId, int level, CastSource castSource) {
        var spell = AbstractSpell.getSpell(spellId, level);
        spell.onClientCastComplete(Minecraft.getInstance().player.level, Minecraft.getInstance().player, null);
    }

    public static void handleClientboundTeleport(Vec3 pos1, Vec3 pos2) {
        var player = Minecraft.getInstance().player;

        if (player != null) {
            var level = Minecraft.getInstance().player.level;
            TeleportSpell.particleCloud(level, player, pos1);
            TeleportSpell.particleCloud(level, player, pos2);
        }
    }

    /**
     * Right Click Suppression
     */
    private static boolean suppressRightClicks;

    public static boolean shouldSuppressRightClicks() {
        return suppressRightClicks;
    }

    public static void setSuppressRightClicks(boolean suppressRightClicks) {
        ClientSpellCastHelper.suppressRightClicks = suppressRightClicks;
    }

    /**
     * Particles
     */
    public static void handleClientboundBloodSiphonParticles(Vec3 pos1, Vec3 pos2) {
        if (Minecraft.getInstance().player == null)
            return;
        var level = Minecraft.getInstance().player.level;
        Vec3 direction = pos2.subtract(pos1).scale(.1f);
        for (int i = 0; i < 40; i++) {
            Vec3 scaledDirection = direction.scale(1 + getRandomScaled(.35));
            Vec3 random = new Vec3(getRandomScaled(.08f), getRandomScaled(.08f), getRandomScaled(.08f));
            level.addParticle(ParticleHelper.BLOOD, pos1.x, pos1.y, pos1.z, scaledDirection.x + random.x, scaledDirection.y + random.y, scaledDirection.z + random.z);
        }

    }

    public static void handleClientboundHealParticles(Vec3 pos) {
        //Copied from arrow because these particles use their motion for color??
        var player = Minecraft.getInstance().player;

        if (player != null) {
            var level = Minecraft.getInstance().player.level;
            int i = PotionUtils.getColor(Potion.byName("healing"));
            double d0 = (double) (i >> 16 & 255) / 255.0D;
            double d1 = (double) (i >> 8 & 255) / 255.0D;
            double d2 = (double) (i >> 0 & 255) / 255.0D;

            for (int j = 0; j < 15; ++j) {
                level.addParticle(ParticleTypes.ENTITY_EFFECT, pos.x + getRandomScaled(0.25D), pos.y + getRandomScaled(1), pos.z + getRandomScaled(0.25D), d0, d1, d2);
            }
        }
    }

    public static void handleClientboundRegenCloudParticles(Vec3 pos) {
        var player = Minecraft.getInstance().player;

        if (player != null) {
            var level = player.level;
            int ySteps = 16;
            int xSteps = 48;
            float yDeg = 180 / ySteps * Mth.DEG_TO_RAD;
            float xDeg = 360 / xSteps * Mth.DEG_TO_RAD;
            for (int x = 0; x < xSteps; x++) {
                for (int y = 0; y < ySteps; y++) {
                    Vec3 offset = new Vec3(0, 0, 5).yRot(y * yDeg).xRot(x * xDeg).zRot(-Mth.PI / 2).multiply(1, .85f, 1);
                    level.addParticle(DustParticleOptions.REDSTONE, pos.x + offset.x, pos.y + offset.y, pos.z + offset.z, 0, 0, 0);
                }
            }
        }
    }

    public static void handleClientBoundOnCastStarted(UUID castingEntityId, SpellType spellType) {
        var player = Minecraft.getInstance().player.level.getPlayerByUUID(castingEntityId);
        if (player != null) {
            TestMod.LOGGER.debug("handleClientBoundOnCastStarted {} {}", player, spellType);
            var spell = AbstractSpell.getSpell(spellType, 1);
            var keyframeAnimation = spell.keyFrameAnimationOf(spell.getCastAnimation(player));
            keyframeAnimation.ifPresent((keyFrame) -> {
                if (keyFrame != null) {
                    var animation = (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData((AbstractClientPlayer) player).get(ANIMATION_RESOURCE);
                    if (animation != null) {
                        ClientMagicData.castingAnimationPlayer = new KeyframeAnimationPlayer(keyFrame);
                        ClientMagicData.castingAnimationPlayer.setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
                        ClientMagicData.castingAnimationPlayer.setFirstPersonConfiguration(new FirstPersonConfiguration().setShowLeftArm(true).setShowRightArm(true));
                        animation.setAnimation(ClientMagicData.castingAnimationPlayer);

                        //You might use  animation.replaceAnimationWithFade(); to create fade effect instead of sudden change
                    }
                }
            });
        }
    }

    private static double getRandomScaled(double scale) {
        return (2.0D * Math.random() - 1.0D) * scale;
    }
}
