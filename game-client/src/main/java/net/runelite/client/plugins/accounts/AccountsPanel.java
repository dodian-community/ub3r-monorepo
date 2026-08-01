package net.runelite.client.plugins.accounts;

import com.osroyale.Client;
import com.osroyale.profile.Profile;
import com.osroyale.profile.ProfileManager;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.PluginPanel;

public class AccountsPanel extends PluginPanel
{
	// PluginPanel's no-arg constructor (wrap=true) wraps this panel in its own outer JScrollPane.
	// This class already builds its own inner JScrollPane around accountsContainer below, so the
	// default left two nested scroll panes fighting over mouse wheel / trackpad scroll events -
	// the outer one (with nothing of its own to scroll) intercepted the event and the inner one
	// never saw it, so the wheel/trackpad did nothing. super(false) opts out of the outer wrap,
	// same pattern PluginListPanel/PluginHubPanel/ConfigPanel use for the same reason.
	public AccountsPanel()
	{
		super(false);
	}

	// ── Palette ───────────────────────────────────────────────────────────
	private static final Color BG_DEEP        = new Color(14, 17, 22);
	private static final Color BG_CARD        = new Color(20, 25, 33);
	private static final Color BG_CARD_HOVER  = new Color(26, 32, 43);
	private static final Color BG_PREVIEW     = new Color(8, 10, 15);
	private static final Color BORDER_SUBTLE  = new Color(38, 48, 62);
	private static final Color BORDER_ACCENT  = new Color(255, 152, 0, 140);
	private static final Color ACCENT_ORANGE  = new Color(255, 152, 0);
	private static final Color ACCENT_GOLD    = new Color(255, 200, 80);
	private static final Color TEXT_SECONDARY = new Color(140, 152, 168);
	private static final Color BTN_LOGIN_BG   = new Color(35, 100, 55);
	private static final Color BTN_LOGIN_HOV  = new Color(45, 130, 70);
	private static final Color BTN_EDIT_BG    = new Color(35, 55, 90);
	private static final Color BTN_EDIT_HOV   = new Color(48, 75, 120);

	// ── True semi-transparent popup colors ────────────────────────────────
	// These are drawn on a transparent JWindow so alpha is real
	private static final Color POPUP_BG     = new Color(12, 16, 22, 195);
	private static final Color POPUP_BORDER = new Color(255, 152, 0, 110);

	// ── Fonts ─────────────────────────────────────────────────────────────
	private static final Font FONT_TITLE = new Font("Arial", Font.BOLD,  14);
	private static final Font FONT_NAME  = new Font("Arial", Font.BOLD,  12);
	private static final Font FONT_META  = new Font("Arial", Font.PLAIN, 10);
	private static final Font FONT_BTN   = new Font("Arial", Font.BOLD,  11);
	private static final Font FONT_BADGE = new Font("Arial", Font.BOLD,   9);
	private static final Font FONT_PILL  = new Font("Arial", Font.BOLD,  10);
	private static final Font FONT_POPUP = new Font("Arial", Font.BOLD,  11);

	private final JPanel accountsContainer = new JPanel();
	private Client client;
	private AccountsConfig config;
	private Runnable refreshAction;

	// ─────────────────────────────────────────────────────────────────────
	public void init(Client client, AccountsConfig config, Runnable refreshAction)
	{
		this.client = client;
		this.config = config;
		this.refreshAction = refreshAction;

		// No getParent() wrapping here (unlike before super(false)): this panel isn't attached to
		// anything yet at init() time - AccountsPlugin.startUp() calls init() before handing the
		// panel to NavigationButton/ClientToolbar. With wrap=false, this *is* the panel the sidebar
		// hosts directly, so it lays itself out - same pattern as PluginListPanel/ConfigPanel.
		setLayout(new BorderLayout());
		setBackground(BG_DEEP);

		// ── Header ──────────────────────────────────────────────────────
		JPanel header = new JPanel(new BorderLayout())
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setPaint(new GradientPaint(0, 0, new Color(28, 18, 6), getWidth(), 0, new Color(8, 12, 18)));
				g2.fillRect(0, 0, getWidth(), getHeight());
				g2.setColor(new Color(255, 152, 0, 55));
				g2.fillRect(0, getHeight() - 1, getWidth(), 1);
				g2.dispose();
			}
		};
		header.setOpaque(false);
		header.setBorder(new EmptyBorder(13, 14, 11, 14));

		JLabel titleLabel = new JLabel("ACCOUNTS MANAGER");
		titleLabel.setFont(FONT_TITLE);
		titleLabel.setForeground(ACCENT_ORANGE);
		titleLabel.setHorizontalAlignment(JLabel.CENTER);
		header.add(titleLabel, BorderLayout.CENTER);
		add(header, BorderLayout.NORTH);

		// ── Scroll container ─────────────────────────────────────────────
		accountsContainer.setLayout(new BoxLayout(accountsContainer, BoxLayout.Y_AXIS));
		accountsContainer.setBackground(BG_DEEP);
		accountsContainer.setBorder(new EmptyBorder(10, 8, 10, 8));

		JScrollPane scrollPane = new JScrollPane(accountsContainer);
		scrollPane.setBackground(BG_DEEP);
		scrollPane.getViewport().setBackground(BG_DEEP);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.getVerticalScrollBar().setUnitIncrement(14);
		add(scrollPane, BorderLayout.CENTER);

		updatePanel();
	}

	// ─────────────────────────────────────────────────────────────────────
	public void updatePanel()
	{
		accountsContainer.removeAll();

		int count = 0;
		if (ProfileManager.profiles != null)
		{
			for (Profile p : ProfileManager.profiles)
			{
				if (p == null || p.emptySlot()) continue;
				accountsContainer.add(createAccountCard(p));
				accountsContainer.add(Box.createRigidArea(new Dimension(0, 10)));
				count++;
			}
		}

		if (count == 0) accountsContainer.add(buildEmptyState());

		accountsContainer.add(Box.createRigidArea(new Dimension(0, 6)));
		accountsContainer.add(buildRefreshButton());
		accountsContainer.revalidate();
		accountsContainer.repaint();
	}

	// ── Empty state ───────────────────────────────────────────────────────
	private JPanel buildEmptyState()
	{
		JPanel p = new JPanel(new BorderLayout());
		p.setBackground(BG_DEEP);
		p.setBorder(new EmptyBorder(32, 10, 24, 10));
		JLabel lbl = new JLabel(
			"<html><center><span style='color:#556;font-size:10px;'>"
			+ "No saved profiles found.<br/>Log in to save your first account.</span></center></html>");
		lbl.setHorizontalAlignment(JLabel.CENTER);
		p.add(lbl, BorderLayout.CENTER);
		return p;
	}

	// ── Refresh button ────────────────────────────────────────────────────
	private JButton buildRefreshButton()
	{
		JButton btn = new JButton("⟳  Refresh")
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				Color bg = getModel().isPressed() ? new Color(45, 55, 72)
					: getModel().isRollover()     ? new Color(33, 42, 56)
					:                               new Color(24, 31, 42);
				g2.setColor(bg);
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
				g2.setColor(new Color(55, 72, 98));
				g2.setStroke(new BasicStroke(1f));
				g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
				g2.dispose();
				super.paintComponent(g);
			}
		};
		btn.setFont(FONT_BTN);
		btn.setForeground(TEXT_SECONDARY);
		btn.setOpaque(false);
		btn.setContentAreaFilled(false);
		btn.setBorderPainted(false);
		btn.setFocusPainted(false);
		btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
		btn.setBorder(new EmptyBorder(7, 20, 7, 20));
		btn.setAlignmentX(CENTER_ALIGNMENT);
		btn.addActionListener(e -> refreshAction.run());
		return btn;
	}

	// ── Account card ──────────────────────────────────────────────────────
	private JPanel createAccountCard(Profile p)
	{
		JPanel card = new JPanel(new BorderLayout(0, 0))
		{
			private boolean hovered = false;
			{
				addMouseListener(new MouseAdapter()
				{
					@Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
					@Override public void mouseExited (MouseEvent e) { hovered = false; repaint(); }
				});
			}

			@Override
			protected void paintComponent(Graphics g)
			{
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(hovered ? BG_CARD_HOVER : BG_CARD);
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
				g2.setColor(BORDER_ACCENT);
				g2.setStroke(new BasicStroke(1.5f));
				g2.drawLine(14, 0, getWidth() - 14, 0);
				g2.setColor(BORDER_SUBTLE);
				g2.setStroke(new BasicStroke(1f));
				g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
				g2.dispose();
			}
		};
		card.setOpaque(false);
		card.setBorder(new EmptyBorder(8, 8, 8, 8));
		card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 265));

		// ── Name + date banner ─────────────────────────────────────────
		JPanel nameBanner = new JPanel(new BorderLayout());
		nameBanner.setOpaque(false);
		nameBanner.setBorder(new EmptyBorder(0, 0, 7, 0));

		JLabel nameLabel = new JLabel(com.osroyale.Utility.formatName(p.getUsername()));
		nameLabel.setFont(FONT_NAME);
		nameLabel.setForeground(ACCENT_GOLD);
		nameBanner.add(nameLabel, BorderLayout.WEST);

		String dateStr = "—";
		if (p.getLastLogin() > 0)
			dateStr = new SimpleDateFormat("MMM d, yyyy").format(new Date(p.getLastLogin()));

		JLabel dateLabel = new JLabel(dateStr);
		dateLabel.setFont(FONT_META);
		dateLabel.setForeground(TEXT_SECONDARY);
		nameBanner.add(dateLabel, BorderLayout.EAST);

		card.add(nameBanner, BorderLayout.NORTH);

		// ── Character preview (NO tooltip) ────────────────────────────
		JPanel previewWrapper = new JPanel(new BorderLayout())
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				int w = getWidth();
				int h = getHeight();

				// 1. Rich dark slate background gradient
				g2.setPaint(new GradientPaint(0, 0, new Color(18, 24, 34), 0, h, new Color(9, 12, 18)));
				g2.fillRoundRect(0, 0, w, h, 10, 10);

				// 2. Warm spotlight aura behind character
				g2.setPaint(new GradientPaint(w / 2f, h * 0.4f, new Color(255, 160, 30, 22), w / 2f, h, new Color(0, 0, 0, 0)));
				g2.fillRoundRect(0, 0, w, h, 10, 10);

				// 3. Subtle floor pedestal shadow under feet
				int shadowY = h - 14;
				g2.setColor(new Color(0, 0, 0, 160));
				g2.fillOval(w / 2 - 44, shadowY - 7, 88, 14);
				g2.setColor(new Color(255, 170, 0, 45));
				g2.setStroke(new BasicStroke(1.2f));
				g2.drawOval(w / 2 - 44, shadowY - 7, 88, 14);

				// 4. Subtle top highlight line & outer border
				g2.setColor(new Color(255, 180, 50, 60));
				g2.drawLine(15, 1, w - 15, 1);

				g2.setColor(new Color(42, 54, 72));
				g2.setStroke(new BasicStroke(1f));
				g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);

				g2.dispose();
			}
		};
		previewWrapper.setOpaque(false);
		previewWrapper.setPreferredSize(new Dimension(0, 150));

		JLabel bodyLabel = new JLabel();
		bodyLabel.setHorizontalAlignment(JLabel.CENTER);

		File bodyImgFile = new File(net.runelite.client.RuneLite.PROFILES_DIR,
			p.getUsername().toLowerCase() + "_body.png");
		if (bodyImgFile.exists())
		{
			try
			{
				BufferedImage img = normalizeLegacyPreview(ImageIO.read(bodyImgFile));
				if (img != null)
				{
					int maxW = 115, maxH = 145;
					double scale = Math.min(maxW / (double) img.getWidth(), maxH / (double) img.getHeight());
					int sw = Math.max(1, (int) (img.getWidth()  * scale));
					int sh = Math.max(1, (int) (img.getHeight() * scale));
					BufferedImage hi = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
					Graphics2D sg = hi.createGraphics();
					sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
					sg.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
					sg.drawImage(img, 0, 0, sw, sh, null);
					sg.dispose();
					bodyLabel.setIcon(new ImageIcon(hi));
				}
			}
			catch (Exception ex)
			{
				setNoPreview(bodyLabel);
			}
		}
		else
		{
			setNoPreview(bodyLabel);
		}

		previewWrapper.add(bodyLabel, BorderLayout.CENTER);
		card.add(previewWrapper, BorderLayout.CENTER);

		// ── Bottom row: stats pill | combat badge | manage button ──────
		JPanel bottomRow = new JPanel(new BorderLayout(5, 0));
		bottomRow.setOpaque(false);
		bottomRow.setBorder(new EmptyBorder(7, 0, 0, 0));

		// Stats pill — the ONLY trigger for the skill popup
		JPanel statsPill = buildStatsPill(p);
		bottomRow.add(statsPill, BorderLayout.CENTER);

		// Combat level badge
		int[] lvls = p.getSkillLevels();
		JLabel combatBadge = buildCombatBadge(calcCombatLevel(lvls));
		bottomRow.add(combatBadge, BorderLayout.EAST);

		// ── Action buttons ─────────────────────────────────────────────
		// BorderLayout instead of GridLayout: with Delete folded into the gear menu below, this
		// is at most 2 buttons of very different natural widths (Login's text vs. a small icon),
		// and equal-width grid columns were what pushed the row wider than the sidebar. CENTER
		// lets Login take the remaining space; EAST keeps the icon button at its own compact size
		// whether or not Login is present.
		JPanel btnRow = new JPanel(new BorderLayout(5, 0));
		btnRow.setOpaque(false);
		btnRow.setBorder(new EmptyBorder(5, 0, 0, 0));

		if (!Client.loggedIn)
		{
			JButton loginBtn = buildStyledButton("Login", BTN_LOGIN_BG, BTN_LOGIN_HOV, Color.WHITE);
			loginBtn.addActionListener(e -> {
				Client osClient = (Client) client;
				osClient.myUsername = p.getUsername();
				osClient.myPassword = p.getPassword();
				osClient.attemptLogin(osClient.myUsername, osClient.myPassword, false);
			});
			btnRow.add(loginBtn, BorderLayout.CENTER);
		}

		// Account options (edit password + delete) - both live behind this one icon button now.
		JButton manageBtn = buildIconButton(
			"/net/runelite/client/plugins/config/config_edit_icon.png",
			BTN_EDIT_BG, BTN_EDIT_HOV);
		manageBtn.setToolTipText("Account options");
		manageBtn.addActionListener(e -> showAccountOptionsMenu(manageBtn, p));
		btnRow.add(manageBtn, BorderLayout.EAST);

		// Stack bottom row + buttons
		JPanel southPanel = new JPanel();
		southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));
		southPanel.setOpaque(false);
		southPanel.add(bottomRow);
		southPanel.add(btnRow);
		card.add(southPanel, BorderLayout.SOUTH);

		return card;
	}

	// ── Stats pill (hover triggers transparent JWindow popup) ─────────────
	private JPanel buildStatsPill(Profile p)
	{
		// Load overall icon for the pill
		ImageIcon pillIcon = loadScaledIcon("/skill_icons_small/overall.png", 14, 14);

		JLabel pillLabel = new JLabel(pillIcon);
		pillLabel.setText(" Skills");
		pillLabel.setFont(FONT_PILL);
		pillLabel.setForeground(new Color(175, 190, 210));
		pillLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

		JPanel pill = new JPanel(new BorderLayout())
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(new Color(28, 36, 50));
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
				g2.setColor(new Color(50, 68, 95));
				g2.setStroke(new BasicStroke(1f));
				g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
				g2.dispose();
			}
		};
		pill.setOpaque(false);
		pill.setBorder(new EmptyBorder(3, 10, 3, 10));
		pill.add(pillLabel, BorderLayout.CENTER);

		// Build the popup content once
		String[] skills = buildSkillLinesData(p);

		// Hover listener — show/hide a real transparent JWindow
		pill.addMouseListener(new MouseAdapter()
		{
			private JWindow popup;

			@Override
			public void mouseEntered(MouseEvent e)
			{
				if (p.getLastLogin() <= 0) return;
				popup = buildSkillsPopup(skills, p);
				// Position the popup to the right of the pill, aligned to its top
				Point loc;
				try { loc = pill.getLocationOnScreen(); }
				catch (Exception ex) { return; }
				popup.setLocation(loc.x + pill.getWidth() + 4, loc.y - 10);
				popup.setVisible(true);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				if (popup != null)
				{
					popup.dispose();
					popup = null;
				}
			}
		});

		return pill;
	}

	// ── Build the transparent JWindow popup with skill grid ───────────────
	private JWindow buildSkillsPopup(String[] skillLines, Profile p)
	{
		JWindow win = new JWindow();
		// This is the key: make the OS window itself transparent so our
		// alpha background actually shows through
		win.setBackground(new Color(0, 0, 0, 0));

		JPanel content = new JPanel()
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(POPUP_BG);                                       // semi-transparent fill
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
				g2.setColor(POPUP_BORDER);
				g2.setStroke(new BasicStroke(1.2f));
				g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 14, 14);
				g2.dispose();
				// Do NOT call super.paintComponent — we own the background
			}
		};
		content.setOpaque(false);
		content.setLayout(new BorderLayout(0, 4));
		content.setBorder(new EmptyBorder(8, 10, 8, 10));

		// Title row
		int[] levels = p.getSkillLevels();
		int total = 0;
		if (levels != null) for (int lv : levels) total += lv;

		JLabel title = new JLabel("Skills   Total: " + total);
		title.setFont(FONT_POPUP);
		title.setForeground(new Color(255, 170, 0));
		title.setHorizontalAlignment(JLabel.CENTER);
		content.add(title, BorderLayout.NORTH);

		// 3-column grid of skill rows (icon + level)
		JPanel grid = new JPanel(new GridLayout(0, 3, 3, 3));
		grid.setOpaque(false);

		for (int i = 0; i < 23; i++)
		{
			String iconName = getSkillIconName(i);
			int lvl = (levels != null && i < levels.length) ? levels[i] : 1;
			String col = lvl >= 99 ? "#ffd700" : lvl >= 70 ? "#ffaa00" : "#d8d8d8";

			JPanel cell = new JPanel(new BorderLayout(2, 0))
			{
				@Override
				protected void paintComponent(Graphics g)
				{
					Graphics2D g2 = (Graphics2D) g.create();
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g2.setColor(new Color(255, 255, 255, 8));
					g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
					g2.dispose();
				}
			};
			cell.setOpaque(false);
			cell.setBorder(new EmptyBorder(3, 5, 3, 5));

			ImageIcon icon = loadScaledIcon("/skill_icons_small/" + iconName + ".png", 16, 16);
			JLabel iconLbl = icon != null ? new JLabel(icon) : new JLabel();
			cell.add(iconLbl, BorderLayout.WEST);

			JLabel lvlLbl = new JLabel("<html><b><font color='" + col + "'>" + lvl + "</font></b></html>");
			lvlLbl.setFont(FONT_PILL);
			cell.add(lvlLbl, BorderLayout.CENTER);

			grid.add(cell);
		}
		content.add(grid, BorderLayout.CENTER);

		win.add(content);
		win.pack();
		return win;
	}

	// ── Combat level badge ────────────────────────────────────────────────
	private JLabel buildCombatBadge(int combatLvl)
	{
		ImageIcon combatIcon = loadScaledIcon("/skill_icons_small/combat.png", 12, 12);
		JLabel badge = new JLabel("Combat lvl: " + combatLvl, combatIcon, JLabel.LEFT)
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(new Color(55, 28, 8));
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
				g2.setColor(new Color(180, 80, 20, 140));
				g2.setStroke(new BasicStroke(1f));
				g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
				g2.dispose();
				super.paintComponent(g);
			}
		};
		badge.setFont(FONT_BADGE);
		badge.setForeground(new Color(255, 155, 55));
		badge.setOpaque(false);
		badge.setBorder(new EmptyBorder(3, 7, 3, 7));
		return badge;
	}

	// ── Account options menu (edit password / delete) ─────────────────────
	private void showAccountOptionsMenu(JButton anchor, Profile p)
	{
		JPopupMenu menu = new JPopupMenu();
		menu.setBackground(BG_CARD);
		menu.setBorder(BorderFactory.createLineBorder(BORDER_SUBTLE));

		JMenuItem editItem = new JMenuItem("Edit Password");
		styleMenuItem(editItem, Color.WHITE);
		editItem.addActionListener(e -> showEditPasswordDialog(p));
		menu.add(editItem);

		JMenuItem deleteItem = new JMenuItem("Delete Account");
		styleMenuItem(deleteItem, new Color(255, 140, 140));
		deleteItem.addActionListener(e -> {
			int confirm = JOptionPane.showConfirmDialog(
				this,
				"Remove the saved login for \"" + com.osroyale.Utility.formatName(p.getUsername())
					+ "\"?\nThis only forgets it on this device - it does not delete the character.",
				"Remove saved login",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);
			if (confirm == JOptionPane.YES_OPTION)
			{
				ProfileManager.delete(p);
				updatePanel();
			}
		});
		menu.add(deleteItem);

		menu.show(anchor, 0, anchor.getHeight());
	}

	private void styleMenuItem(JMenuItem item, Color fg)
	{
		item.setFont(FONT_BTN);
		item.setBackground(BG_CARD);
		item.setForeground(fg);
		item.setBorder(new EmptyBorder(6, 12, 6, 12));
	}

	// ── Edit password dialog ──────────────────────────────────────────────
	private void showEditPasswordDialog(Profile p)
	{
		JPasswordField pwField = new JPasswordField(20);
		pwField.setBackground(new Color(20, 25, 33));
		pwField.setForeground(new Color(235, 235, 235));
		pwField.setCaretColor(ACCENT_ORANGE);
		pwField.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(60, 80, 110)),
			new EmptyBorder(4, 6, 4, 6)));

		Object[] message = {
			new JLabel("<html><b style='color:#ffaa00'>Edit Password</b><br/>"
				+ "<small style='color:#888'>Account: " + com.osroyale.Utility.formatName(p.getUsername())
				+ "</small></html>"),
			pwField
		};

		int result = JOptionPane.showConfirmDialog(
			SwingUtilities.getWindowAncestor(this),
			message,
			"Edit Account Password",
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.PLAIN_MESSAGE);

		if (result == JOptionPane.OK_OPTION)
		{
			String newPassword = new String(pwField.getPassword()).trim();
			if (newPassword.isEmpty())
			{
				JOptionPane.showMessageDialog(
					SwingUtilities.getWindowAncestor(this),
					"Password cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			// Reconstruct Profile with updated password
			Profile updated = new Profile(
				p.getUsername(), newPassword, p.getGender(),
				p.getEquipment(), p.getRecolours(),
				p.getSkillLevels(), p.getLastLogin());
			ProfileManager.add(updated);  // add() replaces by username and calls save()
			updatePanel();
		}
	}

	// ── Styled button ─────────────────────────────────────────────────────
	private JButton buildStyledButton(String text, Color normalBg, Color hoverBg, Color fg)
	{
		JButton btn = new JButton(text)
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				Color bg = getModel().isPressed() ? normalBg.darker()
					: getModel().isRollover()     ? hoverBg
					:                               normalBg;
				g2.setColor(bg);
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), 7, 7);
				g2.setColor(bg.brighter());
				g2.setStroke(new BasicStroke(0.8f));
				g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 7, 7);
				g2.dispose();
				super.paintComponent(g);
			}
		};
		btn.setFont(FONT_BTN);
		btn.setForeground(fg);
		btn.setOpaque(false);
		btn.setContentAreaFilled(false);
		btn.setBorderPainted(false);
		btn.setFocusPainted(false);
		btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
		btn.setBorder(new EmptyBorder(6, 10, 6, 10));
		return btn;
	}

	// ── Icon-only button ──────────────────────────────────────────────────
	private JButton buildIconButton(String iconPath, Color normalBg, Color hoverBg)
	{
		ImageIcon icon = loadScaledIcon(iconPath, 14, 14);
		JButton btn = new JButton(icon)
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				Color bg = getModel().isPressed() ? normalBg.darker()
					: getModel().isRollover()     ? hoverBg
					:                               normalBg;
				g2.setColor(bg);
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), 7, 7);
				g2.setColor(bg.brighter());
				g2.setStroke(new BasicStroke(0.8f));
				g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 7, 7);
				g2.dispose();
				super.paintComponent(g);
			}
		};
		if (icon == null) btn.setText("⚙");
		btn.setOpaque(false);
		btn.setContentAreaFilled(false);
		btn.setBorderPainted(false);
		btn.setFocusPainted(false);
		btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
		btn.setBorder(new EmptyBorder(6, 8, 6, 8));
		return btn;
	}

	// ── Icon loader helper ────────────────────────────────────────────────
	private ImageIcon loadScaledIcon(String resourcePath, int w, int h)
	{
		try
		{
			URL url = getClass().getResource(resourcePath);
			if (url == null) return null;
			BufferedImage img = ImageIO.read(url);
			if (img == null) return null;
			BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2 = scaled.createGraphics();
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
			g2.drawImage(img, 0, 0, w, h, null);
			g2.dispose();
			return new ImageIcon(scaled);
		}
		catch (IOException | IllegalArgumentException e)
		{
			return null;
		}
	}

	// ── Combat level OSRS formula ─────────────────────────────────────────
	private static int calcCombatLevel(int[] lvls)
	{
		if (lvls == null || lvls.length < 7) return 3;
		int atk = lvls[0], def = lvls[1], str = lvls[2], hp = lvls[3];
		int range = lvls[4], prayer = lvls[5], magic = lvls[6];
		double base   = 0.25 * (def + hp + Math.floor(prayer / 2.0));
		double melee  = 0.325 * (atk + str);
		double ranged = 0.325 * Math.floor(range  * 1.5);
		double mage   = 0.325 * Math.floor(magic  * 1.5);
		return (int) Math.floor(base + Math.max(melee, Math.max(ranged, mage)));
	}

	// ── Build skill lines (pre-computed for popup) ────────────────────────
	private String[] buildSkillLinesData(Profile p)
	{
		// Not used in the new JPanel popup, kept for potential future HTML tooltips
		return new String[0];
	}

	// ── Legacy image alpha repair ─────────────────────────────────────────
	static BufferedImage normalizeLegacyPreview(BufferedImage source)
	{
		if (source == null) return null;
		boolean needsRepair = false;
		outer:
		for (int y = 0; y < source.getHeight(); y++)
		{
			for (int x = 0; x < source.getWidth(); x++)
			{
				int px = source.getRGB(x, y);
				if ((px >>> 24) == 0 && (px & 0x00FFFFFF) != 0) { needsRepair = true; break outer; }
			}
		}
		if (!needsRepair) return source;

		BufferedImage fixed = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < source.getHeight(); y++)
		{
			for (int x = 0; x < source.getWidth(); x++)
			{
				int px = source.getRGB(x, y);
				if ((px >>> 24) == 0 && (px & 0x00FFFFFF) != 0) px |= 0xFF000000;
				fixed.setRGB(x, y, px);
			}
		}
		return fixed;
	}

	private static void setNoPreview(JLabel lbl)
	{
		lbl.setText("No Preview");
		lbl.setForeground(new Color(80, 90, 105));
		lbl.setFont(FONT_META);
	}

	// ── Skill icon name lookup ────────────────────────────────────────────
	private String getSkillIconName(int index)
	{
		switch (index)
		{
			case  0: return "attack";
			case  1: return "defence";
			case  2: return "strength";
			case  3: return "hitpoints";
			case  4: return "ranged";
			case  5: return "prayer";
			case  6: return "magic";
			case  7: return "cooking";
			case  8: return "woodcutting";
			case  9: return "fletching";
			case 10: return "fishing";
			case 11: return "firemaking";
			case 12: return "crafting";
			case 13: return "smithing";
			case 14: return "mining";
			case 15: return "herblore";
			case 16: return "agility";
			case 17: return "thieving";
			case 18: return "slayer";
			case 19: return "farming";
			case 20: return "runecraft";
			case 21: return "construction";
			case 22: return "hunter";
			default: return "overall";
		}
	}
}
