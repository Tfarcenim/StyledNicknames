package eu.pb4.stylednicknames;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class CardboardWarning {
    public static final Logger LOGGER = LogUtils.getLogger();


    public static void checkAndAnnounce() {
            LOGGER.error("==============================================");
            for (var i = 0; i < 4; i++) {
                LOGGER.error("");
                LOGGER.error("Cardboard/Banner detected! This mod doesn't work with it!");
                LOGGER.error("You won't get any support as long as it's present!");
                LOGGER.error("");
                LOGGER.error("Read more at: https://gist.github.com/Patbox/e44844294c358b614d347d369b0fc3bf");
                LOGGER.error("");
                LOGGER.error("==============================================");
            }
    }
}
