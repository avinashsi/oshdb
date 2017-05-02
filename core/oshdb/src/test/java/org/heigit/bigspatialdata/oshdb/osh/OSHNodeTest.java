package org.heigit.bigspatialdata.oshdb.osh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.heigit.bigspatialdata.oshdb.osh.OSHNode;
import org.heigit.bigspatialdata.oshdb.osm.OSMNode;
import org.junit.Test;

public class OSHNodeTest {

	public static final int USER_A = 1;
	public static final int USER_B = 2;
	public static final int[] TAGS_A = new int[] { 1, 1 };
	public static final int[] TAGS_B = new int[] { 2, 2 };
	public static final long[] LONLAT_A = new long[] { 86756350l, 494186210l };
	public static final long[] LONLAT_B = new long[] { 87153340l, 494102830l };

	
	@Test
	public void testBuild() throws IOException{
		List<OSMNode> versions = new ArrayList<>();
	    
	    versions.add(new OSMNode(123l,1,1l,0l,USER_A,TAGS_A,LONLAT_A[0], LONLAT_A[1]));
	    versions.add(new OSMNode(123l,-2,2l,0l,USER_A,TAGS_A,LONLAT_A[0], LONLAT_A[1]));
	    
	    
	    OSHNode hnode = OSHNode.build(versions);    
	    assertNotNull(hnode);
	    
	    List<OSMNode> v = hnode.getVersions();
	    assertNotNull(v);
	    assertEquals(2, v.size());
	}
	
	@Test
	public void testCompact() {
		fail("Not yet implemented");
	}
	
	@Test
	public void testRebase() throws IOException{
		
		long baseLongitude=85341796875l/100;
		long baseLatitude= 27597656250l/100;
		
		List<OSMNode> versions = new ArrayList<>();
		//NODE: ID:3718143950 V:+2+ TS:1480304071000 CS:43996323 VIS:true USER:4803525 TAGS:[] 85391383800:27676689900
		// NODE: ID:3718143950 V:+1+ TS:1440747974000 CS:33637224 VIS:true USER:3191558 TAGS:[] 85391416000:27676640000
		
		//NODE: ID:3718143950 V:+2+ TS:1480304071000 CS:43996323 VIS:true USER:4803525 TAGS:[] 85391383800:27676689900
		//NODE: ID:3718143950 V:+1+ TS:1440747974000 CS:33637224 VIS:true USER:3191558 TAGS:[] 49619125:78983750
	    
	    versions.add(new OSMNode(3718143950l,2,1480304071000l/1000,43996323l,4803525,new int[0], 85391383800l/100, 27676689900l/100));
	    versions.add(new OSMNode(3718143950l,1,1440747974000l/1000,33637224, 3191558,new int[0], 85391416000l/100, 27676640000l/100));
	    
	    OSHNode hosm = OSHNode.build(versions); 
	    for(OSMNode osm : hosm){
	    	System.out.println(osm);
	    }
	    
	    System.out.println("Datasize:"+hosm.getData().length);
	    
	    hosm =  hosm.rebase(0, 0, baseLongitude, baseLatitude);
	    System.out.println("Datasize:"+hosm.getData().length);
	    for(OSMNode osm : hosm){
	    	System.out.println(osm);
	    }
		
	}

}