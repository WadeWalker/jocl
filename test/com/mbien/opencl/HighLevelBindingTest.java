package com.mbien.opencl;

import com.mbien.opencl.CLMemory.Mem;
import com.mbien.opencl.CLCommandQueue.Mode;
import com.mbien.opencl.CLDevice.FPConfig;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static java.lang.System.*;
import static com.mbien.opencl.TestUtils.*;
import static com.sun.gluegen.runtime.BufferFactory.*;

/**
 * Test testing the high level bindings.
 * @author Michael Bien
 */
public class HighLevelBindingTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        out.println("OS: " + System.getProperty("os.name"));
        out.println("ARCH: " + System.getProperty("os.arch"));
        out.println("VM: " + System.getProperty("java.vm.name"));
        out.println("lib path: " + System.getProperty("java.library.path"));
    }

    @Test
    public void contextlessTest() {

        out.println(" - - - highLevelTest; contextless - - - ");

        // enum tests
        final EnumSet<FPConfig> singleFPConfig = FPConfig.valuesOf(CL.CL_FP_DENORM | CL.CL_FP_ROUND_TO_INF);
        assertEquals(0, FPConfig.valuesOf(0).size());
        assertTrue(singleFPConfig.contains(FPConfig.DENORM));
        assertTrue(singleFPConfig.contains(FPConfig.ROUND_TO_INF));

        final EnumSet<Mode> queueMode = Mode.valuesOf(CL.CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE | CL.CL_QUEUE_PROFILING_ENABLE);
        assertEquals(0, Mode.valuesOf(0).size());
        assertTrue(queueMode.contains(Mode.OUT_OF_ORDER_EXEC_MODE));
        assertTrue(queueMode.contains(Mode.PROFILING_MODE));

        // platform/device info tests
        CLPlatform[] clPlatforms = CLPlatform.listCLPlatforms();

        for (CLPlatform platform : clPlatforms) {

            out.println("platform info:");
            out.println("    name: "+platform.getName());
            out.println("    id: "+platform.ID);
            out.println("    profile: "+platform.getProfile());
            out.println("    version: "+platform.getVersion());
            out.println("    vendor: "+platform.getVendor());
            out.println("    max FLOPS device: "+platform.getMaxFlopsDevice());

            CLDevice[] clDevices = platform.listCLDevices();
            for (CLDevice device : clDevices) {
                out.println("device info:");
                out.println("    name: "+device.getName());
                out.println("    profile: "+device.getProfile());
                out.println("    vendor: "+device.getVendor());
                out.println("    vendor id: "+device.getVendorID());
                out.println("    version: "+device.getVersion());
                out.println("    driver version: "+device.getDriverVersion());
                out.println("    type: "+device.getType());
                out.println("    global mem: "+device.getGlobalMemSize()/(1024*1024)+" MB");
                out.println("    max alloc mem: "+device.getMaxMemAllocSize()/(1024*1024)+" MB");
                out.println("    max param size: "+device.getMaxParameterSize()+" byte");
                out.println("    local mem: "+device.getLocalMemSize()/1024+" KB");
                out.println("    local mem type: "+device.getLocalMemType());
                out.println("    global mem cache size: "+device.getGlobalMemCachSize());
                out.println("    global mem cache type: "+device.getGlobalMemCacheType());
                out.println("    constant buffer size: "+device.getMaxConstantBufferSize());
                out.println("    error correction support: "+device.isErrorCorrectionSupported());
                out.println("    queue properties: "+device.getQueueProperties());
                out.println("    clock: "+device.getMaxClockFrequency()+" MHz");
                out.println("    timer res: "+device.getProfilingTimerResolution()+" ns");
                out.println("    max work group size: "+device.getMaxWorkGroupSize());
                out.println("    max compute units: "+device.getMaxComputeUnits());
                out.println("    max work item dimensions: "+device.getMaxWorkItemDimensions());
                out.println("    max work item sizes: "+Arrays.toString(device.getMaxWorkItemSizes()));
                out.println("    compiler available: "+device.isCompilerAvailable());
                out.println("    image support: "+device.isImageSupportAvailable());
                out.println("    max read image args: "+device.getMaxReadImageArgs());
                out.println("    max write image args: "+device.getMaxWriteImageArgs());
                out.println("    max image2d dimensions: "+Arrays.asList(device.getMaxImage2dWidth(), device.getMaxImage2dHeight()));
                out.println("    max image3d dimensions: "+Arrays.asList(device.getMaxImage2dWidth(), device.getMaxImage2dHeight(), device.getMaxImage3dDepth()));
                out.println("    number of address bits: "+device.getAddressBits());
                out.println("    half FP available: "+device.isHalfFPAvailable());
                out.println("    double FP available: "+device.isDoubleFPAvailable());
                out.println("    little endian: "+device.isLittleEndianAvailable());
                out.println("    half FP config: "+device.getHalfFPConfig());
                out.println("    single FP config: "+device.getSingleFPConfig());
                out.println("    double FP config: "+device.getDoubleFPConfig());
                out.println("    extensions: "+device.getExtensions());
            }
        }

    }

    @Test
    public void vectorAddGMTest() throws IOException {

        out.println(" - - - highLevelTest; global memory kernel - - - ");

        CLPlatform[] clPlatforms = CLPlatform.listCLPlatforms();
        CLContext context = CLContext.create(clPlatforms[0]);

        CLDevice[] contextDevices = context.getCLDevices();

        out.println("context devices:");
        for (CLDevice device : contextDevices) {
            out.println("   "+device.toString());
        }

        out.println("max FLOPS device: " + context.getMaxFlopsDevice());

        CLProgram program = context.createProgram(getClass().getResourceAsStream("testkernels.cl")).build();

        CLDevice[] programDevices = program.getCLDevices();

        assertEquals(contextDevices.length, programDevices.length);

        out.println("build log:\n"+program.getBuildLog());
        out.println("build status:\n"+program.getBuildStatus());

        String source = program.getSource();
        assertFalse(source.trim().isEmpty());
//        out.println("source:\n"+source);

        Map<CLDevice, byte[]> binaries = program.getBinaries();
        assertFalse(binaries.isEmpty());

        int elementCount = 11444777;	// Length of float arrays to process (odd # for illustration)
        int localWorkSize = 256;      // set and log Global and Local work size dimensions
        int globalWorkSize = roundUp(localWorkSize, elementCount);  // rounded up to the nearest multiple of the LocalWorkSize

        out.println("allocateing buffers of size: "+globalWorkSize);

        ByteBuffer srcA = newDirectByteBuffer(globalWorkSize*SIZEOF_INT);
        ByteBuffer srcB = newDirectByteBuffer(globalWorkSize*SIZEOF_INT);
        ByteBuffer dest = newDirectByteBuffer(globalWorkSize*SIZEOF_INT);

        fillBuffer(srcA, 23456);
        fillBuffer(srcB, 46987);

        CLBuffer<ByteBuffer> clBufferA = context.createBuffer(srcA, Mem.READ_ONLY);
        CLBuffer<ByteBuffer> clBufferB = context.createBuffer(srcB, Mem.READ_ONLY);
        CLBuffer<ByteBuffer> clBufferC = context.createBuffer(dest, Mem.WRITE_ONLY);

        Map<String, CLKernel> kernels = program.createCLKernels();
        for (CLKernel kernel : kernels.values()) {
            out.println("kernel: "+kernel.toString());
        }

        assertNotNull(kernels.get("VectorAddGM"));
        assertNotNull(kernels.get("Test"));

        CLKernel vectorAddKernel = kernels.get("VectorAddGM");

        vectorAddKernel.setArg(0, clBufferA)
                       .setArg(1, clBufferB)
                       .setArg(2, clBufferC)
                       .setArg(3, elementCount);

        CLCommandQueue queue = programDevices[0].createCommandQueue();

        // Asynchronous write of data to GPU device, blocking read later
        queue.putWriteBuffer(clBufferA, false)
             .putWriteBuffer(clBufferB, false)
             .put1DRangeKernel(vectorAddKernel, 0, globalWorkSize, localWorkSize)
             .putReadBuffer(clBufferC, true)
             .finish().release();

        out.println("a+b=c result snapshot: ");
        for(int i = 0; i < 10; i++)
            out.print(dest.getInt()+", ");
        out.println("...; "+dest.remaining()/SIZEOF_INT + " more");

        assertTrue(3 == context.getCLMemoryObjects().size());
        clBufferA.release();
        assertTrue(2 == context.getCLMemoryObjects().size());

        assertTrue(2 == context.getCLMemoryObjects().size());
        clBufferB.release();
        assertTrue(1 == context.getCLMemoryObjects().size());

        assertTrue(1 == context.getCLMemoryObjects().size());
        clBufferC.release();
        assertTrue(0 == context.getCLMemoryObjects().size());


        assertTrue(1 == context.getCLPrograms().size());
        program.release();
        assertTrue(0 == context.getCLPrograms().size());

        context.release();
    }

    
}
