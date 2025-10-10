package draylar.goml.other;

import java.net.URI;
import java.security.Permission;

import com.jamieswhiteshirt.rtree3i.RTreeMap;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import draylar.goml.GetOffMyLawn;
import draylar.goml.api.Claim;
import draylar.goml.api.ClaimBox;
import draylar.goml.api.ClaimUtils;
import draylar.goml.api.DataKey;
import draylar.goml.api.event.ClaimEvents;
import draylar.goml.config.GOMLConfig;
import draylar.goml.registry.GOMLEntities;
import draylar.goml.ui.ClaimListGui;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Blocks;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

@ApiStatus.Internal
public class ClaimCommand {

    private ClaimCommand() {
        // NO-OP
    }

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            dispatcher.register(literal("goml")
                    .then(literal("help")
                            .requires(Permissions.require("goml.command.command.help", true))
                            .executes(ClaimCommand::help)
                    )
                    .then(literal("trust")
                            .requires(Permissions.require("goml.command.command.trust", true))
                            .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                    .executes(context -> trust(context, false))
                            )
                    )
                    .then(literal("untrust")
                            .requires(Permissions.require("goml.command.command.untrust", true))
                            .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                    .executes((ctx) -> ClaimCommand.untrust(ctx, false)))
                    )
                    .then(literal("addowner")
                            .requires(Permissions.require("goml.command.command.addowner", true))
                            .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                    .executes(context -> trust(context, true)))
                    )

                    .then(literal("list")
                            .requires(Permissions.require("goml.command.command.list", true))
                            .executes(context -> openList(context, context.getSource().getPlayer().getGameProfile()))
                    )

                    .then(literal("escape")
                            .requires(Permissions.require("goml.command.command.escape", true))
                            .executes(context -> escape(context, context.getSource().getPlayerOrThrow()))
                    )

                    .then(literal("admin")
                            .requires(Permissions.require("goml.command.command.admin", 3))
                            .then(literal("fixaugments")
                                    .requires(Permissions.require("goml.command.command.fixaugments", true))
                                    .executes(ClaimCommand::fixAugments)
                            )
                            .then(literal("escape")
                                    .requires(Permissions.require("goml.command.command.admin.escape", true))
                                    .then(argument("player", EntityArgumentType.player())
                                            .executes(context -> escape(context, EntityArgumentType.getPlayer(context, "player")))
                                    )
                            )

                            .then(literal("adminmode")
                                    .requires(Permissions.require("goml.command.command.admin.admin_mode", 3))
                                    .executes(ClaimCommand::adminMode)
                            )
                            .then(literal("removeowner")
                                    .requires(Permissions.require("goml.command.command.admin.removeowner", true))
                                    .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                            .executes((ctx) -> ClaimCommand.untrust(ctx, true)))
                            )
                            .then(literal("info")
                                    .requires(Permissions.require("goml.command.command.admin.info", 3))
                                    .executes(ClaimCommand::infoAdmin)
                            )
                            .then(literal("world")
                                    .requires(Permissions.require("goml.command.command.admin.world", 3))
                                    .executes(ClaimCommand::world)
                            )
                            .then(literal("general")
                                    .requires(Permissions.require("goml.command.command.admin.general", 3))
                                    .executes(ClaimCommand::general)
                            )
                            .then(literal("remove")
                                    .requires(Permissions.require("goml.command.command.admin.remove", 3))
                                    .executes(ClaimCommand::remove)
                            )
                            .then(literal("reload")
                                    .requires(Permissions.require("goml.command.command.admin.reload", 4))
                                    .executes(ClaimCommand::reload)
                            )
                            .then(literal("list")
                                    .requires(Permissions.require("goml.command.command.list", true))
                                    .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                            .executes(context -> {
                                                var player = GameProfileArgumentType.getProfileArgument(context, "player").toArray(new GameProfile[0]);

                                                if (player.length == 0) {
                                                    context.getSource().sendFeedback(() -> Text.translatable("argument.player.unknown").formatted(Formatting.RED), false);
                                                }

                                                return openList(context, player[0]);
                                            })
                                    )
                            )
                            .then(literal("updateallclaims")
                                    .requires(Permissions.require("goml.command.command.admin.updateallclaims", 4))
                                    .executes(ClaimCommand::updateAllClaims)
                            )
                    )
            );
        });
    }

    private static int fixAugments(CommandContext<ServerCommandSource> context) {
        ClaimUtils.getClaimsAt(context.getSource().getWorld(), BlockPos.ofFloored(context.getSource().getPosition())).forEach(x -> {
            var copy = new ArrayList<>(x.getValue().getAugments().entrySet());

            for (var y : copy) {
                if (context.getSource().getWorld().getBlockState(y.getKey()).getBlock() != y.getValue()) {
                    x.getValue().removeAugment(y.getKey());
                }
            }
        });


        return 0;
    }

    private static int escape(CommandContext<ServerCommandSource> context, ServerPlayerEntity player) {
        var claims = ClaimUtils.getClaimsAt(player.getWorld(), player.getBlockPos()).filter(x -> !x.getValue().hasPermission(player));

        if (claims.isNotEmpty()) {
            claims.forEach((claim) -> {
                if (!claim.getKey().minecraftBox().contains(player.getPos())) {
                    return;
                }

                var pair = ClaimUtils.getClosestXZBorder(claim.getValue(), player.getPos(), 1);

                var pos = pair.getLeft();
                var dir = pair.getRight();

                double y;
                if (player.getWorld().isSpaceEmpty(player, player.getDimensions(player.getPose()).getBoxAt(pos.x, player.getY(), pos.z))) {
                    y = player.getY();
                } else {
                    y = player.getWorld().getTopY(Heightmap.Type.MOTION_BLOCKING, (int) pos.x, (int) pos.z);
                }

                player.teleport(pos.x, y, pos.z, true);

                player.setVelocity(Vec3d.of(dir.getVector()).multiply(0.2));

                if (player.hasVehicle()) {
                    player.getVehicle().teleport((ServerWorld) player.getVehicle().getWorld(), pos.x, y, pos.z, PositionFlag.VALUES, player.getVehicle().getYaw(), player.getVehicle().getPitch(), false);
                    player.getVehicle().setVelocity(Vec3d.of(dir.getVector()).multiply(0.2));
                }


                player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));

                if (player.hasVehicle()) {
                    player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player.getVehicle()));
                }
            });
            context.getSource().sendFeedback(() -> prefix(Text.translatable("text.goml.command.escaped").formatted(Formatting.GREEN)), false);

        } else {
            context.getSource().sendFeedback(() -> prefix(Text.translatable("text.goml.command.cant_escape").formatted(Formatting.RED)), false);

        }

        return 0;
    }

    private static int adminMode(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();

        var newMode = !((GomlPlayer) player).goml_getAdminMode();
        ((GomlPlayer) player).goml_setAdminMode(newMode);
        context.getSource().sendFeedback(() -> prefix(Text.translatable(newMode ? "text.goml.admin_mode.enabled" : "text.goml.admin_mode.disabled")), false);

        return 1;
    }

    /**
     * Sends the player general information about all claims on the server.
     *
     * @param context context
     * @return success flag
     */
    private static int general(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        ServerPlayerEntity player = context.getSource().getPlayer();
        AtomicInteger numberOfClaimsTotal = new AtomicInteger();

        bumpChat(player);

        server.getWorlds().forEach(world -> {
            var worldClaims = GetOffMyLawn.CLAIM.get(world).getClaims();
            int numberOfClaimsWorld = worldClaims.size();
            numberOfClaimsTotal.addAndGet(1);

            player.sendMessage(prefix(Text.translatable("text.goml.command.number_in", world.getRegistryKey().getValue(), numberOfClaimsWorld)), false);
        });

        player.sendMessage(prefix(Text.translatable("text.goml.command.number_all", numberOfClaimsTotal.get()).formatted(Formatting.WHITE)), false);

        return 1;
    }

    /**
     * Sends the player information about the claim they are standing in, if it exists.
     *
     * @param context context
     * @return success flag
     */
    private static int infoAdmin(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerWorld world = context.getSource().getWorld();
        ServerPlayerEntity player = context.getSource().getPlayer();

        if (!world.isClient()) {
            ClaimUtils.getClaimsAt(world, player.getBlockPos()).forEach(claimedArea -> {
                player.sendMessage(prefix(Text.literal("Origin: " + claimedArea.getValue().getOrigin().toShortString())), false);
                player.sendMessage(prefix(Text.literal("Radius: " + claimedArea.getValue().getRadius() + " Height: " + claimedArea.getKey().getY())), false);
                {
                    var owners = Text.literal("Owners: ");

                    {
                        var iter = claimedArea.getValue().getOwners().iterator();

                        while (iter.hasNext()) {
                            var uuid = iter.next();
                            var gameProfile = context.getSource().getServer().getUserCache().getByUuid(uuid);
                            owners.append(Text.literal((gameProfile.isPresent() ? gameProfile.get().getName() : "<unknown>") + " -> " + uuid.toString())
                                    .setStyle(Style.EMPTY
                                            .withColor(Formatting.GRAY)
                                            .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to copy")))
                                            .withClickEvent(new ClickEvent.CopyToClipboard( uuid.toString()))
                                    )
                            );

                            if (iter.hasNext()) {
                                owners.append(", ");
                            }
                        }
                    }

                    var trusted = Text.literal("Trusted: ");

                    {
                        var iter = claimedArea.getValue().getTrusted().iterator();

                        while (iter.hasNext()) {
                            var uuid = iter.next();
                            var gameProfile = context.getSource().getServer().getUserCache().getByUuid(uuid);
                            trusted.append(Text.literal((gameProfile.isPresent() ? gameProfile.get().getName() : "<unknown>") + " -> " + uuid.toString())
                                    .setStyle(Style.EMPTY
                                            .withColor(Formatting.GRAY)
                                            .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to copy")))
                                            .withClickEvent(new ClickEvent.CopyToClipboard(uuid.toString()))
                                    )
                            );

                            if (iter.hasNext()) {
                                owners.append(", ");
                            }
                        }
                    }

                    player.sendMessage(prefix(owners), false);
                    player.sendMessage(prefix(trusted), false);
                }
                player.sendMessage(prefix(Text.literal("ClaimData: ")), false);
                for (var key : (Collection<DataKey<Object>>) (Object) claimedArea.getValue().getDataKeys()) {
                    player.sendMessage(Text.literal("- " + key.key() + " -> ").append(NbtHelper.toPrettyPrintedText(key.serializer().apply(claimedArea.getValue().getData(key)))), false);
                }

            });
        }

        return 1;
    }

    /**
     * Sends the player general information about all claims in the given world.
     *
     * @param context context
     * @return success flag
     */
    private static int world(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerWorld world = context.getSource().getWorld();
        ServerPlayerEntity player = context.getSource().getPlayer();

        var worldClaims = GetOffMyLawn.CLAIM.get(world).getClaims();
        int numberOfClaims = worldClaims.size();

        player.sendMessage(prefix(Text.translatable("text.goml.command.number_in", world.getRegistryKey().getValue(), numberOfClaims)), false);

        return 1;
    }

    /**
     * Removes the claim the player is currently standing in, if it exists.
     *
     * @param context context
     * @return success flag
     */
    private static int remove(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerWorld world = context.getSource().getWorld();
        ServerPlayerEntity player = context.getSource().getPlayer();

        if (!world.isClient()) {
            ClaimUtils.getClaimsAt(world, player.getBlockPos()).forEach(claimedArea -> {
                claimedArea.getValue().destroy();
                player.sendMessage(prefix(Text.translatable("text.goml.command.removed_claim", world.getRegistryKey().getValue(), claimedArea.getValue().getOrigin().toShortString())), false);
                var blockEntity = world.getBlockEntity(claimedArea.getValue().getOrigin(), GOMLEntities.CLAIM_ANCHOR);

                if (blockEntity.isPresent()) {
                    world.breakBlock(claimedArea.getValue().getOrigin(), true);
                    for (var lPos : new ArrayList<>(blockEntity.get().getAugments().keySet())) {
                        world.breakBlock(lPos, true);
                    }
                }

            });
        }

        return 1;
    }

    /**
     * Sends the player information on using the /goml command.
     *
     * @param context context
     * @return success flag
     */
    private static int help(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();

        Function<String, Text> write = (command) -> Text.literal("/goml " + command)
                .append(Text.literal(" - ").formatted(Formatting.GRAY))
                .append(Text.translatable("text.goml.command.help." + command).setStyle(Style.EMPTY.withColor(0xededed)));

        player.sendMessage(Text.literal("[").formatted(Formatting.DARK_GRAY).append(Text.literal("Get Off My Lawn").setStyle(Style.EMPTY.withColor(0xa1ff59))).append("]"), false);
        player.sendMessage(Text.literal("-------------------------------------").formatted(Formatting.DARK_GRAY), false);

        for (var cmd : context.getSource().getServer().getCommandManager().getDispatcher().findNode(Collections.singleton("goml")).getChildren()) {
            if (cmd.canUse(context.getSource())) {
                player.sendMessage(write.apply(cmd.getName()), false);
            }
        }
        player.sendMessage(Text.literal("-------------------------------------").formatted(Formatting.DARK_GRAY), false);
        player.sendMessage(Text.literal("GitHub: ")
                .append(
                        Text.literal("https://github.com/Patbox/get-off-my-lawn-reserved")
                                .setStyle(Style.EMPTY.withColor(Formatting.BLUE).withUnderline(true).withClickEvent(new ClickEvent.OpenUrl(URI.create("https://github.com/Patbox/get-off-my-lawn-reserved"))))
                ), false);

        return 1;
    }

    private static int openList(CommandContext<ServerCommandSource> context, GameProfile target) throws CommandSyntaxException {

        ClaimListGui.open(context.getSource().getPlayer(), target);

        return 1;
    }

    private static int openGui(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var player = context.getSource().getPlayer();

        var claim = ClaimUtils.getClaimsAt(player.getWorld(), player.getBlockPos());

        if (claim.isEmpty()) {
            player.sendMessage(prefix(Text.translatable("text.goml.command.no_claims").formatted(Formatting.RED)), false);
            return 0;
        }

        claim.collect(Collectors.toList()).get(0).getValue().openUi(player);

        return 1;
    }

    private static int trust(CommandContext<ServerCommandSource> context, boolean owner) throws CommandSyntaxException {
        ServerWorld world = context.getSource().getWorld();
        ServerPlayerEntity player = context.getSource().getPlayer();
        var toAddCol = GameProfileArgumentType.getProfileArgument(context, "player");


        if (!world.isClient()) {
            var skipChecks = ClaimUtils.isInAdminMode(player);
            ClaimUtils.getClaimsAt(world, player.getBlockPos()).forEach(claimedArea -> {
                for (var toAdd : toAddCol) {
                    if (skipChecks || claimedArea.getValue().isOwner(player)) {
                        if (owner && !claimedArea.getValue().isOwner(toAdd.getId())) {
                            claimedArea.getValue().addOwner(toAdd.getId());
                            player.sendMessage(prefix(Text.translatable("text.goml.command.owner_added", toAdd.getName())), false);
                        } else if (!owner && !claimedArea.getValue().getTrusted().contains(toAdd.getId())) {
                            claimedArea.getValue().trust(toAdd.getId());
                            player.sendMessage(prefix(Text.translatable("text.goml.command.trusted", toAdd.getName())), false);
                        } else {
                            player.sendMessage(prefix(Text.translatable("text.goml.command.already_added", toAdd.getName())), false);
                        }
                    }
                }
            });
        }

        return 1;
    }

    private static int untrust(CommandContext<ServerCommandSource> context, boolean owner) throws CommandSyntaxException {
        ServerWorld world = context.getSource().getWorld();
        ServerPlayerEntity player = context.getSource().getPlayer();
        var toRemoveCol = GameProfileArgumentType.getProfileArgument(context, "player");

        // Owner/trusted tried to remove themselves from the claim

        ClaimUtils.getClaimsAt(world, player.getBlockPos()).forEach(claimedArea -> {
            for (var toRemove : toRemoveCol) {

                if (toRemove.getId().equals(player.getUuid()) && !ClaimUtils.isInAdminMode(player)) {
                    player.sendMessage(prefix(Text.translatable("text.goml.command.remove_self")), false);
                    return;
                }

                if (claimedArea.getValue().isOwner(player)) {
                    if (owner) {
                        claimedArea.getValue().getOwners().remove(toRemove.getId());
                    } else {
                        claimedArea.getValue().untrust(toRemove.getId());
                    }


                    player.sendMessage(prefix(Text.translatable("text.goml.command." + (owner ? "owner_removed" : "untrusted"), toRemove.getName())), false);
                }
            }
        });


        return 1;
    }

    private static int reload(CommandContext<ServerCommandSource> context) {
        GetOffMyLawn.CONFIG = GOMLConfig.loadOrCreateConfig();
        context.getSource().sendFeedback(() -> prefix(Text.literal("Reloaded config")), false);
        return 1;
    }

    private static int updateAllClaims(CommandContext<ServerCommandSource> context) {
        ServerWorld world = context.getSource().getWorld();
        ClaimUtils.getClaimsInDimension(world).forEach(claim -> {
            ClaimEvents.CLAIM_UPDATED.invoker().onEvent(claim.getValue());
        });
        context.getSource().sendFeedback(() -> prefix(Text.literal("Updated all claims")), false);
        return 1;
    }

    private static void bumpChat(ServerPlayerEntity player) {
        player.sendMessage(Text.literal(" "), false);
    }

    private static MutableText prefix(MutableText text) {
        return GetOffMyLawn.CONFIG.prefix(text);
    }
}
