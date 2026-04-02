package draylar.goml.ui;

import com.mojang.authlib.GameProfile;
import draylar.goml.api.group.PlayerGroup;
import draylar.goml.api.group.PlayerGroupProvider;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

public class GenericPlayerAndGroupSelectionGui extends GenericPlayerSelectionGui {
    private final Predicate<PlayerGroup> shouldDisplayGroup;
    private final Consumer<PlayerGroup> onClickGroup;
    private final List<PlayerGroup> groups = new ArrayList<>();


    public GenericPlayerAndGroupSelectionGui(ServerPlayer player, Component title, Predicate<NameAndId> shouldDisplay,
                                             Predicate<PlayerGroup> shouldDisplayGroup, Consumer<NameAndId> onClick, Consumer<PlayerGroup> onClickGroup, Runnable postClose) {
        super(player, title, shouldDisplay, onClick, postClose);
        this.shouldDisplayGroup = shouldDisplayGroup;
        this.onClickGroup = onClickGroup;
    }

    @Override
    protected void updateDisplay() {
        this.groups.clear();
        for (var group : PlayerGroupProvider.getAllGroups(player)) {
            if (this.shouldDisplayGroup.test(group)) {
                this.groups.add(group);
            }
        }

        super.updateDisplay();
    }

    @Override
    protected DisplayElement getElement(int id) {
        return id < this.groups.size() ? getGroupElement(id) : getPlayerElement(id - this.groups.size());
    }

    protected DisplayElement getGroupElement(int id) {
        var group = this.groups.get(id);
        var b = GuiElementBuilder.from(group.icon())
                .setName(Component.empty().append(group.selfDisplayName()).append(Component.literal(" (").withStyle(ChatFormatting.DARK_GRAY)
                                .append(Component.empty().append(group.provider().getName()).setStyle(Style.EMPTY.withColor(0x45abff))
                                        .append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY)))
                        )
                )
                .hideDefaultTooltip()
                .setCallback(() -> {
                    playClickSound(this.player);
                    this.onClickGroup.accept(group);
                    this.close(this.closeCallback != null);
                });

        return DisplayElement.of(b);
    }
}
