package net.dodian.uber.game.coroutines.resume

public interface ResumeCondition<T> {

    public fun resumeOrNull(): T?
}
