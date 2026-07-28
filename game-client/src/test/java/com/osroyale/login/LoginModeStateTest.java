package com.osroyale.login;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LoginModeStateTest {

    @Test
    public void accountCreationModeSurvivesUsernameCollisionAndRetry() {
        LoginModeState state = new LoginModeState();

        state.beginAccountCreation();
        state.usernameTaken();

        assertTrue(state.isAccountCreation());
        assertEquals(1, state.protocolValue(false));
    }

    @Test
    public void reconnectAlwaysUsesOrdinaryLoginWithoutDestroyingScreenIntent() {
        LoginModeState state = new LoginModeState();
        state.beginAccountCreation();

        assertEquals(0, state.protocolValue(true));
        assertTrue(state.isAccountCreation());
    }

    @Test
    public void selectingExistingAccountClearsCreationMode() {
        LoginModeState state = new LoginModeState();
        state.beginAccountCreation();
        state.selectExistingAccount();

        assertFalse(state.isAccountCreation());
        assertEquals(LoginModeState.Mode.LOGIN, state.getMode());
        assertEquals(0, state.protocolValue(false));
    }
}
