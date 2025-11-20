package com.runescape.cache.anim;
import com.runescape.cache.FileArchive;
import com.runescape.io.Buffer;

public final class Animation {

	public static void init(FileArchive streamLoader)
	{
		Buffer stream = new Buffer(streamLoader.readFile("seq.dat"));
		int length = stream.readUShort();
		if(animations == null)
			animations = new Animation[length + 5000];
		for(int j = 0; j < length; j++) {
			if(animations[j] == null)
				animations[j] = new Animation();
			animations[j].readValues(stream);

		}
		
		System.out.println("Loaded: "+length+" Animations");
	}

	public int duration(int i) {
		int j = durations[i];
		if(j == 0)
		{
			Frame frame = Frame.method531(primaryFrames[i]);
			if(frame != null) {
				j = durations[i] = frame.duration;
			}
		}
		if(j == 0)
			j = 1;
		return j;
	}

	private void readValues(Buffer stream) {
		int i;
		while ((i = stream.readUnsignedByte()) != 0){


			if (i == 1) {
				frameCount = stream.readUShort();
				primaryFrames = new int[frameCount];
				secondaryFrames = new int[frameCount];
				durations = new int[frameCount];
				for (int j = 0; j < frameCount; j++) {
					primaryFrames[j] = stream.readInt();
					secondaryFrames[j] = -1;
				}


				for (int j = 0; j < frameCount; j++)
					durations[j] = stream.readUnsignedByte();

			} else if (i == 2)
				loopOffset = stream.readUShort();
			else if (i == 3) {
				int k = stream.readUnsignedByte();
				interleaveOrder = new int[k + 1];
				for (int l = 0; l < k; l++)
					interleaveOrder[l] = stream.readUnsignedByte();
				interleaveOrder[k] = 9999999;
			} else if (i == 4)
				stretches = true;
			else if (i == 5)
				forcedPriority = stream.readUnsignedByte();
			else if (i == 6)
				playerOffhand = stream.readUShort();
			else if (i == 7)
				playerMainhand = stream.readUShort();
			else if (i == 8)
				maximumLoops = stream.readUnsignedByte();
			else if (i == 9)
				animatingPrecedence = stream.readUnsignedByte();
			else if (i == 10)
				priority = stream.readUnsignedByte();
			else if (i == 11)
				replayMode = stream.readUnsignedByte();
			else if (i == 12)
				stream.readInt();
			else
				System.out.println("Error unrecognised seq config code: " + i);
		}
		if (frameCount == 0) {
			frameCount = 1;
			primaryFrames = new int[1];
			primaryFrames[0] = -1;
			secondaryFrames = new int[1];
			secondaryFrames[0] = -1;
			durations = new int[1];
			durations[0] = -1;
		}
		if (animatingPrecedence == -1)
			if (interleaveOrder != null)
				animatingPrecedence = 2;
			else
				animatingPrecedence = 0;
		if (priority == -1) {
			if (interleaveOrder != null) {
				priority = 2;
				return;
			}
			priority = 0;
		}
	}

	private Animation() {
		loopOffset = -1;
		stretches = false;
		forcedPriority = 5;
		playerOffhand = -1; //Removes shield
		playerMainhand = -1; //Removes weapon
		maximumLoops = 99;
		animatingPrecedence = -1; //Stops character from moving
		priority = -1;
		replayMode = 1; 
	}

	public static Animation animations[];
	public int frameCount;
	public int primaryFrames[];
	public int secondaryFrames[];
	public int[] durations;
	public int loopOffset;
	public int interleaveOrder[];
	public boolean stretches;
	public int forcedPriority;
	public int playerOffhand;
	public int playerMainhand;
	public int maximumLoops;
	public int animatingPrecedence;
	public int priority;
	public int replayMode;
	public static int anInt367;

}