package an.anelytrapilot.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class MessageSend {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public static void sendMessage(String message) {
        if (mc.player != null) {
            Text prefix = Text.literal("[")
                    .formatted(Formatting.RED)
                    .append(Text.literal("ANElytraPilot").formatted(Formatting.GREEN))
                    .append(Text.literal("] ").formatted(Formatting.RED));

            Text content = Text.literal(message).formatted(Formatting.LIGHT_PURPLE);
            mc.player.sendMessage(prefix.copy().append(content), false);
        }

    }
}
