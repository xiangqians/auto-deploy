# FROM命令定义构建镜像的基础镜像
FROM openjdk:13

# 以root执行
USER root

# 设置镜像的时间格式与时区
#RUN cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo 'Asia/Shanghai' > /etc/timezone
#RUN cp -f /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

# 创建java应用目录
RUN mkdir -p /opt/application

# 将多个文件copy到 /tmp/目录下
#COPY logback.xml xxx.jar /tmp/
#COPY ${FILE_NAMES} /opt/application/
# 将文件或文件夹拷贝到 /opt/application 目录下
JS "var files = ${DOCKER_FILES}; \
    for (var i = 0, length = files.length; i < length; i++) { \
        var file = files[i]; \
        var isDir = file.isDir; \
        var fileName = file.name; \
        if (isDir) { \
            out('COPY ' + fileName + ' /opt/application/' + fileName); \
        } else { \
            out('COPY ' + fileName + ' /opt/application/'); \
        } \
    };"

# 暴露端口
#EXPOSE 8080

# WORKDIR：设置工作目录，即cd命令
WORKDIR /opt/application

# 容器入口
# 设置JVM栈内存、最小堆内存、最大堆内存
ENTRYPOINT ["java","-Xss4096K","-Xms512M","-Xmx512M","-jar","/opt/application/${JAR_NAME}"]
