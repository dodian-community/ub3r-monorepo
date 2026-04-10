// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

public abstract class Actor extends Entity {

	public void resetWalkingQueue() {
		walkingQueueSize = 0;
		anInt1613 = 0;
	}

	public boolean isVisible() {
		return false;
	}

	public void addStep(int direction, boolean running) {
		int x = walkingQueueX[0];
		int y = walkingQueueY[0];
		if (direction == 0) {
			x--;
			y++;
		}
		if (direction == 1)
			y++;
		if (direction == 2) {
			x++;
			y++;
		}
		if (direction == 3)
			x--;
		if (direction == 4)
			x++;
		if (direction == 5) {
			x--;
			y--;
		}
		if (direction == 6)
			y--;
		if (direction == 7) {
			x++;
			y--;
		}
		if (currentAnimation != -1 && Animation.animations[currentAnimation].anInt306 == 1)
			currentAnimation = -1;
		if (walkingQueueSize < 9)
			walkingQueueSize++;
		for (int pos = walkingQueueSize; pos > 0; pos--) {
			walkingQueueX[pos] = walkingQueueX[pos - 1];
			walkingQueueY[pos] = walkingQueueY[pos - 1];
			runningQueue[pos] = runningQueue[pos - 1];
		}

		walkingQueueX[0] = x;
		walkingQueueY[0] = y;
		runningQueue[0] = running;
	}

	public void applyHit(int i, boolean flag, int j, int k) {
		for (int l = 0; l < 4; l++)
			if (anIntArray1632[l] <= i) {
				anIntArray1630[l] = j;
				anIntArray1631[l] = k;
				anIntArray1632[l] = i + 70;
				return;
			}

		if (flag)
			anInt1581 = -52;
	}

	public void teleport(int x, int y, boolean discard) {
		if (currentAnimation != -1 && Animation.animations[currentAnimation].anInt306 == 1)
			currentAnimation = -1;
		if (!discard) {
			int k = x - walkingQueueX[0];
			int i1 = y - walkingQueueY[0];
			if (k >= -8 && k <= 8 && i1 >= -8 && i1 <= 8) {
				if (walkingQueueSize < 9)
					walkingQueueSize++;
				for (int j1 = walkingQueueSize; j1 > 0; j1--) {
					walkingQueueX[j1] = walkingQueueX[j1 - 1];
					walkingQueueY[j1] = walkingQueueY[j1 - 1];
					runningQueue[j1] = runningQueue[j1 - 1];
				}

				walkingQueueX[0] = x;
				walkingQueueY[0] = y;
				runningQueue[0] = false;
				return;
			}
		}
		walkingQueueSize = 0;
		anInt1613 = 0;
		anInt1623 = 0;
		walkingQueueX[0] = x;
		walkingQueueY[0] = y;
		unitX = walkingQueueX[0] * 128 + anInt1601 * 64;
		unitY = walkingQueueY[0] * 128 + anInt1601 * 64;
	}

	public Actor() {
		anInt1581 = -89;
		forcedChatTicks = 100;
		walkingQueueX = new int[10];
		walkingQueueY = new int[10];
		anInt1588 = -1;
		runningQueue = new boolean[10];
		aBoolean1592 = false;
		anInt1594 = 200;
		lastHitCycle = -1000;
		anInt1600 = 32;
		anInt1601 = 1;
		transformationId = -1;
		anInt1614 = -1;
		anInt1619 = -1;
		anInt1620 = -1;
		anInt1621 = -1;
		anInt1622 = -1;
		currentAnimation = -1;
		anInt1629 = -1;
		anIntArray1630 = new int[4];
		anIntArray1631 = new int[4];
		anIntArray1632 = new int[4];
		anInt1634 = -1;
		anInt1635 = -1;
	}

	public String forcedChatMessage;
	public int anInt1581;
	public int forcedChatTicks;
	public int anInt1583;
	public int anInt1584;
	public int pulseCycle;
	public int walkingQueueX[];
	public int walkingQueueY[];
	public int anInt1588;
	public int anInt1589;
	public int anInt1590;
	public boolean runningQueue[];
	public boolean aBoolean1592;
	public int anInt1593;
	public int anInt1594;
	public int lastHitCycle;
	public int hitType;
	public int hitAmount;
	public int nextStepX;
	public int nextStepY;
	public int anInt1600;
	public int anInt1601;
	public int anInt1602;
	public int anInt1603;
	public int anInt1604;
	public int anInt1605;
	public int anInt1606;
	public int anInt1607;
	public int anInt1608;
	public int transformationId;
	public int unitX;
	public int unitY;
	public int anInt1612;
	public int anInt1613;
	public int anInt1614;
	public int anInt1615;
	public int anInt1616;
	public int anInt1617;
	public int anInt1618;
	public int anInt1619;
	public int anInt1620;
	public int anInt1621;
	public int anInt1622;
	public int anInt1623;
	public int currentAnimation;
	public int animationFrame;
	public int animationFrameCycle;
	public int animationDelay;
	public int animationResetCycle;
	public int anInt1629;
	public int anIntArray1630[];
	public int anIntArray1631[];
	public int anIntArray1632[];
	public int walkingQueueSize;
	public int anInt1634;
	public int anInt1635;
}
