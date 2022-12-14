package org.auto.deploy.core.source;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.auto.deploy.util.Assert;
import org.auto.deploy.util.DateUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;

/**
 * git资源
 *
 * @author xiangqian
 * @date 01:29 2022/09/10
 */
@Slf4j
public class GitSource implements Source {

    private Config config;

    private volatile File tempFile;

    private CredentialsProvider credentialsProvider;
    private Git git;
    private String commitId;
    private RevCommit lastRevCommit;

    public GitSource(Config config) {
        this.config = config;
    }

    @SneakyThrows
    @Override
    public boolean isChanged() {
        return pull();
    }

    @Override
    public synchronized File get() throws Exception {
        if (Objects.nonNull(tempFile)) {
            return tempFile;
        }

        // 获取临时目录，用于存放git资源
        tempFile = Path.of(FileUtils.getTempDirectoryPath(), String.format("temp_%s", UUID.randomUUID().toString().replace("-", ""))).toFile();
        log.debug("准备从git上clone代码到本地 ...\n\t{}", tempFile.getAbsolutePath());

        // CredentialsProvider
        if (ObjectUtils.allNotNull(config.getUsername(), config.getPassword())) {
            credentialsProvider = new UsernamePasswordCredentialsProvider(config.getUsername(), config.getPassword());
            log.debug("加载凭证提供者!\n\t{}", credentialsProvider);
        }

        // clone
        git = Git.cloneRepository()
                .setCredentialsProvider(credentialsProvider)
                .setURI(config.getRepoUrl())
                .setBranch(config.getBranch())
                .setDirectory(tempFile)
                .call();

        // lastRevCommit
        lastRevCommit = getLastRevCommit(git);
        log.debug("最新一次提交信息: {}", getRevCommitStr(lastRevCommit));

        log.debug("已从git上clone代码到本地!\n\t{}", tempFile.getAbsolutePath());
        return tempFile;
    }

    /**
     * pull
     *
     * @return 是否是最新代码
     */
    public synchronized boolean pull() throws IOException, GitAPIException {
        // repo
        Repository localRepo = git.getRepository();

        // pull
        PullCommand pullCommand = git.pull();
        pullCommand.setCredentialsProvider(credentialsProvider);
        pullCommand.setRemoteBranchName(config.getBranch());
        pullCommand.call();

        lastRevCommit = getLastRevCommit(git);
        if (Objects.isNull(lastRevCommit)) {
            return false;
        }

        if (Objects.isNull(commitId) || commitId.equals(lastRevCommit.getName())) {
            commitId = lastRevCommit.getName();
            return false;
        }

        log.debug("最新一次提交信息: {}", getRevCommitStr(lastRevCommit));

        commitId = lastRevCommit.getName();
        return true;
    }

    public String getLastRevCommitStr() {
        return getRevCommitStr(lastRevCommit);
    }

    private static String getRevCommitStr(RevCommit revCommit) {
        if (Objects.isNull(revCommit)) {
            return null;
        }

        StringBuilder lastRevCommitStrBuilder = new StringBuilder();
        lastRevCommitStrBuilder.append("\n\t").append("id: ").append("\t").append(revCommit.getId());
        lastRevCommitStrBuilder.append("\n\t").append("提交时间: ").append("\t").append(DateUtils.format(DateUtils.timestampToLocalDateTime(revCommit.getCommitTime() * 1000L)));
        lastRevCommitStrBuilder.append("\n\t").append("提交者标识: ").append("\t").append(revCommit.getCommitterIdent());
        lastRevCommitStrBuilder.append("\n\t").append("作者身份: ").append("\t").append(revCommit.getAuthorIdent());
        lastRevCommitStrBuilder.append("\n\t").append("提交信息: ").append("\t").append(revCommit.getFullMessage());
        return lastRevCommitStrBuilder.toString();
    }

    /**
     * 最近一次提交信息
     *
     * @return
     */
    private static RevCommit getLastRevCommit(Git git) throws IOException {
        Repository localRepo = git.getRepository();

        // log
        RevWalk revWalk = new RevWalk(localRepo);
        for (Ref ref : localRepo.getAllRefs().values()) {
            revWalk.markStart(revWalk.parseCommit(ref.getObjectId()));
        }
        Iterator<RevCommit> iterator = revWalk.iterator();
        if (iterator.hasNext()) {
            RevCommit revCommit = iterator.next();
            return revCommit;
        }

        return null;
    }


//    /**
//     * 下一版本支持 GitHook
//     *
//     * @param notice
//     * @throws Exception
//     */
//    public synchronized void listen(Notice notice) throws Exception {
//        if (config.getPollTimer() == 0) {
//            notice.on();
//            return;
//        }
//
//        // init
//        get();
//
//        // poll
//        while (listenFlag) {
//            try {
//                TimeUnit.SECONDS.sleep(config.getPollTimer());
//            } catch (InterruptedException e) {
//                log.error("主线程被中断了", e);
//            }
//            if (listenFlag && pull() && listenFlag) {
//                notice.on();
//            }
//        }
////    }
//
//    public static interface Notice {
//        void on() throws Exception;
//    }

    @Override
    public void close() throws IOException {
        log.debug("删除git资源临时目录! \n\t{}", tempFile.getAbsolutePath());
        try {
            git.close();
        } catch (Exception e) {
        }
        FileUtils.forceDelete(tempFile);
    }


    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Config {

        private String username;
        private String password;

        @JsonProperty("repo-url")
        private String repoUrl;
        private String branch;

        /**
         * 钩子
         */
        private String hook;

        /**
         * cron定时执行表达式
         * 每30s执行任务:
         * 0/30 * * * * ?
         */
        private String cron;

        public void validate() {
            Assert.notNull(repoUrl, "source.git.repo-url不能为null");
            Assert.notNull(branch, "source.git.branch不能为null");
        }

    }

}
