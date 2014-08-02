package pt.minecraft.mobcontrol;

import pt.minecraft.mobcontrol.GroupDescriptorProvider.ChunkLocation;

public class Test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
//		Object i = "50a";
//		
//		System.out.println(i instanceof Number);
		
		ChunkLocation cLock1 = new ChunkLocation(0, 0),
				      cLock2 = new ChunkLocation(-1, -1);
		
		System.out.println((int)cLock1.distance(cLock2));

	}

}
