package com.runescape.cache.def;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class Definitions {
	

private static boolean isNoted = false;

public static void dumpDefinitions(int totalItems) {

	String itemName = null;
	String examine = null;
	
	for (int index = 13189; index < totalItems; index++) {
		ItemDefinition defs = ItemDefinition.lookup(index);
		if (defs == null) {
			continue;
		}
		if (defs.name != null) {
			itemName = defs.name.replace(" ", "_");
		}
		if (itemName == null) {
			continue;
		}
		if (defs.name == null) {
			continue;
		}
		if (defs.description != null) {
			examine = defs.description;
		}
		if (defs.description == null) {
			examine = "";
		}
		if (itemName == null || itemName.length() <= 0) {
			continue;
		}
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(
					"definitions.txt", true));
		writer.write("	{");
		writer.newLine();
		writer.write("		~id~:");
		writer.write("" + index);
		writer.write(",");
		writer.newLine();
		writer.write("		~name~:");
		writer.write("	~"+itemName.replace("_", " ")+"~");
		writer.write(",");
		
		writer.newLine();
		writer.write("		~examine~:");
		writer.write("	~"+defs.description+"~");
		writer.write(",");

		writer.newLine();
		writer.write("		~value~:");
		writer.write("	" + defs.value);
		writer.write(",");
		writer.newLine();
		writer.write("		~stackable~:");
		writer.write("	" + defs.stackable);
		writer.write(",");
		writer.newLine();
	
			writer.write("		~noted~:");
			writer.write("	false");
			writer.write(",");
			writer.newLine();
		writer.write("		~noteId~:");
		writer.write("	-1");
		writer.write(",");
		if(defs.name.contains("claws")|| defs.name.contains("bow") && !defs.name.contains("cross") ||defs.name.contains("bludgeon")|| defs.name.contains("bulwark")) {
			writer.write("		~doubleHanded~:");
			writer.write("	true");
			writer.write(",");
			writer.newLine();
	 }
		examine = null;
		itemName = null;
		writer.newLine();
		if(!isNoted) {
			if(defs.name.contains("platebody") || defs.name.contains("Platebody")
					|| defs.name.contains("shirt")|| defs.name.contains("top")|| defs.name.contains("body")
					|| defs.name.contains("blouse")|| defs.name.contains("hauberk")|| defs.name.contains("robe") &&defs.name.contains("top")) {
			writer.write("		~equipmentType~:");
			writer.write("	~PLATEBODY~");
			writer.write(",");
			writer.newLine();
			} else if(defs.name.contains("Hooded")) {
			writer.write("		~equipmentType~:");
			writer.write("	~HOODED_CAPE~");
			writer.write(",");
			writer.newLine();
			} else if(defs.name.contains("cloak")&& !defs.name.contains("Hooded")|| defs.name.contains("cape") && !defs.name.contains("Hooded")
					 || defs.name.contains("Cape")|| defs.name.contains("Cloak")|| defs.name.contains("Ava's")) {
				writer.write("		~equipmentType~:");
				writer.write("	~CAPE~");
				writer.write(",");
				writer.newLine();
			} else if(defs.name.contains("shield")|| defs.name.contains("kite")
					 || defs.name.contains("defender")|| defs.name.contains("ward")
					 || defs.name.contains("buckler")){
				writer.write("		~equipmentType~:");
				writer.write("	~SHIELD~");
				writer.write(",");
				writer.newLine();
			} else if(defs.name.contains("gloves")|| defs.name.contains("bracelet")|| defs.name.contains("Bracelet")
					 || defs.name.contains("bracers")) {
				writer.write("		~equipmentType~:");
				writer.write("	~GLOVES~");
				writer.write(",");
				writer.newLine();
			} else if(defs.name.contains("boots")|| defs.name.contains("shoes") || defs.name.contains("scarf")) {
				writer.write("		~equipmentType~:");
				writer.write("	~BOOTS~");
				writer.write(",");
				writer.newLine();
			} else if(defs.name.contains("necklace")|| defs.name.contains("amulet")|| defs.name.contains("Necklace")
					 || defs.name.contains("Amulet")) {
				writer.write("		~equipmentType~:");
				writer.write("	~AMULET~");
				writer.write(",");
				writer.newLine();
			} else if(defs.name.contains("ring")|| defs.name.contains("Ring")) {
				writer.write("		~equipmentType~:");
				writer.write("	~RING~");
				writer.write(",");
				writer.newLine();
			} else if(defs.name.contains("arrows")|| defs.name.contains("arrow")|| defs.name.contains("bolts")
					|| defs.name.contains("bolt")) {
				writer.write("		~equipmentType~:");
				writer.write("	~ARROWS~");
				writer.write(",");
				writer.newLine();
			} else if(defs.name.contains("coif")|| defs.name.contains("mask")|| defs.name.contains("bandana")
					|| defs.name.contains("hood")|| defs.name.contains("head")) {
				writer.write("		~equipmentType~:");
				writer.write("	~COIF~");
				writer.write(",");
				writer.newLine();
			} else if(defs.name.contains("Helm")|| defs.name.contains("helm")|| defs.name.contains("helmet")
					|| defs.name.contains("Helmet")) {
				writer.write("		~equipmentType~:");
				writer.write("	~FULL_HELMET~");
				writer.write(",");
				writer.newLine();
			} else if(defs.name.contains("hat")|| defs.name.contains("Hat")) {
				writer.write("		~equipmentType~:");
				writer.write("	~HAT~");
				writer.write(",");
				writer.newLine();
			} else if(defs.name.contains("top")|| defs.name.contains("tunic")|| defs.name.contains("armour")) {
				writer.write("		~equipmentType~:");
				writer.write("	~BODY~");
				writer.write(",");
				writer.newLine();
			} else if(defs.name.contains("platelegs")|| defs.name.contains("plateskirt")|| defs.name.contains("chaps")
					|| defs.name.contains("legs")|| defs.name.contains("leggings")|| defs.name.contains("plateskirt")|| defs.name.contains("plateskirt")
					|| defs.name.contains("robe") && !defs.name.contains("top")) {
				writer.write("		~equipmentType~:");
				writer.write("	~LEGS~");
				writer.write(",");
				writer.newLine();
			} else if(defs.name.contains("sword")|| defs.name.contains("bow")| defs.name.contains("Staff")| defs.name.contains("Wand")|| defs.name.contains("scimitar")
					|| defs.name.contains("staff")|| defs.name.contains("maul")|| defs.name.contains("wand")
					|| defs.name.contains("harpoon")|| defs.name.contains("bulwark")|| defs.name.contains("banner")|| defs.name.contains("bludgeon")|| defs.name.contains("pickaxe")|| defs.name.contains("claws")|| defs.name.contains("axe")
					|| defs.name.contains("warhammer")) {
				writer.write("		~equipmentType~:");
				writer.write("	~WEAPON~");
				writer.write(",");
				writer.newLine();
		}
			
		isNoted = false;
		 if(defs.name.endsWith("bow") && !defs.name.contains("cross") || defs.name.endsWith("Bow") && !defs.name.contains("cross")) {
				writer.write("		~weaponInterface~:");
				writer.write("	~LONGBOW~");
				writer.write(",");
				writer.newLine();
		 }
		 if(defs.name.endsWith("crossbow")) {
				writer.write("		~weaponInterface~:");
				writer.write("	~CROSSBOW~");
				writer.write(",");
				writer.newLine();
		 }
		 if(defs.name.contains("maul") ||  defs.name.contains("bludgeon")) {
				writer.write("		~weaponInterface~:");
				writer.write("	~MAUL~");
				writer.write(",");
				writer.newLine();
		 }
		 if(defs.name.contains("wand") ||  defs.name.contains("staff")||  defs.name.contains("Staff")) {
				writer.write("		~weaponInterface~:");
				writer.write("	~STAFF~");
				writer.write(",");
				writer.newLine();
		 }
		 if(defs.name.contains("claws")) {
				writer.write("		~weaponInterface~:");
				writer.write("	~CLAWS~");
				writer.write(",");
				writer.newLine();
		 }
		 if(defs.name.contains("pickaxe")) {
				writer.write("		~weaponInterface~:");
				writer.write("	~PICKAXE~");
				writer.write(",");
				writer.newLine();
		 }
		 if(defs.name.contains("axe") && !defs.name.contains("pick")) {
				writer.write("		~weaponInterface~:");
				writer.write("	~BATTLEAXE~");
				writer.write(",");
				writer.newLine();
		 }
		 if(defs.name.contains("harpoon")) {
				writer.write("		~weaponInterface~:");
				writer.write("	~SWORD~");
				writer.write(",");
				writer.newLine();
		 }
		 if(defs.name.contains("warhammer")) {
				writer.write("		~weaponInterface~:");
				writer.write("	~WARHAMMER~");
				writer.write(",");
				writer.newLine();
		 }
		writer.write("		~tradeable~:");
		writer.write("	false");
		writer.write(",");
		writer.newLine();
		writer.write("		~sellable~:");
		writer.write("	true");
		writer.write(",");
		writer.newLine();
		writer.write("		~dropable~:");
		writer.write("	true");
		writer.write(",");
		writer.newLine();
		writer.write("		\"interfaceId\": 5855");
		writer.newLine();
		writer.write("	},");
		writer.newLine();
		writer.close();
		}
		}
		catch(Exception e) {
			
		}
	}
}
}

	
	/*	String itemName = null;
		String examine = null;

		for (int index = 13189; index < totalItems; index++) {
			ItemDefinition defs = ItemDefinition.lookup(index);

			if (defs == null) {
				continue;
			}

			if (defs.name != null) {
				itemName = defs.name.replace(" ", "_");
			}

			if (itemName == null) {
				continue;
			}

			if (defs.description != null) {
				examine = defs.description;
			}

			if (examine == null) {
				examine = "null";
			}

			if (itemName == null || itemName.length() <= 0) {
				continue;
			}

			//int[] bonus = new int[14];
			//int bonusIndex = 0;

			try {

				BufferedWriter writer = new BufferedWriter(new FileWriter(
						"definitions.txt", true));

				//URL url = new URL("http://2007.runescape.wikia.com/wiki/"
				//		+ itemName);

				System.out.println("starting on " + itemName);

				//URLConnection connection = url.openConnection();

			//	if (connection == null) {
				//	System.out.println("Unable to establish connection.");
				//	continue;
			//	}

				//System.out.println("Connection established");
				
			//	if (connection.getInputStream() == null) {
			//		continue;
			//	}

			//	BufferedReader reader = new BufferedReader(
			//			new InputStreamReader(connection.getInputStream()));

			//	String line = "";

			//	while ((line = reader.readLine()) != null) {
			//		if (line.contains("<td style=\"text-align: center; width: 35px;\">")
			//				|| line.contains("</td><td style=\"text-align: center; width: 35px;\">")) {
			//			line = line
		//						.replace(
			//							"<td style=\"text-align: center; width: 35px;\">",
			//							"")
		//						.replace(
			//							"</td><td style=\"text-align: center; width: 35px;\">",
			//							"").replace("</td>", "");
					//	bonus[bonusIndex] = Integer.valueOf(line);
					//	bonusIndex++;
		//			} else if (line
		//					.startsWith("<td style=\"text-align: center; width: 30px;\">")
		//					|| line.startsWith("</td><td style=\"text-align: center; width: 30px;\">")) {
		//				line = line
		//						.replace(
		//								"<td style=\"text-align: center; width: 30px;\">",
		//								"")
		//						.replace(
		//								"</td><td style=\"text-align: center; width: 30px;\">",
		//								"").replace("</td>", "")
		//						.replace("%", "");
					//	bonus[bonusIndex] = Integer.valueOf(line);
					//	bonusIndex++;
					//	System.out.println(index);
					//	System.out.println("Bonus2: " + line);
				//	}
			//	}

				writer.write("[");
				writer.newLine();
				writer.write("	{");
				writer.newLine();
				writer.write("		~id~:");
				writer.write("" + index);
				writer.write(",");
				writer.newLine();
				writer.write("		~name~:");
				writer.write("	~"+itemName.replace("_", " ")+"~");
				writer.write(",");
				
				writer.newLine();
				writer.write("		~examine~:");
				writer.write("	~"+examine+"~");
				writer.write(",");

				writer.newLine();
				writer.write("		~value~:");
				writer.write("	" + defs.value);
				writer.write(",");
				writer.newLine();
				writer.write("		~stackable~:");
				writer.write("	" + defs.stackable);
				writer.write(",");
				writer.newLine();
				if(examine.contains("Swap this note")) {
				writer.write("		~noted~:");
				writer.write("	true");
				writer.write(",");
				writer.newLine();
				isNoted = true;
				} else {
					writer.write("		~noted~:");
					writer.write("	false");
					writer.write(",");
					writer.newLine();
				}
				/*if(ItemDefinition.lookup(defs.id - 1).name.contains(defs.name)) {
					writer.write("		~noteId~:");
					writer.write("	" + ((defs.id) - 1));
					writer.write(",");
				} else
				if(ItemDefinition.lookup(defs.id + 1).name.contains(defs.name)) {
					writer.write("		~noteId~:");
					writer.write("	" + ((defs.id) + 1));
					writer.write(",");
				} else
				if(!ItemDefinition.lookup(defs.id - 1).name.contains(defs.name) && !ItemDefinition.lookup(defs.id + 1).name.contains(defs.name)) {
					writer.write("		~noteId~:");
					writer.write("	-1");
					writer.write(",");
				}
				writer.write("		~noteId~:");
				writer.write("	-1");
				writer.write(",");
				examine = null;
				itemName = null;
				writer.newLine();
				if(!isNoted) {
					int equType = 0; // armour shirt top robe body blouse hauberk
					if(defs.name.contains("platebody") || defs.name.contains("Platebody")
							|| defs.name.contains("shirt")|| defs.name.contains("top")|| defs.name.contains("body")
							|| defs.name.contains("blouse")|| defs.name.contains("hauberk")|| defs.name.contains("robe") &&defs.name.contains("top")) {
					writer.write("		~equipmentType~:");
					writer.write("	~PLATEBODY~");
					writer.write(",");
					writer.newLine();
					} else if(defs.name.contains("Hooded")) {
					writer.write("		~equipmentType~:");
					writer.write("	~HOODED_CAPE~");
					writer.write(",");
					writer.newLine();
					} else if(defs.name.contains("cloak")&& !defs.name.contains("Hooded")|| defs.name.contains("cape") && !defs.name.contains("Hooded")
							 || defs.name.contains("Cape")|| defs.name.contains("Cloak")|| defs.name.contains("Ava's")) {
						writer.write("		~equipmentType~:");
						writer.write("	~CAPE~");
						writer.write(",");
						writer.newLine();
					} else if(defs.name.contains("shield")|| defs.name.contains("kite")
							 || defs.name.contains("defender")|| defs.name.contains("ward")|| defs.name.contains("bulwark")
							 || defs.name.contains("buckler")){
						writer.write("		~equipmentType~:");
						writer.write("	~SHIELD~");
						writer.write(",");
						writer.newLine();
					} else if(defs.name.contains("gloves")|| defs.name.contains("bracelet")|| defs.name.contains("Bracelet")
							 || defs.name.contains("bracers")) {
						writer.write("		~equipmentType~:");
						writer.write("	~GLOVES~");
						writer.write(",");
						writer.newLine();
					} else if(defs.name.contains("boots")|| defs.name.contains("shoes") || defs.name.contains("scarf")) {
						writer.write("		~equipmentType~:");
						writer.write("	~BOOTS~");
						writer.write(",");
						writer.newLine();
					} else if(defs.name.contains("necklace")|| defs.name.contains("amulet")
							 || defs.name.contains("Amulet")) {
						writer.write("		~equipmentType~:");
						writer.write("	~AMULET~");
						writer.write(",");
						writer.newLine();
					} else if(defs.name.contains("ring")|| defs.name.contains("Ring")) {
						writer.write("		~equipmentType~:");
						writer.write("	~RING~");
						writer.write(",");
						writer.newLine();
					} else if(defs.name.contains("arrows")|| defs.name.contains("arrow")|| defs.name.contains("bolts")
							|| defs.name.contains("bolt")) {
						writer.write("		~equipmentType~:");
						writer.write("	~ARROWS~");
						writer.write(",");
						writer.newLine();
					} else if(defs.name.contains("coif")|| defs.name.contains("mask")|| defs.name.contains("bandana")
							|| defs.name.contains("hood")|| defs.name.contains("head")|| defs.name.contains("hat")
							|| defs.name.contains("Helm")|| defs.name.contains("helm")|| defs.name.contains("helmet")
							|| defs.name.contains("Helmet")) {
						writer.write("		~equipmentType~:");
						writer.write("	~COIF~");
						writer.write(",");
						writer.newLine();
					} else if(defs.name.contains("top")|| defs.name.contains("tunic")|| defs.name.contains("armour")) {
						writer.write("		~equipmentType~:");
						writer.write("	~BODY~");
						writer.write(",");
						writer.newLine();
					} else if(defs.name.contains("platelegs")|| defs.name.contains("plateskirt")|| defs.name.contains("chaps")
							|| defs.name.contains("legs")|| defs.name.contains("plateskirt")|| defs.name.contains("plateskirt")
							|| defs.name.contains("robe") && !defs.name.contains("top")) {
						writer.write("		~equipmentType~:");
						writer.write("	~LEGS~");
						writer.write(",");
						writer.newLine();
					} else if(defs.name.contains("sword")|| defs.name.contains("bow")|| defs.name.contains("scimitar")
							|| defs.name.contains("staff")|| defs.name.contains("maul")|| defs.name.contains("wand")
							|| defs.name.contains("harpoon")|| defs.name.contains("banner")) {
						writer.write("		~equipmentType~:");
						writer.write("	~WEAPON~");
						writer.write(",");
						writer.newLine();
				}
				isNoted = false;

				writer.write("		~tradeable~:");
				writer.write("	false");
				writer.write(",");
				writer.newLine();
				writer.write("		~sellable~:");
				writer.write("	true");
				writer.write(",");
				writer.newLine();
				writer.write("		~dropable~:");
				writer.write("	true");
				writer.write(",");
				writer.newLine();
				writer.write("	},");
				writer.newLine();
				writer.close();
				}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}*/

