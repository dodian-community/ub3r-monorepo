package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.game.model.entity.UpdateFlag;

final class PlayerAppearanceState {
    private final Player owner;
    private int playerNpc = -1;
    private int pGender;
    private int pHead;
    private int pTorso;
    private int pArms;
    private int pHands;
    private int pLegs;
    private int pFeet;
    private int pBeard;
    private int playerSE = 0x328;
    private int playerSEW = 0x333;
    private int playerSER = 0x338;

    PlayerAppearanceState(Player owner) {
        this.owner = owner;
    }

    void defaultCharacterLook(Client temp) {
        int[] testLook = {0, 3, 14, 18, 26, 34, 38, 42, 2, 14, 5, 4, 0};
        System.arraycopy(testLook, 0, owner.playerLooks, 0, 13);
        temp.setLook(owner.playerLooks);
    }

    int getGender() {
        return pGender;
    }

    void setGender(int pGender) {
        this.pGender = pGender;
        owner.markAppearanceDirty();
    }

    int getTorso() {
        return pTorso;
    }

    void setTorso(int pTorso) {
        this.pTorso = pTorso;
        owner.markAppearanceDirty();
    }

    int getArms() {
        return pArms;
    }

    void setArms(int pArms) {
        this.pArms = pArms;
        owner.markAppearanceDirty();
    }

    int getLegs() {
        return pLegs;
    }

    void setLegs(int pLegs) {
        this.pLegs = pLegs;
        owner.markAppearanceDirty();
    }

    int getHands() {
        return pHands;
    }

    void setHands(int pHands) {
        this.pHands = pHands;
        owner.markAppearanceDirty();
    }

    int getFeet() {
        return pFeet;
    }

    void setFeet(int pFeet) {
        this.pFeet = pFeet;
        owner.markAppearanceDirty();
    }

    int getBeard() {
        return pBeard;
    }

    void setBeard(int pBeard) {
        this.pBeard = pBeard;
        owner.markAppearanceDirty();
    }

    int getHead() {
        return pHead;
    }

    void setHead(int pHead) {
        this.pHead = pHead;
        owner.markAppearanceDirty();
    }

    int getStandAnim() {
        return playerSE;
    }

    void setStandAnim(int playerSE) {
        this.playerSE = playerSE;
    }

    int getWalkAnim() {
        return playerSEW;
    }

    void setAgilityEmote(int walk, int run) {
        setWalkAnim(walk);
        setRunAnim(run);
        owner.markAppearanceDirty();
        owner.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
    }

    void setWalkAnim(int playerSEW) {
        this.playerSEW = playerSEW;
        owner.markAppearanceDirty();
        owner.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
    }

    int getRunAnim() {
        return playerSER;
    }

    void setRunAnim(int playerSER) {
        this.playerSER = playerSER;
        owner.markAppearanceDirty();
        owner.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
    }

    int getPlayerNpc() {
        return playerNpc;
    }

    void setPlayerNpc(int playerNpc) {
        this.playerNpc = playerNpc;
        owner.markAppearanceDirty();
    }
}
