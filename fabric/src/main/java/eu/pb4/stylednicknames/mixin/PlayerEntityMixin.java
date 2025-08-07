package eu.pb4.stylednicknames.mixin;

import eu.pb4.stylednicknames.NicknameHolder;
import eu.pb4.stylednicknames.config.ConfigManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;


@Mixin(Player.class)
public abstract class PlayerEntityMixin {
    @Unique boolean styledNicknames$ignoreNextCall = false;

    @ModifyArg(method = "getDisplayName", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/scores/PlayerTeam;formatNameForTeam(Lnet/minecraft/world/scores/Team;Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/MutableComponent;"))
    private Component styledNicknames$replaceName(Component text) {
        try {
            if (ConfigManager.isEnabled() && ConfigManager.getConfig().configData.changeDisplayName) {
                if (!this.styledNicknames$ignoreNextCall) {
                    this.styledNicknames$ignoreNextCall = true;
                    var holder = NicknameHolder.of(this);
                    if (holder != null && holder.styledNicknames$shouldDisplay()) {
                        Component name = holder.styledNicknames$getOutput();
                        if (name != null) {
                            this.styledNicknames$ignoreNextCall = false;
                            return name;
                        }
                    }
                    this.styledNicknames$ignoreNextCall = false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return text;
    }

}