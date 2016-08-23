package org.javagram.model;

import org.javagram.dao.ApiBridgeTelegramDAO;
import org.javagram.dao.TelegramDAO;

import java.io.IOException;

/**
 * Created by HerrSergio on 23.08.2016.
 */
public class ApiBridgeTelegramModel extends AbstractTelegramModel {
    public ApiBridgeTelegramModel(String server, int appId, String appHash) throws IOException {
        super(new ApiBridgeTelegramDAO(server, appId, appHash));
    }
}
