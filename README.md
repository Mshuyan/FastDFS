# FastDFS

## 介绍

> 参见：[CentOS 7下FastDFS分布式文件服务器搭建实战](https://www.linuxidc.com/Linux/2017-05/143633.htm) 

### FastDFS

FastDFS是一个开源的轻量级分布式文件系统，由跟踪服务器（tracker server）、存储服务器（storage server）和客户端（client）三个部分组成，主要解决了海量数据存储问题，特别适合以中小文件（建议范围：4KB < file_size <500MB）为载体的在线服务。

### Tracker（跟踪器）

Tracker主要做调度工作，相当于mvc中的controller的角色，在访问上起负载均衡的作用。跟踪器和存储节点都可以由一台或多台服务器构成，跟踪器和存储节点中的服务器均可以随时增加或下线而不会影响线上服务，其中跟踪器中的所有服务器都是对等的，可以根据服务器的压力情况随时增加或减少。Tracker负责管理所有的Storage和group，每个storage在启动后会连接Tracker，告知自己所属的group等信息，并保持周期性的心跳，tracker根据storage的心跳信息，建立group==>[storage server list]的映射表，Tracker需要管理的元信息很少，会全部存储在内存中；另外tracker上的元信息都是由storage汇报的信息生成的，本身不需要持久化任何数据，这样使得tracker非常容易扩展，直接增加tracker机器即可扩展为tracker cluster来服务，cluster里每个tracker之间是完全对等的，所有的tracker都接受stroage的心跳信息，生成元数据信息来提供读写服务。

### Storage（存储节点）

Storage采用了分卷[Volume]（或分组[group]）的组织方式，存储系统由一个或多个组组成，组与组之间的文件是相互独立的，所有组的文件容量累加就是整个存储系统中的文件容量。一个卷[Volume]（组[group]）可以由一台或多台存储服务器组成，一个组中的存储服务器中的文件都是相同的，组中的多台存储服务器起到了冗余备份和负载均衡的作用，数据互为备份，存储空间以group内容量最小的storage为准，所以建议group内的多个storage尽量配置相同，以免造成存储空间的浪费。

## 安装

> 参见：[FastDFS 文件服务器的搭建](https://blog.csdn.net/qq_33547169/article/details/77923298) 

### 下载

+ [libfastcommon](https://github.com/happyfish100/libfastcommon/releases) 
+ [fastdfs](https://github.com/happyfish100/fastdfs/releases) 
+ [fastdfs-nginx-module](https://github.com/happyfish100/fastdfs-nginx-module/releases) 
+ [nginx](http://nginx.org/en/download.html) 

### 安装fastdfs

+ 安装`gcc`环境

  ```shell
  yum install -y gcc-c++
  ```

+ 安装`libevent`，`FastDFS`依赖`libevent`库

  ```shell
  yum install -y libevent
  ```

+ 安装`libfastcommon`

  > 上传`libfastcommonV1.0.39.tar.gz`，解压安装

  ```shell
  $ tar -zxvf libfastcommonV1.0.39.tar.gz
  $ cd libfastcommon-1.0.39/
  $ ./make.sh
  $ ./make.sh install
  ```

+ 安装`FastDFS`

  + 解压安装

    > 上传`fastdfs-5.11.tar.gz`，解压安装

    ```shell
    $ tar -zxvf fastdfs-5.11.tar.gz
    $ cd fastdfs-5.11/
    $ ./make.sh
    $ ./make.sh install
    ```

  + 拷贝`FastDFS/conf`目录下的文件到`/etc/fdfs`目录下

    ```shell
    $ cp conf/* /etc/fdfs/
    ```

+ `fastdfs`配置

  > 本例中是在单机中同时配置`tracker`和`storage`，如果`tracker`和`storage`配置在不同的机器中，则只配置自己相关的配置就可以；`tracker`和`storage`分别可以配置多个

  + 创建目录

    > 找个目录来创建`fastdfs`存放数据使用的目录，这里以`/usr/local/`为例

    ```shell
    $ cd /usr/local/
    $ mkdir fdfs
    $ cd fdfs/
    $ mkdir tracker
    $ mkdir storage
    $ mkdir client
    ```

    > 进入`/etc/fdfs`下进行配置

  + 配置`tracker`

    > 将`/etc/fdfs/tracker.conf`文件中的`base_path`配置为刚才创建的`tracker`目录

    ```shell
    base_path=/usr/local/fdfs/tracker
    ```

  + 配置`storage`

    ```shell
    $ vim /etc/fdfs/storage.conf
    ```

    需要修改信息如下：

    ```shell
    # 指定storage的组名
    group_name=cpmall
    base_path=/usr/local/fdfs/storage
    # 数据存储路径，如果有多个挂载磁盘则定义多个store_path，如：
    store_path0=/usr/local/fdfs/storage
    # store_path1=...
    
    # 配置tracker服务器ip
    # 该ip不可以为 127.0.0.1 或 localhost
    # 如果存在多个tracker服务器，可以配置多个tracker_server
    tracker_server=10.211.55.4:22122   
    # tracker_server=10.211.55.3:22122  
    ```

### 启动测试

#### 常用命令

+ 启动

  ```shell
  $ /usr/bin/fdfs_trackerd /etc/fdfs/tracker.conf
  $ /usr/bin/fdfs_storaged /etc/fdfs/storage.conf
  ```

+ 重启

  ```shell
  $ /usr/local/bin/restart.sh /usr/local/bin/fdfs_trackerd /etc/fdfs/tracker.conf
  $ /usr/local/bin/restart.sh /usr/local/bin/fdfs_storaged /etc/fdfs/storage.conf
  ```

+ 停止

  + kill

    ```shell
    [root@centos-7 fdfs]# ps -ef | grep fdfs
    root     20522     1  0 10:00 ?        00:00:00 /usr/bin/fdfs_trackerd /etc/fdfs/tracker.conf
    root     20594     1  0 10:00 ?        00:00:02 /usr/bin/fdfs_storaged /etc/fdfs/storage.conf
    root     21780  6377  0 10:06 pts/1    00:00:00 grep --color=auto fdfs
    [root@centos-7 fdfs]# kill 20522
    ```

  + killall

    ```shell
    $ killall fdfs_trackerd
    $ killall fdfs_storaged
    ```

  + `/usr/bin/stop.sh`

    ```shell
    $ /usr/local/bin/stop.sh /usr/local/bin/fdfs_trackerd /etc/fdfs/tracker.conf
    $ /usr/local/bin/stop.sh /usr/local/bin/fdfs_storaged /etc/fdfs/storage.conf
    ```

#### 启动

> 执行如下命令，启动`tracker`和`storage`

```shell
$ /usr/bin/fdfs_trackerd /etc/fdfs/tracker.conf
$ /usr/bin/fdfs_storaged /etc/fdfs/storage.conf
```

#### 测试

> `fdfs`自带了`fdfs_test`程序，通过该程序可以进行测试

+ 修改`/etc/fdfs/client.conf`文件

  > 修改内容如下

  ```shell
  base_path=/usr/local/fdfs/client
  tracker_server=10.211.55.4:22122
  ```

+ 上传文件

  ```shell
  $ cd /etc/fdfs/
  $ /usr/bin/fdfs_test /etc/fdfs/client.conf upload anti-steal.jpg
  ```

  > + 没有打印`error`则前面配置正确，且上传成功
  >
  > + 上传成功后会打印文件的`remote_filename`和`url`，如：
  >
  >   ```
  >   remote_filename=M00/00/00/CtM3BFvbtbqAFmNPAABdrSqbHGQ066_big.jpg
  >   url: http://10.211.55.4/cpmall/M00/00/00/CtM3BFvbtbqAFmNPAABdrSqbHGQ066_big.jpg
  >   ```
  >
  >   根据`remote_filename`，该文件的存储位置为`/usr/local/fdfs/storage/data/00/00`
  >
  > + `fdfs_test`会将文件上传2次，并产生4个文件，其中2个文件名以`_big`结尾
  >
  >   ```shell
  >   $ cd /usr/local/fdfs/storage/data/00/00/
  >   $ ll
  >   total 56
  >   -rw-r--r--. 1 root root 23981 Nov  2 10:26 CtM3BFvbtbqAFmNPAABdrSqbHGQ066_big.jpg
  >   -rw-r--r--. 1 root root    49 Nov  2 10:26 CtM3BFvbtbqAFmNPAABdrSqbHGQ066_big.jpg-m
  >   -rw-r--r--. 1 root root 23981 Nov  2 10:26 CtM3BFvbtbqAFmNPAABdrSqbHGQ066.jpg
  >   -rw-r--r--. 1 root root    49 Nov  2 10:26 CtM3BFvbtbqAFmNPAABdrSqbHGQ066.jpg-m
  >   ```
  >
  > + 现在还没有配置nginx，无法进行下载

### 安装nginx提供下载

> `nginx`只安装在`storage`服务器上即可

#### 安装`fastdfs-nginx-module`

+ 上传解压

+ 修改`src/config`文件

  ```shell
  [root@centos-7 src]# pwd
  /home/shuyan/fastdfs-nginx-module-1.20/src
  [root@centos-7 src]# vim config
  ```

  > 修改内容如下

  ```shell
  ngx_addon_name=ngx_http_fastdfs_module
  
  if test -n "${ngx_module_link}"; then
      ngx_module_type=HTTP
      ngx_module_name=$ngx_addon_name
  # 修改前
  #    ngx_module_incs="/usr/local/include"
  # 修改后
  	ngx_module_incs="/usr/include/fastdfs /usr/include/fastcommon/"
  	
      ngx_module_libs="-lfastcommon -lfdfsclient"
      ngx_module_srcs="$ngx_addon_dir/ngx_http_fastdfs_module.c"
      ngx_module_deps=
      CFLAGS="$CFLAGS -D_FILE_OFFSET_BITS=64 -DFDFS_OUTPUT_CHUNK_SIZE='256*1024' -DFDFS_MOD_CONF_FILENAME='\"/etc/fdfs/mod_fastdfs.conf\"'"
      . auto/module
  else
      HTTP_MODULES="$HTTP_MODULES ngx_http_fastdfs_module"
      NGX_ADDON_SRCS="$NGX_ADDON_SRCS $ngx_addon_dir/ngx_http_fastdfs_module.c"
  # 修改前
  #     CORE_INCS="$CORE_INCS /usr/local/include"
  # 修改后
  	CORE_INCS="$CORE_INCS /usr/include/fastdfs /usr/include/fastcommon/"
  	
      CORE_LIBS="$CORE_LIBS -lfastcommon -lfdfsclient"
      CFLAGS="$CFLAGS -D_FILE_OFFSET_BITS=64 -DFDFS_OUTPUT_CHUNK_SIZE='256*1024' -DFDFS_MOD_CONF_FILENAME='\"/etc/fdfs/mod_fastdfs.conf\"'"
  fi
  ```

+ 拷贝`mod_fastdfs.conf`到`/etc/fdfs/`

  ```shell
  [root@centos-7 src]# pwd
  /home/shuyan/fastdfs-nginx-module-1.20/src
  [root@centos-7 src]# cp mod_fastdfs.conf /etc/fdfs/
  ```

+ 修改`/etc/fdfs/mod_fastdfs.conf`

  > 需要修改内容如下

  ```shell
  base_path=/usr/local/fastdfs/storage
  tracker_server=10.211.55.4:22122
  # 设置组名
  group_name=cpmall
  # url中是否包含组名
  url_have_group_name=true
  # 指定storage使用的文件存储路径，根据该路径下载文件
  store_path0=/usr/local/fastdfs/storage 
  ```

#### 安装nginx

+ 安装`pcre`

  ```shell
  $ yum install –y pcre pcre-devel
  ```

+ 安装`zlib`

  ```shell
  $ yum install –y zlib zlib-devel
  ```

+ 安装`openssl`

  ```shell
  $ yum install –y openssl openssl-devel
  ```

+ 安装`nginx`

  + 上传解压

  + 添加模块

    ```shell
    $ cd nginx-1.14.0/
    $ ./configure --add-module=/home/shuyan/fastdfs-nginx-module-1.20/src
    ```

  + 编译安装

    ```shell
    $ make
    $ make install
    ```

  + 修改`nginx`配置文件

    ```shell
    $ vim /usr/local/nginx/conf/nginx.conf
    ```

    > 添加如下内容

    ```shell
    server {
            listen       80;
            server_name  10.211.55.4;
            location     /cpmall/M00/{
                    ngx_fastdfs_module;
            }
        }
    ```

  + 启动nginx并访问

    > 启动nginx

    ```shell
    $ /usr/local/nginx/sbin/nginx
    ```

    > 防火墙开启80端口

    ```shell
    $ firewall-cmd --zone=public --add-port=80/tcp --permanent
    $ systemctl restart firewalld
    ```

    > 访问上面上传测试中返回的url，即可查看上传的图片

## 上传下载（java）

> 基于springboot整合
>
> 参见[Spring Boot集成FastDFS](https://blog.csdn.net/qq_31871785/article/details/75174554) 

+ 引入依赖

  ```xml
  <dependency>
      <groupId>com.github.tobato</groupId>
      <artifactId>fastdfs-client</artifactId>
      <version>1.26.3</version>
  </dependency>
  ```

+ 启动类配置

  > 在启动类上加上如下2个注解

  ```java
  @Import(FdfsClientConfig.class)
  @EnableMBeanExport(registration = RegistrationPolicy.IGNORE_EXISTING)
  ```

+ `application.properties`

  ```properties
  # 读取时间
  fdfs.so-timeout=1501
  fdfs.connect-timeout=601
  # 缩略图大小
  fdfs.thumb-image.width=150
  fdfs.thumb-image.height=150
  # tracker服务器地址
  fdfs.tracker-list[0]=10.211.55.4:22122
  # 线程配置
  fdfs.pool.max-total=100
  fdfs.pool.max-wait-millis=60
  ```

  > **注意**
  >
  > 必须先打开`tracker`的`22122`和`23000`端口，否则无法连接

+ 主从文件上传

  ```java
  @Autowired
  private FastFileStorageClient fastFileStorageClient;
  
  @Test
  public void upload() {
      File file = new File("/Users/will/Downloads/1039111.jpg");
      StorePath storePath;
      try (FileInputStream inputStream = new FileInputStream(file)) {
          // 上传主文件
          storePath = fastFileStorageClient.uploadFile(inputStream, file.length(), "jpg", null);
          System.out.println("主文件上传成功 " + storePath.getGroup() + " " + storePath.getPath());
      } catch (Exception e) {
          return;
      }
      try (FileInputStream inputStream = new FileInputStream(file)) {
          // 上传从文件
          // prefixName：指定从文件的尾缀名，`.jpg`之前的内容，而不是添加在整个文件名之前；该参数不能为null或""
          // fileExtName：指定文件后缀名，必须是该文件真实的后缀名
          fastFileStorageClient.uploadSlaveFile(storePath.getGroup(), storePath.getPath(), inputStream, file.length(), "_slave", "jpg");
          System.out.println("从文件上传成功");
      } catch (Exception e) {
          e.printStackTrace();
      }
  }
  ```

  > 主从文件概念参见[主从文件](#主从文件) 

+ 上传并创建缩略图

  ```java
  @Test
  public void uploadCrtThumbImage() {
      File file = new File("/Users/will/Downloads/1039111.jpg");
      try (FileInputStream inputStream = new FileInputStream(file)) {
          // 测试上传原图片并自动创建缩略图
          // 缩略图尺寸在 application.properties 中定义
          // 缩略图文件名为：storePath.getPath()_宽x高.后缀名；如：CtM3BFvcF6iALYiSAAAvfv9Uu40001_130x150.jpg
          StorePath storePath = fastFileStorageClient.uploadImageAndCrtThumbImage(inputStream, file.length(), "jpg", null);
          System.out.println(storePath.getGroup() + "  " + storePath.getPath());
      } catch (Exception e) {
          e.printStackTrace();
      }
  }
  ```

+ 下载及删除参见[FastdfsApplicationTests.java](src/test/java/com/shuyan/fastdfs/FastdfsApplicationTests.java) 

## 概念

### 主从文件

+ `主从文件`只在`文件名`上存在联系，其他方面没有任何联系
+ `fastdfs`不会自动生成从文件，从文件是应用层自己上传的，`fastdfs`只不过使用主从文件的模式使这两个文件在文件名上产生关联，方便查找