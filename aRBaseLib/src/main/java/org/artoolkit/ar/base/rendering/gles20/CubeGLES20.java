package org.artoolkit.ar.base.rendering.gles20;

import org.artoolkit.ar.base.rendering.Cube;

public class CubeGLES20 extends Cube implements ARDrawableOpenGLES20 {

    private ShaderProgram shaderProgram;

    public CubeGLES20(ShaderProgram shaderProgram) {
        super();
        this.shaderProgram = shaderProgram;
    }

    public CubeGLES20(float size) {
        super(size);
    }

    public CubeGLES20(float size, float x, float y, float z) {
        super(size, x, y, z);
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

        shaderProgram.render(this.getmVertexBuffer(), this.getmColorBuffer(), this.getmIndexBuffer());

    }

    @Override
    /**
     * Sets the shader program used by this geometry.
     */
    public void setShaderProgram(ShaderProgram shaderProgram) {
        this.shaderProgram = shaderProgram;
    }
}
