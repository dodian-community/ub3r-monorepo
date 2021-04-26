package net.dodian.uber.game.model.player.packets.outgoing;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.OutgoingPacket;

public class SendCamera implements OutgoingPacket {

    private int x, y, z, speed, angle, sp1, sp2;
    private String mode;

    public SendCamera(String mode, int x, int y, int z, int speed, int angle) {
        this.mode = mode;
        this.x = x;
        this.y = y;
        this.z = z;
        this.speed = speed;
        this.angle = angle;
    }

    public SendCamera(String mode, int x, int y, int z, int speed1, int speed2, String dummy) {
        this.mode = mode;
        this.x = x;
        this.y = y;
        this.z = z;
        this.sp1 = speed1;
        this.sp2 = speed2;
    }

    @Override
    public void send(Client client) {
        if (mode.equals("update")) {
            client.getOutputStream().createFrame(177);
            client.getOutputStream().writeByte(x / 64);
            client.getOutputStream().writeByte(y / 64);
            client.getOutputStream().writeWord(z);
            client.getOutputStream().writeByte(speed);
            client.getOutputStream().writeByte(angle);
            client.flushOutStream();
        } else if (mode.equals("rotation")) {
            client.getOutputStream().createFrame(177);
            client.getOutputStream().writeByte((byte) x);
            client.getOutputStream().writeByte((byte) y);
            client.getOutputStream().writeWord(z);
            client.getOutputStream().writeByte((byte) sp1);
            client.getOutputStream().writeByte((byte) sp2);
        } else if (mode.equals("location")) {
            client.getOutputStream().createFrame(166);
            // out.writeByte((byte)(location.getLocalX(player.getPosition())));
            // out.writeByte((byte)(location.getLocalY(player.getPosition())));
            // out.writeShort(zPos);
            // out.writeByte((byte)constantSpeed);
            // out.writeByte((byte)variableSpeed);
            // Leave for now!
        } else if (mode.equals("spin")) {
            client.getOutputStream().createFrame(166);
            client.getOutputStream().writeByte(x / 64);
            client.getOutputStream().writeByte(y / 64);
            client.getOutputStream().writeWord(z);
            client.getOutputStream().writeByte(speed);
            client.getOutputStream().writeByte(angle);
        }
        client.flushOutStream();
    }

}
