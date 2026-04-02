package draylar.goml.block.augment;

import com.mojang.authlib.GameProfile;
import draylar.goml.GetOffMyLawn;
import draylar.goml.api.Claim;
import draylar.goml.api.ClaimUtils;
import draylar.goml.api.DataKey;
import draylar.goml.block.ClaimAugmentBlock;
import draylar.goml.registry.GOMLTextures;
import draylar.goml.ui.GenericPlayerListGui;
import draylar.goml.ui.GenericPlayerSelectionGui;
import draylar.goml.ui.PagedGui;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class ForceFieldAugmentBlock extends ClaimAugmentBlock {

    public static final DataKey<Set<UUID>> UUID_KEY = DataKey.ofUuidSet(GetOffMyLawn.id("force_field/uuids"));
    public static final DataKey<Boolean> WHITELIST_KEY = DataKey.ofBoolean(GetOffMyLawn.id("force_field/whitelist"), true);

    public ForceFieldAugmentBlock(Properties settings, String texture) {
        super(settings, texture);
    }

    @Override
    public void onPlayerEnter(Claim claim, Player player) {
        if (shouldBlock(claim, player) && claim.getClaimBox().minecraftBox().contains(player.position())) {
            Tuple<Vec3, Direction> pair = ClaimUtils.getClosestXZBorder(claim, player.position(), 2);
            int distance = 0;
            while (true) {
                var i = shouldBlock(player.level(), pair.getA(), player);

                if (i == -1) {
                    break;
                }
                distance += i;
                pair = ClaimUtils.getClosestXZBorder(claim, player.position(), 2 + distance);
            }


            var pairPart = ClaimUtils.getClosestXZBorder(claim, player.position(), distance);

            var pos = pair.getA();
            var dir = pair.getB();
            var pos2 = pairPart.getA();

            var dir2 = pairPart.getB();

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    ((ServerLevel) player.level()).sendParticles(
                            (ServerPlayer) player, new BlockParticleOption(ParticleTypes.BLOCK_MARKER, Blocks.BARRIER.defaultBlockState()), true, true,
                            pos2.x + dir2.getStepZ() * x, player.getEyeY() + y, pos2.z + dir2.getStepX() * x,
                            1,
                            0.0, 0.0, 0.0,
                            0.0
                    );
                }
            }

            double y;
            if (player.level().noCollision(player, player.getDimensions(player.getPose()).makeBoundingBox(pos.x, player.getY(), pos.z))) {
                y = player.getY();
            } else {
                y = player.level().getHeight(Heightmap.Types.MOTION_BLOCKING, (int) pos.x, (int) pos.z);
            }

            player.randomTeleport(pos.x, y, pos.z, true);

            player.setDeltaMovement(Vec3.atLowerCornerOf(dir.getUnitVec3i()).scale(0.2));

            if (player.isPassenger()) {
                player.getVehicle().teleportTo((ServerLevel) player.getVehicle().level(), pos.x, y, pos.z, Relative.ALL, player.getVehicle().getYRot(), player.getVehicle().getXRot(), false);
                player.getVehicle().setDeltaMovement(Vec3.atLowerCornerOf(dir.getUnitVec3i()).scale(0.2));
            }


            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(serverPlayer));

                if (player.isPassenger()) {
                    serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(player.getVehicle()));
                }
            }
        }
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public void playerTick(Claim claim, Player player) {
        onPlayerEnter(claim, player);
    }

    @Override
    public boolean ticks() {
        return true;
    }

    public boolean shouldBlock(Claim claim, Player player) {
        var uuids = claim.getData(UUID_KEY);

        if (claim.hasPermission(player) || ClaimUtils.isInAdminMode(player)) {
            return false;
        }
        var doThing = uuids.contains(player.getUUID());

        if (claim.getData(WHITELIST_KEY)) {
            doThing = !doThing;
        }

        return doThing;
    }


    private int shouldBlock(Level world, Vec3 pos, Player player) {
        var x = ClaimUtils.getClaimsAt(world, BlockPos.containing(pos))
                .filter(entry -> entry.getValue().hasAugment(this) && shouldBlock(entry.getValue(), player)).collect(Collectors.toList());

        return x.isEmpty() ? -1 : x.get(0).getValue().getRadius();
    }

    @Override
    public void openSettings(Claim claim, ServerPlayer player, @Nullable Runnable closeCallback) {
        var gui = new SimpleGui(MenuType.HOPPER, player, false) {
            boolean ingore = false;

            @Override
            public void onManualClose() {
                if (closeCallback != null && !this.ingore) {
                    closeCallback.run();
                }
            }
        };

        gui.setTitle(this.getGuiName());
        {
            var change = new MutableObject<Runnable>();
            change.setValue(() -> {
                var currentMode = claim.getData(WHITELIST_KEY).booleanValue();
                gui.setSlot(0, new GuiElementBuilder(currentMode ? Items.WHITE_WOOL : Items.BLACK_WOOL)
                        .setName(Component.translatable("text.goml.gui.force_field.whitelist_mode", CommonComponents.optionStatus(currentMode)))
                        .addLoreLine(Component.translatable("text.goml.mode_toggle.help").withStyle(ChatFormatting.GRAY))
                        .setCallback(() -> {
                            PagedGui.playClickSound(player);
                            claim.setData(WHITELIST_KEY, !currentMode);
                            change.getValue().run();
                        })
                );
            });

            change.getValue().run();
        }
        {
            var change = new MutableObject<Runnable>();
            change.setValue(() -> {
                gui.setSlot(1, new GuiElementBuilder(Items.PLAYER_HEAD)
                        .setName(Component.translatable("text.goml.gui.force_field.player_list"))
                        .setCallback(() -> {
                            PagedGui.playClickSound(player);
                            gui.ingore = true;
                            gui.close(true);
                            gui.ingore = false;
                            new ListGui(player, claim, gui::open);
                        })
                );
            });

            change.getValue().run();
        }

        gui.setSlot(4, new GuiElementBuilder(Items.STRUCTURE_VOID)
                .setName(Component.translatable(closeCallback != null ? "text.goml.gui.back" : "text.goml.gui.close").withStyle(ChatFormatting.RED))
                .setCallback(() -> {
                    PagedGui.playClickSound(player);
                    gui.close(closeCallback != null);
                })
        );

        while (gui.getFirstEmptySlot() != -1) {
            gui.addSlot(PagedGui.DisplayElement.filler().element());
        }

        gui.open();
    }

    private class ListGui extends GenericPlayerListGui {
        private final Claim claim;

        public ListGui(ServerPlayer player, Claim claim, @Nullable Runnable onClose) {
            super(player, onClose);
            this.setTitle(Component.translatable("text.goml.gui.force_field.player_list"));
            this.claim = claim;
            this.updateDisplay();
            this.open();
        }

        @Override
        protected void updateDisplay() {
            this.uuids.clear();
            this.uuids.addAll(this.claim.getData(UUID_KEY));
            super.updateDisplay();
        }

        @Override
        protected DisplayElement getNavElement(int id) {
            return switch (id) {
                case 5 -> DisplayElement.of(new GuiElementBuilder(Items.PLAYER_HEAD)
                        .setName(Component.translatable("text.goml.gui.player_list.add_player").withStyle(ChatFormatting.GREEN))
                        .setProfileSkinTexture(GOMLTextures.GUI_ADD)
                        .setCallback(() -> {
                            playClickSound(this.player);
                            this.ignoreCloseCallback = true;
                            this.close(true);
                            this.ignoreCloseCallback = false;

                            new GenericPlayerSelectionGui(
                                    this.player,
                                    Component.translatable("text.goml.gui.force_field.add_player.title"),
                                    (p) -> !this.claim.hasDirectPermission(p.id()) && !this.claim.getData(UUID_KEY).contains(p.id()),
                                    (p) -> this.claim.getData(UUID_KEY).add(p.id()),
                                    this::refreshOpen).updateAndOpen();
                        }));
                default -> super.getNavElement(id);
            };
        }

        @Override
        protected void modifyBuilder(GuiElementBuilder builder, Optional<NameAndId> optional, UUID uuid) {
            builder.addLoreLine(Component.translatable("text.goml.gui.click_to_remove"));
            builder.setCallback(() -> {
                playClickSound(player);
                this.claim.getData(UUID_KEY).remove(uuid);
                this.updateDisplay();
            });

        }
    }
}
