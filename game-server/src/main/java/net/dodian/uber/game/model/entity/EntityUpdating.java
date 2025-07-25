package net.dodian.uber.game.model.entity;

import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.netty.codec.ByteMessage;

/**
 * @author Dashboard
 */
public abstract class EntityUpdating<T extends Entity> {

    public abstract void update(Player player, ByteMessage buffer);

    public abstract void appendBlockUpdate(T t, ByteMessage buffer);

    public abstract void appendAnimationRequest(T t, ByteMessage buffer);

    public abstract void appendFaceCoordinates(T t, ByteMessage buffer);

    public abstract void appendFaceCharacter(T t, ByteMessage buffer);

    public abstract void appendPrimaryHit(T t, ByteMessage buffer);

}
