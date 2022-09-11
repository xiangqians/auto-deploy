package org.auto.deploy.sftp.impl;

import com.jcraft.jsch.SftpProgressMonitor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Map;

/**
 * public static void main(String[] args){}
 * 只能在main方法使用才会显示进度条，@Test运行无法显示
 *
 * @author xiangqian
 * @date 00:38 2022/07/26
 */
public class DefaultSftpProgressMonitor implements SftpProgressMonitor {

    private static final Map<Integer, String> OP_MAP = Map.of(PUT, "PUT", GET, "GET");
    private long totalSize;
    private long transferredSize;
    private DecimalFormat format;

    private DefaultSftpProgressMonitor() {
    }

    @Override
    public void init(int op, String src, String dest, long max) {
        System.out.println("Start transferring files ...");
        format = new DecimalFormat("###,###,###.##");
        totalSize = max;
        System.out.format("[%s] %s (%s KB = %s MB) -> %s",
                OP_MAP.get(op),
                src,
                format.format(new BigDecimal(String.valueOf(totalSize)).divide(new BigDecimal(String.valueOf(1024))).setScale(2, RoundingMode.DOWN).floatValue()),
                format.format(new BigDecimal(String.valueOf(totalSize)).divide(new BigDecimal(String.valueOf(1024 * 1024))).setScale(2, RoundingMode.DOWN).floatValue()),
                dest).println();
    }

    @Override
    public boolean count(long count) {
        transferredSize += count;
        System.out.print('\r');
        String temp = String.format("%s bytes have currently been transferred. (%s bytes in total)",
                format.format(transferredSize),
                format.format(totalSize));
        System.out.print(temp);
        if (transferredSize >= totalSize) {
            System.out.println();
        }
        return true;
    }

    @Override
    public void end() {
        System.out.println("File transfer ended.\n");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Builder() {
        }

        public DefaultSftpProgressMonitor build() {
            return new DefaultSftpProgressMonitor();
        }
    }

}
