package net.dodian.uber.game.model.object;

/**
 * Whoever authored this class is a massive dumbass.
 *
 * @author Logan
 * ^^^^^^^^^^^^^^^^^
 */
public class RS2Object {
    public int id, x, y, type, face, oldId;
    private java.lang.Object attachment = null;

    public RS2Object(int id, int x, int y, int type) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public RS2Object(int id, int x, int y, int type, int face) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.type = type;
        this.face = face;
    }

    public RS2Object(int id, int x, int y, int type, int face, int oldId) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.type = type;
        this.face = face;
        this.oldId = oldId;
    }

    public void setAttachment(java.lang.Object o) {
        this.attachment = o;
    }

    /**
     * @return the attachment
     */
    public java.lang.Object getAttachment() {
        return attachment;
    }
}