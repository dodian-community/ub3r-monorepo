package com.runescape;

import java.applet.AppletContext;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.stream.*;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import org.seven.cache.graphics.RSFont;
import org.seven.scene.graphic.Fog;
import org.seven.util.CacheUtils;

import com.runescape.cache.FileArchive;
import com.runescape.cache.FileStore;
import com.runescape.cache.FileStore.Store;
import com.runescape.cache.anim.Animation;
import com.runescape.cache.anim.Frame;
import com.runescape.cache.anim.Graphic;
import com.runescape.cache.config.VariableBits;
import com.runescape.cache.config.VariablePlayer;
import com.runescape.cache.def.Definitions;
import com.runescape.cache.def.FloorDefinition;
import com.runescape.cache.def.ItemDefinition;
import com.runescape.cache.def.NpcDefinition;
import com.runescape.cache.def.ObjectDefinition;
import com.runescape.cache.graphics.GameFont;
import com.runescape.cache.graphics.IndexedImage;
import com.runescape.cache.graphics.Sprite;
import com.runescape.cache.graphics.SpriteLoader;
import com.runescape.cache.graphics.Widget;
import com.runescape.collection.Deque;
import com.runescape.collection.Linkable;
import com.runescape.draw.ProducingGraphicsBuffer;
import com.runescape.draw.Rasterizer2D;
import com.runescape.draw.Rasterizer3D;
import com.runescape.draw.skillorbs.SkillOrbs;
import com.runescape.entity.GameObject;
import com.runescape.entity.Item;
import com.runescape.entity.Mob;
import com.runescape.entity.Npc;
import com.runescape.entity.Player;
import com.runescape.entity.Renderable;
import com.runescape.entity.model.IdentityKit;
import com.runescape.entity.model.Model;
import com.runescape.io.Buffer;
import com.runescape.io.ByteBuffer;
import com.runescape.io.jaggrab.JagGrab;
import com.runescape.io.packets.outgoing.OutgoingPacket;
import com.runescape.io.packets.outgoing.impl.AddFriend;
import com.runescape.io.packets.outgoing.impl.AddIgnore;
import com.runescape.io.packets.outgoing.impl.AttackNpc;
import com.runescape.io.packets.outgoing.impl.BankTabCreation;
import com.runescape.io.packets.outgoing.impl.BasicPing;
import com.runescape.io.packets.outgoing.impl.ChangeAppearance;
import com.runescape.io.packets.outgoing.impl.Chat;
import com.runescape.io.packets.outgoing.impl.ChatSettings;
import com.runescape.io.packets.outgoing.impl.ChatboxDuel;
import com.runescape.io.packets.outgoing.impl.ClickButton;
import com.runescape.io.packets.outgoing.impl.ClickButtonAction;
import com.runescape.io.packets.outgoing.impl.CloseInterface;
import com.runescape.io.packets.outgoing.impl.Command;
import com.runescape.io.packets.outgoing.impl.DeleteFriend;
import com.runescape.io.packets.outgoing.impl.DeleteIgnore;
import com.runescape.io.packets.outgoing.impl.DropItem;
import com.runescape.io.packets.outgoing.impl.EnterAmount;
import com.runescape.io.packets.outgoing.impl.EquipItem;
import com.runescape.io.packets.outgoing.impl.ExamineItem;
import com.runescape.io.packets.outgoing.impl.ExamineNpc;
import com.runescape.io.packets.outgoing.impl.FinalizedRegionChange;
import com.runescape.io.packets.outgoing.impl.FollowPlayer;
import com.runescape.io.packets.outgoing.impl.ItemContainerOption1;
import com.runescape.io.packets.outgoing.impl.ItemContainerOption2;
import com.runescape.io.packets.outgoing.impl.ItemContainerOption3;
import com.runescape.io.packets.outgoing.impl.ItemContainerOption4;
import com.runescape.io.packets.outgoing.impl.ItemContainerOption5;
import com.runescape.io.packets.outgoing.impl.ItemOnGroundItem;
import com.runescape.io.packets.outgoing.impl.ItemOnItem;
import com.runescape.io.packets.outgoing.impl.ItemOnNpc;
import com.runescape.io.packets.outgoing.impl.ItemOnObject;
import com.runescape.io.packets.outgoing.impl.ItemOnPlayer;
import com.runescape.io.packets.outgoing.impl.ItemOption2;
import com.runescape.io.packets.outgoing.impl.ItemOption3;
import com.runescape.io.packets.outgoing.impl.MagicOnGroundItem;
import com.runescape.io.packets.outgoing.impl.MagicOnItem;
import com.runescape.io.packets.outgoing.impl.MagicOnNpc;
import com.runescape.io.packets.outgoing.impl.MagicOnPlayer;
import com.runescape.io.packets.outgoing.impl.NextDialogue;
import com.runescape.io.packets.outgoing.impl.NpcOption1;
import com.runescape.io.packets.outgoing.impl.NpcOption2;
import com.runescape.io.packets.outgoing.impl.NpcOption3;
import com.runescape.io.packets.outgoing.impl.NpcOption4;
import com.runescape.io.packets.outgoing.impl.ObjectOption1;
import com.runescape.io.packets.outgoing.impl.ObjectOption2;
import com.runescape.io.packets.outgoing.impl.ObjectOption3;
import com.runescape.io.packets.outgoing.impl.ObjectOption4;
import com.runescape.io.packets.outgoing.impl.ObjectOption5;
import com.runescape.io.packets.outgoing.impl.OperateItem;
import com.runescape.io.packets.outgoing.impl.PickupItem;
import com.runescape.io.packets.outgoing.impl.PlayerAttackOption;
import com.runescape.io.packets.outgoing.impl.PlayerInactive;
import com.runescape.io.packets.outgoing.impl.PlayerOption1;
import com.runescape.io.packets.outgoing.impl.PrivateMessage;
import com.runescape.io.packets.outgoing.impl.RegionChange;
import com.runescape.io.packets.outgoing.impl.SendButtonAndId;
import com.runescape.io.packets.outgoing.impl.SendSyntax;
import com.runescape.io.packets.outgoing.impl.SpawnTabClick;
import com.runescape.io.packets.outgoing.impl.SpecialAttack;
import com.runescape.io.packets.outgoing.impl.SwitchItemSlot;
import com.runescape.io.packets.outgoing.impl.TradePlayer;
import com.runescape.io.packets.outgoing.impl.UpdatePlane;
import com.runescape.io.packets.outgoing.impl.UseItem;
import com.runescape.model.EffectTimer;
import com.runescape.model.WeaponInterface;
import com.runescape.model.content.TabBindings;
import com.runescape.net.BufferedConnection;
import com.runescape.net.IsaacCipher;
import com.runescape.net.requester.Resource;
import com.runescape.net.requester.ResourceProvider;
import com.runescape.scene.AnimableObject;
import com.runescape.scene.CollisionMap;
import com.runescape.scene.MapRegion;
import com.runescape.scene.Projectile;
import com.runescape.scene.SceneGraph;
import com.runescape.scene.SceneObject;
import com.runescape.scene.object.GroundDecoration;
import com.runescape.scene.object.SpawnedObject;
import com.runescape.scene.object.WallDecoration;
import com.runescape.scene.object.WallObject;
import com.runescape.sign.SignLink;
import com.runescape.sound.SoundPlayer;
import com.runescape.sound.Track;
import com.runescape.util.MessageCensor;
import com.runescape.util.MiscUtils;
import com.runescape.util.MouseDetection;
import com.runescape.util.PacketConstants;
import com.runescape.util.SecondsTimer;
import com.runescape.util.SkillConstants;
import com.runescape.util.StringUtils;
import java.awt.event.KeyEvent;

public class Client extends GameApplet {
	
	private Sprite top508;
	private Sprite bottom508;
	public String autochatString = "";
	public boolean world1Hover = false;
	public boolean dmmworldhover = false;
	public static List<Integer> itemIds = new ArrayList<>();
	String searchString = "null";
	float spinSpeed = 1;
	int amountValue = 0;
	int[] rewards = {1043, 1045, 1047, 1049};
	int[] skillIdsOrder = {0, 3, 14, 2, 16, 13, 1, 15, 10, 4, 17, 7, 5, 12, 11, 6, 9, 8, 20, 18, 19, 22, 21};
	int totalLevel = 32;
	int pcOpacity = 0;
	boolean maxOpacity = false;
	int itemIdToSend;
	private boolean sidebarGlow = false;
	
	public void draw508Scrollbar(int height, int pos, int y, int x, int maxScroll,
			boolean transparent) {
		 if (transparent) {
	            drawTransparentScrollBar(x, y, height, maxScroll, pos);
	        } else {
	        	
	        	top508.drawSprite(x, y);
	        	bottom508.drawSprite(x, (y + height) - 16);
	            Rasterizer2D.drawBox(x, y + 16, 16, height - 32, 0x746241);
	            Rasterizer2D.drawBox(x, y + 16, 15, height - 32, 0x77603e);
	            Rasterizer2D.drawBox(x, y + 16, 14, height - 32, 0x95784a);
	            Rasterizer2D.drawBox(x, y + 16, 12, height - 32, 0x997c52);
	            Rasterizer2D.drawBox(x, y + 16, 11, height - 32, 0x9e8155);
	            Rasterizer2D.drawBox(x, y + 16, 10, height - 32, 0xa48558);
	            Rasterizer2D.drawBox(x, y + 16, 8, height - 32, 0xaa8b5c);
	            Rasterizer2D.drawBox(x, y + 16, 6, height - 32, 0xb09060);
	            Rasterizer2D.drawBox(x, y + 16, 3, height - 32, 0x866c44);
	            Rasterizer2D.drawBox(x, y + 16, 1, height - 32, 0x7c6945);
	            
	            int k1 = ((height - 32) * height) / maxScroll;
	            if (k1 < 8) {
	                k1 = 8;
	            }
	            int l1 = ((height - 32 - k1) * pos) / (maxScroll - height);
	            int l2 = ((height - 32 - k1) * pos) / (maxScroll - height) + 6;
	            Rasterizer2D.drawVerticalLine(x + 1, y + 16 + l1, k1, 0x5c492d);
	            Rasterizer2D.drawVerticalLine(x + 14, y + 16 + l1, k1, 0x5c492d);
	            Rasterizer2D.drawHorizontalLine(x + 1, y + 16 + l1, 14, 0x5c492d);
	            Rasterizer2D.drawHorizontalLine(x + 1, y + 15 + l1 + k1, 14, 0x5c492d);
	            Rasterizer2D.drawHorizontalLine(x + 4, y + 18 + l1, 8, 0x664f2b);
	            Rasterizer2D.drawHorizontalLine(x + 4, y + 13 + l1 + k1, 8, 0x664f2b);
	            Rasterizer2D.drawHorizontalLine(x + 3, y + 19 + l1, 2, 0x664f2b);
	            Rasterizer2D.drawHorizontalLine(x + 11, y + 19 + l1, 2, 0x664f2b);
	            Rasterizer2D.drawHorizontalLine(x + 3, y + 12 + l1 + k1, 2, 0x664f2b);
	            Rasterizer2D.drawHorizontalLine(x + 11, y + 12 + l1 + k1, 2, 0x664f2b);
	            Rasterizer2D.drawHorizontalLine(x + 3, y + 14 + l1 + k1, 11, 0x866c44);
	            Rasterizer2D.drawHorizontalLine(x + 3, y + 17 + l1, 11, 0x866c44);
	            Rasterizer2D.drawVerticalLine(x + 13, y + 12 + l2, k1 - 4, 0x866c44);
	            Rasterizer2D.drawVerticalLine(x + 3, y + 13 + l2, k1 - 6, 0x664f2b);
	            Rasterizer2D.drawVerticalLine(x + 12, y + 13 + l2, k1 - 6, 0x664f2b);
	            Rasterizer2D.drawHorizontalLine(x + 2, y + 18 + l1, 2, 0x866c44);
	            Rasterizer2D.drawHorizontalLine(x + 2, y + 13 + l1 + k1, 2, 0x866c44);
	            Rasterizer2D.drawHorizontalLine(x + 12, y + 18 + l1, 1, 0x866c44);
	            Rasterizer2D.drawHorizontalLine(x + 12, y + 13 + l1 + k1, 1, 0x866c44);
	        }
	    }

	public void savePlayerData() {
		try {
			File file = new File(SignLink.findcachedir() + "/settings.dat");
			if (!file.exists()) {
				file.createNewFile();
			}
			DataOutputStream stream = new DataOutputStream(new FileOutputStream(file));
			if (stream != null) {
				stream.writeBoolean(rememberMe);
				stream.writeUTF(rememberMe ? myUsername : "");
				stream.writeBoolean(skillOrbs);
				stream.writeBoolean(expDrops);
				stream.writeBoolean(Configuration.alwaysLeftClickAttack);
				stream.writeBoolean(Configuration.combatOverlayBox);
				stream.writeBoolean(Configuration.hitmarks554);
				stream.writeBoolean(Configuration.hpBar554);
				stream.writeBoolean(Configuration.enableTweening);
				stream.writeBoolean(Configuration.enableRoofs);
				stream.writeBoolean(Configuration.enableFog);
				stream.writeBoolean(Configuration.bountyHunterInterface);
				stream.writeBoolean(Configuration.escapeCloseInterface);
				stream.writeBoolean(hideUsername); 
				stream.write(settings[166]); //Brightness
				
				for(int i = 0; i < 14; i++) {
					stream.writeByte(TabBindings.tabBindings[i]);
				}
				
				stream.close();		
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private void loadPlayerData() throws IOException {

		File file = new File(SignLink.findcachedir() + "/settings.dat");
		if (!file.exists()) {
			return;
		}

		DataInputStream stream = new DataInputStream(new FileInputStream(file));

		try {
			
			rememberMe = stream.readBoolean();
			myUsername = stream.readUTF();
			skillOrbs = stream.readBoolean();
			expDrops = stream.readBoolean();
			Configuration.alwaysLeftClickAttack = stream.readBoolean();
			Configuration.combatOverlayBox = stream.readBoolean();
			Configuration.hitmarks554 = stream.readBoolean();
			Configuration.hpBar554 = stream.readBoolean();
			Configuration.enableTweening = stream.readBoolean();
			Configuration.enableRoofs = stream.readBoolean();
			Configuration.enableFog = stream.readBoolean();
			Configuration.bountyHunterInterface = stream.readBoolean();
			Configuration.escapeCloseInterface = stream.readBoolean();
			hideUsername = stream.readBoolean();
			int brightnessState = stream.readByte();
			
			for(int i = 0; i < TabBindings.tabBindings.length; i++) {
				TabBindings.tabBindings[i] = stream.readByte();
			}
			
			if(rememberMe) {
				if(myUsername.length() > 0) {
					loginScreenCursorPos = 1;
				}
			}
			
			//Brightness
			if (brightnessState == 1) {
				Rasterizer3D.setBrightness(0.9);
			} else if (brightnessState == 2) {
				Rasterizer3D.setBrightness(0.8);
			} else if (brightnessState == 3) {
				Rasterizer3D.setBrightness(0.7);
			} else if (brightnessState == 4) {
				Rasterizer3D.setBrightness(0.6);
			}
			settings[166] = brightnessState;
			
			
		} catch (IOException e) {
			file.delete();
		} finally {
			stream.close();
		}
	}
	
	private void updateSettings() {
		settings[809] = Configuration.alwaysLeftClickAttack ? 1 : 0;
		settings[810] = Configuration.combatOverlayBox ? 1 : 0;
		settings[811] = Configuration.hitmarks554 ? 1 : 0;
		settings[812] = Configuration.hpBar554 ? 1 : 0;
		settings[813] = Configuration.enableTweening ? 1 : 0;
		settings[814] = Configuration.enableRoofs ? 1 : 0;
		settings[815] = Configuration.enableFog ? 1 : 0;
		settings[816] = Configuration.bountyHunterInterface ? 1 : 0;
		settings[817] = Configuration.escapeCloseInterface ? 1 : 0;
	}
	
	public enum ScreenMode {
		FIXED, RESIZABLE, FULLSCREEN;
	}

	private Fog fog = new Fog();

	private int[][] xp_added = new int[10][3];
	private Sprite[] skill_sprites = new Sprite[SkillConstants.SKILL_COUNT];
	private Sprite hp;

	//Timers
	public List<EffectTimer> effects_list = new CopyOnWriteArrayList<EffectTimer>();

	public void addEffectTimer(EffectTimer et) {

		//Check if exists.. If so, update delay.
		for(EffectTimer timer : effects_list) {
			if(timer.getSprite() == et.getSprite()) {
				timer.setSeconds(et.getSecondsTimer().secondsRemaining());
				return;
			}
		}

		effects_list.add(et);
	}

	public void drawEffectTimers() {
		int yDraw = frameHeight - 195;
		int xDraw = frameWidth - 330;
		for(EffectTimer timer : effects_list) {
			if(timer.getSecondsTimer().finished()) {
				effects_list.remove(timer);
				continue;
			}

			Sprite sprite = cacheSprite[timer.getSprite()];

			if(sprite != null) {
				sprite.drawAdvancedSprite(xDraw + 12, yDraw);
				newSmallFont.drawBasicString(calculateInMinutes(timer.getSecondsTimer().secondsRemaining()) + "", xDraw + 40, yDraw + 13, 0xFF8C00);
				yDraw-= 25;
			}			
		}
	}

	private String calculateInMinutes(int paramInt) {
		int i = (int) Math.floor(paramInt / 60);
		int j = paramInt - i * 60;
		String str1 = "" + i;
		String str2 = "" + j;
		if (j < 10) {
			str2 = "0" + str2;
		}
		if (i < 10) {
			str1 = "0" + str1;
		}
		return str1 + ":" + str2;
	}

	/**
	 * Draws information about our current target
	 * during combat.
	 */

	private SecondsTimer combatBoxTimer = new SecondsTimer();
	private Mob currentInteract;

	public boolean shouldDrawCombatBox() {
		if(!Configuration.combatOverlayBox) {
			return false;
		}
		return currentInteract != null && !combatBoxTimer.finished();
	}

	public void drawCombatBox() {
		//Get health..
		int currentHp = currentInteract.currentHealth;
		int maxHp = currentInteract.maxHealth;

		//Make sure the mob isn't dead!
		if(currentHp == 0) {
			return;
		}

		//Get name..
		String name = null;
		if(currentInteract instanceof Player) {
			name = ((Player)currentInteract).name;
		} else if(currentInteract instanceof Npc) {
			if(((Npc)currentInteract).desc != null) {
				name = ((Npc)currentInteract).desc.name;
			}
		}

		//Make sure the mob has a name!
		if(name == null) {
			return;
		}

		//Positioning..
		int height = 40;
		int width = 126;
		int xPos = 2;
		int yPos = 18;

		if(Client.instance.openWalkableInterface == 23300) {
			xPos = Client.frameWidth - 382;
			yPos = 100;
		}
		
		

		//Draw box ..
		Rasterizer2D.drawTransparentBox(xPos, yPos, width, height, 000000, 70);

		//Draw name..
		if(name != null) {
			Client.instance.newBoldFont.drawCenteredString(name, xPos+(width/2), yPos + 14, 16777215, 0);
		}

		int percent = (int) (((double) currentHp / (double) maxHp) * (width - 9));
		if(percent > (width - 9)) {
			percent = (width - 9);
		}
		//Draw missing health
		Rasterizer2D.drawBox(xPos + 4, yPos + 19, width - 9, 16, 11740160);
		//Draw existing health
		Rasterizer2D.drawBox(xPos + 4, yPos + 19, percent, 16, 31744);

		//Draw health..
		Client.instance.newBoldFont.drawCenteredString(currentHp + "/" + maxHp, xPos+(width/2), yPos + 32, 16777215, 0);

	}
	
	//Bindings
	private void processKeyBindings() {
		for(int i = 0; i < TabBindings.tabBindings.length; i++) {
			int bound = TabBindings.tabBindings[i];
			String boundName = "";
			
			if(bound == -1) {
				boundName = "---";
			} else if(bound >= 112 && bound <= 123) {
				int index = bound - 111;
				boundName = "F"+index;
			} else {
				boundName = "Esc";
			}
			
			Widget.interfaceCache[53112 + i].defaultText = boundName;
		}
	}
	
	//Spawn
	private String searchSyntax = "";
	private int[] searchResults = new int[10000];
	private boolean fetchSearchResults;
	private boolean searchingSpawnTab;
	private SpawnTabType spawnType = SpawnTabType.INVENTORY;
	private int totalResults = 0;
	private boolean searched = false;

	private enum SpawnTabType {
		INVENTORY,
		BANK;
	}

	/**
	 * Spawnable Items
	 */
	public static final int[] ALLOWED_SPAWNS = {2, 6, 8, 10, 12, 28, 30, 36, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 50, 52, 53, 54, 56, 58, 60, 62, 64, 66, 68, 70, 72, 91, 93, 95, 97, 99, 101, 103, 105, 107, 109, 111, 113, 115, 117, 119, 121, 123, 125, 127, 129, 131, 133, 135, 137, 139, 141, 143, 145, 147, 149, 151, 153, 155, 157, 159, 161, 163, 165, 167, 169, 171, 173, 175, 177, 179, 181, 183, 185, 187, 189, 191, 193, 197, 199, 201, 203, 205, 207, 209, 211, 213, 215, 217, 219, 221, 223, 225, 227, 229, 231, 233, 235, 237, 239, 241, 243, 245, 247, 249, 251, 253, 255, 257, 259, 261, 263, 265, 267, 269, 272, 273, 288, 299, 301, 303, 305, 307, 309, 311, 313, 314, 315, 317, 319, 321, 325, 327, 329, 331, 333, 335, 339, 341, 345, 347, 349, 351, 353, 355, 359, 361, 363, 365, 371, 373, 377, 379, 383, 385, 389, 391, 395, 397, 401, 403, 405, 407, 411, 413, 426, 428, 434, 436, 438, 440, 442, 444, 447, 449, 451, 453, 464, 526, 528, 530, 532, 534, 536, 538, 540, 542, 544, 546, 548, 554, 555, 556, 557, 558, 559, 560, 561, 562, 563, 564, 565, 566, 567, 569, 571, 573, 575, 577, 579, 581, 590, 592, 596, 621, 626, 628, 630, 632, 634, 636, 638, 640, 642, 644, 646, 648, 650, 652, 654, 656, 658, 660, 662, 664, 751, 753, 800, 801, 802, 803, 804, 805, 806, 807, 808, 809, 810, 811, 812, 813, 814, 815, 816, 817, 819, 820, 821, 822, 823, 824, 825, 826, 827, 828, 829, 830, 831, 832, 833, 834, 835, 836, 837, 839, 841, 843, 845, 847, 849, 851, 853, 855, 857, 859, 861, 863, 864, 865, 866, 867, 868, 869, 870, 871, 872, 873, 874, 875, 876, 877, 878, 879, 880, 881, 882, 883, 884, 885, 886, 887, 888, 889, 890, 891, 892, 893, 946, 948, 950, 952, 954, 958, 960, 962, 970, 973, 975, 981, 983, 985, 987, 989, 991, 993, 1005, 1007, 1009, 1011, 1013, 1015, 1017, 1019, 1021, 1023, 1025, 1027, 1029, 1031, 1033, 1035, 1038, 1040, 1042, 1044, 1046, 1048, 1050, 1053, 1055, 1057, 1059, 1061, 1063, 1065, 1067, 1069, 1071, 1073, 1075, 1077, 1079, 1081, 1083, 1085, 1087, 1089, 1091, 1093, 1095, 1097, 1099, 1101, 1103, 1105, 1107, 1109, 1111, 1113, 1115, 1117, 1119, 1121, 1123, 1125, 1127, 1129, 1131, 1133, 1135, 1137, 1139, 1141, 1143, 1145, 1147, 1149, 1151, 1153, 1155, 1157, 1159, 1161, 1163, 1165, 1167, 1169, 1171, 1173, 1175, 1177, 1179, 1181, 1183, 1185, 1187, 1189, 1191, 1193, 1195, 1197, 1199, 1201, 1203, 1205, 1207, 1209, 1211, 1213, 1215, 1217, 1219, 1221, 1223, 1225, 1227, 1229, 1231, 1233, 1237, 1239, 1241, 1243, 1245, 1247, 1249, 1251, 1253, 1255, 1257, 1259, 1261, 1263, 1265, 1267, 1269, 1271, 1273, 1275, 1277, 1279, 1281, 1283, 1285, 1287, 1289, 1291, 1293, 1295, 1297, 1299, 1301, 1303, 1305, 1307, 1309, 1311, 1313, 1315, 1317, 1319, 1321, 1323, 1325, 1327, 1329, 1331, 1333, 1335, 1337, 1339, 1341, 1343, 1345, 1347, 1349, 1351, 1353, 1355, 1357, 1359, 1361, 1363, 1365, 1367, 1369, 1371, 1373, 1375, 1377, 1379, 1381, 1383, 1385, 1387, 1389, 1391, 1393, 1395, 1397, 1399, 1401, 1403, 1405, 1407, 1420, 1422, 1424, 1426, 1428, 1430, 1432, 1434, 1436, 1438, 1440, 1442, 1444, 1446, 1448, 1452, 1454, 1456, 1462, 1464, 1470, 1472, 1474, 1476, 1478, 1511, 1513, 1515, 1517, 1519, 1521, 1523, 1539, 1540, 1550, 1552, 1573, 1592, 1595, 1597, 1599, 1601, 1603, 1605, 1607, 1609, 1611, 1613, 1615, 1617, 1619, 1621, 1623, 1625, 1627, 1629, 1631, 1635, 1637, 1639, 1641, 1643, 1645, 1654, 1656, 1658, 1660, 1662, 1664, 1673, 1675, 1677, 1679, 1681, 1683, 1692, 1694, 1696, 1698, 1700, 1702, 1704, 1712, 1714, 1716, 1718, 1720, 1722, 1724, 1725, 1727, 1729, 1731, 1733, 1734, 1735, 1737, 1739, 1741, 1743, 1745, 1747, 1749, 1751, 1753, 1755, 1757, 1759, 1761, 1763, 1765, 1767, 1769, 1771, 1773, 1775, 1777, 1779, 1781, 1783, 1785, 1787, 1789, 1791, 1793, 1794, 1823, 1831, 1833, 1835, 1837, 1854, 1859, 1861, 1865, 1869, 1871, 1873, 1875, 1877, 1879, 1881, 1885, 1887, 1891, 1897, 1905, 1907, 1909, 1911, 1913, 1915, 1917, 1919, 1921, 1923, 1925, 1927, 1929, 1931, 1933, 1935, 1937, 1939, 1941, 1942, 1944, 1947, 1949, 1951, 1953, 1955, 1957, 1959, 1961, 1963, 1965, 1969, 1971, 1973, 1975, 1978, 1980, 1982, 1985, 1987, 1989, 1993, 2003, 2007, 2011, 2015, 2017, 2019, 2021, 2023, 2025, 2026, 2028, 2030, 2032, 2034, 2036, 2038, 2040, 2048, 2054, 2064, 2074, 2080, 2084, 2092, 2102, 2104, 2106, 2108, 2110, 2112, 2114, 2116, 2118, 2120, 2122, 2124, 2126, 2128, 2130, 2132, 2134, 2136, 2138, 2140, 2142, 2150, 2152, 2162, 2164, 2165, 2166, 2167, 2169, 2171, 2185, 2187, 2191, 2195, 2203, 2205, 2209, 2213, 2217, 2219, 2221, 2223, 2225, 2227, 2229, 2231, 2233, 2235, 2237, 2239, 2241, 2243, 2253, 2255, 2259, 2277, 2281, 2283, 2289, 2293, 2297, 2301, 2307, 2309, 2313, 2315, 2317, 2319, 2321, 2323, 2325, 2327, 2337, 2341, 2343, 2347, 2349, 2351, 2353, 2355, 2357, 2359, 2361, 2363, 2366, 2368, 2370, 2428, 2430, 2432, 2434, 2436, 2438, 2440, 2442, 2444, 2446, 2448, 2450, 2452, 2454, 2456, 2458, 2460, 2462, 2464, 2466, 2468, 2470, 2472, 2474, 2476, 2481, 2483, 2485, 2487, 2489, 2491, 2493, 2495, 2497, 2499, 2501, 2503, 2505, 2507, 2509, 2520, 2522, 2524, 2526, 2550, 2552, 2568, 2570, 2572, 2577, 2579, 2581, 2583, 2585, 2587, 2589, 2591, 2593, 2595, 2597, 2599, 2601, 2603, 2605, 2607, 2609, 2611, 2613, 2615, 2617, 2619, 2621, 2623, 2625, 2627, 2629, 2631, 2633, 2635, 2637, 2639, 2641, 2643, 2645, 2647, 2649, 2651, 2653, 2655, 2657, 2659, 2661, 2663, 2665, 2667, 2669, 2671, 2673, 2675, 2859, 2861, 2862, 2864, 2865, 2866, 2876, 2878, 2890, 2894, 2896, 2898, 2900, 2902, 2904, 2906, 2908, 2910, 2912, 2914, 2916, 2918, 2920, 2922, 2924, 2926, 2928, 2930, 2932, 2934, 2936, 2938, 2940, 2942, 2955, 2961, 2970, 2972, 2974, 2976, 2997, 2998, 3000, 3002, 3004, 3008, 3010, 3012, 3014, 3016, 3018, 3020, 3022, 3024, 3026, 3028, 3030, 3032, 3034, 3036, 3038, 3040, 3042, 3044, 3046, 3049, 3051, 3053, 3054, 3093, 3094, 3095, 3096, 3097, 3098, 3099, 3100, 3101, 3105, 3107, 3122, 3123, 3125, 3138, 3140, 3142, 3144, 3157, 3159, 3162, 3183, 3188, 3190, 3192, 3194, 3196, 3198, 3200, 3202, 3204, 3211, 3216, 3226, 3228, 3239, 3325, 3327, 3329, 3331, 3333, 3335, 3337, 3339, 3341, 3343, 3345, 3347, 3349, 3351, 3353, 3355, 3357, 3359, 3361, 3363, 3365, 3367, 3369, 3371, 3373, 3379, 3381, 3385, 3387, 3389, 3391, 3393, 3396, 3398, 3400, 3402, 3404, 3406, 3408, 3410, 3412, 3414, 3420, 3422, 3424, 3426, 3428, 3430, 3432, 3434, 3436, 3438, 3440, 3442, 3444, 3446, 3448, 3470, 3472, 3473, 3474, 3475, 3476, 3477, 3478, 3479, 3480, 3481, 3483, 3485, 3486, 3488, 3678, 3749, 3751, 3753, 3755, 3759, 3761, 3763, 3765, 3767, 3769, 3771, 3773, 3775, 3777, 3779, 3781, 3783, 3785, 3787, 3789, 3791, 3793, 3795, 3797, 3799, 3801, 3803, 3827, 3828, 3829, 3830, 3831, 3832, 3833, 3834, 3835, 3836, 3837, 3838, 3853, 4012, 4014, 4016, 4087, 4089, 4091, 4093, 4095, 4097, 4099, 4101, 4103, 4105, 4107, 4109, 4111, 4113, 4115, 4117, 4119, 4121, 4123, 4125, 4127, 4129, 4131, 4151, 4153, 4156, 4161, 4162, 4164, 4166, 4168, 4170, 4207, 4212, 4224, 4298, 4300, 4302, 4304, 4306, 4308, 4310, 4315, 4317, 4319, 4321, 4323, 4325, 4327, 4329, 4331, 4333, 4335, 4337, 4339, 4341, 4343, 4345, 4347, 4349, 4351, 4353, 4355, 4357, 4359, 4361, 4363, 4365, 4367, 4369, 4371, 4373, 4375, 4377, 4379, 4381, 4383, 4385, 4387, 4389, 4391, 4393, 4395, 4397, 4399, 4401, 4403, 4405, 4407, 4409, 4411, 4413, 4417, 4419, 4421, 4423, 4436, 4438, 4440, 4456, 4458, 4460, 4517, 4522, 4525, 4527, 4529, 4532, 4535, 4537, 4540, 4542, 4544, 4546, 4548, 4551, 4580, 4582, 4585, 4587, 4591, 4593, 4595, 4600, 4608, 4627, 4668, 4675, 4684, 4687, 4689, 4694, 4695, 4696, 4697, 4698, 4699, 4708, 4710, 4712, 4714, 4716, 4718, 4720, 4722, 4724, 4726, 4728, 4730, 4732, 4734, 4736, 4738, 4740, 4745, 4747, 4749, 4751, 4753, 4755, 4757, 4759, 4773, 4778, 4783, 4788, 4793, 4798, 4803, 4812, 4819, 4820, 4821, 4822, 4823, 4824, 4825, 4827, 4830, 4832, 4834, 4842, 4844, 4846, 4848, 4850, 4860, 4866, 4872, 4878, 4884, 4890, 4896, 4902, 4908, 4914, 4920, 4926, 4932, 4938, 4944, 4950, 4956, 4962, 4968, 4974, 4980, 4986, 4992, 4998, 5001, 5003, 5014, 5016, 5018, 5024, 5026, 5028, 5030, 5032, 5034, 5036, 5038, 5040, 5042, 5044, 5046, 5048, 5050, 5052, 5075, 5096, 5097, 5098, 5099, 5100, 5101, 5102, 5103, 5104, 5105, 5106, 5280, 5281, 5282, 5283, 5284, 5285, 5286, 5287, 5288, 5289, 5290, 5291, 5292, 5293, 5294, 5295, 5296, 5297, 5298, 5299, 5300, 5301, 5302, 5303, 5304, 5305, 5306, 5307, 5308, 5309, 5310, 5311, 5312, 5313, 5314, 5315, 5316, 5318, 5319, 5320, 5321, 5322, 5323, 5324, 5325, 5329, 5331, 5341, 5343, 5345, 5418, 5438, 5458, 5478, 5496, 5497, 5498, 5499, 5500, 5501, 5502, 5503, 5504, 5516, 5521, 5523, 5525, 5527, 5529, 5531, 5533, 5535, 5537, 5539, 5541, 5543, 5547, 5574, 5575, 5576, 5616, 5617, 5618, 5619, 5620, 5621, 5622, 5623, 5624, 5625, 5626, 5627, 5628, 5629, 5630, 5631, 5632, 5633, 5634, 5635, 5636, 5637, 5638, 5639, 5640, 5641, 5642, 5643, 5644, 5645, 5646, 5647, 5648, 5649, 5650, 5651, 5652, 5653, 5654, 5655, 5656, 5657, 5658, 5659, 5660, 5661, 5662, 5663, 5664, 5665, 5666, 5667, 5668, 5670, 5672, 5674, 5676, 5678, 5680, 5682, 5686, 5688, 5690, 5692, 5694, 5696, 5698, 5700, 5704, 5706, 5708, 5710, 5712, 5714, 5716, 5718, 5720, 5722, 5724, 5726, 5728, 5730, 5734, 5736, 5739, 5741, 5743, 5745, 5747, 5749, 5751, 5753, 5755, 5757, 5759, 5761, 5763, 5765, 5767, 5769, 5777, 5785, 5793, 5801, 5809, 5817, 5825, 5833, 5841, 5849, 5857, 5865, 5873, 5881, 5889, 5897, 5905, 5913, 5921, 5929, 5931, 5933, 5935, 5937, 5940, 5943, 5945, 5947, 5949, 5952, 5954, 5956, 5958, 5968, 5970, 5972, 5974, 5980, 5982, 5984, 5986, 5988, 5992, 5994, 5996, 5998, 6000, 6002, 6004, 6006, 6008, 6010, 6012, 6014, 6016, 6018, 6032, 6034, 6036, 6038, 6043, 6045, 6047, 6049, 6051, 6055, 6061, 6062, 6128, 6129, 6130, 6131, 6133, 6135, 6137, 6139, 6141, 6143, 6145, 6147, 6149, 6151, 6153, 6155, 6157, 6159, 6161, 6163, 6165, 6167, 6169, 6171, 6173, 6211, 6213, 6215, 6235, 6237, 6257, 6259, 6279, 6281, 6283, 6285, 6287, 6289, 6291, 6297, 6299, 6305, 6306, 6311, 6313, 6315, 6317, 6319, 6322, 6324, 6326, 6328, 6330, 6332, 6333, 6335, 6337, 6339, 6341, 6343, 6345, 6347, 6349, 6351, 6353, 6355, 6357, 6359, 6361, 6363, 6365, 6367, 6369, 6371, 6373, 6375, 6377, 6379, 6382, 6384, 6386, 6388, 6390, 6392, 6394, 6396, 6398, 6400, 6402, 6404, 6406, 6408, 6410, 6412, 6414, 6416, 6418, 6420, 6470, 6472, 6474, 6476, 6522, 6523, 6524, 6525, 6526, 6527, 6528, 6562, 6563, 6568, 6571, 6573, 6575, 6577, 6579, 6581, 6583, 6585, 6587, 6589, 6591, 6593, 6595, 6597, 6599, 6601, 6603, 6605, 6607, 6609, 6611, 6613, 6615, 6617, 6619, 6621, 6623, 6625, 6627, 6629, 6631, 6633, 6667, 6681, 6685, 6687, 6689, 6691, 6693, 6697, 6701, 6703, 6705, 6724, 6729, 6731, 6733, 6735, 6737, 6739, 6750, 6752, 6760, 6762, 6764, 6794, 6809, 6812, 6814, 6889, 6891, 6908, 6910, 6912, 6914, 6916, 6918, 6920, 6922, 6924, 6959, 6962, 6971, 6973, 6975, 6977, 6979, 6981, 6983, 7051, 7054, 7056, 7058, 7060, 7062, 7064, 7066, 7068, 7070, 7072, 7074, 7076, 7078, 7080, 7082, 7084, 7086, 7088, 7110, 7112, 7114, 7116, 7122, 7124, 7126, 7128, 7130, 7132, 7134, 7136, 7138, 7158, 7159, 7162, 7168, 7170, 7176, 7178, 7186, 7188, 7196, 7198, 7206, 7208, 7216, 7218, 7223, 7225, 7228, 7319, 7321, 7323, 7325, 7327, 7329, 7330, 7331, 7332, 7334, 7336, 7338, 7340, 7342, 7344, 7346, 7348, 7350, 7352, 7354, 7356, 7358, 7360, 7362, 7364, 7366, 7368, 7370, 7372, 7374, 7376, 7378, 7380, 7382, 7384, 7386, 7388, 7390, 7392, 7394, 7396, 7398, 7399, 7400, 7416, 7418, 7433, 7435, 7437, 7439, 7441, 7443, 7445, 7447, 7449, 7451, 7466, 7468, 7521, 7566, 7568, 7650, 7660, 7662, 7664, 7666, 7668, 7759, 7761, 7763, 7765, 7767, 7769, 7771, 7801, 7919, 7936, 7939, 7944, 7946, 8007, 8008, 8009, 8010, 8011, 8012, 8013, 8014, 8015, 8016, 8017, 8018, 8019, 8020, 8021, 8417, 8419, 8421, 8423, 8425, 8427, 8429, 8431, 8433, 8435, 8437, 8439, 8441, 8443, 8445, 8447, 8449, 8451, 8453, 8455, 8457, 8459, 8461, 8496, 8498, 8500, 8502, 8504, 8506, 8508, 8510, 8512, 8514, 8516, 8518, 8520, 8522, 8524, 8526, 8528, 8530, 8532, 8534, 8536, 8538, 8540, 8542, 8544, 8546, 8548, 8550, 8552, 8554, 8556, 8558, 8560, 8562, 8564, 8566, 8568, 8570, 8572, 8574, 8576, 8578, 8580, 8582, 8584, 8586, 8588, 8590, 8592, 8594, 8596, 8598, 8600, 8602, 8604, 8606, 8608, 8610, 8612, 8614, 8616, 8618, 8620, 8622, 8624, 8626, 8628, 8630, 8632, 8634, 8636, 8638, 8640, 8642, 8644, 8646, 8648, 8778, 8780, 8782, 8784, 8786, 8788, 8790, 8792, 8794, 8837, 8872, 8874, 8876, 8878, 8880, 8882, 8901, 8921, 8924, 8925, 8926, 8927, 8928, 9003, 9004, 9026, 9028, 9030, 9032, 9034, 9036, 9038, 9040, 9042, 9044, 9050, 9052, 9075, 9140, 9141, 9142, 9143, 9144, 9145, 9174, 9177, 9179, 9181, 9183, 9185, 9187, 9188, 9189, 9190, 9191, 9192, 9193, 9194, 9236, 9238, 9239, 9240, 9241, 9242, 9243, 9244, 9245, 9287, 9288, 9289, 9290, 9291, 9292, 9294, 9295, 9296, 9297, 9298, 9299, 9301, 9302, 9303, 9304, 9305, 9306, 9336, 9337, 9338, 9339, 9340, 9341, 9342, 9375, 9377, 9378, 9379, 9380, 9381, 9382, 9416, 9418, 9419, 9420, 9423, 9425, 9427, 9429, 9431, 9434, 9436, 9438, 9440, 9442, 9444, 9446, 9448, 9450, 9452, 9454, 9457, 9459, 9461, 9463, 9465, 9469, 9470, 9472, 9475, 9629, 9634, 9636, 9638, 9640, 9642, 9644, 9666, 9668, 9670, 9672, 9674, 9676, 9678, 9729, 9731, 9733, 9735, 9736, 9739, 9741, 9743, 9745, 9843, 9844, 9845, 9846, 9847, 9848, 9849, 9850, 9851, 9852, 9853, 9854, 9855, 9856, 9857, 9858, 9859, 9860, 9861, 9862, 9863, 9864, 9865, 9866, 9867, 9978, 9980, 9986, 9988, 9994, 9996, 9998, 10000, 10002, 10004, 10006, 10008, 10010, 10012, 10014, 10016, 10018, 10020, 10025, 10029, 10031, 10033, 10034, 10035, 10037, 10039, 10041, 10043, 10045, 10047, 10049, 10051, 10053, 10055, 10057, 10059, 10061, 10063, 10065, 10067, 10069, 10071, 10075, 10077, 10079, 10081, 10083, 10085, 10087, 10088, 10089, 10090, 10091, 10093, 10095, 10097, 10099, 10101, 10103, 10105, 10107, 10109, 10111, 10113, 10115, 10117, 10119, 10121, 10123, 10125, 10127, 10129, 10132, 10134, 10136, 10138, 10142, 10143, 10144, 10145, 10146, 10147, 10148, 10149, 10150, 10156, 10158, 10159, 10280, 10282, 10284, 10286, 10288, 10290, 10292, 10294, 10296, 10298, 10300, 10302, 10304, 10306, 10308, 10310, 10312, 10314, 10316, 10318, 10320, 10322, 10324, 10326, 10327, 10330, 10332, 10334, 10336, 10338, 10340, 10342, 10344, 10346, 10348, 10350, 10352, 10354, 10362, 10364, 10366, 10368, 10370, 10372, 10374, 10376, 10378, 10380, 10382, 10384, 10386, 10388, 10390, 10392, 10394, 10396, 10398, 10400, 10402, 10404, 10406, 10408, 10410, 10412, 10414, 10416, 10418, 10420, 10422, 10424, 10426, 10428, 10430, 10432, 10434, 10436, 10438, 10440, 10442, 10444, 10446, 10448, 10450, 10452, 10454, 10456, 10458, 10460, 10462, 10464, 10466, 10468, 10470, 10472, 10474, 10476, 10496, 10564, 10589, 10808, 10810, 10812, 10814, 10816, 10818, 10820, 10822, 10824, 10826, 10828, 10891, 10925, 10927, 10929, 10931, 10937, 10952, 10954, 10956, 10958, 10973, 10978, 10981, 10999, 11037, 11061, 11065, 11069, 11072, 11074, 11076, 11079, 11085, 11088, 11090, 11092, 11095, 11105, 11113, 11115, 11118, 11126, 11128, 11130, 11133, 11200, 11205, 11212, 11227, 11228, 11229, 11230, 11231, 11232, 11233, 11234, 11235, 11237, 11238, 11240, 11242, 11244, 11246, 11248, 11250, 11252, 11254, 11256, 11260, 11280, 11284, 11286, 11324, 11326, 11328, 11330, 11332, 11334, 11335, 11367, 11369, 11371, 11373, 11375, 11377, 11379, 11382, 11384, 11386, 11389, 11391, 11393, 11396, 11398, 11400, 11403, 11405, 11407, 11410, 11412, 11414, 11417, 11419, 11429, 11431, 11433, 11435, 11437, 11439, 11441, 11443, 11445, 11447, 11449, 11451, 11453, 11455, 11457, 11459, 11461, 11463, 11465, 11467, 11469, 11471, 11473, 11475, 11477, 11479, 11481, 11483, 11485, 11487, 11489, 11491, 11493, 11495, 11497, 11499, 11501, 11503, 11505, 11507, 11509, 11511, 11513, 11515, 11517, 11519, 11521, 11523, 11785, 11787, 11789, 11791, 11798, 11802, 11804, 11806, 11808, 11810, 11812, 11814, 11816, 11818, 11820, 11822, 11824, 11826, 11828, 11830, 11832, 11834, 11836, 11838, 11840, 11874, 11875, 11876, 11889, 11902, 11905, 11908, 11920, 11924, 11926, 11928, 11929, 11930, 11931, 11932, 11933, 11934, 11936, 11940, 11943, 11951, 11953, 11955, 11957, 11959, 11960, 11962, 11964, 11968, 11972, 11978, 11980, 11990, 11992, 11994, 11998, 12000, 12002, 12004, 12007, 12193, 12195, 12197, 12199, 12201, 12203, 12205, 12207, 12209, 12211, 12213, 12215, 12217, 12219, 12221, 12223, 12225, 12227, 12229, 12231, 12233, 12235, 12237, 12239, 12241, 12243, 12245, 12247, 12249, 12251, 12253, 12255, 12257, 12259, 12261, 12263, 12265, 12267, 12269, 12271, 12273, 12275, 12277, 12279, 12281, 12283, 12285, 12287, 12289, 12291, 12293, 12295, 12297, 12299, 12301, 12303, 12305, 12307, 12309, 12311, 12313, 12315, 12317, 12319, 12321, 12323, 12325, 12327, 12329, 12331, 12333, 12335, 12337, 12339, 12341, 12343, 12345, 12347, 12349, 12351, 12353, 12355, 12357, 12359, 12361, 12363, 12365, 12367, 12369, 12371, 12373, 12375, 12377, 12379, 12381, 12383, 12385, 12387, 12389, 12391, 12393, 12395, 12397, 12399, 12402, 12403, 12404, 12405, 12406, 12407, 12408, 12409, 12410, 12411, 12412, 12422, 12424, 12426, 12428, 12430, 12432, 12434, 12437, 12439, 12441, 12443, 12445, 12447, 12449, 12451, 12453, 12455, 12460, 12462, 12464, 12466, 12468, 12470, 12472, 12474, 12476, 12478, 12480, 12482, 12484, 12486, 12488, 12490, 12492, 12494, 12496, 12498, 12500, 12502, 12504, 12506, 12508, 12510, 12512, 12514, 12516, 12518, 12520, 12522, 12524, 12526, 12528, 12530, 12532, 12534, 12536, 12538, 12540, 12596, 12598, 12601, 12603, 12605, 12613, 12614, 12615, 12616, 12617, 12618, 12619, 12620, 12621, 12622, 12623, 12624, 12625, 12627, 12629, 12631, 12633, 12635, 12640, 12642, 12695, 12697, 12699, 12701, 12746, 12757, 12759, 12761, 12763, 12769, 12771, 12775, 12776, 12777, 12778, 12779, 12780, 12781, 12782, 12783, 12786, 12789, 12798, 12800, 12802, 12804, 12817, 12819, 12821, 12823, 12825, 12827, 12829, 12831, 12833, 12846, 12849, 12851, 12863, 12865, 12867, 12869, 12871, 12873, 12875, 12877, 12879, 12881, 12883, 12885, 12900, 12902, 12905, 12907, 12909, 12911, 12913, 12915, 12917, 12919, 12922, 12924, 12927, 12929, 12932, 12934, 12936, 12938, 12960, 12962, 12964, 12966, 12968, 12970, 12972, 12974, 12976, 12978, 12980, 12982, 12984, 12986, 12988, 12990, 12992, 12994, 12996, 12998, 13000, 13002, 13004, 13006, 13008, 13010, 13012, 13014, 13016, 13018, 13020, 13022, 13024, 13026, 13028, 13030, 13032, 13034, 13036, 13038, 13040, 13042, 13044, 13046, 13048, 13050, 13052, 13054, 13056, 13058, 13060, 13062, 13064, 13066, 13149, 13151, 13153, 13155, 13157, 13159, 13161, 13163, 13165, 13167, 13169, 13171, 13173, 13175, 13190, 13227, 13229, 13231, 13233, 13235, 13237, 13239, 13245, 13256, 13263, 13265, 13267, 13269, 13271, 13277, 13383, 13385, 13387, 13389, 13391, 13421, 13431, 13439, 13441, 13448, 13451, 13454, 13457, 13460, 13463, 13466, 13469, 13472, 13475, 13478, 13481, 13484, 13487, 13490, 13493, 13496, 13499, 13502, 13505, 13508, 13511, 13573, 13576, 13652, 13657, 19478, 19481, 19484, 19486, 19488, 19490, 19493, 19496, 19501, 19529, 19532, 19535, 19538, 19541, 19544, 19547, 19550, 19553, 19570, 19572, 19574, 19576, 19578, 19580, 19582, 19584, 19586, 19589, 19592, 19595, 19598, 19601, 19604, 19607, 19610, 19613, 19615, 19617, 19619, 19621, 19623, 19625, 19627, 19629, 19631, 19653, 19656, 19662, 19665, 19669, 19672, 19701, 19707, 19724, 19727, 19912, 19915, 19918, 19921, 19924, 19927, 19930, 19933, 19936, 19943, 19946, 19949, 19952, 19955, 19958, 19961, 19964, 19967, 19970, 19973, 19976, 19979, 19982, 19985, 19988, 19991, 19994, 19997, 20002, 20005, 20008, 20011, 20014, 20017, 20020, 20023, 20026, 20029, 20032, 20035, 20038, 20041, 20044, 20047, 20050, 20053, 20056, 20059, 20062, 20065, 20068, 20071, 20074, 20077, 20080, 20083, 20086, 20089, 20092, 20095, 20098, 20101, 20104, 20107, 20110, 20113, 20116, 20119, 20122, 20125, 20128, 20131, 20134, 20137, 20140, 20143, 20146, 20149, 20152, 20155, 20158, 20161, 20166, 20169, 20172, 20175, 20178, 20181, 20184, 20187, 20190, 20193, 20196, 20199, 20202, 20205, 20208, 20211, 20214, 20217, 20220, 20223, 20226, 20229, 20232, 20235, 20238, 20240, 20243, 20246, 20251, 20254, 20257, 20260, 20263, 20266, 20269, 20272, 20275, 20376, 20379, 20382, 20385, 20433, 20436, 20439, 20442, 20517, 20520, 20590, 20595, 20716, 20718, 20724, 20727, 20730, 20733, 20736, 20739, 20749, 20756, 20849, 20997, 21000, 21003, 21006, 21009, 21012, 21015, 21018, 21021, 21024, 21028, 21034, 21043, 21047, 21049, 21079, 21081, 21084, 21087, 21090, 21093, 21096, 21099, 21102, 21105, 21108, 21111, 21114, 21117, 21120, 21123, 21126, 21129, 21140, 21143, 21146, 21157, 21160, 21163, 21166, 21177, 21180, 21183, 21202, 21257, 21270, 21279, 21298, 21301, 21304, 21316, 21318, 21320, 21322, 21324, 21326, 21332, 21334, 21336, 21338, 21347, 21350, 21352, 21387, 21477, 21480, 21483, 21486, 21488, 21490, 21504, 21512, 21515, 21518, 21521, 21543, 21545, 21555, 21622, 21626, 21634, 21637, 21643, 21646, 21649, 21652, 21684, 21690, 21730, 21733, 21736, 21739, 21742, 21745, 21754, 21802, 21804, 21807, 21810, 21813, 21817, 21820, 21838, 21880, 21882, 21892, 21895, 21902, 21905, 21918, 21921, 21924, 21926, 21928, 21930, 21932, 21934, 21936, 21938, 21940, 21942, 21944, 21946, 21948, 21950, 21952, 21955, 21957, 21959, 21961, 21963, 21965, 21967, 21969, 21971, 21973, 21975, 21978, 21981, 21984, 21987, 21994, 22003, 22006, 22097, 22100, 22103, 22106, 22111, 22118, 22121, 22124, 22192, 22195, 22198, 22201, 22204, 22209, 22212, 22215, 22218, 22221, 22224, 22231, 22236, 22239, 22246, 22251, 22254, 22257, 22260, 22263, 22266, 22269, 22272, 22275, 22278, 22281, 22284, 22290, 22294, 22296, 22299, 22302, 22305};
	public void processSpawnTab() {
		// Draw checks..
		if(searchString != searchSyntax) {
		for (int i = 41003; i < 43103; i+=4) {
			Widget w = Widget.interfaceCache[i + 3];
			w.defaultText = "";
		}
		for(int i = 41003; i < 43103; i+=4) {
			Widget.interfaceCache[(i+1)].drawingDisabled = true;
			Widget.interfaceCache[(i+2)].drawingDisabled = true;
			Widget.interfaceCache[(i+1)].tooltip = "";
			Widget.interfaceCache[(i+2)].tooltip = "";
		}

		if (fetchSearchResults) {
			Widget itemContainer = Widget.interfaceCache[33000];
			for (int i = 0; i < searchResults.length; i++) {
				searchResults[i] = -1;
			}
			totalResults = 0;
			if (searchSyntax.length() < 1) {
				Widget.interfaceCache[41000].invisible = false;
				Widget.interfaceCache[41002].invisible = true;
				Widget.interfaceCache[41001].defaultText = "Start typing the name of an item to search for it.";
				itemIds.clear();
			}
			if (searchSyntax.length() == 1 ||searchSyntax.length() == 2) {
				Widget.interfaceCache[41000].invisible = false;
				Widget.interfaceCache[41002].invisible = true;
				Widget.interfaceCache[41001].defaultText = "Too many matches found. Please refine your search.";
				itemIds.clear();
				
			}
			if (searchSyntax.length() >= 3) {
				itemIds.clear();
				try {
				for (int itemId : ALLOWED_SPAWNS) {
					ItemDefinition def = ItemDefinition.lookup(itemId);

					if (def == null || def.name == null || def.noted_item_id != -1) {
						continue;
					}
					if (def.name.toLowerCase().contains(searchSyntax)) {
						searchResults[totalResults++] = def.id;
					}
				}
				}
				
				catch(Exception e) {
					System.out.println(e);
				}
				
				for(int i = 0, id = 41006; i < totalResults; i++, id += 4) {
					Widget.interfaceCache[id].defaultText = ItemDefinition.lookup(searchResults[i]).name;
					Widget.interfaceCache[(id-2)].drawingDisabled = false;
					Widget.interfaceCache[(id-1)].drawingDisabled = false;
					Widget.interfaceCache[(id-2)].tooltip = "Select";
					Widget.interfaceCache[(id-1)].tooltip = "Select";
					
				}
				for (int j22 = 0; j22 < totalResults; j22++) {
					if(j22 == itemContainer.inventoryItemId.length) {
						break;
					}
						itemContainer.inventoryItemId[j22] = (searchResults[j22]) + 1;
						itemContainer.inventoryAmounts[j22] = 0;
					}
				for (int slot = totalResults; slot < itemContainer.inventoryItemId.length; slot++) {
					itemContainer.inventoryItemId[slot] = 0;
					itemContainer.inventoryAmounts[slot] = 0;
				}
			}
			
			fetchSearchResults = false;
	
		// Draw input
		String textInput = "";
		if (searchSyntax.length() > 0) {
			textInput = searchSyntax;
			searchString = searchSyntax;
		}

		Widget.interfaceCache[40501].defaultText = "@bla@Select an item to ask about its price: <col=000080>" + textInput + "*";
		}
		
		if(totalResults > 300) {
			Widget.interfaceCache[41000].invisible = false;
			Widget.interfaceCache[41002].invisible = true;
			Widget.interfaceCache[41001].defaultText = "Too many matches found. Please refine your search.";
			itemIds.clear();
		}
		if(totalResults == 0 && searchSyntax.length() >= 3) {
			Widget.interfaceCache[41000].invisible = false;
			Widget.interfaceCache[41002].invisible = true;
			Widget.interfaceCache[41001].defaultText = "No matches found.";
			itemIds.clear();
		}
		if(totalResults > 0 && searchSyntax.length() > 2) {
			Widget.interfaceCache[41000].invisible = true;
			Widget.interfaceCache[41002].invisible = false;
		}
		}
		Widget w = Widget.interfaceCache[41002];
		
			w.scrollMax = (totalResults / 3) * 32;
		
	}

	public int[] getResultsArray() {
		return searchSyntax.length() >= 2 ? searchResults :
			searchResults;
	}

	public static ScreenMode frameMode = ScreenMode.FIXED;
	public static int frameWidth = 765;
	public static int frameHeight = 503;
	public static int screenAreaWidth = 512;
	public static int screenAreaHeight = 334;
	public static int cameraZoom = 600;
	public static boolean showChatComponents = true;
	public static boolean showTabComponents = true;
	public static boolean changeTabArea = frameMode == ScreenMode.FIXED ? false : true;
	public static boolean changeChatArea = frameMode == ScreenMode.FIXED ? false : true;
	public static boolean transparentTabArea = false;
	private final int[] soundVolume;

	private final NumberFormat format = NumberFormat.getInstance(Locale.US);

	public static void frameMode(ScreenMode screenMode) {
		if (frameMode != screenMode) {
			frameMode = screenMode;
			if (screenMode == ScreenMode.FIXED) {
				frameWidth = 765;
				frameHeight = 503;
				cameraZoom = 600;
				SceneGraph.viewDistance = 9;
				changeChatArea = false;
				changeTabArea = false;
			} else if (screenMode == ScreenMode.RESIZABLE) {
				frameWidth = 766;
				frameHeight = 529;
				cameraZoom = 850;
				SceneGraph.viewDistance = 10;
			} else if (screenMode == ScreenMode.FULLSCREEN) {
				cameraZoom = 600;
				SceneGraph.viewDistance = 10;
				frameWidth = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
				frameHeight = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();
			}
			rebuildFrameSize(screenMode, frameWidth, frameHeight);
			setBounds();
		}
		showChatComponents = screenMode == ScreenMode.FIXED ? true : showChatComponents;
		showTabComponents = screenMode == ScreenMode.FIXED ? true : showTabComponents;
	}

	private void addToXPCounter(int skill, int xp) {
		int font_height = 24;
		if (xp <= 0)
			return;

		xpCounter += xp;

		int lowest_y_off = Integer.MAX_VALUE;
		for (int i = 0; i < xp_added.length; i++)
			if (xp_added[i][0] > -1)
				lowest_y_off = Math.min(lowest_y_off, xp_added[i][2]);

		if (Configuration.xp_merge && lowest_y_off != Integer.MAX_VALUE && lowest_y_off <= 0) {
			for (int i = 0; i < xp_added.length; i++) {
				if (xp_added[i][2] != lowest_y_off)
					continue;

				xp_added[i][0] |= (1 << skill);
				xp_added[i][1] += xp;
				return;
			}
		} else {
			ArrayList<Integer> list = new ArrayList<Integer>();
			int y = font_height;

			boolean go_on = true;
			while (go_on) {
				go_on = false;

				for (int i = 0; i < xp_added.length; i++) {
					if (xp_added[i][0] == -1 || list.contains(new Integer(i)))
						continue;

					if (xp_added[i][2] < y) {
						xp_added[i][2] = y;
						y += font_height;
						go_on = true;
						list.add(new Integer(i));
					}
				}
			}

			if (lowest_y_off == Integer.MAX_VALUE || lowest_y_off >= font_height)
				lowest_y_off = 0;
			else
				lowest_y_off = 0;

			for (int i = 0; i < xp_added.length; i++)
				if (xp_added[i][0] == -1) {
					xp_added[i][0] = (1 << skill);
					xp_added[i][1] = xp;
					xp_added[i][2] = lowest_y_off;
					return;
				}
		}
	}

	public static void rebuildFrameSize(ScreenMode screenMode, int screenWidth,
			int screenHeight) {
		try {
			screenAreaWidth = (screenMode == ScreenMode.FIXED) ? 512 : screenWidth;
			screenAreaHeight = (screenMode == ScreenMode.FIXED) ? 334 : screenHeight;
			frameWidth = screenWidth;
			frameHeight = screenHeight;
			instance.refreshFrameSize(screenMode == ScreenMode.FULLSCREEN, screenWidth,
					screenHeight, screenMode == ScreenMode.RESIZABLE,
					screenMode != ScreenMode.FIXED);
			setBounds();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void refreshFrameSize() {
		if (frameMode == ScreenMode.RESIZABLE) {
			if (frameWidth != (appletClient() ? getGameComponent().getWidth()
					: gameFrame.getFrameWidth())) {
				frameWidth = (appletClient() ? getGameComponent().getWidth()
						: gameFrame.getFrameWidth());
				screenAreaWidth = frameWidth;
				setBounds();
			}
			if (frameHeight != (appletClient() ? getGameComponent().getHeight()
					: gameFrame.getFrameHeight())) {
				frameHeight = (appletClient() ? getGameComponent().getHeight()
						: gameFrame.getFrameHeight());
				screenAreaHeight = frameHeight;
				setBounds();
			}
		}
	}

	private static void setBounds() {
		Rasterizer3D.reposition(frameWidth, frameHeight);
		fullScreenTextureArray = Rasterizer3D.scanOffsets;
		Rasterizer3D.reposition(
				frameMode == ScreenMode.FIXED
				? (chatboxImageProducer != null
				? chatboxImageProducer.canvasWidth : 519)
						: frameWidth,
						frameMode == ScreenMode.FIXED
						? (chatboxImageProducer != null
						? chatboxImageProducer.canvasHeight : 165)
								: frameHeight);
		anIntArray1180 = Rasterizer3D.scanOffsets;
		Rasterizer3D.reposition(
				frameMode == ScreenMode.FIXED
				? (tabImageProducer != null ? tabImageProducer.canvasWidth
						: 249)
						: frameWidth,
						frameMode == ScreenMode.FIXED ? (tabImageProducer != null
						? tabImageProducer.canvasHeight : 335) : frameHeight);
		anIntArray1181 = Rasterizer3D.scanOffsets;
		Rasterizer3D.reposition(screenAreaWidth, screenAreaHeight);
		anIntArray1182 = Rasterizer3D.scanOffsets;
		int ai[] = new int[9];
		for (int i8 = 0; i8 < 9; i8++) {
			int k8 = 128 + i8 * 32 + 15;
			int l8 = 600 + k8 * 3;
			int i9 = Rasterizer3D.anIntArray1470[k8];
			ai[i8] = l8 * i9 >> 16;
		}
		if (frameMode == ScreenMode.RESIZABLE && (frameWidth >= 766) && (frameWidth <= 1025)
				&& (frameHeight >= 504) && (frameHeight <= 850)) {
			SceneGraph.viewDistance = 9;
			cameraZoom = 575;
		} else if (frameMode == ScreenMode.FIXED) {
			cameraZoom = 600;
		} else if (frameMode == ScreenMode.RESIZABLE || frameMode == ScreenMode.FULLSCREEN) {
			SceneGraph.viewDistance = 10;
			cameraZoom = 600;
		}
		SceneGraph.setupViewport(500, 800, screenAreaWidth, screenAreaHeight, ai);
		if (loggedIn) {
			gameScreenImageProducer =
					new ProducingGraphicsBuffer(screenAreaWidth, screenAreaHeight);
		}
	}

	public boolean getMousePositions() {
		if (mouseInRegion(frameWidth - (frameWidth <= 1000 ? 240 : 420),
				frameHeight - (frameWidth <= 1000 ? 90 : 37), frameWidth, frameHeight)) {
			return false;
		}
		if (showChatComponents) {
			if (changeChatArea) {
				if (super.mouseX > 0 && super.mouseX < 494
						&& super.mouseY > frameHeight - 175
						&& super.mouseY < frameHeight) {
					return true;
				} else {
					if (super.mouseX > 494 && super.mouseX < 515
							&& super.mouseY > frameHeight - 175
							&& super.mouseY < frameHeight) {
						return false;
					}
				}
			} else if (!changeChatArea) {
				if (super.mouseX > 0 && super.mouseX < 519
						&& super.mouseY > frameHeight - 175
						&& super.mouseY < frameHeight) {
					return false;
				}
			}
		}
		if (mouseInRegion(frameWidth - 216, 0, frameWidth, 172)) {
			return false;
		}
		if (!changeTabArea) {
			if (super.mouseX > 0 && super.mouseY > 0 && super.mouseY < frameWidth
					&& super.mouseY < frameHeight) {
				if (super.mouseX >= frameWidth - 242 && super.mouseY >= frameHeight - 335) {
					return false;
				}
				return true;
			}
			return false;
		}
		if (showTabComponents) {
			if (frameWidth > 1000) {
				if (super.mouseX >= frameWidth - 420 && super.mouseX <= frameWidth
						&& super.mouseY >= frameHeight - 37
						&& super.mouseY <= frameHeight
						|| super.mouseX > frameWidth - 225 && super.mouseX < frameWidth
						&& super.mouseY > frameHeight - 37 - 274
						&& super.mouseY < frameHeight) {
					return false;
				}
			} else {
				if (super.mouseX >= frameWidth - 210 && super.mouseX <= frameWidth
						&& super.mouseY >= frameHeight - 74
						&& super.mouseY <= frameHeight
						|| super.mouseX > frameWidth - 225 && super.mouseX < frameWidth
						&& super.mouseY > frameHeight - 74 - 274
						&& super.mouseY < frameHeight) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean mouseInRegion(int x1, int y1, int x2, int y2) {
		if (super.mouseX >= x1 && super.mouseX <= x2 && super.mouseY >= y1
				&& super.mouseY <= y2) {
			return true;
		}
		return false;
	}

	public boolean mouseMapPosition() {
		if (super.mouseX >= frameWidth - 21 && super.mouseX <= frameWidth && super.mouseY >= 0
				&& super.mouseY <= 21) {
			return false;
		}
		return true;
	}

	private void drawLoadingMessages(int used, String s, String s1) {
		int width = regularText.getTextWidth(used == 1 ? s : s1);
		int height = s1 == null ? 25 : 38;
		Rasterizer2D.drawBox(1, 1, width + 6, height, 0);
		Rasterizer2D.drawBox(1, 1, width + 6, 1, 0xffffff);
		Rasterizer2D.drawBox(1, 1, 1, height, 0xffffff);
		Rasterizer2D.drawBox(1, height, width + 6, 1, 0xffffff);
		Rasterizer2D.drawBox(width + 6, 1, 1, height, 0xffffff);
		regularText.drawText(0xffffff, s, 18, width / 2 + 5);
		if (s1 != null) {
			regularText.drawText(0xffffff, s1, 31, width / 2 + 5);
		}
	}


	private static final long serialVersionUID = 5707517957054703648L;

	private static String intToKOrMilLongName(int i) {
		String s = String.valueOf(i);
		for (int k = s.length() - 3; k > 0; k -= 3)
			s = s.substring(0, k) + "," + s.substring(k);
		if (s.length() > 8)
			s = "@gre@" + s.substring(0, s.length() - 8) + " million @whi@(" + s + ")";
		else if (s.length() > 4)
			s = "@cya@" + s.substring(0, s.length() - 4) + "K @whi@(" + s + ")";
		return " " + s;
	}

	public final String formatCoins(int coins) {
		if (coins >= 0 && coins < 10000)
			return String.valueOf(coins);
		if (coins >= 10000 && coins < 10000000)
			return coins / 1000 + "K";
		if (coins >= 10000000 && coins < 999999999)
			return coins / 1000000 + "M";
		if (coins >= 999999999)
			return "*";
		else
			return "?";
	}

	public static final byte[] ReadFile(String fileName) {
		try {
			byte abyte0[];
			File file = new File(fileName);
			int i = (int) file.length();
			abyte0 = new byte[i];
			DataInputStream datainputstream = new DataInputStream(
					new BufferedInputStream(new FileInputStream(fileName)));
			datainputstream.readFully(abyte0, 0, i);
			datainputstream.close();
			return abyte0;
		} catch (Exception e) {
			System.out.println((new StringBuilder()).append("Read Error: ").append(fileName)
					.toString());
			return null;
		}
	}

	private boolean menuHasAddFriend(int j) {
		if (j < 0)
			return false;
		int k = menuActionTypes[j];
		if (k >= 2000)
			k -= 2000;
		return k == 337;
	}

	private final int[] modeX = {164, 230, 296, 362},
			modeNamesX = {26, 86, 150, 212, 286, 349, 427},
			modeNamesY = {158, 158, 153, 153, 153, 153, 158},
			channelButtonsX = {5, 71, 137, 203, 269, 335, 404};

	private final String[] modeNames =
		{"All", "Game", "Public", "Private", "Clan", "Trade", "Report Abuse"};

	public void drawChannelButtons() {
		final int yOffset = frameMode == ScreenMode.FIXED ? 0 : frameHeight - 165;
		cacheSprite[49].drawSprite(0, 143 + yOffset);
		String text[] = { "On", "Friends", "Off", "Hide", "Autochat" };
		int textColor[] = { 65280, 0xffff00, 0xff0000, 65535, 0x0d9ddc };
		switch (cButtonCPos) {
		case 0:
		case 1:
		case 2:
		case 3:
		case 4:
		case 5:
		case 6:
			cacheSprite[16].drawSprite(channelButtonsX[cButtonCPos], 143 + yOffset);
			break;
		}
		if (cButtonHPos == cButtonCPos) {
			switch (cButtonHPos) {
			case 0:
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
			case 7:
				cacheSprite[17].drawSprite(channelButtonsX[cButtonHPos],
						143 + yOffset);
				break;
			}
		} else {
			switch (cButtonHPos) {
			case 0:
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
				cacheSprite[15].drawSprite(channelButtonsX[cButtonHPos],
						143 + yOffset);
				break;
			case 6:
				cacheSprite[18].drawSprite(channelButtonsX[cButtonHPos],
						143 + yOffset);
				break;
			}
		}
		int[] modes = {publicChatMode, privateChatMode, clanChatMode, tradeMode};
		for (int i = 0; i < modeNamesX.length; i++) {
			smallText.drawTextWithPotentialShadow(true, modeNamesX[i], 0xffffff, modeNames[i],
					modeNamesY[i] + yOffset);
		}
		for (int i = 0; i < modeX.length; i++) {
			smallText.method382(textColor[modes[i]], modeX[i], text[modes[i]], 164 + yOffset,
					true);
		}
	}

	private boolean chatStateCheck() {
		return messagePromptRaised || inputDialogState != 0 || clickToContinueString != null
				|| backDialogueId != -1 || dialogueId != -1;
	}

	private String enter_amount_title = "Enter amount:";
	private String enter_name_title = "Enter name:";

	private void drawChatArea() {
		int yOffset = frameMode == ScreenMode.FIXED ? 0 : frameHeight - 165;
		if (frameMode == ScreenMode.FIXED) {
			chatboxImageProducer.initDrawingArea();
		}
		Rasterizer3D.scanOffsets = anIntArray1180;
		if (chatStateCheck()) {
			showChatComponents = true;
			cacheSprite[20].drawSprite(0, yOffset);
		}
		if (showChatComponents) {
			if (changeChatArea && !chatStateCheck()) {
				Rasterizer2D.drawHorizontalLine(7, 7 + yOffset, 506, 0x575757);
				Rasterizer2D.drawTransparentGradientBox(7, 7 + yOffset, 506, 135, 0, 0xFFFFFF, 20);
			} else {
				cacheSprite[20].drawSprite(0, yOffset);
			}
		}
		if (!showChatComponents || changeChatArea) {
			Rasterizer2D.drawTransparentBox(7, frameHeight - 23, 506, 24, 0, 100);
		}
		drawChannelButtons();
		GameFont font = regularText;
		if (messagePromptRaised) {
			newBoldFont.drawCenteredString(aString1121, 259, 60 + yOffset, 0, -1);
			newBoldFont.drawCenteredString(promptInput + "*", 259, 80 + yOffset, 128, -1);
		} else if (inputDialogState == 1) {
			newBoldFont.drawCenteredString(enter_amount_title, 259, yOffset + 60, 0, -1);
			newBoldFont.drawCenteredString(amountOrNameInput + "*", 259, 80 + yOffset, 128,
					-1);
		} else if (inputDialogState == 2) {
			newBoldFont.drawCenteredString(enter_name_title, 259, 60 + yOffset, 0, -1);
			newBoldFont.drawCenteredString(amountOrNameInput + "*", 259, 80 + yOffset, 128,
					-1);
		} else if (clickToContinueString != null) {
			newBoldFont.drawCenteredString(clickToContinueString, 259, 60 + yOffset, 0, -1);
			newBoldFont.drawCenteredString("Click to continue", 259, 80 + yOffset, 128, -1);
		} else if (backDialogueId == 40500) {
			 try { 
				 drawInterface(0, 0, Widget.interfaceCache[backDialogueId], 0 + yOffset);
			 } catch (Exception ex) { 
			 }
		} else if (backDialogueId != -1 && backDialogueId != 40500) {
			try {
				drawInterface(0, 20, Widget.interfaceCache[backDialogueId], 20 + yOffset);
			} catch (Exception ex) {

			}
		} else if (dialogueId != -1) {
			try {
				drawInterface(0, 20, Widget.interfaceCache[dialogueId], 20 + yOffset);
			} catch (Exception ex) {

			}
		} else if (showChatComponents) {
			int j77 = -3;
			int j = 0;
			int shadow = changeChatArea ? 0 : -1;
			Rasterizer2D.setDrawingArea(122 + yOffset, 8, 497, 7 + yOffset);
			for (int k = 0; k < 500; k++) {
				if (chatMessages[k] != null) {
					int chatType = chatTypes[k];
					int yPos = (70 - j77 * 14) + anInt1089 + 5;
					String s1 = chatNames[k];
					byte data = 0;
					if (s1 != null && s1.startsWith("@cr1@")) {
						s1 = s1.substring(5);
						data = 1;
					} else if (s1 != null && s1.startsWith("@cr2@")) {
						s1 = s1.substring(5);
						data = 2;
					} else if (s1 != null && s1.startsWith("@cr3@")) {
						s1 = s1.substring(5);
						data = 3;
					} else if (s1 != null && s1.startsWith("@cr4@")) {
						s1 = s1.substring(5);
						data = 4;
					} else if (s1 != null && s1.startsWith("@cr5@")) {
						s1 = s1.substring(5);
						data = 5;
					} else if (s1 != null && s1.startsWith("@cr6@")) {
						s1 = s1.substring(5);
						data = 6;
					} else if (s1 != null && s1.startsWith("@cr7@")) {
						s1 = s1.substring(5);
						data = 7;
					} else if (s1 != null && s1.startsWith("@cr8@")) {
						s1 = s1.substring(5);
						data = 8;
					} else if (s1 != null && s1.startsWith("@cr9@")) {
						s1 = s1.substring(5);
						data = 9;
					} else if (s1 != null && s1.startsWith("@cr10@")) {
						s1 = s1.substring(6);
						data = 10;
					}
					if (chatType == 0) {
						if (chatTypeView == 5 || chatTypeView == 0) {
							newRegularFont.drawBasicString(chatMessages[k], 11,
									yPos + yOffset, changeChatArea ? 0xFFFFFF : 0,
											shadow);
							j++;
							j77++;
						}
					}
					if ((chatType == 1 || chatType == 2) && (chatType == 1
							|| publicChatMode == 0
							|| publicChatMode == 1 || publicChatMode == 4 && isFriendOrSelf(s1) || publicChatMode == 4)) {
						if (chatTypeView == 1 || chatTypeView == 0) {
							int xPos = 11;
							
							if(data > 0 && data < 10) {
								modIcons[data - 1].drawSprite(xPos + 1,
										yPos - 12 + yOffset);
								xPos += 14;
							}

							newRegularFont.drawBasicString(s1 + ":", xPos,
									yPos + yOffset, changeChatArea ? 0xFFFFFF : 0,
											shadow);
							xPos += font.getTextWidth(s1) + 8;
							newRegularFont.drawBasicString(chatMessages[k], xPos,
									yPos + yOffset,
									changeChatArea ? 0x7FA9FF : 255, shadow);
							j++;
							j77++;
						}
					}
					if ((chatType == 3 || chatType == 7)
							&& (splitPrivateChat == 0 || chatTypeView == 2)
							&& (chatType == 7 || privateChatMode == 0
							|| privateChatMode == 1
							&& isFriendOrSelf(s1))) {
						if (chatTypeView == 2 || chatTypeView == 0) {
							int k1 = 11;
							newRegularFont.drawBasicString("From", k1, yPos + yOffset,
									changeChatArea ? 0 : 0x000000, shadow);
							k1 += font.getTextWidth("From ");
							
							if(data > 0 && data < 10) {
								modIcons[data - 1].drawSprite(k1, yPos - 12 + yOffset);
								k1 += 12;
							}
							
							newRegularFont.drawBasicString(s1 + ":", k1,
									yPos + yOffset, changeChatArea ? 0xFFFFFF : 0,
											shadow);
							k1 += font.getTextWidth(s1) + 8;
							newRegularFont.drawBasicString(chatMessages[k], k1,
									yPos + yOffset, 0x800000, shadow);
							j++;
							j77++;
						}
					}
					if (chatType == 4 && (tradeMode == 0
							|| tradeMode == 1 && isFriendOrSelf(s1))) {
						if (chatTypeView == 3 || chatTypeView == 0) {
							newRegularFont.drawBasicString(s1 + " " + chatMessages[k],
									11, yPos + yOffset, 0x800080, shadow);
							j++;
							j77++;
						}
					}
					if (chatType == 5 && splitPrivateChat == 0 && privateChatMode < 2) {
						if (chatTypeView == 2 || chatTypeView == 0) {
							newRegularFont.drawBasicString(s1 + " " + chatMessages[k],
									11, yPos + yOffset, 0x800000, shadow);
							j++;
							j77++;
						}
					}
					if (chatType == 6 && (splitPrivateChat == 0 || chatTypeView == 2)
							&& privateChatMode < 2) {
						if (chatTypeView == 2 || chatTypeView == 0) {
							newRegularFont.drawBasicString("To " + s1 + ":", 11,
									yPos + yOffset, changeChatArea ? 0x000000 : 0,
											shadow);
							newRegularFont.drawBasicString(chatMessages[k],
									15 + font.getTextWidth("To :" + s1),
									yPos + yOffset, 0x800000, shadow);
							j++;
							j77++;
						}
					}
					if (chatType == 8 && (tradeMode == 0
							|| tradeMode == 1 && isFriendOrSelf(s1))) {
						if (chatTypeView == 3 || chatTypeView == 0) {
							newRegularFont.drawBasicString(s1 + " " + chatMessages[k],
									11, yPos + yOffset, 0x7e3200, shadow);
							j++;
							j77++;
						}
						if (chatType == 11 && (clanChatMode == 0)) {
							if (chatTypeView == 11) {
								newRegularFont.drawBasicString(
										s1 + " " + chatMessages[k], 11,
										yPos + yOffset, 0x7e3200, shadow);
								j++;
								j77++;
							}
							if (chatType == 12) {
								newRegularFont.drawBasicString(chatMessages[k] + "",
										11, yPos + yOffset, 0x7e3200, shadow);
								j++;
							}
						}
					}
					if (chatType == 16) {
						if (chatTypeView == 11 || chatTypeView == 0) {

							newRegularFont.drawBasicString(chatMessages[k], 10,
									yPos + yOffset, 0x800080, shadow);

							j++;
							j77++;
						}
					}
				}
			}
			Rasterizer2D.defaultDrawingAreaSize();
			anInt1211 = j * 14 + 7 + 5;
			if (anInt1211 < 111) {
				anInt1211 = 111;
			}
			drawScrollbar(114, anInt1211 - anInt1089 - 113, 7 + yOffset, 496, anInt1211,
					changeChatArea);
			String s;
			if (localPlayer != null && localPlayer.name != null) {
				s = localPlayer.name;
			} else {
				s = StringUtils.formatText(capitalize(myUsername));
			}
			Rasterizer2D.setDrawingArea(140 + yOffset, 8, 509, 120 + yOffset);
			int xOffset = 0;
			if (myPrivilege > 0 && myPrivilege < 10) {
				modIcons[myPrivilege - 1].drawSprite(10, 122 + yOffset);
				xOffset += 14;
			}
			newRegularFont.drawBasicString(s + ":", xOffset + 11, 133 + yOffset,
					changeChatArea ? 0xFFFFFF : 0, shadow);
			newRegularFont.drawBasicString(inputString + "*",
					xOffset + 12 + font.getTextWidth(s + ": "), 133 + yOffset,
					changeChatArea ? 0x7FA9FF : 255, shadow);
			Rasterizer2D.drawHorizontalLine(7, 121 + yOffset, 506, changeChatArea ? 0x575757 : 0x807660);
			Rasterizer2D.defaultDrawingAreaSize();
		}
		if (menuOpen) {
			drawMenu(0, frameMode == ScreenMode.FIXED ? 338 : 0);
		}
		if (frameMode == ScreenMode.FIXED) {
			chatboxImageProducer.drawGraphics(338, super.graphics, 0);
		}
		gameScreenImageProducer.initDrawingArea();
		Rasterizer3D.scanOffsets = anIntArray1182;
	}

	public static String capitalize(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (i == 0) {
				s = String.format("%s%s", Character.toUpperCase(s.charAt(0)),
						s.substring(1));
			}
			if (!Character.isLetterOrDigit(s.charAt(i))) {
				if (i + 1 < s.length()) {
					s = String.format("%s%s%s", s.subSequence(0, i + 1),
							Character.toUpperCase(s.charAt(i + 1)),
							s.substring(i + 2));
				}
			}
		}
		return s;
	}

	/**
	 * Initializes the client for startup
	 */
	public void initialize() {
		try {
			nodeID = 10;
			portOffset = 0;
			setHighMem();
			isMembers = true;
			SignLink.storeid = 32;
			SignLink.startpriv(InetAddress.getLocalHost());
			initClientFrame(frameWidth, frameHeight);
			instance = this;
		} catch (Exception exception) {
			return;
		}
	}

	public void startRunnable(Runnable runnable, int priority) {
		if (priority > 10)
			priority = 10;
		if (SignLink.mainapp != null) {
			SignLink.startthread(runnable, priority);
		} else {
			super.startRunnable(runnable, priority);
		}
	}

	public Socket openSocket(int port) throws IOException {
		return new Socket(InetAddress.getByName(server), port);
	}

	private void processMenuClick() {
		if (activeInterfaceType != 0)
			return;
		int j = super.clickMode3;
		if (spellSelected == 1 && super.saveClickX >= 516 && super.saveClickY >= 160
				&& super.saveClickX <= 765 && super.saveClickY <= 205)
			j = 0;
		if (menuOpen) {
			if (j != 1) {
				int k = super.mouseX;
				int j1 = super.mouseY;
				if (menuScreenArea == 0) {
					k -= 4;
					j1 -= 4;
				}
				if (menuScreenArea == 1) {
					k -= 519;
					j1 -= 168;
				}
				if (menuScreenArea == 2) {
					k -= 17;
					j1 -= 338;
				}
				if (menuScreenArea == 3) {
					k -= 519;
					j1 -= 0;
				}
				if (k < menuOffsetX - 10 || k > menuOffsetX + menuWidth + 10
						|| j1 < menuOffsetY - 10
						|| j1 > menuOffsetY + menuHeight + 10) {
					menuOpen = false;
					if (menuScreenArea == 1) {
					}
					if (menuScreenArea == 2)
						updateChatbox = true;
				}
			}
			if (j == 1) {
				int l = menuOffsetX;
				int k1 = menuOffsetY;
				int i2 = menuWidth;
				int k2 = super.saveClickX;
				int l2 = super.saveClickY;
				switch (menuScreenArea) {
				case 0:
					k2 -= 4;
					l2 -= 4;
					break;
				case 1:
					k2 -= 519;
					l2 -= 168;
					break;
				case 2:
					k2 -= 5;
					l2 -= 338;
					break;
				case 3:
					k2 -= 519;
					l2 -= 0;
					break;
				}
				int i3 = -1;
				for (int j3 = 0; j3 < menuActionRow; j3++) {
					int k3 = k1 + 31 + (menuActionRow - 1 - j3) * 15;
					if (k2 > l && k2 < l + i2 && l2 > k3 - 13 && l2 < k3 + 3)
						i3 = j3;
				}
				if (i3 != -1)
					processMenuActions(i3);
				menuOpen = false;
				if (menuScreenArea == 1) {
				}
				if (menuScreenArea == 2) {
					updateChatbox = true;
				}
			}
		} else {
			if (j == 1 && menuActionRow > 0) {
				int i1 = menuActionTypes[menuActionRow - 1];
				if (i1 == 632 || i1 == 78 || i1 == 867 || i1 == 431 || i1 == 53 || i1 == 74
						|| i1 == 454 || i1 == 539 || i1 == 493 || i1 == 847 || i1 == 447
						|| i1 == 1125) {
					int l1 = firstMenuAction[menuActionRow - 1];
					int j2 = secondMenuAction[menuActionRow - 1];
					Widget class9 = Widget.interfaceCache[j2];
					if (class9.allowSwapItems || class9.replaceItems) {
						aBoolean1242 = false;
						anInt989 = 0;
						anInt1084 = j2;
						anInt1085 = l1;
						activeInterfaceType = 2;
						anInt1087 = super.saveClickX;
						anInt1088 = super.saveClickY;
						if (Widget.interfaceCache[j2].parent == openInterfaceId)
							activeInterfaceType = 1;
						if (Widget.interfaceCache[j2].parent == backDialogueId)
							activeInterfaceType = 3;
						return;
					}
				}
			}
			if (j == 1 && (anInt1253 == 1 || menuHasAddFriend(menuActionRow - 1))
					&& menuActionRow > 2)
				j = 2;
			if (j == 1 && menuActionRow > 0)
				processMenuActions(menuActionRow - 1);
			if (j == 2 && menuActionRow > 0)
				determineMenuSize();
			processMainScreenClick();
			processTabClick();
			processChatModeClick();
			minimapHovers();
		}
	}

	private void saveMidi(boolean flag, byte abyte0[]) {
		SignLink.fadeMidi = flag ? 1 : 0;
		SignLink.saveMidi(abyte0, abyte0.length);
	}

	private void updateWorldObjects() {
		try {
			lastKnownPlane = -1;
			incompleteAnimables.clear();
			projectiles.clear();
			Rasterizer3D.clearTextureCache();
			unlinkCaches();
			scene.initToNull();
			System.gc();
			for (int i = 0; i < 4; i++)
				collisionMaps[i].initialize();
			for (int l = 0; l < 4; l++) {
				for (int k1 = 0; k1 < 104; k1++) {
					for (int j2 = 0; j2 < 104; j2++)
						tileFlags[l][k1][j2] = 0;
				}
			}

			MapRegion objectManager = new MapRegion(tileFlags, tileHeights);
			int k2 = localRegionMapData.length;
			sendPacket(new BasicPing());
			if (!constructedViewport) {
				for (int i3 = 0; i3 < k2; i3++) {
					int i4 = (localRegionIds[i3] >> 8) * 64 - regionBaseX;
					int k5 = (localRegionIds[i3] & 0xff) * 64 - regionBaseY;
					byte abyte0[] = localRegionMapData[i3];
					if (abyte0 != null)
						objectManager.method180(abyte0, k5, i4, (this.regionX - 6) * 8,
								(this.regionY - 6) * 8, collisionMaps);
				}
				for (int j4 = 0; j4 < k2; j4++) {
					int l5 = (localRegionIds[j4] >> 8) * 64 - regionBaseX;
					int k7 = (localRegionIds[j4] & 0xff) * 64 - regionBaseY;
					byte abyte2[] = localRegionMapData[j4];
					if (abyte2 == null && this.regionY < 800)
						objectManager.initiateVertexHeights(k7, 64, 64, l5);
				}
				/*  anInt1097++;
                        if (anInt1097 > 160) {
                              anInt1097 = 0;
                              //anticheat?
                              outgoing.writeOpcode(238);
                              outgoing.writeByte(96);
                        }*/
				sendPacket(new BasicPing());
				for (int i6 = 0; i6 < k2; i6++) {
					byte abyte1[] = localRegionLandscapeData[i6];
					if (abyte1 != null) {
						int l8 = (localRegionIds[i6] >> 8) * 64 - regionBaseX;
						int k9 = (localRegionIds[i6] & 0xff) * 64 - regionBaseY;
						objectManager.method190(l8, collisionMaps, k9, scene, abyte1);
					}
				}
			}
			if (constructedViewport) {
				for (int j3 = 0; j3 < 4; j3++) {
					for (int k4 = 0; k4 < 13; k4++) {
						for (int j6 = 0; j6 < 13; j6++) {
							int l7 = localRegions[j3][k4][j6];
							if (l7 != -1) {
								int i9 = l7 >> 24 & 3;
						int l9 = l7 >> 1 & 3;
				int j10 = l7 >> 14 & 0x3ff;
			int l10 = l7 >> 3 & 0x7ff;
		int j11 = (j10 / 8 << 8) + l10 / 8;
		for (int l11 =
				0; l11 < localRegionIds.length; l11++) {
			if (localRegionIds[l11] != j11
					|| localRegionMapData[l11] == null)
				continue;
			objectManager.method179(i9, l9, collisionMaps,
					k4 * 8, (j10 & 7) * 8,
					localRegionMapData[l11],
					(l10 & 7) * 8, j3, j6 * 8);
			break;
		}

							}
						}
					}
				}
				for (int l4 = 0; l4 < 13; l4++) {
					for (int k6 = 0; k6 < 13; k6++) {
						int i8 = localRegions[0][l4][k6];
						if (i8 == -1)
							objectManager.initiateVertexHeights(k6 * 8, 8, 8, l4 * 8);
					}
				}

				sendPacket(new BasicPing());
				for (int l6 = 0; l6 < 4; l6++) {
					for (int j8 = 0; j8 < 13; j8++) {
						for (int j9 = 0; j9 < 13; j9++) {
							int i10 = localRegions[l6][j8][j9];
							if (i10 != -1) {
								int k10 = i10 >> 24 & 3;
						int i11 = i10 >> 1 & 3;
				int k11 = i10 >> 14 & 0x3ff;
				int i12 = i10 >> 3 & 0x7ff;
		int j12 = (k11 / 8 << 8) + i12 / 8;
		for (int k12 =
				0; k12 < localRegionIds.length; k12++) {
			if (localRegionIds[k12] != j12
					|| localRegionLandscapeData[k12] == null) {
				continue;
			}
			objectManager.method183(collisionMaps, scene,
					k10, j8 * 8, (i12 & 7) * 8, l6,
					localRegionLandscapeData[k12],
					(k11 & 7) * 8, i11, j9 * 8);
			break;
		}

							}
						}

					}

				}
			}
			sendPacket(new BasicPing());
			objectManager.createRegionScene(collisionMaps, scene);
			gameScreenImageProducer.initDrawingArea();
			sendPacket(new BasicPing());
			int k3 = MapRegion.maximumPlane;
			if (k3 > plane)
				k3 = plane;
			if (k3 < plane - 1)
				k3 = plane - 1;
			if (lowMemory)
				scene.method275(MapRegion.maximumPlane);
			else
				scene.method275(0);
			for (int i5 = 0; i5 < 104; i5++) {
				for (int i7 = 0; i7 < 104; i7++)
					updateGroundItems(i5, i7);

			}

			anInt1051++;
			if (anInt1051 > 98) {
				anInt1051 = 0;
				//anticheat?
				//outgoing.writeOpcode(150);
			}

			method63();
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		ObjectDefinition.baseModels.clear();
		if (super.gameFrame != null) {
			sendPacket(new RegionChange());
		}
		if (lowMemory && SignLink.cache_dat != null) {
			int j = resourceProvider.getVersionCount(0);
			for (int i1 = 0; i1 < j; i1++) {
				int l1 = resourceProvider.getModelIndex(i1);
				if ((l1 & 0x79) == 0)
					Model.method461(i1);
			}

		}
		System.gc();
		Rasterizer3D.initiateRequestBuffers();
		resourceProvider.clearExtras();
		int k = (this.regionX - 6) / 8 - 1;
		int j1 = (this.regionX + 6) / 8 + 1;
		int i2 = (this.regionY - 6) / 8 - 1;
		int l2 = (this.regionY + 6) / 8 + 1;
		if (inPlayerOwnedHouse) {
			k = 49;
			j1 = 50;
			i2 = 49;
			l2 = 50;
		}
		for (int l3 = k; l3 <= j1; l3++) {
			for (int j5 = i2; j5 <= l2; j5++)
				if (l3 == k || l3 == j1 || j5 == i2 || j5 == l2) {
					int j7 = resourceProvider.resolve(0, j5, l3);
					if (j7 != -1)
						resourceProvider.loadExtra(j7, 3);
					int k8 = resourceProvider.resolve(1, j5, l3);
					if (k8 != -1)
						resourceProvider.loadExtra(k8, 3);
				}

		}

	}

	public static AbstractMap.SimpleEntry<Integer, Integer> getNextInteger(
			ArrayList<Integer> values) {
		ArrayList<AbstractMap.SimpleEntry<Integer, Integer>> frequencies = new ArrayList<>();
		int maxIndex = 0;
		main: for (int i = 0; i < values.size(); ++i) {
			int value = values.get(i);
			for (int j = 0; j < frequencies.size(); ++j) {
				if (frequencies.get(j).getKey() == value) {
					frequencies.get(j).setValue(frequencies.get(j).getValue() + 1);
					if (frequencies.get(maxIndex).getValue() < frequencies.get(j)
							.getValue()) {
						maxIndex = j;
					}
					continue main;
				}
			}
			frequencies.add(new AbstractMap.SimpleEntry<Integer, Integer>(value, 1));
		}
		return frequencies.get(maxIndex);
	}

	private void unlinkCaches() {
		ObjectDefinition.baseModels.clear();
		ObjectDefinition.models.clear();
		NpcDefinition.modelCache.clear();
		ItemDefinition.models.clear();
		ItemDefinition.sprites.clear();
		Player.models.clear();
		Graphic.models.clear();
	}

	private void renderMapScene(int plane) {
		int pixels[] = minimapImage.myPixels;            
		int length = pixels.length;

		for (int pixel = 0; pixel < length; pixel++) {            	
			pixels[pixel] = 0;
		}


		for (int y = 1; y < 103; y++) {            	
			int i1 = 24628 + (103 - y) * 512 * 4;
			for (int x = 1; x < 103; x++) {                	  
				if ((tileFlags[plane][x][y] & 0x18) == 0)
					scene.drawTileOnMinimapSprite(pixels, i1, plane, x, y);
				if (plane < 3 && (tileFlags[plane + 1][x][y] & 8) != 0)
					scene.drawTileOnMinimapSprite(pixels, i1, plane + 1, x, y);
				i1 += 4;
			}

		}

		int j1 = 0xFFFFFF;
		int l1 = 0xEE0000;
		minimapImage.init();

		for (int y = 1; y < 103; y++) {            	
			for (int x = 1; x < 103; x++) {                	  
				if ((tileFlags[plane][x][y] & 0x18) == 0)
					drawMapScenes(y, j1, x, l1, plane);
				if (plane < 3 && (tileFlags[plane + 1][x][y] & 8) != 0)
					drawMapScenes(y, j1, x, l1, plane + 1);
			}

		}

		gameScreenImageProducer.initDrawingArea();
		anInt1071 = 0;

		for (int x = 0; x < 104; x++) {            	
			for (int y = 0; y < 104; y++) {                	  
				int id = scene.getGroundDecorationUid(plane, x, y);
				if (id != 0) {
					id = id >> 14 & 0x7fff;

			int function = ObjectDefinition.lookup(id).minimapFunction;

			if (function >= 0) {
				int viewportX = x;                                    
				int viewportY = y;                                    
				minimapHint[anInt1071] = mapFunctions[function];
				minimapHintX[anInt1071] = viewportX;
				minimapHintY[anInt1071] = viewportY;
				anInt1071++;
			}
				}
			}

		}

		if (Configuration.dumpMapRegions) {

			File directory = new File("MapImageDumps/");
			if (!directory.exists()) {
				directory.mkdir();
			}
			BufferedImage bufferedimage = new BufferedImage(minimapImage.myWidth, minimapImage.myHeight, 1);
			bufferedimage.setRGB(0, 0, minimapImage.myWidth, minimapImage.myHeight, minimapImage.myPixels, 0, minimapImage.myWidth);
			Graphics2D graphics2d = bufferedimage.createGraphics();
			graphics2d.dispose();
			try {
				File file1 = new File("MapImageDumps/"+(directory.listFiles().length+1)+".png");
				ImageIO.write(bufferedimage, "png", file1);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}

	private void updateGroundItems(int i, int j) {
		Deque class19 = groundItems[plane][i][j];
		if (class19 == null) {
			scene.removeGroundItemTile(plane, i, j);
			return;
		}
		int k = 0xfa0a1f01;
		Object obj = null;
		for (Item item = (Item) class19.reverseGetFirst(); item != null; item =
				(Item) class19.reverseGetNext()) {
			ItemDefinition itemDef = ItemDefinition.lookup(item.ID);
			int l = itemDef.value;
			if (itemDef.stackable)
				l *= item.itemCount + 1;
			// notifyItemSpawn(item, i + baseX, j + baseY);

			if (l > k) {
				k = l;
				obj = item;
			}
		}

		class19.insertTail(((Linkable) (obj)));
		Object obj1 = null;
		Object obj2 = null;
		for (Item class30_sub2_sub4_sub2_1 = (Item) class19
				.reverseGetFirst(); class30_sub2_sub4_sub2_1 != null; class30_sub2_sub4_sub2_1 =
				(Item) class19.reverseGetNext()) {
			if (class30_sub2_sub4_sub2_1.ID != ((Item) (obj)).ID && obj1 == null)
				obj1 = class30_sub2_sub4_sub2_1;
			if (class30_sub2_sub4_sub2_1.ID != ((Item) (obj)).ID
					&& class30_sub2_sub4_sub2_1.ID != ((Item) (obj1)).ID && obj2 == null)
				obj2 = class30_sub2_sub4_sub2_1;
		}

		int i1 = i + (j << 7) + 0x60000000;
		scene.addGroundItemTile(i, i1, ((Renderable) (obj1)),
				getCenterHeight(plane, j * 128 + 64, i * 128 + 64), ((Renderable) (obj2)),
				((Renderable) (obj)), plane, j);
	}

	private boolean prioritizedNpc(Npc npc) {

		//Check if it's being interacted with
		if(localPlayer.interactingEntity != -1 && 
				localPlayer.interactingEntity < 32768) {
			if(npc.index == localPlayer.interactingEntity) {
				return true;
			}
		}

		return npc.desc.priorityRender;
	}

	private void showPrioritizedNPCs() {
		for (int index = 0; index < npcCount; index++) {
			Npc npc = npcs[npcIndices[index]];

			if(prioritizedNpc(npc)) {
				showNpc(npc, index, npc.desc.priorityRender);
			}
		}
	}

	private void showOtherNpcs() {
		for (int index = 0; index < npcCount; index++) {
			Npc npc = npcs[npcIndices[index]];
			showNpc(npc, index, false);
		}
	}


	private boolean showNpc(Npc npc, int index, boolean priorityRender) {
		int k = 0x20000000 + (npcIndices[index] << 14);
		if (npc == null || !npc.isVisible() || npc.desc.priorityRender != priorityRender)
			return false;
		int l = npc.x >> 7;
		int i1 = npc.y >> 7;
		if (l < 0 || l >= 104 || i1 < 0 || i1 >= 104)
			return false;
		if (npc.size == 1 && (npc.x & 0x7f) == 64 && (npc.y & 0x7f) == 64) {
			if (anIntArrayArray929[l][i1] == anInt1265)
				return false;
			anIntArrayArray929[l][i1] = anInt1265;
		}
		if (!npc.desc.clickable)
			k += 0x80000000;
		scene.addAnimableA(plane, npc.orientation, getCenterHeight(plane, npc.y, npc.x), k, npc.y,
				(npc.size - 1) * 64 + 60, npc.x, npc, npc.animationStretches);
		return true;
	}

	public void drawHoverBox(int xPos, int yPos, String text) {
		String[] results = text.split("\n");
		int height = (results.length * 16);
		int width;
		width = newRegularFont.getTextWidth(results[0]) + 6;
		for (int i = 1; i < results.length; i++)
			if (width <= newRegularFont.getTextWidth(results[i]) + 6)
				width = newRegularFont.getTextWidth(results[i]) + 6;
		Rasterizer2D.drawBox(xPos, yPos, width, height, 0xFFFFA0);
		Rasterizer2D.drawBoxOutline(xPos, yPos, width, height, 0);
		yPos += 14;
		for (int i = 0; i < results.length; i++) {
			newRegularFont.drawBasicString(results[i], xPos + 2, yPos, 0x000000,
					-1);
			yPos += 12;
		}
	}
	
	public void drawSkillHoverBox(int xPos, int yPos, String skillName, int skillId, int width, boolean right) {
		int height = (3 * 16) - 3;
		if(maximumLevels[skillId] <= 98) {
		height = (3 * 16) - 3;
		} else if (maximumLevels[skillId] > 98) {
		height = (1 * 16) + 3;
		}
		int finalWidth;
		int finalWidth2;
		int finalWidth3;
		
		int extraWidth4 = newRegularFont.getTextWidth(StringUtils.insertCommasToNumber(Integer.toString(currentExp[skillId])));
		int extraWidth5 =  newRegularFont.getTextWidth(StringUtils.insertCommasToNumber(Integer.toString(getXPForLevel((maximumLevels[skillId]) + 1))));
		int extraWidth2 = newRegularFont.getTextWidth(skillName + " XP:");
		int extraWidth3 = newRegularFont.getTextWidth("Remaining XP:");
		if(extraWidth4 > extraWidth5) {
			finalWidth2 = extraWidth4;
		} else {
			finalWidth2 = extraWidth5;
		}
		if(extraWidth2 > extraWidth3) {
			finalWidth = extraWidth2;
		} else {
			finalWidth = extraWidth3;
		}
		if(maximumLevels[skillId] > 98) {
			finalWidth = newRegularFont.getTextWidth(StringUtils.insertCommasToNumber(Integer.toString(currentExp[skillId])));
		}
		finalWidth3 = extraWidth2 + extraWidth4 +  4;
		if(!right) {
		if(maximumLevels[skillId] <= 98) {
		Rasterizer2D.drawBox(xPos, yPos, finalWidth + finalWidth2 + 6, height, 0xFFFFA0);
		Rasterizer2D.drawBoxOutline(xPos, yPos, finalWidth + finalWidth2 + 6, height, 0);
		} else {
			Rasterizer2D.drawBox(xPos, yPos, extraWidth2 + extraWidth4 + 6, height, 0xFFFFA0);
			Rasterizer2D.drawBoxOutline(xPos, yPos, extraWidth2 + extraWidth4 + 6, height, 0);
		}
		} if(right) {
				if(maximumLevels[skillId] <= 98) {
			xPos = 210 - (finalWidth + finalWidth2);
				} else {
					xPos = 210 - (extraWidth2 + extraWidth4);
				}
			if(maximumLevels[skillId] <= 98) {
				Rasterizer2D.drawBox(xPos, yPos, finalWidth + finalWidth2 + 6, height, 0xFFFFA0);
				Rasterizer2D.drawBoxOutline(xPos, yPos, finalWidth + finalWidth2 + 6, height, 0);
				} else {
					Rasterizer2D.drawBox(xPos, yPos, extraWidth2 + extraWidth4 + 6, height, 0xFFFFA0);
					Rasterizer2D.drawBoxOutline(xPos, yPos, extraWidth2 + extraWidth4 + 6, height, 0);
				}
		}
		yPos += 14;
			newRegularFont.drawBasicString(skillName + " XP:", xPos + 2, yPos, 0x000000,
					-1);
			if(maximumLevels[skillId] <= 98) {
			newRegularFont.drawRightString(StringUtils.insertCommasToNumber(Integer.toString(currentExp[skillId])), xPos + finalWidth + finalWidth2 + 4, yPos, 0x000000,
					-1, newRegularFont);
			} else {
				newRegularFont.drawRightString(StringUtils.insertCommasToNumber(Integer.toString(currentExp[skillId])), xPos + 4 + extraWidth2 + extraWidth4, yPos, 0x000000,
						-1, newRegularFont);
			}
			if(maximumLevels[skillId] <= 98) {
			yPos += 12;
			newRegularFont.drawBasicString("Next level at:", xPos + 2, yPos, 0x000000,
					-1);
				newRegularFont.drawRightString(StringUtils.insertCommasToNumber(Integer.toString(getXPForLevel((maximumLevels[skillId]) + 1))),xPos + finalWidth + finalWidth2 + 4, yPos, 0x000000,
						-1, newRegularFont);
			
			yPos += 12;
			newRegularFont.drawBasicString("Remaining XP:", xPos + 2, yPos, 0x000000,
					-1);
				newRegularFont.drawRightString(StringUtils.insertCommasToNumber(Integer.toString(getXPForLevel((maximumLevels[skillId]) + 1) - (currentExp[skillId]))),xPos + finalWidth + finalWidth2 + 4, yPos, 0x000000,
						-1, newRegularFont);
			}
			
	}
	private void buildInterfaceMenu(int i, Widget widget, int mouseX, int l, int mouseY, int j1) {
		if (widget == null || widget.type != 0 || widget.children == null || widget.invisible || widget.drawingDisabled)
			return;
		if (mouseX < i || mouseY < l || mouseX > i + widget.width || mouseY > l + widget.height)
			return;
		int size = widget.children.length;
		for (int l1 = 0; l1 < size; l1++) {
			int i2 = widget.childX[l1] + i;
			int j2 = (widget.childY[l1] + l) - j1;
			Widget childInterface = Widget.interfaceCache[widget.children[l1]];
			if(childInterface == null) {
				continue;
			}
			i2 += childInterface.horizontalOffset;
			j2 += childInterface.verticalOffset;
			if ((mouseX < childInterface.width || mouseY < childInterface.height || mouseX > + childInterface.width || mouseY > + childInterface.height) && childInterface.isHovered) {
				childInterface.enabledSprite = cacheSprite[507];
				childInterface.disabledSprite = cacheSprite[507];
			}
			if (mouseX >= i2 && mouseY >= j2 && mouseX < i2 + childInterface.width
					&& mouseY < j2 + childInterface.height && childInterface.isHovered) {
					childInterface.enabledSprite = cacheSprite[537];
					childInterface.disabledSprite = cacheSprite[537];
					childInterface.drawsTransparent = true;
					childInterface.transparency = 50;
				}
			if ((childInterface.hoverType >= 0 || childInterface.defaultHoverColor != 0)
					&& mouseX >= i2 && mouseY >= j2 && mouseX < i2 + childInterface.width
					&& mouseY < j2 + childInterface.height)
				
				//if (childInterface.hoverType >= 0 && childInterface.contentlol == 1) {
				//	childInterface.enabledSprite = cacheSprite[537];
				//	childInterface.disabledSprite = cacheSprite[537];
				//	childInterface.drawsTransparent = true;
				//	childInterface.transparency = 50;
				//}
				if (childInterface.hoverType >= 0 && !childInterface.isHovered)
					anInt886 = childInterface.hoverType;
				else
					anInt886 = childInterface.id;
			if (childInterface.type == 8 && mouseX >= i2 && mouseY >= j2
					&& mouseX < i2 + childInterface.width && mouseY < j2 + childInterface.height) {
				anInt1315 = childInterface.id;
			}
			
			if (childInterface.type == Widget.TYPE_CONTAINER) {
				buildInterfaceMenu(i2, childInterface, mouseX, j2, mouseY,
						childInterface.scrollPosition);
				if (childInterface.scrollMax > childInterface.height)
					method65(i2 + childInterface.width, childInterface.height, mouseX, mouseY,
							childInterface, j2, true, childInterface.scrollMax);
			  } else if (childInterface.atActionType == 69) {
				  childInterface.hovered = false;
                  if (childInterface.dropDown.isOpen()) {
                	  childInterface.dropDown.hover(childInterface, childInterface, mouseX, mouseY, i2, j2);
                
                	  
                  }
                  if (mouseX >= i2 && mouseY >= j2 && mouseX < i2 + childInterface.dropDown.getWidth() && mouseY < j2 + 24) {
                	  childInterface.hovered = true;
                      menuActionText[menuActionRow] = childInterface.dropDown.isOpen() ? "Hide other options" : "Show other options";
                      menuActionTypes[menuActionRow] = 769;
                      secondMenuAction[menuActionRow] = childInterface.id;
                      firstMenuAction[menuActionRow] = childInterface.id;
                      menuActionRow++;
                  }
			} else {
				if (childInterface.atActionType == Widget.OPTION_OK && mouseX >= i2 && mouseY >= j2
						&& mouseX < i2 + childInterface.width
						&& mouseY < j2 + childInterface.height) {
					boolean flag = false;
					if (childInterface.contentType != 0)
						flag = buildFriendsListMenu(childInterface);
					if(childInterface.tooltip == null ||
							childInterface.tooltip.length() == 0) {
						flag = true;
					}
					if (!flag) {
						if ((myPrivilege >= 2 && myPrivilege <= 4)) {
							menuActionText[menuActionRow] = childInterface.tooltip + " "+childInterface.id;
							menuActionTypes[menuActionRow] = 315;
							secondMenuAction[menuActionRow] = childInterface.id;
							if(childInterface.geSearchButton) {
							itemIdToSend = childInterface.itemId;
							}
							menuActionRow++;
						} else {
							menuActionText[menuActionRow] = childInterface.tooltip;
							menuActionTypes[menuActionRow] = 315;
							secondMenuAction[menuActionRow] = childInterface.id;
							if(childInterface.geSearchButton) {
							itemIdToSend = childInterface.itemId;
							}
							menuActionRow++;
						}
					}
				}
				if (childInterface.atActionType == Widget.OPTION_USABLE && spellSelected == 0
						&& mouseX >= i2 && mouseY >= j2 && mouseX < i2 + childInterface.width
						&& mouseY < j2 + childInterface.height) {
					String s = childInterface.selectedActionName;
					if (s.indexOf(" ") != -1)
						s = s.substring(0, s.indexOf(" "));
					if (childInterface.spellName.endsWith("Rush")
							|| childInterface.spellName.endsWith("Burst")
							|| childInterface.spellName.endsWith("Blitz")
							|| childInterface.spellName.endsWith("Barrage")
							|| childInterface.spellName.endsWith("strike")
							|| childInterface.spellName.endsWith("bolt")
							|| childInterface.spellName.equals("Crumble undead")
							|| childInterface.spellName.endsWith("blast")
							|| childInterface.spellName.endsWith("wave")
							|| childInterface.spellName.equals("Claws of Guthix")
							|| childInterface.spellName.equals("Flames of Zamorak")
							|| childInterface.spellName.equals("Magic Dart")) {
						menuActionText[menuActionRow] =
								"Autocast @gre@" + childInterface.spellName;

						menuActionTypes[menuActionRow] = 104;
						secondMenuAction[menuActionRow] = childInterface.id;
						menuActionRow++;
					}	if ((myPrivilege >= 2 && myPrivilege <= 4)) {
						menuActionText[menuActionRow] =
								s + " @gre@" + childInterface.spellName + " "+childInterface.id;
						menuActionTypes[menuActionRow] = 626;
						secondMenuAction[menuActionRow] = childInterface.id;
						menuActionRow++;
					} else {
						menuActionText[menuActionRow] =
								s + " @gre@" + childInterface.spellName;
						menuActionTypes[menuActionRow] = 626;
						secondMenuAction[menuActionRow] = childInterface.id;
						menuActionRow++;
					}
				}
				if (childInterface.atActionType == Widget.OPTION_CLOSE && mouseX >= i2 && mouseY >= j2
						&& mouseX < i2 + childInterface.width
						&& mouseY < j2 + childInterface.height) {
					menuActionText[menuActionRow] = "Close";
					menuActionTypes[menuActionRow] = 200;
					secondMenuAction[menuActionRow] = childInterface.id;
					menuActionRow++;
				}
				if (childInterface.atActionType == Widget.OPTION_TOGGLE_SETTING && mouseX >= i2
						&& mouseY >= j2 && mouseX < i2 + childInterface.width
						&& mouseY < j2 + childInterface.height) {
					if ((myPrivilege >= 2 && myPrivilege <= 4)) {
						menuActionText[menuActionRow] = childInterface.tooltip + " @lre@("+childInterface.id+")";
					} else {
						menuActionText[menuActionRow] = childInterface.tooltip;
					}
					menuActionTypes[menuActionRow] = 169;
					secondMenuAction[menuActionRow] = childInterface.id;
					menuActionRow++;
				}

				if (childInterface.atActionType == Widget.OPTION_RESET_SETTING && mouseX >= i2
						&& mouseY >= j2 && mouseX < i2 + childInterface.width
						&& mouseY < j2 + childInterface.height) {
					boolean flag = false;
					if(childInterface.tooltip == null ||
							childInterface.tooltip.length() == 0) {
						flag = true;
					}
					if (!flag) {
						if ((myPrivilege >= 2 && myPrivilege <= 4)) {
							menuActionText[menuActionRow] = childInterface.tooltip + " @lre@("+childInterface.id+")";
						} else {
							menuActionText[menuActionRow] = childInterface.tooltip;
						}
						menuActionTypes[menuActionRow] = 646;
						secondMenuAction[menuActionRow] = childInterface.id;
						menuActionRow++;
					}
				}

				if (childInterface.atActionType == Widget.OPTION_CONTINUE
						&& !continuedDialogue && mouseX >= i2 && mouseY >= j2
						&& mouseX < i2 + childInterface.width
						&& mouseY < j2 + childInterface.height) {
					menuActionText[menuActionRow] = childInterface.tooltip;
					menuActionTypes[menuActionRow] = 679;
					secondMenuAction[menuActionRow] = childInterface.id;
					menuActionRow++;
				}

				if (mouseX >= i2 && mouseY >= j2
						&& mouseX < i2 + (childInterface.type == 4 ? 100
								: childInterface.width)
						&& mouseY < j2 + childInterface.height) {
					if (childInterface.actions != null && !childInterface.invisible && !childInterface.drawingDisabled) {

						if(!(childInterface.contentType == 206 && interfaceIsSelected(childInterface))) {
							if ((childInterface.type == 4 && childInterface.defaultText.length() > 0)  || childInterface.type == 5) {

								boolean drawOptions = true;

								//HARDCODE CLICKABLE TEXT HERE
								
								if(childInterface.parent == 37128) { //Clan chat interface, dont show options for guests
									drawOptions = showClanOptions;
								}

								if(drawOptions) {
									for (int action = childInterface.actions.length
											- 1; action >= 0; action--) {
										if (childInterface.actions[action] != null) {
											String s = childInterface.actions[action] + (childInterface.type == 4 ? " @or1@" + childInterface.defaultText : "");

											if(s.contains("img")) {
												int prefix = s.indexOf("<img=");
												int suffix = s.indexOf(">");
												s = s.replaceAll(s.substring(prefix + 5, suffix), "");
												s = s.replaceAll("</img>", "");
												s = s.replaceAll("<img=>", "");	
											}
											menuActionText[menuActionRow] = s;
											menuActionTypes[menuActionRow] = 647;
											firstMenuAction[menuActionRow] = action;
											secondMenuAction[menuActionRow] = childInterface.id;
											menuActionRow++;
										}
									}
								}
							}
						}
					}
				}

				if (childInterface.type == Widget.TYPE_INVENTORY && !childInterface.invisible && !childInterface.drawingDisabled && !(childInterface.id >= 22035 && childInterface.id <= 22042)) {
					int k2 = 0;
					for (int l2 = 0; l2 < childInterface.height; l2++) {
						for (int i3 = 0; i3 < childInterface.width; i3++) {
							int j3 = i2 + i3 * (32 + childInterface.spritePaddingX);
							int k3 = j2 + l2 * (32 + childInterface.spritePaddingY);
							if (k2 < 20) {
								j3 += childInterface.spritesX[k2];
								k3 += childInterface.spritesY[k2];
							}
							if (mouseX >= j3 && mouseY >= k3 && mouseX < j3 + 32 && mouseY < k3 + 32) {
								mouseInvInterfaceIndex = k2;
								lastActiveInvInterface = childInterface.id;
								if(k2 >= childInterface.inventoryItemId.length) {
									continue;
								}
								if (childInterface.inventoryItemId[k2] > 0) {
									ItemDefinition itemDef = ItemDefinition
											.lookup(childInterface.inventoryItemId[k2]
													- 1);
									if (itemSelected == 1
											&& childInterface.hasActions) {
										if (childInterface.id != anInt1284
												|| k2 != anInt1283) {
											menuActionText[menuActionRow] =
													"Use " + selectedItemName
													+ " with @lre@"
													+ itemDef.name;
											menuActionTypes[menuActionRow] =
													870;
											selectedMenuActions[menuActionRow] =
													itemDef.id;
											firstMenuAction[menuActionRow] =
													k2;
											secondMenuAction[menuActionRow] =
													childInterface.id;
											menuActionRow++;
										}
									} else if (spellSelected == 1
											&& childInterface.hasActions) {
										if ((spellUsableOn & 0x10) == 16) {
											menuActionText[menuActionRow] =
													spellTooltip + " @lre@"
															+ itemDef.name;
											menuActionTypes[menuActionRow] =
													543;
											selectedMenuActions[menuActionRow] =
													itemDef.id;
											firstMenuAction[menuActionRow] =
													k2;
											secondMenuAction[menuActionRow] =
													childInterface.id;
											menuActionRow++;
										}
									} else {
										if (childInterface.hasActions) {
											for (int l3 = 4; l3 >= 3; l3--)
												if (itemDef.actions != null
												&& itemDef.actions[l3] != null) {
													menuActionText[menuActionRow] =
															itemDef.actions[l3]
																	+ " @lre@"
																	+ itemDef.name;
													if (l3 == 3)
														menuActionTypes[menuActionRow] =
														493;
													if (l3 == 4)
														menuActionTypes[menuActionRow] =
														847;
													selectedMenuActions[menuActionRow] =
															itemDef.id;
													firstMenuAction[menuActionRow] =
															k2;
													secondMenuAction[menuActionRow] =
															childInterface.id;
													menuActionRow++;
												} else if (l3 == 4 ) {
													menuActionText[menuActionRow] =
															"Drop @lre@" + itemDef.name;
													menuActionTypes[menuActionRow] =
															847;
													selectedMenuActions[menuActionRow] =
															itemDef.id;
													firstMenuAction[menuActionRow] =
															k2;
													secondMenuAction[menuActionRow] =
															childInterface.id;
													menuActionRow++;
												}
										}
										if (childInterface.usableItems) {
											menuActionText[menuActionRow] =
													"Use @lre@" + itemDef.name;
											menuActionTypes[menuActionRow] =
													447;
											selectedMenuActions[menuActionRow] =
													itemDef.id;
											firstMenuAction[menuActionRow] =
													k2;
											secondMenuAction[menuActionRow] =
													childInterface.id;
											menuActionRow++;
										}
										if (childInterface.hasActions
												&& itemDef.actions != null) {
											for (int i4 = 2; i4 >= 0; i4--)
												if (itemDef.actions[i4] != null) {
													menuActionText[menuActionRow] =
															itemDef.actions[i4]
																	+ " @lre@"
																	+ itemDef.name;
													if (i4 == 0)
														menuActionTypes[menuActionRow] =
														74;
													if (i4 == 1)
														menuActionTypes[menuActionRow] =
														454;
													if (i4 == 2)
														menuActionTypes[menuActionRow] =
														539;
													selectedMenuActions[menuActionRow] =
															itemDef.id;
													firstMenuAction[menuActionRow] =
															k2;
													secondMenuAction[menuActionRow] =
															childInterface.id;
													menuActionRow++;
												}

										}

										//Menu actions, item options etc in interfaces n
										//Hardcoding
										if (childInterface.actions != null) {
											
											for (int type =
													5; type >= 0; type--) {
												if (childInterface.actions[type] != null && !childInterface.actions[type].equalsIgnoreCase("operate") 
														|| itemDef.equipActions[type] != null && childInterface.id == 1688 ) {
													
													if(childInterface.id == 1688) {
														if (itemDef.equipActions[type] != null) {
														    menuActionText[menuActionRow] = itemDef.equipActions[menuActionRow] + " @lre@" + itemDef.name;
														} else {
														    if (childInterface.actions[type] != null) {
															menuActionText[menuActionRow] = childInterface.actions[type] + " @lre@" + itemDef.name;
														    }
														}
														if (type == 0)
															menuActionTypes[menuActionRow] = 632; // remove
														if (type == 1)
															menuActionTypes[menuActionRow] = 661; // operate 1
													        if (type == 2)
													        	menuActionTypes[menuActionRow] = 662; // operate 2
														if (type == 3)
															menuActionTypes[menuActionRow] = 663; //operate 3
													        if (type == 4)
													        	menuActionTypes[menuActionRow] = 664; //operate 4
													        if (type == 5)
													        	menuActionTypes[menuActionRow] = 665; //operate 4
													
														
													} else {
													menuActionText[menuActionRow] = childInterface.actions[type] + " @lre@" + itemDef.name;

													if (type == 0)
														menuActionTypes[menuActionRow] =
														632;
													if (type == 1)
														menuActionTypes[menuActionRow] =
														78;
													if (type == 2)
														menuActionTypes[menuActionRow] =
														867;
													if (type == 3)
														menuActionTypes[menuActionRow] =
														431;
													if (type == 4)
														menuActionTypes[menuActionRow] =
														53;
													}
													selectedMenuActions[menuActionRow] =
															itemDef.id;
													firstMenuAction[menuActionRow] =
															k2;
													secondMenuAction[menuActionRow] =
															childInterface.id;
													menuActionRow++;
												}
													
											}

										}
										if ((myPrivilege >= 2 && myPrivilege <= 4)) {
											menuActionText[menuActionRow] =
													"Examine @lre@"
															+ itemDef.name
															+ " @gre@(@whi@"
															+ (childInterface.inventoryItemId[k2]
																	- 1)
															+ "@gre@) int: "+childInterface.id;
										} else {
											menuActionText[menuActionRow] =
													"Examine @lre@"
															+ itemDef.name;
										}
										menuActionTypes[menuActionRow] = 1125;
										selectedMenuActions[menuActionRow] =
												itemDef.id;
										firstMenuAction[menuActionRow] = k2;
										secondMenuAction[menuActionRow] =
												childInterface.id;
										menuActionRow++;
									}
								}
							}
							k2++;
						}
					}
				}
			}
		}
	}

	public void drawTransparentScrollBar(int x, int y, int height, int maxScroll, int pos) {
		cacheSprite[29].drawAdvancedSprite(x, y, 120);
		cacheSprite[30].drawAdvancedSprite(x, y + height - 16, 120);
		Rasterizer2D.drawTransparentVerticalLine(x, y + 16, height - 32, 0xffffff, 64);
		Rasterizer2D.drawTransparentVerticalLine(x + 15, y + 16, height - 32, 0xffffff, 64);
		int barHeight = (height - 32) * height / maxScroll;
		if (barHeight < 10) {
			barHeight = 10;
		}
		int barPos = 0;
		if (maxScroll != height) {
			barPos = (height - 32 - barHeight) * pos / (maxScroll - height);
		}
		Rasterizer2D.drawTransparentBoxOutline(x, y + 16 + barPos, 16,
				5 + y + 16 + barPos + barHeight - 5 - (y + 16 + barPos), 0xffffff, 32);
	}

	public void drawScrollbar(int height, int pos, int y, int x, int maxScroll,
			boolean transparent) {
		if (transparent) {
			drawTransparentScrollBar(x, y, height, maxScroll, pos);
		} else {
			scrollBar1.drawSprite(x, y);
			scrollBar2.drawSprite(x, (y + height) - 16);
			Rasterizer2D.drawBox(x, y + 16, 16, height - 32, 0x000001);
			Rasterizer2D.drawBox(x, y + 16, 15, height - 32, 0x3d3426);
			Rasterizer2D.drawBox(x, y + 16, 13, height - 32, 0x342d21);
			Rasterizer2D.drawBox(x, y + 16, 11, height - 32, 0x2e281d);
			Rasterizer2D.drawBox(x, y + 16, 10, height - 32, 0x29241b);
			Rasterizer2D.drawBox(x, y + 16, 9, height - 32, 0x252019);
			Rasterizer2D.drawBox(x, y + 16, 1, height - 32, 0x000001);
			int k1 = ((height - 32) * height) / maxScroll;
			if (k1 < 8) {
				k1 = 8;
			}
			int l1 = ((height - 32 - k1) * pos) / (maxScroll - height);
			Rasterizer2D.drawBox(x, y + 16 + l1, 16, k1, barFillColor);
			Rasterizer2D.drawVerticalLine(x, y + 16 + l1, k1, 0x000001);
			Rasterizer2D.drawVerticalLine(x + 1, y + 16 + l1, k1, 0x817051);
			Rasterizer2D.drawVerticalLine(x + 2, y + 16 + l1, k1, 0x73654a);
			Rasterizer2D.drawVerticalLine(x + 3, y + 16 + l1, k1, 0x6a5c43);
			Rasterizer2D.drawVerticalLine(x + 4, y + 16 + l1, k1, 0x6a5c43);
			Rasterizer2D.drawVerticalLine(x + 5, y + 16 + l1, k1, 0x655841);
			Rasterizer2D.drawVerticalLine(x + 6, y + 16 + l1, k1, 0x655841);
			Rasterizer2D.drawVerticalLine(x + 7, y + 16 + l1, k1, 0x61553e);
			Rasterizer2D.drawVerticalLine(x + 8, y + 16 + l1, k1, 0x61553e);
			Rasterizer2D.drawVerticalLine(x + 9, y + 16 + l1, k1, 0x5d513c);
			Rasterizer2D.drawVerticalLine(x + 10, y + 16 + l1, k1, 0x5d513c);
			Rasterizer2D.drawVerticalLine(x + 11, y + 16 + l1, k1, 0x594e3a);
			Rasterizer2D.drawVerticalLine(x + 12, y + 16 + l1, k1, 0x594e3a);
			Rasterizer2D.drawVerticalLine(x + 13, y + 16 + l1, k1, 0x514635);
			Rasterizer2D.drawVerticalLine(x + 14, y + 16 + l1, k1, 0x4b4131);
			Rasterizer2D.drawHorizontalLine(x, y + 16 + l1, 15, 0x000001);
			Rasterizer2D.drawHorizontalLine(x, y + 17 + l1, 15, 0x000001);
			Rasterizer2D.drawHorizontalLine(x, y + 17 + l1, 14, 0x655841);
			Rasterizer2D.drawHorizontalLine(x, y + 17 + l1, 13, 0x6a5c43);
			Rasterizer2D.drawHorizontalLine(x, y + 17 + l1, 11, 0x6d5f48);
			Rasterizer2D.drawHorizontalLine(x, y + 17 + l1, 10, 0x73654a);
			Rasterizer2D.drawHorizontalLine(x, y + 17 + l1, 7, 0x76684b);
			Rasterizer2D.drawHorizontalLine(x, y + 17 + l1, 5, 0x7b6a4d);
			Rasterizer2D.drawHorizontalLine(x, y + 17 + l1, 4, 0x7e6e50);
			Rasterizer2D.drawHorizontalLine(x, y + 17 + l1, 3, 0x817051);
			Rasterizer2D.drawHorizontalLine(x, y + 17 + l1, 2, 0x000001);
			Rasterizer2D.drawHorizontalLine(x, y + 18 + l1, 16, 0x000001);
			Rasterizer2D.drawHorizontalLine(x, y + 18 + l1, 15, 0x564b38);
			Rasterizer2D.drawHorizontalLine(x, y + 18 + l1, 14, 0x5d513c);
			Rasterizer2D.drawHorizontalLine(x, y + 18 + l1, 11, 0x625640);
			Rasterizer2D.drawHorizontalLine(x, y + 18 + l1, 10, 0x655841);
			Rasterizer2D.drawHorizontalLine(x, y + 18 + l1, 7, 0x6a5c43);
			Rasterizer2D.drawHorizontalLine(x, y + 18 + l1, 5, 0x6e6046);
			Rasterizer2D.drawHorizontalLine(x, y + 18 + l1, 4, 0x716247);
			Rasterizer2D.drawHorizontalLine(x, y + 18 + l1, 3, 0x7b6a4d);
			Rasterizer2D.drawHorizontalLine(x, y + 18 + l1, 2, 0x817051);
			Rasterizer2D.drawHorizontalLine(x, y + 18 + l1, 1, 0x000001);
			Rasterizer2D.drawHorizontalLine(x, y + 19 + l1, 16, 0x000001);
			Rasterizer2D.drawHorizontalLine(x, y + 19 + l1, 15, 0x514635);
			Rasterizer2D.drawHorizontalLine(x, y + 19 + l1, 14, 0x564b38);
			Rasterizer2D.drawHorizontalLine(x, y + 19 + l1, 11, 0x5d513c);
			Rasterizer2D.drawHorizontalLine(x, y + 19 + l1, 9, 0x61553e);
			Rasterizer2D.drawHorizontalLine(x, y + 19 + l1, 7, 0x655841);
			Rasterizer2D.drawHorizontalLine(x, y + 19 + l1, 5, 0x6a5c43);
			Rasterizer2D.drawHorizontalLine(x, y + 19 + l1, 4, 0x6e6046);
			Rasterizer2D.drawHorizontalLine(x, y + 19 + l1, 3, 0x73654a);
			Rasterizer2D.drawHorizontalLine(x, y + 19 + l1, 2, 0x817051);
			Rasterizer2D.drawHorizontalLine(x, y + 19 + l1, 1, 0x000001);
			Rasterizer2D.drawHorizontalLine(x, y + 20 + l1, 16, 0x000001);
			Rasterizer2D.drawHorizontalLine(x, y + 20 + l1, 15, 0x4b4131);
			Rasterizer2D.drawHorizontalLine(x, y + 20 + l1, 14, 0x544936);
			Rasterizer2D.drawHorizontalLine(x, y + 20 + l1, 13, 0x594e3a);
			Rasterizer2D.drawHorizontalLine(x, y + 20 + l1, 10, 0x5d513c);
			Rasterizer2D.drawHorizontalLine(x, y + 20 + l1, 8, 0x61553e);
			Rasterizer2D.drawHorizontalLine(x, y + 20 + l1, 6, 0x655841);
			Rasterizer2D.drawHorizontalLine(x, y + 20 + l1, 4, 0x6a5c43);
			Rasterizer2D.drawHorizontalLine(x, y + 20 + l1, 3, 0x73654a);
			Rasterizer2D.drawHorizontalLine(x, y + 20 + l1, 2, 0x817051);
			Rasterizer2D.drawHorizontalLine(x, y + 20 + l1, 1, 0x000001);
			Rasterizer2D.drawVerticalLine(x + 15, y + 16 + l1, k1, 0x000001);
			Rasterizer2D.drawHorizontalLine(x, y + 15 + l1 + k1, 16, 0x000001);
			Rasterizer2D.drawHorizontalLine(x, y + 14 + l1 + k1, 15, 0x000001);
			Rasterizer2D.drawHorizontalLine(x, y + 14 + l1 + k1, 14, 0x3f372a);
			Rasterizer2D.drawHorizontalLine(x, y + 14 + l1 + k1, 10, 0x443c2d);
			Rasterizer2D.drawHorizontalLine(x, y + 14 + l1 + k1, 9, 0x483e2f);
			Rasterizer2D.drawHorizontalLine(x, y + 14 + l1 + k1, 7, 0x4a402f);
			Rasterizer2D.drawHorizontalLine(x, y + 14 + l1 + k1, 4, 0x4b4131);
			Rasterizer2D.drawHorizontalLine(x, y + 14 + l1 + k1, 3, 0x564b38);
			Rasterizer2D.drawHorizontalLine(x, y + 14 + l1 + k1, 2, 0x000001);
			Rasterizer2D.drawHorizontalLine(x, y + 13 + l1 + k1, 16, 0x000001);
			Rasterizer2D.drawHorizontalLine(x, y + 13 + l1 + k1, 15, 0x443c2d);
			Rasterizer2D.drawHorizontalLine(x, y + 13 + l1 + k1, 11, 0x4b4131);
			Rasterizer2D.drawHorizontalLine(x, y + 13 + l1 + k1, 9, 0x514635);
			Rasterizer2D.drawHorizontalLine(x, y + 13 + l1 + k1, 7, 0x544936);
			Rasterizer2D.drawHorizontalLine(x, y + 13 + l1 + k1, 6, 0x564b38);
			Rasterizer2D.drawHorizontalLine(x, y + 13 + l1 + k1, 4, 0x594e3a);
			Rasterizer2D.drawHorizontalLine(x, y + 13 + l1 + k1, 3, 0x625640);
			Rasterizer2D.drawHorizontalLine(x, y + 13 + l1 + k1, 2, 0x6a5c43);
			Rasterizer2D.drawHorizontalLine(x, y + 13 + l1 + k1, 1, 0x000001);
			Rasterizer2D.drawHorizontalLine(x, y + 12 + l1 + k1, 16, 0x000001);
			Rasterizer2D.drawHorizontalLine(x, y + 12 + l1 + k1, 15, 0x443c2d);
			Rasterizer2D.drawHorizontalLine(x, y + 12 + l1 + k1, 14, 0x4b4131);
			Rasterizer2D.drawHorizontalLine(x, y + 12 + l1 + k1, 12, 0x544936);
			Rasterizer2D.drawHorizontalLine(x, y + 12 + l1 + k1, 11, 0x564b38);
			Rasterizer2D.drawHorizontalLine(x, y + 12 + l1 + k1, 10, 0x594e3a);
			Rasterizer2D.drawHorizontalLine(x, y + 12 + l1 + k1, 7, 0x5d513c);
			Rasterizer2D.drawHorizontalLine(x, y + 12 + l1 + k1, 4, 0x61553e);
			Rasterizer2D.drawHorizontalLine(x, y + 12 + l1 + k1, 3, 0x6e6046);
			Rasterizer2D.drawHorizontalLine(x, y + 12 + l1 + k1, 2, 0x7b6a4d);
			Rasterizer2D.drawHorizontalLine(x, y + 12 + l1 + k1, 1, 0x000001);
			Rasterizer2D.drawHorizontalLine(x, y + 11 + l1 + k1, 16, 0x000001);
			Rasterizer2D.drawHorizontalLine(x, y + 11 + l1 + k1, 15, 0x4b4131);
			Rasterizer2D.drawHorizontalLine(x, y + 11 + l1 + k1, 14, 0x514635);
			Rasterizer2D.drawHorizontalLine(x, y + 11 + l1 + k1, 13, 0x564b38);
			Rasterizer2D.drawHorizontalLine(x, y + 11 + l1 + k1, 11, 0x594e3a);
			Rasterizer2D.drawHorizontalLine(x, y + 11 + l1 + k1, 9, 0x5d513c);
			Rasterizer2D.drawHorizontalLine(x, y + 11 + l1 + k1, 7, 0x61553e);
			Rasterizer2D.drawHorizontalLine(x, y + 11 + l1 + k1, 5, 0x655841);
			Rasterizer2D.drawHorizontalLine(x, y + 11 + l1 + k1, 4, 0x6a5c43);
			Rasterizer2D.drawHorizontalLine(x, y + 11 + l1 + k1, 3, 0x73654a);
			Rasterizer2D.drawHorizontalLine(x, y + 11 + l1 + k1, 2, 0x7b6a4d);
			Rasterizer2D.drawHorizontalLine(x, y + 11 + l1 + k1, 1, 0x000001);
		}
	}

	private void updateNPCs(Buffer stream, int i) {
		removedMobCount = 0;
		mobsAwaitingUpdateCount = 0;
		method139(stream);
		updateNPCMovement(i, stream);
		npcUpdateMask(stream);
		for (int k = 0; k < removedMobCount; k++) {
			int l = removedMobs[k];
			if (npcs[l].time != tick) {
				npcs[l].desc = null;
				npcs[l] = null;
			}
		}

		if (stream.currentPosition != i) {
			SignLink.reporterror(myUsername + " size mismatch in getnpcpos - pos:"
					+ stream.currentPosition + " psize:" + i);
			throw new RuntimeException("eek");
		}
		for (int i1 = 0; i1 < npcCount; i1++)
			if (npcs[npcIndices[i1]] == null) {
				SignLink.reporterror(myUsername + " null entry in npc list - pos:" + i1
						+ " size:" + npcCount);
				throw new RuntimeException("eek");
			}

	}

	private int cButtonHPos;
	private int cButtonCPos;
	private int setChannel;

	public void processChatModeClick() {

		final int yOffset = frameMode == ScreenMode.FIXED ? 0 : frameHeight - 503;
		if (super.mouseX >= 5 && super.mouseX <= 61 && super.mouseY >= yOffset + 482
				&& super.mouseY <= yOffset + 503) {
			cButtonHPos = 0;
			updateChatbox = true;
		} else if (super.mouseX >= 71 && super.mouseX <= 127 && super.mouseY >= yOffset + 482
				&& super.mouseY <= yOffset + 503) {
			cButtonHPos = 1;
			updateChatbox = true;
		} else if (super.mouseX >= 137 && super.mouseX <= 193 && super.mouseY >= yOffset + 482
				&& super.mouseY <= yOffset + 503) {
			cButtonHPos = 2;
			updateChatbox = true;
		} else if (super.mouseX >= 203 && super.mouseX <= 259 && super.mouseY >= yOffset + 482
				&& super.mouseY <= yOffset + 503) {
			cButtonHPos = 3;
			updateChatbox = true;
		} else if (super.mouseX >= 269 && super.mouseX <= 325 && super.mouseY >= yOffset + 482
				&& super.mouseY <= yOffset + 503) {
			cButtonHPos = 4;
			updateChatbox = true;
		} else if (super.mouseX >= 335 && super.mouseX <= 391 && super.mouseY >= yOffset + 482
				&& super.mouseY <= yOffset + 503) {
			cButtonHPos = 5;
			updateChatbox = true;
		} else if (super.mouseX >= 404 && super.mouseX <= 515 && super.mouseY >= yOffset + 482
				&& super.mouseY <= yOffset + 503) {
			cButtonHPos = 6;
			updateChatbox = true;
		} else {
			cButtonHPos = -1;
			updateChatbox = true;
		}
		if (super.clickMode3 == 1) {
			if (super.saveClickX >= 5 && super.saveClickX <= 61
					&& super.saveClickY >= yOffset + 482
					&& super.saveClickY <= yOffset + 505) {
				if (frameMode != ScreenMode.FIXED) {
					if (setChannel != 0) {
						cButtonCPos = 0;
						chatTypeView = 0;
						updateChatbox = true;
						setChannel = 0;
					} else {
						showChatComponents = showChatComponents ? false : true;
					}
				} else {
					cButtonCPos = 0;
					chatTypeView = 0;
					updateChatbox = true;
					setChannel = 0;
				}
			} else if (super.saveClickX >= 71 && super.saveClickX <= 127
					&& super.saveClickY >= yOffset + 482
					&& super.saveClickY <= yOffset + 505) {
				if (frameMode != ScreenMode.FIXED) {
					if (setChannel != 1 && frameMode != ScreenMode.FIXED) {
						cButtonCPos = 1;
						chatTypeView = 5;
						updateChatbox = true;
						setChannel = 1;
					} else {
						showChatComponents = showChatComponents ? false : true;
					}
				} else {
					cButtonCPos = 1;
					chatTypeView = 5;
					updateChatbox = true;
					setChannel = 1;
				}
			} else if (super.saveClickX >= 137 && super.saveClickX <= 193
					&& super.saveClickY >= yOffset + 482
					&& super.saveClickY <= yOffset + 505) {
				if (frameMode != ScreenMode.FIXED) {
					if (setChannel != 2 && frameMode != ScreenMode.FIXED) {
						cButtonCPos = 2;
						chatTypeView = 1;
						updateChatbox = true;
						setChannel = 2;
					} else {
						showChatComponents = showChatComponents ? false : true;
					}
				} else {
					cButtonCPos = 2;
					chatTypeView = 1;
					updateChatbox = true;
					setChannel = 2;
				}
			} else if (super.saveClickX >= 203 && super.saveClickX <= 259
					&& super.saveClickY >= yOffset + 482
					&& super.saveClickY <= yOffset + 505) {
				if (frameMode != ScreenMode.FIXED) {
					if (setChannel != 3 && frameMode != ScreenMode.FIXED) {
						cButtonCPos = 3;
						chatTypeView = 2;
						updateChatbox = true;
						setChannel = 3;
					} else {
						showChatComponents = showChatComponents ? false : true;
					}
				} else {
					cButtonCPos = 3;
					chatTypeView = 2;
					updateChatbox = true;
					setChannel = 3;
				}
			} else if (super.saveClickX >= 269 && super.saveClickX <= 325
					&& super.saveClickY >= yOffset + 482
					&& super.saveClickY <= yOffset + 505) {
				if (frameMode != ScreenMode.FIXED) {
					if (setChannel != 4 && frameMode != ScreenMode.FIXED) {
						cButtonCPos = 4;
						chatTypeView = 11;
						updateChatbox = true;
						setChannel = 4;
					} else {
						showChatComponents = showChatComponents ? false : true;
					}
				} else {
					cButtonCPos = 4;
					chatTypeView = 11;
					updateChatbox = true;
					setChannel = 4;
				}
			} else if (super.saveClickX >= 335 && super.saveClickX <= 391
					&& super.saveClickY >= yOffset + 482
					&& super.saveClickY <= yOffset + 505) {
				if (frameMode != ScreenMode.FIXED) {
					if (setChannel != 5 && frameMode != ScreenMode.FIXED) {
						cButtonCPos = 5;
						chatTypeView = 3;
						updateChatbox = true;
						setChannel = 5;
					} else {
						showChatComponents = showChatComponents ? false : true;
					}
				} else {
					cButtonCPos = 5;
					chatTypeView = 3;
					updateChatbox = true;
					setChannel = 5;
				}
			} else if (super.saveClickX >= 404 && super.saveClickX <= 515
					&& super.saveClickY >= yOffset + 482
					&& super.saveClickY <= yOffset + 505) {
				if (openInterfaceId == -1) {
					clearTopInterfaces();
					reportAbuseInput = "";
					canMute = false;
					for (int i = 0; i < Widget.interfaceCache.length; i++) {
						if (Widget.interfaceCache[i] == null
								|| Widget.interfaceCache[i].contentType != 600) {
							continue;
						}
						reportAbuseInterfaceID =
								openInterfaceId = Widget.interfaceCache[i].parent;
						break;
					}
				} else {
					sendMessage("Please close the interface you have open before using 'report abuse'",
							0, "");
				}
			}
		}
	}

	public void updateVarp(int id) {


		int parameter = VariablePlayer.variables[id].getActionId();

		if (parameter == 0) {
			return;
		}

		int state = settings[id];

		if (parameter == 1) {

			if (state == 1) {
				Rasterizer3D.setBrightness(0.9);
				savePlayerData();
			}

			if (state == 2) {
				Rasterizer3D.setBrightness(0.8);
				savePlayerData();
			}

			if (state == 3) {
				Rasterizer3D.setBrightness(0.7);
				savePlayerData();
			}

			if (state == 4) {
				Rasterizer3D.setBrightness(0.6);
				savePlayerData();
			}

			ItemDefinition.sprites.clear();
			welcomeScreenRaised = true;
		}

		if (parameter == 3) {

			boolean previousPlayingMusic = Configuration.enableMusic;

			if (state == 0) {

				if (SignLink.music != null) {
					adjustVolume(Configuration.enableMusic, 500);
				}

				Configuration.enableMusic = true;
			}
			if (state == 1) {

				if (SignLink.music != null) {
					adjustVolume(Configuration.enableMusic, 300);
				}

				Configuration.enableMusic = true;
			}
			if (state == 2) {

				if (SignLink.music != null) {
					adjustVolume(Configuration.enableMusic, 100);
				}

				Configuration.enableMusic = true;
			}
			if (state == 3) {

				if (SignLink.music != null) {
					adjustVolume(Configuration.enableMusic, 0);
				}

				Configuration.enableMusic = true;
			}
			if (state == 4)
				Configuration.enableMusic = false;
			if (Configuration.enableMusic != previousPlayingMusic && !lowMemory) {
				if (Configuration.enableMusic) {
					nextSong = currentSong;
					fadeMusic = true;
					resourceProvider.provide(2, nextSong);
				} else {
					stopMidi();
				}
				prevSong = 0;
			}
		}

		if (parameter == 4) {
			SoundPlayer.setVolume(state);
			if (state == 0) {
				aBoolean848 = true;
				setWaveVolume(0);
			}
			if (state == 1) {
				aBoolean848 = true;
				setWaveVolume(-400);
			}
			if (state == 2) {
				aBoolean848 = true;
				setWaveVolume(-800);
			}
			if (state == 3) {
				aBoolean848 = true;
				setWaveVolume(-1200);
			}
			if (state == 4)
				aBoolean848 = false;
		}

		if (parameter == 5) {
			anInt1253 = state;
		}

		if (parameter == 6) {
			anInt1249 = state;
		}

		if (parameter == 8) {
			splitPrivateChat = state;
			updateChatbox = true;
		}

		if (parameter == 9) {
			anInt913 = state;
		}

	}

	public FileArchive mediaStreamLoader;

	private final int[] hitmarks562 = {31, 32, 33, 34};

	public void updateEntities() {
		try {
			int messageLength = 0;

			for (int j = -1; j < playerCount + npcCount; j++) {
				Object obj;
				if (j == -1)
					obj = localPlayer;
				else if (j < playerCount)
					obj = players[playerList[j]];
				else
					obj = npcs[npcIndices[j - playerCount]];
				if (obj == null || !((Mob) (obj)).isVisible())
					continue;
				if (obj instanceof Npc) {
					NpcDefinition entityDef = ((Npc) obj).desc;
					if (Configuration.namesAboveHeads) {
						npcScreenPos(((Mob) (obj)), ((Mob) (obj)).height + 15);
						smallText.drawText(0x0099FF, entityDef.name, spriteDrawY - 5,
								spriteDrawX); // -15
						// from
						// original
					}
					if (entityDef.childrenIDs != null)
						entityDef = entityDef.morph();
					if (entityDef == null)
						continue;
				}
				if (j < playerCount) {
					int text_over_head_offset = 0;
					int l = 30;
					Player player = (Player) obj;
					if (player.headIcon >= 0) {
						npcScreenPos(((Mob) (obj)), ((Mob) (obj)).height + 15);
						if (spriteDrawX > -1) {
							if (player.skullIcon < 2) {
								skullIcons[player.skullIcon].drawSprite(
										spriteDrawX - 12, spriteDrawY - l);
								l += 25;
								if (Configuration.hpAboveHeads && Configuration.namesAboveHeads) {
									text_over_head_offset -= 25;
								} else if (Configuration.namesAboveHeads) {
									text_over_head_offset -= 23;
								} else if(Configuration.hpAboveHeads) {
									text_over_head_offset -= 33;
								}
							}
							if (player.headIcon < 13) {
								headIcons[player.headIcon].drawSprite(
										spriteDrawX - 12, spriteDrawY - l - 3);
								l += 21;
								text_over_head_offset -= 5;
								if (Configuration.hpAboveHeads && Configuration.namesAboveHeads) {
									text_over_head_offset -= 25;
								} else if (Configuration.namesAboveHeads) {
									text_over_head_offset -= 26;
								} else if(Configuration.hpAboveHeads) {
									text_over_head_offset -= 33;
								}
							}
						}
					}
					if (j >= 0 && hintIconDrawType == 10
							&& hintIconPlayerId == playerList[j]) {
						npcScreenPos(((Mob) (obj)), ((Mob) (obj)).height + 15);
						if (spriteDrawX > -1) {
							l += 13;
							text_over_head_offset -= 17;
							headIconsHint[player.hintIcon].drawSprite(
									spriteDrawX - 12, spriteDrawY - l);
						}
					}
					if (Configuration.hpAboveHeads && Configuration.namesAboveHeads) {
						newSmallFont.drawCenteredString(
								(new StringBuilder())
								.append(((Mob) (Mob) obj).currentHealth)
								.append("/")
								.append(((Mob) (Mob) obj).maxHealth)
								.toString(),
								spriteDrawX, spriteDrawY - 29 + text_over_head_offset, 0x3399ff, 100);
					} // draws HP above head
					else if (Configuration.hpAboveHeads
							&& !Configuration.namesAboveHeads) {
						newSmallFont.drawCenteredString(
								(new StringBuilder())
								.append(((Mob) (Mob) obj).currentHealth)
								.append("/")
								.append(((Mob) (Mob) obj).maxHealth)
								.toString(),
								spriteDrawX, spriteDrawY - 5 + text_over_head_offset, 0x3399ff, 100);
					}
					if (Configuration.namesAboveHeads) {
						npcScreenPos(((Mob) (obj)), ((Mob) (obj)).height + 15);
						int col = 0x0000ff;
						if (player.clanName == localPlayer.clanName)
							col = 0x00ff00;
						smallText.drawText(col, player.name, spriteDrawY - 15 + text_over_head_offset,
								spriteDrawX);
						/*if (player.clanName != "" && player.clanName != "None")
							smallText.drawText(col, "<" + player.clanName + ">",
									spriteDrawY - 5 + text_over_head_offset, spriteDrawX);*/
					}
				} else {
					Npc npc = ((Npc) obj);
					if (npc.getHeadIcon() >= 0
							&& npc.getHeadIcon() < headIcons.length) {
						npcScreenPos(((Mob) (obj)), ((Mob) (obj)).height + 15);
						if (spriteDrawX > -1)
							headIcons[npc.getHeadIcon()].drawSprite(
									spriteDrawX - 12, spriteDrawY - 30);
					}
					if (hintIconDrawType == 1
							&& hintIconNpcId == npcIndices[j - playerCount]
									&& tick % 20 < 10) {
						npcScreenPos(((Mob) (obj)), ((Mob) (obj)).height + 15);
						if (spriteDrawX > -1)
							headIconsHint[0].drawSprite(spriteDrawX - 12,
									spriteDrawY - 28);
					}
				}
				if (((Mob) (obj)).spokenText != null && (j >= playerCount
						|| publicChatMode == 0|| publicChatMode == 4 || publicChatMode == 3
						|| publicChatMode == 1
						&& isFriendOrSelf(((Player) obj).name))) {
					npcScreenPos(((Mob) (obj)), ((Mob) (obj)).height);
					if (spriteDrawX > -1 && messageLength < anInt975) {
						anIntArray979[messageLength] =
								boldText.method384(((Mob) (obj)).spokenText) / 2;
						anIntArray978[messageLength] = boldText.verticalSpace;
						anIntArray976[messageLength] = spriteDrawX;
						anIntArray977[messageLength] = spriteDrawY;
						textColourEffect[messageLength] = ((Mob) (obj)).textColour;
						anIntArray981[messageLength] = ((Mob) (obj)).textEffect;
						anIntArray982[messageLength] = ((Mob) (obj)).textCycle;
						aStringArray983[messageLength++] = ((Mob) (obj)).spokenText;
						if (anInt1249 == 0 && ((Mob) (obj)).textEffect >= 1
								&& ((Mob) (obj)).textEffect <= 3) {
							anIntArray978[messageLength] += 10;
							anIntArray977[messageLength] += 5;
						}
						if (anInt1249 == 0 && ((Mob) (obj)).textEffect == 4)
							anIntArray979[messageLength] = 60;
						if (anInt1249 == 0 && ((Mob) (obj)).textEffect == 5)
							anIntArray978[messageLength] += 5;
					}
				}
				if (((Mob) (obj)).loopCycleStatus > tick) {
					try {
						npcScreenPos(((Mob) (obj)), ((Mob) (obj)).height + 15);
						if (spriteDrawX > -1) {
							int i1 = (((Mob) (obj)).currentHealth * 30)
									/ ((Mob) (obj)).maxHealth;

							if (i1 > 30) {
								i1 = 30;
							}
							int hpPercent = (((Mob) (obj)).currentHealth * 56)
									/ ((Mob) (obj)).maxHealth;

							if (hpPercent > 56) {
								hpPercent = 56;
							} 
							
							if (!Configuration.hpBar554) {
								Rasterizer2D.drawBox(spriteDrawX - 15, spriteDrawY - 3, i1, 5,
										65280);
								Rasterizer2D.drawBox((spriteDrawX - 15) + i1, spriteDrawY - 3, 30 - i1, 5,
										0xff0000
										);
							} else {
								cacheSprite[41].drawSprite(spriteDrawX - 28, spriteDrawY - 3);
								
								cacheSprite[40] = new Sprite(hp, hpPercent, 7);
								cacheSprite[40].drawSprite(spriteDrawX - 28,
                                         spriteDrawY - 3);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				if(obj instanceof Npc) {
					Npc npc = ((Npc)obj);
					if(localPlayer.interactingEntity == -1) {

						//Is the npc interacting with us?
						//If we aren't interacting with others,
						//Start combat box timer.
						if((npc.interactingEntity - 32768) == localPlayerIndex) {
							currentInteract = npc;
							combatBoxTimer.start(10);
						}

					} else {

						//Are we interacting with the npc?
						//Start combat box timer.
						if(npc.index == localPlayer.interactingEntity) {
							currentInteract = npc;
							combatBoxTimer.start(10);
						}
					}
				} else if(obj instanceof Player) {
					Player player = ((Player)obj);
					if(localPlayer.interactingEntity == -1) {

						//Is the player interacting with us?
						//If we aren't interacting with others,
						//Start combat box timer.
						if((player.interactingEntity - 32768) == localPlayerIndex) {
							currentInteract = player;
							combatBoxTimer.start(10);
						}

					} else {
						//Are we interacting with the player?
						//Start combat box timer.
						if(player.index == localPlayer.interactingEntity - 32768) {
							currentInteract = player;
							combatBoxTimer.start(10);
						}
					}
				}

				//Drawing hits..
				if (!Configuration.hitmarks554) {
					for (int j1 = 0; j1 < 4; j1++) {
						if (((Mob) (obj)).hitsLoopCycle[j1] > tick) {
							npcScreenPos(((Mob) (obj)),
									((Mob) (obj)).height / 2);
							if (spriteDrawX > -1) {
								if (j1 == 1)
									spriteDrawY -= 20;
								if (j1 == 2) {
									spriteDrawX -= 15;
									spriteDrawY -= 10;
								}
								if (j1 == 3) {
									spriteDrawX += 15;
									spriteDrawY -= 10;
								}
								hitMarks[((Mob) (obj)).hitMarkTypes[j1]]
										.drawSprite(spriteDrawX - 12,
												spriteDrawY - 12);

								smallText.drawText(0,
										Configuration.tenXHp
										? String.valueOf(
												((Mob) (obj)).hitDamages[j1]
														* 10)
												: String.valueOf(
														((Mob) (obj)).hitDamages[j1]
																* 1),
												spriteDrawY + 4, spriteDrawX);

								smallText.drawText(0xffffff,
										Configuration.tenXHp
										? String.valueOf(
												((Mob) (obj)).hitDamages[j1]
														* 10)
												: String.valueOf(
														((Mob) (obj)).hitDamages[j1]
																* 1),
												spriteDrawY + 3, spriteDrawX - 1);
							}
						}
					}
				} else {
					for (int j2 = 0; j2 < 4; j2++) {
						if (((Mob) (obj)).hitsLoopCycle[j2] > tick) {
							npcScreenPos(((Mob) (obj)),
									((Mob) (obj)).height / 2);
							if (spriteDrawX > -1) {
								if (j2 == 0 && ((Mob) (obj)).hitDamages[j2] > 99)
									((Mob) (obj)).hitMarkTypes[j2] = 3;
								else if (j2 == 1
										&& ((Mob) (obj)).hitDamages[j2] > 99)
									((Mob) (obj)).hitMarkTypes[j2] = 3;
								else if (j2 == 2
										&& ((Mob) (obj)).hitDamages[j2] > 99)
									((Mob) (obj)).hitMarkTypes[j2] = 3;
								else if (j2 == 3
										&& ((Mob) (obj)).hitDamages[j2] > 99)
									((Mob) (obj)).hitMarkTypes[j2] = 3;
								if (j2 == 1) {
									spriteDrawY -= 20;
								}
								if (j2 == 2) {
									spriteDrawX -=
											(((Mob) (obj)).hitDamages[j2] > 99
													? 30 : 20);
									spriteDrawY -= 10;
								}
								if (j2 == 3) {
									spriteDrawX +=
											(((Mob) (obj)).hitDamages[j2] > 99
													? 30 : 20);
									spriteDrawY -= 10;
								}
								if (((Mob) (obj)).hitMarkTypes[j2] == 3) {
									spriteDrawX -= 8;
								}
								cacheSprite[hitmarks562[((Mob) (obj)).hitMarkTypes[j2]]]
										.draw24BitSprite(spriteDrawX - 12,
												spriteDrawY - 12);
								smallText.drawText(0xffffff,
										String.valueOf(
												((Mob) (obj)).hitDamages[j2]),
										spriteDrawY + 3,
										(((Mob) (obj)).hitMarkTypes[j2] == 3
										? spriteDrawX + 7
												: spriteDrawX - 1));
							}
						}
					}
				}
			}
			for (int defaultText = 0; defaultText < messageLength; defaultText++) {
				int k1 = anIntArray976[defaultText];
				int l1 = anIntArray977[defaultText];
				int j2 = anIntArray979[defaultText];
				int k2 = anIntArray978[defaultText];
				boolean flag = true;
				while (flag) {
					flag = false;
					for (int l2 = 0; l2 < defaultText; l2++)
						if (l1 + 2 > anIntArray977[l2] - anIntArray978[l2]
								&& l1 - k2 < anIntArray977[l2] + 2
								&& k1 - j2 < anIntArray976[l2] + anIntArray979[l2]
										&& k1 + j2 > anIntArray976[l2] - anIntArray979[l2]
												&& anIntArray977[l2] - anIntArray978[l2] < l1) {
							l1 = anIntArray977[l2] - anIntArray978[l2];
							flag = true;
						}

				}
				spriteDrawX = anIntArray976[defaultText];
				spriteDrawY = anIntArray977[defaultText] = l1;
				String s = aStringArray983[defaultText];
				if (anInt1249 == 0) {
					int i3 = 0xffff00;
					if (textColourEffect[defaultText] < 6)
						i3 = anIntArray965[textColourEffect[defaultText]];
					if (textColourEffect[defaultText] == 6)
						i3 = anInt1265 % 20 >= 10 ? 0xffff00 : 0xff0000;
						if (textColourEffect[defaultText] == 7)
							i3 = anInt1265 % 20 >= 10 ? 65535 : 255;
							if (textColourEffect[defaultText] == 8)
								i3 = anInt1265 % 20 >= 10 ? 0x80ff80 : 45056;
								if (textColourEffect[defaultText] == 9) {
									int j3 = 150 - anIntArray982[defaultText];
									if (j3 < 50)
										i3 = 0xff0000 + 1280 * j3;
									else if (j3 < 100)
										i3 = 0xffff00 - 0x50000 * (j3 - 50);
									else if (j3 < 150)
										i3 = 65280 + 5 * (j3 - 100);
								}
								if (textColourEffect[defaultText] == 10) {
									int k3 = 150 - anIntArray982[defaultText];
									if (k3 < 50)
										i3 = 0xff0000 + 5 * k3;
									else if (k3 < 100)
										i3 = 0xff00ff - 0x50000 * (k3 - 50);
									else if (k3 < 150)
										i3 = (255 + 0x50000 * (k3 - 100)) - 5 * (k3 - 100);
								}
								if (textColourEffect[defaultText] == 11) {
									int l3 = 150 - anIntArray982[defaultText];
									if (l3 < 50)
										i3 = 0xffffff - 0x50005 * l3;
									else if (l3 < 100)
										i3 = 65280 + 0x50005 * (l3 - 50);
									else if (l3 < 150)
										i3 = 0xffffff - 0x50000 * (l3 - 100);
								}
								if (anIntArray981[defaultText] == 0) {
									boldText.drawText(0, s, spriteDrawY + 1, spriteDrawX);
									boldText.drawText(i3, s, spriteDrawY, spriteDrawX);
								}
								if (anIntArray981[defaultText] == 1) {
									boldText.wave(0, s, spriteDrawX, anInt1265, spriteDrawY + 1);
									boldText.wave(i3, s, spriteDrawX, anInt1265, spriteDrawY);
								}
								if (anIntArray981[defaultText] == 2) {
									boldText.wave2(spriteDrawX, s, anInt1265, spriteDrawY + 1, 0);
									boldText.wave2(spriteDrawX, s, anInt1265, spriteDrawY, i3);
								}
								if (anIntArray981[defaultText] == 3) {
									boldText.shake(150 - anIntArray982[defaultText], s, anInt1265,
											spriteDrawY + 1, spriteDrawX, 0);
									boldText.shake(150 - anIntArray982[defaultText], s, anInt1265,
											spriteDrawY, spriteDrawX, i3);
								}
								if (anIntArray981[defaultText] == 4) {
									int i4 = boldText.method384(s);
									int k4 = ((150 - anIntArray982[defaultText]) * (i4 + 100))
											/ 150;
									Rasterizer2D.setDrawingArea(334, spriteDrawX - 50, spriteDrawX + 50,
											0);
									boldText.render(0, s, spriteDrawY + 1, (spriteDrawX + 50) - k4);
									boldText.render(i3, s, spriteDrawY, (spriteDrawX + 50) - k4);
									Rasterizer2D.defaultDrawingAreaSize();
								}
								if (anIntArray981[defaultText] == 5) {
									int j4 = 150 - anIntArray982[defaultText];
									int l4 = 0;
									if (j4 < 25)
										l4 = j4 - 25;
									else if (j4 > 125)
										l4 = j4 - 125;
									Rasterizer2D.setDrawingArea(spriteDrawY + 5, 0, 512,
											spriteDrawY - boldText.verticalSpace - 1);
									boldText.drawText(0, s, spriteDrawY + 1 + l4, spriteDrawX);
									boldText.drawText(i3, s, spriteDrawY + l4, spriteDrawX);
									Rasterizer2D.defaultDrawingAreaSize();
								}
				} else {
					boldText.drawText(0, s, spriteDrawY + 1, spriteDrawX);
					boldText.drawText(0xffff00, s, spriteDrawY, spriteDrawX);
				}
			}
		} catch (Exception e) {
		}
	}

	// local variables will not stay in memory which will help with performance.
	final int[] sideIconsX =
		{17, 49, 81, 114, 146, 180, 214, 16, 49, 82, 116, 148, 184, 217},
		sideIconsY = {9, 7, 5, 5, 2, 3, 7, 303, 306, 306, 302, 305, 303, 304, 303},
		sideIconsId = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13},
		sideIconsTab = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};

	public void drawSideIcons() {

		int xOffset = frameMode == ScreenMode.FIXED ? 0 : frameWidth - 247;
		int yOffset = frameMode == ScreenMode.FIXED ? 0 : frameHeight - 336;
		if (frameMode == ScreenMode.FIXED || frameMode != ScreenMode.FIXED && !changeTabArea) {
			for (int i = 0; i < sideIconsTab.length; i++) {
				if (tabInterfaceIDs[sideIconsTab[i]] != -1) {
					if (sideIconsId[i] != -1) {
						Sprite sprite = sideIcons[sideIconsId[i]];

						//Replace quest tab icon with spawn tab icon
						//Replace music tab icon with pvp icon
						if(i == 2) {
							Client.cacheSprite[336].drawAdvancedSprite(sideIconsX[i] + xOffset,
									sideIconsY[i] + yOffset);
						} else if(i == 13) {
							Client.cacheSprite[444].drawAdvancedSprite(sideIconsX[i] + xOffset,
									sideIconsY[i] + yOffset);
						} else {
							sprite.drawSprite(sideIconsX[i] + xOffset,
									sideIconsY[i] + yOffset );
						}

					}
				}
			}
		} else if (changeTabArea && frameWidth < 1000) {
			int[] iconId = {0, 1, 2, 3, 4, 5, 6, -1, 8, 9, 7, 11, 12, 13};
			int[] iconX = {219, 189, 156, 126, 93, 62, 30, 219, 189, 156, 124, 92, 59, 28};
			int[] iconY = {67, 69, 67, 69, 72, 72, 69, 32, 29, 29, 32, 30, 33, 31, 32};
			for (int i = 0; i < sideIconsTab.length; i++) {
				if (tabInterfaceIDs[sideIconsTab[i]] != -1) {
					if (iconId[i] != -1) {
						Sprite sprite = sideIcons[iconId[i]];

						//Replace quest tab icon with spawn tab icon
						//Replace music tab icon with pvp icon
						if(i == 2) {
							Client.cacheSprite[336].drawAdvancedSprite(frameWidth - iconX[i],
									frameHeight - iconY[i]);
						} else if(i == 13) {
							Client.cacheSprite[352].drawAdvancedSprite(frameWidth - iconX[i],
									frameHeight - iconY[i]);
						} else {

							sprite.drawSprite(frameWidth - iconX[i],
									frameHeight - iconY[i]);
						}
					}
				}
			}
		} else if (changeTabArea && frameWidth >= 1000) {
			int[] iconId = {0, 1, 2, 3, 4, 5, 6, -1, 8, 9, 7, 11, 12, 13};
			int[] iconX =
				{50, 80, 114, 143, 176, 208, 240, 242, 273, 306, 338, 370, 404, 433};
			int[] iconY = {30, 32, 30, 32, 34, 34, 32, 32, 29, 29, 32, 31, 32, 32, 32};
			for (int i = 0; i < sideIconsTab.length; i++) {
				if (tabInterfaceIDs[sideIconsTab[i]] != -1) {
					if (iconId[i] != -1) {
						Sprite sprite = sideIcons[iconId[i]];

						//Replace quest tab icon with spawn tab icon
						if(i == 2) {
							sprite = Client.cacheSprite[336];
						}

						sprite.drawSprite(frameWidth - 461 + iconX[i],
								frameHeight - iconY[i]);
					}
				}
			}
		}
	}

	private void drawRedStones() {

		final int[] redStonesX =
			{6, 44, 77, 110, 143, 176, 209, 6, 44, 77, 110, 143, 176, 209},
			redStonesY = {0, 0, 0, 0, 0, 0, 0, 298, 298, 298, 298, 298, 298, 298},
			redStonesId = {35, 39, 39, 39, 39, 39, 36, 37, 39, 39, 39, 39, 39, 38};

		int xOffset = frameMode == ScreenMode.FIXED ? 0 : frameWidth - 247;
		int yOffset = frameMode == ScreenMode.FIXED ? 0 : frameHeight - 336;
		if (frameMode == ScreenMode.FIXED || frameMode != ScreenMode.FIXED && !changeTabArea) {
			if (tabInterfaceIDs[tabId] != -1 && tabId != 15) {
				cacheSprite[redStonesId[tabId]].drawSprite(redStonesX[tabId] + xOffset,
						redStonesY[tabId] + yOffset);
			}
		} else if (changeTabArea && frameWidth < 1000) {
			int[] stoneX = {226, 194, 162, 130, 99, 65, 34, 219, 195, 161, 130, 98, 65, 33};
			int[] stoneY = {73, 73, 73, 73, 73, 73, 73, -1, 37, 37, 37, 37, 37, 37, 37};
			if (tabInterfaceIDs[tabId] != -1 && tabId != 10 && showTabComponents) {
				if (tabId == 7) {
					cacheSprite[39].drawSprite(frameWidth - 130, frameHeight - 37);
				}
				cacheSprite[39].drawSprite(frameWidth - stoneX[tabId],
						frameHeight - stoneY[tabId]);
			}
		} else if (changeTabArea && frameWidth >= 1000) {
			int[] stoneX =
				{417, 385, 353, 321, 289, 256, 224, 129, 193, 161, 130, 98, 65, 33};
			if (tabInterfaceIDs[tabId] != -1 && tabId != 10 && showTabComponents) {
				cacheSprite[39].drawSprite(frameWidth - stoneX[tabId], frameHeight - 37);
			}
		}
	}
	
	public String getNameForTab(int tab) {
		switch(tab) {
		case 0:
			return "Combat";
		case 1:
			return "Stats";
		case 2:
			return "Spawn tab";
		case 3:
			return "Inventory";
		case 4:
			return "Equipment";
		case 5:
			return "Prayer";
		case 6:
			return "Magic";
		case 7:
			return "Clan chat";
		case 8:
			return "Friends";
		case 9:
			return "Ignores";
		case 10:
			return "Logout";
		case 11:
			return "Settings";
		case 12:
			return "Emotes";
		case 13:
			return "PvP";
		}
		return "";
	}

	private void drawTabArea() {
		final int xOffset = frameMode == ScreenMode.FIXED ? 0 : frameWidth - 241;
		final int yOffset = frameMode == ScreenMode.FIXED ? 0 : frameHeight - 336;
		if (frameMode == ScreenMode.FIXED) {
			tabImageProducer.initDrawingArea();
		}
		Rasterizer3D.scanOffsets = anIntArray1181;
		if (frameMode == ScreenMode.FIXED) {
			cacheSprite[21].drawSprite(0, 0);
			if(sidebarGlow) {
				if(pcOpacity < 70) {
					pcOpacity = 70;
					maxOpacity = false;
				}
				if(pcOpacity > 252) {
					pcOpacity = 252;
					maxOpacity = true;
				}
				if(pcOpacity >= 70 && !maxOpacity) {
					pcOpacity += 4;
					if(pcOpacity == 252) {
						maxOpacity = true;
					}
				}
				if(pcOpacity <= 252 && maxOpacity) {
					pcOpacity -= 4;
					if(pcOpacity == 70) {
						maxOpacity = false;
					}
				}
			cacheSprite[602].drawTransparentSprite(0, 0, pcOpacity);	
			}
		} else if (frameMode != ScreenMode.FIXED && !changeTabArea) {
			Rasterizer2D.drawTransparentBox(frameWidth - 217, frameHeight - 304, 195, 270, 0x3E3529,
					transparentTabArea ? 80 : 256);
			cacheSprite[47].drawSprite(xOffset, yOffset);
		} else {
			if (frameWidth >= 1000) {
				if (showTabComponents) {
					Rasterizer2D.drawTransparentBox(frameWidth - 197, frameHeight - 304, 197, 265, 0x3E3529,
							transparentTabArea ? 80 : 256);
					cacheSprite[50].drawSprite(frameWidth - 204, frameHeight - 311);
				}
				for (int x = frameWidth - 417, y = frameHeight - 37, index =
						0; x <= frameWidth - 30 && index < 13; x += 32, index++) {
					cacheSprite[46].drawSprite(x, y);
				}
			} else if (frameWidth < 1000) {
				if (showTabComponents) {
					Rasterizer2D.drawTransparentBox(frameWidth - 197, frameHeight - 341, 195, 265, 0x3E3529,
							transparentTabArea ? 80 : 256);
					cacheSprite[50].drawSprite(frameWidth - 204, frameHeight - 348);
				}
				for (int x = frameWidth - 226, y = frameHeight - 73, index =
						0; x <= frameWidth - 32 && index < 7; x += 32, index++) {
					cacheSprite[46].drawSprite(x, y);
				}
				for (int x = frameWidth - 226, y = frameHeight - 37, index =
						0; x <= frameWidth - 32 && index < 7; x += 32, index++) {
					cacheSprite[46].drawSprite(x, y);
				}
			}
		}
		if (overlayInterfaceId == -1) {
			drawRedStones();
			drawSideIcons();
		}
		if (showTabComponents) {
			int x = frameMode == ScreenMode.FIXED ? 31 : frameWidth - 215;
			int y = frameMode == ScreenMode.FIXED ? 37 : frameHeight - 299;
			if (changeTabArea) {
				x = frameWidth - 197;
				y = frameWidth >= 1000 ? frameHeight - 303 : frameHeight - 340;
			}
			try {
				if (overlayInterfaceId != -1) {
					drawInterface(0, x, Widget.interfaceCache[overlayInterfaceId], y);
				} else if (tabInterfaceIDs[tabId] != -1) {
					drawInterface(0, x, Widget.interfaceCache[tabInterfaceIDs[tabId]], y);
				}
			} catch (Exception ex) {

			}
		}
		if (menuOpen) {
			drawMenu(frameMode == ScreenMode.FIXED ? 516 : 0,
					frameMode == ScreenMode.FIXED ? 168 : 0);
		}
		if (frameMode == ScreenMode.FIXED) {
			tabImageProducer.drawGraphics(168, super.graphics, 516);
			gameScreenImageProducer.initDrawingArea();
		}
		Rasterizer3D.scanOffsets = anIntArray1182;
	}

	private void writeBackgroundTexture(int j) {
		if (Rasterizer3D.textureLastUsed[59] >= j) {
			IndexedImage background_1 = Rasterizer3D.textures[59];
			int l = background_1.width * background_1.height - 1;
			int k1 = background_1.width * tickDelta * 2;
			byte abyte1[] = background_1.palettePixels;
			byte abyte4[] = aByteArray912;
			for (int j2 = 0; j2 <= l; j2++)
				abyte4[j2] = abyte1[j2 - k1 & l];

			background_1.palettePixels = abyte4;
			aByteArray912 = abyte1;
			Rasterizer3D.requestTextureUpdate(59);
		}
		if (!lowMemory) {
			if (Rasterizer3D.textureLastUsed[17] >= j) {
				IndexedImage background = Rasterizer3D.textures[17];
				int k = background.width * background.height - 1;
				int j1 = background.width * tickDelta * 2;
				byte raster[] = background.palettePixels;
				byte abyte3[] = aByteArray912;
				for (int i2 = 0; i2 <= k; i2++)
					abyte3[i2] = raster[i2 - j1 & k];

				background.palettePixels = abyte3;
				aByteArray912 = raster;
				Rasterizer3D.requestTextureUpdate(17);
				anInt854++;
				if (anInt854 > 1235) {
					anInt854 = 0;
					/*Anticheat?
                              outgoing.writeOpcode(226);
                              outgoing.writeByte(0);
                              int l2 = outgoing.currentPosition;
                              outgoing.writeShort(58722);
                              outgoing.writeByte(240);
                              outgoing.writeShort((int) (Math.random() * 65536D));
                              outgoing.writeByte((int) (Math.random() * 256D));
                              if ((int) (Math.random() * 2D) == 0)
                                    outgoing.writeShort(51825);
                              outgoing.writeByte((int) (Math.random() * 256D));
                              outgoing.writeShort((int) (Math.random() * 65536D));
                              outgoing.writeShort(7130);
                              outgoing.writeShort((int) (Math.random() * 65536D));
                              outgoing.writeShort(61657);
                              outgoing.writeBytes(outgoing.currentPosition - l2);*/
				}
			}
			if (Rasterizer3D.textureLastUsed[24] >= j) {
				IndexedImage background_1 = Rasterizer3D.textures[24];
				int l = background_1.width * background_1.height - 1;
				int k1 = background_1.width * tickDelta * 2;
				byte abyte1[] = background_1.palettePixels;
				byte abyte4[] = aByteArray912;
				for (int j2 = 0; j2 <= l; j2++)
					abyte4[j2] = abyte1[j2 - k1 & l];

				background_1.palettePixels = abyte4;
				aByteArray912 = abyte1;
				Rasterizer3D.requestTextureUpdate(24);
			}
			if (Rasterizer3D.textureLastUsed[34] >= j) {
				IndexedImage background_2 = Rasterizer3D.textures[34];
				int i1 = background_2.width * background_2.height - 1;
				int l1 = background_2.width * tickDelta * 2;
				byte abyte2[] = background_2.palettePixels;
				byte abyte5[] = aByteArray912;
				for (int k2 = 0; k2 <= i1; k2++)
					abyte5[k2] = abyte2[k2 - l1 & i1];

				background_2.palettePixels = abyte5;
				aByteArray912 = abyte2;
				Rasterizer3D.requestTextureUpdate(34);
			}
			if (Rasterizer3D.textureLastUsed[40] >= j) {
				IndexedImage background_2 = Rasterizer3D.textures[40];
				int i1 = background_2.width * background_2.height - 1;
				int l1 = background_2.width * tickDelta * 2;
				byte abyte2[] = background_2.palettePixels;
				byte abyte5[] = aByteArray912;
				for (int k2 = 0; k2 <= i1; k2++)
					abyte5[k2] = abyte2[k2 - l1 & i1];

				background_2.palettePixels = abyte5;
				aByteArray912 = abyte2;
				Rasterizer3D.requestTextureUpdate(40);
			}
		}
	}

	private void processMobChatText() {
		for (int i = -1; i < playerCount; i++) {
			int j;
			if (i == -1)
				j = internalLocalPlayerIndex;
			else
				j = playerList[i];
			Player player = players[j];
			if (player != null && player.textCycle > 0) {
				player.textCycle--;
				if (player.textCycle == 0)
					player.spokenText = null;
			}
		}
		for (int k = 0; k < npcCount; k++) {
			int l = npcIndices[k];
			Npc npc = npcs[l];
			if (npc != null && npc.textCycle > 0) {
				npc.textCycle--;
				if (npc.textCycle == 0)
					npc.spokenText = null;
			}
		}
	}

	private void calculateCameraPosition() {
		int i = x * 128 + 64;
		int j = y * 128 + 64;
		int k = getCenterHeight(plane, j, i) - height;
		if (xCameraPos < i) {
			xCameraPos += speed + ((i - xCameraPos) * angle) / 1000;
			if (xCameraPos > i)
				xCameraPos = i;
		}
		if (xCameraPos > i) {
			xCameraPos -= speed + ((xCameraPos - i) * angle) / 1000;
			if (xCameraPos < i)
				xCameraPos = i;
		}
		if (zCameraPos < k) {
			zCameraPos += speed + ((k - zCameraPos) * angle) / 1000;
			if (zCameraPos > k)
				zCameraPos = k;
		}
		if (zCameraPos > k) {
			zCameraPos -= speed + ((zCameraPos - k) * angle) / 1000;
			if (zCameraPos < k)
				zCameraPos = k;
		}
		if (yCameraPos < j) {
			yCameraPos += speed + ((j - yCameraPos) * angle) / 1000;
			if (yCameraPos > j)
				yCameraPos = j;
		}
		if (yCameraPos > j) {
			yCameraPos -= speed + ((yCameraPos - j) * angle) / 1000;
			if (yCameraPos < j)
				yCameraPos = j;
		}
		i = cinematicCamXViewpointLoc * 128 + 64;
		j = cinematicCamYViewpointLoc * 128 + 64;
		k = getCenterHeight(plane, j, i) - cinematicCamZViewpointLoc;
		int l = i - xCameraPos;
		int i1 = k - zCameraPos;
		int j1 = j - yCameraPos;
		int k1 = (int) Math.sqrt(l * l + j1 * j1);
		int l1 = (int) (Math.atan2(i1, k1) * 325.94900000000001D) & 0x7ff;
		int i2 = (int) (Math.atan2(l, j1) * -325.94900000000001D) & 0x7ff;
		if (l1 < 128)
			l1 = 128;
		if (l1 > 383)
			l1 = 383;
		if (yCameraCurve < l1) {
			yCameraCurve += constCinematicCamRotationSpeed + ((l1 - yCameraCurve) * varCinematicCamRotationSpeedPromille) / 1000;
			if (yCameraCurve > l1)
				yCameraCurve = l1;
		}
		if (yCameraCurve > l1) {
			yCameraCurve -= constCinematicCamRotationSpeed + ((yCameraCurve - l1) * varCinematicCamRotationSpeedPromille) / 1000;
			if (yCameraCurve < l1)
				yCameraCurve = l1;
		}
		int j2 = i2 - xCameraCurve;
		if (j2 > 1024)
			j2 -= 2048;
		if (j2 < -1024)
			j2 += 2048;
		if (j2 > 0) {
			xCameraCurve += constCinematicCamRotationSpeed + (j2 * varCinematicCamRotationSpeedPromille) / 1000;
			xCameraCurve &= 0x7ff;
		}
		if (j2 < 0) {
			xCameraCurve -= constCinematicCamRotationSpeed + (-j2 * varCinematicCamRotationSpeedPromille) / 1000;
			xCameraCurve &= 0x7ff;
		}
		int k2 = i2 - xCameraCurve;
		if (k2 > 1024)
			k2 -= 2048;
		if (k2 < -1024)
			k2 += 2048;
		if (k2 < 0 && j2 > 0 || k2 > 0 && j2 < 0)
			xCameraCurve = i2;
	}

	public void drawMenu(int x, int y) {
		int xPos = menuOffsetX - (x - 4);
		int yPos = (-y + 4) + menuOffsetY;
		int w = menuWidth;
		int h = menuHeight + 1;
		updateChatbox = true;
		tabAreaAltered = true;
		int menuColor = 0x5d5447;
		Rasterizer2D.drawBox(xPos, yPos, w, h, menuColor);
		Rasterizer2D.drawBox(xPos + 1, yPos + 1, w - 2, 16, 0);
		Rasterizer2D.drawBoxOutline(xPos + 1, yPos + 18, w - 2, h - 19, 0);
		boldText.render(menuColor, "Choose Option", yPos + 14, xPos + 3);
		int mouseX = super.mouseX - (x);
		int mouseY = (-y) + super.mouseY;
		for (int i = 0; i < menuActionRow; i++) {
			int textY = yPos + 31 + (menuActionRow - 1 - i) * 15;
			int textColor = 0xffffff;
			if (mouseX > xPos && mouseX < xPos + w && mouseY > textY - 13
					&& mouseY < textY + 3) {
				Rasterizer2D.drawBox(xPos + 3, textY - 11, menuWidth - 6, 15, 0x6f695d);
				textColor = 0xffff00;
			}
			boldText.drawTextWithPotentialShadow(true, xPos + 3, textColor, menuActionText[i],
					textY);
		}
	}

	private void addFriend(long nameHash) {
		//try {
		if (nameHash == 0L)
			return;
		sendPacket(new AddFriend(nameHash)); 
		/*if (friendsCount >= 100 && member != 1) {
				sendMessage("Your friendlist is full. Max of 100 for free users, and 200 for members",
						0, "");
				return;
			}
			if (friendsCount >= 200) {
				sendMessage("Your friendlist is full. Max of 100 for free users, and 200 for members",
						0, "");
				return;
			}
			String s = StringUtils.formatText(StringUtils.decodeBase37(nameHash));
			for (int i = 0; i < friendsCount; i++)
				if (friendsListAsLongs[i] == nameHash) {
					sendMessage(s + " is already on your friend list", 0, "");
					return;
				}
			for (int j = 0; j < ignoreCount; j++)
				if (ignoreListAsLongs[j] == nameHash) {
					sendMessage("Please remove " + s + " from your ignore list first", 0,
							"");
					return;
				}

			if (s.equals(localPlayer.name)) {
				return;
			} else {
				friendsList[friendsCount] = s;
				friendsListAsLongs[friendsCount] = nameHash;
				friendsNodeIDs[friendsCount] = 0;
				friendsCount++;
				sendPacket(new AddFriend(nameHash));                  
				return;
			}
		} catch (RuntimeException runtimeexception) {
			SignLink.reporterror("15283, " + (byte) 68 + ", " + nameHash + ", "
					+ runtimeexception.toString());
		}
		throw new RuntimeException();*/
	}

	private int getCenterHeight(int z, int y, int x) {
		int worldX = x >> 7;
			int worldY = y >> 7;
						if (worldX < 0 || worldY < 0 || worldX > 103 || worldY > 103)
							return 0;
						int plane = z;
						if (plane < 3 && (tileFlags[1][worldX][worldY] & 2) == 2)
							plane++;
						int sizeX = x & 0x7f;
						int sizeY = y & 0x7f;
						int i2 = tileHeights[plane][worldX][worldY] * (128 - sizeX)
								+ tileHeights[plane][worldX + 1][worldY] * sizeX >> 7;
						int j2 = tileHeights[plane][worldX][worldY + 1] * (128 - sizeX)
								+ tileHeights[plane][worldX + 1][worldY + 1] * sizeX >> 7;
						return i2 * (128 - sizeY) + j2 * sizeY >> 7;
	}

	private static String intToKOrMil(int j) {
		if (j < 0x186a0)
			return String.valueOf(j);
		if (j < 0x989680)
			return j / 1000 + "K";
		else
			return j / 0xf4240 + "M";
	}

	private void resetLogout() {
		try {
			if (socketStream != null)
				socketStream.close();
		} catch (Exception _ex) {
		}
		firstLoginMessage = secondLoginMessage = "";
		effects_list.clear();
		socketStream = null;
		loggedIn = false;
		loginScreenState = 0;
		unlinkCaches();
		scene.initToNull();
		for (int i = 0; i < 4; i++)
			collisionMaps[i].initialize();
		Arrays.fill(chatMessages, null);
		System.gc();
		stopMidi();
		currentSong = -1;
		nextSong = -1;
		prevSong = 0;
		frameMode(ScreenMode.FIXED);
	}

	private void changeCharacterGender() {
		aBoolean1031 = true;
		for (int j = 0; j < 7; j++) {
			anIntArray1065[j] = -1;
			for (int k = 0; k < IdentityKit.length; k++) {
				if (IdentityKit.kits[k].validStyle
						|| IdentityKit.kits[k].part != j + (maleCharacter ? 0 : 7))
					continue;
				anIntArray1065[j] = k;
				break;
			}
		}
	}

	private void updateNPCMovement(int i, Buffer stream) {
		while (stream.bitPosition + 21 < i * 8) {
			int k = stream.readBits(14);
			if (k == 16383)
				break;
			if (npcs[k] == null)
				npcs[k] = new Npc();
			Npc npc = npcs[k];
			npcIndices[npcCount++] = k;
			npc.time = tick;
			int l = stream.readBits(5);
			if (l > 15)
				l -= 32;
			int i1 = stream.readBits(5);
			if (i1 > 15)
				i1 -= 32;
			int j1 = stream.readBits(1);
			npc.desc = NpcDefinition.lookup(stream.readBits(Configuration.npcBits));
			int k1 = stream.readBits(1);
			if (k1 == 1)
				mobsAwaitingUpdate[mobsAwaitingUpdateCount++] = k;
			npc.size = npc.desc.size;
			npc.degreesToTurn = npc.desc.degreesToTurn;
			npc.walkAnimIndex = npc.desc.walkAnim;
			npc.turn180AnimIndex = npc.desc.turn180AnimIndex;
			npc.turn90CWAnimIndex = npc.desc.turn90CWAnimIndex;
			npc.turn90CCWAnimIndex = npc.desc.turn90CCWAnimIndex;
			npc.idleAnimation = npc.desc.standAnim;
			npc.setPos(localPlayer.pathX[0] + i1, localPlayer.pathY[0] + l, j1 == 1);
		}
		stream.disableBitAccess();
	}

	public void processGameLoop() {
		if (rsAlreadyLoaded || loadingError || genericLoadingError)
			return;
		tick++;
		if (!loggedIn) {
			processLoginScreenInput();
		} else {
			mainGameProcessor();
		}
		processOnDemandQueue();
	}

	private void showPrioritizedPlayers() {
		showPlayer(localPlayer, internalLocalPlayerIndex << 14, true);

		//Draw the player we're interacting with
		//Interacting includes combat, following, etc.
		int interact = localPlayer.interactingEntity - 32768;
		if(interact > 0) {
			Player player = players[interact];			
			showPlayer(player, interact << 14, false);
		}
	}

	private void showOtherPlayers() {
		for (int l = 0; l < playerCount; l++) {
			Player player = players[playerList[l]];
			int index = playerList[l] << 14;

			//Don't draw interacting player as we've already drawn it on top
			int interact_index = (localPlayer.interactingEntity - 32768);
			if(interact_index > 0 && index == interact_index << 14) {
				continue;
			}

			if(!showPlayer(player, index, false)) {
				continue;
			}
		}
	}

	private boolean showPlayer(Player player, int i1, boolean flag) {
		if (player == null || !player.isVisible()) {
			return false;
		}
		if (localPlayer.x >> 7 == destinationX && localPlayer.y >> 7 == destY)
			destinationX = 0;
		player.aBoolean1699 = (lowMemory && playerCount > 50 || playerCount > 200) && !flag && player.movementAnimation == player.idleAnimation;
		int j1 = player.x >> 7;
			int k1 = player.y >> 7;
			if (j1 < 0 || j1 >= 104 || k1 < 0 || k1 >= 104) {
				return false;
			}
			if (player.playerModel != null && tick >= player.objectModelStart && tick < player.objectModelStop) {
				player.aBoolean1699 = false;
				player.anInt1709 = getCenterHeight(plane, player.y, player.x);
				scene.addToScenePlayerAsObject(plane, player.y, player, player.orientation, player.objectAnInt1722GreaterYLoc, player.x, player.anInt1709, player.objectAnInt1719LesserXLoc, player.objectAnInt1721GreaterXLoc, i1, player.objectAnInt1720LesserYLoc);
				return false;
			}
			if ((player.x & 0x7f) == 64 && (player.y & 0x7f) == 64) {
				if (anIntArrayArray929[j1][k1] == anInt1265) {
					return false;
				}
				anIntArrayArray929[j1][k1] = anInt1265;
			}
			player.anInt1709 = getCenterHeight(plane, player.y, player.x);
			scene.addAnimableA(plane, player.orientation, player.anInt1709, i1, player.y, 60, player.x, player, player.animationStretches);
			return true;
	}

	private boolean promptUserForInput(Widget widget) {              
		int contentType = widget.contentType;
		if (friendServerStatus == 2) {
			if (contentType == 201) {
				updateChatbox = true;
				inputDialogState = 0;
				messagePromptRaised = true;
				promptInput = "";
				friendsListAction = 1;
				aString1121 = "Enter name of friend to add to list";
			}
			if (contentType == 202) {
				updateChatbox = true;
				inputDialogState = 0;
				messagePromptRaised = true;
				promptInput = "";
				friendsListAction = 2;
				aString1121 = "Enter name of friend to delete from list";
			}
		}
		if (contentType == 205) {
			anInt1011 = 250;
			return true;
		}
		if (contentType == 501) {
			updateChatbox = true;
			inputDialogState = 0;
			messagePromptRaised = true;
			promptInput = "";
			friendsListAction = 4;
			aString1121 = "Enter name of player to add to list";
		}
		if (contentType == 502) {
			updateChatbox = true;
			inputDialogState = 0;
			messagePromptRaised = true;
			promptInput = "";
			friendsListAction = 5;
			aString1121 = "Enter name of player to delete from list";
		}
		if (contentType == 550) {
			updateChatbox = true;
			inputDialogState = 0;
			messagePromptRaised = true;
			promptInput = "";
			friendsListAction = 6;
			aString1121 = "Enter the name of the chat you wish to join";
		}
		if (contentType >= 300 && contentType <= 313) {
			int k = (contentType - 300) / 2;
			int j1 = contentType & 1;
			int i2 = anIntArray1065[k];
			if (i2 != -1) {
				do {
					if (j1 == 0 && --i2 < 0)
						i2 = IdentityKit.length - 1;
					if (j1 == 1 && ++i2 >= IdentityKit.length)
						i2 = 0;
				} while (IdentityKit.kits[i2].validStyle
						|| IdentityKit.kits[i2].part != k + (maleCharacter ? 0 : 7));
				anIntArray1065[k] = i2;
				aBoolean1031 = true;
			}
		}
		if (contentType >= 314 && contentType <= 323) {
			int l = (contentType - 314) / 2;
			int k1 = contentType & 1;
			int j2 = characterDesignColours[l];
			if (k1 == 0 && --j2 < 0)
				j2 = PLAYER_BODY_RECOLOURS[l].length - 1;
			if (k1 == 1 && ++j2 >= PLAYER_BODY_RECOLOURS[l].length)
				j2 = 0;
			characterDesignColours[l] = j2;
			aBoolean1031 = true;
		}
		if (contentType == 324 && !maleCharacter) {
			maleCharacter = true;
			changeCharacterGender();
		}
		if (contentType == 325 && maleCharacter) {
			maleCharacter = false;
			changeCharacterGender();
		}
		if (contentType == 326) {
			sendPacket(new ChangeAppearance(maleCharacter, anIntArray1065, characterDesignColours));
			return true;
		}

		if (contentType == 613) {
			canMute = !canMute;
		}

		if (contentType >= 601 && contentType <= 612) {
			clearTopInterfaces();
			if (reportAbuseInput.length() > 0) {
				/* outgoing.writeOpcode(PacketConstants.REPORT_PLAYER);
                        outgoing.writeLong(StringUtils.encodeBase37(reportAbuseInput));
                        outgoing.writeByte(contentType - 601);
                        outgoing.writeByte(canMute ? 1 : 0);*/
			}
		}
		return false;
	}

	private void parsePlayerSynchronizationMask(Buffer stream) {            
		for (int count = 0; count < mobsAwaitingUpdateCount; count++) {                  
			int index = mobsAwaitingUpdate[count];                  
			Player player = players[index];

			int mask = stream.readUnsignedByte();

			if ((mask & 0x40) != 0) {
				mask += stream.readUnsignedByte() << 8;
			}

			appendPlayerUpdateMask(mask, index, stream, player);                  
		}
	}

	private void drawMapScenes(int i, int k, int l, int i1, int j1) {
		int k1 = scene.getWallObjectUid(j1, l, i);
		if (k1 != 0) {
			int l1 = scene.getMask(j1, l, i, k1);
			int k2 = l1 >> 6 & 3;
				int i3 = l1 & 0x1f;
				int k3 = k;
				if (k1 > 0)
					k3 = i1;
				int ai[] = minimapImage.myPixels;
				int k4 = 24624 + l * 4 + (103 - i) * 512 * 4;
				int i5 = k1 >> 14 & 0x7fff;
									ObjectDefinition def = ObjectDefinition.lookup(i5);                  
									if (def.mapscene != -1) {
										IndexedImage background_2 = mapScenes[def.mapscene];
										if (background_2 != null) {
											int i6 = (def.objectSizeX * 4 - background_2.width) / 2;
											int j6 = (def.objectSizeY * 4 - background_2.height) / 2;
											background_2.draw(48 + l * 4 + i6,
													48 + (104 - i - def.objectSizeY) * 4 + j6);
										}
									} else {
										if (i3 == 0 || i3 == 2)
											if (k2 == 0) {
												ai[k4] = k3;
												ai[k4 + 512] = k3;
												ai[k4 + 1024] = k3;
												ai[k4 + 1536] = k3;
											} else if (k2 == 1) {
												ai[k4] = k3;
												ai[k4 + 1] = k3;
												ai[k4 + 2] = k3;
												ai[k4 + 3] = k3;
											} else if (k2 == 2) {
												ai[k4 + 3] = k3;
												ai[k4 + 3 + 512] = k3;
												ai[k4 + 3 + 1024] = k3;
												ai[k4 + 3 + 1536] = k3;
											} else if (k2 == 3) {
												ai[k4 + 1536] = k3;
												ai[k4 + 1536 + 1] = k3;
												ai[k4 + 1536 + 2] = k3;
												ai[k4 + 1536 + 3] = k3;
											}
										if (i3 == 3)
											if (k2 == 0)
												ai[k4] = k3;
											else if (k2 == 1)
												ai[k4 + 3] = k3;
											else if (k2 == 2)
												ai[k4 + 3 + 1536] = k3;
											else if (k2 == 3)
												ai[k4 + 1536] = k3;
										if (i3 == 2)
											if (k2 == 3) {
												ai[k4] = k3;
												ai[k4 + 512] = k3;
												ai[k4 + 1024] = k3;
												ai[k4 + 1536] = k3;
											} else if (k2 == 0) {
												ai[k4] = k3;
												ai[k4 + 1] = k3;
												ai[k4 + 2] = k3;
												ai[k4 + 3] = k3;
											} else if (k2 == 1) {
												ai[k4 + 3] = k3;
												ai[k4 + 3 + 512] = k3;
												ai[k4 + 3 + 1024] = k3;
												ai[k4 + 3 + 1536] = k3;
											} else if (k2 == 2) {
												ai[k4 + 1536] = k3;
												ai[k4 + 1536 + 1] = k3;
												ai[k4 + 1536 + 2] = k3;
												ai[k4 + 1536 + 3] = k3;
											}
									}
		}
		k1 = scene.getGameObjectUid(j1, l, i);
		if (k1 != 0) {
			int i2 = scene.getMask(j1, l, i, k1);
			int l2 = i2 >> 6 & 3;
									int j3 = i2 & 0x1f;
									int l3 = k1 >> 14 & 0x7fff;
									ObjectDefinition class46_1 = ObjectDefinition.lookup(l3);
									if (class46_1.mapscene != -1) {
										IndexedImage background_1 = mapScenes[class46_1.mapscene];
										if (background_1 != null) {
											int j5 = (class46_1.objectSizeX * 4 - background_1.width) / 2;
											int k5 = (class46_1.objectSizeY * 4 - background_1.height) / 2;
											background_1.draw(48 + l * 4 + j5,
													48 + (104 - i - class46_1.objectSizeY) * 4 + k5);
										}
									} else if (j3 == 9) {
										int l4 = 0xeeeeee;
										if (k1 > 0)
											l4 = 0xee0000;
										int ai1[] = minimapImage.myPixels;
										int l5 = 24624 + l * 4 + (103 - i) * 512 * 4;
										if (l2 == 0 || l2 == 2) {
											ai1[l5 + 1536] = l4;
											ai1[l5 + 1024 + 1] = l4;
											ai1[l5 + 512 + 2] = l4;
											ai1[l5 + 3] = l4;
										} else {
											ai1[l5] = l4;
											ai1[l5 + 512 + 1] = l4;
											ai1[l5 + 1024 + 2] = l4;
											ai1[l5 + 1536 + 3] = l4;
										}
									}
		}
		k1 = scene.getGroundDecorationUid(j1, l, i);
		if (k1 != 0) {
			int j2 = k1 >> 14 & 0x7fff;
			ObjectDefinition class46 = ObjectDefinition.lookup(j2);
			if (class46.mapscene != -1) {
				IndexedImage background = mapScenes[class46.mapscene];
				if (background != null) {
					int i4 = (class46.objectSizeX * 4 - background.width) / 2;
					int j4 = (class46.objectSizeY * 4 - background.height) / 2;
					background.draw(48 + l * 4 + i4,
							48 + (104 - i - class46.objectSizeY) * 4 + j4);
				}
			}
		}
	}

	private void loadTitleScreen() {
		titleBoxIndexedImage = new IndexedImage(titleArchive, "titlebox", 0);
		titleButtonIndexedImage = new IndexedImage(titleArchive, "titlebutton", 0);

		titleIndexedImages = new IndexedImage[12];
		int icon = 0;
		try {
			icon = Integer.parseInt(getParameter("fl_icon"));
		} catch (Exception ex) {

		}
		if (icon == 0) {
			for (int index = 0; index < 12; index++) {
				titleIndexedImages[index] = new IndexedImage(titleArchive, "runes", index);
			}

		} else {
			for (int index = 0; index < 12; index++) {
				titleIndexedImages[index] = new IndexedImage(titleArchive, "runes", 12 + (index & 3));
			}

		}
		flameLeftSprite = new Sprite(128, 265);
		flameRightSprite = new Sprite(128, 265);

		System.arraycopy(flameLeftBackground.canvasRaster, 0, flameLeftSprite.myPixels, 0, 33920);

		System.arraycopy(flameRightBackground.canvasRaster, 0, flameRightSprite.myPixels, 0, 33920);

		anIntArray851 = new int[256];

		for (int k1 = 0; k1 < 64; k1++)
			anIntArray851[k1] = k1 * 0x40000;

		for (int l1 = 0; l1 < 64; l1++)
			anIntArray851[l1 + 64] = 0xff0000 + 1024 * l1;

		for (int i2 = 0; i2 < 64; i2++)
			anIntArray851[i2 + 128] = 0xffff00 + 4 * i2;

		for (int j2 = 0; j2 < 64; j2++)
			anIntArray851[j2 + 192] = 0xffffff;

		anIntArray852 = new int[256];
		for (int k2 = 0; k2 < 64; k2++)
			anIntArray852[k2] = k2 * 1024;

		for (int l2 = 0; l2 < 64; l2++)
			anIntArray852[l2 + 64] = 65280 + 4 * l2;

		for (int i3 = 0; i3 < 64; i3++)
			anIntArray852[i3 + 128] = 65535 + 0x40000 * i3;

		for (int j3 = 0; j3 < 64; j3++)
			anIntArray852[j3 + 192] = 0xffffff;

		anIntArray853 = new int[256];
		for (int k3 = 0; k3 < 64; k3++)
			anIntArray853[k3] = k3 * 4;

		for (int l3 = 0; l3 < 64; l3++)
			anIntArray853[l3 + 64] = 255 + 0x40000 * l3;

		for (int i4 = 0; i4 < 64; i4++)
			anIntArray853[i4 + 128] = 0xff00ff + 1024 * i4;

		for (int j4 = 0; j4 < 64; j4++)
			anIntArray853[j4 + 192] = 0xffffff;

		anIntArray850 = new int[256];
		anIntArray1190 = new int[32768];
		anIntArray1191 = new int[32768];
		randomizeBackground(null);
		anIntArray828 = new int[32768];
		anIntArray829 = new int[32768];
		drawLoadingText(10, "Connecting to fileserver");
		if (!aBoolean831) {
			drawFlames = true;
			aBoolean831 = true;
			startRunnable(this, 2);
		}
	}



	public boolean hover(int x1, int y1, Sprite drawnSprite) {
		return super.mouseX >= x1 && super.mouseX <= x1 + drawnSprite.myWidth && super.mouseY >= y1 && super.mouseY <= y1 + drawnSprite.myHeight;
	}

	private static void setHighMem() {
		SceneGraph.lowMem = false;
		Rasterizer3D.lowMem = false;
		lowMemory = false;
		MapRegion.lowMem = false;
		ObjectDefinition.lowMemory = false;
	}

	public static void main(String args[]) {
		try {
			nodeID = 10;
			portOffset = 0;
			setHighMem();
			isMembers = true;
			SignLink.storeid = 32;
			SignLink.startpriv(InetAddress.getLocalHost());
			frameMode(ScreenMode.FIXED);
			instance = new Client();
			instance.createClientFrame(frameWidth, frameHeight);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Client instance;

	private void loadingStages() {
		if (lowMemory && loadingStage == 2 && MapRegion.anInt131 != plane) {
			gameScreenImageProducer.initDrawingArea();
			drawLoadingMessages(1, "Loading - please wait.", null);
			gameScreenImageProducer.drawGraphics(frameMode == ScreenMode.FIXED ? 4 : 0,
					super.graphics, frameMode == ScreenMode.FIXED ? 4 : 0);
			loadingStage = 1;
			loadingStartTime = System.currentTimeMillis();
		}
		if (loadingStage == 1) {
			int j = getMapLoadingState();
			if (j != 0 && System.currentTimeMillis() - loadingStartTime > 0x57e40L) {
				SignLink.reporterror(myUsername + " glcfb " + serverSeed + "," + j + ","
						+ lowMemory + "," + indices[0] + ","
						+ resourceProvider.remaining() + "," + plane + "," + this.regionX
						+ "," + this.regionY);
				loadingStartTime = System.currentTimeMillis();
			}
		}
		if (loadingStage == 2 && plane != lastKnownPlane) {
			lastKnownPlane = plane;
			renderMapScene(plane);
		}
	}

	private int getMapLoadingState() {
		if (!floorMaps.equals("") || !objectMaps.equals("")) {
			floorMaps = "";
			objectMaps = "";
		}

		for (int i = 0; i < localRegionMapData.length; i++) {
			floorMaps += "  " + localRegionMapIds[i];
			objectMaps += "  " + localRegionLandscapeIds[i];
			if (localRegionMapData[i] == null && localRegionMapIds[i] != -1)
				return -1;
			if (localRegionLandscapeData[i] == null && localRegionLandscapeIds[i] != -1)
				return -2;
		}
		boolean flag = true;
		for (int j = 0; j < localRegionMapData.length; j++) {
			byte abyte0[] = localRegionLandscapeData[j];
			if (abyte0 != null) {
				int k = (localRegionIds[j] >> 8) * 64 - regionBaseX;
				int l = (localRegionIds[j] & 0xff) * 64 - regionBaseY;
				if (constructedViewport) {
					k = 10;
					l = 10;
				}
				flag &= MapRegion.method189(k, abyte0, l);
			}
		}
		if (!flag)
			return -3;
		if (validLocalMap) {
			return -4;
		} else {
			loadingStage = 2;
			MapRegion.anInt131 = plane;
			updateWorldObjects();
			sendPacket(new FinalizedRegionChange());
			return 0;
		}
	}

	private void createProjectiles() {
		for (Projectile class30_sub2_sub4_sub4 = (Projectile) projectiles
				.reverseGetFirst(); class30_sub2_sub4_sub4 != null; class30_sub2_sub4_sub4 =
				(Projectile) projectiles.reverseGetNext())
			if (class30_sub2_sub4_sub4.projectileZ != plane
			|| tick > class30_sub2_sub4_sub4.stopCycle)
				class30_sub2_sub4_sub4.unlink();
			else if (tick >= class30_sub2_sub4_sub4.startCycle) {
				if (class30_sub2_sub4_sub4.target > 0) {
					Npc npc = npcs[class30_sub2_sub4_sub4.target - 1];
					if (npc != null && npc.x >= 0 && npc.x < 13312 && npc.y >= 0
							&& npc.y < 13312)
						class30_sub2_sub4_sub4.calculateIncrements(tick, npc.y,
								getCenterHeight(class30_sub2_sub4_sub4.projectileZ, npc.y,
										npc.x)
								- class30_sub2_sub4_sub4.endHeight,
								npc.x);
				}
				if (class30_sub2_sub4_sub4.target < 0) {
					int j = -class30_sub2_sub4_sub4.target - 1;
					Player player;
					if (j == localPlayerIndex)
						player = localPlayer;
					else
						player = players[j];
					if (player != null && player.x >= 0 && player.x < 13312
							&& player.y >= 0 && player.y < 13312)
						class30_sub2_sub4_sub4.calculateIncrements(tick, player.y,
								getCenterHeight(class30_sub2_sub4_sub4.projectileZ, player.y,
										player.x)
								- class30_sub2_sub4_sub4.endHeight,
								player.x);
				}
				class30_sub2_sub4_sub4.progressCycles(tickDelta);
				scene.addAnimableA(plane, class30_sub2_sub4_sub4.turnValue,
						(int) class30_sub2_sub4_sub4.cnterHeight, -1,
						(int) class30_sub2_sub4_sub4.yPos, 60,
						(int) class30_sub2_sub4_sub4.xPos,
						class30_sub2_sub4_sub4, false);
			}

	}

	public AppletContext getAppletContext() {
		if (SignLink.mainapp != null)
			return SignLink.mainapp.getAppletContext();
		else
			return super.getAppletContext();
	}

	private void drawLogo() {
			byte sprites[] = titleArchive.readFile("title.dat");
		Sprite sprite = new Sprite(sprites, this);
		flameLeftBackground.initDrawingArea();
		sprite.method346(0, 0);
		flameRightBackground.initDrawingArea();
		sprite.method346(-637, 0);
		topLeft1BackgroundTile.initDrawingArea();
		sprite.method346(-128, 0);
		bottomLeft1BackgroundTile.initDrawingArea();
		sprite.method346(-202, -371);
		loginBoxImageProducer.initDrawingArea();
		sprite.method346(-202, -171);
		loginScreenAccessories.initDrawingArea();
		sprite.method346(0, -400);
		bottomLeft0BackgroundTile.initDrawingArea();
		sprite.method346(0, -265);
		bottomRightImageProducer.initDrawingArea();
		sprite.method346(-562, -265);
		loginMusicImageProducer.initDrawingArea();
		sprite.method346(-562, -265);
		middleLeft1BackgroundTile.initDrawingArea();
		sprite.method346(-128, -171);
		aRSImageProducer_1115.initDrawingArea();
		sprite.method346(-562, -171);
		int ai[] = new int[sprite.myWidth];
		for (int j = 0; j < sprite.myHeight; j++) {
			for (int k = 0; k < sprite.myWidth; k++)
				ai[k] = sprite.myPixels[(sprite.myWidth - k - 1) + sprite.myWidth * j];

			System.arraycopy(ai, 0, sprite.myPixels, sprite.myWidth * j, sprite.myWidth);
		}
		flameLeftBackground.initDrawingArea();
		sprite.method346(382, 0);
		flameRightBackground.initDrawingArea();
		sprite.method346(-255, 0);
		topLeft1BackgroundTile.initDrawingArea();
		sprite.method346(254, 0);
		bottomLeft1BackgroundTile.initDrawingArea();
		sprite.method346(180, -371);
		loginBoxImageProducer.initDrawingArea();
		sprite.method346(180, -171);
		bottomLeft0BackgroundTile.initDrawingArea();
		sprite.method346(382, -265);
		bottomRightImageProducer.initDrawingArea();
		sprite.method346(-180, -265);
		loginMusicImageProducer.initDrawingArea();
		sprite.method346(-180, -265);
		middleLeft1BackgroundTile.initDrawingArea();
		sprite.method346(254, -171);
		aRSImageProducer_1115.initDrawingArea();
		sprite.method346(-180, -171);
		sprite = new Sprite(titleArchive, "logo", 0);
		topLeft1BackgroundTile.initDrawingArea();
		sprite.drawSprite(382 - sprite.myWidth / 2 - 128, 18);
		sprite = null;
		System.gc();
	}

	private void processOnDemandQueue() {
		do {
			Resource resource;
			do {
				resource = resourceProvider.next();
				if (resource == null)
					return;
				if (resource.dataType == 0) {
					Model.method460(resource.buffer, resource.ID);
					if (backDialogueId != -1)
						updateChatbox = true;
				}
				if (resource.dataType == 1) {
					Frame.load(resource.ID, resource.buffer);
				}
				if (resource.dataType == 2 && resource.ID == nextSong
						&& resource.buffer != null)
					saveMidi(fadeMusic, resource.buffer);
				if (resource.dataType == 3 && loadingStage == 1) {
					for (int i = 0; i < localRegionMapData.length; i++) {
						if (localRegionMapIds[i] == resource.ID) {
							localRegionMapData[i] = resource.buffer;
							if (resource.buffer == null)
								localRegionMapIds[i] = -1;
							break;
						}
						if (localRegionLandscapeIds[i] != resource.ID)
							continue;
						localRegionLandscapeData[i] = resource.buffer;
						if (resource.buffer == null)
							localRegionLandscapeIds[i] = -1;
						break;
					}

				}
			} while (resource.dataType != 93
					|| !resourceProvider.landscapePresent(resource.ID));
			MapRegion.passiveRequestGameObjectModels(new Buffer(resource.buffer), resourceProvider);
		} while (true);
	}

	private void calcFlamesPosition() {
		char c = '\u0100';
		for (int j = 10; j < 117; j++) {
			int k = (int) (Math.random() * 100D);
			if (k < 50)
				anIntArray828[j + (c - 2 << 7)] = 255;
		}
		for (int l = 0; l < 100; l++) {
			int i1 = (int) (Math.random() * 124D) + 2;
			int k1 = (int) (Math.random() * 128D) + 128;
			int k2 = i1 + (k1 << 7);
			anIntArray828[k2] = 192;
		}

		for (int j1 = 1; j1 < c - 1; j1++) {
			for (int l1 = 1; l1 < 127; l1++) {
				int l2 = l1 + (j1 << 7);
				anIntArray829[l2] = (anIntArray828[l2 - 1] + anIntArray828[l2 + 1]
						+ anIntArray828[l2 - 128] + anIntArray828[l2 + 128]) / 4;
			}

		}

		anInt1275 += 128;
		if (anInt1275 > anIntArray1190.length) {
			anInt1275 -= anIntArray1190.length;
			int i2 = (int) (Math.random() * 12D);
			randomizeBackground(titleIndexedImages[i2]);
		}
		for (int j2 = 1; j2 < c - 1; j2++) {
			for (int i3 = 1; i3 < 127; i3++) {
				int k3 = i3 + (j2 << 7);
				int i4 = anIntArray829[k3 + 128]
						- anIntArray1190[k3 + anInt1275 & anIntArray1190.length - 1]
								/ 5;
				if (i4 < 0)
					i4 = 0;
				anIntArray828[k3] = i4;
			}

		}

		System.arraycopy(anIntArray969, 1, anIntArray969, 0, c - 1);

		anIntArray969[c - 1] = (int) (Math.sin((double) tick / 14D) * 16D
				+ Math.sin((double) tick / 15D) * 14D
				+ Math.sin((double) tick / 16D) * 12D);
		if (anInt1040 > 0)
			anInt1040 -= 4;
		if (anInt1041 > 0)
			anInt1041 -= 4;
		if (anInt1040 == 0 && anInt1041 == 0) {
			int l3 = (int) (Math.random() * 2000D);
			if (l3 == 0)
				anInt1040 = 1024;
			if (l3 == 1)
				anInt1041 = 1024;
		}
	}

	private void resetAnimation(int i) {
		Widget class9 = Widget.interfaceCache[i];
		if(class9 == null || class9.children == null) {
			return;
		}
		for (int j = 0; j < class9.children.length; j++) {
			if (class9.children[j] == -1)
				break;
			Widget class9_1 = Widget.interfaceCache[class9.children[j]];
			if (class9_1.type == 1)
				resetAnimation(class9_1.id);
			class9_1.currentFrame = 0;
			class9_1.lastFrameTime = 0;
		}
	}

	private void drawHeadIcon() {
		if (hintIconDrawType != 2)
			return;
		calcEntityScreenPos((hintIconX - regionBaseX << 7) + hintIconLocationArrowRelX, hintIconLocationArrowHeight * 2,
				(hintIconY - regionBaseY << 7) + hintIconLocationArrowRelY);
		if (spriteDrawX > -1 && tick % 20 < 10) {
			headIconsHint[0].drawSprite(spriteDrawX - 12, spriteDrawY - 28);
		}
	}

	private void mainGameProcessor() {
		refreshFrameSize();
		if (systemUpdateTime > 1) {
			systemUpdateTime--;
		}
		if (anInt1011 > 0) {
			anInt1011--;
		}
		for (int j = 0; j < 5; j++) {
			if (!readPacket()) {
				break;
			}
		}

		if (!loggedIn) {
			return;
		}

		synchronized (mouseDetection.syncObject) {
			if (flagged) {
				if (super.clickMode3 != 0 || mouseDetection.coordsIndex >= 40) {
					// botting
					/* outgoing.writeOpcode(PacketConstants.FLAG_ACCOUNT);
                              outgoing.writeByte(0);
                              int j2 = outgoing.currentPosition;
                              int j3 = 0;
                              for (int j4 = 0; j4 < mouseDetection.coordsIndex; j4++) {
                                    if (j2 - outgoing.currentPosition >= 240)
                                          break;
                                    j3++;
                                    int l4 = mouseDetection.coordsY[j4];
                                    if (l4 < 0)
                                          l4 = 0;
                                    else if (l4 > 502)
                                          l4 = 502;
                                    int k5 = mouseDetection.coordsX[j4];
                                    if (k5 < 0)
                                          k5 = 0;
                                    else if (k5 > 764)
                                          k5 = 764;
                                    int i6 = l4 * 765 + k5;
                                    if (mouseDetection.coordsY[j4] == -1
                                                && mouseDetection.coordsX[j4] == -1) {
                                          k5 = -1;
                                          l4 = -1;
                                          i6 = 0x7ffff;
                                    }
                                    if (k5 == anInt1237 && l4 == anInt1238) {
                                          if (duplicateClickCount < 2047)
                                                duplicateClickCount++;
                                    } else {
                                          int j6 = k5 - anInt1237;
                                          anInt1237 = k5;
                                          int k6 = l4 - anInt1238;
                                          anInt1238 = l4;
                                          if (duplicateClickCount < 8 && j6 >= -32 && j6 <= 31
                                                      && k6 >= -32 && k6 <= 31) {
                                                j6 += 32;
                                                k6 += 32;
                                                outgoing.writeShort((duplicateClickCount << 12)
                                                            + (j6 << 6) + k6);
                                                duplicateClickCount = 0;
                                          } else if (duplicateClickCount < 8) {
                                                outgoing.writeTriByte(0x800000
                                                            + (duplicateClickCount << 19) + i6);
                                                duplicateClickCount = 0;
                                          } else {
                                                outgoing.writeInt(0xc0000000
                                                            + (duplicateClickCount << 19) + i6);
                                                duplicateClickCount = 0;
                                          }
                                    }
                              }

                              outgoing.writeBytes(outgoing.currentPosition - j2);
                              if (j3 >= mouseDetection.coordsIndex) {
                                    mouseDetection.coordsIndex = 0;
                              } else {
                                    mouseDetection.coordsIndex -= j3;
                                    for (int i5 = 0; i5 < mouseDetection.coordsIndex; i5++) {
                                          mouseDetection.coordsX[i5] =
                                                      mouseDetection.coordsX[i5 + j3];
                                          mouseDetection.coordsY[i5] =
                                                      mouseDetection.coordsY[i5 + j3];
                                    }

                              }*/
				}
			} else {
				mouseDetection.coordsIndex = 0;
			}
		}
		if (super.clickMode3 != 0) {
			long l = (super.aLong29 - aLong1220) / 50L;
			if (l > 4095L)
				l = 4095L;
			aLong1220 = super.aLong29;
			int k2 = super.saveClickY;
			if (k2 < 0)
				k2 = 0;
			else if (k2 > 502)
				k2 = 502;
			int k3 = super.saveClickX;
			if (k3 < 0)
				k3 = 0;
			else if (k3 > 764)
				k3 = 764;
			int k4 = k2 * 765 + k3;
			int j5 = 0;
			if (super.clickMode3 == 2)
				j5 = 1;
			int l5 = (int) l;
			/* outgoing.writeOpcode(PacketConstants.MOUSE_CLICK);
                  outgoing.writeInt((l5 << 20) + (j5 << 19) + k4);*/
		}

		if (anInt1016 > 0) {
			anInt1016--;
		}

		if (super.keyArray[1] == 1 || super.keyArray[2] == 1 || super.keyArray[3] == 1
				|| super.keyArray[4] == 1)
			aBoolean1017 = true;
		if (aBoolean1017 && anInt1016 <= 0) {
			anInt1016 = 20;
			aBoolean1017 = false;
			/* outgoing.writeOpcode(PacketConstants.CAMERA_MOVEMENT);
                  outgoing.writeShort(anInt1184);
                  outgoing.writeShortA(cameraHorizontal);*/
		}
		if (super.awtFocus && !aBoolean954) {
			aBoolean954 = true;
			//  sendPacket(new ClientFocused(false));
		}
		if (!super.awtFocus && aBoolean954) {
			aBoolean954 = false;
			//   sendPacket(new ClientFocused(false));
		}
		loadingStages();
		method115();
		timeoutCounter++;
		if (timeoutCounter > 150)
			dropClient();
		processPlayerMovement();            
		processNpcMovement();            
		processTrackUpdates();
		processMobChatText();            
		
		tickDelta++;            
		if (crossType != 0) {
			crossIndex += 20;
			if (crossIndex >= 400)
				crossType = 0;
		}
		if (atInventoryInterfaceType != 0) {
			atInventoryLoopCycle++;
			if (atInventoryLoopCycle >= 15) {
				if (atInventoryInterfaceType == 2) {
				}
				if (atInventoryInterfaceType == 3)
					updateChatbox = true;
				atInventoryInterfaceType = 0;
			}
		}
		if (activeInterfaceType != 0) {
			anInt989++;
			if (super.mouseX > anInt1087 + 5 || super.mouseX < anInt1087 - 5
					|| super.mouseY > anInt1088 + 5 || super.mouseY < anInt1088 - 5)
				aBoolean1242 = true;
			if (super.clickMode2 == 0) {
				if (activeInterfaceType == 2) {
				}
				if (activeInterfaceType == 3)
					updateChatbox = true;
				activeInterfaceType = 0;
				if (aBoolean1242 && anInt989 >= 15) {
					bankItemDragSprite = null;
					lastActiveInvInterface = -1;
					processRightClick();
					if(!createBankTab()) {
						if (lastActiveInvInterface == anInt1084
								&& mouseInvInterfaceIndex != anInt1085) {
							Widget childInterface = Widget.interfaceCache[anInt1084];
							int j1 = 0;
							if (anInt913 == 1 && childInterface.contentType == 206)
								j1 = 1;
							if (childInterface.inventoryItemId[mouseInvInterfaceIndex] <= 0)
								j1 = 0;
							if (childInterface.replaceItems) {
								int l2 = anInt1085;
								int l3 = mouseInvInterfaceIndex;
								childInterface.inventoryItemId[l3] =
										childInterface.inventoryItemId[l2];
								childInterface.inventoryAmounts[l3] =
										childInterface.inventoryAmounts[l2];
								childInterface.inventoryItemId[l2] = -1;
								childInterface.inventoryAmounts[l2] = 0;
							} else if (j1 == 1) {
								int i3 = anInt1085;
								for (int i4 = mouseInvInterfaceIndex; i3 != i4;)
									if (i3 > i4) {
										childInterface.swapInventoryItems(i3, i3 - 1);
										i3--;
									} else if (i3 < i4) {
										childInterface.swapInventoryItems(i3, i3 + 1);
										i3++;
									}

							} else {
								childInterface.swapInventoryItems(anInt1085,
										mouseInvInterfaceIndex);
							}

							sendPacket(new SwitchItemSlot(anInt1084, j1, anInt1085, mouseInvInterfaceIndex));
						}		
					}
				} else if ((anInt1253 == 1 || menuHasAddFriend(menuActionRow - 1))
						&& menuActionRow > 2)
					determineMenuSize();
				else if (menuActionRow > 0)
					processMenuActions(menuActionRow - 1);
				atInventoryLoopCycle = 10;
				super.clickMode3 = 0;
			}
		}
		if (SceneGraph.clickedTileX != -1) {
			int k = SceneGraph.clickedTileX;
			int k1 = SceneGraph.clickedTileY;
			boolean flag = doWalkTo(0, 0, 0, 0, localPlayer.pathY[0], 0, 0, k1,
					localPlayer.pathX[0], true, k);
			SceneGraph.clickedTileX = -1;
			if (flag) {
				crossX = super.saveClickX;
				crossY = super.saveClickY;
				crossType = 1;
				crossIndex = 0;
			}
		}
		if (super.clickMode3 == 1 && clickToContinueString != null) {
			clickToContinueString = null;
			updateChatbox = true;
			super.clickMode3 = 0;
		}
		processMenuClick();
		if (super.clickMode2 == 1 || super.clickMode3 == 1)
			anInt1213++;
		if (anInt1500 != 0 || anInt1044 != 0 || anInt1129 != 0) {
			if (anInt1501 < 0 && !menuOpen) {
				anInt1501++;
				if (anInt1501 == 0) {
					if (anInt1500 != 0) {
						updateChatbox = true;
					}
					if (anInt1044 != 0) {
					}
				}
			}
		} else if (anInt1501 > 0) {
			anInt1501--;
		}
		if (loadingStage == 2)
			checkForGameUsages();
		if (loadingStage == 2 && oriented)
			calculateCameraPosition();
		for (int i1 = 0; i1 < 5; i1++)
			quakeTimes[i1]++;

		manageTextInputs();

		if (super.idleTime++ > 9000) {
			anInt1011 = 250;
			super.idleTime = 0;
			sendPacket(new PlayerInactive());
		}

		if (ping_packet_counter++ > 50) {
			sendPacket(new BasicPing());
		}
	}

	private void processSkillTab() {
		for(int i = 10061, j = 10091, id = 0; id <= 22; i++, j++, id++) {
			Widget.interfaceCache[i].defaultText = Integer.toString(maximumLevels[skillIdsOrder[id]]);
			Widget.interfaceCache[j].defaultText = Integer.toString(maximumLevels[skillIdsOrder[id]]);
			
			
		}
		totalLevel = IntStream.of(maximumLevels).sum();
		Widget.interfaceCache[10121].defaultText = Integer.toString(totalLevel);
	}

	private void method63() {
		SpawnedObject spawnedObject = (SpawnedObject) spawns.reverseGetFirst();
		for (; spawnedObject != null; spawnedObject = (SpawnedObject) spawns.reverseGetNext())
			if (spawnedObject.getLongetivity == -1) {
				spawnedObject.delay = 0;
				method89(spawnedObject);
			} else {
				spawnedObject.unlink();
			}

	}

	private void setupLoginScreen() {
		if (topLeft1BackgroundTile != null)
			return;
		super.fullGameScreen = null;
		chatboxImageProducer = null;
		minimapImageProducer = null;
		tabImageProducer = null;
		gameScreenImageProducer = null;
		chatSettingImageProducer = null;
		WorldSelector = new ProducingGraphicsBuffer(frameWidth, frameHeight);
		Rasterizer2D.clear();
		titleScreen = new ProducingGraphicsBuffer(frameWidth, frameHeight);
		Rasterizer2D.clear();
		flameLeftBackground = new ProducingGraphicsBuffer(128, 265);
		Rasterizer2D.clear();
		flameRightBackground = new ProducingGraphicsBuffer(128, 265);
		Rasterizer2D.clear();
		topLeft1BackgroundTile = new ProducingGraphicsBuffer(509, 171);
		Rasterizer2D.clear();
		bottomLeft1BackgroundTile = new ProducingGraphicsBuffer(360, 132);
		Rasterizer2D.clear();
		loginBoxImageProducer = new ProducingGraphicsBuffer(360, 200);
		Rasterizer2D.clear();
		loginScreenAccessories = new ProducingGraphicsBuffer(300, 800);
		Rasterizer2D.clear();
		bottomLeft0BackgroundTile = new ProducingGraphicsBuffer(202, 238);
		Rasterizer2D.clear();
		bottomRightImageProducer = new ProducingGraphicsBuffer(203, 238);
		Rasterizer2D.clear();
		loginMusicImageProducer = new ProducingGraphicsBuffer(203, 238);
		Rasterizer2D.clear();
		middleLeft1BackgroundTile = new ProducingGraphicsBuffer(74, 94);
		Rasterizer2D.clear();
		aRSImageProducer_1115 = new ProducingGraphicsBuffer(75, 94);
		Rasterizer2D.clear();
		Rasterizer2D.clear();
		if (titleArchive != null) {
			drawLogo();
			loadTitleScreen();
		}
		welcomeScreenRaised = true;
	}
	
	public void drawLoadingText(int i, String s) {
		anInt1079 = i;
		aString1049 = s;
		setupLoginScreen();
		if (titleArchive == null) {
			super.drawLoadingText(i, s);
			return;
		}
		loginBoxImageProducer.initDrawingArea();
		char c = '\u0168';
		char c1 = '\310';
		byte byte1 = 20;
		boldText.drawText(0xffffff, Configuration.CLIENT_NAME + " is loading - please wait...",
				c1 / 2 - 26 - byte1, c / 2);
		int j = c1 / 2 - 18 - byte1;
		Rasterizer2D.drawBoxOutline(c / 2 - 152, j, 304, 34, 0x8c1111);
		Rasterizer2D.drawBoxOutline(c / 2 - 151, j + 1, 302, 32, 0);
		Rasterizer2D.drawBox(c / 2 - 150, j + 2, i * 3, 30, 0x8c1111);
		Rasterizer2D.drawBox((c / 2 - 150) + i * 3, j + 2, 300 - i * 3, 30, 0);
		boldText.drawText(0xffffff, s, (c1 / 2 + 5) - byte1, c / 2);
		loginBoxImageProducer.drawGraphics(171, super.graphics, 202);
		if (welcomeScreenRaised) {
			welcomeScreenRaised = false;
			if (!aBoolean831) {
				flameLeftBackground.drawGraphics(0, super.graphics, 0);
				flameRightBackground.drawGraphics(0, super.graphics, 637);
			}
			topLeft1BackgroundTile.drawGraphics(0, super.graphics, 128);
			bottomLeft1BackgroundTile.drawGraphics(371, super.graphics, 202);
			bottomLeft0BackgroundTile.drawGraphics(265, super.graphics, 0);
			bottomRightImageProducer.drawGraphics(265, super.graphics, 562);
			loginMusicImageProducer.drawGraphics(265, super.graphics, 562);
			middleLeft1BackgroundTile.drawGraphics(171, super.graphics, 128);
			aRSImageProducer_1115.drawGraphics(171, super.graphics, 562);
		}
	}

	private void method65(int i, int j, int k, int l, Widget class9, int i1, boolean flag,
			int j1) {
		int anInt992;
		if (aBoolean972)
			anInt992 = 32;
		else
			anInt992 = 0;
		aBoolean972 = false;
		if (k >= i && k < i + 16 && l >= i1 && l < i1 + 16) {
			class9.scrollPosition -= anInt1213 * 4;
			if (flag) {
			}
		} else if (k >= i && k < i + 16 && l >= (i1 + j) - 16 && l < i1 + j) {
			class9.scrollPosition += anInt1213 * 4;
			if (flag) {
			}
		} else if (k >= i - anInt992 && k < i + 16 + anInt992 && l >= i1 + 16
				&& l < (i1 + j) - 16 && anInt1213 > 0) {
			int l1 = ((j - 32) * j) / j1;
			if (l1 < 8)
				l1 = 8;
			int i2 = l - i1 - 16 - l1 / 2;
			int j2 = j - 32 - l1;
			class9.scrollPosition = ((j1 - j) * i2) / j2;
			if (flag) {
			}
			aBoolean972 = true;
		}
	}

	private boolean clickObject(int i, int j, int k) {
		int i1 = i >> 14 & 0x7fff;
			int j1 = scene.getMask(plane, k, j, i);
			if (j1 == -1)
				return false;
			int k1 = j1 & 0x1f;
			int l1 = j1 >> 6 & 3;
			if (k1 == 10 || k1 == 11 || k1 == 22) {
				ObjectDefinition class46 = ObjectDefinition.lookup(i1);
				int i2;
				int j2;
				if (l1 == 0 || l1 == 2) {
					i2 = class46.objectSizeX;
					j2 = class46.objectSizeY;
				} else {
					i2 = class46.objectSizeY;
					j2 = class46.objectSizeX;
				}
				int k2 = class46.surroundings;
				if (l1 != 0)
					k2 = (k2 << l1 & 0xf) + (k2 >> 4 - l1);
				doWalkTo(2, 0, j2, 0, localPlayer.pathY[0], i2, k2, j, localPlayer.pathX[0],
						false, k);
			} else {
				doWalkTo(2, l1, 0, k1 + 1, localPlayer.pathY[0], 0, 0, j, localPlayer.pathX[0],
						false, k);
			}
			crossX = super.saveClickX;
			crossY = super.saveClickY;
			crossType = 2;
			crossIndex = 0;
			return true;
	}

	public void playSong(int id) {
		if (id != currentSong && Configuration.enableMusic && !lowMemory && prevSong == 0) {
			nextSong = id;
			fadeMusic = true;
			resourceProvider.provide(2, nextSong);
			currentSong = id;
		}
	}

	public void stopMidi() {
		if (SignLink.music != null) {
			SignLink.music.stop();
		}
		SignLink.fadeMidi = 0;
		SignLink.midi = "stop";
	}

	private void adjustVolume(boolean updateMidi, int volume) {
		SignLink.setVolume(volume);
		if (updateMidi) {
			SignLink.midi = "voladjust";
		}
	}

	private int currentTrackTime;
	private long trackTimer;
	private boolean sendingAutochat = false;

	private boolean saveWave(byte data[], int id) {
		return data == null || SignLink.wavesave(data, id);
	}

	@SuppressWarnings("unused")
	private int currentTrackLoop;

	private void processTrackUpdates() {
		for (int count = 0; count < trackCount; count++) {
			boolean replay = false;
			try {
				Buffer stream = Track.data(trackLoops[count], tracks[count]);
				new SoundPlayer(
						(InputStream) new ByteArrayInputStream(stream.payload, 0,
								stream.currentPosition),
						soundVolume[count], soundDelay[count]);
				if (System.currentTimeMillis()
						+ (long) (stream.currentPosition / 22) > trackTimer
						+ (long) (currentTrackTime / 22)) {
					currentTrackTime = stream.currentPosition;
					trackTimer = System.currentTimeMillis();
					if (saveWave(stream.payload, stream.currentPosition)) {
						currentTrackPlaying = tracks[count];
						currentTrackLoop = trackLoops[count];
					} else {
						replay = true;
					}
				}
			} catch (Exception exception) {
			}
			if (!replay || soundDelay[count] == -5) {
				trackCount--;
				for (int index = count; index < trackCount; index++) {
					tracks[index] = tracks[index + 1];
					trackLoops[index] = trackLoops[index + 1];
					soundDelay[index] = soundDelay[index + 1];
					soundVolume[index] = soundVolume[index + 1];
				}
				count--;
			} else {
				soundDelay[count] = -5;
			}
		}

		if (prevSong > 0) {
			prevSong -= 20;
			if (prevSong < 0)
				prevSong = 0;
			if (prevSong == 0 && Configuration.enableMusic && !lowMemory) {
				nextSong = currentSong;
				fadeMusic = true;
				resourceProvider.provide(2, nextSong);
			}
		}
	}


	private FileArchive createArchive(int file, String displayedName, String name,
			int expectedCRC, int x) {
		byte buffer[] = null;

		try {
			if (indices[0] != null)
				buffer = indices[0].decompress(file);
		} catch (Exception _ex) {
		}

		//Compare crc...
		if(buffer != null) {
			if(Configuration.JAGCACHED_ENABLED) {
				if (!JagGrab.compareCrc(buffer, expectedCRC)) {
					buffer = null;
				}
			}
		}

		if (buffer != null) {
			FileArchive streamLoader = new FileArchive(buffer);
			return streamLoader;
		}

		//Retry to redl cache cause it's obvious corrupt or something
		if(buffer == null && !Configuration.JAGCACHED_ENABLED) {
			CacheDownloader.init(true);
			return createArchive(file, displayedName, name, expectedCRC, x);
		}

		while (buffer == null) {
			drawLoadingText(x, "Requesting " + displayedName);
			try(DataInputStream in = JagGrab.openJagGrabRequest(name)) {

				//Try to get the file..
				buffer = JagGrab.getBuffer(in);

				//Compare crc again...
				if(buffer != null) {
					if (!JagGrab.compareCrc(buffer, expectedCRC)) {
						buffer = null;
					}
				}

				//Write file
				if(buffer != null) {
					try {
						if (indices[0] != null)
							indices[0].writeFile(buffer.length, buffer, file);
					} catch (Exception _ex) {
						indices[0] = null;
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
				buffer = null;
			}

			if(buffer == null) {
				JagGrab.error("Archives");
			}
		}

		FileArchive streamLoader_1 = new FileArchive(buffer);
		return streamLoader_1;
	}

	private void dropClient() {
		if (anInt1011 > 0) {
			resetLogout();
			return;
		}
		Rasterizer2D.drawBoxOutline(2, 2, 229, 39, 0xffffff); // white box around
		Rasterizer2D.drawBox(3, 3, 227, 37, 0); // black fill
		regularText.drawText(0, "Connection lost.", 19, 120);
		regularText.drawText(0xffffff, "Connection lost.", 18, 119);
		regularText.drawText(0, "Please wait - attempting to reestablish.", 34, 117);
		regularText.drawText(0xffffff, "Please wait - attempting to reestablish.", 34, 116);
		gameScreenImageProducer.drawGraphics(frameMode == ScreenMode.FIXED ? 4 : 0,
				super.graphics, frameMode == ScreenMode.FIXED ? 4 : 0);
		minimapState = 0;
		destinationX = 0;
		BufferedConnection rsSocket = socketStream;
		loggedIn = false;
		loginFailures = 0;
		login(myUsername, myPassword, true);
		if (!loggedIn)
			resetLogout();
		try {
			rsSocket.close();
		} catch (Exception _ex) {
		}
	}

	public void setNorth() {
		cameraX = 0;
		cameraY = 0;
		cameraRotation = 0;
		cameraHorizontal = 0;
		minimapRotation = 0;
		minimapZoom = 0;
	}

	private boolean isSelectingQuickPrayers() {
		return tabInterfaceIDs[5] == 17200;
	}

	//TODO menu actions
	private void processMenuActions(int id) {
		if (id < 0) {
			return;
		}

		searchingSpawnTab = false;
		bankItemDragSprite = null;

		if (inputDialogState != 0) {
			inputDialogState = 0;
			updateChatbox = true;
		}

		int first = firstMenuAction[id];
		int button = secondMenuAction[id];            
		int action = menuActionTypes[id];
		int clicked = selectedMenuActions[id];
		

		if (action >= 2000) {
			action -= 2000;
		}

		if (action == 851) { //Spec orb
			sendPacket(new ClickButton(155));
			return;
		}
		
		if (action == 661) { //Spec orb
			int slot = 1;
			sendPacket(new OperateItem(slot, clicked));
			return;
		}
		if (action == 662) { //Spec orb
			int slot = 2;
			sendPacket(new OperateItem(slot, clicked));
			return;
		}
		if (action == 663) { //Spec orb
			int slot = 3;
			sendPacket(new OperateItem(slot, clicked));
			return;
		}
		if (action == 664) { //Spec orb
			int slot = 4;
			sendPacket(new OperateItem(slot, clicked));
			return;
		}
		
		if (action == 665) { //Spec orb
			int slot = 5;
			sendPacket(new OperateItem(slot, clicked));
			return;
		}

		// click logout tab
		if (action == 700) {
			if (tabInterfaceIDs[10] != -1) {
				if (tabId == 10) {
					showTabComponents = !showTabComponents;
				} else {
					showTabComponents = true;
				}
				tabId = 10;
				tabAreaAltered = true;
			}
		}

		// reset compass to north
		if (action == 696) {
			setNorth();
		}
		 if (action == 769) {
	            Widget d = Widget.interfaceCache[button];
	            Widget p = Widget.interfaceCache[localPlayerIndex];
	            if (!d.dropDown.isOpen()) {
	                if (p.dropDownOpen != null) {
	                    p.dropDownOpen.dropDown.setOpen(false);
	                }
	                p.dropDownOpen = d;
	            } else {
	                p.dropDownOpen = null;
	            }
	            d.dropDown.setOpen(!d.dropDown.isOpen());
	        }
	        if (action == 770) {
	        	Widget d = Widget.interfaceCache[button];
	        	Widget p = Widget.interfaceCache[localPlayerIndex];
	            if (first >= d.dropDown.getOptions().length)
	                return;

	            d.dropDown.setSelected(d.dropDown.getOptions()[first]);
	            d.dropDown.setOpen(false);
	            d.dropDown.getDrop().selectOption(first, this, d);
	            p.dropDownOpen = null;
	        }

		// custom
		if (action == 1506 && Configuration.enableOrbs) { // Select quick
			// prayers
			/*  outgoing.writeOpcode(185);
                  outgoing.writeShort(5001);*/
			sendPacket(new ClickButton(1506));
			return;
		}

		// custom
		if (action == 1500 && Configuration.enableOrbs) { // Toggle quick
			// prayers
			sendPacket(new ClickButton(1500));
			return;
		}

		// button clicks
		switch (action) {

		case 1315:
		case 1316:
		case 1317:
		case 1318:
		case 1319:
		case 1320:
		case 1321:
		case 879:
		case 850:
		case 475:
		case 476:
		case 1050:
			// button click
			sendPacket(new ClickButton(action));
			break;
		}

		// custom
		if (action == 1508 && Configuration.enableOrbs) { // Toggle HP above
			// heads
			Configuration.hpAboveHeads = !Configuration.hpAboveHeads;
		}

		if(action == 257) {
			expDrops = !expDrops;
			savePlayerData();
		} else if(action == 258) {
			skillOrbs = !skillOrbs;
			savePlayerData();
		}

		// click autocast
		if (action == 104) {
			Widget widget = Widget.interfaceCache[button];
			sendPacket(new ClickButton(widget.id));
			/*spellId = widget.id;
			if (!autocast) {
				autocast = true;
				autoCastId = widget.id;
				sendPacket(new ClickButton(widget.id));
			} else if (autoCastId == widget.id) {
				autocast = false;
				autoCastId = 0;
				sendPacket(new ClickButton(widget.id));
			} else if (autoCastId != widget.id) {
				autocast = true;
				autoCastId = widget.id;
				sendPacket(new ClickButton(widget.id));
			}*/
		}

		// item on npc
		if (action == 582) {
			Npc npc = npcs[clicked];
			if (npc != null) {
				doWalkTo(2, 0, 1, 0, localPlayer.pathY[0], 1, 0, npc.pathY[0],
						localPlayer.pathX[0], false, npc.pathX[0]);
				crossX = super.saveClickX;
				crossY = super.saveClickY;
				crossType = 2;
				crossIndex = 0;
				sendPacket(new ItemOnNpc(anInt1285, clicked, anInt1283, anInt1284));
			}
		}

		// picking up ground item
		if (action == 234) {
			boolean flag1 = doWalkTo(2, 0, 0, 0, localPlayer.pathY[0], 0, 0, button,
					localPlayer.pathX[0], false, first);
			if (!flag1)
				flag1 = doWalkTo(2, 0, 1, 0, localPlayer.pathY[0], 1, 0, button,
						localPlayer.pathX[0], false, first);
			crossX = super.saveClickX;
			crossY = super.saveClickY;
			crossType = 2;
			crossIndex = 0;
			// pickup ground item                  
			sendPacket(new PickupItem(button + regionBaseY, clicked, first + regionBaseX));
		}

		// using item on object
		if (action == 62 && clickObject(clicked, button, first)) {
			sendPacket(new ItemOnObject(anInt1284, clicked >> 14 & 0x7fff, button + regionBaseY, anInt1283, first + regionBaseX, anInt1285));
		}

		// using item on ground item
		if (action == 511) {
			boolean flag2 = doWalkTo(2, 0, 0, 0, localPlayer.pathY[0], 0, 0, button,
					localPlayer.pathX[0], false, first);
			if (!flag2)
				flag2 = doWalkTo(2, 0, 1, 0, localPlayer.pathY[0], 1, 0, button,
						localPlayer.pathX[0], false, first);
			crossX = super.saveClickX;
			crossY = super.saveClickY;
			crossType = 2;
			crossIndex = 0;
			// item on ground item
			sendPacket(new ItemOnGroundItem(anInt1284, anInt1285, clicked, button + regionBaseY, anInt1283, first + regionBaseX));
		}

		// item option 1
		if (action == 74) {     
			sendPacket(new UseItem(button, clicked, first));
			atInventoryLoopCycle = 0;
			atInventoryInterface = button;
			atInventoryIndex = first;
			atInventoryInterfaceType = 2;
			if (Widget.interfaceCache[button].parent == openInterfaceId) {
				atInventoryInterfaceType = 1;
			}
			if (Widget.interfaceCache[button].parent == backDialogueId) {
				atInventoryInterfaceType = 3;
			}
		}

		// widget action
		if (action == 315) {
			Widget widget = Widget.interfaceCache[button];
			boolean flag8 = true;
			if (widget.contentType > 0)
				flag8 = promptUserForInput(widget);
			if (flag8) {

				switch (button) {

				case 19144:
					inventoryOverlay(15106, 3213);
					resetAnimation(15106);
					updateChatbox = true;
					break;

				case 42501:
					frameMode(ScreenMode.FIXED);
					break;

				case 42504:
					frameMode(ScreenMode.RESIZABLE);
					break;

				case 40500:
				case 40501:
					searchingSpawnTab = true;
					break;
				case 23003:
					Configuration.alwaysLeftClickAttack = !Configuration.alwaysLeftClickAttack;
					savePlayerData();
					updateSettings();
					break;
				case 23005:
					Configuration.combatOverlayBox = !Configuration.combatOverlayBox;
					savePlayerData();
					updateSettings();
					break;
				case 23007:
					Configuration.hitmarks554 = !Configuration.hitmarks554;
					savePlayerData();
					updateSettings();
					break;
				case 23009:
					Configuration.hpBar554 = !Configuration.hpBar554;
					savePlayerData();
					updateSettings();
					break;
				case 23011:
					Configuration.enableTweening = !Configuration.enableTweening;
					savePlayerData();
					updateSettings();
					break;
				case 23013:
					Configuration.enableRoofs = !Configuration.enableRoofs;
					savePlayerData();
					updateSettings();
					break;
				case 23015:
					Configuration.enableFog = !Configuration.enableFog;
					savePlayerData();
					updateSettings();
					break;
				case 23024:
					Configuration.bountyHunterInterface = !Configuration.bountyHunterInterface;
					savePlayerData();
					updateSettings();
					sendPacket(new ClickButton(button));
					break;
				case 53007:
					Configuration.escapeCloseInterface = !Configuration.escapeCloseInterface;
					savePlayerData();
					updateSettings();
					break;
				case 53002:
					TabBindings.restoreDefault();
					savePlayerData();
					break;
					
				/** Faster spec bars toggle **/
				case 29138:
				case 29038:
				case 29063:
				case 29113:
				case 29163:
				case 29188:
				case 29213:
				case 29238:
				case 30007:
				case 48023:
				case 33033:
				case 30108:
				case 7473:
				case 7562:
				case 7487:
				case 7788:
				case 8481:
				case 7612:
				case 7587:
				case 7662:
				case 7462:
				case 7548:
				case 7687:
				case 7537:
				case 7623:
				case 12322:
				case 7637:
				case 12311:
				case 155:

					/** Just update the color of the bar before sending packet, to make it look smoother **/
					WeaponInterface wepInterface = WeaponInterface.get(tabInterfaceIDs[0]);
					if(wepInterface != null && wepInterface.getSpecialMeter() > 0) {
						boolean active = Widget.interfaceCache[wepInterface.getSpecialMeter()].defaultText.contains("@yel@");
						if(active) {
							Widget.interfaceCache[wepInterface.getSpecialMeter()].defaultText = 
									Widget.interfaceCache[wepInterface.getSpecialMeter()].defaultText.replaceAll("@yel@", "@bla@");
						} else {
							Widget.interfaceCache[wepInterface.getSpecialMeter()].defaultText = Widget.interfaceCache[wepInterface.getSpecialMeter()].defaultText.replaceAll("@bla@", "@yel@");
						}
					}

					sendPacket(new SpecialAttack(button));
					break;

				default:
					if(backDialogueId == 40500 && widget.geSearchButton) {
						sendPacket(new SendButtonAndId(button, searchResults[itemIdToSend]));
						backDialogueId = -1;
						searchSyntax = "";
					}
					sendPacket(new ClickButton(button));
					break;
				}
			}
		}

		// player option
		if (action == 561) {
			Player player = players[clicked];
			if (player != null) {
				doWalkTo(2, 0, 1, 0, localPlayer.pathY[0], 1, 0, player.pathY[0],
						localPlayer.pathX[0], false, player.pathX[0]);
				crossX = super.saveClickX;
				crossY = super.saveClickY;
				crossType = 2;
				crossIndex = 0;
				anInt1188 += clicked;
				if (anInt1188 >= 90) {
					//(anti-cheat)
					//   outgoing.writeOpcode(136);
					anInt1188 = 0;
				}
				sendPacket(new PlayerOption1(clicked));
			}
		}

		// npc option 1
		if (action == 20) {
			Npc npc = npcs[clicked];                  
			if (npc != null) {
				doWalkTo(2, 0, 1, 0, localPlayer.pathY[0], 1, 0,
						npc.pathY[0], localPlayer.pathX[0],
						false, npc.pathX[0]);
				crossX = super.saveClickX;
				crossY = super.saveClickY;
				crossType = 2;
				crossIndex = 0;
				// npc action 1
				sendPacket(new NpcOption1(clicked));
			}
		}

		// player option 2
		if (action == 779) {
			Player player = players[clicked];                  
			if (player != null) {
				doWalkTo(2, 0, 1, 0, localPlayer.pathY[0], 1, 0,
						player.pathY[0], localPlayer.pathX[0],
						false, player.pathX[0]);
				crossX = super.saveClickX;
				crossY = super.saveClickY;
				crossType = 2;
				crossIndex = 0;
				// player option 2
				sendPacket(new PlayerAttackOption(clicked));
			}
		}

		// clicking tiles
		if (action == 519) {
			if (!menuOpen) {
				scene.clickTile(super.saveClickY - 4, super.saveClickX - 4);
			} else {
				scene.clickTile(button - 4, first - 4);
			}
		}

		// object option 5
		if (action == 1062) {
			anInt924 += regionBaseX;
			if (anInt924 >= 113) {
				// validates clicking object option 4
				// outgoing.writeOpcode(183);
				// outgoing.writeTriByte(0xe63271);
				anInt924 = 0;
			}
			clickObject(clicked, button, first);

			// object option 5
			sendPacket(new ObjectOption5(clicked >> 14 & 0x7fff, button + regionBaseY, first + regionBaseX));
		}

		// continue dialogue
		if (action == 679 && !continuedDialogue) {
			sendPacket(new NextDialogue(button));
			continuedDialogue = true;
		}

		//Pressed button
		if(action == 647) {

			//Spawn tab?
			if(button >= 31031 && button <= 31731) {
				int index = button - 31031;
				int item = getResultsArray()[index];
				if(item > 0) {
					sendPacket(new SpawnTabClick(item, first == 1, spawnType == SpawnTabType.BANK));
				}
				if(first == 0) {
					searchingSpawnTab = true;
				}
				return;
			}
			
			//Key bindings?
			if(openInterfaceId == 53000) {
				for(int i = 0; i < 14; i++) {
					if(button == 53048 + (i*3)) {
						int key = KeyEvent.VK_F1 + first;
						if(key > KeyEvent.VK_F12) {
							key = KeyEvent.VK_ESCAPE;
						}
						TabBindings.bind(i, key);
						return;
					}
				}
			}

			sendPacket(new ClickButtonAction(button, first));
		}

		// using bank all option of the bank interface
		if (action == 431) {
			sendPacket(new ItemContainerOption4(first, button, clicked));
			atInventoryLoopCycle = 0;
			atInventoryInterface = button;
			atInventoryIndex = first;
			atInventoryInterfaceType = 2;
			if (Widget.interfaceCache[button].parent == openInterfaceId) {
				atInventoryInterfaceType = 1;
			}
			if (Widget.interfaceCache[button].parent == backDialogueId) {
				atInventoryInterfaceType = 3;
			}
		}


		if (action == 337 || action == 42 || action == 792 || action == 322) {            	
			String string = menuActionText[id];
			int indexOf = string.indexOf("@whi@");                  
			if (indexOf != -1) {
				long usernameHash = StringUtils.encodeBase37(string.substring(indexOf + 5).trim());                        
				if (action == 337) {
					addFriend(usernameHash);
				}
				if (action == 42) {
					addIgnore(usernameHash);
				}
				if (action == 792) {
					removeFriend(usernameHash);
				}
				if (action == 322) {
					removeIgnore(usernameHash);
				}
			}
		}
		// using the bank x option on the bank interface
		if (action == 53) {
			// bank x
			sendPacket(new ItemContainerOption5(first, button, clicked));
			atInventoryLoopCycle = 0;
			atInventoryInterface = button;
			atInventoryIndex = first;
			atInventoryInterfaceType = 2;
			if (Widget.interfaceCache[button].parent == openInterfaceId)
				atInventoryInterfaceType = 1;
			if (Widget.interfaceCache[button].parent == backDialogueId)
				atInventoryInterfaceType = 3;
		}

		// using the second option of an item
		if (action == 539) {
			// item option 2
			sendPacket(new ItemOption3(clicked, first, button));

			atInventoryLoopCycle = 0;
			atInventoryInterface = button;
			atInventoryIndex = first;
			atInventoryInterfaceType = 2;
			if (Widget.interfaceCache[button].parent == openInterfaceId) {
				atInventoryInterfaceType = 1;
			}
			if (Widget.interfaceCache[button].parent == backDialogueId) {
				atInventoryInterfaceType = 3;
			}
		}
		if (action == 484 || action == 6) {
			String string = menuActionText[id];                  
			int indexOf = string.indexOf("@whi@");                  
			if (indexOf != -1) {
				string = string.substring(indexOf + 5).trim();
				String username = StringUtils.formatText(StringUtils.decodeBase37(StringUtils.encodeBase37(string)));
				boolean flag9 = false;
				for (int count = 0; count < playerCount; count++) {                        	
					Player player = players[playerList[count]];                              
					if (player == null || player.name == null || !player.name.equalsIgnoreCase(username)) {
						continue;
					}
					doWalkTo(2, 0, 1, 0, localPlayer.pathY[0], 1, 0,
							player.pathY[0],
							localPlayer.pathX[0], false,
							player.pathX[0]);

					// accepting trade
					if (action == 484) {
						//sendPacket(new ChatboxTrade(playerList[count]));
						sendPacket(new TradePlayer(playerList[count]));
					}

					// accepting a challenge
					if (action == 6) {
						anInt1188 += clicked;
						if (anInt1188 >= 90) {
							// (anti-cheat)
							//	outgoing.writeOpcode(136);
							anInt1188 = 0;
						}

						sendPacket(new ChatboxDuel(playerList[count]));
					}
					flag9 = true;
					break;
				}

				if (!flag9)
					sendMessage("Unable to find " + username, 0, "");
			}
		}

		// Using an item on another item
		if (action == 870) {
			// item on item
			sendPacket(new ItemOnItem(first, anInt1283, clicked, anInt1284, anInt1285, button));
			atInventoryLoopCycle = 0;
			atInventoryInterface = button;
			atInventoryIndex = first;
			atInventoryInterfaceType = 2;
			if (Widget.interfaceCache[button].parent == openInterfaceId)
				atInventoryInterfaceType = 1;
			if (Widget.interfaceCache[button].parent == backDialogueId)
				atInventoryInterfaceType = 3;
		}

		// Using the drop option of an item            
		if (action == 847) {
			// drop item
			sendPacket(new DropItem(clicked, button, first));
			atInventoryLoopCycle = 0;
			atInventoryInterface = button;
			atInventoryIndex = first;
			atInventoryInterfaceType = 2;
			if (Widget.interfaceCache[button].parent == openInterfaceId)
				atInventoryInterfaceType = 1;
			if (Widget.interfaceCache[button].parent == backDialogueId)
				atInventoryInterfaceType = 3;
		}
		// useable spells
		if (action == 626) {
			Widget widget = Widget.interfaceCache[button];
			spellSelected = 1;
			spellId = widget.id;
			anInt1137 = button;
			spellUsableOn = widget.spellUsableOn;
			itemSelected = 0;
			String actionName = widget.selectedActionName;
			if (actionName.indexOf(" ") != -1)
				actionName = actionName.substring(0, actionName.indexOf(" "));
			String s8 = widget.selectedActionName;
			if (s8.indexOf(" ") != -1)
				s8 = s8.substring(s8.indexOf(" ") + 1);
			spellTooltip = actionName + " " + widget.spellName + " " + s8;
			// class9_1.sprite1.drawSprite(class9_1.x, class9_1.anInt265,
			// 0xffffff);
			// class9_1.sprite1.drawSprite(200,200);
			//if (Configuration.client_debug)
			//	System.out.println(
			//		"spellId: " + spellId + " - spellSelected: " + spellSelected);
			//	System.out.println(button + " " + widget.selectedActionName + " " + anInt1137);
			if (spellUsableOn == 16) {
				tabId = 3;
				tabAreaAltered = true;
			}
			return;
		}

		// Using the bank 5 option on a bank widget
		if (action == 78) {
			// bank 5
			sendPacket(new ItemContainerOption2(button, clicked, first));
			atInventoryLoopCycle = 0;
			atInventoryInterface = button;
			atInventoryIndex = first;
			atInventoryInterfaceType = 2;
			if (Widget.interfaceCache[button].parent == openInterfaceId)
				atInventoryInterfaceType = 1;
			if (Widget.interfaceCache[button].parent == backDialogueId)
				atInventoryInterfaceType = 3;
		}

		// player option 2
		if (action == 27) {
			Player player = players[clicked];                  
			if (player != null) {
				doWalkTo(2, 0, 1, 0, localPlayer.pathY[0], 1, 0,
						player.pathY[0], localPlayer.pathX[0],
						false, player.pathX[0]);
				crossX = super.saveClickX;
				crossY = super.saveClickY;
				crossType = 2;
				crossIndex = 0;
				anInt986 += clicked;
				if (anInt986 >= 54) {
					//(anti-cheat)
					//	outgoing.writeOpcode(189);
					//	outgoing.writeByte(234);
					anInt986 = 0;
				}
				// attack player
				sendPacket(new FollowPlayer(clicked));
			}
		}

		// Used for lighting logs
		if (action == 213) {
			boolean flag3 = doWalkTo(2, 0, 0, 0, localPlayer.pathY[0], 0, 0, button,
					localPlayer.pathX[0], false, first);
			if (!flag3)
				flag3 = doWalkTo(2, 0, 1, 0, localPlayer.pathY[0], 1, 0, button,
						localPlayer.pathX[0], false, first);
			crossX = super.saveClickX;
			crossY = super.saveClickY;
			crossType = 2;
			crossIndex = 0;
			// light item
			/*outgoing.writeOpcode(79);
			outgoing.writeLEShort(button + regionBaseY);
			outgoing.writeShort(clicked);
			outgoing.writeShortA(first + regionBaseX);*/
		}

		// Using the unequip option on the equipment tab interface
		if (action == 632) {
			sendPacket(new ItemContainerOption1(button, first, clicked));
			atInventoryLoopCycle = 0;
			atInventoryInterface = button;
			atInventoryIndex = first;
			atInventoryInterfaceType = 2;
			if (Widget.interfaceCache[button].parent == openInterfaceId)
				atInventoryInterfaceType = 1;
			if (Widget.interfaceCache[button].parent == backDialogueId)
				atInventoryInterfaceType = 3;
		}

		if (action == 1004) {
			if (tabInterfaceIDs[10] != -1) {
				tabId = 10;
				tabAreaAltered = true;
			}
		}
		if (action == 1003) {
			clanChatMode = 2;
			updateChatbox = true;
		}
		if (action == 1002) {
			clanChatMode = 1;
			updateChatbox = true;
		}
		if (action == 1001) {
			clanChatMode = 0;
			updateChatbox = true;
		}
		if (action == 1000) {
			cButtonCPos = 4;
			chatTypeView = 11;
			updateChatbox = true;
		}

		if (action == 999) {
			cButtonCPos = 0;
			chatTypeView = 0;
			updateChatbox = true;
		}
		if (action == 998) {
			cButtonCPos = 1;
			chatTypeView = 5;
			updateChatbox = true;
		}
		
		if (action == 912) {
			if(!sendingAutochat) {
			publicChatMode = 4;
			updateChatbox = true;
			messagePromptRaised = true;
			aString1121 = "Send autochat: @dre@[off]";
			promptInput = "";
			sendingAutochat = true;
			updateChatbox = true;
			} else {
				autochatString = "";
				publicChatMode = 0;
				localPlayer.spokenText = "";
				updateChatbox = true;
				sendingAutochat = false;
				sendPacket(new ChatSettings(publicChatMode, privateChatMode, tradeMode));
			}

		}

		// public chat "hide" option
		if (action == 997) {
			publicChatMode = 3;
			updateChatbox = true;
			sendingAutochat = false; 
			sendPacket(new ChatSettings(publicChatMode, privateChatMode, tradeMode));
		}

		// public chat "off" option
		if (action == 996) {
			publicChatMode = 2;
			updateChatbox = true;
			sendingAutochat = false; 
			sendPacket(new ChatSettings(publicChatMode, privateChatMode, tradeMode));
		}

		// public chat "friends" option
		if (action == 995) {
			publicChatMode = 1;
			updateChatbox = true;
			sendingAutochat = false; 
			sendPacket(new ChatSettings(publicChatMode, privateChatMode, tradeMode));
		}

		// public chat "on" option
		if (action == 994) {
			publicChatMode = 0;
			updateChatbox = true;
			sendingAutochat = false; 
			sendPacket(new ChatSettings(publicChatMode, privateChatMode, tradeMode));
		}

		// public chat main click
		if (action == 993) {
			cButtonCPos = 2;
			chatTypeView = 1;
			updateChatbox = true;
		}

		// private chat "off" option
		if (action == 992) {
			privateChatMode = 2;
			updateChatbox = true;

			sendPacket(new ChatSettings(publicChatMode, privateChatMode, tradeMode));
		}

		// private chat "friends" option
		if (action == 991) {
			privateChatMode = 1;
			updateChatbox = true;

			sendPacket(new ChatSettings(publicChatMode, privateChatMode, tradeMode));
		}

		// private chat "on" option
		if (action == 990) {
			privateChatMode = 0;
			updateChatbox = true;

			sendPacket(new ChatSettings(publicChatMode, privateChatMode, tradeMode));
		}

		// private chat main click
		if (action == 989) {
			cButtonCPos = 3;
			chatTypeView = 2;
			updateChatbox = true;
		}

		// trade message privacy option "off" option
		if (action == 987) {
			tradeMode = 2;
			updateChatbox = true;

			sendPacket(new ChatSettings(publicChatMode, privateChatMode, tradeMode));
		}

		// trade message privacy option "friends" option
		if (action == 986) {
			tradeMode = 1;
			updateChatbox = true;

			sendPacket(new ChatSettings(publicChatMode, privateChatMode, tradeMode));
		}

		// trade message privacy option "on" option
		if (action == 985) {
			tradeMode = 0;
			updateChatbox = true;

			sendPacket(new ChatSettings(publicChatMode, privateChatMode, tradeMode));
		}

		// trade message privacy option main click
		if (action == 984) {
			cButtonCPos = 5;
			chatTypeView = 3;
			updateChatbox = true;
		}

		if (action == 980) {
			cButtonCPos = 6;
			chatTypeView = 4;
			updateChatbox = true;
		}

		// Using 3rd option of an item
		if (action == 493) {
			// item option 3
			sendPacket(new ItemOption2(button, first, clicked));
			atInventoryLoopCycle = 0;
			atInventoryInterface = button;
			atInventoryIndex = first;
			atInventoryInterfaceType = 2;
			if (Widget.interfaceCache[button].parent == openInterfaceId)
				atInventoryInterfaceType = 1;
			if (Widget.interfaceCache[button].parent == backDialogueId)
				atInventoryInterfaceType = 3;
		}

		// clicking some sort of tile
		if (action == 652) {
			boolean flag4 = doWalkTo(2, 0, 0, 0, localPlayer.pathY[0], 0, 0, button,
					localPlayer.pathX[0], false, first);
			if (!flag4)
				flag4 = doWalkTo(2, 0, 1, 0, localPlayer.pathY[0], 1, 0, button,
						localPlayer.pathX[0], false, first);
			crossX = super.saveClickX;
			crossY = super.saveClickY;
			crossType = 2;
			crossIndex = 0;
			//unknown (non-anti bot)
			/*outgoing.writeOpcode(156);
			outgoing.writeShortA(first + regionBaseX);
			outgoing.writeLEShort(button + regionBaseY);
			outgoing.writeLEShortA(clicked);*/
		}

		// Using a spell on a ground item
		if (action == 94) {
			boolean flag5 = doWalkTo(2, 0, 0, 0, localPlayer.pathY[0], 0, 0, button,
					localPlayer.pathX[0], false, first);
			if (!flag5)
				flag5 = doWalkTo(2, 0, 1, 0, localPlayer.pathY[0], 1, 0, button,
						localPlayer.pathX[0], false, first);
			crossX = super.saveClickX;
			crossY = super.saveClickY;
			crossType = 2;
			crossIndex = 0;
			// magic on ground item
			sendPacket(new MagicOnGroundItem(button + regionBaseY, clicked, first + regionBaseX, anInt1137));
		}
		if (action == 646) {
			// button click

			switch(button) {
			case 930:
				Client.cameraZoom = 1200;
				break;
			case 931:
				Client.cameraZoom = 800;
				break;
			case 932:
				Client.cameraZoom = 400;
				break;
			case 933:
				Client.cameraZoom = 200;
				break;
			case 934:
				Client.cameraZoom = 0;
				break;
			case 32506:
				bankTabShow = BankTabShow.FIRST_ITEM_IN_TAB;
				break;
			case 32507:
				bankTabShow = BankTabShow.DIGIT;
				break;
			case 32508:
				bankTabShow = BankTabShow.ROMAN_NUMERAL;
				break;

			default:
				sendPacket(new ClickButton(button));
				break;
			}

			Widget widget = Widget.interfaceCache[button];                  
			if (widget.valueIndexArray != null && widget.valueIndexArray[0][0] == 5) {
				int i2 = widget.valueIndexArray[0][1];
				if (settings[i2] != widget.requiredValues[0]) {
					settings[i2] = widget.requiredValues[0];
					updateVarp(i2);
				}
			}
		}

		// Using the 2nd option of an npc
		if (action == 225) {
			Npc npc = npcs[clicked];                  
			if (npc != null) {
				doWalkTo(2, 0, 1, 0, localPlayer.pathY[0], 1, 0,
						npc.pathY[0], localPlayer.pathX[0],
						false, npc.pathX[0]);
				crossX = super.saveClickX;
				crossY = super.saveClickY;
				crossType = 2;
				crossIndex = 0;
				anInt1226 += clicked;
				if (anInt1226 >= 85) {
					// (anti-cheat)
					//outgoing.writeOpcode(230);
					//outgoing.writeByte(239);
					anInt1226 = 0;
				}
				// npc option 2
				sendPacket(new NpcOption2(clicked));
			}
		}

		// Using the 3rd option of an npc
		if (action == 965) {
			Npc npc = npcs[clicked];                  
			if (npc != null) {
				doWalkTo(2, 0, 1, 0, localPlayer.pathY[0], 1, 0,
						npc.pathY[0], localPlayer.pathX[0],
						false, npc.pathX[0]);
				crossX = super.saveClickX;
				crossY = super.saveClickY;
				crossType = 2;
				crossIndex = 0;
				anInt1134++;
				if (anInt1134 >= 96) {
					//(anti-cheat)
					//outgoing.writeOpcode(152);
					//outgoing.writeByte(88);
					anInt1134 = 0;
				}
				// npc option 3
				sendPacket(new NpcOption3(clicked));
			}
		}

		// Using a spell on an npc
		if (action == 413) {
			Npc npc = npcs[clicked];
			if (npc != null) {
				doWalkTo(2, 0, 1, 0, localPlayer.pathY[0], 1, 0, npc.pathY[0],
						localPlayer.pathX[0], false, npc.pathX[0]);
				crossX = super.saveClickX;
				crossY = super.saveClickY;
				crossType = 2;
				crossIndex = 0;
				// magic on npc
				sendPacket(new MagicOnNpc(clicked, anInt1137));
			}
		}

		// close open interfaces
		if (action == 200) {
			clearTopInterfaces();
		}

		// Clicking "Examine" option on an npc
		if (action == 1025 || action == 1025) {
			Npc npc = npcs[clicked];                  
			if (npc != null) {
				NpcDefinition entityDef = npc.desc;
				if (entityDef.childrenIDs != null)
					entityDef = entityDef.morph();
				if (entityDef != null) {
					sendPacket(new ExamineNpc(entityDef.id));
				}
			}
		}

		if (action == 900) {
			clickObject(clicked, button, first);
			// object option 2
			sendPacket(new ObjectOption2(clicked >> 14 & 0x7fff, button + regionBaseY, first + regionBaseX));
		}

		// Using the "Attack" option on a npc
		if (action == 412) {
			Npc npc = npcs[clicked];                  
			if (npc != null) {
				doWalkTo(2, 0, 1, 0, localPlayer.pathY[0], 1, 0,
						npc.pathY[0], localPlayer.pathX[0],
						false, npc.pathX[0]);
				crossX = super.saveClickX;
				crossY = super.saveClickY;
				crossType = 2;
				crossIndex = 0;
				sendPacket(new AttackNpc(clicked));
			}
		}

		// Using spells on a player
		if (action == 365) {
			Player player = players[clicked];
			if (player != null) {
				doWalkTo(2, 0, 1, 0, localPlayer.pathY[0], 1, 0, player.pathY[0],
						localPlayer.pathX[0], false, player.pathX[0]);
				crossX = super.saveClickX;
				crossY = super.saveClickY;
				crossType = 2;
				crossIndex = 0;
				// spells on plr
				sendPacket(new MagicOnPlayer(clicked, anInt1137));
			}
		}

		// Using the 3rd option of a player
		if (action == 729) {
			Player player = players[clicked];                  
			if (player != null) {
				doWalkTo(2, 0, 1, 0, localPlayer.pathY[0], 1, 0,
						player.pathY[0], localPlayer.pathX[0],
						false, player.pathX[0]);
				crossX = super.saveClickX;
				crossY = super.saveClickY;
				crossType = 2;
				crossIndex = 0;
				sendPacket(new TradePlayer(clicked));
			}
		}

		// Using the 4th option of a player
		if (action == 577) {
			Player player = players[clicked];                  
			if (player != null) {
				doWalkTo(2, 0, 1, 0, localPlayer.pathY[0], 1, 0,
						player.pathY[0], localPlayer.pathX[0],
						false, player.pathX[0]);
				crossX = super.saveClickX;
				crossY = super.saveClickY;
				crossType = 2;
				crossIndex = 0;
				// trade request
				sendPacket(new TradePlayer(clicked));
			}
		}

		// Using a spell on an item
		if (action == 956 && clickObject(clicked, button, first)) {
			// magic on item
			//	sendPacket(new MagicOnItem(first + regionBaseX, anInt1137, button + regionBaseY, clicked >> 14 & 0x7fff));
		}

		// Some walking action (packet 23)
		if (action == 567) {
			boolean flag6 = doWalkTo(2, 0, 0, 0, localPlayer.pathY[0], 0, 0, button,
					localPlayer.pathX[0], false, first);
			if (!flag6)
				flag6 = doWalkTo(2, 0, 1, 0, localPlayer.pathY[0], 1, 0, button,
						localPlayer.pathX[0], false, first);
			crossX = super.saveClickX;
			crossY = super.saveClickY;
			crossType = 2;
			crossIndex = 0;
			//anti-cheat)
			
			/*outgoing.writeOpcode(23);
			outgoing.writeLEShort(button + regionBaseY);
			outgoing.writeLEShort(clicked);
			outgoing.writeLEShort(first + regionBaseX);*/
		}

		// Using the bank 10 option on the bank interface
		if (action == 867) {

			if ((clicked & 3) == 0) {
				anInt1175++;
			}

			if (anInt1175 >= 59) {
				//(anti-cheat)
				//outgoing.writeOpcode(200);
				//outgoing.writeShort(25501);
				anInt1175 = 0;
			}
			// bank 10
			sendPacket(new ItemContainerOption3(button, clicked, first));
			atInventoryLoopCycle = 0;
			atInventoryInterface = button;
			atInventoryIndex = first;
			atInventoryInterfaceType = 2;
			if (Widget.interfaceCache[button].parent == openInterfaceId)
				atInventoryInterfaceType = 1;
			if (Widget.interfaceCache[button].parent == backDialogueId)
				atInventoryInterfaceType = 3;
		}

		// Using a spell on an inventory item
		if (action == 543) {
			// magic on item
			sendPacket(new MagicOnItem(first, clicked, button, anInt1137));
			atInventoryLoopCycle = 0;
			atInventoryInterface = button;
			atInventoryIndex = first;
			atInventoryInterfaceType = 2;
			if (Widget.interfaceCache[button].parent == openInterfaceId)
				atInventoryInterfaceType = 1;
			if (Widget.interfaceCache[button].parent == backDialogueId)
				atInventoryInterfaceType = 3;
		}

		// Clicking report abuse button
		if (action == 606) {
			String s2 = menuActionText[id];
			int j2 = s2.indexOf("@whi@");
			if (j2 != -1)
				if (openInterfaceId == -1) {
					clearTopInterfaces();
					reportAbuseInput = s2.substring(j2 + 5).trim();
					canMute = false;
					for (int index = 0; index < Widget.interfaceCache.length; index++) {
						if (Widget.interfaceCache[index] == null
								|| Widget.interfaceCache[index].contentType != 600)
							continue;
						reportAbuseInterfaceID = openInterfaceId =
								Widget.interfaceCache[index].parent;
						break;
					}

				} else {
					sendMessage("Please close the interface you have open before using 'report abuse'",
							0, "");
				}
		}

		// Using an inventory item on a player
		if (action == 491) {
			Player player = players[clicked];

			if (player != null) {
				doWalkTo(2, 0, 1, 0, localPlayer.pathY[0], 1, 0,
						player.pathY[0], localPlayer.pathX[0],
						false, player.pathX[0]);
				crossX = super.saveClickX;
				crossY = super.saveClickY;
				crossType = 2;
				crossIndex = 0;
				// TODO item on player
				sendPacket(new ItemOnPlayer(anInt1284, clicked, anInt1285, anInt1283));
			}
		}

		// reply to private message
		if (action == 639) {
			String text = menuActionText[id]; 

			int indexOf = text.indexOf("@whi@");

			if (indexOf != -1) {
				long usernameHash = StringUtils.encodeBase37(text.substring(indexOf + 5).trim());                        
				int resultIndex = -1;
				for (int friendIndex = 0; friendIndex < friendsCount; friendIndex++) {                        	
					if (friendsListAsLongs[friendIndex] != usernameHash) {
						continue;
					}
					resultIndex = friendIndex;
					break;
				}

				if (resultIndex != -1 && friendsNodeIDs[resultIndex] > 0) {                        	
					updateChatbox = true;
					inputDialogState = 0;
					messagePromptRaised = true;
					promptInput = "";
					friendsListAction = 3;
					aLong953 = friendsListAsLongs[resultIndex];
					aString1121 = "Enter a message to send to " + friendsList[resultIndex];
				}
			}
		}

		// Using the equip option of an item in the inventory
		if (action == 454) {
			//equip item
			sendPacket(new EquipItem(clicked, first, button));
			atInventoryLoopCycle = 0;
			atInventoryInterface = button;
			atInventoryIndex = first;
			atInventoryInterfaceType = 2;
			if (Widget.interfaceCache[button].parent == openInterfaceId)
				atInventoryInterfaceType = 1;
			if (Widget.interfaceCache[button].parent == backDialogueId)
				atInventoryInterfaceType = 3;
		}

		// Npc option 4
		if (action == 478) {
			Npc npc = npcs[clicked];                  
			if (npc != null) {
				doWalkTo(2, 0, 1, 0, localPlayer.pathY[0], 1, 0,
						npc.pathY[0], localPlayer.pathX[0],
						false, npc.pathX[0]);
				crossX = super.saveClickX;
				crossY = super.saveClickY;
				crossType = 2;
				crossIndex = 0;

				if ((clicked & 3) == 0) {
					anInt1155++;
				}

				if (anInt1155 >= 53) {
					//TODO unknown (anti-cheat)
					//	outgoing.writeOpcode(85);
					//	outgoing.writeByte(66);
					anInt1155 = 0;
				}

				// npc option 4
				sendPacket(new NpcOption4(clicked));
			}
		}

		// Object option 3
		if (action == 113) {
			clickObject(clicked, button, first);
			// object option 3
			sendPacket(new ObjectOption3(first + regionBaseX, button + regionBaseY, clicked >> 14 & 0x7fff));
		}

		// Object option 4
		if (action == 872) {
			clickObject(clicked, button, first);
			sendPacket(new ObjectOption4(first + regionBaseX, clicked >> 14 & 0x7fff, button + regionBaseY));
		}

		// Object option 1
		if (action == 502) {
			clickObject(clicked, button, first);
			sendPacket(new ObjectOption1(first + regionBaseX, clicked >> 14 & 0x7fff, button + regionBaseY));
		}


		if (action == 169) {

			sendPacket(new ClickButton(button));

			if(button != 19158) { //Run button, server handles config
				Widget widget = Widget.interfaceCache[button];

				if (widget.valueIndexArray != null && widget.valueIndexArray[0][0] == 5) {
					int setting = widget.valueIndexArray[0][1];
					settings[setting] = 1 - settings[setting];
					updateVarp(setting);
				}
			}
		}

		if (action == 447) {
			itemSelected = 1;
			anInt1283 = first;
			anInt1284 = button;
			anInt1285 = clicked;
			selectedItemName = ItemDefinition.lookup(clicked).name;
			spellSelected = 0;
			return;
		}

		if (action == 1226) {
			int objectId = clicked >> 14 & 0x7fff;            
			ObjectDefinition definition = ObjectDefinition.lookup(objectId);                  
			String message;                  
			if (definition.description != null)
				message = new String(definition.description);
			else
				message = "It's a " + definition.name + ".";
			sendMessage(message, 0, "");
		}

		// Click First Option Ground Item
		if (action == 244) {
			boolean flag7 = doWalkTo(2, 0, 0, 0, localPlayer.pathY[0], 0, 0, button,
					localPlayer.pathX[0], false, first);
			if (!flag7)
				flag7 = doWalkTo(2, 0, 1, 0, localPlayer.pathY[0], 1, 0, button,
						localPlayer.pathX[0], false, first);
			crossX = super.saveClickX;
			crossY = super.saveClickY;
			crossType = 2;
			crossIndex = 0;
			/*TODO: NO idea SOMETHING WITH GROUNDITEMS
			outgoing.writeOpcode(253);
			outgoing.writeLEShort(first + regionBaseX);
			outgoing.writeLEShortA(button + regionBaseY);
			outgoing.writeShortA(clicked);*/
		}


		if (action == 1448 || action == 1125) {
			ItemDefinition definition = ItemDefinition.lookup(clicked);                  
			if(definition != null) {
				sendPacket(new ExamineItem(clicked));
			}
		}

		itemSelected = 0;
		spellSelected = 0;

	}

	public void run() {
		if (drawFlames) {
			drawFlames();
		} else {
			super.run();
		}
	}

	private void createMenu() {
		if (openInterfaceId == 15244) {
			return;
		}
		if (itemSelected == 0 && spellSelected == 0) {
			menuActionText[menuActionRow] = "Walk here";
			menuActionTypes[menuActionRow] = 519;
			firstMenuAction[menuActionRow] = super.mouseX;
			secondMenuAction[menuActionRow] = super.mouseY;
			menuActionRow++;
		}

		int j = -1;
		for (int k = 0; k < Model.anInt1687; k++) {
			int l = Model.anIntArray1688[k];
			int i1 = l & 0x7f;
			int j1 = l >> 7 & 0x7f;
		int k1 = l >> 29 & 3;
		int l1 = l >> 14 & 0x7fff;
		if (l == j)
			continue;
		j = l;
		if (k1 == 2 && scene.getMask(plane, i1, j1, l) >= 0) {
			ObjectDefinition objectDef = ObjectDefinition.lookup(l1);
			if (objectDef.childrenIDs != null)
				objectDef = objectDef.method580();
			if (objectDef == null)
				continue;
			if (itemSelected == 1) {
				menuActionText[menuActionRow] = "Use " + selectedItemName
						+ " with @cya@" + objectDef.name;
				menuActionTypes[menuActionRow] = 62;
				selectedMenuActions[menuActionRow] = l;
				firstMenuAction[menuActionRow] = i1;
				secondMenuAction[menuActionRow] = j1;
				menuActionRow++;
			} else if (spellSelected == 1) {
				if ((spellUsableOn & 4) == 4) {
					menuActionText[menuActionRow] =
							spellTooltip + " @cya@" + objectDef.name;
					menuActionTypes[menuActionRow] = 956;
					selectedMenuActions[menuActionRow] = l;
					firstMenuAction[menuActionRow] = i1;
					secondMenuAction[menuActionRow] = j1;
					menuActionRow++;
				}
			} else {
				if (objectDef.interactions != null) {
					for (int type = 4; type >= 0; type--)
						if (objectDef.interactions[type] != null) {
							menuActionText[menuActionRow] =
									objectDef.interactions[type] + " @cya@"
											+ objectDef.name;
							if (type == 0)
								menuActionTypes[menuActionRow] = 502;
							if (type == 1)
								menuActionTypes[menuActionRow] = 900;
							if (type == 2)
								menuActionTypes[menuActionRow] = 113;
							if (type == 3)
								menuActionTypes[menuActionRow] = 872;
							if (type == 4)
								menuActionTypes[menuActionRow] = 1062;
							selectedMenuActions[menuActionRow] = l;
							firstMenuAction[menuActionRow] = i1;
							secondMenuAction[menuActionRow] = j1;
							menuActionRow++;
						}

				}
				if ((myPrivilege >= 2 && myPrivilege <= 4)) {
					menuActionText[menuActionRow] = "Examine @cya@" + objectDef.name
							+ " @gre@(@whi@" + l1 + "@gre@) (@whi@"
							+ (i1 + regionBaseX) + "," + (j1 + regionBaseY)
							+ "@gre@)";
				} else {
					menuActionText[menuActionRow] =
							"Examine @cya@" + objectDef.name;
				}
				menuActionTypes[menuActionRow] = 1226;
				selectedMenuActions[menuActionRow] = objectDef.type << 14;
				firstMenuAction[menuActionRow] = i1;
				secondMenuAction[menuActionRow] = j1;
				menuActionRow++;
			}
		}
		if (k1 == 1) {
			Npc npc = npcs[l1];
			try {
				if (npc.desc.size == 1 && (npc.x & 0x7f) == 64
						&& (npc.y & 0x7f) == 64) {
					for (int j2 = 0; j2 < npcCount; j2++) {
						Npc npc2 = npcs[npcIndices[j2]];
						if (npc2 != null && npc2 != npc && npc2.desc.size == 1
								&& npc2.x == npc.x && npc2.y == npc.y) {
							if(npc2.showActions()) {
								buildAtNPCMenu(npc2.desc, npcIndices[j2], j1, i1);
							}
						}
					}
					for (int l2 = 0; l2 < playerCount; l2++) {
						Player player = players[playerList[l2]];
						if (player != null && player.x == npc.x
								&& player.y == npc.y)
							buildAtPlayerMenu(i1, playerList[l2], player,
									j1);
					}
				}
				if(npc.showActions()) {
					buildAtNPCMenu(npc.desc, l1, j1, i1);
				}
			} catch (Exception e) {
			}
		}
		if (k1 == 0) {
			Player player = players[l1];
			if ((player.x & 0x7f) == 64 && (player.y & 0x7f) == 64) {
				for (int k2 = 0; k2 < npcCount; k2++) {
					Npc class30_sub2_sub4_sub1_sub1_2 = npcs[npcIndices[k2]];
					if (class30_sub2_sub4_sub1_sub1_2 != null
							&& class30_sub2_sub4_sub1_sub1_2.desc.size == 1
							&& class30_sub2_sub4_sub1_sub1_2.x == player.x
							&& class30_sub2_sub4_sub1_sub1_2.y == player.y)
						buildAtNPCMenu(class30_sub2_sub4_sub1_sub1_2.desc,
								npcIndices[k2], j1, i1);
				}

				for (int i3 = 0; i3 < playerCount; i3++) {
					Player class30_sub2_sub4_sub1_sub2_2 =
							players[playerList[i3]];
					if (class30_sub2_sub4_sub1_sub2_2 != null
							&& class30_sub2_sub4_sub1_sub2_2 != player
							&& class30_sub2_sub4_sub1_sub2_2.x == player.x
							&& class30_sub2_sub4_sub1_sub2_2.y == player.y)
						buildAtPlayerMenu(i1, playerList[i3],
								class30_sub2_sub4_sub1_sub2_2, j1);
				}

			}
			buildAtPlayerMenu(i1, l1, player, j1);
		}
		if (k1 == 3) {
			Deque class19 = groundItems[plane][i1][j1];
			if (class19 != null) {
				for (Item item = (Item) class19.getFirst(); item != null; item =
						(Item) class19.getNext()) {
					ItemDefinition itemDef = ItemDefinition.lookup(item.ID);
					if (itemSelected == 1) {
						menuActionText[menuActionRow] = "Use " + selectedItemName
								+ " with @lre@" + itemDef.name;
						menuActionTypes[menuActionRow] = 511;
						selectedMenuActions[menuActionRow] = item.ID;
						firstMenuAction[menuActionRow] = i1;
						secondMenuAction[menuActionRow] = j1;
						menuActionRow++;
					} else if (spellSelected == 1) {
						if ((spellUsableOn & 1) == 1) {
							menuActionText[menuActionRow] =
									spellTooltip + " @lre@" + itemDef.name;
							menuActionTypes[menuActionRow] = 94;
							selectedMenuActions[menuActionRow] = item.ID;
							firstMenuAction[menuActionRow] = i1;
							secondMenuAction[menuActionRow] = j1;
							menuActionRow++;
						}
					} else {
						for (int j3 = 4; j3 >= 0; j3--)
							if (itemDef.groundActions != null
							&& itemDef.groundActions[j3] != null) {
								menuActionText[menuActionRow] =
										itemDef.groundActions[j3]
												+ " @lre@"
												+ itemDef.name;
								if (j3 == 0)
									menuActionTypes[menuActionRow] = 652;
								if (j3 == 1)
									menuActionTypes[menuActionRow] = 567;
								if (j3 == 2)
									menuActionTypes[menuActionRow] = 234;
								if (j3 == 3)
									menuActionTypes[menuActionRow] = 244;
								if (j3 == 4)
									menuActionTypes[menuActionRow] = 213;
								selectedMenuActions[menuActionRow] = item.ID;
								firstMenuAction[menuActionRow] = i1;
								secondMenuAction[menuActionRow] = j1;
								menuActionRow++;
							} else if (j3 == 2) {
								menuActionText[menuActionRow] =
										"Take @lre@" + itemDef.name;
								menuActionTypes[menuActionRow] = 234;
								selectedMenuActions[menuActionRow] = item.ID;
								firstMenuAction[menuActionRow] = i1;
								secondMenuAction[menuActionRow] = j1;
								menuActionRow++;
							}
					}
					if ((myPrivilege >= 2 && myPrivilege <= 4)) {
						menuActionText[menuActionRow] = "Examine @lre@"
								+ itemDef.name + " @gre@ (@whi@" + item.ID
								+ "@gre@)";
					} else {
						menuActionText[menuActionRow] =
								"Examine @lre@" + itemDef.name;
					}
					menuActionTypes[menuActionRow] = 1448;
					selectedMenuActions[menuActionRow] = item.ID;
					firstMenuAction[menuActionRow] = i1;
					secondMenuAction[menuActionRow] = j1;
					menuActionRow++;
				}
			}
		}
		}
	}

	public boolean exitRequested = false;
	private boolean startSpin = false;
	public void cleanUpForQuit() {
		exitRequested = true;
		SignLink.reporterror = false;
		try {
			if (socketStream != null) {
				socketStream.close();
			}
		} catch (Exception _ex) {
		}
		socketStream = null;
		stopMidi();
		if (mouseDetection != null)
			mouseDetection.running = false;
		mouseDetection = null;
		resourceProvider.disable();
		resourceProvider = null;
		outgoing = null;
		login = null;
		incoming = null;
		localRegionIds = null;
		localRegionMapData = null;
		localRegionLandscapeData = null;
		localRegionMapIds = null;
		localRegionLandscapeIds = null;
		tileHeights = null;
		tileFlags = null;
		scene = null;
		collisionMaps = null;
		anIntArrayArray901 = null;
		anIntArrayArray825 = null;
		bigX = null;
		bigY = null;
		aByteArray912 = null;
		tabImageProducer = null;
		leftFrame = null;
		topFrame = null;
		minimapImageProducer = null;
		gameScreenImageProducer = null;
		chatboxImageProducer = null;
		chatSettingImageProducer = null;
		/* Null pointers for custom sprites */
		cacheSprite = null;
		mapBack = null;
		sideIcons = null;
		compass = null;
		hitMarks = null;
		headIcons = null;
		skullIcons = null;
		headIconsHint = null;
		crosses = null;
		mapDotItem = null;
		mapDotNPC = null;
		mapDotPlayer = null;
		mapDotFriend = null;
		mapDotTeam = null;
		mapScenes = null;
		mapFunctions = null;
		anIntArrayArray929 = null;
		players = null;
		playerList = null;
		mobsAwaitingUpdate = null;
		playerSynchronizationBuffers = null;
		removedMobs = null;
		npcs = null;
		npcIndices = null;
		groundItems = null;
		spawns = null;
		projectiles = null;
		incompleteAnimables = null;
		firstMenuAction = null;
		secondMenuAction = null;
		menuActionTypes = null;
		selectedMenuActions = null;
		menuActionText = null;
		settings = null;
		minimapHintX = null;
		minimapHintY = null;
		minimapHint = null;
		minimapImage = null;
		friendsList = null;
		friendsListAsLongs = null;
		friendsNodeIDs = null;
		flameLeftBackground = null;
		flameRightBackground = null;
		topLeft1BackgroundTile = null;
		bottomLeft1BackgroundTile = null;
		loginBoxImageProducer = null;
		loginScreenAccessories = null;
		bottomLeft0BackgroundTile = null;
		bottomRightImageProducer = null;
		loginMusicImageProducer = null;
		middleLeft1BackgroundTile = null;
		aRSImageProducer_1115 = null;
		multiOverlay = null;
		nullLoader();
		ObjectDefinition.clear();
		NpcDefinition.clear();
		ItemDefinition.clear();
		FloorDefinition.underlays = null;
		FloorDefinition.overlays = null;
		IdentityKit.kits = null;
		Widget.interfaceCache = null;
		Animation.animations = null;
		Graphic.cache = null;
		Graphic.models = null;
		VariablePlayer.variables = null;
		super.fullGameScreen = null;
		Player.models = null;
		Rasterizer3D.clear();
		SceneGraph.destructor();
		Model.clear();
		Frame.clear();
		System.gc();
	}

	Component getGameComponent() {
		if (SignLink.mainapp != null)
			return SignLink.mainapp;
		if (super.gameFrame != null)
			return super.gameFrame;
		else
			return this;
	}

	private void manageTextInputs() {
		do {
			int key = readChar(-796);
			if (key == -1 || key == 96)
				break;
			if(key == 167 || key == 96) {
				if(myPrivilege >= 1 && myPrivilege <= 4) {
					consoleOpen = !consoleOpen;
				}
				return;
			}
			if (consoleOpen) {
				if (key == 8 && consoleInput.length() > 0)
					consoleInput = consoleInput.substring(0,
							consoleInput.length() - 1);
				if (key >= 32 && key <= 122
						&& consoleInput.length() < 80)
					consoleInput += (char) key;

				if ((key == 13 || key == 10)
						&& consoleInput.length() >= 1) {
					printConsoleMessage(consoleInput, 0);
					sendCommandPacket(consoleInput);
					consoleInput = "";
					updateChatbox = true;
				}
				return;
			}
			if(searchingSpawnTab) {
				if (key == 8 && searchSyntax.length() > 0) {
					searchSyntax = searchSyntax.substring(0, searchSyntax.length() - 1);
				}
				if (key >= 32 && key <= 122
						&& searchSyntax.length() < 15) {
					searchSyntax += (char) key;
				}
				fetchSearchResults = true;
				return;
			}

			//Space bar skipping dialogue
			if (!continuedDialogue && inputDialogState == 0 && backDialogueId > 0 && loggedIn && openInterfaceId == -1) {
				//System.out.println(key);

				//Simple continue action with space bar
				if(key == 32 && backDialogueId == 4893) {
					sendPacket(new NextDialogue(4899));
					continuedDialogue = true;
				} 

				//3 Options
				if(backDialogueId == 2469) {
					if(key == 49) { //Option 1
						sendPacket(new ClickButton(2471));
						continuedDialogue = true;
					} else if(key == 50) { //Option 2
						sendPacket(new ClickButton(2472));
						continuedDialogue = true;
					} else if(key == 51) { //Option 3
						sendPacket(new ClickButton(2473));
						continuedDialogue = true;
					}
				}

			}

			if (openInterfaceId != -1 && openInterfaceId == reportAbuseInterfaceID) {
				if (key == 8 && reportAbuseInput.length() > 0)
					reportAbuseInput = reportAbuseInput.substring(0,
							reportAbuseInput.length() - 1);
				if ((key >= 97 && key <= 122 || key >= 65 && key <= 90 || key >= 48 && key <= 57
						|| key == 32) && reportAbuseInput.length() < 12)
					reportAbuseInput += (char) key;
			} else if (messagePromptRaised) {
				if (key >= 32 && key <= 122 && promptInput.length() < 80) {
					promptInput += (char) key;
					updateChatbox = true;
				}
				if (key == 8 && promptInput.length() > 0) {
					promptInput = promptInput.substring(0, promptInput.length() - 1);
					updateChatbox = true;
				}
				if (key == 13 || key == 10) {
					messagePromptRaised = false;
					updateChatbox = true;
					if (friendsListAction == 1) {
						long l = StringUtils.encodeBase37(promptInput);
						addFriend(l);
					}
					if (friendsListAction == 2 && friendsCount > 0) {
						long l1 = StringUtils.encodeBase37(promptInput);
						removeFriend(l1);
					}
					if (friendsListAction == 3 && promptInput.length() > 0) {
						// private message
						/*	outgoing.writeOpcode(126);
						outgoing.writeByte(0);
						int k = outgoing.currentPosition;
						outgoing.writeLong(aLong953);
						ChatMessageCodec.encode(promptInput, outgoing);
						outgoing.writeBytes(outgoing.currentPosition - k);
						promptInput = ChatMessageCodec.processText(promptInput);*/
						
						promptInput = promptInput.substring(0, 1).toUpperCase() + promptInput.substring(1);
						sendPacket(new PrivateMessage(aLong953, promptInput));
						// promptInput = Censor.doCensor(promptInput);
						sendMessage(promptInput, 6, StringUtils.formatText(
								StringUtils.decodeBase37(aLong953)));
						if (privateChatMode == 2) {
							privateChatMode = 1;
							// privacy option
							sendPacket(new ChatSettings(publicChatMode, privateChatMode, tradeMode));
						}
					}
					if (friendsListAction == 4 && ignoreCount < 100) {
						long l2 = StringUtils.encodeBase37(promptInput);
						addIgnore(l2);
					}
					if (friendsListAction == 5 && ignoreCount > 0) {
						long l3 = StringUtils.encodeBase37(promptInput);
						removeIgnore(l3);
					}
					if (friendsListAction == 6) {
						long l3 = StringUtils.encodeBase37(promptInput);
						//	chatJoin(l3);
					}
					autochatString = promptInput;
				}
			} else if (inputDialogState == 1) {
				if (key >= 48 && key <= 57 && amountOrNameInput.length() < 10) {
					amountOrNameInput += (char) key;
					updateChatbox = true;
				}
				if (key == 8 && amountOrNameInput.length() > 0) {
					amountOrNameInput = amountOrNameInput.substring(0,
							amountOrNameInput.length() - 1);
					updateChatbox = true;
				}
				if (key == 13 || key == 10) {
					if (amountOrNameInput.length() > 0) {
						int length = amountOrNameInput.length();
						char lastChar = amountOrNameInput.charAt(length - 1);

						if (lastChar == 'k') {
							amountOrNameInput = amountOrNameInput.substring(0, length - 1) + "000";
						} else if (lastChar == 'm') {
							amountOrNameInput = amountOrNameInput.substring(0, length - 1) + "000000";
						} else if (lastChar == 'b') {
							amountOrNameInput = amountOrNameInput.substring(0, length - 1) + "000000000";
						}

						long amount = 0;

						try {
							amount = Long.parseLong(amountOrNameInput);

							// overflow concious code
							if (amount < 0) {
								amount = 0;
							} else if (amount > Integer.MAX_VALUE) {
								amount = Integer.MAX_VALUE;
							}
						} catch (Exception ignored) {
						}

						if (amount > 0) {
							sendPacket(new EnterAmount((int)amount));
						}
					}
					inputDialogState = 0;
					updateChatbox = true;
				}
			} else if (inputDialogState == 2) {
				if (key >= 32 && key <= 122 && amountOrNameInput.length() < 12) {
					amountOrNameInput += (char) key;
					updateChatbox = true;
				}
				if (key == 8 && amountOrNameInput.length() > 0) {
					amountOrNameInput = amountOrNameInput.substring(0,
							amountOrNameInput.length() - 1);
					updateChatbox = true;
				}
				if (key == 13 || key == 10) {
					if (amountOrNameInput.length() > 0) {
						sendPacket(new SendSyntax(amountOrNameInput));
					}
					inputDialogState = 0;
					updateChatbox = true;
				}
			} else if (backDialogueId == -1) {
				if (key >= 32 && key <= 122 && inputString.length() < 80) {
					inputString += (char) key;
					updateChatbox = true;
				}
				if (key == 8 && inputString.length() > 0) {
					inputString = inputString.substring(0, inputString.length() - 1);
					updateChatbox = true;
				}
				if (key == 9) {
					if(openInterfaceId == -1) {
						sendPacket(new CloseInterface());
					}
					tabToReplyPm();
				}
				
				//Remove the ability for players to do crowns..
				if(inputString.contains("@cr")) {
					inputString = inputString.replaceAll("@cr", "");
				} else if(inputString.contains("<img=")) {
					inputString = inputString.replaceAll("<img=", "");
				}
				
				if ((key == 13 || key == 10) && inputString.length() > 0) {

					if (inputString.startsWith("/")) {
						inputString = "::" + inputString;
					}
					if (inputString.contains("::int 13000")) {
						Widget w = Widget.interfaceCache[13200];
						Widget w2 = Widget.interfaceCache[13202];
						for(int i = 0; i < rewards.length; i++) {
							w2.inventoryItemId[i] = rewards[i];
							w2.inventoryAmounts[i] = 1;
							if(w2.inventoryItemId[i] == 996) {
									w2.inventoryAmounts[i] = 50000000;
							}
							
						}
						for(int j22 = 0; j22 < 300; j22++) {
						if(j22 == w.inventoryItemId.length) {
							break;
						}
							w.inventoryItemId[j22] = rewards[MiscUtils.random(rewards.length - 1)];
							w.inventoryAmounts[j22] = 1;
							if(w.inventoryItemId[j22] == 996) {
								w.inventoryAmounts[j22] = 50000000;
						}
							
						}
					}
					if (inputString.startsWith("::datadump")) {
						Widget.interfaceCache[25002 + 6].inventoryItemId[0] = 557;
						Widget.interfaceCache[25002 + 6].inventoryAmounts[0] = 1000;
						Widget.interfaceCache[25002 + 7].defaultText = "Air rune";
						Widget.interfaceCache[25010 + 6].inventoryItemId[0] = 556;
						Widget.interfaceCache[25010 + 6].inventoryAmounts[0] = 25000;
						Widget.interfaceCache[25010 + 7].defaultText = "Water rune";
					}
					if (inputString.startsWith("::")) {
						// command
						sendPacket(new Command(inputString.substring(2)));
					} else {
						String text = inputString.toLowerCase();
						int colorCode = 0;                                    
						if (text.startsWith("yellow:")) {
							colorCode = 0;
							inputString = inputString.substring(7);
						} else if (text.startsWith("red:")) {
							colorCode = 1;                                          
							inputString = inputString.substring(4);
						} else if (text.startsWith("green:")) {
							colorCode = 2;
							inputString = inputString.substring(6);
						} else if (text.startsWith("cyan:")) {
							colorCode = 3;
							inputString = inputString.substring(5);
						} else if (text.startsWith("purple:")) {
							colorCode = 4;
							inputString = inputString.substring(7);
						} else if (text.startsWith("white:")) {
							colorCode = 5;
							inputString = inputString.substring(6);
						} else if (text.startsWith("flash1:")) {
							colorCode = 6;
							inputString = inputString.substring(7);
						} else if (text.startsWith("flash2:")) {
							colorCode = 7;
							inputString = inputString.substring(7);
						} else if (text.startsWith("flash3:")) {
							colorCode = 8;
							inputString = inputString.substring(7);
						} else if (text.startsWith("glow1:")) {
							colorCode = 9;
							inputString = inputString.substring(6);
						} else if (text.startsWith("glow2:")) {
							colorCode = 10;
							inputString = inputString.substring(6);
						} else if (text.startsWith("glow3:")) {                                    	
							colorCode = 11;
							inputString = inputString.substring(6);
						}
						text = inputString.toLowerCase();                                    
						int effectCode = 0;
						if (text.startsWith("wave:")) {
							effectCode = 1;
							inputString = inputString.substring(5);
						} else if (text.startsWith("wave2:")) {
							effectCode = 2;
							inputString = inputString.substring(6);
						} else if (text.startsWith("shake:")) {
							effectCode = 3;
							inputString = inputString.substring(6);
						} else if (text.startsWith("scroll:")) {
							effectCode = 4;
							inputString = inputString.substring(7);
						} else if (text.startsWith("slide:")) {
							effectCode = 5;
							inputString = inputString.substring(6);
						}
						// chat
						/*outgoing.writeOpcode(4);
						outgoing.writeByte(0);
						int bufPos = outgoing.currentPosition;                                    
						outgoing.writeByteS(effectCode);
						outgoing.writeByteS(colorCode);
						chatBuffer.currentPosition = 0;
						ChatMessageCodec.encode(inputString, chatBuffer);
						outgoing.writeReverseDataA(chatBuffer.payload, 0,
								chatBuffer.currentPosition);
						outgoing.writeBytes(outgoing.currentPosition - bufPos);
						inputString = ChatMessageCodec.processText(inputString);*/
						// inputString = Censor.doCensor(inputString);
						inputString = StringUtils.formatText(inputString);
						sendPacket(new Chat(colorCode, effectCode, inputString));
						localPlayer.spokenText = inputString;
						localPlayer.textColour = colorCode;
						localPlayer.textEffect = effectCode;
						localPlayer.textCycle = 150;
						
						if (myPrivilege > 0 && myPrivilege < 10) {
							sendMessage(localPlayer.spokenText, 2,
									"@cr"+myPrivilege+"@" + localPlayer.name);
						} else  {
							sendMessage(localPlayer.spokenText, 2, localPlayer.name);
						}
						
						if (publicChatMode == 2) {
							publicChatMode = 3;
							// privacy option
							sendPacket(new ChatSettings(publicChatMode, privateChatMode, tradeMode));
						}
					}
					inputString = "";
					updateChatbox = true;
				}
			}
		} while (true);
	}

	private void buildPublicChat(int j) {
		int l = 0;
		for (int message = 0; message < 500; message++) {

			if (chatMessages[message] == null) {
				continue;
			}

			if (chatTypeView != 1) {
				continue;
			}

			int privacyOptionType = chatTypes[message];

			String crownName = chatNames[message];                  

			int k1 = (70 - l * 14 + 42) + anInt1089 + 4 + 5;

			if (k1 < -23) {
				break;
			}
			byte data = 0;
			if (crownName != null && crownName.startsWith("@cr1@")) {
				crownName = crownName.substring(5);
				data = 1;
			} else if (crownName != null && crownName.startsWith("@cr2@")) {
				crownName = crownName.substring(5);
				data = 2;
			} else if (crownName != null && crownName.startsWith("@cr3@")) {
				crownName = crownName.substring(5);
				data = 3;
			} else if (crownName != null && crownName.startsWith("@cr4@")) {
				crownName = crownName.substring(5);
				data = 4;
			} else if (crownName != null && crownName.startsWith("@cr5@")) {
				crownName = crownName.substring(5);
				data = 5;
			} else if (crownName != null && crownName.startsWith("@cr6@")) {
				crownName = crownName.substring(5);
				data = 6;
			} else if (crownName != null && crownName.startsWith("@cr7@")) {
				crownName = crownName.substring(5);
				data = 7;
			} else if (crownName != null && crownName.startsWith("@cr8@")) {
				crownName = crownName.substring(5);
				data = 8;
			} else if (crownName != null && crownName.startsWith("@cr9@")) {
				crownName = crownName.substring(5);
				data = 9;
			} else if (crownName != null && crownName.startsWith("@cr10@")) {
				crownName = crownName.substring(6);
				data = 10;
			}
			

			if ((privacyOptionType == 1 || privacyOptionType == 2) && (privacyOptionType == 1 || publicChatMode == 0
					|| publicChatMode == 1 && isFriendOrSelf(crownName))) {
				if (j > k1 - 14 && j <= k1 && !crownName.equals(localPlayer.name)) {
					if (!isFriendOrSelf(name)) {
						menuActionText[menuActionRow] = "Add ignore @whi@" + crownName;
						menuActionTypes[menuActionRow] = 42;
						menuActionRow++;
						menuActionText[menuActionRow] = "Add friend @whi@" + crownName;
						menuActionTypes[menuActionRow] = 337;
						menuActionRow++;
					} else {
						menuActionText[menuActionRow] = "Message @whi@" + name;
						menuActionTypes[menuActionRow] = 2639;
						menuActionRow++;
					}
				}
				l++;
			}
		}
	}

	private void buildFriendChat(int j) {
		int l = 0;
		for (int i1 = 0; i1 < 500; i1++) {
			if (chatMessages[i1] == null)
				continue;
			if (chatTypeView != 2)
				continue;
			int j1 = chatTypes[i1];
			String s = chatNames[i1];
			int k1 = (70 - l * 14 + 42) + anInt1089 + 4 + 5;
			if (k1 < -23)
				break;
			byte data = 0;
			if (s != null && s.startsWith("@cr1@")) {
				s = s.substring(5);
				data = 1;
			} else if (s != null && s.startsWith("@cr2@")) {
				s = s.substring(5);
				data = 2;
			} else if (s != null && s.startsWith("@cr3@")) {
				s = s.substring(5);
				data = 3;
			} else if (s != null && s.startsWith("@cr4@")) {
				s = s.substring(5);
				data = 4;
			} else if (s != null && s.startsWith("@cr5@")) {
				s = s.substring(5);
				data = 5;
			} else if (s != null && s.startsWith("@cr6@")) {
				s = s.substring(5);
				data = 6;
			} else if (s != null && s.startsWith("@cr7@")) {
				s = s.substring(5);
				data = 7;
			} else if (s != null && s.startsWith("@cr8@")) {
				s = s.substring(5);
				data = 8;
			} else if (s != null && s.startsWith("@cr9@")) {
				s = s.substring(5);
				data = 9;
			} else if (s != null && s.startsWith("@cr10@")) {
				s = s.substring(6);
				data = 10;
			}
			if ((j1 == 5 || j1 == 6) && (splitPrivateChat == 0 || chatTypeView == 2)
					&& (j1 == 6 || privateChatMode == 0
					|| privateChatMode == 1 && isFriendOrSelf(s)))
				l++;
			if ((j1 == 3 || j1 == 7) && (splitPrivateChat == 0 || chatTypeView == 2)
					&& (j1 == 7 || privateChatMode == 0
					|| privateChatMode == 1 && isFriendOrSelf(s))) {
				if (j > k1 - 14 && j <= k1) {
					if (!isFriendOrSelf(s)) {
						menuActionText[menuActionRow] = "Add ignore @whi@" + s;
						menuActionTypes[menuActionRow] = 42;
						menuActionRow++;
						menuActionText[menuActionRow] = "Add friend @whi@" + s;
						menuActionTypes[menuActionRow] = 337;
						menuActionRow++;
					} else {
						menuActionText[menuActionRow] = "Message @whi@" + s;
						menuActionTypes[menuActionRow] = 2639;
						menuActionRow++;
					}
				}
				l++;
			}
		}
	}

	private void buildDuelorTrade(int j) {
		int l = 0;
		for (int i1 = 0; i1 < 500; i1++) {
			if (chatMessages[i1] == null)
				continue;
			if (chatTypeView != 3 && chatTypeView != 4)
				continue;
			int j1 = chatTypes[i1];
			String s = chatNames[i1];
			int k1 = (70 - l * 14 + 42) + anInt1089 + 4 + 5;
			if (k1 < -23)
				break;
			byte data = 0;
			if (s != null && s.startsWith("@cr1@")) {
				s = s.substring(5);
				data = 1;
			} else if (s != null && s.startsWith("@cr2@")) {
				s = s.substring(5);
				data = 2;
			} else if (s != null && s.startsWith("@cr3@")) {
				s = s.substring(5);
				data = 3;
			} else if (s != null && s.startsWith("@cr4@")) {
				s = s.substring(5);
				data = 4;
			} else if (s != null && s.startsWith("@cr5@")) {
				s = s.substring(5);
				data = 5;
			} else if (s != null && s.startsWith("@cr6@")) {
				s = s.substring(5);
				data = 6;
			} else if (s != null && s.startsWith("@cr7@")) {
				s = s.substring(5);
				data = 7;
			} else if (s != null && s.startsWith("@cr8@")) {
				s = s.substring(5);
				data = 8;
			} else if (s != null && s.startsWith("@cr9@")) {
				s = s.substring(5);
				data = 9;
			} else if (s != null && s.startsWith("@cr10@")) {
				s = s.substring(6);
				data = 10;
			}
			if (chatTypeView == 3 && j1 == 4
					&& (tradeMode == 0 || tradeMode == 1 && isFriendOrSelf(s))) {
				if (j > k1 - 14 && j <= k1) {
					menuActionText[menuActionRow] = "Accept trade @whi@" + s;
					menuActionTypes[menuActionRow] = 484;
					menuActionRow++;
				}
				l++;
			}
			if (chatTypeView == 4 && j1 == 8
					&& (tradeMode == 0 || tradeMode == 1 && isFriendOrSelf(s))) {
				if (j > k1 - 14 && j <= k1) {
					menuActionText[menuActionRow] = "Accept challenge @whi@" + s;
					menuActionTypes[menuActionRow] = 6;
					menuActionRow++;
				}
				l++;
			}
			if (j1 == 12) {
				if (j > k1 - 14 && j <= k1) {
					menuActionText[menuActionRow] = "Go-to @blu@" + s;
					menuActionTypes[menuActionRow] = 915;
					menuActionRow++;
				}
				l++;
			}
		}
	}

	private void buildChatAreaMenu(int j) {
		int l = 0;
		for (int i1 = 0; i1 < 500; i1++) {
			if (chatMessages[i1] == null)
				continue;
			int j1 = chatTypes[i1];
			int k1 = (70 - l * 14 + 42) + anInt1089 + 4 + 5;
			String s = chatNames[i1];
			if (chatTypeView == 1) {
				buildPublicChat(j);
				break;
			}
			if (chatTypeView == 2) {
				buildFriendChat(j);
				break;
			}
			if (chatTypeView == 3 || chatTypeView == 4) {
				buildDuelorTrade(j);
				break;
			}
			if (chatTypeView == 5) {
				break;
			}
			byte data = 0;
			if (s != null && s.startsWith("@cr1@")) {
				s = s.substring(5);
				data = 1;
			} else if (s != null && s.startsWith("@cr2@")) {
				s = s.substring(5);
				data = 2;
			} else if (s != null && s.startsWith("@cr3@")) {
				s = s.substring(5);
				data = 3;
			} else if (s != null && s.startsWith("@cr4@")) {
				s = s.substring(5);
				data = 4;
			} else if (s != null && s.startsWith("@cr5@")) {
				s = s.substring(5);
				data = 5;
			} else if (s != null && s.startsWith("@cr6@")) {
				s = s.substring(5);
				data = 6;
			} else if (s != null && s.startsWith("@cr7@")) {
				s = s.substring(5);
				data = 7;
			} else if (s != null && s.startsWith("@cr8@")) {
				s = s.substring(5);
				data = 8;
			} else if (s != null && s.startsWith("@cr9@")) {
				s = s.substring(5);
				data = 9;
			} else if (s != null && s.startsWith("@cr10@")) {
				s = s.substring(6);
				data = 10;
			}
			if(s == null) {
				continue;
			}
			if (j1 == 0)
				l++;
			if ((j1 == 1 || j1 == 2) && (j1 == 1 || publicChatMode == 0
					|| publicChatMode == 1 && isFriendOrSelf(s))) {
				if (j > k1 - 14 && j <= k1 && !s.equals(localPlayer.name)) {
					if(!isFriendOrSelf(s)) {
						menuActionText[menuActionRow] = "Add ignore @whi@" + s;
						menuActionTypes[menuActionRow] = 42;
						menuActionRow++;
						menuActionText[menuActionRow] = "Add friend @whi@" + s;
						menuActionTypes[menuActionRow] = 337;
						menuActionRow++;
					} else {
						menuActionText[menuActionRow] = "Message @whi@" + s;
						menuActionTypes[menuActionRow] = 2639;
						menuActionRow++;
					}
				}
				l++;
			}
			if ((j1 == 3 || j1 == 7) && splitPrivateChat == 0
					&& (j1 == 7 || privateChatMode == 0
					|| privateChatMode == 1 && isFriendOrSelf(s))) {
				if (j > k1 - 14 && j <= k1) {
					if(!isFriendOrSelf(s)) {
						menuActionText[menuActionRow] = "Add ignore @whi@" + s;
						menuActionTypes[menuActionRow] = 42;
						menuActionRow++;
						menuActionText[menuActionRow] = "Add friend @whi@" + s;
						menuActionTypes[menuActionRow] = 337;
						menuActionRow++;
					} else {
						menuActionText[menuActionRow] = "Message @whi@" + s;
						menuActionTypes[menuActionRow] = 2639;
						menuActionRow++;
					}
				}
				l++;
			}
			if (j1 == 4 && (tradeMode == 0 || tradeMode == 1 && isFriendOrSelf(s))) {
				if (j > k1 - 14 && j <= k1) {
					menuActionText[menuActionRow] = "Accept trade @whi@" + s;
					menuActionTypes[menuActionRow] = 484;
					menuActionRow++;
				}
				l++;
			}
			if ((j1 == 5 || j1 == 6) && splitPrivateChat == 0 && privateChatMode < 2)
				l++;
			if (j1 == 8 && (tradeMode == 0 || tradeMode == 1 && isFriendOrSelf(s))) {
				if (j > k1 - 14 && j <= k1) {
					menuActionText[menuActionRow] = "Accept challenge @whi@" + s;
					menuActionTypes[menuActionRow] = 6;
					menuActionRow++;
				}
				l++;
			}
		}
	}

	public int getLevelForXP(int exp) {
		int points = 0;
		int output = 0;

		if (exp > 13034430) {
			return 99;
		}

		for (int lvl = 1; lvl <= 99; lvl++) {
			points += Math.floor(lvl + 300.0 * Math.pow(2.0, lvl / 7.0));
			output = (int) Math.floor(points / 4);

			if (output >= exp) {
				return lvl;
			}
		}

		return 0;
	}

	/**
	 * interface_handle_auto_content
	 */
	private void drawFriendsListOrWelcomeScreen(Widget widget) {
		int index = widget.contentType;
		if (index >= 1 && index <= 100 || index >= 701 && index <= 800) {
			if (index == 1 && friendServerStatus == 0) {
				widget.defaultText = "Loading friend list";
				widget.atActionType = 0;
				return;
			}
			if (index == 1 && friendServerStatus == 1) {
				widget.defaultText = "Connecting to friendserver";
				widget.atActionType = 0;
				return;
			}
			if (index == 2 && friendServerStatus != 2) {
				widget.defaultText = "Please wait...";
				widget.atActionType = 0;
				return;
			}
			int k = friendsCount;
			if (friendServerStatus != 2)
				k = 0;
			if (index > 700)
				index -= 601;
			else
				index--;
			if (index >= k) {
				widget.defaultText = "";
				widget.atActionType = 0;
				return;
			} else {
				widget.defaultText = friendsList[index];
				widget.atActionType = 1;
				return;
			}
		}
		if (index >= 101 && index <= 200 || index >= 801 && index <= 900) {
			int l = friendsCount;
			if (friendServerStatus != 2)
				l = 0;
			if (index > 800)
				index -= 701;
			else
				index -= 101;
			if (index >= l) {
				widget.defaultText = "";
				widget.atActionType = 0;
				return;
			}
			if (friendsNodeIDs[index] == 0)
				widget.defaultText = "@red@Offline";
			else if (friendsNodeIDs[index] == nodeID)
				widget.defaultText = "@gre@Online"/* + (friendsNodeIDs[j] - 9) */;
			else
				widget.defaultText = "@red@Offline"/* + (friendsNodeIDs[j] - 9) */;
			widget.atActionType = 1;
			return;
		}

		if (index == 203) {
			int i1 = friendsCount;
			if (friendServerStatus != 2)
				i1 = 0;
			widget.scrollMax = i1 * 15 + 20;
			if (widget.scrollMax <= widget.height)
				widget.scrollMax = widget.height + 1;
			return;
		}
		if (index >= 401 && index <= 500) {
			if ((index -= 401) == 0 && friendServerStatus == 0) {
				widget.defaultText = "Loading ignore list";
				widget.atActionType = 0;
				return;
			}
			if (index == 1 && friendServerStatus == 0) {
				widget.defaultText = "Please wait...";
				widget.atActionType = 0;
				return;
			}
			int j1 = ignoreCount;
			if (friendServerStatus == 0)
				j1 = 0;
			if (index >= j1) {
				widget.defaultText = "";
				widget.atActionType = 0;
				return;
			} else {
				widget.defaultText = StringUtils.formatText(
						StringUtils.decodeBase37(ignoreListAsLongs[index]));
				widget.atActionType = 1;
				return;
			}
		}
		if (index == 503) {
			widget.scrollMax = ignoreCount * 15 + 20;
			if (widget.scrollMax <= widget.height)
				widget.scrollMax = widget.height + 1;
			return;
		}
		if (index == 327) {
			widget.modelRotation1 = 150;
			widget.modelRotation2 = (int) (Math.sin((double) tick / 40D) * 256D) & 0x7ff;
			if (aBoolean1031) {
				for (int k1 = 0; k1 < 7; k1++) {
					int l1 = anIntArray1065[k1];
					if (l1 >= 0 && !IdentityKit.kits[l1].bodyLoaded())
						return;
				}

				aBoolean1031 = false;
				Model aclass30_sub2_sub4_sub6s[] = new Model[7];
				int i2 = 0;
				for (int j2 = 0; j2 < 7; j2++) {
					int k2 = anIntArray1065[j2];
					if (k2 >= 0)
						aclass30_sub2_sub4_sub6s[i2++] =
						IdentityKit.kits[k2].bodyModel();
				}

				Model model = new Model(i2, aclass30_sub2_sub4_sub6s);
				for (int l2 = 0; l2 < 5; l2++)
					if (characterDesignColours[l2] != 0) {
						model.recolor(PLAYER_BODY_RECOLOURS[l2][0],
								PLAYER_BODY_RECOLOURS[l2][characterDesignColours[l2]]);
						if (l2 == 1)
							model.recolor(anIntArray1204[0],
									anIntArray1204[characterDesignColours[l2]]);
					}

				model.skin();
				model.applyTransform(Animation.animations[localPlayer.idleAnimation].primaryFrames[0]);
				model.light(64, 850, -30, -50, -30, true);
				widget.defaultMediaType = 5;
				widget.defaultMedia = 0;
				Widget.method208(aBoolean994, model);
			}
			return;
		}
		if (index == 328) {
			Widget rsInterface = widget;
			int verticleTilt = 150;
			int animationSpeed = (int) (Math.sin((double) tick / 40D) * 256D) & 0x7ff;
			rsInterface.modelRotation1 = verticleTilt;
			rsInterface.modelRotation2 = animationSpeed;
			if (aBoolean1031) {
				Model characterDisplay = localPlayer.getAnimatedModel();
				for (int l2 = 0; l2 < 5; l2++)
					if (characterDesignColours[l2] != 0) {
						characterDisplay.recolor(PLAYER_BODY_RECOLOURS[l2][0],
								PLAYER_BODY_RECOLOURS[l2][characterDesignColours[l2]]);
						if (l2 == 1)
							characterDisplay.recolor(anIntArray1204[0],
									anIntArray1204[characterDesignColours[l2]]);
					}
				int staticFrame = localPlayer.idleAnimation;
				characterDisplay.skin();
				characterDisplay.applyTransform(Animation.animations[staticFrame].primaryFrames[0]);
				// characterDisplay.light(64, 850, -30, -50, -30, true);
				rsInterface.defaultMediaType = 5;
				rsInterface.defaultMedia = 0;
				Widget.method208(aBoolean994, characterDisplay);
			}
			return;
		}
		if (index == 324) {
			if (aClass30_Sub2_Sub1_Sub1_931 == null) {
				aClass30_Sub2_Sub1_Sub1_931 = widget.disabledSprite;
				aClass30_Sub2_Sub1_Sub1_932 = widget.enabledSprite;
			}
			if (maleCharacter) {
				widget.disabledSprite = aClass30_Sub2_Sub1_Sub1_932;
				return;
			} else {
				widget.disabledSprite = aClass30_Sub2_Sub1_Sub1_931;
				return;
			}
		}
		if (index == 325) {
			if (aClass30_Sub2_Sub1_Sub1_931 == null) {
				aClass30_Sub2_Sub1_Sub1_931 = widget.disabledSprite;
				aClass30_Sub2_Sub1_Sub1_932 = widget.enabledSprite;
			}
			if (maleCharacter) {
				widget.disabledSprite = aClass30_Sub2_Sub1_Sub1_931;
				return;
			} else {
				widget.disabledSprite = aClass30_Sub2_Sub1_Sub1_932;
				return;
			}
		}
		if (index == 600) {
			widget.defaultText = reportAbuseInput;
			if (tick % 20 < 10) {
				widget.defaultText += "|";
				return;
			} else {
				widget.defaultText += " ";
				return;
			}
		}
		if (index == 613)
			if (myPrivilege >= 1) {
				if (canMute) {
					widget.textColor = 0xff0000;
					widget.defaultText =
							"Moderator option: Mute player for 48 hours: <ON>";
				} else {
					widget.textColor = 0xffffff;
					widget.defaultText =
							"Moderator option: Mute player for 48 hours: <OFF>";
				}
			} else {
				widget.defaultText = "";
			}
		if (index == 650 || index == 655)
			if (anInt1193 != 0) {
				String s;
				if (daysSinceLastLogin == 0)
					s = "earlier today";
				else if (daysSinceLastLogin == 1)
					s = "yesterday";
				else
					s = daysSinceLastLogin + " days ago";
				widget.defaultText = "You last logged in " + s + " from: " + SignLink.dns;
			} else {
				widget.defaultText = "";
			}
		if (index == 651) {
			if (unreadMessages == 0) {
				widget.defaultText = "0 unread messages";
				widget.textColor = 0xffff00;
			}
			if (unreadMessages == 1) {
				widget.defaultText = "1 unread defaultText";
				widget.textColor = 65280;
			}
			if (unreadMessages > 1) {
				widget.defaultText = unreadMessages + " unread messages";
				widget.textColor = 65280;
			}
		}
		if (index == 652)
			if (daysSinceRecovChange == 201) {
				if (membersInt == 1)
					widget.defaultText =
					"@yel@This is a non-members world: @whi@Since you are a member we";
				else
					widget.defaultText = "";
			} else if (daysSinceRecovChange == 200) {
				widget.defaultText =
						"You have not yet set any password recovery questions.";
			} else {
				String s1;
				if (daysSinceRecovChange == 0)
					s1 = "Earlier today";
				else if (daysSinceRecovChange == 1)
					s1 = "Yesterday";
				else
					s1 = daysSinceRecovChange + " days ago";
				widget.defaultText = s1 + " you changed your recovery questions";
			}
		if (index == 653)
			if (daysSinceRecovChange == 201) {
				if (membersInt == 1)
					widget.defaultText =
					"@whi@recommend you use a members world instead. You may use";
				else
					widget.defaultText = "";
			} else if (daysSinceRecovChange == 200)
				widget.defaultText =
				"We strongly recommend you do so now to secure your account.";
			else
				widget.defaultText =
				"If you do not remember making this change then cancel it immediately";
		if (index == 654) {
			if (daysSinceRecovChange == 201)
				if (membersInt == 1) {
					widget.defaultText =
							"@whi@this world but member benefits are unavailable whilst here.";
					return;
				} else {
					widget.defaultText = "";
					return;
				}
			if (daysSinceRecovChange == 200) {
				widget.defaultText =
						"Do this from the 'account management' area on our front webpage";
				return;
			}
			widget.defaultText =
					"Do this from the 'account management' area on our front webpage";
		}
	}

	private void drawSplitPrivateChat() {
		if (splitPrivateChat == 0) {
			return;
		}
		GameFont textDrawingArea = regularText;
		int i = 0;
		 if (AnnouncementBool) { 
			  i = 1; 
		 }
		if (systemUpdateTime != 0) {
			i = 1;
		}
		for (int j = 0; j < 100; j++) {
			if (chatMessages[j] != null) {
				int k = chatTypes[j];
				String s = chatNames[j];
				byte data = 0;
				if (s != null && s.startsWith("@cr1@")) {
					s = s.substring(5);
					data = 1;
				} else if (s != null && s.startsWith("@cr2@")) {
					s = s.substring(5);
					data = 2;
				} else if (s != null && s.startsWith("@cr3@")) {
					s = s.substring(5);
					data = 3;
				} else if (s != null && s.startsWith("@cr4@")) {
					s = s.substring(5);
					data = 4;
				} else if (s != null && s.startsWith("@cr5@")) {
					s = s.substring(5);
					data = 5;
				} else if (s != null && s.startsWith("@cr6@")) {
					s = s.substring(5);
					data = 6;
				} else if (s != null && s.startsWith("@cr7@")) {
					s = s.substring(5);
					data = 7;
				} else if (s != null && s.startsWith("@cr8@")) {
					s = s.substring(5);
					data = 8;
				} else if (s != null && s.startsWith("@cr9@")) {
					s = s.substring(5);
					data = 9;
				} else if (s != null && s.startsWith("@cr10@")) {
					s = s.substring(6);
					data = 10;
				}
				if ((k == 3 || k == 7) && (k == 7 || privateChatMode == 0
						|| privateChatMode == 1 && isFriendOrSelf(s))) {
					int l = 329 - i * 13;
					if (frameMode != ScreenMode.FIXED) {
						l = frameHeight - 170 - i * 13;
					}
					int k1 = 4;
					textDrawingArea.render(0, "From", l, k1);
					textDrawingArea.render(65535, "From", l - 1, k1);
					k1 += textDrawingArea.getTextWidth("From ");
					if (data > 0 && data < 10) {
						modIcons[data - 1].drawSprite(k1, l - 12);
						k1 += 12;
					}
					textDrawingArea.render(0, s + ": " + chatMessages[j], l, k1);
					textDrawingArea.render(65535, s + ": " + chatMessages[j], l - 1, k1);
					if (++i >= 5) {
						return;
					}
				}
				if (k == 5 && privateChatMode < 2) {
					int i1 = 329 - i * 13;
					if (frameMode != ScreenMode.FIXED) {
						i1 = frameHeight - 170 - i * 13;
					}
					textDrawingArea.render(0, chatMessages[j], i1, 4);
					textDrawingArea.render(65535, chatMessages[j], i1 - 1, 4);
					if (++i >= 5) {
						return;
					}
				}
				if (k == 6 && privateChatMode < 2) {
					int j1 = 329 - i * 13;
					if (frameMode != ScreenMode.FIXED) {
						j1 = frameHeight - 170 - i * 13;
					}
					textDrawingArea.render(0, "To " + s + ": " + chatMessages[j], j1, 4);
					textDrawingArea.render(65535, "To " + s + ": " + chatMessages[j],
							j1 - 1, 4);
					if (++i >= 5) {
						return;
					}
				}
			}
		}
	}

	public void sendMessage(String message, int type, String name) {

		if (type == 0 && dialogueId != -1) {
			clickToContinueString = message;
			super.clickMode3 = 0;
		}

		if (backDialogueId == -1) {
			updateChatbox = true;
		}

		for (int index = 499; index > 0; index--) {            	
			chatTypes[index] = chatTypes[index - 1];
			chatNames[index] = chatNames[index - 1];
			chatMessages[index] = chatMessages[index - 1];
			chatRights[index] = chatRights[index - 1];
		}

		chatTypes[0] = type;
		chatNames[0] = name;
		chatMessages[0] = message;
		chatRights[0] = rights;
	}

	public static void setTab(int id) {
		tabId = id;
		tabAreaAltered = true;
	}

	private final void minimapHovers() {
		final boolean fixed = frameMode == ScreenMode.FIXED;
		
		hpHover = fixed ? hpHover = super.mouseX >= 516 && super.mouseX <= 571 && super.mouseY >= 47 && super.mouseY < 72 : 
			super.mouseX >= frameWidth - 220 && super.mouseX <= frameWidth - 160 && super.mouseY >= 42 && super.mouseY < 74;
		
		prayHover = fixed ? prayHover = super.mouseX >= 518 && super.mouseX <= 572 && super.mouseY >= 85 && super.mouseY < 117 : 
			super.mouseX >= frameWidth - 220 && super.mouseX <= frameWidth - 160 && super.mouseY >= 85 && super.mouseY < 117;
		
		
		runHover = fixed ? runHover = super.mouseX >= 540 && super.mouseX <= 593 && super.mouseY >= 123 && super.mouseY < 154 : 
			super.mouseX >= frameWidth - 193 && super.mouseX <= frameWidth - 137 && super.mouseY >= 123 && super.mouseY < 153;
	
		worldHover = fixed ? super.mouseX >= 710 && super.mouseX <= 738 && super.mouseY >= 130 && super.mouseY <= 154 :
			super.mouseX >= frameWidth - 35 && super.mouseX <= frameWidth - 10 && super.mouseY >= 133 && super.mouseY <= 160;
		
		//specialHover = fixed ? super.mouseX >= 686 && super.mouseX <= 742 && super.mouseY >= 124 && super.mouseY <= 156 : 
		//	super.mouseX >= frameWidth - 58 && super.mouseX <= frameWidth && super.mouseY >= 151 && super.mouseY <= 179;
		
			expCounterHover = frameMode == ScreenMode.FIXED ? super.mouseX >= 515 && super.mouseX <= 540 && super.mouseY >= 20 && super.mouseY <= 45 : super.mouseX >= frameWidth - 215 && super.mouseX <= frameWidth - 190 && super.mouseY >= 20 && super.mouseY <= 45;
	}

	private final int[] tabClickX = {38, 33, 33, 33, 33, 33, 38, 38, 33, 33, 33, 33, 33, 38},
			tabClickStart = {522, 560, 593, 625, 659, 692, 724, 522, 560, 593, 625, 659, 692,
					724},
			tabClickY = {169, 169, 169, 169, 169, 169, 169, 466, 466, 466, 466, 466, 466,
					466};
	

	private void processTabClick() {
		if (super.clickMode3 == 1) {
			if (frameMode == ScreenMode.FIXED
					|| frameMode != ScreenMode.FIXED && !changeTabArea) {
				int xOffset = frameMode == ScreenMode.FIXED ? 0 : frameWidth - 765;
				int yOffset = frameMode == ScreenMode.FIXED ? 0 : frameHeight - 503;
				for (int i = 0; i < tabClickX.length; i++) {
					if (super.mouseX >= tabClickStart[i] + xOffset
							&& super.mouseX <= tabClickStart[i] + tabClickX[i]
									+ xOffset
									&& super.mouseY >= tabClickY[i] + yOffset
									&& super.mouseY < tabClickY[i] + 37 + yOffset
									&& tabInterfaceIDs[i] != -1) {
						tabId = i;
						tabAreaAltered = true;

						//Spawn tab
						if(tabId == 2) {
							searchingSpawnTab = true;
						} else {
							searchingSpawnTab = false;
						}

						break;
					}
				}
			} else if (changeTabArea && frameWidth < 1000) {
				if (super.saveClickX >= frameWidth - 226
						&& super.saveClickX <= frameWidth - 195
						&& super.saveClickY >= frameHeight - 72
						&& super.saveClickY < frameHeight - 40
						&& tabInterfaceIDs[0] != -1) {
					if (tabId == 0) {
						showTabComponents = !showTabComponents;
					} else {
						showTabComponents = true;
					}
					tabId = 0;
					tabAreaAltered = true;

				}
				if (super.saveClickX >= frameWidth - 194
						&& super.saveClickX <= frameWidth - 163
						&& super.saveClickY >= frameHeight - 72
						&& super.saveClickY < frameHeight - 40
						&& tabInterfaceIDs[1] != -1) {
					if (tabId == 1) {
						showTabComponents = !showTabComponents;
					} else {
						showTabComponents = true;
					}
					tabId = 1;
					tabAreaAltered = true;

				}
				if (super.saveClickX >= frameWidth - 162
						&& super.saveClickX <= frameWidth - 131
						&& super.saveClickY >= frameHeight - 72
						&& super.saveClickY < frameHeight - 40
						&& tabInterfaceIDs[2] != -1) {
					if (tabId == 2) {
						showTabComponents = !showTabComponents;
					} else {
						showTabComponents = true;
					}
					tabId = 2;
					tabAreaAltered = true;

				}
				if (super.saveClickX >= frameWidth - 129
						&& super.saveClickX <= frameWidth - 98
						&& super.saveClickY >= frameHeight - 72
						&& super.saveClickY < frameHeight - 40
						&& tabInterfaceIDs[3] != -1) {
					if (tabId == 3) {
						showTabComponents = !showTabComponents;
					} else {
						showTabComponents = true;
					}
					tabId = 3;
					tabAreaAltered = true;

				}
				if (super.saveClickX >= frameWidth - 97
						&& super.saveClickX <= frameWidth - 66
						&& super.saveClickY >= frameHeight - 72
						&& super.saveClickY < frameHeight - 40
						&& tabInterfaceIDs[4] != -1) {
					if (tabId == 4) {
						showTabComponents = !showTabComponents;
					} else {
						showTabComponents = true;
					}
					tabId = 4;
					tabAreaAltered = true;

				}
				if (super.saveClickX >= frameWidth - 65
						&& super.saveClickX <= frameWidth - 34
						&& super.saveClickY >= frameHeight - 72
						&& super.saveClickY < frameHeight - 40
						&& tabInterfaceIDs[5] != -1) {
					if (tabId == 5) {
						showTabComponents = !showTabComponents;
					} else {
						showTabComponents = true;
					}
					tabId = 5;
					tabAreaAltered = true;

				}
				if (super.saveClickX >= frameWidth - 33 && super.saveClickX <= frameWidth
						&& super.saveClickY >= frameHeight - 72
						&& super.saveClickY < frameHeight - 40
						&& tabInterfaceIDs[6] != -1) {
					if (tabId == 6) {
						showTabComponents = !showTabComponents;
					} else {
						showTabComponents = true;
					}
					tabId = 6;
					tabAreaAltered = true;

				}

				if (super.saveClickX >= frameWidth - 194
						&& super.saveClickX <= frameWidth - 163
						&& super.saveClickY >= frameHeight - 37
						&& super.saveClickY < frameHeight - 0
						&& tabInterfaceIDs[8] != -1) {
					if (tabId == 8) {
						showTabComponents = !showTabComponents;
					} else {
						showTabComponents = true;
					}
					tabId = 8;
					tabAreaAltered = true;

				}
				if (super.saveClickX >= frameWidth - 162
						&& super.saveClickX <= frameWidth - 131
						&& super.saveClickY >= frameHeight - 37
						&& super.saveClickY < frameHeight - 0
						&& tabInterfaceIDs[9] != -1) {
					if (tabId == 9) {
						showTabComponents = !showTabComponents;
					} else {
						showTabComponents = true;
					}
					tabId = 9;
					tabAreaAltered = true;

				}
				if (super.saveClickX >= frameWidth - 129
						&& super.saveClickX <= frameWidth - 98
						&& super.saveClickY >= frameHeight - 37
						&& super.saveClickY < frameHeight - 0
						&& tabInterfaceIDs[10] != -1) {
					if (tabId == 7) {
						showTabComponents = !showTabComponents;
					} else {
						showTabComponents = true;
					}
					tabId = 7;
					tabAreaAltered = true;

				}
				if (super.saveClickX >= frameWidth - 97
						&& super.saveClickX <= frameWidth - 66
						&& super.saveClickY >= frameHeight - 37
						&& super.saveClickY < frameHeight - 0
						&& tabInterfaceIDs[11] != -1) {
					if (tabId == 11) {
						showTabComponents = !showTabComponents;
					} else {
						showTabComponents = true;
					}
					tabId = 11;
					tabAreaAltered = true;

				}
				if (super.saveClickX >= frameWidth - 65
						&& super.saveClickX <= frameWidth - 34
						&& super.saveClickY >= frameHeight - 37
						&& super.saveClickY < frameHeight - 0
						&& tabInterfaceIDs[12] != -1) {
					if (tabId == 12) {
						showTabComponents = !showTabComponents;
					} else {
						showTabComponents = true;
					}
					tabId = 12;
					tabAreaAltered = true;

				}
				if (super.saveClickX >= frameWidth - 33 && super.saveClickX <= frameWidth
						&& super.saveClickY >= frameHeight - 37
						&& super.saveClickY < frameHeight - 0
						&& tabInterfaceIDs[13] != -1) {
					if (tabId == 13) {
						showTabComponents = !showTabComponents;
					} else {
						showTabComponents = true;
					}
					tabId = 13;
					tabAreaAltered = true;

				}
			} else if (changeTabArea && frameWidth >= 1000) {
				if (super.mouseY >= frameHeight - 37 && super.mouseY <= frameHeight) {
					if (super.mouseX >= frameWidth - 417
							&& super.mouseX <= frameWidth - 386) {
						if (tabId == 0) {
							showTabComponents = !showTabComponents;
						} else {
							showTabComponents = true;
						}
						tabId = 0;
						tabAreaAltered = true;
					}
					if (super.mouseX >= frameWidth - 385
							&& super.mouseX <= frameWidth - 354) {
						if (tabId == 1) {
							showTabComponents = !showTabComponents;
						} else {
							showTabComponents = true;
						}
						tabId = 1;
						tabAreaAltered = true;
					}
					if (super.mouseX >= frameWidth - 353
							&& super.mouseX <= frameWidth - 322) {
						if (tabId == 2) {
							showTabComponents = !showTabComponents;
						} else {
							showTabComponents = true;
						}
						tabId = 2;
						tabAreaAltered = true;
					}
					if (super.mouseX >= frameWidth - 321
							&& super.mouseX <= frameWidth - 290) {
						if (tabId == 3) {
							showTabComponents = !showTabComponents;
						} else {
							showTabComponents = true;
						}
						tabId = 3;
						tabAreaAltered = true;
					}
					if (super.mouseX >= frameWidth - 289
							&& super.mouseX <= frameWidth - 258) {
						if (tabId == 4) {
							showTabComponents = !showTabComponents;
						} else {
							showTabComponents = true;
						}
						tabId = 4;
						tabAreaAltered = true;
					}
					if (super.mouseX >= frameWidth - 257
							&& super.mouseX <= frameWidth - 226) {
						if (tabId == 5) {
							showTabComponents = !showTabComponents;
						} else {
							showTabComponents = true;
						}
						tabId = 5;
						tabAreaAltered = true;
					}
					if (super.mouseX >= frameWidth - 225
							&& super.mouseX <= frameWidth - 194) {
						if (tabId == 6) {
							showTabComponents = !showTabComponents;
						} else {
							showTabComponents = true;
						}
						tabId = 6;
						tabAreaAltered = true;
					}
					if (super.mouseX >= frameWidth - 193
							&& super.mouseX <= frameWidth - 163) {
						if (tabId == 8) {
							showTabComponents = !showTabComponents;
						} else {
							showTabComponents = true;
						}
						tabId = 8;
						tabAreaAltered = true;
					}
					if (super.mouseX >= frameWidth - 162
							&& super.mouseX <= frameWidth - 131) {
						if (tabId == 9) {
							showTabComponents = !showTabComponents;
						} else {
							showTabComponents = true;
						}
						tabId = 9;
						tabAreaAltered = true;
					}
					if (super.mouseX >= frameWidth - 130
							&& super.mouseX <= frameWidth - 99) {
						if (tabId == 7) {
							showTabComponents = !showTabComponents;
						} else {
							showTabComponents = true;
						}
						tabId = 7;
						tabAreaAltered = true;
					}
					if (super.mouseX >= frameWidth - 98
							&& super.mouseX <= frameWidth - 67) {
						if (tabId == 11) {
							showTabComponents = !showTabComponents;
						} else {
							showTabComponents = true;
						}
						tabId = 11;
						tabAreaAltered = true;
					}
					if (super.mouseX >= frameWidth - 66
							&& super.mouseX <= frameWidth - 45) {
						if (tabId == 12) {
							showTabComponents = !showTabComponents;
						} else {
							showTabComponents = true;
						}
						tabId = 12;
						tabAreaAltered = true;
					}
					if (super.mouseX >= frameWidth - 31 && super.mouseX <= frameWidth) {
						if (tabId == 13) {
							showTabComponents = !showTabComponents;
						} else {
							showTabComponents = true;
						}
						tabId = 13;
						tabAreaAltered = true;
					}
				}
			}
		}
	}

	private void setupGameplayScreen() {
		if (chatboxImageProducer != null) {
			return;
		}

		nullLoader();
		super.fullGameScreen = null;
		topLeft1BackgroundTile = null;
		bottomLeft1BackgroundTile = null;
		loginBoxImageProducer = null;
		loginScreenAccessories = null;
		flameLeftBackground = null;
		flameRightBackground = null;
		bottomLeft0BackgroundTile = null;
		bottomRightImageProducer = null;
		loginMusicImageProducer = null;
		middleLeft1BackgroundTile = null;
		aRSImageProducer_1115 = null;
		chatboxImageProducer = new ProducingGraphicsBuffer(519, 165);// chatback
		minimapImageProducer = new ProducingGraphicsBuffer(249, 168);// mapback
		Rasterizer2D.clear();
		cacheSprite[19].drawSprite(0, 0);
		tabImageProducer = new ProducingGraphicsBuffer(249, 335);// inventory
		gameScreenImageProducer = new ProducingGraphicsBuffer(512, 334);// gamescreen
		Rasterizer2D.clear();
		chatSettingImageProducer = new ProducingGraphicsBuffer(249, 45);
		welcomeScreenRaised = true;
	}

	private void refreshMinimap(Sprite sprite, int j, int k) {
		int l = k * k + j * j;
		if (l > 4225 && l < 0x15f90) {
			int i1 = cameraHorizontal + minimapRotation & 0x7ff;
			int j1 = Model.SINE[i1];
			int k1 = Model.COSINE[i1];
			j1 = (j1 * 256) / (minimapZoom + 256);
			k1 = (k1 * 256) / (minimapZoom + 256);
		} else {
			markMinimap(sprite, k, j);
		}
	}

	public void rightClickChatButtons() {
		if (mouseY >= frameHeight - 22 && mouseY <= frameHeight) {
			if (super.mouseX >= 5 && super.mouseX <= 61) {
				menuActionText[1] = "View All";
				menuActionTypes[1] = 999;
				menuActionRow = 2;
			} else if (super.mouseX >= 71 && super.mouseX <= 127) {
				menuActionText[1] = "View Game";
				menuActionTypes[1] = 998;
				menuActionRow = 2;
			} else if (super.mouseX >= 137 && super.mouseX <= 193) {
				if(!sendingAutochat) {
					menuActionText[1] = "@aut@Setup your autochat";
					} else {
						menuActionText[1] = "@aut@Stop autochat";
					}
				menuActionTypes[1] = 912;
				menuActionText[2] = "Hide public";
				menuActionTypes[2] = 997;
				menuActionText[3] = "Off public";
				menuActionTypes[3] = 996;
				menuActionText[4] = "Friends public";
				menuActionTypes[4] = 995;
				menuActionText[5] = "On public";
				menuActionTypes[5] = 994;
				menuActionText[6] = "View public";
				menuActionTypes[6] = 993;
				menuActionRow = 6;
			} else if (super.mouseX >= 203 && super.mouseX <= 259) {
				menuActionText[1] = "Off private";
				menuActionTypes[1] = 992;
				menuActionText[2] = "Friends private";
				menuActionTypes[2] = 991;
				menuActionText[3] = "On private";
				menuActionTypes[3] = 990;
				menuActionText[4] = "View private";
				menuActionTypes[4] = 989;
				menuActionRow = 5;
			} else if (super.mouseX >= 269 && super.mouseX <= 325) {
				menuActionText[1] = "Off clan chat";
				menuActionTypes[1] = 1003;
				menuActionText[2] = "Friends clan chat";
				menuActionTypes[2] = 1002;
				menuActionText[3] = "On clan chat";
				menuActionTypes[3] = 1001;
				menuActionText[4] = "View clan chat";
				menuActionTypes[4] = 1000;
				menuActionRow = 5;
			} else if (super.mouseX >= 335 && super.mouseX <= 391) {
				menuActionText[1] = "Off trade";
				menuActionTypes[1] = 987;
				menuActionText[2] = "Friends trade";
				menuActionTypes[2] = 986;
				menuActionText[3] = "On trade";
				menuActionTypes[3] = 985;
				menuActionText[4] = "View trade";
				menuActionTypes[4] = 984;
				menuActionRow = 5;
			} else if (super.mouseX >= 404 && super.mouseX <= 515) {
				menuActionText[1] = "Report Abuse";
				menuActionTypes[1] = 606;
				menuActionRow = 2;
			}
		}
	}

	public void processRightClick() {
		if (activeInterfaceType != 0) {
			return;
		}
		menuActionText[0] = "Cancel";
		menuActionTypes[0] = 1107;
		menuActionRow = 1;
		if (showChatComponents) {
			buildSplitPrivateChatMenu();
		}
		anInt886 = 0;
		anInt1315 = 0;
		if (frameMode == ScreenMode.FIXED ) {			
			if (super.mouseX > 4 && super.mouseY > 4 && super.mouseX < 516 && super.mouseY < 338) {
				if (openInterfaceId != -1) {
					buildInterfaceMenu(4, Widget.interfaceCache[openInterfaceId], super.mouseX, 4, super.mouseY, 0);
				} else {
					createMenu();
				}
			}
		} else if (frameMode != ScreenMode.FIXED ) {
			if (getMousePositions()) {
				if (super.mouseX > (frameWidth / 2) - 356 && super.mouseY > (frameHeight / 2) - 230 && super.mouseX < ((frameWidth / 2) + 356) && super.mouseY < (frameHeight / 2) + 230 && openInterfaceId != -1) {
					buildInterfaceMenu((frameWidth / 2) - 356, Widget.interfaceCache[openInterfaceId], super.mouseX, (frameHeight / 2) - 230, super.mouseY, 0);
				} else {
					createMenu();
				}
			}
		}
		if (anInt886 != anInt1026) {
			anInt1026 = anInt886;
		}
		if (anInt1315 != anInt1129) {
			anInt1129 = anInt1315;
		}
		anInt886 = 0;
		anInt1315 = 0;
		if (!changeTabArea) {
			final int yOffset = frameMode == ScreenMode.FIXED ? 0 : frameHeight - 503;
			final int xOffset = frameMode == ScreenMode.FIXED ? 0 : frameWidth - 765;
			if (super.mouseX > 548 + xOffset && super.mouseX < 740 + xOffset
					&& super.mouseY > 207 + yOffset && super.mouseY < 468 + yOffset) {
				if (overlayInterfaceId != -1) {
					buildInterfaceMenu(548 + xOffset,
							Widget.interfaceCache[overlayInterfaceId], super.mouseX,
							207 + yOffset, super.mouseY, 0);
				} else if (tabInterfaceIDs[tabId] != -1) {
					buildInterfaceMenu(548 + xOffset,
							Widget.interfaceCache[tabInterfaceIDs[tabId]],
							super.mouseX, 207 + yOffset, super.mouseY, 0);
				}
			}
		} else if (changeTabArea) {
			final int yOffset = frameWidth >= 1000 ? 37 : 74;
			if (super.mouseX > frameWidth - 197 && super.mouseY > frameHeight - yOffset - 267
					&& super.mouseX < frameWidth - 7
					&& super.mouseY < frameHeight - yOffset - 7 && showTabComponents) {
				if (overlayInterfaceId != -1) {
					buildInterfaceMenu(frameWidth - 197,
							Widget.interfaceCache[overlayInterfaceId], super.mouseX,
							frameHeight - yOffset - 267, super.mouseY, 0);
				} else if (tabInterfaceIDs[tabId] != -1) {
					buildInterfaceMenu(frameWidth - 197,
							Widget.interfaceCache[tabInterfaceIDs[tabId]],
							super.mouseX, frameHeight - yOffset - 267, super.mouseY,
							0);
				}
			}
		}
		if (anInt886 != anInt1048) {
			tabAreaAltered = true;
			anInt1048 = anInt886;
		}
		if (anInt1315 != anInt1044) {
			tabAreaAltered = true;
			anInt1044 = anInt1315;
		}
		anInt886 = 0;
		anInt1315 = 0;
		if (backDialogueId == 40500) {
		if (super.mouseX > 0
				&& super.mouseY > (frameMode == ScreenMode.FIXED ? 338 : frameHeight - 165)
				&& super.mouseX < 513
				&& super.mouseY < (frameMode == ScreenMode.FIXED ? 470 : frameHeight - 40)
				&& showChatComponents) {
			if (backDialogueId == 40500) {
				buildInterfaceMenu(0, Widget.interfaceCache[backDialogueId], super.mouseX, (frameMode == ScreenMode.FIXED ? 340 : frameHeight - 165), super.mouseY, 0);
			}
			else if (backDialogueId != -1 && backDialogueId != 40500) {
				buildInterfaceMenu(20, Widget.interfaceCache[backDialogueId], super.mouseX, (frameMode == ScreenMode.FIXED ? 358 : frameHeight - 145), super.mouseY, 0);

			} else if (super.mouseY < (frameMode == ScreenMode.FIXED ? 463 : frameHeight - 40)
					&& super.mouseX < 490) {
				buildChatAreaMenu(super.mouseY
						- (frameMode == ScreenMode.FIXED ? 338 : frameHeight - 165));
			}
		}
		}
		if (backDialogueId != 40500) {
			if (super.mouseX > 0
					&& super.mouseY > (frameMode == ScreenMode.FIXED ? 338 : frameHeight - 165)
					&& super.mouseX < 490
					&& super.mouseY < (frameMode == ScreenMode.FIXED ? 463 : frameHeight - 40)
					&& showChatComponents) {
				if (backDialogueId == 40500) {
					buildInterfaceMenu(0, Widget.interfaceCache[backDialogueId], super.mouseX, (frameMode == ScreenMode.FIXED ? 340 : frameHeight - 165), super.mouseY, 0);
				}
				else if (backDialogueId != -1 && backDialogueId != 40500) {
					buildInterfaceMenu(20, Widget.interfaceCache[backDialogueId], super.mouseX, (frameMode == ScreenMode.FIXED ? 358 : frameHeight - 145), super.mouseY, 0);

				} else if (super.mouseY < (frameMode == ScreenMode.FIXED ? 463 : frameHeight - 40)
						&& super.mouseX < 490) {
					buildChatAreaMenu(super.mouseY
							- (frameMode == ScreenMode.FIXED ? 338 : frameHeight - 165));
				}
			}
		}
		if (backDialogueId != -1 && anInt886 != anInt1039) {
			updateChatbox = true;
			anInt1039 = anInt886;
		}
		if (backDialogueId != -1 && anInt1315 != anInt1500) {
			updateChatbox = true;
			anInt1500 = anInt1315;
		}
		if (super.mouseX > 4 && super.mouseY > 480 && super.mouseX < 516
				&& super.mouseY < frameHeight) {
			rightClickChatButtons();
		}
		processMinimapActions();
		boolean flag = false;
		while (!flag) {
			flag = true;
			for (int j = 0; j < menuActionRow - 1; j++) {
				if (menuActionTypes[j] < 1000 && menuActionTypes[j + 1] > 1000) {
					String s = menuActionText[j];
					menuActionText[j] = menuActionText[j + 1];
					menuActionText[j + 1] = s;
					int k = menuActionTypes[j];
					menuActionTypes[j] = menuActionTypes[j + 1];
					menuActionTypes[j + 1] = k;
					k = firstMenuAction[j];
					firstMenuAction[j] = firstMenuAction[j + 1];
					firstMenuAction[j + 1] = k;
					k = secondMenuAction[j];
					secondMenuAction[j] = secondMenuAction[j + 1];
					secondMenuAction[j + 1] = k;
					k = selectedMenuActions[j];
					selectedMenuActions[j] = selectedMenuActions[j + 1];
					selectedMenuActions[j + 1] = k;
					flag = false;
				}
			}
		}
	}

	private int method83(int i, int j, int k) {
		int l = 256 - k;
		return ((i & 0xff00ff) * l + (j & 0xff00ff) * k & 0xff00ff00)
				+ ((i & 0xff00) * l + (j & 0xff00) * k & 0xff0000) >> 8;
	}

	/**
	 * The login method for the 317 protocol.
	 * 
	 * @param name The name of the user trying to login.
	 * @param password The password of the user trying to login.
	 * @param reconnecting The flag for the user indicating to attempt to reconnect.
	 */
	private void login(String name, String password, boolean reconnecting) {
		SignLink.setError(name);
		try {
			if(name.length() < 3) {
				firstLoginMessage = "";
				secondLoginMessage = "Your username is too short.";
				return;
			}
			if(password.length() < 3) {
				firstLoginMessage = "";
				secondLoginMessage = "Your password is too short.";
				return;
			}
			if (!reconnecting) {
				firstLoginMessage = "";
				secondLoginMessage = "Connecting to server...";
				drawLoginScreen(true);
			}

			outgoing = ByteBuffer.create(5000, false, null);
			socketStream = new BufferedConnection(this,
					openSocket(Configuration.server_port + portOffset));


			outgoing.putByte(14); //REQUEST
			outgoing.putByte(0); // nameHash byte (server ignores this)
			socketStream.queueBytes(2, outgoing.getBuffer());


			// Read 17-byte handshake response: 8 zeros + response code + 8-byte server seed
			socketStream.flushInputStream(incoming.payload, 17);
			incoming.currentPosition = 0;
			for (int i = 0; i < 8; i++) {
				incoming.readUnsignedByte(); // Skip 8 zeros
			}
			int response = incoming.readUnsignedByte(); // Response code

			int copy = response;

			//Our encryption for outgoing messages for this player's session
			IsaacCipher cipher = null;

			if (response == 0) {
				serverSeed = incoming.readLong(); // aka server session key
				
				// Generate 64-bit client session key
				long clientSessionKey = ((long) (Math.random() * 99999999D) << 32) 
						| (long) (Math.random() * 99999999D);
				
				// Build RSA block (plaintext, no actual RSA encryption per protocol)
				outgoing.resetPosition();
				outgoing.putByte(10); // RSA Packet ID
				outgoing.putLong(clientSessionKey); // Client session key (8 bytes)
				outgoing.putLong(serverSeed); // Server session key (8 bytes)
				outgoing.putByte(10); // Server string: empty, just newline terminator (0x0A)
				outgoing.putString(name); // Username (newline-terminated)
				outgoing.putString(password); // Password (newline-terminated)
				int rsaBlockSize = outgoing.getPosition();

				// Build login packet
				login.currentPosition = 0;
				login.writeByte(reconnecting ? 18 : 16); // Login type
				// Calculate login packet size: RSA_MAGIC(1) + version(2) + lowMem(1) + CRC keys(36) + RSA block size(1) + rsaBlockSize
				int loginPacketSize = 1 + 2 + 1 + 36 + 1 + rsaBlockSize;
				login.writeByte(loginPacketSize); // Login packet size
				login.writeByte(255); // RSA_MAGIC
				login.writeShort(Configuration.CLIENT_VERSION); // Client version (317)
				login.writeByte(lowMemory ? 1 : 0); // Low memory flag
				// Write 36 bytes of CRC keys (9 x 4-byte ints, all zeros is acceptable)
				for (int i = 0; i < 9; i++) {
					login.writeInt(0);
				}
				login.writeByte(rsaBlockSize); // RSA block size
				login.writeBytes(outgoing.getBuffer(), rsaBlockSize, 0); // RSA block (plaintext)
				
				// Setup ISAAC ciphers per protocol spec
				int seed[] = new int[4];
				seed[0] = (int) (clientSessionKey >>> 32);
				seed[1] = (int) clientSessionKey;
				seed[2] = (int) (serverSeed >>> 32);
				seed[3] = (int) serverSeed;
				cipher = new IsaacCipher(seed); // Client's out-cipher
				// Server's out-cipher (our in-cipher) adds 50 to each seed element
				for (int index = 0; index < 4; index++)
					seed[index] += 50;
				encryption = new IsaacCipher(seed);
				socketStream.queueBytes(login.currentPosition, login.payload);
				response = socketStream.read();
			}

			outgoing = ByteBuffer.create(5000, true, cipher);

			if (response == 1) {
				try {
					Thread.sleep(2000L);
				} catch (Exception _ex) {
				}
				login(name, password, reconnecting);
				return;
			}
			if (response == 2) {
				myPrivilege = socketStream.read();
				//flagged = socketStream.read() == 1;
				spawnType = SpawnTabType.INVENTORY;
				searchSyntax = "";
				fetchSearchResults = true;
				currentSkill = -1;
				totalExp = 0L;
				aLong1220 = 0L;
				mouseDetection.coordsIndex = 0;
				super.awtFocus = true;
				aBoolean954 = true;
				loggedIn = true;
				outgoing = ByteBuffer.create(5000, true, cipher);
				incoming.currentPosition = 0;
				opcode = -1;
				lastOpcode = -1;
				secondLastOpcode = -1;
				thirdLastOpcode = -1;
				packetSize = 0;
				timeoutCounter = 0;
				systemUpdateTime = 0;
				anInt1011 = 0;
				hintIconDrawType = 0;
				menuActionRow = 0;
				menuOpen = false;
				super.idleTime = 0;
				for (int index = 0; index < 100; index++)
					chatMessages[index] = null;
				itemSelected = 0;
				spellSelected = 0;
				loadingStage = 0;
				trackCount = 0;
				setNorth();
				minimapState = 0;
				lastKnownPlane = -1;
				destinationX = 0;
				destY = 0;
				playerCount = 0;
				npcCount = 0;
				for (int index = 0; index < maxPlayers; index++) {
					players[index] = null;
					playerSynchronizationBuffers[index] = null;
				}
				for (int index = 0; index < 16384; index++)
					npcs[index] = null;
				localPlayer = players[internalLocalPlayerIndex] = new Player();
				projectiles.clear();
				incompleteAnimables.clear();
				for (int z = 0; z < 4; z++) {
					for (int x = 0; x < 104; x++) {
						for (int y = 0; y < 104; y++)
							groundItems[z][x][y] = null;
					}
				}
				spawns = new Deque();
				fullscreenInterfaceID = -1;
				friendServerStatus = 0;
				friendsCount = 0;
				dialogueId = -1;
				backDialogueId = -1;
				openInterfaceId = -1;
				overlayInterfaceId = -1;
				openWalkableInterface = -1;
				continuedDialogue = false;
				tabId = 3;
				inputDialogState = 0;
				menuOpen = false;
				messagePromptRaised = false;
				clickToContinueString = null;
				multicombat = 0;
				flashingSidebarId = -1;
				maleCharacter = true;
				changeCharacterGender();
				for (int index = 0; index < 5; index++)
					characterDesignColours[index] = 0;
				for (int index = 0; index < 5; index++) {
					playerOptions[index] = null;
					playerOptionsHighPriority[index] = false;
				}
				anInt1175 = 0;
				anInt1134 = 0;
				anInt986 = 0;
				anInt1288 = 0;
				anInt924 = 0;
				anInt1188 = 0;
				anInt1155 = 0;
				anInt1226 = 0;
				// sendConfiguration(429, 1);
				this.stopMidi();
				setupGameplayScreen();

				return;
			}
			if (response == 28) {
				firstLoginMessage = "Username or password contains illegal";
				secondLoginMessage = "characters. Try other combinations.";
				return;
			}
			if (response == 30) {
				firstLoginMessage = "Old client usage detected.";
				secondLoginMessage = "Please download the latest one.";
				MiscUtils.launchURL("http://www.Dodian.net");
				return;
			}
			if (response == 3) {
				firstLoginMessage = "";
				secondLoginMessage = "Invalid username or password.";
				return;
			}
			if (response == 4) {
				firstLoginMessage = "Your account has been banned.";
				secondLoginMessage = "";
				return;
			}
			if(response == 22) {
				firstLoginMessage = "Your computer has been banned.";
				secondLoginMessage = "";
				return;
			}
			if(response == 27) {
				firstLoginMessage = "Your host-address has been banned.";
				secondLoginMessage = "";
				return;
			}
			if (response == 5) {
				firstLoginMessage = "Your account is already logged in.";
				secondLoginMessage = "Try again in 60 secs...";
				return;
			}
			if (response == 6) {
				firstLoginMessage = Configuration.CLIENT_NAME + " is being updated.";
				secondLoginMessage = "Try again in 60 secs...";
				return;
			}
			if (response == 7) {
				firstLoginMessage = "The world is currently full.";
				secondLoginMessage = "";
				return;
			}
			if (response == 8) {
				firstLoginMessage = "Unable to connect.";
				secondLoginMessage = "Login server offline.";
				return;
			}
			if (response == 9) {
				firstLoginMessage = "Login limit exceeded.";
				secondLoginMessage = "Too many connections from your address.";
				return;
			}
			if (response == 10) {
				firstLoginMessage = "Unable to connect. Bad session id.";
				secondLoginMessage = "Try again in 60 secs...";
				return;
			}
			if (response == 11) {
				secondLoginMessage = "Login server rejected session.";
				secondLoginMessage = "Try again in 60 secs...";
				return;
			}
			if (response == 12) {
				firstLoginMessage = "You need a members account to login to this world.";
				secondLoginMessage = "Please subscribe, or use a different world.";
				return;
			}
			if (response == 13) {
				firstLoginMessage = "Could not complete login.";
				secondLoginMessage = "Please try using a different world.";
				return;
			}
			if (response == 14) {
				firstLoginMessage = "The server is being updated.";
				secondLoginMessage = "Please wait 1 minute and try again.";
				return;
			}
			if (response == 15) {
				loggedIn = true;
				incoming.currentPosition = 0;
				opcode = -1;
				lastOpcode = -1;
				secondLastOpcode = -1;
				thirdLastOpcode = -1;
				packetSize = 0;
				timeoutCounter = 0;
				systemUpdateTime = 0;
				menuActionRow = 0;
				menuOpen = false;
				loadingStartTime = System.currentTimeMillis();
				return;
			}
			if (response == 16) {
				firstLoginMessage = "Login attempts exceeded.";
				secondLoginMessage = "Please wait 1 minute and try again.";
				return;
			}
			if (response == 17) {
				firstLoginMessage = "You are standing in a members-only area.";
				secondLoginMessage = "To play on this world move to a free area first";
				return;
			}
			if (response == 20) {
				firstLoginMessage = "Invalid loginserver requested";
				secondLoginMessage = "Please try using a different world.";
				return;
			}
			if (response == 21) {
				for (int k1 = socketStream.read(); k1 >= 0; k1--) {
					firstLoginMessage = "You have only just left another world";
					secondLoginMessage =
							"Your profile will be transferred in: " + k1 + " seconds";
					drawLoginScreen(true);
					try {
						Thread.sleep(1000L);
					} catch (Exception _ex) {
					}
				}
				login(name, password, reconnecting);
				return;
			}
			if (response == 22) {
				firstLoginMessage = "Your computer has been UUID banned.";
				secondLoginMessage = "Please appeal on the forums.";
				return;
			}
			if (response == -1) {
				if (copy == 0) {
					if (loginFailures < 2) {
						try {
							Thread.sleep(2000L);
						} catch (Exception _ex) {
						}
						loginFailures++;
						login(name, password, reconnecting);
						return;
					} else {
						firstLoginMessage = "No response from loginserver";
						secondLoginMessage = "Please wait 1 minute and try again.";
						return;
					}
				} else {
					firstLoginMessage = "No response from server";
					secondLoginMessage = "Please try using a different world.";
					return;
				}
			} else {
				firstLoginMessage = "Unexpected server response";
				secondLoginMessage = "Please try using a different world.";
				return;
			}
		} catch (IOException _ex) {
			firstLoginMessage = "";
		} catch (Exception e) {
			System.out.println("Error while generating uid. Skipping step.");
			e.printStackTrace();
		}
		secondLoginMessage = "Error connecting to server.";
	}

	private boolean doWalkTo(int type, int j, int k, int i1, int j1, int k1, int l1, int i2, int j2,
			boolean flag, int k2) {
		try {
			byte byte0 = 104;
			byte byte1 = 104;
			for (int l2 = 0; l2 < byte0; l2++) {
				for (int i3 = 0; i3 < byte1; i3++) {
					anIntArrayArray901[l2][i3] = 0;
					anIntArrayArray825[l2][i3] = 0x5f5e0ff;
				}
			}
			int j3 = j2;
			int k3 = j1;
			anIntArrayArray901[j2][j1] = 99;
			anIntArrayArray825[j2][j1] = 0;
			int l3 = 0;
			int i4 = 0;
			bigX[l3] = j2;
			bigY[l3++] = j1;
			boolean flag1 = false;
			int j4 = bigX.length;
			int ai[][] = collisionMaps[plane].adjacencies;
			while (i4 != l3) {
				j3 = bigX[i4];
				k3 = bigY[i4];
				i4 = (i4 + 1) % j4;
				if (j3 == k2 && k3 == i2) {
					flag1 = true;
					break;
				}
				if (i1 != 0) {
					if ((i1 < 5 || i1 == 10)
							&& collisionMaps[plane].method219(k2, j3, k3, j, i1 - 1, i2)) {
						flag1 = true;
						break;
					}
					if (i1 < 10 && collisionMaps[plane].method220(k2, i2, k3, i1 - 1, j, j3)) {
						flag1 = true;
						break;
					}
				}
				if (k1 != 0 && k != 0
						&& collisionMaps[plane].method221(i2, k2, j3, k, l1, k1, k3)) {
					flag1 = true;
					break;
				}
				int l4 = anIntArrayArray825[j3][k3] + 1;
				if (j3 > 0 && anIntArrayArray901[j3 - 1][k3] == 0
						&& (ai[j3 - 1][k3] & 0x1280108) == 0) {
					bigX[l3] = j3 - 1;
					bigY[l3] = k3;
					l3 = (l3 + 1) % j4;
					anIntArrayArray901[j3 - 1][k3] = 2;
					anIntArrayArray825[j3 - 1][k3] = l4;
				}
				if (j3 < byte0 - 1 && anIntArrayArray901[j3 + 1][k3] == 0
						&& (ai[j3 + 1][k3] & 0x1280180) == 0) {
					bigX[l3] = j3 + 1;
					bigY[l3] = k3;
					l3 = (l3 + 1) % j4;
					anIntArrayArray901[j3 + 1][k3] = 8;
					anIntArrayArray825[j3 + 1][k3] = l4;
				}
				if (k3 > 0 && anIntArrayArray901[j3][k3 - 1] == 0
						&& (ai[j3][k3 - 1] & 0x1280102) == 0) {
					bigX[l3] = j3;
					bigY[l3] = k3 - 1;
					l3 = (l3 + 1) % j4;
					anIntArrayArray901[j3][k3 - 1] = 1;
					anIntArrayArray825[j3][k3 - 1] = l4;
				}
				if (k3 < byte1 - 1 && anIntArrayArray901[j3][k3 + 1] == 0
						&& (ai[j3][k3 + 1] & 0x1280120) == 0) {
					bigX[l3] = j3;
					bigY[l3] = k3 + 1;
					l3 = (l3 + 1) % j4;
					anIntArrayArray901[j3][k3 + 1] = 4;
					anIntArrayArray825[j3][k3 + 1] = l4;
				}
				if (j3 > 0 && k3 > 0 && anIntArrayArray901[j3 - 1][k3 - 1] == 0
						&& (ai[j3 - 1][k3 - 1] & 0x128010e) == 0
						&& (ai[j3 - 1][k3] & 0x1280108) == 0
						&& (ai[j3][k3 - 1] & 0x1280102) == 0) {
					bigX[l3] = j3 - 1;
					bigY[l3] = k3 - 1;
					l3 = (l3 + 1) % j4;
					anIntArrayArray901[j3 - 1][k3 - 1] = 3;
					anIntArrayArray825[j3 - 1][k3 - 1] = l4;
				}
				if (j3 < byte0 - 1 && k3 > 0 && anIntArrayArray901[j3 + 1][k3 - 1] == 0
						&& (ai[j3 + 1][k3 - 1] & 0x1280183) == 0
						&& (ai[j3 + 1][k3] & 0x1280180) == 0
						&& (ai[j3][k3 - 1] & 0x1280102) == 0) {
					bigX[l3] = j3 + 1;
					bigY[l3] = k3 - 1;
					l3 = (l3 + 1) % j4;
					anIntArrayArray901[j3 + 1][k3 - 1] = 9;
					anIntArrayArray825[j3 + 1][k3 - 1] = l4;
				}
				if (j3 > 0 && k3 < byte1 - 1 && anIntArrayArray901[j3 - 1][k3 + 1] == 0
						&& (ai[j3 - 1][k3 + 1] & 0x1280138) == 0
						&& (ai[j3 - 1][k3] & 0x1280108) == 0
						&& (ai[j3][k3 + 1] & 0x1280120) == 0) {
					bigX[l3] = j3 - 1;
					bigY[l3] = k3 + 1;
					l3 = (l3 + 1) % j4;
					anIntArrayArray901[j3 - 1][k3 + 1] = 6;
					anIntArrayArray825[j3 - 1][k3 + 1] = l4;
				}
				if (j3 < byte0 - 1 && k3 < byte1 - 1 && anIntArrayArray901[j3 + 1][k3 + 1] == 0
						&& (ai[j3 + 1][k3 + 1] & 0x12801e0) == 0
						&& (ai[j3 + 1][k3] & 0x1280180) == 0
						&& (ai[j3][k3 + 1] & 0x1280120) == 0) {
					bigX[l3] = j3 + 1;
					bigY[l3] = k3 + 1;
					l3 = (l3 + 1) % j4;
					anIntArrayArray901[j3 + 1][k3 + 1] = 12;
					anIntArrayArray825[j3 + 1][k3 + 1] = l4;
				}
			}
			anInt1264 = 0;
			if (!flag1) {
				if (flag) {
					int i5 = 100;
					for (int k5 = 1; k5 < 2; k5++) {
						for (int i6 = k2 - k5; i6 <= k2 + k5; i6++) {
							for (int l6 = i2 - k5; l6 <= i2 + k5; l6++) {
								if (i6 >= 0 && l6 >= 0 && i6 < 104 && l6 < 104
										&& anIntArrayArray825[i6][l6] < i5) {
									i5 = anIntArrayArray825[i6][l6];
									j3 = i6;
									k3 = l6;
									anInt1264 = 1;
									flag1 = true;
								}
							}
						}
						if (flag1)
							break;
					}
				}
				if (!flag1)
					return false;
			}
			i4 = 0;
			bigX[i4] = j3;
			bigY[i4++] = k3;
			int l5;
			for (int j5 = l5 = anIntArrayArray901[j3][k3]; j3 != j2 || k3 != j1; j5 =
					anIntArrayArray901[j3][k3]) {
				if (j5 != l5) {
					l5 = j5;
					bigX[i4] = j3;
					bigY[i4++] = k3;
				}
				if ((j5 & 2) != 0)
					j3++;
				else if ((j5 & 8) != 0)
					j3--;
				if ((j5 & 1) != 0)
					k3++;
				else if ((j5 & 4) != 0)
					k3--;
			}
			if (i4 > 0) {
				int k4 = i4;
				if (k4 > 25)
					k4 = 25;
				i4--;
				int k6 = bigX[i4];
				int i7 = bigY[i4];
				anInt1288 += k4;
				if (anInt1288 >= 92) {
					/*Anti-cheatValidates, walking. Not used. OUTPUT_BUFFER.createFrame(36);
					OUTPUT_BUFFER.writeDWord(0);*/
					anInt1288 = 0;
				}
				sendPacket(new UpdatePlane(plane));

				final int k5 = k4;
				final int i_4 = i4;
				final int keyArr = super.keyArray[5] != 1 ? 0 : 1;
				final int testX = 0;
				OutgoingPacket movementPacket = new OutgoingPacket() {
					@Override
					public void buildPacket(ByteBuffer buf) {

						if (type == 0) {
							buf.putOpcode(164);
							buf.putByte(k5 + k5 + 3);
						} else if (type == 1) {
							buf.putOpcode(248);
							buf.putByte(k5 + k5 + 3);
						} else if (type == 2) {
							buf.putOpcode(98);
							buf.putByte(k5 + k5 + 3);
						}

						buf.resetPosition();
						buf.writeSignedBigEndian(k6 + regionBaseX);
						destinationX = bigX[0];
						destY = bigY[0];
						int i__4 = i_4;
						for (int j7 = 1; j7 < k5; j7++) {
							i__4--;
							buf.putByte(bigX[i__4] - k6);
							buf.putByte(bigY[i__4] - i7);
							
							
						}
						buf.writeUnsignedWordBigEndian(i7 + regionBaseY);
						buf.method424(keyArr);
						
						
					}
				};
				sendPacket(movementPacket);

				return true;
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		return type != 1;
	}

	private void npcUpdateMask(Buffer stream) {
		for (int j = 0; j < mobsAwaitingUpdateCount; j++) {
			int k = mobsAwaitingUpdate[j];
			Npc npc = npcs[k];
			int mask = stream.readUnsignedByte();
			if ((mask & 0x10) != 0) {
				int i1 = stream.readLEUShort();
				if (i1 == 65535)
					i1 = -1;
				int i2 = stream.readUnsignedByte();
				if (i1 == npc.emoteAnimation && i1 != -1) {
					int l2 = Animation.animations[i1].replayMode;
					if (l2 == 1) {
						npc.displayedEmoteFrames = 0;
						npc.emoteTimeRemaining = 0;
						npc.animationDelay = i2;
						npc.currentAnimationLoops = 0;
					}
					if (l2 == 2)
						npc.currentAnimationLoops = 0;
				} else if (i1 == -1 || npc.emoteAnimation == -1
						|| Animation.animations[i1].forcedPriority >= Animation.animations[npc.emoteAnimation].forcedPriority) {
					npc.emoteAnimation = i1;
					npc.displayedEmoteFrames = 0;
					npc.emoteTimeRemaining = 0;
					npc.animationDelay = i2;
					npc.currentAnimationLoops = 0;
					npc.anInt1542 = npc.remainingPath;
				}
			}
			if ((mask & 0x80) != 0) {
				npc.graphic = stream.readUShort();
				int k1 = stream.readInt();
				npc.graphicHeight = k1 >> 16;
						npc.graphicDelay = tick + (k1 & 0xffff);
						npc.currentAnimation = 0;
						npc.anInt1522 = 0;
						if (npc.graphicDelay > tick)
							npc.currentAnimation = -1;
						if (npc.graphic == 65535)
							npc.graphic = -1;
			}
			if ((mask & 8) != 0) {
				int damage = stream.readShort();
				int type = stream.readUnsignedByte();
				int hp = stream.readShort();
				int maxHp = stream.readShort();                   
				npc.updateHitData(type, damage, tick);
				npc.loopCycleStatus = tick + 300;
				npc.currentHealth = hp;
				npc.maxHealth = maxHp;
			}
			if ((mask & 0x20) != 0) {
				npc.interactingEntity = stream.readUShort();
				if (npc.interactingEntity == 65535)
					npc.interactingEntity = -1;
			}
			if ((mask & 1) != 0) {
				npc.spokenText = stream.readString();
				npc.textCycle = 100;
			}
			if ((mask & 0x40) != 0) {                	  
				int damage = stream.readShort();
				int type = stream.readUnsignedByte();
				int hp = stream.readShort();
				int maxHp = stream.readShort();
				npc.updateHitData(type, damage, tick);
				npc.loopCycleStatus = tick + 300;
				npc.currentHealth = hp;
				npc.maxHealth = maxHp;
			}
			if ((mask & 2) != 0) {
				npc.headIcon = stream.readUnsignedByte();
				boolean transform = stream.readUnsignedByte() == 1;

				if(transform) {
					npc.desc = NpcDefinition.lookup(stream.readLEUShortA());
					npc.size = npc.desc.size;
					npc.degreesToTurn = npc.desc.degreesToTurn;
					npc.walkAnimIndex = npc.desc.walkAnim;
					npc.turn180AnimIndex = npc.desc.turn180AnimIndex;
					npc.turn90CWAnimIndex = npc.desc.turn90CWAnimIndex;
					npc.turn90CCWAnimIndex = npc.desc.turn90CCWAnimIndex;
					npc.idleAnimation = npc.desc.standAnim;
				}
			}
			if ((mask & 4) != 0) {
				npc.faceX = stream.readLEUShort();
				npc.faceY = stream.readLEUShort();
			}
		}
	}

	private void buildAtNPCMenu(NpcDefinition entityDef, int i, int j, int k) {
		if (openInterfaceId == 15244) {
			return;
		}
		if (menuActionRow >= 400)
			return;
		if (entityDef.childrenIDs != null)
			entityDef = entityDef.morph();
		if (entityDef == null)
			return;
		if (!entityDef.clickable)
			return;
		String s = entityDef.name;

		if (entityDef.combatLevel != 0)
			s = s + combatDiffColor(localPlayer.combatLevel, entityDef.combatLevel)
			+ " (level-" + entityDef.combatLevel + ")";
		if (itemSelected == 1) {
			menuActionText[menuActionRow] = "Use " + selectedItemName + " with @yel@" + s;
			menuActionTypes[menuActionRow] = 582;
			selectedMenuActions[menuActionRow] = i;
			firstMenuAction[menuActionRow] = k;
			secondMenuAction[menuActionRow] = j;
			menuActionRow++;
			return;
		}
		if (spellSelected == 1) {
			if ((spellUsableOn & 2) == 2) {
				menuActionText[menuActionRow] = spellTooltip + " @yel@" + s;
				menuActionTypes[menuActionRow] = 413;
				selectedMenuActions[menuActionRow] = i;
				firstMenuAction[menuActionRow] = k;
				secondMenuAction[menuActionRow] = j;
				menuActionRow++;
			}
		} else {
			if (entityDef.actions != null) {
				for (int l = 4; l >= 0; l--)
					if (entityDef.actions[l] != null
					&& !entityDef.actions[l].equalsIgnoreCase("attack")) {
						menuActionText[menuActionRow] =
								entityDef.actions[l] + " @yel@" + s;
						if (l == 0)
							menuActionTypes[menuActionRow] = 20;
						if (l == 1)
							menuActionTypes[menuActionRow] = 412;
						if (l == 2)
							menuActionTypes[menuActionRow] = 225;
						if (l == 3)
							menuActionTypes[menuActionRow] = 965;
						if (l == 4)
							menuActionTypes[menuActionRow] = 478;
						selectedMenuActions[menuActionRow] = i;
						firstMenuAction[menuActionRow] = k;
						secondMenuAction[menuActionRow] = j;
						menuActionRow++;
					}

			}
			if (entityDef.actions != null) {
				for (int i1 = 4; i1 >= 0; i1--)
					if (entityDef.actions[i1] != null
					&& entityDef.actions[i1].equalsIgnoreCase("attack")) {
						char c = '\0';
						if(!Configuration.alwaysLeftClickAttack) {
							if (entityDef.combatLevel > localPlayer.combatLevel)
								c = '\u07D0';
						}
						menuActionText[menuActionRow] =
								entityDef.actions[i1] + " @yel@" + s;
						if (i1 == 0)
							menuActionTypes[menuActionRow] = 20 + c;
						if (i1 == 1)
							menuActionTypes[menuActionRow] = 412 + c;
						if (i1 == 2)
							menuActionTypes[menuActionRow] = 225 + c;
						if (i1 == 3)
							menuActionTypes[menuActionRow] = 965 + c;
						if (i1 == 4)
							menuActionTypes[menuActionRow] = 478 + c;
						selectedMenuActions[menuActionRow] = i;
						firstMenuAction[menuActionRow] = k;
						secondMenuAction[menuActionRow] = j;
						menuActionRow++;
					}

			}
			if ((myPrivilege >= 2 && myPrivilege <= 4)) {
				menuActionText[menuActionRow] = "Examine @yel@" + s + " @gre@(@whi@"
						+ entityDef.interfaceType + "@gre@)";
			} else {
				menuActionText[menuActionRow] = "Examine @yel@" + s;
			}
			menuActionTypes[menuActionRow] = 1025;
			selectedMenuActions[menuActionRow] = i;
			firstMenuAction[menuActionRow] = k;
			secondMenuAction[menuActionRow] = j;
			menuActionRow++;
		}
	}

	private void buildAtPlayerMenu(int i, int j, Player player, int k) {
		if (openInterfaceId == 15244) {
			return;
		}
		if (player == localPlayer)
			return;
		if (menuActionRow >= 400)
			return;
		String s;
		if (player.skill == 0)
			s = player.name + combatDiffColor(localPlayer.combatLevel, player.combatLevel)
			+ " (level-" + player.combatLevel + ")";
		else
			s = player.name + " (skill-" + player.skill + ")";
		if (itemSelected == 1) {
			menuActionText[menuActionRow] = "Use " + selectedItemName + " with @whi@" + s;
			menuActionTypes[menuActionRow] = 491;
			selectedMenuActions[menuActionRow] = j;
			firstMenuAction[menuActionRow] = i;
			secondMenuAction[menuActionRow] = k;
			menuActionRow++;
		} else if (spellSelected == 1) {
			if ((spellUsableOn & 8) == 8) {
				menuActionText[menuActionRow] = spellTooltip + " @whi@" + s;
				menuActionTypes[menuActionRow] = 365;
				selectedMenuActions[menuActionRow] = j;
				firstMenuAction[menuActionRow] = i;
				secondMenuAction[menuActionRow] = k;
				menuActionRow++;
			}
		} else {
			for (int type = 4; type >= 0; type--)  {               	  
				if (playerOptions[type] != null) {
					menuActionText[menuActionRow] = playerOptions[type] + " @whi@" + s;
					char c = '\0';
					if (playerOptions[type].equalsIgnoreCase("attack")) {
						
						if(!Configuration.alwaysLeftClickAttack) {
							if (player.combatLevel > localPlayer.combatLevel)
								c = '\u07D0';
						}
						
						if (localPlayer.team != 0 && player.team != 0)
							if (localPlayer.team == player.team) {
								c = '\u07D0';
							} else {
								c = '\0';
							}
					} else if (playerOptionsHighPriority[type])
						c = '\u07D0';
					if (type == 0) {
						menuActionTypes[menuActionRow] = 561 + c;
					}
					if (type == 1) {
						menuActionTypes[menuActionRow] = 779 + c;
					}
					if (type == 2) {
						menuActionTypes[menuActionRow] = 27 + c;
					}
					if (type == 3) {
						menuActionTypes[menuActionRow] = 577 + c;
					}
					if (type == 4) {
						menuActionTypes[menuActionRow] = 729 + c;
					}
					selectedMenuActions[menuActionRow] = j;
					firstMenuAction[menuActionRow] = i;
					secondMenuAction[menuActionRow] = k;
					menuActionRow++;
				}
			}
		}
		for (int row = 0; row < menuActionRow; row++) {            	
			if (menuActionTypes[row] == 519) {
				menuActionText[row] = "Walk here @whi@" + s;
				return;
			}
		}
	}

	private void method89(SpawnedObject class30_sub1) {
		int i = 0;
		int j = -1;
		int k = 0;
		int l = 0;
		if (class30_sub1.group == 0)
			i = scene.getWallObjectUid(class30_sub1.plane, class30_sub1.x, class30_sub1.y);
		if (class30_sub1.group == 1)
			i = scene.getWallDecorationUid(class30_sub1.plane, class30_sub1.x,
					class30_sub1.y);
		if (class30_sub1.group == 2)
			i = scene.getGameObjectUid(class30_sub1.plane, class30_sub1.x,
					class30_sub1.y);
		if (class30_sub1.group == 3)
			i = scene.getGroundDecorationUid(class30_sub1.plane, class30_sub1.x,
					class30_sub1.y);
		if (i != 0) {
			int i1 = scene.getMask(class30_sub1.plane, class30_sub1.x, class30_sub1.y, i);
			j = i >> 14 & 0x7fff;
		k = i1 & 0x1f;
		l = i1 >> 6;
		}
		class30_sub1.getPreviousId = j;
		class30_sub1.previousType = k;
		class30_sub1.previousOrientation = l;
	}

	void startUp() {
		drawLoadingText(20, "Starting up");
		if (SignLink.cache_dat != null) {
			for (int i = 0; i < 5; i++)
				indices[i] = new FileStore(SignLink.cache_dat, SignLink.indices[i], i + 1);
		}
		try {

			if (Configuration.JAGCACHED_ENABLED) {
				JagGrab.onStart();
			} else {
			//	CacheDownloader.init(false);
			}

			titleArchive = createArchive(JagGrab.TITLE_CRC, "title screen", "title", JagGrab.CRCs[JagGrab.TITLE_CRC], 25);        
			smallText = new GameFont(false, "p11_full", titleArchive);
			regularText = new GameFont(false, "p12_full", titleArchive);
			boldText = new GameFont(false, "b12_full", titleArchive);
			newSmallFont = new RSFont(false, "p11_full", titleArchive);
			newRegularFont = new RSFont(false, "p12_full", titleArchive);
			newBoldFont = new RSFont(false, "b12_full", titleArchive);
			newFancyFont = new RSFont(true, "q8_full", titleArchive);
			newFancyFont2 = new RSFont(true, "q8_large", titleArchive);
			gameFont = new GameFont(true, "q8_full", titleArchive);			

			drawLogo();
			loadTitleScreen();
			FileArchive configArchive = createArchive(JagGrab.CONFIG_CRC, "config", "config", JagGrab.CRCs[JagGrab.CONFIG_CRC], 30);                  
			FileArchive interfaceArchive = createArchive(JagGrab.INTERFACE_CRC, "interface", "interface", JagGrab.CRCs[JagGrab.INTERFACE_CRC], 35);                  
			FileArchive mediaArchive = createArchive(JagGrab.MEDIA_CRC, "2d graphics", "media", JagGrab.CRCs[JagGrab.MEDIA_CRC], 40);
			FileArchive streamLoader_6 = createArchive(JagGrab.UPDATE_CRC, "update list", "versionlist", JagGrab.CRCs[JagGrab.UPDATE_CRC], 60);
			this.mediaStreamLoader = mediaArchive; 
			FileArchive textureArchive = createArchive(JagGrab.TEXTURES_CRC, "textures", "textures", JagGrab.CRCs[JagGrab.TEXTURES_CRC], 45);
			FileArchive wordencArchive = createArchive(JagGrab.CHAT_CRC, "chat system", "wordenc", JagGrab.CRCs[JagGrab.CHAT_CRC], 50);

			FileArchive soundArchive = createArchive(JagGrab.SOUNDS_CRC, "sound effects", "sounds", JagGrab.CRCs[JagGrab.SOUNDS_CRC], 55);

			tileFlags = new byte[4][104][104];
			tileHeights = new int[4][105][105];
			scene = new SceneGraph(tileHeights);

			for (int j = 0; j < 4; j++)
				collisionMaps[j] = new CollisionMap();

			minimapImage = new Sprite(512, 512);			
			drawLoadingText(60, "Connecting to update server");
			
			Frame.animationlist = new Frame[3000][0];
			resourceProvider = new ResourceProvider();
			resourceProvider.initialize(streamLoader_6, this);
			Model.method459(resourceProvider.getModelCount(), resourceProvider);
			drawLoadingText(80, "Unpacking media");

			byte soundData[] = soundArchive.readFile("sounds.dat");
			Buffer stream = new Buffer(soundData);
			Track.unpack(stream);

			if (Configuration.repackIndexOne) {
				CacheUtils.repackCacheIndex(this, Store.MODEL);
			}

			if (Configuration.repackIndexTwo) {
				CacheUtils.repackCacheIndex(this, Store.ANIMATION);
			}

			if (Configuration.repackIndexThree) {
				CacheUtils.repackCacheIndex(this, Store.MUSIC);
			}

			if (Configuration.repackIndexFour) {
				CacheUtils.repackCacheIndex(this, Store.MAP);
			}

			if (Configuration.dumpIndexOne) {
				CacheUtils.dumpCacheIndex(this, Store.MODEL);
			}

			if (Configuration.dumpIndexTwo) {
				CacheUtils.dumpCacheIndex(this, Store.ANIMATION);
			}

			if (Configuration.dumpIndexThree) {
				CacheUtils.dumpCacheIndex(this, Store.MUSIC);
			}

			if (Configuration.dumpIndexFour) {
				CacheUtils.dumpCacheIndex(this, Store.MAP);
			}

			SpriteLoader.loadSprites();
			cacheSprite = SpriteLoader.getSprites();
			if(gameFrame != null) {
				gameFrame.setIcon(Sprite.create(SpriteLoader.getData(454)));
			}
			
			SkillOrbs.init();
			this.hp = cacheSprite[40];

			for (int imageId = 73, index = 0; index < SkillConstants.SKILL_COUNT; imageId++, index++) {
				skill_sprites[index] = cacheSprite[imageId];
			}
			multiOverlay = new Sprite(mediaArchive, "overlay_multiway", 0);
			mapBack = new IndexedImage(mediaArchive, "mapback", 0);
			for (int j3 = 0; j3 <= 14; j3++)
				sideIcons[j3] = new Sprite(mediaArchive, "sideicons", j3);
			compass = new Sprite(mediaArchive, "compass", 0);
			try {
				for (int k3 = 0; k3 < 100; k3++)
					mapScenes[k3] = new IndexedImage(mediaArchive, "mapscene", k3);
			} catch (Exception _ex) {
			}
			try {
				for (int l3 = 0; l3 < 100; l3++)
					mapFunctions[l3] = new Sprite(mediaArchive, "mapfunction", l3);
			} catch (Exception _ex) {
			}
			try {
				for (int i4 = 0; i4 < 20; i4++)
					hitMarks[i4] = new Sprite(mediaArchive, "hitmarks", i4);
			} catch (Exception _ex) {
			}
			try {
				for (int h1 = 0; h1 < 6; h1++)
					headIconsHint[h1] = new Sprite(mediaArchive, "headicons_hint", h1);
			} catch (Exception _ex) {
			}
			try {
				for (int j4 = 0; j4 < 8; j4++)
					headIcons[j4] = new Sprite(mediaArchive, "headicons_prayer", j4);
				for (int j45 = 0; j45 < 3; j45++)
					skullIcons[j45] = new Sprite(mediaArchive, "headicons_pk", j45);
			} catch (Exception _ex) {
			}
			mapFlag = new Sprite(mediaArchive, "mapmarker", 0);
			mapMarker = new Sprite(mediaArchive, "mapmarker", 1);
			for (int k4 = 0; k4 < 8; k4++)
				crosses[k4] = new Sprite(mediaArchive, "cross", k4);
			mapDotItem = new Sprite(mediaArchive, "mapdots", 0);
			mapDotNPC = new Sprite(mediaArchive, "mapdots", 1);
			mapDotPlayer = new Sprite(mediaArchive, "mapdots", 2);
			mapDotFriend = new Sprite(mediaArchive, "mapdots", 3);
			mapDotTeam = new Sprite(mediaArchive, "mapdots", 4);
			mapDotClan = new Sprite(mediaArchive, "mapdots", 5);
			scrollBar1 = new Sprite(mediaArchive, "scrollbar", 0);
			scrollBar2 = new Sprite(mediaArchive, "scrollbar", 1);
			top508 = cacheSprite[499];
			bottom508 = cacheSprite[500];
			for (int l4 = 0; l4 < 12; l4++)
				modIcons[l4] = new Sprite(mediaArchive, "mod_icons", l4);
			Sprite[] clanIcons = new Sprite[9];
			for (int index = 0; index < clanIcons.length; index++) {
				//clanIcons[index] = new Sprite("Interfaces/Clan Chat/Icons/" + index);
			}

			RSFont.unpackImages(modIcons, clanIcons);
			Sprite sprite = new Sprite(mediaArchive, "screenframe", 0);
			leftFrame = new ProducingGraphicsBuffer(sprite.myWidth, sprite.myHeight);
			sprite.method346(0, 0);
			sprite = new Sprite(mediaArchive, "screenframe", 1);
			topFrame = new ProducingGraphicsBuffer(sprite.myWidth, sprite.myHeight);
			sprite.method346(0, 0);
			int i5 = (int) (Math.random() * 21D) - 10;
			int j5 = (int) (Math.random() * 21D) - 10;
			int k5 = (int) (Math.random() * 21D) - 10;
			int l5 = (int) (Math.random() * 41D) - 20;
			for (int i6 = 0; i6 < 100; i6++) {
				if (mapFunctions[i6] != null)
					mapFunctions[i6].method344(i5 + l5, j5 + l5, k5 + l5);
				if (mapScenes[i6] != null)
					mapScenes[i6].offsetColor(i5 + l5, j5 + l5, k5 + l5);
			}

			drawLoadingText(83, "Unpacking textures");
			Rasterizer3D.loadTextures(textureArchive);
			Rasterizer3D.setBrightness(0.80000000000000004D);
			Rasterizer3D.initiateRequestBuffers();
			drawLoadingText(86, "Unpacking config");
			Animation.init(configArchive);
			ObjectDefinition.init(configArchive);
			FloorDefinition.init(configArchive);
			NpcDefinition.init(configArchive);
			IdentityKit.init(configArchive);
			Graphic.init(configArchive);
			VariablePlayer.init(configArchive);
			VariableBits.init(configArchive);

			ItemDefinition.init(null, null);

			ItemDefinition.isMembers = isMembers;
			drawLoadingText(95, "Unpacking interfaces");
			GameFont gameFonts[] = {smallText, regularText, boldText, gameFont};
			Widget.load(interfaceArchive, gameFonts, mediaArchive);
			drawLoadingText(100, "Preparing game engine");
			for (int j6 = 0; j6 < 33; j6++) {
				int k6 = 999;
				int i7 = 0;
				for (int k7 = 0; k7 < 34; k7++) {
					if (mapBack.palettePixels[k7 + j6 * mapBack.width] == 0) {
						if (k6 == 999)
							k6 = k7;
						continue;
					}
					if (k6 == 999)
						continue;
					i7 = k7;
					break;
				}
				anIntArray968[j6] = k6;
				anIntArray1057[j6] = i7 - k6;
			}
			for (int l6 = 1; l6 < 153; l6++) {
				int j7 = 999;
				int l7 = 0;
				for (int j8 = 24; j8 < 177; j8++) {
					if (mapBack.palettePixels[j8 + l6 * mapBack.width] == 0
							&& (j8 > 34 || l6 > 34)) {
						if (j7 == 999) {
							j7 = j8;
						}
						continue;
					}
					if (j7 == 999) {
						continue;
					}
					l7 = j8;
					break;
				}
				minimapLeft[l6 - 1] = j7 - 24;
				minimapLineWidth[l6 - 1] = l7 - j7;
			} 
			setBounds();
			MessageCensor.load(wordencArchive);
			mouseDetection = new MouseDetection(this);
			startRunnable(mouseDetection, 10);
			SceneObject.clientInstance = this;
			ObjectDefinition.clientInstance = this;
			NpcDefinition.clientInstance = this;

			if(Configuration.JAGCACHED_ENABLED) {
			//	new Thread(new BackgroundRequester()).start();
			}

			loadPlayerData();
			updateSettings();

			//resourceProvider.writeAll();
			return;
		} catch (Exception exception) {
			exception.printStackTrace();
			SignLink.reporterror("loaderror " + aString1049 + " " + anInt1079);
		}
		loadingError = true;
	}

	private void updatePlayerList(Buffer stream, int packetSize) {
		while (stream.bitPosition + 10 < packetSize * 8) {
			int index = stream.readBits(11);                  
			if (index == 2047) {
				break;
			}
			if (players[index] == null) {
				players[index] = new Player();
				if (playerSynchronizationBuffers[index] != null) {
					players[index].updateAppearance(playerSynchronizationBuffers[index]);
				}
			}
			playerList[playerCount++] = index;
			Player player = players[index];
			player.time = tick;

			int update = stream.readBits(1);

			if (update == 1)
				mobsAwaitingUpdate[mobsAwaitingUpdateCount++] = index;

			int discardWalkingQueue = stream.readBits(1);

			int y = stream.readBits(5);

			if (y > 15) {
				y -= 32;
			}

			int x = stream.readBits(5);

			if (x > 15) {
				x -= 32;
			}
			
			player.setPos(localPlayer.pathX[0] + x, localPlayer.pathY[0] + y, discardWalkingQueue == 1);
		}
		stream.disableBitAccess();
	}

	public boolean inCircle(int circleX, int circleY, int clickX, int clickY, int radius) {
		return java.lang.Math.pow((circleX + radius - clickX), 2)
				+ java.lang.Math.pow((circleY + radius - clickY), 2) < java.lang.Math
				.pow(radius, 2);
	}

	private void processMainScreenClick() {
		if (openInterfaceId == 15244) {
			return;
		}
		if (minimapState != 0) {
			return;
		}
		if(specialHover) {
			return;
		}
		if (super.clickMode3 == 1) {
			int i = super.saveClickX - 25 - 547;
			int j = super.saveClickY - 5 - 3;
			if (frameMode != ScreenMode.FIXED) {
				i = super.saveClickX - (frameWidth - 182 + 24);
				j = super.saveClickY - 8;
			}
			if (inCircle(0, 0, i, j, 76) && mouseMapPosition() && !runHover) {
				i -= 73;
				j -= 75;
				int k = cameraHorizontal + minimapRotation & 0x7ff;
				int i1 = Rasterizer3D.anIntArray1470[k];
				int j1 = Rasterizer3D.COSINE[k];
				i1 = i1 * (minimapZoom + 256) >> 8;
		j1 = j1 * (minimapZoom + 256) >> 8;
			int k1 = j * i1 + i * j1 >> 11;
			int l1 = j * j1 - i * i1 >> 11;
				int i2 = localPlayer.x + k1 >> 7;
				int j2 = localPlayer.y - l1 >> 7;
				boolean flag1 = doWalkTo(1, 0, 0, 0, localPlayer.pathY[0], 0, 0, j2,
						localPlayer.pathX[0], true, i2);
				
				
				if (flag1) {

					/*outgoing.writeByte(i);
					outgoing.writeByte(j);
					outgoing.writeShort(cameraHorizontal);
					outgoing.writeByte(57);
					outgoing.writeByte(minimapRotation);
					outgoing.writeByte(minimapZoom);
					outgoing.writeByte(89);
					outgoing.writeShort(localPlayer.x);
					outgoing.writeShort(localPlayer.y);
					outgoing.writeByte(anInt1264);
					outgoing.writeByte(63);*/
				}
			}
			anInt1117++;
			if (anInt1117 > 1151) {
				anInt1117 = 0;
				// anti-cheat
				/*outgoing.writeOpcode(246);
				outgoing.writeByte(0);
				int bufPos = outgoing.currentPosition;                        

				if ((int) (Math.random() * 2D) == 0) {
					outgoing.writeByte(101);
				}

				outgoing.writeByte(197);
				outgoing.writeShort((int) (Math.random() * 65536D));
				outgoing.writeByte((int) (Math.random() * 256D));
				outgoing.writeByte(67);
				outgoing.writeShort(14214);

				if ((int) (Math.random() * 2D) == 0) {
					outgoing.writeShort(29487);
				}

				outgoing.writeShort((int) (Math.random() * 65536D));

				if ((int) (Math.random() * 2D) == 0) {
					outgoing.writeByte(220);
				}

				outgoing.writeByte(180);
				outgoing.writeBytes(outgoing.currentPosition - bufPos);*/
			}
		}
	}

	private String interfaceIntToString(int j) {
		if (j < 0x3b9ac9ff)
			return String.valueOf(format.format(j));
		else
			return "*";
	}

	private void showErrorScreen() {
		Graphics g = getGameComponent().getGraphics();
		g.setColor(Color.black);
		g.fillRect(0, 0, 765, 503);
		method4(1);
		if (loadingError) {
			aBoolean831 = false;
			g.setFont(new Font("Helvetica", 1, 16));
			g.setColor(Color.yellow);
			int k = 35;
			g.drawString("Sorry, an error has occured whilst loading "
					+ Configuration.CLIENT_NAME, 30, k);
			k += 50;
			g.setColor(Color.white);
			g.drawString("To fix this try the following (in order):", 30, k);
			k += 50;
			g.setColor(Color.white);
			g.setFont(new Font("Helvetica", 1, 12));
			g.drawString("1: Try closing ALL open web-browser windows, and reloading", 30, k);
			k += 30;
			g.drawString(
					"2: Try clearing your web-browsers cache from tools->internet options",
					30, k);
			k += 30;
			g.drawString("3: Try using a different game-world", 30, k);
			k += 30;
			g.drawString("4: Try rebooting your computer", 30, k);
			k += 30;
			g.drawString(
					"5: Try selecting a different version of Java from the play-game menu",
					30, k);
		}
		if (genericLoadingError) {
			aBoolean831 = false;
			g.setFont(new Font("Helvetica", 1, 20));
			g.setColor(Color.white);
			g.drawString("Error - unable to load game!", 50, 50);
			g.drawString("To play " + Configuration.CLIENT_NAME + " make sure you play from",
					50, 100);
			g.drawString("http://www.Dodian.net", 50, 150);
		}
		if (rsAlreadyLoaded) {
			aBoolean831 = false;
			g.setColor(Color.yellow);
			int l = 35;
			g.drawString("Error a copy of " + Configuration.CLIENT_NAME
					+ " already appears to be loaded", 30, l);
			l += 50;
			g.setColor(Color.white);
			g.drawString("To fix this try the following (in order):", 30, l);
			l += 50;
			g.setColor(Color.white);
			g.setFont(new Font("Helvetica", 1, 12));
			g.drawString("1: Try closing ALL open web-browser windows, and reloading", 30, l);
			l += 30;
			g.drawString("2: Try rebooting your computer, and reloading", 30, l);
			l += 30;
		}
	}

	public URL getCodeBase() {
		try {
			return new URL(server + ":" + (80 + portOffset));
		} catch (Exception _ex) {
		}
		return null;
	}

	private void processNpcMovement() {
		for (int j = 0; j < npcCount; j++) {
			int k = npcIndices[j];
			Npc npc = npcs[k];
			if (npc != null)
				processMovement(npc);
		}
	}

	private void processMovement(Mob mob) {            
		if (mob.x < 128 || mob.y < 128 || mob.x >= 13184 || mob.y >= 13184) {
			mob.emoteAnimation = -1;
			mob.graphic = -1;
			mob.startForceMovement = 0;
			mob.endForceMovement = 0;
			mob.x = mob.pathX[0] * 128 + mob.size * 64;
			mob.y = mob.pathY[0] * 128 + mob.size * 64;
			mob.resetPath();
		}
		if (mob == localPlayer && (mob.x < 1536 || mob.y < 1536 || mob.x >= 11776
				|| mob.y >= 11776)) {
			mob.emoteAnimation = -1;
			mob.graphic = -1;
			mob.startForceMovement = 0;
			mob.endForceMovement = 0;
			mob.x = mob.pathX[0] * 128 + mob.size * 64;
			mob.y = mob.pathY[0] * 128 + mob.size * 64;
			mob.resetPath();
		}
		if (mob.startForceMovement > tick) {
			mob.nextPreForcedStep();                  
		} else if (mob.endForceMovement >= tick) {
			mob.nextForcedMovementStep();                  
		} else {
			mob.nextStep();                  
		}
		appendFocusDestination(mob);
		mob.updateAnimation();            
	}

	private void appendFocusDestination(Mob entity) {
		if (entity.degreesToTurn == 0)
			return;
		if (entity.interactingEntity != -1 && entity.interactingEntity < 32768 && entity.interactingEntity < npcs.length) {
			Npc npc = npcs[entity.interactingEntity];
			if (npc != null) {
				int i1 = entity.x - npc.x;
				int k1 = entity.y - npc.y;
				if (i1 != 0 || k1 != 0)
					entity.nextStepOrientation =
					(int) (Math.atan2(i1, k1) * 325.94900000000001D) & 0x7ff;
			}
		}
		if (entity.interactingEntity >= 32768) {
			int j = entity.interactingEntity - 32768;
			if (j == localPlayerIndex) {
				j = internalLocalPlayerIndex;
			}
			Player player = players[j];
			if (player != null) {
				int l1 = entity.x - player.x;
				int i2 = entity.y - player.y;
				if (l1 != 0 || i2 != 0) {
					entity.nextStepOrientation =
							(int) (Math.atan2(l1, i2) * 325.94900000000001D) & 0x7ff;
				}
			}
		}
		if ((entity.faceX != 0 || entity.faceY != 0) && (entity.remainingPath == 0 || entity.anInt1503 > 0)) {
			int k = entity.x - (entity.faceX - regionBaseX - regionBaseX) * 64;
			int j1 = entity.y - (entity.faceY - regionBaseY - regionBaseY) * 64;
			if (k != 0 || j1 != 0)
				entity.nextStepOrientation =
				(int) (Math.atan2(k, j1) * 325.94900000000001D) & 0x7ff;
			entity.faceX = 0;
			entity.faceY = 0;
		}
		int l = entity.nextStepOrientation - entity.orientation & 0x7ff;
		if (l != 0) {
			if (l < entity.degreesToTurn || l > 2048 - entity.degreesToTurn)
				entity.orientation = entity.nextStepOrientation;
			else if (l > 1024)
				entity.orientation -= entity.degreesToTurn;
			else
				entity.orientation += entity.degreesToTurn;
			entity.orientation &= 0x7ff;
			if (entity.movementAnimation == entity.idleAnimation
					&& entity.orientation != entity.nextStepOrientation) {
				if (entity.standTurnAnimIndex != -1) {
					entity.movementAnimation = entity.standTurnAnimIndex;
					return;
				}
				entity.movementAnimation = entity.walkAnimIndex;
			}
		}
	}



	private void drawGameScreen() {
		if (fullscreenInterfaceID != -1
				&& (loadingStage == 2 || super.fullGameScreen != null)) {
			if (loadingStage == 2) {
				try {
					processWidgetAnimations(tickDelta, fullscreenInterfaceID);
					if (openInterfaceId != -1) {
						processWidgetAnimations(tickDelta, openInterfaceId);
					}
				} catch(Exception ex) {

				}
				tickDelta = 0;
				resetAllImageProducers();
				super.fullGameScreen.initDrawingArea();
				Rasterizer3D.scanOffsets = fullScreenTextureArray;
				Rasterizer2D.clear();
				welcomeScreenRaised = true;
				if (openInterfaceId != -1) {
					Widget rsInterface_1 = Widget.interfaceCache[openInterfaceId];
					if (rsInterface_1.width == 512 && rsInterface_1.height == 334
							&& rsInterface_1.type == 0) {
						rsInterface_1.width = 765;
						rsInterface_1.height = 503;
					}
					try {
						drawInterface(0, 0, rsInterface_1, 8);
					} catch(Exception ex) {

					}
				}
				Widget rsInterface = Widget.interfaceCache[fullscreenInterfaceID];
				if (rsInterface.width == 512 && rsInterface.height == 334
						&& rsInterface.type == 0) {
					rsInterface.width = 765;
					rsInterface.height = 503;
				}
				try {
					drawInterface(0, 0, rsInterface, 8);
				} catch (Exception ex) {

				}
				if (!menuOpen) {
					processRightClick();
					drawTooltip();
				} else {
					drawMenu(frameMode == ScreenMode.FIXED ? 4 : 0,
							frameMode == ScreenMode.FIXED ? 4 : 0);
				}
			}
			drawCount++;
			super.fullGameScreen.drawGraphics(0, super.graphics, 0);
			return;
		} else {
			if (drawCount != 0) {
				setupGameplayScreen();
			}
		}
		if (welcomeScreenRaised) {
			welcomeScreenRaised = false;
			if (frameMode == ScreenMode.FIXED) {
				topFrame.drawGraphics(0, super.graphics, 0);
				leftFrame.drawGraphics(4, super.graphics, 0);
			}
			updateChatbox = true;
			tabAreaAltered = true;
			if (loadingStage != 2) {
				if (frameMode == ScreenMode.FIXED) {
					gameScreenImageProducer.drawGraphics(
							frameMode == ScreenMode.FIXED ? 4 : 0, super.graphics,
									frameMode == ScreenMode.FIXED ? 4 : 0);
					minimapImageProducer.drawGraphics(0, super.graphics, 516);
				}
			}
		}
		if (overlayInterfaceId != -1) {
			try {
				processWidgetAnimations(tickDelta, overlayInterfaceId);
			} catch (Exception ex) {

			}
		}
		drawTabArea();
		if (backDialogueId == -1) {
			aClass9_1059.scrollPosition = anInt1211 - anInt1089 - 110;
			if (super.mouseX >= 496 && super.mouseX <= 511
					&& super.mouseY > (frameMode == ScreenMode.FIXED ? 345
							: frameHeight - 158))
				method65(494, 110, super.mouseX,
						super.mouseY - (frameMode == ScreenMode.FIXED ? 345
								: frameHeight - 158),
						aClass9_1059, 0, false, anInt1211);
			int i = anInt1211 - 110 - aClass9_1059.scrollPosition;
			if (i < 0) {
				i = 0;
			}
			if (i > anInt1211 - 110) {
				i = anInt1211 - 110;
			}
			if (anInt1089 != i) {
				anInt1089 = i;
				updateChatbox = true;
			}
		}
		if (backDialogueId != -1) {
			boolean flag2 = false;

			try {
				flag2 = processWidgetAnimations(tickDelta, backDialogueId);
			} catch(Exception ex) {
				ex.printStackTrace();
			}

			if (flag2) {
				updateChatbox = true;
			}
		}
		if (atInventoryInterfaceType == 3)
			updateChatbox = true;
		if (activeInterfaceType == 3)
			updateChatbox = true;
		if (clickToContinueString != null)
			updateChatbox = true;
		if (menuOpen && menuScreenArea == 2)
			updateChatbox = true;
		if (updateChatbox) {
			drawChatArea();
			updateChatbox = false;
		}
		if (loadingStage == 2)
			moveCameraWithPlayer();
		if (loadingStage == 2) {
			if (frameMode == ScreenMode.FIXED) {
				drawMinimap();
				minimapImageProducer.drawGraphics(0, super.graphics, 516);
			}
		}
		if (flashingSidebarId != -1)
			tabAreaAltered = true;
		if (tabAreaAltered) {
			if (flashingSidebarId != -1 && flashingSidebarId == tabId) {
				flashingSidebarId = -1;
				// flashing sidebar
				/*outgoing.writeOpcode(120);
				outgoing.writeByte(tabId);*/
			}
			tabAreaAltered = false;
			chatSettingImageProducer.initDrawingArea();
			gameScreenImageProducer.initDrawingArea();
		}
		tickDelta = 0;
	}

	private boolean buildFriendsListMenu(Widget class9) {
		int i = class9.contentType;
		if (i >= 1 && i <= 200 || i >= 701 && i <= 900) {
			if (i >= 801)
				i -= 701;
			else if (i >= 701)
				i -= 601;
			else if (i >= 101)
				i -= 101;
			else
				i--;
			menuActionText[menuActionRow] = "Remove @whi@" + friendsList[i];
			menuActionTypes[menuActionRow] = 792;
			menuActionRow++;
			menuActionText[menuActionRow] = "Message @whi@" + friendsList[i];
			menuActionTypes[menuActionRow] = 639;
			menuActionRow++;
			return true;
		}
		if (i >= 401 && i <= 500) {
			menuActionText[menuActionRow] = "Remove @whi@" + class9.defaultText;
			menuActionTypes[menuActionRow] = 322;
			menuActionRow++;
			return true;
		} else {
			return false;
		}
	}

	private void createStationaryGraphics() {
		AnimableObject class30_sub2_sub4_sub3 =
				(AnimableObject) incompleteAnimables.reverseGetFirst();
		for (; class30_sub2_sub4_sub3 != null; class30_sub2_sub4_sub3 =
				(AnimableObject) incompleteAnimables.reverseGetNext())
			if (class30_sub2_sub4_sub3.anInt1560 != plane
			|| class30_sub2_sub4_sub3.aBoolean1567)
				class30_sub2_sub4_sub3.unlink();
			else if (tick >= class30_sub2_sub4_sub3.anInt1564) {
				class30_sub2_sub4_sub3.method454(tickDelta);
				if (class30_sub2_sub4_sub3.aBoolean1567)
					class30_sub2_sub4_sub3.unlink();
				else
					scene.addAnimableA(class30_sub2_sub4_sub3.anInt1560, 0,
							class30_sub2_sub4_sub3.anInt1563, -1,
							class30_sub2_sub4_sub3.anInt1562, 60,
							class30_sub2_sub4_sub3.anInt1561, class30_sub2_sub4_sub3,
							false);
			}

	}

	public void drawBlackBox(int xPos, int yPos) {
		Rasterizer2D.drawBox(xPos - 2, yPos - 1, 1, 71, 0x726451);
		Rasterizer2D.drawBox(xPos + 174, yPos, 1, 69, 0x726451);
		Rasterizer2D.drawBox(xPos - 2, yPos - 2, 178, 1, 0x726451);
		Rasterizer2D.drawBox(xPos, yPos + 68, 174, 1, 0x726451);
		Rasterizer2D.drawBox(xPos - 1, yPos - 1, 1, 71, 0x2E2B23);
		Rasterizer2D.drawBox(xPos + 175, yPos - 1, 1, 71, 0x2E2B23);
		Rasterizer2D.drawBox(xPos, yPos - 1, 175, 1, 0x2E2B23);
		Rasterizer2D.drawBox(xPos, yPos + 69, 175, 1, 0x2E2B23);
		Rasterizer2D.drawTransparentBox(xPos, yPos, 174, 68, 0, 220);
	}      

	final static int[] IDs = {1196, 1199, 1206, 1215, 1224, 1231, 1240, 1249, 1258, 1267, 1274,
			1283, 1573, 1290, 1299, 1308, 1315, 1324, 1333, 1340, 1349, 1358,
			1367, 1374, 1381, 1388, 1397, 1404, 1583, 12038, 1414, 1421, 1430,
			1437, 1446, 1453, 1460, 1469, 15878, 1602, 1613, 1624, 7456, 1478,
			1485, 1494, 1503, 1512, 1521, 1530, 1544, 1553, 1563, 1593, 1635,
			12426, 12436, 12446, 12456, 6004, 18471,
			/* Ancients */
			12940, 12988, 13036, 12902, 12862, 13046, 12964, 13012, 13054, 12920,
			12882, 13062, 12952, 13000, 13070, 12912, 12872, 13080, 12976, 13024,
			13088, 12930, 12892, 13096};

	final static int[] runeChildren = {1202, 1203, 1209, 1210, 1211, 1218, 1219, 1220, 1227, 1228,
			1234, 1235, 1236, 1243, 1244, 1245, 1252, 1253, 1254, 1261, 1262,
			1263, 1270, 1271, 1277, 1278, 1279, 1286, 1287, 1293, 1294, 1295,
			1302, 1303, 1304, 1311, 1312, 1318, 1319, 1320, 1327, 1328, 1329,
			1336, 1337, 1343, 1344, 1345, 1352, 1353, 1354, 1361, 1362, 1363,
			1370, 1371, 1377, 1378, 1384, 1385, 1391, 1392, 1393, 1400, 1401,
			1407, 1408, 1410, 1417, 1418, 1424, 1425, 1426, 1433, 1434, 1440,
			1441, 1442, 1449, 1450, 1456, 1457, 1463, 1464, 1465, 1472, 1473,
			1474, 1481, 1482, 1488, 1489, 1490, 1497, 1498, 1499, 1506, 1507,
			1508, 1515, 1516, 1517, 1524, 1525, 1526, 1533, 1534, 1535, 1547,
			1548, 1549, 1556, 1557, 1558, 1566, 1567, 1568, 1576, 1577, 1578,
			1586, 1587, 1588, 1596, 1597, 1598, 1605, 1606, 1607, 1616, 1617,
			1618, 1627, 1628, 1629, 1638, 1639, 1640, 6007, 6008, 6011, 8673,
			8674, 12041, 12042, 12429, 12430, 12431, 12439, 12440, 12441, 12449,
			12450, 12451, 12459, 12460, 15881, 15882, 15885, 18474, 18475, 18478};

	private void drawInterface(int scroll_y, int x, Widget rsInterface, int y) throws Exception {
		if (rsInterface == null)
			return;
		if (rsInterface.type != 0 || rsInterface.children == null)
			return;
		if (rsInterface.invisible && anInt1026 != rsInterface.id && anInt1048 != rsInterface.id
				&& anInt1039 != rsInterface.id || rsInterface.drawingDisabled) {
			return;
		}
		if(rsInterface.id == 23300) {
			if(!Configuration.bountyHunterInterface) {
				return;
			}
		}
		if(rsInterface.id == 36100) {
			Rasterizer2D.drawTransparentBox(-100, 0, Rasterizer2D.width + 100, Rasterizer2D.height, 0x3e2f1d, 130); 
		}
		
		int clipLeft = Rasterizer2D.leftX;
		int clipTop = Rasterizer2D.topY;
		int clipRight = Rasterizer2D.bottomX;
		int clipBottom = Rasterizer2D.bottomY;
		Rasterizer2D.setDrawingArea(y + rsInterface.height, x, x + rsInterface.width, y);
		int childCount = rsInterface.children.length;
		processSkillTab();
		if(rsInterface.id == 25500 || rsInterface.id == 26000 ) {
			if(Widget.interfaceCache[25560].inventoryItemId[0] > 0) {
				Widget.interfaceCache[25555].drawingDisabled = true;
				Widget.interfaceCache[25557].drawingDisabled = false;
				Widget.interfaceCache[25558].drawingDisabled = false;
				Widget.interfaceCache[25555].tooltip = "";
				Widget.interfaceCache[25557].tooltip = "Confirm";
				Widget.interfaceCache[25558].tooltip = "Confirm";
			} else {
				Widget.interfaceCache[25555].drawingDisabled = false;
				Widget.interfaceCache[25557].drawingDisabled = true;
				Widget.interfaceCache[25558].drawingDisabled = true;
				Widget.interfaceCache[25555].tooltip = "Confirm";
				Widget.interfaceCache[25557].tooltip = "";
				Widget.interfaceCache[25558].tooltip = "";
			}
		}
		if(backDialogueId == 40500) {
			    	 processSpawnTab();
			searchingSpawnTab = true; 
		} else if(rsInterface.id == 53000) {
			processKeyBindings();
		}

		for (int childId = 0; childId < childCount; childId++) {
			int _x = rsInterface.childX[childId] + x;
			int currentY = (rsInterface.childY[childId] + y) - scroll_y;
			Widget childInterface = Widget.interfaceCache[rsInterface.children[childId]]; 
			if(childInterface == null) {
				continue;
			}

			if(childInterface.drawingDisabled) {
				continue;
			}
		if(openInterfaceId == 26000 && childInterface.id == 3322) {
			childInterface.actions[0] = "Offer";
			childInterface.actions[1] = null;
			childInterface.actions[2] = null;
			childInterface.actions[3] = null;
			childInterface.actions[4] = null;
			childInterface.actions[5] = null;
			sidebarGlow = true;
		} else if(openInterfaceId != 26000 && childInterface.id != 3322 && !childInterface.glowing) {
			sidebarGlow = false;
		}
			if(childInterface.glowing) {
				if(pcOpacity < 0) {
					pcOpacity = 0;
					maxOpacity = false;
				}
				if(pcOpacity > 255) {
					pcOpacity = 255;
					maxOpacity = true;
				}
				if(pcOpacity >= 0 && !maxOpacity) {
					pcOpacity += 5;
					if(pcOpacity == 255) {
						maxOpacity = true;
					}
				}
				if(pcOpacity <= 255 && maxOpacity) {
					pcOpacity -= 5;
					if(pcOpacity == 0) {
						maxOpacity = false;
					}
				}
						childInterface.transparency = pcOpacity;
			}

			_x += childInterface.horizontalOffset;
			currentY += childInterface.verticalOffset;
			if (childInterface.contentType > 0)
				drawFriendsListOrWelcomeScreen(childInterface);
			for (int m5 = 0; m5 < IDs.length; m5++) {
				if (childInterface.id == IDs[m5] + 1) {
					if (m5 > 61)
						drawBlackBox(_x + 1, currentY);
					else
						drawBlackBox(_x, currentY + 1);
				}
			}
			for (int r = 0; r < runeChildren.length; r++)
				if (childInterface.id == runeChildren[r])
					childInterface.modelZoom = 775;

			if (childInterface.type == Widget.TYPE_CONTAINER) {
				if (childInterface.scrollPosition > childInterface.scrollMax
						- childInterface.height)
					childInterface.scrollPosition =
					childInterface.scrollMax - childInterface.height;
				if (childInterface.scrollPosition < 0)
					childInterface.scrollPosition = 0;
				drawInterface(childInterface.scrollPosition, _x, childInterface, currentY);
				if (childInterface.scrollMax > childInterface.height) {
					if(childInterface.id == 36350) {
						draw508Scrollbar(childInterface.height, childInterface.scrollPosition,
								currentY, _x + childInterface.width, childInterface.scrollMax,
								false);
						} else {
							drawScrollbar(childInterface.height, childInterface.scrollPosition,
									currentY, _x + childInterface.width, childInterface.scrollMax,
									false);
						}
				}
			} else if (childInterface.type != 1)
				if (childInterface.type == Widget.TYPE_INVENTORY) {

					int item = 0;
					for (int row = 0; row < childInterface.height; row++) {
						for (int column = 0; column < childInterface.width; column++) {
							int tileX = _x + column
									* (32 + childInterface.spritePaddingX);
							int tileY = currentY
									+ row * (32 + childInterface.spritePaddingY);
							if (item < 20) {
								tileX += childInterface.spritesX[item];
								tileY += childInterface.spritesY[item];
							}
							if (item < childInterface.inventoryItemId.length && childInterface.inventoryItemId[item] > 0) {
								int differenceX = 0;
								int differenceY = 0;
								int itemId = childInterface.inventoryItemId[item]
										- 1;
								if (tileX > Rasterizer2D.leftX - 32
										&& tileX < Rasterizer2D.bottomX
										&& tileY > Rasterizer2D.topY - 32
										&& tileY < Rasterizer2D.bottomY
										|| activeInterfaceType != 0
										&& anInt1085 == item) {
									int l9 = 0;
									if (itemSelected == 1 && anInt1283 == item
											&& anInt1284 == childInterface.id)
										l9 = 0xffffff;
									Sprite item_icon =
											ItemDefinition.getSprite(itemId,
													childInterface.inventoryAmounts[item],
													l9);
									if (item_icon != null) {
										if (activeInterfaceType != 0
												&& anInt1085 == item
												&& anInt1084 == childInterface.id) {
											differenceX = super.mouseX
													- anInt1087;
											differenceY = super.mouseY
													- anInt1088;
											if (differenceX < 5
													&& differenceX > -5)
												differenceX = 0;
											if (differenceY < 5
													&& differenceY > -5)
												differenceY = 0;
											if (anInt989 < 10) {
												differenceX = 0;
												differenceY = 0;
											}
											item_icon.drawSprite1(
													tileX + differenceX,
													tileY + differenceY);
											int yy = frameMode == ScreenMode.FIXED ? 40 : 40 + (frameHeight / 2) - 167;
											if(openInterfaceId == 5292) {
												if(super.mouseY >= yy && super.mouseY <= yy+37) {
													bankItemDragSprite = item_icon;
													bankItemDragSpriteX = super.mouseX;
													bankItemDragSpriteY = super.mouseY;
												} else {
													bankItemDragSprite = null;
												}
											}
											if (tileY + differenceY < Rasterizer2D.topY
													&& rsInterface.scrollPosition > 0) {
												int i10 = (tickDelta
														* (Rasterizer2D.topY
																- tileY
																- differenceY))
														/ 3;
												if (i10 > tickDelta * 10)
													i10 = tickDelta * 10;
												if (i10 > rsInterface.scrollPosition)
													i10 = rsInterface.scrollPosition;
												rsInterface.scrollPosition -=
														i10;
												anInt1088 += i10;
											}
											if (tileY + differenceY
													+ 32 > Rasterizer2D.bottomY
													&& rsInterface.scrollPosition < rsInterface.scrollMax
													- rsInterface.height) {
												int j10 = (tickDelta
														* ((tileY + differenceY
																+ 32)
																- Rasterizer2D.bottomY))
														/ 3;
												if (j10 > tickDelta * 10)
													j10 = tickDelta * 10;
												if (j10 > rsInterface.scrollMax
														- rsInterface.height
														- rsInterface.scrollPosition)
													j10 = rsInterface.scrollMax
													- rsInterface.height
													- rsInterface.scrollPosition;
												rsInterface.scrollPosition +=
														j10;
												anInt1088 -= j10;
											}
										} else if (atInventoryInterfaceType != 0
												&& atInventoryIndex == item
												&& atInventoryInterface == childInterface.id)
											item_icon.drawSprite1(tileX,
													tileY);
										else
											item_icon.drawSprite(tileX,
													tileY);
										if (item_icon.maxWidth == 33
												|| childInterface.inventoryAmounts[item] != 1) {

											boolean flag = true;
											if(childInterface.id >= 22035 && childInterface.id <= 22043) {
												flag = false;
											}
											if(flag) {
												int k10 = childInterface.inventoryAmounts[item];
												if(k10 >= 1500000000 && childInterface.drawInfinity) {
													cacheSprite[105].drawSprite(tileX, tileY);
												} else {
													if(childInterface.drawNumber) {
														cacheSprite[507].drawSprite(tileX, tileY);
													} else {
													smallText.render(0,
															intToKOrMil(k10),
															tileY + 10 + differenceY,
															tileX + 1 + differenceX);
													if (k10 >= 1)
														smallText.render(0xFFFF00,
																intToKOrMil(k10),
																tileY + 9 + differenceY,
																tileX + differenceX);
													if (k10 >= 100000)
														smallText.render(0xFFFFFF,
																intToKOrMil(k10),
																tileY + 9 + differenceY,
																tileX + differenceX);
													if (k10 >= 10000000)
														smallText.render(0x00FF80,
																intToKOrMil(k10),
																tileY + 9 + differenceY,
																tileX + differenceX);
													}
												}
											}
										}
									}
								}
							} else if (childInterface.sprites != null && item < 20) {
								Sprite image = childInterface.sprites[item];
								if (image != null)
									image.drawSprite(tileX, tileY);
							}
							item++;
						}
					}

				} else if (childInterface.type == Widget.TYPE_RECTANGLE) {
					boolean hover = false;
					if (anInt1039 == childInterface.id || anInt1048 == childInterface.id
							|| anInt1026 == childInterface.id)
						hover = true;
					int colour;
					if (interfaceIsSelected(childInterface)) {
						colour = childInterface.secondaryColor;
						if (hover && childInterface.secondaryHoverColor != 0)
							colour = childInterface.secondaryHoverColor;
					} else {
						colour = childInterface.textColor;
						if (hover && childInterface.defaultHoverColor != 0)
							colour = childInterface.defaultHoverColor;
					}
					if (childInterface.opacity == 0) {
						if (childInterface.filled)
							Rasterizer2D.drawBox(_x, currentY, childInterface.width, childInterface.height, colour
									);
						else
							Rasterizer2D.drawBoxOutline(_x, currentY, childInterface.width,
									childInterface.height, colour);
					} else if (childInterface.filled)
						Rasterizer2D.drawTransparentBox(_x, currentY, childInterface.width, childInterface.height, colour,
								256 - (childInterface.opacity & 0xff));
					else
						Rasterizer2D.drawTransparentBoxOutline(_x, currentY, childInterface.width, childInterface.height,
								colour, 256 - (childInterface.opacity & 0xff)
								);
				} else if (childInterface.type == Widget.TYPE_TEXT) {
					GameFont textDrawingArea = childInterface.textDrawingAreas;
					String text = childInterface.defaultText;
					if(text == null) {
						continue;
					}
					
					boolean flag1 = false;
					if (anInt1039 == childInterface.id || anInt1048 == childInterface.id
							|| anInt1026 == childInterface.id)
						flag1 = true;
					int colour;
					if (interfaceIsSelected(childInterface)) {
						colour = childInterface.secondaryColor;
						if (flag1 && childInterface.secondaryHoverColor != 0)
							colour = childInterface.secondaryHoverColor;
						if (childInterface.secondaryText.length() > 0)
							text = childInterface.secondaryText;
					} else {
						colour = childInterface.textColor;
						if (flag1 && childInterface.defaultHoverColor != 0)
							colour = childInterface.defaultHoverColor;
					}
					if (childInterface.atActionType == Widget.OPTION_CONTINUE
							&& continuedDialogue) {
						text = "Please wait...";
						colour = childInterface.textColor;
					}
					if (Rasterizer2D.width == 519) {
						if (colour == 0xffff00)
							colour = 255;
						if (colour == 49152)
							colour = 0xffffff;
					}
					if (frameMode != ScreenMode.FIXED) {
						if ((backDialogueId != -1 || dialogueId != -1
								|| childInterface.defaultText
								.contains("Click here to continue"))
								&& (rsInterface.id == backDialogueId
								|| rsInterface.id == dialogueId)) {
							if (colour == 0xffff00) {
								colour = 255;
							}
							if (colour == 49152) {
								colour = 0xffffff;
							}
						}
					}
					if ((childInterface.parent == 1151)
							|| (childInterface.parent == 12855)) {
						switch (colour) {
						case 16773120:
							colour = 0xFE981F;
							break;
						case 7040819:
							colour = 0xAF6A1A;
							break;                                                
						}                                    
					}

					int image = -1;
					final String INITIAL_MESSAGE = text;
					if (text.contains("<img=")) {
						int prefix = text.indexOf("<img=");
						int suffix = text.indexOf(">");
						try {
							image = Integer.parseInt(text.substring(prefix + 5, suffix));
							text = text.replaceAll(text.substring(prefix + 5, suffix), "");
							text = text.replaceAll("</img>", "");
							text = text.replaceAll("<img=>", "");							
						} catch (NumberFormatException nfe) { 
							//System.out.println("Unable to draw player crown on interface. Unable to read rights.");
							text = INITIAL_MESSAGE;
						} catch (IllegalStateException ise) {
							//System.out.println("Unable to draw player crown on interface, rights too low or high.");
							text = INITIAL_MESSAGE;
						}
						if(suffix > prefix) {
							//_x += 14;
						}
					}

					for (int drawY = currentY + textDrawingArea.verticalSpace; text.length() > 0; drawY +=
							textDrawingArea.verticalSpace) {

						if(image != -1) {

							//CLAN CHAT LIST = 37128
							if(childInterface.parent == 37128) {
								cacheSprite[image].drawAdvancedSprite(_x, drawY - cacheSprite[image].myHeight - 1);
								_x += cacheSprite[image].myWidth + 3;
							} else {
								cacheSprite[image].drawAdvancedSprite(_x, drawY - cacheSprite[image].myHeight + 3);
								_x += cacheSprite[image].myWidth + 4;
							}
						}

						if (text.indexOf("%") != -1) {                                  
							do {
								int index = text.indexOf("%1");
								if (index == -1)
									break;
								if (childInterface.id < 4000 || childInterface.id > 5000
										&& childInterface.id != 13921
										&& childInterface.id != 13922
										&& childInterface.id != 12171
										&& childInterface.id != 12172) {
									text = text.substring(0, index)
											+ formatCoins(executeScript(
													childInterface, 0))
											+ text.substring(index + 2);

								} else {
									text = text.substring(0, index) + interfaceIntToString(executeScript(childInterface, 0))
									+ text.substring(index + 2);

								}
							} while (true);
							do {
								int index = text.indexOf("%2");                                                
								if (index == -1) {
									break;
								}
								text = text.substring(0, index)
										+ interfaceIntToString(executeScript(
												childInterface, 1))
										+ text.substring(index + 2);
							} while (true);
							do {
								int index = text.indexOf("%3");

								if (index == -1) {
									break;
								}

								text = text.substring(0, index)
										+ interfaceIntToString(executeScript(
												childInterface, 2))
										+ text.substring(index + 2);
							} while (true);
							do {
								int index = text.indexOf("%4");  

								if (index == -1) {
									break;
								}
								text = text.substring(0, index)
										+ interfaceIntToString(executeScript(
												childInterface, 3))
										+ text.substring(index + 2);
							} while (true);
							do {
								int index = text.indexOf("%5");

								if (index == -1) {
									break;
								}

								text = text.substring(0, index)
										+ interfaceIntToString(executeScript(
												childInterface, 4))
										+ text.substring(index + 2);
							} while (true);
						}

						int line = text.indexOf("\\n");

						String drawn;

						if (line != -1) {
							drawn = text.substring(0, line);
							text = text.substring(line + 2);
						} else {
							drawn = text;
							text = "";
						}
						RSFont font = null;
						if (textDrawingArea == smallText) {
							font = newSmallFont;
						} else if (textDrawingArea == regularText) {
							font = newRegularFont;
						} else if (textDrawingArea == boldText) {
							font = newBoldFont;
						} else if (textDrawingArea == gameFont) {
							font = newFancyFont;
						} else if (textDrawingArea == fancyFont) {
							font = newFancyFont2;
						}
							if (childInterface.centerText) {
								if(childInterface.fancy) {
									newFancyFont2.drawCenteredString(drawn, _x + childInterface.width / 2,
											drawY, colour,
											childInterface.textShadow ? 0 : -1);
								}
								if(!childInterface.fancy) {
								font.drawCenteredString(drawn, _x + childInterface.width / 2,
										drawY, colour,
										childInterface.textShadow ? 0 : -1);
								}
							} else if (childInterface.rightText) {
								font.drawBasicString(drawn, _x - font.getTextWidth(drawn),
										drawY, colour,
										childInterface.textShadow ? 0 : -1);
							} else {
								font.drawBasicString(drawn, _x, drawY, colour,
										childInterface.textShadow ? 0 : -1);
							}
						}
				 } else if (childInterface.type == 69) {
					 childInterface.dropDown.drawDropdown(childInterface, _x, currentY);
				 
				} else if (childInterface.type == Widget.TYPE_SPRITE) {

					Sprite sprite;

					if(childInterface.spriteXOffset != 0) {
						_x += childInterface.spriteXOffset;
					}

					if(childInterface.spriteYOffset != 0) {
						currentY += childInterface.spriteYOffset;
					}

					if (interfaceIsSelected(childInterface)) {
						sprite = childInterface.enabledSprite;
					} else {
						sprite = childInterface.disabledSprite;
					}

					if (spellSelected == 1 && childInterface.id == spellId && spellId != 0
							&& sprite != null) {
						sprite.drawSprite(_x, currentY, 0xffffff);
					} else {
						if (sprite != null) {

							boolean drawTransparent = childInterface.drawsTransparent;

							//Check if parent draws as transparent..
							if(!drawTransparent && childInterface.parent > 0 &&
									Widget.interfaceCache[childInterface.parent] != null) {
								drawTransparent = Widget.interfaceCache[childInterface.parent].drawsTransparent;
							}

							if (drawTransparent) {
								sprite.drawTransparentSprite(_x, currentY, childInterface.transparency);
							} else {
								sprite.drawSprite(_x, currentY);
							}
						}
					}
					if (autocast && childInterface.id == autoCastId)
						cacheSprite[43].drawSprite(_x - 2, currentY - 1);
				} else if (childInterface.type == Widget.TYPE_MODEL) {
					int centreX = Rasterizer3D.originViewX;
					int centreY = Rasterizer3D.originViewY;
					Rasterizer3D.originViewX = _x + childInterface.width / 2;
					Rasterizer3D.originViewY = currentY + childInterface.height / 2;
					int sine = Rasterizer3D.anIntArray1470[childInterface.modelRotation1]
							* childInterface.modelZoom >> 16;
							int cosine = Rasterizer3D.COSINE[childInterface.modelRotation1]
									* childInterface.modelZoom >> 16;
									boolean selected = interfaceIsSelected(childInterface);
									int emoteAnimation;
									if (selected)
										emoteAnimation = childInterface.secondaryAnimationId;
									else
										emoteAnimation = childInterface.defaultAnimationId;
									Model model;
									if (emoteAnimation == -1) {
										model = childInterface.method209(-1, -1, selected);
									} else {
										Animation animation = Animation.animations[emoteAnimation];
										model = childInterface.method209(
												animation.secondaryFrames[childInterface.currentFrame],
												animation.primaryFrames[childInterface.currentFrame],
												selected);
									}
									if (model != null)
										model.method482(childInterface.modelRotation2, 0,
												childInterface.modelRotation1, 0, sine, cosine);
									Rasterizer3D.originViewX = centreX;
									Rasterizer3D.originViewY = centreY;
				} else if (childInterface.type == Widget.TYPE_ITEM_LIST) {
					GameFont font = childInterface.textDrawingAreas;
					int slot = 0;
					for (int row = 0; row < childInterface.height; row++) {
						for (int column = 0; column < childInterface.width; column++) {
							if (childInterface.inventoryItemId[slot] > 0) {
								ItemDefinition item = ItemDefinition
										.lookup(childInterface.inventoryItemId[slot]
												- 1);
								String name = item.name;
								if (item.stackable
										|| childInterface.inventoryAmounts[slot] != 1)
									name = name + " x" + intToKOrMilLongName(
											childInterface.inventoryAmounts[slot]);
								int __x = _x + column
										* (115 + childInterface.spritePaddingX);
								int __y = currentY + row
										* (12 + childInterface.spritePaddingY);
								if (childInterface.centerText)
									font.method382(childInterface.textColor,
											__x + childInterface.width / 2,
											name, __y,
											childInterface.textShadow);
								else
									font.drawTextWithPotentialShadow(
											childInterface.textShadow, __x,
											childInterface.textColor, name,
											__y);
							}
							slot++;
						}
					}	
				} else if (childInterface.type == 8
						&& (anInt1500 == childInterface.id
						|| anInt1044 == childInterface.id
						|| anInt1129 == childInterface.id)
						&& anInt1501 == 0 && !menuOpen) {

					/**
					 * Skill tab hovers
					 * Remove "next level at" and "remaining" 
					 * for xp if we're level 99.
					 */
					if(childInterface.parent == 10141) {
						if(childInterface.id != 24154) { //Not total level
							int maxLevel = executeScript(childInterface, 1);
							int next = childInterface.defaultText.indexOf("Next");
							if(maxLevel >= 99) {
								if(next != -1) {
									childInterface.defaultText = childInterface.defaultText.substring(0, next);
								}
							} else {
								if(next == -1) {
									childInterface.defaultText = childInterface.defaultText +"\\nNext level: %4\\nRemainder: %5";
								}

								//Hover fix
								if(currentY > 200) {
									currentY -= 100;
								}
							}
						}

						//Hover fix
						if(_x > 100) {
							_x = 100;
						}
						if(currentY > 200) {
							currentY -= 80;
						}
					}

					if(childInterface.hoverXOffset != 0) {
						_x += childInterface.hoverXOffset;
					}

					if(childInterface.hoverYOffset != 0) {
						currentY += childInterface.hoverYOffset;
					}

					if(childInterface.skillHoverBox) {
						drawSkillHoverBox(_x, currentY, childInterface.skillName, childInterface.skillId, childInterface.intWidth, childInterface.rightHover);
					}
					if(childInterface.regularHoverBox) {
						drawHoverBox(_x, currentY, childInterface.defaultText);

					} else {
						int boxWidth = 0;
						int boxHeight = 0;
						GameFont font = regularText;
						for (String text = childInterface.defaultText; text.length() > 0;) {
							if (text.indexOf("%") != -1) {
								do {
									int index = text.indexOf("%1");
									if (index == -1)
										break;
									text = text.substring(0, index)
											+ interfaceIntToString(executeScript(
													childInterface, 0))
											+ text.substring(index + 2);
								} while (true);
								do {
									int index = text.indexOf("%2");
									if (index == -1)
										break;
									text = text.substring(0, index)
											+ interfaceIntToString(executeScript(
													childInterface, 1))
											+ text.substring(index + 2);
								} while (true);
								do {
									int index = text.indexOf("%3");
									if (index == -1)
										break;
									text = text.substring(0, index)
											+ interfaceIntToString(executeScript(
													childInterface, 2))
											+ text.substring(index + 2);
								} while (true);
								do {
									int index = text.indexOf("%4");
									if (index == -1)
										break;
									text = text.substring(0, index)
											+ interfaceIntToString(executeScript(
													childInterface, 3))
											+ text.substring(index + 2);
								} while (true);
								do {
									int index = text.indexOf("%5");
									if (index == -1)
										break;
									text = text.substring(0, index)
											+ interfaceIntToString(executeScript(
													childInterface, 4))
											+ text.substring(index + 2);
								} while (true);
							}
							int line = text.indexOf("\\n");
							String drawn;
							if (line != -1) {
								drawn = text.substring(0, line);
								text = text.substring(line + 2);
							} else {
								drawn = text;
								text = "";
							}
							int j10 = font.getTextWidth(drawn);
							if (j10 > boxWidth) {
								boxWidth = j10;
							}
							boxHeight += font.verticalSpace + 1;
						}
						boxWidth += 6;
						boxHeight += 7;

						int xPos = (_x + childInterface.width) - 5 - boxWidth;
						int yPos = currentY + childInterface.height + 5;
						if (xPos < _x + 5) {
							xPos = _x + 5;
						}

						if (xPos + boxWidth > x + rsInterface.width) {
							xPos = (x + rsInterface.width) - boxWidth;
						}
						if (yPos + boxHeight > y + rsInterface.height) {
							yPos = (currentY - boxHeight);
						}

						String s2 = childInterface.defaultText;

						Rasterizer2D.drawBox(xPos, yPos, boxWidth, boxHeight, 0xFFFFA0);
						Rasterizer2D.drawBoxOutline(xPos, yPos, boxWidth, boxHeight, 0);

						//Script hovers here

						for (int j11 = yPos + font.verticalSpace + 2; s2.length() > 0; j11 +=
								font.verticalSpace + 1) {// verticalSpace
							if (s2.indexOf("%") != -1) {

								do {
									int k7 = s2.indexOf("%1");
									if (k7 == -1)
										break;
									s2 = s2.substring(0, k7)
											+ interfaceIntToString(executeScript(
													childInterface, 0))
											+ s2.substring(k7 + 2);
								} while (true);

								do {
									int l7 = s2.indexOf("%2");
									if (l7 == -1)
										break;
									s2 = s2.substring(0, l7)
											+ interfaceIntToString(executeScript(
													childInterface, 1))
											+ s2.substring(l7 + 2);
								} while (true);
								do {
									int i8 = s2.indexOf("%3");
									if (i8 == -1)
										break;
									s2 = s2.substring(0, i8)
											+ interfaceIntToString(executeScript(
													childInterface, 2))
											+ s2.substring(i8 + 2);
								} while (true);
								do {
									int j8 = s2.indexOf("%4");
									if (j8 == -1)
										break;
									s2 = s2.substring(0, j8)
											+ interfaceIntToString(executeScript(
													childInterface, 3))
											+ s2.substring(j8 + 2);
								} while (true);
								do {
									int k8 = s2.indexOf("%5");
									if (k8 == -1)
										break;
									s2 = s2.substring(0, k8)
											+ interfaceIntToString(executeScript(
													childInterface, 4))
											+ s2.substring(k8 + 2);
								} while (true);
							}
							int l11 = s2.indexOf("\\n");
							String s5;
							if (l11 != -1) {
								s5 = s2.substring(0, l11);
								s2 = s2.substring(l11 + 2);
							} else {
								s5 = s2;
								s2 = "";
							}
							if (childInterface.centerText) {
								font.method382(yPos, xPos + childInterface.width / 2, s5,
										j11, false);
							} else {
								if (s5.contains("\\r")) {
									String text = s5.substring(0, s5.indexOf("\\r"));
									String text2 = s5.substring(s5.indexOf("\\r") + 2);
									font.drawTextWithPotentialShadow(false, xPos + 3, 0,
											text, j11);
									int rightX = boxWidth + xPos
											- font.getTextWidth(text2) - 2;
									font.drawTextWithPotentialShadow(false, rightX, 0,
											text2, j11);
								} else
									font.drawTextWithPotentialShadow(false, xPos + 3, 0,
											s5, j11);
							}
						}
					}
				}
		}
		Rasterizer2D.setDrawingArea(clipBottom, clipLeft, clipRight, clipTop);
	}
	
	


	public enum BankTabShow {
		FIRST_ITEM_IN_TAB,
		DIGIT,
		ROMAN_NUMERAL;
	}


	/**
	 * @author Gabriel aka Swiffy
	 */

	public static final int MAX_BANK_TABS = 11;
	private String staffMessage = ""; 
	private boolean InWorldSelect = false;

	private BankTabShow bankTabShow = BankTabShow.FIRST_ITEM_IN_TAB;
	private int currentBankTab;

	private void drawBank(int _x, int currentY) {

		int id_start = 50070;
		int x = _x + 20;
		int y = currentY;
		int final_loop = 1;
		boolean drawPlus = false;

		for(int tab = 0; tab < MAX_BANK_TABS - 1; tab++) {

			Widget containers = Widget.interfaceCache[50300 + tab];

			//First we search for an item in the tab...
			int first_item = -1;
			int item_amount = 0;
			for(int index = 0; index < containers.inventoryItemId.length; index++) {
				if(containers.inventoryItemId[index] > 0 && containers.inventoryAmounts[index] > 0) {
					first_item = containers.inventoryItemId[index] - 1;
					item_amount = containers.inventoryAmounts[index];
					break;
				}
			}

			Widget button = Widget.interfaceCache[id_start + (tab * 4)];
			//Draw the tab if it isn't empty or is id 0
			if(first_item > 0 || tab == 0) {

				//If it's currently being viewed, change its sprite
				if(tab == currentBankTab || tab == 0 && searchingBank()) {
					button.disabledSprite = Client.cacheSprite[205];
				} else {
					button.disabledSprite = Client.cacheSprite[206];
				}

				//We have a tab! Draw it!
				settings[1000+tab] = 0;
				button.drawingDisabled = false;

				//Draw its options
				button.actions = new String[]{"Select", tab == 0 ? null : "Collapse", null, null, null};

				//Draw item or tab number
				if(tab != 0) {

					Sprite sprite = null;

					switch(bankTabShow) {
					case DIGIT:
						sprite = cacheSprite[219 + tab];
						break;
					case FIRST_ITEM_IN_TAB:
						sprite = ItemDefinition.getSprite(first_item, item_amount, 0);
						break;
					case ROMAN_NUMERAL:
						sprite = cacheSprite[210 + tab];
						break;					
					}

					if(sprite != null) {
						sprite.drawSprite(x+3, y+41);
					}
				}

				x += 40;
			} else {

				//Empty tab
				//We don't have a tab. Remove the button
				settings[1000+tab] = 1;
				button.drawingDisabled = true;

				//Add the draw plus sprite
				if(!drawPlus) {
					final_loop = tab;
					drawPlus = true;
				}
			}

		}

		//Draws a tab and a plus icon after our final bank tab
		if(drawPlus) {

			//Show tab
			settings[1000+final_loop] = 0;

			//Show option
			Widget.interfaceCache[id_start + (final_loop * 4)].actions = new String[]{"Create Tab", null, null, null, null};
			Widget.interfaceCache[id_start + (final_loop * 4)].drawingDisabled = false;

			//Draw plus icon
			cacheSprite[210].drawSprite(x, y + 40);
		}


		//Now let's draw the actual bank interface.

		//First set the proper Y value depending on the scroll position
		final Widget scrollBar = Widget.interfaceCache[5385];
		if(scrollBar.scrollPosition > 0) {
			y -= scrollBar.scrollPosition;
		}

		//Now reset all children
		for(int i = 0; i < scrollBar.children.length; i++) {

			//Reset their positioning
			scrollBar.childX[i] = 40;
			scrollBar.childY[i] = 0;

			//Also make the children invisible
			Widget.interfaceCache[scrollBar.children[i]].drawingDisabled = true;
		}

		//Now reset scroll max
		scrollBar.scrollMax = 500;

		//Now draw the actual bank
		if(currentBankTab != 0) {

			//Make the container we're currently viewing visible
			Widget.interfaceCache[50300 + currentBankTab].drawingDisabled = false;

			//Set scroll max
			scrollBar.scrollMax = 1680;

		} else {

			int containerY = 0;
			int lineY = y;

			for(int i = 0; i < MAX_BANK_TABS - 1; i++) {

				//This container is the actual bank tab we're going through in this loop.
				//Let's make sure it's visible...
				Widget container = Widget.interfaceCache[50300 + i];
				container.drawingDisabled = false;

				//Calculate amount of rows that are occupied in this container
				int rows = 0;
				label0: for(int j = 0; j < container.inventoryItemId.length; j += 10) {

					//Is this row empty or not?
					//Set to true default
					boolean emptyRow = true;

					//Check the next rows for items...
					label1: for(int k = 0; k < container.inventoryItemId.length; k++) {

						if(j + k >= container.inventoryItemId.length) {
							break label0;
						}

						//Check for items...
						if(container.inventoryItemId[j + k] > 0 && container.inventoryAmounts[j + k] > 0) {
							emptyRow = false;
							break label1;
						}
					}

					//If the row wasn't empty, increment the amount of rows we have.
					if(!emptyRow) {
						rows++;
					}
				}

				//Position this container properly
				scrollBar.childY[i] = containerY;

				//Increase yDraw so the next container will not be drawn in the same place.
				containerY += 45 * (rows + 1);

				//Position the line seperator properly
				if(rows == 0) {

					//Empty container, small tab!!
					lineY += 25;

				} else {

					//Not empty bank tab.
					//Increase scroll max
					scrollBar.scrollMax += rows * 50;

					//Increment line draw positioning
					if(i == 0) {
						lineY += containerY + 50;
					} else {
						lineY += 45 * (rows + 1);
					}
				}

				//Only draw the line if it's properly inside the bank interface
				if(lineY > currentY + 80 && lineY < currentY + 294) {
					Rasterizer2D.drawHorizontalLine(_x + 19, lineY, 450, 0x73654a);
				}
			}
		}
	}

	private boolean searchingBank() {
		return currentBankTab == MAX_BANK_TABS - 1;
	}

	private void randomizeBackground(IndexedImage background) {
		int j = 256;
		for (int k = 0; k < anIntArray1190.length; k++)
			anIntArray1190[k] = 0;

		for (int l = 0; l < 5000; l++) {
			int i1 = (int) (Math.random() * 128D * (double) j);
			anIntArray1190[i1] = (int) (Math.random() * 256D);
		}
		for (int j1 = 0; j1 < 20; j1++) {
			for (int k1 = 1; k1 < j - 1; k1++) {
				for (int i2 = 1; i2 < 127; i2++) {
					int k2 = i2 + (k1 << 7);
					anIntArray1191[k2] = (anIntArray1190[k2 - 1] + anIntArray1190[k2 + 1]
							+ anIntArray1190[k2 - 128] + anIntArray1190[k2 + 128])
							/ 4;
				}

			}
			int ai[] = anIntArray1190;
			anIntArray1190 = anIntArray1191;
			anIntArray1191 = ai;
		}
		if (background != null) {
			int l1 = 0;
			for (int j2 = 0; j2 < background.height; j2++) {
				for (int l2 = 0; l2 < background.width; l2++)
					if (background.palettePixels[l1++] != 0) {
						int i3 = l2 + 16 + background.drawOffsetX;
						int j3 = j2 + 16 + background.drawOffsetY;
						int k3 = i3 + (j3 << 7);
						anIntArray1190[k3] = 0;
					}
			}
		}
	}

	private void appendPlayerUpdateMask(int mask, int index, Buffer buffer, Player player) {
		if ((mask & 0x400) != 0) {

			int initialX = buffer.readUByteS();
			int initialY = buffer.readUByteS();
			int destinationX = buffer.readUByteS();
			int destinationY = buffer.readUByteS();
			int startForceMovement = buffer.readLEUShortA() + tick;
			int endForceMovement = buffer.readUShortA() + tick;
			int animation = buffer.readLEUShortA();
			int direction = buffer.readUByteS();

			player.initialX = initialX;
			player.initialY = initialY;
			player.destinationX = destinationX;
			player.destinationY = destinationY;
			player.startForceMovement = startForceMovement;
			player.endForceMovement = endForceMovement;
			player.direction = direction;

			if(animation >= 0) {
				player.emoteAnimation = animation;
				player.displayedEmoteFrames = 0;
				player.emoteTimeRemaining = 0;
				player.animationDelay = 0;
				player.currentAnimationLoops = 0;
				player.anInt1542 = player.remainingPath;
			}


			player.resetPath();
		}
		if ((mask & 0x100) != 0) {
			player.graphic = buffer.readLEUShort();
			int info = buffer.readInt();
			player.graphicHeight = info >> 16;
						player.graphicDelay = tick + (info & 0xffff);
						player.currentAnimation = 0;
						player.anInt1522 = 0;
						if (player.graphicDelay > tick)
							player.currentAnimation = -1;
						if (player.graphic == 65535)
							player.graphic = -1;

						//Load the gfx...
						try {

							if (Frame.animationlist[Graphic.cache[player.graphic].animationSequence.primaryFrames[0] >> 16].length == 0) {
								resourceProvider.provide(1, Graphic.cache[player.graphic].animationSequence.primaryFrames[0] >> 16);
							}

						} catch (Exception e) {
							//	e.printStackTrace();
						}

		}
		if ((mask & 8) != 0) {
			int animation = buffer.readLEUShort();
			if (animation == 65535)
				animation = -1;
			int delay = buffer.readNegUByte();

			if (animation == player.emoteAnimation && animation != -1) {
				int replayMode = Animation.animations[animation].replayMode;
				if (replayMode == 1) {
					player.displayedEmoteFrames = 0;
					player.emoteTimeRemaining = 0;
					player.animationDelay = delay;
					player.currentAnimationLoops = 0;
				}
				if (replayMode == 2)
					player.currentAnimationLoops = 0;
			} else if (animation == -1 || player.emoteAnimation == -1
					|| Animation.animations[animation].forcedPriority >= Animation.animations[player.emoteAnimation].forcedPriority) {
				player.emoteAnimation = animation;
				player.displayedEmoteFrames = 0;
				player.emoteTimeRemaining = 0;
				player.animationDelay = delay;
				player.currentAnimationLoops = 0;
				player.anInt1542 = player.remainingPath;
			}
		}
		if ((mask & 4) != 0) {
			player.spokenText = buffer.readString();
			if (player.spokenText.charAt(0) == '~') {
				player.spokenText = player.spokenText.substring(1);
				sendMessage(player.spokenText, 2, player.name);
			} else if (player == localPlayer)
				sendMessage(player.spokenText, 2, player.name);
			player.textColour = 0;
			player.textEffect = 0;
			player.textCycle = 150;
		}
		if ((mask & 0x80) != 0) {
			int textInfo = buffer.readLEUShort();
			int privilege = buffer.readUnsignedByte();
			//	int offset = buffer.readNegUByte();
			//int off = buffer.currentPosition;
			if (player.name != null && player.visible) {
				long name = StringUtils.encodeBase37(player.name);
				boolean ignored = false;
				if (privilege <= 1) {
					for (int count = 0; count < ignoreCount; count++) {
						if (ignoreListAsLongs[count] != name)
							continue;
						ignored = true;
						break;
					}

				}
				if (!ignored && onTutorialIsland == 0)
					try {
						/*chatBuffer.currentPosition = 0;
						buffer.readReverseData(chatBuffer.payload, offset, 0);
						chatBuffer.currentPosition = 0;
						String text = ChatMessageCodec.decode(offset, chatBuffer);*/
						String text = buffer.readString();
						// s = Censor.doCensor(s);
						player.spokenText = text;
						player.textColour = textInfo >> 8;
					player.rights = privilege;
					player.textEffect = textInfo & 0xff;
					player.textCycle = 150;
					
					if(privilege > 0 && privilege < 10) {
						sendMessage(text, 1, "@cr"+privilege+"@" + player.name);
					} else {
						sendMessage(text, 2, player.name);
					}
					
					} catch (Exception exception) {
						SignLink.reporterror("cde2");
					}
			}
			//	buffer.currentPosition = off + offset;
		}
		if ((mask & 1) != 0) {
			player.interactingEntity = buffer.readLEUShort();
			if (player.interactingEntity == 65535)
				player.interactingEntity = -1;
		}
		if ((mask & 0x10) != 0) {
			int length = buffer.readNegUByte();
			byte data[] = new byte[length];
			Buffer appearanceBuffer = new Buffer(data);
			buffer.readBytes(length, 0, data);
			playerSynchronizationBuffers[index] = appearanceBuffer;
			player.updateAppearance(appearanceBuffer);
		}
		if ((mask & 2) != 0) {
			player.faceX = buffer.readLEUShortA();
			player.faceY = buffer.readLEUShort();
		}
		if ((mask & 0x20) != 0) {
			int damage = buffer.readShort();
			int type = buffer.readUnsignedByte();
			int hp = buffer.readShort();
			int maxHp = buffer.readShort();
			player.updateHitData(type, damage, tick);
			player.loopCycleStatus = tick + 300;
			player.currentHealth = hp;
			player.maxHealth = maxHp;
		}
		if ((mask & 0x200) != 0) {
			int damage = buffer.readShort();
			int type = buffer.readUnsignedByte();
			int hp = buffer.readShort();
			int maxHp = buffer.readShort();
			player.updateHitData(type, damage, tick);
			player.loopCycleStatus = tick + 300;
			player.currentHealth = hp;
			player.maxHealth = maxHp;
		}
	}

	private void checkForGameUsages() {
		try {
			int j = localPlayer.x + cameraX;
			int k = localPlayer.y + cameraY;
			if (anInt1014 - j < -500 || anInt1014 - j > 500 || anInt1015 - k < -500
					|| anInt1015 - k > 500) {
				anInt1014 = j;
				anInt1015 = k;
			}
			//Key camera rotation speeds below
			if (anInt1014 != j)
				anInt1014 += (j - anInt1014) / 16;
			if (anInt1015 != k)
				anInt1015 += (k - anInt1015) / 16;
			if (super.keyArray[1] == 1)
				anInt1186 += (-26 - anInt1186) / 2;
			else if (super.keyArray[2] == 1)
				anInt1186 += (26 - anInt1186) / 2;
			else
				anInt1186 /= 2;
			if (super.keyArray[3] == 1)
				anInt1187 += (12 - anInt1187) / 2;
			else if (super.keyArray[4] == 1)
				anInt1187 += (-12 - anInt1187) / 2;
			else
				anInt1187 /= 2;
			cameraHorizontal = cameraHorizontal + anInt1186 / 2 & 0x7ff;
			anInt1184 += anInt1187 / 2;
			if (anInt1184 < 128)
				anInt1184 = 128;
			if (anInt1184 > 383)
				anInt1184 = 383;
			int l = anInt1014 >> 7;
		int i1 = anInt1015 >> 7;
				int j1 = getCenterHeight(plane, anInt1015, anInt1014);
				int k1 = 0;
				if (l > 3 && i1 > 3 && l < 100 && i1 < 100) {
					for (int l1 = l - 4; l1 <= l + 4; l1++) {
						for (int k2 = i1 - 4; k2 <= i1 + 4; k2++) {
							int l2 = plane;
							if (l2 < 3 && (tileFlags[1][l1][k2] & 2) == 2)
								l2++;
							int i3 = j1 - tileHeights[l2][l1][k2];
							if (i3 > k1)
								k1 = i3;
						}

					}

				}
				anInt1005++;
				if (anInt1005 > 1512) {
					anInt1005 = 0;
					//unknown (anti-cheat) or maybe cutscene-related
					/*	outgoing.writeOpcode(77);
					outgoing.writeByte(0);                        
					int bufPos = outgoing.currentPosition;
					outgoing.writeByte((int) (Math.random() * 256D));
					outgoing.writeByte(101);
					outgoing.writeByte(233);
					outgoing.writeShort(45092);

					if ((int) (Math.random() * 2D) == 0) {
						outgoing.writeShort(35784);
					}

					outgoing.writeByte((int) (Math.random() * 256D));
					outgoing.writeByte(64);
					outgoing.writeByte(38);
					outgoing.writeShort((int) (Math.random() * 65536D));
					outgoing.writeShort((int) (Math.random() * 65536D));
					outgoing.writeBytes(outgoing.currentPosition - bufPos);*/
				}
				int j2 = k1 * 192;
				if (j2 > 0x17f00)
					j2 = 0x17f00;
				if (j2 < 32768)
					j2 = 32768;
				if (j2 > anInt984) {
					anInt984 += (j2 - anInt984) / 24;
					return;
				}
				if (j2 < anInt984) {
					anInt984 += (j2 - anInt984) / 80;
				}
		} catch (Exception _ex) {
			SignLink.reporterror("glfc_ex " + localPlayer.x + "," + localPlayer.y + ","
					+ anInt1014 + "," + anInt1015 + "," + this.regionX + "," + this.regionY
					+ "," + regionBaseX + "," + regionBaseY);
			throw new RuntimeException("eek");
		}
	}

	public void processDrawing() {
		if (rsAlreadyLoaded || loadingError || genericLoadingError) {
			showErrorScreen();
			return;
		}
		if (!loggedIn)
			if(!InWorldSelect) { 
				 drawLoginScreen(false);
			 } else { 
				 drawWorldSelect();
			 }
		else
			drawGameScreen();
		anInt1213 = 0;
	}

	private boolean isFriendOrSelf(String s) {
		if (s == null)
			return false;
		for (int i = 0; i < friendsCount; i++)
			if (s.equalsIgnoreCase(friendsList[i]))
				return true;
		return s.equalsIgnoreCase(localPlayer.name);
	}

	private static String combatDiffColor(int i, int j) {
		int k = i - j;
		if (k < -9)
			return "@red@";
		if (k < -6)
			return "@or3@";
		if (k < -3)
			return "@or2@";
		if (k < 0)
			return "@or1@";
		if (k > 9)
			return "@gre@";
		if (k > 6)
			return "@gr3@";
		if (k > 3)
			return "@gr2@";
		if (k > 0)
			return "@gr1@";
		else
			return "@yel@";
	}

	private void setWaveVolume(int i) {
		SignLink.wavevol = i;
	}

	private String objectMaps = "", floorMaps = "";
	private boolean AnnouncementBool;
	int secondsPassed = 0;

	private void draw3dScreen() {
		if (showChatComponents) {
			drawSplitPrivateChat();
		}
		if (crossType == 1) {
			int offSet = frameMode == ScreenMode.FIXED ? 4 : 0;
			crosses[crossIndex / 100].drawSprite(crossX - 8 - offSet, crossY - 8 - offSet);
			anInt1142++;
			if (anInt1142 > 67) {
				anInt1142 = 0;
				//sendPacket(new ClearMinimapFlag()); //Not server-sided, flag is only handled in the client
			}
		}
		if (crossType == 2) {
			int offSet = frameMode == ScreenMode.FIXED ? 4 : 0;
			crosses[4 + crossIndex / 100].drawSprite(crossX - 8 - offSet,
					crossY - 8 - offSet);
		}
		if (openWalkableInterface != -1) { 
			try {
				processWidgetAnimations(tickDelta, openWalkableInterface);
				Widget rsinterface = Widget.interfaceCache[openWalkableInterface];
				if (frameMode == ScreenMode.FIXED) {
					drawInterface(0, 0, rsinterface, 0);
				} else {
					Widget r =  Widget.interfaceCache[openWalkableInterface];
					int x = frameWidth - 215;
					x -= r.width;
					int min_y = Integer.MAX_VALUE;
					for(int i =0; i < r.children.length; i++) {
						min_y = Math.min(min_y, r.childY[i]);
					}   
					drawInterface(0, x,  Widget.interfaceCache[openWalkableInterface], 0 - min_y + 10);
				}
			} catch (Exception ex) {
			}
		}
		if (openInterfaceId != -1) {

			try {
				if(openInterfaceId == 5292) {
					processWidgetAnimations(tickDelta, openInterfaceId);
					drawInterface(0, frameMode == ScreenMode.FIXED ? 0 : (frameWidth / 2) - 356, Widget.interfaceCache[openInterfaceId], frameMode == ScreenMode.FIXED ? 0 : (frameHeight / 2) - 230);
					if(openInterfaceId == 5292) {
						drawBank(frameMode == ScreenMode.FIXED ? 0 : (frameWidth / 2) - 356, frameMode == ScreenMode.FIXED ? 0 : (frameHeight / 2) - 230);
					}
					if(bankItemDragSprite != null) {
						bankItemDragSprite.drawSprite(bankItemDragSpriteX, bankItemDragSpriteY);
					}
				} else {
					processWidgetAnimations(tickDelta, openInterfaceId);
					drawInterface(0, frameMode == ScreenMode.FIXED ? 0 : (frameWidth / 2) - 356,
							Widget.interfaceCache[openInterfaceId],
							frameMode == ScreenMode.FIXED ? 0 : (frameHeight / 2) - 230);
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		if (!menuOpen) {
			processRightClick();
			drawTooltip();
		} else if (menuScreenArea == 0) {
			drawMenu(frameMode == ScreenMode.FIXED ? 4 : 0,
					frameMode == ScreenMode.FIXED ? 4 : 0);
		}

		//Multi sign
		if (multicombat == 1) {
			multiOverlay.drawSprite(frameMode == ScreenMode.FIXED ? 10 : frameWidth - frameWidth + 10,
					frameHeight - 200);
		}

		//Effect timers
		drawEffectTimers();
		int x = regionBaseX + (localPlayer.x - 6 >> 7);
		int y = regionBaseY + (localPlayer.y - 6 >> 7);
		final String screenMode = frameMode == ScreenMode.FIXED ? "Fixed" : "Resizable";
		if (Configuration.clientData) {
			int textColour = 0xffff00;
			int fpsColour = 0xffff00;
			if (super.fps < 15) {
				fpsColour = 0xff0000;
			}
			// regularText.render(textColour,
			// "frameWidth: " + (mouseX - frameWidth) + ", frameHeight: " +
			// (mouseY - frameHeight),
			// frameHeight - 271, 5);
			regularText.render(fpsColour, "Fps: " + super.fps, 12,
					frameMode == ScreenMode.FIXED ? 470 : frameWidth - 265);
			Runtime runtime = Runtime.getRuntime();
			int clientMemory = (int) ((runtime.totalMemory() - runtime.freeMemory()) / 1024L);
			regularText.render(textColour, "Mem: " + clientMemory + "k", 27,
					frameMode == ScreenMode.FIXED ? 428 : frameWidth - 265);
			regularText.render(textColour, "Client Zoom: " + cameraZoom, 90,
					5);
			regularText.render(textColour,
					"Mouse X: " + super.mouseX + " , Mouse Y: " + super.mouseY, 30,
					5);
			regularText.render(textColour, "Coords: " + x + ", " + y, 45,
					5);
			regularText.render(textColour, "Client Mode: " + screenMode + "", 60,
					5);
			regularText.render(textColour,
					"Client Resolution: " + frameWidth + "x" + frameHeight, 75,
					5);
			regularText.render(0xffff00, "Object Maps: " + objectMaps,
					115,
					5);
			regularText.render(0xffff00, "Floor Maps: " + floorMaps,
					130,
					5);
		}
		if(AnnouncementBool) {
			int yOffset = frameMode == ScreenMode.FIXED ? 0 : frameHeight - 498;
			//regularText.render(0xffff00, "" + StringUtils.formatText(staffMessage), 329 + yOffset, 4);
			newRegularFont.drawBasicString(StringUtils.formatText(staffMessage), 4, 329 + yOffset, 0xffff00,
					1);
			
			secondsPassed++;
			if(secondsPassed > 10000) {
				AnnouncementBool = false;
				secondsPassed = 0;
			}
			
		}
		if (systemUpdateTime != 0) {
			int seconds = systemUpdateTime / 50;
			int minutes = seconds / 60;
			int yOffset = frameMode == ScreenMode.FIXED ? 0 : frameHeight - 498;
			seconds %= 60;
			if (seconds < 10)
				regularText.render(0xffff00,
						"System update in: " + minutes + ":0" + seconds, 329 + yOffset,
						4);
			else
				regularText.render(0xffff00, "System update in: " + minutes + ":" + seconds,
						329 + yOffset, 4);
			anInt849++;
			if (anInt849 > 75) {
				anInt849 = 0;
				//unknown (system updating)
				//outgoing.writeOpcode(148);
			}
		}
	}

	public void drawOnBankInterface()
	{
		/*if(openInterfaceId == 5292 && Widget.interfaceCache[27000].defaultText.equals("1"))
		{
			int total_tabs = Integer.parseInt(Widget.interfaceCache[27001].defaultText);
			currentBankTab = Integer.parseInt(Widget.interfaceCache[27002].defaultText);

			for(int k = 0; k <= total_tabs; k++)
			{
				Widget.interfaceCache[27014 + k].disabledSprite  = cacheSprite[123];
				if(k == 0) {
					Widget.interfaceCache[27014 + k].actions = new String[]{"Main Tab"};
				} else {
					Widget.interfaceCache[27014 + k].actions = new String[]{"Select Tab", "Collapse Tab"};
				}
			}

			for(int l = total_tabs + 1; l <= 8; l++)
			{
				Widget.interfaceCache[27014+ l].disabledSprite = null;
				Widget.interfaceCache[27014 + l].tooltip = null;
				Widget.interfaceCache[27014 + l].defaultText = null;
			}

			if(total_tabs != 8)
			{
				Widget.interfaceCache[27015 + total_tabs].disabledSprite = cacheSprite[122];
				Widget.interfaceCache[27015 + total_tabs].actions = new String[]{"Create Tab"};

			}

			if(currentBankTab == -1)
			{
				Widget.interfaceCache[27013].disabledSprite = cacheSprite[124];
			} else
				if(currentBankTab > 0)
				{
					Widget.interfaceCache[27014 + currentBankTab].disabledSprite = cacheSprite[126];
					Widget.interfaceCache[27014].disabledSprite =  cacheSprite[124];
				} else
				{
					Widget.interfaceCache[27014].disabledSprite = cacheSprite[121];
				}
			Widget.interfaceCache[27000].defaultText = "0";
		}*/
	}

	public boolean createBankTab() {
		if(openInterfaceId != 5292) {
			return false;
		}

		boolean fixed = frameMode == ScreenMode.FIXED;

		int offsetX = fixed ? 0 : (frameWidth - 765) / 2;
		int offsetY = fixed ? 0 : (frameHeight - 503) / 2;

		int[] offsets = { 61, 102, 142, 182, 222, 262, 302, 342, 382, 422 };
		if (anInt1084 >= 50300 && anInt1084 < 50312 && super.mouseY >= 40 + offsetY && super.mouseY <= 77 + offsetY) {
			for (int i = 0; i < offsets.length; i++) {
				if (super.mouseX < offsets[i] + offsetX) {
					sendPacket(new BankTabCreation(anInt1084, anInt1085, i));
					return true;
				}
			}
		}
		return false;
	}

	private Sprite bankItemDragSprite;
	private int bankItemDragSpriteX, bankItemDragSpriteY;

	private void addIgnore(long name) {
		if (name == 0L)
			return;
		if (ignoreCount >= 100) {
			sendMessage("Your ignore list is full. Max of 100 hit", 0, "");
			return;
		}
		String s = StringUtils.formatText(StringUtils.decodeBase37(name));
		for (int j = 0; j < ignoreCount; j++) {
			if (ignoreListAsLongs[j] == name) {
				sendMessage(s + " is already on your ignore list", 0, "");
				return;
			}
		}
		for (int k = 0; k < friendsCount; k++) {
			if (friendsListAsLongs[k] == name) {
				sendMessage("Please remove " + s + " from your friend list first", 0, "");
				return;
			}
		}
		ignoreListAsLongs[ignoreCount++] = name;
		sendPacket(new AddIgnore(name));
	}

	private void processPlayerMovement() {
		for (int index = -1; index < playerCount; index++) { 

			int playerIndex;  

			if (index == -1) {
				playerIndex = internalLocalPlayerIndex;
			} else {
				playerIndex = playerList[index];                        
			}

			Player player = players[playerIndex];

			if (player != null) {
				processMovement(player);                        
			}
		}

	}

	private void method115() {
		if (loadingStage == 2) {
			for (SpawnedObject spawnedObject = (SpawnedObject) spawns
					.reverseGetFirst(); spawnedObject != null; spawnedObject =
					(SpawnedObject) spawns.reverseGetNext()) {
				if (spawnedObject.getLongetivity > 0)
					spawnedObject.getLongetivity--;
				if (spawnedObject.getLongetivity == 0) {
					if (spawnedObject.getPreviousId < 0
							|| MapRegion.modelReady(spawnedObject.getPreviousId,
									spawnedObject.previousType)) {
						removeObject(spawnedObject.y, spawnedObject.plane,
								spawnedObject.previousOrientation,
								spawnedObject.previousType, spawnedObject.x,
								spawnedObject.group, spawnedObject.getPreviousId);
						spawnedObject.unlink();
					}
				} else {
					if (spawnedObject.delay > 0)
						spawnedObject.delay--;
					if (spawnedObject.delay == 0 && spawnedObject.x >= 1
							&& spawnedObject.y >= 1 && spawnedObject.x <= 102
							&& spawnedObject.y <= 102
							&& (spawnedObject.id < 0 || MapRegion.modelReady(
									spawnedObject.id, spawnedObject.type))) {
						removeObject(spawnedObject.y, spawnedObject.plane,
								spawnedObject.orientation, spawnedObject.type,
								spawnedObject.x, spawnedObject.group,
								spawnedObject.id);
						spawnedObject.delay = -1;
						if (spawnedObject.id == spawnedObject.getPreviousId
								&& spawnedObject.getPreviousId == -1)
							spawnedObject.unlink();
						else if (spawnedObject.id == spawnedObject.getPreviousId
								&& spawnedObject.orientation == spawnedObject.previousOrientation
								&& spawnedObject.type == spawnedObject.previousType)
							spawnedObject.unlink();
					}
				}
			}

		}
	}

	private void determineMenuSize() {
		int boxLength = boldText.getTextWidth("Choose option");
		for (int row = 0; row < menuActionRow; row++) {
			int actionLength = boldText.getTextWidth(menuActionText[row]);
			if (actionLength > boxLength)
				boxLength = actionLength;
		}
		boxLength += 8;
		int offset = 15 * menuActionRow + 21;
		if (super.saveClickX > 0 && super.saveClickY > 0 && super.saveClickX < frameWidth
				&& super.saveClickY < frameHeight) {
			int xClick = super.saveClickX - boxLength / 2;
			if (xClick + boxLength > frameWidth - 4) {
				xClick = frameWidth - 4 - boxLength;
			}
			if (xClick < 0) {
				xClick = 0;
			}
			int yClick = super.saveClickY - 0;
			if (yClick + offset > frameHeight - 6) {
				yClick = frameHeight - 6 - offset;
			}
			if (yClick < 0) {
				yClick = 0;
			}
			menuOpen = true;
			menuOffsetX = xClick;
			menuOffsetY = yClick;
			menuWidth = boxLength;
			menuHeight = 15 * menuActionRow + 22;
		}
	}

	private void updateLocalPlayerMovement(Buffer stream) {
		stream.initBitAccess();

		int update = stream.readBits(1);

		if (update == 0) {
			return;
		}

		int type = stream.readBits(2);
		if (type == 0) {
			mobsAwaitingUpdate[mobsAwaitingUpdateCount++] = internalLocalPlayerIndex;
			return;
		}
		if (type == 1) {
			int direction = stream.readBits(3);
			localPlayer.moveInDir(false, direction);
			int updateRequired = stream.readBits(1);

			if (updateRequired == 1) {
				mobsAwaitingUpdate[mobsAwaitingUpdateCount++] = internalLocalPlayerIndex;
			}
			return;
		}
		if (type == 2) {
			int firstDirection = stream.readBits(3);
			localPlayer.moveInDir(true, firstDirection);

			int secondDirection = stream.readBits(3);
			localPlayer.moveInDir(true, secondDirection);

			int updateRequired = stream.readBits(1);

			if (updateRequired == 1) {
				mobsAwaitingUpdate[mobsAwaitingUpdateCount++] = internalLocalPlayerIndex;
			}
			return;
		}
		if (type == 3) {
			plane = stream.readBits(2);

			//Fix for height changes
			if(lastKnownPlane != plane) {
				loadingStage = 1;
			}
			lastKnownPlane = plane;

			int teleport = stream.readBits(1);
			int updateRequired = stream.readBits(1);


			if (updateRequired == 1) {
				mobsAwaitingUpdate[mobsAwaitingUpdateCount++] = internalLocalPlayerIndex;
			}

			int y = stream.readBits(7);
			int x = stream.readBits(7);

			localPlayer.setPos(x, y, teleport == 1);
		}
	}

	private void nullLoader() {
		aBoolean831 = false;
		while (drawingFlames) {
			aBoolean831 = false;
			try {
				Thread.sleep(50L);
			} catch (Exception _ex) {
			}
		}
		titleBoxIndexedImage = null;
		titleButtonIndexedImage = null;
		titleIndexedImages = null;
		anIntArray850 = null;
		anIntArray851 = null;
		anIntArray852 = null;
		anIntArray853 = null;
		anIntArray1190 = null;
		anIntArray1191 = null;
		anIntArray828 = null;
		anIntArray829 = null;
		flameLeftSprite = null;
		flameRightSprite = null;
	}

	private boolean processWidgetAnimations(int tick, int interfaceId) throws Exception {
		boolean redrawRequired = false;
		Widget widget = Widget.interfaceCache[interfaceId];

		if(widget == null
				|| widget.children == null) {
			return false;
		}

		for (int element : widget.children) {                  
			if (element == -1) {
				break;
			}

			Widget child = Widget.interfaceCache[element];  

			if (child.type == Widget.TYPE_MODEL_LIST) {
				redrawRequired |= processWidgetAnimations(tick, child.id);
			}
			

			if (child.type == 6 && (child.defaultAnimationId != -1 || child.secondaryAnimationId != -1)) {
				boolean updated = interfaceIsSelected(child); 

				int animationId = updated ? child.secondaryAnimationId : child.defaultAnimationId;

				if (animationId != -1) {
					Animation animation = Animation.animations[animationId];
					for (child.lastFrameTime += tick; child.lastFrameTime > animation .duration(child.currentFrame);) {
						child.lastFrameTime -= animation.duration(child.currentFrame) + 1;
						child.currentFrame++;
						if (child.currentFrame >= animation.frameCount) {
							child.currentFrame -= animation.loopOffset;
							if (child.currentFrame < 0
									|| child.currentFrame >= animation.frameCount)
								child.currentFrame = 0;
						}
						redrawRequired = true;
					}

				}
			}
		}

		return redrawRequired;
	}

	private int setCameraLocation() {
		if (!Configuration.enableRoofs)
			return plane;
		int j = 3;
		if (yCameraCurve < 310) {
			int k = xCameraPos >> 7;
		int l = yCameraPos >> 7;
		int i1 = localPlayer.x >> 7;
		int j1 = localPlayer.y >> 7;
		if ((tileFlags[plane][k][l] & 4) != 0)
			j = plane;
		int k1;
		if (i1 > k)
			k1 = i1 - k;
		else
			k1 = k - i1;
		int l1;
		if (j1 > l)
			l1 = j1 - l;
		else
			l1 = l - j1;
		if (k1 > l1) {
			int i2 = (l1 * 0x10000) / k1;
			int k2 = 32768;
			while (k != i1) {
				if (k < i1)
					k++;
				else if (k > i1)
					k--;
				if ((tileFlags[plane][k][l] & 4) != 0)
					j = plane;
				k2 += i2;
				if (k2 >= 0x10000) {
					k2 -= 0x10000;
					if (l < j1)
						l++;
					else if (l > j1)
						l--;
					if ((tileFlags[plane][k][l] & 4) != 0)
						j = plane;
				}
			}
		} else {
			int j2 = (k1 * 0x10000) / l1;
			int l2 = 32768;
			while (l != j1) {
				if (l < j1)
					l++;
				else if (l > j1)
					l--;
				if ((tileFlags[plane][k][l] & 4) != 0)
					j = plane;
				l2 += j2;
				if (l2 >= 0x10000) {
					l2 -= 0x10000;
					if (k < i1)
						k++;
					else if (k > i1)
						k--;
					if ((tileFlags[plane][k][l] & 4) != 0)
						j = plane;
				}
			}
		}
		}
		if ((tileFlags[plane][localPlayer.x >> 7][localPlayer.y >> 7] & 4) != 0)
			j = plane;
		return j;
	}

	private int resetCameraHeight() {
		int orientation = getCenterHeight(plane, yCameraPos, xCameraPos);
		if (orientation - zCameraPos < 800
				&& (tileFlags[plane][xCameraPos >> 7][yCameraPos >> 7] & 4) != 0)
			return plane;
		else
			return 3;
	}

	private void removeFriend(long name) {
		if (name == 0L)
			return;
		sendPacket(new DeleteFriend(name));
	}

	private void removeIgnore(long name) {
		if (name == 0L)
			return;
		for (int index = 0; index < ignoreCount; index++) {
			if (ignoreListAsLongs[index] == name) {
				ignoreCount--;
				System.arraycopy(ignoreListAsLongs, index + 1, ignoreListAsLongs,
						index, ignoreCount - index);
				break;
			}
		}
		sendPacket(new DeleteIgnore(name));
	}

	public String getParameter(String s) {
		if (SignLink.mainapp != null)
			return SignLink.mainapp.getParameter(s);
		else
			return super.getParameter(s);
	}

	private int executeScript(Widget widget, int id) {
		if (widget.valueIndexArray == null || id >= widget.valueIndexArray.length)
			return -2;
		try {
			int script[] = widget.valueIndexArray[id];
			int accumulator = 0;
			int counter = 0;
			int operator = 0;
			do {
				int instruction = script[counter++];
				int value = 0;
				byte next = 0;

				if (instruction == 0) {
					return accumulator;
				}

				if (instruction == 1) {
					value = currentLevels[script[counter++]];
				}

				if (instruction == 2) {
					value = maximumLevels[script[counter++]];
				}

				if (instruction == 3) {
					value = currentExp[script[counter++]];
				}

				if (instruction == 4) {
					Widget other = Widget.interfaceCache[script[counter++]];
					int item = script[counter++];
					if (item >= 0 && item < ItemDefinition.item_count
							&& (!ItemDefinition.lookup(item).is_members_only
									|| isMembers)) {
						for (int slot = 0; slot < other.inventoryItemId.length; slot++)
							if (other.inventoryItemId[slot] == item + 1)
								value += other.inventoryAmounts[slot];

					}
				}
				if (instruction == 5) {
					value = settings[script[counter++]];
				}

				if (instruction == 6) {
					value = SKILL_EXPERIENCE[maximumLevels[script[counter++]] - 1];
				}

				if (instruction == 7) {
					value = (settings[script[counter++]] * 100) / 46875;
				}

				if (instruction == 8) {
					value = localPlayer.combatLevel;
				}

				if (instruction == 9) {
					for (int skill = 0; skill < SkillConstants.SKILL_COUNT; skill++)
						if (SkillConstants.ENABLED_SKILLS[skill])
							value += maximumLevels[skill];
				}

				if (instruction == 10) {
					Widget other = Widget.interfaceCache[script[counter++]];
					int item = script[counter++] + 1;
					if (item >= 0 && item < ItemDefinition.item_count && isMembers) {
						for (int stored =
								0; stored < other.inventoryItemId.length; stored++) {
							if (other.inventoryItemId[stored] != item)
								continue;
							value = 0x3b9ac9ff;
							break;
						}

					}
				}

				if (instruction == 11) {
					value = runEnergy;
				}

				if (instruction == 12) {
					value = weight;
				}

				if (instruction == 13) {
					int bool = settings[script[counter++]];
					int shift = script[counter++];
					value = (bool & 1 << shift) == 0 ? 0 : 1;
				}

				if (instruction == 14) {
					int index = script[counter++];
					VariableBits bits = VariableBits.varbits[index];
					int setting = bits.getSetting();
					int low = bits.getLow();
					int high = bits.getHigh();
					int mask = BIT_MASKS[high - low];
					value = settings[setting] >> low & mask;
				}

				if (instruction == 15) {
					next = 1;
				}

				if (instruction == 16) {
					next = 2;
				}

				if (instruction == 17) {
					next = 3;
				}

				if (instruction == 18) {
					value = (localPlayer.x >> 7) + regionBaseX;
				}

				if (instruction == 19) {
					value = (localPlayer.y >> 7) + regionBaseY;
				}

				if (instruction == 20) {
					value = script[counter++];
				}

				if (next == 0) {

					if (operator == 0) {
						accumulator += value;
					}

					if (operator == 1) {
						accumulator -= value;
					}

					if (operator == 2 && value != 0) {
						accumulator /= value;
					}

					if (operator == 3) {
						accumulator *= value;
					}
					operator = 0;
				} else {
					operator = next;
				}
			} while (true);
		} catch (Exception _ex) {
			return -1;
		}
	}

	private void drawTooltip() {
		if (menuActionRow < 2 && itemSelected == 0 && spellSelected == 0)
			return;
		String s;
		if (itemSelected == 1 && menuActionRow < 2)
			s = "Use " + selectedItemName + " with...";
		else if (spellSelected == 1 && menuActionRow < 2)
			s = spellTooltip + "...";
		else
			s = menuActionText[menuActionRow - 1];
		if (menuActionRow > 2)
			s = s + "@whi@ / " + (menuActionRow - 2) + " more options";
		boldText.method390(4, 0xffffff, s, tick / 1000, 15);
	}

	private void markMinimap(Sprite sprite, int x, int y) {
		if (sprite == null) {
			return;
		}
		int angle = cameraHorizontal + minimapRotation & 0x7ff;
		int l = x * x + y * y;
		if (l > 6400) {
			return;
		}
		int sineAngle = Model.SINE[angle];
		int cosineAngle = Model.COSINE[angle];
		sineAngle = (sineAngle * 256) / (minimapZoom + 256);
		cosineAngle = (cosineAngle * 256) / (minimapZoom + 256);
		int spriteOffsetX = y * sineAngle + x * cosineAngle >> 16;
		int spriteOffsetY = y * cosineAngle - x * sineAngle >> 16;
		if (frameMode == ScreenMode.FIXED) {
			sprite.drawSprite(((94 + spriteOffsetX) - sprite.maxWidth / 2) + 4 + 30,
					83 - spriteOffsetY - sprite.maxHeight / 2 - 4 + 5);
		} else {
			sprite.drawSprite(
					((77 + spriteOffsetX) - sprite.maxWidth / 2) + 4 + 5
					+ (frameWidth - 167),
					85 - spriteOffsetY - sprite.maxHeight / 2);
		}
	}

	private void drawMinimap() {
		if (frameMode == ScreenMode.FIXED) {
			minimapImageProducer.initDrawingArea();
		}
		if (minimapState == 2) {
			if (frameMode == ScreenMode.FIXED) {
				cacheSprite[19].drawSprite(0, 0);
			} else {
				cacheSprite[44].drawSprite(frameWidth - 181, 0);
				cacheSprite[45].drawSprite(frameWidth - 158, 7);
			}
			
			if (frameMode != ScreenMode.FIXED && changeTabArea) {
				if (super.mouseX >= frameWidth - 26 && super.mouseX <= frameWidth - 1
						&& super.mouseY >= 2 && super.mouseY <= 24 || tabId == 15) {
					cacheSprite[27].drawSprite(frameWidth - 25, 2);
				} else {
					cacheSprite[27].drawAdvancedSprite(frameWidth - 25, 2, 165);
				}
			}
			loadAllOrbs(frameMode == ScreenMode.FIXED ? 0 : frameWidth - 217);
			compass.rotate(33, cameraHorizontal, anIntArray1057, 256, anIntArray968,
					(frameMode == ScreenMode.FIXED ? 25 : 24), 4,
					(frameMode == ScreenMode.FIXED ? 29 : frameWidth - 176), 33, 25);
			if (menuOpen) {
				drawMenu(frameMode == ScreenMode.FIXED ? 516 : 0, 0);
			}
			if (frameMode == ScreenMode.FIXED) {
				minimapImageProducer.initDrawingArea();
			}
			return;
		}
		int angle = cameraHorizontal + minimapRotation & 0x7ff;
		int centreX = 48 + localPlayer.x / 32;
		int centreY = 464 - localPlayer.y / 32;
		minimapImage.rotate(151, angle, minimapLineWidth, 256 + minimapZoom, minimapLeft,
				centreY, (frameMode == ScreenMode.FIXED ? 9 : 7),
				(frameMode == ScreenMode.FIXED ? 54 : frameWidth - 158), 146, centreX);
		for (int icon = 0; icon < anInt1071; icon++) {
			int mapX = (minimapHintX[icon] * 4 + 2) - localPlayer.x / 32;
			int mapY = (minimapHintY[icon] * 4 + 2) - localPlayer.y / 32;
			markMinimap(minimapHint[icon], mapX, mapY);
		}
		for (int x = 0; x < 104; x++) {
			for (int y = 0; y < 104; y++) {
				Deque class19 = groundItems[plane][x][y];
				if (class19 != null) {
					int mapX = (x * 4 + 2) - localPlayer.x / 32;
					int mapY = (y * 4 + 2) - localPlayer.y / 32;
					markMinimap(mapDotItem, mapX, mapY);
				}
			}
		}
		for (int n = 0; n < npcCount; n++) {
			Npc npc = npcs[npcIndices[n]];
			if (npc != null && npc.isVisible()) {
				NpcDefinition entityDef = npc.desc;
				if (entityDef.childrenIDs != null) {
					entityDef = entityDef.morph();
				}
				if (entityDef != null && entityDef.drawMinimapDot && entityDef.clickable) {
					int mapX = npc.x / 32 - localPlayer.x / 32;
					int mapY = npc.y / 32 - localPlayer.y / 32;
					markMinimap(mapDotNPC, mapX, mapY);
				}
			}
		}
		for (int p = 0; p < playerCount; p++) {            	
			Player player = players[playerList[p]];
			if (player != null && player.isVisible()) {
				int mapX = player.x / 32 - localPlayer.x / 32;
				int mapY = player.y / 32 - localPlayer.y / 32;
				boolean friend = false;
				boolean clanMember = false;

				for (int i = 37144; i <= 37244; i++) {
					if(Widget.interfaceCache[i].defaultText.toLowerCase().
							contains(player.name.toLowerCase())) {
						clanMember = true;
					}
				}

				long nameHash = StringUtils.encodeBase37(player.name);
				for (int f = 0; f < friendsCount; f++) {
					if (nameHash != friendsListAsLongs[f] || friendsNodeIDs[f] == 0) {
						continue;
					}
					friend = true;
					break;
				}
				boolean team = false;
				if (localPlayer.team != 0 && player.team != 0
						&& localPlayer.team == player.team) {
					team = true;
				}
				if (friend) {
					markMinimap(mapDotFriend, mapX, mapY);
				} else if (clanMember) {
					markMinimap(mapDotClan, mapX, mapY);
				} else if (team) {
					markMinimap(mapDotTeam, mapX, mapY);
				} else {
					markMinimap(mapDotPlayer, mapX, mapY);
				}
			}
		}
		if (hintIconDrawType != 0 && tick % 20 < 10) {
			if (hintIconDrawType == 1 && hintIconNpcId >= 0 && hintIconNpcId < npcs.length) {
				Npc npc = npcs[hintIconNpcId];
				if (npc != null) {
					int mapX = npc.x / 32 - localPlayer.x / 32;
					int mapY = npc.y / 32 - localPlayer.y / 32;
					refreshMinimap(mapMarker, mapY, mapX);
				}
			}
			if (hintIconDrawType == 2) {
				int mapX = ((hintIconX - regionBaseX) * 4 + 2) - localPlayer.x / 32;
				int mapY = ((hintIconY - regionBaseY) * 4 + 2) - localPlayer.y / 32;
				refreshMinimap(mapMarker, mapY, mapX);
			}
			if (hintIconDrawType == 10 && hintIconPlayerId >= 0
					&& hintIconPlayerId < players.length) {
				Player player = players[hintIconPlayerId];
				if (player != null) {
					int mapX = player.x / 32 - localPlayer.x / 32;
					int mapY = player.y / 32 - localPlayer.y / 32;
					refreshMinimap(mapMarker, mapY, mapX);
				}
			}
		}
		if (destinationX != 0) {
			int mapX = (destinationX * 4 + 2) - localPlayer.x / 32;
			int mapY = (destY * 4 + 2) - localPlayer.y / 32;
			markMinimap(mapFlag, mapX, mapY);
		}
		Rasterizer2D.drawBox((frameMode == ScreenMode.FIXED ? 127 : frameWidth - 88), (frameMode == ScreenMode.FIXED ? 83 : 80), 3, 3,
				0xffffff);
		if (frameMode == ScreenMode.FIXED) {
			cacheSprite[19].drawSprite(0, 0);
		} else {
			cacheSprite[44].drawSprite(frameWidth - 181, 0);
		}
		compass.rotate(33, cameraHorizontal, anIntArray1057, 256, anIntArray968,
				(frameMode == ScreenMode.FIXED ? 25 : 24), 4,
				(frameMode == ScreenMode.FIXED ? 29 : frameWidth - 176), 33, 25);
		
		if (frameMode != ScreenMode.FIXED && changeTabArea) {
			if (super.mouseX >= frameWidth - 26 && super.mouseX <= frameWidth - 1
					&& super.mouseY >= 2 && super.mouseY <= 24 || tabId == 10) {
				cacheSprite[27].drawSprite(frameWidth - 25, 2);
			} else {
				cacheSprite[27].drawAdvancedSprite(frameWidth - 25, 2, 165);
			}
		}
		loadAllOrbs(frameMode == ScreenMode.FIXED ? 0 : frameWidth - 217);
		if (menuOpen) {
			drawMenu(frameMode == ScreenMode.FIXED ? 516 : 0, 0);
		}
		if (frameMode == ScreenMode.FIXED) {
			gameScreenImageProducer.initDrawingArea();
		}
	}

	private void loadAllOrbs(int xOffset) {
		loadHpOrb(xOffset);
		loadPrayerOrb(xOffset);
		loadRunOrb(xOffset);
		//loadSpecialOrb(xOffset);
		if (frameMode == ScreenMode.FIXED) {
			cacheSprite[expCounterHover ? 23 : 22].drawSprite(0, 21);
		} else {
			cacheSprite[expCounterHover ? 23 : 22].drawSprite(frameWidth - 215, 21);
		}
		if (frameMode == ScreenMode.FIXED) {
			cacheSprite[503].drawSprite(197, 130);
			cacheSprite[worldHover ? 505 : 504].drawSprite(201, 134);
		} else {
			cacheSprite[503].drawSprite(frameWidth - 36, 135);
			cacheSprite[worldHover ? 505 : 504].drawSprite(frameWidth - 32, 139);
		}
	}

	private int poisonType = 0;

	private void loadHpOrb(int xOffset) {
		int hover = poisonType == 0 ? 8 : 7;
		Sprite bg = cacheSprite[hpHover ? hover : 7];
		int id = 0;
		if (poisonType == 0)
			id = 0;
		if (poisonType == 1)
			id = 177;
		if (poisonType == 2)
			id = 5;
		Sprite fg = cacheSprite[id];
		bg.drawSprite(0 + xOffset, 41);
		fg.drawSprite(27 + xOffset, 45);
		int level = currentLevels[3];
		int max = maximumLevels[3];
		double percent = level / (double) max;
		cacheSprite[14].myHeight = (int) (26 * (1 - percent));
		cacheSprite[14].drawSprite(27 + xOffset, 45);
		if (percent <= .25) {
			cacheSprite[9].drawSprite1(33 + xOffset, 52,
					200 + (int) (50 * Math.sin(tick / 7.0)));
		} else {
			cacheSprite[9].drawSprite(33 + xOffset, 52);
		}
		smallText.method382(getOrbTextColor((int) (percent * 100)), 15 + xOffset, "" + level,
				67, true);
	}

	private void loadPrayerOrb(int xOffset) {
		Sprite bg = cacheSprite[prayHover ? 8 : 7];
		Sprite fg = cacheSprite[prayClicked ? 2 : 1];
		bg.drawSprite(0 + xOffset, 85);
		fg.drawSprite(27 + xOffset, 89);
		int level = currentLevels[5];
		int max = maximumLevels[5];
		double percent = level / (double) max;
		cacheSprite[14].myHeight = (int) (26 * (1 - percent));
		cacheSprite[14].drawSprite(27 + xOffset, 89);
		if (percent <= .25) {
			cacheSprite[10].drawSprite1(30 + xOffset, 92,
					200 + (int) (50 * Math.sin(tick / 7.0)));
		} else {
			cacheSprite[10].drawSprite(30 + xOffset, 92);
		}
		smallText.method382(getOrbTextColor((int) (percent * 100)), 16 + xOffset, level + "",
				111, true);
	}

	private void loadRunOrb(int xOffset) {
		Sprite bg = cacheSprite[runHover ? 8 : 7];
		Sprite fg = cacheSprite[settings[152] == 1 ? 4 : 3];
		bg.drawSprite(24 + xOffset, 122);
		fg.drawSprite(51 + xOffset, 126);
		int level = runEnergy;
		double percent = level / (double) 100;
		cacheSprite[14].myHeight = (int) (26 * (1 - percent));
		cacheSprite[14].drawSprite(51 + xOffset, 126);
		if (percent <= .25) {
			cacheSprite[settings[152] == 1 ? 12 : 11].drawSprite1(58 + xOffset, 130,
					200 + (int) (50 * Math.sin(tick / 7.0)));
		} else {
			cacheSprite[settings[152] == 1 ? 12 : 11].drawSprite(58 + xOffset, 130);
		}
		smallText.method382(getOrbTextColor((int) (percent * 100)), 40 + xOffset, level + "",
				148, true);
	}

	private int specialAttack = 0;

	private void loadSpecialOrb(int xOffset) {
		Sprite image = cacheSprite[specialHover ? 56 : 42];
		Sprite fill = cacheSprite[specialEnabled == 0 ? 5 : 6];
		Sprite sword = cacheSprite[55];
		double percent = specialAttack / (double) 100;
		boolean isFixed = frameMode == ScreenMode.FIXED;
		image.drawSprite((isFixed ? 170 : 159) + xOffset, isFixed ? 122 : 147);
		fill.drawSprite((isFixed ? 174 : 163) + xOffset, isFixed ? 126 : 151);
		cacheSprite[14].myHeight = (int) (26 * (1 - percent));
		cacheSprite[14].drawSprite((isFixed ? 175 : 163) + xOffset, isFixed ? 127 : 151);
		sword.drawSprite((isFixed ? 179 : 168) + xOffset, isFixed ? 131 : 156);
		smallText.method382(getOrbTextColor((int) (percent * 100)),
				(isFixed ? 212 : 202) + xOffset, specialAttack + "", isFixed ? 148 : 173,
						true);
	}

	private void npcScreenPos(Mob entity, int i) {
		calcEntityScreenPos(entity.x, i, entity.y);
	}

	private void calcEntityScreenPos(int i, int j, int l) {
		if (i < 128 || l < 128 || i > 13056 || l > 13056) {
			spriteDrawX = -1;
			spriteDrawY = -1;
			return;
		}
		int i1 = getCenterHeight(plane, l, i) - j;
		i -= xCameraPos;
		i1 -= zCameraPos;
		l -= yCameraPos;
		int j1 = Model.SINE[yCameraCurve];
		int k1 = Model.COSINE[yCameraCurve];
		int l1 = Model.SINE[xCameraCurve];
		int i2 = Model.COSINE[xCameraCurve];
		int j2 = l * l1 + i * i2 >> 16;
		l = l * i2 - i * l1 >> 16;
		i = j2;
		j2 = i1 * k1 - l * j1 >> 16;
				l = i1 * j1 + l * k1 >> 16;
		i1 = j2;
		if (l >= 50) {
			spriteDrawX = Rasterizer3D.originViewX + (i << SceneGraph.viewDistance) / l;
			spriteDrawY = Rasterizer3D.originViewY + (i1 << SceneGraph.viewDistance) / l;
		} else {
			spriteDrawX = -1;
			spriteDrawY = -1;
		}
	}
	
	private void buildSplitPrivateChatMenu() {
		if (splitPrivateChat == 0)
			return;
		int message = 0;
		 if(AnnouncementBool) { 
			 message = 1; 
		 }
		if (systemUpdateTime != 0)
			message = 1;
		for (int index = 0; index < 100; index++)
			if (chatMessages[index] != null) {
				int type = chatTypes[index];
				String name = chatNames[index];
				byte data = 0;
				if (name != null && name.startsWith("@cr1@")) {
					name = name.substring(5);
					data = 1;
				} else if (name != null && name.startsWith("@cr2@")) {
					name = name.substring(5);
					data = 2;
				} else if (name != null && name.startsWith("@cr3@")) {
					name = name.substring(5);
					data = 3;
				} else if (name != null && name.startsWith("@cr4@")) {
					name = name.substring(5);
					data = 4;
				} else if (name != null && name.startsWith("@cr5@")) {
					name = name.substring(5);
					data = 5;
				} else if (name != null && name.startsWith("@cr6@")) {
					name = name.substring(5);
					data = 6;
				} else if (name != null && name.startsWith("@cr7@")) {
					name = name.substring(5);
					data = 7;
				} else if (name != null && name.startsWith("@cr8@")) {
					name = name.substring(5);
					data = 8;
				} else if (name != null && name.startsWith("@cr9@")) {
					name = name.substring(5);
					data = 9;
				} else if (name != null && name.startsWith("@cr10@")) {
					name = name.substring(6);
					data = 10;
				}
				if ((type == 3 || type == 7) && (type == 7 || privateChatMode == 0
						|| privateChatMode == 1 && isFriendOrSelf(name))) {
					int offSet = frameMode == ScreenMode.FIXED ? 4 : 0;
					int y = 329 - message * 13;
					if (frameMode != ScreenMode.FIXED) {
						y = frameHeight - 170 - message * 13;
					}
					if (super.mouseX > 4 && super.mouseY - offSet > y - 10
							&& super.mouseY - offSet <= y + 3) {
						int i1 = regularText.getTextWidth(
								"From:  " + name + chatMessages[index]) + 25;
						if (i1 > 450)
							i1 = 450;
						if (super.mouseX < 4 + i1) {
							if(!isFriendOrSelf(name)) {
								menuActionText[menuActionRow] = "Add ignore @whi@" + name;
								menuActionTypes[menuActionRow] = 2042;
								menuActionRow++;
								menuActionText[menuActionRow] = "Add friend @whi@" + name;
								menuActionTypes[menuActionRow] = 2337;
								menuActionRow++;
							} else {
								menuActionText[menuActionRow] = "Message @whi@" + name;
								menuActionTypes[menuActionRow] = 2639;
								menuActionRow++;
							}
						}
					}
					if (++message >= 5)
						return;
				}
				if ((type == 5 || type == 6) && privateChatMode < 2 && ++message >= 5)
					return;
			}

	}

	private void requestSpawnObject(int longetivity, int id, int orientation, int group, int y, int type,
			int plane, int x, int delay) {
		SpawnedObject object = null;
		for (SpawnedObject node = (SpawnedObject) spawns.reverseGetFirst(); node != null; node =
				(SpawnedObject) spawns.reverseGetNext()) {
			if (node.plane != plane || node.x != x || node.y != y || node.group != group)
				continue;
			object = node;
			break;
		}

		if (object == null) {
			object = new SpawnedObject();
			object.plane = plane;
			object.group = group;
			object.x = x;
			object.y = y;
			method89(object);
			spawns.insertHead(object);
		}
		object.id = id;
		object.type = type;
		object.orientation = orientation;
		object.delay = delay;
		object.getLongetivity = longetivity;
	}

	private boolean interfaceIsSelected(Widget widget) {
		if (widget.valueCompareType == null)
			return false;
		for (int i = 0; i < widget.valueCompareType.length; i++) {
			int j = executeScript(widget, i);
			int k = widget.requiredValues[i];
			if (widget.valueCompareType[i] == 2) {
				if (j >= k)
					return false;
			} else if (widget.valueCompareType[i] == 3) {
				if (j <= k)
					return false;
			} else if (widget.valueCompareType[i] == 4) {
				if (j == k)
					return false;
			} else if (j != k)
				return false;
		}

		return true;
	}

	private void doFlamesDrawing() {
		char c = '\u0100';
		if (anInt1040 > 0) {
			for (int i = 0; i < 256; i++)
				if (anInt1040 > 768)
					anIntArray850[i] = method83(anIntArray851[i], anIntArray852[i],
							1024 - anInt1040);
				else if (anInt1040 > 256)
					anIntArray850[i] = anIntArray852[i];
				else
					anIntArray850[i] = method83(anIntArray852[i], anIntArray851[i],
							256 - anInt1040);

		} else if (anInt1041 > 0) {
			for (int j = 0; j < 256; j++)
				if (anInt1041 > 768)
					anIntArray850[j] = method83(anIntArray851[j], anIntArray853[j],
							1024 - anInt1041);
				else if (anInt1041 > 256)
					anIntArray850[j] = anIntArray853[j];
				else
					anIntArray850[j] = method83(anIntArray853[j], anIntArray851[j],
							256 - anInt1041);

		} else {
			System.arraycopy(anIntArray851, 0, anIntArray850, 0, 256);

		}
		System.arraycopy(flameLeftSprite.myPixels, 0,
				flameLeftBackground.canvasRaster, 0, 33920);

		int i1 = 0;
		int j1 = 1152;
		for (int k1 = 1; k1 < c - 1; k1++) {
			int l1 = (anIntArray969[k1] * (c - k1)) / c;
			int j2 = 22 + l1;
			if (j2 < 0)
				j2 = 0;
			i1 += j2;
			for (int l2 = j2; l2 < 128; l2++) {
				int j3 = anIntArray828[i1++];
				if (j3 != 0) {
					int l3 = j3;
					int j4 = 256 - j3;
					j3 = anIntArray850[j3];
					int l4 = flameLeftBackground.canvasRaster[j1];
					flameLeftBackground.canvasRaster[j1++] =
							((j3 & 0xff00ff) * l3 + (l4 & 0xff00ff) * j4 & 0xff00ff00)
							+ ((j3 & 0xff00) * l3 + (l4 & 0xff00) * j4
									& 0xff0000) >> 8;
				} else {
					j1++;
				}
			}

			j1 += j2;
		}

		flameLeftBackground.drawGraphics(0, super.graphics, 0);
		System.arraycopy(flameRightSprite.myPixels, 0,
				flameRightBackground.canvasRaster, 0, 33920);

		i1 = 0;
		j1 = 1176;
		for (int k2 = 1; k2 < c - 1; k2++) {
			int i3 = (anIntArray969[k2] * (c - k2)) / c;
			int k3 = 103 - i3;
			j1 += i3;
			for (int i4 = 0; i4 < k3; i4++) {
				int k4 = anIntArray828[i1++];
				if (k4 != 0) {
					int i5 = k4;
					int j5 = 256 - k4;
					k4 = anIntArray850[k4];
					int k5 = flameRightBackground.canvasRaster[j1];
					flameRightBackground.canvasRaster[j1++] =
							((k4 & 0xff00ff) * i5 + (k5 & 0xff00ff) * j5 & 0xff00ff00)
							+ ((k4 & 0xff00) * i5 + (k5 & 0xff00) * j5
									& 0xff0000) >> 8;
				} else {
					j1++;
				}
			}

			i1 += 128 - k3;
			j1 += 128 - k3 - i3;
		}

		flameRightBackground.drawGraphics(0, super.graphics, 637);
	}

	private void updateOtherPlayerMovement(Buffer stream) {
		int count = stream.readBits(8);

		if (count < playerCount) {
			for (int index = count; index < playerCount; index++) {                        
				removedMobs[removedMobCount++] = playerList[index];
			}
		}
		if (count > playerCount) {
			SignLink.reporterror(myUsername + " Too many players");
			throw new RuntimeException("eek");
		}
		playerCount = 0;
		for (int globalIndex = 0; globalIndex < count; globalIndex++) {                   
			int index = playerList[globalIndex];                  
			Player player = players[index];
			player.index = index;
			int updateRequired = stream.readBits(1);

			if (updateRequired == 0) {
				playerList[playerCount++] = index;
				player.time = tick;
			} else {                        
				int movementType = stream.readBits(2);
				if (movementType == 0) {
					playerList[playerCount++] = index;
					player.time = tick;
					mobsAwaitingUpdate[mobsAwaitingUpdateCount++] = index;
				} else if (movementType == 1) {
					playerList[playerCount++] = index;
					player.time = tick;

					int direction = stream.readBits(3);

					player.moveInDir(false, direction);

					int update = stream.readBits(1);

					if (update == 1) {
						mobsAwaitingUpdate[mobsAwaitingUpdateCount++] = index;
					}
				} else if (movementType == 2) {
					playerList[playerCount++] = index;
					player.time = tick;

					int firstDirection = stream.readBits(3);
					player.moveInDir(true, firstDirection);

					int secondDirection = stream.readBits(3);
					player.moveInDir(true, secondDirection);

					int update = stream.readBits(1);
					if (update == 1) {
						mobsAwaitingUpdate[mobsAwaitingUpdateCount++] = index;
					}
				} else if (movementType == 3) {
					removedMobs[removedMobCount++] = index;
				}
			}
		}
	}

	private ProducingGraphicsBuffer loginScreenAccessories;

	public void loginScreenAccessories() {
		/**
		 * World-selection
		 */
		setupLoginScreen();

		loginScreenAccessories.drawGraphics(400, super.graphics, 0);
		loginScreenAccessories.initDrawingArea();
		cacheSprite[57].drawSprite(6, 63);
		if (!Configuration.worldSwitch) {
			boldText.method382(0xffffff, 55, worldText, 78, true);
			smallText.method382(0xffffff, 55, "Click to switch", 92, true);
			Configuration.server_address = "localhost";
			Configuration.server_port = 43594;
		}

		if (loginScreenState == 4) { 
		
		}
		loginMusicImageProducer.drawGraphics(265, super.graphics, 562);
		loginMusicImageProducer.initDrawingArea();
		if (Configuration.enableMusic) {
			cacheSprite[58].drawSprite(158, 196);
		} else {
			cacheSprite[59].drawSprite(158, 196);
			stopMidi();
		}

	}

	public void drawMusicSprites() {

		int musicState = 0;
		bottomRightImageProducer.initDrawingArea();
		switch (musicState) {
		case 0:
			cacheSprite[58].drawSprite(158, 196);
			break;

		case 1:
			cacheSprite[59].drawSprite(158, 196);
			break;
		}
	}

	private boolean registeringAccount;
	  private boolean usernameInputHover, passwordInputHover, rememberMeHover, loginHover, hideUserHover;
	private boolean rememberMe = true;
	private boolean hideUsername = false; 
	private String worldText = "World 1";
	private int autochatTimer = 0;
	public static int optionSize = 0;
	public static List<String> optionsToSend;

	/*private void drawLoginScreen() {
		setupLoginScreen();
		titleScreen.initDrawingArea();

		//Draw bg
		cacheSprite[339].drawAdvancedSprite(0, 0);

		//newBoldFont.drawBasicString("MouseX: "+super.mouseX+", MouseY: "+super.mouseY, 20, 200);

		if(registeringAccount) {

		} else {

			//Hovers
			usernameInputHover = mouseInRegion(270, 231, 472, 252);
			passwordInputHover = mouseInRegion(270, 278, 472, 299);
			rememberMeHover = mouseInRegion(397, 310, 478, 320);
			loginHover = mouseInRegion(300, 330, 456, 358);

			//Draw login box
			cacheSprite[340].drawAdvancedSprite(0, 0);

			//Draw username text box
			cacheSprite[usernameInputHover ? 342 : 341].drawAdvancedSprite(270, 215);
			//Username
			newBoldFont.drawBasicString(myUsername, 278, 248, 0xD3D3D3);
			if((loginScreenCursorPos == 0) & (tick % 40 < 20)) {
				newBoldFont.drawBasicString(myUsername + "|", 278, 248, 0xD3D3D3);
			}

			//Draw password text box
			cacheSprite[passwordInputHover ? 344 : 343].drawAdvancedSprite(272, 260);
			//password
			String password = StringUtils.passwordAsterisks(myPassword);
			newBoldFont.drawBasicString(password, 278, 295, 0xD3D3D3);
			if((loginScreenCursorPos == 1) & (tick % 40 < 20)) {
				newBoldFont.drawBasicString(password + "|", 278, 295, 0xD3D3D3);
			}

			//Remember me button
			if(rememberMe) {
				cacheSprite[rememberMeHover ? 346 : 348].drawAdvancedSprite(397, 300);
			} else {
				cacheSprite[rememberMeHover ? 345 : 347].drawAdvancedSprite(397, 300);
			}

			//Login button
			cacheSprite[loginHover ? 350 : 349].drawAdvancedSprite(300, 330);

			//Draw errors
			int errorY = 380;
			if(firstLoginMessage.length() > 0) {
				newFancyFont.drawBasicString(firstLoginMessage, 267, errorY, 0xffff00);
				errorY += 22;
			}
			if(secondLoginMessage.length() > 0) {
				newFancyFont.drawBasicString(secondLoginMessage, 267, errorY, 0xffff00);
			}
		}

		titleScreen.drawGraphics(0, super.graphics, 0);
	}*/

	private void drawLoginScreen(boolean flag) {
		setupLoginScreen();
		rememberMeHover = mouseInRegion(265, 270, 280, 290);
		hideUserHover = mouseInRegion(405, 270, 420, 290);
		loginBoxImageProducer.initDrawingArea();
		titleBoxIndexedImage.draw(0, 0);
		// regularText.render(0xffffff, "Mouse X: " + super.mouseX +
		// " , Mouse Y: " + super.mouseY, 30, frameMode == ScreenMode.FIXED ? 5
		// : frameWidth - 5);
		char c = '\u0168';
		char c1 = '\310';
		if (Configuration.enableMusic && !lowMemory) {
			// playSong(SoundConstants.SCAPE_RUNE);
		}
		if (loginScreenState == 0) {
			int i = c1 / 2 + 80;
			smallText.method382(0x75a9a9, c / 2, resourceProvider.loadingMessage, i, true);
			i = c1 / 2 - 20;
			boldText.method382(0xffff00, c / 2, "Welcome to " + Configuration.CLIENT_NAME, i, true);
			i += 30;
			int l = c / 2 - 80;
			int k1 = c1 / 2 + 20;
			titleButtonIndexedImage.draw(l - 73, k1 - 20);
			boldText.method382(0xffffff, l, "New User", k1 + 5, true);
			l = c / 2 + 80;
			titleButtonIndexedImage.draw(l - 73, k1 - 20);
			boldText.method382(0xffffff, l, "Existing User", k1 + 5, true);
		}
		if (loginScreenState == 2) {
			int j = c1 / 2 - 45;
			if (firstLoginMessage.length() > 0) {
				boldText.method382(0xffff00, c / 2, firstLoginMessage, j - 15, true);
				boldText.method382(0xffff00, c / 2, secondLoginMessage, j, true);
				j += 30;
			} else {
				boldText.method382(0xffff00, c / 2, secondLoginMessage, j - 7, true);
				j += 30;
			}
			if (hideUsername) {
				boldText.drawTextWithPotentialShadow(true, c / 2 - 90, 0xffffff,
						"Login: " + StringUtils.passwordAsterisks(myUsername)
								+ ((loginScreenCursorPos == 0) & (tick % 40 < 20) ? "@yel@|" : ""),
						j);
			} else {
				boldText.drawTextWithPotentialShadow(true, c / 2 - 90, 0xffffff,
						"Login: " + myUsername + ((loginScreenCursorPos == 0) & (tick % 40 < 20) ? "@yel@|" : ""), j);
			}
			j += 15;
			boldText.drawTextWithPotentialShadow(true, c / 2 - 88, 0xffffff,
					"Password: " + StringUtils.passwordAsterisks(myPassword)
							+ ((loginScreenCursorPos == 1) & (tick % 40 < 20) ? "@yel@|" : ""),
					j);
			j += 15;
			if (!flag) {
				int i1 = c / 2 - 80;
				int l1 = c1 / 2 + 50;
				titleButtonIndexedImage.draw(i1 - 73, l1 - 20);
				boldText.method382(0xffffff, i1, "Login", l1 + 5, true);
				i1 = c / 2 + 80;
				titleButtonIndexedImage.draw(i1 - 73, l1 - 20);
				boldText.method382(0xffffff, i1, "Cancel", l1 + 5, true);
				smallText.method382(0xfffa00, i1 - 125, "Remember username", l1 - 33, true);
				smallText.method382(0xfffa00, i1, "Hide username", l1 - 33, true);
				smallText.method382(0xfffa00, i1 - 80, "Beta client build version: @whi@" + Configuration.CLIENT_BUILD,
						l1 + 34, true);
				if (rememberMe) {
					cacheSprite[rememberMeHover ? 346 : 348].drawAdvancedSprite(i1 - 194, l1 - 45);
				} else {
					cacheSprite[rememberMeHover ? 345 : 347].drawAdvancedSprite(i1 - 194, l1 - 45);
				}
				if (hideUsername) {
					cacheSprite[hideUserHover ? 346 : 348].drawAdvancedSprite(i1 - 55, l1 - 45);
				} else {
					cacheSprite[hideUserHover ? 345 : 347].drawAdvancedSprite(i1 - 55, l1 - 45);
				}

			}

		}
		if (loginScreenState == 3) {
			loginScreenState = 0;
			// MiscUtils.launchURL(Configuration.REGISTER_ACCOUNT);
		}
		
		loginBoxImageProducer.drawGraphics(171, super.graphics, 202);
		if (welcomeScreenRaised) {
			welcomeScreenRaised = false;
			topLeft1BackgroundTile.drawGraphics(0, super.graphics, 128);
			bottomLeft1BackgroundTile.drawGraphics(371, super.graphics, 202);
			bottomLeft0BackgroundTile.drawGraphics(265, super.graphics, 0);
			bottomRightImageProducer.drawGraphics(265, super.graphics, 562);
			middleLeft1BackgroundTile.drawGraphics(171, super.graphics, 128);
			aRSImageProducer_1115.drawGraphics(171, super.graphics, 562);
		}
		loginScreenAccessories();
		
	}

	private void drawFlames() {
		drawingFlames = true;
		try {
			long l = System.currentTimeMillis();
			int i = 0;
			int j = 20;
			while (aBoolean831) {
				calcFlamesPosition();
				calcFlamesPosition();
				doFlamesDrawing();
				if (++i > 10) {
					long l1 = System.currentTimeMillis();
					int k = (int) (l1 - l) / 10 - j;
					j = 40 - k;
					if (j < 5)
						j = 5;
					i = 0;
					l = l1;
				}
				try {
					Thread.sleep(j);
				} catch (Exception _ex) {
				}
			}
		} catch (Exception _ex) {
		}
		drawingFlames = false;
	}

	public void raiseWelcomeScreen() {
		welcomeScreenRaised = true;
	}

	private void parseRegionPackets(Buffer stream, int packetType) {
		if (packetType == PacketConstants.SEND_ALTER_GROUND_ITEM_COUNT) {
			int offset = stream.readUnsignedByte();
			int xLoc = localX + (offset >> 4 & 7);
			int yLoc = localY + (offset & 7);
			int itemId = stream.readUShort();
			int oldItemCount = stream.readUShort();
			int newItemCount = stream.readUShort();
			if (xLoc >= 0 && yLoc >= 0 && xLoc < 104 && yLoc < 104) {
				Deque groundItemsDeque = groundItems[plane][xLoc][yLoc];
				if (groundItemsDeque != null) {
					for (Item groundItem = (Item) groundItemsDeque
							.reverseGetFirst(); groundItem != null; groundItem =
							(Item) groundItemsDeque.reverseGetNext()) {
						if (groundItem.ID != (itemId & 0x7fff)
								|| groundItem.itemCount != oldItemCount)
							continue;
						groundItem.itemCount = newItemCount;
						break;
					}

					updateGroundItems(xLoc, yLoc);
				}
			}
			return;
		}
		if (packetType == 105) {
			int l = stream.readUnsignedByte();
			int k3 = localX + (l >> 4 & 7);
			int j6 = localY + (l & 7);
			int i9 = stream.readUShort();
			int l11 = stream.readUnsignedByte();
			int i14 = l11 >> 4 & 0xf;
						int i16 = l11 & 7;
						if (localPlayer.pathX[0] >= k3 - i14 && localPlayer.pathX[0] <= k3 + i14
								&& localPlayer.pathY[0] >= j6 - i14
								&& localPlayer.pathY[0] <= j6 + i14 && aBoolean848 && !lowMemory
								&& trackCount < 50) {
							tracks[trackCount] = i9;
							trackLoops[trackCount] = i16;
							soundDelay[trackCount] = Track.delays[i9];
							trackCount++;
						}
		}
		if (packetType == 215) {
			int i1 = stream.readUShortA();
			int l3 = stream.readUByteS();
			int k6 = localX + (l3 >> 4 & 7);
			int j9 = localY + (l3 & 7);
			int i12 = stream.readUShortA();
			int j14 = stream.readUShort();
			if (k6 >= 0 && j9 >= 0 && k6 < 104 && j9 < 104 && i12 != localPlayerIndex) {
				Item class30_sub2_sub4_sub2_2 = new Item();
				class30_sub2_sub4_sub2_2.ID = i1;
				class30_sub2_sub4_sub2_2.itemCount = j14;
				if (groundItems[plane][k6][j9] == null)
					groundItems[plane][k6][j9] = new Deque();
				groundItems[plane][k6][j9].insertHead(class30_sub2_sub4_sub2_2);
				updateGroundItems(k6, j9);
			}
			return;
		}
		if (packetType == PacketConstants.SEND_REMOVE_GROUND_ITEM) {
			int offset = stream.readUByteA();
			int xLoc = localX + (offset >> 4 & 7);
			int yLoc = localY + (offset & 7);
			int itemId = stream.readUShort();
			if (xLoc >= 0 && yLoc >= 0 && xLoc < 104 && yLoc < 104) {
				Deque groundItemsDeque = groundItems[plane][xLoc][yLoc];
				if (groundItemsDeque != null) {
					for (Item item =
							(Item) groundItemsDeque.reverseGetFirst(); item != null; item =
							(Item) groundItemsDeque.reverseGetNext()) {
						if (item.ID != (itemId & 0x7fff))
							continue;
						item.unlink();
						break;
					}

					if (groundItemsDeque.reverseGetFirst() == null)
						groundItems[plane][xLoc][yLoc] = null;
					updateGroundItems(xLoc, yLoc);
				}
			}
			return;
		}
		if (packetType == PacketConstants.ANIMATE_OBJECT) {
			int offset = stream.readUByteS();
			int xLoc = localX + (offset >> 4 & 7);
			int yLoc = localY + (offset & 7);
			int objectTypeFace = stream.readUByteS();
			int objectType = objectTypeFace >> 2;
			int objectFace = objectTypeFace & 3;
			int objectGenre = objectGroups[objectType];
			int animId = stream.readUShortA();
			if (xLoc >= 0 && yLoc >= 0 && xLoc < 103 && yLoc < 103) {
				int heightA = tileHeights[plane][xLoc][yLoc];
				int heightB = tileHeights[plane][xLoc + 1][yLoc];
				int heightC = tileHeights[plane][xLoc + 1][yLoc + 1];
				int heightD = tileHeights[plane][xLoc][yLoc + 1];
				if (objectGenre == 0) {//WallObject
					WallObject wallObjectObject = scene.getWallObject(plane, xLoc, yLoc);
					if (wallObjectObject != null) {
						int objectId = wallObjectObject.uid >> 14 & 0x7fff;
			if (objectType == 2) {
				wallObjectObject.renderable1 = new SceneObject(objectId, 4 + objectFace, 2, heightB, heightC, heightA, heightD, animId, false);
				wallObjectObject.renderable2 = new SceneObject(objectId, objectFace + 1 & 3, 2, heightB, heightC, heightA, heightD, animId, false);
			} else {
				wallObjectObject.renderable1 = new SceneObject(objectId, objectFace, objectType, heightB, heightC, heightA, heightD, animId, false);
			}
					}
				}
				if (objectGenre == 1) { //WallDecoration
					WallDecoration wallDecoration = scene.getWallDecoration(xLoc, yLoc, plane);
					if (wallDecoration != null)
						wallDecoration.renderable = new SceneObject(wallDecoration.uid >> 14 & 0x7fff, 0, 4, heightB, heightC, heightA, heightD, animId, false);
				}
				if (objectGenre == 2) { //TiledObject
					GameObject tiledObject = scene.getGameObject(xLoc, yLoc, plane);
					if (objectType == 11)
						objectType = 10;
					if (tiledObject != null)
						tiledObject.renderable = new SceneObject(tiledObject.uid >> 14 & 0x7fff, objectFace, objectType, heightB, heightC, heightA, heightD, animId, false);
				}
				if (objectGenre == 3) { //GroundDecoration
					GroundDecoration groundDecoration = scene.getGroundDecoration(yLoc, xLoc, plane);
					if (groundDecoration != null)
						groundDecoration.renderable = new SceneObject(groundDecoration.uid >> 14 & 0x7fff, objectFace, 22, heightB, heightC, heightA, heightD, animId, false);
				}
			}
			return;
		}
		if (packetType == PacketConstants.TRANSFORM_PLAYER_TO_OBJECT) {
			int offset = stream.readUByteS();
			int xLoc = localX + (offset >> 4 & 7);
			int yLoc = localY + (offset & 7);
			int playerIndex = stream.readUShort();
			byte byte0GreaterXLoc = stream.readByteS();
			int startDelay = stream.readLEUShort();
			byte byte1GreaterYLoc = stream.readNegByte();
			int stopDelay = stream.readUShort();
			int objectTypeFace = stream.readUByteS();
			int objectType = objectTypeFace >> 2;
			int objectFace = objectTypeFace & 3;
			int objectGenre = objectGroups[objectType];
			byte byte2LesserXLoc = stream.readSignedByte();
			int objectId = stream.readUShort();
			byte byte3LesserYLoc = stream.readNegByte();
			Player player;
			if (playerIndex == localPlayerIndex)
				player = localPlayer;
			else
				player = players[playerIndex];
			if (player != null) {
				ObjectDefinition objectDefinition = ObjectDefinition.lookup(objectId);
				int heightA = tileHeights[plane][xLoc][yLoc];
				int heightB = tileHeights[plane][xLoc + 1][yLoc];
				int heightC = tileHeights[plane][xLoc + 1][yLoc + 1];
				int heightD = tileHeights[plane][xLoc][yLoc + 1];
				Model model = objectDefinition.modelAt(objectType, objectFace, heightA, heightB, heightC, heightD, -1);
				if (model != null) {
					requestSpawnObject(stopDelay + 1, -1, 0, objectGenre, yLoc, 0, plane, xLoc, startDelay + 1);
					player.objectModelStart = startDelay + tick;
					player.objectModelStop = stopDelay + tick;
					player.playerModel = model;
					int playerSizeX = objectDefinition.objectSizeX;
					int playerSizeY = objectDefinition.objectSizeY;
					if (objectFace == 1 || objectFace == 3) {
						playerSizeX = objectDefinition.objectSizeY;
						playerSizeY = objectDefinition.objectSizeX;
					}
					player.objectXPos = xLoc * 128 + playerSizeX * 64;
					player.objectYPos = yLoc * 128 + playerSizeY * 64;
					player.objectCenterHeight = getCenterHeight(plane, player.objectYPos, player.objectXPos);
					if (byte2LesserXLoc > byte0GreaterXLoc) {
						byte tmp = byte2LesserXLoc;
						byte2LesserXLoc = byte0GreaterXLoc;
						byte0GreaterXLoc = tmp;
					}
					if (byte3LesserYLoc > byte1GreaterYLoc) {
						byte tmp = byte3LesserYLoc;
						byte3LesserYLoc = byte1GreaterYLoc;
						byte1GreaterYLoc = tmp;
					}
					player.objectAnInt1719LesserXLoc = xLoc + byte2LesserXLoc;
					player.objectAnInt1721GreaterXLoc = xLoc + byte0GreaterXLoc;
					player.objectAnInt1720LesserYLoc = yLoc + byte3LesserYLoc;
					player.objectAnInt1722GreaterYLoc = yLoc + byte1GreaterYLoc;
				}
			}
		}
		if (packetType == PacketConstants.SEND_OBJECT) {
			int offset = stream.readUByteA();
			int x = localX + (offset >> 4 & 7);
			int y = localY + (offset & 7);
			int id = stream.readLEUShort();                  
			int objectTypeFace = stream.readUByteS();
			int type = objectTypeFace >> 2;
			int orientation = objectTypeFace & 3;                  
			int group = objectGroups[type];                  
			if (x >= 0 && y >= 0 && x < 104 && y < 104) {              	  
				requestSpawnObject(-1, id, orientation, group, y, type, plane, x, 0);
			}
			return;
		}
		if (packetType == PacketConstants.SEND_GFX) {
			int offset = stream.readUnsignedByte();
			int xLoc = localX + (offset >> 4 & 7);
			int yLoc = localY + (offset & 7);
			int gfxId = stream.readUShort();
			int gfxHeight = stream.readUnsignedByte();
			int gfxDelay = stream.readUShort();
			if (xLoc >= 0 && yLoc >= 0 && xLoc < 104 && yLoc < 104) {
				xLoc = xLoc * 128 + 64;
				yLoc = yLoc * 128 + 64;
				AnimableObject loneGfx = new AnimableObject(plane, tick,
						gfxDelay, gfxId, getCenterHeight(plane, yLoc, xLoc) - gfxHeight, yLoc, xLoc);
				incompleteAnimables.insertHead(loneGfx);
			}
			return;
		}
		if (packetType == PacketConstants.SEND_GROUND_ITEM) {
			int itemId = stream.readLEUShortA();
			int itemCount = stream.readUShort();
			int offset = stream.readUnsignedByte();
			int xLoc = localX + (offset >> 4 & 7);
			int yLoc = localY + (offset & 7);
			if (xLoc >= 0 && yLoc >= 0 && xLoc < 104 && yLoc < 104) {
				Item groundItem = new Item();
				groundItem.ID = itemId;
				groundItem.itemCount = itemCount;
				if (groundItems[plane][xLoc][yLoc] == null)
					groundItems[plane][xLoc][yLoc] = new Deque();
				groundItems[plane][xLoc][yLoc].insertHead(groundItem);
				updateGroundItems(xLoc, yLoc);
			}
			return;
		}
		if (packetType == PacketConstants.SEND_REMOVE_OBJECT) {
			int objectTypeFace = stream.readNegUByte();
			int type = objectTypeFace >> 2;
			int orientation = objectTypeFace & 3;
			int group = objectGroups[type];                  
			int offset = stream.readUnsignedByte();
			int x = localX + (offset >> 4 & 7);
			int y = localY + (offset & 7);
			if (x >= 0 && y >= 0 && x < 104 && y < 104) {
				requestSpawnObject(-1, -1, orientation, group, y, type, plane, x, 0);
			}
			return;
		}
		if (packetType == PacketConstants.SEND_PROJECTILE) {
			int offset = stream.readUnsignedByte();
			int x1 = localX + (offset >> 4 & 7);
			int y1 = localY + (offset & 7);
			int x2 = x1 + stream.readSignedByte();
			int y2 = y1 + stream.readSignedByte();
			int target = stream.readShort();
			int gfxMoving = stream.readUShort();
			int startHeight = stream.readUnsignedByte() * 4;
			int endHeight = stream.readUnsignedByte() * 4;
			int startDelay = stream.readUShort();
			int speed = stream.readUShort();
			int initialSlope = stream.readUnsignedByte();
			int frontOffset = stream.readUnsignedByte();
			if (x1 >= 0 && y1 >= 0 && x1 < 104 && y1 < 104 && x2 >= 0 && y2 >= 0
					&& x2 < 104 && y2 < 104 && gfxMoving != 65535) {
				x1 = x1 * 128 + 64;
				y1 = y1 * 128 + 64;
				x2 = x2 * 128 + 64;
				y2 = y2 * 128 + 64;
				Projectile projectile = new Projectile(initialSlope, endHeight, startDelay + tick, speed + tick, frontOffset, plane, getCenterHeight(plane, y1, x1) - startHeight, y1, x1, target, gfxMoving);
				projectile.calculateIncrements(startDelay + tick, y2, getCenterHeight(plane, y2, x2) - endHeight, x2);
				projectiles.insertHead(projectile);
			}
		}
	}

	private void method139(Buffer stream) {
		stream.initBitAccess();
		int k = stream.readBits(8);
		if (k < npcCount) {
			for (int l = k; l < npcCount; l++)
				removedMobs[removedMobCount++] = npcIndices[l];

		}
		if (k > npcCount) {
			SignLink.reporterror(myUsername + " Too many npcs");
			throw new RuntimeException("eek");
		}
		npcCount = 0;
		for (int i1 = 0; i1 < k; i1++) {
			int j1 = npcIndices[i1];
			Npc npc = npcs[j1];
			npc.index = j1;
			int k1 = stream.readBits(1);
			if (k1 == 0) {
				npcIndices[npcCount++] = j1;
				npc.time = tick;
			} else {
				int l1 = stream.readBits(2);
				if (l1 == 0) {
					npcIndices[npcCount++] = j1;
					npc.time = tick;
					mobsAwaitingUpdate[mobsAwaitingUpdateCount++] = j1;
				} else if (l1 == 1) {
					npcIndices[npcCount++] = j1;
					npc.time = tick;
					int i2 = stream.readBits(3);
					npc.moveInDir(false, i2);
					int k2 = stream.readBits(1);
					if (k2 == 1)
						mobsAwaitingUpdate[mobsAwaitingUpdateCount++] = j1;
				} else if (l1 == 2) {
					npcIndices[npcCount++] = j1;
					npc.time = tick;
					int j2 = stream.readBits(3);
					npc.moveInDir(true, j2);
					int l2 = stream.readBits(3);
					npc.moveInDir(true, l2);
					int i3 = stream.readBits(1);
					if (i3 == 1)
						mobsAwaitingUpdate[mobsAwaitingUpdateCount++] = j1;
				} else if (l1 == 3)
					removedMobs[removedMobCount++] = j1;
			}
		}

	}

	private void processLoginScreenInput() {
		if (world1Hover && super.clickMode3 == 1) {
			Configuration.server_address = "localhost";
			worldText = "World 1";
			//worldSwitchText = "PvP Economy";
			InWorldSelect = false;
			welcomeScreenRaised = true;
			aBoolean831 = false;
			drawLoginScreen(false);

		}
		if (dmmworldhover && super.clickMode3 == 1) {
			Configuration.server_address = "localhost";
			worldText = "World 2";
			//worldSwitchText = "Deadman Mode";
			InWorldSelect = false;
			welcomeScreenRaised = true;
			aBoolean831 = false;
			drawLoginScreen(false);
		}
		if (super.clickMode3 == 1 && rememberMeHover) {
			rememberMe = !rememberMe;
			savePlayerData();
		}
		if (super.clickMode3 == 1 && hideUserHover) {
			hideUsername = !hideUsername;
			savePlayerData();
		}
		if(InWorldSelect) {
			if (super.clickMode3 == 1 && super.saveClickX >= 700 && super.saveClickX <= 760 && super.saveClickY >= 3
					&& super.saveClickY <= 20) {
				InWorldSelect = false;
				welcomeScreenRaised = true;
				aBoolean831 = false;

				
			}
		}

		if (loginScreenState == 0) {
			if (super.clickMode3 == 1 && super.saveClickX >= 722 && super.saveClickX <= 751 && super.saveClickY >= 463
					&& super.saveClickY <= 493) {
				Configuration.enableMusic = !Configuration.enableMusic;
			}

			if (super.clickMode3 == 1 && super.saveClickX >= 7 && super.saveClickX <= 104 && super.saveClickY >= 464
					&& super.saveClickY <= 493) {
				InWorldSelect = true;
				drawWorldSelect();
			}

			int i = super.myWidth / 2 - 80;
			int l = super.myHeight / 2 + 20;
			l += 20;
			if (usernameInputHover) {
				// loginScreenState = 3;
				loginScreenCursorPos = 0;
			}
			i = super.myWidth / 2 + 80;
			if (super.clickMode3 == 1 && super.saveClickX >= i - 75 && super.saveClickX <= i + 75
					&& super.saveClickY >= l - 20 && super.saveClickY <= l + 20) {
				firstLoginMessage = "";
				secondLoginMessage = "Enter your username/email & password.";
				loginScreenState = 2;
				loginScreenCursorPos = 0;
			}
		} else if (loginScreenState == 2) {

			if (super.clickMode3 == 1 && super.saveClickX >= 722 && super.saveClickX <= 751 && super.saveClickY >= 463
					&& super.saveClickY <= 493) {
				Configuration.enableMusic = !Configuration.enableMusic;
			}
			if (super.clickMode3 == 1 && super.saveClickX >= 7 && super.saveClickX <= 104 && super.saveClickY >= 464
					&& super.saveClickY <= 493) {
				//Configuration.worldSwitch = !Configuration.worldSwitch;
				InWorldSelect = true;
				drawWorldSelect();
				
			}
			int j = super.myHeight / 2 - 40;
			j += 30;
			j += 25;
			if (super.clickMode3 == 1 && super.saveClickY >= j - 15 && super.saveClickY < j)
				loginScreenCursorPos = 0;
			j += 15;
			if (super.clickMode3 == 1 && super.saveClickY >= j - 15 && super.saveClickY < j)
				loginScreenCursorPos = 1;
			j += 15;
			int i1 = super.myWidth / 2 - 80;
			int k1 = super.myHeight / 2 + 50;
			k1 += 20;
			if (super.clickMode3 == 1 && super.saveClickX >= i1 - 75 && super.saveClickX <= i1 + 75
					&& super.saveClickY >= k1 - 20 && super.saveClickY <= k1 + 20) {
				loginFailures = 0;
				login(myUsername, myPassword, false);
				if (loggedIn)
					return;
			}
			i1 = super.myWidth / 2 + 80;
			if (super.clickMode3 == 1 && super.saveClickX >= i1 - 75 && super.saveClickX <= i1 + 75
					&& super.saveClickY >= k1 - 20 && super.saveClickY <= k1 + 20) {
				loginScreenState = 0;
			}
			do {
				int l1 = readChar(-796);
				if (l1 == -1)
					break;
				boolean flag1 = false;
				for (int i2 = 0; i2 < validUserPassChars.length(); i2++) {
					if (l1 != validUserPassChars.charAt(i2))
						continue;
					flag1 = true;
					break;
				}

				if (loginScreenCursorPos == 0) {
					if (l1 == 8 && myUsername.length() > 0)
						myUsername = myUsername.substring(0, myUsername.length() - 1);
					if (l1 == 9 || l1 == 10 || l1 == 13)
						loginScreenCursorPos = 1;
					if (flag1)
						myUsername += (char) l1;
					if (myUsername.length() > 12)
						myUsername = myUsername.substring(0, 12);
					if (myUsername.length() > 0) {
						myUsername = StringUtils.formatText(StringUtils.capitalize(myUsername));
					}
				} else if (loginScreenCursorPos == 1) {
					if (l1 == 8 && myPassword.length() > 0)
						myPassword = myPassword.substring(0, myPassword.length() - 1);
					if (l1 == 9) {
						loginScreenCursorPos = 0;
					} else if (l1 == 10 || l1 == 13) {
						login(myUsername, myPassword, false);
						return;
					}
					if (flag1)
						myPassword += (char) l1;
					if (myPassword.length() > 15)
						myPassword = myPassword.substring(0, 15);
				}
			} while (true);
			return;
		} else if (loginScreenState == 3) {
			int k = super.myWidth / 2;
			int j1 = super.myHeight / 2 + 50;
			j1 += 20;
			if (super.clickMode3 == 1 && super.saveClickX >= k - 75 && super.saveClickX <= k + 75
					&& super.saveClickY >= j1 - 20 && super.saveClickY <= j1 + 20)
				loginScreenState = 0;
		}
	}

	private void drawWorldSelect() {
		resetAllImageProducers();
		WorldSelector = new ProducingGraphicsBuffer(frameWidth, frameHeight);
		WorldSelector.initDrawingArea();
		world1Hover = mouseInRegion(320, 218, 407, 239);
		dmmworldhover = mouseInRegion(320, 240, 407, 259);
		cacheSprite[532].drawSprite(0, 0);
		cacheSprite[524].drawSprite(0, 0);
		if(!world1Hover) {
			addWorld(525, 531, 0, "1", 320, 220);
		} else {
			addWorld(533, 531, 0, "1", 320, 220);
		}
		if(!dmmworldhover) {
			addWorld(526, 531, 0, "2", 320, 240);
		} else {
			addWorld(534, 531, 0, "2", 320, 240);
		}
		if(world1Hover) {
			drawHoverBox(super.mouseX - 38, super.mouseY + 20, "World 1 - Pking");
		}
		if(dmmworldhover) {
			drawHoverBox(super.mouseX - 38, super.mouseY + 20, "Deadman Mode\n Coming soon!");
		}
		newSmallFont.drawCenteredString("Close", 733, 17, 0xffffff, -1);
		WorldSelector.drawGraphics(0, super.graphics, 0);
		setupGameplayScreen();

		
	}
	
	/*
	 * WorldType = color of world (f2p/mem/pvp/deadman) check sprites
	 * WorldLoc = location of world (aus, uk, usa) check sprites
	 * playerCount = playercount of world, get it with a packet
	 * world = world number (1, 2, 3...)
	 */
	public void addWorld(int worldType, int WorldLoc, int playerCount, String world, int x, int y) {
		cacheSprite[worldType].drawSprite(x, y);
		cacheSprite[WorldLoc].drawSprite(x + 25, y + 1);
		if(worldType == 528) {
		newBoldFont.drawBasicString(world, x + 12, y + 15, 0xffffff);
		} else {
			newBoldFont.drawBasicString(world, x + 12, y + 15, 0x000000);
		}
		newSmallFont.drawCenteredString(Integer.toString(playerCount), x + 60, y + 15, 0xffffff, -1);
	}

	private void removeObject(int y, int z, int k, int l, int x, int group, int previousId) {
		if (x >= 1 && y >= 1 && x <= 102 && y <= 102) {
			if (lowMemory && z != plane)
				return;
			int key = 0;
			if (group == 0)
				key = scene.getWallObjectUid(z, x, y);
			if (group == 1)
				key = scene.getWallDecorationUid(z, x, y);
			if (group == 2)
				key = scene.getGameObjectUid(z, x, y);
			if (group == 3)
				key = scene.getGroundDecorationUid(z, x, y);
			if (key != 0) {
				int config = scene.getMask(z, x, y, key);
				int id = key >> 14 & 0x7fff;
		int objectType = config & 0x1f;
		int orientation = config >> 6;

		if (group == 0) {
			scene.removeWallObject(x, z, y);
			ObjectDefinition objectDef = ObjectDefinition.lookup(id);
			if (objectDef.solid)
				collisionMaps[z].removeObject(orientation, objectType,
						objectDef.impenetrable, x, y);
		}
		if (group == 1)
			scene.removeWallDecoration(y, z, x);
		if (group == 2) {
			scene.removeTiledObject(z, x, y);
			ObjectDefinition objectDef = ObjectDefinition.lookup(id);
			if (x + objectDef.objectSizeX > 103 || y + objectDef.objectSizeX > 103
					|| x + objectDef.objectSizeY > 103
					|| y + objectDef.objectSizeY > 103)
				return;
			if (objectDef.solid)
				collisionMaps[z].removeObject(orientation, objectDef.objectSizeX, x,
						y, objectDef.objectSizeY, objectDef.impenetrable);
		}
		if (group == 3) {
			scene.removeGroundDecoration(z, y, x);
			ObjectDefinition objectDef = ObjectDefinition.lookup(id);
			if (objectDef.solid && objectDef.isInteractive)
				collisionMaps[z].removeFloorDecoration(y, x);
		}
			}
			if (previousId >= 0) {
				int plane = z;
				if (plane < 3 && (tileFlags[1][x][y] & 2) == 2)
					plane++;
				MapRegion.placeObject(scene, k, y, l, plane, collisionMaps[z], tileHeights,
						x, previousId, z);
			}
		}
	}

	private void updatePlayers(int packetSize, Buffer stream) {             
		removedMobCount = 0;            
		mobsAwaitingUpdateCount = 0;
		updateLocalPlayerMovement(stream);            
		updateOtherPlayerMovement(stream);            
		updatePlayerList(stream, packetSize);
		parsePlayerSynchronizationMask(stream);
		for (int count = 0; count < removedMobCount; count++) {                  
			int index = removedMobs[count];  

			if (players[index].time != tick) {
				players[index] = null;
			}
		}

		if (stream.currentPosition != packetSize) {
			SignLink.reporterror("Error packet size mismatch in getplayer pos:"
					+ stream.currentPosition + " psize:" + packetSize);
			throw new RuntimeException("eek");
		}
		for (int count = 0; count < playerCount; count++) {                  
			if (players[playerList[count]] == null) {
				SignLink.reporterror(myUsername + " null entry in pl list - pos:" + count
						+ " size:" + playerCount);
				throw new RuntimeException("eek");
			}
		}

	}

	private void setCameraPos(int j, int k, int l, int i1, int j1, int k1) {
		int l1 = 2048 - k & 0x7ff;
		int i2 = 2048 - j1 & 0x7ff;
		int j2 = 0;
		int k2 = 0;
		int l2 = j;
		if (l1 != 0) {
			int i3 = Model.SINE[l1];
			int k3 = Model.COSINE[l1];
			int i4 = k2 * k3 - l2 * i3 >> 16;
		l2 = k2 * i3 + l2 * k3 >> 16;
				k2 = i4;
		}
		if (i2 != 0) {
			int j3 = Model.SINE[i2];
			int l3 = Model.COSINE[i2];
			int j4 = l2 * j3 + j2 * l3 >> 16;
			l2 = l2 * l3 - j2 * j3 >> 16;
			j2 = j4;
		}
		xCameraPos = l - j2;
		zCameraPos = i1 - k2;
		yCameraPos = k1 - l2;
		yCameraCurve = k;
		xCameraCurve = j1;
	}

	/**
	 * This method updates default messages upon login to the desired text of the interface text.
	 */
	public void updateStrings(String message, int index) {
		switch (index) {
		case 1675:
			sendString(message, 17508);
			break;// Stab
		case 1676:
			sendString(message, 17509);
			break;// Slash
		case 1677:
			sendString(message, 17510);
			break;// Crush
		case 1678:
			sendString(message, 17511);
			break;// Magic
		case 1679:
			sendString(message, 17512);
			break;// Range
		case 1680:
			sendString(message, 17513);
			break;// Stab
		case 1681:
			sendString(message, 17514);
			break;// Slash
		case 1682:
			sendString(message, 17515);
			break;// Crush
		case 1683:
			sendString(message, 17516);
			break;// Magic
		case 1684:
			sendString(message, 17517);
			break;// Range
		case 1686:
			sendString(message, 17518);
			break;// Strength
		case 1687:
			sendString(message, 17519);
			break;// Prayer
		}
	}

	/**
	 * Sends a string
	 */
	public static void sendString(String text, int index) {
		if(Widget.interfaceCache[index] == null) {
			return;
		}
		Widget.interfaceCache[index].defaultText = text;
		if (Widget.interfaceCache[index].parent == tabInterfaceIDs[tabId]) {
		}
	}

	public void sendButtonClick(int button, int toggle, int type) {
		Widget widget = Widget.interfaceCache[button];
		switch (type) {
		case 135:
			boolean flag8 = true;

			if (widget.contentType > 0) {
				flag8 = promptUserForInput(widget);
			}

			if (flag8) {
				sendPacket(new ClickButton(button));
			}
			break;

			// case reset setting widget
		case 646:
			sendPacket(new ClickButton(button));

			if (widget.valueIndexArray != null && widget.valueIndexArray[0][0] == 5) {
				if (settings[toggle] != widget.requiredValues[0]) {
					settings[toggle] = widget.requiredValues[0];
					updateVarp(toggle);
				}
			}
			break;

		case 169:
			sendPacket(new ClickButton(button));
			if (widget.valueIndexArray != null && widget.valueIndexArray[0][0] == 5) {
				settings[toggle] = 1 - settings[toggle];
				updateVarp(toggle);
			}
			break;

		default:
			System.out.println("button: " + button + " - toggle: " + toggle
					+ " - type: " + type);
			break;
		}
	}

	/**
	 * Sets button configurations on interfaces.
	 */
	public void sendConfiguration(int id, int state) {
		anIntArray1045[id] = state;
		if (settings[id] != state) {
			settings[id] = state;
			updateVarp(id);
			if (dialogueId != -1)
				updateChatbox = true;
		}
	}

	/**
	 * Clears the screen of all open interfaces.
	 */
	public void clearScreen() {
		if (overlayInterfaceId != -1) {
			overlayInterfaceId = -1;
			tabAreaAltered = true;
		}
		if (backDialogueId != -1) {
			backDialogueId = -1;
			updateChatbox = true;
		}
		if (inputDialogState != 0) {
			inputDialogState = 0;
			updateChatbox = true;
		}
		openInterfaceId = -1;
		continuedDialogue = false;
	}

	/**
	 * Displays an interface over the sidebar area.
	 */
	public void inventoryOverlay(int interfaceId, int sideInterfaceId) {
		if (backDialogueId != -1) {
			backDialogueId = -1;
			updateChatbox = true;
		}
		if (inputDialogState != 0) {
			inputDialogState = 0;
			updateChatbox = true;
		}
		openInterfaceId = interfaceId;
		overlayInterfaceId = sideInterfaceId;
		tabAreaAltered = true;
		continuedDialogue = false;
	}

	public void sendPacket(OutgoingPacket packet) {

		//Make sure we're logged in and that we can encrypt our packet.
		if(!loggedIn || outgoing.getCipher() == null) {
			ping_packet_counter = 0;
			return;
		}

		outgoing.resetPosition();

		packet.buildPacket(outgoing);

		try {

			if (socketStream != null) {
				socketStream.queueBytes(outgoing.bufferLength(), outgoing.getBuffer());
				outgoing.resetPosition();
			}

		} catch (IOException _ex) {
			dropClient();
			System.out.println(_ex);
		} catch (Exception exception) {
			resetLogout();
			System.out.println(exception);
		}

		ping_packet_counter = 0;
	}

	private boolean readPacket() {  

		if (socketStream == null) {
			return false;
		}

		try {

			int available = socketStream.available();
			if (available < 2) {
				return false;
			}

			//First we read opcode...
			if(opcode == -1) {

				socketStream.flushInputStream(incoming.payload, 1);

				opcode = incoming.payload[0] & 0xff;

				if (encryption != null) {
					opcode = opcode - encryption.getNextKey() & 0xff;
				}

				//Now attempt to read packet size..
				socketStream.flushInputStream(incoming.payload, 2);
				packetSize = ((incoming.payload[0] & 0xff) << 8)
						+ (incoming.payload[1] & 0xff);

			}

			if(!(opcode >= 0 && opcode < 256)) {
				opcode = -1;
				return false;
			}

			incoming.currentPosition = 0;
			socketStream.flushInputStream(incoming.payload, packetSize);

			timeoutCounter = 0;
			thirdLastOpcode = secondLastOpcode;
			secondLastOpcode = lastOpcode;
			lastOpcode = opcode;

			if(opcode == PacketConstants.SET_TOTAL_EXP) {
				totalExp = incoming.readLong();
				opcode = -1;
				return true;
			}
			
			if(opcode == 158) {
				int id = incoming.readInt();
				int length = incoming.readShort();
				
				Widget w = Widget.interfaceCache[id];
				w.width = length;
				if(length == w.totalWidth) {
					w.defaultHoverColor = 0x005f00;
					w.secondaryColor = 0x005f00;
					w.secondaryHoverColor = 0x005f00;
					w.textColor = 0x005f00;
				} else {
					w.defaultHoverColor = 0xd88020;
					w.secondaryColor = 0xd88020;
					w.secondaryHoverColor = 0xd88020;
					w.textColor =0xd88020;
				}
				opcode = -1;
				return true;
			}
			
			 if (opcode == 157) { 
				 AnnouncementBool = incoming.readUnsignedByte() == 1; 
				 staffMessage = incoming.readString();
				 opcode = -1;
				 return true;
			 }
			
			if(opcode == PacketConstants.SET_SCROLLBAR_HEIGHT) {
				int interface_ = incoming.readInt();
				int scrollMax = incoming.readShort();
				Widget w = Widget.interfaceCache[interface_];
				if(w != null) {
					w.scrollMax = scrollMax;
				}
				opcode = -1;
				return true;
			}
			
			if(opcode == 173) {
				optionsToSend.clear();
				optionSize = 0;
				optionSize = incoming.readShort();
				for(int i = 0; i < optionSize; i++) {
					optionsToSend.add(incoming.readString());
				}
				opcode = -1;
				return true;
			}

			if(opcode == PacketConstants.INTERFACE_SCROLL_RESET) {
				int interface_ = incoming.readInt();
				Widget w = Widget.interfaceCache[interface_];
				if(w != null) {
					w.scrollPosition = 0;
				}
				opcode = -1;
				return true;
			}

			if(opcode == PacketConstants.UPDATE_PLAYER_RIGHTS) {
				myPrivilege = incoming.readUnsignedByte();
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.PLAYER_UPDATING) {
				updatePlayers(packetSize, incoming);
				validLocalMap = false;
				opcode = -1;
				return true;
			}

			if (opcode == 183) {
				try {
					specialEnabled = incoming.readNegUByte();
				} catch (Exception e) {
					e.printStackTrace();
				}
				opcode = -1;
				return true;
			}

			if(opcode == PacketConstants.SEND_CONSOLE_COMMAND) {
				String msg = incoming.readString();
				printConsoleMessage(msg, 0);
				opcode = -1;
				return true;
			}

			if(opcode == PacketConstants.SHOW_CLANCHAT_OPTIONS) {
				showClanOptions = incoming.readUnsignedByte() == 1;
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.OPEN_WELCOME_SCREEN) {
				daysSinceRecovChange = incoming.readNegUByte();
				unreadMessages = incoming.readUShortA();
				membersInt = incoming.readUnsignedByte();
				anInt1193 = incoming.readIMEInt();
				daysSinceLastLogin = incoming.readUShort();
				if (anInt1193 != 0 && openInterfaceId == -1) {
					SignLink.dnslookup(StringUtils.decodeIp(anInt1193));
					clearTopInterfaces();
					char character = '\u028A';
					if (daysSinceRecovChange != 201 || membersInt == 1)
						character = '\u028F';
					reportAbuseInput = "";
					canMute = false;
					for (int interfaceId =
							0; interfaceId < Widget.interfaceCache.length; interfaceId++) {
						if (Widget.interfaceCache[interfaceId] == null
								|| Widget.interfaceCache[interfaceId].contentType != character)
							continue;
						openInterfaceId = Widget.interfaceCache[interfaceId].parent;

					}
				}
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.DELETE_GROUND_ITEM) {
				localX = incoming.readNegUByte();
				localY = incoming.readUByteS();
				for (int x = localX; x < localX + 8; x++) {
					for (int y = localY; y < localY + 8; y++)
						if (groundItems[plane][x][y] != null) {
							groundItems[plane][x][y] = null;
							updateGroundItems(x, y);
						}
				}
				for (SpawnedObject object = (SpawnedObject) spawns
						.reverseGetFirst(); object != null; object =
						(SpawnedObject) spawns.reverseGetNext())
					if (object.x >= localX && object.x < localX + 8 && object.y >= localY
					&& object.y < localY + 8 && object.plane == plane)
						object.getLongetivity = 0;
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SHOW_PLAYER_HEAD_ON_INTERFACE) {
				int playerHeadModelId = incoming.readLEUShortA();
				Widget.interfaceCache[playerHeadModelId].defaultMediaType = 3;
				if (localPlayer.npcDefinition == null)
					Widget.interfaceCache[playerHeadModelId].defaultMedia =
					(localPlayer.appearanceColors[0] << 25)
					+ (localPlayer.appearanceColors[4] << 20)
					+ (localPlayer.equipment[0] << 15)
					+ (localPlayer.equipment[8] << 10)
					+ (localPlayer.equipment[11] << 5)
					+ localPlayer.equipment[1];
				else
					Widget.interfaceCache[playerHeadModelId].defaultMedia =
					(int) (0x12345678L + localPlayer.npcDefinition.interfaceType);
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.CLAN_CHAT) {
				try {
					name = incoming.readString();
					defaultText = incoming.readString();
					clanname = incoming.readString();
					rights = incoming.readUShort();
					sendMessage(defaultText, 16, name);
				} catch (Exception e) {
					e.printStackTrace();
				}
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.RESET_CAMERA) {
				oriented = false;
				for (int l = 0; l < 5; l++)
					quakeDirectionActive[l] = false;
				xpCounter = 0;
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.CLEAN_ITEMS_OF_INTERFACE) {
				int id = incoming.readUShort();
				Widget widget = Widget.interfaceCache[id];
				for (int slot = 0; slot < widget.inventoryItemId.length; slot++) {
					widget.inventoryItemId[slot] = -1;
					widget.inventoryItemId[slot] = 0;
				}
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SPIN_CAMERA) {
				oriented = true;
				x = incoming.readUnsignedByte();
				y = incoming.readUnsignedByte();
				height = incoming.readUShort();
				speed = incoming.readUnsignedByte();
				angle = incoming.readUnsignedByte();
				if (angle >= 100) {
					xCameraPos = x * 128 + 64;
					yCameraPos = y * 128 + 64;
					zCameraPos = getCenterHeight(plane, yCameraPos, xCameraPos) - height;
				}
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SEND_SKILL) {
				int skill = incoming.readUnsignedByte();
				int level = incoming.readInt();
				int maxLevel = incoming.readInt();
				int experience = incoming.readInt();

				if (skill < currentExp.length) {


					if(currentExp[skill] > 0) {
						int gainedexp = experience - currentExp[skill];
						if(gainedexp > 0) {

							//Update current skill
							//Don't update it if it's hp.
							if(skill != 3) {
								currentSkill = skill;
							}

							//Update total exp
							totalExp += gainedexp;

							//if(skillOrbs) {
							SkillOrbs.orbs[skill].receivedExperience();
							//}
							if(expDrops) {
								if(gainedexp < Integer.MAX_VALUE) {
									addToXPCounter(skill, gainedexp);
								}
							}
						}
					}
					currentExp[skill] = experience;
					currentLevels[skill] = level;
					maximumLevels[skill] = maxLevel;

					if(skill == 3 && localPlayer != null) {
						localPlayer.currentHealth = level;
						localPlayer.maxHealth = maxLevel;
					}
					/*	for (int index = 0; index < 98; index++)
						if (experience >= SKILL_EXPERIENCE[index])
							maximumLevels[skill] = index + 2;*/
				}
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SEND_SIDE_TAB) {
				int id = incoming.readUShort();
				int tab = incoming.readUByteA();
				if (id == 65535)
					id = -1;
				tabInterfaceIDs[tab] = id;
				tabAreaAltered = true;
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.PLAY_SONG) {
				int id = incoming.readLEUShort();
				if (id == 65535)
					id = -1;
				if (id != currentSong && Configuration.enableMusic && !lowMemory
						&& prevSong == 0) {
					nextSong = id;
					fadeMusic = true;
					resourceProvider.provide(2, nextSong);
				}
				currentSong = id;
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.NEXT_OR_PREVIOUS_SONG) {
				int id = incoming.readLEUShortA();
				int delay = incoming.readUShortA();
				if (Configuration.enableMusic && !lowMemory) {
					nextSong = id;
					fadeMusic = false;
					resourceProvider.provide(2, nextSong);
					prevSong = delay;
				}
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.LOGOUT) {
				resetLogout();
				opcode = -1;
				return false;
			}

			if (opcode == PacketConstants.MOVE_COMPONENT) {
				int horizontalOffset = incoming.readShort();
				int verticalOffset = incoming.readLEShort();
				int id = incoming.readLEUShort();
				Widget widget = Widget.interfaceCache[id];
				widget.horizontalOffset = horizontalOffset;
				widget.verticalOffset = verticalOffset;
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SEND_MAP_REGION
					|| opcode == PacketConstants.SEND_REGION_MAP_REGION) {
				int regionX = this.regionX;                        
				int regionY = this.regionY;
				if (opcode == 73) {
					regionX = incoming.readUShortA();
					regionY = incoming.readUShort();
					constructedViewport = false;
				} else if (opcode == 241) {
					regionY = incoming.readUShortA();
					incoming.initBitAccess();
					for (int z = 0; z < 4; z++) {                                    
						for (int x = 0; x < 13; x++) {                                          
							for (int y = 0; y < 13; y++) {  

								int visible = incoming.readBits(1);

								if (visible == 1) {                                                      
									localRegions[z][x][y] = incoming.readBits(26);
								} else {
									localRegions[z][x][y] = -1;
								}
							}
						}
					}
					incoming.disableBitAccess();
					regionX = incoming.readUShort();
					constructedViewport = true;
				}
				if (this.regionX == regionX && this.regionY == regionY && loadingStage == 2) {
					opcode = -1;
					return true;
				}
				this.regionX = regionX;
				this.regionY = regionY;
				regionBaseX = (this.regionX - 6) * 8;
				regionBaseY = (this.regionY - 6) * 8;
				inPlayerOwnedHouse = (this.regionX / 8 == 48 || this.regionX / 8 == 49)
						&& this.regionY / 8 == 48;
				if (this.regionX / 8 == 48 && this.regionY / 8 == 148)
					inPlayerOwnedHouse = true;
				loadingStage = 1;                       
				loadingStartTime = System.currentTimeMillis();
				gameScreenImageProducer.initDrawingArea();
				drawLoadingMessages(1, "Loading - please wait.", null);
				gameScreenImageProducer.drawGraphics(frameMode == ScreenMode.FIXED ? 4 : 0,
						super.graphics, frameMode == ScreenMode.FIXED ? 4 : 0);
				if (opcode == 73) {
					int regionCount = 0;                                
					for (int x = (this.regionX - 6) / 8; x <= (this.regionX + 6)
							/ 8; x++) {
						for (int y = (this.regionY - 6) / 8; y <= (this.regionY + 6)
								/ 8; y++)
							regionCount++;
					}
					localRegionMapData = new byte[regionCount][];                              
					localRegionLandscapeData = new byte[regionCount][];                              
					localRegionIds = new int[regionCount];                              
					localRegionMapIds = new int[regionCount];                              
					localRegionLandscapeIds = new int[regionCount];                              
					regionCount = 0;                              

					for (int x = (this.regionX - 6) / 8; x <= (this.regionX + 6)
							/ 8; x++) {
						for (int y = (this.regionY - 6) / 8; y <= (this.regionY + 6)
								/ 8; y++) {
							localRegionIds[regionCount] = (x << 8) + y;
							if (inPlayerOwnedHouse && (y == 49 || y == 149
									|| y == 147 || x == 50
									|| x == 49 && y == 47)) {
								localRegionMapIds[regionCount] = -1;
								localRegionLandscapeIds[regionCount] = -1;
								regionCount++;                                                
							} else {                                                
								int map = localRegionMapIds[regionCount] =
										resourceProvider.resolve(0, y, x);
								if (map != -1) {
									resourceProvider.provide(3, map);
								}

								int landscape = localRegionLandscapeIds[regionCount] =
										resourceProvider.resolve(1, y, x);
								if (landscape != -1) {
									resourceProvider.provide(3, landscape);
								}
								regionCount++;
							}
						}
					}
				}
				if (opcode == 241) {
					int regionCount = 0;

					int regionIds[] = new int[676];

					for (int z = 0; z < 4; z++) {                                    
						for (int x = 0; x < 13; x++) {                                          
							for (int y = 0; y < 13; y++) { 

								int data = localRegions[z][x][y];

								if (data != -1) {                                                       
									int constructedRegionX = data >> 14 & 0x3ff;
							int constructedRegionY = data >> 3 & 0x7ff;
					int region = (constructedRegionX / 8 << 8) + constructedRegionY / 8;
					for (int index = 0; index < regionCount; index++) {
						if (regionIds[index] != region) {
							continue;
						}
						region = -1;
						break;
					}
					if (region != -1) {
						regionIds[regionCount++] = region;
					}
								}
							}
						}
					}
					localRegionMapData = new byte[regionCount][];
					localRegionLandscapeData = new byte[regionCount][];
					localRegionIds = new int[regionCount];
					localRegionMapIds = new int[regionCount];
					localRegionLandscapeIds = new int[regionCount];
					for (int index = 0; index < regionCount; index++) {                                    
						int id = localRegionIds[index] = regionIds[index];                                    
						int constructedRegionX = id >> 8 & 0xff;                                
					int constructedRegionY = id & 0xff;                                    
					int map = localRegionMapIds[index] = resourceProvider.resolve(0, constructedRegionY, constructedRegionX);

					if (map != -1) {
						resourceProvider.provide(3, map);
					}                                    

					int landscape = localRegionLandscapeIds[index] = resourceProvider.resolve(1, constructedRegionY, constructedRegionX);

					if (landscape != -1) {
						resourceProvider.provide(3, landscape);
					}

					}
				}
				int dx = regionBaseX - previousAbsoluteX;                         
				int dy = regionBaseY - previousAbsoluteY;                         
				previousAbsoluteX = regionBaseX;
				previousAbsoluteY = regionBaseY;
				for (int index = 0; index < 16384; index++) {                              
					Npc npc = npcs[index];
					if (npc != null) {
						for (int point = 0; point < 10; point++) {                                          
							npc.pathX[point] -= dx;
							npc.pathY[point] -= dy;
						}
						npc.x -= dx * 128;
						npc.y -= dy * 128;
					}
				}
				for (int index = 0; index < maxPlayers; index++) {                              
					Player player = players[index];
					if (player != null) {
						for (int point = 0; point < 10; point++) {                                          
							player.pathX[point] -= dx;
							player.pathY[point] -= dy;
						}
						player.x -= dx * 128;
						player.y -= dy * 128;
					}
				}
				validLocalMap = true;
				byte startX = 0;                        
				byte endX = 104;                        
				byte stepX = 1;                        
				if (dx < 0) {
					startX = 103;
					endX = -1;
					stepX = -1;
				}
				byte startY = 0;                        
				byte endY = 104;                        
				byte stepY = 1;  

				if (dy < 0) {
					startY = 103;
					endY = -1;
					stepY = -1;
				}
				for (int x = startX; x != endX; x += stepX) {                              
					for (int y = startY; y != endY; y += stepY) {                                    
						int shiftedX = x + dx;                                    
						int shiftedY = y + dy;                                    
						for (int plane = 0; plane < 4; plane++)                                          
							if (shiftedX >= 0 && shiftedY >= 0 && shiftedX < 104 && shiftedY < 104) {
								groundItems[plane][x][y] = groundItems[plane][shiftedX][shiftedY];
							} else {
								groundItems[plane][x][y] = null;
							}
					}
				}                        
				for (SpawnedObject object = (SpawnedObject) spawns
						.reverseGetFirst(); object != null; object =
						(SpawnedObject) spawns.reverseGetNext()) {
					object.x -= dx;
					object.y -= dy;
					if (object.x < 0 || object.y < 0
							|| object.x >= 104 || object.y >= 104)
						object.unlink();
				}
				if (destinationX != 0) {
					destinationX -= dx;
					destY -= dy;
				}
				oriented = false;
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SEND_WALKABLE_INTERFACE) {
				int interfaceId = incoming.readInt();
				if (interfaceId >= 0)
					resetAnimation(interfaceId);
				openWalkableInterface = interfaceId;
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SEND_MINIMAP_STATE) {
				minimapState = incoming.readUnsignedByte();
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SHOW_NPC_HEAD_ON_INTERFACE) {
				int npcId = incoming.readLEUShortA();
				int interfaceId = incoming.readLEUShortA();
				Widget.interfaceCache[interfaceId].defaultMediaType = 2;
				Widget.interfaceCache[interfaceId].defaultMedia = npcId;
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SYSTEM_UPDATE) {
				systemUpdateTime = incoming.readLEUShort() * 30;
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SEND_MULTIPLE_MAP_PACKETS) {
				localY = incoming.readUnsignedByte();
				localX = incoming.readNegUByte();
				while (incoming.currentPosition < packetSize) {
					int k3 = incoming.readUnsignedByte();
					parseRegionPackets(incoming, k3);
				}
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SEND_EARTHQUAKE) {
				int quakeDirection = incoming.readUnsignedByte();
				int quakeMagnitude = incoming.readUnsignedByte();
				int quakeAmplitude = incoming.readUnsignedByte();
				int fourPiOverPeriod = incoming.readUnsignedByte();
				quakeDirectionActive[quakeDirection] = true;
				quakeMagnitudes[quakeDirection] = quakeMagnitude;
				quakeAmplitudes[quakeDirection] = quakeAmplitude;
				quake4PiOverPeriods[quakeDirection] = fourPiOverPeriod;
				quakeTimes[quakeDirection] = 0;
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.PLAY_SOUND_EFFECT) {
				int soundId = incoming.readUShort();
				int type = incoming.readUnsignedByte();
				int delay = incoming.readUShort();
				int volume = incoming.readUShort();
				tracks[trackCount] = soundId;
				trackLoops[trackCount] = type;
				soundDelay[trackCount] = delay + Track.delays[soundId];
				soundVolume[trackCount] = volume;
				trackCount++;
				opcode = -1;
				return true;
			}

			if(opcode == PacketConstants.SET_AUTOCAST_ID) {
				int auto = incoming.readUShort();
				if(auto == -1) {
					autocast = false;
					autoCastId = 0;
				} else {
					autocast = true;
					autoCastId = auto;
				}
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SEND_PLAYER_OPTION) {
				int slot = incoming.readNegUByte();
				int lowPriority = incoming.readUByteA();
				String message = incoming.readString();
				if (slot >= 1 && slot <= 5) {
					if (message.equalsIgnoreCase("null"))
						message = null;
					playerOptions[slot - 1] = message;
					playerOptionsHighPriority[slot - 1] = lowPriority == 0;
				}
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.CLEAR_MINIMAP_FLAG) {
				destinationX = 0;
				opcode = -1;
				return true;
			}
			
			if(opcode == PacketConstants.ENABLE_NOCLIP) {
				for (int plane = 0; plane < 4; plane++) {
					for (int x = 1; x < 103; x++) {                                                  	
						for (int y = 1; y < 103; y++) {                                                    	  
							collisionMaps[plane].adjacencies[x][y] =
									0;
						}
					}
				}
				opcode = -1;
				return true;
			}
			
			if(opcode == PacketConstants.SEND_URL) {
				String url = incoming.readString();
				MiscUtils.launchURL(url);
				opcode = -1;
				return true;
			}

			if(opcode == PacketConstants.SEND_CLAN_CHAT_MESSAGE) {
				String message = incoming.readString();
				sendMessage(message, 16, "");
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SEND_MESSAGE) {
				String message = incoming.readString();
				if (message.endsWith(":tradereq:")) {
					String name = message.substring(0, message.indexOf(":"));
					long encodedName = StringUtils.encodeBase37(name);
					boolean ignored = false;
					for (int index = 0; index < ignoreCount; index++) {
						if (ignoreListAsLongs[index] != encodedName)
							continue;
						ignored = true;

					}
					if (!ignored && onTutorialIsland == 0)
						sendMessage("wishes to trade with you.", 4, name);
				} else if (message.endsWith("#url#")) {
					String link = message.substring(0, message.indexOf("#"));
					sendMessage("Join us at: ", 9, link);
				} else if (message.endsWith(":duelreq:")) {
					String name = message.substring(0, message.indexOf(":"));
					long encodedName = StringUtils.encodeBase37(name);
					boolean ignored = false;
					for (int count = 0; count < ignoreCount; count++) {
						if (ignoreListAsLongs[count] != encodedName)
							continue;
						ignored = true;

					}
					if (!ignored && onTutorialIsland == 0)
						sendMessage("wishes to duel with you.", 8, name);
				} else if (message.endsWith(":chalreq:")) {
					String name = message.substring(0, message.indexOf(":"));
					long encodedName = StringUtils.encodeBase37(name);
					boolean ignored = false;
					for (int index = 0; index < ignoreCount; index++) {
						if (ignoreListAsLongs[index] != encodedName)
							continue;
						ignored = true;

					}
					if (!ignored && onTutorialIsland == 0) {
						String msg = message.substring(message.indexOf(":") + 1,
								message.length() - 9);
						sendMessage(msg, 8, name);
					}
				} else {
					sendMessage(message, 0, "");
				}
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.STOP_ALL_ANIMATIONS) {
				for (int index = 0; index < players.length; index++) {
					if (players[index] != null)
						players[index].emoteAnimation = -1;
				}
				for (int index = 0; index < npcs.length; index++) {
					if (npcs[index] != null)
						npcs[index].emoteAnimation = -1;
				}
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.ADD_FRIEND) {
				long encodedName = incoming.readLong();
				int world = incoming.readUnsignedByte();
				String name = StringUtils
						.formatText(StringUtils.decodeBase37(encodedName));
				for (int playerIndex = 0; playerIndex < friendsCount; playerIndex++) {
					if (encodedName != friendsListAsLongs[playerIndex])
						continue;
					if (friendsNodeIDs[playerIndex] != world) {
						friendsNodeIDs[playerIndex] = world;

							sendMessage(name + " has logged in.", 5, "");
						}
//						if (world <= 1) {
//							sendMessage(name + " has logged out.", 5, "");
//						}*/

					name = null;

				}
				if (name != null && friendsCount < 200) {
					friendsListAsLongs[friendsCount] = encodedName;
					friendsList[friendsCount] = name;
					friendsNodeIDs[friendsCount] = world;
					friendsCount++;
				}
				for (boolean stopSorting = false; !stopSorting;) {
					stopSorting = true;
					for (int friendIndex = 0; friendIndex < friendsCount - 1; friendIndex++)
						if (friendsNodeIDs[friendIndex] != nodeID && friendsNodeIDs[friendIndex + 1] == nodeID || friendsNodeIDs[friendIndex] == 0 && friendsNodeIDs[friendIndex + 1] != 0) {
							int tempFriendNodeId = friendsNodeIDs[friendIndex];
							friendsNodeIDs[friendIndex] = friendsNodeIDs[friendIndex + 1];
							friendsNodeIDs[friendIndex + 1] = tempFriendNodeId;
							String tempFriendName = friendsList[friendIndex];
							friendsList[friendIndex] = friendsList[friendIndex + 1];
							friendsList[friendIndex + 1] = tempFriendName;
							long tempFriendLong = friendsListAsLongs[friendIndex];
							friendsListAsLongs[friendIndex] = friendsListAsLongs[friendIndex + 1];
							friendsListAsLongs[friendIndex + 1] = tempFriendLong;
							stopSorting = false;
						}
				}
				opcode = -1;
				return true;
			}

			if(opcode == PacketConstants.REMOVE_FRIEND) {
				long nameHash = incoming.readLong();

				for (int i = 0; i < friendsCount; i++) {
					if (friendsListAsLongs[i] != nameHash) {
						continue;
					}

					friendsCount--;
					for (int n = i; n < friendsCount; n++) {
						friendsList[n] = friendsList[n + 1];
						friendsNodeIDs[n] = friendsNodeIDs[n + 1];
						friendsListAsLongs[n] = friendsListAsLongs[n + 1];
					}
					break;
				}

				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.ADD_IGNORE) {
				long encodedName = incoming.readLong();
				if (ignoreCount < 200) {
					ignoreListAsLongs[ignoreCount] = encodedName;
					ignoreCount++;
				}
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.REMOVE_IGNORE) {
				long nameHash = incoming.readLong();
				for (int index = 0; index < ignoreCount; index++) {
					if (ignoreListAsLongs[index] == nameHash) {
						ignoreCount--;
						System.arraycopy(ignoreListAsLongs, index + 1, ignoreListAsLongs,
								index, ignoreCount - index);
						break;
					}
				}
				opcode = -1;
				return true;
			}

			if(opcode == PacketConstants.SEND_TOGGLE_QUICK_PRAYERS) {
				prayClicked = incoming.readUnsignedByte() == 1;
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SEND_RUN_ENERGY) {
				runEnergy = incoming.readUnsignedByte();
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SEND_TOGGLE_RUN) {
				settings[152] = settings[173] = incoming.readUnsignedByte();
				opcode = -1;
				return true;
			}
			

			if (opcode == PacketConstants.SEND_EXIT) {
				System.exit(1);
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SEND_HINT_ICON) {
				// the first byte, which indicates the type of mob
				hintIconDrawType = incoming.readUnsignedByte();
				if (hintIconDrawType == 1) //NPC Hint Arrow
					// the world index or slot of the npc in the server (which is also the same for the client (should))
					hintIconNpcId = incoming.readUShort();
				if (hintIconDrawType >= 2 && hintIconDrawType <= 6) { //Location Hint Arrow
					if (hintIconDrawType == 2) { //Center
						hintIconLocationArrowRelX = 64;
						hintIconLocationArrowRelY = 64;
					}
					if (hintIconDrawType == 3) { //West side
						hintIconLocationArrowRelX = 0;
						hintIconLocationArrowRelY = 64;
					}
					if (hintIconDrawType == 4) { //East side
						hintIconLocationArrowRelX = 128;
						hintIconLocationArrowRelY = 64;
					}
					if (hintIconDrawType == 5) { //South side
						hintIconLocationArrowRelX = 64;
						hintIconLocationArrowRelY = 0;
					}
					if (hintIconDrawType == 6) { //North side
						hintIconLocationArrowRelX = 64;
						hintIconLocationArrowRelY = 128;
					}
					hintIconDrawType = 2;
					//x offset
					hintIconX = incoming.readUShort();

					// y offset
					hintIconY = incoming.readUShort();

					// z offset
					hintIconLocationArrowHeight = incoming.readUnsignedByte();
				}
				if (hintIconDrawType == 10) //Player Hint Arrow
					hintIconPlayerId = incoming.readUShort();
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SEND_DUO_INTERFACE) { //Send Duo Interface: Main + Sidebar
				int mainInterfaceId = incoming.readUShortA();
				int sidebarOverlayInterfaceId = incoming.readUShort();
				if (backDialogueId != -1) {
					backDialogueId = -1;
					updateChatbox = true;
				}
				if (inputDialogState != 0) {
					inputDialogState = 0;
					updateChatbox = true;
				}
				openInterfaceId = mainInterfaceId;
				overlayInterfaceId = sidebarOverlayInterfaceId;
				tabAreaAltered = true;
				continuedDialogue = false;
				opcode = -1;
				return true;
			}

			if (opcode == 79) {
				int id = incoming.readLEUShort();
				int scrollPosition = incoming.readUShortA();
				Widget widget = Widget.interfaceCache[id];
				if (widget != null && widget.type == 0) {
					if (scrollPosition < 0)
						scrollPosition = 0;
					if (scrollPosition > widget.scrollMax - widget.height)
						scrollPosition = widget.scrollMax - widget.height;
					widget.scrollPosition = scrollPosition;
				}
				opcode = -1;
				return true;
			}

			if (opcode == 68) {
				for (int k5 = 0; k5 < settings.length; k5++)
					if (settings[k5] != anIntArray1045[k5]) {
						settings[k5] = anIntArray1045[k5];
						updateVarp(k5);
					}
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SEND_RECEIVED_PRIVATE_MESSAGE) {
				long encodedName = incoming.readLong();
				int messageId = incoming.readInt();
				int rights = incoming.readUnsignedByte();
				boolean ignoreRequest = false;

				if (rights <= 1) {
					for (int index = 0; index < ignoreCount; index++) {
						if (ignoreListAsLongs[index] != encodedName)
							continue;
						ignoreRequest = true;

					}
				}
				if (!ignoreRequest && onTutorialIsland == 0)
					try {
						privateMessageIds[privateMessageCount] = messageId;
						privateMessageCount = (privateMessageCount + 1) % 100;
						String message = incoming.readString();
						//ChatMessageCodec.decode(packetSize - 13, incoming);
						// if(l21 != 3)
						// s9 = Censor.doCensor(s9);
						if(rights > 0 && rights < 10) {
							sendMessage(message, 7, "@cr"+rights+"@"
									+ StringUtils.formatText(StringUtils
											.decodeBase37(encodedName)));
						} else {
							sendMessage(message, 3, StringUtils.formatText(
									StringUtils.decodeBase37(encodedName)));
						}
						
					} catch (Exception ex) {
						SignLink.reporterror("cde1");
					}
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SEND_REGION) {
				localY = incoming.readNegUByte();
				localX = incoming.readNegUByte();
				opcode = -1;
				return true;
			}

			if (opcode == 24) {
				flashingSidebarId = incoming.readUByteS();
				if (flashingSidebarId == tabId) {
					if (flashingSidebarId == 3)
						tabId = 1;
					else
						tabId = 3;
				}
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SEND_ITEM_TO_INTERFACE) {
				int widget = incoming.readLEUShort();
				int scale = incoming.readUShort();
				int item = incoming.readUShort();
				if (item == 65535) {
					Widget.interfaceCache[widget].defaultMediaType = 0;
					opcode = -1;
					return true;
				} else {
					ItemDefinition definition = ItemDefinition.lookup(item);
					Widget.interfaceCache[widget].defaultMediaType = 4;
					Widget.interfaceCache[widget].defaultMedia = item;
					Widget.interfaceCache[widget].modelRotation1 = definition.rotation_y;
					Widget.interfaceCache[widget].modelRotation2 = definition.rotation_x;
					Widget.interfaceCache[widget].modelZoom =
							(definition.model_zoom * 100) / scale;
					opcode = -1;
					return true;
				}
			}
			
			if (opcode == PacketConstants.SEND_ITEM_POSITION) {
				int widget = incoming.readLEUShort();
				int scale = incoming.readUShort();
				int item = incoming.readUShort();
				int x = incoming.readUShort();
				int y = incoming.readUShort();
				if (item == 65535) {
					Widget.interfaceCache[widget].defaultMediaType = 0;
					opcode = -1;
					return true;
				} else {
					ItemDefinition definition = ItemDefinition.lookup(item);
					Widget.interfaceCache[widget].defaultMediaType = 4;
					Widget.interfaceCache[widget].defaultMedia = item;
					Widget.interfaceCache[widget].modelRotation1 = definition.rotation_y;
					Widget.interfaceCache[widget].modelRotation2 = definition.rotation_x;
					Widget.interfaceCache[widget].modelZoom =
							(definition.model_zoom * 100) / scale;
					Widget.interfaceCache[widget].verticalOffset = 0;
					Widget.interfaceCache[widget].horizontalOffset = 0;
					Widget.interfaceCache[widget].verticalOffset += y;
					Widget.interfaceCache[widget].horizontalOffset += x;
					opcode = -1;
					return true;
				}
			}

			if (opcode == PacketConstants.SHOW_HIDE_INTERFACE_CONTAINER) {
				boolean hide = incoming.readUnsignedByte() == 1;
				int id = incoming.readUShort();
				Widget.interfaceCache[id].invisible = hide;
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SEND_SOLO_NON_WALKABLE_SIDEBAR_INTERFACE) {
				int id = incoming.readLEUShort();
				resetAnimation(id);
				if (backDialogueId != -1) {
					backDialogueId = -1;
					updateChatbox = true;
				}
				if (inputDialogState != 0) {
					inputDialogState = 0;
					updateChatbox = true;
				}
				overlayInterfaceId = id;
				tabAreaAltered = true;
				openInterfaceId = -1;
				continuedDialogue = false;
				opcode = -1;
				return true;
			}

			if (opcode == 137) {
				specialAttack = incoming.readUnsignedByte();
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SET_INTERFACE_TEXT) {
				try {

					String text = incoming.readString();
					int id = incoming.readInt();

					//	updateStrings(text, id);
					sendString(text, id);

				} catch (Exception e) {
					e.printStackTrace();
				}
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.UPDATE_CHAT_MODES) {
				publicChatMode = incoming.readUnsignedByte();
				privateChatMode = incoming.readUnsignedByte();
				tradeMode = incoming.readUnsignedByte();
				updateChatbox = true;
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SEND_PLAYER_WEIGHT) {
				weight = incoming.readShort();
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SEND_MODEL_TO_INTERFACE) {
				int id = incoming.readLEUShortA();
				int model = incoming.readUShort();
				Widget.interfaceCache[id].defaultMediaType = 1;
				Widget.interfaceCache[id].defaultMedia = model;
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SEND_CHANGE_INTERFACE_COLOUR) {
				int id = incoming.readLEUShortA();
				int color = incoming.readLEUShortA();
				int red = color >> 10 & 0x1f;
					int green = color >> 5 & 0x1f;
				int blue = color & 0x1f;
				Widget.interfaceCache[id].textColor =
						(red << 19) + (green << 11) + (blue << 3);
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SEND_UPDATE_ITEMS) {
				try {

					int interfaceId = incoming.readInt();
					int itemCount = incoming.readShort();

					Widget widget = Widget.interfaceCache[interfaceId];
					if(widget == null || widget.inventoryItemId == null || widget.inventoryAmounts == null) {
						opcode = -1;
						return true;
					}

					for (int j22 = 0; j22 < itemCount; j22++) { 
						if(j22 == widget.inventoryItemId.length) {
							break;
						}
						int amount = incoming.readInt();

						if(amount == 0) {
							widget.inventoryItemId[j22] = -1;
						} else {
							widget.inventoryItemId[j22] = incoming.readShort();
						}

						widget.inventoryAmounts[j22] = amount;
					}

					for (int slot = itemCount; slot < widget.inventoryItemId.length; slot++) {
						widget.inventoryItemId[slot] = 0;
						widget.inventoryAmounts[slot] = 0;
					}

				} catch(Exception e) {
					e.printStackTrace();
				}
				opcode = -1;
				return true;
			}

			if(opcode == PacketConstants.SEND_CURRENT_BANK_TAB) {
				currentBankTab = incoming.readUnsignedByte();				
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SEND_EFFECT_TIMER) {
				try {

					int timer = incoming.readShort();
					int sprite = incoming.readShort();

					addEffectTimer(new EffectTimer(timer, sprite));

				} catch(Exception e) {
					e.printStackTrace();
				}
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SET_MODEL_INTERFACE_ZOOM) {
				int scale = incoming.readUShortA();
				int id = incoming.readUShort();
				int pitch = incoming.readUShort();
				int roll = incoming.readLEUShortA();
				Widget.interfaceCache[id].modelRotation1 = pitch;
				Widget.interfaceCache[id].modelRotation2 = roll;
				Widget.interfaceCache[id].modelZoom = scale;
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SET_FRIENDSERVER_STATUS) {
				friendServerStatus = incoming.readUnsignedByte();
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.MOVE_CAMERA) { //Gradually turn camera to spatial point.
				oriented = true;
				cinematicCamXViewpointLoc = incoming.readUnsignedByte();
				cinematicCamYViewpointLoc = incoming.readUnsignedByte();
				cinematicCamZViewpointLoc = incoming.readUShort();
				constCinematicCamRotationSpeed = incoming.readUnsignedByte();
				varCinematicCamRotationSpeedPromille = incoming.readUnsignedByte();
				if (varCinematicCamRotationSpeedPromille >= 100) {
					int cinCamXViewpointPos = cinematicCamXViewpointLoc * 128 + 64;
					int cinCamYViewpointPos = cinematicCamYViewpointLoc * 128 + 64;
					int cinCamZViewpointPos = getCenterHeight(plane, cinCamYViewpointPos, cinCamXViewpointPos) - cinematicCamZViewpointLoc;
					int dXPos = cinCamXViewpointPos - xCameraPos;
					int dYPos = cinCamYViewpointPos - yCameraPos;
					int dZPos = cinCamZViewpointPos - zCameraPos;
					int flatDistance = (int) Math.sqrt(dXPos * dXPos + dYPos * dYPos);
					yCameraCurve = (int) (Math.atan2(dZPos, flatDistance) * 325.94900000000001D)
							& 0x7ff;
					xCameraCurve = (int) (Math.atan2(dXPos, dYPos) * -325.94900000000001D)
							& 0x7ff;
					if (yCameraCurve < 128)
						yCameraCurve = 128;
					if (yCameraCurve > 383)
						yCameraCurve = 383;
				}
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SEND_INITIALIZE_PACKET) {
				member = incoming.readUByteA();
				localPlayerIndex = incoming.readShort();
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.NPC_UPDATING) {
				updateNPCs(incoming, packetSize);
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SEND_ENTER_AMOUNT) {
				String title = incoming.readString();
				enter_amount_title = title;
				messagePromptRaised = false;
				inputDialogState = 1;
				amountOrNameInput = "";
				updateChatbox = true;
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SEND_ENTER_NAME) { //Send Enter Name Dialogue (still allows numbers)
				String title = incoming.readString();
				enter_name_title = title;
				messagePromptRaised = false;
				inputDialogState = 2;
				amountOrNameInput = "";
				updateChatbox = true;
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SEND_NON_WALKABLE_INTERFACE) {
				int interfaceId = incoming.readUShort();
				resetAnimation(interfaceId);
				if (overlayInterfaceId != -1) {
					overlayInterfaceId = -1;
					tabAreaAltered = true;
				}
				if (backDialogueId != -1) {
					backDialogueId = -1;
					updateChatbox = true;
				}
				if (inputDialogState != 0) {
					inputDialogState = 0;
					updateChatbox = true;
				}
				if (interfaceId == 15244) {
					fullscreenInterfaceID = 17511;
					openInterfaceId = 15244;
				}
				openInterfaceId = interfaceId;
				continuedDialogue = false;
				opcode = -1;

				return true;
			}

			if (opcode == PacketConstants.SEND_WALKABLE_CHATBOX_INTERFACE) {
				dialogueId = incoming.readLEShortA();
				updateChatbox = true;
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SEND_CONFIG_INT) {
				int id = incoming.readLEUShort();
				int value = incoming.readMEInt();
				anIntArray1045[id] = value;
				if (settings[id] != value) {
					settings[id] = value;

					updateVarp(id);
					if (dialogueId != -1)
						updateChatbox = true;
				}
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SEND_CONFIG_BYTE) {
				int id = incoming.readLEUShort();
				byte value = incoming.readSignedByte();
				if(id < anIntArray1045.length) {
					anIntArray1045[id] = value;
					if (settings[id] != value) {
						settings[id] = value;
						updateVarp(id);
						if (dialogueId != -1)
							updateChatbox = true;
					}
				}
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SEND_MULTICOMBAT_ICON) {
				multicombat = incoming.readUnsignedByte(); //1 is active
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SEND_ANIMATE_INTERFACE) {
				int id = incoming.readUShort();
				int animation = incoming.readShort();
				Widget widget = Widget.interfaceCache[id];
				widget.defaultAnimationId = animation;
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.CLOSE_INTERFACE) {
				if (overlayInterfaceId != -1) {
					overlayInterfaceId = -1;
					tabAreaAltered = true;
				}
				if (backDialogueId != -1) {
					backDialogueId = -1;
					updateChatbox = true;
				}
				if (inputDialogState != 0) {
					inputDialogState = 0;
					updateChatbox = true;
				}
				openInterfaceId = -1;
				continuedDialogue = false;
				opcode = -1;
				return true;
			}
			if (opcode == PacketConstants.UPDATE_SPECIFIC_ITEM) {
				int interfaceId = incoming.readUShort();
				Widget widget = Widget.interfaceCache[interfaceId];

				int slot = incoming.readUnsignedByte();
				int amount = incoming.readInt();
				int id = incoming.readUShort();
				if(id == 65535 || id < 0) {
					id = 0;
				}
				if(amount < 0) {
					amount = 0;
				}
				if(widget == null || widget.inventoryItemId == null) {
					opcode = -1;
					return true;
				}

				if (slot >= 0 && slot < widget.inventoryItemId.length) {					
					widget.inventoryItemId[slot] = id;
					widget.inventoryAmounts[slot] = amount;
				}

				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SEND_GFX || opcode == PacketConstants.SEND_GROUND_ITEM || opcode == PacketConstants.SEND_ALTER_GROUND_ITEM_COUNT || opcode == PacketConstants.SEND_REMOVE_OBJECT || opcode == 105
					|| opcode == PacketConstants.SEND_PROJECTILE || opcode == PacketConstants.TRANSFORM_PLAYER_TO_OBJECT || opcode == PacketConstants.SEND_OBJECT || opcode == PacketConstants.SEND_REMOVE_GROUND_ITEM
					|| opcode == PacketConstants.ANIMATE_OBJECT || opcode == 215) {
				parseRegionPackets(incoming, opcode);
				opcode = -1;
				return true;
			}

			if (opcode == PacketConstants.SWITCH_TAB) {
				tabId = incoming.readNegUByte();
				tabAreaAltered = true;
				opcode = -1;
				return true;
			}
			if (opcode == PacketConstants.SEND_NONWALKABLE_CHATBOX_INTERFACE) {
				int id = incoming.readLEUShort();

				resetAnimation(id);
				if (overlayInterfaceId != -1) {
					overlayInterfaceId = -1;
					tabAreaAltered = true;
				}
				backDialogueId = id;
				updateChatbox = true;
				openInterfaceId = -1;
				continuedDialogue = false;
				opcode = -1;
				return true;
			}
			
			if (opcode == PacketConstants.SEND_DUO_CHATBOX_INTERFACE) {
				int id = incoming.readLEUShort();

				resetAnimation(id);
				backDialogueId = id;
				updateChatbox = true;
				continuedDialogue = false;
				opcode = -1;
				return true;
			}

			SignLink.reporterror("T1 - " + opcode + "," + packetSize + " - "
					+ secondLastOpcode + "," + thirdLastOpcode);
			resetLogout();
		} catch (IOException _ex) {
			dropClient();
			_ex.printStackTrace();
		} catch (Exception exception) {
			String s2 = "T2 - " + opcode + "," + secondLastOpcode + "," + thirdLastOpcode
					+ " - " + packetSize + "," + (regionBaseX + localPlayer.pathX[0])
					+ "," + (regionBaseY + localPlayer.pathY[0]) + " - ";
			for (int j15 = 0; j15 < packetSize && j15 < 50; j15++)
				s2 = s2 + incoming.payload[j15] + ",";
			SignLink.reporterror(s2);
			exception.printStackTrace();
			// resetLogout();
		}
		opcode = -1;
		return true;
	}

	/*
	 * FIXME fix autochat, for some reason it stops packets from being sent
	 */
	
	private void moveCameraWithPlayer() { 
		anInt1265++;
		if(sendingAutochat && autochatString != null && autochatString.length() > 0) {
			if(autochatTimer > 100) {
				sendPacket(new Chat(0, 0, StringUtils.formatText(autochatString)));
				localPlayer.spokenText = StringUtils.formatText(autochatString);
				if (myPrivilege > 0 && myPrivilege < 10) {
											sendMessage(StringUtils.capitalize(autochatString), 2, "@cr" + myPrivilege + "@" + localPlayer.name);
										} else {
											sendMessage(StringUtils.capitalize(autochatString), 2, localPlayer.name);
										}
				updateChatbox = true;
				autochatTimer = 0;
			}
			autochatTimer++;
		}

		showPrioritizedPlayers();
		showPrioritizedNPCs();

		showOtherPlayers();
		showOtherNpcs();

		createProjectiles();
		createStationaryGraphics();
		if (!oriented) {
			int i = anInt1184;
			if (anInt984 / 256 > i)
				i = anInt984 / 256;
			if (quakeDirectionActive[4] && quakeAmplitudes[4] + 128 > i)
				i = quakeAmplitudes[4] + 128;
			int k = cameraHorizontal + cameraRotation & 0x7ff;
			setCameraPos(
					cameraZoom + i * ((SceneGraph.viewDistance == 9)
							&& (frameMode == ScreenMode.RESIZABLE) ? 2
									: SceneGraph.viewDistance == 10 ? 5 : 3),
					i, anInt1014, getCenterHeight(plane, localPlayer.y, localPlayer.x) - 50, k,
					anInt1015);
		}
		int j;
		if (!oriented)
			j = setCameraLocation();
		else
			j = resetCameraHeight();
		int l = xCameraPos;
		int i1 = zCameraPos;
		int j1 = yCameraPos;
		int k1 = yCameraCurve;
		int l1 = xCameraCurve;
		for (int i2 = 0; i2 < 5; i2++)
			if (quakeDirectionActive[i2]) {
				int j2 = (int) ((Math.random() * (double) (quakeMagnitudes[i2] * 2 + 1)
						- (double) quakeMagnitudes[i2]) + Math
						.sin((double) quakeTimes[i2]
								* ((double) quake4PiOverPeriods[i2] / 100D))
						* (double) quakeAmplitudes[i2]);
				if (i2 == 0)
					xCameraPos += j2;
				if (i2 == 1)
					zCameraPos += j2;
				if (i2 == 2)
					yCameraPos += j2;
				if (i2 == 3)
					xCameraCurve = xCameraCurve + j2 & 0x7ff;
				if (i2 == 4) {
					yCameraCurve += j2;
					if (yCameraCurve < 128)
						yCameraCurve = 128;
					if (yCameraCurve > 383)
						yCameraCurve = 383;
				}
			}
		int k2 = Rasterizer3D.lastTextureRetrievalCount;
		Model.aBoolean1684 = true;
		Model.anInt1687 = 0;
		Model.anInt1685 = super.mouseX - (frameMode == ScreenMode.FIXED ? 4 : 0);
		Model.anInt1686 = super.mouseY - (frameMode == ScreenMode.FIXED ? 4 : 0);
		Rasterizer2D.clear();
		scene.render(xCameraPos, yCameraPos, xCameraCurve, zCameraPos, j, yCameraCurve);
		scene.clearGameObjectCache();
		if (Configuration.enableFog) {
			int baseFogDistance = (int) Math.sqrt(Math.pow(zCameraPos, 2));
			int fogStart = baseFogDistance + 1100;
			int fogEnd = baseFogDistance + 2000;
			fog.renderFog(false, fogStart, fogEnd, 4);
		}
		updateEntities();
		drawHeadIcon();
		writeBackgroundTexture(k2);
		draw3dScreen();
		//	SkillOrbHandler.drawOrbs();
		drawConsoleArea();
		drawConsole();
		if(startSpin) {
			startSpinner();
		}
		if(openInterfaceId == -1) {

			//Combat hp overlay
			if(shouldDrawCombatBox()) {
				drawCombatBox();
			}

			if(skillOrbs) {
				SkillOrbs.process();
			}
			if(expDrops) {
				drawExpCounterDrops();
			}
		}
		if (frameMode != ScreenMode.FIXED) {
			drawChatArea();
			drawMinimap();
			drawTabArea();
		}
		gameScreenImageProducer.drawGraphics(frameMode == ScreenMode.FIXED ? 4 : 0,
				super.graphics, frameMode == ScreenMode.FIXED ? 4 : 0);
		xCameraPos = l;
		zCameraPos = i1;
		yCameraPos = j1;
		yCameraCurve = k1;
		xCameraCurve = l1;
	}
	
	private void startSpinner() {
		Widget w = Widget.interfaceCache[13101];
		Widget w2 = Widget.interfaceCache[13200];
		Widget rewardInt = Widget.interfaceCache[13300];
		Widget rewards = Widget.interfaceCache[13303];
		if(w.horizontalOffset >= -1000) {
			w.horizontalOffset -= 25;
			w2.horizontalOffset -= 25;
		}
		if(w.horizontalOffset >= -1912 && w.horizontalOffset <= -1001) {
			w.horizontalOffset -= (25 / spinSpeed);
			w2.horizontalOffset -= (25 / spinSpeed);
		spinSpeed = spinSpeed + 0.07f;
		}
		if(w.horizontalOffset >= -2000 && w.horizontalOffset < -1913) {
			w.horizontalOffset -= (25 / spinSpeed);
			w2.horizontalOffset -= (25 / spinSpeed);
			spinSpeed = spinSpeed + 2f;
		}
		if(w.horizontalOffset <= -1913) {
			rewardInt.invisible = false;
			amountValue += (w2.inventoryAmounts[51] * ItemDefinition.lookup((w2.inventoryItemId[51]) - 1).value);
			Widget.interfaceCache[13019].defaultText = Integer.toString(amountValue);
			Widget.interfaceCache[13305].defaultText = ItemDefinition.lookup((w2.inventoryItemId[51]) - 1).name.replaceAll("_", " ");
			rewards.inventoryItemId[0] = w2.inventoryItemId[51];
			rewards.inventoryAmounts[0] = w2.inventoryAmounts[51];
			startSpin = false;
			
		}
	}

	private void tabToReplyPm() {
		String name = null;

		for (int k = 0; k < 100; k++) {
			if (chatMessages[k] == null) {
				continue;
			}

			int l = chatTypes[k];

			if (l == 3 || l == 7) {
				name = chatNames[k];
				break;
			}
		}

		if (name == null) {
			sendMessage("You haven't received any messages to which you can reply.", 0, "");
			return;
		}

		if (name != null) {
			if(name.contains("@")) {
				name = name.substring(5);
			}
		}

		long nameAsLong = MiscUtils.longForName(name.trim());

		if (nameAsLong != -1) {
			
			updateChatbox = true;
			inputDialogState = 0;
			messagePromptRaised = true;
			promptInput = "";
			friendsListAction = 3;
			aLong953 = nameAsLong;
			aString1121 = "Enter a message to send to " + name;
		}
	}

	private void processMinimapActions() {
		if (openInterfaceId == 15244) {
			return;
		}
		final boolean fixed = frameMode == ScreenMode.FIXED;
		if (fixed ? super.mouseX >= 542 && super.mouseX <= 579 && super.mouseY >= 2
				&& super.mouseY <= 38
				: super.mouseX >= frameWidth - 180 && super.mouseX <= frameWidth - 139
				&& super.mouseY >= 0 && super.mouseY <= 40) {
			menuActionText[1] = "Look North";
			menuActionTypes[1] = 696;
			menuActionRow = 2;
		}
		if (frameMode != ScreenMode.FIXED && changeTabArea) {
			if (super.mouseX >= frameWidth - 26 && super.mouseX <= frameWidth - 1
					&& super.mouseY >= 2 && super.mouseY <= 24) {
				menuActionText[1] = "Logout";
				menuActionTypes[1] = 700;
				menuActionRow = 2;
			}
		}
		if (worldHover && Configuration.enableOrbs) {
			menuActionText[1] = "Floating @lre@World Map";
			menuActionTypes[1] = 850;
			menuActionRow = 2;
		}
		if (expCounterHover) {
			if (expDrops) {
				menuActionText[3] = "Hide @lre@XP drops";
				menuActionTypes[3] = 258;
			}
			if (!expDrops) {
				menuActionText[3] = "Show @lre@XP drops";
				menuActionTypes[3] = 258;
			}
			menuActionText[2] = "Setup @lre@XP drops";
			menuActionTypes[2] = 257;
			menuActionRow = 4;
		}
		if (prayHover && Configuration.enableOrbs) {
			menuActionText[2] =
					prayClicked ? "Turn Quick Prayers off" : "Turn Quick Prayers on";
			menuActionTypes[2] = 1500;
			menuActionRow = 2;
			menuActionText[1] = "Setup Quick Prayers";
			menuActionTypes[1] = 1506;
			menuActionRow = 3;
		}
		if (runHover && Configuration.enableOrbs) {
			menuActionText[1] = settings[152] == 1 ? "Toggle Run" : "Toggle Run";
			menuActionTypes[1] = 1050;
			menuActionRow = 2;
		}
	}

	public boolean skillOrbs = true;
	public boolean expDrops = true;

	/**
	 * Draws the exp counter
	 */
	public void drawExpCounter() {

		final boolean wilderness = openWalkableInterface == 23300;

		int height = 45;
		int width = 138;
		int xPos = wilderness && frameMode != ScreenMode.FIXED ? frameWidth - 383 : frameWidth - 395;
		int yPos = wilderness ? (frameMode != ScreenMode.FIXED ? 114 : 100) : 2;

		//Draw box ..
		//Rasterizer2D.drawTransparentBox(xPos, yPos, width, height, 0x5a5245, 150);
		cacheSprite[337].drawAdvancedSprite(xPos, yPos);

		//Draw skill info..
		if(currentSkill != -1) {

			//Draw sprite icon..
			cacheSprite[73 + currentSkill].drawAdvancedSprite(xPos + 6, yPos + 7);

			//Draw black
			//	Rasterizer2D.drawBox(xPos + 5, yPos + 30, width - 8, 10, 00000);
			cacheSprite[338].drawAdvancedSprite(xPos + 5, yPos + 30);

			//Draw green
			int currentLevel = maximumLevels[currentSkill];
			int percent = (width - 11);

			//Attempt to calculate percent...
			if(currentLevel != 99) {
				try {
					int initExp = getXPForLevel(currentLevel);
					int newExp = getXPForLevel(currentLevel + 1);					
					int gained = currentExp[currentSkill] - initExp;
					int remainder = newExp - initExp;
					percent = (int) (((double) gained / (double) remainder) * (width - 11));
					if(percent > (width - 11)) {
						percent = (width - 11);
					}
				} catch(ArithmeticException e) {
					e.printStackTrace();
				}
			}

			Rasterizer2D.drawBox(xPos + 6, yPos + 32, percent, 8, getProgressColor(percent));
		}

		//Draw total exp..
		String totalExpString = "+" + StringUtils.insertCommasToNumber(""+totalExp+"");
		int textDrawX = totalExp < 1000 ? xPos + 120 : totalExp < 10000 ? xPos + 110 :  xPos + 100 - totalExpString.length();
		newBoldFont.drawCenteredString(totalExpString, textDrawX, yPos + 14, 16777215, 0);
	}

	/**
	 * Gets the progress color for the xp bar
	 * @param percent
	 * @return
	 */
	public static int getProgressColor(int percent) {
		if(percent <= 15) {
			return 0x808080;
		}
		if(percent <= 45) {
			return 0x7f7f00;
		}
		if(percent <= 65) {
			return 0x999900;
		}
		if(percent <= 75) {
			return 0xb2b200;
		}
		if(percent <= 90) {
			return 0x007f00;
		}
		return 31744;	
	}

	/**
	 * The current skill being practised.
	 */
	private int currentSkill = -1;

	/**
	 * The player's total exp
	 */
	private long totalExp;
	
	public static String insertCommas(long i) {
        return String.format("%,d", i);
    }

	/**
	 * Drawing of exp counter drops
	 */
	private int digits, xpCounter;
	private void drawExpCounterDrops() {

		final boolean wilderness = openWalkableInterface == 23300;

		RSFont xp_font = newSmallFont;
		int font_height = 24;
		int x = frameWidth - 280;
		int x2 = frameWidth;
		int y = wilderness ? -100 : -100;
		String xpString = insertCommas(totalExp);
		digits = xpCounter == 0 ? 1 : 1 + (int) Math.floor(Math.log10(xpCounter));
		int lengthToRemove = Integer.toString(xpCounter).length();
		int i = regularText.getTextWidth(Integer.toString(xpCounter))
				- regularText.getTextWidth(Integer.toString(xpCounter)) / 2;
		int a = lengthToRemove == 1 ? 5 : ((lengthToRemove - 1) * 5);
		Rasterizer2D.drawBoxOutline(x2 - 380, 4, 120, 30, 0x383023); // 5a5245
		Rasterizer2D.drawBoxOutline(x2 - 379, 5, 118, 28, 0x5a5245); // 5a5245
		Rasterizer2D.drawTransparentBox(x2 - 378, 6, 116, 26, 0x5a5245, 150); // 5a5245
		cacheSprite[432].drawSprite(x2 - 379, 1);
		smallText.drawTextWithPotentialShadow(true, x2 - 265 - smallText.getTextWidth(xpString), 0xffffff, xpString,
				25);
		for (i = 0; i < xp_added.length; i++) {
			if (xp_added[i][0] > -1) {
				if (xp_added[i][2] >= 0) {
					int transparency = 256;
					if (xp_added[i][2] > 120)
						transparency = (10 - (xp_added[i][2] - 120)) * 256 / 20;
					if (transparency > 0) {
						String s = "<col=ffffff><shad=000000><trans=" + transparency + ">+"
								+ NumberFormat.getIntegerInstance().format(xp_added[i][1]);
						int icons_x_off = 0;
						Sprite sprite = null;
						for (int i2 = 0; i2 < skill_sprites.length; i2++) {
							if ((xp_added[i][0] & (1 << i2)) == 0)
								continue;

							sprite = skill_sprites[i2];
							icons_x_off += sprite.myWidth + 3;
							sprite.drawSprite(x - a + 12 - xp_font.getTextWidth(s) - icons_x_off,
									y + 157 + (140 - xp_added[i][2]) - (font_height / 2) - (sprite.myHeight / 2),
									transparency);
						}
						xp_font.drawBasicString(s, x - a + 12 - xp_font.getTextWidth(s),
								y + 150 + (140 - xp_added[i][2]), 0xFF9900, -1);
					}

				}

				xp_added[i][2]++;

				if (xp_added[i][2] >= (wilderness ? 60 : 240))
					xp_added[i][0] = -1;
			}
		}
	}

	public static int getXPForLevel(int level) {
		int points = 0;
		int output = 0;
		for (int lvl = 1; lvl <= level; lvl++) {
			points += Math.floor(lvl + 300.0 * Math.pow(2.0, lvl / 7.0));
			if (lvl >= level) {
				return output;
			}
			output = (int)Math.floor(points / 4);
		}
		return 0;
	}

	private int specialEnabled = 0;

	public boolean isPoisoned, clickedQuickPrayers;

	public final int[] // Perfected (Hp, pray and run orb)
			orbX = {0, 0, 24}, orbY = {41, 85, 122}, orbTextX = {15, 16, 40}, orbTextY = {67, 111, 148},
			coloredOrbX = {27, 27, 51}, coloredOrbY = {45, 89, 126},
			currentInterface = {4016, 4012, 149}, maximumInterface = {4017, 4013, 149},
			orbIconX = {33, 30, 58}, orbIconY = {51, 92, 130};

	private boolean runHover, prayHover, hpHover, prayClicked,
	specialHover, expCounterHover, worldHover, autocast;

	public int getOrbTextColor(int statusInt) {
		if (statusInt >= 75 && statusInt <= Integer.MAX_VALUE)
			return 0x00FF00;
		else if (statusInt >= 50 && statusInt <= 74)
			return 0xFFFF00;
		else if (statusInt >= 25 && statusInt <= 49)
			return 0xFF981F;
		else
			return 0xFF0000;
	}

	public int getOrbFill(int statusInt) {
		if (statusInt <= Integer.MAX_VALUE && statusInt >= 97)
			return 0;
		else if (statusInt <= 96 && statusInt >= 93)
			return 1;
		else if (statusInt <= 92 && statusInt >= 89)
			return 2;
		else if (statusInt <= 88 && statusInt >= 85)
			return 3;
		else if (statusInt <= 84 && statusInt >= 81)
			return 4;
		else if (statusInt <= 80 && statusInt >= 77)
			return 5;
		else if (statusInt <= 76 && statusInt >= 73)
			return 6;
		else if (statusInt <= 72 && statusInt >= 69)
			return 7;
		else if (statusInt <= 68 && statusInt >= 65)
			return 8;
		else if (statusInt <= 64 && statusInt >= 61)
			return 9;
		else if (statusInt <= 60 && statusInt >= 57)
			return 10;
		else if (statusInt <= 56 && statusInt >= 53)
			return 11;
		else if (statusInt <= 52 && statusInt >= 49)
			return 12;
		else if (statusInt <= 48 && statusInt >= 45)
			return 13;
		else if (statusInt <= 44 && statusInt >= 41)
			return 14;
		else if (statusInt <= 40 && statusInt >= 37)
			return 15;
		else if (statusInt <= 36 && statusInt >= 33)
			return 16;
		else if (statusInt <= 32 && statusInt >= 29)
			return 17;
		else if (statusInt <= 28 && statusInt >= 25)
			return 18;
		else if (statusInt <= 24 && statusInt >= 21)
			return 19;
		else if (statusInt <= 20 && statusInt >= 17)
			return 20;
		else if (statusInt <= 16 && statusInt >= 13)
			return 21;
		else if (statusInt <= 12 && statusInt >= 9)
			return 22;
		else if (statusInt <= 8 && statusInt >= 7)
			return 23;
		else if (statusInt <= 6 && statusInt >= 5)
			return 24;
		else if (statusInt <= 4 && statusInt >= 3)
			return 25;
		else if (statusInt <= 2 && statusInt >= 1)
			return 26;
		else if (statusInt <= 0)
			return 27;
		return 0;
	}

	public void clearTopInterfaces() {
		// close interface
		sendPacket(new CloseInterface());
		if (overlayInterfaceId != -1) {
			overlayInterfaceId = -1;
			continuedDialogue = false;
			tabAreaAltered = true;
		}
		if (backDialogueId != -1) {
			backDialogueId = -1;
			updateChatbox = true;
			continuedDialogue = false;
		}
		openInterfaceId = -1;
		fullscreenInterfaceID = -1;
	}

	public void addObject(int x, int y, int objectId, int face, int type, int height) {
		int mX = this.regionX - 6;
		int mY = this.regionY - 6;
		int x2 = x - mX * 8;
		int y2 = y - mY * 8;
		int i15 = 40 >> 2;
		int l17 = objectGroups[i15];
		if (y2 > 0 && y2 < 103 && x2 > 0 && x2 < 103) {
			requestSpawnObject(-1, objectId, face, l17, y2, type, height, x2, 0);

		}
	}

	@SuppressWarnings("unused")
	private int currentTrackPlaying;

	public Client() {
		consoleInput = "";
		consoleOpen = false;
		consoleMessages = new String[50];
		fullscreenInterfaceID = -1;
		chatRights = new int[500];
		soundVolume = new int[50];
		chatTypeView = 0;
		clanChatMode = 0;
		cButtonHPos = -1;
		currentTrackPlaying = -1;
		cButtonCPos = 0;
		server = Configuration.server_address;
		anIntArrayArray825 = new int[104][104];
		friendsNodeIDs = new int[200];
		groundItems = new Deque[4][104][104];
		aBoolean831 = false;
		npcs = new Npc[16384];
		npcIndices = new int[16384];
		removedMobs = new int[1000];
		login = Buffer.create();
		aBoolean848 = true;
		openInterfaceId = -1;
		currentExp = new int[SkillConstants.SKILL_COUNT];
		quakeMagnitudes = new int[5];
		quakeDirectionActive = new boolean[5];
		drawFlames = false;
		reportAbuseInput = "";
		localPlayerIndex = -1;
		menuOpen = false;
		inputString = "";
		maxPlayers = 2048;
		internalLocalPlayerIndex = 2047;
		players = new Player[maxPlayers];
		playerList = new int[maxPlayers];
		mobsAwaitingUpdate = new int[maxPlayers];
		playerSynchronizationBuffers = new Buffer[maxPlayers];
		anInt897 = 1;
		anIntArrayArray901 = new int[104][104];
		aByteArray912 = new byte[16384];
		currentLevels = new int[SkillConstants.SKILL_COUNT];
		ignoreListAsLongs = new long[100];
		loadingError = false;
		quake4PiOverPeriods = new int[5];
		anIntArrayArray929 = new int[104][104];
		chatTypes = new int[500];
		chatNames = new String[500];
		chatMessages = new String[500];
		sideIcons = new Sprite[15];
		aBoolean954 = true;
		friendsListAsLongs = new long[200];
		currentSong = -1;
		drawingFlames = false;
		spriteDrawX = -1;
		spriteDrawY = -1;
		anIntArray968 = new int[33];
		anIntArray969 = new int[256];
		indices = new FileStore[5];
		settings = new int[VariablePlayer.customSize]; //Varbits?
		aBoolean972 = false;
		anInt975 = 50;
		anIntArray976 = new int[anInt975];
		anIntArray977 = new int[anInt975];
		anIntArray978 = new int[anInt975];
		anIntArray979 = new int[anInt975];
		textColourEffect = new int[anInt975];
		anIntArray981 = new int[anInt975];
		anIntArray982 = new int[anInt975];
		aStringArray983 = new String[anInt975];
		lastKnownPlane = -1;
		hitMarks = new Sprite[20];
		characterDesignColours = new int[5];
		aBoolean994 = false;
		amountOrNameInput = "";
		projectiles = new Deque();
		aBoolean1017 = false;
		openWalkableInterface = -1;
		quakeTimes = new int[5];
		aBoolean1031 = false;
		mapFunctions = new Sprite[100];
		dialogueId = -1;
		maximumLevels = new int[SkillConstants.SKILL_COUNT];
		anIntArray1045 = new int[VariablePlayer.customSize]; //Varbits?
		maleCharacter = true;
		minimapLeft = new int[152];
		minimapLineWidth = new int[152];
		flashingSidebarId = -1;
		incompleteAnimables = new Deque();
		anIntArray1057 = new int[33];
		aClass9_1059 = new Widget();
		mapScenes = new IndexedImage[100];
		barFillColor = 0x4d4233;
		anIntArray1065 = new int[7];
		minimapHintX = new int[1000];
		minimapHintY = new int[1000];
		validLocalMap = false;
		friendsList = new String[200];
		incoming = Buffer.create();
		firstMenuAction = new int[500];
		secondMenuAction = new int[500];
		menuActionTypes = new int[500];
		selectedMenuActions = new int[500];
		headIcons = new Sprite[20];
		skullIcons = new Sprite[20];
		headIconsHint = new Sprite[20];
		tabAreaAltered = false;
		aString1121 = "";
		playerOptions = new String[5];
		playerOptionsHighPriority = new boolean[5];
		localRegions = new int[4][13][13];
		anInt1132 = 2;
		minimapHint = new Sprite[1000];
		inPlayerOwnedHouse = false;
		continuedDialogue = false;
		crosses = new Sprite[8];
		loggedIn = false;
		canMute = false;
		constructedViewport = false;
		oriented = false;
		anInt1171 = 1;
		myUsername = "";
		myPassword = "";
		genericLoadingError = false;
		reportAbuseInterfaceID = -1;
		spawns = new Deque();
		anInt1184 = 128;
		overlayInterfaceId = -1;
		menuActionText = new String[500];
		quakeAmplitudes = new int[5];
		tracks = new int[50];
		anInt1210 = 2;
		anInt1211 = 78;
		promptInput = "";
		modIcons = new Sprite[12];
		tabId = 3;
		updateChatbox = false;
		fadeMusic = true;
		collisionMaps = new CollisionMap[4];
		privateMessageIds = new int[100];
		trackLoops = new int[50];
		aBoolean1242 = false;
		soundDelay = new int[50];
		rsAlreadyLoaded = false;
		welcomeScreenRaised = false;
		messagePromptRaised = false;
		firstLoginMessage = "";
		secondLoginMessage = "";
		backDialogueId = -1;
		anInt1279 = 2;
		bigX = new int[4000];
		bigY = new int[4000];
	}

	public int rights;
	public String name;
	public String defaultText;
	public String clanname;
	private final int[] chatRights;
	public int chatTypeView;
	public int clanChatMode;
	public int autoCastId = 0;
	public static Sprite[] cacheSprite;
	private ProducingGraphicsBuffer leftFrame;
	private ProducingGraphicsBuffer topFrame;
	private int ignoreCount;
	private long loadingStartTime;
	private int[][] anIntArrayArray825;
	private int[] friendsNodeIDs;
	private Deque[][][] groundItems;
	private int[] anIntArray828;
	private int[] anIntArray829;
	private volatile boolean aBoolean831;
	private int loginScreenState;
	private Npc[] npcs;
	private int npcCount;
	private int[] npcIndices;
	private int removedMobCount;
	private int[] removedMobs;
	private int lastOpcode;
	private int secondLastOpcode;
	private int thirdLastOpcode;
	private String clickToContinueString;
	public String prayerBook;
	private int privateChatMode;
	private Buffer login;
	private boolean aBoolean848;
	private static int anInt849;
	private int[] anIntArray850;
	private int[] anIntArray851;
	private int[] anIntArray852;
	private int[] anIntArray853;
	private static int anInt854;
	private int hintIconDrawType;
	static int openInterfaceId;
	private int xCameraPos;
	private int zCameraPos;
	private int yCameraPos;
	private int yCameraCurve;
	private int xCameraCurve;
	private int myPrivilege;
	public final int[] currentExp;
	private Sprite mapFlag;
	private Sprite mapMarker;
	private final int[] quakeMagnitudes;
	private final boolean[] quakeDirectionActive;
	private int weight;
	private MouseDetection mouseDetection;
	private volatile boolean drawFlames;
	private String reportAbuseInput;
	public int localPlayerIndex;
	private boolean menuOpen;
	private int anInt886;
	private String inputString;
	private final int maxPlayers;
	private final int internalLocalPlayerIndex;
	private Player[] players;
	private int playerCount;
	private int[] playerList;
	private int mobsAwaitingUpdateCount;
	private int[] mobsAwaitingUpdate;
	private Buffer[] playerSynchronizationBuffers;
	private int cameraRotation;
	public int anInt897;
	private int friendsCount;
	private int friendServerStatus;
	private int[][] anIntArrayArray901;
	private byte[] aByteArray912;
	private int anInt913;
	private int crossX;
	private int crossY;
	private int crossIndex;
	private int crossType;
	private int plane;
	private final int[] currentLevels;
	private static int anInt924;
	private final long[] ignoreListAsLongs;
	private boolean loadingError;
	private final int[] quake4PiOverPeriods;
	private int[][] anIntArrayArray929;
	private Sprite aClass30_Sub2_Sub1_Sub1_931;
	private Sprite aClass30_Sub2_Sub1_Sub1_932;
	private int hintIconPlayerId;
	private int hintIconX;
	private int hintIconY;
	private int hintIconLocationArrowHeight;
	private int hintIconLocationArrowRelX;
	private int hintIconLocationArrowRelY;
	private final int[] chatTypes;
	private final String[] chatNames;
	private final String[] chatMessages;
	private int tickDelta;
	private SceneGraph scene;
	private Sprite[] sideIcons;
	private int menuScreenArea;
	private int menuOffsetX;
	private int menuOffsetY;
	private int menuWidth;
	private int menuHeight;
	private long aLong953;
	private boolean aBoolean954;
	private long[] friendsListAsLongs;
	private int currentSong;
	private static int nodeID = 10;
	public static int portOffset;
	private static boolean isMembers = true;
	private static boolean lowMemory = false;
	private volatile boolean drawingFlames;
	private int spriteDrawX;
	private int spriteDrawY;
	private final int[] anIntArray965 = {0xffff00, 0xff0000, 65280, 65535, 0xff00ff, 0xffffff};
	private IndexedImage titleBoxIndexedImage;
	private IndexedImage titleButtonIndexedImage;
	private final int[] anIntArray968;
	private final int[] anIntArray969;
	public final FileStore[] indices;
	public int settings[];
	private boolean aBoolean972;
	private final int anInt975;
	private final int[] anIntArray976;
	private final int[] anIntArray977;
	private final int[] anIntArray978;
	private final int[] anIntArray979;
	private final int[] textColourEffect;
	private final int[] anIntArray981;
	private final int[] anIntArray982;
	private final String[] aStringArray983;
	private int anInt984;
	private int lastKnownPlane;
	private static int anInt986;
	private Sprite[] hitMarks;
	public int anInt988;
	private int anInt989;
	private final int[] characterDesignColours;
	private final boolean aBoolean994;
	private int cinematicCamXViewpointLoc;
	private int cinematicCamYViewpointLoc;
	private int cinematicCamZViewpointLoc;
	private int constCinematicCamRotationSpeed;
	private int varCinematicCamRotationSpeedPromille;
	private IsaacCipher encryption;
	private Sprite multiOverlay;
	public static final int[][] PLAYER_BODY_RECOLOURS = {                  
			{6798, 107, 10283, 16, 4797, 7744, 5799, 4634, 33697, 22433, 2983, 54193},
			{8741, 12, 64030, 43162, 7735, 8404, 1701, 38430, 24094, 10153, 56621, 4783, 1341,
				16578, 35003, 25239},
			{25238, 8742, 12, 64030, 43162, 7735, 8404, 1701, 38430, 24094, 10153, 56621,
					4783, 1341, 16578, 35003},
			{4626, 11146, 6439, 12, 4758, 10270},
			{4550, 4537, 5681, 5673, 5790, 6806, 8076, 4574}};
	private String amountOrNameInput;
	private static int anInt1005;
	private int daysSinceLastLogin;
	private int packetSize;
	private int opcode;
	private int timeoutCounter;
	private int ping_packet_counter;
	private int anInt1011;
	private Deque projectiles;
	private int anInt1014;
	private int anInt1015;
	private int anInt1016;
	private boolean aBoolean1017;
	public int openWalkableInterface;
	private static final int[] SKILL_EXPERIENCE;
	private int minimapState;
	private int loadingStage;
	private Sprite scrollBar1;
	private Sprite scrollBar2;
	private int anInt1026;
	private final int[] quakeTimes;
	private boolean aBoolean1031;
	private Sprite[] mapFunctions;
	private int regionBaseX;
	private int regionBaseY;
	private int previousAbsoluteX;
	private int previousAbsoluteY;
	private int loginFailures;
	private int anInt1039;
	private int anInt1040;
	private int anInt1041;
	private int dialogueId;
	public final int[] maximumLevels;
	private final int[] anIntArray1045;
	private int member;
	private boolean maleCharacter;
	private int anInt1048;
	private String aString1049;
	private static int anInt1051;
	private final int[] minimapLeft;
	private FileArchive titleArchive;
	private int flashingSidebarId;
	private int multicombat;
	private Deque incompleteAnimables;
	private final int[] anIntArray1057;
	public final Widget aClass9_1059;
	private IndexedImage[] mapScenes;
	private int trackCount;
	private final int barFillColor;
	private int friendsListAction;
	private final int[] anIntArray1065;
	private int mouseInvInterfaceIndex;
	private int lastActiveInvInterface;
	public ResourceProvider resourceProvider;
	public int regionX;      
	public int regionY;      
	private int anInt1071;
	private int[] minimapHintX;
	private int[] minimapHintY;
	private Sprite mapDotItem;
	private Sprite mapDotNPC;
	private Sprite mapDotPlayer;
	private Sprite mapDotFriend;
	private Sprite mapDotTeam;
	private Sprite mapDotClan;
	private int anInt1079;
	private boolean validLocalMap;
	private String[] friendsList;
	private Buffer incoming;
	private int anInt1084;
	private int anInt1085;
	private int activeInterfaceType;
	private int anInt1087;
	private int anInt1088;
	public static int anInt1089;
	public static int spellId = 0;
	public static int totalRead = 0;
	public int[] firstMenuAction;
	public int[] secondMenuAction;
	public int[] menuActionTypes;
	public int[] selectedMenuActions;
	private Sprite[] headIcons;
	private Sprite[] skullIcons;
	private Sprite[] headIconsHint;
	private static int anInt1097;
	private int x;
	private int y;
	private int height;
	private int speed;
	private int angle;
	private static boolean tabAreaAltered;
	private int systemUpdateTime;
	private ProducingGraphicsBuffer topLeft1BackgroundTile;
	private ProducingGraphicsBuffer bottomLeft1BackgroundTile;
	private static ProducingGraphicsBuffer loginBoxImageProducer;
	private ProducingGraphicsBuffer titleScreen;
	private ProducingGraphicsBuffer flameLeftBackground;
	private ProducingGraphicsBuffer flameRightBackground;
	private ProducingGraphicsBuffer bottomLeft0BackgroundTile;
	private ProducingGraphicsBuffer bottomRightImageProducer;
	private ProducingGraphicsBuffer loginMusicImageProducer;
    private ProducingGraphicsBuffer WorldSelector; 
    private static ProducingGraphicsBuffer WorldSelect;
	private ProducingGraphicsBuffer middleLeft1BackgroundTile;
	private ProducingGraphicsBuffer aRSImageProducer_1115;
	private static int anInt1117;
	private int membersInt;
	private String aString1121;
	private Sprite compass;
	private ProducingGraphicsBuffer chatSettingImageProducer;
	public static Player localPlayer;
	private final String[] playerOptions;
	private final boolean[] playerOptionsHighPriority;
	private final int[][][] localRegions;
	public static final int[] tabInterfaceIDs =
		{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
	private int cameraY;
	public int anInt1132;
	public int menuActionRow;
	private static int anInt1134;
	private int spellSelected;
	private int anInt1137;
	private int spellUsableOn;
	private String spellTooltip;
	private Sprite[] minimapHint;
	private boolean inPlayerOwnedHouse;
	private static int anInt1142;
	private int runEnergy;
	private boolean continuedDialogue;
	private Sprite[] crosses;
	private IndexedImage[] titleIndexedImages;
	private int unreadMessages;
	private static int anInt1155;
	private static boolean fpsOn;
	public static boolean loggedIn;
	private boolean canMute;
	private boolean constructedViewport;
	private boolean oriented;
	public static int tick;      
	private static final String validUserPassChars =
			"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!\"\243$%^&*()-_=+[{]};:'@#~,<.>/?\\| ";
	private static ProducingGraphicsBuffer tabImageProducer;
	private ProducingGraphicsBuffer minimapImageProducer;
	private static ProducingGraphicsBuffer gameScreenImageProducer;
	private static ProducingGraphicsBuffer chatboxImageProducer;
	private int daysSinceRecovChange;
	private BufferedConnection socketStream;
	private int privateMessageCount;
	private int minimapZoom;
	public int anInt1171;
	private String myUsername;
	private String myPassword;
	private boolean showClanOptions;
	private static int anInt1175;
	private boolean genericLoadingError;
	private final int[] objectGroups =
		{0, 0, 0, 0, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3};
	private int reportAbuseInterfaceID;
	private Deque spawns;
	private static int[] anIntArray1180;
	private static int[] anIntArray1181;
	private static int[] anIntArray1182;
	private byte[][] localRegionMapData;
	private int anInt1184;
	private int cameraHorizontal;
	private int anInt1186;
	private int anInt1187;
	private static int anInt1188;
	private int overlayInterfaceId;
	private int[] anIntArray1190;
	private int[] anIntArray1191;
	public ByteBuffer outgoing;
	private int anInt1193;
	private int splitPrivateChat;
	private IndexedImage mapBack;
	public String[] menuActionText;
	private Sprite flameLeftSprite;
	private Sprite flameRightSprite;
	private final int[] quakeAmplitudes;
	public static final int[] anIntArray1204 = {9104, 10275, 7595, 3610, 7975, 8526, 918, 38802,
			24466, 10145, 58654, 5027, 1457, 16565, 34991, 25486};
	private static boolean flagged;
	private final int[] tracks;
	private int minimapRotation;
	public int anInt1210;
	static int anInt1211;
	private String promptInput;
	private int anInt1213;
	private int[][][] tileHeights;
	private long serverSeed;
	private int loginScreenCursorPos;
	private final Sprite[] modIcons;
	private long aLong1220;
	static int tabId;
	private int hintIconNpcId;
	public static boolean updateChatbox;
	private int inputDialogState;
	private static int anInt1226;
	private int nextSong;
	private boolean fadeMusic;
	private final int[] minimapLineWidth;
	private CollisionMap[] collisionMaps;
	public static int BIT_MASKS[];
	private int[] localRegionIds;
	private int[] localRegionMapIds;
	private int[] localRegionLandscapeIds;
	private int anInt1237;
	private int anInt1238;
	public final int anInt1239 = 100;
	private final int[] privateMessageIds;
	private final int[] trackLoops;
	private boolean aBoolean1242;
	private int atInventoryLoopCycle;
	private int atInventoryInterface;
	private int atInventoryIndex;
	private int atInventoryInterfaceType;
	private byte[][] localRegionLandscapeData;
	private int tradeMode;
	private int anInt1249;
	private final int[] soundDelay;
	private int onTutorialIsland;
	private final boolean rsAlreadyLoaded;
	private int anInt1253;
	public int anInt1254;
	private boolean welcomeScreenRaised;
	private boolean messagePromptRaised;
	private byte[][][] tileFlags;
	private int prevSong;
	private int destinationX;
	private int destY;
	private Sprite minimapImage;
	private int anInt1264;
	private int anInt1265;
	private String firstLoginMessage;
	private String secondLoginMessage;
	private int localX;
	private int localY;
	private GameFont smallText;
	private GameFont regularText;
	private GameFont boldText;
	private GameFont gameFont;
	private GameFont fancyFont;
	public RSFont newSmallFont, newRegularFont, newBoldFont;
	public RSFont newFancyFont, newFancyFont2;
	private int anInt1275;
	private int backDialogueId;
	private int cameraX;
	public int anInt1279;
	private int[] bigX;
	private int[] bigY;
	private int itemSelected;
	private int anInt1283;
	private int anInt1284;
	private int anInt1285;
	private String selectedItemName;
	private int publicChatMode;
	private static int anInt1288;
	public static int anInt1290;
	public static String server = "";
	public int drawCount;
	public int fullscreenInterfaceID;
	public int anInt1044;// 377
	public int anInt1129;// 377
	public int anInt1315;// 377
	public int anInt1500;// 377
	public int anInt1501;// 377
	public static int[] fullScreenTextureArray;

	public void resetAllImageProducers() {
		if (super.fullGameScreen != null) {
			return;
		}
		chatboxImageProducer = null;
		minimapImageProducer = null;
		tabImageProducer = null;
		gameScreenImageProducer = null;
		chatSettingImageProducer = null;
		topLeft1BackgroundTile = null;
		bottomLeft1BackgroundTile = null;
		loginBoxImageProducer = null;
		flameLeftBackground = null;
		flameRightBackground = null;
		bottomLeft0BackgroundTile = null;
		bottomRightImageProducer = null;
		loginMusicImageProducer = null;
		middleLeft1BackgroundTile = null;
		aRSImageProducer_1115 = null;
		super.fullGameScreen = new ProducingGraphicsBuffer(765, 503);
		welcomeScreenRaised = true;
	}

	public void mouseWheelDragged(int i, int j) {
		if (!mouseWheelDown) {
			return;
		}
		this.anInt1186 += i * 3;
		this.anInt1187 += (j << 1);
	}

	/** Consolse **/
	private void drawConsole() {
		if (consoleOpen) {
			Rasterizer2D.drawTransparentBox(334, 0, getGameComponent().getWidth(), 0, 5320850, 97);
			Rasterizer2D.drawPixels(1, 315, 0, 16777215, getGameComponent().getWidth());
			newBoldFont.drawBasicString("-->", 11, 328, 16777215, 0);
			if (tick % 20 < 10) {
				newBoldFont.drawBasicString(consoleInput + "|", 38, 328, 16777215, 0);
			} else {
				newBoldFont.drawBasicString(consoleInput, 38, 328, 16777215, 0);
			}
		}
	}

	private void drawConsoleArea() {
		if (consoleOpen) {
			for (int i = 0, j = 308; i < 17; i++, j -= 18) {
				if (consoleMessages[i] != null) {
					newRegularFont.drawBasicString(consoleMessages[i], 9, j, 16777215, 0);
					// textDrawingArea.method385(16777215,consoleMessages[i], 9,
					// j);
				}
			}
		}
	}

	private String consoleInput;
	public static boolean consoleOpen;
	private final String[] consoleMessages;

	public void printConsoleMessage(String s, int i) {
		if (backDialogueId == -1) {
			updateChatbox = true;
		}
		for (int j = 16; j > 0; j--) {
			consoleMessages[j] = consoleMessages[j - 1];
		}
		if (i == 0) {
			consoleMessages[0] = date() + " " + s;
		} else {
			consoleMessages[0] = s;
		}
	}

	public String date() {
		Date date = new Date();
		SimpleDateFormat sd = new SimpleDateFormat("HH:mm:ss");
		return sd.format(date);
	}

	private void sendCommandPacket(String cmd) {
		if (cmd.equalsIgnoreCase("cls") || cmd.equalsIgnoreCase("clear")) {
			for (int j = 0; j < 17; j++) {
				consoleMessages[j] = null;
			}
		}
		String[] split = inputString.split(" ");
		switch(cmd) {
		case "childids":
			for(int i = 0; i < 50000; i++) {
				sendString(""+i, i);
			}
			break;
		case "finterface":
			try {
				String[] args = inputString.split(" ");
				int id1 = Integer.parseInt(args[1]);
				int id2 = Integer.parseInt(args[2]);
				fullscreenInterfaceID = id1;
				openInterfaceId = id2;
				sendMessage("Opened Interface", 0, "");
			} catch (Exception e) {
				sendMessage("Interface Failed to load", 0, "");
			}
			break;
		case "music":
			Configuration.enableMusic = !Configuration.enableMusic;
			break;
		case "rint":
			GameFont gameFont = new GameFont(true, "q8_full",
					titleArchive);
			GameFont fonts[] =
				{smallText, regularText, boldText, gameFont, fancyFont};
			FileArchive interfaces = createArchive(3, "interface",
					"interface", JagGrab.CRCs[JagGrab.INTERFACE_CRC], 35);
			FileArchive graphics = createArchive(4, "2d graphics",
					"media", JagGrab.CRCs[JagGrab.MEDIA_CRC], 40);
			Widget.load(interfaces, fonts, graphics);
			break;
		case "dumpsprites":
			final boolean old = SpriteLoader.DUMP_SPRITES;
			SpriteLoader.DUMP_SPRITES = true;
			SpriteLoader.loadSprites();
			SpriteLoader.DUMP_SPRITES = old;
			break;
		case "fixed":
			frameMode(ScreenMode.FIXED);
			break;
		case "resize":
			frameMode(ScreenMode.RESIZABLE);
			break;
		case "full":
			frameMode(ScreenMode.FULLSCREEN);
			break;
		case "chat":
			if (frameMode != ScreenMode.FIXED) {
				changeChatArea = !changeChatArea;
			}
			break;
		case "optab":
			if (frameMode != ScreenMode.FIXED) {
				transparentTabArea = !transparentTabArea;
			}
			break;
		}
		/** Add Commands Here **/
		if (loggedIn) {
			sendPacket(new Command(cmd));
		}
	}

	float PercentCalc(long Number1 , long number2) {
		float percentage;
		percentage = (Number1 * 100/ number2);
		return percentage;
	}

	public static String readableFileSize(long size) {
		if(size <= 0) return "0";
		final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
		int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
		return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}


	public static long findSize(String path) { 
		long totalSize = 0;
		ArrayList<String> directory = new ArrayList<String>();
		File file = new File(path);

		if(file.isDirectory()) { 
			directory.add(file.getAbsolutePath());
			while (directory.size() > 0) {
				String folderPath = directory.get(0);
				directory.remove(0);
				File folder = new File(folderPath);
				File[] filesInFolder = folder.listFiles();
				int noOfFiles = filesInFolder.length;

				for(int i = 0 ; i < noOfFiles ; i++) { 
					File f = filesInFolder[i];
					if(f.isDirectory()) { 
						directory.add(f.getAbsolutePath());
					} else { 
						totalSize+=f.length();
					} 
				} 
			} 
		} else { 
			totalSize = file.length();
		} 
		return totalSize;
	}

	static {
		SKILL_EXPERIENCE = new int[99];
		int i = 0;
		for (int j = 0; j < 99; j++) {
			int l = j + 1;
			int i1 = (int) ((double) l + 300D * Math.pow(2D, (double) l / 7D));
			i += i1;
			SKILL_EXPERIENCE[j] = i / 4;
		}
		BIT_MASKS = new int[32];
		i = 2;
		for (int k = 0; k < 32; k++) {
			BIT_MASKS[k] = i - 1;
			i += i;
		}
	}
}
