package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;

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
        ByteMessage message = null;
        switch (mode) {
            case "update":
                // SPIN_CAMERA (opcode 166): x, y, height, speed, angle
                message = ByteMessage.message(166);
                message.put(x / 64);
                message.put(y / 64);
                message.putShort(z);
                message.put(speed);
                message.put(angle);
                break;
            case "rotation":
                // MOVE_CAMERA (opcode 177): x, y, height, speed1, speed2
                message = ByteMessage.message(177);
                message.put(x);
                message.put(y);
                message.putShort(z);
                message.put(sp1);
                message.put(sp2);
                break;
            case "location":
                // This mode was not implemented in the legacy code.
                break;
            case "spin":
                // SPIN_CAMERA (opcode 166): x, y, height, speed, angle
                message = ByteMessage.message(166);
                message.put(x / 64);
                message.put(y / 64);
                message.putShort(z);
                message.put(speed);
                message.put(angle);
                break;
        }

        if (message != null) {
            client.send(message);
        }
    }

}
