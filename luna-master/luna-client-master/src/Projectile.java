// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

public class Projectile extends Entity {

	public void trackTarget(int offsetX, int offsetY, int heightDelta, int createTime) {
		if (!aBoolean1575) {
			double d = offsetX - x;
			double d2 = offsetY - y;
			double d3 = Math.sqrt(d * d + d2 * d2);
			aDouble1555 = x + (d * distanceFromSource) / d3;
			aDouble1556 = y + (d2 * distanceFromSource) / d3;
			aDouble1557 = heightStart;
		}
		double d1 = (speed + 1) - createTime;
		aDouble1569 = (offsetX - aDouble1555) / d1;
		aDouble1570 = (offsetY - aDouble1556) / d1;
		aDouble1571 = Math.sqrt(aDouble1569 * aDouble1569 + aDouble1570 * aDouble1570);
		if (!aBoolean1575)
			aDouble1572 = -aDouble1571 * Math.tan(initialSlope * 0.02454369D);
		aDouble1574 = (2D * (heightDelta - aDouble1557 - aDouble1572 * d1)) / (d1 * d1);
	}

	public void method563(int i, boolean flag) {
		aBoolean1575 = true;
		aDouble1555 += aDouble1569 * i;
		if (flag) {
			for (int j = 1; j > 0; j++);
		}
		aDouble1556 += aDouble1570 * i;
		aDouble1557 += aDouble1572 * i + 0.5D * aDouble1574 * i * i;
		aDouble1572 += aDouble1574 * i;
		anInt1562 = (int) (Math.atan2(aDouble1569, aDouble1570) * 325.94900000000001D) + 1024 & 0x7ff;
		anInt1563 = (int) (Math.atan2(aDouble1572, aDouble1571) * 325.94900000000001D) & 0x7ff;
		if (spotAnimation.animation != null)
			for (anInt1568 += i; anInt1568 > spotAnimation.animation.method205(0, anInt1567);) {
				anInt1568 -= spotAnimation.animation.method205(0, anInt1567);
				anInt1567++;
				if (anInt1567 >= spotAnimation.animation.anInt294)
					anInt1567 = 0;
			}

	}

	@Override
	public Model getModel() {
		Model class50_sub1_sub4_sub4 = spotAnimation.getModel();
		if (class50_sub1_sub4_sub4 == null)
			return null;
		int i = -1;
		if (spotAnimation.animation != null)
			i = spotAnimation.animation.anIntArray295[anInt1567];
		Model class50_sub1_sub4_sub4_1 = new Model(false, false, true,
				class50_sub1_sub4_sub4, Class21.method239(i));
		if (i != -1) {
			class50_sub1_sub4_sub4_1.method584(7);
			class50_sub1_sub4_sub4_1.method585(i, (byte) 6);
			class50_sub1_sub4_sub4_1.anIntArrayArray1679 = null;
			class50_sub1_sub4_sub4_1.anIntArrayArray1678 = null;
		}
		if (spotAnimation.anInt561 != 128 || spotAnimation.anInt562 != 128)
			class50_sub1_sub4_sub4_1.method593(spotAnimation.anInt562, spotAnimation.anInt561, 9,
					spotAnimation.anInt561);
		class50_sub1_sub4_sub4_1.method589(anInt1563, 341);
		class50_sub1_sub4_sub4_1.method594(64 + spotAnimation.anInt564, 850 + spotAnimation.anInt565, -30, -50, -30,
				true);
		return class50_sub1_sub4_sub4_1;
	}

	public Projectile(int plane, int heightEnd, int distanceFromSource, int y, int id, int speed, int initialSlope, int target, int heightStart, int x,
					  int createdTime) {
		aBoolean1561 = false;
		aBoolean1573 = true;
		spotAnimation = SpotAnimation.spotAnimations[id];
		this.plane = plane;
		this.x = x;
		this.y = y;
		this.heightStart = heightStart;
		this.createdTime = createdTime;
		this.speed = speed;
		this.initialSlope = initialSlope;
		this.distanceFromSource = distanceFromSource;
		this.target = target;
		this.heightEnd = heightEnd;
		aBoolean1575 = false;
		return;
	}

	public SpotAnimation spotAnimation;
	public int plane;
	public double aDouble1555;
	public double aDouble1556;
	public double aDouble1557;
	public int initialSlope;
	public int distanceFromSource;
	public int target;
	public boolean aBoolean1561;
	public int anInt1562;
	public int anInt1563;
	public int createdTime;
	public int speed;
	public int anInt1567;
	public int anInt1568;
	public double aDouble1569;
	public double aDouble1570;
	public double aDouble1571;
	public double aDouble1572;
	public boolean aBoolean1573;
	public double aDouble1574;
	public boolean aBoolean1575;
	public int x;
	public int y;
	public int heightStart;
	public int heightEnd;
}
