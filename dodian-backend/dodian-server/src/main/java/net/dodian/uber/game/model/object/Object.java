package net.dodian.uber.game.model.object;

public class Object {
    public int id, x, y, z, type, face, oldId;
    private java.lang.Object attachment = null;

    public Object(int id, int x, int y, int z, int type) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = type;
    }

    public Object(int id, int x, int y, int z, int type, int face) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = type;
        this.face = face;
    }

    public Object(int id, int x, int y, int z, int type, int face, int oldId) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
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
