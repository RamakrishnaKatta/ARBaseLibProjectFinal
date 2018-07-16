package org.artoolkit.ar.base.rendering.gles20;

import android.opengl.GLES20;

/**
 * Here you define your fragment shader and what it does with the given color.
 * <p/>
 * This class also provides the implementation of the {@link #configureShader()} method. So all you need to do is
 * call this one from your fragment shader implementation.
 */
public class BaseFragmentShader implements OpenGLShader {
    String fragmentShader =
            "precision lowp float; \n"     // Set the default precision to medium. We don't need as high of a
                    // precision in the fragment shader.
                    + "void main() \n"     // The entry point for our fragment shader.
                    + "{ \n"
                    + "} \n";

    public int configureShader() {

        int fragmentShaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        String fragmentShaderErrorLog = "";

        if (fragmentShaderHandle != 0) {

            //Pass in the shader source
            GLES20.glShaderSource(fragmentShaderHandle, fragmentShader);

            //Compile the shader
            GLES20.glCompileShader(fragmentShaderHandle);

            //Get the compilation status.
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(fragmentShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            //If the compilation failed, delete the shader
            if (compileStatus[0] == 0) {
                fragmentShaderErrorLog = GLES20.glGetShaderInfoLog(fragmentShaderHandle);
                GLES20.glDeleteShader(fragmentShaderHandle);
                fragmentShaderHandle = 0;
            }
        }
        if (fragmentShaderHandle == 0) {
            throw new RuntimeException("Error creating fragment shader.\\n" + fragmentShaderErrorLog);
        }
        return fragmentShaderHandle;
    }

    @Override
    public void setShaderSource(String source) {
        this.fragmentShader = source;
    }
}
