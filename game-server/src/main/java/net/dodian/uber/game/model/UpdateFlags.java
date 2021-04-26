package net.dodian.uber.game.model;

import java.util.EnumMap;

/**
 * @author Dashboard
 */
@SuppressWarnings("serial")
public class UpdateFlags extends EnumMap<UpdateFlag, Boolean> {

    private boolean updateRequired;

    public UpdateFlags() {
        super(UpdateFlag.class);
        for (UpdateFlag flag : UpdateFlag.values())
            put(flag, false);
    }

    public boolean isRequired(UpdateFlag flag) {
        return get(flag);
    }

    public void setRequired(UpdateFlag flag, boolean required) {
        put(flag, required);
        if (required) {
            updateRequired = true;
        }
    }

    public void clear() {
        for (UpdateFlag flag : UpdateFlag.values()) {
            put(flag, false);
        }
        updateRequired = false;
    }

    public boolean isUpdateRequired() {
        return updateRequired;
    }

}
