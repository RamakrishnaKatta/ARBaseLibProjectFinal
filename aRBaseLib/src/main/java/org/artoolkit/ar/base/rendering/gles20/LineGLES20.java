package org.artoolkit.ar.base.rendering.gles20;

import org.artoolkit.ar.base.rendering.Line;

public class LineGLES20 extends Line implements ARDrawableOpenGLES20 {

    private ShaderProgram shaderProgram;

    /**
     * @param width Width of the line
     */
    public LineGLES20(float width) {
        shaderProgram = null;
        this.setWidth(width);
    }

    public LineGLES20(float width, ShaderProgram shaderProgram) {
        this(width);
        this.shaderProgram = shaderProgram;
    }

    @Override
    /**
     * Used to render objects when working with OpenGL ES 2.x
     *
     * @param projectionMatrix The projection matrix obtained from the ARToolkit
     * @param modelViewMatrix  The marker transformation matrix obtained from ARToolkit
     */
    public void draw(float[] projectionMatrix, float[] modelViewMatrix) {

        shaderProgram.setProjectionMatrix(projectionMatrix);
        shaderProgram.setModelViewMatrix(modelViewMatrix);

        this.setArrays();
        shaderProgram.render(this.getMVertexBuffer(), this.getmColorBuffer(), null);

    }

    @Override
    /**
     * Sets the shader program used by this geometry.
     */
    public void setShaderProgram(ShaderProgram program) {
        this.shaderProgram = program;
    }
}
