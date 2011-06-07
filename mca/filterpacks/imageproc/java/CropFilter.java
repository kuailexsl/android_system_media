/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package android.filterpacks.imageproc;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.FilterParameter;
import android.filterfw.core.Frame;
import android.filterfw.core.FrameFormat;
import android.filterfw.core.KeyValueMap;
import android.filterfw.core.MutableFrameFormat;
import android.filterfw.core.NativeProgram;
import android.filterfw.core.NativeFrame;
import android.filterfw.core.Program;
import android.filterfw.core.ShaderProgram;
import android.filterfw.geometry.Point;
import android.filterfw.geometry.Quad;
import android.filterfw.format.ImageFormat;

import android.util.Log;

public class CropFilter extends Filter {

    private ShaderProgram mProgram;

    @FilterParameter(name = "owidth", isOptional = true, isUpdatable = true)
    private int mOutputWidth = -1;

    @FilterParameter(name = "oheight", isOptional = true, isUpdatable = true)
    private int mOutputHeight = -1;

    public CropFilter(String name) {
        super(name);
    }

    public String[] getInputNames() {
        return new String[] { "image", "box" };
    }

    public String[] getOutputNames() {
        return new String[] { "image" };
    }

    // TODO: Support non-GPU image frames
    public boolean acceptsInputFormat(int index, FrameFormat format) {
        switch(index) {
            case 0: { // image
                FrameFormat requiredFormat = ImageFormat.create(ImageFormat.COLORSPACE_RGBA,
                                                                FrameFormat.TARGET_GPU);
                return format.isCompatibleWith(requiredFormat);
            }

            case 1: // box
                return (format.getTarget() == FrameFormat.TARGET_JAVA &&
                        format.getBaseType() == FrameFormat.TYPE_OBJECT &&
                        Quad.class.isAssignableFrom(format.getObjectClass()));
        }
        return false;
    }

    public FrameFormat getOutputFormat(int index) {
        return getInputFormat(0);
    }

    public void prepare(FilterContext env) {
        mProgram = ShaderProgram.createIdentity();
    }

    public int process(FilterContext env) {
        // Get input frame
        Frame imageFrame = pullInput(0);
        Frame boxFrame = pullInput(1);

        // Get the box
        Quad box = (Quad)boxFrame.getObjectValue();

        // Create output format
        MutableFrameFormat outputFormat = imageFrame.getFormat().mutableCopy();
        outputFormat.setDimensions(mOutputWidth == -1 ? outputFormat.getWidth() : mOutputWidth,
                                   mOutputHeight == -1 ? outputFormat.getHeight() : mOutputHeight);

        // Create output frame
        Frame output = env.getFrameManager().newFrame(outputFormat);

        mProgram.setSourceRegion(box);
        mProgram.process(imageFrame, output);

        // Push output
        putOutput(0, output);

        // Release pushed frame
        output.release();

        // Wait for next input and free output
        return Filter.STATUS_WAIT_FOR_ALL_INPUTS |
               Filter.STATUS_WAIT_FOR_FREE_OUTPUTS;
    }


}
