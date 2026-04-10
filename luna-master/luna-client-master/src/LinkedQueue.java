// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

public class LinkedQueue {

	public LinkedQueue() {
		head = new QueueNode();
		head.next = head;
		head.prev = head;
	}

	public void push(QueueNode node) {
		if (node.prev != null)
			node.unlinkFromQueue();
		node.prev = head.prev;
		node.next = head;
		node.prev.next = node;
		node.next.prev = node;
	}

	public QueueNode pop() {
		QueueNode node = head.next;
		if (node == head) {
			return null;
		} else {
			node.unlinkFromQueue();
			return node;
		}
	}

	public QueueNode first() {
		QueueNode node = head.next;
		if (node == head) {
			current = null;
			return null;
		} else {
			current = node.next;
			return node;
		}
	}

	public QueueNode next() {
		QueueNode node = current;
		if (node == head) {
			current = null;
			return null;
		}
		current = node.next;
		return node;
	}

	public int size() {
		int size = 0;
		for (QueueNode node = head.next; node != head; node = node.next)
			size++;
		return size;
	}

	public QueueNode head;
	public QueueNode current;
}
