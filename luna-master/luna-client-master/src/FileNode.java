// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

public class FileNode extends QueueNode {

	public FileNode() {
		immediate = true;
	}

	public int type;
	public int id;
	public int cycles;
	public byte buf[];
	public boolean immediate;
}
