// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

public class GroundItem extends Entity {

	@Override
	public Model getModel() {
		ItemDefinition def = ItemDefinition.forId(id);
		return def.getModel(amount);
	}

	public GroundItem() {
	}

	public int id;
	public int amount;
}
