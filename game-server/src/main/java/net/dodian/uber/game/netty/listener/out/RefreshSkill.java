package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;

/**
 * Updates a player's skill level and experience on the client.
 */
public class RefreshSkill implements OutgoingPacket {

    private final Skill skill;
    private final int level;
    private final int experience;

    /**
     * Creates a new RefreshSkill packet.
     * 
     * @param skill The skill to refresh
     * @param level The current level to display (including boosts)
     * @param experience The current experience in the skill
     */
    public RefreshSkill(Skill skill, int level, int experience) {
        this.skill = skill;
        this.level = level;
        this.experience = experience;
    }

    @Override
    public void send(Client client) {
        ByteMessage out = ByteMessage.message(134);
        out.put(skill.getId());
        // Using ByteOrder.MIDDLE to fix the experience display issue
        // The client expects the experience in a specific byte order
        out.putInt(experience, ByteOrder.MIDDLE, ValueType.NORMAL);
        out.put(level);
        client.send(out);
       // System.out.println("Sending RefreshSkill packet for skill " + skill + " with level " + level + " and experience " + experience);
    }
}
