// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

public class LruHashTable {

	public LruHashTable(int size) {
		queue = new LinkedQueue();
		capacity = size;
		remaining = size;
		table = new HashTable(1024);
	}

	public QueueNode get(long id) {
		QueueNode node = (QueueNode) table.get(id);
		if (node != null) {
			queue.push(node);
			hits++;
		} else {
			misses++;
		}
		return node;
	}

	public void put(QueueNode node, long id) {
		if (remaining == 0) {
			QueueNode oldestNode = queue.pop();
			oldestNode.unlink();
			oldestNode.unlinkFromQueue();
		} else {
			remaining--;
		}
		table.put(node, id);
		queue.push(node);
	}

	public void clear() {
		do {
			QueueNode node = queue.pop();
			if (node != null) {
				node.unlink();
				node.unlinkFromQueue();
			} else {
				remaining = capacity;
				return;
			}
		} while (true);
	}

	public int misses;
	public int hits;
	public int capacity;
	public int remaining;
	public HashTable table;
	public LinkedQueue queue;

}
