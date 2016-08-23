package org.javagram.model;

import org.javagram.dao.DebugTelegramDAO;
import org.javagram.dao.TelegramDAO;

/**
 * Created by HerrSergio on 23.08.2016.
 */
public class DebugTelegramModel extends AbstractTelegramModel {
    public DebugTelegramModel() {
        super(new DebugTelegramDAO());
    }
}
