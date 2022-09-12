# 自动化部署

部署java、vue应用

# 自动化部署配置

```
服务器配置(server) --> 资源配置(source) --> 构建器配置(builder) --> 部署配置(deployment)
```

# config.yml

```yaml
# 服务器配置
server:
  # host
  host: debian
  # 端口
  port: 22
  # 用户名
  username: xiangqian
  # 密码
  password: xiangqian
  # session连接超时, 单位s
  session-conn-timeout: 60
  # channel连接超时, 单位s
  channel-conn-timeout: 60
  # 工作目录
  work-dir: test
  # 是否以sudo执行命令
  sudo: true


# 资源配置
source:
  # 资源类型, 可选值: [ local, git ]
  type: local

  # 本地资源配置
  local:
    # 本地资源位置
    location: /opt/auto-deploy
    # 是否使用临时工作空间
    # 如果设置为true, 则会创建一个临时目录来处理资源；
    # 如果设置为false, 则会在当前指定的位置处理资源；
    use-temp-workspace: true

  # git资源配置
  git:
    # git用户名
    username: xiangqian
    # git密码
    password: xiangqian
    # git仓库地址
    repo-url: https://github.com/xiangqians/auto-deploy
    # 分支名
    branch: master
    # 资源更新检测轮询计时器, 单位s
    poll-timer: 30


# 构建器配置
builder:
  # 构建命令集
  cmds:
    # maven clean命令
    - [ 'mvn', 'clean' ]
    # maven package命令
    # -P { dev | test | prod }
    - [ 'mvn', '-P test', 'package' ]


# 部署配置
deployment:
  # 部署类型, 可选值: [ static, jar, jar-docker ]
  type: static

  # 静态资源部署
  static:
    # 部署位置
    location: /opt/html
    # 打包后文件位置，相对路径（仅 ${deployment.static.pkg-file} 支持通配符 '*'）
    # 示例：
    # 1、如果 ${deployment.static.pkg-file}=./desc，  将会把 ./desc 文件发布到         ${deployment.static.location} 位置
    # 2、如果 ${deployment.static.pkg-file}=./desc/*，将会把 ./desc 目录下的文件集发布到 ${deployment.static.location} 位置
    pkg-file: ./desc/*
    # 附加文件或目录集
    addl-files: [ ]

  # jar部署, java -jar xxx.jar
  jar:
    # java家目录
    java-home: /opt/jdk-12.0.2
    # 打包后文件位置，相对路径
    pkg-file: ./target/auto-deploy-2022.7
    # 附加文件或目录集
    addl-files:
      # 相对路径的附加文件或目录
      - ./test
      # 绝对路径的附加文件或目录
      - /opt/auto-deploy/config

  # jar docker部署
  jar-docker:
    # 打包后文件位置，相对路径
    pkg-file: ./target/auto-deploy-2022.7
    # 附加文件或目录集
    addl-files: [ ]
    # 执行docker超时时间, 单位s
    timeout: 180
    # docker run
    # https://docs.docker.com/engine/reference/commandline/run/
    run-cmd:
      # 创建一个新的容器并运行一个命令
      - docker run
      # -i: 以交互模式运行容器, 通常与 -t 同时使用；
      # -d: 后台运行容器, 并返回容器ID；
      - -id
      # --name [name]
      # 为容器指定一个名称, 后续可以通过名字进行容器管理
      - --name auto-deploy-2022.7
      # -p [host port]:[container port]
      # 指定端口映射, 格式为: 主机(宿主)端口:容器端口
      - -p 8080:8080
      # -h [hostname]
      # 指定容器的hostname；
      - -h hostname
      # --link=[]
      # 添加链接到另一个容器
      - --link redis
      # --add-host [hostname]:[ip]
      # --add-host list           Add a custom host-to-IP mapping (host:ip)
      - --add-host db:10.194.188.183
      # -t: 为容器重新分配一个伪输入终端, 通常与 -i 同时使用
      - -t org/auto-deploy:2022.7
```
