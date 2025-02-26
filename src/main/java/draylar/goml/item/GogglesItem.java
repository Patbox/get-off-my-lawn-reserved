package draylar.goml.item;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketItem;
import dev.emi.trinkets.api.TrinketsApi;
import draylar.goml.GetOffMyLawn;
import draylar.goml.api.ClaimUtils;
import draylar.goml.api.WorldParticleUtils;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;
import net.minecraft.item.equipment.ArmorMaterials;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.Registry;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.stream.Collectors;

public class GogglesItem extends TrinketItem implements PolymerItem {
    public GogglesItem(Item.Settings settings) {
        super(settings.maxCount(1));
        TrinketsApi.registerTrinket(this, this);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (entity instanceof ServerPlayerEntity player && (selected || player.getEquippedStack(EquipmentSlot.OFFHAND) == stack)) {
            if (player.age % 70 == 0) {
                var distance = player.getServer().getPlayerManager().getViewDistance() * 16;

                ClaimUtils.getClaimsInBox(
                        world,
                        entity.getBlockPos().add(-distance, -distance, -distance),
                        entity.getBlockPos().add(distance, distance, distance)).forEach(
                        claim -> {
                            var box = claim.getKey().toBox();
                            var minPos = new BlockPos(box.x1(), Math.max(box.y1(), world.getBottomY()), box.z1());
                            var maxPos = new BlockPos(box.x2() - 1, Math.min(box.y2() - 1, world.getTopYInclusive()), box.z2() - 1);

                            BlockState state = ClaimUtils.gogglesClaimColor(claim.getValue());

                            WorldParticleUtils.render(player, minPos, maxPos,
                                    //new DustParticleEffect(new Vec3f(0.8f, 0.8f, 0.8f), 2)
                                    new BlockStateParticleEffect(ParticleTypes.BLOCK_MARKER, state)
                            );
                        });
            }
        }
    }

    @Override
    public void tick(ItemStack stack, SlotReference slot, LivingEntity entity) {
        if (entity instanceof ServerPlayerEntity player) {
            if (player.age % 70 == 0) {
                var distance = player.getServer().getPlayerManager().getViewDistance() * 16;

                var world = player.getWorld();

                ClaimUtils.getClaimsInBox(
                        world,
                        entity.getBlockPos().add(-distance, -distance, -distance),
                        entity.getBlockPos().add(distance, distance, distance)).forEach(
                        claim -> {
                            var box = claim.getKey().toBox();
                            var minPos = new BlockPos(box.x1(), Math.max(box.y1(), world.getBottomY()), box.z1());
                            var maxPos = new BlockPos(box.x2() - 1, Math.min(box.y2() - 1, world.getTopYInclusive()), box.z2() - 1);

                            BlockState state = ClaimUtils.gogglesClaimColor(claim.getValue());

                            WorldParticleUtils.render(player, minPos, maxPos,
                                    //new DustParticleEffect(new Vec3f(0.8f, 0.8f, 0.8f), 2)
                                    new BlockStateParticleEffect(ParticleTypes.BLOCK_MARKER, state)
                            );
                        });
            }
        }
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.IRON_INGOT;
    }

    @Override
    public Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
        return PolymerResourcePackUtils.hasMainPack(context) ? PolymerItem.super.getPolymerItemModel(stack, context) : null;
    }
}
