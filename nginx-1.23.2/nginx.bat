@echo off
rem 提供Windows下nginx的启动，重启，关闭功能
 
echo ========================begin========================

cls 
::ngxin 所在的盘符
set NGINX_PATH=%~d0
 echo
::nginx 所在目录
set NGINX_DIR=%~dp0\
color 0a 
TITLE Nginx 管理程序增强版
 
CLS 
   
echo.Yb  dP Yb  dP 8888b.  .dP"Y8 
echo. YbdP   YbdP   8I  Yb `Ybo." 
echo.  8P     8P    8I  dY o.`Y8b 
echo. dP     dP    8888Y"  8bodP' 

echo. =============================== Nginx 管理程序 ===============================
echo. create 2022-04-08 			       
echo. 作者:刘苏义 			       
 
:MENU 
 
echo. ***** nginx 进程list ****** 
::tasklist|findstr /i "nginx.exe"
::tasklist /fi "imagename eq nginx.exe"
tasklist|findstr /i "nginx.exe"
 
echo. ------------------------------------------------------------------------------
 
    if "%errorlevel%"=="1" (
        echo  nginx.exe 不存在
    ) else (
        echo  nginx.exe 存在
    )
 
echo. 
::*************************************************************************************************************
echo. 
	echo.  [1] 启动Nginx  
	echo.  [2] 关闭Nginx  
	echo.  [3] 重启Nginx 
	echo.  [4] 刷新控制台  
	echo.  [5] 重新加载Nginx配置文件
	echo.  [6] 检查测试nginx配置文件
	echo.  [7] 查看nginx version
	echo.  [0] 退 出 
echo. 
 
echo.请输入选择的序号:
set /p ID=
	IF "%id%"=="1" GOTO start 
	IF "%id%"=="2" GOTO stop 
	IF "%id%"=="3" GOTO restart 
	IF "%id%"=="4" GOTO MENU
	IF "%id%"=="5" GOTO reloadConf 
	IF "%id%"=="6" GOTO checkConf 
	IF "%id%"=="7" GOTO showVersion 
	IF "%id%"=="0" EXIT
PAUSE 
 
::*************************************************************************************************************
::启动
:start 
	call :startNginx
	GOTO MENU
 
::停止
:stop 
	call :shutdownNginx
	GOTO MENU
 
::重启
:restart 
	call :shutdownNginx
	call :startNginx
	GOTO MENU
 
::检查测试配置文件
:checkConf 
	call :checkConfNginx
	GOTO MENU
 
::重新加载Nginx配置文件
:reloadConf 
    call :checkConfNginx
	call :reloadConfNginx
	GOTO MENU
	
::显示nginx版本
:showVersion 
    call :showVersionNginx
	GOTO MENU	
	
	
::*************************************************************************************
::底层
::*************************************************************************************
:shutdownNginx
	echo. 
	echo.关闭Nginx...... 
	taskkill /F /IM nginx.exe > nul
	echo.OK,关闭所有nginx 进程
	goto :eof
 
:startNginx
	echo. 
	echo.启动Nginx...... 
	IF NOT EXIST "%NGINX_DIR%nginx.exe" (
        echo "%NGINX_DIR%nginx.exe"不存在
        goto :eof
     )
    if "%errorlevel%"=="1" (
        %NGINX_PATH% 
	cd "%NGINX_DIR%" 
 
	IF EXIST "%NGINX_DIR%nginx.exe" (
		echo "start '' nginx.exe"
		start "" nginx.exe
	)
	echo.OK
    ) else (
        echo nginx.exe存在
    )
	
	goto :eof
	
 
:checkConfNginx
	echo. 
	echo.检查测试 nginx 配置文件...... 
	IF NOT EXIST "%NGINX_DIR%nginx.exe" (
        echo "%NGINX_DIR%nginx.exe"不存在
        goto :eof
     )
 
	%NGINX_PATH% 
	cd "%NGINX_DIR%" 
	nginx -t -c conf/nginx.conf
 
	goto :eof
	
::重新加载 nginx 配置文件
:reloadConfNginx
	echo. 
	echo.重新加载 nginx 配置文件...... 
	IF NOT EXIST "%NGINX_DIR%nginx.exe" (
        echo "%NGINX_DIR%nginx.exe"不存在
        goto :eof
     )
 
	%NGINX_PATH% 
	cd "%NGINX_DIR%" 
	nginx -s reload
 
	goto :eof
	
::显示nginx版本
:showVersionNginx
	echo. 
	%NGINX_PATH% 
	cd "%NGINX_DIR%" 
	nginx -V
 	goto :eof