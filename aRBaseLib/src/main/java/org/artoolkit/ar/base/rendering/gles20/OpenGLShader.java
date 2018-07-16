package org.artoolkit.ar.base.rendering.gles20;

/**
 * Created by Thorsten Bux on 21.01.2016.
 * <p/>
 * Provides an interface that ensures the basic shader methods and information in a shader implementation
 * are provided.
 */
public interface OpenGLShader {

    //These properties are used to make the connection between the code and the shader. We use them
    //to link the projection and model matrix to the shader and to pass these matrices to the shader
    //from the AR application.
    String projectionMatrixString = "u_projection";
    String modelViewMatrixString = "u_modelView";
    //Also used to provide a link to the shader program. In this case we pass in the position vectors from the
    //AR application to the shader.
    String positionVectorString = "a_Position";

    public int configureShader();

    public void setShaderSource(String source);

}
