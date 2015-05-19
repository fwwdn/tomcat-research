# tomcat-research
Tomcat源码研究学习

##Tomcat简介

- Tomcat比较关键的几个目录:

	- /bin - Startup, shutdown, and other scripts. The *.sh files (for Unix systems) are functional duplicates of the *.bat files (for Windows systems). Since the Win32 command-line lacks certain functionality, there are some additional files in here.
	- /conf - Configuration files and related DTDs. The most important file in here is server.xml. It is the main configuration file for the container.
	- /logs - Log files are here by default.
	- /webapps - This is where your webapps go.

##Building Tomcat

- Eclipse工程
	- 下载源码：https://github.com/apache/tomcat
	- 安装ant1.8.2以上版本
	- 进到tomcat目录，执行ant命令：ant ide-eclipse
	- import eclipse
- Tomcat启动过程分析
