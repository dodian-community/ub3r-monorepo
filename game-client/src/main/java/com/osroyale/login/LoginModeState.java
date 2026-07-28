package com.osroyale.login;

/**
 * Explicit login intent sent in the encrypted login block.
 *
 * <p>This state is deliberately independent from login-screen labels and error text so changing
 * the UI cannot silently turn an account-creation attempt into an ordinary password login.</p>
 */
public final class LoginModeState {

    public enum Mode {
        LOGIN(0),
        CREATE_ACCOUNT(1);

        private final int protocolValue;

        Mode(int protocolValue) {
            this.protocolValue = protocolValue;
        }

        public int getProtocolValue() {
            return protocolValue;
        }
    }

    private Mode mode = Mode.LOGIN;

    public Mode getMode() {
        return mode;
    }

    public boolean isAccountCreation() {
        return mode == Mode.CREATE_ACCOUNT;
    }

    public void beginAccountCreation() {
        mode = Mode.CREATE_ACCOUNT;
    }

    public void selectExistingAccount() {
        mode = Mode.LOGIN;
    }

    public void usernameTaken() {
        // A username collision does not consume the preflighted Discord authorization.
        mode = Mode.CREATE_ACCOUNT;
    }

    public int protocolValue(boolean reconnecting) {
        return reconnecting ? Mode.LOGIN.getProtocolValue() : mode.getProtocolValue();
    }
}
