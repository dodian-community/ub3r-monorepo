package net.runelite.client.plugins.accounts;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.SessionClose;
import net.runelite.client.events.SessionOpen;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import com.osroyale.profile.Profile;
import com.osroyale.profile.ProfileManager;

@PluginDescriptor(
	name = "Accounts Manager",
	description = "Switch between saved accounts and view their status/skills",
	tags = {"login", "accounts", "sidebar"},
	loadWhenOutdated = true
)
public class AccountsPlugin extends Plugin
{
	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private AccountsConfig config;

	private AccountsPanel panel;
	private NavigationButton navButton;

	@Provides
	AccountsConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AccountsConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		panel = injector.getInstance(AccountsPanel.class);
		panel.init((com.osroyale.Client) client, config, this::refreshPanel);

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "accounts_icon.png");

		navButton = NavigationButton.builder()
			.tooltip("Accounts Manager")
			.icon(icon)
			.priority(9)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
	}

	@Subscribe
	public void onGameStateChanged(net.runelite.api.events.GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			refreshPanel();
		}
		else
		{
			SwingUtilities.invokeLater(panel::updatePanel);
		}
	}

	@Subscribe
	public void onSessionOpen(SessionOpen event)
	{
		refreshPanel();
	}

	@Subscribe
	public void onSessionClose(SessionClose event)
	{
		SwingUtilities.invokeLater(panel::updatePanel);
	}

	private void refreshPanel()
	{
		if (!com.osroyale.Client.loggedIn || com.osroyale.Client.localPlayer == null)
		{
			SwingUtilities.invokeLater(panel::updatePanel);
			return;
		}

		clientThread.invokeLater(() ->
		{
			if (com.osroyale.Client.localPlayer != null)
			{
				com.osroyale.Client.localPlayer.saveProfile((com.osroyale.Client) client);
			}
			SwingUtilities.invokeLater(panel::updatePanel);
		});
	}
}
