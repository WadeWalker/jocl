package com.mbien.opencl;

import java.nio.Buffer;

import static com.mbien.opencl.CL.*;
import static com.mbien.opencl.CLException.*;

/**
 *
 * @author Michael Bien
 */
public class CLEvent implements CLResource {

    public final CLContext context;
    public final long ID;

    private final CL cl;

    private final CLEventInfoAccessor eventInfo;

    CLEvent(CLContext context, long id) {
        this.context = context;
        this.cl = context.cl;
        this.ID = id;
        this.eventInfo = new CLEventInfoAccessor();
    }

    public void release() {
        int ret = cl.clReleaseEvent(ID);
        checkForError(ret, "can not release event");
    }

    public ExecutionStatus getStatus() {
        int status = (int)eventInfo.getLong(CL_EVENT_COMMAND_EXECUTION_STATUS);
        return ExecutionStatus.valueOf(status);
    }

    public CommandType getType() {
        int status = (int)eventInfo.getLong(CL_EVENT_COMMAND_TYPE);
        return CommandType.valueOf(status);
    }

    @Override
    public String toString() {
        return "CLEvent [id: " + ID
                      + " name: " + getType()
                      + " status: " + getStatus()+"]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CLEvent other = (CLEvent) obj;
        if (this.context != other.context && (this.context == null || !this.context.equals(other.context))) {
            return false;
        }
        if (this.ID != other.ID) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 13 * hash + (this.context != null ? this.context.hashCode() : 0);
        hash = 13 * hash + (int) (this.ID ^ (this.ID >>> 32));
        return hash;
    }

    

    private class CLEventInfoAccessor extends CLInfoAccessor {

        @Override
        protected int getInfo(int name, long valueSize, Buffer value, long[] valueSizeRet, int valueSizeRetOffset) {
            return cl.clGetEventInfo(ID, name, valueSize, value, valueSizeRet, valueSizeRetOffset);
        }

    }

    public enum ExecutionStatus {

        /**
         * Command has been enqueued in the command-queue.
         */
        QUEUED(CL_QUEUED),

        /**
         * Enqueued command has been submitted by the host to the device
         * associated with the command-queue.
         */
        SUBMITTED(CL_SUBMITTED),
        
        /**
         * Device is currently executing this command.
         */
        RUNNING(CL_RUNNING),

        /**
         * The command has completed.
         */
        COMPLETE(CL_COMPLETE);


        /**
         * Value of wrapped OpenCL command excecution status.
         */
        public final int STATUS;

        private ExecutionStatus(int status) {
            this.STATUS = status;
        }

        public static ExecutionStatus valueOf(int status) {
            switch(status) {
                case(CL_QUEUED):
                    return QUEUED;
                case(CL_SUBMITTED):
                    return SUBMITTED;
                case(CL_RUNNING):
                    return RUNNING;
                case(CL_COMPLETE):
                    return COMPLETE;
            }
            return null;
        }
    }

    public enum CommandType {

        NDRANGE_KERNEL(CL_COMMAND_NDRANGE_KERNEL),
        TASK(CL_COMMAND_TASK),
        NATIVE_KERNEL(CL_COMMAND_NATIVE_KERNEL),
        READ_BUFFER(CL_COMMAND_READ_BUFFER),
        WRITE_BUFFER(CL_COMMAND_WRITE_BUFFER),
        COPY_BUFFER(CL_COMMAND_COPY_BUFFER),
        READ_IMAGE(CL_COMMAND_READ_IMAGE),
        WRITE_IMAGE(CL_COMMAND_WRITE_IMAGE),
        COPY_IMAGE(CL_COMMAND_COPY_IMAGE),
        COPY_BUFFER_TO_IMAGE(CL_COMMAND_COPY_BUFFER_TO_IMAGE),
        COPY_IMAGE_TO_BUFFER(CL_COMMAND_COPY_IMAGE_TO_BUFFER),
        MAP_BUFFER(CL_COMMAND_MAP_BUFFER),
        MAP_IMAGE(CL_COMMAND_MAP_IMAGE),
        UNMAP_MEM_OBJECT(CL_COMMAND_UNMAP_MEM_OBJECT),
        MARKER(CL_COMMAND_MARKER),
        ACQUIRE_GL_OBJECTS(CL_COMMAND_ACQUIRE_GL_OBJECTS),
        RELEASE_GL_OBJECTS(CL_COMMAND_RELEASE_GL_OBJECTS);

        /**
         * Value of wrapped OpenCL command type.
         */
        public final int TYPE;

        private CommandType(int type) {
            this.TYPE = type;
        }

        public static CommandType valueOf(int commandType) {
            CommandType[] values = CommandType.values();
            for (CommandType value : values) {
                if(value.TYPE == commandType)
                    return value;
            }
            return null;
        }

    }

}
