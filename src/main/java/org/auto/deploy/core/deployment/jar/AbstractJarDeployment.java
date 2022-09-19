package org.auto.deploy.core.deployment.jar;

import lombok.extern.slf4j.Slf4j;
import org.auto.deploy.core.server.Server;
import org.auto.deploy.core.deployment.AbstractDeployment;
import org.auto.deploy.core.source.Source;

import java.io.File;

/**
 * @author xiangqian
 * @date 20:33 2022/09/10
 */
@Slf4j
public class AbstractJarDeployment extends AbstractDeployment {

    protected Source source;

    // jar file
    private File pkgFile;

    // addl files
    private File[] addlFiles;

    // script file
    private File[] scriptFiles;

    // files
    private File[] files;

    public AbstractJarDeployment(Server server, Source source) {
        super(server);
        this.source = source;
    }

    @Override
    protected File[] getFiles() {
        return new File[0];
    }

    @Override
    protected void init() throws Exception {

    }

//    @Override
//    protected void afterPost() throws Exception {
//        chmodX(scriptFiles);
//        log.debug("准备启动java应用 ...\n\t{}", jarFile.getName());
//        server.executeCmd("./startup.sh");
//        log.debug("已启动java应用!\n\t{}", jarFile.getName());
//    }
//
//    @Override
//    protected File[] getFiles() {
//        List<File> sourceFiles = new ArrayList<>(16);
//        sourceFiles.add(jarFile);
//        sourceFiles.addAll(addlFiles);
//        sourceFiles.addAll(Arrays.stream(scriptFiles).collect(Collectors.toList()));
////        return sourceFiles;
//        return null;
//    }
//
//    @Override
//    protected final void init() throws Exception {
//        // jarFile
//        if (Objects.nonNull(getPkgName())) {
//            jarFile = Path.of(source.get().getAbsolutePath(), "target", String.format("%s.jar", getPkgName())).toFile();
//        }
//        // 解析jar文件
//        else {
//            File file = Path.of(source.get().getAbsolutePath(), "target").toFile();
//            Assert.isTrue(Objects.nonNull(file) && file.exists(), String.format("target文件夹不存在: %s", file.getAbsolutePath()));
//            log.debug("targetFile: {}", file.getAbsolutePath());
//
//            File[] jarFiles = file.listFiles(pathname -> pathname.isFile() && pathname.getName().endsWith(".jar"));
//            Assert.isTrue(ArrayUtils.isNotEmpty(jarFiles), "无法获取target目录下的jar文件");
//            for (File jf : jarFiles) {
//                if (Objects.isNull(jarFile)) {
//                    jarFile = jf;
//                    continue;
//                }
//
//                if (jarFile.length() < jf.length()) {
//                    jarFile = jf;
//                }
//            }
//            log.debug("选举jar: {}", jarFile.getAbsolutePath());
//        }
//        Assert.isTrue(Objects.nonNull(jarFile) && jarFile.exists(), String.format("pkg文件不存在: %s", jarFile.getAbsolutePath()));
//        log.debug("jarFile: {}", jarFile.getAbsolutePath());
//
//        // addl-files
//        if (CollectionUtils.isNotEmpty(getAddlFiles())) {
//            addlFiles = new ArrayList<>(getAddlFiles().size());
//            for (String addlFilePath : getAddlFiles()) {
//                File addlFile = null;
//                // 绝对路径
//                if (addlFilePath.contains(":") || addlFilePath.startsWith("/")) {
//                    addlFile = new File(addlFilePath);
//                }
//                // 相对路径
//                // 将相对路径修改为绝对路径
//                else {
//                    addlFile = Path.of(source.get().getAbsolutePath(), addlFilePath).toFile();
//                }
//
//                // check && add
//                Assert.isTrue(addlFile.exists(), String.format("附加文件或目录不存在: %s", addlFilePath));
//                addlFiles.add(addlFile);
//            }
//            log.debug("addlFiles: {}", addlFiles);
//        }
//
//        // 获取脚本文件
//        this.scriptFiles = getFilesOnClasspathByFilePaths(getScriptFilePaths());
//
//        // init后置处理器
//        initAfterPost();
//    }
//
//    protected abstract void initAfterPost() throws Exception;
//
//    protected abstract String[] getScriptFilePaths();
//
//    protected abstract String getPkgName();
//
//    protected abstract List<String> getAddlFiles();

}
