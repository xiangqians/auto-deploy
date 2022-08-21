package org.net.cd.jar;

import org.net.cd.AbstractCd;
import org.net.cd.source.GitSource;
import org.net.cd.source.MavenSource;
import org.net.cd.source.Source;
import org.net.util.Assert;

import java.io.File;

/**
 * Jar
 * Docker
 * Maven
 * Git
 *
 * @author xiangqian
 * @date 01:15 2022/08/21
 */
public class JarGitCd extends AbstractCd {

    private GitSource gitSource;
    private MavenSource.Builder mavenSourceBuilder;
    private JarDockerCd.Builder jarDockerCdBuilder;

    private JarGitCd() {
        super();
    }

    @Override
    protected File[] getFilesToBeCompressed() {
        return new File[0];
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends AbstractCd.Builder<Builder, JarGitCd> {
        private JarDockerCd.DockerBuild<Builder> dockerBuild;
        private JarDockerCd.DockerRun<Builder> dockerRun;

        private String mavenHome;
        private String[] filePaths;
        private MavenSource.Mvn<Builder> mvn;

        private Builder() {
            this.dockerBuild = new JarDockerCd.DockerBuild(this);
            this.dockerRun = new JarDockerCd.DockerRun(this);
            this.mvn = new MavenSource.Mvn<>(this);
        }

        public JarDockerCd.DockerBuild<Builder> dockerBuild() {
            return dockerBuild;
        }

        public JarDockerCd.DockerRun<Builder> dockerRun() {
            return dockerRun;
        }

        public Builder mavenHome(String mavenHome) {
            this.mavenHome = mavenHome;
            return this;
        }

        public Builder filePaths(String... filePaths) {
            this.filePaths = filePaths;
            return this;
        }

        public MavenSource.Mvn<Builder> mvn() {
            return mvn;
        }

        @Override
        public Builder source(Source source) {
            Assert.isTrue(source instanceof GitSource, "source必须是GitSource的实现类");
            return super.source(source);
        }

        @Override
        public JarGitCd build() throws Exception {
            Assert.isTrue(false, "目前还未实现此功能!");
            return super.build();
        }
    }

}
