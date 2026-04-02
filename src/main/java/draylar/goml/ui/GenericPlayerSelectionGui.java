package draylar.goml.ui;

import com.mojang.authlib.GameProfile;
import draylar.goml.api.group.PlayerGroupProvider;
import draylar.goml.registry.GOMLTextures;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

@ApiStatus.Internal
public class GenericPlayerSelectionGui extends PagedGui {
    private final PlayerList playerManager;
    private final Predicate<NameAndId> shouldDisplay;
    private final Consumer<NameAndId> onClick;
    private int ticker;
    private List<NameAndId> cachedPlayers = Collections.emptyList();

    public GenericPlayerSelectionGui(ServerPlayer player, Component title, Predicate<NameAndId> shouldDisplay, Consumer<NameAndId> onClick, Runnable postClose) {
        super(player, postClose);
        this.shouldDisplay = shouldDisplay;
        this.onClick = onClick;
        this.playerManager = Objects.requireNonNull(player.level().getServer()).getPlayerList();
        this.setTitle(title);
    }

    public void updateAndOpen() {
        this.updateDisplay();
        this.open();
    }

    @Override
    protected int getPageAmount() {
        return this.getContentAmount() / PAGE_SIZE;
    }

    protected int getContentAmount() {
        return this.cachedPlayers.size();
    }

    @Override
    protected void updateDisplay() {
        List<NameAndId> list = new ArrayList<>();
        for (var p : this.playerManager.getPlayers()) {
            if (this.shouldDisplay.test(p.nameAndId())) {
                list.add(p.nameAndId());
            }
        }

        for (var group : PlayerGroupProvider.getAllGroups(this.player)) {
            for (var member : group.getMembers()) {
                if (!list.contains(member.profile()) && this.shouldDisplay.test(member.profile())) {
                    list.add(member.profile());
                }
            }
        }


        list.sort(Comparator.comparing(NameAndId::name));
        this.cachedPlayers = list;
        super.updateDisplay();
    }

    @Override
    protected DisplayElement getElement(int id) {
        return this.getPlayerElement(id);
    }

    protected DisplayElement getPlayerElement(int id) {
        if (this.cachedPlayers.size() > id) {
            var player = this.cachedPlayers.get(id);
            var b = new GuiElementBuilder(Items.PLAYER_HEAD)
                    .setName(Component.literal(player.name()))
                    .setProfile(player.id())
                    .hideDefaultTooltip()
                    .setCallback(() -> {
                        playClickSound(this.player);
                        this.onClick.accept(player);
                        this.close(this.closeCallback != null);
                    });

            var x = PlayerGroupProvider.getShared(this.player, player.id());

            if (!x.isEmpty()) {
                b.addLoreLine(Component.translatable("text.goml.gui.shared_groups").withStyle(ChatFormatting.GOLD));

                for (var g : x) {
                    b.addLoreLine(Component.literal("- ").append(g.fullDisplayName()).withStyle(ChatFormatting.GRAY));
                }
            }


            return DisplayElement.of(b);
        }

        return DisplayElement.empty();
    }

    @Override
    protected DisplayElement getNavElement(int id) {
        return switch (id) {
            case 5 -> DisplayElement.of(new GuiElementBuilder(Items.NAME_TAG)
                    .setName(Component.translatable("text.goml.gui.player_selector.by_name").withStyle(ChatFormatting.GREEN))
                    .setCallback(() -> {
                        playClickSound(this.player);

                        this.ignoreCloseCallback = true;
                        this.close(true);
                        this.ignoreCloseCallback = false;
                        new NamePlayerSelectorGui(this.player, this.shouldDisplay, this::refreshOpen, (p) -> {
                            this.onClick.accept(p);
                            this.close(true);
                            if (this.closeCallback != null) {
                                this.closeCallback.run();
                            }
                        });
                    }));
            default -> super.getNavElement(id);
        };
    }

    @Override
    public void onTick() {
        this.ticker++;
        if (this.ticker == 20) {
            this.ticker = 0;
            this.updateDisplay();
        }
        super.onTick();
    }
}
