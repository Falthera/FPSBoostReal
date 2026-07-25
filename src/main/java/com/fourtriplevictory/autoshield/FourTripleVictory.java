package com.fourtriplevictory.autoshield;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FourTripleVictory implements ModInitializer {
    public static final String MOD_ID = "fourtriplevictory";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("FourTripleVictory initialized");
    }
}
