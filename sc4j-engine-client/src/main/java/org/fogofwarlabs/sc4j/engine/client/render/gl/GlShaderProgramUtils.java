package org.fogofwarlabs.sc4j.engine.client.render.gl;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.base.exception.IgnoreException;
import org.jetbrains.annotations.NotNull;

import static org.lwjgl.opengl.GL33C.*;

/**
 * Compiles and links the OpenGL shader programs of the renderers. A failing step deletes the shader objects and the program it already created, so
 * that a failed compilation or link leaves no OpenGL object behind.
 */
public final class GlShaderProgramUtils {
    /**
     * Compiles a vertex and a fragment shader on the current OpenGL context and links them into a program. The shader objects are deleted once the
     * program is linked.
     *
     * @param name           the renderer name that the failure messages start with
     * @param vertexSource   the vertex shader source
     * @param fragmentSource the fragment shader source
     * @return the linked program
     * @throws IllegalStateException if a shader does not compile or the program does not link
     */
    public static int linkProgram(@NotNull String name, @NotNull String vertexSource, @NotNull String fragmentSource) {
        int vertexShader = compileShader(name, GL_VERTEX_SHADER, vertexSource);

        try {
            int fragmentShader = compileShader(name, GL_FRAGMENT_SHADER, fragmentSource);

            try {
                int program = glCreateProgram();

                try {
                    glAttachShader(program, vertexShader);
                    glAttachShader(program, fragmentShader);
                    glLinkProgram(program);

                    if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
                        String message = glGetProgramInfoLog(program);

                        throw Exceptions.newIllegalStateException("{0} shader linking failed: {1}", name, message);
                    }

                    return program;
                } catch (RuntimeException exception) {
                    IgnoreException.execute(
                            () -> glDeleteProgram(program)
                    );

                    throw exception;
                }
            } finally {
                IgnoreException.execute(
                        () -> glDeleteShader(fragmentShader)
                );
            }
        } finally {
            IgnoreException.execute(
                    () -> glDeleteShader(vertexShader)
            );
        }
    }

    private static int compileShader(@NotNull String name, int type, @NotNull String source) {
        int shader = glCreateShader(type);

        try {
            glShaderSource(shader, source);
            glCompileShader(shader);

            if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
                String message = glGetShaderInfoLog(shader);

                throw Exceptions.newIllegalStateException("{0} shader compilation failed: {1}", name, message);
            }

            return shader;
        } catch (RuntimeException exception) {
            IgnoreException.execute(
                    () -> glDeleteShader(shader)
            );

            throw exception;
        }
    }

    private GlShaderProgramUtils() {
    }
}
