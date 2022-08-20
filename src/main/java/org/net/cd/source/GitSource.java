package org.net.cd.source;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.net.cd.AbstractCd;
import org.net.util.Assert;
import org.net.util.DateUtils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;

/**
 * @author xiangqian
 * @date 12:10 2022/08/20
 */
@Slf4j
public class GitSource implements Source {

    private CredentialsProvider credentialsProvider;
    private String repoUrl;
    private String branch;


    private File tempRepoDirFile;

    private Git git;

    private String commitId;

    private GitSource(String username, String password, String repoUrl, String branch) {
        this.credentialsProvider = ObjectUtils.allNotNull(username, password) ? new UsernamePasswordCredentialsProvider(username, password) : null;
        this.repoUrl = repoUrl;
        this.branch = branch;
    }

    @Override
    public synchronized File[] get() throws Exception {
        if (Objects.nonNull(tempRepoDirFile)) {
            return new File[]{tempRepoDirFile};
        }

        // 获取临时目录，用于存放仓库文件
        String tempRepoDir = FileUtils.getTempDirectoryPath() + File.separator + "temp_" + UUID.randomUUID().toString().replace("-", "");
        log.debug("tempDirPath: {}", tempRepoDir);
        tempRepoDirFile = new File(tempRepoDir);

        // clone
        log.debug("准备从git上clone代码到本地 ...");
        git = Git.cloneRepository()
                .setCredentialsProvider(credentialsProvider)
                .setURI(repoUrl)
                .setBranch(branch)
                .setDirectory(tempRepoDirFile)
                .call();
        log.debug("git: {}", git);
        log.debug("已从git上clone代码到本地! ({})", tempRepoDirFile.getAbsolutePath());

        return new File[]{tempRepoDirFile};
    }

    /**
     * @return 是否是最新代码
     */
    public synchronized boolean pull() throws Exception {
        // repo
        Repository localRepo = git.getRepository();

        // pull
        PullCommand pullCommand = git.pull();
        pullCommand.setCredentialsProvider(credentialsProvider);
        pullCommand.setRemoteBranchName(branch);
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
                commitId = revCommit.getName();
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

    @Override
    public void close() throws IOException {
        AbstractCd.closeQuietly(git);
        if (Objects.nonNull(tempRepoDirFile)) {
            FileUtils.forceDelete(tempRepoDirFile);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String username;
        private String password;
        private String repoUrl;
        private String branch;

        private Builder() {
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder repoUrl(String repoUrl) {
            this.repoUrl = repoUrl;
            return this;
        }

        public Builder branch(String branch) {
            this.branch = branch;
            return this;
        }

        public GitSource build() throws Exception {
            Assert.notNull(repoUrl, "repoUrl不能为空");
            Assert.notNull(branch, "branch不能为空");
            return new GitSource(username, password, repoUrl, branch);
        }

    }

}
