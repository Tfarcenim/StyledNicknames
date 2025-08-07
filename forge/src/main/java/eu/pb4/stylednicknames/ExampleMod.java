package eu.pb4.stylednicknames;

import net.minecraftforge.fml.common.Mod;

@Mod(StyledNicknames.MOD_ID)
public class ExampleMod {
    
    public ExampleMod() {
    
        // This method is invoked by the Forge mod loader when it is ready
        // to load your mod. You can access Forge and Common code in this
        // project.
    
        // Use Forge to bootstrap the Common mod.
        StyledNicknames.LOG.info("Hello Forge world!");
        StyledNicknames.init();
        
    }
}