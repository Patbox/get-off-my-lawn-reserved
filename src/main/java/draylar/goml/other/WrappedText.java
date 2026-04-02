package draylar.goml.other;

import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.parsers.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public record WrappedText(Component text, TextNode node, String input) {
    public static WrappedText of(String input) {
        var val = TagParser.QUICK_TEXT_WITH_STF.parseNode(input);
        return new WrappedText(val.toComponent(ParserContext.of(), true), val, input);
    }

    public static WrappedText ofSafe(String input) {
        var val = TagParser.QUICK_TEXT_WITH_STF_SAFE.parseNode(input);
        return new WrappedText(val.toComponent(ParserContext.of(), true), val, input);
    }

    public MutableComponent mutableText() {
        return this.text.copy();
    }
}
