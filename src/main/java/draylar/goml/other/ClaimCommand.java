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
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static draylar.goml.GetOffMyLawn.id;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@ApiStatus.Internal
public class ClaimCommand {

    private ClaimCommand() {
        // NO-OP
    }

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            dispatcher.register(literal("goml")
                    .then(literal("help")
                            .requires(FabricPermissionBridge.require(id("command/help"), true))
                            .executes(ClaimCommand::help)
                    )
                    .then(literal("trust")
                            .requires(FabricPermissionBridge.require(id("command/trust"), true))
                            .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                    .executes(context -> trust(context, false))
                            )
                    )
                    .then(literal("untrust")
                            .requires(FabricPermissionBridge.require(id("command/untrust"), true))
                            .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                    .executes((ctx) -> ClaimCommand.untrust(ctx, false)))
                    )
                    .then(literal("addowner")
                            .requires(FabricPermissionBridge.require(id("command/addowner"), true))
                            .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                    .executes(context -> trust(context, true)))
                    )

                    .then(literal("list")
                            .requires(FabricPermissionBridge.require(id("command/list"), true))
                            .executes(context -> openList(context, context.getSource().getPlayer().getGameProfile()))
                    )

                    .then(literal("escape")
                            .requires(FabricPermissionBridge.require(id("command/escape"), true))
                            .executes(context -> escape(context, context.getSource().getPlayerOrException()))
                    )

                    .then(literal("admin")
                            .requires(FabricPermissionBridge.require(id("command/admin"), PermissionLevel.ADMINS))
                            .then(literal("fixaugments")
                                    .requires(FabricPermissionBridge.require(id("command/fixaugments"), true))
                                    .executes(ClaimCommand::fixAugments)
                            )
                            .then(literal("escape")
                                    .requires(FabricPermissionBridge.require(id("command/admin/escape"), true))
                                    .then(argument("player", EntityArgument.player())
                                            .executes(context -> escape(context, EntityArgument.getPlayer(context, "player")))
                                    )
                            )

                            .then(literal("adminmode")
                                    .requires(FabricPermissionBridge.require(id("command/admin/admin_mode"), PermissionLevel.ADMINS))
                                    .executes(ClaimCommand::adminMode)
                            )
                            .then(literal("removeowner")
                                    .requires(FabricPermissionBridge.require(id("command/admin/removeowner"), true))
                                    .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                            .executes((ctx) -> ClaimCommand.untrust(ctx, true)))
                            )
                            .then(literal("info")
                                    .requires(FabricPermissionBridge.require(id("command/admin/info"), PermissionLevel.ADMINS))
                                    .executes(ClaimCommand::infoAdmin)
                            )
                            .then(literal("world")
                                    .requires(FabricPermissionBridge.require(id("command/admin/world"), PermissionLevel.ADMINS))
                                    .executes(ClaimCommand::world)
                            )
                            .then(literal("general")
                                    .requires(FabricPermissionBridge.require(id("command/admin/general"), PermissionLevel.ADMINS))
                                    .executes(ClaimCommand::general)
                            )
                            .then(literal("remove")
                                    .requires(FabricPermissionBridge.require(id("command/admin/remove"), PermissionLevel.ADMINS))
                                    .executes(ClaimCommand::remove)
                            )
                            .then(literal("reload")
                                    .requires(FabricPermissionBridge.require(id("command/admin/reload"), PermissionLevel.OWNERS))
                                    .executes(ClaimCommand::reload)
                            )
                            .then(literal("list")
                                    .requires(FabricPermissionBridge.require(id("command/list"), true))
                                    .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                            .executes(context -> {
                                                var player = GameProfileArgument.getGameProfiles(context, "player").toArray(new GameProfile[0]);

                                                if (player.length == 0) {
                                                    context.getSource().sendSuccess(() -> Component.translatable("argument.player.unknown").withStyle(ChatFormatting.RED), false);
                                                }

                                                return openList(context, player[0]);
                                            })
                                    )
                            )
                            .then(literal("updateallclaims")
                                    .requires(FabricPermissionBridge.require(id("command/admin/updateallclaims"), PermissionLevel.OWNERS))
                                    .executes(ClaimCommand::updateAllClaims)
                            )
                    )
            );
        });
    }

    private static int fixAugments(CommandContext<CommandSourceStack> context) {
        ClaimUtils.getClaimsAt(context.getSource().getLevel(), BlockPos.containing(context.getSource().getPosition())).forEach(x -> {
            var copy = new ArrayList<>(x.getValue().getAugments().entrySet());

            for (var y : copy) {
                if (context.getSource().getLevel().getBlockState(y.getKey()).getBlock() != y.getValue()) {
                    x.getValue().removeAugment(y.getKey());
                }
            }
        });


        return 0;
    }

    private static int escape(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        var claims = ClaimUtils.getClaimsAt(player.level(), player.blockPosition()).filter(x -> !x.getValue().hasPermission(player));

        if (claims.isNotEmpty()) {
            claims.forEach((claim) -> {
                if (!claim.getKey().minecraftBox().contains(player.position())) {
                    return;
                }

                var pair = ClaimUtils.getClosestXZBorder(claim.getValue(), player.position(), 1);

                var pos = pair.getA();
                var dir = pair.getB();

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


                player.connection.send(new ClientboundSetEntityMotionPacket(player));

                if (player.isPassenger()) {
                    player.connection.send(new ClientboundSetEntityMotionPacket(player.getVehicle()));
                }
            });
            context.getSource().sendSuccess(() -> prefix(Component.translatable("text.goml.command/escaped").withStyle(ChatFormatting.GREEN)), false);

        } else {
            context.getSource().sendSuccess(() -> prefix(Component.translatable("text.goml.command/cant_escape").withStyle(ChatFormatting.RED)), false);

        }

        return 0;
    }

    private static int adminMode(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayer();

        var newMode = !((GomlPlayer) player).goml_getAdminMode();
        ((GomlPlayer) player).goml_setAdminMode(newMode);
        context.getSource().sendSuccess(() -> prefix(Component.translatable(newMode ? "text.goml.admin_mode.enabled" : "text.goml.admin_mode.disabled")), false);

        return 1;
    }

    /**
     * Sends the player general information about all claims on the server.
     *
     * @param context context
     * @return success flag
     */
    private static int general(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        ServerPlayer player = context.getSource().getPlayer();
        AtomicInteger numberOfClaimsTotal = new AtomicInteger();

        bumpChat(player);

        server.getAllLevels().forEach(world -> {
            var worldClaims = GetOffMyLawn.CLAIM.get(world).getClaims();
            int numberOfClaimsWorld = worldClaims.size();
            numberOfClaimsTotal.addAndGet(1);

            player.sendSystemMessage(prefix(Component.translatable("text.goml.command/number_in", world.dimension().identifier(), numberOfClaimsWorld)), false);
        });

        player.sendSystemMessage(prefix(Component.translatable("text.goml.command/number_all", numberOfClaimsTotal.get()).withStyle(ChatFormatting.WHITE)), false);

        return 1;
    }

    /**
     * Sends the player information about the claim they are standing in, if it exists.
     *
     * @param context context
     * @return success flag
     */
    private static int infoAdmin(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerLevel world = context.getSource().getLevel();
        ServerPlayer player = context.getSource().getPlayer();

        if (!world.isClientSide()) {
            ClaimUtils.getClaimsAt(world, player.blockPosition()).forEach(claimedArea -> {
                player.sendSystemMessage(prefix(Component.literal("Origin: " + claimedArea.getValue().getOrigin().toShortString())), false);
                player.sendSystemMessage(prefix(Component.literal("Radius: " + claimedArea.getValue().getRadius() + " Height: " + claimedArea.getKey().getY())), false);
                {
                    var owners = Component.literal("Owners: ");

                    {
                        var iter = claimedArea.getValue().getOwners().iterator();

                        while (iter.hasNext()) {
                            var uuid = iter.next();
                            var gameProfile = context.getSource().getServer().services().nameToIdCache().get(uuid);
                            owners.append(Component.literal((gameProfile.isPresent() ? gameProfile.get().name() : "<unknown>") + " -> " + uuid.toString())
                                    .setStyle(Style.EMPTY
                                            .withColor(ChatFormatting.GRAY)
                                            .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy")))
                                            .withClickEvent(new ClickEvent.CopyToClipboard( uuid.toString()))
                                    )
                            );

                            if (iter.hasNext()) {
                                owners.append(", ");
                            }
                        }
                    }

                    var trusted = Component.literal("Trusted: ");

                    {
                        var iter = claimedArea.getValue().getTrusted().iterator();

                        while (iter.hasNext()) {
                            var uuid = iter.next();
                            var gameProfile = context.getSource().getServer().services().nameToIdCache().get(uuid);
                            trusted.append(Component.literal((gameProfile.isPresent() ? gameProfile.get().name() : "<unknown>") + " -> " + uuid.toString())
                                    .setStyle(Style.EMPTY
                                            .withColor(ChatFormatting.GRAY)
                                            .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy")))
                                            .withClickEvent(new ClickEvent.CopyToClipboard(uuid.toString()))
                                    )
                            );

                            if (iter.hasNext()) {
                                owners.append(", ");
                            }
                        }
                    }

                    player.sendSystemMessage(prefix(owners), false);
                    player.sendSystemMessage(prefix(trusted), false);
                }
                player.sendSystemMessage(prefix(Component.literal("ClaimData: ")), false);
                for (var key : (Collection<DataKey<Object>>) (Object) claimedArea.getValue().getDataKeys()) {
                    player.sendSystemMessage(Component.literal("- " + key.key() + " -> ").append(NbtUtils.toPrettyComponent(key.serializer().apply(claimedArea.getValue().getData(key)))), false);
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
    private static int world(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerLevel world = context.getSource().getLevel();
        ServerPlayer player = context.getSource().getPlayer();

        var worldClaims = GetOffMyLawn.CLAIM.get(world).getClaims();
        int numberOfClaims = worldClaims.size();

        player.sendSystemMessage(prefix(Component.translatable("text.goml.command/number_in", world.dimension().identifier(), numberOfClaims)), false);

        return 1;
    }

    /**
     * Removes the claim the player is currently standing in, if it exists.
     *
     * @param context context
     * @return success flag
     */
    private static int remove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerLevel world = context.getSource().getLevel();
        ServerPlayer player = context.getSource().getPlayer();

        if (!world.isClientSide()) {
            ClaimUtils.getClaimsAt(world, player.blockPosition()).forEach(claimedArea -> {
                claimedArea.getValue().destroy();
                player.sendSystemMessage(prefix(Component.translatable("text.goml.command/removed_claim", world.dimension().identifier().toString(), claimedArea.getValue().getOrigin().toShortString())), false);
                var blockEntity = world.getBlockEntity(claimedArea.getValue().getOrigin(), GOMLEntities.CLAIM_ANCHOR);

                if (blockEntity.isPresent()) {
                    world.destroyBlock(claimedArea.getValue().getOrigin(), true);
                    for (var lPos : new ArrayList<>(blockEntity.get().getAugments().keySet())) {
                        world.destroyBlock(lPos, true);
                    }
                }

            });
        }

        return 1;
    }

    /**
     * Sends the player information on using the /goml command/
     *
     * @param context context
     * @return success flag
     */
    private static int help(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayer();

        Function<String, Component> write = (command) -> Component.literal("/goml " + command)
                .append(Component.literal(" - ").withStyle(ChatFormatting.GRAY))
                .append(Component.translatable("text.goml.command/help." + command).setStyle(Style.EMPTY.withColor(0xededed)));

        player.sendSystemMessage(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY).append(Component.literal("Get Off My Lawn").setStyle(Style.EMPTY.withColor(0xa1ff59))).append("]"), false);
        player.sendSystemMessage(Component.literal("-------------------------------------").withStyle(ChatFormatting.DARK_GRAY), false);

        for (var cmd : context.getSource().getServer().getCommands().getDispatcher().findNode(Collections.singleton("goml")).getChildren()) {
            if (cmd.canUse(context.getSource())) {
                player.sendSystemMessage(write.apply(cmd.getName()), false);
            }
        }
        player.sendSystemMessage(Component.literal("-------------------------------------").withStyle(ChatFormatting.DARK_GRAY), false);
        player.sendSystemMessage(Component.literal("GitHub: ")
                .append(
                        Component.literal("https://github.com/Patbox/get-off-my-lawn-reserved")
                                .setStyle(Style.EMPTY.withColor(ChatFormatting.BLUE).withUnderlined(true).withClickEvent(new ClickEvent.OpenUrl(URI.create("https://github.com/Patbox/get-off-my-lawn-reserved"))))
                ), false);

        return 1;
    }

    private static int openList(CommandContext<CommandSourceStack> context, GameProfile target) throws CommandSyntaxException {

        ClaimListGui.open(context.getSource().getPlayer(), target);

        return 1;
    }

    private static int openGui(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var player = context.getSource().getPlayer();

        var claim = ClaimUtils.getClaimsAt(player.level(), player.blockPosition());

        if (claim.isEmpty()) {
            player.sendSystemMessage(prefix(Component.translatable("text.goml.command/no_claims").withStyle(ChatFormatting.RED)), false);
            return 0;
        }

        claim.collect(Collectors.toList()).get(0).getValue().openUi(player);

        return 1;
    }

    private static int trust(CommandContext<CommandSourceStack> context, boolean owner) throws CommandSyntaxException {
        ServerLevel world = context.getSource().getLevel();
        ServerPlayer player = context.getSource().getPlayer();
        var toAddCol = GameProfileArgument.getGameProfiles(context, "player");


        if (!world.isClientSide()) {
            var skipChecks = ClaimUtils.isInAdminMode(player);
            ClaimUtils.getClaimsAt(world, player.blockPosition()).forEach(claimedArea -> {
                for (var toAdd : toAddCol) {
                    if (skipChecks || claimedArea.getValue().isOwner(player)) {
                        if (owner && !claimedArea.getValue().isOwner(toAdd.id())) {
                            claimedArea.getValue().addOwner(toAdd.id());
                            player.sendSystemMessage(prefix(Component.translatable("text.goml.command/owner_added", toAdd.name())), false);
                        } else if (!owner && !claimedArea.getValue().getTrusted().contains(toAdd.id())) {
                            claimedArea.getValue().trust(toAdd.id());
                            player.sendSystemMessage(prefix(Component.translatable("text.goml.command/trusted", toAdd.name())), false);
                        } else {
                            player.sendSystemMessage(prefix(Component.translatable("text.goml.command/already_added", toAdd.name())), false);
                        }
                    }
                }
            });
        }

        return 1;
    }

    private static int untrust(CommandContext<CommandSourceStack> context, boolean owner) throws CommandSyntaxException {
        ServerLevel world = context.getSource().getLevel();
        ServerPlayer player = context.getSource().getPlayer();
        var toRemoveCol = GameProfileArgument.getGameProfiles(context, "player");

        // Owner/trusted tried to remove themselves from the claim

        ClaimUtils.getClaimsAt(world, player.blockPosition()).forEach(claimedArea -> {
            for (var toRemove : toRemoveCol) {

                if (toRemove.id().equals(player.getUUID()) && !ClaimUtils.isInAdminMode(player)) {
                    player.sendSystemMessage(prefix(Component.translatable("text.goml.command/remove_self")), false);
                    return;
                }

                if (claimedArea.getValue().isOwner(player)) {
                    if (owner) {
                        claimedArea.getValue().getOwners().remove(toRemove.id());
                    } else {
                        claimedArea.getValue().untrust(toRemove.id());
                    }


                    player.sendSystemMessage(prefix(Component.translatable("text.goml.command/" + (owner ? "owner_removed" : "untrusted"), toRemove.name())), false);
                }
            }
        });


        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        GetOffMyLawn.CONFIG = GOMLConfig.loadOrCreateConfig();
        context.getSource().sendSuccess(() -> prefix(Component.literal("Reloaded config")), false);
        return 1;
    }

    private static int updateAllClaims(CommandContext<CommandSourceStack> context) {
        ServerLevel world = context.getSource().getLevel();
        ClaimUtils.getClaimsInDimension(world).forEach(claim -> {
            ClaimEvents.CLAIM_UPDATED.invoker().onEvent(claim.getValue());
        });
        context.getSource().sendSuccess(() -> prefix(Component.literal("Updated all claims")), false);
        return 1;
    }

    private static void bumpChat(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(" "), false);
    }

    private static MutableComponent prefix(MutableComponent text) {
        return GetOffMyLawn.CONFIG.prefix(text);
    }
}
