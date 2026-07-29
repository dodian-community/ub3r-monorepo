package com.osroyale.profile;

import com.osroyale.Model;
import com.osroyale.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.*;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.runelite.client.RuneLite.PROFILES_DIR;
import static net.runelite.client.RuneLite.RUNELITE_DIR;

public class ProfileManager {

    private static final Logger logger = LoggerFactory.getLogger(ProfileManager.class);

    public static final int MAX_PROFILES = 10;
    public static final List<Profile> profiles = Collections.synchronizedList(new ArrayList<>(MAX_PROFILES));

    static {
        for (int i = 0; i < MAX_PROFILES; i++) {
            profiles.add(new Profile());
        }
    }

    public static void add(Profile newProfile) {
        int index = 0;
        Profile existingProfile = null;
        for (int i = 0; i < MAX_PROFILES; i++) {
            Profile profile = profiles.get(i);
            if (newProfile.getUsername().equalsIgnoreCase(profile.getUsername())) {
                existingProfile = profile;
                index = i;
                break;
            }

            // replace first empty slot.
            if (profile.emptySlot()) {
                profiles.remove(i);
                profiles.add(i, newProfile);
                break;
            }
        }

        if (existingProfile == null) {
            if (profiles.size() < MAX_PROFILES) {
                profiles.add(newProfile);
            }
        } else {
            profiles.remove(index);
            profiles.add(index, newProfile);
        }
        save();
    }

    /** Saves a full appearance preview from the already assembled live-player model. */
    public static void saveBodyPreview(Profile profile, Model model) {
        if (profile == null || profile.getUsername().isEmpty() || model == null) {
            return;
        }
        saveImage(
                createImageFromPixels(
                        profile.convertBodyModelToSprite(model).getPixels(),
                        ProfilePreviewRenderer.PREVIEW_WIDTH,
                        ProfilePreviewRenderer.PREVIEW_HEIGHT
                ),
                PROFILES_DIR + "/" + profile.getUsername().toLowerCase() + "_body.png"
        );
    }

    public static void delete(Profile profile) {
        profiles.remove(profile);
        // add back empty slot
        profiles.add(new Profile());
        save();
    }

    public static void save() {
        File file = new File(Utility.findcachedir() + File.separator + "profiles.dat");
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            int validCount = 0;
            for (Profile p : profiles) {
                if (p != null && !p.emptySlot()) {
                    validCount++;
                }
            }
            output.write(validCount);
            for (Profile profile : profiles) {
                if (profile == null || profile.emptySlot()) {
                    continue;
                }
                output.writeUTF(profile.getUsername());
                output.writeUTF(profile.getPassword());

                output.writeByte(profile.getGender());
                output.writeByte(profile.getEquipment().length);
                for (int i = 0; i < profile.getEquipment().length; i++) {
                    output.writeShort(profile.getEquipment()[i]);
                }
                output.writeByte(profile.getRecolours().length);
                for (int i = 0; i < profile.getRecolours().length; i++) {
                    output.writeShort(profile.getRecolours()[i]);
                }
                output.writeLong(profile.getLastLogin());
                output.writeByte(profile.getSkillLevels().length);
                for (int i = 0; i < profile.getSkillLevels().length; i++) {
                    output.writeShort(profile.getSkillLevels()[i]);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load() throws IOException {
        File file = new File(Utility.findcachedir() + File.separator + "profiles.dat");
        if (!file.exists()) {
            if (!file.createNewFile()) {
                System.err.println("could not create profiles.dat in " + file.getAbsolutePath());
                return;
            }
        }
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            if (input.available() <= 0) {
                return;
            }
            int totalProfiles = input.read();
            for (int i = 0; i < totalProfiles && i < MAX_PROFILES; i++) {
                String username = input.readUTF();
                String password = input.readUTF();

                int gender = input.readByte();
                int[] equipment = new int[input.readByte()];
                for (int j = 0; j < equipment.length; j++) {
                    equipment[j] = input.readShort();
                }
                int[] recolours = new int[input.readByte()];
                for (int j = 0; j < recolours.length; j++) {
                    recolours[j] = input.readShort();
                }

                long lastLogin = 0L;
                int[] skillLevels = null;
                try {
                    lastLogin = input.readLong();
                    int skillLen = input.readByte();
                    skillLevels = new int[skillLen];
                    for (int j = 0; j < skillLen; j++) {
                        skillLevels[j] = input.readShort();
                    }
                } catch (EOFException e) {
                    // Backwards compatibility fallback
                }

                Profile loadedProfile;
                if (skillLevels != null) {
                    loadedProfile = new Profile(username, password, gender, equipment, recolours, skillLevels, lastLogin);
                } else {
                    loadedProfile = new Profile(username, password, gender, equipment, recolours);
                }

                if (i < profiles.size()) {
                    profiles.set(i, loadedProfile);
                } else {
                    profiles.add(loadedProfile);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Creates a BufferedImage from an array of pixels.
     *
     * @param pixels The array of pixels representing the image.
     * @param width  The width of the image.
     * @param height The height of the image.
     * @return The created BufferedImage.
     */
    public static BufferedImage createImageFromPixels(int[] pixels, int width, int height) {
        if (pixels == null || pixels.length < width * height) {
            throw new IllegalArgumentException("Profile raster does not contain " + (width * height) + " pixels");
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Client raster pixels are RGB values (0x00RRGGBB). BufferedImage.TYPE_INT_ARGB
        // interprets the high byte as alpha, so copying them verbatim makes every rendered
        // model pixel transparent. Preserve a transparent zero background and make rendered
        // RGB pixels opaque for profile previews.
        int[] dataBuffer = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        for (int index = 0; index < width * height; index++) {
            int rgb = pixels[index] & 0x00FFFFFF;
            dataBuffer[index] = rgb == 0 ? 0 : 0xFF000000 | rgb;
        }

        return image;
    }

    /**
     * Saves a BufferedImage to the specified file path as a PNG image.
     *
     * @param image    The BufferedImage to save.
     * @param filePath The file path where the image will be saved.
     */
    public static void saveImage(BufferedImage image, String filePath) {
        Path destination = Path.of(filePath).toAbsolutePath().normalize();
        Path temporary = null;
        try {
            Path parent = destination.getParent();
            if (parent == null) {
                throw new IOException("Profile image has no parent directory: " + destination);
            }
            Files.createDirectories(parent);
            temporary = Files.createTempFile(parent, destination.getFileName() + ".", ".tmp");
            if (!ImageIO.write(image, "png", temporary.toFile())) {
                throw new IOException("No PNG image writer is available");
            }
            try {
                Files.move(temporary, destination,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            logger.warn("Unable to save profile image {}: {}", destination, e.getMessage());
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException cleanupFailure) {
                    logger.debug("Unable to remove temporary profile image {}", temporary, cleanupFailure);
                }
            }
        }
    }
}
