package net.runelite.client.plugins.accounts;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("accounts")
public interface AccountsConfig extends Config
{
	@ConfigItem(
		keyName = "showQuickLogin",
		name = "Show Quick Login",
		description = "Configure whether the quick login is active"
	)
	default boolean showQuickLogin()
	{
		return true;
	}
}
