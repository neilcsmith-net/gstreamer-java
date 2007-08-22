/*
 * GarbageCollectionTest.java
 * JUnit 4.x based test
 *
 * Created on 21 August 2007, 17:50
 */

package org.gstreamer;

import com.sun.jna.Pointer;
import java.lang.ref.WeakReference;
import org.gstreamer.lowlevel.GObjectAPI;
import org.gstreamer.lowlevel.IntPtr;
import org.junit.Test;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

/**
 *
 * @author wayne
 */
public class GarbageCollectionTest {
    
    public GarbageCollectionTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        Gst.init("test", new String[] {});
    }
    
    @AfterClass
    public static void tearDownClass() throws Exception {
        Gst.deinit();
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }
    
    public boolean waitGC(WeakReference<? extends Object> ref) throws InterruptedException {
        System.gc();
        for (int i = 0; ref.get() != null && i < 20; ++i) {
            Thread.sleep(10);
            System.gc();
        }
        return ref.get() == null;
    }
    @Test
    public void testElement() throws Exception {
        final boolean[] destroyed = new boolean[] { false };
        Element e = ElementFactory.make("fakesrc", "test element");
        GObjectAPI.GWeakNotify notify = new GObjectAPI.GWeakNotify() {
            public void callback(IntPtr id, Pointer obj) {
                destroyed[0] = true;
            }
        };
        GObjectAPI.gobj.g_object_weak_ref(e, notify, new IntPtr(System.identityHashCode(this)));
        WeakReference<Element> ref = new WeakReference<Element>(e);
        e = null;
        destroyed[0] = false;
        assertTrue("Element not garbage collected", waitGC(ref));        
        assertTrue("GObject not destroyed", destroyed[0]);
    }
    @Test
    public void testBin() throws Exception {
        Bin bin = new Bin("test");
        Element e1 = ElementFactory.make("fakesrc", "source");
        Element e2 = ElementFactory.make("fakesink", "sink");
        bin.addMany(e1, e2);
        
        assertEquals("source not returned", e1, bin.getElementByName("source"));
        assertEquals("sink not returned", e2, bin.getElementByName("sink"));
        WeakReference<Element> binRef = new WeakReference<Element>(bin);
        bin = null;
        assertTrue("Bin not garbage collected", waitGC(binRef));
        WeakReference<Element> e1Ref = new WeakReference<Element>(e1);
        WeakReference<Element> e2Ref = new WeakReference<Element>(e2);
        e1 = null;
        e2 = null;
        
        assertTrue("First Element not garbage collected", waitGC(e1Ref));
        assertTrue("Second Element not garbage collected", waitGC(e2Ref));
        
    }
    @Test
    public void testBinRetrieval() throws Exception {
        Bin bin = new Bin("test");
        Element e1 = ElementFactory.make("fakesrc", "source");
        Element e2 = ElementFactory.make("fakesink", "sink");
        bin.addMany(e1, e2);
        int id1 = System.identityHashCode(e1);
        int id2 = System.identityHashCode(e2);
        
        e1 = null;
        e2 = null;
        System.gc();
        Thread.sleep(10);
        // Should return the same object that was put into the bin
        assertEquals("source ID does not match", id1, System.identityHashCode(bin.getElementByName("source")));
        assertEquals("sink ID does not match", id2, System.identityHashCode(bin.getElementByName("sink")));       
    } 
}