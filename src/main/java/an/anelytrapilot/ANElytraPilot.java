package an.anelytrapilot;

import an.anelytrapilot.commend.ANPilotCommend;
import an.anelytrapilot.commend.ANSharedStates;
import an.anelytrapilot.commend.cmdmanage.CommandInit;
import an.anelytrapilot.config.ANPilotConfig;
import an.anelytrapilot.config.PlayerDataSender;
import an.anelytrapilot.fuction.ElytraCollect;
import an.anelytrapilot.fuction.EndElytraPilot;
import an.anelytrapilot.fuction.OverWorldElytraPilot;
import an.anelytrapilot.mic.PlaceModule;
import an.anelytrapilot.path.WalkToward;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ANElytraPilot implements ModInitializer {
	public static final String MOD_ID = "anelytrapilot";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final MinecraftClient mc = MinecraftClient.getInstance();
	@Override
	public void onInitialize() {
		ANPilotConfig.load();           // 启动时加载配置
		ANPilotCommend.register();      // 注册指令
		CommandInit.tick();

		/*
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			PlayerDataSender.sendPlayerData(mc);
		});


		 */
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null || client.world == null) ANSharedStates.FlyStop = true;

			if(ANSharedStates.FlyStop){
				ANSharedStates.FlyCommend = false;
				ANSharedStates.OverWorldFlyCommend = false;

			}
			EndElytraPilot.tick();
			WalkToward.tick();
			OverWorldElytraPilot.tick();
			//ElytraCollect.tick();

		});
		LOGGER.info("ANElytraPolit Initing~~~");
	}
}