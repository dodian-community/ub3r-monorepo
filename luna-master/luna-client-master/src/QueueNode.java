// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

public class QueueNode extends Node {

	public void unlinkFromQueue() {
		if (prev == null) {
			return;
		} else {
			prev.next = next;
			next.prev = prev;
			next = null;
			prev = null;
			return;
		}
	}

	public QueueNode() {
	}

	public QueueNode next;
	public QueueNode prev;
}
