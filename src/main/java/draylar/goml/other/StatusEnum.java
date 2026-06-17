package draylar.goml.other;

import org.jetbrains.annotations.ApiStatus;

import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

@ApiStatus.Internal
public interface StatusEnum<T> {
    Item getIcon();
    T getNext();
    T getPrevious();
    Component getName();

    enum TargetPlayer implements StatusEnum<TargetPlayer> {
        EVERYONE,
        TRUSTED,
        UNTRUSTED,
        DISABLED;

        public Item getIcon() {
            return switch (this) {
                case EVERYONE -> Items.WOOL.green();
                case TRUSTED -> Items.WOOL.yellow();
                case UNTRUSTED -> Items.WOOL.red();
                case DISABLED -> Items.WOOL.gray();
            };
        }

        public TargetPlayer getNext() {
            return switch (this) {
                case EVERYONE -> TRUSTED;
                case TRUSTED -> UNTRUSTED;
                case UNTRUSTED -> DISABLED;
                case DISABLED -> EVERYONE;
            };
        }

        public TargetPlayer getPrevious() {
            return switch (this) {
                case EVERYONE -> DISABLED;
                case TRUSTED -> EVERYONE;
                case UNTRUSTED -> TRUSTED;
                case DISABLED -> UNTRUSTED;
            };
        }

        public Component getName() {
            return Component.translatable("text.goml.mode." + this.name().toLowerCase(Locale.ROOT));
        }
    }

    enum Toggle implements StatusEnum<Toggle> {
        ENABLED,
        DISABLED;

        public Item getIcon() {
            return switch (this) {
                case ENABLED -> Items.WOOL.green();
                case DISABLED -> Items.WOOL.gray();
            };
        }

        public Toggle getNext() {
            return ENABLED == this ? DISABLED : ENABLED;
        }

        public Toggle getPrevious() {
            return ENABLED == this ? DISABLED : ENABLED;

        }

        public Component getName() {
            return Component.translatable("text.goml.mode." + this.name().toLowerCase(Locale.ROOT));
        }
    }
}