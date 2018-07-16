package org.artoolkit.ar.base.rendering.gles20;

/**
 * Interface should be used for all classed that paint some geometry in the space.
 * Like {@link CubeGLES20} or {@link LineGLES20}
 * It ensures that the needed methods {@link #draw(float[], float[])} and {@link #setShaderProgram(ShaderProgram)}
 * are implemented by these classes.
 */
public interface ARDrawableOpenGLES20 {
    public void draw(float[] projectionMatrix, float[] modelViewMatrix);

    public void setShaderProgram(ShaderProgram program);
}
