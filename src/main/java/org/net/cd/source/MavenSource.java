package org.net.cd.source;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.net.cd.AbstractCd;
import org.net.util.Assert;
import org.net.util.CmdUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author xiangqian
 * @date 00:15 2022/08/20
 */
@Slf4j
public class MavenSource implements Source {

    private String mavenHome;
    private File projectDirFile;
    private File[] sourceFiles;

    private Mvn mvn;

    private File tempProjectDirFile;

    private MavenSource(String mavenHome, File projectDirFile, File[] sourceFiles, Mvn mvn) {
        this.mavenHome = mavenHome;
        this.projectDirFile = projectDirFile;
        this.sourceFiles = sourceFiles;
        this.mvn = mvn;
    }

    @Override
    public File[] get() throws Exception {
        copy();
        clean();
        _package();
        return sourceFiles;
    }

    @Override
    public void close() throws IOException {
        if (Objects.nonNull(tempProjectDirFile)) {
            FileUtils.forceDelete(tempProjectDirFile);
        }
    }

    private void _package() throws IOException {
        log.debug("准备打包maven项目 ...");

        List<String> cmds = new ArrayList<>(16);
        cmds.add("cmd /c");
        cmds.add(String.format("cd /d %s%s%s", tempProjectDirFile.getAbsolutePath(), File.separator, projectDirFile.getName()));
        cmds.add("&&");

        // mvn
        if (Objects.nonNull(mavenHome)) {
            cmds.add(mavenHome + File.separator + "bin" + File.separator + "mvn");
        } else {
            cmds.add("mvn");
        }

        // -P 指定 Profile 配置，可以用于区分环境；
        if (Objects.nonNull(mvn.P())) {
            cmds.add(String.format("-P %s", mvn.P()));
        }

        cmds.add("package");

        String cmd = StringUtils.join(cmds, " ");
        log.debug("{}", cmd);
        List<String> resultList = new ArrayList<>();
        CmdUtils.execute(cmd, "GBK", result -> {
            System.out.println(result);
            resultList.add(result);
        });
        log.debug("已打包maven项目!");

        File jarFile = null;
        for (String result : resultList) {
            int index = -1;
            if ((index = result.indexOf("Building jar:")) > 0) {
                jarFile = new File(result.substring(index + "Building jar:".length()).trim());
                break;
            }
        }
        Assert.notNull(jarFile, "无法获取Maven打包后jar文件");
        Assert.isTrue(jarFile.exists(), "Maven打包后jar文件不存在");

        if (Objects.nonNull(sourceFiles)) {
            File[] newSourceFiles = new File[sourceFiles.length + 1];
            System.arraycopy(sourceFiles, 0, newSourceFiles, 0, sourceFiles.length);
            newSourceFiles[sourceFiles.length] = jarFile;
            sourceFiles = newSourceFiles;
        } else {
            sourceFiles = new File[]{jarFile};
        }
    }

    private void clean() throws IOException {
        log.debug("准备清除编译产生的target ...");

        List<String> cmds = new ArrayList<>(16);
        cmds.add("cmd /c");
        cmds.add(String.format("cd /d %s%s%s", tempProjectDirFile.getAbsolutePath(), File.separator, projectDirFile.getName()));
        cmds.add("&&");

        // mvn
        if (Objects.nonNull(mavenHome)) {
            cmds.add(mavenHome + File.separator + "bin" + File.separator + "mvn");
        } else {
            cmds.add("mvn");
        }

        cmds.add("clean");

        String cmd = StringUtils.join(cmds, " ");
        log.debug("{}", cmd);
        CmdUtils.execute(cmd, "GBK", System.out::println);
        log.debug("已清除编译产生的target!");
    }

    private void copy() throws IOException {
        log.debug("准备拷贝maven项目到临时目录下 ...");

        // 获取临时目录，用于存放maven项目
        String tempDirPath = FileUtils.getTempDirectoryPath() + File.separator + "temp_" + UUID.randomUUID().toString().replace("-", "");
        log.debug("tempDirPath: {}", tempDirPath);
        tempProjectDirFile = new File(tempDirPath);

        // 拷贝
        FileUtils.copyDirectoryToDirectory(projectDirFile, tempProjectDirFile);

        log.debug("已拷贝maven项目到临时目录下! (临时目录 {})", tempDirPath);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String mavenHome;
        private String projectDir;
        private String[] filePaths;
        private Mvn<Builder> mvn;

        private Builder() {
            mvn = new Mvn(this);
        }

        public Builder mavenHome(String mavenHome) {
            this.mavenHome = mavenHome;
            return this;
        }

        public Builder projectDir(String projectDir) {
            this.projectDir = projectDir;
            return this;
        }

        public Builder filePaths(String... filePaths) {
            this.filePaths = filePaths;
            return this;
        }

        public Mvn<Builder> mvn() {
            return mvn;
        }

        public MavenSource build() throws Exception {
            Assert.notNull(projectDir, "projectDir不能为空");
            File projectDirFile = AbstractCd.getFiles(projectDir)[0];
            File[] sourceFiles = null;
            if (Objects.nonNull(filePaths)) {
                for (int i = 0, length = filePaths.length; i < length; i++) {
                    String filePath = filePaths[i];
                    // 绝对路径
                    if (filePath.contains(":") || filePath.startsWith("/")) {
                        continue;
                    }
                    // 相对路径
                    // 将相对路径修改为绝对路径
                    filePaths[i] = projectDir + File.separator + filePath;
                }
                sourceFiles = AbstractCd.getFiles(filePaths);
            }
            return new MavenSource(mavenHome, projectDirFile, sourceFiles, mvn);
        }
    }

    public static class Mvn<B> {
        private B builder;
        private String P;

        public Mvn(B builder) {
            this.builder = builder;
        }

        private String P() {
            return P;
        }

        public Mvn<B> P(String P) {
            this.P = P;
            return this;
        }

        private void check() {
        }

        public B and() {
            return builder;
        }

    }

}