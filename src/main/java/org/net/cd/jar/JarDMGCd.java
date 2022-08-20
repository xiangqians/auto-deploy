package org.net.cd.jar;

import org.net.cd.Cd;
import org.net.cd.source.GitSource;
import org.net.cd.source.MavenSource;

import java.io.IOException;

/**
 * Jar
 * Docker
 * Maven
 * Git
 *
 * @author xiangqian
 * @date 01:15 2022/08/21
 */
public class JarDMGCd extends JarDockerCd {

    private GitSource gitSource;
    private MavenSource mavenSource;

    private JarDMGCd() {
        super();
    }

    @Override
    public void execute() throws Exception {
    }

    @Override
    public void close() throws IOException {
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends JarDockerCd.Builder {
        private GitSource gitSource;

        private Builder() {
        }

        public Builder gitSource(GitSource gitSource) {
            this.gitSource = gitSource;
            return this;
        }

        public JarDMGCd build() throws Exception {
            return null;
        }

    }


}
