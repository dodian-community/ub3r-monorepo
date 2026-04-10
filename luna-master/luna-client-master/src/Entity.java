// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

public class Entity extends QueueNode {

	public void method560(int i, int j, int k, int l, int i1, int j1, int k1, int l1, int i2) {
		Model model = getModel();
		if (model != null) {
			height = ((Entity) (model)).height;
			model.method560(i, j, k, l, i1, j1, k1, l1, i2);
		}
	}

	public Model getModel() {
		return null;
	}

	public Entity() {
		height = 1000;
	}

	public VertexNormal normals[];
	public int height;

}
