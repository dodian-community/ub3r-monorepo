package net.runelite.client.plugins.accounts;

import com.osroyale.Client;
import com.osroyale.profile.Profile;
import com.osroyale.profile.ProfileManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

public class AccountsPanel extends PluginPanel
{
	private final JPanel accountsContainer = new JPanel();
	private Client client;
	private AccountsConfig config;
	private Runnable refreshAction;

	public void init(Client client, AccountsConfig config, Runnable refreshAction)
	{
		this.client = client;
		this.config = config;
		this.refreshAction = refreshAction;

		getParent().setLayout(new BorderLayout());
		getParent().add(this, BorderLayout.CENTER);

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Title header
		JLabel titleLabel = new JLabel("Accounts Manager");
		titleLabel.setForeground(ColorScheme.BRAND_ORANGE);
		titleLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
		titleLabel.setHorizontalAlignment(JLabel.CENTER);
		add(titleLabel, BorderLayout.NORTH);

		// Container for profiles
		accountsContainer.setLayout(new BoxLayout(accountsContainer, BoxLayout.Y_AXIS));
		accountsContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JScrollPane scrollPane = new JScrollPane(accountsContainer);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		add(scrollPane, BorderLayout.CENTER);

		updatePanel();
	}

	public void updatePanel()
	{
		accountsContainer.removeAll();

		if (ProfileManager.profiles == null || ProfileManager.profiles.isEmpty())
		{
			JLabel noProfiles = new JLabel("No saved profiles found.");
			noProfiles.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			noProfiles.setBorder(new EmptyBorder(15, 10, 10, 10));
			noProfiles.setHorizontalAlignment(JLabel.CENTER);
			accountsContainer.add(noProfiles);
		}
		else
		{
			int count = 0;
			for (Profile p : ProfileManager.profiles)
			{
				if (p == null || p.emptySlot())
				{
					continue;
				}

				JPanel card = createAccountCard(p);
				accountsContainer.add(card);
				accountsContainer.add(javax.swing.Box.createRigidArea(new Dimension(0, 10)));
				count++;
			}

			if (count == 0)
			{
				JLabel noProfiles = new JLabel("No saved profiles found.");
				noProfiles.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
				noProfiles.setBorder(new EmptyBorder(15, 10, 10, 10));
				noProfiles.setHorizontalAlignment(JLabel.CENTER);
				accountsContainer.add(noProfiles);
			}
		}

		// Refresh action button
		JButton refreshBtn = new JButton("Refresh");
		refreshBtn.addActionListener(e -> refreshAction.run());
		accountsContainer.add(refreshBtn);

		accountsContainer.revalidate();
		accountsContainer.repaint();
	}

	private JPanel createAccountCard(Profile p)
	{
		JPanel card = new JPanel();
		card.setLayout(new BorderLayout());
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR, 1),
			BorderFactory.createEmptyBorder(8, 8, 8, 8)
		));

		// Text info panel
		JPanel infoPanel = new JPanel();
		infoPanel.setLayout(new GridLayout(3, 1));
		infoPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JLabel nameLabel = new JLabel(com.osroyale.Utility.formatName(p.getUsername()));
		nameLabel.setForeground(ColorScheme.BRAND_ORANGE);
		infoPanel.add(nameLabel);

		String dateStr = "Never";
		if (p.getLastLogin() > 0)
		{
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			dateStr = sdf.format(new Date(p.getLastLogin()));
		}
		JLabel loginLabel = new JLabel("Last Login: " + dateStr);
		loginLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		infoPanel.add(loginLabel);

		card.add(infoPanel, BorderLayout.NORTH);

		// Render Player 3D Full Body Image Preview
		JLabel bodyLabel = new JLabel();
		bodyLabel.setHorizontalAlignment(JLabel.CENTER);
		bodyLabel.setPreferredSize(new Dimension(100, 150));
		bodyLabel.setMinimumSize(new Dimension(100, 150));
		bodyLabel.setBorder(BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR, 1));
		String tooltipText = (p.getLastLogin() > 0) ? getStatsTooltipHtml(p) : null;

		card.setToolTipText(tooltipText);
		infoPanel.setToolTipText(tooltipText);
		bodyLabel.setToolTipText(tooltipText);

		File bodyImgFile = new File(net.runelite.client.RuneLite.PROFILES_DIR, p.getUsername().toLowerCase() + "_body.png");
		if (bodyImgFile.exists())
		{
			try
			{
				BufferedImage img = normalizeLegacyPreview(ImageIO.read(bodyImgFile));
				bodyLabel.setIcon(new ImageIcon(img));
			}
			catch (Exception ex)
			{
				bodyLabel.setText("No Preview");
			}
		}
		else
		{
			bodyLabel.setText("No Preview");
		}
		card.add(bodyLabel, BorderLayout.CENTER);

		// Action Buttons Panel - only show Login button if NOT logged in
		boolean isCurrentlyLoggedIn = Client.loggedIn;
		JPanel btnPanel = new JPanel(new GridLayout(1, isCurrentlyLoggedIn ? 1 : 2, 5, 0));
		btnPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		btnPanel.setBorder(new EmptyBorder(5, 0, 0, 0));

		if (!isCurrentlyLoggedIn)
		{
			JButton loginBtn = new JButton("Login");
			loginBtn.addActionListener(e -> {
				Client osClient = (Client) client;
				osClient.myUsername = p.getUsername();
				osClient.myPassword = p.getPassword();
				osClient.attemptLogin(osClient.myUsername, osClient.myPassword, false);
			});
			btnPanel.add(loginBtn);
		}

		JButton deleteBtn = new JButton("Delete");
		deleteBtn.addActionListener(e -> {
			com.osroyale.profile.ProfileManager.delete(p);
			updatePanel();
		});
		btnPanel.add(deleteBtn);

		card.add(btnPanel, BorderLayout.SOUTH);
		card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));

		return card;
	}

	/**
	 * Old profile previews were written from the client's RGB raster directly into an ARGB PNG.
	 * Their character pixels therefore have RGB data but a zero alpha channel. Keep them readable
	 * in the sidebar until the next logout/save writes the corrected PNG format.
	 */
	static BufferedImage normalizeLegacyPreview(BufferedImage source)
	{
		if (source == null)
		{
			return null;
		}

		boolean requiresRepair = false;
		for (int y = 0; y < source.getHeight() && !requiresRepair; y++)
		{
			for (int x = 0; x < source.getWidth(); x++)
			{
				int pixel = source.getRGB(x, y);
				if ((pixel >>> 24) == 0 && (pixel & 0x00FFFFFF) != 0)
				{
					requiresRepair = true;
					break;
				}
			}
		}
		if (!requiresRepair)
		{
			return source;
		}

		BufferedImage repaired = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < source.getHeight(); y++)
		{
			for (int x = 0; x < source.getWidth(); x++)
			{
				int pixel = source.getRGB(x, y);
				if ((pixel >>> 24) == 0 && (pixel & 0x00FFFFFF) != 0)
				{
					pixel |= 0xFF000000;
				}
				repaired.setRGB(x, y, pixel);
			}
		}
		return repaired;
	}

	private String getStatsTooltipHtml(Profile p)
	{
		StringBuilder sb = new StringBuilder("<html><body style='font-family: sans-serif; background-color: #2b2b2b; color: #ffffff; padding: 5px;'>");
		sb.append("<b style='color: #ff9800;'>Skill Stats:</b><br>");
		sb.append("<table border='0' cellspacing='3' cellpadding='0'>");
		int[] levels = p.getSkillLevels();
		for (int i = 0; i < 23; i++)
		{
			if (i % 2 == 0)
			{
				sb.append("<tr>");
			}
			String name = com.osroyale.SkillConstants.SKILL_NAMES[i];
			int lvl = (levels != null && i < levels.length) ? levels[i] : 1;
			sb.append("<td style='padding-right: 10px;'>").append(name).append(": <b>").append(lvl).append("</b></td>");
			if (i % 2 == 1 || i == 22)
			{
				sb.append("</tr>");
			}
		}
		sb.append("</table></body></html>");
		return sb.toString();
	}
}
