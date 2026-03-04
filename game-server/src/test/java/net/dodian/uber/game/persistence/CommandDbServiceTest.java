package net.dodian.uber.game.persistence;

import net.dodian.uber.game.model.player.skills.Skills;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandDbServiceTest {

    @Test
    void parseContainerEntriesParsesSerializedItems() {
        ArrayList<CommandDbService.ContainerEntry> items = CommandDbService.parseContainerEntries("0-4151-1 1-995-250 ");

        assertEquals(2, items.size());
        assertEquals(4151, items.get(0).getItemId());
        assertEquals(1, items.get(0).getAmount());
        assertEquals(995, items.get(1).getItemId());
        assertEquals(250, items.get(1).getAmount());
    }

    @Test
    void applyItemRemovalRewritesContainerAndTracksRemainingAmount() {
        CommandDbService.ContainerMutation mutation =
                CommandDbService.applyItemRemoval("0-100-5 1-200-3 2-100-4 ", 100, 7);

        assertEquals("1-200-3 2-100-2 ", mutation.getUpdatedText());
        assertEquals(7, mutation.getRemovedAmount());
        assertEquals(0, mutation.getRemainingAmount());
    }

    @Test
    void computeSkillMutationClampsRemovalAndRecomputesTotals() {
        CommandDbService.SkillMutationComputation computation =
                CommandDbService.computeSkillMutation(5000, 12000, 50, 9000);

        assertEquals(5000, computation.getCurrentXp());
        assertEquals(5000, computation.getRemovedXp());
        assertEquals(0, computation.getNewXp());
        assertEquals(7000, computation.getNewTotalXp());
        assertEquals(
                50 - Skills.getLevelForExperience(5000) + Skills.getLevelForExperience(0),
                computation.getNewTotalLevel()
        );
    }
}
