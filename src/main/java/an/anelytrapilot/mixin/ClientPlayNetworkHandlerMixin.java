package an.anelytrapilot.mixin;

import an.anelytrapilot.commend.cmdmanage.CommandInit;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void sendChatMessageHook(@NotNull String message, CallbackInfo ci) {


        if (message.startsWith(CommandInit.ANCOMMAND.getPrefix())) {
            try {
                CommandInit.ANCOMMAND.getDispatcher().execute(
                        message.substring(CommandInit.ANCOMMAND.getPrefix().length()),
                        CommandInit.ANCOMMAND.getSource()
                );
            } catch (CommandSyntaxException ignored) {}

            ci.cancel();
        }
    }
}
