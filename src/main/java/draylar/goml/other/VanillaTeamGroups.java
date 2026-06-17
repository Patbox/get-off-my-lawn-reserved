package draylar.goml.other;

import draylar.goml.api.Claim;
import draylar.goml.api.group.PlayerGroup;
import draylar.goml.api.group.PlayerGroupProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.UserNameToIdResolver;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.TeamColor;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class VanillaTeamGroups {
    public static void init() {
        PlayerGroupProvider.register("minecraft_team", TeamProvider.INSTANCE);

    }

    public static void onRemove(PlayerTeam team) {
        var value = TeamGroup.CACHE.get(team);
        if (value != null) {
            for (var claim : List.copyOf(value.claims)) {
                claim.untrust(value);
            }
        }
    }

    private record TeamProvider() implements PlayerGroupProvider {
        public static final PlayerGroupProvider INSTANCE = new TeamProvider();
        @Override
        public @Nullable PlayerGroup getGroupOf(Player player) {
            var team = player.getTeam();
            if (team != null) {
                return TeamGroup.of(player.level().getServer().services().nameToIdCache(), team);
            }
            return null;
        }

        @Override
        public @Nullable PlayerGroup getGroupOf(MinecraftServer server, UUID uuid) {
            var profile = server.services().nameToIdCache().get(uuid);

            if (profile.isPresent()) {
                var team = server.getScoreboard().getPlayerTeam(profile.get().name());
                if (team  != null) {
                    return TeamGroup.of(server.services().nameToIdCache(), team);
                }
            }
            return null;
        }

        @Override
        @Nullable
        public PlayerGroup fromKey(MinecraftServer server, PlayerGroup.Key key) {
            var team = server.getScoreboard().getPlayerTeam(key.groupId());
            if (team  != null) {
                return TeamGroup.of(server.services().nameToIdCache(), team);
            }
            return null;
        }

        @Override
        public Component getName() {
            return Component.translatable("text.goml.vanilla_team.name");
        }
    }

    private record TeamGroup(UserNameToIdResolver cache, PlayerTeam team, HashSet<Claim> claims) implements PlayerGroup {
        public static final WeakHashMap<PlayerTeam, TeamGroup> CACHE = new WeakHashMap<>();
        private TeamGroup(UserNameToIdResolver cache, PlayerTeam guild) {
            this(cache, guild, new HashSet<>());
        }

        public static PlayerGroup of(UserNameToIdResolver cache, PlayerTeam team) {
            var g = CACHE.get(team);

            if (g == null) {
                g = new TeamGroup(cache, team);
                CACHE.put(team, g);
            }

            return g;
        }

        @Override
        public Component selfDisplayName() {
            return this.team.getDisplayName();
        }

        @Override
        public Component fullDisplayName() {
            return Component.translatable("text.goml.vanilla_team.display", Component.empty().append(this.selfDisplayName()).withStyle(ChatFormatting.WHITE));
        }

        @Override
        public Key getKey() {
            return new Key("minecraft_team", team.getName());
        }

        @Override
        public ItemStack icon() {
            var stack = new ItemStack(Items.LEATHER_HELMET);
            var i = this.team.getColor().map(TeamColor::rgb).orElse(0xFFFFFF);
            stack.set(DataComponents.DYED_COLOR, new DyedItemColor(i));
            stack.set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.DYED_COLOR, false));
            return stack;
        }

        @Override
        public PlayerGroupProvider provider() {
            return TeamProvider.INSTANCE;
        }

        @Override
        public boolean isPartOf(UUID uuid) {
            var profile = this.cache.get(uuid);
            return profile.isPresent() && this.team.getPlayers().contains(profile.get().name());
        }

        @Override
        public boolean canSave() {
            return true;
        }

        @Override
        public List<Member> getMembers() {
            List<Member> list = new ArrayList<>();
            for (var x : this.team.getPlayers()) {
                var profile = this.cache.get(x);
                if (profile.isPresent()) {
                    Member member = new Member(profile.get(), "");
                    list.add(member);
                }
            }
            return list;
        }

        @Override
        public boolean addClaim(Claim claim) {
            return this.claims.add(claim);
        }

        @Override
        public boolean removeClaim(Claim claim) {
            return this.claims.remove(claim);
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this;
        }

        @Override
        public int hashCode() {
            return this.team.getName().hashCode() + 31 * "minecraft_team".hashCode();
        }
    }
}
