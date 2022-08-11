# FROM命令定义构建镜像的基础镜像
FROM openjdk:13

# 以root执行
USER root

# 自定义镜像的时间格式与时区
#RUN cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo 'Asia/Shanghai' > /etc/timezone
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo '$TZ' > /etc/timezone

# 创建java应用目录
RUN mkdir -p /opt/application

# 将多个文件copy到 /tmp/目录下
#COPY logback.xml xxx.jar /tmp/
COPY ${FILE_NAMES} /opt/application/

# 暴露端口
#EXPOSE 8080

# 容器入口
# 设置JVM栈内存、最小堆内存、最大堆内存
ENTRYPOINT ["java","-Xss4096K","-Xms512M","-Xmx512M","-jar","/opt/application/${JAR_NAME}"]
