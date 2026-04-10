// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

public class HashTable {

	public HashTable(int _size) {
		size = _size;
		buckets = new Node[_size];
		for (int pos = 0; pos < _size; pos++) {
			Node bucket = buckets[pos] = new Node();
			bucket.next = bucket;
			bucket.previous = bucket;
		}
	}

	public Node get(long id) {
		Node bucket = buckets[(int) (id & (size - 1))];
		for (Node node = bucket.next; node != bucket; node = node.next)
			if (node.id == id)
				return node;
		return null;
	}

	public void put(Node node, long id) {
		if (node.previous != null)
			node.unlink();
		Node bucket = buckets[(int) (id & (size - 1))];
		node.previous = bucket.previous;
		node.next = bucket;
		node.previous.next = node;
		node.next.previous = node;
		node.id = id;
		return;
	}

	public int size;
	public Node buckets[];
}
