// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

public class LinkedList {

	public LinkedList() {
		head = new Node();
		head.next = head;
		head.previous = head;
	}

	public void addLast(Node node) {
		if (node.previous != null)
			node.unlink();
		node.previous = head.previous;
		node.next = head;
		node.previous.next = node;
		node.next.previous = node;
	}

	public void addFirst(Node node) {
		if (node.previous != null)
			node.unlink();
		node.previous = head;
		node.next = head.next;
		node.previous.next = node;
		node.next.previous = node;
	}

	public Node removeFirst() {
		Node node = head.next;
		if (node == head) {
			return null;
		} else {
			node.unlink();
			return node;
		}
	}

	public Node first() {
		Node node = head.next;
		if (node == head) {
			current = null;
			return null;
		} else {
			current = node.next;
			return node;
		}
	}

	public Node last() {
		Node node = head.previous;
		if (node == head) {
			current = null;
			return null;
		} else {
			current = node.previous;
			return node;
		}
	}

	public Node next() {
		Node node = current;
		if (node == head) {
			current = null;
			return null;
		}
		current = node.next;
		return node;
	}

	public Node previous() {
		Node node = current;
		if (node == head) {
			current = null;
			return null;
		} else {
			current = node.previous;
			return node;
		}
	}

	public void clear() {
		if (head.next == head)
			return;
		do {
			Node node = head.next;
			if (node == head)
				return;
			node.unlink();
		} while (true);
	}

	public Node head;
	public Node current;
}
