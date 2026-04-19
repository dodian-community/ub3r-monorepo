package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.game.model.entity.npc.Npc;

import java.util.ArrayList;

final class PlayerProgressState {
    private final Player owner;
    private final ArrayList<Integer> slayerData = new ArrayList<>();
    private final ArrayList<Boolean> travelData = new ArrayList<>();
    private final ArrayList<Integer> paid = new ArrayList<>();
    private final ArrayList<Boolean> unlocked = new ArrayList<>();
    private final boolean[] songUnlocked = new boolean[net.dodian.uber.game.engine.systems.zone.RegionSong.values().length];

    PlayerProgressState(Player owner) {
        this.owner = owner;
    }

    void setTask(String input) {
        if (input.isEmpty()) {
            input = "-1,-1,0,0,0,0,-1";
        }
        slayerData.clear();
        String[] tasks = input.split(",");
        for (String task : tasks) {
            slayerData.add(Integer.parseInt(task));
        }
        ensureSlayerDataSize();
    }

    String saveTaskAsString() {
        StringBuilder tasks = new StringBuilder();
        for (int i = 0; i < slayerData.size(); i++) {
            tasks.append(slayerData.get(i)).append(i == slayerData.size() - 1 ? "" : ",");
        }
        return tasks.toString();
    }

    ArrayList<Integer> getSlayerData() {
        ensureSlayerDataSize();
        return slayerData;
    }

    void ensureSlayerDataSize() {
        final int[] defaultSlayerData = {-1, -1, 0, 0, 0, 0, -1};
        for (int i = slayerData.size(); i < defaultSlayerData.length; i++) {
            slayerData.add(defaultSlayerData[i]);
        }
    }

    void setTravel(String input) {
        if (input.isEmpty()) {
            input = "0:0:0:0:0";
        }
        travelData.clear();
        String[] travel = input.split(":");
        for (String s : travel) {
            travelData.add(s.equals("1"));
        }
    }

    String saveTravelAsString() {
        StringBuilder travel = new StringBuilder();
        for (int i = 0; i < travelData.size(); i++) {
            int id = travelData.get(i) ? 1 : 0;
            travel.append(id).append(i == travelData.size() - 1 ? "" : ":");
        }
        return travel.toString();
    }

    boolean getTravel(int i) {
        return travelData.get(i);
    }

    void saveTravel(int i) {
        travelData.set(i, true);
    }

    void addUnlocks(int i, String... check) {
        if (check.length == 1) {
            if (unlocked.isEmpty() || i == unlocked.size()) {
                paid.add(i, -1);
                unlocked.add(i, check[0].equals("1"));
            } else {
                paid.set(i, -1);
                unlocked.set(i, check[0].equals("1"));
            }
        } else if (check.length == 2) {
            if (unlocked.isEmpty() || i == unlocked.size()) {
                unlocked.add(i, check[1].equals("1"));
                paid.add(i, check[0].equals("1") ? 1 : 0);
            } else {
                unlocked.set(i, check[1].equals("1"));
                paid.set(i, check[0].equals("1") ? 1 : 0);
            }
        }
    }

    String saveUnlocksAsString() {
        StringBuilder unlocks = new StringBuilder();
        for (int i = 0; i < unlocked.size(); i++) {
            int unlock = unlocked.get(i) ? 1 : 0;
            unlocks.append(paid.get(i) == -1 ? unlock + "" : paid.get(i) + "," + unlock).append(i == unlocked.size() - 1 ? "" : ":");
        }
        return unlocks.toString();
    }

    boolean checkUnlock(int i) {
        return !unlocked.isEmpty() && unlocked.size() >= i && unlocked.get(i);
    }

    int checkUnlockPaid(int i) {
        return paid.isEmpty() || paid.size() < i ? -1 : paid.get(i);
    }

    String getSongUnlockedSaveText() {
        StringBuilder out = new StringBuilder();
        for (boolean b : songUnlocked) {
            out.append(b ? 1 : 0).append(" ");
        }
        return out.toString();
    }

    boolean isSongUnlocked(int songId) {
        return songUnlocked[songId];
    }

    void setSongUnlocked(int songId, boolean unlocked) {
        songUnlocked[songId] = unlocked;
    }

    boolean areAllSongsUnlocked() {
        for (boolean unlocked : songUnlocked) {
            if (!unlocked) {
                return false;
            }
        }
        return true;
    }

    void bossCount(String name, int amount) {
        for (int i = 0; i < owner.boss_name.length; i++) {
            if (owner.boss_name[i].equalsIgnoreCase(name)) {
                owner.boss_amount[i] = amount;
            }
        }
    }

    void addMonsterName(String name) {
        int index = owner.monsterName.size();
        if (index == 0) {
            owner.monsterName.add(name);
            owner.monsterCount.add(1);
        } else {
            ArrayList<String> nameClone = (ArrayList<String>) owner.monsterName.clone();
            ArrayList<Integer> countClone = (ArrayList<Integer>) owner.monsterCount.clone();
            owner.monsterName.clear();
            owner.monsterCount.clear();
            owner.monsterName.add(name);
            owner.monsterCount.add(1);
            for (int i = 0; i < nameClone.size(); i++) {
                owner.monsterName.add(nameClone.get(i));
                owner.monsterCount.add(countClone.get(i));
            }
        }
    }

    int getMonsterIndex(String name) {
        int slot = -1;
        for (int i = 0; i < owner.monsterName.size() && slot == -1; i++) {
            if (owner.monsterName.get(i).equals(name)) {
                slot = i;
            }
        }
        return slot;
    }

    void incrementMonsterLog(Npc npc) {
        String name = npc.npcName().toLowerCase();
        int index = getMonsterIndex(name);
        if (index >= 0) {
            addMonsterLog(npc, index);
        } else {
            addMonsterName(name);
        }
    }

    void addMonsterLog(Npc npc, int index) {
        String name = npc.npcName().toLowerCase();
        int amount = index == -1 ? 0 : owner.monsterCount.get(index);
        int newAmount = amount < 1048576 ? amount + 1 : amount;
        if (index == 0) {
            owner.monsterCount.set(index, newAmount);
        } else if (index > 0) {
            ArrayList<String> nameClone = (ArrayList<String>) owner.monsterName.clone();
            ArrayList<Integer> countClone = (ArrayList<Integer>) owner.monsterCount.clone();
            owner.monsterName.clear();
            owner.monsterCount.clear();
            nameClone.remove(index);
            countClone.remove(index);
            owner.monsterName.add(name);
            owner.monsterCount.add(newAmount);
            for (int i = 0; i < nameClone.size(); i++) {
                owner.monsterName.add(nameClone.get(i));
                owner.monsterCount.add(countClone.get(i));
            }
        }
    }

    int monsterKC(Npc npc) {
        int index = getMonsterIndex(npc.npcName().toLowerCase());
        if (index >= 0) {
            return owner.monsterCount.get(index);
        }
        return 0;
    }
}
