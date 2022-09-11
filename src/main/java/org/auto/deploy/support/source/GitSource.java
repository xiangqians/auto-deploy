package org.auto.deploy.support.source;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.auto.deploy.config.source.GitSourceConfig;
import org.auto.deploy.util.DateUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
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
import java.util.concurrent.TimeUnit;

/**
 * git资源
 *
 * @author xiangqian
 * @date 01:29 2022/09/10
 */
@Slf4j
public class GitSource implements Source {

    private GitSourceConfig config;

    private volatile File tempFile;

    private CredentialsProvider credentialsProvider;
    private Git git;
    private String commitId;

    public GitSource(GitSourceConfig config) {
        config.validate();
        this.config = config;
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

        log.debug("已从git上clone代码到本地!\n\t{}", tempFile.getAbsolutePath());
        return tempFile;
    }

    /**
     * @return 是否是最新代码
     */
    private synchronized boolean pull() throws Exception {
        // repo
        Repository localRepo = git.getRepository();

        // pull
        PullCommand pullCommand = git.pull();
        pullCommand.setCredentialsProvider(credentialsProvider);
        pullCommand.setRemoteBranchName(config.getBranch());
        pullCommand.call();

        // log
        RevWalk revWalk = new RevWalk(localRepo);
        for (Ref ref : localRepo.getAllRefs().values()) {
            revWalk.markStart(revWalk.parseCommit(ref.getObjectId()));
        }
        Iterator<RevCommit> iterator = revWalk.iterator();
        if (iterator.hasNext()) {
            RevCommit revCommit = iterator.next();
            if (Objects.isNull(commitId) || commitId.equals(revCommit.getName())) {
                if (Objects.isNull(commitId)) {
                    commitId = revCommit.getName();
                }
                return false;
            }

            log.debug("最新一次提交信息: {}" +
                            "\n\t提交时间:\t{}" +
                            "\n\t提交者标识:\t{}" +
                            "\n\t作者身份:\t{}" +
                            "\n\t提交信息:\t{}",
                    revCommit.getId(),
                    DateUtils.format(DateUtils.timestampToLocalDateTime(revCommit.getCommitTime() * 1000L)),
                    revCommit.getCommitterIdent(),
                    revCommit.getAuthorIdent(),
                    revCommit.getFullMessage());
            commitId = revCommit.getName();
            return true;
        }
        return false;
    }

    /**
     * 下一版本支持 GitHook
     *
     * @param notice
     * @throws Exception
     */
    public synchronized void listen(Notice notice) throws Exception {
        if (config.getPollTimer() == 0) {
            notice.on();
            return;
        }

        while (true) {
            TimeUnit.SECONDS.sleep(config.getPollTimer());
            if (pull()) {
                notice.on();
            }
        }
    }

    public static interface Notice {
        void on() throws Exception;
    }

    @Override
    public void close() throws IOException {
        log.debug("删除git资源临时目录! \n\t{}", tempFile.getAbsolutePath());
        try {
            git.close();
        } catch (Exception e) {
        }
        FileUtils.forceDelete(tempFile);
    }

}
